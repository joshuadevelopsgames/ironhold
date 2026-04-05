package kingdom.smp.game;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.ArcaneBoltEntity;
import kingdom.smp.entity.ArcaneOrbEntity;
import kingdom.smp.entity.HexBoltEntity;
import kingdom.smp.item.AnkhShieldItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetActionBarTextPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingEquipmentChangeEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * While blocking with the Ankh Shield: negates magic-type damage and destroys mod magic
 * projectiles that enter a spherical barrier around the player.
 */
public final class AnkhShieldHandler {
    private AnkhShieldHandler() {}

    /**
     * The Ankh Shield is main-hand only; if it lands in the offhand, put it back and warn the player.
     */
    @SubscribeEvent
    public static void onAnkhOffhandRejected(LivingEquipmentChangeEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (event.getSlot() != EquipmentSlot.OFFHAND) {
            return;
        }
        ItemStack to = event.getTo();
        if (!to.is(Ironhold.ANKH_SHIELD.get())) {
            return;
        }
        ItemStack reject = to.copy();
        player.setItemSlot(EquipmentSlot.OFFHAND, event.getFrom().copy());
        if (!player.getInventory().add(reject)) {
            player.drop(reject, false);
        }
        player.connection.send(new ClientboundSetActionBarTextPacket(
                Component.translatable("message.ironhold.ankh_shield_two_handed").withStyle(ChatFormatting.RED)));
    }

    private static boolean isMagicDamage(DamageSource source) {
        return source.is(DamageTypes.MAGIC) || source.is(DamageTypes.INDIRECT_MAGIC);
    }

    /**
     * Negates magic-type damage while the Ankh Shield forcefield is up (blocking). Uses highest
     * priority so the hit is cancelled before other reductions.
     */
    @SubscribeEvent(priority = EventPriority.HIGHEST)
    public static void onIncomingMagic(LivingIncomingDamageEvent event) {
        LivingEntity target = event.getEntity();
        if (!(target instanceof Player) || !AnkhShieldItem.isBlockingWithAnkh(target)) {
            return;
        }
        if (!isMagicDamage(event.getSource())) {
            return;
        }
        event.setAmount(0.0F);
        event.setCanceled(true);
    }

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (event.getEntity().level().isClientSide() || !(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        if (!AnkhShieldItem.isBlockingWithAnkh(player)) {
            return;
        }
        ServerLevel sl = (ServerLevel) player.level();
        Vec3 center = AnkhShieldBarrier.barrierCenter(player);
        AABB search = player.getBoundingBox().inflate(AnkhShieldBarrier.RADIUS + 1.0);
        for (Entity e : sl.getEntities(player, search, AnkhShieldHandler::isMagicProjectileToBlock)) {
            Projectile proj = (Projectile) e;
            if (proj.getOwner() != null && proj.getOwner().is(player)) {
                continue;
            }
            if (proj.position().distanceTo(center) > AnkhShieldBarrier.RADIUS) {
                continue;
            }
            blockMagicProjectile(sl, proj, proj.position());
            proj.discard();
        }
    }

    private static boolean isMagicProjectileToBlock(Entity e) {
        return e instanceof ArcaneOrbEntity || e instanceof ArcaneBoltEntity || e instanceof HexBoltEntity;
    }

    private static void blockMagicProjectile(ServerLevel sl, Projectile projectile, Vec3 hit) {
        sl.sendParticles(ParticleTypes.ENCHANT, hit.x, hit.y, hit.z, 14, 0.35, 0.35, 0.35, 0.06);
        sl.sendParticles(ParticleTypes.END_ROD, hit.x, hit.y, hit.z, 6, 0.12, 0.12, 0.12, 0.02);
        sl.sendParticles(ParticleTypes.TOTEM_OF_UNDYING, hit.x, hit.y, hit.z, 4, 0.2, 0.2, 0.2, 0.04);
        sl.playSound(null, hit.x, hit.y, hit.z, SoundEvents.ENCHANTMENT_TABLE_USE, projectile.getSoundSource(), 0.55F, 1.6F);
    }
}
