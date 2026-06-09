package kingdom.smp.enchant;

import com.mojang.serialization.MapCodec;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.effects.EnchantmentEntityEffect;
import net.minecraft.world.phys.Vec3;

/**
 * Strips gravity from a freshly-spawned projectile so it flies dead straight instead of arcing.
 *
 * <p>Hooked in via the vanilla {@code minecraft:projectile_spawned} enchantment effect component
 * (see {@code data/ironhold/enchantment/trueflight.json}). When a bow/crossbow carrying that
 * enchantment fires, {@link net.minecraft.world.entity.projectile.Projectile#applyOnProjectileSpawned}
 * runs every {@code projectile_spawned} effect on the new projectile — this one calls
 * {@link Entity#setNoGravity(boolean)}.
 *
 * <p>{@code setNoGravity} is honoured by {@link net.minecraft.world.entity.Entity#getGravity()}
 * (which the arrow's {@code applyGravity()} uses), so the vertical drop becomes zero. Air drag still
 * applies, so the arrow flies in a flat line and gradually slows rather than ever curving down —
 * exactly the "true flight" trajectory.
 *
 * <p>The effect carries no configuration, so its codec is a {@link MapCodec#unit unit} codec.
 */
public record NoGravityProjectileEffect() implements EnchantmentEntityEffect {
    public static final MapCodec<NoGravityProjectileEffect> CODEC =
        MapCodec.unit(new NoGravityProjectileEffect());

    @Override
    public void apply(ServerLevel level, int enchantmentLevel, EnchantedItemInUse item, Entity entity, Vec3 origin) {
        entity.setNoGravity(true);
    }

    @Override
    public MapCodec<? extends EnchantmentEntityEffect> codec() {
        return CODEC;
    }
}
