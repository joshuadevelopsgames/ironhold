package kingdom.smp.rpg.ability;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import kingdom.smp.Ironhold;
import kingdom.smp.rpg.PlayerClass;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;

/** <b>Cleanse — Cleric/Bishop.</b> Strip all harmful effects from yourself + nearby allies. */
public final class Cleanse implements Ability {
    public static final Identifier ID = Identifier.fromNamespaceAndPath(Ironhold.MODID, "cleanse");
    private static final double RANGE = 8.0;

    @Override public Identifier id() { return ID; }
    @Override public int cooldownTicks() { return 18 * 20; }
    @Override public int unlockLevel() { return 1; }
    @Override public Set<PlayerClass> classes() { return Set.of(PlayerClass.CLERIC, PlayerClass.BISHOP); }
    @Override public String translationKey() { return "ability.ironhold.cleanse"; }

    @Override
    public boolean cast(ServerPlayer player) {
        ServerLevel level = (ServerLevel) player.level();
        boolean cleansedAny = false;
        for (ServerPlayer ally : HealSupport.alliesInRange(level, player, RANGE, true)) {
            List<Holder<MobEffect>> harmful = new ArrayList<>();
            for (MobEffectInstance inst : ally.getActiveEffects()) {
                if (inst.getEffect().value().getCategory() == MobEffectCategory.HARMFUL) {
                    harmful.add(inst.getEffect());
                }
            }
            for (Holder<MobEffect> h : harmful) {
                ally.removeEffect(h);
                cleansedAny = true;
            }
            if (!harmful.isEmpty()) {
                level.sendParticles(ParticleTypes.WAX_OFF, ally.getX(), ally.getY() + 1.2, ally.getZ(),
                    8, 0.3, 0.4, 0.3, 0.0);
            }
        }
        if (!cleansedAny) {
            return false;
        }
        level.playSound(null, player.blockPosition(), SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 0.6F, 1.5F);
        return true;
    }
}
