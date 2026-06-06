package kingdom.smp.lobby;

import java.util.Set;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/** Helpers for the {@code ironhold:lobby} dimension: teleport in/out and chunk warm-up. */
public final class Lobby {
    private Lobby() {}

    public static final ResourceKey<Level> DIMENSION =
        ResourceKey.create(Registries.DIMENSION, Identifier.parse("ironhold:lobby"));

    /** Fallback landing spot when no {@code /lobby setspawn} has been recorded. */
    public static final double DEFAULT_SPAWN_X = 8.5;
    public static final double DEFAULT_SPAWN_Y = 1.0;
    public static final double DEFAULT_SPAWN_Z = 8.5;

    /** Compares by id so it works regardless of ResourceKey identity across reloads. */
    public static boolean isLobby(Entity e) {
        return e.level().dimension().identifier().equals(DIMENSION.identifier());
    }

    /** Teleport a player into the lobby. Returns false if the dimension isn't loaded. */
    public static boolean sendToLobby(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        ServerLevel level = server.getLevel(DIMENSION);
        if (level == null) return false;

        LobbySavedData data = LobbySavedData.get(server);
        double x = data.hasSpawn() ? data.spawnX() : DEFAULT_SPAWN_X;
        double y = data.hasSpawn() ? data.spawnY() : DEFAULT_SPAWN_Y;
        double z = data.hasSpawn() ? data.spawnZ() : DEFAULT_SPAWN_Z;
        float yaw = data.hasSpawn() ? data.spawnYaw() : 0.0f;

        player.teleportTo(level, x, y, z, Set.of(), yaw, 0.0f, false);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        return true;
    }

    /** Teleport a player to the real world (overworld world-spawn). */
    public static boolean sendToWorld(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return false;
        ServerLevel overworld = server.getLevel(Level.OVERWORLD);
        if (overworld == null) return false;

        var respawn = overworld.getRespawnData();
        var pos = respawn.pos();
        player.teleportTo(overworld, pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5,
            Set.of(), respawn.yaw(), respawn.pitch(), false);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 0;
        return true;
    }

    /**
     * Force-load the 3×3 (9) chunks around the lobby spawn so the landing area is
     * always ready and players never hit a generation hitch stepping in. The rest
     * of the dimension streams in normally as anyone walks around.
     */
    public static void warmUpSpawnChunks(MinecraftServer server) {
        ServerLevel level = server.getLevel(DIMENSION);
        if (level == null) return;
        LobbySavedData data = LobbySavedData.get(server);
        double sx = data.hasSpawn() ? data.spawnX() : DEFAULT_SPAWN_X;
        double sz = data.hasSpawn() ? data.spawnZ() : DEFAULT_SPAWN_Z;
        int cx = ((int) Math.floor(sx)) >> 4;
        int cz = ((int) Math.floor(sz)) >> 4;
        for (int dx = -1; dx <= 1; dx++) {
            for (int dz = -1; dz <= 1; dz++) {
                level.setChunkForced(cx + dx, cz + dz, true);
            }
        }
    }
}
