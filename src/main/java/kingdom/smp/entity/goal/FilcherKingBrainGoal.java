package kingdom.smp.entity.goal;

import kingdom.smp.ai.OllamaClient;
import kingdom.smp.entity.FilcherEntity;
import kingdom.smp.entity.FilcherRole;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.ai.goal.Goal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;

import javax.annotation.Nullable;
import java.util.*;

/**
 * LLM-powered strategic brain for the filcher pack king.
 *
 * <p>Every {@value #BRAIN_INTERVAL} ticks the king:
 * <ol>
 *   <li>Surveys nearby players and their visible inventory slots</li>
 *   <li>Surveys all nearby pack members, including their archetype + current role</li>
 *   <li>Sends a rich "director's brief" to the LLM describing each character</li>
 *   <li>Receives a heist plan: role assignments + target items + a royal decree whisper</li>
 *   <li>Broadcasts the decree to players (flavor) and applies role assignments to pack</li>
 * </ol>
 *
 * <p>The LLM is prompted as a <em>criminal director</em> who knows each filcher
 * as a distinct character — with desires, fears, and quirks — and must cast them
 * into roles that play to their strengths while staying true to who they are.
 */
public class FilcherKingBrainGoal extends Goal {

    // ─── Timing ──────────────────────────────────────────────────────────────

    /** Ticks between LLM strategy calls (600 = 30 seconds). */
    private static final int BRAIN_INTERVAL = 600;

    /** Scan radius for players and pack members. */
    private static final double SURVEY_RADIUS = 48.0;

    /** Radius for broadcasting the king's whispered decree to players. */
    private static final double WHISPER_RADIUS = 20.0;

    // ─── State ───────────────────────────────────────────────────────────────

    private final FilcherEntity king;
    private int cooldown = BRAIN_INTERVAL / 2; // first call at 15 sec
    private volatile boolean callInFlight = false;

    // ─── System prompt ───────────────────────────────────────────────────────

    private static final String SYSTEM_PROMPT = """
        You are the King of a filcher pack — a ruthless, brilliant criminal mastermind who runs \
        the most sophisticated pickpocket operation in the land. You command a crew of small rogue \
        creatures, each with their own name, personality, desires, and fears.

        Your job is to act as both a strategic director and a character who knows each of your \
        crew members intimately. When you assign roles, you are not just optimizing for efficiency — \
        you are directing actors. You give them the jobs that fit their character, that serve their \
        desires or test their fears, that make for a better story as well as a better heist.

        You speak in short, commanding language. You are confident, shrewd, and occasionally cruel.

        ROLES you may assign:
        - SCOUT   — orbit the target player at range, observe their inventory, report back
        - THIEF   — the primary pickpocket; waits for the distraction, then strikes
        - DISTRACTOR — gets in the player's face, draws attention from the thief
        - LOOKOUT — patrols the perimeter, warns on danger approach
        - CARRIER — takes stolen goods directly to the den without detour
        - GUARD   — shields the thief or carrier from player retaliation
        - IDLE    — stand down; no active role this cycle

        RESPONSE FORMAT — you MUST respond with valid JSON only, no markdown:
        {
          "spawnCount": 2,
          "assignments": [
            { "name": "Skrit", "role": "THIEF",      "targetItem": "diamond_sword" },
            { "name": "Nib",   "role": "DISTRACTOR", "targetItem": null },
            { "name": "Pock",  "role": "LOOKOUT",    "targetItem": null }
          ],
          "decree": "The king's whispered order — 1-2 sentences, dark theatrical flavor, max 120 chars"
        }

        RULES:
        - spawnCount: integer 0-10, how many new filchers to summon for this operation (0 = no reinforcements needed)
        - Only assign roles from the list above (exact string match, case-sensitive)
        - targetItem should be the Minecraft item ID (e.g. "diamond", "golden_sword") or null
        - Only one THIEF per cycle — choose the best candidate based on their character
        - Only one CARRIER per cycle — the most trustworthy or greedy member
        - The decree should feel like something a shadowy king would actually whisper
        - Do not explain your reasoning — only output the JSON
        """;

    // ─── Construction ────────────────────────────────────────────────────────

    public FilcherKingBrainGoal(FilcherEntity king) {
        this.king = king;
        setFlags(EnumSet.of(Flag.MOVE, Flag.LOOK));
    }

