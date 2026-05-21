package kingdom.smp.tally;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kingdom.smp.Ironhold;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.storage.LevelResource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Ranks a player against everyone who has ever played on the server, across the
 * curated {@link TrackedStat} set, and reports the stat where they rank highest
 * (their "specialty"), plus a gold-coin reward scaled to how impressive that
 * standing is.
 *
 * <p>All comparison data comes from the per-player stat files in
 * {@code world/stats/}. The interacting player's live stats are flushed to disk
 * first so their numbers are current. This is I/O over (number of players who
 * have ever logged in) files — fine for a normal server, and only runs on an
 * explicit NPC interaction.
 */
public final class StatsRanking {
    private StatsRanking() {}

    /** Reward bounds (coins). */
    private static final int MIN_REWARD = 5;
    private static final int MAX_REWARD = 50;

    public record Result(TrackedStat stat, int rank, int population, long value, int reward) {
        public boolean isTop() { return rank == 1; }
    }

    /**
     * The player's standing: their best (reward-bearing) result, plus a terse
     * summary of every stat they place in, for the herald to riff on.
     */
    public record Ranking(Result best, String summary) {}

    /**
     * Compute the player's server-wide standing. Empty if the player has no
     * non-zero tracked stats yet (brand-new player) or stats can't be read.
     */
    public static Optional<Ranking> compute(ServerPlayer player) {
        MinecraftServer server = player.level().getServer();
        if (server == null) return Optional.empty();

        // Flush the interacting player's live stats so their file is current.
        player.getStats().save();

        Path dir = server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
        if (!Files.isDirectory(dir)) return Optional.empty();

        UUID myId = player.getUUID();
        Map<TrackedStat, Long> mine = null;
        List<Map<TrackedStat, Long>> population = new ArrayList<>();

        try (var stream = Files.list(dir)) {
            for (Path p : (Iterable<Path>) stream::iterator) {
                String name = p.getFileName().toString();
                if (!name.endsWith(".json")) continue;
                Map<TrackedStat, Long> vals = parseFile(p);
                if (vals == null) continue;
                population.add(vals);
                String uuidStr = name.substring(0, name.length() - ".json".length());
                if (uuidStr.equalsIgnoreCase(myId.toString())) mine = vals;
            }
        } catch (Exception e) {
            Ironhold.LOGGER.warn("[Tallykeeper] failed reading stats dir {}: {}", dir, e.toString());
            return Optional.empty();
        }

        if (mine == null) return Optional.empty();

        int pop = population.size();
        Result best = null;
        double bestScore = -1.0;
        StringBuilder summary = new StringBuilder();

        for (TrackedStat stat : TrackedStat.values()) {
            long myVal = mine.getOrDefault(stat, 0L);
            if (myVal <= 0) continue;

            int rank = 1;
            for (Map<TrackedStat, Long> other : population) {
                if (other == mine) continue;
                if (other.getOrDefault(stat, 0L) > myVal) rank++;
            }

            if (summary.length() > 0) summary.append("; ");
            summary.append(stat.displayName()).append(": rank ").append(rank)
                   .append(" of ").append(pop).append(" (").append(stat.format(myVal)).append(")");

            double score = stat.prestige() * rankScore(rank, pop);
            if (score > bestScore) {
                bestScore = score;
                best = new Result(stat, rank, pop, myVal, computeReward(score, pop, rank));
            }
        }

        if (best == null) return Optional.empty();
        return Optional.of(new Ranking(best, summary.toString()));
    }

    /** How impressive a given rank is, 0..1. */
    private static double rankScore(int rank, int pop) {
        if (rank == 1) return 1.0;
        if (rank == 2) return 0.75;
        if (rank == 3) return 0.6;
        double pct = (double) rank / Math.max(1, pop);
        if (pct <= 0.10) return 0.45;
        if (pct <= 0.25) return 0.30;
        if (pct <= 0.50) return 0.18;
        return 0.08;
    }

    /** Map an impressiveness score to a coin reward, with a small population bonus. */
    private static int computeReward(double score, int pop, int rank) {
        int playersBeaten = Math.max(0, pop - rank);
        int base = (int) Math.round(score * 30.0);
        int popBonus = (int) Math.round(Math.min(playersBeaten / 4.0, 10.0) * score);
        return Math.max(MIN_REWARD, Math.min(MAX_REWARD, base + popBonus));
    }

    /** Parse one stat file into summed tracked-stat values. Null on read/parse error. */
    private static Map<TrackedStat, Long> parseFile(Path p) {
        try {
            String content = Files.readString(p);
            JsonObject root = JsonParser.parseString(content).getAsJsonObject();
            EnumMap<TrackedStat, Long> out = new EnumMap<>(TrackedStat.class);
            if (!root.has("stats") || !root.get("stats").isJsonObject()) return out;
            JsonObject stats = root.getAsJsonObject("stats");

            for (TrackedStat stat : TrackedStat.values()) {
                long sum = 0;
                for (TrackedStat.StatKey k : stat.keys()) {
                    if (stats.has(k.category()) && stats.get(k.category()).isJsonObject()) {
                        JsonObject cat = stats.getAsJsonObject(k.category());
                        if (cat.has(k.entry()) && cat.get(k.entry()).isJsonPrimitive()) {
                            sum += cat.get(k.entry()).getAsLong();
                        }
                    }
                }
                if (sum > 0) out.put(stat, sum);
            }
            return out;
        } catch (Exception e) {
            return null;
        }
    }
}
