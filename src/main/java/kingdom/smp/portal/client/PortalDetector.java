package kingdom.smp.portal.client;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import kingdom.smp.Ironhold;
import kingdom.smp.block.MoonPortalBlock;
import kingdom.smp.moon.ModMoonDimensions;
import kingdom.smp.net.ServerboundRequestPortalViewsPayload;
import kingdom.smp.portal.PortalLink;
import kingdom.smp.portal.PortalSurface;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.NetherPortalBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.phys.Vec3;

/**
 * Finds the cross-dimensional portal surfaces near the player and publishes them to
 * {@link ClientPortalRegistry} for the render pass to consume.
 *
 * <p><b>M0 scope:</b> detects {@link MoonPortalBlock} planes by a throttled, bounded block scan and
 * coalesces connected blocks into a single {@link PortalSurface} via in-plane flood fill. The scan is
 * a deliberate placeholder — M4 replaces it with an event-driven / POI portal registry so we are not
 * walking blocks at all. Region portals (the Lobby box) are added in M1 once their corners are synced
 * to the client. No rendering happens here yet; this milestone only validates the data model.
 */
public final class PortalDetector {
    private PortalDetector() {}

    /** Ticks between scans (20 = once per second). */
    private static final int SCAN_INTERVAL = 20;
    /** Horizontal / vertical search half-extents around the player, in blocks. */
    private static final int RANGE_H = 32;
    private static final int RANGE_V = 16;
    /** Safety cap on a single flood-filled plane. */
    private static final int MAX_PLANE_BLOCKS = 4096;

    private static int ticks;
    private static String lastSignature = "";

    /** Reset throttle/change state, e.g. on disconnect so a rejoin re-requests its views. */
    public static void reset() {
        ticks = 0;
        lastSignature = "";
        ClientPortalRegistry.clear();
    }

