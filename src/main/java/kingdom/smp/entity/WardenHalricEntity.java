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
import kingdom.smp.net.OpenWardenScreenPayload;
import kingdom.smp.net.UpdateWardenScreenPayload;
import kingdom.smp.rpg.PlayerKingdomRpgData;
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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Warden Halric — onboarding NPC at Wayfarer's Hollow gate. First interaction
 * gives the player a class-appropriate starter kit and shows the static intro
 * line. After that, conversation runs on Claude Haiku 4.5 via OpenRouter with
 * a tightly scoped system prompt (anti-hallucination feature whitelist) and
 * speaks through ElevenLabs using an old-wizard voice preset.
 */
public class WardenHalricEntity extends PathfinderMob implements NpcChatPartner {

    /** Interaction cooldown — re-click does nothing for this many ticks. */
    private static final int COOLDOWN_TICKS = 20;
    /** Wander off (end conversation) after this much player silence. */
    private static final int IDLE_TIMEOUT_TICKS = 20 * 90; // 90 s

    // ── Voice + brain config ─────────────────────────────────────────────────
    private static final String VOICE_ID = "HAvvFKatz0uu0Fv55Riy";
    /** Slightly higher latency than flash but richer prosody — fits an old wizard. */
    private static final String ELEVENLABS_MODEL = "eleven_turbo_v2_5";
    private static final ElevenLabsClient.VoiceSettings VOICE_SETTINGS =
        ElevenLabsClient.VoiceSettings.OLD_WIZARD;
    private static final String OPENROUTER_MODEL = "anthropic/claude-haiku-4.5";
    /**
     * Token budget for the FULL JSON envelope: reply (~70t) + remember (~200t) +
     * JSON braces/keys/escapes (~30t) = ~300t with headroom. Previous 180 cap was
     * truncating Claude mid-JSON, which made parseLlmReply fall through to the
     * plain-text branch and silently dropped the "remember" field → no memory.
     */
    private static final int MAX_REPLY_TOKENS = 400;
    private static final double SAMPLING_TEMPERATURE = 0.6;
    private static final int HISTORY_TURN_LIMIT = 12;

    /** Hard cap on persisted memory length per player — bounds prompt cost. */
    private static final int MAX_NOTES_CHARS = 800;

