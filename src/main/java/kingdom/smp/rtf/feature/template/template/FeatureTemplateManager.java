package kingdom.smp.rtf.feature.template.template;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import com.google.common.collect.ImmutableList;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.packs.resources.ResourceManager;

public class FeatureTemplateManager {
    private static volatile FeatureTemplateManager INSTANCE;

    private final MinecraftServer server;
    private volatile ResourceManager resourceManager;
    private final Map<Identifier, FeatureTemplate> cache = new ConcurrentHashMap<>();

    public FeatureTemplateManager(MinecraftServer server, ResourceManager resourceManager) {
        this.server = server;
        this.resourceManager = resourceManager;
    }

    public static void install(MinecraftServer server, ResourceManager resourceManager) {
        INSTANCE = new FeatureTemplateManager(server, resourceManager);
    }

    public static FeatureTemplateManager get() {
        FeatureTemplateManager mgr = INSTANCE;
        if (mgr == null) {
            throw new IllegalStateException("FeatureTemplateManager not installed yet (server not started?)");
        }
        return mgr;
    }

    public void onReload(ResourceManager resourceManager) {
        this.resourceManager = resourceManager;
        this.cache.clear();
    }

    public FeatureTemplate load(Identifier location) {
        return this.cache.computeIfAbsent(location, this::read);
    }

    private FeatureTemplate read(Identifier location) {
        return this.resourceManager.getResource(location).flatMap((resource) -> {
            try (InputStream stream = resource.open()) {
                return FeatureTemplate.load(this.server.registryAccess().lookupOrThrow(Registries.BLOCK).filterFeatures(this.server.getWorldData().enabledFeatures()), stream);
            } catch (IOException e) {
                e.printStackTrace();
                return Optional.empty();
            }
        }).orElse(new FeatureTemplate(ImmutableList.of()));
    }
}
