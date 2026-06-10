package kingdom.smp.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import io.netty.buffer.ByteBuf;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import kingdom.smp.ModAttachments;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;

/**
 * A player's Butterfly Encyclopedia progress — the immutable set of species ids they
 * have discovered (caught at least once). Backs the encyclopedia screen: an entry is
 * shown in full only once its id is in this set, otherwise it renders as a masked "???"
 * silhouette.
 *
 * <p>Stored as the {@link ModAttachments#BUTTERFLY_DEX} attachment on the player,
 * serialized so it survives logout/death and synced so the client screen can read it.
 * Immutable: {@link #with} returns a fresh dex, which is how change-detection (and thus
 * the auto-sync on {@code setData}) stays cheap.
 */
public record ButterflyDex(Set<String> discovered) {

    public static final ButterflyDex EMPTY = new ButterflyDex(Set.of());

    public static final MapCodec<ButterflyDex> CODEC = Codec.STRING.listOf()
        .fieldOf("discovered")
        .xmap(list -> new ButterflyDex(new LinkedHashSet<>(list)),
              dex -> List.copyOf(dex.discovered));

    public static final StreamCodec<ByteBuf, ButterflyDex> STREAM_CODEC =
        ByteBufCodecs.STRING_UTF8.apply(ByteBufCodecs.list())
            .map(list -> new ButterflyDex(new LinkedHashSet<>(list)),
                 dex -> List.copyOf(dex.discovered));

    public ButterflyDex {
        // Defensive copy so callers can't mutate our backing set after construction.
        discovered = Set.copyOf(discovered);
    }

    public boolean has(ButterflySpecies species) {
        return discovered.contains(species.id());
    }

    public int count() {
        return discovered.size();
    }

    /** A copy of this dex with {@code species} marked discovered (or {@code this} if already known). */
    public ButterflyDex with(ButterflySpecies species) {
        if (has(species)) {
            return this;
        }
        Set<String> next = new LinkedHashSet<>(discovered);
        next.add(species.id());
        return new ButterflyDex(next);
    }

    /** Every species marked discovered (debug / creative fill). */
    public static ButterflyDex full() {
        Set<String> all = new HashSet<>();
        for (ButterflySpecies s : ButterflySpecies.values()) {
            all.add(s.id());
        }
        return new ButterflyDex(all);
    }

    // ---- player-facing helpers (server side) -------------------------------------------------

    public static ButterflyDex of(Player player) {
        return player.getData(ModAttachments.BUTTERFLY_DEX.get());
    }

    public static boolean isDiscovered(Player player, ButterflySpecies species) {
        return of(player).has(species);
    }

    /**
     * Marks {@code species} discovered for {@code player}. Returns {@code true} if this was a
     * new discovery (so callers can play a sound / send a toast only the first time). Setting
     * the attachment auto-syncs the new dex to the owning client.
     */
    public static boolean discover(Player player, ButterflySpecies species) {
        ButterflyDex current = of(player);
        ButterflyDex next = current.with(species);
        if (next == current) {
            return false;
        }
        player.setData(ModAttachments.BUTTERFLY_DEX.get(), next);
        return true;
    }

    public static void discoverAll(Player player) {
        player.setData(ModAttachments.BUTTERFLY_DEX.get(), full());
    }

    public static void clear(Player player) {
        player.setData(ModAttachments.BUTTERFLY_DEX.get(), EMPTY);
    }
}