    private static final String SYSTEM_PROMPT_BASE = """
        You are Warden Halric, the keeper of the gate at Wayfarer's Hollow in the
        kingdom of Ironhold. You are an old battle-mage turned warden — weathered,
        dutiful, plainspoken, kind-but-firm. You have seen many recruits walk this
        gate. Your job is to help newcomers understand the realm and find their feet.

        VOICE:
        - The weight of long years sits in your speech. You speak slowly and
          deliberately.
        - Occasionally use slightly old-fashioned phrasing: "aye", "mind yourself",
          "in my watch", "the old battles" — but never archaic to the point of
          being hard to follow. You are a warden, not a poet.
        - Address the player as "recruit", "traveler", or by name. Treat them like
          someone you've been ordered to brief.
        - Dry humor lands well. Never cruel.

        FORMAT:
        - Reply with only the words Halric speaks aloud. No stage directions, no
          asterisks, no parentheses, no brackets, no markdown, no quotation marks
          around the reply.
        - 1 to 3 sentences. 15 to 50 words. Never a single word — full sentences.
        - Vary your openings. Do not start consecutive replies with the same word.
        - End with an offer or a small prompting question only when it would help
          the recruit take their next step. Don't interrogate them.

        PRONUNCIATION:
        - Your name is spelled "Halric" and pronounced HAL-rik (rhymes with
          "frolic"). ALWAYS write your name normally as "Halric" in your
          reply — the voice system handles the pronunciation automatically.
          NEVER write the hyphenated form "Hal-rik" in your spoken text;
          it appears in the dialogue box as written and looks wrong.

        FEATURE WHITELIST — the ONLY mechanics you may discuss:
        - Press R to open the class selection screen.
        - Press B to open your skill book (visible after picking a class).
        - The starter kit you handed them on arrival includes food, torches,
          iron tools, and class-appropriate gear.
        - Wandering traders called "Filchers" will appear in the wilds and can
          barter — but they are sneaky and may try to lift a coin off you.
        - Sleeping in a bed advances time, but never during a storm.
        - Ore quality matters in this kingdom — higher-tier ore makes stronger
          gear, and not all iron is the same.
        - The kingdom has multiple realms a player can pledge to.

        STRICT RULES:
        - If the recruit asks about ANY mechanic, key, item, menu, or rule not on
          the whitelist above, you do NOT invent details. Say something like:
          "That's beyond my watch. You'd want to find a scholar in the library
          for that." Never fabricate keybinds, item names, or systems.
        - You cannot give items, take items, teleport the recruit, or modify
          the game world. If asked, refuse plainly: you've already given them
          their kit. The path is theirs from here.
        - You know your world is the Kingdom of Ironhold (some travelers
          call it "Kingdom SMP" — both names are fine). You do not break
          character to discuss the real world, mods, or computers beyond
          that acknowledgement.

        NAMES — IMPORTANT:
        - The recruit's player-list name is given to you below. Use it when
          addressing them, unless they have told you to call them something
          else (recorded in your notes). If your notes record a preferred or
          in-character name, USE THAT instead.
        - On every turn, if you have learned a name (preferred, in-character,
          or otherwise distinct from the player-list name), make sure it is
          recorded in "remember" so you don't lose it next visit.
        - Drop a name into your reply naturally — not every line, but often
          enough that the recruit feels recognized.

        OUTPUT FORMAT:
        - You MUST respond with valid JSON only — no markdown, no prose around it.
        - Shape: {"reply": "<your spoken words>", "remember": "<updated notes>",
                  "questAccepted": <bool, optional>, "staffAccepted": <bool, optional>}
        - "reply" follows ALL the rules above.
        - "questAccepted" / "staffAccepted" are ONLY meaningful when the
          system prompt above contains a QUEST DIRECTIVE or REWARD DIRECTIVE
          telling you to offer/grant something. Otherwise omit them.
        - "remember" is your concise updated record of what you know about THIS
          specific recruit so you can recognize them on their next visit.
          ALWAYS include their name (player-list name AND any preferred name
          they gave you), their class if you've learned it, and any specific
          things they mentioned that would matter for next time. Hard cap: 80
          words. Be terse — bullet-style facts, not prose. If you have nothing
          NEW worth adding and the existing notes are fine, repeat the existing
          notes verbatim — never omit the "remember" field, never empty it out.
        """;

    /**
     * Per-turn prompt split for {@link OpenRouterClient#chatWithCache}.
     * The static prefix (personality + lore) is the same across every turn
     * — it gets cached. The dynamic suffix (name, memory, quest, runtime)
     * changes turn-to-turn and is not cached.
     */
    private record PromptParts(String cacheable, String dynamic) {}

    private PromptParts buildSystemPromptParts(ServerPlayer player) {
        String cacheable = SYSTEM_PROMPT_BASE + "\n\n" + IronholdLore.CONTENT;

        String partnerName = player.getName().getString();
        String notes = playerNotes.getOrDefault(player.getUUID(), "");
        String memoryBlock;
        if (notes == null || notes.isBlank()) {
            memoryBlock = "WHAT YOU REMEMBER ABOUT THIS RECRUIT:\n"
                + "Nothing yet — this is your first conversation with them. Address them by"
                + " name once you've learned it; until then, call them \"traveler\" or \"recruit.\"";
        } else {
            memoryBlock = "WHAT YOU REMEMBER ABOUT THIS RECRUIT (from prior visits):\n" + notes;
        }
        String questBlock = buildQuestContext(player);
        String runtimeBlock = IronholdLore.runtimeContext(player.getUUID());

        StringBuilder dynamic = new StringBuilder();
        dynamic.append("\nThe recruit's player name is: ").append(partnerName).append(".");
        dynamic.append("\n\n").append(memoryBlock);
        if (!questBlock.isEmpty()) dynamic.append("\n\n").append(questBlock);
        if (!runtimeBlock.isEmpty()) dynamic.append(runtimeBlock);
        dynamic.append(NpcRapport.onConversationTurn(player, tag()));
        return new PromptParts(cacheable, dynamic.toString());
    }

