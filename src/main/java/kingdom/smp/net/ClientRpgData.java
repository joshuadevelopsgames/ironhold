package kingdom.smp.net;

import kingdom.smp.client.screen.ClassSelectionScreen;
import kingdom.smp.client.screen.KingdomSelectionScreen;
import kingdom.smp.client.screen.MainMenuScreen;
import kingdom.smp.client.screen.ProfileScreen;
import kingdom.smp.rpg.PlayerClass;
import net.minecraft.client.Minecraft;

/**
 * Client-side cache of RPG data received from the server.
 * Read by HUD overlays and screens — never written to by the client.
 * Also holds client-only preferences (HUD toggles, privacy settings).
 */
public final class ClientRpgData {
    private ClientRpgData() {}

    // ── Synced from server ────────────────────────────────────────────────────
    private static int kingdomIndex   = 0;
    private static int classIndex     = PlayerClass.PEASANT.ordinal();
    private static int classLevel     = 1;
    private static int xpIntoLevel    = 0;
    private static int xpToNext       = 70;
    private static int carryWeight    = 0;
    private static int maxCarryWeight = 120;

    // ── Client-only preferences ───────────────────────────────────────────────
    private static boolean classHudEnabled    = true;
    private static boolean carryHudEnabled    = true;
    private static boolean broadcastLevelUps  = true;

    // ── Receive ───────────────────────────────────────────────────────────────
    public static void receive(SyncRpgDataPayload payload) {
        kingdomIndex   = payload.kingdomIndex();
        classIndex     = payload.classIndex();
        classLevel     = payload.classLevel();
        xpIntoLevel    = payload.xpIntoLevel();
        xpToNext       = payload.xpToNext();
        carryWeight    = payload.carryWeight();
        maxCarryWeight = payload.maxCarryWeight();
    }

    // ── Screen openers ────────────────────────────────────────────────────────
    public static void openClassSelection()  { Minecraft.getInstance().setScreen(new ClassSelectionScreen()); }
    public static void openKingdomSelection(){ Minecraft.getInstance().setScreen(new KingdomSelectionScreen()); }
    public static void openProfile()         { Minecraft.getInstance().setScreen(new ProfileScreen()); }
    public static void openMenu()            { Minecraft.getInstance().setScreen(new MainMenuScreen()); }

    // ── RPG data getters ──────────────────────────────────────────────────────
    public static int         kingdomIndex()   { return kingdomIndex; }
    public static int         classIndex()     { return classIndex; }
    public static PlayerClass playerClass()    { return PlayerClass.fromIndex(classIndex); }
    public static int         classLevel()     { return classLevel; }
    public static int         xpIntoLevel()    { return xpIntoLevel; }
    public static int         xpToNext()       { return xpToNext; }
    public static int         carryWeight()    { return carryWeight; }
    public static int         maxCarryWeight() { return maxCarryWeight; }
    public static float       xpProgress()    { return xpToNext <= 0 ? 0f : (float) xpIntoLevel / xpToNext; }
    public static boolean     isOverEncumbered() { return carryWeight > maxCarryWeight; }

    // ── Preference getters ────────────────────────────────────────────────────
    public static boolean classHudEnabled()   { return classHudEnabled; }
    public static boolean carryHudEnabled()   { return carryHudEnabled; }
    public static boolean broadcastLevelUps() { return broadcastLevelUps; }

    // ── Preference setters ────────────────────────────────────────────────────
    public static void setClassHudEnabled  (boolean v) { classHudEnabled   = v; }
    public static void setCarryHudEnabled  (boolean v) { carryHudEnabled   = v; }
    public static void setBroadcastLevelUps(boolean v) { broadcastLevelUps = v; }
}
