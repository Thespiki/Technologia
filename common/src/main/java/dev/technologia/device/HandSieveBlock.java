package dev.technologia.device;

import com.mojang.serialization.MapCodec;
import dev.technologia.machine.FactoryShapes;
import dev.technologia.machine.MeshItem;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * The unpowered sieve. Put one block of gravel, sand or coarse dirt in it, then use it a few times
 * to shake out what the Auto Sieve would give. It takes the same meshes.
 */
public final class HandSieveBlock extends BaseEntityBlock {
    /** How much is left in the tray: 0 empty, {@link HandSieveBlockEntity#SHAKES} just filled. */
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, HandSieveBlockEntity.SHAKES);
    public HandSieveBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FILL, 0));
    }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FILL); }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return MapCodec.unit(this); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FactoryShapes.get("hand_sieve", Direction.NORTH);
    }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new HandSieveBlockEntity(pos, state); }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
            InteractionHand hand, BlockHitResult hit) {
        if (!(level.getBlockEntity(pos) instanceof HandSieveBlockEntity sieve)) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.getItem() instanceof MeshItem) {
            if (!level.isClientSide) MeshItem.install(stack, sieve.mesh(), sieve::installMesh, player, level, pos);
            return ItemInteractionResult.sidedSuccess(level.isClientSide);
        }
        if (!sieve.isEmpty() || stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (level.isClientSide) return ItemInteractionResult.SUCCESS;
        if (!sieve.fill(stack)) {
            player.displayClientMessage(Component.translatable("message.technologia.sieve_refused"), true);
            // The click ends here: a failed result would let the item be used on the sieve, placing a held block against it.
            return ItemInteractionResult.CONSUME;
        }
        if (!player.getAbilities().instabuild) stack.shrink(1);
        return ItemInteractionResult.CONSUME;
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof HandSieveBlockEntity sieve) {
            if (sieve.isEmpty()) player.displayClientMessage(Component.translatable("message.technologia.sieve_empty"), true);
            else sieve.shake();
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock()) && level.getBlockEntity(pos) instanceof HandSieveBlockEntity sieve) sieve.dropAll();
        super.onRemove(state, level, pos, newState, moving);
    }
}
