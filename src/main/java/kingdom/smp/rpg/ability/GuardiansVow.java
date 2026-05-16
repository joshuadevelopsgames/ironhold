package kingdom.smp.rpg.ability;

import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.rpg.PlayerClass;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.Set;

/**
 * <b>Guardian's Vow — Knight, slot V.</b>
 *
 * <p>Link to one ally (look-targeted, falls back to lowest-HP nearby kingdom-mate) and bleed
 * 50% of their incoming damage onto yourself. PVP mode (link established between two players
 * across kingdoms is impossible; only same-kingdom allies are valid targets) caps the redirect
 * at 35% to prevent unkillable duos. Auto-breaks if the Knight drops below 20% HP — see
 * {@link AbilityEffects#tickPlayerVow(ServerPlayer)}.
 */
public final class GuardiansVow implements Ability {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Ironhold.MODID, "guardians_vow");

    /** Range to find the initial target (8 blocks). */
    public static final double TARGET_RANGE = 8.0;

    /** Maximum link distance — beyond this the link breaks (12 blocks). */
    public static final double LINK_RANGE = 12.0;

    /** Vow duration in ticks (10s). */
    public static final int DURATION_TICKS = 10 * 20;

    /** Redirect fraction in PVE. */
    public static final float REDIRECT_PVE = 0.50F;

    /** Redirect fraction in PVP. (Kept for tuning; not currently used because allies are always same-kingdom.) */
    public static final float REDIRECT_PVP = 0.35F;

    /** Auto-break threshold — Knight HP fraction. */
    public static final float AUTOBREAK_HP_FRACTION = 0.20F;

    @Override public Identifier id() { return ID; }
    @Override public int cooldownTicks() { return 45 * 20; }
    @Override public int unlockLevel() { return 15; }
    @Override public Set<PlayerClass> classes() { return Set.of(PlayerClass.KNIGHT); }
    @Override public String translationKey() { return "ability.ironhold.guardians_vow"; }

    @Override
    public boolean cast(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        long now = level.getGameTime();

        Player target = pickTarget(player, level);
        if (target == null) {
            player.sendSystemMessage(Component.translatable("ability.ironhold.guardians_vow.no_target"));
            return false;
        }

        boolean pvpMode = false; // currently always same-kingdom; reserved for future kingdom-vs-kingdom revisions

        target.setData(ModAttachments.GUARDIAN_VOW.get(),
            new GuardianVowData(player.getUUID(), now + DURATION_TICKS, pvpMode));

        level.playSound(null, player.getX(), player.getY(), player.getZ(),
            SoundEvents.ARMOR_EQUIP_CHAIN.value(), SoundSource.PLAYERS, 0.8F, 1.4F);

        // Cast burst at the Knight + a small ring at the linked ally.
        level.sendParticles(ParticleTypes.ENCHANT,
            player.getX(), player.getY() + 1.2, player.getZ(), 14, 0.4, 0.6, 0.4, 0.0);
        level.sendParticles(ParticleTypes.END_ROD,
            target.getX(), target.getY() + 1.0, target.getZ(), 8, 0.3, 0.4, 0.3, 0.02);

        return true;
    }

    /**
     * Resolve the target ally:
     *  1) the player they are looking at within {@value TARGET_RANGE} blocks if same-kingdom,
     *  2) otherwise the nearest same-kingdom ally with the lowest HP-fraction.
     * Returns {@code null} if no valid target exists.
     */
    private static Player pickTarget(ServerPlayer caster, ServerLevel level) {
        int casterKingdom = caster.getData(ModAttachments.PLAYER_RPG.get()).kingdomIndexClamped();

        // 1) Crosshair-aimed ally
        HitResult hr = caster.pick(TARGET_RANGE, 1.0F, false);
        if (hr instanceof net.minecraft.world.phys.EntityHitResult ehr
            && ehr.getEntity() instanceof Player p
            && p != caster
            && p.isAlive()
            && sameKingdom(p, casterKingdom)) {
            return p;
        }

        // 2) Lowest-HP-fraction nearby same-kingdom ally
        Player best = null;
        float bestFrac = Float.MAX_VALUE;
        AABB box = caster.getBoundingBox().inflate(TARGET_RANGE);
        for (Player p : level.getEntitiesOfClass(Player.class, box)) {
            if (p == caster || !p.isAlive()) continue;
            if (!sameKingdom(p, casterKingdom)) continue;
            float frac = p.getHealth() / Math.max(0.01F, p.getMaxHealth());
            if (frac < bestFrac) {
                bestFrac = frac;
                best = p;
            }
        }
        return best;
    }

    private static boolean sameKingdom(Player p, int kingdom) {
        try {
            return p.getData(ModAttachments.PLAYER_RPG.get()).kingdomIndexClamped() == kingdom;
        } catch (Throwable t) {
            return false;
        }
    }

    /** Squared distance check for the ongoing link maintenance handler. */
    public static boolean inRange(LivingEntity a, LivingEntity b) {
        Vec3 ap = a.position();
        Vec3 bp = b.position();
        double dx = ap.x - bp.x;
        double dy = ap.y - bp.y;
        double dz = ap.z - bp.z;
        return (dx * dx + dy * dy + dz * dz) <= LINK_RANGE * LINK_RANGE;
    }
}
