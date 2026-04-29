package kingdom.smp.gear;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.ChatFormatting;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * Four gear quality tiers from the ore-quality-system spec.
 * Poor = wilderness/junk, Fine = below-average mine output, Good = vanilla baseline,
 * Mint = Royal-mine prestige.
 *
 * Order matters — {@link #defaultQuality()} returns GOOD, so unmarked vanilla items
 * keep vanilla durability and behave exactly as before.
 *
 * @see <a href="../../../../specs/ore-quality-system.md">ore-quality-system.md §2</a>
 */
public enum ItemQuality implements StringRepresentable {
    POOR("poor", 0.5f, 0.6f, false, ChatFormatting.DARK_GRAY),
    FINE("fine", 0.8f, 0.9f, true, ChatFormatting.GRAY),
    GOOD("good", 1.0f, 1.0f, true, ChatFormatting.WHITE),
    MINT("mint", 1.2f, 1.2f, true, ChatFormatting.AQUA);

    public static final Codec<ItemQuality> CODEC = StringRepresentable.fromEnum(ItemQuality::values);
    public static final StreamCodec<ByteBuf, ItemQuality> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(ItemQuality::byId, ItemQuality::ordinal);

    private final String id;
    private final float durabilityMultiplier;
    private final float repairEfficiencyMultiplier;
    private final boolean pristineEligible;
    private final ChatFormatting tooltipColor;

    ItemQuality(String id, float durabilityMultiplier, float repairEfficiencyMultiplier,
                boolean pristineEligible, ChatFormatting tooltipColor) {
        this.id = id;
        this.durabilityMultiplier = durabilityMultiplier;
        this.repairEfficiencyMultiplier = repairEfficiencyMultiplier;
        this.pristineEligible = pristineEligible;
        this.tooltipColor = tooltipColor;
    }

    public float durabilityMultiplier() { return durabilityMultiplier; }
    public float repairEfficiencyMultiplier() { return repairEfficiencyMultiplier; }
    public boolean isPristineEligible() { return pristineEligible; }
    public ChatFormatting tooltipColor() { return tooltipColor; }

    public String displayName() {
        return name().charAt(0) + name().substring(1).toLowerCase();
    }

    @Override
    public String getSerializedName() { return id; }

    public static ItemQuality byId(int ordinal) {
        ItemQuality[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }

    /** Default quality for items that have no quality component yet. Vanilla = Good (1.0×). */
    public static ItemQuality defaultQuality() { return GOOD; }
}
