package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import kingdom.smp.item.BattleHammerItem;
import kingdom.smp.item.ForgeCharge;
import kingdom.smp.item.IronholdItemComponents;
import net.minecraft.world.item.ItemStack;
import com.geckolib.constant.dataticket.DataTicket;
import com.geckolib.renderer.GeoItemRenderer;
import com.geckolib.renderer.base.GeoRenderState;
import com.geckolib.renderer.base.GeoRenderer;
import com.geckolib.renderer.base.RenderPassInfo;
import com.geckolib.renderer.layer.builtin.AutoGlowingGeoLayer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;

/**
 * Forge-power glow on the Battle Hammer's inner ring. Subclasses AutoGlowingGeoLayer
 * so it reuses GeckoLib's aligned whole-model re-render (renderer.submitRenderTasks) —
 * hand-rolled bone rendering detaches from the in-hand transform, this does not. We
 * only change two things:
 *   - gate it on the forge-power crit combo (invisible at level 0), and
 *   - swap the render type to additive emissive so the glow reads as emitted light;
 *     brightness ramps through stages with the combo level (see {@link ForgeCharge}).
 */
public class BattleHammerForgeGlowLayer
        extends AutoGlowingGeoLayer<BattleHammerItem, GeoItemRenderer.RenderData, GeoRenderState> {

    private static final DataTicket<Float> CHARGE_FRAC =
        DataTicket.create("battle_hammer_charge_frac", Float.class);
    /** Trim material key (e.g. "emerald"); "" = untrimmed default forge orange. */
    private static final DataTicket<String> TRIM =
        DataTicket.create("battle_hammer_trim", String.class);

    public BattleHammerForgeGlowLayer(
            GeoRenderer<BattleHammerItem, GeoItemRenderer.RenderData, GeoRenderState> renderer) {
        super(renderer);
    }

    @Override
    public void addRenderData(BattleHammerItem animatable, GeoItemRenderer.RenderData data,
                              GeoRenderState renderState, float partialTick) {
        super.addRenderData(animatable, data, renderState, partialTick);
        // Glow brightness now reflects the stored forge-power crit combo (synced via the
        // FORGE_CHARGE component on the stack), not how long right-click is held.
        ItemStack stack = data.itemStack();
        ForgeCharge fc = stack.getOrDefault(IronholdItemComponents.FORGE_CHARGE.get(), ForgeCharge.NONE);
        float frac = Math.min(fc.level() / (float) BattleHammerItem.MAX_FORGE_CHARGE, 1f);
        renderState.addGeckolibData(CHARGE_FRAC, frac);
        // Trim material selects which tinted glow texture set to use.
        renderState.addGeckolibData(TRIM, stack.getOrDefault(IronholdItemComponents.FORGE_TRIM.get(), ""));
    }

    @Override
    public void submitRenderTask(RenderPassInfo<GeoRenderState> info, SubmitNodeCollector collector) {
        // Invisible at forge-charge level 0 (no combo built up).
        if (info.renderState().getOrDefaultGeckolibData(CHARGE_FRAC, 0f) <= 0.02f) return;
        super.submitRenderTask(info, collector);
    }

    // One glow texture per forge-charge level (battle_hammer_glow_1..8.png).
    private static final int STAGES = BattleHammerItem.MAX_FORGE_CHARGE;

    @Override
    protected RenderType getRenderType(GeoRenderState renderState) {
        // Additive "eyes" blend: the orange ADDS to whatever is behind it, so it reads
        // as emitted light (a glow) and is inherently see-through, rather than a flat decal.
        return RenderTypes.eyes(getTextureResource(renderState));
    }

    @Override
    protected Identifier getTextureResource(GeoRenderState renderState) {
        // Pick a glow texture by charge -> the glow ramps through brightness stages
        // (additive, so a brighter texture = a stronger glow) instead of nothing->full.
        float frac = Math.min(renderState.getOrDefaultGeckolibData(CHARGE_FRAC, 0f), 1f);
        int stage = Math.max(1, Math.min(STAGES, (int) Math.ceil(frac * STAGES)));
        // Trimmed hammers use a per-material tinted texture set; untrimmed uses forge orange.
        String trim = renderState.getOrDefaultGeckolibData(TRIM, "");
        String name = (trim == null || trim.isEmpty())
            ? "textures/item/battle_hammer_glow_" + stage + ".png"
            : "textures/item/battle_hammer_glow_" + trim + "_" + stage + ".png";
        return Identifier.fromNamespaceAndPath(Ironhold.MODID, name);
    }
}
