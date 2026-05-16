package kingdom.smp.entity.dragon;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * Stub. The full KingdomDragon was removed — this minimal {@link Mob} subclass
 * preserves the registration so existing world saves don't break, but has no
 * AI, no model, and no behavior. Re-implement when the dragon is rebuilt.
 */
public class KingdomDragonEntity extends Mob {

    public KingdomDragonEntity(EntityType<? extends KingdomDragonEntity> type, Level level) {
        super(type, level);
    }

    /** Referenced from {@code Ironhold} entity-attribute event. Vanilla defaults. */
    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 20.0)
                .add(Attributes.MOVEMENT_SPEED, 0.25);
    }
}
