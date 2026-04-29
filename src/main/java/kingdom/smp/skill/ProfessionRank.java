package kingdom.smp.skill;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Linear rank chain shared across all professions. Cumulative point cost to reach
 * Master is 1 + 1 + 2 + 2 + 3 = 9 — meaning a player can max two trees with the v1
 * career budget of ~19 points.
 *
 * @see <a href="../../../../specs/profession-skill-system.md">profession-skill-system.md §4</a>
 */
public enum ProfessionRank implements StringRepresentable {
    NOVICE("novice", "Novice", 1, 0),
    APPRENTICE("apprentice", "Apprentice", 1, 1),
    JOURNEYMAN("journeyman", "Journeyman", 2, 2),
    EXPERT("expert", "Expert", 2, 3),
    MASTER("master", "Master", 3, 4);

    public static final Codec<ProfessionRank> CODEC = StringRepresentable.fromEnum(ProfessionRank::values);
    public static final StreamCodec<ByteBuf, ProfessionRank> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(ProfessionRank::byId, ProfessionRank::ordinal);

    private final String id;
    private final String displayName;
    private final int pointCost;
    private final int order;

    ProfessionRank(String id, String displayName, int pointCost, int order) {
        this.id = id;
        this.displayName = displayName;
        this.pointCost = pointCost;
        this.order = order;
    }

    public String displayName() { return displayName; }
    public int pointCost() { return pointCost; }
    public int order() { return order; }

    /** Cumulative cost from Novice through this rank. */
    public int cumulativeCost() {
        int total = 0;
        for (ProfessionRank r : values()) {
            total += r.pointCost;
            if (r == this) return total;
        }
        return total;
    }

    public ProfessionRank prerequisite() {
        return order == 0 ? null : values()[order - 1];
    }

    @Override
    public String getSerializedName() { return id; }

    public static ProfessionRank byId(int ordinal) {
        ProfessionRank[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
