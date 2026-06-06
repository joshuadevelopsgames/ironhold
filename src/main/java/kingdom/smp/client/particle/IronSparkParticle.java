package kingdom.smp.client.particle;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.particle.ParticleProvider;
import net.minecraft.client.particle.SimpleAnimatedParticle;
import net.minecraft.client.particle.SingleQuadParticle;
import net.minecraft.client.particle.SpriteSet;
import net.minecraft.core.particles.SimpleParticleType;
import net.minecraft.util.RandomSource;

/** Tiny orange-white forge spark: arcs up, cools, shrinks, dies fast. */
public class IronSparkParticle extends SimpleAnimatedParticle {

    protected IronSparkParticle(ClientLevel level, double x, double y, double z,
                                double vx, double vy, double vz, SpriteSet sprites) {
        super(level, x, y, z, sprites, 0.0F); // gravity scalar handled via field below
        this.gravity = 0.7F;        // pulls the arc back down
        this.friction = 0.96F;      // air drag
        this.hasPhysics = true;     // collide off the anvil / ground
        this.xd = vx;
        this.yd = vy;
        this.zd = vz;
        this.lifetime = 12 + this.random.nextInt(10);          // ~0.6-1.1s
        this.quadSize *= 0.5F + this.random.nextFloat() * 0.4F; // small
        this.setColor(0xFFE6A8);    // hot white-gold start
        this.setFadeColor(0xFF5A14); // ember orange as it cools
        // Desync comes from the variant's rotated frame order (see ModParticles), not
        // an age offset, so every spark plays its full lifetime.
        this.setSpriteFromAge(sprites);
    }

    @Override
    public SingleQuadParticle.Layer getLayer() {
        // OPAQUE (alpha-cutout) like vanilla GlowParticle: crisper and brighter for
        // hard pixel-art sparks than the inherited TRANSLUCENT blend. Combined with
        // the full-bright emissive lighting from SimpleAnimatedParticle, this gives
        // the glowing-in-the-dark forge look.
        return SingleQuadParticle.Layer.OPAQUE;
    }

    @Override
    public float getQuadSize(float partialTick) {
        // shrink to ~40% over its life
        return this.quadSize * (1.0F - (this.age + partialTick) / this.lifetime * 0.6F);
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
            return new IronSparkParticle(level, x, y, z, vx, vy, vz, sprites);
        }
    }
}
