package kingdom.smp.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.allay.Allay;
import net.minecraft.world.level.Level;

/**
 * Variant of the Will-o'-the-Wisp using a custom orb-and-tendrils model
 * (inspired by Terraria Pixie / fantasy wisp art). Behaves identically to
 * {@link WillOWispEntity} — same AI, same spawn-suppression — only the
 * visual model differs, so the two can be compared side by side.
 */
public class WillOWisp2Entity extends WillOWispEntity {

    public WillOWisp2Entity(EntityType<? extends Allay> type, Level level) {
        super(type, level);
    }
}
