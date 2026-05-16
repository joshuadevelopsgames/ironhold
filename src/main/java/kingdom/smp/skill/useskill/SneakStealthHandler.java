package kingdom.smp.skill.useskill;

import kingdom.smp.ModAttachments;
import kingdom.smp.entity.KnightEntity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.animal.golem.IronGolem;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Ravager;
import net.minecraft.world.entity.monster.illager.AbstractIllager;
import net.minecraft.world.entity.monster.piglin.AbstractPiglin;
import net.minecraft.world.entity.monster.skeleton.WitherSkeleton;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.AABB;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Skyrim-style "stay hidden" effect of the {@link UseSkill#SNEAK} skill.
 *
 * <p>Two mechanisms while the player is crouched:
 * <ul>
 *   <li><b>Acquisition ignore</b> — on {@link LivingChangeTargetEvent}, mobs
 *       about to set this player as their target fail the acquisition with a
 *       probability scaling with Sneak level.</li>
 *   <li><b>Lose interest</b> — every {@value #LOSE_INTEREST_INTERVAL} ticks,
 *       mobs that currently have the player as target roll to drop the
 *       target. Probability scales with Sneak level.</li>
 * </ul>
 *
 * <p><b>Cunning mobs</b> (Warden, Enderman, Iron Golem, Ironhold Knights,
 * Piglins, Illagers, Wither, Wither Skeletons, Ravager) are immune until
 * Sneak ≥ {@value #CUNNING_THRESHOLD}, then gain partial vulnerability that
 * caps at half the rate of regular mobs.
 *
 * <p>Tuning (regular mobs, per acquisition / per second):
 * <table border="1">
 *   <tr><th>Sneak</th><th>Ignore acquisition</th><th>Lose interest / s</th></tr>
 *   <tr><td>0</td><td>0%</td><td>0%</td></tr>
 *   <tr><td>50</td><td>50%</td><td>10%</td></tr>
 *   <tr><td>100</td><td>99.5%</td><td>20%</td></tr>
 * </table>
 *
 * <p>Tuning (cunning mobs):
 * <table border="1">
 *   <tr><th>Sneak</th><th>Ignore acquisition</th><th>Lose interest / s</th></tr>
 *   <tr><td>&lt;90</td><td>0%</td><td>0%</td></tr>
 *   <tr><td>95</td><td>25%</td><td>5%</td></tr>
 *   <tr><td>100</td><td>50%</td><td>10%</td></tr>
 * </table>
 */
public final class SneakStealthHandler {
    private SneakStealthHandler() {}

    private static final int LOSE_INTEREST_INTERVAL = 20;
    private static final double INTEREST_LOSS_RADIUS = 48.0;
    private static final int CUNNING_THRESHOLD = 90;

    @SubscribeEvent
    public static void onLivingChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer target)) return;
        if (!(event.getEntity() instanceof Mob mob)) return;
        if (!target.isCrouching()) return;

        int sneakLevel = sneakLevel(target);
        if (sneakLevel <= 0) return;

        float ignoreChance = ignoreAcquisitionChance(sneakLevel, isCunning(mob));
        if (ignoreChance <= 0f) return;

        if (mob.getRandom().nextFloat() < ignoreChance) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;
        if (player.level().isClientSide()) return;
        if ((player.tickCount % LOSE_INTEREST_INTERVAL) != 0) return;
        if (!player.isCrouching()) return;

        int sneakLevel = sneakLevel(player);
        if (sneakLevel <= 0) return;

        AABB box = player.getBoundingBox().inflate(INTEREST_LOSS_RADIUS);
        for (Mob mob : player.level().getEntitiesOfClass(Mob.class, box)) {
            if (mob.getTarget() != player) continue;
            float loseChance = loseInterestChance(sneakLevel, isCunning(mob));
            if (loseChance <= 0f) continue;
            if (mob.getRandom().nextFloat() < loseChance) {
                mob.setTarget(null);
            }
        }
    }

    private static int sneakLevel(ServerPlayer player) {
        PlayerUseSkills skills = player.getData(ModAttachments.USE_SKILLS.get());
        return skills.levelFor(UseSkill.SNEAK);
    }

    private static float ignoreAcquisitionChance(int sneakLevel, boolean cunning) {
        if (cunning) {
            if (sneakLevel < CUNNING_THRESHOLD) return 0f;
            float t = (sneakLevel - CUNNING_THRESHOLD) / (float) (UseSkill.MAX_LEVEL - CUNNING_THRESHOLD);
            return 0.50f * t;
        }
        return Math.min(0.995f, sneakLevel * 0.01f);
    }

    private static float loseInterestChance(int sneakLevel, boolean cunning) {
        if (cunning) {
            if (sneakLevel < CUNNING_THRESHOLD) return 0f;
            float t = (sneakLevel - CUNNING_THRESHOLD) / (float) (UseSkill.MAX_LEVEL - CUNNING_THRESHOLD);
            return 0.10f * t;
        }
        return Math.min(0.20f, sneakLevel * 0.002f);
    }

    /** Mobs that don't fall for sneak tricks until the player is genuinely a master. */
    private static boolean isCunning(Mob mob) {
        return mob instanceof Warden
            || mob instanceof EnderMan
            || mob instanceof IronGolem
            || mob instanceof KnightEntity
            || mob instanceof Ravager
            || mob instanceof AbstractPiglin
            || mob instanceof AbstractIllager
            || mob instanceof WitherSkeleton
            || mob instanceof WitherBoss;
    }
}
