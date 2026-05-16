package kingdom.smp.mixin;

import kingdom.smp.Ironhold;
import kingdom.smp.client.WizardStickTransformDebug;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.model.cuboid.ItemTransform;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * After {@link ItemModelResolver#appendItemLayers} populates the render state
 * for the Wizard Stick, walks every active layer and replaces its
 * {@link ItemTransform} with the live values from
 * {@link WizardStickTransformDebug} for the current display context.
 *
 * <p>This is what lets {@code /wizardstickdebug} live-tune ALL display contexts
 * (third-person, GUI, ground, item frame, head) — the {@code RenderHandEvent}
 * approach only covers first-person.
 *
 * <p>Note: the model JSON's own {@code display} block still loads as the
 * starting transform, but it's overwritten here before the state is rendered.
 * Defaults in {@link WizardStickTransformDebug} are tuned to match the JSON
 * so the visual is unchanged until the player runs an in-game tweak.
 *
 * <p>Translation values are in 1/16-of-a-block units to match the model JSON
 * convention (so {@code /wsdebug print} output is paste-ready).
 */
@Mixin(ItemModelResolver.class)
public abstract class ItemModelResolverMixin {

    @Inject(
        method = "appendItemLayers",
        at = @At("RETURN")
    )
    private void ironhold$overrideWizardStickTransforms(
        ItemStackRenderState output, ItemStack item, ItemDisplayContext displayContext,
        @Nullable Level level, @Nullable ItemOwner owner, int seed,
        CallbackInfo ci
    ) {
        // Note: HALRIC_STAFF is NOT handled here. Its held-context rendering goes
        // through HalricStaffRenderer (GeckoLib) which reads HalricStaffTransformDebug
        // directly in adjustRenderPose. Overriding layer transforms here too would
        // double-apply the pose. Tuning still works because the renderer reads the
        // same live values; only the application site differs.
        if (!item.is(Ironhold.WIZARD_STICK.get())) return;

        String ctxKey = displayContextKey(displayContext);
        if (ctxKey == null) return;

        float rotX, rotY, rotZ, transX, transY, transZ, scale;
        WizardStickTransformDebug.Transform t = WizardStickTransformDebug.forContext(ctxKey);
        if (t == null) return;
        rotX = t.rotX; rotY = t.rotY; rotZ = t.rotZ;
        transX = t.transX; transY = t.transY; transZ = t.transZ;
        scale = t.scale;

        // Build an ItemTransform mirroring how vanilla parses one from JSON
        // (Deserializer multiplies translation by 0.0625 = 1/16 — we do the same).
        ItemTransform debug = new ItemTransform(
            new Vector3f(rotX, rotY, rotZ),
            new Vector3f(transX * 0.0625F, transY * 0.0625F, transZ * 0.0625F),
            new Vector3f(scale, scale, scale)
        );

        ItemStackRenderStateAccessor acc = (ItemStackRenderStateAccessor) output;
        ItemStackRenderState.LayerRenderState[] layers = acc.ironhold$getLayers();
        int active = acc.ironhold$getActiveLayerCount();
        for (int i = 0; i < active; i++) {
            layers[i].setItemTransform(debug);
        }

        // Suppress unused parameter warnings (level, owner, seed not needed here).
        if (level == null && owner == null && seed == -1) return;
    }

    private static String displayContextKey(ItemDisplayContext ctx) {
        return switch (ctx) {
            case THIRD_PERSON_RIGHT_HAND -> "thirdperson_righthand";
            case THIRD_PERSON_LEFT_HAND  -> "thirdperson_lefthand";
            case FIRST_PERSON_RIGHT_HAND -> "firstperson_righthand";
            case FIRST_PERSON_LEFT_HAND  -> "firstperson_lefthand";
            case HEAD                    -> "head";
            case GUI                     -> "gui";
            case GROUND                  -> "ground";
            case FIXED                   -> "fixed";
            default                      -> null;
        };
    }
}