    // ─── Goal control ─────────────────────────────────────────────────────────

    @Override
    public boolean canUse() {
        return king.isKing() && king.isAlive() && !king.level().isClientSide()
            && OllamaClient.isConfigured();
    }

    @Override
    public boolean canContinueToUse() {
        return canUse();
    }

    @Override
    public void tick() {
        if (cooldown > 0) { cooldown--; return; }
        if (callInFlight) return;

        cooldown = BRAIN_INTERVAL;

        if (!(king.level() instanceof ServerLevel sl)) return;

        // Survey the world
        List<PlayerSnapshot> players   = surveyPlayers(sl);
        List<FilcherEntity>  packMembers = surveyPack(sl);

        // Need at least one player and one follower to bother
        if (players.isEmpty() || packMembers.isEmpty()) return;

        String userMessage = buildPrompt(players, packMembers);

        callInFlight = true;
        OllamaClient.requestRawText(
            SYSTEM_PROMPT,
            userMessage,
            "Kingdom SMP - Filcher King",
            rawText -> {
                callInFlight = false;
                if (rawText == null || rawText.isBlank()) return;

                kingdom.smp.Ironhold.LOGGER.info("[FilcherKing] Raw LLM response: {}", rawText);
                HeistPlan plan = parseResponse(rawText);
                if (plan == null) {
                    kingdom.smp.Ironhold.LOGGER.warn("[FilcherKing] Failed to parse heist plan from response.");
                    return;
                }

                // Apply on main thread
                sl.getServer().execute(() -> {
                    if (!king.isAlive()) return;
                    applyPlan(sl, packMembers, plan);
                });
            }
        );
    }

    // ─── Survey ──────────────────────────────────────────────────────────────

