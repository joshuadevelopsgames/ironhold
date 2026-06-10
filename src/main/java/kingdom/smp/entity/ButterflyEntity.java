package kingdom.smp.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.RandomSource;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.control.FlyingMoveControl;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.TemptGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomFlyingGoal;
import net.minecraft.world.entity.ai.navigation.FlyingPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

/**
 * Butterfly — a fragile, passive {@link net.minecraft.world.entity.MobCategory#AMBIENT}
 * flyer. One entity class drives all {@link ButterflySpecies} variants via a synced
 * species id (texture + bait stats). Flight uses the Bee/Allay {@link FlyingMoveControl}
 * + {@link FlyingPathNavigation} stack (same as {@link GargoyleEntity}); gravity is kept
 * off so it hovers and drifts.
 *
 * <p>Caught with the Butterfly Net as a loose butterfly item. Players can then put
 * it into a butterfly jar to create fishing bait.
 */
public class ButterflyEntity extends PathfinderMob implements GeoEntity {

    private static final EntityDataAccessor<String> DATA_SPECIES =
        SynchedEntityData.defineId(ButterflyEntity.class, EntityDataSerializers.STRING);

    private static final RawAnimation FLY = RawAnimation.begin().thenLoop("fly");
    private static final RawAnimation IDLE = RawAnimation.begin().thenLoop("idle");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public ButterflyEntity(EntityType<? extends ButterflyEntity> type, Level level) {
        super(type, level);
        this.moveControl = new FlyingMoveControl(this, 10, true);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 2.0)
            .add(Attributes.MOVEMENT_SPEED, 0.3)
            .add(Attributes.FLYING_SPEED, 0.4)
            .add(Attributes.FOLLOW_RANGE, 16.0)
            // Required by the TemptGoal (it reads minecraft:tempt_range) — without it the
            // first AI tick throws "Can't find attribute minecraft:tempt_range".
            .add(Attributes.TEMPT_RANGE, 8.0);
    }

    /**
     * Natural spawns. In the Overworld: surface, daytime-ish light (the meadow species).
     * In the Nether / End: no daylight requirement — those species are luminous and the
     * biome modifier weights already gate how rare they are. Spawn eggs / commands bypass.
     */
    public static boolean checkButterflySpawnRules(EntityType<ButterflyEntity> type, ServerLevelAccessor level,
                                                   EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (reason == EntitySpawnReason.SPAWNER) return true;
        if (level.getLevel().dimension() == Level.OVERWORLD) {
            return level.getRawBrightness(pos, 0) >= 8 && pos.getY() >= level.getLevel().getSeaLevel() - 8;
        }
        return true;
    }

    @Override
    protected PathNavigation createNavigation(Level level) {
        FlyingPathNavigation nav = new FlyingPathNavigation(this, level);
        nav.setCanOpenDoors(false);
        nav.setCanFloat(true);
        return nav;
    }

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Tempted by its favourite flower/block — the encyclopedia's "Likes" line made real.
        // The predicate reads the live species so it tracks whatever this butterfly rolled.
        this.goalSelector.addGoal(1, new TemptGoal(this, 1.15,
            stack -> stack.is(this.getSpecies().likesItem()), false));
        this.goalSelector.addGoal(2, new WaterAvoidingRandomFlyingGoal(this, 1.0));
        this.goalSelector.addGoal(3, new LookAtPlayerGoal(this, Player.class, 6.0F));
        this.goalSelector.addGoal(4, new RandomLookAroundGoal(this));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_SPECIES, ButterflySpecies.MONARCH.id());
    }

    public ButterflySpecies getSpecies() {
        return ButterflySpecies.byId(this.entityData.get(DATA_SPECIES));
    }

    public void setSpecies(ButterflySpecies species) {
        this.entityData.set(DATA_SPECIES, species.id());
    }

    @Override
    public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, DifficultyInstance difficulty,
                                        EntitySpawnReason reason, @Nullable SpawnGroupData groupData) {
        SpawnGroupData result = super.finalizeSpawn(level, difficulty, reason, groupData);
        // Pick a species that actually lives in this biome, so the butterfly you catch matches
        // the encyclopedia's "Location" line (which is derived from the same biome key).
        setSpecies(pickSpeciesForBiome(level, this.blockPosition()));
        return result;
    }

    private static ButterflySpecies pickSpeciesForBiome(ServerLevelAccessor level, BlockPos pos) {
        RandomSource random = level.getRandom();
        Holder<Biome> biome = level.getBiome(pos);
        List<ButterflySpecies> local = new ArrayList<>(2);
        for (ButterflySpecies s : ButterflySpecies.values()) {
            if (biome.is(s.biome())) {
                local.add(s);
            }
        }
        if (local.isEmpty()) {
            // Spawn egg / command in a biome no species calls home — fall back to a weighted roll.
            return pickWeightedAny(random);
        }
        if (local.size() == 1) {
            return local.get(0);
        }
        // Biome hosts more than one species: weight toward the commoner (lower bait power).
        int total = 0;
        for (ButterflySpecies s : local) {
            total += weightFor(s);
        }
        int roll = random.nextInt(total);
        for (ButterflySpecies s : local) {
            roll -= weightFor(s);
            if (roll < 0) {
                return s;
            }
        }
        return local.get(local.size() - 1);
    }

    /** Commoner species (lower bait power) get a higher spawn weight. */
    private static int weightFor(ButterflySpecies s) {
        return Math.max(1, 60 - s.baitPower());
    }

    private static ButterflySpecies pickWeightedAny(RandomSource random) {
        ButterflySpecies[] all = ButterflySpecies.values();
        // Rejection sampling weighted by (60 - baitPower): low-power commons are far likelier.
        for (int i = 0; i < 8; i++) {
            ButterflySpecies s = all[random.nextInt(all.length)];
            if (random.nextInt(60) >= s.baitPower()) return s;
        }
        return ButterflySpecies.MONARCH;
    }

    @Override
    public void tick() {
        super.tick();
        // Keep gravity off so it hovers/drifts instead of sinking (same as the gargoyle flyer).
        this.setNoGravity(true);
    }

    @Override
    public boolean causeFallDamage(double fallDistance, float damageModifier, DamageSource damageSource) {
        return false; // a butterfly never takes fall damage
    }

    @Override
    public boolean onClimbable() {
        return false;
    }

    @Override
    public void addAdditionalSaveData(ValueOutput out) {
        super.addAdditionalSaveData(out);
        out.putString("Species", this.entityData.get(DATA_SPECIES));
    }

    @Override
    public void readAdditionalSaveData(ValueInput in) {
        super.readAdditionalSaveData(in);
        setSpecies(ButterflySpecies.byId(in.getStringOr("Species", ButterflySpecies.MONARCH.id())));
    }

    // ── GeckoLib ──────────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("movement", 4, this::movementPredicate));
    }

    private PlayState movementPredicate(AnimationTest<ButterflyEntity> test) {
        // Wings beat whenever aloft (which is almost always for a hovering flyer).
        if (test.isMoving() || !this.onGround()) {
            test.controller().setAnimation(FLY);
        } else {
            test.controller().setAnimation(IDLE);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
