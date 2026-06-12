package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.ai.ElevenLabsClient;
import kingdom.smp.ai.MicGate;
import kingdom.smp.ai.NpcChatPartner;
import kingdom.smp.ai.NpcChatRegistry;
import kingdom.smp.npc.NpcSessionGreetings;
import kingdom.smp.npc.NpcRapport;
import kingdom.smp.ai.NpcMuteRegistry;
import kingdom.smp.ai.NpcSpeech;
import kingdom.smp.ai.OpenAiWhisperClient;
import kingdom.smp.ai.OpenRouterClient;
import kingdom.smp.ai.SentenceChunker;
import kingdom.smp.entity.goal.AlwaysLookNearestPlayerGoal;
import kingdom.smp.net.OpenWardenScreenPayload;
import kingdom.smp.net.UpdateWardenScreenPayload;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Shared base for the village's voiced AI NPCs (Mira, Cedric, Wren, Bram,
 * Roselind, Eilan, Hesta, Tobias, …). Holds the entire right-click → opener →
 * LLM reply → ElevenLabs voice → Simple Voice Chat conversation pipeline. A
 * concrete NPC supplies only its personality: voice id, system prompt, opener +
 * return greetings, display strings, and (optionally) a couple of tunables.
 *
 * <p>This used to be copy-pasted, near byte-for-byte, into every NPC entity, so
 * a fix in one had to be hand-ported to the others. Keep behaviour changes here.
 */
public abstract class AbstractVoicedNpcEntity extends PathfinderMob implements NpcChatPartner {

    // ── Per-NPC personality (required) ───────────────────────────────────────
    /** ElevenLabs voice id. Return a value starting with "REPLACE_ME" to disable voice until assigned. */
    protected abstract String voiceId();
    /** The full system prompt that defines this NPC's character. */
    protected abstract String systemPrompt();
    /** Line spoken the first time a given player greets this NPC in a session. */
    protected abstract String firstDialogue();
    /** Return-greeting templates (each may contain one %s for the player name); cycled in order. */
    protected abstract String[] returnDialogues();
    /** The styled name shown floating above the entity and re-applied on load. */
    protected abstract Component customNameComponent();

    // ── Per-NPC tunables (override only when they differ from these defaults) ──
    protected String elevenLabsModel() { return "eleven_turbo_v2_5"; }
    protected ElevenLabsClient.VoiceSettings voiceSettings() { return ElevenLabsClient.VoiceSettings.DEFAULT; }
    protected String openRouterModel() { return "anthropic/claude-haiku-4.5"; }
    protected int maxReplyTokens() { return 180; }
    protected double samplingTemperature() { return 0.7; }
    protected int historyTurnLimit() { return 12; }
    protected int cooldownTicks() { return 20; }
    protected int idleTimeoutTicks() { return 20 * 90; }

    /** Standard attributes for a stationary, unpushable, invulnerable talker. */
    protected static AttributeSupplier.Builder voicedNpcAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    // ── Conversation state (not persisted) ───────────────────────────────────
    private int returnDialogueIndex = 0;
    private long lastInteractTick = 0;
    private @Nullable UUID partnerId;
    private long lastTurnGameTime;
    private final List<OpenRouterClient.Message> history = new ArrayList<>();
    private boolean replyInFlight;
    private ReplyChannel replyChannel = ReplyChannel.SCREEN;