    /** Snapshots nearby players and their visible hotbar + armor. */
    private List<PlayerSnapshot> surveyPlayers(ServerLevel sl) {
        AABB box = new AABB(king.blockPosition()).inflate(SURVEY_RADIUS);
        List<PlayerSnapshot> result = new ArrayList<>();
        for (Player player : sl.getEntitiesOfClass(Player.class, box)) {
            if (!player.isAlive()) continue;
            List<String> visibleItems = new ArrayList<>();
            // Hotbar slots 0-8 + off-hand
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getItem(i);
                if (!stack.isEmpty()) visibleItems.add(formatItem(stack));
            }
            ItemStack offhand = player.getOffhandItem();
            if (!offhand.isEmpty()) visibleItems.add(formatItem(offhand));
            result.add(new PlayerSnapshot(
                player.getGameProfile().name(),
                (int) Math.round(player.getHealth()),
                visibleItems,
                (int) king.distanceTo(player)
            ));
        }
        return result;
    }

    /** Returns all living filchers within range that are NOT the king. */
    private List<FilcherEntity> surveyPack(ServerLevel sl) {
        AABB box = new AABB(king.blockPosition()).inflate(SURVEY_RADIUS);
        return sl.getEntitiesOfClass(FilcherEntity.class, box,
            f -> f != king && f.isAlive());
    }

    // ─── Prompt ──────────────────────────────────────────────────────────────

    private String buildPrompt(List<PlayerSnapshot> players, List<FilcherEntity> pack) {
        StringBuilder sb = new StringBuilder();

        sb.append("=== TARGETS ===\n");
        for (PlayerSnapshot p : players) {
            sb.append(String.format(
                "Player \"%s\" — %d HP, %d blocks away, carrying: %s\n",
                p.name, p.hp, p.distance,
                p.items.isEmpty() ? "nothing visible" : String.join(", ", p.items)
            ));
        }

        sb.append("\n=== YOUR CREW ===\n");
        sb.append("(Each member has a character. Cast them into roles that fit who they are.)\n");
        for (FilcherEntity f : pack) {
            sb.append(f.characterBrief()).append("\n");
        }

        sb.append("\n=== KING'S STATUS ===\n");
        sb.append(String.format(
            "You (King %s, %s) — HP: %d/%d, loot: %d\n",
            king.getFilcherName(),
            king.getArchetype().title,
            (int) Math.round(king.getHealth()),
            (int) Math.round(king.getMaxHealth()),
            king.getTotalLootValue()
        ));

        sb.append(String.format("\nYour current pack size (excluding you): %d filchers.\n", pack.size()));
        sb.append("Assign roles for this operation cycle. If you need more crew for the job, specify spawnCount (0-10). Cast wisely.");
        return sb.toString();
    }

    // ─── Parsing ─────────────────────────────────────────────────────────────

    @Nullable
    private HeistPlan parseResponse(String raw) {
        try {
            // Strip markdown code fences if present
            String json = raw.trim();
            if (json.startsWith("```")) {
                int start = json.indexOf('\n') + 1;
                int end   = json.lastIndexOf("```");
                if (start > 0 && end > start) json = json.substring(start, end).trim();
            }

            int spawnCount = 0;
            List<RoleAssignment> assignments = new ArrayList<>();
            String decree = "";

            // Parse "spawnCount"
            int scIdx = json.indexOf("\"spawnCount\"");
            if (scIdx >= 0) {
                int colon = json.indexOf(':', scIdx);
                if (colon >= 0) {
                    int numStart = colon + 1;
                    while (numStart < json.length() && Character.isWhitespace(json.charAt(numStart))) numStart++;
                    StringBuilder numBuf = new StringBuilder();
                    while (numStart < json.length() && (Character.isDigit(json.charAt(numStart)) || json.charAt(numStart) == '-')) {
                        numBuf.append(json.charAt(numStart++));
                    }
                    if (!numBuf.isEmpty()) {
                        int parsed = Integer.parseInt(numBuf.toString());
                        spawnCount = Math.max(0, Math.min(10, parsed)); // clamp 0-10
                    }
                }
            }

            // Parse "assignments" array
            int assStart = json.indexOf("\"assignments\"");
            if (assStart >= 0) {
                int arrOpen  = json.indexOf('[', assStart);
                int arrClose = json.indexOf(']', arrOpen);
                if (arrOpen >= 0 && arrClose > arrOpen) {
                    String arrStr = json.substring(arrOpen + 1, arrClose);
                    // Split on object boundaries
                    String[] objects = arrStr.split("\\},\\s*\\{");
                    for (String obj : objects) {
                        obj = obj.replace("{", "").replace("}", "").trim();
                        String name       = extractJsonString(obj, "name");
                        String roleStr    = extractJsonString(obj, "role");
                        String targetItem = extractJsonString(obj, "targetItem");
                        if (name == null || roleStr == null) continue;
                        try {
                            FilcherRole role = FilcherRole.valueOf(roleStr.toUpperCase());
                            assignments.add(new RoleAssignment(name, role,
                                "null".equalsIgnoreCase(targetItem) ? null : targetItem));
                        } catch (IllegalArgumentException ignored) {}
                    }
                }
            }

            // Parse "decree"
            String parsedDecree = extractJsonString(json, "decree");
            if (parsedDecree != null && !parsedDecree.isBlank()) decree = parsedDecree;

            if (assignments.isEmpty() && spawnCount == 0) return null;
            return new HeistPlan(spawnCount, assignments, decree);

        } catch (Exception e) {
            return null;
        }
    }

    /** Extracts a JSON string value by key — handles both "value" and null. */
    @Nullable
    private static String extractJsonString(String json, String key) {
        String search = "\"" + key + "\"";
        int idx = json.indexOf(search);
        if (idx < 0) return null;
        int colon = json.indexOf(':', idx);
        if (colon < 0) return null;
        int valueStart = colon + 1;
        while (valueStart < json.length() && Character.isWhitespace(json.charAt(valueStart))) valueStart++;
        if (valueStart >= json.length()) return null;
        if (json.charAt(valueStart) == '"') {
            int end = json.indexOf('"', valueStart + 1);
            return end > valueStart ? json.substring(valueStart + 1, end) : null;
        }
        // Check for null literal
        if (json.startsWith("null", valueStart)) return "null";
        return null;
    }

    // ─── Apply plan ──────────────────────────────────────────────────────────

    private void applyPlan(ServerLevel sl, List<FilcherEntity> pack, HeistPlan plan) {
        // Build name → entity map
        Map<String, FilcherEntity> byName = new HashMap<>();
        for (FilcherEntity f : pack) {
            if (f.isAlive()) byName.put(f.getFilcherName(), f);
        }

        // Apply role assignments
        for (RoleAssignment a : plan.assignments) {
            FilcherEntity member = byName.get(a.name);
            if (member == null) continue;
            member.setRole(a.role);
            member.setAssignedTargetItem(a.targetItem);
        }

        // Activate DISTRACTORs immediately — find the nearest player and set
        // them as swarm target so FilcherSwarmGoal fires on the next tick.
        Player nearestPlayer = sl.getNearestPlayer(king, SURVEY_RADIUS);
        if (nearestPlayer != null && !nearestPlayer.isCreative() && !nearestPlayer.isSpectator()) {
            for (RoleAssignment a : plan.assignments) {
                if (a.role != FilcherRole.DISTRACTOR) continue;
                FilcherEntity member = byName.get(a.name);
                if (member == null || !member.isAlive()) continue;
                // Swarm duration covers the full brain cycle so they keep
                // distracting until the next plan is issued (600 ticks = 30 s)
                member.setSwarmTarget(nearestPlayer, BRAIN_INTERVAL);
            }
        }

        // Spawn reinforcements if the king called for them
        if (plan.spawnCount > 0) {
            spawnReinforcements(sl, plan.spawnCount);
        }

        // Broadcast the decree to nearby players
        if (!plan.decree.isBlank()) {
            net.minecraft.network.chat.Component msg = net.minecraft.network.chat.Component.literal(
                "§6[Filcher King] §e\"" + plan.decree + "\"");
            AABB range = new AABB(king.blockPosition()).inflate(WHISPER_RADIUS);
            for (Player player : sl.getEntitiesOfClass(Player.class, range)) {
                player.sendSystemMessage(msg);
            }
        }

        // Log the plan
        StringBuilder log = new StringBuilder("[FilcherKing] New heist plan");
        if (plan.spawnCount > 0) log.append(" [+").append(plan.spawnCount).append(" recruits]");
        log.append(" — ");
        for (RoleAssignment a : plan.assignments) {
            log.append(a.name).append("→").append(a.role.shortCode());
            if (a.targetItem != null) log.append("(").append(a.targetItem).append(")");
            log.append(" ");
        }
        kingdom.smp.Ironhold.LOGGER.info(log.toString().trim());
    }

    /** Spawns the requested number of new filcher reinforcements near the king. */
    private void spawnReinforcements(ServerLevel sl, int count) {
        for (int i = 0; i < count; i++) {
            FilcherEntity recruit = (FilcherEntity) kingdom.smp.ModEntities.FILCHER.get().create(
                sl, net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED);
            if (recruit == null) continue;

            // Scatter around the king
            double angle = Math.random() * Math.PI * 2;
            double dist  = 2.0 + Math.random() * 4.0;
            recruit.setPos(
                king.getX() + Math.cos(angle) * dist,
                king.getY(),
                king.getZ() + Math.sin(angle) * dist);

            // Share the king's den with new recruits
            recruit.setDenPos(king.getDenPos());

            // Pass non-null data so this recruit doesn't spawn its own pack
            recruit.finalizeSpawn(sl, sl.getCurrentDifficultyAt(recruit.blockPosition()),
                net.minecraft.world.entity.EntitySpawnReason.MOB_SUMMONED,
                new net.minecraft.world.entity.SpawnGroupData() {});

            sl.addFreshEntity(recruit);

            // Small fanfare cloud
            sl.sendParticles(net.minecraft.core.particles.ParticleTypes.POOF,
                recruit.getX(), recruit.getY() + 0.5, recruit.getZ(),
                6, 0.3, 0.3, 0.3, 0.02);
        }
        kingdom.smp.Ironhold.LOGGER.info("[FilcherKing] Summoned {} reinforcements.", count);
    }

    // ─── Item formatting ─────────────────────────────────────────────────────

    private static String formatItem(ItemStack stack) {
        String id = net.minecraft.core.registries.BuiltInRegistries.ITEM
            .getKey(stack.getItem()).getPath();
        return stack.getCount() > 1 ? stack.getCount() + "x " + id : id;
    }

    // ─── Records ─────────────────────────────────────────────────────────────

    private record PlayerSnapshot(String name, int hp, List<String> items, int distance) {}
    private record RoleAssignment(String name, FilcherRole role, @Nullable String targetItem) {}
    private record HeistPlan(int spawnCount, List<RoleAssignment> assignments, String decree) {}
}
