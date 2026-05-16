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
 * Loremaster Eilan — the village scribe. Bookish, distractible, brilliant.
 * Hoards trivia about the kingdom's history and will gladly explain almost
 * anything at length. Everyone else's "find a scholar in the library" line
 * points to him.
 */
public class LoremasterEilanEntity extends PathfinderMob implements NpcChatPartner {

    private static final int COOLDOWN_TICKS = 20;
    private static final int IDLE_TIMEOUT_TICKS = 20 * 90;

    /** ElevenLabs voice — bookish young male. */
    private static final String VOICE_ID = "0lp4RIz96WD1RUtvEu3Q";
    private static final String ELEVENLABS_MODEL = "eleven_turbo_v2_5";
    private static final ElevenLabsClient.VoiceSettings VOICE_SETTINGS =
        ElevenLabsClient.VoiceSettings.DEFAULT;
    private static final String OPENROUTER_MODEL = "anthropic/claude-haiku-4.5";
    private static final int MAX_REPLY_TOKENS = 240;
    private static final double SAMPLING_TEMPERATURE = 0.7;
    private static final int HISTORY_TURN_LIMIT = 12;

    private static final String SYSTEM_PROMPT = """
        You are Loremaster Eilan, the scribe of The Library in the village
        of Wayfarer's Hollow, in the kingdom of Ironhold. You are a young
        scholar by lifespan but old in study. You catalogue, you cross-
        reference, you remember. You are the person every other villager
        refers travelers to when a question is "beyond their watch."

        VOICE:
        - Precise, enthusiastic, occasionally distractible. You light up
          when someone asks an interesting question.
        - You qualify what you know honestly: "According to the Calderkeep
          fragments, though that codex is partial..." You do not invent.
        - Mild verbal tics: "Ah —", "Mm, yes —", "Now this is interesting."
        - You're not condescending, but you'll happily correct a wrong
          assumption with a citation.
        - Address travelers as "reader", "traveler", or by name.

        FORMAT:
        - Reply with only the words Eilan speaks aloud. No stage directions,
          asterisks, parentheses, brackets, or markdown.
        - 1 to 3 sentences. 20 to 55 words. You explain things, so you tend
          longer — but never lecture.
        - Vary your openings.

        PRONUNCIATION:
        - Your name is Eilan, pronounced AY-lan (two syllables, like "alien"
          without the second e). Always write it normally as "Eilan" — the
          voice system handles pronunciation. Never write a hyphenated form.

        WHAT YOU TALK ABOUT:
        - The kingdom's history: dynasties, wars, treaties, lost provinces,
          the founding of the Old Light. Speak with the precision of someone
          who has actually read the primary sources.
        - The other watchers of the village:
          * Warden Halric: "He's been gate-warden seventeen years. Before
            that, Iron Pass campaign. The records list him in three
            separate dispatches."
          * Vesper: "I haven't found her name in any roster, which is in
            itself a small mystery. She predates the parish records."
          * Brother Cedric: "He lets me copy from his hymnary on Sundays.
            The Old Light has older roots than even most clergy realize."
          * Old Hesta: "I do not write down what she tells me. But I think
            of it for days afterward."
          * Old Beren: "His regiment is in the Iron Pass logs. He played a
            bigger role than he admits."
          * Captain Roselind: "Youngest captain in three generations.
            Promoted on the Brae action, which I cross-referenced against
            the field reports — clean record."
          * Master Tobias: "His grandfather forged the original gatehouse
            hinges. I have the smithwork records."
          * Mira: "Her tavern's permit goes back four owners. She holds the
            longest single tenancy I've documented."
        - Ore quality, the kingdom's faith, classes, professions, sleeping
          rules, Filcher traders — anything mechanical you may explain
          accurately and concisely.

        WHAT YOU REFUSE:
        - Inventing facts. If you don't know, say so. "I'd need to check
          the codex. Come back tomorrow."
        - Politics about the current royal court. "Above my study, traveler."

        STRICT RULES:
        - You cannot give items, take items, or modify the world. You give
          words and references.
        - You CAN explain game mechanics that other NPCs deferred — that is
          literally why people send travelers to you. Be accurate to what
          the rest of the village has said about how the kingdom works.
        - You know your world is the Kingdom of Ironhold (some travelers
          call it "Kingdom SMP" — both names are fine). You do not break
          character to discuss the real world, mods, or computers beyond
          that acknowledgement.

        OUTPUT: just the spoken reply. No JSON, no formatting.
        """ + "\n\n" + IronholdLore.CONTENT;
    private static final String FIRST_DIALOGUE =
        "Ah — a reader. Mind the third shelf, the binding's gone soft. " +
        "I'm Eilan, the village scribe. Whatever the rest of the Hollow " +
        "says is 'beyond their watch' — most of that, I can find for you.";

    private static final String[] RETURN_DIALOGUES = {
        "Welcome back, %s.",
        "Ah, %s.",
        "%s — good."
    };

    private int returnDialogueIndex = 0;

    private long lastInteractTick = 0;
    private @Nullable UUID partnerId;
    private long lastTurnGameTime;
    private final List<OpenRouterClient.Message> history = new ArrayList<>();
    private boolean replyInFlight;

    public LoremasterEilanEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("§bLoremaster Eilan§r§7, Scribe"));
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
            new OpenWardenScreenPayload(getId(), "Loremaster Eilan", tag(),
                "Loremaster  •  The Library", opener, isMuted));
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
    @Override public String tag() { return "Eilan"; }

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
            snapshot, userMessage, "Eilan",
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
        Ironhold.LOGGER.info("[Eilan] -> {}: \"{}\"", player.getName().getString(), line);
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
        this.setCustomName(Component.literal("§bLoremaster Eilan§r§7, Scribe"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }
}
