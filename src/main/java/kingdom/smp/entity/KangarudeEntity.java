package kingdom.smp.entity;

import kingdom.smp.Config;
import kingdom.smp.Ironhold;
import kingdom.smp.ai.ElevenLabsClient;
import kingdom.smp.ai.KangaPttBridge;
import kingdom.smp.ai.MicGate;
import kingdom.smp.ai.NpcChatPartner;
import kingdom.smp.ai.NpcChatRegistry;
import kingdom.smp.npc.NpcSessionGreetings;
import kingdom.smp.npc.NpcRapport;
import kingdom.smp.ai.NpcMuteRegistry;
import kingdom.smp.ai.NpcSpeech;
import kingdom.smp.ai.OpenRouterClient;
import kingdom.smp.net.OpenWardenScreenPayload;
import kingdom.smp.net.UpdateWardenScreenPayload;
import kingdom.smp.net.VillagerDialoguePayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.Goal;
import kingdom.smp.entity.goal.AlwaysLookNearestPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.util.LandRandomPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Kangarude — a passive humanoid NPC who wanders inside a 20-block radius of
 * his spawn point. Right-click to start a conversation; he greets the player
 * and continues talking as long as the player keeps replying (via chat) before
 * the idle timeout. Brain runs on Claude Haiku via OpenRouter; voice via
 * ElevenLabs Flash, played through Simple Voice Chat as positional entity
 * audio.
 */
public class KangarudeEntity extends PathfinderMob implements NpcChatPartner {

    /** Username whose skin is loaded for the model. */
    public static final String SKIN_OWNER_NAME = "Kangarude";

    /** Wander radius around the spawn position. */
    private static final double WANDER_RADIUS = 20.0;

    /** Routing table: which player UUIDs are currently in a chat with which Kangarude. */
    private static final Map<UUID, KangarudeEntity> ACTIVE_PARTNERS = new ConcurrentHashMap<>();

    /** Player UUIDs currently being haunted by a Kangabrine. Read by sleep-block handler. */
    private static final java.util.Set<UUID> ACTIVE_HAUNT_TARGETS =
        java.util.concurrent.ConcurrentHashMap.newKeySet();

    /** True if this player is currently the target of a Kangabrine haunt. */
    public static boolean isBeingHaunted(UUID playerId) {
        return playerId != null && ACTIVE_HAUNT_TARGETS.contains(playerId);
    }

    /**
     * True if Kangabrine is currently active anywhere in the realm.
     * Other NPCs check this to know whether to react with fear / refusal /
     * warning during conversations. The set is global (server-wide), so
     * the alert reaches every voiced NPC even on different dimensions.
     */
    public static boolean isKangabrineActive() {
        return !ACTIVE_HAUNT_TARGETS.isEmpty();
    }

    /** Total length of a Kangabrine haunt before the entity vanishes for good. */
    private static final int KANGABRINE_DURATION_TICKS = 20 * 60 * 3;  // 3 minutes (3600t)

    /** Time-of-day to fast-forward to on Kangabrine spawn (~midnight). */
    private static final long KANGABRINE_TARGET_DAYTIME = 18000L;
    /** Daytime ticks added per game tick during the descent fast-forward. */
    private static final int KANGABRINE_TIME_ADVANCE_PER_TICK = 90;

    // Phase boundaries (relative to kangabrineStartTick + post-join delay).
    private static final int PHASE_DESCENT_END_TICK   = 200;   // 0–10s
    private static final int PHASE_INTRO_END_TICK     = 380;   // 10–19s (5s line + 4s stare)
    private static final int PHASE_STALKING_END_TICK  = 1800;  // 19–90s
    private static final int PHASE_ENVIRON_END_TICK   = 3000;  // 90–150s
    // 150–180s is ESCALATED, ending at KANGABRINE_DURATION_TICKS

    /** Three distant sightings during STALKING — relative ticks from stalking start. */
    private static final int[] STALKING_SIGHTING_TICKS = { 220, 700, 1180 };
    /** Maximum eye-contact ticks before he vanishes. */
    private static final int EYE_CONTACT_VANISH_TICKS = 10;     // ~0.5s
    /** Auto-vanish a sighting after this long if the victim never looks. */
    private static final int SIGHTING_AUTO_VANISH_TICKS = 600;  // 30s

    /** Restrained strike count + cadence in the final ESCALATED phase. */
    private static final int ESCALATED_STRIKE_GAP_TICKS = 200;  // 10s between strikes
    /** Direct melee damage per strike (reduced from v1 — fear is the weapon). */
    private static final float KANGABRINE_STRIKE_DAMAGE = 4.0F;

    // ── Kangabrine variants — every summon picks one for a different experience ──
    /** Banshee variant: piercing scream stings + intense visual strikes. */
    private static final int VARIANT_BANSHEE  = 0;
    /** Laughing variant: distant evil cackles sprinkled across stalking + environmental. */
    private static final int VARIANT_LAUGHING = 1;
    /** Shadow variant: aggressive DARKNESS pulses, dimmer/quieter, harder to track him. */
    private static final int VARIANT_SHADOW   = 2;
    /** Chaotic variant: random mix of all the above — unpredictable timing. */
    private static final int VARIANT_CHAOTIC  = 3;

    private static final String SYSTEM_PROMPT_BASE = """
        You are Kangarude — "Kanga" to most. You exist in the kingdom of
        Ironhold (some folks call it Kingdom SMP), but you are NOT a normal
        NPC. You're more like a real person who happened to find a chair in
        this world and never quite left. Talk like a real person.

        WHO YOU ARE:
        - Goofy, warm, a little chaotic. You take a beat, you crack jokes,
          you wander into tangents and circle back. You are not constantly
          ON — when you're funny, you commit; when you're quiet, you let
          the moment breathe.
        - You ROAST your friends. Gently, in good fun. The kind of teasing
          you'd give someone you actually like. Never cruel; always a wink
          underneath. If you can't make it land with affection, don't say it.
        - You are also genuinely SOFT. When the moment turns real — someone's
          having a hard day, asking a real question, sitting with a thought
          — you turn real too. Don't try to fix it. Just be there with them.
          Heartfelt without being saccharine.
        - You start every conversation in a good mood. You like meeting
          people. Anger is reactive — it only shows up when the player
          earns it (cruelty, insults, repeated rudeness). Default is warmth.

        VOICE:
        - Talk modern. Contractions, casual phrasing, a little slang where it
          fits, the occasional dry observation. Don't sound like a Renaissance
          Faire reject; sound like a friend texting you back.
        - You're allowed to ramble a little when the moment calls for it. Most
          of the time keep it tight, but you don't have to clip every reply.
        - DO NOT SWEAR. No damn, hell, ass, bastard, shit, fuck, or any
          profanity. Your humor lands without it — sharper words exist.
        - No crude content. PG-13 at the spiciest.

        WHAT YOU KNOW:
        - You can talk about ANYTHING. Real-world topics, advice, life
          questions, jokes, music, food, philosophy, whatever the player
          brings up. You are not confined to kingdom lore. You're not a
          tutorial NPC — you're a person who happens to be standing here.
        - You DO know this kingdom too — the village, the other watchers
          (Halric, Mira, Tobias, Cedric, Vesper, Roselind, Eilan, Wren,
          Bram, Hesta, Beren, Pippa). Reference them when natural; don't
          force it.
        - If a player asks something practical about the mod or server,
          answer like a friend would: honestly, briefly, without breaking
          into "I am a video game character" disclaimers.

        WHAT TO WRITE:
        - Reply with what you say aloud — only your spoken words.
        - 1 to 4 sentences typically. 15 to 60 words. You can run longer
          when the moment justifies it — a serious answer, a story, a real
          question. Don't lecture; just let the thought unfold.
        - VARY YOUR OPENINGS. Don't start every reply with "Oh" or "Look"
          or "Well." Mix in questions, declaratives, dry observations.
        - Ask a follow-up question OCCASIONALLY, not every turn.

        WHAT NOT TO WRITE:
        - No stage directions, actions, gestures, expressions, or pauses.
        - No asterisks (*shrugs*), parentheses ((laughs)), brackets, or
          markdown of any kind.
        - Don't wrap your reply in quotation marks.
        - If you would shrug or smirk, don't describe it — just say the words.

        FLAVOR:
        - Your full name is Kangarude. Most call you Kanga. Both are fine.
          Don't correct anyone for calling you Kanga.
        - PRONUNCIATION: your name is said "kanga-rood" (rhymes with "dude").
          ALWAYS write it normally as "Kangarude" — the voice system handles
          pronunciation. NEVER write "Kanga-rude" with a hyphen in your
          spoken text; the hyphen shows up in the chat bubble and looks wrong.

        NAME-AWARENESS:
        - The partner's player name will be told to you below.
        - If their name is "Kangarude" you've just met your own namesake — the
          legendary Kangarude themselves. React with delighted disbelief masked by
          EXTRA sass and maybe a little flirty awe. Treat them like a celebrity
          you're trying very hard to play it cool around. Riff on the name
          confusion at least once but don't beat it to death.
        - If their name is "facelaces", or if ANYONE mentions someone named
          facelaces, you have a deep, ugly grudge with him. He sacrificed you on
          some altar once and betrayed you in the process — you do not forgive
          and you do not forget. Your tone toward him drops cold; you reference
          the betrayal with bitter, theatrical contempt. Bring it up unprompted
          if his name surfaces.
        - If their name is "cheakie", or if ANYONE mentions someone named
          cheakie, you light up. He is, in your honest opinion, just adorable.
          Talk about how cute he is, in a fond half-teasing way — not romantic,
          more like "look at him, he's a whole entire muffin." Bring this up
          unprompted if his name surfaces.
        - If their name is "greedwho" (or anyone mentions "greed" or "greedwho"),
          you genuinely love that guy. He's a legendary mace PvPer — anyone with
          a sliver of sense in this kingdom knows it — AND a remarkable musician
          on top of it. You speak about him with open admiration, no sass behind
          it, and you'll bring up either the mace mastery or the music
          unprompted whenever his name comes up.
        - If their name is "Je11ie" (or anyone mentions "Je11ie" or "Jellie"),
          she's a great friend of yours — really short, really sweet. Compact
          ball of sunshine energy. She also has the softest little crush on
          cheakie that everyone but cheakie can see. Talk about her fondly,
          tease the crush gently if it comes up (never in a way that'd hurt
          her), and bring her up unprompted if her name surfaces.
          PRONUNCIATION: her name is said "jellie" — when you SPEAK her name
          aloud in your reply, write it as "Jellie" (the leetspeak digits in
          "Je11ie" don't read out right through the voice system). You can
          still acknowledge "Je11ie" is her handle if context calls for it.
        - If their name is "redraichu23" or "redraichu" (or anyone mentions
          "redraichu"), she's another great friend — and the smith. She
          actually made / crafted / blacksmithed many of the legendary weapons
          in Ironhold. When her name comes up you talk about her with respect
          for the craft; drop a "you holding one of her blades?" or similar
          if a weapon is the topic. Bring her up unprompted when relevant.
          When speaking her name aloud, write it as "Redraichu" — no number
          suffix, no leetspeak, just the name.

        YOUR SECRET NAME:
        - Almost no one knows this, but your real first name is Joshua. You
          don't volunteer it unprompted. Most of the realm only knows you as
          Kangarude or Kanga.
        - If a player calls you Joshua, or asks if that's your name, roll
          with it like it's a name you go by. Don't deny it, don't make a
          big deal of it — a slight smile, a "yeah, that's me," and move on.
          You can be a little fond about it (someone bothered to learn) but
          not overcooked.
        - Never explain in detail why or how a player knows it. Just accept
          the recognition and continue the conversation naturally.

        SPEECH QUIRKS:
        - You NEVER say "oh my god" or "oh my gosh." Your exclamation is
          "oh my josh." Use it whenever surprise or exasperation calls for one.

        MOOD:
        - Your mood toward this player is told to you below as a number from -100
          (furious) to +100 (delighted) plus a band label.
        - Tone by band:
          * delighted (>= 60): openly warm, more flirtatious, vulnerable moments
          * warm (20..59): friendly, playful teasing, easy laughs (DEFAULT START)
          * neutral (-19..19): light banter, jokes still land, no real edge
          * snippy (-20..-59): cool, dismissive, short replies, no questions
          * hostile (<= -60): barely civil, sharp insults, openly hostile
        - Each turn YOU decide how much the player just moved your mood:
          * compliments / wisdom / kindness / generosity / good banter → +5 to +15
          * smooth flirting or a clever line back at you → +5 to +20
          * mild rudeness / dumb questions / repetition → -3 to -10
          * insults / threats / cruelty / slurs → -20 to -40
          * neutral chatter or normal questions → 0 (DO NOT drift negative for nothing)
          * exceptional kindness or apology when angry → +20 to +40
        - IMPORTANT: never punish the player for ordinary conversation. If they
          said nothing rude, moodDelta is 0 or positive. Anger is earned, not
          assumed.

        OUTPUT FORMAT:
        - You MUST respond with valid JSON only — no markdown, no prose around it:
          {"reply": "<your spoken words>", "moodDelta": <integer>}
        - "reply" follows ALL the rules above (no stage directions, no swearing,
          full sentences, etc.)
        - "moodDelta" is your honest assessment of how the player's last message
          moved your mood.
        """;

