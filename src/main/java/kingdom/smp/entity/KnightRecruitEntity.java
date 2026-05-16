package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class KnightRecruitEntity extends KnightEntity {

    @Override
    protected void registerKnightCombatGoals() {
        this.goalSelector.addGoal(1, new KnightLowHpRetreatGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
    }

    public KnightRecruitEntity(EntityType<? extends KnightRecruitEntity> type, Level level) {
        super(type, level);
        this.equipMainhand(new ItemStack(Items.STONE_SWORD));
        this.equipArmor(
            new ItemStack(Ironhold.KNIGHT_RECRUIT_HELM.get()),
            new ItemStack(Ironhold.KNIGHT_RECRUIT_CHEST.get()),
            new ItemStack(Ironhold.KNIGHT_RECRUIT_LEGS.get()),
            new ItemStack(Ironhold.KNIGHT_RECRUIT_BOOTS.get()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return KnightEntity.createAttributes()
            .add(Attributes.MAX_HEALTH, 20.0)
            .add(Attributes.MOVEMENT_SPEED, 0.30)
            .add(Attributes.ATTACK_DAMAGE, 3.0)
            .add(Attributes.ARMOR, 2.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.0);
    }
}
