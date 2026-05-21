package kingdom.smp.mixin;

import kingdom.smp.client.screen.SkillTreeScreen;
import kingdom.smp.inventory.IronholdInventoryLayout;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adds a vanilla-native "Skills" button to the survival inventory that opens the
 * {@link SkillTreeScreen}. The button is hidden while the recipe book is open
 * (the inventory shifts right and the button would otherwise overlap / run off-screen).
 */
@Mixin(InventoryScreen.class)
public abstract class InventoryScreenSkillButtonMixin extends Screen {

    private Button ironhold$skillsButton;

    protected InventoryScreenSkillButtonMixin(Component title) {
        super(title);
    }

    @Inject(method = "init", at = @At("TAIL"))
    private void ironhold$addSkillsButton(CallbackInfo ci) {
        AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
        int x = self.getGuiLeft() + IronholdInventoryLayout.SKILLS_BTN_X;
        int y = self.getGuiTop()  + IronholdInventoryLayout.SKILLS_BTN_Y;

        Screen returnTo = (Screen) (Object) this;
        ironhold$skillsButton = Button.builder(Component.literal("Skills"),
                        b -> Minecraft.getInstance().setScreen(new SkillTreeScreen(returnTo)))
                .bounds(x, y, IronholdInventoryLayout.SKILLS_BTN_W, IronholdInventoryLayout.SKILLS_BTN_H)
                .build();
        this.addRenderableWidget(ironhold$skillsButton);
    }

    /** Keep the Skills button positioned (it follows the up-shift / recipe-book move) and hide it
     *  while the recipe book is open. */
    @Inject(method = "extractBackground", at = @At("HEAD"))
    private void ironhold$toggleSkillsButton(GuiGraphicsExtractor gfx, int mouseX, int mouseY,
                                             float partialTick, CallbackInfo ci) {
        if (ironhold$skillsButton != null) {
            AbstractContainerScreen<?> self = (AbstractContainerScreen<?>) (Object) this;
            ironhold$skillsButton.setPosition(
                    self.getGuiLeft() + IronholdInventoryLayout.SKILLS_BTN_X,
                    self.getGuiTop()  + IronholdInventoryLayout.SKILLS_BTN_Y);
            ironhold$skillsButton.visible = !ironhold$recipeBookOpen();
        }
    }

    private boolean ironhold$recipeBookOpen() {
        if (!(this instanceof AbstractRecipeBookScreenAccessor acc)) return false;
        RecipeBookComponent<?> book = acc.ironhold$getRecipeBookComponent();
        return book != null && book.isVisible();
    }
}
