package kingdom.smp.block.wardheart;

import net.minecraft.network.chat.Component;
import net.minecraft.util.StringRepresentable;

public enum WardheartTier implements StringRepresentable {
    DORMANT     ("dormant",      0,    0, 0x000000, 0.00f),
    /** Smallest setting — used for permanent natural shields (e.g., dragon crystals).
     *  Not selectable through normal fuel progression; only set when permanent flag is on. */
    MICRO       ("micro",         5,    0, 0x9445FF, 0.70f),
    WEAK        ("weak",          8,  500, 0x4A1F8E, 0.55f),
    STABLE      ("stable",       16, 2000, 0x6E2DC8, 0.65f),
    ROYAL       ("royal",        32, 6000, 0x9445FF, 0.75f),
    OVERCHARGED ("overcharged",  48,12000, 0xB76BFF, 0.90f);

    private final String name;
    private final int radius;
    private final int fuelToReach;
    private final int color;
    private final float alpha;

    WardheartTier(String name, int radius, int fuelToReach, int color, float alpha) {
        this.name = name;
        this.radius = radius;
        this.fuelToReach = fuelToReach;
        this.color = color;
        this.alpha = alpha;
    }

    @Override public String getSerializedName() { return name; }
    public int radius() { return radius; }
    public int fuelToReach() { return fuelToReach; }
    public int color() { return color; }
    public float alpha() { return alpha; }
    public boolean isActive() { return this != DORMANT; }

    public Component displayName() {
        return Component.translatable("ironhold.wardheart.tier." + name);
    }

    public static WardheartTier fromFuel(int fuel) {
        // MICRO is intentionally skipped here — it is reserved for permanent
        // shields (e.g., the auto-spawned shields around dragon end crystals)
        // and never applies to fuel-driven progression.
        if (fuel >= OVERCHARGED.fuelToReach) return OVERCHARGED;
        if (fuel >= ROYAL.fuelToReach)       return ROYAL;
        if (fuel >= STABLE.fuelToReach)      return STABLE;
        if (fuel >= WEAK.fuelToReach)        return WEAK;
        return DORMANT;
    }
}
