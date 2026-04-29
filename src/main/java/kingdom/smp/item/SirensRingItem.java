package kingdom.smp.item;

import kingdom.smp.ModAttachments;
import kingdom.smp.accessory.AccessoryInventory;
import kingdom.smp.accessory.AccessoryItem;
import net.minecraft.ChatFormatting;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class SirensRingItem extends AccessoryItem {

    private static final int COOLDOWN_TICKS = 300; // 15 seconds
    private static final int LURE_DURATION  = 100; // 5 seconds
    private static final double LURE_RANGE    = 40.0;
    private static final double LURE_STRENGTH = 0.055;

    private static final Map<UUID, Integer> cooldowns  = new HashMap<>();
    private static final Map<UUID, Integer> lureTicks  = new HashMap<>();
    private static final Map<UUID, UUID>    lureTarget = new HashMap<>();

    public SirensRingItem(Properties props) {
        super(props);
    }

    public static boolean isEquipped(Player player) {
        AccessoryInventory inv = player.getData(ModAttachments.ACCESSORY_INV.get());
        for (int i = 0; i < AccessoryInventory.ACCESSORY_SLOTS; i++) {
            if (inv.getItem(i).getItem() instanceof SirensRingItem) return true;
        }
        return false;
    }

    public static void tryActivate(ServerPlayer player) {
        UUID id = player.getUUID();
        if (cooldowns.getOrDefault(id, 0) > 0) return;

        LivingEntity target = findLookedAtTarget(player);
        if (target == null) return;

        lureTarget.put(id, target.getUUID());
        lureTicks.put(id, LURE_DURATION);
        cooldowns.put(id, COOLDOWN_TICKS);
        activateLure(player);
    }

    @Override
    public void onAccessoryTick(Player player, ItemStack stack) {
        if (player.level().isClientSide()) return;
        if (!(player instanceof ServerPlayer sp)) return;

        UUID id = player.getUUID();

        int cd = cooldowns.getOrDefault(id, 0);
        if (cd > 0) cooldowns.put(id, cd - 1);

        int lt = lureTicks.getOrDefault(id, 0);
        if (lt > 0) {
            lureTicks.put(id, lt - 1);
            applyLure(sp);
        }
    }

    private static void activateLure(ServerPlayer player) {
        player.level().playSound(null, player.blockPosition(),
            SoundEvents.ALLAY_AMBIENT_WITHOUT_ITEM, SoundSource.PLAYERS, 1.5F, 0.6F);
        ((ServerLevel) player.level()).sendParticles(ParticleTypes.ENCHANT,
            player.getX(), player.getEyeY(), player.getZ(),
            25, 1.0, 0.5, 1.0, 0.15);
    }

    private static void applyLure(ServerPlayer player) {
        UUID targetId = lureTarget.get(player.getUUID());
        if (targetId == null) return;

        ServerLevel sl = (ServerLevel) player.level();
        LivingEntity target = sl.getEntitiesOfClass(
            LivingEntity.class,
            player.getBoundingBox().inflate(LURE_RANGE + 10),
            e -> e.getUUID().equals(targetId) && e.isAlive()
        ).stream().findFirst().orElse(null);

        if (target == null) {
            lureTicks.put(player.getUUID(), 0);
            lureTarget.remove(player.getUUID());
            return;
        }

        double dist = player.distanceTo(target);
        if (dist > 2.0) {
            Vec3 pull = player.position().subtract(target.position()).normalize().scale(LURE_STRENGTH);
            target.setDeltaMovement(target.getDeltaMovement().add(pull));
            target.hurtMarked = true;

            if (player.tickCount % 20 == 0) {
                target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 40, 0, true, false, true));
            }
        }

        if (player.tickCount % 4 == 0) {
            sl.sendParticles(ParticleTypes.NOTE,
                player.getX() + (player.getRandom().nextDouble() - 0.5) * 1.5,
                player.getEyeY() + 0.3,
                player.getZ() + (player.getRandom().nextDouble() - 0.5) * 1.5,
                1, 0, 0.1, 0, 0);
            sl.sendParticles(ParticleTypes.ENCHANT,
                target.getX(), target.getEyeY(), target.getZ(),
                2, 0.3, 0.2, 0.3, 0.05);
        }
    }

    private static LivingEntity findLookedAtTarget(ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookDir = player.getLookAngle();
        Vec3 endPos  = eyePos.add(lookDir.scale(LURE_RANGE));
        AABB searchBox = player.getBoundingBox().expandTowards(lookDir.scale(LURE_RANGE)).inflate(2.0);

        EntityHitResult hit = ProjectileUtil.getEntityHitResult(
            player, eyePos, endPos, searchBox,
            e -> e instanceof LivingEntity && e.isAlive() && e != player
                && !(e instanceof Player p && (p.isCreative() || p.isSpectator())),
            LURE_RANGE
        );

        if (hit != null && hit.getEntity() instanceof LivingEntity living) {
            return living;
        }
        return null;
    }

    @Override
    public void onUnequipped(Player player, ItemStack stack) {
        UUID id = player.getUUID();
        cooldowns.remove(id);
        lureTicks.remove(id);
        lureTarget.remove(id);
    }

    @Override
    public List<Component> getAccessoryTooltip() {
        return List.of(
            Component.literal("Siren's Call").withStyle(ChatFormatting.AQUA),
            Component.literal("Press [C] to lure the target you're looking at").withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
            Component.literal("5s duration  \u2022  15s cooldown").withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC)
        );
    }
}
