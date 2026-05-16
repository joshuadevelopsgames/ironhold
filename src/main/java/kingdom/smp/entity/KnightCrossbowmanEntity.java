package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.CrossbowAttackMob;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class KnightCrossbowmanEntity extends KnightEntity implements CrossbowAttackMob {

    private static final EntityDataAccessor<Boolean> IS_CHARGING_CROSSBOW =
        SynchedEntityData.defineId(KnightCrossbowmanEntity.class, EntityDataSerializers.BOOLEAN);

    public KnightCrossbowmanEntity(EntityType<? extends KnightCrossbowmanEntity> type, Level level) {
        super(type, level);
        this.equipMainhand(new ItemStack(Items.CROSSBOW));
        this.equipOffhand(new ItemStack(Items.ARROW, 64));
        this.equipArmor(
            new ItemStack(Ironhold.KNIGHT_CROSSBOWMAN_HELM.get()),
            new ItemStack(Ironhold.KNIGHT_CROSSBOWMAN_CHEST.get()),
            new ItemStack(Ironhold.KNIGHT_CROSSBOWMAN_LEGS.get()),
            new ItemStack(Ironhold.KNIGHT_CROSSBOWMAN_BOOTS.get()));
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(IS_CHARGING_CROSSBOW, false);
    }

    @Override
    protected void registerKnightCombatGoals() {
        // No MeleeAttackGoal: it constantly moveTo(target) and fights ranged charging/kiting (same MOVE flag).
        this.goalSelector.addGoal(1, new KnightCrossbowRangedGoal(this, 1.0D, 20.0F));
    }

    @Override
    public ItemStack getProjectile(ItemStack weapon) {
        if (weapon.is(Items.CROSSBOW)) {
            return new ItemStack(Items.ARROW);
        }
        return super.getProjectile(weapon);
    }

    @Override
    public boolean canUseNonMeleeWeapon(ItemStack stack) {
        return stack.is(Items.CROSSBOW);
    }

    public boolean isChargingCrossbow() {
        return this.entityData.get(IS_CHARGING_CROSSBOW);
    }

    @Override
    public void setChargingCrossbow(boolean charging) {
        this.entityData.set(IS_CHARGING_CROSSBOW, charging);
    }

    @Override
    public void onCrossbowAttackPerformed() {
        this.noActionTime = 0;
    }

    @Override
    public void performRangedAttack(LivingEntity target, float power) {
        this.performCrossbowAttack(this, 1.6F);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return KnightEntity.createAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.26)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.FOLLOW_RANGE, 36.0)
            .add(Attributes.ARMOR, 4.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }
}
