package kingdom.smp.client.hud;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.client.IronholdKeys;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import kingdom.smp.rpg.ability.Ability;
import kingdom.smp.rpg.ability.AbilityCooldowns;
import kingdom.smp.rpg.ability.AbilityRegistry;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.HumanoidArm;

/**
 * HUD: ability icon row beside the hotbar.
 *
 * <p>Visibility / layout rules:
 * <ul>
 *   <li>Hidden entirely if the player has no abilities unlocked at their current class level.</li>
 *   <li>Only renders the slots that are unlocked — locked slots produce no icon and no gap.</li>
 *   <li>Default position: same side as the offhand slot. With offhand empty, icons sit flush
 *       against hotbar slot 1; with an offhand item, icons shift outward to clear the offhand.</li>
 *   <li>Mirrors automatically for left-handed players.</li>
 * </ul>
 */
public final class AbilityHud {

    /** Inner icon size — same as hotbar item icons. */
    private static final int ICON = 16;
    /** Slot frame size — matches vanilla 22×22 hotbar slot. */
    private static final int SLOT = 22;
    /** Inset of the icon inside the slot frame. */
    private static final int ICON_INSET = (SLOT - ICON) / 2;
    /** Gap between adjacent slot frames — 0 keeps them flush like the hotbar. */
    private static final int GAP = 0;
    /** Hotbar half-width (vanilla hotbar is 182 px, so 91 from center). */
    private static final int HOTBAR_HALF = 91;
    /** Width of the offhand slot when an offhand item is present. */
    private static final int OFFHAND_SLOT_WIDTH = 22;
    /** Cosmetic gap between the icon row and the hotbar / offhand boundary. */
    private static final int HOTBAR_GAP = 4;

    /** Vanilla sprite key for the right-handed offhand slot frame (offhand on left of hotbar). */
    private static final Identifier OFFHAND_SLOT_LEFT =
        Identifier.withDefaultNamespace("hud/hotbar_offhand_left");
    /** Vanilla sprite key for the left-handed offhand slot frame (offhand on right of hotbar). */
    private static final Identifier OFFHAND_SLOT_RIGHT =
        Identifier.withDefaultNamespace("hud/hotbar_offhand_right");

    private AbilityHud() {}

    public static void render(GuiGraphicsExtractor gfx, DeltaTracker delta) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (mc.screen != null) return;

        LocalPlayer player = mc.player;
        PlayerKingdomRpgData rpg;
        AbilityCooldowns cds;
        try {
            rpg = player.getData(ModAttachments.PLAYER_RPG.get());
            cds = player.getData(ModAttachments.ABILITY_COOLDOWNS.get());
        } catch (Throwable t) {
            return;
        }

        // Collect unlocked slots, preserving slot order so keybind labels stay correct.
        Ability[] kit = new Ability[AbilityRegistry.SLOT_COUNT];
        int unlockedCount = 0;
        for (int i = 0; i < AbilityRegistry.SLOT_COUNT; i++) {
            Ability a = AbilityRegistry.forSlot(rpg.playerClass(), i);
            if (a != null && rpg.classLevel() >= a.unlockLevel()) {
                kit[i] = a;
                unlockedCount++;
            }
        }
        if (unlockedCount == 0) {
            return; // Visibility rule: hide entirely when nothing is unlocked.
        }

        boolean offhandPresent = !player.getOffhandItem().isEmpty();
        boolean rightHanded = mc.options.mainHand().get() == HumanoidArm.RIGHT;
        Identifier slotFrame = rightHanded ? OFFHAND_SLOT_LEFT : OFFHAND_SLOT_RIGHT;

        int rowWidth = unlockedCount * SLOT + Math.max(0, unlockedCount - 1) * GAP;

        int centerX = gfx.guiWidth() / 2;
        int rowLeft;
        if (rightHanded) {
            // Default — offhand sits on the player's left, icons live there too.
            int rightEdge = centerX - HOTBAR_HALF - HOTBAR_GAP - (offhandPresent ? OFFHAND_SLOT_WIDTH : 0);
            rowLeft = rightEdge - rowWidth;
        } else {
            int leftEdge = centerX + HOTBAR_HALF + HOTBAR_GAP + (offhandPresent ? OFFHAND_SLOT_WIDTH : 0);
            rowLeft = leftEdge;
        }
        // Align slot top so the inner icon row matches the hotbar item icons (y = guiHeight - 19).
        int slotY = gfx.guiHeight() - 22 + 3 - ICON_INSET;

