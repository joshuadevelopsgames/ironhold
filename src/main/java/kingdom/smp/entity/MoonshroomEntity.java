package kingdom.smp.entity;

import kingdom.smp.ModBlocks;
import kingdom.smp.ModEntities;
import kingdom.smp.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Moonshroom — light-blue lunar cousin of the Mooshroom. Behaves exactly like a
 * vanilla mushroom cow (mushroom-stew bowl, shearing) but grazes the moon's dust
 * seas and is recoloured from red to the same soft blue as the {@link ShroomlingEntity}.
 * Its appearance (body + back mushrooms) is fully overridden client-side by
 * {@code MoonshroomRenderer}, so the inherited RED/BROWN variant never shows.
 */
public class MoonshroomEntity extends MushroomCow {

    public MoonshroomEntity(EntityType<? extends MushroomCow> type, Level level) {
        super(type, level);
    }

    /** Breed moonshrooms true — a calf is another moonshroom, not a vanilla mooshroom. */
    @Override
    @Nullable
    public MushroomCow getBreedOffspring(ServerLevel level, AgeableMob partner) {
        return ModEntities.MOONSHROOM.get().create(level, EntitySpawnReason.BREEDING);
    }

    /**
     * Bowl → Moonshroom Stew. Mirrors the vanilla mooshroom's bowl interaction but fills the bowl
     * with {@link ModItems#MOONSHROOM_STEW} (which steeps the drinker in Lunar Levity) instead of
     * plain mushroom stew. Every other interaction — shearing, breeding, flower-feeding — falls
     * through to {@link MushroomCow}.
     */
    @Override
    public InteractionResult mobInteract(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        if (held.is(Items.BOWL) && !this.isBaby()) {
            ItemStack stew = new ItemStack(ModItems.MOONSHROOM_STEW.get());
            player.setItemInHand(hand, ItemUtils.createFilledResult(held, player, stew, false));
            this.playSound(SoundEvents.MOOSHROOM_MILK, 1.0F, 1.0F);
            return InteractionResult.SUCCESS;
        }
        return super.mobInteract(player, hand);
    }

    /** Number of glowshrooms harvested when a moonshroom is sheared (matches the vanilla mooshroom yield of 5). */
    private static final int GLOWSHROOM_SHEAR_DROPS = 5;

    /**
     * Lunar shearing: like a mooshroom, shearing converts the moonshroom into a cow, but instead of
     * dropping the vanilla red/brown mushrooms it harvests {@link ModItems#GLOWSHROOM}. Mirrors the
     * vanilla {@code MushroomCow#shear} flow (conversion event hooks + explosion particle) so other
     * behaviour stays identical.
     */
    @Override
    public void shear(ServerLevel level, SoundSource soundSource, ItemStack tool) {
        level.playSound(null, this, SoundEvents.MOOSHROOM_SHEAR, soundSource, 1.0F, 1.0F);
        if (!net.neoforged.neoforge.event.EventHooks.canLivingConvert(this, EntityType.COW, timer -> {})) {
            return;
        }
        this.convertTo(EntityType.COW, ConversionParams.single(this, false, false), cow -> {
            net.neoforged.neoforge.event.EventHooks.onLivingConvert(this, cow);
            level.sendParticles(ParticleTypes.EXPLOSION, this.getX(), this.getY(0.5), this.getZ(), 1, 0.0, 0.0, 0.0, 0.0);
            for (int i = 0; i < GLOWSHROOM_SHEAR_DROPS; i++) {
                ItemEntity item = this.spawnAtLocation(level, new ItemStack(ModItems.GLOWSHROOM.get()), this.getBbHeight());
                if (item != null) {
                    item.setNoPickUpDelay();
                }
            }
        });
    }

    /** Moon creature: spawns on lunar regolith (moon stone / moon dust) at any light level. */
    public static boolean checkMoonshroomSpawnRules(EntityType<MoonshroomEntity> type, ServerLevelAccessor level,
                                                    EntitySpawnReason reason, BlockPos pos, RandomSource random) {
        if (reason == EntitySpawnReason.SPAWNER) return true;
        BlockState below = level.getBlockState(pos.below());
        return below.is(ModBlocks.MOON_STONE.get()) || below.is(ModBlocks.MOON_DUST.get());
    }
}
