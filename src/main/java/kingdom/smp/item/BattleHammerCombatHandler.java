package kingdom.smp.item;

import kingdom.smp.Ironhold;
import kingdom.smp.ModParticles;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.player.CriticalHitEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Builds the Battle Hammer's forge-power charge from a critical-hit combo. Each consecutive
 * crit on a living target (while holding the hammer) bumps the stack's {@link ForgeCharge}
 * level via {@link BattleHammerItem#addCritCharge}; the level drives the inner-ring glow and
 * the ground-slam power. A non-crit hit that still lands keeps the charge — only a full miss
 * (whiffing a swing into the air, reported from the client via {@code BattleHammerMissPayload}
 * → {@link #onSwingMiss}) breaks the combo. Server-authoritative — the synced component
 * carries the level to the client for rendering. Also grants knockback resistance while a
 * charged hammer is held — forge power roots the wielder like an anvil.
 */
public final class BattleHammerCombatHandler {

    private BattleHammerCombatHandler() {}

    /** Knockback-resistance bonus while holding a charged hammer (+2 = effectively immovable). */
    private static final Identifier FORGE_ANCHOR_ID =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "battle_hammer_forge_anchor");
    private static final double FORGE_ANCHOR_AMOUNT = 2.0;

    @SubscribeEvent
    public static void onCriticalHit(CriticalHitEvent event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;
        if (!(event.getTarget() instanceof LivingEntity)) return;

        // The combo builds on the hammer whether it's the attacking weapon (main hand) or
        // just carried in the off hand while you swing something else. Prefer the main hand
        // if it's the hammer, otherwise fall back to the off hand.
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof BattleHammerItem)) {
            stack = player.getOffhandItem();
            if (!(stack.getItem() instanceof BattleHammerItem)) return;
        }

        ServerLevel level = (ServerLevel) player.level();

        // A non-crit hit still landed, so the combo holds — only a full miss (handled in
        // {@link #onSwingMiss}) clears it. The combo otherwise persists indefinitely:
        // switching weapons, waiting, or landing plain hits never drops it.
        if (!event.isCriticalHit()) return;

        int newLevel = BattleHammerItem.addCritCharge(stack, level);
        if (newLevel <= 0) return;

        // Forge feedback that climbs with the combo: a brighter anvil clang + more sparks.
        float frac = newLevel / (float) BattleHammerItem.MAX_FORGE_CHARGE;
        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ANVIL_LAND, SoundSource.PLAYERS, 0.35F, 0.9F + 0.6F * frac);
        level.sendParticles(ModParticles.IRON_SPARK.get(),
            player.getX(), player.getY() + player.getBbHeight() * 0.6, player.getZ(),
            4 + newLevel, 0.3, 0.4, 0.3, 0.05);
    }

    /**
     * Grants +{@value #FORGE_ANCHOR_AMOUNT} knockback resistance while the player holds a
     * charged hammer (in either hand), and clears it otherwise. Uses a transient modifier so
     * it never persists — it's re-applied each tick the condition holds.
     */
    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide()) return;

        AttributeInstance kbResist = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kbResist == null) return;

        if (isHoldingChargedHammer(player)) {
            kbResist.addOrUpdateTransientModifier(new AttributeModifier(
                FORGE_ANCHOR_ID, FORGE_ANCHOR_AMOUNT, AttributeModifier.Operation.ADD_VALUE));
        } else {
            kbResist.removeModifier(FORGE_ANCHOR_ID);
        }
    }

    /**
     * Breaks the forge combo when the wielder whiffs a swing into the air (a "full miss").
     * Resets a Battle Hammer held in either hand — mirroring {@link #onCriticalHit}, which
     * builds the combo whether the hammer is the swung weapon or just carried off-hand.
     * Invoked from the network layer when the client reports a {@code BattleHammerMissPayload}
     * (misses fire only client-side, so they round-trip through the server to clear the
     * server-authoritative charge component).
     */
    public static void onSwingMiss(Player player) {
        BattleHammerItem.resetCharge(player.getMainHandItem());
        BattleHammerItem.resetCharge(player.getOffhandItem());
    }

    private static boolean isHoldingChargedHammer(Player player) {
        return BattleHammerItem.hasForgeCharge(player.getMainHandItem())
            || BattleHammerItem.hasForgeCharge(player.getOffhandItem());
    }
}
