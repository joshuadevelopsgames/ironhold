package kingdom.smp.client.entity;

import kingdom.smp.Ironhold;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.resources.model.EquipmentClientInfo;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.client.extensions.common.IClientItemExtensions;
import net.neoforged.neoforge.client.extensions.common.RegisterClientExtensionsEvent;

import java.util.function.Supplier;

/**
 * Registers per-item armor model overrides for knight helmets via NeoForge's
 * {@link IClientItemExtensions#getHumanoidArmorModel}. This is the same hook the
 * Epic Knights NeoForge port uses (see Magistu/Epic-Knights:1.21 →
 * neoforge/.../MedievalArmorItemNeoForge), and it remains the most reliable way
 * to swap in a custom armor model in MC 1.26 — the call lives in
 * {@code EquipmentLayerRenderer.renderLayers} at:
 *
 *     model = extensions.getGenericArmorModel(itemStack, layerType, model);
 *
 * which delegates to {@code getHumanoidArmorModel} for HumanoidModel replacement.
 *
 * Models are baked lazily on first use (no {@code EntityRendererProvider.Context}
 * needed — {@link net.minecraft.client.model.geom.builders.LayerDefinition#bakeRoot()}
 * is self-contained).
 */
public final class KnightArmorClientExtensions {

    private static HumanoidModel<HumanoidRenderState> kettlehat;
    private static HumanoidModel<HumanoidRenderState> bascinet;
    private static HumanoidModel<HumanoidRenderState> barbute;
    private static HumanoidModel<HumanoidRenderState> crusader;
    private static HumanoidModel<HumanoidRenderState> armet;
    private static HumanoidModel<HumanoidRenderState> maximilianHelmet;
    private static HumanoidModel<HumanoidRenderState> grandBascinet;
    private static HumanoidModel<HumanoidRenderState> stechhelm;
    private static HumanoidModel<HumanoidRenderState> sallet;

    private KnightArmorClientExtensions() {}

    public static void register(IEventBus modEventBus) {
        modEventBus.addListener(KnightArmorClientExtensions::onRegisterClientExtensions);
    }

    private static void onRegisterClientExtensions(RegisterClientExtensionsEvent event) {
        register(event, () -> kettlehat(),
            kingdom.smp.ModItems.KNIGHT_RECRUIT_HELM.get());
        register(event, () -> grandBascinet(),
            kingdom.smp.ModItems.KNIGHT_MAN_AT_ARMS_HELM.get());
        register(event, () -> maximilianHelmet(),
            kingdom.smp.ModItems.KNIGHT_ARMORED_HELM.get());
        register(event, () -> barbute(),
            kingdom.smp.ModItems.KNIGHT_CROSSBOWMAN_HELM.get());
        register(event, () -> crusader(),
            kingdom.smp.ModItems.KNIGHT_CRUSADER_HELM.get());
        register(event, () -> armet(),
            kingdom.smp.ModItems.KNIGHT_GOTHIC_HELM.get(),
            kingdom.smp.ModItems.KNIGHT_GOLD_HELM.get());
        register(event, () -> stechhelm(),
            kingdom.smp.ModItems.KNIGHT_JOUSTER_HELM.get());
        register(event, () -> sallet(),
            kingdom.smp.ModItems.KNIGHT_VETERAN_HELM.get());
    }

    private static void register(RegisterClientExtensionsEvent event,
                                 Supplier<HumanoidModel<HumanoidRenderState>> modelSupplier,
                                 Item... items) {
        event.registerItem(new IClientItemExtensions() {
            @Override
            public Model getHumanoidArmorModel(ItemStack stack, EquipmentClientInfo.LayerType layerType, Model original) {
                // Only swap the head/helmet layer. Leggings / overlays / babies pass through.
                if (layerType == EquipmentClientInfo.LayerType.HUMANOID) {
                    return modelSupplier.get();
                }
                return original;
            }
        }, items);
    }

    // Lazy bakes — first call creates the HumanoidModel from the LayerDefinition.
    // bakeRoot() does not require an EntityRendererProvider.Context.

    private static HumanoidModel<HumanoidRenderState> kettlehat() {
        if (kettlehat == null) kettlehat = new HumanoidModel<>(KnightArmorModelDefs.createKettlehat().bakeRoot());
        return kettlehat;
    }

    private static HumanoidModel<HumanoidRenderState> bascinet() {
        if (bascinet == null) bascinet = new HumanoidModel<>(KnightArmorModelDefs.createBascinet().bakeRoot());
        return bascinet;
    }

    private static HumanoidModel<HumanoidRenderState> barbute() {
        if (barbute == null) barbute = new HumanoidModel<>(KnightArmorModelDefs.createBarbute().bakeRoot());
        return barbute;
    }

    private static HumanoidModel<HumanoidRenderState> crusader() {
        if (crusader == null) crusader = new HumanoidModel<>(KnightArmorModelDefs.createCrusader().bakeRoot());
        return crusader;
    }

    private static HumanoidModel<HumanoidRenderState> armet() {
        if (armet == null) armet = new HumanoidModel<>(KnightArmorModelDefs.createArmet().bakeRoot());
        return armet;
    }

    private static HumanoidModel<HumanoidRenderState> maximilianHelmet() {
        if (maximilianHelmet == null) maximilianHelmet = new HumanoidModel<>(KnightArmorModelDefs.createMaximilianHelmet().bakeRoot());
        return maximilianHelmet;
    }

    private static HumanoidModel<HumanoidRenderState> grandBascinet() {
        if (grandBascinet == null) grandBascinet = new HumanoidModel<>(KnightArmorModelDefs.createGrandBascinet().bakeRoot());
        return grandBascinet;
    }

    private static HumanoidModel<HumanoidRenderState> stechhelm() {
        if (stechhelm == null) stechhelm = new HumanoidModel<>(KnightArmorModelDefs.createStechhelm().bakeRoot());
        return stechhelm;
    }

    private static HumanoidModel<HumanoidRenderState> sallet() {
        if (sallet == null) sallet = new HumanoidModel<>(KnightArmorModelDefs.createSallet().bakeRoot());
        return sallet;
    }
}