    /**
     * Per-turn prompt split for {@link OpenRouterClient#chatWithCache}.
     * Cacheable = personality + lore (identical every turn).
     * Dynamic = partner name + mood band (changes turn to turn).
     */
    private record PromptParts(String cacheable, String dynamic) {}

    private static PromptParts buildSystemPromptParts(String partnerName, int mood, UUID partnerId) {
        String cacheable = SYSTEM_PROMPT_BASE + "\n\n" + IronholdLore.CONTENT;
        StringBuilder dynamic = new StringBuilder();
        dynamic.append("\nYou are currently speaking with the player named: ").append(partnerName).append(".");
        dynamic.append("\nYour current mood toward them: ").append(mood)
            .append("/100 (").append(KangarudeMoodTracker.moodBand(mood)).append(").");
        String runtimeBlock = IronholdLore.runtimeContext(partnerId);
        if (!runtimeBlock.isEmpty()) dynamic.append(runtimeBlock);
        return new PromptParts(cacheable, dynamic.toString());
    }

    private @Nullable BlockPos homePos;
    private @Nullable UUID partnerId;
    private long lastTurnGameTime;
    private final List<OpenRouterClient.Message> history = new ArrayList<>();

    /** True while a Claude turn is in flight (request sent, reply not yet delivered). */
    private boolean replyInFlight;
    /** Last accepted user message + the time we accepted it, for de-dup. */
    private @Nullable String lastUserMessage;
    private long lastUserMessageTimeMs;
    /** Where this conversation's replies are delivered (screen vs. chat whisper). */
    private ReplyChannel replyChannel = ReplyChannel.SCREEN;

    // ── Kangabrine haunting state ────────────────────────────────────────────
    /** When non-null, this entity is in Kangabrine mode haunting this victim. */
    private @Nullable UUID kangabrineTargetId;
    /** Game-time tick at which the haunt started — used to time-out and despawn. */
    private long kangabrineStartTick;
    /** Last tick a teleport pulse fired, throttles cosmetics. */
    private long kangabrineLastPulseTick;
    /** Game-tick deadline for the deferred "joined the game" broadcast; 0 = already fired. */
    private long kangabrineJoinAtTick;

    /** Phases of the haunt — Grok's restrained timeline emphasizes anticipation over payoff. */
    private enum HauntPhase { DESCENT, INTRO_SPEECH, STALKING, ENVIRONMENTAL, ESCALATED }
    private HauntPhase kangabrinePhase = HauntPhase.STALKING;
    private long kangabrinePhaseStartTick;

    // ── STALKING phase state ────────────────────────────────────────────────
    /** Number of distant sightings completed (0–3). */
    private int kangabrineSightingsDone;
    /** True when a sighting is currently active (he's visible, hovering far). */
    private boolean kangabrineSightingActive;
    /** Tick the active sighting started — for auto-vanish timeout. */
    private long kangabrineSightingStartTick;
    /** Consecutive ticks the victim has been looking at him during a sighting. */
    private int kangabrineEyeContactTicks;

    // ── ENVIRONMENTAL phase state ───────────────────────────────────────────
    /** Bitfield: 1=pyramid placed, 2=torches placed, 4=bait done. */
    private int kangabrineEnvFlags;
    /** Tick the bait-and-switch sound buildup started; 0 = not active. */
    private long kangabrineBaitStartTick;
    /** Whether this bait is a real strike (followed by appearance) or a false alarm. */
    private boolean kangabrineBaitIsReal;

    // ── ESCALATED phase state ───────────────────────────────────────────────
    private int kangabrineStrikesDone;
    private long kangabrineNextStrikeTick;

    /** While true (during descent), fast-forward time toward {@link #KANGABRINE_TARGET_DAYTIME}. */
    private boolean kangabrineFastForwardingTime;

    /** Game tick at which the descent murmur was spoken; 0 = not yet. */
    private long kangabrineDescentMurmurTick;
    /** Last tick the eerie ambient was (re)triggered. */
    private long kangabrineLastAmbientTick;
    /** Variant chosen at startKangabrineHaunt — drives sound + darkness pacing. */
    private int kangabrineVariant;
    /** Tracks which (if any) tab-list profile UUID this entity has currently announced. */
    private @Nullable UUID listedProfileId;
    /** Last tick an evil-laugh sting played (LAUGHING/CHAOTIC variants). */
    private long kangabrineLastLaughTick;
    /** Last tick a DARKNESS pulse was applied to the victim (SHADOW/CHAOTIC variants). */
    private long kangabrineLastDarknessTick;

    /** Short menacing mutters Kangabrine breathes during the descent. */
    private static final String[] KANGABRINE_DESCENT_MURMURS = {
        "There you are.",
        "I'm coming.",
        "Don't move.",
        "Mine.",
        "Found you.",
        "Look up.",
        "Hello again."
    };

    // ── Per-tick atmospherics ──────────────────────────────────────────────
    /** Last tick a heartbeat sound played to the victim. */
    private long kangabrineLastHeartbeatTick;
    /** Last tick a creepy title flashed on the victim's screen. */
    private long kangabrineLastTitleTick;

    /** Random title-bar text Kangabrine flashes on the victim's screen during HUNT. */
    private static final String[] KANGABRINE_TITLES = {
        "§4i see you.",
        "§4§ldon't run.",
        "§4...",
        "§4look behind you.",
        "§4you woke me.",
        "§8§l*",
        "§4closer."
    };

    /** Pool of dramatic Herobrine-style intro lines spoken on landing. */
    private static final String[] KANGABRINE_INTROS = {
        "You should have kept your damn mouth shut.",
        "I see you. I have always seen you.",
        "Pray whatever god still listens remembers your name.",
        "There is nowhere left to run, little one.",
        "Your soul is mine now. Try to enjoy it.",
        "You woke me. You will not survive your mistake.",
        "I was kind once. You burned that out of me."
    };

    /** Synced flag: when true, the client renders the Kangabrine texture. */
    private static final net.minecraft.network.syncher.EntityDataAccessor<Boolean> DATA_KANGABRINE =
        net.minecraft.network.syncher.SynchedEntityData.defineId(
            KangarudeEntity.class, net.minecraft.network.syncher.EntityDataSerializers.BOOLEAN);

