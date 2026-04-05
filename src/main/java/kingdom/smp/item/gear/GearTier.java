package kingdom.smp.item.gear;

/** Equipment tier — governs stats, class-level equip gate, and material family. */
public enum GearTier {
    IRON(5, "iron"),
    STEEL(15, "steel"),
    TANZANITE(25, "tanzanite");

    /** Minimum class level required to equip items of this tier. */
    public final int classLevelRequired;

    /** Lowercase name used in item IDs and resource paths. */
    public final String id;

    GearTier(int classLevelRequired, String id) {
        this.classLevelRequired = classLevelRequired;
        this.id = id;
    }
}
