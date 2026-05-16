package kingdom.smp.entity;

import com.geckolib.animatable.GeoEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.level.Level;

/**
 * Piglin Villager — a piglin-themed villager that spawns in/around bastions.
 *
 * <p>Behaves like a vanilla {@link Villager}: trades via the standard right-click
 * GUI, restocks daily, breeds, panics, picks up bread/wheat, can be cured if
 * zombified, gossips with other villagers.
 *
 * <p>Rendered with GeckoLib using
 * {@code geckolib/models/entity/piglin_villager.geo.json} (converted from the
 * Blockbench bbmodel — preserves head/ear/nose subparts and the 43°-tilted
 * "arms" praying-villager bone). Animations:
 * {@code geckolib/animations/entity/piglin_villager.animation.json}.
 */
public class PiglinVillagerEntity extends Villager implements GeoEntity {

    private static final RawAnimation IDLE_ANIM = RawAnimation.begin().thenLoop("idle");
    private static final RawAnimation WALK_ANIM = RawAnimation.begin().thenLoop("walk");

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    public PiglinVillagerEntity(EntityType<? extends Villager> type, Level level) {
        super(type, level);
    }

    /** Stay loaded — settled bastion residents, not patrol mobs. */
    @Override
    public boolean removeWhenFarAway(double distanceSquared) {
        return false;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<PiglinVillagerEntity>("main", 5, this::animationPredicate));
    }

    private PlayState animationPredicate(AnimationTest<PiglinVillagerEntity> test) {
        if (test.isMoving()) {
            test.controller().setAnimation(WALK_ANIM);
        } else {
            test.controller().setAnimation(IDLE_ANIM);
        }
        return PlayState.CONTINUE;
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }

    // ── Sound overrides — replace villager grunts with piglin oinks ──────────

    @Override
    protected SoundEvent getAmbientSound() {
        return SoundEvents.PIGLIN_AMBIENT;
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.PIGLIN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.PIGLIN_DEATH;
    }

    /** Trade offer clicked: PIGLIN_ADMIRING_ITEM for valid offers, PIGLIN_ANGRY for refused. */
    @Override
    protected SoundEvent getTradeUpdatedSound(boolean success) {
        return success ? SoundEvents.PIGLIN_ADMIRING_ITEM : SoundEvents.PIGLIN_ANGRY;
    }

    /** Played when a trade is completed (vanilla villager plays VILLAGER_YES here). */
    @Override
    public SoundEvent getNotifyTradeSound() {
        return SoundEvents.PIGLIN_ADMIRING_ITEM;
    }

    /** Played during the post-trade celebration (vanilla plays VILLAGER_CELEBRATE). */
    @Override
    public void playCelebrateSound() {
        this.playSound(SoundEvents.PIGLIN_CELEBRATE, this.getSoundVolume(), this.getVoicePitch());
    }

    /** Played when working at a job site (composter, anvil, etc.). */
    @Override
    public void playWorkSound() {
        this.playSound(SoundEvents.PIGLIN_AMBIENT, this.getSoundVolume(), this.getVoicePitch());
    }
}
