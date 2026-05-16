package kingdom.smp.client.hud;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kingdom.smp.Ironhold;
import net.neoforged.fml.loading.FMLPaths;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Client-side persisted positioning for the Skyrim-style sneak-eye HUD.
 *
 * <p>Stored as {@code config/ironhold-sneakeye.json}. Loaded once on first access;
 * mutated via {@code /sneakeye} debug command and saved on every change.
 */
public final class SneakEyeConfig {
    private SneakEyeConfig() {}

    /** Horizontal offset from screen center, in GUI pixels. */
    public static int offsetX = 0;
    /** Vertical offset from screen center, in GUI pixels. Negative = above crosshair. */
    public static int offsetY = -10;
    /** Multiplier on the eye's rendered width. */
    public static float scale = 0.1f;

    private static final int DEFAULT_OFFSET_X = 0;
    private static final int DEFAULT_OFFSET_Y = -10;
    private static final float DEFAULT_SCALE = 0.1f;

    private static boolean loaded = false;

    public static void loadIfNeeded() {
        if (loaded) return;
        loaded = true;
        Path file = configPath();
        if (!Files.exists(file)) return;
        try {
            String json = Files.readString(file);
            JsonObject obj = JsonParser.parseString(json).getAsJsonObject();
            if (obj.has("offsetX")) offsetX = obj.get("offsetX").getAsInt();
            if (obj.has("offsetY")) offsetY = obj.get("offsetY").getAsInt();
            if (obj.has("scale")) scale = obj.get("scale").getAsFloat();
        } catch (IOException | RuntimeException e) {
            Ironhold.LOGGER.warn("[Ironhold] failed to load sneak-eye config: {}", e.toString());
        }
    }

    public static void save() {
        Path file = configPath();
        JsonObject obj = new JsonObject();
        obj.addProperty("offsetX", offsetX);
        obj.addProperty("offsetY", offsetY);
        obj.addProperty("scale", scale);
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, obj.toString());
        } catch (IOException e) {
            Ironhold.LOGGER.warn("[Ironhold] failed to save sneak-eye config: {}", e.toString());
        }
    }

    public static void resetDefaults() {
        offsetX = DEFAULT_OFFSET_X;
        offsetY = DEFAULT_OFFSET_Y;
        scale = DEFAULT_SCALE;
        save();
    }

    public static String summary() {
        return String.format("offset=(%d, %d), scale=%.2f", offsetX, offsetY, scale);
    }

    private static Path configPath() {
        return FMLPaths.CONFIGDIR.get().resolve("ironhold-sneakeye.json");
    }
}
