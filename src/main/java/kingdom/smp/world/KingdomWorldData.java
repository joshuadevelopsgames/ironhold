package kingdom.smp.world;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

/**
 * Per-world kingdom progress: pooled Class XP per kingdom (0–3) and dimension gates (SYSTEMS.md stub).
 */
public class KingdomWorldData extends SavedData {
    public static final long NETHER_UNLOCK_XP = 500L;
    public static final long END_UNLOCK_XP = 2500L;

    public static final Codec<KingdomWorldData> CODEC = RecordCodecBuilder.create(
        i -> i.group(
            Codec.LONG.fieldOf("k0").forGetter(d -> d.kingdomXp[0]),
            Codec.LONG.fieldOf("k1").forGetter(d -> d.kingdomXp[1]),
            Codec.LONG.fieldOf("k2").forGetter(d -> d.kingdomXp[2]),
            Codec.LONG.fieldOf("k3").forGetter(d -> d.kingdomXp[3]))
            .apply(i, KingdomWorldData::new));

    public static final SavedDataType<KingdomWorldData> TYPE =
        new SavedDataType<>(Identifier.parse("ironhold:kingdom_progress"), KingdomWorldData::new, CODEC, DataFixTypes.SAVED_DATA_COMMAND_STORAGE);

    private final long[] kingdomXp = new long[4];
    private boolean netherUnlocked;
    private boolean endUnlocked;

    public KingdomWorldData() {
        this(0L, 0L, 0L, 0L);
    }

    private KingdomWorldData(long k0, long k1, long k2, long k3) {
        this.kingdomXp[0] = k0;
        this.kingdomXp[1] = k1;
        this.kingdomXp[2] = k2;
        this.kingdomXp[3] = k3;
        recomputeUnlockFlags();
    }

    public long getKingdomXp(int kingdomIndex) {
        if (kingdomIndex < 0 || kingdomIndex > 3) {
            return 0L;
        }
        return kingdomXp[kingdomIndex];
    }

    public void addKingdomXp(int kingdomIndex, long amount) {
        if (amount == 0L || kingdomIndex < 0 || kingdomIndex > 3) {
            return;
        }
        kingdomXp[kingdomIndex] = Math.max(0L, kingdomXp[kingdomIndex] + amount);
        recomputeUnlockFlags();
        setDirty();
    }

    public boolean isNetherUnlocked() {
        return netherUnlocked;
    }

    public boolean isEndUnlocked() {
        return endUnlocked;
    }

    public long maxKingdomXp() {
        long m = 0L;
        for (long v : kingdomXp) {
            m = Math.max(m, v);
        }
        return m;
    }

    /** Any single kingdom reaching the threshold opens the gate for everyone (design stub). */
    private void recomputeUnlockFlags() {
        boolean nether = false;
        boolean end = false;
        for (long v : kingdomXp) {
            if (v >= NETHER_UNLOCK_XP) {
                nether = true;
            }
            if (v >= END_UNLOCK_XP) {
                end = true;
            }
        }
        this.netherUnlocked = nether;
        this.endUnlocked = end;
    }
}
