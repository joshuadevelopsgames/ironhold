package kingdom.smp.block;

import kingdom.smp.Ironhold;
import kingdom.smp.entity.GuillotineSeatEntity;
import com.geckolib.animatable.GeoBlockEntity;
import com.geckolib.animatable.instance.AnimatableInstanceCache;
import com.geckolib.animatable.manager.AnimatableManager;
import com.geckolib.animation.AnimationController;
import com.geckolib.animation.RawAnimation;
import com.geckolib.animation.object.PlayState;
import com.geckolib.animation.state.AnimationTest;
import com.geckolib.util.GeckoLibUtil;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Block entity for the guillotine. Drives the GeckoLib blade animation and the
 * "drop the blade → behead whoever is locked in" mechanic.
 *
 * The blade is triggered (server-side) via {@link #release}, which fires the
 * {@code chop} animation and starts a short countdown. When the blade reaches
 * the lunette (IMPACT_TICK) any seated {@link GuillotineSeatEntity} rider is
 * decapitated. The blade re-arms automatically once the animation finishes.
 */
public class GuillotineBlockEntity extends BlockEntity implements GeoBlockEntity {

    private static final RawAnimation CHOP = RawAnimation.begin().thenPlay("animation.guillotine.chop");
    private static final RawAnimation ARMED = RawAnimation.begin().thenLoop("animation.guillotine.armed");

    /** Ticks after release when the blade reaches the neck (~0.4s into the slam). */
    private static final int IMPACT_TICK = 8;

    private final AnimatableInstanceCache cache = GeckoLibUtil.createInstanceCache(this);

    /** -1 = not falling. >= 0 counts ticks since the blade was released, until impact. */
    private int fallTick = -1;

    public GuillotineBlockEntity(BlockPos pos, BlockState state) {
        super(kingdom.smp.ModBlocks.GUILLOTINE_BLOCK_ENTITY.get(), pos, state);
    }

    /** True while the blade is mid-drop (so we don't re-trigger). */
    public boolean isFalling() {
        return fallTick >= 0;
    }

    /** Drop the blade. No-op if already falling. Server-side only. */
    public void release(ServerLevel level) {
        if (fallTick >= 0) {
            return;
        }
        fallTick = 0;
        // The blade's visual drop is driven by the CHOPPED blockstate (see bladePredicate),
        // which the block sets and syncs to clients. Here we just kick off the impact
        // countdown and the release sound.
        level.playSound(null, getBlockPos(), SoundEvents.PISTON_CONTRACT, SoundSource.BLOCKS, 1.1f, 0.55f);
        setChanged();
    }

    /** Server tick driver — registered from {@link GuillotineBlock#getTicker}. */
    public static void serverTick(ServerLevel level, BlockPos pos, BlockState state, GuillotineBlockEntity be) {
        if (be.fallTick < 0) {
            return;
        }
        be.fallTick++;
        if (be.fallTick >= IMPACT_TICK) {
            be.decapitate(level, pos);
            be.fallTick = -1; // blade stays down (CHOPPED) until someone resets it
        }
    }

    private void decapitate(ServerLevel level, BlockPos pos) {
        AABB area = new AABB(pos).inflate(1.0);
        List<GuillotineSeatEntity> seats = level.getEntitiesOfClass(GuillotineSeatEntity.class, area);
        boolean beheaded = false;

        for (GuillotineSeatEntity seat : seats) {
            for (Entity passenger : List.copyOf(seat.getPassengers())) {
                if (passenger instanceof LivingEntity victim) {
                    beheaded = true;
                    dropHead(level, victim);
                    victim.stopRiding();
                    // Guaranteed instant kill: kill() deals genericKill damage, which is
                    // tagged BYPASSES_INVULNERABILITY — ignores totems, armor, invuln ticks
                    // and creative mode. No one survives the guillotine.
                    victim.kill(level);
                }
            }
            seat.ejectPassengers();
            seat.discard();
        }

        // Gore + sound at the lunette regardless of whether anyone was caught (satisfying thunk).
        double nx = pos.getX() + 0.5;
        double ny = pos.getY() + 0.9;
        double nz = pos.getZ() + 0.5;
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.6f, 0.7f);
        if (beheaded) {
            level.playSound(null, pos, SoundEvents.PLAYER_ATTACK_CRIT, SoundSource.BLOCKS, 1.0f, 0.8f);
            DustParticleOptions blood = new DustParticleOptions(0x6E0B0B, 1.6f);
            level.sendParticles(blood, nx, ny, nz, 70, 0.28, 0.22, 0.28, 0.04);
            level.sendParticles(ParticleTypes.DAMAGE_INDICATOR, nx, ny, nz, 14, 0.25, 0.2, 0.25, 0.1);
        }
    }

    /** Drop the victim's head as an item above the block. */
    private void dropHead(ServerLevel level, LivingEntity victim) {
        ItemStack head;
        if (victim instanceof Player player) {
            head = new ItemStack(Items.PLAYER_HEAD);
            head.set(DataComponents.PROFILE, ResolvableProfile.createResolved(player.getGameProfile()));
        } else {
            head = mobHead(victim.getType());
        }
        if (!head.isEmpty()) {
            Block.popResource(level, getBlockPos().above(), head);
        }
    }

    private static ItemStack mobHead(EntityType<?> type) {
        if (type == EntityType.ZOMBIE)                 return new ItemStack(Items.ZOMBIE_HEAD);
        if (type == EntityType.SKELETON)               return new ItemStack(Items.SKELETON_SKULL);
        if (type == EntityType.WITHER_SKELETON)        return new ItemStack(Items.WITHER_SKELETON_SKULL);
        if (type == EntityType.CREEPER)                return new ItemStack(Items.CREEPER_HEAD);
        if (type == EntityType.PIGLIN)                 return new ItemStack(Items.PIGLIN_HEAD);
        if (type == EntityType.ENDER_DRAGON)           return new ItemStack(Items.DRAGON_HEAD);
        return ItemStack.EMPTY;
    }

    @Override
    public void registerControllers(AnimatableManager.ControllerRegistrar controllers) {
        controllers.add(new AnimationController<>("blade", 0, this::bladePredicate));
    }

    private PlayState bladePredicate(AnimationTest<GuillotineBlockEntity> test) {
        // Fully state-driven: while CHOPPED, play the chop (which slams to the base and
        // holds on its last frame), so the blade STAYS down until someone resets the
        // block. When CHOPPED clears, we stop → the blade snaps back to its default
        // (armed) pose — an instant teleport back up into position.
        BlockState st = getBlockState();
        boolean chopped = st.hasProperty(GuillotineBlock.CHOPPED) && st.getValue(GuillotineBlock.CHOPPED);
        // Always drive an animation (never STOP): a stopped GeckoLib controller freezes on
        // the held last frame, so the blade would stay down on reset. ARMED explicitly pins
        // the blade at position 0 (raised), making the reset an instant teleport back up.
        return test.setAndContinue(chopped ? CHOP : ARMED);
    }

    @Override
    public AnimatableInstanceCache getAnimatableInstanceCache() {
        return cache;
    }
}
