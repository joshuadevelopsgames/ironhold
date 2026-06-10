package kingdom.smp.gear;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Registry of gear affixes (random stat bonuses + special on-hit abilities). Affix <i>count</i> on an
 * item is gated by {@link ItemQuality} (Poor 0 / Fine 1 / Good 2 / Mint 3) — see {@link AffixData}.
 *
 * <p>Two kinds: <b>attribute</b> affixes (non-null {@link #attribute}) apply an {@link AttributeModifier}
 * via {@link AffixAttributeHandler}; <b>on-hit</b> affixes (null attribute, category {@link AffixCategory#ON_HIT})
 * are dispatched by id in {@link AffixCombatHandler}.
 *
 * <p>All 21 designed affixes ship: attribute + on-hit ones here/{@link AffixCombatHandler}, and the
 * three non-combat hooks (Prospector/Scholar/Enduring) in {@link AffixUtilityHandler} +
 * {@code ItemStackMaxDamageMixin}. Spec: {@code specs/fantasia-ports/07-gear-affixes.md}.
 */
public enum Affix {
    // ── Offensive (weapons) ──
    KEEN("keen", AffixCategory.OFFENSIVE, "Keen", ChatFormatting.AQUA, 0.05f, 0.15f, true,
        Attributes.ATTACK_DAMAGE, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
    SWIFT("swift", AffixCategory.OFFENSIVE, "Swift", ChatFormatting.AQUA, 0.05f, 0.12f, true,
        Attributes.ATTACK_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
    SAVAGE("savage", AffixCategory.OFFENSIVE, "Savage", ChatFormatting.GOLD, 0.15f, 0.35f, true, null, null),
    PIERCING("piercing", AffixCategory.OFFENSIVE, "Piercing", ChatFormatting.GOLD, 0.10f, 0.25f, true, null, null),
    BRUTAL("brutal", AffixCategory.OFFENSIVE, "Brutal", ChatFormatting.GOLD, 0.15f, 0.30f, true, null, null),
    // ── Defensive (armor) ──
    STALWART("stalwart", AffixCategory.DEFENSIVE, "Stalwart", ChatFormatting.GRAY, 1f, 3f, false,
        Attributes.ARMOR, AttributeModifier.Operation.ADD_VALUE),
    VITAL("vital", AffixCategory.DEFENSIVE, "Vital", ChatFormatting.GRAY, 2f, 6f, false,
        Attributes.MAX_HEALTH, AttributeModifier.Operation.ADD_VALUE),
    BULWARK("bulwark", AffixCategory.DEFENSIVE, "Bulwark", ChatFormatting.GRAY, 0.10f, 0.30f, true,
        Attributes.KNOCKBACK_RESISTANCE, AttributeModifier.Operation.ADD_VALUE),
    WARDED("warded", AffixCategory.DEFENSIVE, "Warded", ChatFormatting.GRAY, 0.03f, 0.08f, true, null, null),
    THORNS("thorns", AffixCategory.DEFENSIVE, "Thorns", ChatFormatting.GRAY, 0.10f, 0.25f, true, null, null),
    // ── Utility (armor/tools) ──
    FLEET("fleet", AffixCategory.UTILITY, "Fleet", ChatFormatting.GREEN, 0.04f, 0.10f, true,
        Attributes.MOVEMENT_SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE),
    REACHING("reaching", AffixCategory.UTILITY, "Reaching", ChatFormatting.GREEN, 0.5f, 1.0f, false,
        Attributes.BLOCK_INTERACTION_RANGE, AttributeModifier.Operation.ADD_VALUE),
    LUCKY("lucky", AffixCategory.UTILITY, "Lucky", ChatFormatting.GREEN, 1f, 2f, false,
        Attributes.LUCK, AttributeModifier.Operation.ADD_VALUE),
    PROSPECTOR("prospector", AffixCategory.UTILITY, "Prospector", ChatFormatting.GREEN, 0.08f, 0.20f, true, null, null),
    SCHOLAR("scholar", AffixCategory.UTILITY, "Scholar", ChatFormatting.GREEN, 0.05f, 0.15f, true, null, null),
    ENDURING("enduring", AffixCategory.UTILITY, "Enduring", ChatFormatting.GREEN, 0.10f, 0.25f, true, null, null),
    // ── Special on-hit (weapons) ──
    LEECHING("leeching", AffixCategory.ON_HIT, "Leeching", ChatFormatting.RED, 0.05f, 0.12f, true, null, null),
    SERRATED("serrated", AffixCategory.ON_HIT, "Serrated", ChatFormatting.RED, 0.20f, 0.40f, true, null, null),
    CONCUSSIVE("concussive", AffixCategory.ON_HIT, "Concussive", ChatFormatting.RED, 0.15f, 0.25f, true, null, null),
    VOLTAIC("voltaic", AffixCategory.ON_HIT, "Voltaic", ChatFormatting.RED, 0.15f, 0.25f, true, null, null),
    SOULRENDING("soulrending", AffixCategory.ON_HIT, "Soulrending", ChatFormatting.RED, 1f, 1f, false, null, null);

    private final String id;
    private final AffixCategory category;
    private final String displayName;
    private final ChatFormatting color;
    private final float min;
    private final float max;
    private final boolean percent; // tooltip formatting only
    private final Holder<Attribute> attribute;
    private final AttributeModifier.Operation operation;

    Affix(String id, AffixCategory category, String displayName, ChatFormatting color,
          float min, float max, boolean percent,
          Holder<Attribute> attribute, AttributeModifier.Operation operation) {
        this.id = id;
        this.category = category;
        this.displayName = displayName;
        this.color = color;
        this.min = min;
        this.max = max;
        this.percent = percent;
        this.attribute = attribute;
        this.operation = operation;
    }

    public String id() { return id; }
    public AffixCategory category() { return category; }
    public String displayName() { return displayName; }
    public ChatFormatting color() { return color; }
    public float min() { return min; }
    public float max() { return max; }
    public boolean percent() { return percent; }
    public Holder<Attribute> attribute() { return attribute; }
    public AttributeModifier.Operation operation() { return operation; }

    public static Affix byId(String id) {
        for (Affix a : values()) {
            if (a.id.equals(id)) return a;
        }
        return null;
    }

    public static List<Affix> forCategories(AffixCategory... cats) {
        List<Affix> out = new ArrayList<>();
        for (Affix a : values()) {
            for (AffixCategory c : cats) {
                if (a.category == c) { out.add(a); break; }
            }
        }
        return out;
    }
}
