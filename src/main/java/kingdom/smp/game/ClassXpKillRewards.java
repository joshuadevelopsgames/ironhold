package kingdom.smp.game;

import kingdom.smp.ModAttachments;
import kingdom.smp.rpg.PlayerClass;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDeathEvent;

/**
 * Class XP + kingdom pool from kills: bosses, monsters, minimal non-monsters, and PVP (class-based; Rogue + backstab).
 */
public final class ClassXpKillRewards {
    private ClassXpKillRewards() {}

    @SubscribeEvent
    public static void onLivingDeath(LivingDeathEvent event) {
        LivingEntity victim = event.getEntity();
        if (victim.level().isClientSide()) {
            return;
        }
        ServerPlayer killer = resolveKillerPlayer(event.getSource());
        if (killer == null || killer == victim) {
            return;
        }
        int xp = computeClassXp(victim, killer);
        if (xp <= 0) {
            return;
        }
        xp = ClassFavoredKillXp.apply(xp, killer, victim);
        RpgProgressionActions.grantClassXp(killer, xp);
    }

    private static ServerPlayer resolveKillerPlayer(DamageSource source) {
        Entity causing = source.getEntity();
        Entity direct = source.getDirectEntity();
        if (causing instanceof ServerPlayer sp) {
            return sp;
        }
        if (direct instanceof ServerPlayer sp2) {
            return sp2;
        }
        if (direct instanceof Projectile proj) {
            Entity owner = proj.getOwner();
            if (owner instanceof ServerPlayer sp3) {
                return sp3;
            }
        }
        if (causing instanceof Projectile proj2) {
            Entity owner = proj2.getOwner();
            if (owner instanceof ServerPlayer sp4) {
                return sp4;
            }
        }
        return null;
    }

    private static int computeClassXp(LivingEntity victim, ServerPlayer killer) {
        if (victim instanceof ArmorStand) {
            return 0;
        }
        EntityType<?> vt = victim.getType();
        if (vt == EntityType.VILLAGER || vt == EntityType.WANDERING_TRADER) {
            return 0;
        }
        if (victim instanceof ServerPlayer victimPlayer) {
            return pvpClassXp(killer, victimPlayer);
        }
        int boss = bossXp(victim);
        if (boss > 0) {
            return boss;
        }
        MobCategory cat = victim.getType().getCategory();
        if (cat == MobCategory.MONSTER) {
            int base = 1 + Mth.floor(victim.getMaxHealth() * 0.4f);
            return Mth.clamp(base, 2, 50);
        }
        return switch (cat) {
            case CREATURE -> 2;
            case WATER_CREATURE, WATER_AMBIENT, AMBIENT -> 1;
            default -> 1;
        };
    }

    private static int bossXp(LivingEntity victim) {
        if (victim instanceof EnderDragon) {
            return 500;
        }
        if (victim instanceof WitherBoss) {
            return 280;
        }
        if (victim instanceof Warden) {
            return 200;
        }
        if (victim instanceof ElderGuardian) {
            return 70;
        }
        return 0;
    }

    /**
     * Killer’s class sets baseline; victim’s level adds a little; Rogue gains a large bonus when striking from behind.
     */
    private static int pvpClassXp(ServerPlayer killer, ServerPlayer victim) {
        if (killer.isSpectator()) {
            return 0;
        }
        PlayerKingdomRpgData krpg = killer.getData(ModAttachments.PLAYER_RPG.get());
        PlayerKingdomRpgData vrpg = victim.getData(ModAttachments.PLAYER_RPG.get());
        PlayerClass kc = krpg.playerClass();
        int base = pvpBaseForKillerClass(kc);
        base += Mth.floor(vrpg.classLevel() / 3f);
        if (kc == PlayerClass.ROGUE && isKillerBehindVictim(victim, killer)) {
            base += 40;
        }
        return Mth.clamp(base, 2, 150);
    }

    private static int pvpBaseForKillerClass(PlayerClass kc) {
        return switch (kc) {
            case ROGUE -> 32;
            case KNIGHT -> 14;
            case RANGER -> 12;
            case WIZARD -> 9;
            case CLERIC -> 8;
            case PEASANT -> 5;
            default -> 10;
        };
    }

    /**
     * True if killer is roughly in the victim’s rear arc (horizontal), for backstab credit.
     */
    static boolean isKillerBehindVictim(LivingEntity victim, LivingEntity killer) {
        Vec3 look = victim.getLookAngle();
        Vec3 lookH = new Vec3(look.x, 0, look.z);
        if (lookH.lengthSqr() < 1.0E-6) {
            return false;
        }
        lookH = lookH.normalize();
        Vec3 toKiller = killer.position().subtract(victim.position());
        Vec3 toH = new Vec3(toKiller.x, 0, toKiller.z);
        if (toH.lengthSqr() < 1.0E-6) {
            return false;
        }
        toH = toH.normalize();
        double dot = lookH.dot(toH);
        return dot < -0.35;
    }
}
