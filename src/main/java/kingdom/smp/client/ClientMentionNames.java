package kingdom.smp.client;

import kingdom.smp.net.SyncMentionNamesPayload;

import java.util.Set;

/**
 * Client-side cache of mention-able NPC names, fed by
 * {@link SyncMentionNamesPayload}. Read by the chat-box tab-completer to suggest
 * {@code @npc} names alongside online players.
 */
public final class ClientMentionNames {

    private static volatile Set<String> NPC_NAMES = Set.of();

    private ClientMentionNames() {}

    public static void receive(SyncMentionNamesPayload payload) {
        NPC_NAMES = Set.copyOf(payload.names());
    }

    public static Set<String> npcNames() {
        return NPC_NAMES;
    }
}
