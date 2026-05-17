package kingdom.smp.dynlight;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kingdom.smp.Ironhold;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimplePreparableReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.util.HashMap;
import java.util.Map;

/**
 * Reads {@code assets/<ns>/dynamiclights/item/*.json} and {@code .../dynamiclights/entity/*.json}
 * into the static caches consulted by {@link ItemLightRegistry} and {@link EntityLightRegistry}.
 *
 * <p>JSON shape (matches the existing files under {@code src/main/resources/assets/ironhold/dynamiclights/}):
 * <pre>{ "match": { "items": "ns:id" }, "luminance": 12 }</pre>
 * Also accepted: {@code "match.items"} or {@code "match.entity_type"} as a JSON array of ids.
 */
public final class DynamicLightLoader extends SimplePreparableReloadListener<DynamicLightLoader.Loaded> {

    public static final Identifier LISTENER_ID =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "dynamic_lights");

    private static final Logger LOG = LoggerFactory.getLogger("ironhold/dynlight");
    private static final String ITEM_DIR = "dynamiclights/item";
    private static final String ENTITY_DIR = "dynamiclights/entity";
    private static final String SUFFIX = ".json";

    /** Captured registry maps; applied to the registries on {@link #apply}. */
    public record Loaded(Map<Identifier, Integer> items, Map<Identifier, Integer> entities) {}

    @Override
    protected Loaded prepare(ResourceManager mgr, ProfilerFiller profiler) {
        return new Loaded(loadDir(mgr, ITEM_DIR, "items"), loadDir(mgr, ENTITY_DIR, "entity_type"));
    }

    @Override
    protected void apply(Loaded data, ResourceManager mgr, ProfilerFiller profiler) {
        ItemLightRegistry.setRegistered(data.items);
        EntityLightRegistry.setRegistered(data.entities);
        LOG.info("Loaded {} item light source(s) and {} entity light source(s)",
            data.items.size(), data.entities.size());
    }

    private static Map<Identifier, Integer> loadDir(ResourceManager mgr, String dir, String matchKey) {
        Map<Identifier, Integer> out = new HashMap<>();
        String prefix = dir + "/";
        Map<Identifier, Resource> resources = mgr.listResources(dir,
            rl -> rl.getPath().endsWith(SUFFIX));
        for (Map.Entry<Identifier, Resource> entry : resources.entrySet()) {
            Identifier file = entry.getKey();
            try (BufferedReader reader = entry.getValue().openAsReader()) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) continue;
                JsonObject root = parsed.getAsJsonObject();
                if (!root.has("luminance") || !root.has("match")) continue;
                int luminance = root.get("luminance").getAsInt();
                if (luminance <= 0) continue;
                JsonObject match = root.getAsJsonObject("match");
                JsonElement m = match.get(matchKey);
                if (m == null) continue;
                if (m.isJsonPrimitive()) {
                    addId(out, m.getAsString(), luminance);
                } else if (m.isJsonArray()) {
                    JsonArray arr = m.getAsJsonArray();
                    for (JsonElement el : arr) addId(out, el.getAsString(), luminance);
                }
            } catch (Exception ex) {
                LOG.warn("Failed to parse dynamic-light entry {}: {}", file, ex.getMessage());
            }
        }
        return out;
    }

    private static void addId(Map<Identifier, Integer> out, String idStr, int luminance) {
        if (idStr == null || idStr.isEmpty() || idStr.startsWith("#")) return;
        Identifier id = Identifier.tryParse(idStr);
        if (id == null) return;
        out.merge(id, luminance, Math::max);
    }
}
