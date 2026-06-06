package kingdom.smp.perms;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Server-level {@link SavedData} holding role definitions, per-player role assignments, and per-player
 * overrides. Stored under {@code ironhold:permissions} in the overworld's data folder, mirroring
 * {@code SkillSavedData}. (Event-sourced persistence — an append-only audit log — is a later slice;
 * this snapshot store validates the resolver and explain UX first.)
 */
public class PermissionData extends SavedData {

    private static final Codec<UUID> UUID_CODEC = Codec.STRING.xmap(UUID::fromString, UUID::toString);
    private static final Codec<Map<String, PermState>> OVERRIDE_MAP_CODEC =
        Codec.unboundedMap(Codec.STRING, Role.STATE_CODEC);

    public static final Codec<PermissionData> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.unboundedMap(Codec.STRING, Role.CODEC).optionalFieldOf("roles", Map.of())
            .forGetter(d -> d.roles),
        Codec.unboundedMap(UUID_CODEC, Codec.STRING.listOf()).optionalFieldOf("player_roles", Map.of())
            .forGetter(d -> d.playerRoles),
        Codec.unboundedMap(UUID_CODEC, OVERRIDE_MAP_CODEC).optionalFieldOf("overrides", Map.of())
            .forGetter(d -> d.overrides)
    ).apply(i, PermissionData::new));

    public static final SavedDataType<PermissionData> TYPE = new SavedDataType<>(
        Identifier.parse("ironhold:permissions"),
        PermissionData::new,
        CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Map<String, Role> roles;
    private final Map<UUID, List<String>> playerRoles;
    private final Map<UUID, Map<String, PermState>> overrides;

    public PermissionData() {
        this(Map.of(), Map.of(), Map.of());
    }

    private PermissionData(Map<String, Role> roles,
                           Map<UUID, List<String>> playerRoles,
                           Map<UUID, Map<String, PermState>> overrides) {
        this.roles = new LinkedHashMap<>(roles);
        this.playerRoles = new HashMap<>();
        playerRoles.forEach((k, v) -> this.playerRoles.put(k, new ArrayList<>(v)));
        this.overrides = new HashMap<>();
        overrides.forEach((k, v) -> this.overrides.put(k, new LinkedHashMap<>(v)));
    }

    /** Fetch the store from the overworld's storage, creating a fresh instance if absent. */
    public static PermissionData get(ServerLevel anyLevel) {
        return anyLevel.getServer().overworld().getDataStorage().computeIfAbsent(TYPE);
    }

    // ---- roles ----

    public Role role(String id) {
        return roles.get(id);
    }

    public Collection<Role> roles() {
        return roles.values();
    }

    public Role createRole(String id, int rank) {
        Role r = new Role(id, rank, Map.of());
        roles.put(id, r);
        setDirty();
        return r;
    }

    public void setRoleRule(String roleId, String node, PermState state) {
        Role r = roles.get(roleId);
        if (r != null) {
            r.setRule(node, state);
            setDirty();
        }
    }

    // ---- assignments ----

    public List<String> rolesOf(UUID player) {
        return playerRoles.getOrDefault(player, List.of());
    }

    public void assignRole(UUID player, String roleId) {
        List<String> list = playerRoles.computeIfAbsent(player, p -> new ArrayList<>());
        list.remove(roleId); // de-dup, then append so assignment order is preserved
        list.add(roleId);
        setDirty();
    }

    public void unassignRole(UUID player, String roleId) {
        List<String> list = playerRoles.get(player);
        if (list != null && list.remove(roleId)) {
            setDirty();
        }
    }

    // ---- overrides ----

    public Map<String, PermState> overridesOf(UUID player) {
        return overrides.getOrDefault(player, Map.of());
    }

    public void setOverride(UUID player, String node, PermState state) {
        if (state == PermState.UNSET) {
            Map<String, PermState> m = overrides.get(player);
            if (m != null) {
                m.remove(node);
            }
        } else {
            overrides.computeIfAbsent(player, p -> new LinkedHashMap<>()).put(node, state);
        }
        setDirty();
    }
}
