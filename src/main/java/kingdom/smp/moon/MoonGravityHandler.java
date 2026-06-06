package kingdom.smp.moon;

import kingdom.smp.Ironhold;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingFallEvent;
import net.neoforged.neoforge.event.tick.EntityTickEvent;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Directional moon gravity (iteration 1 of "walk on any face").
 *
 * On the moon we cancel vanilla's straight-down gravity entirely and instead
 * pull every living entity toward the moon's core, snapped to the nearest
 * cardinal axis by {@link GravityHelper}. The result: stepping off the top
 * edge flips "down" onto the side face and presses you against it, the
 * underside holds you up, etc. Magnitude is ~1/6 g, like the real moon.
 *
 * Still TODO (needs in-game tuning): reorienting the camera so a side face
 * reads as the floor, and remapping WASD/jump into the gravity-local frame so
 * walking feels natural rather than "magnetised".
 */
public class MoonGravityHandler {

    private static final Identifier MOON_GRAVITY_ID =
        Identifier.fromNamespaceAndPath("ironhold", "moon_directional_gravity");

    // GRAVITY attribute multipliers (ADD_MULTIPLIED_TOTAL): -1.0 -> 0 (cancel, sides use
    // custom travel); -5/6 -> 1/6 g (low-gravity moon feel on the top face).
    private static final double CANCEL_VANILLA = -1.0;
    private static final double MOON_LOW_GRAVITY = -5.0 / 6.0;

    // Per-tick pull toward the core for the custom (side-face) physics: ~1/6 of 0.08.
    private static final double MOON_PULL = 0.08 / 6.0;

    /** Ensure the moon gravity modifier is present with exactly {@code amount}. */
    private static void ironhold$setGravityModifier(AttributeInstance attr, double amount) {
        AttributeModifier cur = attr.getModifier(MOON_GRAVITY_ID);
        if (cur != null) {
            if (cur.amount() == amount) return;
            attr.removeModifier(MOON_GRAVITY_ID);
        }
        attr.addTransientModifier(new AttributeModifier(
            MOON_GRAVITY_ID, amount, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
    }

    @SubscribeEvent
    public static void onEntityTick(EntityTickEvent.Pre event) {
        if (!(event.getEntity() instanceof LivingEntity living)) return;

        AttributeInstance gravityAttr = living.getAttribute(Attributes.GRAVITY);
        if (gravityAttr == null) return;

        boolean onMoon = living.level().dimension() == ModMoonDimensions.MOON_LEVEL;
        // Single per-tick writer of the gravity state; everyone else reads it.
        Direction g = onMoon ? GravityHelper.updateGravityDirection(living) : Direction.DOWN;

        if (!onMoon) {
            if (gravityAttr.getModifier(MOON_GRAVITY_ID) != null) gravityAttr.removeModifier(MOON_GRAVITY_ID);
            return;
        }

        // Top face: vanilla physics, but at 1/6 g so it feels like the rest of the moon.
        if (g == Direction.DOWN) {
            ironhold$setGravityModifier(gravityAttr, MOON_LOW_GRAVITY);
            return;
        }

        // Side/ceiling faces: fully cancel vanilla gravity; the custom travel mixin (player)
        // or the pull below (mobs) supplies the 1/6-g pull toward the face.
        ironhold$setGravityModifier(gravityAttr, CANCEL_VANILLA);

        if (!(living instanceof Player)) {
            living.setDeltaMovement(living.getDeltaMovement().add(
                new Vec3(g.getStepX() * MOON_PULL, g.getStepY() * MOON_PULL, g.getStepZ() * MOON_PULL)));
            living.resetFallDistance();
            return;
        }

        // Players: no fall damage from gravity flips; show the live readout.
        living.resetFallDistance();
        {
            Player player = (Player) living;
            if (living.level().isClientSide()) {
                net.minecraft.world.phys.AABB bb = living.getBoundingBox();
                player.sendOverlayMessage(Component.literal(String.format(
                    "g:%s grnd:%b box:%.1fx%.1fx%.1f pos=(%.1f,%.1f,%.1f)",
                    g, living.onGround(),
                    bb.getXsize(), bb.getYsize(), bb.getZsize(),
                    living.getX(), living.getY(), living.getZ())));
            } else {
                // Server-side: log only when it changes, so I can read it back.
                UUID id = player.getUUID();
                Direction prev = LAST_LOGGED.get(id);
                if (g != prev) {
                    LAST_LOGGED.put(id, g);
                    net.minecraft.world.phys.AABB bb = living.getBoundingBox();
                    Ironhold.LOGGER.info("[MoonGrav] {} gravity {} -> {} at ({}, {}, {}) box={}x{}x{} onGround={}",
                        player.getName().getString(), prev, g,
                        (int) living.getX(), (int) living.getY(), (int) living.getZ(),
                        String.format("%.1f", bb.getXsize()), String.format("%.1f", bb.getYsize()),
                        String.format("%.1f", bb.getZsize()), living.onGround());
                }
            }
        }
    }

    /**
     * Cancel fall damage anywhere on the moon. Gravity here flips between faces and runs at
     * ~1/6 g, so vanilla's accumulated fall distance produces bogus damage (e.g. a wall flip
     * mid-air, or a gentle low-g drop, reads as a killer fall). Low-gravity moon = soft
     * landings, so we drop it entirely rather than try to scale it.
     */
    @SubscribeEvent
    public static void onLivingFall(LivingFallEvent event) {
        if (event.getEntity().level().dimension() == ModMoonDimensions.MOON_LEVEL) {
            event.setCanceled(true);
        }
    }

    private static final Map<UUID, Direction> LAST_LOGGED = new ConcurrentHashMap<>();
}
