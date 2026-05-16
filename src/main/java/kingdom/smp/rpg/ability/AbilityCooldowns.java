package kingdom.smp.rpg.ability;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;

import java.util.HashMap;
import java.util.Map;

/**
 * Per-player ability cooldown state. Stored as ability-id → world-tick-of-expiry. Persisted with
 * the player attachment, synced to the client for HUD display, and reset to empty on death.
 */
public record AbilityCooldowns(Map<String, Long> expirations) {

    public static final AbilityCooldowns EMPTY = new AbilityCooldowns(Map.of());

    public static final MapCodec<AbilityCooldowns> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
        Codec.unboundedMap(Codec.STRING, Codec.LONG)
            .optionalFieldOf("expirations", Map.of())
            .forGetter(AbilityCooldowns::expirations)
    ).apply(i, AbilityCooldowns::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, AbilityCooldowns> STREAM_CODEC =
        StreamCodec.composite(
            ByteBufCodecs.map(HashMap::new, ByteBufCodecs.STRING_UTF8, ByteBufCodecs.VAR_LONG),
            AbilityCooldowns::expirations,
            AbilityCooldowns::new
        );

    public static AbilityCooldowns empty() {
        return EMPTY;
    }

    public boolean isOnCooldown(String id, long now) {
        Long expiry = expirations.get(id);
        return expiry != null && expiry > now;
    }

    public long remainingTicks(String id, long now) {
        Long expiry = expirations.get(id);
        if (expiry == null) return 0L;
        return Math.max(0L, expiry - now);
    }

    public AbilityCooldowns withCooldown(String id, long expiryTick) {
        Map<String, Long> next = new HashMap<>(expirations);
        next.put(id, expiryTick);
        return new AbilityCooldowns(Map.copyOf(next));
    }
}
