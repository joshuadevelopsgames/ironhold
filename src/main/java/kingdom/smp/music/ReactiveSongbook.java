package kingdom.smp.music;

import kingdom.smp.ModSounds;
import net.minecraft.client.Minecraft;
import net.minecraft.client.sounds.MusicManager;
import net.minecraft.sounds.Music;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.registries.DeferredHolder;

/**
 * Maps the active {@link MusicTrigger}s onto a track from the bundled "Adventure Redefined"
 * pack (transcoded from CircuitLord's ReactiveMusic — see THIRD_PARTY.md). Priority-ordered,
 * first match wins, mirroring how ReactiveMusic prioritises songpack entries by order.
 *
 * <p>Each situation has a <b>pool</b> of tracks; one is picked at random and held stable for
 * the duration of that song (we don't re-roll every tick — that would make vanilla's
 * {@link MusicManager} thrash). When the current song finishes, the next pick uses the
 * then-current situation, so the soundtrack drifts with the world. Combat/boss situations
 * use {@code replaceCurrentMusic=true} so they cut in immediately over ambient music.
 *
 * <p>The pools are a clean-room re-expression of ReactiveMusic's {@code ReactiveMusic.yaml}
 * mapping, folded onto the triggers Ironhold's engine evaluates (its HOME / SNOW / per-biome /
 * block-scan entries collapse into the nearest DAY/NIGHT pool so every track stays reachable).
 */
public final class ReactiveSongbook {
    private ReactiveSongbook() {}

    public enum Action { PLAY, SILENCE, PASS }

    public record Result(Action action, Music music) {
        static final Result SILENCE = new Result(Action.SILENCE, null);
        static Result play(Music m) { return new Result(Action.PLAY, m); }
    }

    // Ambient songs leave a short gap before the next; combat re-picks immediately.
    private static final int AMBIENT_MIN_DELAY = 600;   // 30s
    private static final int AMBIENT_MAX_DELAY = 2400;  // 2min

    private static final RandomSource RANDOM = RandomSource.create();

    // ── Track pools (ids resolve to streamed SoundEvents in ModSounds.MUSIC) ──
    private static final String[] MENU = {
        "du_nock_street_title_edit", "the_shapers_realm", "mirrormere", "frozen_in_time", "reverie" };
    private static final String[] BOSS = { "last_stand", "ruthless" };
    private static final String[] PVP_CLIMAX = { "last_stand", "ode_to_the_fallen", "wildfire_p2" };
    private static final String[] PVP_SKIRMISH = { "for_the_king", "ruthless", "wildfire_p1", "deliverance" };
    private static final String[] UNDERWATER = { "eventide", "shimmering_in_the_shallows" };
    private static final String[] NETHER = {
        "mufaya_dark_ascension", "the_shapers_realm", "storm", "crann_na_beatha", "world_unbound" };
    private static final String[] END = {
        "du_nock_street_title_edit", "mufaya_dark_ascension", "storm", "the_shapers_realm",
        "winters_breath", "world_unbound" };
    private static final String[] UNDERGROUND = {
        "winters_breath", "into_silence_p1", "world_unbound", "storm", "sleeper" };
    private static final String[] SUNSET = { "alvae", "siochain_shuthain", "sombre", "fall_of_the_leaf" };
    private static final String[] SUNRISE = { "alvae", "siochain_shuthain", "sombre" };
    private static final String[] RAIN = {
        "world_unbound", "storm", "mufaya_dark_ascension", "moonsong", "keeper_of_the_forest",
        "into_silence_p1", "du_nock_street_title_edit", "crystal_forest", "frozen_in_time" };
    private static final String[] NIGHT = {
        "into_silence_p1", "moonsong", "crystal_forest", "storm", "alvae", "celtic_lore",
        "fall_of_the_leaf", "into_silence_p2", "ode_to_the_fallen", "sleeper", "sombre",
        "the_shapers_realm", "frozen_in_time", "quests_end", "shimmering_in_the_shallows",
        "winters_night" };
    private static final String[] DAY = {
        "a_celtic_tale", "alpha", "a_song_from_the_deep", "beautiful_dreams", "celtic_lore",
        "deliverance", "into_the_unknown", "keeper_of_the_forest", "kingdom_of_bards", "mourning",
        "never_to_return", "ode_to_the_fallen", "reverie", "sacred_earth", "woodland_tales",
        "where_i_belong", "what_lies_beyond", "wanderer", "unsung_heroes", "mirrormere",
        "night_at_the_eolian", "wildfire_p1", "wildfire_p2", "last_light", "wanderer_p2",
        "spring_morning", "fairy_tale", "for_the_king", "freedom", "breath_of_the_forest",
        "myth", "castle_in_the_sky", "circle_of_life", "cliffs_of_moher", "clarity",
        "the_gate_to_avalon", "mufaya_song1", "du_nock_street" };

