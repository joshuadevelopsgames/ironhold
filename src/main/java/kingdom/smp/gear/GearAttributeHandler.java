package kingdom.smp.gear;

import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.ItemAttributeModifierEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Scales item attribute modifiers by the item's quality multiplier. Listens to
 * {@link ItemAttributeModifierEvent}, which fires whenever an item's attribute modifiers are
 * queried — for armor protection lookups, weapon damage calculation, equipment HUDs, etc.
 *
 * Scaled attributes:
 * <ul>
 *   <li>{@link Attributes#ARMOR ARMOR} — armor points (per piece)</li>
 *   <li>{@link Attributes#ARMOR_TOUGHNESS ARMOR_TOUGHNESS}</li>
 *   <li>{@link Attributes#KNOCKBACK_RESISTANCE KNOCKBACK_RESISTANCE}</li>
 *   <li>{@link Attributes#ATTACK_DAMAGE ATTACK_DAMAGE} — sword/axe/mace etc.</li>
 * </ul>
 *
 * Quality values:
 * <ul>
 *   <li>Poor → 0.5×</li>
 *   <li>Fine → 0.8×</li>
 *   <li>Good (default) → 1.0× (no-op fast path)</li>
 *   <li>Mint → 1.2×</li>
 * </ul>
 *
 * Rounding rules:
 * <ul>
 *   <li>ARMOR / ARMOR_TOUGHNESS — round to nearest integer, floor 0</li>
 *   <li>KNOCKBACK_RESISTANCE / ATTACK_DAMAGE — float, no rounding</li>
 * </ul>
 *
 * Out of scope by design (do NOT scale):
 * <ul>
 *   <li>Attack speed — keeps weapon swing rhythm vanilla so PvP timing isn't tier-dependent.</li>
 *   <li>Mining speed — gathering pace stays vanilla.</li>
 *   <li>Mining tier — firewalled by spec §8 (quality NEVER changes which tier a tool can mine).</li>
 *   <li>Enchantability — kept vanilla so the enchanting table behaves the same regardless of quality.</li>
 * </ul>
 *
 * @see <a href="../../../../specs/ore-quality-system.md">ore-quality-system.md §3</a>
 */
public final class GearAttributeHandler {
    private GearAttributeHandler() {}

    @SubscribeEvent
    public static void onItemAttributeModifiers(ItemAttributeModifierEvent event) {
        // Defensive: even if a utility item somehow has a quality component set, skip scaling.
        if (!QualityScope.isEligible(event.getItemStack())) return;

        ItemQuality quality = GearComponents.getQuality(event.getItemStack());
        // Default Good = 1.0× — no-op.
        if (quality == ItemQuality.defaultQuality()) return;

        float multiplier = quality.durabilityMultiplier();

        // Snapshot to avoid concurrent modification while replacing entries.
        List<ItemAttributeModifiers.Entry> snapshot = new ArrayList<>(event.getModifiers());
        for (ItemAttributeModifiers.Entry entry : snapshot) {
            Holder<Attribute> attr = entry.attribute();
            if (!isQualityScalable(attr)) continue;

            AttributeModifier original = entry.modifier();
            double scaled = original.amount() * multiplier;
            double finalAmount = needsIntegerRounding(attr) ? Math.max(0, Math.round(scaled)) : scaled;

            AttributeModifier replacement = new AttributeModifier(
                    original.id(),
                    finalAmount,
                    original.operation()
            );
            event.replaceModifier(attr, replacement, entry.slot());
        }
    }

    private static boolean isQualityScalable(Holder<Attribute> attr) {
        return attr.equals(Attributes.ARMOR)
                || attr.equals(Attributes.ARMOR_TOUGHNESS)
                || attr.equals(Attributes.KNOCKBACK_RESISTANCE)
                || attr.equals(Attributes.ATTACK_DAMAGE);
    }

    /** Armor and toughness display as integers; KBR and attack damage are fractional. */
    private static boolean needsIntegerRounding(Holder<Attribute> attr) {
        return attr.equals(Attributes.ARMOR) || attr.equals(Attributes.ARMOR_TOUGHNESS);
    }
}
