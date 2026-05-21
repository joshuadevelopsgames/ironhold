package kingdom.smp.mixin;

import kingdom.smp.client.ClientPayloads;
import kingdom.smp.client.ForgeButtonDebug;
import kingdom.smp.client.gui.ForgeHammerButton;
import kingdom.smp.net.ForgeHammerRequestPayload;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.world.inventory.AnvilMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a transparent "forge hammer" click target to the vanilla anvil screen.
 * Clicking it sends a {@link ForgeHammerRequestPayload} so the server can open
 * the blacksmithing forge minigame for the gear + repair material in the anvil.
 *
 * <p>Position/size come from {@link ForgeButtonDebug} (offsets from the panel
 * top-left) and are live-tunable with {@code /forgebutton}. The button is
 * rebuilt each time the screen opens. Injected at the tail of {@code subInit}
 * (where vanilla adds the rename field) so {@code leftPos}/{@code topPos} are
 * already set.
 */
@Mixin(AnvilScreen.class)
public abstract class AnvilHammerButtonMixin extends AbstractContainerScreen<AnvilMenu> {

    // Synthesized constructor — never invoked.
    private AnvilHammerButtonMixin() {
        super(null, null, null);
    }

    @Inject(method = "subInit", at = @At("TAIL"))
    private void ironhold$addHammerButton(CallbackInfo ci) {
        ForgeButtonDebug.originX = this.leftPos;
        ForgeButtonDebug.originY = this.topPos;

        ForgeHammerButton btn = new ForgeHammerButton(
                this.leftPos + ForgeButtonDebug.x,
                this.topPos + ForgeButtonDebug.y,
                ForgeButtonDebug.w,
                ForgeButtonDebug.h,
                b -> ClientPayloads.sendToServer(new ForgeHammerRequestPayload()));
        ForgeButtonDebug.active = btn;
        this.addRenderableWidget(btn);
    }
}
