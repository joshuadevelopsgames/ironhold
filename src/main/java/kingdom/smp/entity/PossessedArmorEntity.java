package kingdom.smp.entity;

import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.Registries;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterial;
import net.minecraft.world.item.equipment.trim.TrimPattern;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Possessed Armor — an invisible entity wearing netherite armor, floating
 * slightly off the ground, wielding a netherite sword. Every 5th attack
 * is a dash attack that lunges toward the target.
 */
public class PossessedArmorEntity extends Monster {

    private int attackCount = 0;
    private static final int DASH_EVERY = 5;
    private static final double DASH_SPEED = 1.8;

    /** Mansion-spawned variants (rerolled from a Vindicator) drop a Wraith's Sigil on death. */
    private boolean dropsWraithsSigil = false;

    public PossessedArmorEntity(EntityType<? extends PossessedArmorEntity> type, Level level) {
        super(type, level);
        ItemStack helmet = new ItemStack(Items.NETHERITE_HELMET);
        ItemStack chestplate = new ItemStack(Items.NETHERITE_CHESTPLATE);

        // Apply a random matching trim to both armor pieces
        applyRandomTrim(level, helmet, chestplate);

        this.setItemSlot(EquipmentSlot.HEAD, helmet);
        this.setItemSlot(EquipmentSlot.CHEST, chestplate);
        this.setItemSlot(EquipmentSlot.MAINHAND, new ItemStack(Items.NETHERITE_SWORD));
        this.setDropChance(EquipmentSlot.HEAD, 0.0F);
        this.setDropChance(EquipmentSlot.CHEST, 0.0F);
        this.setDropChance(EquipmentSlot.MAINHAND, 0.0F);
    }

    private static void applyRandomTrim(Level level, ItemStack... pieces) {
        Registry<TrimPattern> patterns = level.registryAccess().lookup(Registries.TRIM_PATTERN).orElse(null);
        Registry<TrimMaterial> materials = level.registryAccess().lookup(Registries.TRIM_MATERIAL).orElse(null);
        if (patterns == null || materials == null) return;

        List<Holder.Reference<TrimPattern>> patternList = patterns.listElements().toList();
        List<Holder.Reference<TrimMaterial>> materialList = materials.listElements().toList();
        if (patternList.isEmpty() || materialList.isEmpty()) return;

        RandomSource rng = level.getRandom();
        Holder<TrimPattern> pattern = patternList.get(rng.nextInt(patternList.size()));
        Holder<TrimMaterial> material = materialList.get(rng.nextInt(materialList.size()));
        ArmorTrim trim = new ArmorTrim(material, pattern);

        for (ItemStack piece : pieces) {
            piece.set(DataComponents.TRIM, trim);
        }
    }

    public void setDropsWraithsSigil(boolean drops) {
        this.dropsWraithsSigil = drops;
    }

    @Override
    protected void dropCustomDeathLoot(net.minecraft.server.level.ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);
        if (dropsWraithsSigil) {
            this.spawnAtLocation(level, new ItemStack(kingdom.smp.ModItems.WRAITHS_SIGIL.get()));
        }
    }

    @Override
    public void addAdditionalSaveData(net.minecraft.world.level.storage.ValueOutput output) {
        super.addAdditionalSaveData(output);
        output.putBoolean("DropsWraithsSigil", dropsWraithsSigil);
    }

    @Override
    public void readAdditionalSaveData(net.minecraft.world.level.storage.ValueInput input) {
        super.readAdditionalSaveData(input);
        this.dropsWraithsSigil = input.getBooleanOr("DropsWraithsSigil", false);
    }

    // ── Attributes ───────────────────────────────────────────────────────────

    public static AttributeSupplier.Builder createAttributes() {
        return Monster.createMonsterAttributes()
            .add(Attributes.MAX_HEALTH, 60.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 9.0)
            .add(Attributes.FOLLOW_RANGE, 24.0)
            .add(Attributes.ARMOR, 12.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }

    // ── Goals ────────────────────────────────────────────────────────────────

    @Override
    protected void registerGoals() {
        this.goalSelector.addGoal(1, new PossessedArmorAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 0.8));
        this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0F));
        this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));

        this.targetSelector.addGoal(1, new HurtByTargetGoal(this));
        this.targetSelector.addGoal(2, new NearestAttackableTargetGoal<>(this, Player.class, true));
    }

    // ── Floating over water ─────────────────────────────────────────────────

    @Override
    public void tick() {
        super.tick();

        // Spooky particles
        if (this.level().isClientSide() && this.tickCount % 5 == 0) {
            this.level().addParticle(ParticleTypes.SOUL,
                this.getRandomX(0.5), this.getY() + 0.1, this.getRandomZ(0.5),
                0, 0.02, 0);
        }
    }

    @Override
    public boolean isPushedByFluid() {
        return false;
    }

    // ── No footstep sounds ─────────────────────────────────────────────────

    @Override
    protected void playStepSound(net.minecraft.core.BlockPos pos, net.minecraft.world.level.block.state.BlockState state) {
        // Silent — possessed armor glides, not walks
    }

    // ── Dash attack logic ────────────────────────────────────────────────────

    /** Called after a successful melee hit. Every 5th hit triggers a dash. */
    public void onSuccessfulAttack(LivingEntity target) {
        attackCount++;
        if (attackCount >= DASH_EVERY) {
            attackCount = 0;
            performDashAttack(target);
        }
    }

    private void performDashAttack(LivingEntity target) {
        Vec3 dir = target.position().subtract(this.position()).normalize();
        this.setDeltaMovement(dir.x * DASH_SPEED, 0.3, dir.z * DASH_SPEED);
        this.hurtMarked = true;

        this.level().playSound(null, this.blockPosition(),
            SoundEvents.PHANTOM_BITE, this.getSoundSource(), 1.0F, 0.7F);

        // Burst of soul particles for the dash
        if (!this.level().isClientSide()) {
            var sl = (net.minecraft.server.level.ServerLevel) this.level();
            sl.sendParticles(ParticleTypes.SOUL_FIRE_FLAME,
                this.getX(), this.getY() + 0.5, this.getZ(),
                12, 0.3, 0.2, 0.3, 0.05);
        }
    }

    // Invisible body — the renderer skips the body model but still renders
    // armor layers and held items, creating the floating armor effect.
    @Override
    public boolean isInvisible() {
        return true;
    }

    // ── Sounds ───────────────────────────────────────────────────────────────

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.SOUL_ESCAPE.value();
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ANVIL_LAND;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.SHIELD_BREAK.value();
    }

    // ── Custom melee goal that triggers dash ─────────────────────────────────

    private static class PossessedArmorAttackGoal extends MeleeAttackGoal {
        private final PossessedArmorEntity armor;

        PossessedArmorAttackGoal(PossessedArmorEntity mob, double speed, boolean followUnseen) {
            super(mob, speed, followUnseen);
            this.armor = mob;
        }

        @Override
        protected void checkAndPerformAttack(LivingEntity target) {
            if (this.canPerformAttack(target)) {
                this.resetAttackCooldown();
                this.mob.swing(this.mob.getUsedItemHand());
                if (this.mob.level() instanceof net.minecraft.server.level.ServerLevel sl) {
                    this.mob.doHurtTarget(sl, target);
                }
                armor.onSuccessfulAttack(target);
            }
        }
    }
}
