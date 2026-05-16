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
 * Veteran knight — wears a Sallet over chainmail. Mid-high tier infantry,
 * faster than the heavily armored variants but tougher than recruits.
 */
public class KnightVeteranEntity extends KnightEntity {

    @Override
    protected void registerKnightCombatGoals() {
        this.goalSelector.addGoal(1, new KnightStrafeMeleeGoal(this));
        this.goalSelector.addGoal(2, new MeleeAttackGoal(this, 1.0, false));
    }

    public KnightVeteranEntity(EntityType<? extends KnightVeteranEntity> type, Level level) {
        super(type, level);
        this.equipMainhand(new ItemStack(Items.IRON_SWORD));
        this.equipArmor(
            new ItemStack(Ironhold.KNIGHT_VETERAN_HELM.get()),
            new ItemStack(Ironhold.KNIGHT_VETERAN_CHEST.get()),
            new ItemStack(Ironhold.KNIGHT_VETERAN_LEGS.get()),
            new ItemStack(Ironhold.KNIGHT_VETERAN_BOOTS.get()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return KnightEntity.createAttributes()
            .add(Attributes.MAX_HEALTH, 90.0)
            .add(Attributes.MOVEMENT_SPEED, 0.24)
            .add(Attributes.ATTACK_DAMAGE, 11.0)
            .add(Attributes.ARMOR, 12.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.5);
    }
}
