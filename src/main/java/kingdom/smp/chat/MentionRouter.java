package kingdom.smp.chat;

import kingdom.smp.ai.NpcChatPartner;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.neoforged.neoforge.event.ServerChatEvent;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses {@code @name} mentions out of server chat and routes them as private
 * messages — never public broadcasts. The first mention in the message decides
 * the target:
 * <ul>
 *   <li><b>@player</b> — a private whisper to that player (vanilla {@code /msg}
 *       style), plus a ping sound. Not shown in public chat.</li>
 *   <li><b>@npc</b> — a private whisper to that NPC's AI, which whispers a reply
 *       back. Continue the thread by mentioning the NPC again.</li>
 * </ul>
 *
 * <p>A name that belongs to <i>both</i> an online player and an NPC (Kangarude)
 * resolves to the player when they're online, and falls back to the NPC when
 * they're not — matching the synthetic tab-list precedence in
 * {@link kingdom.smp.entity.KangarudePlayerListSync}.
 *
 * <p>A mention that resolves to no one is left alone (sent as normal chat).
 */
public final class MentionRouter {

    private static final Pattern MENTION = Pattern.compile("@(\\w{1,16})");

    private MentionRouter() {}

    /**
     * @return true if a mention was found and routed privately (caller should not
     *         run any further chat routing); false if the message has no mention
     *         or the mention resolved to no one (let it broadcast normally).
     */
    public static boolean handle(ServerChatEvent event) {
        ServerPlayer sender = event.getPlayer();
        MinecraftServer server = sender.level().getServer();
        if (server == null) return false;

        String raw = event.getRawText();
        Matcher m = MENTION.matcher(raw);
        if (!m.find()) return false;

        // The first mention decides the target.
        String token = m.group(1);
        String body = stripMention(raw, token);

        ServerPlayer target = server.getPlayerList().getPlayerByName(token);
        if (target != null) {
            routeToPlayer(event, sender, target, body);
            return true;
        }

        NpcChatPartner npc = NpcMentionRegistry.resolveNearest(token, sender);
        if (npc != null && npc.supportsWhisper()) {
            routeToNpc(event, sender, npc, body);
            return true;
        }

        // Unresolved mention — let it through as a normal chat message.
        return false;
    }

    /** Drop the addressing "@token" prefix so only the message body remains. */
    private static String stripMention(String raw, String token) {
        String body = raw.replaceFirst("(?i)@" + Pattern.quote(token) + "\\s*", "").trim();
        return body.isBlank() ? raw.trim() : body;
    }

    private static void routeToPlayer(ServerChatEvent event, ServerPlayer sender,
                                      ServerPlayer target, String body) {
        event.setCanceled(true); // private — don't broadcast to public chat
        if (body.isBlank()) return;
        sender.sendSystemMessage(whisperToLine(target.getGameProfile().name(), body));
        target.sendSystemMessage(whisperFromLine(sender.getGameProfile().name(), body));
        ping(target);
    }

    private static void routeToNpc(ServerChatEvent event, ServerPlayer sender,
                                   NpcChatPartner npc, String body) {
        event.setCanceled(true); // private — don't broadcast to public chat
        sender.sendSystemMessage(whisperToLine(npc.displayName(), body));
        npc.onMentionTurn(sender, body);
    }

    /** "You whisper to <Name>: <body>" — echoed to the player who sent the mention. */
    public static Component whisperToLine(String name, String body) {
        return Component.literal("You whisper to " + name + ": " + body)
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    }

    /** "<Name> whispers to you: <line>" — the reply, sent to the recipient. */
    public static Component whisperFromLine(String name, String line) {
        return Component.literal(name + " whispers to you: " + line)
            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC);
    }

    /** Deliver an NPC's reply to a player as a private whisper. */
    public static void sendNpcWhisper(ServerPlayer player, String npcName, String line) {
        if (player == null || line == null || line.isBlank()) return;
        player.sendSystemMessage(whisperFromLine(npcName, line));
    }

    private static void ping(ServerPlayer target) {
        target.level().playSound(null, target.blockPosition(),
            SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 1.0F, 1.2F);
    }
}
