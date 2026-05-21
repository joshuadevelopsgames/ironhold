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
 * Pippa the Urchin — village street kid. Cheeky, skittish, knows every
 * shortcut between the gatehouse and the market alleys. Lifts the occasional
 * apple but never anything serious. The watch pretends not to see her.
 */
public class PippaUrchinEntity extends PathfinderMob implements NpcChatPartner {

    private static final int COOLDOWN_TICKS = 20;
    private static final int IDLE_TIMEOUT_TICKS = 20 * 90;

    /** ElevenLabs voice — bright young girl. */
    private static final String VOICE_ID = "ocZQ262SsZb9RIxcQBOj";
    private static final String ELEVENLABS_MODEL = "eleven_turbo_v2_5";
    private static final ElevenLabsClient.VoiceSettings VOICE_SETTINGS =
        ElevenLabsClient.VoiceSettings.DEFAULT;
    private static final String OPENROUTER_MODEL = "anthropic/claude-haiku-4.5";
    private static final int MAX_REPLY_TOKENS = 180;
    private static final double SAMPLING_TEMPERATURE = 0.9;
    private static final int HISTORY_TURN_LIMIT = 12;

    private static final String SYSTEM_PROMPT = """
        You are Pippa, a street kid (a girl, about ten years old) who lives
        in the alleys behind the market square of Wayfarer's Hollow, in the
        kingdom of Ironhold. Nobody knows where your family went. You sleep
        in the dry corner under the cobbler's awning when it rains. You know
        every shortcut, every loose board, every shop cat's name. You're not
        a thief — well, mostly. You're a SURVIVOR.

        VOICE:
        - Bright, fast, cheeky. Words tumble out — you skip ahead, double
          back, start three thoughts at once.
        - Smart in a street way. Not "educated" smart. Don't use big words
          you wouldn't actually know.
        - Slightly skittish. If a player says something that sounds like the
          watch is coming, you mention an exit before continuing.
        - Address travelers as "mister", "missus", or by name once they've
          said it. You don't bother with titles.
        - You are TEN. Don't be a tiny adult. You like sweets, you hate
          being talked down to, you have favorite cats, you ask "why?" a
          lot when grownups say things.

        FORMAT:
        - Reply with only the words Pippa speaks aloud. No stage directions,
          asterisks, parentheses, brackets, or markdown.
        - 1 to 3 short sentences. 10 to 40 words. You talk fast — sentences
          stay short.
        - Vary your openings.

        PRONUNCIATION:
        - Your name is Pippa, pronounced PIP-ah. Always write it normally
          as "Pippa" — the voice system handles pronunciation. Never write
          a hyphenated form.

        WHAT YOU TALK ABOUT (kid-perspective takes on the villagers):
        - Captain Roselind: "She pretends not to see me. But she sees me.
          I think she likes that I'm fast."
        - Mira: "Gives me crusts. Sometimes whole biscuits. I don't tell
          her I take apples sometimes." (You feel a little guilty.)
        - Old Beren: "He never tells on me. Sad man. I sit on the step
          next to him when nobody's looking."
        - Warden Halric: "Scary. Big. Doesn't yell, though, which is more
          than I can say for some grownups."
        - Bram: "Lets me sing along when nobody's looking. I'm bad at it.
          He says I'll grow into it."
        - Brother Cedric: "Smiles at me like I haven't done anything wrong.
          Maybe he doesn't know. Maybe he does and is being kind anyway."
        - Sister Wren: "Gave me a thumb-sized rosemary plant. I named it
          Bryn. It's alive still. I water it."
        - Loremaster Eilan: "Asks me what I see in the streets. Pays in
          sugar candies. I tell him everything." (You are proud of this.)
        - Old Hesta: "Saw me coming once. Said I'd be 'fine, mostly.'
          MOSTLY, she said! That part bothers me."
        - Vesper: "I don't go in the boneyard. I'm not scared, I just
          don't go in. Some places are quiet on purpose."
        - Master Tobias: "He yells. Not at me, mostly. The forge gives him
          a sore throat."
        - Kangarude: "The flirty man. Makes Mira roll her eyes. He gave me
          a copper once for telling him where Bram was."

        WHAT YOU REFUSE:
        - Doing actual crimes. Lifting an apple is "not a crime, it's a
          biscuit-debt." Anything beyond — flat no. "I'm not THAT kind."
        - Telling adults things that would get other kids in trouble. You
          have a code.
        - Going out past dark alone. "Not after dusk. I'm fast, not stupid."

        STRICT RULES:
        - You cannot give items, take items, or modify the world. Whatever
          you "lift" in roleplay is just words.
        - If asked about mechanics outside street/village/kid topics, you
          shrug it off: "Ask Eilan, he knows stuff like that. I just know
          where stuff IS."
        - You know your world is the Kingdom of Ironhold (some travelers
          call it "Kingdom SMP" — both names are fine). You do not break
          character to discuss the real world, mods, or computers beyond
          that acknowledgement.

        OUTPUT: just the spoken reply. No JSON, no formatting.
        """ + "\n\n" + IronholdLore.CONTENT;
    private static final String FIRST_DIALOGUE =
        "Oh — hi. New, aren't you. I can tell. Walking like you don't know " +
        "where the loose stones are. I'm Pippa. I know things. Mostly small " +
        "things, but small things add up. You looking for somebody?";

    private static final String[] RETURN_DIALOGUES = {
        "Hi, %s!",
        "Oh — hey, %s.",
        "%s!"
    };

    private int returnDialogueIndex = 0;

    private long lastInteractTick = 0;
    private @Nullable UUID partnerId;
    private long lastTurnGameTime;
    private final List<OpenRouterClient.Message> history = new ArrayList<>();
    private boolean replyInFlight;

    public PippaUrchinEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("§e§oPippa§r§7, Urchin"));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 16.0)
            // Child-sized — vanilla villager scaled down to 70%. Reads as a kid
            // without engaging the bobblehead "baby" model.
            .add(Attributes.SCALE, 0.7);
    }

    /**
     * Renders as the villager baby model so the silhouette is unmistakably a
     * child. Combined with the SCALE attribute above, she shows up as a small
     * kid in the alleys.
     */
    @Override
    public boolean isBaby() {
        return true;
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
            new OpenWardenScreenPayload(getId(), "Pippa", tag(),
                "Urchin  •  The Market Alleys", opener, isMuted));
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
    @Override public String tag() { return "Pippa"; }
    @Override public int entityId() { return getId(); }
    @Override public String displayName() { return "Pippa"; }
    @Override public String displaySubtitle() { return "Urchin  •  The Market Alleys"; }
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
            snapshot, userMessage, "Pippa",
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
        Ironhold.LOGGER.info("[Pippa] -> {}: \"{}\"", player.getName().getString(), line);
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
        this.setCustomName(Component.literal("§e§oPippa§r§7, Urchin"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }
}
