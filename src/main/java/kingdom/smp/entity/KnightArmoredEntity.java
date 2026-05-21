package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class KnightArmoredEntity extends KnightEntity {

    public KnightArmoredEntity(EntityType<? extends KnightArmoredEntity> type, Level level) {
        super(type, level);
        this.equipMainhand(new ItemStack(Items.IRON_SWORD));
        this.equipArmor(
            new ItemStack(kingdom.smp.ModItems.KNIGHT_ARMORED_HELM.get()),
            new ItemStack(kingdom.smp.ModItems.KNIGHT_ARMORED_CHEST.get()),
            new ItemStack(kingdom.smp.ModItems.KNIGHT_ARMORED_LEGS.get()),
            new ItemStack(kingdom.smp.ModItems.KNIGHT_ARMORED_BOOTS.get()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return KnightEntity.createAttributes()
            .add(Attributes.MAX_HEALTH, 50.0)
            .add(Attributes.MOVEMENT_SPEED, 0.24)
            .add(Attributes.ATTACK_DAMAGE, 8.0)
            .add(Attributes.ARMOR, 12.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.4);
    }
}
