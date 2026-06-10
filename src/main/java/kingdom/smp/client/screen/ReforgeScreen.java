package kingdom.smp.client.screen;

import java.util.ArrayList;
import java.util.List;

import kingdom.smp.client.ClientPayloads;
import kingdom.smp.gear.Affix;
import kingdom.smp.gear.AffixData;
import kingdom.smp.gear.AffixInstance;
import kingdom.smp.net.ReforgeActionPayload;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.world.item.ItemStack;

/**
 * Blacksmith lock-and-reroll screen (spec 07 §5). Lists the main-hand gear's affixes, each with a
 * lock toggle (count gated by Blacksmithing rank, shipped in {@code OpenReforgePayload}); REROLL
 * sends {@code ReforgeActionPayload} with the lock bitmask and the server replies with a fresh
 * open payload, so the screen refreshes in place with the new affixes + escalated cost.
 *
 * <p>Drawn with plain fills — runtime look polish (parchment art like the quest board) can come
 * later without touching the flow.
 */
public class ReforgeScreen extends Screen {

    private static final int PANEL_W = 260;
    private static final int ROW_H = 22;

    private final int locksAllowed;
    private final int cost;

    private final List<AffixInstance> affixes = new ArrayList<>();
    private final List<Boolean> locked = new ArrayList<>();
    private final List<Button> lockButtons = new ArrayList<>();
    private Button rerollButton;
    private ItemStack gear = ItemStack.EMPTY;

    public ReforgeScreen(int locksAllowed, int cost) {
        super(Component.literal("Reforge"));
        this.locksAllowed = locksAllowed;
        this.cost = cost;
    }

    private int panelHeight() {
        return 96 + affixes.size() * ROW_H;
    }

    private int panelLeft() { return (width - PANEL_W) / 2; }
    private int panelTop()  { return (height - panelHeight()) / 2; }

    @Override
    protected void init() {
        super.init();
        affixes.clear();
        locked.clear();
        lockButtons.clear();
        if (minecraft != null && minecraft.player != null) {
            gear = minecraft.player.getMainHandItem();
            affixes.addAll(AffixData.get(gear));
        }
        for (int i = 0; i < affixes.size(); i++) locked.add(false);

        int left = panelLeft();
        int top = panelTop();
        for (int i = 0; i < affixes.size(); i++) {
            final int idx = i;
            Button b = Button.builder(lockLabel(i), btn -> toggleLock(idx))
                .bounds(left + PANEL_W - 70, top + 52 + i * ROW_H, 60, 16)
                .build();
            lockButtons.add(b);
            addRenderableWidget(b);
        }
        rerollButton = Button.builder(rerollLabel(), btn -> sendReroll())
            .bounds(left + 10, top + panelHeight() - 30, 150, 20)
            .build();
        rerollButton.active = !affixes.isEmpty() || !gear.isEmpty();
        addRenderableWidget(rerollButton);
        addRenderableWidget(Button.builder(Component.literal("Close"), btn -> onClose())
            .bounds(left + PANEL_W - 70, top + panelHeight() - 30, 60, 20)
            .build());
        refreshButtons();
    }

    private Component lockLabel(int i) {
        return locked.get(i)
            ? Component.literal("LOCKED").withStyle(Style.EMPTY.withBold(true))
            : Component.literal("Lock");
    }

    private Component rerollLabel() {
        return Component.literal("Reroll — " + cost + " coins");
    }

    private int locksUsed() {
        int n = 0;
        for (boolean b : locked) if (b) n++;
        return n;
    }

    private void toggleLock(int idx) {
        if (locked.get(idx)) {
            locked.set(idx, false);
        } else if (locksUsed() < locksAllowed) {
            locked.set(idx, true);
        }
        refreshButtons();
    }

    private void refreshButtons() {
        for (int i = 0; i < lockButtons.size(); i++) {
            Button b = lockButtons.get(i);
            b.setMessage(lockLabel(i));
            // Lockable only if already locked (to unlock) or a lock slot is free.
            b.active = locked.get(i) || locksUsed() < locksAllowed;
        }
    }

    private void sendReroll() {
        int mask = 0;
        for (int i = 0; i < locked.size(); i++) {
            if (locked.get(i)) mask |= (1 << i);
        }
        ClientPayloads.sendToServer(new ReforgeActionPayload(mask));
        // The server answers with a fresh OpenReforgePayload, which replaces this screen.
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor gfx, int mouseX, int mouseY, float partialTick) {
        gfx.fill(0, 0, width, height, 0xAA0A0A14);
        int left = panelLeft();
        int top = panelTop();
        int h = panelHeight();
        gfx.fill(left, top, left + PANEL_W, top + h, 0xF01A1410);
        gfx.fill(left, top, left + PANEL_W, top + 1, 0xFF8A6A3A);
        gfx.fill(left, top + h - 1, left + PANEL_W, top + h, 0xFF8A6A3A);
        gfx.fill(left, top, left + 1, top + h, 0xFF8A6A3A);
        gfx.fill(left + PANEL_W - 1, top, left + PANEL_W, top + h, 0xFF8A6A3A);

        gfx.centeredText(font, Component.literal("Reforge — Master Tobias")
            .withStyle(Style.EMPTY.withBold(true)), width / 2, top + 10, 0xFFFFDD55);

        if (!gear.isEmpty()) {
            gfx.item(gear, left + 10, top + 26);
            gfx.text(font, gear.getHoverName(), left + 32, top + 30, 0xFFEEEEEE, false);
        }
        gfx.text(font, "Locks: " + locksUsed() + "/" + locksAllowed + " (Blacksmithing)",
            left + 10, top + 52 - 12, 0xFF999999, false);

        if (affixes.isEmpty()) {
            gfx.text(font, "No affixes yet — reroll to first-roll this item.",
                left + 10, top + 56, 0xFFAAAAAA, false);
        }
        for (int i = 0; i < affixes.size(); i++) {
            AffixInstance inst = affixes.get(i);
            Affix a = inst.affix();
            String name = a != null ? a.displayName() : inst.id();
            String value = a != null && a.percent()
                ? Math.round(inst.roll() * 100) + "%"
                : String.format("%.1f", inst.roll());
            int color = a != null ? (0xFF000000 | a.color().getColor()) : 0xFFFFFFFF;
            gfx.text(font, name + "  " + value, left + 10, top + 56 + i * ROW_H, color, false);
        }

        super.extractRenderState(gfx, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}
