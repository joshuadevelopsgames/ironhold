package kingdom.smp.entity;

import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.PathfinderMob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

/**
 * An Ender Villager — a kingdom villager touched by the power of the End.
 * Same AI/dialogue/trading framework as KingdomVillagerEntity, but the profession
 * is permanently {@link VillagerProfession#ENDER} and the renderer pipeline is
 * different (uses {@code ender_villager.png} + an emissive eye-glow layer instead
 * of the vanilla profession-overlay layers).
 */
public class EnderVillagerEntity extends KingdomVillagerEntity {

    public EnderVillagerEntity(EntityType<? extends PathfinderMob> type, Level level) {
        super(type, level);
        setProfession(VillagerProfession.ENDER);
    }

    /** Slightly tougher than a regular kingdom villager — they ARE touched by the End. */
    public static AttributeSupplier.Builder createAttributes() {
        return PathfinderMob.createMobAttributes()
            .add(Attributes.MAX_HEALTH, 30.0)
            .add(Attributes.MOVEMENT_SPEED, 0.5)
            .add(Attributes.ATTACK_DAMAGE, 4.0)
            .add(Attributes.FOLLOW_RANGE, 32.0);
    }
}
