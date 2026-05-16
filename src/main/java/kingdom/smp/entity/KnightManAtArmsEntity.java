package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class KnightManAtArmsEntity extends KnightEntity {

    @Override
    protected boolean usesShieldCombat() {
        return true;
    }

    public KnightManAtArmsEntity(EntityType<? extends KnightManAtArmsEntity> type, Level level) {
        super(type, level);
        this.equipMainhand(new ItemStack(Items.IRON_SWORD));
        this.equipOffhand(new ItemStack(Items.SHIELD));
        this.equipArmor(
            new ItemStack(Ironhold.KNIGHT_MAN_AT_ARMS_HELM.get()),
            new ItemStack(Ironhold.KNIGHT_MAN_AT_ARMS_CHEST.get()),
            new ItemStack(Ironhold.KNIGHT_MAN_AT_ARMS_LEGS.get()),
            new ItemStack(Ironhold.KNIGHT_MAN_AT_ARMS_BOOTS.get()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return KnightEntity.createAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.28)
            .add(Attributes.ATTACK_DAMAGE, 5.0)
            .add(Attributes.ARMOR, 6.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.1);
    }
}
