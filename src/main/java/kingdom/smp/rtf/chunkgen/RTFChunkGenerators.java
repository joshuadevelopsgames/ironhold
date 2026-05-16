package kingdom.smp.rtf.chunkgen;

import kingdom.smp.rtf.platform.RegistryUtil;
import net.minecraft.core.registries.BuiltInRegistries;

public final class RTFChunkGenerators {
    private RTFChunkGenerators() {
    }

    public static void bootstrap() {
        RegistryUtil.register(BuiltInRegistries.CHUNK_GENERATOR, "rtf", RTFChunkGenerator.CODEC);
        RegistryUtil.register(BuiltInRegistries.BIOME_SOURCE, "rtf", RTFBiomeSource.CODEC);
    }
}
