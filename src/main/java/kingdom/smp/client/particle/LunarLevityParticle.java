package kingdom.smp.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** Pale moonshroom-blue motes that drift upward around Lunar Levity targets. */
public class LunarLevityParticle extends SimpleAnimatedParticle {

    protected LunarLevityParticle(ClientLevel level, double x, double y, double z,
                                  double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, sprites, 0.0F);
        this.hasPhysics = false;
        this.friction = 0.94F;
        this.gravity = -0.018F;
        this.xd = vx + (this.random.nextDouble() - 0.5) * 0.008;
        this.yd = vy + 0.008 + this.random.nextDouble() * 0.014;
        this.zd = vz + (this.random.nextDouble() - 0.5) * 0.008;
        this.lifetime = 24 + this.random.nextInt(18);
        this.quadSize *= 0.18F + this.random.nextFloat() * 0.10F;
        this.setColor(0.62F, 0.83F, 0.96F);
        this.setFadeColor(0x4E91D2);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.TRANSLUCENT;
    }

    @Override
    public void tick() {
        super.tick();
        if (this.removed) return;

        double wobble = this.age + (System.identityHashCode(this) & 31);
        this.xd += Math.sin(wobble * 0.31) * 0.0009;
        this.zd += Math.cos(wobble * 0.27) * 0.0009;
        if (this.age % 7 == 0) {
            this.yd += 0.0025;
        }
    }

    @Override
    public float getQuadSize(float partialTick) {
        float life = (this.age + partialTick) / this.lifetime;
        float bloom = life < 0.22F ? 0.65F + life * 1.6F : 1.0F;
        return this.quadSize * bloom * (1.0F - life * 0.72F);
    }

    public static class Provider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public Provider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz, RandomSource rnd) {
            return new LunarLevityParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
