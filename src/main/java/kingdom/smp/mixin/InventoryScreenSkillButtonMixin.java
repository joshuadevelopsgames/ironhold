package kingdom.smp.mixin;

import kingdom.smp.client.gui.InvisibleButton;
import kingdom.smp.client.screen.SkillTreeScreen;
import kingdom.smp.inventory.IronholdInventoryLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a transparent click target over the painted "SKILLS" panel. The panel
 * art lives in the texture (drawn by {@link InventoryScreenMixin}); this mixin
 * just routes clicks to {@link SkillTreeScreen}.
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenSkillButtonMixin extends Screen {

    protected InventoryScreenSkillButtonMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void ironhold$addSkillsClickZone(CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        int x = self.getGuiLeft() + IronholdInventoryLayout.SKILLS_PANEL_OFFSET_X;
        int y = self.getGuiTop()  + IronholdInventoryLayout.SKILLS_PANEL_OFFSET_Y;
        int w = IronholdInventoryLayout.SKILLS_PANEL_W;
        int h = IronholdInventoryLayout.SKILLS_PANEL_H;

        Screen returnTo = (Screen) (Object) this;
        InvisibleButton btn = new InvisibleButton(x, y, w, h,
                b -> Minecraft.getInstance().setScreen(new SkillTreeScreen(returnTo)));
        this.addRenderableWidget(btn);
    }
}
