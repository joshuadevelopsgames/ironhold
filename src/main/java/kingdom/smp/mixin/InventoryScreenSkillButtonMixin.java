package kingdom.smp.mixin;

import kingdom.smp.client.screen.SkillTreeScreen;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a "Skills" button to the survival inventory screen. Clicking opens
 * {@link SkillTreeScreen} with the current screen as the return-to, so closing the tree
 * returns to the inventory.
 *
 * Button placement: top-right of the inventory panel.
 *
 * Mixin extends Screen so {@code addRenderableWidget} (protected on Screen) is in scope —
 * the mixin code is merged into InventoryScreen at load time, and InventoryScreen already
 * extends Screen, so the synthesized constructor below is never actually invoked.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenSkillButtonMixin extends Screen {

    protected InventoryScreenSkillButtonMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void ironhold$addSkillTreeButton(CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        int x = self.getGuiLeft() + self.getXSize() + 4;
        int y = self.getGuiTop() + 4;

        Screen returnTo = (Screen) (Object) this;
        Button skillBtn = Button.builder(
                Component.literal("Skills"),
                b -> Minecraft.getInstance().setScreen(new SkillTreeScreen(returnTo))
        ).bounds(x, y, 60, 20).build();

        this.addRenderableWidget(skillBtn);
    }
}
