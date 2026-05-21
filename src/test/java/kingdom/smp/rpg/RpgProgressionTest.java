package kingdom.smp.rpg;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

// Scope: only RpgProgression's dependency-free arithmetic is covered here so the
// suite runs on a plain JVM. The addClassXp/removeClassXp paths build a
// PlayerKingdomRpgData, whose codec static-init transitively loads Minecraft
// classes — those belong in a gametest, not a unit test.
class RpgProgressionTest {

    @Test
    void xpToNextLevelFollowsCurveAndClampsBelowOne() {
        assertEquals(66, RpgProgression.xpToReachNextLevel(1));   // 40 + 1*26
        assertEquals(300, RpgProgression.xpToReachNextLevel(10)); // 40 + 10*26
        // levels below 1 are clamped to 1's cost
        assertEquals(66, RpgProgression.xpToReachNextLevel(0));
        assertEquals(66, RpgProgression.xpToReachNextLevel(-5));
    }

    @Test
    void classTierIsOneTierPerFiveLevels() {
        assertEquals(0, RpgProgression.classTier(-3));
        assertEquals(0, RpgProgression.classTier(0));
        assertEquals(0, RpgProgression.classTier(4));
        assertEquals(1, RpgProgression.classTier(5));
        assertEquals(1, RpgProgression.classTier(9));
        assertEquals(2, RpgProgression.classTier(10));
    }

    @Test
    void promotionLevelPerTierAndMaxTierHasNone() {
        assertEquals(5, RpgProgression.promotionLevelForTier(0));
        assertEquals(10, RpgProgression.promotionLevelForTier(1));
        assertEquals(15, RpgProgression.promotionLevelForTier(2));
        assertEquals(20, RpgProgression.promotionLevelForTier(3));
        assertEquals(-1, RpgProgression.promotionLevelForTier(4)); // tier 4 is max
        assertEquals(-1, RpgProgression.promotionLevelForTier(99));
    }

    @Test
    void justReachedPromotionOnlyFiresOnTheCrossingTick() {
        assertTrue(RpgProgression.justReachedPromotion(0, 4, 5));   // crosses L5
        assertFalse(RpgProgression.justReachedPromotion(0, 5, 6));  // already past
        assertFalse(RpgProgression.justReachedPromotion(0, 3, 4));  // not yet there
        assertFalse(RpgProgression.justReachedPromotion(4, 4, 5));  // tier 4 never promotes
    }
}
