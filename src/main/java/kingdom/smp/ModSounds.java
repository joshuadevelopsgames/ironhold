package kingdom.smp;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

/**
 * Sound-event registrations, split out of {@link Ironhold}.
 *
 * <p><b>Stubbed:</b> the underlying .ogg assets and {@code sounds.json} mappings
 * have been removed for now to keep the jar small. The registrations below remain
 * so call sites keep compiling; playing one is a silent no-op (MC logs a single
 * "unknown sound" warning) until the assets are restored.
 */
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

    // Cute little Shroomling voice — soft, mushroomy chirps/coos (NOT slime squelches).
    // Mapped in sounds.json; .ogg assets pending under sounds/entity/shroomling/.
    public static final DeferredHolder<SoundEvent, SoundEvent> SHROOMLING_AMBIENT =
        variableRange("entity.shroomling.ambient");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHROOMLING_HURT =
        variableRange("entity.shroomling.hurt");
    public static final DeferredHolder<SoundEvent, SoundEvent> SHROOMLING_DEATH =
        variableRange("entity.shroomling.death");

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

    // ── Reactive music (kingdom.smp.music) ─────────────────────────────────
    // Streamed soundtrack tracks selected per-tick by the reactive-music engine and
    // handed to vanilla's MusicManager via SelectMusicEvent. The pack ("Adventure
    // Redefined") is transcoded from CircuitLord's ReactiveMusic — see THIRD_PARTY.md
    // for composer attribution. One streamed SoundEvent per ogg under sounds/music/.
    public static final String[] MUSIC_TRACK_IDS = {
        "a_celtic_tale", "a_song_from_the_deep", "alpha", "alvae", "beautiful_dreams",
        "breath_of_the_forest", "castle_in_the_sky", "celtic_lore", "circle_of_life",
        "clarity", "cliffs_of_moher", "crann_na_beatha", "crystal_forest", "deliverance",
        "du_nock_street", "du_nock_street_title_edit", "eventide", "fairy_tale",
        "fall_of_the_leaf", "for_the_king", "freedom", "frozen_in_time", "into_silence_p1",
        "into_silence_p2", "into_the_unknown", "keeper_of_the_forest", "kingdom_of_bards",
        "last_light", "last_stand", "mirrormere", "moonsong", "mourning",
        "mufaya_dark_ascension", "mufaya_song1", "myth", "never_to_return",
        "night_at_the_eolian", "ode_to_the_fallen", "quests_end", "reverie", "ruthless",
        "sacred_earth", "shimmering_in_the_shallows", "siochain_shuthain", "sleeper",
        "sombre", "spring_morning", "storm", "the_gate_to_avalon", "the_shapers_realm",
        "unsung_heroes", "wanderer", "wanderer_p2", "what_lies_beyond", "where_i_belong",
        "wildfire_p1", "wildfire_p2", "winters_breath", "winters_night", "woodland_tales",
        "world_unbound"
    };

    /** id → registered streamed SoundEvent holder, in declaration order. */
    public static final java.util.Map<String, DeferredHolder<SoundEvent, SoundEvent>> MUSIC =
        registerMusicTracks();

    private static java.util.Map<String, DeferredHolder<SoundEvent, SoundEvent>> registerMusicTracks() {
        java.util.Map<String, DeferredHolder<SoundEvent, SoundEvent>> m = new java.util.LinkedHashMap<>();
        for (String id : MUSIC_TRACK_IDS) {
            m.put(id, variableRange("music.ironhold." + id));
        }
        return m;
    }

    public static void register(IEventBus modEventBus) {
        SOUND_EVENTS.register(modEventBus);
    }
}
