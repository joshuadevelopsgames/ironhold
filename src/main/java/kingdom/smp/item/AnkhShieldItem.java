package kingdom.smp.item;

import java.util.Optional;

import kingdom.smp.Ironhold;
import net.minecraft.core.component.DataComponents;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.BlocksAttacks;
import net.minecraft.world.level.block.entity.BannerPatternLayers;

/**
 * Shield with vanilla block behavior plus server-side magic mitigation and magic-projectile
 * interception (see {@link kingdom.smp.game.AnkhShieldHandler}).
 */
public class AnkhShieldItem extends ShieldItem {

    public AnkhShieldItem(net.minecraft.world.item.Item.Properties props) {
        super(props);
    }

    public static boolean isBlockingWithAnkh(LivingEntity entity) {
        if (!(entity instanceof Player player) || !player.isBlocking()) {
            return false;
        }
        ItemStack active = player.getUseItem();
        return active.is(kingdom.smp.ModItems.ANKH_SHIELD.get());
    }

    public static net.minecraft.world.item.Item.Properties applyAnkhProperties(net.minecraft.world.item.Item.Properties props) {
        return props
            .durability(336)
            .rarity(net.minecraft.world.item.Rarity.EPIC)
            .component(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
            .repairable(net.minecraft.tags.ItemTags.WOODEN_TOOL_MATERIALS)
            .equippableUnswappable(EquipmentSlot.MAINHAND)
            .delayedComponent(DataComponents.BLOCKS_ATTACKS, context ->
                new BlocksAttacks(
                    0.25F,
                    1.0F,
                    java.util.List.of(new BlocksAttacks.DamageReduction(90.0F, Optional.empty(), 0.0F, 1.0F)),
                    new BlocksAttacks.ItemDamageFunction(3.0F, 1.0F, 1.0F),
                    Optional.of(net.minecraft.core.HolderSet.emptyNamed(context.lookupOrThrow(net.minecraft.core.registries.Registries.DAMAGE_TYPE), DamageTypeTags.BYPASSES_SHIELD)),
                    Optional.of(SoundEvents.SHIELD_BLOCK),
                    Optional.of(SoundEvents.SHIELD_BREAK)))
            .component(DataComponents.BREAK_SOUND, SoundEvents.SHIELD_BREAK);
    }
}
