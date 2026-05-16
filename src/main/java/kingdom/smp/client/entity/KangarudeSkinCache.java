package kingdom.smp.client.entity;

import com.mojang.authlib.GameProfile;
import kingdom.smp.Ironhold;
import kingdom.smp.entity.KangarudeEntity;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Loads and caches the {@link KangarudeEntity#SKIN_OWNER_NAME} player skin
 * for use in the entity renderer.
 *
 * <p>Lookup is async (Mojang profile + skin endpoints), falls back to a
 * default Steve skin until the real one finishes loading. One lookup per
 * client session.
 */
public final class KangarudeSkinCache {

    /** Stable UUID derived from the username for the default-skin fallback. */
    private static final UUID FALLBACK_UUID =
        UUID.nameUUIDFromBytes(("OfflinePlayer:" + KangarudeEntity.SKIN_OWNER_NAME).getBytes());

    private static final AtomicReference<PlayerSkin> CACHED = new AtomicReference<>();
    private static volatile boolean lookupStarted = false;

    private KangarudeSkinCache() {}

    public static PlayerSkin getOrFetch() {
        PlayerSkin cached = CACHED.get();
        if (cached != null) return cached;
        if (!lookupStarted) {
            lookupStarted = true;
            startLookup();
        }
        return DefaultPlayerSkin.get(FALLBACK_UUID);
    }

    public static Identifier textureOrDefault() {
        return getOrFetch().body().texturePath();
    }

    public static PlayerModelType modelType() {
        return getOrFetch().model();
    }

    private static void startLookup() {
        // ProfileResolver.fetchByName is synchronous (network call) — push it to a
        // background thread so we don't stall the render thread on first lookup.
        Thread t = new Thread(() -> {
            try {
                Minecraft mc = Minecraft.getInstance();
                Optional<GameProfile> resolved =
                    mc.services().profileResolver().fetchByName(KangarudeEntity.SKIN_OWNER_NAME);
                if (resolved.isEmpty()) {
                    Ironhold.LOGGER.warn(
                        "[Kangarude] Could not resolve game profile for '{}' — using default skin.",
                        KangarudeEntity.SKIN_OWNER_NAME);
                    return;
                }
                GameProfile profile = resolved.get();
                mc.getSkinManager().get(profile).thenAccept(opt -> opt.ifPresent(skin -> {
                    CACHED.set(skin);
                    Ironhold.LOGGER.info("[Kangarude] Loaded skin for {} ({})",
                        profile.name(), profile.id());
                }));
            } catch (Throwable e) {
                Ironhold.LOGGER.warn("[Kangarude] Skin lookup failed: {}", e.toString());
            }
        }, "Kangarude-Skin-Lookup");
        t.setDaemon(true);
        t.start();
    }
}
