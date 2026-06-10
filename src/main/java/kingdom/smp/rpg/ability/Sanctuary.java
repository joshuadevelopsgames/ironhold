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
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

/** <b>Sanctuary — Saint/Bishop.</b> Bless yourself + nearby allies with a short Regeneration pulse. */
public final class Sanctuary implements Ability {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Ironhold.MODID, "sanctuary");
    private static final double RANGE = 8.0;
    private static final int REGEN_TICKS = 5 * 20;

    @Override public Identifier id() { return ID; }
    @Override public int cooldownTicks() { return 25 * 20; }
    @Override public int unlockLevel() { return 1; }
    @Override public Set<PlayerClass> classes() { return Set.of(PlayerClass.SAINT, PlayerClass.BISHOP); }
    @Override public String translationKey() { return "ability.ironhold.sanctuary"; }

    @Override
    public boolean cast(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        for (ServerPlayer ally : HealSupport.alliesInRange(level, player, RANGE, true)) {
            ally.addEffect(new MobEffectInstance(MobEffects.REGENERATION, REGEN_TICKS, 1));
        }
        // Ground ring of blessing particles.
        for (int i = 0; i < 24; i++) {
            double ang = (Math.PI * 2 * i) / 24.0;
            level.sendParticles(ParticleTypes.HAPPY_VILLAGER,
                player.getX() + Math.cos(ang) * RANGE * 0.5, player.getY() + 0.1, player.getZ() + Math.sin(ang) * RANGE * 0.5,
                1, 0.0, 0.0, 0.0, 0.0);
        }
        level.playSound(null, player.blockPosition(), SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.6F, 1.4F);
        return true;
    }
}
