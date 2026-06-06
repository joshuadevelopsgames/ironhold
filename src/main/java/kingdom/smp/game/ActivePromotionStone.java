package kingdom.smp.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;

/**
 * Tracks the class-stone pillar a player has currently summoned for a pending
 * promotion. Stored as a copy-on-death player attachment so the stone they were
 * sent to find survives respawns. {@code present == false} means no active stone.
 *
 * <p>{@code (x, y, z)} is the position of the {@code class_stone} block itself —
 * the top of the {@code [stone_brick, stone_brick, class_stone]} pillar. The two
 * stone bricks sit at {@code y-1} and {@code y-2}.
 */
public record ActivePromotionStone(boolean present, String dimension, int x, int y, int z) {

    public static final ActivePromotionStone NONE = new ActivePromotionStone(false, "", 0, 0, 0);

    public static final MapCodec<ActivePromotionStone> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
            Codec.BOOL.optionalFieldOf("present", false).forGetter(ActivePromotionStone::present),
            Codec.STRING.optionalFieldOf("dimension", "").forGetter(ActivePromotionStone::dimension),
            Codec.INT.optionalFieldOf("x", 0).forGetter(ActivePromotionStone::x),
            Codec.INT.optionalFieldOf("y", 0).forGetter(ActivePromotionStone::y),
            Codec.INT.optionalFieldOf("z", 0).forGetter(ActivePromotionStone::z))
            .apply(i, ActivePromotionStone::new));

    public static ActivePromotionStone at(String dimension, BlockPos pos) {
        return new ActivePromotionStone(true, dimension, pos.getX(), pos.getY(), pos.getZ());
    }

    public BlockPos pos() {
        return new BlockPos(x, y, z);
    }
}