    // Held selection so we don't re-roll mid-song.
    private static String currentId;
    private static Music currentMusic;
    private static boolean currentIsCombat;

    public static Result select(ReactiveMusicState s) {
        String[] pool;
        boolean combat;

        if (s.is(MusicTrigger.PVP_CLIMAX))      { pool = PVP_CLIMAX;   combat = true; }
        else if (s.is(MusicTrigger.BOSS))       { pool = BOSS;         combat = true; }
        else if (s.is(MusicTrigger.PVP_SKIRMISH)){ pool = PVP_SKIRMISH; combat = true; }
        else if (s.is(MusicTrigger.IN_DIALOGUE)) return Result.SILENCE; // duck under NPC voice
        else if (s.is(MusicTrigger.MAIN_MENU))  { pool = MENU;         combat = false; }
        else if (s.is(MusicTrigger.UNDERWATER)) { pool = UNDERWATER;   combat = false; }
        else if (s.is(MusicTrigger.NETHER))     { pool = NETHER;       combat = false; }
        else if (s.is(MusicTrigger.END))        { pool = END;          combat = false; }
        else if (s.is(MusicTrigger.UNDERGROUND) || s.is(MusicTrigger.DEEP_UNDERGROUND))
                                                { pool = UNDERGROUND;  combat = false; }
        else if (s.is(MusicTrigger.SUNSET))     { pool = SUNSET;       combat = false; }
        else if (s.is(MusicTrigger.SUNRISE))    { pool = SUNRISE;      combat = false; }
        else if (s.is(MusicTrigger.STORM) || s.is(MusicTrigger.RAIN))
                                                { pool = RAIN;         combat = false; }
        else if (s.is(MusicTrigger.NIGHT))      { pool = NIGHT;        combat = false; }
        else                                    { pool = DAY;          combat = false; }

        // Hold the current song while it's still playing — unless combat needs to cut in
        // over a non-combat track. This keeps MusicManager from thrashing each tick.
        MusicManager mm = Minecraft.getInstance().getMusicManager();
        if (currentMusic != null && mm.isPlayingMusic(currentMusic)) {
            boolean mustInterrupt = combat && !currentIsCombat;
            if (!mustInterrupt) return Result.play(currentMusic);
        }

        String id = roll(pool);
        DeferredHolder<?, ?> holder = ModSounds.MUSIC.get(id);
        if (holder == null) return Result.play(currentMusic != null ? currentMusic : fallback());

        int minDelay = combat ? 0 : AMBIENT_MIN_DELAY;
        int maxDelay = combat ? 0 : AMBIENT_MAX_DELAY;
        currentMusic = new Music(ModSounds.MUSIC.get(id), minDelay, maxDelay, combat);
        currentId = id;
        currentIsCombat = combat;
        return Result.play(currentMusic);
    }

    /** Random track from the pool, avoiding an immediate repeat when possible. */
    private static String roll(String[] pool) {
        if (pool.length == 1) return pool[0];
        String pick = pool[RANDOM.nextInt(pool.length)];
        if (pick.equals(currentId)) pick = pool[RANDOM.nextInt(pool.length)];
        return pick;
    }

    private static Music fallback() {
        return new Music(ModSounds.MUSIC.get(DAY[0]), AMBIENT_MIN_DELAY, AMBIENT_MAX_DELAY, false);
    }
}
