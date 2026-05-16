package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Jouster knight — wears a Stechhelm (frog-mouth jousting helm) over platemail.
 * Tournament-tier knight, ceremonially heavy but with mobility comparable to
 * the standard Crusader.
 */
public class KnightJousterEntity extends KnightEntity {

    @Override
    protected void registerKnightCombatGoals() {
        this.goalSelector.addGoal(1, new KnightJousterChargeGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
    }

    public KnightJousterEntity(EntityType<? extends KnightJousterEntity> type, Level level) {
        super(type, level);
        this.equipMainhand(new ItemStack(Items.DIAMOND_SWORD));
        this.equipArmor(
            new ItemStack(Ironhold.KNIGHT_JOUSTER_HELM.get()),
            new ItemStack(Ironhold.KNIGHT_JOUSTER_CHEST.get()),
            new ItemStack(Ironhold.KNIGHT_JOUSTER_LEGS.get()),
            new ItemStack(Ironhold.KNIGHT_JOUSTER_BOOTS.get()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return KnightEntity.createAttributes()
            .add(Attributes.MAX_HEALTH, 120.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, 14.0)
            .add(Attributes.ARMOR, 14.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.7);
    }
}
