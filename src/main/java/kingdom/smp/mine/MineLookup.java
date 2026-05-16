package kingdom.smp.mine;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.levelgen.structure.StructureStart;

/**
 * Resolves the {@link MineGeography} of a position at break time.
 *
 * Asks the world's structure manager whether {@code pos} is inside any structure
 * tagged {@code ironhold:mines}; if so, picks a depth band by Y. Otherwise the
 * position is wilderness.
 *
 * The mines tag currently aliases vanilla {@code minecraft:mineshaft} as a
 * bootstrap, so vanilla mineshafts function as ironhold mines until Phase B2
 * introduces a dedicated Ironhold Mine structure. The Y-band thresholds are
 * tuned for vanilla overworld mineshafts (which span roughly Y=-50 to Y=15) —
 * they will be revisited when the custom mine arrives.
 */
public final class MineLookup {
    private MineLookup() {}

    /** Y &gt;= this is the shallow band — quality drops top out at Good. */
    public static final int SHALLOW_MIN_Y = 0;
    /** Y &gt;= this (and &lt; {@link #SHALLOW_MIN_Y}) is the mid band — Mint becomes reachable. */
    public static final int MID_MIN_Y = -32;

    public static MineGeography classify(ServerLevel level, BlockPos pos) {
        StructureStart start = level.structureManager()
                .getStructureWithPieceAt(pos, IronholdStructureTags.MINES);
        if (!start.isValid()) {
            return MineGeography.WILD;
        }
        int y = pos.getY();
        if (y >= SHALLOW_MIN_Y) return MineGeography.MINE_SHALLOW;
        if (y >= MID_MIN_Y)     return MineGeography.MINE_MID;
        return MineGeography.MINE_DEEP;
    }
}
