package kingdom.smp.mixin;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import kingdom.smp.rtf.surface.RTFSurfaceSystem;
import kingdom.smp.rtf.surface.rule.StrataRule;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.levelgen.SurfaceSystem;
import net.minecraft.world.level.levelgen.WorldgenRandom;

@Mixin(SurfaceSystem.class)
public abstract class SurfaceSystemRTFMixin implements RTFSurfaceSystem {
    @Unique
    private final Map<Identifier, List<StrataRule.Strata>> ironhold$strataCache = new HashMap<>();

    @Override
    public List<StrataRule.Strata> getOrCreateStrata(Identifier name, Function<RandomSource, List<StrataRule.Strata>> factory) {
        List<StrataRule.Strata> existing = this.ironhold$strataCache.get(name);
        if (existing != null) {
            return existing;
        }
        synchronized (this.ironhold$strataCache) {
            return this.ironhold$strataCache.computeIfAbsent(name, key -> factory.apply(new WorldgenRandom(new net.minecraft.world.level.levelgen.LegacyRandomSource(key.hashCode()))));
        }
    }
}
