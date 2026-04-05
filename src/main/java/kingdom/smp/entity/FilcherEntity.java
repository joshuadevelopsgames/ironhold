package kingdom.smp.entity;

import kingdom.smp.entity.goal.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.*;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import javax.annotation.Nullable;
import java.util.List;

/**
 * Filcher — a small, quiet rogue-class mob that sneaks up behind players,
 * steals one item from their hotbar, and flees while seeking cover.
 *
 * <p>Filchers are community creatures: they huddle together when idle,
 * follow the richest member of their pack, share junk with empty-handed
 * peers, coordinate lookouts during heists, and mourn their fallen.
 * Each filcher has a unique personality (boldness, greed, sociability)
 * that shapes how it behaves.
 *
 * <p>They establish dens in dark places where they stash stolen goods.
 * Kill a filcher to reclaim whatever it's holding, or raid their den
 * to recover everything they've hoarded.
 */
public class FilcherEntity extends Zombie {

    // ── Dimensions ───────────────────────────────────────────────────────────
    private static final EntityDimensions FILCHER_DIMENSIONS =
        EntityDimensions.scalable(0.49F, 0.99F).withEyeHeight(0.75F);

    // ── Personality traits (0.0–1.0, assigned at spawn) ──────────────────────
    /** Higher boldness → steals even when partially watched, shorter flee. */
    private float boldness = 0.5F;
    /** Higher greed → covets items more aggressively, less willing to gift. */
    private float greed = 0.5F;
    /** Higher sociability → huddles closer, follows alpha more, gifts more. */
    private float sociability = 0.5F;

    // ── Community state ──────────────────────────────────────────────────────
    /** Position of the den this filcher calls home. Null if none established. */
    @Nullable private BlockPos denPos;

    /** Ticks remaining in death-scatter panic (set when a nearby filcher dies). */
    private int scatterTicks;

    /** Ticks remaining for show-off celebration after stashing an item. */
    private int showOffTicks;

    /**
     * Set by {@link FilcherStealGoal} when this filcher is being covered by
     * a lookout. The lookout can cancel the steal by clearing this flag.
     */
    private boolean hasLookout;

    /**
     * Set by {@link FilcherLookoutGoal} when a warning squeak fires —
     * the thief should abort its current steal attempt.
     */
    private boolean lookoutWarning;

    /** Ticks remaining in swarm-distraction mode (set by RecruitGoal on recruits). */
    private int swarmTicks;
    /** Player this filcher is swarming toward as a distraction. Null when not swarming. */
    @Nullable private Player swarmTarget;
    /** Ticks before this filcher may attempt another steal from a player. */
    private int stealCooldownTicks;

    /** Personal stash — 9 slots, dropped in full on death. */
    private final SimpleContainer filcherInventory = new SimpleContainer(9);

    // ── Identity (stable from spawn) ─────────────────────────────────────────
    /** Name derived from UUID — same filcher always has the same name. */
    private String filcherName = "Filcher";

    /** Character archetype derived from trait floats — never changes after spawn. */
    private FilcherArchetype archetype = FilcherArchetype.FENCE;

    // ── King-assigned role (changes each operation cycle) ────────────────────
    /** Role assigned by the pack king's LLM brain. Default is IDLE. */
    private FilcherRole role = FilcherRole.IDLE;

    /** Item name the king wants this filcher to prioritize stealing. Null = any valuable item. */
    @Nullable private String assignedTargetItem = null;

    public FilcherEntity(EntityType<? extends FilcherEntity> type, Level level) {
        super(type, level);
        this.setBaby(true);
        // Roll personality at spawn
        this.boldness     = 0.15F + this.random.nextFloat() * 0.70F;  // 0.15–0.85
        this.greed        = 0.15F + this.random.nextFloat() * 0.70F;
        this.sociability  = 0.15F + this.random.nextFloat() * 0.70F;
        // Derive stable identity from UUID + traits (UUID not finalised until addedToLevel,
        // so we set proper values there; these are safe initial defaults)
        this.archetype   = FilcherArchetype.from(boldness, greed, sociability);
        this.filcherName = FilcherArchetype.nameFor(this.uuid.getLeastSignificantBits());
    }

