package kingdom.smp.mixin;

import kingdom.smp.client.ClientMentionNames;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.multiplayer.PlayerInfo;
import org.lwjgl.glfw.GLFW;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Adds {@code @name} tab-completion to the chat input box. On TAB, if the word
 * the cursor sits in is an {@code @mention}, cycles through matching online
 * players and mention-able NPC names ({@link ClientMentionNames}).
 */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {

    @Shadow protected EditBox input;

    @Inject(method = "keyPressed", at = @At("HEAD"), cancellable = true)
    private void ironhold$mentionTabComplete(KeyEvent event, CallbackInfoReturnable<Boolean> cir) {
        if (event.key() != GLFW.GLFW_KEY_TAB || input == null) return;

        String text = input.getValue();
        int cursor = input.getCursorPosition();

        int start = cursor;
        while (start > 0 && isWordChar(text.charAt(start - 1))) start--;
        if (start == 0 || text.charAt(start - 1) != '@') return; // not inside a mention

        String typed = text.substring(start, cursor);
        List<String> matches = candidates().stream()
            .filter(n -> n.regionMatches(true, 0, typed, 0, typed.length()))
            .sorted(String.CASE_INSENSITIVE_ORDER)
            .toList();
        if (matches.isEmpty()) return;

        // Cycle: if the current token already matches one exactly, advance to the
        // next; otherwise complete to the first match.
        String current = text.substring(start, cursor);
        int next = 0;
        for (int i = 0; i < matches.size(); i++) {
            if (matches.get(i).equalsIgnoreCase(current)) {
                next = (i + 1) % matches.size();
                break;
            }
        }
        String chosen = matches.get(next);

        String replaced = text.substring(0, start) + chosen + text.substring(cursor);
        input.setValue(replaced);
        input.setCursorPosition(start + chosen.length());
        input.setHighlightPos(start + chosen.length());
        cir.setReturnValue(true);
    }

    private static boolean isWordChar(char c) {
        return Character.isLetterOrDigit(c) || c == '_';
    }

    private static Set<String> candidates() {
        Set<String> names = new LinkedHashSet<>();
        Minecraft mc = Minecraft.getInstance();
        if (mc.getConnection() != null) {
            for (PlayerInfo info : mc.getConnection().getOnlinePlayers()) {
                String n = info.getProfile().name();
                if (n != null && !n.isBlank()) names.add(n);
            }
        }
        // NPC names are synced lowercase; title-case the first letter for display.
        for (String n : ClientMentionNames.npcNames()) {
            if (n != null && !n.isBlank()) names.add(capitalize(n));
        }
        return names;
    }

    private static String capitalize(String s) {
        if (s.isEmpty()) return s;
        return Character.toUpperCase(s.charAt(0)) + s.substring(1).toLowerCase(Locale.ROOT);
    }
}
