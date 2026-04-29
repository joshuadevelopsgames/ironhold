package kingdom.smp.gear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/**
 * Per-item repair fatigue counter, 0–5. Each repair increments the counter; level 5 forces a
 * quality tier-drop on the next repair (Mint → Fine, Fine → Standard, Standard → discard).
 *
 * Penalty applies as a max-durability cap, replacing vanilla's "Too Expensive!" anvil mechanic
 * with something more legible.
 *
 * @see <a href="../../../../specs/ore-quality-system.md">ore-quality-system.md §9.2</a>
 */
public record RepairFatigue(int level) {

    public static final int MAX_LEVEL = 5;
    public static final RepairFatigue FRESH = new RepairFatigue(0);

    public static final Codec<RepairFatigue> CODEC = RecordCodecBuilder.create(inst -> inst.group(
            Codec.intRange(0, MAX_LEVEL).optionalFieldOf("level", 0).forGetter(RepairFatigue::level)
    ).apply(inst, RepairFatigue::new));

    public static final StreamCodec<ByteBuf, RepairFatigue> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(RepairFatigue::new, RepairFatigue::level);

    public RepairFatigue {
        if (level < 0 || level > MAX_LEVEL) {
            throw new IllegalArgumentException("RepairFatigue level out of range: " + level);
        }
    }

    /** Max durability multiplier from fatigue. Level 5 returns 0.85 even though the spec says
     *  "Tier Drop" — the tier-drop is handled at repair time; the cap stays meaningful. */
    public float maxDurabilityMultiplier() {
        return switch (level) {
            case 0 -> 1.00f;
            case 1 -> 0.98f;
            case 2 -> 0.95f;
            case 3 -> 0.90f;
            case 4 -> 0.85f;
            default -> 0.85f; // level 5; tier-drop handled separately
        };
    }

    public boolean triggersTierDrop() { return level >= MAX_LEVEL; }

    public RepairFatigue increment() {
        return new RepairFatigue(Math.min(MAX_LEVEL, level + 1));
    }

    /** Reset after tier-drop completes. */
    public RepairFatigue reset() { return FRESH; }
}
