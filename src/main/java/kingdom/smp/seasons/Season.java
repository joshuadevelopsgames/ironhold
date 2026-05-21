package kingdom.smp.seasons;

/**
 * The four standard seasons and their twelve sub-seasons. Each sub-season carries a
 * foliage/grass tint (packed RGB) and a saturation factor — the resolver in
 * {@code SeasonColorHandlers} blends this overlay onto vanilla biome colors at that strength.
 *
 * <p>Saturation of {@code -1f} disables the overlay for that sub-season (vanilla colors only).
 */
public enum Season {
    SPRING,
    SUMMER,
    AUTUMN,
    WINTER;

    public enum SubSeason {
        EARLY_SPRING (SPRING,  0x73B238, 0x72A33D, 0.25f),
        MID_SPRING   (SPRING,  0x76B83A, 0x76A33D, 0.10f),
        LATE_SPRING  (SPRING,  0x79BD3C, 0x7AA33D,-1.00f),
        EARLY_SUMMER (SUMMER,  0x7CC23E, 0x7AA33D,-1.00f),
        MID_SUMMER   (SUMMER,  0x7FC840, 0x7AA33D,-1.00f),
        LATE_SUMMER  (SUMMER,  0xA0B344, 0x9AAB3D, 0.20f),
        EARLY_AUTUMN (AUTUMN,  0xC09F46, 0xBBA13D, 0.45f),
        MID_AUTUMN   (AUTUMN,  0xC97E32, 0xCB7A24, 0.60f),
        LATE_AUTUMN  (AUTUMN,  0xBC5C26, 0xC2611E, 0.65f),
        EARLY_WINTER (WINTER,  0xA85F38, 0xA15C2A, 0.60f),
        MID_WINTER   (WINTER,  0xDB3030, 0xC04A2A, 0.45f),
        LATE_WINTER  (WINTER,  0xC4592E, 0xB45828, 0.40f);

        public static final SubSeason[] VALUES = values();

        private final Season parent;
        private final int grassOverlay;
        private final int foliageOverlay;
        private final float saturation;

        SubSeason(Season parent, int grassOverlay, int foliageOverlay, float saturation) {
            this.parent = parent;
            this.grassOverlay = grassOverlay;
            this.foliageOverlay = foliageOverlay;
            this.saturation = saturation;
        }

        public Season parent() { return parent; }
        public int grassOverlay() { return grassOverlay; }
        public int foliageOverlay() { return foliageOverlay; }
        public float saturation() { return saturation; }
        public boolean hasOverlay() { return saturation >= 0f; }
    }
}
