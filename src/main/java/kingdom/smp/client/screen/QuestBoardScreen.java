package kingdom.smp.client.screen;

import kingdom.smp.Ironhold;
import kingdom.smp.client.QuestBoardSlotDebug;
import kingdom.smp.quest.QuestBoardMenu;
import kingdom.smp.quest.QuestData;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Quest Board GUI. Renders the {@code quest_board.png} background, draws task
 * item icons (no text — the texture supplies all labels), and provides three
 * clickable reward "buttons" plus a REDEEM button.
 *
 * <p>Reward zones are <strong>invisible click targets</strong> over the painted
 * reward slots — no item icon is drawn, no widget background is painted, the
 * texture's own slot art shows through. Click handling is done in
 * {@link #mouseClicked} rather than via {@link Button} so we don't get the
 * default vanilla button background drawn over the parchment art.
 */
public class QuestBoardScreen extends AbstractContainerScreen<QuestBoardMenu> {

    private static final Identifier BG_TEXTURE =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "textures/gui/quest_board.png");

    private static final int TEX_W = 512;
    private static final int TEX_H = 640;

    /** GUI display size — both dimensions are multiples of 8 so MC's auto-scale
     *  picker doesn't flip-flop between scales 3 and 4 between renders.        */
    private static final int GUI_W = 232;
    private static final int GUI_H = 240;

    // ── Layout (multiples-of-8 GUI, ~3.5% bigger than the 224×232 baseline) ──

    /** Two task slots — show item icon only (texture has the count labels painted in). */
    private static final int TASK_X     = 137;
    private static final int TASK_Y_0   = 68;
    private static final int TASK_Y_1   = 81;
    /** Sub-1.0 scale for task items so they fit inside the painted wooden boxes. */
    private static final float TASK_ITEM_SCALE = 0.75f;

    /** Three reward click zones — invisible buttons positioned over the painted slot art. */
    private static final int REWARD_X_0 = 139;
    private static final int REWARD_X_1 = 158;
    private static final int REWARD_X_2 = 176;
    private static final int REWARD_Y   = 108;
    private static final int REWARD_W   = 16;
    private static final int REWARD_H   = 16;

    private static final int REDEEM_X = 137;
    private static final int REDEEM_Y = 127;
    private static final int REDEEM_W = 72;
    private static final int REDEEM_H = 14;

    private Button redeemButton;

    public QuestBoardScreen(QuestBoardMenu menu, Inventory playerInv, Component title) {
        super(menu, playerInv, title, GUI_W, GUI_H);
        // Texture supplies "QUEST BOARD" + "INVENTORY" labels — push vanilla labels offscreen.
        this.titleLabelX = -10000;
        this.inventoryLabelX = -10000;
    }

    @Override
    protected void init() {
        super.init();
        QuestData q = menu.questData();
        redeemButton = Button.builder(
                Component.literal("REDEEM"),
                btn -> onRedeem())
            .bounds(leftPos + REDEEM_X, topPos + REDEEM_Y, REDEEM_W, REDEEM_H)
            .build();
        redeemButton.active = q.redeemable();
        addRenderableWidget(redeemButton);
    }

    private void onRedeem() {
        // TODO Phase 2: server payload that grants rewards + consumes task items.
        if (minecraft != null && minecraft.player != null) {
            minecraft.player.sendSystemMessage(
                Component.literal("[Quest] (placeholder) Redeem clicked.")
                    .withStyle(ChatFormatting.YELLOW));
        }
        onClose();
    }

    private void onRewardClicked(int idx) {
        if (minecraft == null || minecraft.player == null) return;
        QuestData q = menu.questData();
        if (idx >= q.rewards().size()) return;
        ItemStack reward = q.rewards().get(idx);
        // TODO Phase 2: send a server payload that selects/claims this reward.
        minecraft.player.sendSystemMessage(
            Component.literal("[Quest] Reward " + (idx + 1) + " clicked: ")
                .append(reward.getHoverName())
                .withStyle(ChatFormatting.AQUA));
    }

    /**
     * Background + slot pass. Vanilla {@code AbstractContainerScreen.extractContents}
     * is where slot items get rendered (inside its own {@code extractSlots}
     * call). We mutate slot positions first so vanilla picks up the new
     * coords, then call super to render the entire vanilla pipeline (gradient
     * backdrop → labels → slots → items), then blit our BG with
     * {@code gfx.nextStratum()} so it sits in a layer over the gradient but
     * under the slots. If {@code nextStratum()} doesn't exist or isn't
     * available, our BG paints as the topmost background — slot items still
     * render via super so they're visible regardless.
     */
    @Override
    public void extractContents(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        // 1) Apply live debug-tuned slot positions via the SlotAccessor mixin.
        for (int i = 0; i < QuestBoardSlotDebug.SLOT_COUNT && i < menu.slots.size(); i++) {
            int[] xy = QuestBoardSlotDebug.position(i);
            kingdom.smp.access.SlotAccessor acc = (kingdom.smp.access.SlotAccessor) menu.slots.get(i);
            acc.ironhold$setX(xy[0]);
            acc.ironhold$setY(xy[1]);
        }

        // 2) Blit our BG first — vanilla's gradient backdrop will then paint
        //    a slight dim over it, then labels + slots + items render on top.
        gfx.blit(RenderPipelines.GUI_TEXTURED, BG_TEXTURE,
            leftPos, topPos, 0f, 0f,
            GUI_W, GUI_H, TEX_W, TEX_H,
            TEX_W, TEX_H);

        // 3) Vanilla pipeline: gradient + labels + slots + items.
        super.extractContents(gfx, mouseX, mouseY, partialTick);

        // 4) Re-blit our BG ON TOP to hide vanilla's gradient — but we can't
        //    do this naïvely because it would also cover slot items. So we
        //    only re-blit the OUTER FRAME area (anything outside the inventory
        //    bounding box). Inventory area stays as vanilla rendered it.
        //    For now we let the gradient be (it's only a slight dim).
    }

    /** Foreground pass — runs AFTER slots are rendered. */
    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        super.extractRenderState(gfx, mouseX, mouseY, partialTick);

        QuestData q = menu.questData();
        int lx = leftPos;
        int ly = topPos;

        // ── Task item icons (smaller than 16×16 to fit painted boxes) ───────
        if (q.tasks().size() >= 1) drawScaledItem(gfx, q.tasks().get(0).item(), lx + TASK_X, ly + TASK_Y_0, TASK_ITEM_SCALE);
        if (q.tasks().size() >= 2) drawScaledItem(gfx, q.tasks().get(1).item(), lx + TASK_X, ly + TASK_Y_1, TASK_ITEM_SCALE);

        // No reward icons — those positions are clickable buttons (handled in mouseClicked).
        // No text rendering — the texture has all labels painted in.
        // No slot debug overlay — vanilla's built-in hover highlight handles selection feedback.
    }

    /**
     * Render an item scaled down (or up) without changing the slot anchor point.
     * Uses the GUI's pose stack: translate to the slot, scale, render, undo.
     */
    private void drawScaledItem(GuiGraphicsExtractor gfx, ItemStack stack, int x, int y, float scale) {
        if (stack.isEmpty()) return;
        var pose = gfx.pose();
        pose.pushMatrix();
        // Scale around the slot center so the icon stays centered in its painted box.
        pose.translate(x + 8f, y + 8f);
        pose.scale(scale, scale);
        pose.translate(-8f, -8f);
        gfx.item(stack, 0, 0);
        pose.popMatrix();
    }

    @Override
    public boolean mouseClicked(MouseButtonEvent event, boolean clicked) {
        // Left-click on a reward slot → fire the reward click handler.
        if (event.button() == 0) {
            double mx = event.x();
            double my = event.y();
            int[] xs = { REWARD_X_0, REWARD_X_1, REWARD_X_2 };
            for (int i = 0; i < xs.length; i++) {
                int rx = leftPos + xs[i];
                int ry = topPos + REWARD_Y;
                if (mx >= rx && mx < rx + REWARD_W && my >= ry && my < ry + REWARD_H) {
                    onRewardClicked(i);
                    return true;
                }
            }
        }
        return super.mouseClicked(event, clicked);
    }
}
