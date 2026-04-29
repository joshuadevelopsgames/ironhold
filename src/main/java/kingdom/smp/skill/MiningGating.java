package kingdom.smp.skill;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.HashMap;
import java.util.Map;

/**
 * Maps ore blocks to the minimum {@link ProfessionRank} of {@link Profession#MINING}
 * required to mine them. If a player tries to break a gated ore without the rank, the break
 * is cancelled and an action-bar message tells them what they need.
 *
 * Design note: this turns Mining from an optional bonus profession into a *required*
 * progression gate — you must spec Mining to mine iron, gold, diamond, etc. Coal, stone,
 * dirt, and other unlisted vanilla blocks remain mineable at any rank (default open).
 *
 * Tier mapping (cumulative point cost in parentheses):
 * <ul>
 *   <li><b>Default</b> — coal, copper, stone, dirt, sand, all vanilla non-listed blocks</li>
 *   <li><b>Novice</b> (1 pt) — iron</li>
 *   <li><b>Apprentice</b> (2 pt) — gold (overworld + nether), lapis, redstone</li>
 *   <li><b>Journeyman</b> (4 pt) — diamond, emerald</li>
 *   <li><b>Expert</b> (6 pt) — ancient debris (netherite)</li>
 *   <li><b>Master</b> (9 pt) — Veinbreaker (chain-mining)</li>
 * </ul>
 *
 * Design intent: gating creates real interdependence between players. A character who specs
 * into Mining becomes the kingdom's source of high-tier materials; characters who don't have
 * to trade for them. Some struggle is the feature, not a bug.
 */
public final class MiningGating {
    private MiningGating() {}

    private static final Map<Block, ProfessionRank> GATES = new HashMap<>();
    static {
        // Novice (1 pt) — iron only. Copper + coal stay free.
        register(ProfessionRank.NOVICE,
                Blocks.IRON_ORE, Blocks.DEEPSLATE_IRON_ORE, Blocks.RAW_IRON_BLOCK);

        // Apprentice (2 pt) — gold, lapis, redstone
        register(ProfessionRank.APPRENTICE,
                Blocks.GOLD_ORE, Blocks.DEEPSLATE_GOLD_ORE, Blocks.NETHER_GOLD_ORE, Blocks.RAW_GOLD_BLOCK,
                Blocks.LAPIS_ORE, Blocks.DEEPSLATE_LAPIS_ORE,
                Blocks.REDSTONE_ORE, Blocks.DEEPSLATE_REDSTONE_ORE);

        // Journeyman (4 pt) — diamond, emerald
        register(ProfessionRank.JOURNEYMAN,
                Blocks.DIAMOND_ORE, Blocks.DEEPSLATE_DIAMOND_ORE,
                Blocks.EMERALD_ORE, Blocks.DEEPSLATE_EMERALD_ORE);

        // Expert (6 pt) — netherite source
        register(ProfessionRank.EXPERT, Blocks.ANCIENT_DEBRIS);
    }

    private static void register(ProfessionRank required, Block... blocks) {
        for (Block b : blocks) GATES.put(b, required);
    }

    /** The minimum Mining rank required to break this block, or {@code null} if not gated. */
    public static ProfessionRank requiredRank(BlockState state) {
        return GATES.get(state.getBlock());
    }

    /** True if the player has the required Mining rank for this block (or it's not gated). */
    public static boolean canMine(Player player, BlockState state) {
        ProfessionRank required = requiredRank(state);
        if (required == null) return true;
        return SkillEffects.hasAtLeast(player, Profession.MINING, required);
    }
}
