package kingdom.smp.rtf.surface;

import java.util.List;
import java.util.function.Function;

import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import kingdom.smp.rtf.surface.rule.StrataRule.Strata;

public interface RTFSurfaceSystem {
	List<Strata> getOrCreateStrata(Identifier name, Function<RandomSource, List<Strata>> factory);
}