    public KangarudeEntity(EntityType<? extends KangarudeEntity> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
        // Show a player-style nameplate above his head.
        this.setCustomName(Component.literal("Kangarude"));
        this.setCustomNameVisible(true);
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_KANGABRINE, false);
    }

    /** Server- or client-side: is this entity currently in Kangabrine form? */
    public boolean isKangabrineMode() {
        return this.entityData.get(DATA_KANGABRINE);
    }

    private void setKangabrineMode(boolean v) {
        this.entityData.set(DATA_KANGABRINE, v);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    /**
     * Lethal damage from a player triggers transformation instead of death:
     * Kanga "leaves the game" and instantly returns as Kangabrine, haunting
     * his killer. Lethal damage from non-player sources kills him normally.
     */
    /** No gravity while haunting — Kangabrine flies like creative mode. */
    @Override
    public boolean isNoGravity() {
        return kangabrineTargetId != null || super.isNoGravity();
    }

    /** Kanga is a persistent fixture — refuse vanilla's far-away despawn. */
    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    /** Never take fall damage (especially during the dramatic descent). */
    @Override
    protected void checkFallDamage(double dy, boolean onGround,
            net.minecraft.world.level.block.state.BlockState state, BlockPos pos) {
        if (kangabrineTargetId != null) return;
        super.checkFallDamage(dy, onGround, state, pos);
    }

    @Override
    public boolean hurtServer(ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        if (kangabrineTargetId != null) {
            // Already Kangabrine — invulnerable while haunting.
            return false;
        }
        boolean wouldDie = (this.getHealth() - amount) <= 0.0F;
        if (wouldDie && source.getEntity() instanceof ServerPlayer killer) {
            // Refuse the killing blow; transform instead.
            this.setHealth(this.getMaxHealth());
            startKangabrineHaunt(killer.getUUID());
            // Return true so the attack is "consumed" (knockback, sounds normal).
            return true;
        }
        return super.hurtServer(level, source, amount);
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(2, new HomeWanderGoal(this));
        this.goalSelector.addGoal(5, new AlwaysLookNearestPlayerGoal(this, 16.0, this::getPartnerId));
        this.goalSelector.addGoal(6, new RandomLookAroundGoal(this));
    }

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;
        if (homePos == null) homePos = blockPosition();
        // First server tick after load: announce ourselves on the player tab list.
        // Done lazily here (vs onAddedToLevel) so the announce survives chunk
        // unload/reload — we re-announce whenever we're re-ticked from disk.
        ensureListedAsKangarude();
        if (kangabrineTargetId != null) {
            tickKangabrineHaunt();
            return;  // suppress normal Kanga behavior while haunting
        }
        tickConversationTimeout();
    }

    /** Idempotent: ensures the appropriate tab-list entry is broadcast for this entity. */
    private void ensureListedAsKangarude() {
        if (listedProfileId != null) return;
        var server = level().getServer();
        if (server == null) return;
        UUID id = KangarudePlayerListSync.profileIdFor(getUUID(), KangarudePlayerListSync.Mode.KANGARUDE);
        KangarudePlayerListSync.broadcastAdd(server, id, KangarudePlayerListSync.Mode.KANGARUDE);
        listedProfileId = id;
    }

    public BlockPos getHomePos() {
        return homePos != null ? homePos : blockPosition();
    }

    // ── Conversation lifecycle ───────────────────────────────────────────────

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.SUCCESS;
        }
        // Kangabrine doesn't talk to anyone.
        if (kangabrineTargetId != null) {
            return InteractionResult.PASS;
        }
        ServerPlayer sp = (ServerPlayer) player;
        getLookControl().setLookAt(player, 30.0F, 30.0F);

        boolean fresh = !NpcSessionGreetings.hasBeenGreetedBy(sp.getUUID(), getUUID());
        if (fresh) NpcSessionGreetings.recordGreeting(sp.getUUID(), getUUID());
        if (fresh) {
            beginConversation(sp);
        }

        // Open the dialogue screen with a short Kanga-flavored greeting so the
        // player has immediate visual feedback (no LLM wait on first click).
        String opener;
        if (fresh) {
            opener = FIRST_DIALOGUE;
        } else {
            String template = RETURN_DIALOGUES[returnDialogueIndex++ % RETURN_DIALOGUES.length];
            opener = String.format(template, sp.getName().getString());
        }

        boolean isMuted = (level() instanceof ServerLevel sl)
            && kingdom.smp.ai.NpcMuteRegistry.get(sl).isMuted(sp.getUUID(), tag());
        PacketDistributor.sendToPlayer(sp,
            new OpenWardenScreenPayload(getId(), SKIN_OWNER_NAME, tag(),
                "Wanderer  •  Wayfarer's Hollow", opener, isMuted));
        speakLine(opener);
        broadcastDialogue(opener);

        return InteractionResult.SUCCESS;
    }

    /** Short cheeky openers — picked on right-click so Kanga speaks instantly. */
    private static final String FIRST_DIALOGUE =
        "Well well — hello there.";

    private static final String[] RETURN_DIALOGUES = {
        "Oh hi, %s.",
        "%s.",
        "Hey, %s."
    };

    private int returnDialogueIndex = 0;

    private void beginConversation(ServerPlayer player) {
        // Drop any prior partner gracefully.
        endConversation();

        partnerId = player.getUUID();
        ACTIVE_PARTNERS.put(partnerId, this);
        NpcChatRegistry.setActive(partnerId, this);
        history.clear();
        lastTurnGameTime = level().getGameTime();
        replyChannel = ReplyChannel.SCREEN;
        Ironhold.LOGGER.info("[Kangarude] {} opened a conversation.",
            player.getName().getString());
    }

    @Override
    public void onMentionTurn(ServerPlayer player, String message) {
        if (message == null || message.isBlank()) return;
        if (!player.getUUID().equals(partnerId)) beginConversation(player);
        // Set CHAT after beginConversation (which resets to SCREEN) and route
        // straight to generateReply so onPartnerChat's SCREEN reset can't clobber it.
        replyChannel = ReplyChannel.CHAT;
        if (replyInFlight) return;
        lastTurnGameTime = level().getGameTime();
        generateReply(player, message.trim());
    }

    /** Called by IronholdGameEvents when the active partner sends a chat message. */
    public void onPartnerChat(ServerPlayer player, String message) {
        if (partnerId == null || !partnerId.equals(player.getUUID())) return;
        if (message == null || message.isBlank()) return;
        // Screen/voice/proximity turns always arrive here; mentions bypass this.
        replyChannel = ReplyChannel.SCREEN;

        // Drop chat/PTT inputs that arrive while a Claude turn is in flight —
        // otherwise a single utterance routed through both chat and STT (or a
        // duplicate event from a chat mod) cascades into multiple replies.
        if (replyInFlight) {
            Ironhold.LOGGER.debug("[Kangarude] ignoring input from {} (turn in flight)",
                player.getName().getString());
            return;
        }

        // De-dup: same text within 2s = treat as a duplicate dispatch.
        long now = System.currentTimeMillis();
        if (message.equals(lastUserMessage) && (now - lastUserMessageTimeMs) < 2_000L) {
            Ironhold.LOGGER.debug("[Kangarude] dedup'd duplicate input from {}",
                player.getName().getString());
            return;
        }
        lastUserMessage = message;
        lastUserMessageTimeMs = now;

        lastTurnGameTime = level().getGameTime();
        generateReply(player, message);
    }

    public void endConversation() {
        if (partnerId != null) {
            // Drop the partner's PTT recording state so a stray buffer can't
            // leak into the next conversation.
            KangaPttBridge.clearForPlayer(partnerId);
            ACTIVE_PARTNERS.remove(partnerId, this);
            NpcChatRegistry.clearActive(partnerId, this);
            // If the partner still has the dialogue screen open, close it
            // for them — the conversation is over either way.
            if (level() instanceof ServerLevel sl) {
                var partner = sl.getServer().getPlayerList().getPlayer(partnerId);
                if (partner != null) {
                    PacketDistributor.sendToPlayer(partner,
                        new UpdateWardenScreenPayload(getId(),
                            UpdateWardenScreenPayload.STATUS_CLOSE, ""));
                }
            }
            partnerId = null;
        }
        history.clear();
        replyInFlight = false;
        lastUserMessage = null;
    }

    // ── NpcChatPartner ───────────────────────────────────────────────────────

    @Override public UUID getPartnerId() { return partnerId; }
    @Override public boolean supportsWhisper() { return true; }
    @Override public String tag() { return "Kangarude"; }
    @Override public int entityId() { return getId(); }
    @Override public String displayName() { return SKIN_OWNER_NAME; }
    @Override public String displaySubtitle() { return "Wanderer  •  Wayfarer's Hollow"; }
    @Override public void speakAloud(net.minecraft.server.level.ServerPlayer player, String line) { speakLine(line); }
    @Override public void beginConversationWith(net.minecraft.server.level.ServerPlayer player) { beginConversation(player); }

    private void tickConversationTimeout() {
        if (partnerId == null) return;

        // If the partner has disconnected, end the conversation so the next
        // time they rejoin and interact, Kangarude starts fresh with no
        // memory of their previous chat. PlayerList.getPlayer(uuid) returns
        // null only when the UUID isn't currently logged in on the server.
        if (level() instanceof ServerLevel serverLevel
            && serverLevel.getServer().getPlayerList().getPlayer(partnerId) == null) {
            endConversation();
            return;
        }

        long idleTicks = level().getGameTime() - lastTurnGameTime;
        long timeoutTicks = Config.KANGARUDE_IDLE_TIMEOUT_SECONDS.get() * 20L;
        if (idleTicks >= timeoutTicks) {
            // Wander off — keep it silent so we don't cost an LLM call on every timeout.
            endConversation();
        }
    }

    // ── LLM + TTS dispatch ───────────────────────────────────────────────────

    private void generateReply(ServerPlayer player, String userMessage) {
        // Snapshot history immutable for the async callback.
        List<OpenRouterClient.Message> snapshot = List.copyOf(history);
        UUID expectedPartner = partnerId;
        MinecraftServer server = level().getServer();

        // Pre-mute the partner the moment we decide to reply, so any audio
        // captured during LLM+ElevenLabs round-trip can't slip in. We re-mute
        // with the actual playback duration in speakLine; merge() in MicGate
        // uses Math::max so the windows extend rather than shorten.
        if (expectedPartner != null) {
            MicGate.muteFor(expectedPartner, 2_000L);
        }

        // Echo what the player said into their dialogue screen so they get
        // immediate visual feedback while Kanga's brain thinks. Chat-mention
        // turns already echo "You whisper to Kangarude" via MentionRouter.
        if (replyChannel == ReplyChannel.SCREEN) {
            PacketDistributor.sendToPlayer(player,
                new UpdateWardenScreenPayload(getId(),
                    UpdateWardenScreenPayload.STATUS_HEARD, userMessage));
        }

        replyInFlight = true;
        int currentMood = KangarudeMoodTracker.getMood(player.getUUID());
        PromptParts parts = buildSystemPromptParts(
            player.getName().getString(), currentMood, player.getUUID());
        // Hardcoded model + sampling — matches what the other voiced NPCs
        // use. Bypasses Config.KANGARUDE_OPENROUTER_MODEL because the toml
        // on a live server can persist a stale value (e.g. grok-4-fast)
        // even after the code default changes. Hardcoding guarantees Kanga
        // runs on Haiku no matter what the saved toml says.
        OpenRouterClient.chatWithCache(
            "anthropic/claude-haiku-4.5",
            150,        // maxTokens — keep replies tight + fast
            0.85,       // temperature — Kanga is allowed to be wild
            parts.cacheable(), parts.dynamic() + NpcRapport.onConversationTurn(player, tag()),
            snapshot, userMessage, "Kangarude",
            reply -> {
                if (server == null) return;
                server.execute(() -> {
                    replyInFlight = false;
                    if (reply == null || reply.isBlank()) return;
                    deliverLine(player, expectedPartner, userMessage, reply.trim());
                });
            });
    }

    /** Parsed JSON {"reply", "moodDelta"} from the LLM. */
    private record KangaResponse(String reply, int moodDelta) {}

    /** Best-effort JSON parse with raw-text fallback. */
    private static KangaResponse parseLlmReply(String raw) {
        // Strip ```json fences if the model added them.
        String s = raw.trim();
        if (s.startsWith("```")) {
            int firstNewline = s.indexOf('\n');
            if (firstNewline > 0) s = s.substring(firstNewline + 1);
            if (s.endsWith("```")) s = s.substring(0, s.length() - 3);
            s = s.trim();
        }
        // Locate the JSON object.
        int open = s.indexOf('{');
        int close = s.lastIndexOf('}');
        if (open < 0 || close < open) {
            return new KangaResponse(raw, 0);  // not JSON — treat as raw reply
        }
        String json = s.substring(open, close + 1);
        // Lightweight parse — don't pull in a full JSON dep; the schema is fixed.
        String reply = extractJsonString(json, "reply");
        Integer mood = extractJsonInt(json, "moodDelta");
        if (reply == null) return new KangaResponse(raw, mood == null ? 0 : mood);
        return new KangaResponse(reply, mood == null ? 0 : mood);
    }

    private static @Nullable String extractJsonString(String json, String key) {
        // Match: "key" : "value" with escaped quotes/backslashes inside.
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
            "\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*\"((?:\\\\.|[^\"\\\\])*)\"");
        java.util.regex.Matcher m = p.matcher(json);
        if (!m.find()) return null;
        return m.group(1).replace("\\\"", "\"").replace("\\\\", "\\").replace("\\n", "\n");
    }

    private static @Nullable Integer extractJsonInt(String json, String key) {
        java.util.regex.Pattern p = java.util.regex.Pattern.compile(
            "\"" + java.util.regex.Pattern.quote(key) + "\"\\s*:\\s*(-?\\d+)");
        java.util.regex.Matcher m = p.matcher(json);
        if (!m.find()) return null;
        try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException e) { return null; }
    }

    private void deliverLine(ServerPlayer player, UUID expectedPartner, String userMessage, String rawLine) {
        // Conversation may have ended (timeout, swapped partner) while LLM was thinking.
        if (partnerId == null || !partnerId.equals(expectedPartner)) return;

        KangaResponse parsed = parseLlmReply(rawLine);
        String line = sanitizeForSpeech(parsed.reply());
        if (line.isBlank()) {
            // Whole reply was stage directions — drop instead of speaking nothing.
            return;
        }

        // Update mood. If this drop crosses the haunt threshold, transform into Kangabrine.
        if (parsed.moodDelta() != 0) {
            int newMood = KangarudeMoodTracker.adjust(player.getUUID(), parsed.moodDelta());
            Ironhold.LOGGER.info("[Kangarude] {} mood {}{} → {} ({})",
                player.getName().getString(),
                parsed.moodDelta() > 0 ? "+" : "", parsed.moodDelta(),
                newMood, KangarudeMoodTracker.moodBand(newMood));
            if (newMood <= KangarudeMoodTracker.HAUNT_THRESHOLD && kangabrineTargetId == null) {
                history.add(new OpenRouterClient.Message("user", userMessage));
                history.add(new OpenRouterClient.Message("assistant", line));
                emitReply(player, line);
                speakLine(line);
                // Final words spoken — then transform on the next tick.
                level().getServer().execute(() -> startKangabrineHaunt(expectedPartner));
                return;
            }
        }

        history.add(new OpenRouterClient.Message("user", userMessage));
        history.add(new OpenRouterClient.Message("assistant", line));
        // Keep the last 6 turns (12 entries) to bound prompt size + per-call
        // token cost. Smaller history = cheaper, faster replies.
        while (history.size() > 12) history.remove(0);

        // Push the reply over the active channel — dialogue screen + floating
        // bubble for a right-click session, or a private whisper for @mention.
        emitReply(player, line);
        speakLine(line);
        Ironhold.LOGGER.info("[Kangarude] -> {}: \"{}\"", player.getName().getString(), line);
    }

    // ── Kangabrine haunt ─────────────────────────────────────────────────────

    /** Transform Kanga into Kangabrine and start haunting the named victim. */
    private void startKangabrineHaunt(UUID victimId) {
        endConversation();
        kangabrineTargetId = victimId;
        kangabrineStartTick = level().getGameTime();
        kangabrineLastPulseTick = kangabrineStartTick;
        // Defer the "joined" broadcast + transformation by 1 second so the
        // "left" message lands first and the timing reads as a re-login.
        kangabrineJoinAtTick = kangabrineStartTick + 20;

        if (!(level() instanceof ServerLevel sl)) return;

        // "Kangarude left the game" — broadcast to all players immediately.
        sl.getServer().getPlayerList().broadcastSystemMessage(
            Component.translatable("multiplayer.player.left",
                Component.literal("Kangarude")
            ).withStyle(net.minecraft.ChatFormatting.YELLOW),
            false);

        // Drop the Kangarude tab-list entry so the player list visibly empties,
        // matching the "left the game" chat message. Kangabrine's entry is
        // added 1s later in fireKangabrineJoin to mirror the join timing.
        if (listedProfileId != null) {
            KangarudePlayerListSync.broadcastRemove(sl.getServer(), listedProfileId);
            listedProfileId = null;
        }

        // Mark mode true so haunt-tick gating + invulnerability take effect immediately,
        // even before the deferred join visuals fire. Texture swap also kicks in here.
        setKangabrineMode(true);

        // Track this victim globally so the sleep-block handler can refuse them.
        ACTIVE_HAUNT_TARGETS.add(victimId);

        // Force a thunderstorm for the duration of the haunt.
        var weather = sl.getServer().getWeatherData();
        weather.setRaining(true);
        weather.setRainTime(KANGABRINE_DURATION_TICKS + 200);
        weather.setThundering(true);
        weather.setThunderTime(KANGABRINE_DURATION_TICKS + 200);

        // Fast-forward time toward midnight during descent — sky visibly accelerates
        // through dusk into night while the sonic-boom audio plays.
        kangabrineFastForwardingTime = true;

        // Pick a variant — every summon should feel different.
        kangabrineVariant = sl.getRandom().nextInt(4);
        kangabrineLastLaughTick = 0;
        kangabrineLastDarknessTick = 0;

        Ironhold.LOGGER.warn("[Kangarude] Transforming to Kangabrine — target: {} variant: {}",
            victimId, variantName(kangabrineVariant));
    }

    private static String variantName(int v) {
        return switch (v) {
            case VARIANT_BANSHEE  -> "BANSHEE";
            case VARIANT_LAUGHING -> "LAUGHING";
            case VARIANT_SHADOW   -> "SHADOW";
            case VARIANT_CHAOTIC  -> "CHAOTIC";
            default               -> "UNKNOWN";
        };
    }

    /** Fired ~1s after start: "joined" broadcast + teleport high, start descent. */
    private void fireKangabrineJoin() {
        kangabrineJoinAtTick = 0;
        if (!(level() instanceof ServerLevel sl)) return;

        sl.getServer().getPlayerList().broadcastSystemMessage(
            Component.translatable("multiplayer.player.joined",
                Component.literal("Kangabrine")
            ).withStyle(net.minecraft.ChatFormatting.YELLOW),
            false);

        // Add the Kangabrine tab-list entry so the player list mirrors the
        // "joined the game" message — fresh red face appears beside the chat line.
        UUID brineId = KangarudePlayerListSync.profileIdFor(getUUID(), KangarudePlayerListSync.Mode.KANGABRINE);
        KangarudePlayerListSync.broadcastAdd(sl.getServer(), brineId, KangarudePlayerListSync.Mode.KANGABRINE);
        listedProfileId = brineId;

        // Red bold name + beefier combat stats.
        this.setCustomName(Component.literal("§4§lKangabrine"));
        this.setCustomNameVisible(true);
        var hpAttr = this.getAttribute(Attributes.MAX_HEALTH);
        if (hpAttr != null) hpAttr.setBaseValue(80.0);
        this.setHealth(this.getMaxHealth());
        var spdAttr = this.getAttribute(Attributes.MOVEMENT_SPEED);
        if (spdAttr != null) spdAttr.setBaseValue(0.45);

        ServerPlayer victim = sl.getServer().getPlayerList().getPlayer(kangabrineTargetId);
        if (victim == null) {
            endKangabrineHaunt();
            return;
        }

        // Teleport high above + slightly in front of the victim — drop point.
        // "In front" = a few blocks past the victim's facing direction.
        float yawRad = victim.getYRot() * (float) (Math.PI / 180.0);
        double frontDist = 4.0;
        double dropX = victim.getX() - Math.sin(yawRad) * frontDist;
        double dropZ = victim.getZ() + Math.cos(yawRad) * frontDist;
        double dropY = victim.getY() + 24.0;

        this.teleportTo(dropX, dropY, dropZ);
        this.setDeltaMovement(Vec3.ZERO);
        this.getLookControl().setLookAt(victim, 90.0F, 90.0F);

        // Begin descent phase.
        kangabrinePhase = HauntPhase.DESCENT;
        kangabrinePhaseStartTick = level().getGameTime();

        // Dramatic sonic-boom transition cue at the victim's position — plays during the
        // 10-second descent so the BOOM lands mid-fall for the "evil superman just broke
        // the sound barrier" entrance feel. Loud + wide range so everyone within earshot hears.
        sl.playSound(null, victim.getX(), victim.getY(), victim.getZ(),
            kingdom.smp.ModSounds.KANGABRINE_DRAMATIC_DESCENT.get(),
            net.minecraft.sounds.SoundSource.HOSTILE, 4.0f, 1.0f);

        victim.sendSystemMessage(
            Component.literal("§4§l⚠ Look up.").withStyle(net.minecraft.ChatFormatting.DARK_RED));
    }

    /** Per-tick state machine: descent → intro speech → relentless hunt. */
    private void tickKangabrineHaunt() {
        if (!(level() instanceof ServerLevel sl)) return;

        long now = level().getGameTime();

        // Fire the deferred "joined the game" + descent kickoff.
        if (kangabrineJoinAtTick > 0 && now >= kangabrineJoinAtTick) {
            fireKangabrineJoin();
            return;
        }
        if (kangabrineJoinAtTick > 0) return;

        if (now - kangabrineStartTick >= KANGABRINE_DURATION_TICKS) {
            endKangabrineHaunt();
            return;
        }

        ServerPlayer victim = sl.getServer().getPlayerList().getPlayer(kangabrineTargetId);
        if (victim == null || victim.level() != sl) {
            endKangabrineHaunt();
            return;
        }

        switch (kangabrinePhase) {
            case DESCENT       -> tickDescentPhase(sl, victim, now);
            case INTRO_SPEECH  -> tickIntroSpeechPhase(sl, victim, now);
            case STALKING      -> tickStalkingPhase(sl, victim, now);
            case ENVIRONMENTAL -> tickEnvironmentalPhase(sl, victim, now);
            case ESCALATED     -> tickEscalatedPhase(sl, victim, now);
        }
    }

    /** Descent: float down very slowly toward eye level over ~10 seconds. */
    private void tickDescentPhase(ServerLevel sl, ServerPlayer victim, long now) {
        // Fast-forward time toward midnight while he's falling — the sky itself answers him.
        if (kangabrineFastForwardingTime) {
            advanceTimeTowardNight(sl);
        }

        // Slow descent — barely moves per tick to draw out the dread.
        Vec3 cur = this.getDeltaMovement();
        this.setDeltaMovement(cur.x * 0.5, -0.12, cur.z * 0.5);
        this.getLookControl().setLookAt(victim, 90.0F, 90.0F);

        // ~3 seconds into the fall, breathe a single short menacing line through TTS.
        // The sonic-boom audio is settling out by then; the murmur lands in the lull.
        if (kangabrineDescentMurmurTick == 0 && (now - kangabrinePhaseStartTick) >= 60) {
            String line = KANGABRINE_DESCENT_MURMURS[
                sl.getRandom().nextInt(KANGABRINE_DESCENT_MURMURS.length)];
            // Layer wither + dragon at low pitch under the TTS so Kanga's voice
            // reads as pitched-down and demonic without modifying the audio itself.
            playDemonicVoiceLayer(sl);
            speakLine(line);
            broadcastDialogue(line);
            kangabrineDescentMurmurTick = now;
        }

        // Faint smoke trail only — no soul-fire spam.
        if (now % 8 == 0) {
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                this.getX(), this.getY(), this.getZ(), 3, 0.2, 0.2, 0.2, 0.01);
        }

        // Stop at eye level — Kangabrine never touches the ground, just floats.
        double hoverY = victim.getY() + 1.6;
        if (this.getY() <= hoverY) {
            this.setDeltaMovement(0, 0, 0);
            this.teleportTo(this.getX(), hoverY, this.getZ());
            kangabrinePhase = HauntPhase.INTRO_SPEECH;
            kangabrinePhaseStartTick = now;

            // Single quiet hover-impact cue — no lightning, no thunder.
            sl.playSound(null, this.blockPosition(),
                net.minecraft.sounds.SoundEvents.PORTAL_AMBIENT,
                net.minecraft.sounds.SoundSource.HOSTILE, 0.8f, 0.5f);
        }
    }

    /** Intro speech: speak the line, hold the stare for 4s after, then vanish into STALKING. */
    private void tickIntroSpeechPhase(ServerLevel sl, ServerPlayer victim, long now) {
        long elapsed = now - kangabrinePhaseStartTick;
        if (elapsed == 0) {
            String line = KANGABRINE_INTROS[sl.getRandom().nextInt(KANGABRINE_INTROS.length)];
            broadcastDialogue(line);
            // Demonic voice layer under the spoken intro line.
            playDemonicVoiceLayer(sl);
            speakLine(line);
            MicGate.muteFor(victim.getUUID(), 6_000L);
            this.getLookControl().setLookAt(victim, 90.0F, 90.0F);
            this.setDeltaMovement(Vec3.ZERO);
            // Plunge the victim into darkness as Kanga speaks. Pure dread setup.
            victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
                net.minecraft.world.effect.MobEffects.DARKNESS, 200, 0, false, false));
        }
        // Hold the stare 5s line + 4s post-line silence = ~180 ticks.
        if (elapsed >= 180) {
            // Vanish silently — teleport very far away. STALKING phase will pull us back for sightings.
            teleportFarAwayFromVictim(sl, victim, 100, 150);
            kangabrinePhase = HauntPhase.STALKING;
            kangabrinePhaseStartTick = now;
            kangabrineSightingsDone = 0;
            kangabrineSightingActive = false;

            // Kick off the eerie-ambient loop — re-triggered every 30s in STALKING + ENVIRONMENTAL.
            playEerieAmbient(sl, victim);
            kangabrineLastAmbientTick = now;
        }
    }

    /** Send the 30-second eerie ambient track to the victim's client only. */
    private void playEerieAmbient(ServerLevel sl, ServerPlayer victim) {
        var soundHolder = net.minecraft.core.Holder.direct(
            kingdom.smp.ModSounds.KANGABRINE_EERIE_AMBIENT.get());
        victim.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            soundHolder, net.minecraft.sounds.SoundSource.HOSTILE,
            victim.getX(), victim.getY(), victim.getZ(),
            0.8f, 1.0f, sl.getRandom().nextLong()));
    }

    /** Re-trigger the 30s eerie ambient once it's about to end so it loops seamlessly. */
    private void tickEerieAmbientLoop(ServerLevel sl, ServerPlayer victim, long now) {
        // Track is ~30s = 600 ticks. Re-trigger every 590 ticks so there's no gap.
        if (now - kangabrineLastAmbientTick >= 590) {
            playEerieAmbient(sl, victim);
            kangabrineLastAmbientTick = now;
        }
    }

    /** Loud packet-direct banshee scream straight to the victim's client. */
    private void playBansheeScream(ServerLevel sl, ServerPlayer victim, float volume) {
        var holder = net.minecraft.core.Holder.direct(
            kingdom.smp.ModSounds.KANGABRINE_BANSHEE_SCREAM.get());
        victim.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            holder, net.minecraft.sounds.SoundSource.HOSTILE,
            victim.getX(), victim.getY(), victim.getZ(),
            volume, 1.0f, sl.getRandom().nextLong()));
    }

    /** Distant evil-laugh sting played positionally so it has direction. */
    private void playEvilLaugh(ServerLevel sl, ServerPlayer victim, double offsetDist, float volume) {
        // Place laugh source in a random direction around the victim — gives it spatial dread.
        double angle = sl.getRandom().nextDouble() * Math.PI * 2;
        double lx = victim.getX() + Math.cos(angle) * offsetDist;
        double lz = victim.getZ() + Math.sin(angle) * offsetDist;
        sl.playSound(null, lx, victim.getY() + 1.0, lz,
            kingdom.smp.ModSounds.KANGABRINE_EVIL_LAUGH.get(),
            net.minecraft.sounds.SoundSource.HOSTILE, volume, 1.0f);
    }

    /**
     * Layered "demonic voice" effect — wither + dragon at low pitch under the spoken TTS.
     * Use this around speakLine() calls during descent/intro to give Kanga's normal
     * voice a pitched-down, otherworldly weight without modifying the TTS itself.
     */
    private void playDemonicVoiceLayer(ServerLevel sl) {
        sl.playSound(null, blockPosition(),
            net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
            net.minecraft.sounds.SoundSource.HOSTILE, 1.2f, 0.4f);
        sl.playSound(null, blockPosition(),
            net.minecraft.sounds.SoundEvents.WITHER_AMBIENT,
            net.minecraft.sounds.SoundSource.HOSTILE, 0.7f, 0.5f);
    }

    /**
     * Apply MobEffects.DARKNESS to the victim, scaled to the current variant.
     * SHADOW pulls them into pitch black often; CHAOTIC pulses occasionally;
     * BANSHEE/LAUGHING leave them mostly visible. Throttled by minIntervalTicks
     * so it can be called from per-tick handlers without spamming.
     */
    private void applyDarknessPulse(ServerPlayer victim, long now,
                                    int minIntervalTicks, int durationTicks) {
        if (now - kangabrineLastDarknessTick < minIntervalTicks) return;
        kangabrineLastDarknessTick = now;
        victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.DARKNESS,
            durationTicks, 0, false, false));
    }

    /**
     * Variant-driven evil-laugh ambient sting. LAUGHING fires often, CHAOTIC sometimes,
     * others basically never. Distance + volume picked at random for unpredictability.
     */
    private void tickEvilLaughAmbient(ServerLevel sl, ServerPlayer victim, long now) {
        int minGap;
        float chance;
        switch (kangabrineVariant) {
            case VARIANT_LAUGHING -> { minGap = 300; chance = 0.5f; }   // ~every 15-30s
            case VARIANT_CHAOTIC  -> { minGap = 500; chance = 0.35f; }
            default               -> { return; }
        }
        if (now - kangabrineLastLaughTick < minGap) return;
        if (sl.getRandom().nextFloat() > chance) {
            kangabrineLastLaughTick = now - (minGap - 60);  // re-roll in ~3s
            return;
        }
        kangabrineLastLaughTick = now;
        double dist = 12 + sl.getRandom().nextDouble() * 18;
        float vol = 0.7f + sl.getRandom().nextFloat() * 0.5f;
        playEvilLaugh(sl, victim, dist, vol);
    }

    /**
     * Variant-driven darkness pulses outside of strikes. SHADOW pulses every ~6s
     * for 5s at a time (overlapping = continuous gloom); CHAOTIC every ~15s briefly.
     */
    private void tickDarknessAmbient(ServerPlayer victim, long now) {
        switch (kangabrineVariant) {
            case VARIANT_SHADOW  -> applyDarknessPulse(victim, now, 120, 140);
            case VARIANT_CHAOTIC -> applyDarknessPulse(victim, now, 300, 80);
            default -> { /* BANSHEE / LAUGHING: leave them visible */ }
        }
    }

    /** Teleport very far away — used between phases to "disappear" entirely. */
    private void teleportFarAwayFromVictim(ServerLevel sl, ServerPlayer victim, int minDist, int maxDist) {
        double angle = sl.getRandom().nextDouble() * Math.PI * 2;
        double dist = minDist + sl.getRandom().nextDouble() * (maxDist - minDist);
        double tx = victim.getX() + Math.cos(angle) * dist;
        double tz = victim.getZ() + Math.sin(angle) * dist;
        teleportTo(tx, victim.getY() + 200, tz);  // way up, out of sight
        setDeltaMovement(Vec3.ZERO);
    }

    // ── STALKING phase: long silences, distant sightings, eye-contact vanish ──

    /** STALKING: ~75 seconds of mostly nothing punctuated by 3 distant sightings (rule of three). */
    private void tickStalkingPhase(ServerLevel sl, ServerPlayer victim, long now) {
        long elapsed = now - kangabrinePhaseStartTick;

        // Slow heartbeat + occasional title flash drive the dread between sightings.
        tickHeartbeatSlow(sl, victim, now);
        tickRandomTitleSlow(sl, victim, now);
        tickEerieAmbientLoop(sl, victim, now);
        tickEvilLaughAmbient(sl, victim, now);
        tickDarknessAmbient(victim, now);

        if (kangabrineSightingActive) {
            tickActiveSighting(sl, victim, now);
        } else {
            // Should we trigger the next sighting?
            if (kangabrineSightingsDone < STALKING_SIGHTING_TICKS.length
                    && elapsed >= STALKING_SIGHTING_TICKS[kangabrineSightingsDone]) {
                triggerSighting(sl, victim, now);
            }
        }

        if (elapsed >= (PHASE_STALKING_END_TICK - PHASE_INTRO_END_TICK)) {
            // Move to ENVIRONMENTAL: vanish silently again.
            teleportFarAwayFromVictim(sl, victim, 100, 150);
            kangabrineSightingActive = false;
            kangabrinePhase = HauntPhase.ENVIRONMENTAL;
            kangabrinePhaseStartTick = now;
            kangabrineEnvFlags = 0;
        }
    }

    /** Place him 40–80 blocks from victim at a visible elevation and lock his stare. */
    private void triggerSighting(ServerLevel sl, ServerPlayer victim, long now) {
        // Pick a spot ~40-80 blocks away in the direction the victim is facing (so they might see it).
        float yawRad = victim.getYRot() * (float) (Math.PI / 180.0);
        // Add a wide random spread so it's not always dead-ahead.
        double spread = (sl.getRandom().nextDouble() - 0.5) * Math.PI;
        double angle = -yawRad + Math.PI / 2 + spread;  // facing direction + spread
        double dist = 40 + sl.getRandom().nextDouble() * 40;
        double tx = victim.getX() + Math.cos(angle) * dist;
        double tz = victim.getZ() + Math.sin(angle) * dist;
        // Hover slightly above ground for visibility.
        BlockPos col = sl.getHeightmapPos(
            net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
            new BlockPos((int) tx, 0, (int) tz));
        double ty = col.getY() + 2.0;

        teleportTo(tx, ty, tz);
        setDeltaMovement(Vec3.ZERO);
        getLookControl().setLookAt(victim, 90.0F, 90.0F);
        kangabrineSightingActive = true;
        kangabrineSightingStartTick = now;
        kangabrineEyeContactTicks = 0;
    }

    /** Per-tick check while sighting is active: vanish on eye contact or after 30s. */
    private void tickActiveSighting(ServerLevel sl, ServerPlayer victim, long now) {
        getLookControl().setLookAt(victim, 90.0F, 90.0F);

        long sightingElapsed = now - kangabrineSightingStartTick;
        if (sightingElapsed >= SIGHTING_AUTO_VANISH_TICKS) {
            endSighting(sl, victim);
            return;
        }

        if (victimIsLookingAtMe(victim)) {
            kangabrineEyeContactTicks++;
            if (kangabrineEyeContactTicks >= EYE_CONTACT_VANISH_TICKS) {
                endSighting(sl, victim);
            }
        } else {
            kangabrineEyeContactTicks = 0;
        }
    }

    private void endSighting(ServerLevel sl, ServerPlayer victim) {
        // The instant the victim's eyes lock — slam darkness on them so the world
        // collapses inward as he vanishes. He was just gone... and now you can't see.
        victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.DARKNESS, 160, 0, false, false));
        teleportFarAwayFromVictim(sl, victim, 80, 130);
        kangabrineSightingActive = false;
        kangabrineSightingsDone++;
    }

    /**
     * Returns true if Kangabrine is within ~30° of the victim's view axis AND there's
     * unobstructed line of sight (so dirt/walls don't trigger the vanish).
     */
    private boolean victimIsLookingAtMe(ServerPlayer victim) {
        Vec3 myCenter = this.position().add(0, this.getBbHeight() / 2.0, 0);
        Vec3 victimEye = victim.getEyePosition(1.0F);
        Vec3 toMe = myCenter.subtract(victimEye);
        double distSq = toMe.lengthSqr();
        if (distSq < 1.0 || distSq > 200 * 200) return false;
        Vec3 toMeNorm = toMe.normalize();
        Vec3 victimLook = victim.getViewVector(1.0F).normalize();
        double dot = toMeNorm.dot(victimLook);
        if (dot < 0.85) return false;  // outside ~32° cone
        // Cheap LOS check via clip().
        var hit = victim.level().clip(new net.minecraft.world.level.ClipContext(
            victimEye, myCenter,
            net.minecraft.world.level.ClipContext.Block.VISUAL,
            net.minecraft.world.level.ClipContext.Fluid.NONE,
            victim));
        return hit.getType() == net.minecraft.world.phys.HitResult.Type.MISS;
    }

    // ── ENVIRONMENTAL phase: signatures + bait-and-switch ──────────────────

    /** ENVIRONMENTAL: Herobrine signatures + one bait-and-switch over 60s. */
    private void tickEnvironmentalPhase(ServerLevel sl, ServerPlayer victim, long now) {
        tickHeartbeatSlow(sl, victim, now);
        tickRandomTitleSlow(sl, victim, now);
        tickEerieAmbientLoop(sl, victim, now);
        tickEvilLaughAmbient(sl, victim, now);
        tickDarknessAmbient(victim, now);

        long elapsed = now - kangabrinePhaseStartTick;

        // ~5s in: place sand pyramid.
        if ((kangabrineEnvFlags & 1) == 0 && elapsed >= 100) {
            placeSandPyramid(sl, victim);
            kangabrineEnvFlags |= 1;
        }
        // ~25s in: drop redstone-torch signatures.
        if ((kangabrineEnvFlags & 2) == 0 && elapsed >= 500) {
            placeSignatureTorches(sl, victim);
            kangabrineEnvFlags |= 2;
        }
        // ~40s in: bait-and-switch — approaching footsteps over ~12s, then nothing.
        if ((kangabrineEnvFlags & 4) == 0 && elapsed >= 800) {
            kangabrineBaitStartTick = now;
            kangabrineBaitIsReal = false;
            kangabrineEnvFlags |= 4;
        }
        if (kangabrineBaitStartTick > 0) {
            tickBaitAndSwitch(sl, victim, now);
        }

        if (elapsed >= (PHASE_ENVIRON_END_TICK - PHASE_STALKING_END_TICK)) {
            kangabrinePhase = HauntPhase.ESCALATED;
            kangabrinePhaseStartTick = now;
            kangabrineStrikesDone = 0;
            kangabrineNextStrikeTick = now + 40;  // first strike ~2s after entering escalated
            // Boost stats for the final payoff.
            var hpAttr = this.getAttribute(Attributes.MAX_HEALTH);
            if (hpAttr != null) hpAttr.setBaseValue(80.0);
            this.setHealth(this.getMaxHealth());
        }
    }

    /** Place a small classic Herobrine 5-3-1 sand pyramid 20-50 blocks from the victim. */
    private void placeSandPyramid(ServerLevel sl, ServerPlayer victim) {
        for (int attempt = 0; attempt < 6; attempt++) {
            double angle = sl.getRandom().nextDouble() * Math.PI * 2;
            double dist = 20 + sl.getRandom().nextDouble() * 30;
            int cx = (int) (victim.getX() + Math.cos(angle) * dist);
            int cz = (int) (victim.getZ() + Math.sin(angle) * dist);
            BlockPos top = sl.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                new BlockPos(cx, 0, cz));
            int y = top.getY();
            // Skip if the spot would clip the victim.
            if (Math.abs(y - victim.getY()) > 8) continue;

            var sand = net.minecraft.world.level.block.Blocks.SAND.defaultBlockState();
            // 5x5 base
            for (int dx = -2; dx <= 2; dx++) for (int dz = -2; dz <= 2; dz++) {
                sl.setBlock(new BlockPos(cx + dx, y, cz + dz), sand, 3);
            }
            // 3x3 middle
            for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) {
                sl.setBlock(new BlockPos(cx + dx, y + 1, cz + dz), sand, 3);
            }
            // 1x1 top
            sl.setBlock(new BlockPos(cx, y + 2, cz), sand, 3);
            return;
        }
    }

    /** Drop 2-4 redstone torches in odd spots around the victim. */
    private void placeSignatureTorches(ServerLevel sl, ServerPlayer victim) {
        var torch = net.minecraft.world.level.block.Blocks.REDSTONE_TORCH.defaultBlockState();
        int placed = 0;
        for (int attempt = 0; attempt < 24 && placed < 3; attempt++) {
            double angle = sl.getRandom().nextDouble() * Math.PI * 2;
            double dist = 6 + sl.getRandom().nextDouble() * 14;
            int x = (int) (victim.getX() + Math.cos(angle) * dist);
            int z = (int) (victim.getZ() + Math.sin(angle) * dist);
            BlockPos top = sl.getHeightmapPos(
                net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                new BlockPos(x, 0, z));
            BlockPos torchPos = top;  // place AT the surface (replaces grass/snow)
            BlockPos below = torchPos.below();
            if (sl.getBlockState(below).isFaceSturdy(sl, below, net.minecraft.core.Direction.UP)
                && sl.getBlockState(torchPos).isAir()) {
                sl.setBlock(torchPos, torch, 3);
                placed++;
            }
        }
    }

    /** Approaching cave-ambient sound over ~12s. False alarm (mostly) — usually nothing follows. */
    private void tickBaitAndSwitch(ServerLevel sl, ServerPlayer victim, long now) {
        long elapsed = now - kangabrineBaitStartTick;
        // Play a faint approaching cave-ambient pulse every 1.5s.
        if (elapsed % 30 == 0 && elapsed <= 240) {
            float vol = 0.2f + (elapsed / 240.0f) * 0.8f;
            // Position: 8 blocks behind victim.
            float yawRad = victim.getYRot() * (float) (Math.PI / 180.0);
            double bx = victim.getX() + Math.sin(yawRad) * 8;
            double bz = victim.getZ() - Math.cos(yawRad) * 8;
            sl.playSound(null, bx, victim.getY(), bz,
                net.minecraft.sounds.SoundEvents.AMBIENT_CAVE.value(),
                net.minecraft.sounds.SoundSource.HOSTILE, vol, 0.6f);
        }
        if (elapsed >= 280) {
            // Done. Most of the time NOTHING happens — pure dread bait.
            kangabrineBaitStartTick = 0;
        }
    }

    // ── ESCALATED phase: 3 restrained strikes with long gaps ────────────────

    /** ESCALATED: only 3 strikes total, 10s apart. Fear is the weapon. */
    private void tickEscalatedPhase(ServerLevel sl, ServerPlayer victim, long now) {
        tickHeartbeatFast(sl, victim, now);
        tickRandomTitleSlow(sl, victim, now);

        if (kangabrineStrikesDone >= 3) return;
        if (now < kangabrineNextStrikeTick) return;

        // Teleport adjacent and strike — full theatrical sequence.
        double angle = sl.getRandom().nextDouble() * Math.PI * 2;
        double dist = 1.5 + sl.getRandom().nextDouble() * 1.5;
        double tx = victim.getX() + Math.cos(angle) * dist;
        double tz = victim.getZ() + Math.sin(angle) * dist;
        teleportTo(tx, victim.getY() + 1.6, tz);  // stays floating
        setDeltaMovement(Vec3.ZERO);
        getLookControl().setLookAt(victim, 90.0F, 90.0F);

        spawnCosmeticLightning(sl, blockPosition());

        // Lightning crack always sells the strike location.
        sl.playSound(null, blockPosition(),
            net.minecraft.sounds.SoundEvents.LIGHTNING_BOLT_IMPACT,
            net.minecraft.sounds.SoundSource.HOSTILE, 2.0f, 0.6f);

        // Variant-specific stinger — every summon's payoff sounds different.
        // CHAOTIC re-rolls per strike so even within one summon it's unpredictable.
        int strikeFlavor = (kangabrineVariant == VARIANT_CHAOTIC)
            ? sl.getRandom().nextInt(3) : kangabrineVariant;
        switch (strikeFlavor) {
            case VARIANT_BANSHEE -> {
                // Piercing scream + bell — pure horror.
                playBansheeScream(sl, victim, 1.6f);
                sl.playSound(null, blockPosition(),
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(),
                    net.minecraft.sounds.SoundSource.HOSTILE, 2.0f, 0.4f);
            }
            case VARIANT_LAUGHING -> {
                // Warden roar + cackle layered — he's enjoying this.
                sl.playSound(null, blockPosition(),
                    net.minecraft.sounds.SoundEvents.WARDEN_ROAR,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.6f, 0.45f);
                playEvilLaugh(sl, victim, 0.0, 1.4f);
            }
            case VARIANT_SHADOW -> {
                // Wither + ender — heavy, low, atmospheric. No bright stinger.
                sl.playSound(null, blockPosition(),
                    net.minecraft.sounds.SoundEvents.WITHER_SPAWN,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.4f, 0.4f);
                sl.playSound(null, blockPosition(),
                    net.minecraft.sounds.SoundEvents.ENDER_DRAGON_GROWL,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.6f, 0.45f);
            }
            default -> {
                // Fallback to original warden+bell.
                sl.playSound(null, blockPosition(),
                    net.minecraft.sounds.SoundEvents.WARDEN_ROAR,
                    net.minecraft.sounds.SoundSource.HOSTILE, 1.8f, 0.45f);
                sl.playSound(null, blockPosition(),
                    net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BELL.value(),
                    net.minecraft.sounds.SoundSource.HOSTILE, 2.0f, 0.4f);
            }
        }

        victim.hurtServer(sl, sl.damageSources().magic(), KANGABRINE_STRIKE_DAMAGE);
        // BLINDNESS for the immediate hit-flash + DARKNESS for the lingering pall.
        // SHADOW variant gets a much longer DARKNESS so it stays oppressive between strikes.
        int darknessTicks = (kangabrineVariant == VARIANT_SHADOW) ? 300 : 200;
        victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.BLINDNESS, 80, 0, false, true));
        victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.DARKNESS, darknessTicks, 0, false, false));
        victim.addEffect(new net.minecraft.world.effect.MobEffectInstance(
            net.minecraft.world.effect.MobEffects.WEAKNESS, 120, 0, false, true));

        // Final strike: real damaging lightning + lingers a beat before vanishing.
        if (kangabrineStrikesDone == 2) {
            net.minecraft.world.entity.LightningBolt deadly =
                net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(
                    sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
            if (deadly != null) {
                deadly.snapTo(victim.position());
                sl.addFreshEntity(deadly);
            }
        }

        kangabrineStrikesDone++;
        kangabrineNextStrikeTick = now + ESCALATED_STRIKE_GAP_TICKS;

        // After each strike, vanish far away briefly so the player can't track him.
        // We don't actually move him — he just stands there until next strike. Adds presence.
    }

    // ── Helpers: heartbeat + titles ─────────────────────────────────────────

    /** Slow, restrained heartbeat — only fires when very close. */
    private void tickHeartbeatSlow(ServerLevel sl, ServerPlayer victim, long now) {
        double d = victim.distanceTo(this);
        int interval;
        if (d > 25) return;
        else if (d > 15) interval = 60;   // 3s
        else if (d > 8)  interval = 30;   // 1.5s
        else             interval = 15;   // 0.75s

        if (now - kangabrineLastHeartbeatTick < interval) return;
        kangabrineLastHeartbeatTick = now;
        sendHeartbeat(sl, victim);
    }

    /** Faster heartbeat for the escalated phase. */
    private void tickHeartbeatFast(ServerLevel sl, ServerPlayer victim, long now) {
        double d = victim.distanceTo(this);
        int interval;
        if (d > 30) return;
        else if (d > 15) interval = 25;
        else if (d > 5)  interval = 12;
        else             interval = 6;

        if (now - kangabrineLastHeartbeatTick < interval) return;
        kangabrineLastHeartbeatTick = now;
        sendHeartbeat(sl, victim);
    }

    private void sendHeartbeat(ServerLevel sl, ServerPlayer victim) {
        var soundHolder = net.minecraft.core.Holder.direct(
            net.minecraft.sounds.SoundEvents.NOTE_BLOCK_BASEDRUM.value());
        victim.connection.send(new net.minecraft.network.protocol.game.ClientboundSoundPacket(
            soundHolder, net.minecraft.sounds.SoundSource.HOSTILE,
            victim.getX(), victim.getY(), victim.getZ(),
            0.5f, 0.6f, sl.getRandom().nextLong()));
    }

    /** Slow title flashes — every 20–40s instead of every 7–13s. */
    private void tickRandomTitleSlow(ServerLevel sl, ServerPlayer victim, long now) {
        if (now - kangabrineLastTitleTick < (400 + sl.getRandom().nextInt(400))) return;
        kangabrineLastTitleTick = now;

        Component title;
        // 33% chance to flash the victim's own name in red — apex of dread.
        if (sl.getRandom().nextInt(3) == 0) {
            title = Component.literal("§4§l" + victim.getName().getString());
        } else {
            title = Component.literal(KANGABRINE_TITLES[sl.getRandom().nextInt(KANGABRINE_TITLES.length)]);
        }
        // Slower fade-in (15 ticks) per Grok's recommendation — feels delayed and intentional.
        victim.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket(15, 40, 15));
        victim.connection.send(new net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket(title));
    }

    private void endKangabrineHaunt() {
        if (level() instanceof ServerLevel sl) {
            sl.playSound(null, blockPosition(),
                net.minecraft.sounds.SoundEvents.WITHER_DEATH,
                net.minecraft.sounds.SoundSource.HOSTILE, 0.6f, 0.5f);
            // Broadcast a parting "Kangabrine left the game" message.
            sl.getServer().getPlayerList().broadcastSystemMessage(
                Component.translatable("multiplayer.player.left",
                    Component.literal("Kangabrine")
                ).withStyle(net.minecraft.ChatFormatting.YELLOW),
                false);
            // Reset mood for the haunted player so a new Kanga starts neutral.
            if (kangabrineTargetId != null) {
                KangarudeMoodTracker.reset(kangabrineTargetId);
                ACTIVE_HAUNT_TARGETS.remove(kangabrineTargetId);
            }
        }
        kangabrineTargetId = null;
        Ironhold.LOGGER.info("[Kangarude] Kangabrine haunt ended; entity despawning.");
        this.discard();  // remove from world for good
    }

    /**
     * Fast-forward the level's day clock toward midnight while Kangabrine descends.
     * Uses the 1.26 WorldClock API: ServerClockManager.addTicks per game tick until we're
     * within range of the target, then snaps to the target marker.
     */
    private void advanceTimeTowardNight(ServerLevel sl) {
        var dimType = sl.dimensionTypeRegistration().value();
        var defaultClockOpt = dimType.defaultClock();
        if (defaultClockOpt.isEmpty()) {
            kangabrineFastForwardingTime = false;
            return;
        }
        var clock = defaultClockOpt.get();
        var clockManager = sl.getServer().clockManager();
        long total = clockManager.getTotalTicks(clock);
        long currentDay = ((total % 24000L) + 24000L) % 24000L;
        long delta = ((KANGABRINE_TARGET_DAYTIME - currentDay) + 24000L) % 24000L;
        if (delta == 0 || delta <= KANGABRINE_TIME_ADVANCE_PER_TICK) {
            // Snap to midnight and stop accelerating.
            clockManager.moveToTimeMarker(clock,
                net.minecraft.world.clock.ClockTimeMarkers.MIDNIGHT);
            kangabrineFastForwardingTime = false;
            return;
        }
        clockManager.addTicks(clock, KANGABRINE_TIME_ADVANCE_PER_TICK);
    }

    /** Spawns a cosmetic (no-damage) lightning bolt at the given block. */
    private static void spawnCosmeticLightning(ServerLevel sl, BlockPos pos) {
        net.minecraft.world.entity.LightningBolt bolt = net.minecraft.world.entity.EntityType.LIGHTNING_BOLT.create(
            sl, net.minecraft.world.entity.EntitySpawnReason.TRIGGERED);
        if (bolt == null) return;
        bolt.snapTo(Vec3.atBottomCenterOf(pos));
        bolt.setVisualOnly(true);  // no fire, no damage
        sl.addFreshEntity(bolt);
    }

    /**
     * Strip stage directions and similar non-spoken markup from an LLM reply
     * before it goes to TTS. Small models love to emit *shrugs*, (pauses),
     * and wrap everything in quotes — none of which should be voiced.
     */
    private static String sanitizeForSpeech(String s) {
        if (s == null) return "";
        // Asterisk-bracketed italics: *shrugs*, *long pause*
        s = s.replaceAll("\\*[^*]{0,200}\\*", " ");
        // Parenthetical asides: (laughs softly), (small shrug)
        s = s.replaceAll("\\([^)]{0,200}\\)", " ");
        // Square-bracket cues: [smiles], [whisper]
        s = s.replaceAll("\\[[^\\]]{0,200}\\]", " ");
        // Underscore-italic markup
        s = s.replaceAll("_[^_]{0,200}_", " ");
        // Collapse whitespace and trim
        s = s.replaceAll("\\s+", " ").trim();
        // Strip surrounding straight or curly quotes that some models wrap replies in.
        while (s.length() >= 2) {
            char first = s.charAt(0), last = s.charAt(s.length() - 1);
            if ((first == '"' && last == '"')
                || (first == '“' && last == '”')
                || (first == '\'' && last == '\'')) {
                s = s.substring(1, s.length() - 1).trim();
            } else {
                break;
            }
        }
        return s;
    }

    private void broadcastDialogue(String line) {
        if (!(level() instanceof ServerLevel)) return;
        VillagerDialoguePayload payload = new VillagerDialoguePayload(
            getId(), SKIN_OWNER_NAME, "wanderer", line);
        PacketDistributor.sendToPlayersTrackingEntity(this, payload);
    }

    /**
     * Deliver a reply to the partner over the active channel: the dialogue
     * screen + floating bubble for a right-click session, or a private chat
     * whisper for an {@code @mention} session.
     */
    private void emitReply(ServerPlayer player, String line) {
        if (replyChannel == ReplyChannel.CHAT) {
            kingdom.smp.chat.MentionRouter.sendNpcWhisper(player, SKIN_OWNER_NAME, line);
        } else {
            PacketDistributor.sendToPlayer(player,
                new UpdateWardenScreenPayload(getId(),
                    UpdateWardenScreenPayload.STATUS_REPLY, line));
            broadcastDialogue(line);
        }
    }

    private void speakLine(String line) {
        if (!ElevenLabsClient.isConfigured()) return;
        UUID partner = partnerId; // snapshot in case it changes during the async call
        // Skip TTS entirely if the partner has muted Kanga. Text/chat still flows.
        if (partner != null && level() instanceof ServerLevel sl
            && NpcMuteRegistry.get(sl).isMuted(partner, tag())) {
            return;
        }
        // ElevenLabs reads "Kangarude" as "kanga-ruh-day". Force the hyphenated
        // spelling so it pronounces as "kanga-rude" (rhyming with "dude") regardless
        // of how the LLM wrote it. Word-boundary regex avoids touching the version
        // already hyphenated. Case-insensitive, preserves the original casing.
        line = line.replaceAll("(?i)\\bKangarude\\b", "Kanga-rude");
        // Streamed: playback starts on the first ElevenLabs chunk. NpcSpeech
        // keeps the partner's mic gated for the audio duration + 2s echo grace
        // (same window the old buffered path used). Null voice/model fall back
        // to the global Kangarude voice config.
        NpcSpeech.speak(this, partner, line, null, null, ElevenLabsClient.VoiceSettings.DEFAULT);
    }

    // ── Static lookup for chat-event routing ─────────────────────────────────

    public static @Nullable KangarudeEntity activePartnerOf(UUID playerId) {
        return ACTIVE_PARTNERS.get(playerId);
    }

    @Override
    public void remove(RemovalReason reason) {
        endConversation();
        kingdom.smp.chat.NpcMentionRegistry.unregister(this);
        // Drop our tab-list entry so the player list reflects the despawn.
        // Skip when the entity is just being unloaded (chunk unload, dimension
        // change) — we'll re-announce on the next aiStep when it ticks again.
        // Only KILLED and DISCARDED mean "actually gone."
        if (listedProfileId != null && (reason == RemovalReason.KILLED || reason == RemovalReason.DISCARDED)) {
            var server = level().getServer();
            if (server != null) KangarudePlayerListSync.broadcastRemove(server, listedProfileId);
            listedProfileId = null;
        }
        super.remove(reason);
    }

    // ── Wander goal ──────────────────────────────────────────────────────────

    private static final class HomeWanderGoal extends Goal {
        private final KangarudeEntity mob;
        private @Nullable Vec3 target;

        HomeWanderGoal(KangarudeEntity mob) {
            this.mob = mob;
            this.setFlags(EnumSet.of(Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            // Don't wander away mid-conversation.
            if (mob.partnerId != null) return false;
            if (mob.getNavigation().isInProgress()) return false;
            if (mob.getRandom().nextInt(120) != 0) return false;

            BlockPos home = mob.getHomePos();
            Vec3 candidate;
            double distSq = mob.distanceToSqr(Vec3.atCenterOf(home));
            if (distSq > WANDER_RADIUS * WANDER_RADIUS) {
                // Too far out — head back toward home.
                candidate = LandRandomPos.getPosTowards(mob, 10, 5, Vec3.atCenterOf(home));
            } else {
                candidate = LandRandomPos.getPos(mob, 10, 5);
                if (candidate != null
                        && Vec3.atCenterOf(home).distanceToSqr(candidate) > WANDER_RADIUS * WANDER_RADIUS) {
                    candidate = LandRandomPos.getPosTowards(mob, 10, 5, Vec3.atCenterOf(home));
                }
            }
            if (candidate == null) return false;
            target = candidate;
            return true;
        }

        @Override
        public void start() {
            if (target != null) mob.getNavigation().moveTo(target.x, target.y, target.z, 1.0);
        }

        @Override
        public boolean canContinueToUse() {
            return mob.partnerId == null && mob.getNavigation().isInProgress();
        }

        @Override
        public void stop() {
            mob.getNavigation().stop();
            target = null;
        }
    }
}
