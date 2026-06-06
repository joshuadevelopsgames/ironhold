package kingdom.smp.quest;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;

/**
 * Transient per-player quest timer bars. Not persisted — rebuilt from {@link QuestSavedData}
 * on login (see {@link QuestEventHandlers}). One {@link ServerBossEvent} per active quest;
 * the bar drains as the deadline approaches and shifts green→yellow→red.
 */
public final class QuestBossBars {
    private QuestBossBars() {}

    /** playerUuid → (questId → bar). */
    private static final Map<UUID, Map<String, ServerBossEvent>> BARS = new HashMap<>();

    public static void start(ServerPlayer player, QuestDef def, QuestProgress progress, long now) {
        Map<String, ServerBossEvent> byQuest = BARS.computeIfAbsent(player.getUUID(), id -> new HashMap<>());
        ServerBossEvent bar = byQuest.get(def.id());
        if (bar == null) {
            bar = new ServerBossEvent(
                    UUID.randomUUID(),
                    Component.literal(def.title()),
                    BossEvent.BossBarColor.GREEN,
                    BossEvent.BossBarOverlay.NOTCHED_10);
            byQuest.put(def.id(), bar);
        }
        bar.addPlayer(player);
        update(player, def, progress, now);
    }

    /** Refresh one bar's progress/color/name for the current tick. */
    public static void update(ServerPlayer player, QuestDef def, QuestProgress progress, long now) {
        Map<String, ServerBossEvent> byQuest = BARS.get(player.getUUID());
        if (byQuest == null) return;
        ServerBossEvent bar = byQuest.get(def.id());
        if (bar == null) return;

        if (progress.status() == QuestStatus.COMPLETE) {
            bar.setColor(BossEvent.BossBarColor.BLUE);
            bar.setProgress(1.0f);
            bar.setName(Component.literal(def.title() + " — ready to redeem")
                    .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(0x55CCFF))));
            return;
        }

        long total = def.durationTicks();
        long remaining = Math.max(0, progress.deadlineTick() - now);
        float frac = total <= 0 ? 0f : Math.min(1f, (float) remaining / total);
        bar.setProgress(frac);
        bar.setColor(frac > 0.5f ? BossEvent.BossBarColor.GREEN
                : frac > 0.2f ? BossEvent.BossBarColor.YELLOW
                : BossEvent.BossBarColor.RED);
        bar.setName(Component.literal(def.title() + "  " + formatTime(remaining)));
    }

    public static void stop(ServerPlayer player, String questId) {
        Map<String, ServerBossEvent> byQuest = BARS.get(player.getUUID());
        if (byQuest == null) return;
        ServerBossEvent bar = byQuest.remove(questId);
        if (bar != null) bar.removeAllPlayers();
        if (byQuest.isEmpty()) BARS.remove(player.getUUID());
    }

    /** Drop every bar for a player (logout / disconnect). */
    public static void clear(ServerPlayer player) {
        Map<String, ServerBossEvent> byQuest = BARS.remove(player.getUUID());
        if (byQuest == null) return;
        for (Iterator<ServerBossEvent> it = byQuest.values().iterator(); it.hasNext();) {
            it.next().removeAllPlayers();
            it.remove();
        }
    }

    private static String formatTime(long ticks) {
        long secs = ticks / 20;
        return String.format("%d:%02d", secs / 60, secs % 60);
    }
}