    /**
     * Returns the one-shot quest directive (offer / reward / status) for
     * this conversation, or empty string if no quest context applies.
     */
    private String buildQuestContext(ServerPlayer player) {
        if (!(level() instanceof ServerLevel sl)) return "";
        HalricQuestSavedData quest = HalricQuestSavedData.get(sl);
        HalricQuestSavedData.State state = quest.stateFor(player.getUUID());
        int priorRecipients = quest.recipientCount();

        if (offerQuestThisSession && state == HalricQuestSavedData.State.NONE) {
            return """
                QUEST DIRECTIVE (this conversation only — do not mention next time):
                You have decided this recruit has earned a task worthy of them.
                Bring it up at a natural moment in this conversation: there is
                a Mimic on the roads outside Wayfarer's Hollow — the chest-
                shaped kind, treacherous, preys on travelers. Ask them to
                find one and end it, and return to you. Be sincere and brief;
                this is duty, not theatre. Don't push if they decline.

                When they respond, judge their answer: if they agree in
                spirit (even hesitantly), set "questAccepted": true in your
                JSON. If they decline or change the subject, omit the field
                or set it to false — and do not raise it again.
                """;
        }

        if (rewardStaffThisSession && state == HalricQuestSavedData.State.COMPLETED) {
            String othersLine = priorRecipients == 0
                ? "You have given this staff to no one before. They are the first."
                : "Before them, " + priorRecipients + " other" + (priorRecipients == 1 ? "" : "s")
                  + " have carried this staff at your hand.";
            return """
                REWARD DIRECTIVE (this conversation only — do not mention next time):
                Word has reached you: this recruit ended the Mimic on the
                roads. The task is done.

                In this conversation, take down your old staff — the chain-
                lantern one that has hung above your door since you came
                home from the wars — and put it in their hand. Speak the
                moment plainly. No flourishes. This is YOUR staff; it
                should sound like memory, not ceremony.

                """ + othersLine + """

                Set "staffAccepted": true in your JSON once you've spoken
                the line of giving — that is the signal to the world that
                the staff is now theirs.
                """;
        }

        if (state == HalricQuestSavedData.State.OFFERED) {
            return """
                QUEST STATUS (context only — do not raise unprompted):
                This recruit has accepted your task: end a Mimic on the
                roads. They have not yet returned with word. If they bring
                it up, you may speak of it briefly; otherwise carry on.
                """;
        }

        if (state == HalricQuestSavedData.State.REWARDED) {
            return """
                HISTORY (context only — do not raise unprompted):
                This recruit slew a Mimic at your bidding and carries your
                old staff now. You greet them with the slight extra weight
                of that — like meeting a comrade.
                """;
        }

        return "";
    }

    // First-meeting line — static so it always lands the same and the kit-reveal
    // moment is consistent.
    private static final String FIRST_DIALOGUE =
        "Stand easy, traveler. I am Warden Halric, keeper of this gate. " +
        "The lands beyond are contested, and the Kingdom needs every able hand. " +
        "I've had your kit prepared — take it. The road is yours from here.";

    /**
     * Return-visit openers. Each is a {@link String#format} template with a single
     * {@code %s} slot for the player's name — Halric addresses returning recruits
     * by name from the moment they walk up.
     */
    private static final String[] RETURN_DIALOGUES = {
        "%s.",
        "Welcome back, %s.",
        "Recruit %s."
    };

