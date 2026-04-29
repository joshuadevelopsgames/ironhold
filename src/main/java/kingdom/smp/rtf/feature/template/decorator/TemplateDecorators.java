package kingdom.smp.rtf.feature.template.decorator;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;

import kingdom.smp.rtf.platform.RegistryUtil;
import kingdom.smp.rtf.registries.RTFBuiltInRegistries;

public class TemplateDecorators {

	public static void bootstrap() {
		register("tree", TreeDecorator.CODEC);
	}
	
	public static TreeDecorator tree(net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator decorator) {
		return tree(decorator, decorator);
	}
	
	public static TreeDecorator tree(net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator decorator, net.minecraft.world.level.levelgen.feature.treedecorators.TreeDecorator modifiedDecorator) {
		return new TreeDecorator(decorator, modifiedDecorator);
	}

	private static void register(String name, MapCodec<? extends TemplateDecorator<?>> placement) {
		RegistryUtil.register(RTFBuiltInRegistries.TEMPLATE_DECORATOR_TYPE, name, placement);
	}
}
