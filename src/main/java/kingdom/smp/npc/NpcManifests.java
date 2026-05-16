package kingdom.smp.npc;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kingdom.smp.Ironhold;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.jspecify.annotations.Nullable;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Datapack-driven registry of {@link NpcManifest}s. Files live at
 * {@code data/<namespace>/npc_manifests/<path>.json}, e.g.
 * {@code data/ironhold/npc_manifests/profession/blacksmith.json} resolves to
 * {@code ironhold:profession/blacksmith}.
 *
 * <p>NPCs look themselves up via {@link #resolve(Identifier, Identifier)} — pass
 * the specific NPC id first, the profession fallback second. If neither exists,
 * {@link NpcManifest#EMPTY} is returned (no wares, no secrets, commoner tier).
 */
public final class NpcManifests extends SimplePreparableReloadListener<Map<Identifier, NpcManifest>> {

    public static final Identifier LISTENER_ID =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "npc_manifests");

    private static final String DIRECTORY = "npc_manifests";
    private static final String SUFFIX = ".json";
    private static final Gson GSON = new Gson();

    private static volatile Map<Identifier, NpcManifest> manifests = Map.of();

    public static @Nullable NpcManifest get(Identifier id) {
        return id == null ? null : manifests.get(id);
    }

    public static NpcManifest resolve(@Nullable Identifier specific, @Nullable Identifier fallback) {
        NpcManifest m = specific == null ? null : manifests.get(specific);
        if (m != null) return m;
        m = fallback == null ? null : manifests.get(fallback);
        return m != null ? m : NpcManifest.EMPTY;
    }

    public static int loadedCount() { return manifests.size(); }

    @Override
    protected Map<Identifier, NpcManifest> prepare(ResourceManager mgr, ProfilerFiller profiler) {
        Map<Identifier, NpcManifest> out = new HashMap<>();
        String prefix = DIRECTORY + "/";

        Map<Identifier, Resource> resources = mgr.listResources(DIRECTORY,
            rl -> rl.getPath().endsWith(SUFFIX));

        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier full = entry.getKey();
            String path = full.getPath();
            if (!path.startsWith(prefix) || !path.endsWith(SUFFIX)) continue;
            String inner = path.substring(prefix.length(), path.length() - SUFFIX.length());
            Identifier id = Identifier.fromNamespaceAndPath(full.getNamespace(), inner);

            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonObject json = JsonParser.parseReader(reader).getAsJsonObject();
                out.put(id, NpcManifest.parse(id, json));
            } catch (Exception e) {
                Ironhold.LOGGER.warn("[NpcManifests] Failed to load {}: {}", full, e.getMessage());
            }
        }
        return out;
    }

    @Override
    protected void apply(Map<Identifier, NpcManifest> data, ResourceManager mgr, ProfilerFiller p) {
        manifests = Map.copyOf(data);
        Ironhold.LOGGER.info("[NpcManifests] Loaded {} NPC manifest(s).", data.size());
    }
}
