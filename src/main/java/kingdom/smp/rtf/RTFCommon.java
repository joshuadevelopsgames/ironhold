package kingdom.smp.rtf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.minecraft.resources.Identifier;

public final class RTFCommon {
	public static final String MOD_ID = "reterraforged";
	public static final String LEGACY_MOD_ID = "terraforged";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private RTFCommon() {}

	public static Identifier location(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
