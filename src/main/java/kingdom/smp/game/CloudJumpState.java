package kingdom.smp.game;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Server-only: whether the player can use their Breeze in a Bottle mid-air jump this airtime. */
public record CloudJumpState(boolean midairChargeAvailable) {
    public static final CloudJumpState CHARGED = new CloudJumpState(true);
    public static final CloudJumpState SPENT = new CloudJumpState(false);

    public static final MapCodec<CloudJumpState> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Codec.BOOL.fieldOf("midair").forGetter(CloudJumpState::midairChargeAvailable))
            .apply(i, CloudJumpState::new));
}
