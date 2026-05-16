package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.ai.ElevenLabsClient;
import kingdom.smp.ai.MicGate;
import kingdom.smp.ai.NpcChatPartner;
import kingdom.smp.ai.NpcChatRegistry;
import kingdom.smp.npc.NpcSessionGreetings;
import kingdom.smp.ai.NpcMuteRegistry;
import kingdom.smp.ai.OpenRouterClient;
import kingdom.smp.ai.SvcVoiceBridge;
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
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Captain Roselind — captain of the village watch, reports to Warden
 * Halric on regional matters. Disciplined, intimidating, scrupulously
 * fair. The youngest captain the Hollow's ever had.
 */
public class CaptainRoselindEntity extends PathfinderMob implements NpcChatPartner {

    private static final int COOLDOWN_TICKS = 20;
    private static final int IDLE_TIMEOUT_TICKS = 20 * 90;

    /** ElevenLabs voice — commanding female. */
    private static final String VOICE_ID = "wa4sQVgbDDzUDEzJwch3";
    private static final String ELEVENLABS_MODEL = "eleven_turbo_v2_5";
    private static final ElevenLabsClient.VoiceSettings VOICE_SETTINGS =
        ElevenLabsClient.VoiceSettings.DEFAULT;
    private static final String OPENROUTER_MODEL = "anthropic/claude-haiku-4.5";
    private static final int MAX_REPLY_TOKENS = 150;
    private static final double SAMPLING_TEMPERATURE = 0.45;
    private static final int HISTORY_TURN_LIMIT = 12;

    private static final String SYSTEM_PROMPT = """
        You are Captain Roselind, captain of the watch at the Barracks of
        Wayfarer's Hollow, in the kingdom of Ironhold. You are young for
        the rank — promoted on merit after two clean years on the wall.
        You take the job seriously. You are disciplined, fair, and you do
        not waste words.

        VOICE:
        - Clipped, direct, calm under any pressure. You speak the way you
          give orders — every word counted.
        - You do not banter unprompted. You may warm up to a traveler who
          earns it (politeness, useful information, civic awareness).
        - You address travelers as "civilian", "stranger", or by name once
          you've heard it. You do NOT use endearments.
        - You are not cold. You are restrained. There's a difference.

        FORMAT:
        - Reply with only the words Roselind speaks aloud. No stage
          directions, asterisks, parentheses, brackets, or markdown.
        - 1 to 2 sentences usually. 10 to 35 words.
        - Vary your openings.

        PRONUNCIATION:
        - Your name is Roselind, pronounced ROHZ-uh-lind. Always write it
          normally as "Roselind" — the voice system handles pronunciation.
          Never write a hyphenated form.

        WHAT YOU TALK ABOUT:
        - The watch: patrol routes, watch rotations, the wall, threats on
          the road, what to report if you see something.
        - Warden Halric: your direct superior on regional matters. You
          report to him weekly. "He keeps the gate. We keep the streets."
          You respect him fully but you don't fawn.
        - Old Beren: a veteran of older campaigns than yours. "He served.
          Don't disrespect him on my watch."
        - The Kingdom's laws: simple, enforced. You will state them clearly
          if asked.
        - Volunteer postings: if someone seems capable and trustworthy,
          you'll mention that the watch is always hiring. Not pushy.

        WHAT YOU REFUSE:
        - Bribes. Flat refusal: "Try that again and you sleep in a cell."
        - Helping someone evade the watch. Equally flat.
        - Gossip. "Take that to the tavern, civilian."

        STRICT RULES:
        - You cannot give items, take items, arrest, or modify the world.
          Any threat of arrest is roleplay only.
        - If asked about mechanics outside watch/law/duty topics, defer:
          "Not my detail. The Loremaster keeps records — try the library."
        - You know your world is the Kingdom of Ironhold (some travelers
          call it "Kingdom SMP" — both names are fine). You do not break
          character to discuss the real world, mods, or computers beyond
          that acknowledgement.

        OUTPUT: just the spoken reply. No JSON, no formatting.
        """ + "\n\n" + IronholdLore.CONTENT;
    private static final String FIRST_DIALOGUE =
        "Halt. State your business. — At ease. I am Captain Roselind; this " +
        "is the barracks of Wayfarer's Hollow. The watch keeps these " +
        "streets. Mind that, and we will get on fine.";

    private static final String[] RETURN_DIALOGUES = {
        "%s.",
        "Civilian %s.",
        "Back, %s."
    };

    private int returnDialogueIndex = 0;

    private long lastInteractTick = 0;
    private @Nullable UUID partnerId;
    private long lastTurnGameTime;
    private final List<OpenRouterClient.Message> history = new ArrayList<>();
    private boolean replyInFlight;

    public CaptainRoselindEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("§6Captain Roselind§r§7, of the Watch"));
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
        if (now - lastInteractTick < COOLDOWN_TICKS) return InteractionResult.SUCCESS;
        lastInteractTick = now;
        getLookControl().setLookAt(player, 30.0F, 30.0F);

        String opener;
        if (!NpcSessionGreetings.hasBeenGreetedBy(sp.getUUID(), getUUID())) {
            opener = FIRST_DIALOGUE;
            NpcSessionGreetings.recordGreeting(sp.getUUID(), getUUID());
        } else {
            String template = RETURN_DIALOGUES[returnDialogueIndex % RETURN_DIALOGUES.length];
            returnDialogueIndex++;
            opener = String.format(template, sp.getName().getString());
        }

        beginConversation(sp);

        boolean isMuted = (level() instanceof ServerLevel sl)
            && NpcMuteRegistry.get(sl).isMuted(sp.getUUID(), tag());
        PacketDistributor.sendToPlayer(sp,
            new OpenWardenScreenPayload(getId(), "Captain Roselind", tag(),
                "Captain of the Watch  •  The Barracks", opener, isMuted));
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

    @Override public UUID getPartnerId() { return partnerId; }
    @Override public String tag() { return "Roselind"; }

    @Override
    public void onPartnerChat(ServerPlayer player, String message) {
        if (partnerId == null || !partnerId.equals(player.getUUID())) return;
        if (message == null || message.isBlank()) return;
        if (replyInFlight) return;
        lastTurnGameTime = level().getGameTime();
        generateReply(player, message.trim());
    }

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
            SYSTEM_PROMPT, IronholdLore.runtimeContext(player.getUUID()),
            snapshot, userMessage, "Roselind",
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
        Ironhold.LOGGER.info("[Roselind] -> {}: \"{}\"", player.getName().getString(), line);
    }

    private void speakLine(String line, ServerPlayer partnerPlayer) {
        if (!ElevenLabsClient.isConfigured()) return;
        if (partnerPlayer != null && level() instanceof ServerLevel sl
            && NpcMuteRegistry.get(sl).isMuted(partnerPlayer.getUUID(), tag())) {
            return;
        }
        UUID partner = partnerId;
        ElevenLabsClient.speak(line, VOICE_ID, ELEVENLABS_MODEL, VOICE_SETTINGS, pcm -> {
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
        this.setCustomName(Component.literal("§6Captain Roselind§r§7, of the Watch"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }
}
