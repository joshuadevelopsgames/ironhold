package kingdom.smp.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side persistent state for Warden Halric's "Quiet the Roads" quest.
 *
 * <p>Per-player tracking: how many separate sessions they've had with Halric,
 * the current quest state, and whether they've already received the staff.
 *
 * <p>Server-wide: an ordered set of staff recipients. The cap keeps the
 * staff genuinely rare — once {@link #STAFF_CAP} players have it, no more
 * offers go out. The order matters for Halric's prompt ("the staff has gone
 * to X, Y, and Z before you").
 */
public class HalricQuestSavedData extends SavedData {

    public enum State {
        /** Player has not yet been offered the quest. */
        NONE,
        /** Halric has given the player the task; they haven't completed it. */
        OFFERED,
        /** Player killed a Mimic; staff awaits collection on next visit. */
        COMPLETED,
        /** Player has the staff. Quest is closed. */
        REWARDED
    }

    /** Hard cap on staffs given out across the entire server. */
    public static final int STAFF_CAP = 8;
    /** Minimum distinct conversation sessions before Halric considers offering. */
    public static final int MIN_SESSIONS_BEFORE_OFFER = 3;

    public static final Codec<HalricQuestSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.unboundedMap(
            Codec.STRING.xmap(UUID::fromString, UUID::toString),
            Codec.INT
        ).optionalFieldOf("sessions", Map.of()).forGetter(d -> d.sessionCounts),
        Codec.unboundedMap(
            Codec.STRING.xmap(UUID::fromString, UUID::toString),
            Codec.STRING.xmap(State::valueOf, State::name)
        ).optionalFieldOf("states", Map.of()).forGetter(d -> d.states),
        Codec.STRING.xmap(UUID::fromString, UUID::toString).listOf()
            .optionalFieldOf("recipients", List.of()).forGetter(d -> List.copyOf(d.recipients))
    ).apply(i, HalricQuestSavedData::new));

    public static final SavedDataType<HalricQuestSavedData> TYPE = new SavedDataType<>(
        Identifier.parse("ironhold:halric_quest"),
        HalricQuestSavedData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, Integer> sessionCounts;
    private final Map<UUID, State> states;
    /** Ordered: first recipient first. */
    private final LinkedHashSet<UUID> recipients;

    public HalricQuestSavedData() {
        this(new HashMap<>(), new HashMap<>(), List.of());
    }

    private HalricQuestSavedData(Map<UUID, Integer> sessionCounts,
                                 Map<UUID, State> states,
                                 List<UUID> recipients) {
        this.sessionCounts = new HashMap<>(sessionCounts);
        this.states = new HashMap<>(states);
        this.recipients = new LinkedHashSet<>(recipients);
    }

    public static HalricQuestSavedData get(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Increment the session count for a player. Called once per fresh conversation. */
    public int incrementSessions(UUID playerId) {
        int n = sessionCounts.getOrDefault(playerId, 0) + 1;
        sessionCounts.put(playerId, n);
        setDirty();
        return n;
    }

    public int sessionsFor(UUID playerId) {
        return sessionCounts.getOrDefault(playerId, 0);
    }

    public State stateFor(UUID playerId) {
        return states.getOrDefault(playerId, State.NONE);
    }

    public void setState(UUID playerId, State state) {
        states.put(playerId, state);
        setDirty();
    }

    public boolean serverHasCapacity() {
        return recipients.size() < STAFF_CAP;
    }

    public int recipientCount() {
        return recipients.size();
    }

    /** Mark a player as a staff recipient (server-wide). Idempotent. */
    public void recordRecipient(UUID playerId) {
        if (recipients.add(playerId)) {
            setDirty();
        }
    }

    /** Recipients in the order they received the staff — used for Halric's prompt. */
    public Set<UUID> recipientsSnapshot() {
        return Set.copyOf(recipients);
    }

    /**
     * True if Halric should consider offering the quest to this player right
     * now. The caller still rolls dice on top of this — eligibility just
     * gates whether the dice are rolled at all.
     */
    public boolean eligibleToOffer(UUID playerId) {
        return stateFor(playerId) == State.NONE
            && sessionsFor(playerId) >= MIN_SESSIONS_BEFORE_OFFER
            && serverHasCapacity();
    }
}