    // ── Always baby-sized ────────────────────────────────────────────────────

    @Override
    public boolean isBaby() {
        return true;
    }

    @Override
    public EntityDimensions getDefaultDimensions(Pose pose) {
        return FILCHER_DIMENSIONS;
    }

    // ── Personality accessors ────────────────────────────────────────────────

    public float getBoldness()     { return boldness; }
    public float getGreed()        { return greed; }
    public float getSociability()  { return sociability; }

    // ── Identity accessors ───────────────────────────────────────────────────

    public String getFilcherName()           { return filcherName; }
    public FilcherArchetype getArchetype()   { return archetype; }

    // ── Role accessors ───────────────────────────────────────────────────────

    public FilcherRole getRole()                         { return role; }
    public void setRole(FilcherRole r)                   { this.role = r; }

    @Nullable public String getAssignedTargetItem()      { return assignedTargetItem; }
    public void setAssignedTargetItem(@Nullable String item) { this.assignedTargetItem = item; }

    /**
     * Returns a compact one-line character brief for this filcher,
     * suitable for embedding in the king's LLM prompt.
     * Example: "Skrit (the Opportunist) — wants: to pull off the biggest score; fears: being overshadowed; quirk: always grabs one extra item"
     */
    public String characterBrief() {
        return String.format(
            "%s (%s) — role: %s | wants: %s | fears: %s | quirk: %s | loot: %d",
            filcherName, archetype.title,
            role.shortCode(),
            archetype.desire,
            archetype.fear,
            archetype.quirk,
            getTotalLootValue()
        );
    }

    // ── Community accessors ──────────────────────────────────────────────────

    @Nullable
    public BlockPos getDenPos() { return denPos; }

    public void setDenPos(@Nullable BlockPos pos) { this.denPos = pos; }

    /** Shares this filcher's den position with all nearby filchers that lack one. */
    public void shareDenWithNearby() {
        if (denPos == null) return;
        List<FilcherEntity> nearby = level().getEntitiesOfClass(
            FilcherEntity.class,
            getBoundingBox().inflate(16.0),
            f -> f != this && f.isAlive() && f.getDenPos() == null
        );
        for (FilcherEntity f : nearby) {
            f.setDenPos(denPos);
        }
    }

    public int getScatterTicks() { return scatterTicks; }

    public void setScatterTicks(int ticks) { this.scatterTicks = ticks; }

    public boolean isShowingOff() { return showOffTicks > 0; }

    public void triggerShowOff() { this.showOffTicks = 40; }

    public boolean hasLookout() { return hasLookout; }

    public void setHasLookout(boolean v) { this.hasLookout = v; }

    public boolean isLookoutWarning() { return lookoutWarning; }

    public void setLookoutWarning(boolean v) { this.lookoutWarning = v; }

    public int getSwarmTicks() { return swarmTicks; }
    @Nullable public Player getSwarmTarget() { return swarmTarget; }
    public void setSwarmTarget(@Nullable Player target, int ticks) {
        this.swarmTarget = target;
        this.swarmTicks  = ticks;
    }
    public int getStealCooldownTicks() { return stealCooldownTicks; }
    public void setStealCooldownTicks(int ticks) { this.stealCooldownTicks = ticks; }

    /** Returns true if the given stack is Fool's Gold. */
    public static boolean isFoolsGold(ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() == kingdom.smp.Ironhold.FOOLS_GOLD.get();
    }

    /** Returns true if this filcher is wearing the crown — i.e. it is the pack king. */
    public boolean isKing() {
        return !getItemBySlot(EquipmentSlot.HEAD).isEmpty()
            && getItemBySlot(EquipmentSlot.HEAD).is(kingdom.smp.Ironhold.FILCHER_CROWN.get());
    }

