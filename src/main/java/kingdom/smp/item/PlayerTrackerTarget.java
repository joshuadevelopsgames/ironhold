package kingdom.smp.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.UUIDUtil;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.Optional;
import java.util.UUID;

/**
 * Persistent state for {@link PlayerCompassItem}.
 *
 * The UUID is the bound player; the optional name is shown in tooltips while the
 * tracked player is offline; the optional GlobalPos is refreshed every second on
 * the server-side {@code inventoryTick} of the compass and is what the client-side
 * needle reads to compute its angle.
 */
public record PlayerTrackerTarget(UUID uuid, Optional<String> name, Optional<GlobalPos> lastKnownPos) {

    public static final Codec<PlayerTrackerTarget> CODEC = RecordCodecBuilder.create(i -> i.group(
                    UUIDUtil.CODEC.fieldOf("uuid").forGetter(PlayerTrackerTarget::uuid),
                    Codec.STRING.optionalFieldOf("name").forGetter(PlayerTrackerTarget::name),
                    GlobalPos.CODEC.optionalFieldOf("last_known_pos").forGetter(PlayerTrackerTarget::lastKnownPos))
            .apply(i, PlayerTrackerTarget::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, PlayerTrackerTarget> STREAM_CODEC = StreamCodec.composite(
            UUIDUtil.STREAM_CODEC, PlayerTrackerTarget::uuid,
            ByteBufCodecs.optional(ByteBufCodecs.STRING_UTF8), PlayerTrackerTarget::name,
            ByteBufCodecs.optional(asRegistryFriendly(GlobalPos.STREAM_CODEC)), PlayerTrackerTarget::lastKnownPos,
            PlayerTrackerTarget::new);

    public PlayerTrackerTarget withPos(GlobalPos pos) {
        return new PlayerTrackerTarget(uuid, name, Optional.of(pos));
    }

    @SuppressWarnings("unchecked")
    private static <T> StreamCodec<RegistryFriendlyByteBuf, T> asRegistryFriendly(StreamCodec<? super ByteBuf, T> base) {
        return (StreamCodec<RegistryFriendlyByteBuf, T>) (StreamCodec<?, ?>) base;
    }
}
