package kingdom.smp.skill;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-level {@link SavedData} keyed by player UUID. Single source of truth for all profession
 * skill-tree state on the server. Per spec §8.5 — chosen over per-player attachment because
 * server-first milestones need a shared registry, admin export is easier, and player UUIDs
 * are stable across login/logout cycles.
 *
 * Stored under {@code ironhold:player_skills} in the overworld's data folder.
 *
 * Access:
 * <pre>
 * SkillSavedData data = SkillSavedData.get(serverLevel);
 * PlayerSkillState state = data.stateFor(playerUuid);
 * data.setState(playerUuid, state.trySpendOn(Profession.BLACKSMITHING));
 * </pre>
 *
 * @see <a href="../../../../specs/profession-skill-system.md">profession-skill-system.md §8.5</a>
 */
public class SkillSavedData extends SavedData {

    public static final Codec<SkillSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString),
                    PlayerSkillState.CODEC
            ).optionalFieldOf("players", Map.of()).forGetter(d -> d.players),
            Codec.STRING.listOf().optionalFieldOf("server_firsts_claimed", java.util.List.of())
                    .forGetter(d -> new java.util.ArrayList<>(d.serverFirstsClaimed))
    ).apply(i, (players, firsts) -> new SkillSavedData(players, new java.util.HashSet<>(firsts))));

    public static final SavedDataType<SkillSavedData> TYPE = new SavedDataType<>(
            Identifier.parse("ironhold:player_skills"),
            SkillSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, PlayerSkillState> players;
    private final Set<String> serverFirstsClaimed;

    public SkillSavedData() {
        this(new HashMap<>(), new java.util.HashSet<>());
    }

    private SkillSavedData(Map<UUID, PlayerSkillState> players, Set<String> serverFirstsClaimed) {
        this.players = new HashMap<>(players);
        this.serverFirstsClaimed = new java.util.HashSet<>(serverFirstsClaimed);
    }

    /** Fetch the SavedData from the overworld's storage. Creates a fresh instance if absent. */
    public static SkillSavedData get(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Get state for a player, creating the default fresh state if it doesn't exist yet. */
    public PlayerSkillState stateFor(UUID playerId) {
        return players.computeIfAbsent(playerId, id -> {
            setDirty();
            return PlayerSkillState.fresh();
        });
    }

    public void setState(UUID playerId, PlayerSkillState state) {
        players.put(playerId, state);
        setDirty();
    }

    /**
     * Reset a player's state to the fresh defaults. Returns the reset state.
     */
    public PlayerSkillState reset(UUID playerId) {
        PlayerSkillState fresh = PlayerSkillState.fresh();
        players.put(playerId, fresh);
        setDirty();
        return fresh;
    }

    /**
     * Try to claim a server-first milestone. Returns true on success (this is the first claim),
     * false if already claimed by some other player. Caller is responsible for awarding the
     * milestone to the player on success.
     */
    public boolean tryClaimServerFirst(String milestoneId) {
        if (serverFirstsClaimed.contains(milestoneId)) return false;
        serverFirstsClaimed.add(milestoneId);
        setDirty();
        return true;
    }

    public boolean isServerFirstClaimed(String milestoneId) {
        return serverFirstsClaimed.contains(milestoneId);
    }
}
