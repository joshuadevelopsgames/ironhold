package kingdom.smp.block;

import java.util.Optional;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.GlobalPos;
import org.jspecify.annotations.Nullable;

/**
 * Per-player bound Ender Shrine sanctuary (dimension + position), stored in the {@code BOUND_SHRINE}
 * attachment (copyOnDeath, so the link survives the death it rescues). {@code null} pos = unbound.
 *
 * <p>Spec: {@code specs/fantasia-ports/03-ender-shrine.md}.
 */
public record BoundShrine(@Nullable GlobalPos pos) {

    public static final BoundShrine NONE = new BoundShrine(null);

    /** MapCodec so it slots into {@code AttachmentType.builder(...).serialize(...)} like the others. */
    public static final MapCodec<BoundShrine> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        GlobalPos.CODEC.optionalFieldOf("pos").forGetter(b -> Optional.ofNullable(b.pos()))
    ).apply(i, opt -> new BoundShrine(opt.orElse(null))));

    public boolean isBound() {
        return pos != null;
    }
}
