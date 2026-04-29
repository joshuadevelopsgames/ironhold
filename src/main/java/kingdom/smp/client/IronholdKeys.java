package kingdom.smp.client;

import kingdom.smp.Ironhold;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

public final class IronholdKeys {
    private IronholdKeys() {}

    private static final KeyMapping.Category CATEGORY =
        new KeyMapping.Category(Identifier.fromNamespaceAndPath(Ironhold.MODID, "ironhold"));

    public static final KeyMapping SIREN_LURE = new KeyMapping(
        "key.ironhold.siren_lure",
        GLFW.GLFW_KEY_C,
        CATEGORY
    );
}
