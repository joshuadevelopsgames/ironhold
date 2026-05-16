package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class KnightCrusaderEntity extends KnightEntity {

    @Override
    protected boolean usesShieldCombat() {
        return false;
    }

    @Override
    public int getMaxHeadYRot() {
        return 180;
    }

    @Override
    protected void registerKnightCombatGoals() {
        this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.addGoal(2, new KnightCrusaderShieldGoal(this));
    }

    public KnightCrusaderEntity(EntityType<? extends KnightCrusaderEntity> type, Level level) {
        super(type, level);
        this.equipMainhand(new ItemStack(Items.IRON_AXE));
        this.equipOffhand(new ItemStack(Items.SHIELD));
        this.equipArmor(
            new ItemStack(Ironhold.KNIGHT_CRUSADER_HELM.get()),
            new ItemStack(Ironhold.KNIGHT_CRUSADER_CHEST.get()),
            new ItemStack(Ironhold.KNIGHT_CRUSADER_LEGS.get()),
            new ItemStack(Ironhold.KNIGHT_CRUSADER_BOOTS.get()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return KnightEntity.createAttributes()
            .add(Attributes.MAX_HEALTH, 60.0)
            .add(Attributes.MOVEMENT_SPEED, 0.26)
            .add(Attributes.ATTACK_DAMAGE, 7.0)
            .add(Attributes.ARMOR, 10.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }
}
