package kingdom.smp.rtf.platform;

import net.neoforged.fml.ModList;

public class ModLoaderUtil {

	public static boolean isLoaded(String modId) {
		return ModList.get().isLoaded(modId);
	}
}
