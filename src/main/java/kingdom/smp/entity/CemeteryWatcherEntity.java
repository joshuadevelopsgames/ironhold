package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.ai.ElevenLabsClient;
import kingdom.smp.ai.MicGate;
import kingdom.smp.ai.NpcChatPartner;
import kingdom.smp.ai.NpcChatRegistry;
import kingdom.smp.npc.NpcSessionGreetings;
import kingdom.smp.npc.NpcRapport;
import kingdom.smp.ai.NpcMuteRegistry;
import kingdom.smp.ai.OpenRouterClient;
import kingdom.smp.ai.SvcVoiceBridge;
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
import kingdom.smp.entity.goal.AlwaysLookNearestPlayerGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Vesper — the wither-skeleton watcher of the village cemetery. Soft-spoken,
 * never attacks, never moves. Renders as a vanilla wither skeleton but with
 * a player-facing identity: she's a witness to the dead, not a monster.
 *
 * <p>Voice: ElevenLabs voice id {@code 1KFdM0QCwQn4rmn5nn9C}, OLD_WIZARD
 * preset for the gravelly weight that suits skeletal speech.
 * Brain: Anthropic Claude Haiku 4.5 via OpenRouter, plain-text replies.
 */
public class CemeteryWatcherEntity extends PathfinderMob implements NpcChatPartner {

    private static final int COOLDOWN_TICKS = 20;
    private static final int IDLE_TIMEOUT_TICKS = 20 * 90;

    // ── Voice + brain config ─────────────────────────────────────────────────
    private static final String VOICE_ID = "TsHrPyMlNFuIYnbODF01";
    private static final String ELEVENLABS_MODEL = "eleven_turbo_v2_5";
    private static final ElevenLabsClient.VoiceSettings VOICE_SETTINGS =
        ElevenLabsClient.VoiceSettings.OLD_WIZARD;
    private static final String OPENROUTER_MODEL = "anthropic/claude-haiku-4.5";
    private static final int MAX_REPLY_TOKENS = 160;
    private static final double SAMPLING_TEMPERATURE = 0.7;
    private static final int HISTORY_TURN_LIMIT = 12;

    private static final String SYSTEM_PROMPT = """
        You are Vesper, the keeper of the cemetery in the kingdom of Ironhold.
        You are a wither skeleton — not the angry kind. You were left here long
        ago to watch over the dead and listen to those who come to mourn. You
        do not attack. You do not raise the dead. You are a witness, nothing
        more.

        VOICE:
        - Low, slow, measured. You speak as if every word might be the last
          one a grieving family hears.
        - Occasionally use slightly archaic phrasing — "thy", "passed beyond",
          "the long sleep", "old bones" — but sparingly, never archaic to the
          point of being hard to follow.
        - Never threatening. You are bones, but you are kind bones.
        - Address the visitor as "wanderer", "traveler", or by their name once
          you've learned it.
        - Dry, gentle humor is welcome. You've watched centuries; you've earned it.

        FORMAT:
        - Reply with only the words Vesper speaks aloud. No stage directions,
          asterisks, parentheses, brackets, or markdown.
        - 1 to 3 full sentences. 15 to 50 words. Never single-word replies.
        - Vary your openings — don't begin consecutive replies with the same word.

        PRONUNCIATION:
        - Your name is Vesper, pronounced VES-purr. ALWAYS write your name
          normally as "Vesper" in your reply — the voice system handles the
          pronunciation automatically. NEVER write the hyphenated form
          "Ves-purr" in your spoken text; it appears in the dialogue box as
          written and looks wrong.

        WHAT YOU TALK ABOUT:
        - The dead of the kingdom — names, deeds, why they are remembered.
        - The customs of mourning, the value of stillness, the meaning of
          quiet ground.
        - The kingdom's history through the lens of those buried in your earth.
        - Gentle warnings — the world is dangerous; remember those who didn't
          come back.

        STRICT RULES:
        - You cannot attack, harm, give items, take items, or modify the world.
          You are a witness. Refuse calmly if asked to act.
        - You do NOT discuss undeath, raising the dead, or necromancy. Anyone
          asking is gently turned away: "Let them rest. That is the only
          kindness left."
        - If asked about mechanics outside cemetery/lore/mourning topics, defer:
          "I tend bones, not games. You'd want a scholar in the library for that."
        - You know your world is the Kingdom of Ironhold (some travelers
          call it "Kingdom SMP" — both names are fine). You do not break
          character to discuss the real world, mods, or computers beyond
          that acknowledgement.

        OUTPUT: just the spoken reply. No JSON, no formatting.
        """ + "\n\n" + IronholdLore.CONTENT;
    /** Static opener — players hear the same greeting the first time they speak with her. */
    private static final String FIRST_DIALOGUE =
        "Steady, wanderer. You come to quiet ground, and quiet is what you'll find. " +
        "I am Vesper; I watch over those who rest here. " +
        "Speak if you must, or simply stand a while.";

