package kingdom.smp.entity;

import kingdom.smp.Ironhold;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public class KnightGoldEntity extends KnightEntity {

    public KnightGoldEntity(EntityType<? extends KnightGoldEntity> type, Level level) {
        super(type, level);
        this.equipMainhand(new ItemStack(Items.DIAMOND_SWORD));
        this.equipArmor(
            new ItemStack(kingdom.smp.ModItems.KNIGHT_GOLD_HELM.get()),
            new ItemStack(kingdom.smp.ModItems.KNIGHT_GOLD_CHEST.get()),
            new ItemStack(kingdom.smp.ModItems.KNIGHT_GOLD_LEGS.get()),
            new ItemStack(kingdom.smp.ModItems.KNIGHT_GOLD_BOOTS.get()));
    }

    public static AttributeSupplier.Builder createAttributes() {
        return KnightEntity.createAttributes()
            .add(Attributes.MAX_HEALTH, 100.0)
            .add(Attributes.MOVEMENT_SPEED, 0.22)
            .add(Attributes.ATTACK_DAMAGE, 14.0)
            .add(Attributes.ARMOR, 16.0)
            .add(Attributes.KNOCKBACK_RESISTANCE, 0.8);
    }
}
