package kingdom.smp.item;

import kingdom.smp.disguise.DisguiseManager;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.core.particles.ParticleTypes;

/**
 * Tome of the Master of Disguise. Right-click while looking at a creature to take its form;
 * right-click while looking at nothing to drop the disguise. The book itself is the focus —
 * it is not consumed. Any damage breaks the disguise (handled server-side in the damage event).
 */
public class DisguiseTomeItem extends Item {

    /** How far the gaze reaches to pick a target to copy. */
    private static final double REACH = 24.0;
    private static final int COOLDOWN_TICKS = 10;

    public DisguiseTomeItem(Properties props) {
        super(props);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);

        if (player instanceof ServerPlayer sp && level instanceof ServerLevel server) {
            HitResult hit = ProjectileUtil.getHitResultOnViewVector(sp,
                    e -> e instanceof LivingEntity && e != sp && !e.isSpectator(), REACH);

            if (hit instanceof EntityHitResult ehr && ehr.getEntity() instanceof LivingEntity target) {
                if (target instanceof Player) {
                    sp.sendSystemMessage(
                            Component.literal("The tome cannot yet mirror another soul."), true);
                    return InteractionResult.FAIL;
                }
                DisguiseManager.setDisguise(sp, target.getType());
                poof(server, sp);
                server.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.9F, 1.0F);
            } else if (DisguiseManager.isDisguised(sp)) {
                DisguiseManager.clear(sp);
                poof(server, sp);
                server.playSound(null, sp.getX(), sp.getY(), sp.getZ(),
                        SoundEvents.ILLUSIONER_MIRROR_MOVE, SoundSource.PLAYERS, 0.9F, 0.8F);
            } else {
                sp.sendSystemMessage(
                        Component.literal("Look upon a creature to take its form."), true);
                return InteractionResult.FAIL;
            }
            sp.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        }
        return InteractionResult.SUCCESS;
    }

    private static void poof(ServerLevel level, ServerPlayer player) {
        level.sendParticles(ParticleTypes.POOF,
                player.getX(), player.getY() + player.getBbHeight() * 0.5, player.getZ(),
                24, 0.4, 0.6, 0.4, 0.02);
    }
}
