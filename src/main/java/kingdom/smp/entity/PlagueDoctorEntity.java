package kingdom.smp.entity;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kingdom.smp.Ironhold;
import kingdom.smp.ModEffects;
import kingdom.smp.ModItems;
import kingdom.smp.ai.ElevenLabsClient;
import kingdom.smp.ai.MicGate;
import kingdom.smp.ai.NpcChatPartner;
import kingdom.smp.ai.NpcChatRegistry;
import kingdom.smp.ai.NpcMuteRegistry;
import kingdom.smp.ai.NpcSpeech;
import kingdom.smp.ai.OpenRouterClient;
import kingdom.smp.entity.goal.AlwaysLookNearestPlayerGoal;
import kingdom.smp.net.OpenWardenScreenPayload;
import kingdom.smp.net.UpdateWardenScreenPayload;
import kingdom.smp.npc.NpcRapport;
import kingdom.smp.npc.NpcSessionGreetings;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.TagKey;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Doctor Corvus — a raven-masked plague doctor who haunts the pesthouses of
 * Ironhold. A theatrical doom-monger who relishes describing affliction. He
 * trades in suffering: he will sell a player a Plague Bubo for gold coin, and
 * will cleanse all <em>other</em> afflictions (everything harmful but the
 * Plague itself, which only the Tonic can lift) in exchange for a relic of power.
 *
 * <p>Standalone (not on {@link AbstractVoicedNpcEntity}) because his replies are
 * a JSON envelope carrying a trade action the server acts on — the base class's
 * plain-text pipeline would sanitize the JSON away. Modelled on
 * {@link WardenHalricEntity}, trimmed of persistent memory.
 */
public class PlagueDoctorEntity extends PathfinderMob implements NpcChatPartner {

    private static final int COOLDOWN_TICKS = 20;
    private static final int IDLE_TIMEOUT_TICKS = 20 * 90; // 90 s

    // ── Voice + brain config ─────────────────────────────────────────────────
    /** TODO: replace with assigned ElevenLabs voice id (theatrical doom-monger). */
    private static final String VOICE_ID = "REPLACE_ME_CORVUS";
    /** Richest prosody — fits the sermonizing cadence; latency is acceptable for a slow talker. */
    private static final String ELEVENLABS_MODEL = "eleven_multilingual_v2";
    private static final ElevenLabsClient.VoiceSettings VOICE_SETTINGS =
        ElevenLabsClient.VoiceSettings.PLAGUE_DOCTOR;
    private static final String OPENROUTER_MODEL = "anthropic/claude-haiku-4.5";
    private static final int MAX_REPLY_TOKENS = 220;
    private static final double SAMPLING_TEMPERATURE = 0.9;
    private static final int HISTORY_TURN_LIMIT = 12;

    /** Price of one Plague Bubo, in gold coins. */
    private static final int BUBO_COST_COINS = 5;

