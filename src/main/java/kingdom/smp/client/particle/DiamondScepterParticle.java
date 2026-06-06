package kingdom.smp.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** Bright cyan-white crystal motes used by the arcane scepter charge and orb. */
public class DiamondScepterParticle extends SimpleAnimatedParticle {

    protected DiamondScepterParticle(ClientLevel level, double x, double y, double z,
                                     double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, sprites, 0.0F);
        this.hasPhysics = false;
        this.friction = 0.90F;
        this.gravity = -0.02F;
        this.xd = vx + (this.random.nextDouble() - 0.5) * 0.01;
        this.yd = vy + 0.01 + this.random.nextDouble() * 0.02;
        this.zd = vz + (this.random.nextDouble() - 0.5) * 0.01;
        this.lifetime = 16 + this.random.nextInt(10);
        this.quadSize *= 0.28F + this.random.nextFloat() * 0.18F;
        this.setColor(0xBFFFFF);
        this.setFadeColor(0x3DA9FF);
        this.setSpriteFromAge(sprites);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public void tick() {
        super.tick();
        if (!this.removed) {
            this.xd += (this.random.nextDouble() - 0.5) * 0.004;
            this.zd += (this.random.nextDouble() - 0.5) * 0.004;
        }
    }

    @Override
    public float getQuadSize(float partialTick) {
        float life = (this.age + partialTick) / this.lifetime;
        float twinkle = life < 0.25F ? 1.0F + life * 1.6F : 1.4F - life * 0.75F;
        return this.quadSize * Math.max(0.18F, twinkle);
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
            return new DiamondScepterParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