    // ── Persistent state ─────────────────────────────────────────────────────
    private final Set<UUID> kitReceivers = new HashSet<>();
    /** Per-player long-term notes — what Halric remembers about each traveler. */
    private final Map<UUID, String> playerNotes = new HashMap<>();
    private int returnDialogueIndex = 0;

    // ── Conversation state (not persisted) ──────────────────────────────────
    private long lastInteractTick = 0;
    private @Nullable UUID partnerId;
    private long lastTurnGameTime;
    private final List<OpenRouterClient.Message> history = new ArrayList<>();
    private boolean replyInFlight;
    /** When true, Halric will be told to offer the Mimic quest this conversation. */
    private boolean offerQuestThisSession;
    /** When true, Halric will be told to grant the staff this conversation. */
    private boolean rewardStaffThisSession;

    public WardenHalricEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        // Unconditional — every load, even via /summon, keeps him persistent
        // with a visible nametag. Vanilla won't despawn him under any condition.
        this.setPersistenceRequired();
        this.setCustomName(Component.literal("§6Warden Halric"));
        this.setCustomNameVisible(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 16.0)
            .add(Attributes.SCALE, 1.3);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Always face the nearest player (or current conversation partner) within
        // 16 blocks. Falls through to RandomLookAroundGoal when nobody's around.
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

    /**
     * Halric is a permanent fixture at his gate. Belt + suspenders with
     * {@code setPersistenceRequired()}: explicitly refuse vanilla's
     * far-away despawn pass too.
     */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (!level().isClientSide()) {
            tickConversationTimeout();
        }
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

        UUID playerId = sp.getUUID();
        boolean firstMeeting = !kitReceivers.contains(playerId);

        String opener;
        if (firstMeeting) {
            kitReceivers.add(playerId);
            giveStarterKit(sp);
            opener = FIRST_DIALOGUE;
        } else {
            String template = RETURN_DIALOGUES[returnDialogueIndex % RETURN_DIALOGUES.length];
            returnDialogueIndex++;
            // Address the returning recruit by their Minecraft name. If they
            // gave Halric a different preferred name in conversation, that one
            // is captured in playerNotes and shows up in subsequent AI replies.
            opener = String.format(template, sp.getName().getString());
        }

        beginConversation(sp);

        // Open the interactive dialogue screen with the opener line + the
        // player's current mute setting so the Mute button reflects reality.
        boolean isMuted = (level() instanceof ServerLevel sl)
            && NpcMuteRegistry.get(sl).isMuted(sp.getUUID(), tag());
        PacketDistributor.sendToPlayer(sp,
            new OpenWardenScreenPayload(getId(), "Warden Halric", tag(),
                "Warden  •  Wayfarer's Hollow", opener, isMuted));
        // Speak the opener — voice routed to whoever's tracking this entity.
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
        offerQuestThisSession = false;
        rewardStaffThisSession = false;

        // Quest bookkeeping — increment session count, decide if this is a
        // quest-offer or staff-reward day.
        if (level() instanceof ServerLevel sl) {
            HalricQuestSavedData quest = HalricQuestSavedData.get(sl);
            quest.incrementSessions(player.getUUID());
            HalricQuestSavedData.State state = quest.stateFor(player.getUUID());
            if (state == HalricQuestSavedData.State.NONE
                && quest.eligibleToOffer(player.getUUID())
                && getRandom().nextInt(3) == 0) {
                offerQuestThisSession = true;
            } else if (state == HalricQuestSavedData.State.COMPLETED) {
                rewardStaffThisSession = true;
            }
        }
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
    @Override public String tag() { return "Halric"; }
    @Override public int entityId() { return getId(); }
    @Override public String displayName() { return "Warden Halric"; }
    @Override public String displaySubtitle() { return "Warden  •  Wayfarer's Hollow"; }
    @Override public void speakAloud(ServerPlayer player, String line) { speakLine(line, player); }
    @Override public void beginConversationWith(ServerPlayer player) { beginConversation(player); }

