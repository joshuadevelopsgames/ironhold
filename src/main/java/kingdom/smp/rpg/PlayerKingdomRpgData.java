package kingdom.smp.rpg;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

/** Per-player RPG stub: kingdom allegiance, class, class level + bar XP (persisted, copy on death). */
public record PlayerKingdomRpgData(int kingdomIndex, int classIndex, int classLevel, int xpIntoLevel) {
    /**
     * Lenient attachment codec: missing fields default safely so older/corrupt saves do not
     * block login (otherwise the server disconnects with {@code multiplayer.disconnect.invalid_player_data}).
     */
    public static final MapCodec<PlayerKingdomRpgData> CODEC = RecordCodecBuilder.mapCodec(
        i -> i.group(
            Codec.INT.optionalFieldOf("kingdom", 0).forGetter(PlayerKingdomRpgData::kingdomIndex),
            Codec.INT.optionalFieldOf("clazz", PlayerClass.PEASANT.ordinal()).forGetter(PlayerKingdomRpgData::classIndex),
            Codec.INT.optionalFieldOf("class_level", 1).forGetter(PlayerKingdomRpgData::classLevel),
            Codec.INT.optionalFieldOf("xp_into_level", 0).forGetter(PlayerKingdomRpgData::xpIntoLevel))
            .apply(i, PlayerKingdomRpgData::new));

    public static PlayerKingdomRpgData defaultData() {
        return new PlayerKingdomRpgData(0, PlayerClass.PEASANT.ordinal(), 1, 0);
    }

    public PlayerClass playerClass() {
        return PlayerClass.fromIndex(classIndex);
    }

    public int kingdomIndexClamped() {
        if (kingdomIndex < 0 || kingdomIndex > 3) {
            return 0;
        }
        return kingdomIndex;
    }
}