    /** Returns how many stash slots are currently occupied. */
    public int countStashedItems() {
        int count = 0;
        for (int i = 0; i < filcherInventory.getContainerSize(); i++) {
            if (!filcherInventory.getItem(i).isEmpty()) count++;
        }
        return count;
    }

    /**
     * Finds the nearest cave-like position: an underground air block with a
     * solid floor and overhead rock, confirming it is inside terrain rather
     * than just on the surface at night. Used as the filcher's den site.
     */
    @Nullable
    public BlockPos findCaveEntrance() {
        BlockPos base = blockPosition();
        var rand = getRandom();
        int radius = 40;
        for (int i = 0; i < 40; i++) {
            int dx = rand.nextIntBetweenInclusive(-radius, radius);
            int dz = rand.nextIntBetweenInclusive(-radius, radius);
            int surfY = level().getHeight(
                Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
                base.getX() + dx, base.getZ() + dz);
            int dy = rand.nextIntBetweenInclusive(-15, -3);
            BlockPos candidate = new BlockPos(base.getX() + dx, surfY + dy, base.getZ() + dz);
            if (candidate.getY() < level().getMinY() || candidate.getY() > level().getMaxY()) continue;
            if (!level().getBlockState(candidate).isAir()) continue;
            if (!level().getBlockState(candidate.below()).isSolid()) continue;
            if (level().getMaxLocalRawBrightness(candidate) > 4) continue;
            // Solid roof within 3 blocks = confirmed cave, not open sky
            boolean hasRoof = level().getBlockState(candidate.above()).isSolid()
                || level().getBlockState(candidate.above(2)).isSolid()
                || level().getBlockState(candidate.above(3)).isSolid();
            if (!hasRoof) continue;
            return candidate;
        }
        return null;
    }

