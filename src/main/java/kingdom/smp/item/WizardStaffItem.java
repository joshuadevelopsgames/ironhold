package kingdom.smp.item;

import kingdom.smp.client.entity.WizardStaffRenderer;
import com.geckolib.animatable.GeoItem;
import com.geckolib.animatable.client.GeoRenderProvider;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUseAnimation;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomModelData;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Wizard Staff — a gem-socket staff that reads whatever gem/metal is in the player's
 * offhand and displays the matching 3D model variant, then casts the gem's signature
 * spell on right-click charge + release.
 *
 * <p>Gem loading is server-side via inventoryTick: the offhand item is mapped to an
 * integer index stored in the stack's CustomModelData float[0]. The GeckoLib renderer
 * reads that index in captureDefaultRenderState and injects it into the render state so
 * WizardStaffModel can switch geometry without any client/server race.
 */
public class WizardStaffItem extends Item implements GeoItem {

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    private static final int COOLDOWN_TICKS = 40;
    private static final double AOE_RADIUS = 8.0;

    /** Maps offhand item → gem index used for model selection and spell dispatch. */
    public static final Map<Item, Integer> GEM_ITEMS = Map.ofEntries(
        Map.entry(Items.DIAMOND,          1),
        Map.entry(Items.EMERALD,          2),
        Map.entry(Items.AMETHYST_SHARD,   3),
        Map.entry(Items.LAPIS_LAZULI,     4),
        Map.entry(Items.QUARTZ,           5),
        Map.entry(Items.PRISMARINE_SHARD, 6),
        Map.entry(Items.ENDER_PEARL,      7),
        Map.entry(Items.NETHER_STAR,      8),
        Map.entry(Items.HEART_OF_THE_SEA, 9),
        Map.entry(Items.ECHO_SHARD,      10),
        Map.entry(Items.IRON_INGOT,      11),
        Map.entry(Items.GOLD_INGOT,      12),
        Map.entry(Items.COPPER_INGOT,    13),
        Map.entry(Items.NETHERITE_INGOT, 14),
        Map.entry(Items.BLAZE_ROD,       15),
        Map.entry(Items.BONE,            16)
    );

    public WizardStaffItem(Properties props) {
        super(props);
    }

