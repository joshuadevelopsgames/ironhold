package kingdom.smp.skill.useskill;

import kingdom.smp.ModAttachments;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.Brain;
import net.minecraft.world.entity.ai.behavior.EntityTracker;
import net.minecraft.world.entity.ai.behavior.PositionTracker;
import net.minecraft.world.entity.ai.memory.MemoryModuleType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.Optional;

/**
 * Suppresses vanilla-villager head-turns and trade-prep attention toward
 * players who are actively sneaking.
 *
 * <p>Every {@link #TICK_INTERVAL} ticks per crouching player, scans for
 * villagers within {@link #ATTENTION_RADIUS} and erases their
 * {@link MemoryModuleType#NEAREST_VISIBLE_PLAYER} and
 * {@link MemoryModuleType#LOOK_TARGET} brain memories iff those memories
 * point at the sneaking player.
 *
 * <p>The vanilla {@code PlayerSensor} runs every ~20 ticks and refills
 * {@code NEAREST_VISIBLE_PLAYER}; clearing every 5 ticks means the memory
 * is empty most of the cycle, so behaviors that read it
 * ({@code LookAtPlayer}, {@code InteractWithPlayer}) effectively stop
 * firing. Panic and hurt-by behaviors use different memory modules, so
 * attacking a villager still triggers normal aggro/golem-summon.
 *
 * <p>"Stealthing" predicate is intentionally conservative: crouching AND
 * not mid-attack swing. Swinging the arm or sprinting (impossible while
 * crouched but checked anyway) drops stealth and villagers notice
 * normally again on the next sensor cycle.
 */
public final class VillagerStealthHandler {
    private VillagerStealthHandler() {}

    private static final int TICK_INTERVAL = 5;
    private static final double ATTENTION_RADIUS = 32.0;
    /** Base chance per check that a villager forgets a sneaking player. Adds Sneak level on top. */
    private static final float BASE_CLEAR_CHANCE = 0.30f;
    private static final float MAX_CLEAR_CHANCE = 1.00f;

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if ((player.tickCount % TICK_INTERVAL) != 0) return;
        if (!isStealthing(player)) return;

        float clearChance = clearChance(player);
        AABB box = player.getBoundingBox().inflate(ATTENTION_RADIUS);
        for (Villager villager : player.level().getEntitiesOfClass(Villager.class, box)) {
            // Distrusted players never get the attention scrubbed — the villager
            // has them flagged as a known thief and watches them deliberately.
            if (DistrustState.isDistrusted(villager, player.getUUID())) continue;
            if (villager.getRandom().nextFloat() >= clearChance) continue;
            forgetPlayer(villager, player);
        }
    }

    /**
     * Probability per check that a villager will forget a sneaking player.
     * Starts at {@link #BASE_CLEAR_CHANCE} (crouching alone gives some
     * stealth) and ramps to {@link #MAX_CLEAR_CHANCE} at Sneak {@link
     * UseSkill#MAX_LEVEL}.
     */
    private static float clearChance(ServerPlayer player) {
        PlayerUseSkills skills = player.getData(ModAttachments.USE_SKILLS.get());
        int sneakLevel = skills.levelFor(UseSkill.SNEAK);
        float t = sneakLevel / (float) UseSkill.MAX_LEVEL;
        return BASE_CLEAR_CHANCE + (MAX_CLEAR_CHANCE - BASE_CLEAR_CHANCE) * t;
    }

    private static boolean isStealthing(ServerPlayer player) {
        if (!player.isCrouching()) return false;
        if (player.swinging) return false;
        if (player.isSprinting()) return false;
        return true;
    }

    private static void forgetPlayer(Villager villager, ServerPlayer player) {
        Brain<?> brain = villager.getBrain();

        Optional<Player> visible = brain.getMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
        if (visible.isPresent() && visible.get() == player) {
            brain.eraseMemory(MemoryModuleType.NEAREST_VISIBLE_PLAYER);
        }

        Optional<PositionTracker> look = brain.getMemory(MemoryModuleType.LOOK_TARGET);
        if (look.isPresent() && look.get() instanceof EntityTracker tracker
            && tracker.getEntity() == player) {
            brain.eraseMemory(MemoryModuleType.LOOK_TARGET);
        }
    }
}
