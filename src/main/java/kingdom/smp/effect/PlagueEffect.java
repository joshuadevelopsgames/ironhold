package kingdom.smp.effect;

import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;
import java.util.function.Supplier;

/**
 * Black Plague — single-effect, three-stage disease tracked by remaining duration.
 *
 * <p>Total run: 12000 ticks (10 minutes) if uncured.
 * <ul>
 *   <li>Stage 0 (Incubating, first 2 min) — subtle Hunger I, no visible cue. Curable with milk.</li>
 *   <li>Stage 1 (Symptomatic, next 4 min) — Hunger II, Weakness I, sickly particles, coughing.
 *       Milk no longer works; needs Plague Tonic.</li>
 *   <li>Stage 2 (Dying, last 4 min) — 1 damage every 5s, regen blocked, vignette. Tonic still cures.</li>
 * </ul>
 *
 * <p>Spreads each second to nearby living entities within 4 blocks (5% chance each).
 *
 * <p>Stage detection: based on the remaining duration of the active {@link MobEffectInstance},
 * not on amplifier — simpler than re-applying with a stepping amplifier and avoids races.
 */
public class PlagueEffect extends MobEffect {

    public static final int TOTAL_DURATION_TICKS = 12_000; // 10 minutes
    public static final int STAGE_0_LENGTH = 2_400;        // 2 min
    public static final int STAGE_1_LENGTH = 4_800;        // 4 min
    public static final int STAGE_2_LENGTH = 4_800;        // 4 min

    /** Source of the Plague effect holder; set by Ironhold during registration. */
    private static Supplier<Holder<MobEffect>> holderSupplier = () -> null;

    public static void setHolderSupplier(Supplier<Holder<MobEffect>> supplier) {
        holderSupplier = supplier;
    }

    public static Holder<MobEffect> holder() {
        return holderSupplier.get();
    }

    /** 0, 1, or 2. Pure function of remaining duration. */
    public static int stageOf(MobEffectInstance instance) {
        if (instance == null) return -1;
        int remaining = instance.getDuration();
        if (remaining > STAGE_1_LENGTH + STAGE_2_LENGTH) return 0;
        if (remaining > STAGE_2_LENGTH) return 1;
        return 2;
    }

    public PlagueEffect() {
        super(MobEffectCategory.HARMFUL, 0x4a3b2a); // muddy black-brown
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true; // every tick — we manage cadence internally
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        Holder<MobEffect> me = holder();
        if (me == null) return true;
        MobEffectInstance instance = entity.getEffect(me);
        if (instance == null) return true;

        int stage = stageOf(instance);
        long now = level.getGameTime();

        // Symptoms — applied as short re-applications of vanilla effects
        if (stage == 0) {
            if (now % 40 == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 0, true, false));
            }
        } else if (stage == 1) {
            if (now % 40 == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1, true, false));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0, true, false));
            }
            // Sickly green-black particles
            if (now % 8 == 0) {
                double dx = (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                double dy = level.getRandom().nextDouble() * entity.getBbHeight();
                double dz = (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                level.sendParticles(ParticleTypes.SCULK_SOUL,
                    entity.getX() + dx, entity.getY() + dy, entity.getZ() + dz,
                    1, 0, 0.02, 0, 0.0);
                if (level.getRandom().nextInt(3) == 0) {
                    level.sendParticles(ParticleTypes.ASH,
                        entity.getX() + dx, entity.getY() + dy, entity.getZ() + dz,
                        1, 0, 0.01, 0, 0.0);
                }
            }
            // Coughing sound — occasional
            if (now % 200 == level.getRandom().nextInt(200) && entity instanceof Player) {
                level.playSound(null, entity.getX(), entity.getY(), entity.getZ(),
                    SoundEvents.PLAYER_HURT_DROWN, SoundSource.PLAYERS, 0.3F, 0.6F);
            }
        } else { // stage 2
            if (now % 40 == 0) {
                entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 1, true, false));
                entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 1, true, false));
            }
            // Heavier sickly particles
            if (now % 4 == 0) {
                double dx = (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                double dy = level.getRandom().nextDouble() * entity.getBbHeight();
                double dz = (level.getRandom().nextDouble() - 0.5) * entity.getBbWidth();
                level.sendParticles(ParticleTypes.SCULK_SOUL,
                    entity.getX() + dx, entity.getY() + dy, entity.getZ() + dz,
                    1, 0, 0.02, 0, 0.0);
                level.sendParticles(ParticleTypes.ASH,
                    entity.getX() + dx, entity.getY() + dy, entity.getZ() + dz,
                    1, 0, 0.01, 0, 0.0);
            }
            // Damage tick: every 100 ticks (5s)
            if (now % 100 == 0) {
                entity.hurt(level.damageSources().wither(), 1.0F);
            }
        }

        // Spread — once per second, 4-block radius, 5% chance per nearby entity
        if (now % 20 == 0) {
            spreadToNearby(level, entity, me);
        }

        return true;
    }

    private static void spreadToNearby(ServerLevel level, LivingEntity carrier, Holder<MobEffect> me) {
        AABB area = carrier.getBoundingBox().inflate(4.0);
        List<LivingEntity> nearby = level.getEntitiesOfClass(LivingEntity.class, area,
            e -> e != carrier && e.isAlive() && !e.hasEffect(me) && canBeInfected(e));
        for (LivingEntity victim : nearby) {
            float chance = victim instanceof net.minecraft.world.entity.npc.villager.AbstractVillager
                ? 0.005F   // villagers — much rarer (≈1%/sec across full standing range)
                : 0.05F;
            if (level.getRandom().nextFloat() < chance) {
                victim.addEffect(new MobEffectInstance(me, TOTAL_DURATION_TICKS, 0, false, false, true));
            }
        }
    }

    /** Limit who can catch it — players, cows, villagers, rats. Skip undead/inanimate. */
    private static boolean canBeInfected(LivingEntity e) {
        if (e instanceof Player) return true;
        if (e instanceof net.minecraft.world.entity.animal.cow.AbstractCow) return true;
        if (e instanceof net.minecraft.world.entity.npc.villager.AbstractVillager) return true;
        if (e instanceof kingdom.smp.entity.RatEntity) return true;
        return false;
    }
}
