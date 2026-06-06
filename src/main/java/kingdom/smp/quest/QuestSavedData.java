package kingdom.smp.quest;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Server-level {@link SavedData} of every player's quest progress, keyed by player UUID then quest id.
 * Mirrors {@link kingdom.smp.skill.SkillSavedData}: stored on the overworld, UUID-keyed so it survives
 * relog. Stored under {@code ironhold:player_quests}.
 */
public class QuestSavedData extends SavedData {

    private static final Codec<Map<String, QuestProgress>> PER_PLAYER =
            Codec.unboundedMap(Codec.STRING, QuestProgress.CODEC);

    public static final Codec<QuestSavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(
                    Codec.STRING.xmap(UUID::fromString, UUID::toString),
                    PER_PLAYER
            ).optionalFieldOf("players", Map.of()).forGetter(d -> d.players)
    ).apply(i, QuestSavedData::new));

    public static final SavedDataType<QuestSavedData> TYPE = new SavedDataType<>(
            Identifier.parse("ironhold:player_quests"),
            QuestSavedData::new,
            CODEC,
            DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, Map<String, QuestProgress>> players;

    public QuestSavedData() {
        this(new HashMap<>());
    }

    private QuestSavedData(Map<UUID, Map<String, QuestProgress>> players) {
        this.players = new HashMap<>();
        players.forEach((id, quests) -> this.players.put(id, new LinkedHashMap<>(quests)));
    }

    public static QuestSavedData get(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    /** Progress for one quest, or null if the player has never accepted it. */
    public QuestProgress get(UUID playerId, String questId) {
        Map<String, QuestProgress> quests = players.get(playerId);
        return quests == null ? null : quests.get(questId);
    }

    /** All of a player's quest progress records (any status). Never null. */
    public Collection<QuestProgress> all(UUID playerId) {
        Map<String, QuestProgress> quests = players.get(playerId);
        return quests == null ? java.util.List.of() : new ArrayList<>(quests.values());
    }

    public void put(UUID playerId, QuestProgress progress) {
        players.computeIfAbsent(playerId, id -> new LinkedHashMap<>()).put(progress.questId(), progress);
        setDirty();
    }

    public void remove(UUID playerId, String questId) {
        Map<String, QuestProgress> quests = players.get(playerId);
        if (quests != null && quests.remove(questId) != null) {
            setDirty();
        }
    }
}