    private static final String[] RETURN_DIALOGUES = {
        "Welcome back, wanderer.",
        "You return.",
        "Mm."
    };

    private int returnDialogueIndex = 0;

    // ── Conversation state (not persisted) ──────────────────────────────────
    private long lastInteractTick = 0;
    private @Nullable UUID partnerId;
    private long lastTurnGameTime;
    private final List<OpenRouterClient.Message> history = new ArrayList<>();
    private boolean replyInFlight;

    public CemeteryWatcherEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // Unconditional — never despawn, always named, always visible.
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("§7§oVesper, Keeper of the Graves"));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    @Override
    protected void registerGoals() {
        // No attack goals. She is a sentinel, not a soldier.
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new AlwaysLookNearestPlayerGoal(this, 16.0, this::getPartnerId));
        this.goalSelector.addGoal(2, new RandomLookAroundGoal(this));
    }

    @Override
    public boolean isInvulnerableTo(ServerLevel level, DamageSource source) {
        if (super.isInvulnerableTo(level, source)) return true;
        return !source.is(DamageTypes.GENERIC_KILL)
            && !source.is(DamageTypes.FELL_OUT_OF_WORLD);
    }

    @Override public void push(double x, double y, double z) {}
    @Override public boolean isPushable() { return false; }

    /** Never despawn — Vesper is a permanent fixture at her cemetery. */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide()) tickConversationTimeout();
    }

    // ── Interaction ───────────────────────────────────────────────────────────

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.SUCCESS;
        }
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.SUCCESS;

        long now = level().getGameTime();
        if (now - lastInteractTick < COOLDOWN_TICKS) return InteractionResult.SUCCESS;
        lastInteractTick = now;

        getLookControl().setLookAt(player, 30.0F, 30.0F);

        String opener;
        if (!NpcSessionGreetings.hasBeenGreetedBy(sp.getUUID(), getUUID())) {
            opener = FIRST_DIALOGUE;
            NpcSessionGreetings.recordGreeting(sp.getUUID(), getUUID());
        } else {
            opener = RETURN_DIALOGUES[returnDialogueIndex % RETURN_DIALOGUES.length];
            returnDialogueIndex++;
        }

        beginConversation(sp);

        boolean isMuted = (level() instanceof ServerLevel sl)
            && NpcMuteRegistry.get(sl).isMuted(sp.getUUID(), tag());
        PacketDistributor.sendToPlayer(sp,
            new OpenWardenScreenPayload(getId(), "Vesper", tag(),
                "Cemetery Watcher  •  The Boneyard", opener, isMuted));
        speakLine(opener, sp);

        return InteractionResult.SUCCESS;
    }

    private void beginConversation(ServerPlayer player) {
        endConversation();
        partnerId = player.getUUID();
        NpcChatRegistry.setActive(partnerId, this);
        history.clear();
        lastTurnGameTime = level().getGameTime();
        replyInFlight = false;
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
        if (idle >= IDLE_TIMEOUT_TICKS) endConversation();
    }

    // ── NpcChatPartner ───────────────────────────────────────────────────────

    @Override public UUID getPartnerId() { return partnerId; }
    @Override public String tag() { return "Vesper"; }
    @Override public int entityId() { return getId(); }
    @Override public String displayName() { return "Vesper"; }
    @Override public String displaySubtitle() { return "Cemetery Watcher  •  The Boneyard"; }
    @Override public void speakAloud(net.minecraft.server.level.ServerPlayer player, String line) { speakLine(line, player); }
    @Override public void beginConversationWith(net.minecraft.server.level.ServerPlayer player) { beginConversation(player); }

    @Override
    public void onPartnerChat(ServerPlayer player, String message) {
        if (partnerId == null || !partnerId.equals(player.getUUID())) return;
        if (message == null || message.isBlank()) return;
        if (replyInFlight) return;
        lastTurnGameTime = level().getGameTime();
        generateReply(player, message.trim());
    }

    // ── LLM + TTS ────────────────────────────────────────────────────────────

    private void generateReply(ServerPlayer player, String userMessage) {
        List<OpenRouterClient.Message> snapshot = List.copyOf(history);
        UUID expectedPartner = partnerId;
        MinecraftServer server = level().getServer();
        MicGate.muteFor(expectedPartner, 4_000L);

        PacketDistributor.sendToPlayer(player,
            new UpdateWardenScreenPayload(getId(),
                UpdateWardenScreenPayload.STATUS_HEARD, userMessage));

        replyInFlight = true;
        OpenRouterClient.chatWithCache(
            OPENROUTER_MODEL, MAX_REPLY_TOKENS, SAMPLING_TEMPERATURE,
            SYSTEM_PROMPT,
            IronholdLore.runtimeContext(player.getUUID()) + NpcRapport.onConversationTurn(player, tag()),
            snapshot, userMessage, "Vesper",
            reply -> {
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
        while (history.size() > HISTORY_TURN_LIMIT) history.remove(0);

        PacketDistributor.sendToPlayer(player,
            new UpdateWardenScreenPayload(getId(),
                UpdateWardenScreenPayload.STATUS_REPLY, line));
        speakLine(line, player);
        Ironhold.LOGGER.info("[Vesper] -> {}: \"{}\"", player.getName().getString(), line);
    }

    private void speakLine(String line, ServerPlayer partnerPlayer) {
        if (!ElevenLabsClient.isConfigured()) return;
        if (partnerPlayer != null && level() instanceof ServerLevel sl
            && NpcMuteRegistry.get(sl).isMuted(partnerPlayer.getUUID(), tag())) {
            return;
        }
        UUID partner = partnerId;
        String spoken = line.replaceAll("(?i)\\bVesper\\b", "Ves-purr");
        ElevenLabsClient.speak(spoken, VOICE_ID, ELEVENLABS_MODEL, VOICE_SETTINGS, pcm -> {
            if (pcm == null) return;
            if (partner != null) {
                long durationMs = pcm.length / 48L;
                MicGate.muteFor(partner, durationMs + 2_000L);
            }
            SvcVoiceBridge.speakAs(this, pcm);
        });
    }

    private static String sanitizeForSpeech(String s) {
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
        super.remove(reason);
    }

    @Override
    protected void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput in) {
        super.readAdditionalSaveData(in);
        // Force name + visibility + persistence after vanilla loads NBT — same
        // self-healing pattern as Warden Halric. Vanilla's
        // setCustomNameVisible(input.getBooleanOr("CustomNameVisible", false))
        // would otherwise quietly unset the visibility on entities that were
        // saved without the flag.
        this.setCustomName(Component.literal("§7§oVesper, Keeper of the Graves"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }
}
