package kingdom.smp.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** Tiny plague particles with hard alpha-cutout sprites. */
public class PlagueParticle extends SimpleAnimatedParticle {
    private final boolean spore;

    protected PlagueParticle(ClientLevel level, double x, double y, double z,
                             double vx, double vy, double vz, SpriteSet sprites,
                             boolean spore) {
        super(level, x, y, z, sprites, 0.0F);
        this.spore = spore;
        this.hasPhysics = false;
        this.friction = spore ? 0.90F : 0.84F;
        this.gravity = spore ? -0.01F : 0.02F;
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.lifetime = spore ? 24 + this.random.nextInt(14) : 10 + this.random.nextInt(8);
        this.quadSize *= spore
            ? 0.32F + this.random.nextFloat() * 0.18F
            : 0.18F + this.random.nextFloat() * 0.10F;
        this.setSpriteFromAge(sprites);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public void tick() {
        super.tick();
        if (spore) {
            this.xd += (this.random.nextDouble() - 0.5) * 0.002;
            this.zd += (this.random.nextDouble() - 0.5) * 0.002;
        } else if (this.age % 3 == 0) {
            this.yd += 0.025;
            this.xd += (this.random.nextDouble() - 0.5) * 0.025;
            this.zd += (this.random.nextDouble() - 0.5) * 0.025;
        }
    }

    @Override
    public float getQuadSize(float partialTick) {
        float life = (this.age + partialTick) / this.lifetime;
        float shrink = spore ? 1.0F - life * 0.45F : 1.0F - life * 0.25F;
        return this.quadSize * Math.max(0.1F, shrink);
    }

    public static class FleaProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public FleaProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz, RandomSource rnd) {
            return new PlagueParticle(level, x, y, z, vx, vy, vz, sprites, false);
        }
    }

    public static class SporeProvider implements ParticleProvider<SimpleParticleType> {
        private final SpriteSet sprites;

        public SporeProvider(SpriteSet sprites) {
            this.sprites = sprites;
        }

        @Override
        public Particle createParticle(SimpleParticleType type, ClientLevel level,
                                       double x, double y, double z,
                                       double vx, double vy, double vz, RandomSource rnd) {
            return new PlagueParticle(level, x, y, z, vx, vy, vz, sprites, true);
        }
    }
}
