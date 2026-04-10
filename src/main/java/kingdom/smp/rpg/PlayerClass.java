package kingdom.smp.rpg;

import net.minecraft.network.chat.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Kingdom SMP 2.0 class tree — 27 classes across 4 tiers with prerequisite paths.
 * <p>
 * Stat values are 0–100 design-doc values. {@link kingdom.smp.game.ClassStatHandler}
 * converts these into Minecraft attribute modifiers.
 */
public enum PlayerClass {
    // ── Peasant (starting class, no tier) ──────────────────────────
    PEASANT("Peasant", "None", 0, 120, new String[]{},
        40, 30, 30, 35, 10, 40, 30),

    // ── Tier 1 — Starters ──────────────────────────────────────────
    SQUIRE("Squire", "Tank", 1, 160, new String[]{},
        60, 55, 45, 40, 10, 40, 50),
    MAGE_APPRENTICE("Mage Apprentice", "Mage", 1, 100, new String[]{},
        30, 20, 55, 45, 70, 45, 35),
    ARCHER("Archer", "Ranger", 1, 140, new String[]{},
        40, 30, 50, 55, 15, 65, 45),
    MEDIC("Medic", "Support", 1, 150, new String[]{},
        45, 35, 25, 35, 65, 50, 45),

    // ── Tier 2 — Trained ───────────────────────────────────────────
    KNIGHT("Knight", "Tank", 2, 200, new String[]{"SQUIRE"},
        70, 70, 55, 40, 15, 35, 65),
    WIZARD("Wizard", "Mage", 2, 110, new String[]{"MAGE_APPRENTICE"},
        35, 25, 65, 50, 85, 40, 50),
    RANGER("Ranger", "Ranger", 2, 160, new String[]{"ARCHER"},
        50, 35, 60, 65, 25, 70, 45),
    ROGUE("Rogue", "Ranger", 2, 140, new String[]{"ARCHER"},
        45, 30, 70, 70, 20, 75, 60),
    CLERIC("Cleric", "Support", 2, 180, new String[]{"MEDIC"},
        55, 45, 30, 35, 80, 40, 65),

    // ── Tier 3 — Specialists ───────────────────────────────────────
    RAVAGER("Ravager", "Tank", 3, 200, new String[]{"KNIGHT"},
        75, 50, 85, 65, 10, 45, 70),
    PALADIN("Paladin", "Tank", 3, 220, new String[]{"KNIGHT"},
        80, 75, 50, 35, 60, 30, 70),
    ELEMENTALIST("Elementalist", "Mage", 3, 110, new String[]{"WIZARD"},
        40, 30, 80, 55, 95, 40, 60),
    MARKSMAN("Marksman", "Ranger", 3, 160, new String[]{"RANGER"},
        50, 35, 80, 75, 20, 65, 75),
    MARTYR("Martyr", "Support", 3, 180, new String[]{"CLERIC"},
        70, 40, 30, 35, 85, 35, 100),
    SAINT("Saint", "Support", 3, 200, new String[]{"CLERIC"},
        60, 55, 20, 30, 90, 40, 65),

    // ── Tier 4 — Masters ───────────────────────────────────────────
    BERSERKER("Berserker", "Tank", 4, 180, new String[]{"RAVAGER"},
        85, 40, 95, 80, 10, 55, 85),
    CHAMPION("Champion", "Tank", 4, 240, new String[]{"RAVAGER", "PALADIN"},
        90, 85, 70, 45, 20, 40, 100),
    SORCERER_SUPREME("Sorcerer Supreme", "Mage", 4, 100, new String[]{"ELEMENTALIST"},
        45, 35, 95, 60, 100, 35, 80),
    DEADSHOT("Deadshot", "Ranger", 4, 150, new String[]{"MARKSMAN"},
        55, 35, 95, 85, 15, 70, 95),
    REDEEMER("Redeemer", "Support", 4, 170, new String[]{"MARTYR"},
        85, 45, 25, 30, 95, 35, 100),
    BISHOP("Bishop", "Support", 4, 200, new String[]{"SAINT"},
        65, 60, 35, 35, 100, 30, 80),

