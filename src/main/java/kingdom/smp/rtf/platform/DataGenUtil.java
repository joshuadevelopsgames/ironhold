package kingdom.smp.rtf.platform;

import java.util.concurrent.CompletableFuture;

import net.minecraft.core.HolderLookup;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.data.registries.RegistriesDatapackGenerator;

public final class DataGenUtil {

	public static DataProvider createRegistryProvider(PackOutput output, CompletableFuture<HolderLookup.Provider> providerLookup) {
		return new RegistriesDatapackGenerator(output, providerLookup);
	}
}
