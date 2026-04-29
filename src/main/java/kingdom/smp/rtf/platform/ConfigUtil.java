package kingdom.smp.rtf.platform;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import kingdom.smp.rtf.RTFCommon;
import net.neoforged.fml.loading.FMLPaths;

public class ConfigUtil {
	public static final Path RTF_CONFIG_PATH = getConfigPath().resolve(RTFCommon.MOD_ID);
	public static final Path LEGACY_CONFIG_PATH = getConfigPath().resolve(RTFCommon.LEGACY_MOD_ID);

	public static Path rtf(String path) {
		return RTF_CONFIG_PATH.resolve(path);
	}

	public static Path legacy(String path) {
		return LEGACY_CONFIG_PATH.resolve(path);
	}

	public static Path getConfigPath() {
		return FMLPaths.CONFIGDIR.get();
	}

	static {
		if (!Files.exists(RTF_CONFIG_PATH)) {
			try {
				Files.createDirectory(RTF_CONFIG_PATH);
			} catch (IOException e) {
				throw new RuntimeException("Failed to create RTF config directory", e);
			}
		}
	}
}
