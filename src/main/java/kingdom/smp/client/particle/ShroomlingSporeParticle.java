package kingdom.smp.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** Soft floating spores emitted by Shroomlings, tinted per variant. */
public class ShroomlingSporeParticle extends SimpleAnimatedParticle {
    private final boolean orange;

    protected ShroomlingSporeParticle(ClientLevel level, double x, double y, double z,
                                      double vx, double vy, double vz, SpriteSet sprites,
                                      boolean orange) {
        super(level, x, y, z, sprites, 0.0F);
        this.orange = orange;
        this.hasPhysics = false;
        this.friction = 0.91F;
        this.gravity = -0.012F;
        this.xd = vx + (this.random.nextDouble() - 0.5) * 0.01;
        this.yd = vy + 0.004 + this.random.nextDouble() * 0.012;
        this.zd = vz + (this.random.nextDouble() - 0.5) * 0.01;
        this.lifetime = (orange ? 34 : 30) + this.random.nextInt(18);
        this.quadSize *= (orange ? 0.24F : 0.22F) + this.random.nextFloat() * 0.11F;
        this.setColor(orange ? 1.0F : 0.62F, orange ? 0.62F : 0.98F, orange ? 0.28F : 0.92F);
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

        this.xd += Math.sin((this.age + this.idSeed()) * 0.33) * 0.0012;
        this.zd += Math.cos((this.age + this.idSeed()) * 0.29) * 0.0012;
        if (orange && this.age % 6 == 0) {
            this.yd += 0.003;
        }
    }

    @Override
    public float getQuadSize(float partialTick) {
        float life = (this.age + partialTick) / this.lifetime;
        float bloom = life < 0.25F ? 0.7F + life * 1.2F : 1.0F;
        return this.quadSize * bloom * (1.0F - life * 0.65F);
    }

    private int idSeed() {
        return System.identityHashCode(this) & 31;
    }

    public static class BlueProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public BlueProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz, RandomSource rnd) {
            return new ShroomlingSporeParticle(level, x, y, z, vx, vy, vz, sprites, false);
        }
    }

    public static class OrangeProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public OrangeProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz, RandomSource rnd) {
            return new ShroomlingSporeParticle(level, x, y, z, vx, vy, vz, sprites, true);
        }
    }
}
