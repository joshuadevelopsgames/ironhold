package kingdom.smp;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/** Sound-event registrations, split out of {@link Ironhold}. */
public final class ModSounds {
    private ModSounds() {}

    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
        DeferredRegister.create(Registries.SOUND_EVENT, Ironhold.MODID);

    private static DeferredHolder<SoundEvent, SoundEvent> variableRange(String path) {
        return SOUND_EVENTS.register(path,
            () -> SoundEvent.createVariableRangeEvent(Identifier.fromNamespaceAndPath(Ironhold.MODID, path)));
    }

    // Ebonwood Hollow ambient loop — sound by CreativeMD / AmbientSounds mod (LGPL-3.0)
    // https://github.com/CreativeMD/AmbientSounds — credit required
    public static final DeferredHolder<SoundEvent, SoundEvent> EBONWOOD_AMBIENT =
        variableRange("ambient.ebonwood_hollow");

    public static final DeferredHolder<SoundEvent, SoundEvent> HALRIC_STAFF_CHAIN_CLINK =
        variableRange("item.halric_staff.chain_clink");

    public static final DeferredHolder<SoundEvent, SoundEvent> PINK_DEER_AMBIENT =
        variableRange("entity.pink_deer.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> PINK_DEER_HURT =
        variableRange("entity.pink_deer.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> PINK_DEER_DEATH =
        variableRange("entity.pink_deer.death");
    public static final DeferredHolder<SoundEvent, SoundEvent> PINK_DEER_MOM_HURT =
        variableRange("entity.pink_deer.mom.hurt");

    /** Dramatic sonic-boom transition cue for the Kangabrine descent. ~4s long. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KANGABRINE_DRAMATIC_DESCENT =
        variableRange("entity.kangabrine.dramatic_descent");

    /** Fast, nerving, eerie 30-second ambient — plays during the Kangabrine STALKING phase. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KANGABRINE_EERIE_AMBIENT =
        variableRange("entity.kangabrine.eerie_ambient");

    /** Horrifying banshee scream — used as a strike payoff during ESCALATED. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KANGABRINE_BANSHEE_SCREAM =
        variableRange("entity.kangabrine.banshee_scream");

    /** Dark haunted-atmosphere evil laugh — used as a random ambient sting. */
    public static final DeferredHolder<SoundEvent, SoundEvent> KANGABRINE_EVIL_LAUGH =
        variableRange("entity.kangabrine.evil_laugh");

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
