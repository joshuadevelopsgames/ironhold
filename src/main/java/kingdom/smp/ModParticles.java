package kingdom.smp;

import net.minecraft.core.particles.ParticleType;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.core.registries.Registries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Custom particle-type registrations, split out of {@link Ironhold}. */
public final class ModParticles {
    private ModParticles() {}

    public static final DeferredRegister<ParticleType<?>> PARTICLE_TYPES =
        DeferredRegister.create(Registries.PARTICLE_TYPE, Ironhold.MODID);

    // Tiny orange-white forge sparks. Both share the same render class; only the frame
    // order (defined in their particle JSONs) differs:
    //   iron_spark       — cooldown: white-gold star -> orange -> dim ember (frames 0,1,2,3)
    //   iron_spark_flare — flare:    dim ember -> white-gold star -> settle (frames 3,0,1,2)
    //   iron_spark_pulse — bounce:   dim ember -> flare to star -> fade back (frames 3,2,1,0,1,2)
    // Anonymous subclass {} is required: SimpleParticleType's constructor is protected,
    // so it is only reachable through a subclass.
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> IRON_SPARK =
        PARTICLE_TYPES.register("iron_spark", () -> new SimpleParticleType(false) {});
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> IRON_SPARK_FLARE =
        PARTICLE_TYPES.register("iron_spark_flare", () -> new SimpleParticleType(false) {});
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> IRON_SPARK_PULSE =
        PARTICLE_TYPES.register("iron_spark_pulse", () -> new SimpleParticleType(false) {});
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PLAGUE_FLEA =
        PARTICLE_TYPES.register("plague_flea", () -> new SimpleParticleType(false) {});
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> PLAGUE_SPORE =
        PARTICLE_TYPES.register("plague_spore", () -> new SimpleParticleType(false) {});
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> DIAMOND_SCEPTER_SPARK =
        PARTICLE_TYPES.register("diamond_scepter_spark", () -> new SimpleParticleType(false) {});
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> SHROOMLING_SPORE =
        PARTICLE_TYPES.register("shroomling_spore", () -> new SimpleParticleType(false) {});
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> ORANGE_SHROOMLING_SPORE =
        PARTICLE_TYPES.register("orange_shroomling_spore", () -> new SimpleParticleType(false) {});
    public static final DeferredHolder<ParticleType<?>, SimpleParticleType> LUNAR_LEVITY_MOTE =
        PARTICLE_TYPES.register("lunar_levity_mote", () -> new SimpleParticleType(false) {});

    public static void register(IEventBus modEventBus) {
        PARTICLE_TYPES.register(modEventBus);
    }
}
