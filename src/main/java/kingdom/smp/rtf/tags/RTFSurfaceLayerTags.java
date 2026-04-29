package kingdom.smp.rtf.tags;

import net.minecraft.tags.TagKey;
import kingdom.smp.rtf.RTFCommon;
import kingdom.smp.rtf.registries.RTFRegistries;
import kingdom.smp.rtf.surface.rule.LayeredSurfaceRule;

public class RTFSurfaceLayerTags {
	public static final TagKey<LayeredSurfaceRule.Layer> TERRABLENDER = resolve("terrablender");
	
    private static TagKey<LayeredSurfaceRule.Layer> resolve(String path) {
    	return TagKey.create(RTFRegistries.SURFACE_LAYERS, RTFCommon.location(path));
    }
}
