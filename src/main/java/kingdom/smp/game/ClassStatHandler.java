package kingdom.smp.game;

import kingdom.smp.Ironhold;
import kingdom.smp.rpg.PlayerClass;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import kingdom.smp.rpg.RpgProgression;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

/**
 * Combat / movement tweaks per class + scaling every 5 class levels (tiers).
 * Move speed matches class picker text (Knight slower, Ranger/Rogue faster); encumbrance adds its own penalty.
 */
public final class ClassStatHandler {
    private static final Identifier MAX_HEALTH_MOD =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "class_max_health");
    private static final Identifier ATTACK_MOD =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "class_attack_damage");
    private static final Identifier MOVE_MOD =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "class_movement_speed");
    private static final Identifier ATK_SPEED_MOD =
        Identifier.fromNamespaceAndPath(Ironhold.MODID, "class_attack_speed");

    /** Max-health attribute points per tier (2 = 1 heart); tier advances every 5 class levels. */
    private static final double TIER_MAX_HEALTH = 2.0;
    /** Attack damage added per tier. */
    private static final double TIER_ATTACK_DAMAGE = 0.25;
    /** Extra move speed ({@link AttributeModifier.Operation#ADD_MULTIPLIED_TOTAL}) per tier, all classes. */
    private static final double TIER_MOVE_MULT = 0.01;

    private ClassStatHandler() {}

    public static void apply(ServerPlayer player, PlayerKingdomRpgData rpg) {
        PlayerClass c = rpg.playerClass();
        int tier = RpgProgression.classTier(rpg.classLevel());
        var health = player.getAttribute(Attributes.MAX_HEALTH);
        var attack = player.getAttribute(Attributes.ATTACK_DAMAGE);
        var move = player.getAttribute(Attributes.MOVEMENT_SPEED);
        var atkSpeed = player.getAttribute(Attributes.ATTACK_SPEED);
        if (health != null) {
            health.removeModifier(MAX_HEALTH_MOD);
            double h = maxHealthBonus(c) + TIER_MAX_HEALTH * tier;
            if (h != 0.0) {
                health.addTransientModifier(
                    new AttributeModifier(MAX_HEALTH_MOD, h, AttributeModifier.Operation.ADD_VALUE));
            }
            player.setHealth(Math.min(player.getHealth(), player.getMaxHealth()));
        }
        if (attack != null) {
            attack.removeModifier(ATTACK_MOD);
            double a = attackBonus(c) + TIER_ATTACK_DAMAGE * tier;
            if (a != 0.0) {
                attack.addTransientModifier(
                    new AttributeModifier(ATTACK_MOD, a, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        if (move != null) {
            move.removeModifier(MOVE_MOD);
            double moveMult = movementSpeedMultBonus(c) + TIER_MOVE_MULT * tier;
            if (moveMult != 0.0) {
                move.addTransientModifier(
                    new AttributeModifier(MOVE_MOD, moveMult, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
        if (atkSpeed != null) {
            atkSpeed.removeModifier(ATK_SPEED_MOD);
            double asp = attackSpeedMultBonus(c);
            if (asp != 0.0) {
                atkSpeed.addTransientModifier(
                    new AttributeModifier(ATK_SPEED_MOD, asp, AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL));
            }
        }
    }

    /** Class picker: Knight −10%, Ranger +15%, Rogue +25%. */
    private static double movementSpeedMultBonus(PlayerClass c) {
        return switch (c) {
            case KNIGHT -> -0.10;
            case RANGER -> 0.15;
            case ROGUE -> 0.25;
            case PEASANT, WIZARD, CLERIC -> 0.0;
        };
    }

    /** Rogue-only +20% attack speed (class card). */
    private static double attackSpeedMultBonus(PlayerClass c) {
        return c == PlayerClass.ROGUE ? 0.20 : 0.0;
    }

    private static double maxHealthBonus(PlayerClass c) {
        return switch (c) {
            case KNIGHT -> 8.0;
            case CLERIC -> 6.0;
            case RANGER -> 4.0;
            case ROGUE, WIZARD -> 2.0;
            case PEASANT -> 0.0;
        };
    }

    private static double attackBonus(PlayerClass c) {
        return switch (c) {
            case KNIGHT -> 2.0;
            case ROGUE -> 1.5;
            case RANGER -> 1.0;
            case WIZARD, CLERIC, PEASANT -> 0.0;
        };
    }
}
