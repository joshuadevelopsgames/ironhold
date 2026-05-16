package kingdom.smp.entity;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.ai.OllamaClient;
import kingdom.smp.npc.HasNpcManifest;
import kingdom.smp.npc.NpcItemReaction;
import net.minecraft.resources.Identifier;
import org.jspecify.annotations.Nullable;
import kingdom.smp.entity.goal.VillagerFleeFromCombatGoal;
import kingdom.smp.entity.goal.VillagerFollowPlayerGoal;
import kingdom.smp.entity.goal.VillagerWanderGoal;
import kingdom.smp.net.VillagerDialoguePayload;
import kingdom.smp.net.VillagerEmotePayload;
import kingdom.smp.rpg.PlayerClass;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import kingdom.smp.net.OpenVillagerScreenPayload;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Kingdom Villager — an NPC with deep personality, profession-specific mechanics,
 * and (for talker professions) OpenRouter-powered AI dialogue.
 */
public class KingdomVillagerEntity extends PathfinderMob implements HasNpcManifest {

    // ── Emote types for thought bubbles ──────────────────────────────────────
    public enum EmoteType {
        HEART,        // happy / friendly
        ANGER,        // grumpy / annoyed
        SWEAT,        // nervous / scared
        MUSIC,        // content / working / bard singing
        EXCLAMATION,  // surprised
        ZZZ,          // sleepy / bored at night
        SPARKLE,      // excited / rare event
        QUESTION      // confused / eccentric staring
    }

    // ── Personality & profession ─────────────────────────────────────────────
    private VillagerProfession profession = VillagerProfession.FARMER;
    private final VillagerPersonality personality = new VillagerPersonality();
    private boolean initialized = false;

    // ── Home position ────────────────────────────────────────────────────────
    private BlockPos homePos;

    // ── Cooldowns ────────────────────────────────────────────────────────────
    private long lastInteractTick = 0;
    private final Map<UUID, Long> perPlayerCooldown = new HashMap<>();
    private long lastEmoteTick = 0;

    // ── Guard follow state ───────────────────────────────────────────────────
    private Player followTarget;
    private int followTicksRemaining;


