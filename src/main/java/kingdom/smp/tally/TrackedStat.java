package kingdom.smp.tally;

import java.util.List;

/**
 * The curated set of Minecraft statistics the Tallykeeper ranks players on.
 * Each entry carries a flex "title" the herald bestows, a human-readable stat
 * name, a prestige weight (higher = more impressive → bigger reward), and one
 * or more underlying stat keys whose values are summed.
 *
 * <p>Stat keys mirror the JSON layout of {@code world/stats/<uuid>.json}:
 * a category ({@code minecraft:custom}, {@code minecraft:mined}, …) and an
 * entry ({@code minecraft:play_time}, {@code minecraft:diamond_ore}, …).
 */
public enum TrackedStat {
    PLAYTIME      ("the Devoted",              "hours played",          1.0, Unit.TIME,     key("custom", "play_time")),
    PLAYER_KILLS  ("Bloodletter",              "player kills",          1.6, Unit.COUNT,    key("custom", "player_kills")),
    MOB_KILLS     ("Monster Hunter",           "monster kills",         1.2, Unit.COUNT,    key("custom", "mob_kills")),
    DIAMONDS      ("Diamond Baron",            "diamonds mined",        1.4, Unit.COUNT,    key("mined", "diamond_ore"), key("mined", "deepslate_diamond_ore")),
    NETHERITE     ("Debris Lord",              "ancient debris mined",  1.8, Unit.COUNT,    key("mined", "ancient_debris")),
    DISTANCE      ("the Wayfarer",             "distance traveled on foot", 1.0, Unit.DISTANCE, key("custom", "walk_one_cm"), key("custom", "sprint_one_cm")),
    ELYTRA        ("Skydancer",                "distance flown",        1.3, Unit.DISTANCE, key("custom", "aviate_one_cm")),
    DAMAGE_DEALT  ("Heavy Hitter",             "damage dealt",          1.2, Unit.DAMAGE,   key("custom", "damage_dealt")),
    DEATHS        ("Glutton for Punishment",   "deaths",                0.8, Unit.COUNT,    key("custom", "deaths")),
    RAID_WIN      ("Hero of the Village",      "raids won",             1.3, Unit.COUNT,    key("custom", "raid_win")),
    TRADES        ("the Merchant Prince",      "villager trades",       1.0, Unit.COUNT,    key("custom", "traded_with_villager")),
    FISH          ("Master Angler",            "fish caught",           1.0, Unit.COUNT,    key("custom", "fish_caught")),
    BRED          ("the Shepherd",             "animals bred",          0.9, Unit.COUNT,    key("custom", "animals_bred")),
    ENCHANT       ("the Arcanist",             "items enchanted",       1.1, Unit.COUNT,    key("custom", "enchant_item"));

    /** Display formatting for a raw stat value. */
    public enum Unit { COUNT, TIME, DISTANCE, DAMAGE }

    /** A single underlying stat: a fully-namespaced category + entry. */
    public record StatKey(String category, String entry) {}

    private final String title;
    private final String displayName;
    private final double prestige;
    private final Unit unit;
    private final List<StatKey> keys;

    TrackedStat(String title, String displayName, double prestige, Unit unit, StatKey... keys) {
        this.title = title;
        this.displayName = displayName;
        this.prestige = prestige;
        this.unit = unit;
        this.keys = List.of(keys);
    }

    public String title()       { return title; }
    public String displayName() { return displayName; }
    public double prestige()    { return prestige; }
    public List<StatKey> keys() { return keys; }

    private static StatKey key(String category, String entry) {
        return new StatKey("minecraft:" + category, "minecraft:" + entry);
    }

    /** Human-readable rendering of a raw value for this stat's unit. */
    public String format(long value) {
        return switch (unit) {
            case TIME     -> (value / 72000L) + " hours";              // ticks → hours
            case DISTANCE -> (value / 100L) + " blocks";               // cm → blocks
            case DAMAGE   -> (value / 10L) + " hearts of damage";      // tenths of HP → hearts
            case COUNT    -> String.valueOf(value);
        };
    }
}
