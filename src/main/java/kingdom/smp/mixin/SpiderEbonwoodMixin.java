package kingdom.smp.mixin;

import kingdom.smp.Ironhold;
import net.minecraft.core.Holder;
import net.minecraft.world.entity.ai.goal.target.NearestAttackableTargetGoal;
import net.minecraft.world.entity.monster.spider.Spider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.biome.Biome;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Spiders in the Ebonwood Hollow are always hostile, regardless of light level.
 * Adds an unconditional target goal that bypasses the vanilla light check.
 */
@Mixin(Spider.class)
public abstract class SpiderEbonwoodMixin {

    @Inject(method = "registerGoals", at = @At("TAIL"))
    private void ironhold$alwaysHostileInEbonwood(CallbackInfo ci) {
        Spider self = (Spider) (Object) this;
        // Priority 1 — runs before the vanilla SpiderTargetGoal (priority 2)
        // Only activates when the spider is in the Ebonwood Hollow biome
        self.targetSelector.addGoal(1, new NearestAttackableTargetGoal<>(self, Player.class, true) {
            @Override
            public boolean canUse() {
                Holder<Biome> biome = self.level().getBiome(self.blockPosition());
                if (!biome.is(Ironhold.EBONWOOD_HOLLOW)) return false;
                return super.canUse();
            }
        });
    }
}