    public static void tick() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null || mc.player == null) {
            return;
        }
        if (ticks++ % SCAN_INTERVAL != 0) {
            return;
        }

        List<PortalLink> links = scan(mc.level, mc.player.blockPosition());
        ClientPortalRegistry.set(links);

        String sig = links.stream().map(PortalLink::signature).sorted().collect(Collectors.joining("; "));
        if (!sig.equals(lastSignature)) {
            lastSignature = sig;
            if (links.isEmpty()) {
                Ironhold.LOGGER.debug("[portal] no cross-dim portals in range");
            } else {
                Ironhold.LOGGER.info("[portal] {} cross-dim portal(s) visible: {}", links.size(), sig);
            }
            requestStreaming(links);
        }
    }

    /** Ask the server to stream the far side of each visible portal (one view per destination dim). */
    private static void requestStreaming(List<PortalLink> links) {
        if (Minecraft.getInstance().getConnection() == null) {
            return;
        }
        Map<Identifier, ServerboundRequestPortalViewsPayload.Entry> byDim = new LinkedHashMap<>();
        for (PortalLink link : links) {
            Identifier dimId = link.destDim().identifier();
            int cx = Mth.floor(link.destAnchor().x) >> 4;
            int cz = Mth.floor(link.destAnchor().z) >> 4;
            byDim.putIfAbsent(dimId, new ServerboundRequestPortalViewsPayload.Entry(dimId, cx, cz));
        }
        kingdom.smp.client.ClientPayloads.sendToServer(
            new ServerboundRequestPortalViewsPayload(List.copyOf(byDim.values())));
    }

    private static List<PortalLink> scan(ClientLevel level, BlockPos camera) {
        ResourceKey<Level> srcDim = level.dimension();
        if (!isPortalDim(srcDim)) {
            return List.of();
        }

        List<PortalLink> out = new ArrayList<>();
        Set<Long> visited = new HashSet<>();
        BlockPos.MutableBlockPos cur = new BlockPos.MutableBlockPos();
        for (int dx = -RANGE_H; dx <= RANGE_H; dx++) {
            for (int dy = -RANGE_V; dy <= RANGE_V; dy++) {
                for (int dz = -RANGE_H; dz <= RANGE_H; dz++) {
                    cur.set(camera.getX() + dx, camera.getY() + dy, camera.getZ() + dz);
                    if (visited.contains(cur.asLong())) {
                        continue;
                    }
                    BlockState state = level.getBlockState(cur);
                    Block block = state.getBlock();
                    boolean moon = block instanceof MoonPortalBlock;
                    boolean nether = block instanceof NetherPortalBlock;
                    if (!moon && !nether) {
                        continue;
                    }
                    Direction.Axis axis = state.getValue(BlockStateProperties.HORIZONTAL_AXIS);
                    PortalSurface surface = floodFillPlane(level, cur.immutable(), block, axis, visited);
                    out.add(moon ? buildMoonLink(srcDim, surface) : buildNetherLink(srcDim, surface));
                }
            }
        }
        return out;
    }

    /** BFS over connected same-block, same-axis portal blocks within the plane; returns their rectangle. */
    private static PortalSurface floodFillPlane(ClientLevel level, BlockPos seed, Block seedBlock, Direction.Axis axis, Set<Long> visited) {
        // Plane for AXIS=X spans X,Y (normal Z); for AXIS=Z spans Z,Y (normal X). Vertical is always in-plane.
        Direction[] inPlane = axis == Direction.Axis.X
            ? new Direction[] {Direction.UP, Direction.DOWN, Direction.EAST, Direction.WEST}
            : new Direction[] {Direction.UP, Direction.DOWN, Direction.SOUTH, Direction.NORTH};

        int minX = seed.getX(), minY = seed.getY(), minZ = seed.getZ();
        int maxX = minX, maxY = minY, maxZ = minZ;

        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(seed);
        visited.add(seed.asLong());

        int count = 0;
        while (!queue.isEmpty() && count < MAX_PLANE_BLOCKS) {
            BlockPos p = queue.poll();
            count++;
            minX = Math.min(minX, p.getX()); minY = Math.min(minY, p.getY()); minZ = Math.min(minZ, p.getZ());
            maxX = Math.max(maxX, p.getX()); maxY = Math.max(maxY, p.getY()); maxZ = Math.max(maxZ, p.getZ());
            for (Direction d : inPlane) {
                BlockPos n = p.relative(d);
                long key = n.asLong();
                if (visited.contains(key)) {
                    continue;
                }
                BlockState s = level.getBlockState(n);
                if (s.getBlock() == seedBlock && s.getValue(BlockStateProperties.HORIZONTAL_AXIS) == axis) {
                    visited.add(key);
                    queue.add(n);
                }
            }
        }
        return surfaceFrom(axis, minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static PortalSurface surfaceFrom(Direction.Axis axis,
            int minX, int minY, int minZ, int maxX, int maxY, int maxZ) {
        double height = maxY - minY + 1;
        if (axis == Direction.Axis.X) {
            double width = maxX - minX + 1;
            Vec3 center = new Vec3((minX + maxX + 1) / 2.0, (minY + maxY + 1) / 2.0, minZ + 0.5);
            return new PortalSurface(center, new Vec3(0, 0, 1), new Vec3(1, 0, 0), new Vec3(0, 1, 0),
                width / 2.0, height / 2.0);
        }
        double width = maxZ - minZ + 1;
        Vec3 center = new Vec3(minX + 0.5, (minY + maxY + 1) / 2.0, (minZ + maxZ + 1) / 2.0);
        return new PortalSurface(center, new Vec3(1, 0, 0), new Vec3(0, 0, 1), new Vec3(0, 1, 0),
            width / 2.0, height / 2.0);
    }

    /** Mirrors {@code MoonPortalBlock.getPortalDestination}: bidirectional Overworld <-> Moon. */
    private static PortalLink buildMoonLink(ResourceKey<Level> srcDim, PortalSurface surface) {
        boolean onMoon = srcDim.identifier().equals(ModMoonDimensions.MOON_LEVEL.identifier());
        ResourceKey<Level> destDim = onMoon ? Level.OVERWORLD : ModMoonDimensions.MOON_LEVEL;
        Vec3 c = surface.center();
        Vec3 anchor;
        if (onMoon) {
            // moon -> overworld keeps the same position
            anchor = new Vec3(c.x, c.y, c.z);
        } else {
            // overworld -> moon lands on top of the floating cube (~y=230), clamped inside its footprint
            anchor = new Vec3(Mth.clamp(c.x, -119.5, 119.5), 231.0, Mth.clamp(c.z, -119.5, 119.5));
        }
        Vec3 n = surface.normal();
        float yaw = (float) Math.toDegrees(Math.atan2(-n.x, n.z));
        return new PortalLink(surface, srcDim, destDim, anchor, yaw);
    }

    /** Mirrors vanilla nether linking: Overworld &lt;-&gt; Nether with an 8:1 horizontal scale anchor.
     *  The scaled point is the standard preview location (precise PortalForcer linking is server-side). */
    private static PortalLink buildNetherLink(ResourceKey<Level> srcDim, PortalSurface surface) {
        boolean inNether = srcDim.identifier().equals(Level.NETHER.identifier());
        ResourceKey<Level> destDim = inNether ? Level.OVERWORLD : Level.NETHER;
        Vec3 c = surface.center();
        double scale = inNether ? 8.0 : 0.125;
        double ay = inNether ? c.y : Mth.clamp(c.y, 4.0, 124.0);
        Vec3 anchor = new Vec3(c.x * scale, ay, c.z * scale);
        Vec3 n = surface.normal();
        float yaw = (float) Math.toDegrees(Math.atan2(-n.x, n.z));
        return new PortalLink(surface, srcDim, destDim, anchor, yaw);
    }

    private static boolean isPortalDim(ResourceKey<Level> dim) {
        Identifier id = dim.identifier();
        return id.equals(Level.OVERWORLD.identifier())
            || id.equals(Level.NETHER.identifier())
            || id.equals(ModMoonDimensions.MOON_LEVEL.identifier());
    }
}
