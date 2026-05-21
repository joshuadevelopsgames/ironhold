package kingdom.smp.fishing;

/**
 * Duck-typed interface implemented by {@code FishingHook} via
 * {@code FishingHookBiteMixin}. Lets the server-side
 * {@link FishingMinigameManager} flip the per-hook minigame flag so the
 * mixin can pin the nibble window open while the player plays the
 * bite minigame on their client.
 */
public interface IFishingHookMinigame {
    void ironhold$setMinigameActive(boolean active);
    boolean ironhold$isMinigameActive();

    /** The hook's luck field (Luck of the Sea bonus), as used by vanilla retrieve(). */
    int ironhold$getLuck();
}
