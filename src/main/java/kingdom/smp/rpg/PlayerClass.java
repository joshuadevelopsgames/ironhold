package kingdom.smp.rpg;

import net.minecraft.network.chat.Component;

/** Kingdom SMP 2.0 class line — matches design docs (Peasant → promoted classes). */
public enum PlayerClass {
    PEASANT("Peasant", 120),
    KNIGHT("Knight", 200),
    RANGER("Ranger", 160),
    ROGUE("Rogue", 140),
    WIZARD("Wizard", 100),
    CLERIC("Cleric", 220);

    private final String id;
    private final int maxCarryWeight;

    PlayerClass(String id, int maxCarryWeight) {
        this.id = id;
        this.maxCarryWeight = maxCarryWeight;
    }

    public String id() {
        return id;
    }

    public Component displayName() {
        return Component.literal(id);
    }

    /** Stub encumbrance cap (SYSTEMS.md carry weight). */
    public int maxCarryWeight() {
        return maxCarryWeight;
    }

    public static PlayerClass fromIndex(int index) {
        PlayerClass[] values = values();
        if (index < 0 || index >= values.length) {
            return PEASANT;
        }
        return values[index];
    }

    public static PlayerClass parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return PEASANT;
        }
        String s = raw.trim().toLowerCase();
        for (PlayerClass c : values()) {
            if (c.id.toLowerCase().equals(s) || c.name().toLowerCase().equals(s)) {
                return c;
            }
        }
        return PEASANT;
    }
}
