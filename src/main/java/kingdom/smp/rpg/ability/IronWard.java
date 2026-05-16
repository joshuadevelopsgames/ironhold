package kingdom.smp.rpg.ability;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.rpg.PlayerClass;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.boss.enderdragon.EnderDragon;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.entity.monster.warden.Warden;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * <b>Iron Word — Knight, slot C.</b>
 *
 * <p>Bellow a battle command that pulls all hostile mob attention to you and brands them for
 * bonus ally damage. Players are unaffected (mob-only taunt). Vanilla bosses get a shorter
 * forced-target window so the ability does not trivialize boss fights, but the +15% Marked
 * debuff still applies for the full duration.
 */
public final class IronWard implements Ability {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Ironhold.MODID, "iron_ward");

    /** Range in blocks for the AoE search. */
    public static final double RANGE = 12.0;

    /** Forced-target duration in ticks (6s × 20 ticks). */
    public static final int FORCED_TARGET_TICKS = 6 * 20;

    /** Forced-target duration on vanilla bosses (3s). */
    public static final int FORCED_TARGET_BOSS_TICKS = 3 * 20;

    /** Marked debuff duration in ticks (8s — 2s longer than the forced-target so allies finish kills). */
    public static final int MARK_TICKS = 8 * 20;

    /** Bonus damage multiplier when an ally hits a Marked mob. */
    public static final float MARK_DAMAGE_MULT = 1.15F;

    /** Tag of mobs that ignore the taunt (filled by datapack — tamed mob types, golems, kingdom NPCs). */
    public static final TagKey<EntityType<?>> TAUNT_IMMUNE_TAG =
        TagKey.create(net.minecraft.core.registries.Registries.ENTITY_TYPE,
            Identifier.fromNamespaceAndPath(Ironhold.MODID, "taunt_immune"));

    @Override public Identifier id() { return ID; }
    @Override public int cooldownTicks() { return 25 * 20; }
    @Override public int unlockLevel() { return 1; }
    @Override public Set<PlayerClass> classes() { return Set.of(PlayerClass.KNIGHT); }
    @Override public String translationKey() { return "ability.ironhold.iron_ward"; }

    @Override
    public boolean cast(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();
        Vec3 c = player.position();
        AABB box = new AABB(c.x - RANGE, c.y - RANGE, c.z - RANGE, c.x + RANGE, c.y + RANGE, c.z + RANGE);

        int kingdom = player.getData(ModAttachments.PLAYER_RPG.get()).kingdomIndexClamped();
        int affected = 0;

        for (Mob mob : level.getEntitiesOfClass(Mob.class, box, m -> m.isAlive())) {
            if (mob.distanceToSqr(player) > RANGE * RANGE) continue;
            if (!isHostile(mob)) continue;
            if (mob.getType().builtInRegistryHolder().is(TAUNT_IMMUNE_TAG)) continue;
            if (mob instanceof OwnableEntity) continue;

            mob.setTarget(player);
            mob.setLastHurtByMob(player); // helps AI prioritize even after the explicit target clears

            int targetTicks = isVanillaBoss(mob) ? FORCED_TARGET_BOSS_TICKS : FORCED_TARGET_TICKS;
            long markExpiry = now + MARK_TICKS;
            long forcedExpiry = now + targetTicks;
            mob.setData(ModAttachments.TAUNT_MARK.get(),
                new TauntMarkData(player.getUUID(), kingdom, markExpiry, forcedExpiry));

            affected++;
        }

        if (affected == 0) {
            return false;
        }

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.RAVAGER_ROAR, SoundSource.PLAYERS, 1.0F, 0.85F);

        // Expanding red shock-ring centred on the player.
        for (int i = 0; i < 32; i++) {
            double ang = (Math.PI * 2 * i) / 32.0;
            double dx = Math.cos(ang) * 1.2;
            double dz = Math.sin(ang) * 1.2;
            level.sendParticles(ParticleTypes.ANGRY_VILLAGER,
                player.getX() + dx, player.getY() + 0.1, player.getZ() + dz,
                1, 0.0, 0.0, 0.0, 0.0);
        }
        return true;
    }

    private static boolean isHostile(LivingEntity e) {
        // Anything monster-tagged plus catch-all for mobs that target players (skips passive animals/villagers).
        var holder = e.getType().builtInRegistryHolder();
        if (holder.is(EntityTypeTags.UNDEAD)
            || holder.is(EntityTypeTags.ARTHROPOD)
            || holder.is(EntityTypeTags.ILLAGER)) {
            return true;
        }
        return e instanceof Enemy;
    }

    private static boolean isVanillaBoss(LivingEntity e) {
        return e instanceof EnderDragon
            || e instanceof WitherBoss
            || e instanceof Warden
            || e instanceof ElderGuardian;
    }
}
