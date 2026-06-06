package kingdom.smp.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import kingdom.smp.Ironhold;
import kingdom.smp.effect.PlagueEffect;
import net.minecraft.core.BlockPos;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.AvoidEntityGoal;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Optional;

/**
 * Rat — passive vermin (Black Rat variant: rare, hostile, plague-spreader).
 *
 * <h2>Variants</h2>
 * Spawn-time roll: 7% become a Black Rat — darker texture, +HP, +damage, aggressive on sight,
 * 50% bite chance to inflict {@link PlagueEffect}. Normal rats only attack via swarm and apply
 * plague at 5% per bite.
 *
 * <h2>Behaviors</h2>
 * <ul>
 *   <li><b>Pack swarm</b> — being hurt sets every nearby (8-block) rat's target to the attacker
 *       and emits a high-pitched squeak.</li>
 *   <li><b>Loot stealing</b> — picks up dropped items via vanilla {@code canPickUpLoot}; drops
 *       held items on death.</li>
 *   <li><b>Crop eating</b> — once per second on movement, decrements the age of any crop block
 *       it's standing on.</li>
 *   <li><b>Tameable</b> — feed cheese (3 successful feeds; ⅓ chance each). Tame rats follow
 *       owner, can be sat, can be put on shoulder.</li>
 * </ul>
 */
public class RatEntity extends TamableAnimal implements GeoEntity {

    private static final EntityDataAccessor<Boolean> DATA_BLACK_RAT =
        SynchedEntityData.defineId(RatEntity.class, EntityDataSerializers.BOOLEAN);

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final int BREED_COOLDOWN_TICKS = 2400; // 2 minutes
    private static final double PARTNER_RADIUS = 8.0;
    private static final double FOOD_RADIUS = 8.0;
    private static final int CROP_SCAN_RADIUS = 4;
    private static final int LOCAL_RAT_CAP = 16;
    private static final double LOCAL_CAP_RADIUS = 16.0;

    private int eatCropCooldown;
    private int feedingCount;
    private int breedCooldown;
    @Nullable
    private BlockPos nestPos;

    public RatEntity(EntityType<? extends RatEntity> type, Level level) {
        super(type, level);
        this.setCanPickUpLoot(true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return TamableAnimal.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 4.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 1.0)
            .add(Attributes.FOLLOW_RANGE, 16.0);
    }

