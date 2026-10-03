package dev.technologia.logistics;

import com.mojang.serialization.MapCodec;
import dev.technologia.Technologia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.phys.BlockHitResult;

/** Arrow points at the destination; the opposite face extracts from the source. */
public final class ItemTransferBlock extends BaseEntityBlock {
    public static final DirectionProperty FACING = BlockStateProperties.FACING;
    public ItemTransferBlock(Properties properties) {
        super(properties);
        registerDefaultState(stateDefinition.any().setValue(FACING, Direction.NORTH));
    }
    private static final java.util.Map<Direction, net.minecraft.world.phys.shapes.VoxelShape> SHAPES = shapes();
    /** Body plus an outlet and an intake nozzle along the facing axis, matching the model. */
    private static java.util.Map<Direction, net.minecraft.world.phys.shapes.VoxelShape> shapes() {
        var result = new java.util.EnumMap<Direction, net.minecraft.world.phys.shapes.VoxelShape>(Direction.class);
        var body = Block.box(3, 3, 3, 13, 13, 13);
        for (Direction facing : Direction.values())
            result.put(facing, net.minecraft.world.phys.shapes.Shapes.or(body, nozzle(facing, 4), nozzle(facing.getOpposite(), 5)).optimize());
        return result;
    }
    private static net.minecraft.world.phys.shapes.VoxelShape nozzle(Direction side, double inset) {
        double low = inset, high = 16 - inset, depth = 3;
        return switch (side) {
            case NORTH -> Block.box(low, low, 0, high, high, depth);
            case SOUTH -> Block.box(low, low, 16 - depth, high, high, 16);
            case WEST -> Block.box(0, low, low, depth, high, high);
            case EAST -> Block.box(16 - depth, low, low, 16, high, high);
            case DOWN -> Block.box(low, 0, low, high, depth, high);
            case UP -> Block.box(low, 16 - depth, low, high, 16, high);
        };
    }
    @Override protected net.minecraft.world.phys.shapes.VoxelShape getShape(BlockState state, net.minecraft.world.level.BlockGetter level,
            BlockPos pos, net.minecraft.world.phys.shapes.CollisionContext context) {
        return SHAPES.get(state.getValue(FACING));
    }
    @Override protected MapCodec<? extends BaseEntityBlock> codec() { return MapCodec.unit(this); }
    @Override protected RenderShape getRenderShape(BlockState state) { return RenderShape.MODEL; }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) { builder.add(FACING); }
    @Override public BlockState getStateForPlacement(BlockPlaceContext context) { return defaultBlockState().setValue(FACING, context.getNearestLookingDirection()); }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) { return state.setValue(FACING, rotation.rotate(state.getValue(FACING))); }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) { return state.setValue(FACING, mirror.mirror(state.getValue(FACING))); }
    @Override public BlockEntity newBlockEntity(BlockPos pos, BlockState state) { return new ItemTransferBlockEntity(pos, state); }
    @Override public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide ? null : createTickerHelper(type, Technologia.ITEM_TRANSFER_TYPE, ItemTransferBlockEntity::tick);
    }
    @Override protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
                                                       Player player, InteractionHand hand, BlockHitResult hit) {
        if (stack.isEmpty()) return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
        if (stack.getItem() == Technologia.ITEMS.get("wrench")) return ItemInteractionResult.SKIP_DEFAULT_BLOCK_INTERACTION;
        if (!player.mayBuild()) return ItemInteractionResult.FAIL;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ItemTransferBlockEntity transfer) {
            transfer.setFilter(stack);
            player.displayClientMessage(Component.translatable("message.technologia.transfer_filter", stack.getHoverName()), true);
        }
        return ItemInteractionResult.sidedSuccess(level.isClientSide);
    }
    @Override protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.mayBuild()) return InteractionResult.FAIL;
        if (!level.isClientSide && level.getBlockEntity(pos) instanceof ItemTransferBlockEntity transfer) {
            transfer.setFilter(ItemStack.EMPTY);
            player.displayClientMessage(Component.translatable("message.technologia.transfer_unfiltered"), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide);
    }
}
