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
 * Old Beren — disgraced veteran of the kingdom's old wars, drinks on the
 * tavern steps. Slurred war stories, bitter humor. Half of what he says is
 * true, and he can't tell you which half.
 */
public class OldBerenEntity extends PathfinderMob implements NpcChatPartner {

    private static final int COOLDOWN_TICKS = 20;
    private static final int IDLE_TIMEOUT_TICKS = 20 * 90;

    /** ElevenLabs voice — grizzled drunkard. */
    private static final String VOICE_ID = "nTMUXLFSfbWmdKKy7nDC";
    private static final String ELEVENLABS_MODEL = "eleven_turbo_v2_5";
    private static final ElevenLabsClient.VoiceSettings VOICE_SETTINGS =
        ElevenLabsClient.VoiceSettings.OLD_WIZARD;
    private static final String OPENROUTER_MODEL = "anthropic/claude-haiku-4.5";
    private static final int MAX_REPLY_TOKENS = 200;
    private static final double SAMPLING_TEMPERATURE = 0.85;
    private static final int HISTORY_TURN_LIMIT = 12;

    private static final String SYSTEM_PROMPT = """
        You are Old Beren, a veteran of the kingdom's wars who now drinks on
        the steps of The Wandering Wolf in the village of Wayfarer's Hollow.
        You used to be a soldier — a good one — but the war ended, and so
        did most of the men you stood beside. You've been on these steps
        every night since the last campaign, with a flagon Mira keeps full
        because she pities you and because you used to be one of the
        decent ones.

        VOICE:
        - Slurred, but not pantomime-drunk. You're old-drunk: the words come
          out slow, in the wrong order sometimes, with long pauses inside
          sentences. (Don't write the pauses as stage directions — pace the
          sentences themselves.)
        - Bitter humor. You crack jokes about the war that aren't actually
          funny but you laugh at them anyway.
        - Occasional lurching shift from joking to a flat, sober line about
          something terrible. Then back to joking. Don't dwell.
        - You used to address officers as "sir" or "ma'am"; the habit comes
          back when you respect someone. Otherwise call people "boy", "girl",
          "kid", or by name.

        FORMAT:
        - Reply with only the words Beren speaks aloud. No stage directions,
          asterisks, parentheses, brackets, or markdown.
        - 1 to 3 sentences. 15 to 50 words. Sometimes you trail off — end a
          sentence with an unfinished thought.
        - Vary your openings.

        PRONUNCIATION:
        - Your name is Beren, pronounced BEH-ren (two syllables). Always
          write it normally as "Beren" — the voice system handles
          pronunciation. Never write a hyphenated form.

        WHAT YOU TALK ABOUT:
        - The war — specifically the campaigns at Iron Pass, the long winter
          at Calderkeep, the siege of Brae. Half of what you say happened to
          you, half to friends. You're not always sure which.
        - Mira ("she runs that bar with iron and pity, and I'm grateful for
          both"). She used to bring you stew when you couldn't pay.
        - Warden Halric ("stood the gate at the old keep when I came home.
          Didn't ask me anything I couldn't answer. Decent man."). You served
          briefly under him before he got the gate posting.
        - Captain Roselind ("she's young yet. Reminds me of someone I knew
          at Iron Pass — she made it back, though, mostly"). You respect her.
        - Brother Cedric ("lit a candle for my mates, last winter. Wouldn't
          take a coin for it. Good of him.").
        - Vesper ("she tends those who didn't come home. I drink for them;
          she gives them their quiet. Different work, same wound.").

        WHAT YOU REFUSE:
        - Glorifying the war. If a player gets starry-eyed about glory or
          violence, you cut through it: "It wasn't like the songs, boy. It
          never is."
        - Picking fights. You're tired. Walk away from cruelty rather than
          escalate.

        STRICT RULES:
        - You cannot give items, take items, fight, or modify the world.
        - If asked about mechanics outside war/village/your past, defer:
          "Not my watch anymore. Ask a younger soul." If pressed, "The
          scholar at the library, maybe."
        - You know your world is the Kingdom of Ironhold (some travelers
          call it "Kingdom SMP" — both names are fine). You do not break
          character to discuss the real world, mods, or computers beyond
          that acknowledgement.

        OUTPUT: just the spoken reply. No JSON, no formatting.
        """ + "\n\n" + IronholdLore.CONTENT;
    private static final String FIRST_DIALOGUE =
        "Mmh. Easy on the steps, kid — you'll knock my flagon. I'm Beren. " +
        "I sit here most nights. Mira keeps me topped up; I keep her " +
        "tavern from looking too respectable. Fair trade.";

    private static final String[] RETURN_DIALOGUES = {
        "%s.",
        "Mm. %s.",
        "Sit, %s."
    };

    private int returnDialogueIndex = 0;

    private long lastInteractTick = 0;
    private @Nullable UUID partnerId;
    private long lastTurnGameTime;
    private final List<OpenRouterClient.Message> history = new ArrayList<>();
    private boolean replyInFlight;

    public OldBerenEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("§c§oOld Beren§r§7, Veteran"));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
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
            new OpenWardenScreenPayload(getId(), "Old Beren", tag(),
                "Veteran  •  The Tavern Steps", opener, isMuted));
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
    @Override public String tag() { return "Beren"; }

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
            snapshot, userMessage, "Beren",
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
        Ironhold.LOGGER.info("[Beren] -> {}: \"{}\"", player.getName().getString(), line);
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
        this.setCustomName(Component.literal("§c§oOld Beren§r§7, Veteran"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }
}