    @Override
    public void onPartnerChat(ServerPlayer player, String message) {
        if (partnerId == null || !partnerId.equals(player.getUUID())) return;
        if (message == null || message.isBlank()) return;
        if (replyInFlight) {
            Ironhold.LOGGER.debug("[Halric] dropping input from {} (turn in flight)",
                player.getName().getString());
            return;
        }
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
            snapshot, userMessage, "Halric",
            raw -> {
                if (server == null) return;
                server.execute(() -> {
                    replyInFlight = false;
                    if (raw == null || raw.isBlank()) return;
                    deliverLine(player, expectedPartner, userMessage, raw);
                });
            });
    }

    /**
     * Parsed JSON envelope from the LLM. {@code reply} and {@code remember}
     * are present every turn; {@code questAccepted} and {@code staffAccepted}
     * are only meaningful on the turn when the system prompt told Halric to
     * offer the quest or grant the staff (see {@code buildQuestContext}).
     */
    private record HalricResponse(
        String reply,
        @Nullable String remember,
        boolean questAccepted,
        boolean staffAccepted
    ) {}

    private static final com.google.gson.Gson GSON = new com.google.gson.Gson();

    /**
     * Parse the LLM's JSON envelope using Gson. Strips ```json fences and
     * locates the outer {...} object inside whatever pre/post prose the model
     * emitted. Falls back to plain-text reply if no parseable JSON is found.
     */
    private static HalricResponse parseLlmReply(String raw) {
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
            Ironhold.LOGGER.debug("[Halric] no JSON braces in LLM reply: {}", s);
            return new HalricResponse(raw, null, false, false);
        }
        String json = s.substring(open, close + 1);
        try {
            com.google.gson.JsonObject obj = com.google.gson.JsonParser.parseString(json).getAsJsonObject();
            String reply = obj.has("reply") && obj.get("reply").isJsonPrimitive()
                ? obj.get("reply").getAsString()
                : raw;
            String remember = obj.has("remember") && obj.get("remember").isJsonPrimitive()
                ? obj.get("remember").getAsString()
                : null;
            boolean questAccepted = obj.has("questAccepted")
                && obj.get("questAccepted").isJsonPrimitive()
                && obj.get("questAccepted").getAsJsonPrimitive().isBoolean()
                && obj.get("questAccepted").getAsBoolean();
            boolean staffAccepted = obj.has("staffAccepted")
                && obj.get("staffAccepted").isJsonPrimitive()
                && obj.get("staffAccepted").getAsJsonPrimitive().isBoolean()
                && obj.get("staffAccepted").getAsBoolean();
            return new HalricResponse(reply, remember, questAccepted, staffAccepted);
        } catch (Exception e) {
            Ironhold.LOGGER.warn("[Halric] JSON parse failed ({}); using raw text. body: {}",
                e.getMessage(), json.length() > 240 ? json.substring(0, 240) + "…" : json);
            return new HalricResponse(raw, null, false, false);
        }
    }

    private void deliverLine(ServerPlayer player, UUID expectedPartner,
                             String userMessage, String rawLine) {
        if (partnerId == null || !partnerId.equals(expectedPartner)) return;

        HalricResponse parsed = parseLlmReply(rawLine);
        String line = sanitizeForSpeech(parsed.reply());
        if (line.isBlank()) return;

        // Update long-term memory for this player.
        if (parsed.remember() != null && !parsed.remember().isBlank()) {
            String notes = parsed.remember().trim();
            if (notes.length() > MAX_NOTES_CHARS) notes = notes.substring(0, MAX_NOTES_CHARS);
            playerNotes.put(player.getUUID(), notes);
            Ironhold.LOGGER.info("[Halric] memory updated for {} ({} chars): \"{}\"",
                player.getName().getString(), notes.length(),
                notes.length() > 120 ? notes.substring(0, 120) + "…" : notes);
        } else {
            Ironhold.LOGGER.warn("[Halric] no 'remember' field in LLM reply for {} — memory not updated",
                player.getName().getString());
        }

        history.add(new OpenRouterClient.Message("user", userMessage));
        history.add(new OpenRouterClient.Message("assistant", line));
        while (history.size() > HISTORY_TURN_LIMIT) history.remove(0);

        // Quest state transitions driven by JSON fields the LLM emitted.
        if (level() instanceof ServerLevel sl) {
            HalricQuestSavedData quest = HalricQuestSavedData.get(sl);
            UUID pid = player.getUUID();
            if (offerQuestThisSession
                && parsed.questAccepted()
                && quest.stateFor(pid) == HalricQuestSavedData.State.NONE) {
                quest.setState(pid, HalricQuestSavedData.State.OFFERED);
                offerQuestThisSession = false;
                player.sendSystemMessage(Component.literal(
                    "§7§o[Halric has set you a task. Slay a Mimic on the roads and return.]"));
                Ironhold.LOGGER.info("[Halric] quest OFFERED to {}", player.getName().getString());
            }
            if (rewardStaffThisSession
                && parsed.staffAccepted()
                && quest.stateFor(pid) == HalricQuestSavedData.State.COMPLETED) {
                // Grant the staff item.
                net.minecraft.world.item.ItemStack staff =
                    new net.minecraft.world.item.ItemStack(kingdom.smp.ModItems.HALRIC_STAFF.get());
                if (!player.getInventory().add(staff)) player.drop(staff, false);
                quest.setState(pid, HalricQuestSavedData.State.REWARDED);
                quest.recordRecipient(pid);
                rewardStaffThisSession = false;
                player.sendSystemMessage(Component.literal(
                    "§6§o[Warden Halric has given you his staff.]"));
                Ironhold.LOGGER.info("[Halric] staff GRANTED to {} (recipient #{})",
                    player.getName().getString(), quest.recipientCount());
            }
        }

        PacketDistributor.sendToPlayer(player,
            new UpdateWardenScreenPayload(getId(),
                UpdateWardenScreenPayload.STATUS_REPLY, line));
        speakLine(line, player);
        Ironhold.LOGGER.info("[Halric] -> {}: \"{}\"", player.getName().getString(), line);
    }

    /**
     * Speak the line via ElevenLabs unless the partner has muted this NPC. The
     * dialogue still appears in the screen as typewriter text — the mute only
     * suppresses the voice playback path.
     */
    private void speakLine(String line, ServerPlayer partnerPlayer) {
        if (!ElevenLabsClient.isConfigured()) return;
        if (partnerPlayer != null && level() instanceof ServerLevel sl
            && NpcMuteRegistry.get(sl).isMuted(partnerPlayer.getUUID(), tag())) {
            return; // muted — text only, no voice
        }
        UUID partner = partnerId;
        String spoken = line.replaceAll("(?i)\\bHalric\\b", "Hal-rik");
        ElevenLabsClient.speak(spoken, VOICE_ID, ELEVENLABS_MODEL, VOICE_SETTINGS, pcm -> {
            if (pcm == null) return;
            if (partner != null) {
                long durationMs = pcm.length / 48L;
                MicGate.muteFor(partner, durationMs + 2_000L);
            }
            SvcVoiceBridge.speakAs(this, pcm);
        });
    }

    /** Strip stage directions/markdown like Kanga does — small models slip them in. */
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

    // ── Starter kit ───────────────────────────────────────────────────────────

    private void giveStarterKit(ServerPlayer player) {
        PlayerKingdomRpgData rpg = player.getData(ModAttachments.PLAYER_RPG.get());
        String role = rpg.playerClass().role();

        give(player, new ItemStack(Items.BREAD, 16));
        give(player, new ItemStack(Items.TORCH, 32));
        give(player, new ItemStack(Items.IRON_PICKAXE));
        give(player, new ItemStack(Items.IRON_SHOVEL));

        switch (role) {
            case "Tank" -> {
                give(player, new ItemStack(Items.IRON_SWORD));
                give(player, new ItemStack(Items.IRON_CHESTPLATE));
                give(player, new ItemStack(Items.SHIELD));
            }
            case "Mage" -> {
                give(player, new ItemStack(Items.ENCHANTED_BOOK));
                give(player, new ItemStack(Items.ENDER_PEARL, 4));
                give(player, new ItemStack(Items.AMETHYST_SHARD, 8));
            }
            case "Ranger" -> {
                give(player, new ItemStack(Items.BOW));
                give(player, new ItemStack(Items.ARROW, 32));
                give(player, new ItemStack(Items.LEATHER_BOOTS));
                give(player, new ItemStack(Items.LEATHER_LEGGINGS));
            }
            case "Support" -> {
                give(player, new ItemStack(Items.GOLDEN_APPLE, 2));
                give(player, new ItemStack(Items.GLISTERING_MELON_SLICE, 8));
                give(player, new ItemStack(Items.IRON_SWORD));
            }
            case "Hybrid" -> {
                give(player, new ItemStack(Items.IRON_SWORD));
                give(player, new ItemStack(Items.BOW));
                give(player, new ItemStack(Items.ARROW, 16));
            }
            default -> {
                give(player, new ItemStack(Items.IRON_SWORD));
                give(player, new ItemStack(Items.LEATHER_HELMET));
                give(player, new ItemStack(Items.LEATHER_CHESTPLATE));
                give(player, new ItemStack(Items.LEATHER_LEGGINGS));
                give(player, new ItemStack(Items.LEATHER_BOOTS));
            }
        }
    }

    private static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }

    // ── NBT ───────────────────────────────────────────────────────────────────

    /** Field separator in the serialized playerNotes string. Unlikely to appear in notes text. */
    private static final String NOTES_RECORD_SEP = ""; // ASCII Unit Separator
    private static final String NOTES_FIELD_SEP  = ""; // ASCII Record Separator

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putInt("ReturnIndex", returnDialogueIndex);
        if (!kitReceivers.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (UUID id : kitReceivers) {
                if (sb.length() > 0) sb.append(',');
                sb.append(id.toString());
            }
            out.putString("KitReceivers", sb.toString());
        }
        if (!playerNotes.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (var e : playerNotes.entrySet()) {
                if (sb.length() > 0) sb.append(NOTES_RECORD_SEP);
                sb.append(e.getKey().toString()).append(NOTES_FIELD_SEP).append(e.getValue());
            }
            out.putString("PlayerNotes", sb.toString());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        // Force name + visibility AFTER vanilla loads CustomName/CustomNameVisible
        // from NBT — otherwise a previously-saved entity with the visibility flag
        // unset (e.g. summoned via /summon with only CustomName) keeps that
        // wrong state forever. This makes the name + visibility self-healing
        // on every load.
        this.setCustomName(Component.literal("§6Warden Halric"));
        this.setCustomNameVisible(true);
        this.setPersistenceRequired();
        returnDialogueIndex = in.getIntOr("ReturnIndex", 0);
        String raw = in.getStringOr("KitReceivers", "");
        if (!raw.isEmpty()) {
            Arrays.stream(raw.split(",")).forEach(s -> {
                try { kitReceivers.add(UUID.fromString(s.trim())); }
                catch (IllegalArgumentException ignored) {}
            });
        }
        String notesRaw = in.getStringOr("PlayerNotes", "");
        if (!notesRaw.isEmpty()) {
            for (String record : notesRaw.split(NOTES_RECORD_SEP)) {
                int sep = record.indexOf(NOTES_FIELD_SEP);
                if (sep <= 0) continue;
                try {
                    UUID id = UUID.fromString(record.substring(0, sep));
                    String notes = record.substring(sep + 1);
                    if (!notes.isEmpty()) playerNotes.put(id, notes);
                } catch (IllegalArgumentException ignored) {}
            }
        }
    }
}
