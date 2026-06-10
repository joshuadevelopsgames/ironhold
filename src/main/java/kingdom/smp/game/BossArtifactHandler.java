package kingdom.smp.game;

import java.util.Set;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.ModItems;
import kingdom.smp.accessory.AccessoryInventory;
import kingdom.smp.entity.KingEndermanEntity;
import kingdom.smp.entity.StoneGolemEntity;
import kingdom.smp.rpg.ability.AbilityCooldowns;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.EnderMan;
import net.minecraft.world.entity.monster.Endermite;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingChangeTargetEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

/**
 * Behaviour for the boss-dropped signature accessories (passives + earned guaranteed drops + the
 * Ender Regalia blink active). The artifacts themselves are plain {@link kingdom.smp.accessory.AccessoryItem}s;
 * everything that *does* something lives here. Registered to the game bus in {@code Ironhold}.
 *
 * <p>Build identity comes from the 5 accessory slots — you can't wear every boss artifact at once.
 * Spec: {@code specs/fantasia-ports/02-boss-accessories.md}.
 */
public final class BossArtifactHandler {
    private BossArtifactHandler() {}

    private static final Identifier STONEBLOOD_KB =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "accessory_stoneblood_kb");
    private static final String BLINK_CD = "ironhold:ender_blink";
    private static final int BLINK_COOLDOWN = 60;   // ~3s
    private static final double BLINK_DIST = 8.0;
    private static final float STONEBLOOD_DR = 0.85f; // -15% incoming

    // ── Passive ticks (Stoneblood: knockback + slowness immunity) ───────────────

    @SubscribeEvent
    public static void onPlayerTickPost(PlayerTickEvent.Post event) {
        if (!(event.getEntity() instanceof ServerPlayer player) || player.level().isClientSide()) {
            return;
        }
        boolean stoneblood = isEquipped(player, ModItems.STONEBLOOD_AMULET.get());

        // Idempotent: reflect the equipped state onto the knockback-resist attribute each tick
        // (cheap map lookup). No cached flag, so it stays correct across respawn/relog.
        AttributeInstance kb = player.getAttribute(Attributes.KNOCKBACK_RESISTANCE);
        if (kb != null) {
            boolean present = kb.getModifier(STONEBLOOD_KB) != null;
            if (stoneblood && !present) {
                kb.addTransientModifier(new AttributeModifier(STONEBLOOD_KB, 1.0,
                    AttributeModifier.Operation.ADD_VALUE));
            } else if (!stoneblood && present) {
                kb.removeModifier(STONEBLOOD_KB);
            }
        }
        if (stoneblood) {
            player.removeEffect(MobEffects.SLOWNESS); // slowness immunity
        }
    }

    // ── Incoming damage (Stoneblood reduction · Ender Regalia enderman-immune) ──

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) {
            return;
        }
        DamageSource src = event.getSource();

        if (isEquipped(player, ModItems.ENDER_REGALIA.get())
                && (isEnderkin(src.getEntity()) || isEnderkin(src.getDirectEntity()))) {
            event.setAmount(0.0F);
            event.setCanceled(true);
            return;
        }
        if (isEquipped(player, ModItems.STONEBLOOD_AMULET.get())) {
            event.setAmount(event.getAmount() * STONEBLOOD_DR);
        }
    }

    private static boolean isEnderkin(Entity e) {
        return e instanceof EnderMan || e instanceof Endermite;
    }

    // ── Targeting (Endermen ignore Ender Regalia wearers) ──────────────────────

    @SubscribeEvent
    public static void onChangeTarget(LivingChangeTargetEvent event) {
        if (!(event.getNewAboutToBeSetTarget() instanceof ServerPlayer target)) {
            return;
        }
        if (event.getEntity() instanceof EnderMan && isEquipped(target, ModItems.ENDER_REGALIA.get())) {
            event.setNewAboutToBeSetTarget(null);
        }
    }

    // ── Earned guaranteed boss drops ───────────────────────────────────────────

    @SubscribeEvent
    public static void onBossDeath(LivingDeathEvent event) {
        Entity victim = event.getEntity();
        String bossId;
        Item artifact;
        String relicName;
        if (victim instanceof KingEndermanEntity) {
            bossId = "king_enderman";
            artifact = ModItems.ENDER_REGALIA.get();
            relicName = "Ender Regalia";
        } else if (victim instanceof StoneGolemEntity) {
            bossId = "stone_golem";
            artifact = ModItems.STONEBLOOD_AMULET.get();
            relicName = "Stoneblood Amulet";
        } else {
            return;
        }

        ServerPlayer killer = resolveKiller(event.getSource());
        if (killer == null) {
            return;
        }
        EarnedArtifacts earned = killer.getData(ModAttachments.BOSS_ARTIFACTS_EARNED.get());
        if (earned.has(bossId)) {
            return; // already earned — first-kill only, not farmable
        }
        killer.setData(ModAttachments.BOSS_ARTIFACTS_EARNED.get(), earned.with(bossId));

        if (victim.level() instanceof ServerLevel level) {
            ItemEntity drop = new ItemEntity(level,
                victim.getX(), victim.getY() + 0.5, victim.getZ(), new ItemStack(artifact));
            level.addFreshEntity(drop);
        }
        killer.sendSystemMessage(Component.literal("✦ The fallen yields its relic: ")
            .withStyle(ChatFormatting.LIGHT_PURPLE)
            .append(Component.literal(relicName).withStyle(ChatFormatting.AQUA)));
    }

    private static ServerPlayer resolveKiller(DamageSource source) {
        if (source.getEntity() instanceof ServerPlayer sp) {
            return sp;
        }
        if (source.getDirectEntity() instanceof ServerPlayer sp) {
            return sp;
        }
        if (source.getDirectEntity() instanceof Projectile proj && proj.getOwner() instanceof ServerPlayer sp) {
            return sp;
        }
        return null;
    }

    // ── Ender Regalia active: blink (called from ModNetworking) ─────────────────

    public static void tryAccessoryActive(ServerPlayer player) {
        if (!isEquipped(player, ModItems.ENDER_REGALIA.get())) {
            return;
        }
        long now = player.level().getGameTime();
        AbilityCooldowns cds = player.getData(ModAttachments.ABILITY_COOLDOWNS.get());
        if (cds.isOnCooldown(BLINK_CD, now)) {
            return;
        }
        if (blink(player)) {
            player.setData(ModAttachments.ABILITY_COOLDOWNS.get(), cds.withCooldown(BLINK_CD, now + BLINK_COOLDOWN));
        }
    }

    private static boolean blink(ServerPlayer player) {
        if (!(player.level() instanceof ServerLevel level)) {
            return false;
        }
        Vec3 eye = player.getEyePosition();
        Vec3 look = player.getLookAngle();
        Vec3 target = eye.add(look.scale(BLINK_DIST));
        BlockHitResult clip = level.clip(new ClipContext(eye, target,
            ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        Vec3 dest = clip.getType() == HitResult.Type.MISS ? target : clip.getLocation().subtract(look.scale(0.6));
        double feetY = dest.y - player.getEyeHeight();

        level.sendParticles(ParticleTypes.PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
            30, 0.3, 0.5, 0.3, 0.6);
        player.teleportTo(level, dest.x, feetY, dest.z, Set.of(), player.getYRot(), player.getXRot(), false);
        player.fallDistance = 0;
        level.sendParticles(ParticleTypes.REVERSE_PORTAL, player.getX(), player.getY() + 1.0, player.getZ(),
            30, 0.3, 0.5, 0.3, 0.6);
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.8F, 1.2F);
        return true;
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private static boolean isEquipped(ServerPlayer player, Item item) {
        AccessoryInventory inv = player.getData(ModAttachments.ACCESSORY_INV.get());
        for (int i = 0; i < AccessoryInventory.ACCESSORY_SLOTS; i++) {
            if (inv.getItem(i).is(item)) {
                return true;
            }
        }
        return false;
    }
}
