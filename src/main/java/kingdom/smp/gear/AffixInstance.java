package kingdom.smp.gear;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

/** One rolled affix on a gear item: which {@link Affix} and the value it rolled within the affix range. */
public record AffixInstance(String id, float roll) {

    public static final Codec<AffixInstance> CODEC = RecordCodecBuilder.create(i -> i.group(
        Codec.STRING.fieldOf("id").forGetter(AffixInstance::id),
        Codec.FLOAT.fieldOf("roll").forGetter(AffixInstance::roll)
    ).apply(i, AffixInstance::new));

    public static final StreamCodec<ByteBuf, AffixInstance> STREAM_CODEC = StreamCodec.composite(
        ByteBufCodecs.STRING_UTF8, AffixInstance::id,
        ByteBufCodecs.FLOAT, AffixInstance::roll,
        AffixInstance::new);

    public Affix affix() {
        return Affix.byId(id);
    }
}
