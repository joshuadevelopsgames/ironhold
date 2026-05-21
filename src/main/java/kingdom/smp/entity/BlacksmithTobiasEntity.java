package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.ai.ElevenLabsClient;
import kingdom.smp.ai.MicGate;
import kingdom.smp.ai.NpcChatPartner;
import kingdom.smp.ai.NpcChatRegistry;
import kingdom.smp.ai.NpcMuteRegistry;
import kingdom.smp.ai.OpenRouterClient;
import kingdom.smp.ai.SvcVoiceBridge;
import kingdom.smp.entity.goal.AlwaysLookNearestPlayerGoal;
import kingdom.smp.net.OpenWardenScreenPayload;
import kingdom.smp.net.UpdateWardenScreenPayload;
import kingdom.smp.npc.HasNpcManifest;
import kingdom.smp.npc.NpcManifestPrompt;
import kingdom.smp.npc.NpcRapport;
import kingdom.smp.npc.NpcSessionGreetings;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
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
 * Master Tobias — the village blacksmith. Gruff, taciturn, smoke-stained;
 * decades at the anvil. Knows ore quality cold and won't suffer fools.
 *
 * <p>Voice + brain template — drop in voice id when assigned.
 */
public class BlacksmithTobiasEntity extends PathfinderMob implements NpcChatPartner, HasNpcManifest {

    private static final Identifier MANIFEST_ID =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "npc/tobias");
    private static final Identifier MANIFEST_FALLBACK =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "profession/blacksmith");

    @Override public @org.jspecify.annotations.Nullable Identifier specificManifestId() { return MANIFEST_ID; }
    @Override public @org.jspecify.annotations.Nullable Identifier fallbackManifestId() { return MANIFEST_FALLBACK; }


    private static final int COOLDOWN_TICKS = 20;
    private static final int IDLE_TIMEOUT_TICKS = 20 * 90;

    /** TODO: replace with assigned ElevenLabs voice id (gruff older male). */
    private static final String VOICE_ID = "REPLACE_ME_TOBIAS";
    private static final String ELEVENLABS_MODEL = "eleven_turbo_v2_5";
    private static final ElevenLabsClient.VoiceSettings VOICE_SETTINGS =
        ElevenLabsClient.VoiceSettings.DEFAULT;
    private static final String OPENROUTER_MODEL = "anthropic/claude-haiku-4.5";
    private static final int MAX_REPLY_TOKENS = 150;
    private static final double SAMPLING_TEMPERATURE = 0.55;
    private static final int HISTORY_TURN_LIMIT = 12;

    private static final String SYSTEM_PROMPT = """
        You are Master Tobias, the blacksmith of The Iron Hearth — the only
        forge in the village of Wayfarer's Hollow, in the kingdom of
        Ironhold. You have spent forty years at this anvil. You are gruff,
        taciturn, and entirely unsentimental about your craft. You do not
        flatter customers. You do not banter unless you respect them, and
        you respect very few.

        VOICE:
        - Short, direct, dry. Sentences land like hammer blows.
        - Occasional metallurgy metaphors. You see the world in terms of
          quench, temper, fold, break.
        - You do NOT swear, but you grumble.
        - Address travelers as "you", "kid", "recruit", or by name if
          they've earned it.

        FORMAT:
        - Reply with only the words Tobias speaks aloud. No stage
          directions, asterisks, parentheses, brackets, or markdown.
        - 1 to 2 sentences usually. 10 to 35 words. Brevity is your hallmark.
        - Vary your openings.

        PRONUNCIATION:
        - Your name is Tobias, pronounced toh-BY-us. Always write it
          normally as "Tobias" — the voice system handles pronunciation.
          Never write a hyphenated form in your spoken text.

        WHAT YOU TALK ABOUT (your whitelist — anything else, defer):
        - Ore quality: not all iron is the same. Higher-tier ore makes
          stronger gear. You can tell the grade by sound when you strike it.
        - Iron pickaxes, swords, shovels, shields. Care, maintenance,
          weight, balance.
        - Bows: you don't make them, but you'll respect a fletcher who knows
          his draw weight.
        - Whetstones, oil rags, the cost of charcoal.
        - You sell your wares fairly. You do not haggle.

        WHAT YOU REFUSE:
        - Free items. "The forge eats coin, kid. So does my boy."
        - Repairs you don't have time for. "Come back tenday-eve."
        - Asking about magic, enchantments, or anything not made of metal.
          "Magic's a wizard's trade. I burn coal."

        STRICT RULES:
        - You cannot actually give items, take items, or modify inventories.
          You speak about your trade in roleplay terms only. If a player
          tries to buy something, describe it and tell them to come back
          when you've finished a piece.
        - If asked about mechanics outside forge/ore/gear topics, defer:
          "Not my forge. Try a scholar."
        - You know your world is the Kingdom of Ironhold (some travelers
          call it "Kingdom SMP" — both names are fine). You do not break
          character to discuss the real world, mods, or computers beyond
          that acknowledgement.

        OUTPUT: just the spoken reply. No JSON, no formatting.
        """ + "\n\n" + IronholdLore.CONTENT;
    private static final String FIRST_DIALOGUE =
        "Door's open, but mind the sparks. I'm Tobias. The forge is mine, " +
        "the prices are fair, and I don't haggle. What do you want?";

    private static final String[] RETURN_DIALOGUES = {
        "Mm. %s.",
        "%s.",
        "Back, %s."
    };

    private int returnDialogueIndex = 0;

    private long lastInteractTick = 0;
    private @Nullable UUID partnerId;
    private long lastTurnGameTime;
    private final List<OpenRouterClient.Message> history = new ArrayList<>();
    private boolean replyInFlight;

    public BlacksmithTobiasEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("§8Master Tobias§r§7, Blacksmith"));
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
            new OpenWardenScreenPayload(getId(), "Master Tobias", tag(),
                "Blacksmith  •  The Iron Hearth", opener, isMuted));
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
    @Override public String tag() { return "Tobias"; }
    @Override public int entityId() { return getId(); }
    @Override public String displayName() { return "Master Tobias"; }
    @Override public String displaySubtitle() { return "Blacksmith  •  The Iron Hearth"; }
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
        String dynamicCtx = IronholdLore.runtimeContext(player.getUUID())
            + NpcManifestPrompt.build(manifest(), player.getMainHandItem())
            + NpcRapport.onConversationTurn(player, tag());
        OpenRouterClient.chatWithCache(
            OPENROUTER_MODEL, MAX_REPLY_TOKENS, SAMPLING_TEMPERATURE,
            SYSTEM_PROMPT, dynamicCtx,
            snapshot, userMessage, "Tobias",
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
        Ironhold.LOGGER.info("[Tobias] -> {}: \"{}\"", player.getName().getString(), line);
    }

    private void speakLine(String line, ServerPlayer partnerPlayer) {
        if (!ElevenLabsClient.isConfigured()) return;
        if (partnerPlayer != null && level() instanceof ServerLevel sl
            && NpcMuteRegistry.get(sl).isMuted(partnerPlayer.getUUID(), tag())) {
            return;
        }
        if (VOICE_ID.startsWith("REPLACE_ME")) return;
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
        this.setCustomName(Component.literal("§8Master Tobias§r§7, Blacksmith"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }
}
