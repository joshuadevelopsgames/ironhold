package kingdom.smp.effect;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;

/**
 * A hidden effect applied by drinking milk while bleeding.
 * While active, it prevents the BleedingEffect from ticking damage.
 */
public class StifledBleedingEffect extends MobEffect {
    public StifledBleedingEffect() {
        super(MobEffectCategory.BENEFICIAL, 0xffffff); // White
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return false;
    }
}