    public KingdomVillagerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        this.setPersistenceRequired();
    }

    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.5)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }

    // ── AI goals ─────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new VillagerFleeFromCombatGoal(this));
        this.goalSelector.addGoal(2, new VillagerFollowPlayerGoal(this));
        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.0, true));
        this.goalSelector.addGoal(5, new VillagerWanderGoal(this, 32.0));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        // Guards target hostile mobs
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(this, Monster.class, true) {
            @Override
            public boolean canUse() {
                return profession == VillagerProfession.GUARD && followTarget != null && super.canUse();
            }
        });
    }

    // ── Initialization ───────────────────────────────────────────────────────

    private void initIfNeeded() {
        if (!initialized && !level().isClientSide()) {
            personality.randomize(getRandom(), profession);
            homePos = blockPosition();
            initialized = true;
        }
    }

    /** Called after spawn to set profession (e.g. from spawn egg or command). */
    public void setProfession(VillagerProfession prof) {
        this.profession = prof;
        this.initialized = false;
    }

    // ── Tick ─────────────────────────────────────────────────────────────────

    @Override
    public void aiStep() {
        super.aiStep();
        if (level().isClientSide()) return;

        initIfNeeded();

        // Guard follow timer
        if (followTarget != null && followTicksRemaining > 0) {
            followTicksRemaining--;
            if (followTicksRemaining <= 0) {
                clearFollowTarget();
            }
        }
    }


    // ── Emote sending ────────────────────────────────────────────────────────

    public void sendEmote(EmoteType emote) {
        long now = level().getGameTime();
        if (now - lastEmoteTick < 60) return; // 3 second cooldown
        lastEmoteTick = now;

        if (level() instanceof ServerLevel) {
            VillagerEmotePayload payload = new VillagerEmotePayload(getId(), emote.ordinal());
            PacketDistributor.sendToPlayersTrackingEntity(this, payload);
        }
    }

    // ── Player interaction ───────────────────────────────────────────────────

    @Override
    protected InteractionResult mobInteract(Player player, InteractionHand hand) {
        if (level().isClientSide() || hand != InteractionHand.MAIN_HAND) {
            return InteractionResult.SUCCESS;
        }

        initIfNeeded();

        ServerPlayer sp = (ServerPlayer) player;
        long now = level().getGameTime();

        // Per-villager cooldown (30 seconds = 600 ticks)
        if (now - lastInteractTick < 600) {
            return InteractionResult.SUCCESS;
        }

        // Per-player cooldown (60 seconds = 1200 ticks)
        UUID playerId = player.getUUID();
        Long lastForPlayer = perPlayerCooldown.get(playerId);
        if (lastForPlayer != null && now - lastForPlayer < 1200) {
            return InteractionResult.SUCCESS;
        }

        lastInteractTick = now;
        perPlayerCooldown.put(playerId, now);

        getLookControl().setLookAt(player, 30.0F, 30.0F);

        if (handleHeldItemReaction(sp)) {
            return InteractionResult.SUCCESS;
        }

        if (profession.canTalk()) {
            handleTalkingInteraction(sp);
        } else {
            handleSilentInteraction(sp);
        }

        return InteractionResult.SUCCESS;
    }

    /**
     * If the player is holding an item this villager has a tasted/disdained
     * opinion about, deliver that scripted reaction and skip the normal
     * profession flow. Returns true if a reaction was delivered.
     */
    private boolean handleHeldItemReaction(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.isEmpty()) return false;

        NpcItemReaction.Reaction reaction = NpcItemReaction.resolve(manifest(), held);
        if (!reaction.isPresent()) return false;

        sendDialogue(reaction.line(), player);
        personality.shiftOpinion(player.getUUID(), reaction.rapportDelta());
        // Mood follows opinion — a strong gift lifts mood, a snub sours it.
        personality.shiftMood(reaction.rapportDelta() / 50f);

        switch (reaction.kind()) {
            case TASTE -> sendEmote(EmoteType.SPARKLE);
            case DISDAIN -> sendEmote(EmoteType.ANGER);
            default -> sendEmote(EmoteType.EXCLAMATION);
        }
        performProfessionAction(player);
        return true;
    }

    // ── Manifest lookup (HasNpcManifest) ─────────────────────────────────────

    @Override
    public @Nullable Identifier specificManifestId() { return null; }

    @Override
    public @Nullable Identifier fallbackManifestId() {
        return Identifier.fromNamespaceAndPath(Ironhold.MODID, "profession/" + profession.id());
    }

    // ── Talking interaction (LLM-powered) ────────────────────────────────────

    private void handleTalkingInteraction(ServerPlayer player) {
        if (!OllamaClient.isConfigured()) {
            String fallback = profession.randomFallback(getRandom());
            sendDialogue(fallback, player);
            performProfessionAction(player);
            return;
        }

        PlayerKingdomRpgData rpg = player.getData(ModAttachments.PLAYER_RPG.get());
        PlayerClass pClass = rpg.playerClass();

        String systemPrompt = personality.buildSystemPrompt(profession);
        String userMessage = personality.buildUserMessage(
            player.getName().getString(), pClass.id(), rpg.classLevel(), player.getUUID());

        var server = level().getServer();
        OllamaClient.requestRawText(systemPrompt, userMessage,
            "Kingdom SMP - " + profession.displayName(),
            rawResponse -> {
                if (server == null) return;

                if (rawResponse == null) {
                    server.execute(() -> {
                        sendDialogue(profession.randomFallback(getRandom()), player);
                        performProfessionAction(player);
                    });
                    return;
                }

                server.execute(() -> {
                    try {
                        String content = rawResponse.trim();
                        if (content.startsWith("```")) {
                            content = content.replaceAll("(?s)```[a-z]*\\s*", "").trim();
                        }
                        JsonObject parsed = JsonParser.parseString(content).getAsJsonObject();

                        String dialogue = parsed.has("dialogue")
                            ? parsed.get("dialogue").getAsString() : profession.randomFallback(getRandom());
                        String memory = parsed.has("memory")
                            ? parsed.get("memory").getAsString() : "";
                        float moodShift = parsed.has("mood_shift")
                            ? parsed.get("mood_shift").getAsFloat() : 0.0f;
                        int opinionShift = parsed.has("opinion_shift")
                            ? parsed.get("opinion_shift").getAsInt() : 0;

                        sendDialogue(dialogue, player);

                        if (!memory.isEmpty()) {
                            personality.addMemory(memory);
                        }
                        personality.shiftMood(Mth.clamp(moodShift, -0.3f, 0.3f));
                        personality.shiftOpinion(player.getUUID(),
                            Mth.clamp(opinionShift, -10, 10));

                        Ironhold.LOGGER.info("[{}] {} says: \"{}\"",
                            profession.displayName(), personality.name(), dialogue);

                    } catch (Exception e) {
                        Ironhold.LOGGER.warn("[KingdomVillager] Failed to parse LLM response: {}",
                            e.getMessage());
                        sendDialogue(profession.randomFallback(getRandom()), player);
                    }

                    performProfessionAction(player);
                });
            });
    }

    // ── Silent interaction (emote + mechanic) ────────────────────────────────

    private void handleSilentInteraction(ServerPlayer player) {
        playSound(SoundEvents.VILLAGER_AMBIENT, 1.0F, 1.0F);

        if (personality.mood() > 0.2f) {
            sendEmote(EmoteType.HEART);
        } else if (personality.mood() < -0.2f) {
            sendEmote(EmoteType.ANGER);
        } else {
            sendEmote(EmoteType.EXCLAMATION);
        }

        personality.shiftMood(0.1f);
        performProfessionAction(player);
    }

    // ── Profession-specific mechanics ────────────────────────────────────────

    private void performProfessionAction(ServerPlayer player) {
        switch (profession) {
            case BLACKSMITH -> actionBlacksmith(player);
            case FARMER     -> actionFarmer(player);
            case GUARD      -> actionGuard(player);
            case MERCHANT   -> actionMerchant(player);
            case ALCHEMIST  -> actionAlchemist(player);
            case WIZARD     -> actionWizard(player);
            case PRIEST     -> actionPriest(player);
            case LIBRARIAN  -> actionLibrarian(player);
            case BARD       -> actionBard(player);
        }
    }

    private void actionBlacksmith(ServerPlayer player) {
        ItemStack held = player.getMainHandItem();
        if (held.isDamaged()) {
            int cost = 1 + (int) ((1.0f - personality.effectiveFriendliness()) * 2);
            if (removeEmeralds(player, cost)) {
                int repairAmount = (int) (held.getMaxDamage() * (0.3f + personality.effectiveFriendliness() * 0.4f));
                held.setDamageValue(Math.max(0, held.getDamageValue() - repairAmount));
                playSound(SoundEvents.ANVIL_USE, 1.0F, 1.0F);
                sendEmote(EmoteType.SPARKLE);
            } else {
                sendEmote(EmoteType.ANGER);
            }
        }
    }

    private void actionFarmer(ServerPlayer player) {
        long dayTime = level().getGameTime() % 24000;
        int amount = dayTime < 6000 ? 3 : dayTime < 12000 ? 2 : 1;
        amount += personality.effectiveFriendliness() > 0.6f ? 1 : 0;

        ItemStack food = getRandom().nextBoolean()
            ? new ItemStack(Items.BREAD, amount)
            : new ItemStack(Items.COOKED_BEEF, Math.max(1, amount - 1));

        if (!player.getInventory().add(food)) {
            player.drop(food, false);
        }
        playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
        sendEmote(EmoteType.HEART);
    }

    private void actionGuard(ServerPlayer player) {
        if (followTarget != null) {
            sendEmote(EmoteType.EXCLAMATION);
            return;
        }
        followTarget = player;
        followTicksRemaining = 2400; // 2 minutes
        sendEmote(EmoteType.SPARKLE);
        playSound(SoundEvents.ARMOR_EQUIP_IRON.value(), 1.0F, 1.0F);
    }

    private void actionMerchant(ServerPlayer player) {
        int cost = 1 + (int) ((1.0f - personality.effectiveFriendliness()) * 4);
        if (removeEmeralds(player, cost)) {
            ItemStack reward = getMerchantReward();
            if (!player.getInventory().add(reward)) {
                player.drop(reward, false);
            }
            playSound(SoundEvents.VILLAGER_YES, 1.0F, 1.0F);
            sendEmote(EmoteType.SPARKLE);
        } else {
            playSound(SoundEvents.VILLAGER_NO, 1.0F, 1.0F);
            sendEmote(EmoteType.ANGER);
        }
    }

    private void actionAlchemist(ServerPlayer player) {
        PlayerKingdomRpgData rpg = player.getData(ModAttachments.PLAYER_RPG.get());
        PlayerClass pClass = rpg.playerClass();

        MobEffectInstance effect = switch (pClass) {
            case KNIGHT  -> new MobEffectInstance(MobEffects.RESISTANCE, 1200, 0);
            case RANGER  -> new MobEffectInstance(MobEffects.SPEED, 1200, 1);
            case WIZARD  -> new MobEffectInstance(MobEffects.NIGHT_VISION, 2400, 0);
            case CLERIC  -> new MobEffectInstance(MobEffects.REGENERATION, 1200, 0);
            default      -> new MobEffectInstance(MobEffects.SATURATION, 600, 0);
        };

        player.addEffect(effect);
        playSound(SoundEvents.BREWING_STAND_BREW, 1.0F, 1.0F);
        sendEmote(EmoteType.SPARKLE);
    }

    private void actionWizard(ServerPlayer player) {
        if (getRandom().nextFloat() < 0.10f) {
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            if (!player.getInventory().add(book)) {
                player.drop(book, false);
            }
            sendEmote(EmoteType.SPARKLE);
            playSound(SoundEvents.ENCHANTMENT_TABLE_USE, 1.0F, 1.0F);
        }
    }

    private void actionPriest(ServerPlayer player) {
        PlayerKingdomRpgData rpg = player.getData(ModAttachments.PLAYER_RPG.get());
        boolean isCleric = rpg.playerClass() == PlayerClass.CLERIC;

        float healAmount = isCleric ? 10.0f : 6.0f;
        player.heal(healAmount);

        int duration = isCleric ? 600 : 300;
        player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, duration, isCleric ? 1 : 0));

        player.removeEffect(MobEffects.POISON);
        player.removeEffect(MobEffects.WITHER);
        player.removeEffect(MobEffects.WEAKNESS);

        playSound(SoundEvents.PLAYER_LEVELUP, 0.7F, 1.2F);
        sendEmote(EmoteType.SPARKLE);
    }

    private void actionLibrarian(ServerPlayer player) {
        if (getRandom().nextFloat() < 0.10f) {
            ItemStack book = new ItemStack(Items.ENCHANTED_BOOK);
            if (!player.getInventory().add(book)) {
                player.drop(book, false);
            }
            sendEmote(EmoteType.SPARKLE);
        }
    }

    private void actionBard(ServerPlayer player) {
        if (!(level() instanceof ServerLevel serverLevel)) return;

        MobEffectInstance buff = switch (getRandom().nextInt(3)) {
            case 0  -> new MobEffectInstance(MobEffects.SPEED, 2400, 0);
            case 1  -> new MobEffectInstance(MobEffects.STRENGTH, 2400, 0);
            default -> new MobEffectInstance(MobEffects.LUCK, 2400, 0);
        };

        for (Player nearby : serverLevel.getEntitiesOfClass(Player.class,
                getBoundingBox().inflate(16.0), p -> p instanceof ServerPlayer)) {
            ((ServerPlayer) nearby).addEffect(new MobEffectInstance(buff));
        }

        playSound(SoundEvents.NOTE_BLOCK_HARP.value(), 1.5F, 1.0F);
        sendEmote(EmoteType.MUSIC);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private void sendDialogue(String text, ServerPlayer interactingPlayer) {
        if (!(level() instanceof ServerLevel)) return;
        // Floating text for all observers
        VillagerDialoguePayload payload = new VillagerDialoguePayload(
            getId(), personality.name(), profession.id(), text);
        PacketDistributor.sendToPlayersTrackingEntity(this, payload);
        // Full dialogue screen for the interacting player only
        PacketDistributor.sendToPlayer(interactingPlayer,
            new OpenVillagerScreenPayload(
                personality.name(),
                profession.id(),
                text,
                OpenVillagerScreenPayload.encodeMood(personality.mood()),
                getId()));
    }

    private boolean removeEmeralds(ServerPlayer player, int count) {
        int remaining = count;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            ItemStack stack = player.getInventory().getItem(i);
            if (stack.is(Items.EMERALD)) {
                int take = Math.min(remaining, stack.getCount());
                stack.shrink(take);
                remaining -= take;
                if (remaining <= 0) return true;
            }
        }
        return false;
    }

    private ItemStack getMerchantReward() {
        return switch (getRandom().nextInt(8)) {
            case 0 -> new ItemStack(Items.IRON_INGOT, 2 + getRandom().nextInt(3));
            case 1 -> new ItemStack(Items.GOLD_INGOT, 1 + getRandom().nextInt(2));
            case 2 -> new ItemStack(Items.DIAMOND, 1);
            case 3 -> new ItemStack(Items.ENDER_PEARL, 1 + getRandom().nextInt(2));
            case 4 -> new ItemStack(Items.EXPERIENCE_BOTTLE, 3 + getRandom().nextInt(5));
            case 5 -> new ItemStack(Items.GOLDEN_APPLE, 1);
            case 6 -> new ItemStack(Items.NAME_TAG, 1);
            default -> new ItemStack(Items.LAPIS_LAZULI, 4 + getRandom().nextInt(8));
        };
    }

    // ── Damage reaction ──────────────────────────────────────────────────────

    @Override
    public boolean hurtServer(ServerLevel serverLevel, DamageSource source, float amount) {
        boolean result = super.hurtServer(serverLevel, source, amount);
        if (result) {
            personality.shiftMood(-0.3f);
            sendEmote(EmoteType.SWEAT);
            if (source.getEntity() instanceof Player attacker) {
                personality.shiftOpinion(attacker.getUUID(), -20);
            }
        }
        return result;
    }

    // ── Guard follow state ───────────────────────────────────────────────────

    public Player getFollowTarget() { return followTarget; }
    public int getFollowTicksRemaining() { return followTicksRemaining; }

    public void clearFollowTarget() {
        this.followTarget = null;
        this.followTicksRemaining = 0;
    }

    // ── Accessors ────────────────────────────────────────────────────────────

    public VillagerProfession getProfession() { return profession; }
    public VillagerPersonality getPersonality() { return personality; }
    public BlockPos getHomePos() { return homePos; }

    @Override
    public Component getName() {
        String color = profession.canTalk() ? "\u00A7b" : "\u00A7e";
        return Component.literal(color + personality.name() + " the " + profession.displayName());
    }

    // ── NBT persistence (ValueOutput/ValueInput) ─────────────────────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putString("Profession", profession.id());
        out.putBoolean("Initialized", initialized);
        personality.save(out);
        if (homePos != null) {
            out.putInt("HomeX", homePos.getX());
            out.putInt("HomeY", homePos.getY());
            out.putInt("HomeZ", homePos.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        // Only override profession from NBT if data was actually saved (not a fresh spawn)
        String savedProf = in.getStringOr("Profession", "");
        if (!savedProf.isEmpty()) {
            this.profession = VillagerProfession.fromId(savedProf);
        }
        this.initialized = in.getBooleanOr("Initialized", false);
        personality.load(in);

        int hx = in.getIntOr("HomeX", Integer.MIN_VALUE);
        if (hx != Integer.MIN_VALUE) {
            this.homePos = new BlockPos(hx, in.getIntOr("HomeY", 64), in.getIntOr("HomeZ", 0));
        }
    }
}