    // ── Offhand gem detection ─────────────────────────────────────────────────

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner,
                              @Nullable EquipmentSlot slot) {
        if (!(owner instanceof Player player)) return;
        int gemIdx = GEM_ITEMS.getOrDefault(player.getOffhandItem().getItem(), 0);
        CustomModelData current = stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA,
            CustomModelData.EMPTY);
        Float existing = current.getFloat(0);
        float newVal = (float) gemIdx;
        if (existing == null || existing != newVal) {
            stack.set(DataComponents.CUSTOM_MODEL_DATA,
                new CustomModelData(List.of(newVal), List.of(), List.of(), List.of()));
        }
    }

    // ── Use mechanics ─────────────────────────────────────────────────────────

    @Override
    public ItemUseAnimation getUseAnimation(ItemStack stack) {
        return ItemUseAnimation.SPEAR;
    }

    @Override
    public int getUseDuration(ItemStack stack, LivingEntity entity) {
        return 72000;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        if (loadedGemIdx(player.getItemInHand(hand)) == 0) return InteractionResult.PASS;
        player.startUsingItem(hand);
        return InteractionResult.CONSUME;
    }

    @Override
    public boolean releaseUsing(ItemStack stack, Level level, LivingEntity entity, int timeLeft) {
        if (!(entity instanceof Player player) || level.isClientSide()) return false;
        int gemIdx = loadedGemIdx(stack);
        if (gemIdx == 0) return false;

        castSpell(gemIdx, stack, (ServerLevel) level, player);
        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        stack.hurtAndBreak(1, player, player.getUsedItemHand());
        return true;
    }

    // ── Spell dispatch ────────────────────────────────────────────────────────

    private void castSpell(int gemIdx, ItemStack stack, ServerLevel level, Player player) {
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class,
            new AABB(player.position().subtract(AOE_RADIUS, AOE_RADIUS, AOE_RADIUS),
                     player.position().add(AOE_RADIUS, AOE_RADIUS, AOE_RADIUS)),
            e -> e != player && e.isAlive());

        switch (gemIdx) {
            case 1  -> spellDiamond(player);
            case 2  -> spellEmerald(player);
            case 3  -> spellAmethyst(player, nearby);
            case 4  -> spellLapis(player);
            case 5  -> spellQuartz(level, player, nearby);
            case 6  -> spellPrismarine(player, nearby);
            case 7  -> spellEnderPearl(level, player);
            case 8  -> spellNetherStar(level, player, nearby);
            case 9  -> spellHeartOfTheSea(player);
            case 10 -> spellEchoShard(player, nearby);
            case 11 -> spellIron(player);
            case 12 -> spellGold(level, player);
            case 13 -> spellCopper(level, nearby);
            case 14 -> spellNetherite(nearby);
            case 15 -> spellBlazeRod(level, player, nearby);
            case 16 -> spellBone(player);
        }
    }

    // ── Individual spells ─────────────────────────────────────────────────────

    /** Diamond — Deflect Shield: Resistance II (5 s) + Absorption II (10 s). */
    private void spellDiamond(Player p) {
        p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 100, 1));
        p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 200, 1));
    }

    /** Emerald — Verdant Mend: instant heal + Regeneration III (5 s). */
    private void spellEmerald(Player p) {
        p.addEffect(new MobEffectInstance(MobEffects.INSTANT_HEALTH, 1, 1));
        p.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100, 2));
    }

    /** Amethyst — Crystal Resonance: Slowness III AoE (5 s) + knockback. */
    private void spellAmethyst(Player p, List<LivingEntity> nearby) {
        for (LivingEntity e : nearby) {
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
            Vec3 push = e.position().subtract(p.position()).normalize().scale(2.0);
            e.setDeltaMovement(push.x, 0.5, push.z);
        }
        p.level().playSound(null, p.blockPosition(),
            SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** Lapis — Wisdom Surge: grant 2 XP levels. */
    private void spellLapis(Player p) {
        p.giveExperienceLevels(2);
        p.level().playSound(null, p.blockPosition(),
            SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.75F, 1.2F);
    }

    /** Quartz — Lightning Prism: strike at caster + up to 3 nearby mobs. */
    private void spellQuartz(ServerLevel level, Player p, List<LivingEntity> nearby) {
        strikeLightning(level, p.position());
        int n = 0;
        for (LivingEntity e : nearby) {
            if (n++ >= 3) break;
            strikeLightning(level, e.position());
        }
    }

    /** Prismarine — Tidal Surge: Water Breathing (60 s) + Dolphin's Grace (30 s) + pull AoE. */
    private void spellPrismarine(Player p, List<LivingEntity> nearby) {
        p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 1200, 0));
        p.addEffect(new MobEffectInstance(MobEffects.DOLPHINS_GRACE, 600, 0));
        for (LivingEntity e : nearby) {
            Vec3 pull = p.position().subtract(e.position()).normalize().scale(1.5);
            e.setDeltaMovement(pull.x, 0.3, pull.z);
        }
    }

    /** Ender Pearl — Void Step: teleport 15 blocks in look direction. */
    private void spellEnderPearl(ServerLevel level, Player p) {
        Vec3 look = p.getLookAngle();
        Vec3 dest = p.getEyePosition().add(look.scale(15.0));
        p.teleportTo(dest.x, dest.y, dest.z);
        level.playSound(null, p.blockPosition(),
            SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 1.0F, 1.0F);
    }

    /** Nether Star — Stellar Collapse: small explosion + Wither II (5 s) AoE. */
    private void spellNetherStar(ServerLevel level, Player p, List<LivingEntity> nearby) {
        level.explode(null, p.getX(), p.getY(), p.getZ(), 2.0F, Level.ExplosionInteraction.NONE);
        for (LivingEntity e : nearby) {
            e.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1));
        }
    }

    /** Heart of the Sea — Abyssal Hymn: Conduit Power (30 s) + Water Breathing (60 s) + Strength II (15 s). */
    private void spellHeartOfTheSea(Player p) {
        p.addEffect(new MobEffectInstance(MobEffects.CONDUIT_POWER, 600, 0));
        p.addEffect(new MobEffectInstance(MobEffects.WATER_BREATHING, 1200, 0));
        p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 300, 1));
    }

    /** Echo Shard — Sonic Shatter: Blindness (3 s) + Slowness IV (5 s) + push back AoE. */
    private void spellEchoShard(Player p, List<LivingEntity> nearby) {
        for (LivingEntity e : nearby) {
            e.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 60, 0));
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 3));
            Vec3 push = e.position().subtract(p.position()).normalize().scale(3.0);
            e.setDeltaMovement(push.x, 0.4, push.z);
        }
    }

    /** Iron — Iron Will: Resistance II (10 s) + Strength I (10 s). */
    private void spellIron(Player p) {
        p.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 200, 1));
        p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 200, 0));
    }

    /** Gold — Gilded Blessing: Strength II + Absorption II to all nearby players (15 s). */
    private void spellGold(ServerLevel level, Player caster) {
        for (Player p : level.getPlayers(pl -> pl.distanceTo(caster) <= AOE_RADIUS)) {
            p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 300, 1));
            p.addEffect(new MobEffectInstance(MobEffects.ABSORPTION, 300, 1));
        }
        level.playSound(null, caster.blockPosition(),
            SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.4F);
    }

    /** Copper — Static Discharge: chain lightning on up to 5 nearby mobs. */
    private void spellCopper(ServerLevel level, List<LivingEntity> nearby) {
        int n = 0;
        for (LivingEntity e : nearby) {
            if (n++ >= 5) break;
            strikeLightning(level, e.position());
        }
    }

    /** Netherite — Dark Matter: Wither II (4 s) + Slowness III (5 s) AoE. */
    private void spellNetherite(List<LivingEntity> nearby) {
        for (LivingEntity e : nearby) {
            e.addEffect(new MobEffectInstance(MobEffects.WITHER, 80, 1));
            e.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2));
        }
    }

    /** Blaze Rod — Conflagration: set nearby mobs on fire (8 s) + play blaze sound. */
    private void spellBlazeRod(ServerLevel level, Player p, List<LivingEntity> nearby) {
        for (LivingEntity e : nearby) {
            e.setRemainingFireTicks(160); // 8 seconds
        }
        level.playSound(null, p.blockPosition(),
            SoundEvents.BLAZE_SHOOT, SoundSource.PLAYERS, 1.0F, 0.8F);
    }

    /** Bone — Bone Sprint: Speed III + Jump Boost II (10 s). */
    private void spellBone(Player p) {
        p.addEffect(new MobEffectInstance(MobEffects.SPEED, 200, 2));
        p.addEffect(new MobEffectInstance(MobEffects.JUMP_BOOST, 200, 1));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private static int loadedGemIdx(ItemStack stack) {
        CustomModelData cmd = stack.getOrDefault(DataComponents.CUSTOM_MODEL_DATA,
            CustomModelData.EMPTY);
        Float f = cmd.getFloat(0);
        return (f != null) ? Math.round(f) : 0;
    }

    private static void strikeLightning(ServerLevel level, Vec3 pos) {
        LightningBolt bolt = new LightningBolt(EntityType.LIGHTNING_BOLT, level);
        bolt.setPos(pos.x, pos.y, pos.z);
        level.addFreshEntity(bolt);
    }

    // ── GeckoLib ─────────────────────────────────────────────────────────────

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        // Static model — no animations needed.
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    @Override
    public void createGeoRenderer(Consumer<GeoRenderProvider> consumer) {
        consumer.accept(new GeoRenderProvider() {
            private WizardStaffRenderer renderer;

            @Override
            public GeoItemRenderer<?> getGeoItemRenderer() {
                if (renderer == null) renderer = new WizardStaffRenderer();
                return renderer;
            }
        });
    }
}