    // ── Tier 4 — Hybrids ───────────────────────────────────────────
    ARCANE_KNIGHT("Arcane Knight", "Hybrid", 4, 180, new String[]{"BERSERKER", "SORCERER_SUPREME"},
        70, 60, 75, 50, 65, 40, 80),
    IRON_RANGER("Iron Ranger", "Hybrid", 4, 200, new String[]{"CHAMPION", "DEADSHOT"},
        65, 70, 70, 60, 15, 45, 75),
    ARCANE_RANGER("Arcane Ranger", "Hybrid", 4, 140, new String[]{"SORCERER_SUPREME", "DEADSHOT"},
        45, 30, 75, 70, 70, 60, 90),
    DIVINE_KNIGHT("Divine Knight", "Hybrid", 4, 220, new String[]{"CHAMPION", "REDEEMER"},
        75, 70, 60, 40, 70, 30, 85),
    DIVINE_MAGE("Divine Mage", "Hybrid", 4, 160, new String[]{"SORCERER_SUPREME", "REDEEMER"},
        55, 45, 65, 45, 90, 35, 85),
    DIVINE_RANGER("Divine Ranger", "Hybrid", 4, 160, new String[]{"DEADSHOT", "REDEEMER"},
        55, 40, 70, 70, 65, 55, 85);

    private final String displayName;
    private final String role;
    private final int tier;
    private final int maxCarryWeight;
    private final String[] prerequisiteNames;

    // Stats (0–100 design values)
    private final int health;
    private final int defense;
    private final int attackDamage;
    private final int attackSpeed;
    private final int mana;
    private final int speed;
    private final int luck;

    PlayerClass(String displayName, String role, int tier, int maxCarryWeight,
                String[] prerequisites,
                int health, int defense, int attackDamage, int attackSpeed,
                int mana, int speed, int luck) {
        this.displayName = displayName;
        this.role = role;
        this.tier = tier;
        this.maxCarryWeight = maxCarryWeight;
        this.prerequisiteNames = prerequisites;
        this.health = health;
        this.defense = defense;
        this.attackDamage = attackDamage;
        this.attackSpeed = attackSpeed;
        this.mana = mana;
        this.speed = speed;
        this.luck = luck;
    }

    public String id() { return displayName; }
    public String role() { return role; }
    public int tier() { return tier; }
    public Component displayName() { return Component.literal(displayName); }
    public int maxCarryWeight() { return maxCarryWeight; }

    // ── Stats ──────────────────────────────────────────────────────
    public int statHealth() { return health; }
    public int statDefense() { return defense; }
    public int statAttackDamage() { return attackDamage; }
    public int statAttackSpeed() { return attackSpeed; }
    public int statMana() { return mana; }
    public int statSpeed() { return speed; }
    public int statLuck() { return luck; }

    // ── Prerequisites ──────────────────────────────────────────────
    /** Returns the classes required to unlock this class. */
    public List<PlayerClass> prerequisites() {
        return Arrays.stream(prerequisiteNames)
            .map(n -> {
                try { return PlayerClass.valueOf(n); }
                catch (IllegalArgumentException e) { return null; }
            })
            .filter(c -> c != null)
            .toList();
    }

    /** Check if a player who has completed the given classes can unlock this one. */
    public boolean canUnlock(java.util.Set<PlayerClass> completedClasses) {
        if (prerequisiteNames.length == 0) return true;
        return prerequisites().stream().allMatch(completedClasses::contains);
    }

    // ── Lookup ─────────────────────────────────────────────────────
    public static PlayerClass fromIndex(int index) {
        PlayerClass[] values = values();
        if (index < 0 || index >= values.length) return PEASANT;
        return values[index];
    }

    public static PlayerClass parse(String raw) {
        if (raw == null || raw.isBlank()) return PEASANT;
        String s = raw.trim().toLowerCase();
        for (PlayerClass c : values()) {
            if (c.displayName.toLowerCase().equals(s) || c.name().toLowerCase().equals(s))
                return c;
        }
        return PEASANT;
    }
}
