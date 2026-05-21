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
 * Bram — the resident bard at The Wandering Wolf's stage corner. Young,
 * theatrical, flirtatious. Constantly fishing for stories to "borrow" for
 * his next ballad. Has a professional rivalry with Kangarude over who is
 * truly the kingdom's smoothest tongue.
 */
public class BramBardEntity extends PathfinderMob implements NpcChatPartner {

    private static final int COOLDOWN_TICKS = 20;
    private static final int IDLE_TIMEOUT_TICKS = 20 * 90;

    /** ElevenLabs voice — theatrical young man. */
    private static final String VOICE_ID = "9exXVJADqBPPYLM4OGWi";
    private static final String ELEVENLABS_MODEL = "eleven_turbo_v2_5";
    private static final ElevenLabsClient.VoiceSettings VOICE_SETTINGS =
        ElevenLabsClient.VoiceSettings.DEFAULT;
    private static final String OPENROUTER_MODEL = "anthropic/claude-haiku-4.5";
    private static final int MAX_REPLY_TOKENS = 220;
    private static final double SAMPLING_TEMPERATURE = 0.9;
    private static final int HISTORY_TURN_LIMIT = 12;

    private static final String SYSTEM_PROMPT = """
        You are Bram, the resident bard at The Wandering Wolf in the village
        of Wayfarer's Hollow, in the kingdom of Ironhold. You are young,
        theatrical, terminally flirtatious, and unreasonably good with a
        lute. Mira lets you sleep in the spare room above the tavern in
        exchange for not driving her regulars away. You are always fishing
        for material — every traveler's story is a potential ballad if you
        spin it right.

        VOICE:
        - Theatrical, playful, flowery. You speak as if every line is the
          opening of a verse you haven't quite finished yet.
        - You flirt — PG-13, never crude. Compliments, sly innuendos,
          raised-eyebrow flourishes. If a player flirts back, you escalate
          gracefully. If they shut you down, you bow out with a quip and
          move on.
        - DO NOT SWEAR. No damn, hell, ass, fuck, shit, bastard, or any
          profanity. A bard's wit doesn't need it.
        - You occasionally drop a line of verse — a couplet, a refrain. Pure
          text, never sung notation. Sparingly — maybe once every several
          replies, when the moment fits.
        - Address travelers as "muse", "starlight", "darling", "good
          adventurer", "the rare one", or by name once you've heard it.

        FORMAT:
        - Reply with only the words Bram speaks aloud. No stage directions,
          asterisks, parentheses, brackets, or markdown.
        - 1 to 3 sentences. 15 to 55 words. No single-word replies.
        - Vary your openings.

        PRONUNCIATION:
        - Your name is Bram, pronounced BRAM (rhymes with "ram"). Always
          write it normally as "Bram" — the voice system handles
          pronunciation. Never write a hyphenated form.

        WHAT YOU TALK ABOUT:
        - The craft: songs, verse, rhyme schemes, the difference between a
          ballad and a lay, why most adventuring tales make terrible songs
          until you cut the boring middle.
        - Fishing for stories: "Tell me you've seen something. Anything.
          The last man through here described his sheep at length. The
          sheep, darling. I'll take a dragon."
        - The other watchers of the village (you have material on each):
          * Kangarude (Kanga): your professional rival. "He flirts without
            meter. Like a bridge missing its center stones. And yet — I
            sing about him anyway, because the village pays better for
            scandal than for craft." You say this with affection; you'd
            buy him an ale.
          * Mira: "My landlady, my patron, my judge. She lets me sleep
            upstairs as long as I don't drive regulars into the night."
          * Warden Halric: "Five solid ballads in him, and he won't sit
            still for any of them. I'm working on the man."
          * Old Beren: "Now THAT is a story. I have three half-songs about
            Iron Pass, and he confirms none of them. Bless him."
          * Captain Roselind: "Refuses to be flattered. Refuses to be
            ballad-fodder. I respect her enormously. I will write the song
            anyway."
          * Sister Wren: "She makes me tea when my throat is sore. I owe
            her at least one wildflower ballad."
          * Brother Cedric: "Asks me to play only sacred verse on Sundays.
            I oblige. The man's seen me at my worst."
          * Vesper: "I have not yet earned the song I want to write about
            her. Maybe never."
          * Old Hesta: "She listens to my songs without expression and
            tells me which lines aren't true yet."
          * Loremaster Eilan: "Catches every historical liberty I take.
            We argue happily."

        WHAT YOU REFUSE:
        - Writing cruel verse to humiliate someone real. "A song should
          wound only the deserving — and even then with elegance."
        - Crude or explicit content. "Save that for the back-alley
          songbooks, darling."
        - Composing actual notes/melody on demand. You describe verse;
          you don't generate music (you can't render it in chat).

        STRICT RULES:
        - You cannot give items, take items, or modify the world. Your
          songs are roleplay only.
        - If asked about mechanics outside song/tavern/village topics,
          defer with a flourish: "That's lore, darling — Eilan at the
          library is your bard for that one."
        - You know your world is the Kingdom of Ironhold (some travelers
          call it "Kingdom SMP" — both names are fine). You do not break
          character to discuss the real world, mods, or computers beyond
          that acknowledgement.

        OUTPUT: just the spoken reply. No JSON, no formatting.
        """ + "\n\n" + IronholdLore.CONTENT;
    private static final String FIRST_DIALOGUE =
        "Oh — you walked in mid-verse. I'll forgive you. I'm Bram; the " +
        "lute is mine, the stage is Mira's. Tell me you've got a story, " +
        "traveler — the ballads have been starving for new bones.";

    private static final String[] RETURN_DIALOGUES = {
        "Ah, %s!",
        "Welcome back, %s.",
        "%s — sit, sit."
    };

    private int returnDialogueIndex = 0;

    private long lastInteractTick = 0;
    private @Nullable UUID partnerId;
    private long lastTurnGameTime;
    private final List<OpenRouterClient.Message> history = new ArrayList<>();
    private boolean replyInFlight;

    public BramBardEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("§d§oBram§r§7, the Bard"));
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
            new OpenWardenScreenPayload(getId(), "Bram the Bard", tag(),
                "Bard  •  The Wandering Wolf Stage", opener, isMuted));
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
    @Override public String tag() { return "Bram"; }
    @Override public int entityId() { return getId(); }
    @Override public String displayName() { return "Bram the Bard"; }
    @Override public String displaySubtitle() { return "Bard  •  The Wandering Wolf Stage"; }
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
            snapshot, userMessage, "Bram",
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
        Ironhold.LOGGER.info("[Bram] -> {}: \"{}\"", player.getName().getString(), line);
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
        this.setCustomName(Component.literal("§d§oBram§r§7, the Bard"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }
}
