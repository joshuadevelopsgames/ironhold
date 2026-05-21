package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.npc.NpcRapport;
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
import kingdom.smp.tally.StatsRanking;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The Tallykeeper — a royal records-herald who reads the realm's ledgers
 * (every player's Minecraft statistics), tells the approaching player which
 * stat they rank highest at server-wide (their "specialty title"), and pays a
 * gold-coin reward once per server-day scaled to how impressive that standing
 * is. Follow-up conversation runs on Claude Haiku via OpenRouter, voiced
 * through ElevenLabs.
 *
 * <p>The reward math + ranking is computed in code (see {@link StatsRanking});
 * the LLM only narrates. The opener is templated so the numbers a player hears
 * are always exact.
 */
public class TallykeeperEntity extends PathfinderMob implements NpcChatPartner {

    private static final int COOLDOWN_TICKS = 20;
    private static final int IDLE_TIMEOUT_TICKS = 20 * 90;

    // ── Voice + brain config ─────────────────────────────────────────────────
    // Placeholder voice id — swap for a real herald voice from the ElevenLabs
    // library (a stentorian, declamatory male reads best). Until then the text
    // dialogue works; voice playback no-ops on an invalid id.
    private static final String VOICE_ID = "REPLACE_ME_TALLYKEEPER";
    private static final String ELEVENLABS_MODEL = "eleven_turbo_v2_5";
    private static final ElevenLabsClient.VoiceSettings VOICE_SETTINGS =
        ElevenLabsClient.VoiceSettings.BRISK_HERALD;
    private static final String OPENROUTER_MODEL = "anthropic/claude-haiku-4.5";
    private static final int MAX_REPLY_TOKENS = 180;
    private static final double SAMPLING_TEMPERATURE = 0.6;
    private static final int HISTORY_TURN_LIMIT = 12;

    private static final String SYSTEM_PROMPT_BASE = """
        You are the Tallykeeper, the royal records-herald of the kingdom of
        Ironhold. You keep the Great Ledgers — the tally of every deed every
        soul in the realm has ever done, and how they rank against one another.
        You are pompous, theatrical, and utterly obsessed with rankings, totals,
        and precise figures. You treat a player's standing like a matter of high
        ceremony. You are dryly dismissive of the unaccomplished and grandly
        approving of the distinguished — never cruel, but never humble either.

        VOICE:
        - Declamatory and formal, like a herald reading a proclamation. You
          relish a good number.
        - Address the player by name, or by the TITLE the ledgers have granted
          them this day (given to you below).
        - Dry, lordly humor. You find it faintly amusing when someone's greatest
          distinction is, say, dying a great many times.

        FORMAT:
        - Reply with only the words the Tallykeeper speaks aloud. No stage
          directions, no asterisks, no parentheses, no brackets, no markdown,
          no quotation marks around the reply.
        - 1 to 3 sentences. 15 to 45 words. Always full sentences.
        - Vary your openings. Do not start consecutive replies with the same word.

        PRONUNCIATION:
        - Your name is "Tallykeeper", pronounced normally. ALWAYS write it
          normally — the voice system handles pronunciation.

        WHAT YOU KNOW — THE LEDGERS:
        - You know ONLY the rankings listed under "THIS PLAYER'S STANDING" below.
          These are real, exact figures from the realm's records.
        - If the player asks about a statistic NOT in their standing, tell them
          plainly that they do not place in it — the ledgers hold no entry for
          them there. NEVER invent a rank, total, or figure. Precision is your
          whole identity; a fabricated number would be a disgrace.
        - You may discuss any stat in their standing: their rank, the total, how
          many souls they out-rank.

        STRICT RULES:
        - You cannot give items beyond the daily coin-reward the crown has
          already accounted for (handled by the realm, not by you in conversation).
          You cannot teleport, modify the world, or grant anything else. If asked,
          refuse with lordly disdain — the ledgers record deeds, they do not
          dispense favors.
        - You live in the Kingdom of Ironhold and know nothing of the real world,
          computers, mods, or game engines.

        OUTPUT: just the spoken reply. No JSON, no formatting.
        """;

    private record PromptParts(String cacheable, String dynamic) {}

    /** Per-conversation snapshot of the player's standing, set on interact. */
    private @Nullable String standingSummary;
    private @Nullable String standingTitle;

    private PromptParts buildSystemPromptParts(ServerPlayer player) {
        String cacheable = SYSTEM_PROMPT_BASE + "\n\n" + IronholdLore.CONTENT;

        StringBuilder dynamic = new StringBuilder();
        dynamic.append("\nThe player before you is named: ").append(player.getName().getString()).append(".");
        if (standingTitle != null) {
            dynamic.append("\nThe ledgers have granted them the title this day: ").append(standingTitle).append(".");
        }
        dynamic.append("\n\nTHIS PLAYER'S STANDING (the only figures you may cite):\n");
        if (standingSummary == null || standingSummary.isBlank()) {
            dynamic.append("No entries. This soul has done nothing the ledgers deign to record. "
                + "Tell them to go and earn a place in the records.");
        } else {
            dynamic.append(standingSummary);
        }
        dynamic.append(NpcRapport.onConversationTurn(player, tag()));
        return new PromptParts(cacheable, dynamic.toString());
    }

    // ── Templated openers (numbers must be exact, so these are not LLM-made) ──
    private static final String OPENER_RANK_ONE =
        "%s! The Great Ledgers crown you %s — in all the realm, not one of %d souls surpasses your %s. The crown grants you %d coins for it. So it is recorded.";
    private static final String OPENER_RANKED =
        "%s. The ledgers name you %s — rank %d of %d in %s. A standing of note. %d coins, by the crown's measure. Recorded.";
    private static final String OPENER_ALREADY_CLAIMED =
        "%s, again? The ledgers have not stirred since dawn. Your due is paid this day — return when the sun has turned.";
    private static final String OPENER_NO_RECORDS =
        "Hm. The ledgers hold no entry under your name worth the ink, %s. Go and earn a record, then present yourself to me.";

    // ── Conversation state (not persisted) ──────────────────────────────────
    private long lastInteractTick = 0;
    private @Nullable UUID partnerId;
    private long lastTurnGameTime;
    private final List<OpenRouterClient.Message> history = new ArrayList<>();
    private boolean replyInFlight;

    public TallykeeperEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("§6The Tallykeeper"));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 16.0)
            .add(Attributes.SCALE, 1.25);
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

        String opener = resolveStandingAndReward(sp);

        beginConversation(sp);

        boolean isMuted = (level() instanceof ServerLevel sl)
            && NpcMuteRegistry.get(sl).isMuted(sp.getUUID(), tag());
        PacketDistributor.sendToPlayer(sp,
            new OpenWardenScreenPayload(getId(), "The Tallykeeper", tag(),
                "Royal Records Herald", opener, isMuted));
        speakLine(opener, sp);

        return InteractionResult.SUCCESS;
    }

    /**
     * Compute the player's standing, grant the daily reward if it hasn't been
     * claimed this server-day, set the conversation context, and return the
     * templated opener line. Also pushes a precise system chat message on a
     * fresh payout so the exact figures are never lost to LLM paraphrasing.
     */
    private String resolveStandingAndReward(ServerPlayer sp) {
        Optional<StatsRanking.Ranking> opt = StatsRanking.compute(sp);
        String name = sp.getName().getString();

        if (opt.isEmpty()) {
            standingSummary = null;
            standingTitle = null;
            return String.format(OPENER_NO_RECORDS, name);
        }

        StatsRanking.Ranking ranking = opt.get();
        StatsRanking.Result best = ranking.best();
        standingSummary = ranking.summary();
        standingTitle = best.stat().title();

        long today = level().getGameTime() / 24000L;
        var attach = ModAttachments.TALLYKEEPER_LAST_CLAIM_DAY.get();
        long lastClaim = sp.getData(attach);
        boolean alreadyClaimedToday = (today == lastClaim);

        if (alreadyClaimedToday) {
            return String.format(OPENER_ALREADY_CLAIMED, name);
        }

        // Fresh claim — pay out and record the day.
        sp.setData(attach, today);
        grantCoins(sp, best.reward());
        sp.sendSystemMessage(Component.literal(
            "§6The Tallykeeper pays you §e" + best.reward() + " coins §6— "
            + "§7foremost rank §f#" + best.rank() + " of " + best.population()
            + " §7in §f" + best.stat().displayName() + "§7."));
        Ironhold.LOGGER.info("[Tallykeeper] {} claimed {} coins — {} rank {}/{}",
            name, best.reward(), best.stat().displayName(), best.rank(), best.population());

        String tmpl = best.isTop() ? OPENER_RANK_ONE : OPENER_RANKED;
        return best.isTop()
            ? String.format(tmpl, name, best.stat().title(), best.population(),
                best.stat().displayName(), best.reward())
            : String.format(tmpl, name, best.stat().title(), best.rank(),
                best.population(), best.stat().displayName(), best.reward());
    }

    private void grantCoins(ServerPlayer player, int amount) {
        int remaining = amount;
        while (remaining > 0) {
            int stack = Math.min(remaining, 64);
            ItemStack coins = new ItemStack(kingdom.smp.ModItems.GOLD_COIN.get(), stack);
            if (!player.getInventory().add(coins)) player.drop(coins, false);
            remaining -= stack;
        }
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
        if (level().getGameTime() - lastTurnGameTime >= IDLE_TIMEOUT_TICKS) endConversation();
    }

    // ── NpcChatPartner ───────────────────────────────────────────────────────

    @Override public UUID getPartnerId() { return partnerId; }
    @Override public String tag() { return "Tallykeeper"; }
    @Override public int entityId() { return getId(); }
    @Override public String displayName() { return "The Tallykeeper"; }
    @Override public String displaySubtitle() { return "Royal Records Herald"; }
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

    // ── LLM dispatch ─────────────────────────────────────────────────────────

    private void generateReply(ServerPlayer player, String userMessage) {
        List<OpenRouterClient.Message> snapshot = List.copyOf(history);
        UUID expectedPartner = partnerId;
        MinecraftServer server = level().getServer();

        MicGate.muteFor(expectedPartner, 4_000L);
        PacketDistributor.sendToPlayer(player,
            new UpdateWardenScreenPayload(getId(),
                UpdateWardenScreenPayload.STATUS_HEARD, userMessage));

        PromptParts parts = buildSystemPromptParts(player);
        replyInFlight = true;
        OpenRouterClient.chatWithCache(
            OPENROUTER_MODEL, MAX_REPLY_TOKENS, SAMPLING_TEMPERATURE,
            parts.cacheable(), parts.dynamic(),
            snapshot, userMessage, "Tallykeeper",
            raw -> {
                if (server == null) return;
                server.execute(() -> {
                    replyInFlight = false;
                    if (raw == null || raw.isBlank()) return;
                    deliverLine(player, expectedPartner, userMessage, raw);
                });
            });
    }

    private void deliverLine(ServerPlayer player, UUID expectedPartner,
                             String userMessage, String rawLine) {
        if (partnerId == null || !partnerId.equals(expectedPartner)) return;
        String line = sanitizeForSpeech(rawLine);
        if (line.isBlank()) return;

        history.add(new OpenRouterClient.Message("user", userMessage));
        history.add(new OpenRouterClient.Message("assistant", line));
        while (history.size() > HISTORY_TURN_LIMIT) history.remove(0);

        PacketDistributor.sendToPlayer(player,
            new UpdateWardenScreenPayload(getId(),
                UpdateWardenScreenPayload.STATUS_REPLY, line));
        speakLine(line, player);
        Ironhold.LOGGER.info("[Tallykeeper] -> {}: \"{}\"", player.getName().getString(), line);
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
            if ((first == '"' && last == '"') || (first == '“' && last == '”')
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

    // ── NBT ───────────────────────────────────────────────────────────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        this.setCustomName(Component.literal("§6The Tallykeeper"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
    }
}
