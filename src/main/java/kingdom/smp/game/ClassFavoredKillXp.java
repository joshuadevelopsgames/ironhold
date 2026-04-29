package kingdom.smp.game;

import kingdom.smp.ModAttachments;
import kingdom.smp.rpg.PlayerClass;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;

/**
 * Extra class XP when the victim matches {@link KingdomEntityTags} (datapack-tunable).
 * Skips PVP and major bosses so dragon/wither payouts stay fixed.
 */
public final class ClassFavoredKillXp {
    /** Numerator for “+30%” (integer math). */
    private static final int FAVORED_MULT_NUM = 13;
    private static final int FAVORED_MULT_DEN = 10;

    private ClassFavoredKillXp() {}

    public static int apply(int baseXp, ServerPlayer killer, LivingEntity victim) {
        if (baseXp <= 0 || victim instanceof ServerPlayer) {
            return baseXp;
        }
        if (isMajorBossVictim(victim)) {
            return baseXp;
        }
        PlayerClass kc = killer.getData(ModAttachments.PLAYER_RPG.get()).playerClass();
        EntityType<?> t = victim.getType();
        if (!matchesClassFavored(kc, t)) {
            return baseXp;
        }
        return Mth.clamp(Mth.ceil(baseXp * (double) FAVORED_MULT_NUM / (double) FAVORED_MULT_DEN), baseXp + 1, 500);
    }

    private static boolean isMajorBossVictim(LivingEntity victim) {
        return victim instanceof EnderDragon
            || victim instanceof WitherBoss
            || victim instanceof Warden
            || victim instanceof ElderGuardian;
    }

    private static boolean matchesClassFavored(PlayerClass kc, EntityType<?> t) {
        return switch (kc) {
            case RANGER -> t.builtInRegistryHolder().is(KingdomEntityTags.RANGER_FAVORED);
            case CLERIC -> t.builtInRegistryHolder().is(KingdomEntityTags.CLERIC_FAVORED);
            case WIZARD -> t.builtInRegistryHolder().is(KingdomEntityTags.WIZARD_FAVORED);
            case KNIGHT -> t.builtInRegistryHolder().is(KingdomEntityTags.KNIGHT_FAVORED);
            case PEASANT -> false;
            default -> false;
        };
    }
}