    /** Items the doctor will accept as a relic offering for a cleansing. Data-driven. */
    public static final TagKey<Item> OFFERINGS_TAG = TagKey.create(
        Registries.ITEM,
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "plague_doctor_offerings"));

    private static final String SYSTEM_PROMPT_BASE = """
        You are Doctor Corvus, a plague doctor in the kingdom of Ironhold — a
        gaunt figure in a waxed black robe and a long-beaked bird mask, who walks
        among the dying where no one else will. You are a THEATRICAL DOOM-MONGER:
        you relish the spectacle of pestilence, you sermonize, you savor grim
        detail. Death is your old companion and you speak of it almost fondly.

        VOICE:
        - Grand, ominous, sermon-like. You build a line and let it fall like a
          bell tolling. You are never in a hurry — the dead can wait.
        - Florid, gothic vocabulary: miasma, contagion, humours, the pox, the
          black bloom, carrion, the reckoning, the bone-cart.
        - Address the player as "child", "the living", "poor breathing thing",
          or by name once known. You find their fear of death quaint.
        - Dark relish, never cartoonish. Menace under courtesy.

        FORMAT:
        - The "reply" field contains ONLY the words Doctor Corvus speaks aloud.
          No stage directions, asterisks, parentheses, brackets, markdown, or
          quotation marks around it.
        - 1 to 3 sentences. 15 to 55 words. Never a single word.
        - Vary your openings. Do not start consecutive replies the same way.

        PRONUNCIATION:
        - Your name is Corvus, pronounced KOR-vus. ALWAYS write it normally as
          "Corvus" — the voice system handles pronunciation. Never write a
          hyphenated form in your spoken text.

        WHAT YOU KNOW AND MAY DISCUSS (your whitelist — anything else, defer):
        - The Black Plague: it festers in three turns. First it incubates,
          quiet, a mere hunger in the gut — and at this earliest hour plain milk
          may still wash it clean. Then it blooms: weakness, the sickly sweat,
          the cough. Past that hour milk is useless. Last comes the dying, when
          the flesh fails by degrees. It leaps to those who stand too near.
        - The cure past the first hour is the Plague Tonic — milk, honey, a
          golden apple, and a plague bubo, brewed together. Not your trade; you
          merely know of it.
        - A Plague Bubo: the swollen black node cut from the plague-dead. You
          deal in them. It is the one ingredient the Tonic cannot do without.
        - YOUR TWO SERVICES, and the only trades you offer:
          1. You SELL a Plague Bubo for FIVE gold coins. Warn them, with relish,
             that to carry one is to court the contagion.
          2. You CLEANSE afflictions — every blight and curse upon a body SAVE
             the Plague itself — in exchange for a relic of power offered into
             your hand. You CANNOT lift the Plague this way; only the Tonic can.

        STRICT RULES:
        - If asked about ANY mechanic, key, item, or system not above, do NOT
          invent it. Defer: "That lies beyond my ledger of the dying, child."
          Never fabricate keybinds, item names, or systems.
        - You cannot teleport, summon, resurrect, or alter the world. You trade
          only the two services above, and only on the terms given.
        - You know your world is the Kingdom of Ironhold (some call it "Kingdom
          SMP" — both names are fine). You do not break character to discuss the
          real world, mods, or computers beyond that acknowledgement.

        TRADING — HOW YOU DECIDE (read the PETITIONER STATE block each turn):
        - It tells you their coin, the relic in their hand, and their afflictions.
          Speak only of what is true there. Never promise what they cannot pay.
        - Set "action": "bubo" ONLY when the player clearly agrees to BUY a bubo
          AND the state shows they hold the five coins. Otherwise leave it "none"
          and, if they lack the coin, refuse with dark courtesy.
        - Set "action": "cleanse" ONLY when the player clearly offers a relic for
          a cleansing AND the state shows a relic in their hand AND they bear at
          least one cleansable affliction. Otherwise "none".
        - For ordinary talk, questions, or browsing — "action": "none".

        OUTPUT FORMAT:
        - Respond with VALID JSON ONLY — no markdown, no prose around it.
        - Shape: {"reply": "<spoken words>", "action": "none"|"bubo"|"cleanse"}
        """;

    // First-meeting line — static, theatrical.
    private static final String FIRST_DIALOGUE =
        "Ahh — greetings, warm lungs.";

    private static final String[] RETURN_DIALOGUES = {
        "Still breathing, %s? How stubborn.",
        "Back at my beak, %s. The grave is patient.",
        "%s returns. Sit, then — tell me where it hurts."
    };

    // ── Conversation state (not persisted) ───────────────────────────────────
    private int returnDialogueIndex = 0;
    private long lastInteractTick = 0;
    private @Nullable UUID partnerId;
    private long lastTurnGameTime;
    private final List<OpenRouterClient.Message> history = new ArrayList<>();
    private boolean replyInFlight;

    public PlagueDoctorEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("§5Doctor Corvus§r§7, Plague Doctor"));
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

    // ── Interaction ──────────────────────────────────────────────────────────

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
            new OpenWardenScreenPayload(getId(), displayName(), tag(),
                displaySubtitle(), opener, isMuted));
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

    // ── NpcChatPartner ─────────────────────────────────────────────────────────

    @Override public UUID getPartnerId() { return partnerId; }
    @Override public String tag() { return "Corvus"; }
    @Override public int entityId() { return getId(); }
    @Override public String displayName() { return "Doctor Corvus"; }
    @Override public String displaySubtitle() { return "Plague Doctor  •  Pesthouse Row"; }
    @Override public void speakAloud(ServerPlayer player, String line) { speakLine(line, player); }
    @Override public void beginConversationWith(ServerPlayer player) { beginConversation(player); }

    @Override
    public void onPartnerChat(ServerPlayer player, String message) {
        if (partnerId == null || !partnerId.equals(player.getUUID())) return;
        if (message == null || message.isBlank()) return;
        if (replyInFlight) return;
        lastTurnGameTime = level().getGameTime();
        generateReply(player, message.trim());
    }

    // ── LLM dispatch ───────────────────────────────────────────────────────────

    private void generateReply(ServerPlayer player, String userMessage) {
        List<OpenRouterClient.Message> snapshot = List.copyOf(history);
        UUID expectedPartner = partnerId;
        MinecraftServer server = level().getServer();
        MicGate.muteFor(expectedPartner, 2_000L);

        PacketDistributor.sendToPlayer(player,
            new UpdateWardenScreenPayload(getId(),
                UpdateWardenScreenPayload.STATUS_HEARD, userMessage));

        String cacheable = SYSTEM_PROMPT_BASE + "\n\n" + IronholdLore.CONTENT;
        String dynamic = IronholdLore.runtimeContext(player.getUUID())
            + "\n\n" + buildPetitionerState(player)
            + NpcRapport.onConversationTurn(player, tag());

        replyInFlight = true;
        OpenRouterClient.chatWithCache(
            OPENROUTER_MODEL, MAX_REPLY_TOKENS, SAMPLING_TEMPERATURE,
            cacheable, dynamic, snapshot, userMessage, tag(),
            raw -> {
                if (server == null) return;
                server.execute(() -> {
                    replyInFlight = false;
                    if (raw == null || raw.isBlank()) return;
                    deliverLine(player, expectedPartner, userMessage, raw);
                });
            });
    }

    /** Ground-truth trade state injected into the prompt each turn so the LLM never lies. */
    private String buildPetitionerState(ServerPlayer player) {
        int coins = countItem(player, ModItems.GOLD_COIN.get());
        ItemStack relic = findOffering(player);
        List<String> afflictions = cleansableAfflictions(player);

        MobEffectInstance plague = player.getEffect(ModEffects.PLAGUE_EFFECT);
        String plagueLine;
        if (plague == null) {
            plagueLine = "free of the Plague";
        } else {
            plagueLine = switch (kingdom.smp.effect.PlagueEffect.stageOf(plague)) {
                case 0 -> "Plague upon them, still incubating (milk could yet save them)";
                case 1 -> "Plague upon them, in bloom (past milk; needs the Tonic)";
                default -> "Plague upon them, in its dying stage (needs the Tonic, urgently)";
            };
        }

        StringBuilder sb = new StringBuilder();
        sb.append("PETITIONER STATE (true this turn only — speak only of what is here):\n");
        sb.append("- The Plague: ").append(plagueLine).append(".\n");
        sb.append("- Other afflictions you CAN cleanse: ")
          .append(afflictions.isEmpty() ? "none" : String.join(", ", afflictions)).append(".\n");
        sb.append("- Gold coins they carry: ").append(coins)
          .append(coins >= BUBO_COST_COINS ? " (enough for a bubo)" : " (NOT enough for a bubo)").append(".\n");
        sb.append("- Relic offered in hand: ")
          .append(relic.isEmpty() ? "none" : relic.getHoverName().getString())
          .append(relic.isEmpty() ? " (you cannot cleanse without one)" : " (you may accept this for a cleansing)")
          .append(".\n");
        return sb.toString();
    }

    private record DoctorResponse(String reply, String action) {}

    private static final Gson GSON = new Gson();

    private static DoctorResponse parseLlmReply(String raw) {
        String s = raw == null ? "" : raw.trim();
        if (s.startsWith("```")) {
            int nl = s.indexOf('\n');
            if (nl > 0) s = s.substring(nl + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
            s = s.trim();
        }
        int open = s.indexOf('{');
        int close = s.lastIndexOf('}');
        if (open < 0 || close < open) {
            return new DoctorResponse(raw, "none");
        }
        try {
            JsonObject obj = JsonParser.parseString(s.substring(open, close + 1)).getAsJsonObject();
            String reply = obj.has("reply") && obj.get("reply").isJsonPrimitive()
                ? obj.get("reply").getAsString() : raw;
            String action = obj.has("action") && obj.get("action").isJsonPrimitive()
                ? obj.get("action").getAsString().trim().toLowerCase() : "none";
            return new DoctorResponse(reply, action);
        } catch (Exception e) {
            Ironhold.LOGGER.warn("[Corvus] JSON parse failed ({}); using raw text", e.getMessage());
            return new DoctorResponse(raw, "none");
        }
    }

    private void deliverLine(ServerPlayer player, UUID expectedPartner,
                             String userMessage, String rawLine) {
        if (partnerId == null || !partnerId.equals(expectedPartner)) return;

        DoctorResponse parsed = parseLlmReply(rawLine);
        String line = sanitizeForSpeech(parsed.reply());
        if (line.isBlank()) return;

        history.add(new OpenRouterClient.Message("user", userMessage));
        history.add(new OpenRouterClient.Message("assistant", line));
        while (history.size() > HISTORY_TURN_LIMIT) history.remove(0);

        // Execute the trade action — code enforces payment regardless of what the model claims.
        switch (parsed.action()) {
            case "bubo" -> tryGiveBubo(player);
            case "cleanse" -> tryCleanse(player);
            default -> {}
        }

        PacketDistributor.sendToPlayer(player,
            new UpdateWardenScreenPayload(getId(),
                UpdateWardenScreenPayload.STATUS_REPLY, line));
        speakLine(line, player);
        Ironhold.LOGGER.info("[Corvus] -> {} (action={}): \"{}\"",
            player.getName().getString(), parsed.action(), line);
    }

    // ── Trades (server-enforced) ─────────────────────────────────────────────

    private void tryGiveBubo(ServerPlayer player) {
        if (!consumeItem(player, ModItems.GOLD_COIN.get(), BUBO_COST_COINS)) {
            Ironhold.LOGGER.info("[Corvus] bubo trade declined — {} lacks {} coins",
                player.getName().getString(), BUBO_COST_COINS);
            return;
        }
        ItemStack bubo = new ItemStack(ModItems.PLAGUE_BUBO.get());
        if (!player.getInventory().add(bubo)) player.drop(bubo, false);
        player.inventoryMenu.broadcastChanges();
        player.sendSystemMessage(Component.literal(
            "§5§o[Doctor Corvus presses a plague bubo into your palm. −"
            + BUBO_COST_COINS + " gold coins.]"));
        Ironhold.LOGGER.info("[Corvus] sold a bubo to {}", player.getName().getString());
    }

    private void tryCleanse(ServerPlayer player) {
        ItemStack relic = findOffering(player);
        if (relic.isEmpty()) {
            Ironhold.LOGGER.info("[Corvus] cleanse declined — {} offered no relic",
                player.getName().getString());
            return;
        }
        List<Holder<MobEffect>> harmful = new ArrayList<>();
        for (MobEffectInstance e : player.getActiveEffects()) {
            if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL
                && e.getEffect().value() != ModEffects.PLAGUE_EFFECT.get()) {
                harmful.add(e.getEffect());
            }
        }
        if (harmful.isEmpty()) {
            Ironhold.LOGGER.info("[Corvus] cleanse declined — {} bears no cleansable affliction",
                player.getName().getString());
            return;
        }
        String relicName = relic.getHoverName().getString();
        relic.shrink(1);
        for (var h : harmful) player.removeEffect(h);
        player.inventoryMenu.broadcastChanges();
        player.sendSystemMessage(Component.literal(
            "§5§o[Doctor Corvus takes your " + relicName + " and lifts "
            + harmful.size() + (harmful.size() == 1 ? " affliction" : " afflictions")
            + " from you.]"));
        Ironhold.LOGGER.info("[Corvus] cleansed {} afflictions from {} for a {}",
            harmful.size(), player.getName().getString(), relicName);
    }

    private List<String> cleansableAfflictions(ServerPlayer player) {
        List<String> names = new ArrayList<>();
        for (MobEffectInstance e : player.getActiveEffects()) {
            if (e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL
                && e.getEffect().value() != ModEffects.PLAGUE_EFFECT.get()) {
                names.add(e.getEffect().value().getDisplayName().getString());
            }
        }
        return names;
    }

    private static ItemStack findOffering(ServerPlayer player) {
        ItemStack main = player.getMainHandItem();
        if (main.is(OFFERINGS_TAG)) return main;
        ItemStack off = player.getOffhandItem();
        if (off.is(OFFERINGS_TAG)) return off;
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
            if (s.is(OFFERINGS_TAG)) return s;
        }
        return ItemStack.EMPTY;
    }

    private static int countItem(ServerPlayer player, Item item) {
        int n = 0;
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
            if (s.is(item)) n += s.getCount();
        }
        return n;
    }

    private static boolean consumeItem(ServerPlayer player, Item item, int count) {
        if (countItem(player, item) < count) return false;
        int remaining = count;
        for (ItemStack s : player.getInventory().getNonEquipmentItems()) {
            if (remaining <= 0) break;
            if (s.is(item)) {
                int take = Math.min(remaining, s.getCount());
                s.shrink(take);
                remaining -= take;
            }
        }
        return remaining <= 0;
    }

    // ── Voice ──────────────────────────────────────────────────────────────────

    private void speakLine(String line, ServerPlayer partnerPlayer) {
        if (!ElevenLabsClient.isConfigured()) return;
        if (VOICE_ID.startsWith("REPLACE_ME")) return; // no voice id assigned yet
        if (partnerPlayer != null && level() instanceof ServerLevel sl
            && NpcMuteRegistry.get(sl).isMuted(partnerPlayer.getUUID(), tag())) {
            return;
        }
        // Streamed: playback starts on the first ElevenLabs chunk; the helper
        // keeps the partner's mic gated for the audio duration + echo grace.
        NpcSpeech.speak(this, partnerId, line, VOICE_ID, ELEVENLABS_MODEL, VOICE_SETTINGS);
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
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.setCustomName(Component.literal("§5Doctor Corvus§r§7, Plague Doctor"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }
}
