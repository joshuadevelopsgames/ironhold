package kingdom.smp.npc;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-player Stardew-style bond state with each NPC. Persisted via data
 * attachment on the player; copy-on-death so relationships survive respawn.
 *
 * <p>Keyed by NPC tag (e.g. {@code "ironhold:warden_halric"}). Each entry
 * holds the running rapport score (-100..+1000), today's gift count, and the
 * last day a gift was given (server day number = gameTime / 24000).
 *
 * <p>This is the v1 server-side store; UI rendering (the Bonds page) will
 * consume snapshots of this via a sync payload in a follow-up.
 */
public record PlayerNpcBonds(Map<String, Entry> bonds) {

    public record Entry(int rapport, int giftsToday, long lastGiftDay, long lastTalkDay, int claimedMilestone) {
        public static final Entry EMPTY = new Entry(0, 0, -1, -1, 0);

        public static final Codec<Entry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.INT.optionalFieldOf("rapport", 0).forGetter(Entry::rapport),
            Codec.INT.optionalFieldOf("gifts_today", 0).forGetter(Entry::giftsToday),
            Codec.LONG.optionalFieldOf("last_gift_day", -1L).forGetter(Entry::lastGiftDay),
            Codec.LONG.optionalFieldOf("last_talk_day", -1L).forGetter(Entry::lastTalkDay),
            Codec.INT.optionalFieldOf("claimed_milestone", 0).forGetter(Entry::claimedMilestone))
            .apply(i, Entry::new));

        public static final StreamCodec<RegistryFriendlyByteBuf, Entry> STREAM_CODEC =
            StreamCodec.composite(
                ByteBufCodecs.VAR_INT,  Entry::rapport,
                ByteBufCodecs.VAR_INT,  Entry::giftsToday,
                ByteBufCodecs.VAR_LONG, Entry::lastGiftDay,
                ByteBufCodecs.VAR_LONG, Entry::lastTalkDay,
                ByteBufCodecs.VAR_INT,  Entry::claimedMilestone,
                Entry::new);
    }

    public static final MapCodec<PlayerNpcBonds> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
            Codec.unboundedMap(Codec.STRING, Entry.CODEC)
                .optionalFieldOf("bonds", Map.of())
                .forGetter(PlayerNpcBonds::bonds))
            .apply(i, PlayerNpcBonds::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerNpcBonds> STREAM_CODEC =
        ByteBufCodecs.<RegistryFriendlyByteBuf, String, Entry, HashMap<String, Entry>>map(
                HashMap::new, ByteBufCodecs.STRING_UTF8, Entry.STREAM_CODEC)
            .map(PlayerNpcBonds::new, b -> new HashMap<>(b.bonds()));

    public static PlayerNpcBonds empty() {
        return new PlayerNpcBonds(Map.of());
    }

    public Entry get(String npcTag) {
        Entry e = bonds.get(npcTag);
        return e != null ? e : Entry.EMPTY;
    }

    public int rapportFor(String npcTag) {
        return get(npcTag).rapport();
    }

    /** Returns a new bonds map with the rapport for {@code npcTag} clamped to [-100, 1000]. */
    public PlayerNpcBonds withRapport(String npcTag, int newRapport) {
        Entry cur = get(npcTag);
        Entry next = new Entry(clampRapport(newRapport), cur.giftsToday(), cur.lastGiftDay(), cur.lastTalkDay(), cur.claimedMilestone());
        return withEntry(npcTag, next);
    }

    /**
     * Apply a gift delta and bump today's gift counter. If {@code today} differs from
     * the entry's {@code lastGiftDay}, the counter resets to 1; otherwise it increments.
     */
    public PlayerNpcBonds applyGift(String npcTag, int rapportDelta, long today) {
        Entry cur = get(npcTag);
        int gifts = (today == cur.lastGiftDay()) ? cur.giftsToday() + 1 : 1;
        int newRapport = clampRapport(cur.rapport() + rapportDelta);
        return withEntry(npcTag, new Entry(newRapport, gifts, today, cur.lastTalkDay(), cur.claimedMilestone()));
    }

    /** Has the player already earned today's conversation rapport from this NPC? */
    public boolean talkedToday(String npcTag, long today) {
        return get(npcTag).lastTalkDay() == today;
    }

    /**
     * Grant the once-per-day conversation rapport bump and stamp today's talk day.
     * Callers should gate on {@link #talkedToday} so the bump only lands once per day.
     */
    public PlayerNpcBonds withDailyTalk(String npcTag, int gain, long today) {
        Entry cur = get(npcTag);
        return withEntry(npcTag,
            new Entry(clampRapport(cur.rapport() + gain), cur.giftsToday(), cur.lastGiftDay(), today, cur.claimedMilestone()));
    }

    /** Record that the milestone at rapport threshold {@code rapport} has been delivered. */
    public PlayerNpcBonds withClaimedMilestone(String npcTag, int rapport) {
        Entry cur = get(npcTag);
        return withEntry(npcTag,
            new Entry(cur.rapport(), cur.giftsToday(), cur.lastGiftDay(), cur.lastTalkDay(), rapport));
    }

    /** Has the player already given the daily allotment of gifts to this NPC today? */
    public boolean giftCapReachedToday(String npcTag, long today, int dailyCap) {
        Entry cur = get(npcTag);
        return today == cur.lastGiftDay() && cur.giftsToday() >= dailyCap;
    }

    private PlayerNpcBonds withEntry(String npcTag, Entry next) {
        Map<String, Entry> copy = new HashMap<>(bonds);
        copy.put(npcTag, next);
        return new PlayerNpcBonds(Map.copyOf(copy));
    }

    private static int clampRapport(int v) {
        return Math.max(-100, Math.min(1000, v));
    }
}
