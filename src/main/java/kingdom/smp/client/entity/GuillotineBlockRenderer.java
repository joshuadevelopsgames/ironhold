package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.block.GuillotineBlockEntity;
import com.geckolib.constant.DataTickets;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.model.DefaultedBlockGeoModel;
import com.geckolib.renderer.GeoBlockRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;

import java.util.Map;

public class GuillotineBlockRenderer extends GeoBlockRenderer<GuillotineBlockEntity, BlockEntityRenderState> {

    private static final DataTicket<Identifier> GUILLOTINE_TEXTURE =
        DataTickets.create("guillotine_texture", Identifier.class);

    public GuillotineBlockRenderer(BlockEntityRendererProvider.Context ctx) {
        super(ctx, new GuillotineGeoModel());
    }

    @Override
    public void captureDefaultRenderState(GuillotineBlockEntity blockEntity, Void unused,
                                           BlockEntityRenderState state, float partialTick) {
        super.captureDefaultRenderState(blockEntity, unused, state, partialTick);
        // Resolve the texture for this variant and stash it on the render state
        Block block = blockEntity.getBlockState().getBlock();
        Identifier texture = GuillotineGeoModel.TEXTURE_MAP.getOrDefault(block, GuillotineGeoModel.DEFAULT_TEXTURE);
        ((GeoRenderState) state).addGeckolibData(GUILLOTINE_TEXTURE, texture);
    }

    /**
     * Custom GeoModel that reads the texture from the render state's DataTicket.
     */
    static class GuillotineGeoModel extends DefaultedBlockGeoModel<GuillotineBlockEntity> {

        static final Map<Block, Identifier> TEXTURE_MAP = Map.ofEntries(
            Map.entry(Ironhold.GUILLOTINE_OAK_BLOCK.get(),      tex("guillotine_oak")),
            Map.entry(Ironhold.GUILLOTINE_SPRUCE_BLOCK.get(),    tex("guillotine_spruce")),
            Map.entry(Ironhold.GUILLOTINE_BIRCH_BLOCK.get(),     tex("guillotine_birch")),
            Map.entry(Ironhold.GUILLOTINE_JUNGLE_BLOCK.get(),    tex("guillotine_jungle")),
            Map.entry(Ironhold.GUILLOTINE_ACACIA_BLOCK.get(),    tex("guillotine_acacia")),
            Map.entry(Ironhold.GUILLOTINE_DARK_OAK_BLOCK.get(),  tex("guillotine_dark_oak")),
            Map.entry(Ironhold.GUILLOTINE_MANGROVE_BLOCK.get(),  tex("guillotine_mangrove")),
            Map.entry(Ironhold.GUILLOTINE_CHERRY_BLOCK.get(),    tex("guillotine_cherry")),
            Map.entry(Ironhold.GUILLOTINE_CRIMSON_BLOCK.get(),   tex("guillotine_crimson")),
            Map.entry(Ironhold.GUILLOTINE_WARPED_BLOCK.get(),    tex("guillotine_warped")),
            Map.entry(Ironhold.GUILLOTINE_EBONY_BLOCK.get(),     tex("guillotine_ebony"))
        );

        static final Identifier DEFAULT_TEXTURE = tex("guillotine_oak");

        GuillotineGeoModel() {
            super(Identifier.fromNamespaceAndPath(Ironhold.MODID, "guillotine"));
        }

        @Override
        public Identifier getTextureResource(GeoRenderState renderState) {
            if (renderState.hasGeckolibData(GUILLOTINE_TEXTURE)) {
                return renderState.getGeckolibData(GUILLOTINE_TEXTURE);
            }
            return DEFAULT_TEXTURE;
        }

        private static Identifier tex(String name) {
            return Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/block/" + name + ".png");
        }
    }
}