    /** Natural spawns are restricted to nighttime AND within 48 blocks of a village PoI
     *  (HOME/MEETING markers — i.e. inside or right next to an active village). Spawn eggs,
     *  structures, breeding, and commands bypass these checks. */
    public static boolean checkRatSpawnRules(EntityType<RatEntity> type, ServerLevelAccessor level,
                                              EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) {
            long t = level.getLevel().getOverworldClockTime() % 24000L;
            boolean isNight = (t >= 13000L && t <= 23000L);
            boolean nearVill = nearVillage(level, pos);
            boolean isDark = level.getRawBrightness(pos, 0) <= 7;
            
            // Spawn if it's dark (like a monster) OR near a village at night
            if (!isDark && !(isNight && nearVill)) {
                return false;
            }
            
            // Strictly cap natural spawns in the immediate area so they don't monopolize the server's mob cap
            if (level.getEntitiesOfClass(RatEntity.class, new AABB(pos).inflate(24.0)).size() >= 6) {
                return false;
            }
            
            // Allow spawning on any solid block
            return level.getBlockState(pos.below()).canOcclude();
        }
        return true;
    }

    private static boolean nearVillage(ServerLevelAccessor level, BlockPos pos) {
        net.minecraft.world.entity.ai.village.poi.PoiManager poi = level.getLevel().getPoiManager();
        return poi.getCountInRange(
            holder -> holder.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.HOME)
                   || holder.is(net.minecraft.world.entity.ai.village.poi.PoiTypes.MEETING),
            pos, 48,
            net.minecraft.world.entity.ai.village.poi.PoiManager.Occupancy.ANY) > 0;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_BLACK_RAT, false);
    }

    public boolean isBlackRat() {
        return this.entityData.get(DATA_BLACK_RAT);
    }

    public void setBlackRat(boolean black) {
        this.entityData.set(DATA_BLACK_RAT, black);
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                         EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData);
        if (level.getRandom().nextFloat() < 0.07F) {
            setBlackRat(true);
        }
        applyVariantAttributes();
        return result;
    }

    private void applyVariantAttributes() {
        if (isBlackRat()) {
            this.getAttribute(Attributes.MAX_HEALTH).setBaseValue(8.0);
            this.setHealth(8.0F);
            this.getAttribute(Attributes.ATTACK_DAMAGE).setBaseValue(2.0);
        }
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        this.goalSelector.addGoal(1, new SitWhenOrderedToGoal(this));
        this.goalSelector.addGoal(2, new PanicGoal(this, 1.6));
        
        // Flee cats and ocelots on sight
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this,
            net.minecraft.world.entity.animal.feline.Cat.class, 8.0F, 1.5, 1.8));
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this,
            net.minecraft.world.entity.animal.feline.Ocelot.class, 8.0F, 1.5, 1.8));
            
        // Flee players on sight (all untamed rats)
        this.goalSelector.addGoal(2, new AvoidEntityGoal<>(this,
            Player.class, 8.0F, 1.5, 1.8, (entity) -> !this.isTame()));
            
        // Flee the sun / hide in darkness
        this.goalSelector.addGoal(3, new net.minecraft.world.entity.ai.goal.FleeSunGoal(this, 1.6));

        this.goalSelector.addGoal(3, new MeleeAttackGoal(this, 1.4, false));
        this.goalSelector.addGoal(4, new RatHoardGoal());
        this.goalSelector.addGoal(5, new FollowOwnerGoal(this, 1.0, 8.0F, 3.0F));
        this.goalSelector.addGoal(6, new WaterAvoidingRandomStrollGoal(this, 1.0));
        this.goalSelector.addGoal(7, new LookAtPlayerGoal(this, Player.class, 4.0F));
        this.goalSelector.addGoal(8, new RandomLookAroundGoal(this));

        // Swarm defense - Black Rats no longer attack on sight
        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
    }

    class RatHoardGoal extends net.minecraft.world.entity.ai.goal.Goal {
        public RatHoardGoal() {
            this.setFlags(java.util.EnumSet.of(net.minecraft.world.entity.ai.goal.Goal.Flag.MOVE));
        }

        @Override
        public boolean canUse() {
            return !RatEntity.this.getMainHandItem().isEmpty() && RatEntity.this.nestPos != null;
        }

        @Override
        public void tick() {
            if (RatEntity.this.nestPos != null) {
                double dist = RatEntity.this.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(RatEntity.this.nestPos));
                if (dist > 4.0) {
                    RatEntity.this.getNavigation().moveTo(RatEntity.this.nestPos.getX(), RatEntity.this.nestPos.getY(), RatEntity.this.nestPos.getZ(), 1.2);
                } else {
                    if (RatEntity.this.level() instanceof ServerLevel sl) {
                        RatEntity.this.spawnAtLocation(sl, RatEntity.this.getMainHandItem());
                    }
                    RatEntity.this.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                    RatEntity.this.getNavigation().stop();
                }
            }
        }
    }

    @Override
    public boolean isFood(ItemStack stack) {
        return stack.is(kingdom.smp.ModItems.CHEESE_WEDGE.get());
    }

    @Nullable
    @Override
    public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return null;
    }

    @Override
    public boolean canFallInLove() {
        return false;
    }

    // ── Combat: bite + plague + swarm alert ───────────────────────────────────

    @Override
    public boolean doHurtTarget(ServerLevel level, Entity target) {
        boolean ok = super.doHurtTarget(level, target);
        if (ok && target instanceof LivingEntity victim && kingdom.smp.ModEffects.PLAGUE_EFFECT != null) {
            boolean isVillager = victim instanceof net.minecraft.world.entity.npc.villager.AbstractVillager;
            // Plague-on-bite tier: Black-Rat-vs-player is the dangerous case. Villager bites are
            // far less infectious to keep village die-offs from runaway-cascading.
            float plagueChance;
            if (isVillager) {
                plagueChance = isBlackRat() ? 0.05F : 0.01F;
            } else {
                plagueChance = isBlackRat() ? 0.5F : 0.05F;
            }
            if (level.getRandom().nextFloat() < plagueChance && kingdom.smp.effect.PlagueHandler.canReceivePlague(victim)) {
                victim.addEffect(new MobEffectInstance(kingdom.smp.ModEffects.PLAGUE_EFFECT,
                    PlagueEffect.TOTAL_DURATION_TICKS, 0, false, false, true), this);
            }
            squeakAndAlert(target);
        }
        return ok;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        boolean wasHurt = super.hurtServer(level, source, amount);
        if (wasHurt && this.isAlive()) {
            squeakAndAlert(source.getEntity());
        }
        return wasHurt;
    }

    private void squeakAndAlert(@Nullable Entity attacker) {
        // High-pitch squeak
        this.level().playSound(null, this.getX(), this.getY(), this.getZ(),
            SoundEvents.RABBIT_HURT, this.getSoundSource(), 0.6F, 2.0F);
        if (attacker == null) return;
        AABB area = this.getBoundingBox().inflate(8.0);
        for (RatEntity rat : this.level().getEntitiesOfClass(RatEntity.class, area)) {
            if (rat == this) continue;
            if (rat.getTarget() != null) continue;
            if (rat.isOwnedBy(attacker instanceof Player p ? p : null)) continue; // don't turn on owner
            if (attacker instanceof LivingEntity le) {
                rat.setTarget(le);
            }
        }
    }

    // ── Periodic per-tick behavior: crop eating ───────────────────────────────

    @Override
    public void aiStep() {
        super.aiStep();
        if (!this.level().isClientSide()) {
            // Passive flea aura for Black Rats - staggered by entity ID for TPS optimization
            if (this.isBlackRat() && (this.tickCount + this.getId()) % 20 == 0 && kingdom.smp.ModEffects.PLAGUE_EFFECT != null) {
                if (this.level() instanceof ServerLevel sl) {
                    sl.sendParticles(kingdom.smp.ModParticles.PLAGUE_FLEA.get(),
                        this.getX(), this.getY() + 0.25, this.getZ(),
                        3, 0.35, 0.18, 0.35, 0.03);
                }
                AABB aura = this.getBoundingBox().inflate(3.5);
                for (LivingEntity le : this.level().getEntitiesOfClass(LivingEntity.class, aura)) {
                    if ((le instanceof Player || le instanceof net.minecraft.world.entity.npc.villager.AbstractVillager || le instanceof net.minecraft.world.entity.animal.Animal) && !(le instanceof RatEntity)) {
                        if (kingdom.smp.effect.PlagueHandler.canReceivePlague(le)) {
                            // 5% chance per second to passively spread plague
                            if (this.random.nextFloat() < 0.05F) {
                                le.addEffect(new MobEffectInstance(kingdom.smp.ModEffects.PLAGUE_EFFECT,
                                    PlagueEffect.TOTAL_DURATION_TICKS, 0, false, false, true), this);
                            }
                        }
                    }
                }
            }

            // Occasional chest thievery - staggered by entity ID for TPS
            if ((this.tickCount + this.getId()) % 60 == 0 && this.getMainHandItem().isEmpty() && this.random.nextFloat() < 0.15F) {
                tryStealFromChest();
            }

            if (this.breedCooldown > 0) this.breedCooldown--;
            if (--eatCropCooldown <= 0) {
                tryEatCrop();
                eatCropCooldown = 20;
                if (this.level() instanceof ServerLevel sl) {
                    tryBreed(sl);
                }
            }
        }
    }

    private void tryStealFromChest() {
        BlockPos here = this.blockPosition();
        for (int dx = -2; dx <= 2; dx++) {
            for (int dy = -1; dy <= 1; dy++) {
                for (int dz = -2; dz <= 2; dz++) {
                    BlockPos p = here.offset(dx, dy, dz);
                    net.minecraft.world.level.block.entity.BlockEntity be = this.level().getBlockEntity(p);
                    if (be instanceof net.minecraft.world.Container container) {
                        for (int i = 0; i < container.getContainerSize(); i++) {
                            ItemStack stack = container.getItem(i);
                            if (!stack.isEmpty() && stack.has(net.minecraft.core.component.DataComponents.FOOD)) {
                                ItemStack stolen = stack.split(1);
                                container.setItem(i, stack);
                                
                                this.level().playSound(null, p, net.minecraft.sounds.SoundEvents.WOOD_BREAK, net.minecraft.sounds.SoundSource.BLOCKS, 0.5F, 2.0F);
                                
                                net.minecraft.world.entity.item.ItemEntity item = new net.minecraft.world.entity.item.ItemEntity(
                                    this.level(), p.getX() + 0.5, p.getY() + 1.0, p.getZ() + 0.5, stolen
                                );
                                item.setDeltaMovement((this.random.nextDouble() - 0.5) * 0.3, 0.2, (this.random.nextDouble() - 0.5) * 0.3);
                                this.level().addFreshEntity(item);
                                
                                return; // only steal one item
                            }
                        }
                    }
                }
            }
        }
    }

    /** Once-per-second roll. Conditions stack: must have a partner, a food signal, and not be at
     *  the local soft cap. Black Rats roll at 1.3× and have a 15% chance to spawn another Black Rat. */
    private void tryBreed(ServerLevel level) {
        if (this.isBaby() || this.breedCooldown > 0) return;

        // Soft cap — don't breed if the area is already saturated.
        AABB capArea = this.getBoundingBox().inflate(LOCAL_CAP_RADIUS);
        int localRats = level.getEntitiesOfClass(RatEntity.class, capArea, e -> true).size();
        if (localRats >= LOCAL_RAT_CAP) return;

        // Need at least one other adult, off-cooldown rat as a partner.
        AABB partnerArea = this.getBoundingBox().inflate(PARTNER_RADIUS);
        RatEntity partner = level.getEntitiesOfClass(RatEntity.class, partnerArea, e -> e != this
            && !e.isBaby()
            && e.breedCooldown <= 0).stream().findFirst().orElse(null);
        if (partner == null) return;

        // Food signal — cheap checks first; expensive crop scan last.
        if (!hasFoodSignal(level)) return;

        // Roll. Black Rats are slightly more prolific (1.3×).
        float chance = isBlackRat() ? 0.013F : 0.01F;
        if (level.getRandom().nextFloat() >= chance) return;

        spawnRatBaby(level, partner);
        this.breedCooldown = BREED_COOLDOWN_TICKS;
        partner.breedCooldown = BREED_COOLDOWN_TICKS;
    }

    private boolean hasFoodSignal(ServerLevel level) {
        AABB foodArea = this.getBoundingBox().inflate(FOOD_RADIUS);

        // Cheap: nearby cow / villager (settled food source)
        if (!level.getEntitiesOfClass(net.minecraft.world.entity.animal.cow.AbstractCow.class, foodArea).isEmpty()) {
            return true;
        }
        if (!level.getEntitiesOfClass(net.minecraft.world.entity.npc.villager.AbstractVillager.class, foodArea).isEmpty()) {
            return true;
        }

        // Cheap: dropped item
        if (!level.getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, foodArea).isEmpty()) {
            return true;
        }

        // Black-rat-only: a plagued mob (stage 1+) is "food" too.
        if (isBlackRat() && kingdom.smp.ModEffects.PLAGUE_EFFECT != null) {
            for (LivingEntity le : level.getEntitiesOfClass(LivingEntity.class, foodArea)) {
                MobEffectInstance pl = le.getEffect(kingdom.smp.ModEffects.PLAGUE_EFFECT);
                if (pl != null && PlagueEffect.stageOf(pl) >= 1) return true;
            }
        }

        // Expensive: scan for mature crop blocks within a small box.
        BlockPos here = this.blockPosition();
        for (int dx = -CROP_SCAN_RADIUS; dx <= CROP_SCAN_RADIUS; dx++) {
            for (int dz = -CROP_SCAN_RADIUS; dz <= CROP_SCAN_RADIUS; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    BlockPos p = here.offset(dx, dy, dz);
                    BlockState s = level.getBlockState(p);
                    if (s.getBlock() instanceof CropBlock cb && cb.isMaxAge(s)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void spawnRatBaby(ServerLevel level, RatEntity partner) {
        RatEntity baby = kingdom.smp.ModEntities.RAT.get().create(level, EntitySpawnReason.BREEDING);
        if (baby == null) return;
        baby.setBaby(true);
        // Black-rat inheritance: only if at least one parent is Black, and roll 15%.
        boolean parentIsBlack = isBlackRat() || partner.isBlackRat();
        if (parentIsBlack && level.getRandom().nextFloat() < 0.15F) {
            baby.setBlackRat(true);
            baby.applyVariantAttributes();
        }
        baby.snapTo(getX(), getY(), getZ(), 0, 0);
        level.addFreshEntity(baby);
    }

    private void tryEatCrop() {
        BlockPos at = this.blockPosition();
        BlockState state = this.level().getBlockState(at);
        if (state.getBlock() instanceof CropBlock crop) {
            int age = crop.getAge(state);
            if (age > 0) {
                BlockState reverted = state.setValue(CropBlock.AGE, Math.max(0, age - 1));
                this.level().setBlock(at, reverted, 3);
                this.level().levelEvent(2001, at, Block.getId(state));
            }
        }
    }

    // ── Loot stealing — record nest pos on first pickup ──────────────────────

    @Override
    public boolean canHoldItem(ItemStack stack) {
        return true; // pick up anything
    }

    @Override
    protected void pickUpItem(ServerLevel level, net.minecraft.world.entity.item.ItemEntity itemEntity) {
        if (this.nestPos == null) {
            this.nestPos = this.blockPosition();
        }
        super.pickUpItem(level, itemEntity);
    }

    // ── Tame interaction ─────────────────────────────────────────────────────

    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);

        if (this.isTame()) {
            if (this.isOwnedBy(player)) {
                if (held.is(kingdom.smp.ModItems.CHEESE_WEDGE.get()) && this.getHealth() < this.getMaxHealth()) {
                    if (!player.getAbilities().instabuild) held.shrink(1);
                    this.heal(2.0F);
                    return InteractionResult.SUCCESS;
                }
                // Empty hand → toggle sit
                if (held.isEmpty()) {
                    this.setOrderedToSit(!this.isOrderedToSit());
                    this.jumping = false;
                    this.navigation.stop();
                    this.setTarget(null);
                    return InteractionResult.SUCCESS;
                }
            }
            return super.mobInteract(player, hand);
        }

        // Untamed: feed cheese
        if (held.is(kingdom.smp.ModItems.CHEESE_WEDGE.get()) && !isBlackRat()) {
            if (!player.getAbilities().instabuild) held.shrink(1);
            this.feedingCount++;
            if (this.feedingCount >= 3) {
                if (this.random.nextFloat() < 0.5F) {
                    this.tame(player);
                    this.navigation.stop();
                    this.setTarget(null);
                    this.setOrderedToSit(true);
                    this.level().broadcastEntityEvent(this, (byte) 7);
                } else {
                    this.feedingCount = 2;
                    this.level().broadcastEntityEvent(this, (byte) 6);
                }
            }
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    // ── Save / load ──────────────────────────────────────────────────────────

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putBoolean("BlackRat", isBlackRat());
        out.putInt("FedCount", this.feedingCount);
        out.putInt("BreedCooldown", this.breedCooldown);
        if (this.nestPos != null) {
            out.putInt("NestX", this.nestPos.getX());
            out.putInt("NestY", this.nestPos.getY());
            out.putInt("NestZ", this.nestPos.getZ());
        }
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setBlackRat(in.getBooleanOr("BlackRat", false));
        this.feedingCount = in.getIntOr("FedCount", 0);
        this.breedCooldown = in.getIntOr("BreedCooldown", 0);
        Optional<Integer> nx = in.getInt("NestX");
        Optional<Integer> ny = in.getInt("NestY");
        Optional<Integer> nz = in.getInt("NestZ");
        if (nx.isPresent() && ny.isPresent() && nz.isPresent()) {
            this.nestPos = new BlockPos(nx.get(), ny.get(), nz.get());
        }
        applyVariantAttributes();
    }

    // ── Animations / sounds ──────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<RatEntity>("main", 5, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationTest<RatEntity> test) {
        if (test.isMoving()) {
            test.controller().setAnimation(WALK_ANIM);
        } else {
            test.controller().setAnimation(IDLE_ANIM);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.RABBIT_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.RABBIT_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.RABBIT_DEATH;
    }

    @Override
    public int getMaxSpawnClusterSize() {
        return 4;
    }
}
