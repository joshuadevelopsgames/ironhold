package kingdom.smp.gear;

import java.util.List;

import kingdom.smp.Ironhold;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

/**
 * Applies attribute-type affixes (Keen, Stalwart, Fleet, …) as item attribute modifiers. On-hit affixes
 * are handled separately in {@link AffixCombatHandler}. Registered to the game bus in {@code Ironhold}.
 */
public final class AffixAttributeHandler {
    private AffixAttributeHandler() {}

    @SubscribeEvent
    public static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        ItemStack stack = event.getItemStack();
        List<AffixInstance> affixes = AffixData.get(stack);
        if (affixes.isEmpty()) {
            return;
        }
        // Armor affixes apply while worn; weapon/tool affixes while held.
        EquipmentSlotGroup slot = AffixRoller.gearClass(stack) == AffixRoller.GearClass.ARMOR
            ? EquipmentSlotGroup.ARMOR : EquipmentSlotGroup.MAINHAND;

        for (AffixInstance ai : affixes) {
            Affix a = Affix.byId(ai.id());
            if (a == null || a.attribute() == null || a.operation() == null) {
                continue;
            }
            Identifier id = Identifier.fromNamespaceAndPath(Ironhold.MODID, "affix/" + a.id());
            event.addModifier(a.attribute(), new AttributeModifier(id, ai.roll(), a.operation()), slot);
        }
    }
}
