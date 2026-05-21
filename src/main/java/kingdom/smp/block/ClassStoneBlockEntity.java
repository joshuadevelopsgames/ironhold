package kingdom.smp.block;

import kingdom.smp.Ironhold;
import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Block-entity state for {@link ClassStoneBlock}'s hovering item carousel.
 *
 * <p>Server-side this block entity does nothing — the carousel is a pure
 * client visual. Client-side it advances {@code time} every tick; the
 * renderer derives the active item index, fade alpha, and bob from there.
 *
 * <p>The four items rotated through correspond loosely to the four Tier 1
 * starters (Squire / Mage Apprentice / Archer / Medic):
 * <ol>
 *   <li>Iron Sword (Squire / Tank)</li>
 *   <li>Bow (Archer / Ranger)</li>
 *   <li>Arcane Scepter (Mage Apprentice / Mage)</li>
 *   <li>Enchanted Book (Medic / Support — magical scholar vibe)</li>
 * </ol>
 */
public class ClassStoneBlockEntity extends BlockEntity {

    /** How long each item sits front-and-center, in ticks (≈3 sec at 20tps). */
    public static final int TICKS_PER_SLOT = 60;

    /** Crossfade window (ticks) at the END of each slot's hold. */
    public static final int FADE_TICKS = 10;

    /** Total cycle = slots * TICKS_PER_SLOT. */
    public static final int CYCLE_TICKS = 4 * TICKS_PER_SLOT;

    /** Client-only tick counter. Wraps every CYCLE_TICKS. */
    public int time = 0;

    public ClassStoneBlockEntity(BlockPos pos, BlockState state) {
        super(kingdom.smp.ModBlocks.CLASS_STONE_BLOCK_ENTITY.get(), pos, state);
    }

    /** Lazily-built carousel — built on first access so item registry is ready. */
    private static volatile ItemStack[] CACHED_STACKS;

    /** Returns the four-item carousel, building it on first access. */
    public static ItemStack[] carousel() {
        ItemStack[] cached = CACHED_STACKS;
        if (cached != null) return cached;
        synchronized (ClassStoneBlockEntity.class) {
            if (CACHED_STACKS == null) {
                CACHED_STACKS = new ItemStack[] {
                    new ItemStack(Items.IRON_SWORD),
                    new ItemStack(Items.BOW),
                    new ItemStack(Ironhold.ARCANE_SCEPTER.get()),
                    new ItemStack(Items.ENCHANTED_BOOK)
                };
            }
            return CACHED_STACKS;
        }
    }

    public static void clientTick(Level level, BlockPos pos, BlockState state, ClassStoneBlockEntity be) {
        be.time = (be.time + 1) % CYCLE_TICKS;
    }
}
