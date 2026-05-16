package kingdom.smp.client;

import kingdom.smp.Ironhold;
import net.minecraft.core.ClientAsset;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerModelType;
import net.minecraft.world.entity.player.PlayerSkin;

/**
 * Shared {@link PlayerSkin} instances built from the bundled Kangarude /
 * Kangabrine PNGs. Used by {@link kingdom.smp.client.entity.KangarudeRenderer}
 * to skin the entity itself and by the player-list mixin so the same face
 * appears in the tab list.
 */
public final class KangarudeSkins {
    private KangarudeSkins() {}

    public static final Identifier KANGARUDE_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/kangarude/kangarude.png");
    public static final Identifier KANGABRINE_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/entity/kangarude/kangabrine.png");

    public static final PlayerSkin KANGARUDE_SKIN = build(KANGARUDE_TEXTURE);
    public static final PlayerSkin KANGABRINE_SKIN = build(KANGABRINE_TEXTURE);

    private static PlayerSkin build(Identifier bodyTex) {
        PlayerSkin defaults = DefaultPlayerSkin.getDefaultSkin();
        // Two-arg ctor — the single-arg form mangles the path through
        // Identifier.withPath and resolves to the missing-texture asset.
        ClientAsset.Texture body = new ClientAsset.ResourceTexture(bodyTex, bodyTex);
        return new PlayerSkin(body, defaults.cape(), defaults.elytra(), PlayerModelType.WIDE, false);
    }
}
