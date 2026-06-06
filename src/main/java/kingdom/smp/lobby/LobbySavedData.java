package kingdom.smp.lobby;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Persisted lobby state, stored in the overworld's data storage:
 * <ul>
 *   <li>{@code seen} — players who have already passed through the lobby once
 *       (used for first-join-only routing).</li>
 *   <li>lobby spawn — where players land in the lobby dimension, set with
 *       {@code /lobby setspawn}. Falls back to {@link Lobby#DEFAULT_SPAWN_X} etc.</li>
 *   <li>exit-portal region — the block box players step into to leave for the
 *       real world, set with {@code /lobby setportal pos1|pos2}.</li>
 * </ul>
 */
public class LobbySavedData extends SavedData {

    public static final Codec<LobbySavedData> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.listOf().optionalFieldOf("seen", List.of()).forGetter(d -> List.copyOf(d.seen)),
        Codec.BOOL.optionalFieldOf("has_spawn", false).forGetter(d -> d.hasSpawn),
        Codec.DOUBLE.optionalFieldOf("spawn_x", 0.0).forGetter(d -> d.spawnX),
        Codec.DOUBLE.optionalFieldOf("spawn_y", 0.0).forGetter(d -> d.spawnY),
        Codec.DOUBLE.optionalFieldOf("spawn_z", 0.0).forGetter(d -> d.spawnZ),
        Codec.FLOAT.optionalFieldOf("spawn_yaw", 0.0f).forGetter(d -> d.spawnYaw),
        Codec.BOOL.optionalFieldOf("has_portal", false).forGetter(d -> d.hasPortal),
        Codec.INT.optionalFieldOf("p1x", 0).forGetter(d -> d.p1x),
        Codec.INT.optionalFieldOf("p1y", 0).forGetter(d -> d.p1y),
        Codec.INT.optionalFieldOf("p1z", 0).forGetter(d -> d.p1z),
        Codec.INT.optionalFieldOf("p2x", 0).forGetter(d -> d.p2x),
        Codec.INT.optionalFieldOf("p2y", 0).forGetter(d -> d.p2y),
        Codec.INT.optionalFieldOf("p2z", 0).forGetter(d -> d.p2z)
    ).apply(i, LobbySavedData::new));

    public static final SavedDataType<LobbySavedData> TYPE = new SavedDataType<>(
        Identifier.parse("ironhold:lobby"), LobbySavedData::new, CODEC,
        DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final Set<String> seen = new HashSet<>();
    private boolean hasSpawn;
    private double spawnX, spawnY, spawnZ;
    private float spawnYaw;
    private boolean hasPortal;
    private int p1x, p1y, p1z, p2x, p2y, p2z;

    public LobbySavedData() {}

    private LobbySavedData(List<String> seen, boolean hasSpawn, double sx, double sy, double sz,
                           float syaw, boolean hasPortal, int p1x, int p1y, int p1z,
                           int p2x, int p2y, int p2z) {
        this.seen.addAll(seen);
        this.hasSpawn = hasSpawn;
        this.spawnX = sx; this.spawnY = sy; this.spawnZ = sz; this.spawnYaw = syaw;
        this.hasPortal = hasPortal;
        this.p1x = p1x; this.p1y = p1y; this.p1z = p1z;
        this.p2x = p2x; this.p2y = p2y; this.p2z = p2z;
    }

    public static LobbySavedData get(MinecraftServer server) {
        return server.getLevel(Level.OVERWORLD).getDataStorage().computeIfAbsent(TYPE);
    }

    // ── Seen players ──────────────────────────────────────────────────────────
    public boolean hasSeen(UUID id) {
        return seen.contains(id.toString());
    }

    public void markSeen(UUID id) {
        if (seen.add(id.toString())) setDirty();
    }

    // ── Lobby spawn ───────────────────────────────────────────────────────────
    public boolean hasSpawn() { return hasSpawn; }
    public double spawnX() { return spawnX; }
    public double spawnY() { return spawnY; }
    public double spawnZ() { return spawnZ; }
    public float spawnYaw() { return spawnYaw; }

    public void setSpawn(double x, double y, double z, float yaw) {
        this.hasSpawn = true;
        this.spawnX = x; this.spawnY = y; this.spawnZ = z; this.spawnYaw = yaw;
        setDirty();
    }

    // ── Exit portal region ────────────────────────────────────────────────────
    public boolean hasPortal() { return hasPortal; }

    public void setPortalCorner1(int x, int y, int z) {
        this.p1x = x; this.p1y = y; this.p1z = z;
        // Corner 1 clears any active portal; it re-arms only once corner 2 is set,
        // so an incomplete setup can never leave a stray box live.
        this.hasPortal = false;
        setDirty();
    }

    public void setPortalCorner2(int x, int y, int z) {
        this.p2x = x; this.p2y = y; this.p2z = z;
        this.hasPortal = true;
        setDirty();
    }

    /** True if (x,y,z) falls inside the block box spanned by the two portal corners. */
    public boolean portalContains(double x, double y, double z) {
        if (!hasPortal) return false;
        double minX = Math.min(p1x, p2x), maxX = Math.max(p1x, p2x) + 1.0;
        double minY = Math.min(p1y, p2y), maxY = Math.max(p1y, p2y) + 1.0;
        double minZ = Math.min(p1z, p2z), maxZ = Math.max(p1z, p2z) + 1.0;
        return x >= minX && x <= maxX && y >= minY && y <= maxY && z >= minZ && z <= maxZ;
    }

    public String describePortal() {
        if (!hasPortal) return "unset";
        return "(" + p1x + "," + p1y + "," + p1z + ") .. (" + p2x + "," + p2y + "," + p2z + ")";
    }
}
