package kingdom.smp.entity;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.OpenDoorGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.RandomStrollGoal;
import net.minecraft.world.entity.ai.navigation.AmphibiousPathNavigation;
import net.minecraft.world.entity.ai.navigation.PathNavigation;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import org.jspecify.annotations.Nullable;

import java.util.List;

public class KnightEntity extends Monster {

    /** Server-side: drop pursuit after this many ticks without refresh, if still far. */
    private int knightAggroUntilTick;

    public KnightEntity(EntityType<? extends KnightEntity> type, Level level) {
        super(type, level);
    }

    /** Ground + shallow/deep water pathing so knights can cross fluids instead of stalling on GroundPathNavigation. */
    @Override
    protected PathNavigation createNavigation(Level level) {
        AmphibiousPathNavigation navigation = new AmphibiousPathNavigation(this, level);
        navigation.setCanFloat(true);
        navigation.setCanOpenDoors(true);
        return navigation;
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 40.0)
            .add(Attributes.MOVEMENT_SPEED, 0.27)
            .add(Attributes.ATTACK_DAMAGE, 7.0)
            .add(Attributes.FOLLOW_RANGE, 24.0)
            .add(Attributes.ARMOR, 8.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.3);
    }

    protected boolean usesShieldCombat() {
        return false;
    }

    /**
     * Vanilla {@link net.minecraft.world.entity.Mob#getMaxHeadYRot()} is 75° — while pathfinding,
     * {@link net.minecraft.world.entity.Mob#clampHeadRotationToBody()} pulls the head toward the body's
     * movement yaw, which often points off the attacker and breaks shield angle checks.
     */
    @Override
    public int getMaxHeadYRot() {
        return this.usesShieldCombat() ? 180 : super.getMaxHeadYRot();
    }

    @Override
    protected void registerGoals() {
        registerKnightCombatGoals();
        registerKnightIdleGoals();
        registerKnightTargetingGoals();
    }

    protected void registerKnightCombatGoals() {
        // Melee before shield: shield tick can stop() navigation and fix yaw after melee moveTo runs.
        if (usesShieldCombat()) {
            this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
            this.goalSelector.addGoal(2, new KnightShieldBlockGoal(this));
        } else {
            this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
        }
    }

    protected void registerKnightIdleGoals() {
        this.goalSelector.addGoal(0, new FloatGoal(this));
        // Open + close doors when pathing through a village house.
        this.goalSelector.addGoal(3, new OpenDoorGoal(this, true));
        this.goalSelector.addGoal(5, new RandomStrollGoal(this, 0.85));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
    }

    protected void registerKnightTargetingGoals() {
        // Neutral to players by default. Aggro on hurt (and alert nearby knights).
        this.targetSelector.addGoal(1, new KnightAlertHurtGoal(this));
        // Village-defender role — attack hostile mobs on sight, like an iron golem.
        // Excludes creepers (no kamikaze fights) and fellow knights.
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(
            this, Mob.class, 5, false, false,
            (target, level) -> target instanceof Enemy
                && !(target instanceof Creeper)
                && !(target instanceof KnightEntity)));
    }

    @Override
    public void setTarget(@Nullable LivingEntity target) {
        super.setTarget(target);
        if (!this.level().isClientSide() && target != null) {
            this.knightAggroUntilTick = this.tickCount + 620;
        }
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide()) {
            return;
        }
        LivingEntity target = this.getTarget();
        if (target == null) {
            return;
        }
        if (!target.isAlive()) {
            this.setTarget(null);
            return;
        }
        double maxFollow = this.getAttributeValue(Attributes.FOLLOW_RANGE) * 1.65;
        if (this.distanceTo(target) > maxFollow) {
            this.setTarget(null);
            return;
        }
        if (this.tickCount > this.knightAggroUntilTick && this.distanceTo(target) > 22.0) {
            this.setTarget(null);
        }
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (source.getEntity() instanceof KnightEntity attacker && attacker != this) {
            return false;
        }
        boolean ok = super.hurtServer(level, source, amount);
        if (ok && this.getTarget() != null) {
            this.knightAggroUntilTick = this.tickCount + 420;
        }
        return ok;
    }

    protected void equipMainhand(ItemStack weapon) {
        this.setItemSlot(EquipmentSlot.MAINHAND, weapon);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.05F);
    }

    protected void equipOffhand(ItemStack offhand) {
        this.setItemSlot(EquipmentSlot.OFFHAND, offhand);
        this.setDropChance(EquipmentSlot.OFFHAND, 0.02F);
    }

    protected void equipArmor(ItemStack head, ItemStack chest, ItemStack legs, ItemStack feet) {
        this.setItemSlot(EquipmentSlot.HEAD, head);
        this.setItemSlot(EquipmentSlot.CHEST, chest);
        this.setItemSlot(EquipmentSlot.LEGS, legs);
        this.setItemSlot(EquipmentSlot.FEET, feet);
        this.setDropChance(EquipmentSlot.HEAD, 0.01F);
        this.setDropChance(EquipmentSlot.CHEST, 0.01F);
        this.setDropChance(EquipmentSlot.LEGS, 0.01F);
        this.setDropChance(EquipmentSlot.FEET, 0.01F);
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PLAYER_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PLAYER_DEATH;
    }

    /**
     * Variant of {@link HurtByTargetGoal} whose alert step searches the {@link KnightEntity} parent class instead of
     * {@code mob.getClass()}, so a hit on any one knight type rallies nearby knights of every subtype onto the attacker.
     */
    private static class KnightAlertHurtGoal extends HurtByTargetGoal {
        private final KnightEntity self;

        KnightAlertHurtGoal(KnightEntity self) {
            super(self);
            this.self = self;
            this.setAlertOthers();
        }

        @Override
        protected void alertOthers() {
            LivingEntity attacker = this.self.getLastHurtByMob();
            if (attacker == null) {
                return;
            }
            double range = this.getFollowDistance();
            AABB area = AABB.unitCubeFromLowerCorner(this.self.position()).inflate(range, 10.0, range);
            List<KnightEntity> nearby = this.self.level().getEntitiesOfClass(KnightEntity.class, area, k -> k != this.self);
            for (KnightEntity ally : nearby) {
                if (!ally.isAlliedTo(attacker)) {
                    this.alertOther(ally, attacker);
                }
            }
        }
    }
}