    protected AbstractVoicedNpcEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(customNameComponent());
        this.setCustomNameVisible(true);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AlwaysLookNearestPlayerGoal(this, 16.0, this::getPartnerId));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (super.isInvulnerableTo(level, source)) return true;
        return !source.is(DamageTypes.GENERIC_KILL) && !source.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    @Override public void push(double x, double y, double z) {}
    @Override public boolean isPushable() { return false; }
    @Override public boolean removeWhenFarAway(double distance) { return false; }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide()) tickConversationTimeout();
    }

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;

        long now = level().getGameTime();
        if (now - lastInteractTick < cooldownTicks()) return InteractionResult.SUCCESS;
        lastInteractTick = now;
        getLookControl().setLookAt(player, 30.0F, 30.0F);

        String opener;
        if (!NpcSessionGreetings.hasBeenGreetedBy(sp.getUUID(), getUUID())) {
            opener = firstDialogue();
            NpcSessionGreetings.recordGreeting(sp.getUUID(), getUUID());
        } else {
            String[] returns = returnDialogues();
            String template = returns[returnDialogueIndex % returns.length];
            returnDialogueIndex++;
            opener = String.format(template, sp.getName().getString());
        }

        beginConversation(sp);

        boolean isMuted = (level() instanceof ServerLevel sl)
            && NpcMuteRegistry.get(sl).isMuted(sp.getUUID(), tag());
        PacketDistributor.sendToPlayer(sp,
            new OpenWardenScreenPayload(getId(), displayName(), tag(),
                displaySubtitle(), opener, isMuted));
        speakLine(opener, sp);

        return InteractionResult.SUCCESS;
    }

    protected void beginConversation(ServerPlayer player) {
        endConversation();
        partnerId = player.getUUID();
        NpcChatRegistry.setActive(partnerId, this);
        history.clear();
        lastTurnGameTime = level().getGameTime();
        replyInFlight = false;
        replyChannel = ReplyChannel.SCREEN;
        // Warm the TLS connections now so the first real turn doesn't pay
        // three fresh handshakes on top of model latency.
        OpenRouterClient.prewarm();
        ElevenLabsClient.prewarm();
        OpenAiWhisperClient.prewarm();
    }

    @Override
    public void onMentionTurn(ServerPlayer player, String message) {
        if (message == null || message.isBlank()) return;
        if (!player.getUUID().equals(partnerId)) beginConversation(player);
        replyChannel = ReplyChannel.CHAT;
        if (replyInFlight) return;
        lastTurnGameTime = level().getGameTime();
        generateReply(player, message.trim());
    }

    public void endConversation() {
        if (partnerId != null) {
            NpcChatRegistry.clearActive(partnerId, this);
            partnerId = null;
        }
        history.clear();
        replyInFlight = false;
    }

    private void tickConversationTimeout() {
        if (partnerId == null) return;
        if (level() instanceof ServerLevel sl
            && sl.getServer().getPlayerList().getPlayer(partnerId) == null) {
            endConversation();
            return;
        }
        long idle = level().getGameTime() - lastTurnGameTime;
        if (idle >= idleTimeoutTicks()) endConversation();
    }

    @Override public UUID getPartnerId() { return partnerId; }
    @Override public boolean supportsWhisper() { return true; }
    @Override public int entityId() { return getId(); }
    @Override public void speakAloud(ServerPlayer player, String line) { speakLine(line, player); }
    @Override public void beginConversationWith(ServerPlayer player) { beginConversation(player); }

    @Override
    public void onPartnerChat(ServerPlayer player, String message) {
        if (partnerId == null || !partnerId.equals(player.getUUID())) return;
        if (message == null || message.isBlank()) return;
        if (replyInFlight) return;
        // Screen/voice turns always arrive here; mentions go through onMentionTurn.
        replyChannel = ReplyChannel.SCREEN;
        lastTurnGameTime = level().getGameTime();
        generateReply(player, message.trim());
    }

    private void generateReply(ServerPlayer player, String userMessage) {
        List<OpenRouterClient.Message> snapshot = List.copyOf(history);
        UUID expectedPartner = partnerId;
        MinecraftServer server = level().getServer();
        MicGate.muteFor(expectedPartner, 2_000L);

        if (replyChannel == ReplyChannel.SCREEN) {
            PacketDistributor.sendToPlayer(player,
                new UpdateWardenScreenPayload(getId(),
                    UpdateWardenScreenPayload.STATUS_HEARD, userMessage));
        }

        replyInFlight = true;

        // Voice rides the LLM stream: as each sentence completes, it goes to
        // ElevenLabs immediately, so the NPC starts talking while the model is
        // still writing. Text channels still get the full reply at the end.
        NpcSpeech.Session speech = canSpeakTo(player)
            ? NpcSpeech.beginSession(this, expectedPartner) : null;
        SentenceChunker chunker = speech == null ? null
            : new SentenceChunker(30, sentence -> {
                // If the conversation moved on mid-stream, stop queueing audio.
                if (NpcChatRegistry.getActive(expectedPartner) != this) return;
                String spoken = sanitizeForSpeech(sentence);
                if (!spoken.isBlank()) {
                    speech.speakSentence(spoken, voiceId(), elevenLabsModel(), voiceSettings());
                }
            });

        OpenRouterClient.chatWithCacheStreaming(
            openRouterModel(), maxReplyTokens(), samplingTemperature(),
            systemPrompt(),
            IronholdLore.runtimeContext(player.getUUID()) + NpcRapport.onConversationTurn(player, tag()),
            snapshot, userMessage, tag(),
            delta -> {
                if (chunker != null) chunker.accept(delta);
            },
            reply -> {
                if (chunker != null) chunker.finish();
                if (speech != null) speech.finish();
                if (server == null) return;
                server.execute(() -> {
                    replyInFlight = false;
                    if (reply == null || reply.isBlank()) return;
                    deliverLine(player, expectedPartner, userMessage, sanitizeForSpeech(reply));
                });
            });
    }

    private void deliverLine(ServerPlayer player, UUID expectedPartner,
                             String userMessage, String line) {
        if (partnerId == null || !partnerId.equals(expectedPartner)) return;
        if (line.isBlank()) return;

        history.add(new OpenRouterClient.Message("user", userMessage));
        history.add(new OpenRouterClient.Message("assistant", line));
        while (history.size() > historyTurnLimit()) history.remove(0);

        if (replyChannel == ReplyChannel.CHAT) {
            kingdom.smp.chat.MentionRouter.sendNpcWhisper(player, displayName(), line);
        } else {
            PacketDistributor.sendToPlayer(player,
                new UpdateWardenScreenPayload(getId(),
                    UpdateWardenScreenPayload.STATUS_REPLY, line));
        }
        // No speakLine here — the voice already streamed sentence-by-sentence
        // alongside the LLM in generateReply.
        Ironhold.LOGGER.info("[{}] -> {}: \"{}\"", tag(), player.getName().getString(), line);
    }

    /** Voice gate shared by the opener and streamed replies. */
    private boolean canSpeakTo(@Nullable ServerPlayer partnerPlayer) {
        if (!ElevenLabsClient.isConfigured()) return false;
        if (voiceId().startsWith("REPLACE_ME")) return false; // no voice id assigned yet
        if (partnerPlayer != null && level() instanceof ServerLevel sl
            && NpcMuteRegistry.get(sl).isMuted(partnerPlayer.getUUID(), tag())) {
            return false;
        }
        return true;
    }

    private void speakLine(String line, ServerPlayer partnerPlayer) {
        if (!canSpeakTo(partnerPlayer)) return;
        NpcSpeech.speak(this, partnerId, line, voiceId(), elevenLabsModel(), voiceSettings());
    }

    protected static String sanitizeForSpeech(String s) {
        if (s == null) return "";
        s = s.replaceAll("\\*[^*]{0,200}\\*", " ");
        s = s.replaceAll("\\([^)]{0,200}\\)", " ");
        s = s.replaceAll("\\[[^\\]]{0,200}\\]", " ");
        s = s.replaceAll("_[^_]{0,200}_", " ");
        s = s.replaceAll("\\s+", " ").trim();
        while (s.length() >= 2) {
            char first = s.charAt(0), last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"')
                || (first == '“' && last == '”')
                || (first == '\'' && last == '\'')) {
                s = s.substring(1, s.length() - 1).trim();
            } else break;
        }
        return s;
    }

    @Override
    public void remove(RemovalReason reason) {
        endConversation();
        kingdom.smp.chat.NpcMentionRegistry.unregister(this);
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.setCustomName(customNameComponent());
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }
}
