package kingdom.smp.client;

import kingdom.smp.client.screen.VillagerDialogueScreen;
import kingdom.smp.entity.KingdomVillagerEntity;
import kingdom.smp.net.OpenVillagerScreenPayload;
import kingdom.smp.net.VillagerDialoguePayload;
import kingdom.smp.net.VillagerEmotePayload;
import net.minecraft.client.Minecraft;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-side cache for active villager dialogue and emotes.
 * Entries expire after a set duration (ticks). Keyed by entity ID.
 */
public final class VillagerDialogueCache {

    private VillagerDialogueCache() {}

    public record DialogueEntry(String villagerName, String profession, String dialogue, long expireTick) {}
    public record EmoteEntry(int emoteOrdinal, long expireTick) {}

    private static final Map<Integer, DialogueEntry> DIALOGUES = new ConcurrentHashMap<>();
    private static final Map<Integer, EmoteEntry> EMOTES = new ConcurrentHashMap<>();

    private static long clientTick = 0;

    /** Called each client tick to track time and clean up expired entries. */
    public static void tick() {
        clientTick++;
        if (clientTick % 20 == 0) { // cleanup every second
            DIALOGUES.entrySet().removeIf(e -> e.getValue().expireTick <= clientTick);
            EMOTES.entrySet().removeIf(e -> e.getValue().expireTick <= clientTick);
        }
    }

    public static void openDialogueScreen(OpenVillagerScreenPayload payload) {
        Minecraft.getInstance().setScreen(new VillagerDialogueScreen(
            payload.villagerName(),
            payload.profession(),
            payload.dialogue(),
            payload.decodeMood(),
            payload.entityId()));
    }

    public static void receiveDialogue(VillagerDialoguePayload payload) {
        DIALOGUES.put(payload.entityId(), new DialogueEntry(
            payload.villagerName(), payload.profession(), payload.dialogue(),
            clientTick + 160 // 8 seconds
        ));
    }

    public static void receiveEmote(VillagerEmotePayload payload) {
        EMOTES.put(payload.entityId(), new EmoteEntry(
            payload.emoteOrdinal(),
            clientTick + 60 // 3 seconds
        ));
    }

    /** Get active dialogue for an entity, or null if none/expired. */
    public static DialogueEntry getDialogue(int entityId) {
        DialogueEntry entry = DIALOGUES.get(entityId);
        if (entry != null && entry.expireTick > clientTick) return entry;
        return null;
    }

    /** Get active emote for an entity, or null if none/expired. */
    public static EmoteEntry getEmote(int entityId) {
        EmoteEntry entry = EMOTES.get(entityId);
        if (entry != null && entry.expireTick > clientTick) return entry;
        return null;
    }

    /** Convert emote ordinal back to EmoteType. */
    public static KingdomVillagerEntity.EmoteType emoteFromOrdinal(int ordinal) {
        KingdomVillagerEntity.EmoteType[] values = KingdomVillagerEntity.EmoteType.values();
        if (ordinal >= 0 && ordinal < values.length) return values[ordinal];
        return KingdomVillagerEntity.EmoteType.QUESTION;
    }

    /** Get icon character for an emote type (used in rendering). */
    public static String emoteIcon(KingdomVillagerEntity.EmoteType type) {
        return switch (type) {
            case HEART       -> "\u2764"; // heart
            case ANGER       -> "\uD83D\uDCA2"; // anger symbol
            case SWEAT       -> "\uD83D\uDCA7"; // droplet
            case MUSIC       -> "\u266B"; // music notes
            case EXCLAMATION -> "\u2757"; // exclamation
            case ZZZ         -> "Zzz";
            case SPARKLE     -> "\u2728"; // sparkles
            case QUESTION    -> "\u2753"; // question mark
        };
    }
}
