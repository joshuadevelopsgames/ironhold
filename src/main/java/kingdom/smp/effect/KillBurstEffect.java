package kingdom.smp.effect;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.ARGB;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.phys.Vec3;

/**
 * One-shot "kill confirm" burst played at a slain entity. Tiered by the class XP the kill
 * awarded so a mob pop, an elite, and a boss each read differently. Pure vanilla particles +
 * sounds (no asset dependency) so it works the moment it's wired up. Composed in the same
 * server-side {@code sendParticles} style as the Particle Maker effects (see VoidRiftEffect).
 */
public final class KillBurstEffect {
    private KillBurstEffect() {}

    private static final int TIER_ELITE_XP = 10;
    private static final int TIER_BOSS_XP = 70;

    /** @param killXp the (post-bonus) class XP this kill granted; drives the visual/audio tier. */
    public static void spawn(ServerLevel level, LivingEntity victim, int killXp) {
        Vec3 c = victim.position().add(0, victim.getBbHeight() * 0.5, 0);
        if (killXp >= TIER_BOSS_XP) {
            boss(level, c);
        } else if (killXp >= TIER_ELITE_XP) {
            elite(level, c);
        } else {
            normal(level, c);
        }
    }

    // Small mob pop: a tight crit spark ring + soft chime.
    private static void normal(ServerLevel level, Vec3 c) {
        ring(level, ParticleTypes.CRIT, c, 0.45, 8, 0.12);
        level.sendParticles(ParticleTypes.ENCHANTED_HIT, c.x, c.y, c.z, 6, 0.2, 0.2, 0.2, 0.12);
        play(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 0.45f, 1.4f);
    }

    // Elite/strong monster: crimson poof, wider spark, a little rising soul.
    private static void elite(ServerLevel level, Vec3 c) {
        var dust = new DustParticleOptions(ARGB.colorFromFloat(1f, 0.72f, 0.06f, 0.06f), 1.3f);
        level.sendParticles(dust, c.x, c.y, c.z, 18, 0.35, 0.35, 0.35, 0.0);
        ring(level, ParticleTypes.CRIT, c, 0.7, 14, 0.16);
        soulColumn(level, c, 5, 0.7);
        play(level, c, SoundEvents.AMETHYST_BLOCK_CHIME, 0.7f, 0.9f);
        play(level, c, SoundEvents.PLAYER_ATTACK_CRIT, 0.5f, 0.8f);
    }

    // Boss: bright pop, golden eruption, tall soul column, triumphant horn.
    private static void boss(ServerLevel level, Vec3 c) {
        level.sendParticles(ParticleTypes.EXPLOSION, c.x, c.y, c.z, 1, 0, 0, 0, 0);
        var gold = new DustParticleOptions(ARGB.colorFromFloat(1f, 1f, 0.78f, 0.2f), 1.8f);
        level.sendParticles(gold, c.x, c.y, c.z, 60, 0.6, 0.8, 0.6, 0.0);
        ring(level, ParticleTypes.TOTEM_OF_UNDYING, c, 1.1, 24, 0.25);
        soulColumn(level, c, 14, 1.6);
        play(level, c, SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.9f, 1.0f);
        play(level, c, SoundEvents.TOTEM_USE, 0.6f, 1.1f);
    }

    /** Horizontal ring of {@code count} particles at radius {@code r}, given outward speed. */
    private static void ring(ServerLevel level, ParticleOptions p,
                             Vec3 c, double r, int count, double speed) {
        for (int i = 0; i < count; i++) {
            double a = (Math.PI * 2 * i) / count;
            double dx = Math.cos(a);
            double dz = Math.sin(a);
            level.sendParticles(p, c.x + dx * r, c.y, c.z + dz * r, 0, dx * speed, 0.02, dz * speed, 1.0);
        }
    }

    /** Rising souls drifting up from the kill point. */
    private static void soulColumn(ServerLevel level, Vec3 c, int count, double height) {
        for (int i = 0; i < count; i++) {
            double t = (double) i / Math.max(1, count - 1);
            double jitter = 0.18;
            double ox = (level.getRandom().nextDouble() - 0.5) * jitter;
            double oz = (level.getRandom().nextDouble() - 0.5) * jitter;
            level.sendParticles(ParticleTypes.SOUL, c.x + ox, c.y + t * height, c.z + oz, 1, 0, 0.04, 0, 0.01);
        }
    }

    private static void play(ServerLevel level, Vec3 c, SoundEvent sound, float vol, float pitch) {
        level.playSound(null, c.x, c.y, c.z, sound, SoundSource.PLAYERS, vol, pitch);
    }
}
