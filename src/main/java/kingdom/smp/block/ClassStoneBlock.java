package kingdom.smp.block;

import com.mojang.serialization.MapCodec;
import kingdom.smp.Ironhold;
import kingdom.smp.ModAttachments;
import kingdom.smp.net.OpenClassSelectionPayload;
import kingdom.smp.rpg.PlayerClass;
import kingdom.smp.rpg.PlayerKingdomRpgData;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.neoforged.neoforge.network.PacketDistributor;

/**
 * Class Stone — enchanting-table-shaped pedestal that opens the Tier 1 class
 * selection screen on right-click.
 *
 * <p>Hard gate: only players still on {@link PlayerClass#PEASANT} (i.e. they
 * have not yet picked a starter class) may use it. Anyone with a Tier 1+
 * class is rejected with a chat hint.
 *
 * <p>Visuals: the {@link ClassStoneBlockEntity} cycles through four hovering
 * items (sword / bow / arcane scepter / enchanted book) every ~3 seconds; the
 * {@code ClassStoneRenderer} draws the active item bobbing and rotating above
 * the slab the same way vanilla's enchanting table renders its book.
 */
public class ClassStoneBlock extends Block implements EntityBlock {

    public static final MapCodec<ClassStoneBlock> CODEC = simpleCodec(ClassStoneBlock::new);

    /** 12-tall slab matching the model and vanilla enchanting table. */
    private static final VoxelShape SHAPE = Block.box(0, 0, 0, 16, 12, 16);

    public ClassStoneBlock(Properties props) {
        super(props);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx) {
        return SHAPE;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new ClassStoneBlockEntity(pos, state);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        if (type != Ironhold.CLASS_STONE_BLOCK_ENTITY.get()) return null;
        // Only the client needs to tick — the carousel rotation/bob is purely visual.
        return level.isClientSide()
            ? (BlockEntityTicker<T>) (BlockEntityTicker<ClassStoneBlockEntity>) ClassStoneBlockEntity::clientTick
            : null;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos,
                                                Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;

        PlayerKingdomRpgData rpg = sp.getData(ModAttachments.PLAYER_RPG.get());
        PlayerClass current = rpg.playerClass();

        if (current != PlayerClass.PEASANT) {
            // Already picked a starter — reject.
            sp.sendSystemMessage(Component.literal("✗ The stone has no use to you. ")
                .withStyle(ChatFormatting.RED)
                .append(Component.literal("Your path is set: " + current.id() + ".")
                    .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC)));
            level.playSound(null, pos, SoundEvents.NOTE_BLOCK_BASS.value(), SoundSource.BLOCKS, 0.6f, 0.6f);
            return InteractionResult.FAIL;
        }

        // Open the Tier 1 selection screen client-side.
        PacketDistributor.sendToPlayer(sp, new OpenClassSelectionPayload());
        level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE,
            SoundSource.BLOCKS, 0.7f, 1.0f);
        return InteractionResult.SUCCESS;
    }
}
