package kingdom.smp.wishing;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import kingdom.smp.Ironhold;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;
import net.minecraft.world.phys.AABB;

import java.util.Optional;

/**
 * Persistent definition of the server's wishing well: two corners + a dimension form an axis-aligned
 * box that {@link WishingWellManager} watches for tossed gold coins. Configured by an admin with
 * {@code /wishingwell}. Stored on the overworld's data storage (one well per server), mirroring the
 * original Kingdom SMP implementation this was ported from.
 */
public final class WishingWellState extends SavedData {

    private static final Identifier DATA_KEY = Identifier.fromNamespaceAndPath(Ironhold.MODID, "wishing_well");

    private BlockPos cornerOne;
    private BlockPos cornerTwo;
    private ResourceKey<Level> dimension;
    private boolean enabled = true;

    public WishingWellState() {}

    private WishingWellState(Optional<BlockPos> c1, Optional<BlockPos> c2,
                             Optional<ResourceKey<Level>> dim, boolean enabled) {
        this.cornerOne = c1.orElse(null);
        this.cornerTwo = c2.orElse(null);
        this.dimension = dim.orElse(null);
        this.enabled = enabled;
    }

    private static final Codec<WishingWellState> CODEC = RecordCodecBuilder.create(inst -> inst.group(
        BlockPos.CODEC.optionalFieldOf("corner_one").forGetter(s -> Optional.ofNullable(s.cornerOne)),
        BlockPos.CODEC.optionalFieldOf("corner_two").forGetter(s -> Optional.ofNullable(s.cornerTwo)),
        ResourceKey.codec(Registries.DIMENSION).optionalFieldOf("dimension").forGetter(s -> Optional.ofNullable(s.dimension)),
        Codec.BOOL.optionalFieldOf("enabled", true).forGetter(s -> s.enabled)
    ).apply(inst, WishingWellState::new));

    private static final SavedDataType<WishingWellState> TYPE =
        new SavedDataType<>(DATA_KEY, WishingWellState::new, CODEC, DataFixTypes.SAVED_DATA_SCOREBOARD);

    public static WishingWellState get(MinecraftServer server) {
        ServerLevel overworld = server.overworld();
        return overworld.getDataStorage().computeIfAbsent(TYPE);
    }

    public void setCornerOne(BlockPos pos, ResourceKey<Level> dimension) {
        this.cornerOne = pos;
        this.cornerTwo = null; // a fresh corner #1 invalidates the old box until #2 is re-set
        this.dimension = dimension;
        setDirty();
    }

    public void setCornerTwo(BlockPos pos, ResourceKey<Level> dimension) {
        if (this.dimension == null) {
            this.dimension = dimension;
        }
        this.cornerTwo = pos;
        setDirty();
    }

    public Optional<BlockPos> getCornerOne() {
        return Optional.ofNullable(cornerOne);
    }

    public Optional<BlockPos> getCornerTwo() {
        return Optional.ofNullable(cornerTwo);
    }

    public boolean hasDefinedArea() {
        return cornerOne != null && cornerTwo != null && dimension != null;
    }

    public Optional<ResourceKey<Level>> getDimensionKey() {
        return Optional.ofNullable(dimension);
    }

    public Optional<AABB> getWellBox() {
        if (!hasDefinedArea()) {
            return Optional.empty();
        }
        int minX = Math.min(cornerOne.getX(), cornerTwo.getX());
        int minY = Math.min(cornerOne.getY(), cornerTwo.getY());
        int minZ = Math.min(cornerOne.getZ(), cornerTwo.getZ());
        int maxX = Math.max(cornerOne.getX(), cornerTwo.getX()) + 1;
        int maxY = Math.max(cornerOne.getY(), cornerTwo.getY()) + 1;
        int maxZ = Math.max(cornerOne.getZ(), cornerTwo.getZ()) + 1;
        return Optional.of(new AABB(minX, minY, minZ, maxX, maxY, maxZ));
    }

    public boolean isEnabled() {
        return enabled && hasDefinedArea();
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        setDirty();
    }
}
