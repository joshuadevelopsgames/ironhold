package kingdom.smp.rtf.compat.terrablender;

import net.minecraft.world.level.levelgen.SurfaceRules;

public final class TBSurfaceRules {

	private TBSurfaceRules() {}

	public static void bootstrap() {
	}

	public static SurfaceRules.RuleSource rule(String category, String modId) {
		throw new UnsupportedOperationException("TerraBlender compat is disabled in this build.");
	}
}