        long now = mc.level == null ? 0L : mc.level.getGameTime();
        Font font = mc.font;

        int x = rowLeft;
        int slotsRendered = 0;
        for (int i = 0; i < AbilityRegistry.SLOT_COUNT; i++) {
            Ability a = kit[i];
            if (a == null) continue;
            String key = a.id().toString();
            long remaining = cds.remainingTicks(key, now);
            boolean ready = remaining <= 0;
            renderSlot(gfx, font, x, slotY, slotFrame, a, i, ready, remaining);
            x += SLOT + GAP;
            slotsRendered++;
            if (slotsRendered >= unlockedCount) break;
        }
    }

    private static void renderSlot(GuiGraphicsExtractor gfx, Font font, int slotX, int slotY,
                                   Identifier slotFrame, Ability a,
                                   int slotIdx, boolean ready, long remainingTicks) {
        // 1) Slot frame — vanilla offhand-slot sprite, so the row visually matches the hotbar offhand cell.
        gfx.blitSprite(RenderPipelines.GUI_TEXTURED, slotFrame, slotX, slotY, SLOT, SLOT);

        int iconX = slotX + ICON_INSET;
        int iconY = slotY + ICON_INSET;

        // 2) Icon — assets/ironhold/textures/gui/ability/<id>.png. Source size is taken from the
        //    file's own dimensions where reasonable so non-16x16 art (e.g. a 32×29 shield crest)
        //    still renders into the 16×16 icon area scaled to fit.
        Identifier tex = Identifier.fromNamespaceAndPath(Ironhold.MODID,
            "textures/gui/ability/" + a.id().getPath() + ".png");
        gfx.blit(RenderPipelines.GUI_TEXTURED, tex,
            iconX, iconY,
            0f, 0f,
            ICON, ICON,        // dest size
            ICON, ICON,        // sub-region size
            ICON, ICON);       // source texture size; mismatch with native art is OK (linear scaled)

        // 3) Cooldown overlay — vertical fill from bottom + seconds-remaining number.
        if (!ready) {
            int total = a.cooldownTicks();
            float frac = Math.max(0f, Math.min(1f, remainingTicks / (float) total));
            int fillH = Math.round(ICON * frac);
            if (fillH > 0) {
                gfx.fill(iconX, iconY + (ICON - fillH), iconX + ICON, iconY + ICON, 0xA0000000);
            }
            int secs = (int) Math.ceil(remainingTicks / 20.0);
            String text = String.valueOf(secs);
            int tw = font.width(text);
            int tx = iconX + (ICON - tw) / 2;
            int ty = iconY + (ICON - font.lineHeight) / 2 + 1;
            gfx.text(font, text, tx + 1, ty + 1, 0xFF000000, false);
            gfx.text(font, text, tx, ty, 0xFFFFFFFF, false);
        }

        // 4) Keybind letter — bottom-right of the slot frame.
        KeyMapping km = IronholdKeys.ABILITIES[slotIdx];
        String letter = shortKeyLabel(km);
        if (letter != null) {
            int lx = slotX + SLOT - font.width(letter) - 2;
            int ly = slotY + SLOT - font.lineHeight - 1;
            gfx.text(font, letter, lx + 1, ly + 1, 0xFF000000, false);
            gfx.text(font, letter, lx, ly, ready ? 0xFFFFE680 : 0xFF888888, false);
        }
    }

    /** Returns the shortest readable label for the bound key (single letters preferred). */
    private static String shortKeyLabel(KeyMapping km) {
        try {
            String full = km.getTranslatedKeyMessage().getString();
            if (full == null || full.isEmpty()) return null;
            // Strip "key.keyboard." prefix if it leaks through, take last segment
            String s = full;
            int dot = s.lastIndexOf('.');
            if (dot >= 0 && dot < s.length() - 1) s = s.substring(dot + 1);
            // For multi-word keys like "Caps Lock", take initials
            if (s.length() > 3) {
                String[] parts = s.split("\\s+");
                if (parts.length > 1) {
                    StringBuilder sb = new StringBuilder();
                    for (String p : parts) if (!p.isEmpty()) sb.append(Character.toUpperCase(p.charAt(0)));
                    return sb.length() > 3 ? sb.substring(0, 3) : sb.toString();
                }
                return s.substring(0, 3).toUpperCase();
            }
            return s.length() == 1 ? s.toUpperCase() : s;
        } catch (Throwable t) {
            return null;
        }
    }
}
