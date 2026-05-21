package kingdom.smp.block.wardheart;

import kingdom.smp.Ironhold;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.EntityJoinLevelEvent;

/**
 * Auto-spawns small permanent shields around naturally-generated dragon end
 * crystals. Triggers off EntityJoinLevelEvent — fires both for newly generated
 * crystals and for crystals already saved to disk when their chunk reloads.
 *
 * Filtering: only end crystals in {@link Level#END} that sit directly above
 * bedrock (the vanilla dragon-pillar pattern) qualify. Player-placed crystals
 * elsewhere — on obsidian for the dragon respawn, in the overworld for
 * decoration, etc. — are ignored.
 *
 * The spawned shield reuses the existing wardheart block and BE machinery,
 * but with HIDDEN=true (no block model rendered) and permanent=true (no fuel
 * decay, MICRO tier locked in, unbreakable).
 */
public final class EndCrystalShieldHandler {
    private EndCrystalShieldHandler() {}

    @SubscribeEvent
    public static void onEntityJoin(EntityJoinLevelEvent event) {
        if (!(event.getEntity() instanceof EndCrystal crystal)) return;
        if (!(event.getLevel() instanceof ServerLevel sl)) return;
        if (sl.dimension() != Level.END) return;
        if (!isDragonCrystal(sl, crystal)) return;

        BlockPos pos = crystal.blockPosition();
        // Only place if there's nothing there already — handles both first-spawn
        // and reload (where the wardheart block was saved to disk previously).
        BlockState existing = sl.getBlockState(pos);
        if (!existing.isAir()) return;

        BlockState shieldState = kingdom.smp.ModBlocks.WARDHEART_BLOCK.get().defaultBlockState()
            .setValue(WardheartBlock.HIDDEN, true);
        sl.setBlock(pos, shieldState, 3);

        if (sl.getBlockEntity(pos) instanceof WardheartBlockEntity be) {
            be.setPermanent(true);
            be.setChanged();
        }
    }

    private static boolean isDragonCrystal(ServerLevel sl, EndCrystal crystal) {
        if (!crystal.isAlive()) return false;
        // Dragon pillars are tall; player-placed crystals for the respawn
        // ritual sit on obsidian on the central island near Y=64 area.
        if (crystal.getY() < 50) return false;
        // The vanilla TheEndPodium / SpikeFeature places dragon crystals
        // directly above a bedrock block — that's the unique signature we
        // use to distinguish them from anything a player might place.
        BlockPos below = crystal.blockPosition().below();
        return sl.getBlockState(below).is(Blocks.BEDROCK);
    }
}
