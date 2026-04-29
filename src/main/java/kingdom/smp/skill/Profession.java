package kingdom.smp.skill;

import com.mojang.serialization.Codec;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;

/**
 * The 8 profession trees from the existing /menu Skills tab
 * ({@link kingdom.smp.client.screen.MainMenuScreen} SKILL_NAMES).
 *
 * Each profession has a linear 5-rank chain (see {@link ProfessionRank}).
 *
 * @see <a href="../../../../specs/profession-skill-system.md">profession-skill-system.md §4</a>
 */
public enum Profession implements StringRepresentable {
    BLACKSMITHING("blacksmithing", "Blacksmithing"),
    FARMING("farming", "Farming"),
    COOKING("cooking", "Cooking"),
    ALCHEMY("alchemy", "Alchemy"),
    FISHING("fishing", "Fishing"),
    ENCHANTING("enchanting", "Enchanting"),
    MINING("mining", "Mining"),
    TRADING("trading", "Trading");

    public static final Codec<Profession> CODEC = StringRepresentable.fromEnum(Profession::values);
    public static final StreamCodec<ByteBuf, Profession> STREAM_CODEC =
            ByteBufCodecs.VAR_INT.map(Profession::byId, Profession::ordinal);

    private final String id;
    private final String displayName;

    Profession(String id, String displayName) {
        this.id = id;
        this.displayName = displayName;
    }

    public String displayName() { return displayName; }

    @Override
    public String getSerializedName() { return id; }

    public static Profession byId(int ordinal) {
        Profession[] values = values();
        return values[Math.floorMod(ordinal, values.length)];
    }
}
