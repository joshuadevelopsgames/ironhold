package kingdom.smp.item;

import kingdom.smp.entity.MagicMirrorEntity;
import kingdom.smp.entity.MirrorEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.Level;

/** Places a {@link MagicMirrorEntity} — the glowing-text variant — using the same auto-fit logic. */
public class MagicMirrorItem extends MirrorItem {
    public MagicMirrorItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    protected MirrorEntity createMirror(Level level, BlockPos pos, Direction face, int width, int height) {
        return new MagicMirrorEntity(level, pos, face, width, height);
    }
}