    /** Route naturally picked-up items to the stash inventory instead of mainhand. */
    @Override
    protected void pickUpItem(net.minecraft.server.level.ServerLevel serverLevel, ItemEntity itemEntity) {
        ItemStack item = itemEntity.getItem().copy();
        onItemPickup(itemEntity);
        ItemStack leftover = filcherInventory.addItem(item);
        if (!leftover.isEmpty() && getMainHandItem().isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, leftover);
        }
        itemEntity.discard();
    }

    // ── Inventory ────────────────────────────────────────────────────────────

    public SimpleContainer getFilcherInventory() {
        return filcherInventory;
    }

    /** Returns the total value of all items in the filcher's stash + main hand. */
    public int getTotalLootValue() {
        int value = itemValue(getMainHandItem());
        for (int i = 0; i < filcherInventory.getContainerSize(); i++) {
            value += itemValue(filcherInventory.getItem(i));
        }
        return value;
    }

    /** Returns true if every inventory slot is occupied. */
    public boolean isInventoryFull() {
        for (int i = 0; i < filcherInventory.getContainerSize(); i++) {
            if (filcherInventory.getItem(i).isEmpty()) return false;
        }
        return true;
    }

    /** Returns the lowest-value non-empty item in the stash, or EMPTY. */
    public ItemStack getLowestValueItem() {
        ItemStack lowest = ItemStack.EMPTY;
        int lowestVal = Integer.MAX_VALUE;
        for (int i = 0; i < filcherInventory.getContainerSize(); i++) {
            ItemStack s = filcherInventory.getItem(i);
            if (!s.isEmpty() && itemValue(s) < lowestVal) {
                lowestVal = itemValue(s);
                lowest = s;
            }
        }
        return lowest;
    }

    /** Removes and returns the lowest-value item from the stash. */
    public ItemStack removeLowestValueItem() {
        int lowestIdx = -1;
        int lowestVal = Integer.MAX_VALUE;
        for (int i = 0; i < filcherInventory.getContainerSize(); i++) {
            ItemStack s = filcherInventory.getItem(i);
            if (!s.isEmpty() && itemValue(s) < lowestVal) {
                lowestVal = itemValue(s);
                lowestIdx = i;
            }
        }
        if (lowestIdx >= 0) {
            return filcherInventory.removeItem(lowestIdx, filcherInventory.getItem(lowestIdx).getCount());
        }
        return ItemStack.EMPTY;
    }

    // ── Item value heuristic (shared across all goals) ───────────────────────

    public static int itemValue(ItemStack stack) {
        if (stack.isEmpty()) return 0;
        int rarityScore = switch (stack.getRarity()) {
            case COMMON   -> 0;
            case UNCOMMON -> 10;
            case RARE     -> 30;
            case EPIC     -> 60;
        };
        return rarityScore + stack.getCount();
    }

    // ── Sound palette ────────────────────────────────────────────────────────

    /** Quiet chirp for idle chatter between filchers. */
    public void playChatter() {
        level().playSound(null, blockPosition(),
            SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.HOSTILE,
            0.4F, 1.2F + random.nextFloat() * 0.6F);
    }

    /** Triumphant squeak after a successful steal. */
    public void playStealSuccess() {
        level().playSound(null, blockPosition(),
            SoundEvents.FOX_BITE, SoundSource.HOSTILE,
            0.8F, 1.4F + random.nextFloat() * 0.3F);
    }

    /** Happy chirp for show-off / contentment. */
    public void playContentment() {
        level().playSound(null, blockPosition(),
            SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.HOSTILE,
            0.5F, 1.6F + random.nextFloat() * 0.4F);
    }

    /** Distress squeal when a pack member dies. */
    public void playDistress() {
        level().playSound(null, blockPosition(),
            SoundEvents.VEX_HURT, SoundSource.HOSTILE,
            0.7F, 1.2F + random.nextFloat() * 0.3F);
    }

    /** Negotiation chime during filcher-to-filcher interactions. */
    public void playNegotiation() {
        level().playSound(null, blockPosition(),
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.HOSTILE,
            0.6F, 0.8F + random.nextFloat() * 0.8F);
    }

    /** Alert chirp used by lookouts to warn a thief. */
    public void playAlert() {
        level().playSound(null, blockPosition(),
            SoundEvents.FOX_AGGRO, SoundSource.HOSTILE,
            0.7F, 1.5F + random.nextFloat() * 0.3F);
    }

    /** Recruit call to summon nearby filchers for a coordinated heist. */
    public void playRecruitCall() {
        level().playSound(null, blockPosition(),
            SoundEvents.FOX_AGGRO, SoundSource.HOSTILE,
            0.9F, 1.0F + random.nextFloat() * 0.2F);
    }

    /** Soft gift-giving sound. */
    public void playGift() {
        level().playSound(null, blockPosition(),
            SoundEvents.ALLAY_ITEM_GIVEN, SoundSource.HOSTILE,
            0.5F, 1.0F + random.nextFloat() * 0.4F);
    }

    // ── AI ────────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        // --- Survival ---
        this.goalSelector.addGoal(0,  new FloatGoal(this));

        // --- King's LLM brain (only active when this filcher wears the crown) ---
        this.goalSelector.addGoal(1,  new FilcherKingBrainGoal(this));

        // --- Priority: carrying loot → head home → swarm distraction ---
        this.goalSelector.addGoal(2,  new FilcherCarryFleeGoal(this));
        this.goalSelector.addGoal(3,  new FilcherHomeboundGoal(this));          // return when >65 blocks from den
        this.goalSelector.addGoal(4,  new FilcherSwarmGoal(this));              // distraction crowding (recruited by mastermind)

        // --- Stealing (from most to least preferred target) ---
        this.goalSelector.addGoal(5,  new FilcherKingSpawnGoal(this));          // king: spawn reinforcements
        this.goalSelector.addGoal(5,  new FilcherRecruitGoal(this));            // coordinated distraction heist
        this.goalSelector.addGoal(6,  new FilcherStealGoal(this, 1.05D));       // solo player steal (behind back)
        this.goalSelector.addGoal(6,  new FilcherShadowGoal(this));             // non-bold: hide in shadows
        this.goalSelector.addGoal(7,  new FilcherLookoutGoal(this));            // watch while another steals
        this.goalSelector.addGoal(8,  new FilcherVillagerStealGoal(this, 1.5D));// pickpocket villager
        this.goalSelector.addGoal(9,  new FilcherEndermanStealGoal(this, 1.5D));// snatch from enderman

        // --- Community interactions ---
        this.goalSelector.addGoal(10, new FilcherTradeGoal(this));
        this.goalSelector.addGoal(11, new FilcherGiftGoal(this));
        this.goalSelector.addGoal(12, new FilcherShowOffGoal(this));

        // --- Pack behavior ---
        this.goalSelector.addGoal(13, new FilcherFollowAlphaGoal(this));
        this.goalSelector.addGoal(14, new FilcherDenGoal(this));
        this.goalSelector.addGoal(15, new FilcherHuddleGoal(this));

        // --- Fallback idle ---
        this.goalSelector.addGoal(16, new WaterAvoidingRandomStrollGoal(this, 1.0D));
        this.goalSelector.addGoal(17, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(18, new RandomLookAroundGoal(this));

        // Target only players within 75 blocks of den
        this.targetSelector.addGoal(1, new NearestAttackableTargetGoal<Player>(this, Player.class, 10, true, false,
            (entity, serverLevel) -> denPos == null
                || net.minecraft.world.phys.Vec3.atCenterOf(denPos).distanceToSqr(entity.position()) <= 75.0 * 75.0));
    }

    // ── Attributes ───────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return Zombie.createAttributes()
            .add(Attributes.MAX_HEALTH,      10.0D)
            .add(Attributes.MOVEMENT_SPEED,   0.22D)   // sneaky-quick but not disorienting
            .add(Attributes.ATTACK_DAMAGE,    1.0D)
            .add(Attributes.FOLLOW_RANGE,    20.0D);
    }

    // ── No weapons at spawn ──────────────────────────────────────────────────

    @Override
    protected void populateDefaultEquipmentSlots(
            net.minecraft.util.RandomSource random,
            net.minecraft.world.DifficultyInstance difficulty) {
        // Filchers steal for their loot — they don't spawn armed
    }

    // ── No sunburn ───────────────────────────────────────────────────────────

    @Override
    protected boolean isSunSensitive() {
        return false;
    }

    // ── Tick — death scatter + show-off animations ───────────────────────────

    @Override
    public void aiStep() {
        super.aiStep();

        // ── Auto-crouch while stalking ────────────────────────────────────────
        // Visual only (drives isCrouching in render state for model animation).
        boolean stalking = !isShowingOff()
            && scatterTicks == 0
            && getMainHandItem().isEmpty()
            && getTarget() instanceof net.minecraft.world.entity.player.Player;
        setShiftKeyDown(stalking);

        if (level().isClientSide()) return;

        if (stealCooldownTicks > 0) stealCooldownTicks--;
        if (swarmTicks > 0) {
            swarmTicks--;
            if (swarmTicks == 0) swarmTarget = null;
        }

        // ── Curiosity: furtive sidelong glance at nearby unaware players (~4 sec) ─
        if (tickCount % 80 == 11 && getTarget() == null && !isShowingOff() && scatterTicks == 0) {
            net.minecraft.world.entity.player.Player nearest =
                level().getNearestPlayer(this, 6.0);
            if (nearest != null) {
                net.minecraft.world.phys.Vec3 look   = nearest.getLookAngle();
                net.minecraft.world.phys.Vec3 toThis =
                    position().subtract(nearest.getEyePosition()).normalize();
                if (look.dot(toThis) < 0.5) { // player NOT looking at us
                    // Glance toward their feet — furtive, not brazen
                    getLookControl().setLookAt(
                        nearest.getX(), nearest.getY(), nearest.getZ(), 20.0F, 10.0F);
                }
            }
        }

        // ── Quiet snicker when right next to an unaware player ────────────────
        if (tickCount % 100 == 23 && getTarget() == null && scatterTicks == 0) {
            net.minecraft.world.entity.player.Player nearest =
                level().getNearestPlayer(this, 5.0);
            if (nearest != null) {
                net.minecraft.world.phys.Vec3 look   = nearest.getLookAngle();
                net.minecraft.world.phys.Vec3 toThis =
                    position().subtract(nearest.getEyePosition()).normalize();
                if (look.dot(toThis) < 0.5) { // unnoticed — can't help snickering
                    playNegotiation();
                }
            }
        }

        // ── Excited micro-hop when idle in a pack ─────────────────────────────
        if (tickCount % 80 == 5 && getTarget() == null && scatterTicks == 0 && !isShowingOff()
                && onGround() && random.nextFloat() < 0.25F) {
            java.util.List<FilcherEntity> nearby = level().getEntitiesOfClass(
                FilcherEntity.class, getBoundingBox().inflate(4.0),
                f -> f != this && f.isAlive());
            if (!nearby.isEmpty()) {
                setDeltaMovement(getDeltaMovement().add(0, 0.22, 0));
            }
        }

        // Death scatter: sprint away from death site
        if (scatterTicks > 0) {
            scatterTicks--;
            if (scatterTicks == 0) {
                // Resume normal speed
                this.setSpeed(0.0F);
            }
        }

        // Show-off celebration: hop + particles + chirps
        if (showOffTicks > 0) {
            showOffTicks--;
            if (showOffTicks == 38) {
                // Initial hop
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.4, 0));
                playContentment();
            }
            if (showOffTicks == 20) {
                // Second little hop
                this.setDeltaMovement(this.getDeltaMovement().add(0, 0.25, 0));
            }
            // Particle burst every 8 ticks
            if (showOffTicks % 8 == 0 && level() instanceof ServerLevel serverLevel) {
                serverLevel.sendParticles(
                    ParticleTypes.HAPPY_VILLAGER,
                    getX(), getY() + 0.8, getZ(),
                    3, 0.3, 0.3, 0.3, 0.02);
            }
            // Nearby filchers look at the show-off
            if (showOffTicks == 35) {
                for (FilcherEntity f : level().getEntitiesOfClass(
                        FilcherEntity.class, getBoundingBox().inflate(8.0),
                        f -> f != this && f.isAlive())) {
                    f.getLookControl().setLookAt(this, 30.0F, 30.0F);
                }
            }
        }
    }

    // ── Death → broadcast to nearby filchers ─────────────────────────────────

    @Override
    public void die(DamageSource cause) {
        super.die(cause);

        if (level().isClientSide()) return;

        // Alert all nearby filchers — they scatter in panic
        List<FilcherEntity> nearby = level().getEntitiesOfClass(
            FilcherEntity.class,
            getBoundingBox().inflate(16.0),
            f -> f != this && f.isAlive()
        );

        if (isKing()) {
            // King death — the whole pack panics harder
            level().playSound(null, blockPosition(),
                net.minecraft.sounds.SoundEvents.WITHER_DEATH,
                net.minecraft.sounds.SoundSource.HOSTILE,
                0.6F, 2.5F);
            for (FilcherEntity f : nearby) {
                f.playDistress();
                f.setScatterTicks(200);
                f.getNavigation().stop();
                Vec3 away = f.position().subtract(position()).normalize();
                double angle = Math.atan2(away.z, away.x) + (f.getRandom().nextDouble() - 0.5) * Math.PI;
                double dist = 20 + f.getRandom().nextInt(12);
                Vec3 fleeTarget = f.position().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);
                f.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, 2.0);
                if (level() instanceof ServerLevel serverLevel) {
                    serverLevel.sendParticles(net.minecraft.core.particles.ParticleTypes.SMOKE,
                        f.getX(), f.getY() + 0.6, f.getZ(), 5, 0.2, 0.2, 0.2, 0.01);
                }
            }
        } else {
            for (FilcherEntity f : nearby) {
                f.onNearbyFilcherDeath(this);
            }
        }
    }

    /** Called when a nearby filcher dies. Triggers mourning freeze + scatter. */
    public void onNearbyFilcherDeath(FilcherEntity dead) {
        // Distress call
        playDistress();

        // Brief freeze (2 sec = 40 ticks) then scatter for 3 sec
        this.scatterTicks = 100;  // total panic duration
        this.getNavigation().stop();

        // Pick a random direction away from the death site
        Vec3 away = this.position().subtract(dead.position()).normalize();
        double angle = Math.atan2(away.z, away.x) + (random.nextDouble() - 0.5) * Math.PI;
        double dist = 15 + random.nextInt(10);
        Vec3 fleeTarget = this.position().add(Math.cos(angle) * dist, 0, Math.sin(angle) * dist);

        // Delayed flee — the first 40 ticks are freeze, then they bolt
        this.getNavigation().moveTo(fleeTarget.x, fleeTarget.y, fleeTarget.z, 1.8);

        // Emit particles at the mourning filcher
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                ParticleTypes.SMOKE,
                getX(), getY() + 0.6, getZ(),
                5, 0.2, 0.2, 0.2, 0.01);
        }
    }

    // ── Pack spawning ─────────────────────────────────────────────────────────

    @Override
    public net.minecraft.world.entity.SpawnGroupData finalizeSpawn(
            net.minecraft.world.level.ServerLevelAccessor level,
            net.minecraft.world.DifficultyInstance difficulty,
            net.minecraft.world.entity.EntitySpawnReason spawnType,
            @Nullable net.minecraft.world.entity.SpawnGroupData spawnGroupData) {
        boolean isPackLeader = (spawnGroupData == null);
        spawnGroupData = super.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);

        if (isPackLeader && level instanceof ServerLevel serverLevel) {
            // Pack leader is always crowned king from spawn
            crownSelf();

            int additional = 2 + this.random.nextInt(4); // spawn 3–6 total (king + 2-5 members)
            for (int i = 0; i < additional; i++) {
                FilcherEntity member = (FilcherEntity) kingdom.smp.Ironhold.FILCHER.get().create(serverLevel, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
                if (member != null) {
                    double angle = this.random.nextDouble() * Math.PI * 2;
                    double dist  = 1.5 + this.random.nextDouble() * 3.0;
                    member.setPos(
                        getX() + Math.cos(angle) * dist,
                        getY(),
                        getZ() + Math.sin(angle) * dist);
                    // Pass non-null data so pack members don't recursively spawn more packs
                    member.finalizeSpawn(level, difficulty, spawnType, spawnGroupData);
                    serverLevel.addFreshEntity(member);
                }
            }
            // Establish cave den for the whole pack from birth
            BlockPos cave = findCaveEntrance();
            if (cave != null) {
                setDenPos(cave);
                shareDenWithNearby();
            }
        }
        return spawnGroupData;
    }

    /** Promotes this filcher to king: equips the crown and announces the event. */
    private void crownSelf() {
        setItemSlot(EquipmentSlot.HEAD, new ItemStack(kingdom.smp.Ironhold.FILCHER_CROWN.get()));
        // Fanfare: happy-villager particles + pitched-up contentment chirps
        if (level() instanceof ServerLevel serverLevel) {
            serverLevel.sendParticles(
                net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER,
                getX(), getY() + 1.0, getZ(),
                12, 0.4, 0.4, 0.4, 0.02);
        }
        level().playSound(null, blockPosition(),
            net.minecraft.sounds.SoundEvents.AMETHYST_BLOCK_CHIME,
            net.minecraft.sounds.SoundSource.HOSTILE,
            1.2F, 1.8F + random.nextFloat() * 0.4F);
    }

    // ── Drops ────────────────────────────────────────────────────────────────

    @Override
    protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean killedByPlayer) {
        // Handle crown drop manually (30% chance) — prevent vanilla's equipment system from double-dropping
        ItemStack crown = getItemBySlot(EquipmentSlot.HEAD);
        if (!crown.isEmpty() && crown.is(kingdom.smp.Ironhold.FILCHER_CROWN.get())) {
            setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
            if (random.nextFloat() < 0.30f) {
                this.spawnAtLocation(level, crown.copy());
            }
        }
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        // Explicitly drop carried item (guarantees it appears even if equipment system quirks out)
        ItemStack held = getMainHandItem();
        if (!held.isEmpty()) {
            setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            this.spawnAtLocation(level, held);
        }
        // Drop everything in the personal stash
        for (int i = 0; i < filcherInventory.getContainerSize(); i++) {
            ItemStack stack = filcherInventory.getItem(i);
            if (!stack.isEmpty()) this.spawnAtLocation(level, stack);
        }
        filcherInventory.clearContent();
    }

    // ── Persistence ──────────────────────────────────────────────────────────

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);
        filcherInventory.storeAsItemList(output.list("FilcherInventory", ItemStack.CODEC));
        output.putFloat("Boldness", boldness);
        output.putFloat("Greed", greed);
        output.putFloat("Sociability", sociability);
        output.putString("FilcherName", filcherName);
        output.putString("Archetype", archetype.name());
        output.putString("Role", role.name());
        if (assignedTargetItem != null) output.putString("AssignedTargetItem", assignedTargetItem);
        if (denPos != null) {
            output.putInt("DenX", denPos.getX());
            output.putInt("DenY", denPos.getY());
            output.putInt("DenZ", denPos.getZ());
        }
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);
        filcherInventory.fromItemList(input.listOrEmpty("FilcherInventory", ItemStack.CODEC));
        this.boldness     = input.getFloatOr("Boldness", 0.5F);
        this.greed        = input.getFloatOr("Greed", 0.5F);
        this.sociability  = input.getFloatOr("Sociability", 0.5F);
        // Re-derive archetype and name (handles old saves that predate identity system)
        this.archetype   = FilcherArchetype.from(boldness, greed, sociability);
        this.filcherName = FilcherArchetype.nameFor(this.uuid.getLeastSignificantBits());
        // Override with saved values if present
        String savedName = input.getStringOr("FilcherName", "");
        if (!savedName.isEmpty()) this.filcherName = savedName;
        String savedArch = input.getStringOr("Archetype", "");
        if (!savedArch.isEmpty()) {
            try { this.archetype = FilcherArchetype.valueOf(savedArch); } catch (IllegalArgumentException ignored) {}
        }
        String savedRole = input.getStringOr("Role", "");
        if (!savedRole.isEmpty()) {
            try { this.role = FilcherRole.valueOf(savedRole); } catch (IllegalArgumentException ignored) {}
        }
        String savedTarget = input.getStringOr("AssignedTargetItem", "");
        this.assignedTargetItem = savedTarget.isEmpty() ? null : savedTarget;
        int dx = input.getIntOr("DenX", Integer.MIN_VALUE);
        if (dx != Integer.MIN_VALUE) {
            this.denPos = new BlockPos(dx, input.getIntOr("DenY", 0), input.getIntOr("DenZ", 0));
        }
    }

    // ── Silence — filchers are stealthy; sounds fire via goal code ────────────

    @Override
    protected SoundEvent getAmbientSound() {
        return null;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return null;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return null;
    }
}
