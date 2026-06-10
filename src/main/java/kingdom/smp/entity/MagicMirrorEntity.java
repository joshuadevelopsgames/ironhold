package kingdom.smp.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/**
 * A {@link MirrorEntity} that, on top of the live reflection, fades in glowing text on its surface —
 * for now the local player's stats, arranged around their reflection. Eventually this is where a new
 * player gets welcomed. All reflection behaviour is inherited; only the dropped item and the
 * client-side text overlay (see {@code MagicMirrorRenderer}) differ.
 */
public class MagicMirrorEntity extends MirrorEntity {
    /**
     * Client-only, transient: wall-clock millis when the stat text first began fading in (0 = not yet).
     * Lives on the entity so each placed magic mirror keeps its own fade clock and it is GC'd with the
     * entity. Never read or written server-side.
     */
    public long clientFadeStartMillis;

    public MagicMirrorEntity(EntityType<? extends MagicMirrorEntity> type, Level level) {
        super(type, level);
    }

    public MagicMirrorEntity(Level level, BlockPos blockPos, Direction direction, int width, int height) {
        super(kingdom.smp.ModEntities.MAGIC_MIRROR.get(), level, blockPos, direction, width, height);
    }

    @Override
    protected Item getDropItem() {
        return kingdom.smp.ModItems.MAGIC_MIRROR.get();
    }
}
