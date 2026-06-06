package kingdom.smp.moon;

import kingdom.smp.Ironhold;
import kingdom.smp.rtf.platform.RegistryUtil;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.Level;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

public class ModMoonDimensions {
    public static final ResourceKey<Level> MOON_LEVEL = ResourceKey.create(Registries.DIMENSION, 
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "moon_dimension"));

    public static final DeferredRegister<com.mojang.serialization.MapCodec<? extends net.minecraft.world.level.chunk.ChunkGenerator>> CHUNK_GENERATORS =
            DeferredRegister.create(Registries.CHUNK_GENERATOR, Ironhold.MODID);

    static {
        CHUNK_GENERATORS.register("moon_chunk_generator", () -> MoonChunkGenerator.CODEC);
    }

    public static void register(IEventBus bus) {
        CHUNK_GENERATORS.register(bus);
    }
}
