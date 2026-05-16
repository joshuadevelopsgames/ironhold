package kingdom.smp.ai;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Server-side persistent registry of which voiced NPCs each player has muted.
 * Per-player, per-NPC-tag — tags come from {@link NpcChatPartner#tag()}
 * (e.g. {@code "Kangarude"}, {@code "Halric"}). Stored as overworld SavedData
 * so mutes survive restart and follow the player across sessions.
 *
 * <p>Read this on the speaking path — see
 * {@link kingdom.smp.entity.WardenHalricEntity#speakLine(String)} and the
 * Kangarude equivalent — to drop the TTS call entirely when the partner has
 * muted the speaking NPC.
 */
public class NpcMuteRegistry extends SavedData {

    public static final Codec<NpcMuteRegistry> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.unboundedMap(
            Codec.STRING.xmap(UUID::fromString, UUID::toString),
            Codec.STRING.listOf().xmap(HashSet::new, java.util.ArrayList::new)
        ).optionalFieldOf("muted", Map.of()).forGetter(d -> d.muted)
    ).apply(i, NpcMuteRegistry::new));

    public static final SavedDataType<NpcMuteRegistry> TYPE = new SavedDataType<>(
        Identifier.parse("ironhold:npc_mutes"),
        NpcMuteRegistry::new,
        CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<UUID, HashSet<String>> muted;

    public NpcMuteRegistry() {
        this(new HashMap<>());
    }

    private NpcMuteRegistry(Map<UUID, HashSet<String>> muted) {
        this.muted = new HashMap<>();
        muted.forEach((k, v) -> this.muted.put(k, new HashSet<>(v)));
    }

    public static NpcMuteRegistry get(ServerLevel anyLevel) {
        ServerLevel overworld = anyLevel.getServer().overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    /** True if this player has muted the NPC identified by {@code tag}. */
    public boolean isMuted(UUID playerId, String tag) {
        if (playerId == null || tag == null) return false;
        Set<String> tags = muted.get(playerId);
        return tags != null && tags.contains(tag);
    }

    public void setMuted(UUID playerId, String tag, boolean isMuted) {
        if (playerId == null || tag == null) return;
        if (isMuted) {
            muted.computeIfAbsent(playerId, id -> new HashSet<>()).add(tag);
        } else {
            Set<String> tags = muted.get(playerId);
            if (tags == null) return;
            tags.remove(tag);
            if (tags.isEmpty()) muted.remove(playerId);
        }
        setDirty();
    }
}
