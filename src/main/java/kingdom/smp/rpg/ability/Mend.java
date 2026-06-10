package kingdom.smp.rpg.ability;

import java.util.Set;

import kingdom.smp.Ironhold;
import kingdom.smp.rpg.PlayerClass;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;

/** <b>Mend — Medic/Cleric.</b> Instantly heal the most-injured ally in range (or yourself). */
public final class Mend implements Ability {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Ironhold.MODID, "mend");
    private static final double RANGE = 10.0;
    private static final float HEAL = 8.0F;

    @Override public Identifier id() { return ID; }
    @Override public int cooldownTicks() { return 12 * 20; }
    @Override public int unlockLevel() { return 1; }
    @Override public Set<PlayerClass> classes() { return Set.of(PlayerClass.MEDIC, PlayerClass.CLERIC); }
    @Override public String translationKey() { return "ability.ironhold.mend"; }

    @Override
    public boolean cast(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        ServerPlayer target = player;
        float worst = player.getHealth() - player.getMaxHealth();
        for (ServerPlayer ally : HealSupport.alliesInRange(level, player, RANGE, false)) {
            float deficit = ally.getHealth() - ally.getMaxHealth();
            if (deficit < worst) {
                worst = deficit;
                target = ally;
            }
        }
        if (target.getHealth() >= target.getMaxHealth()) {
            return false; // nobody hurt → soft fail, no cooldown
        }
        target.heal(HEAL);
        level.sendParticles(ParticleTypes.HEART, target.getX(), target.getY() + 1.6, target.getZ(),
            6, 0.3, 0.3, 0.3, 0.0);
        level.playSound(null, player.blockPosition(), SoundEvents.PLAYER_LEVELUP, SoundSource.PLAYERS, 0.5F, 1.7F);
        return true;
    }
}
