package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class KnightGothicEntity extends KnightEntity {

    public KnightGothicEntity(EntityType<? extends KnightGothicEntity> type, Level level) {
        super(type, level);
        this.equipMainhand(new ItemStack(Items.DIAMOND_SWORD));
        this.equipArmor(
            new ItemStack(kingdom.smp.ModItems.KNIGHT_GOTHIC_HELM.get()),
            new ItemStack(kingdom.smp.ModItems.KNIGHT_GOTHIC_CHEST.get()),
            new ItemStack(kingdom.smp.ModItems.KNIGHT_GOTHIC_LEGS.get()),
            new ItemStack(kingdom.smp.ModItems.KNIGHT_GOTHIC_BOOTS.get()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return KnightEntity.createAttributes()
            .add(Attributes.MAX_HEALTH, 70.0)
            .add(Attributes.MOVEMENT_SPEED, 0.25)
            .add(Attributes.ATTACK_DAMAGE, 10.0)
            .add(Attributes.ARMOR, 14.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.6);
    }
}
