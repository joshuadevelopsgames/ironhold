package kingdom.smp.client;

import kingdom.smp.Ironhold;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class IronholdKeys {
    private IronholdKeys() {}

    private static final KeyMapping.Category CATEGORY =
        new KeyMapping.Category(Identifier.fromNamespaceAndPath(Ironhold.MODID, "ironhold"));

    public static final KeyMapping SIREN_LURE = new KeyMapping(
        "key.ironhold.siren_lure",
        GLFW.GLFW_KEY_C,
        CATEGORY
    );

    /** Slot 0 — Z. */
    public static final KeyMapping ABILITY_1 = new KeyMapping(
        "key.ironhold.ability_1", GLFW.GLFW_KEY_Z, CATEGORY);
    /** Slot 1 — X. */
    public static final KeyMapping ABILITY_2 = new KeyMapping(
        "key.ironhold.ability_2", GLFW.GLFW_KEY_X, CATEGORY);
    /** Slot 2 — C (conflicts with SIREN_LURE by default; vanilla shows the key-conflict warning and the user resolves it). */
    public static final KeyMapping ABILITY_3 = new KeyMapping(
        "key.ironhold.ability_3", GLFW.GLFW_KEY_C, CATEGORY);
    /** Slot 3 — V. */
    public static final KeyMapping ABILITY_4 = new KeyMapping(
        "key.ironhold.ability_4", GLFW.GLFW_KEY_V, CATEGORY);

    public static final KeyMapping[] ABILITIES = { ABILITY_1, ABILITY_2, ABILITY_3, ABILITY_4 };

    /** Push-to-talk toggle for Kangarude: tap to start recording, tap again to stop + send. */
    public static final KeyMapping KANGARUDE_PTT = new KeyMapping(
        "key.ironhold.kangarude_ptt", GLFW.GLFW_KEY_K, CATEGORY);

    /**
     * Seashell underwater dash — discrete press only via {@code consumeClick()},
     * default Left Shift. Holding it down does not auto-fire.
     */
    public static final KeyMapping SEASHELL_DASH = new KeyMapping(
        "key.ironhold.seashell_dash", GLFW.GLFW_KEY_LEFT_SHIFT, CATEGORY);

    /** Point emote — discrete press via {@code consumeClick()}, default B. */
    public static final KeyMapping EMOTE_POINT = new KeyMapping(
        "key.ironhold.emote_point", GLFW.GLFW_KEY_B, CATEGORY);

    // ── Dev: stone golem hammer-grip tuner (see StoneGolemHammerTuning) ──
    /** Cycle which grip field the -/= keys nudge (posX..scale). */
    public static final KeyMapping GOLEM_HAMMER_CYCLE = new KeyMapping(
        "key.ironhold.golem_hammer_cycle", GLFW.GLFW_KEY_SEMICOLON, CATEGORY);
    /** Decrease the active grip field. */
    public static final KeyMapping GOLEM_HAMMER_DEC = new KeyMapping(
        "key.ironhold.golem_hammer_dec", GLFW.GLFW_KEY_MINUS, CATEGORY);
    /** Increase the active grip field. */
    public static final KeyMapping GOLEM_HAMMER_INC = new KeyMapping(
        "key.ironhold.golem_hammer_inc", GLFW.GLFW_KEY_EQUAL, CATEGORY);
}
