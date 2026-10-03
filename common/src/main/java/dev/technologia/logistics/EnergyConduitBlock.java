package dev.technologia.logistics;

import com.mojang.serialization.MapCodec;
import dev.technologia.Technologia;
import dev.technologia.machine.MachineBlock;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;

/** Connected static geometry. Routing is performed by suppliers, never by cable block entities. */
public final class EnergyConduitBlock extends PipeBlock {
    /** Energy per tick one supplier can push through this conductor material. */
    public final int rating;
    public EnergyConduitBlock(int rating, Properties properties) {
        super(3.0F / 16.0F, properties.noOcclusion());
        this.rating = rating;
        BlockState state = stateDefinition.any();
        for (Direction side : Direction.values()) state = state.setValue(PROPERTY_BY_DIRECTION.get(side), false);
        registerDefaultState(state);
    }

    @Override protected MapCodec<? extends PipeBlock> codec() { return MapCodec.unit(this); }
    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    private static boolean connects(BlockState neighbor) {
        // Other mods can expose power on block entities; routing checks the actual sided energy API.
        return neighbor.getBlock() instanceof EnergyConduitBlock
                || neighbor.getBlock() instanceof MachineBlock machine && machine.kind.capacity > 0
                || !(neighbor.getBlock() instanceof MachineBlock) && !(neighbor.getBlock() instanceof ItemTransferBlock) && neighbor.hasBlockEntity();
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction side : Direction.values()) {
            BlockPos neighbor = context.getClickedPos().relative(side);
            state = state.setValue(PROPERTY_BY_DIRECTION.get(side),
                    context.getLevel().hasChunkAt(neighbor) && connects(context.getLevel().getBlockState(neighbor)));
        }
        return state;
    }
    @Override protected BlockState updateShape(BlockState state, Direction side, BlockState neighbor,
                                               LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        boolean connected = connects(neighbor);
        if (state.getValue(PROPERTY_BY_DIRECTION.get(side)) != connected) Technologia.topologyChanged();
        return state.setValue(PROPERTY_BY_DIRECTION.get(side), connected);
    }
    @Override protected void neighborChanged(BlockState state, Level level, BlockPos pos, Block neighborBlock, BlockPos neighborPos, boolean moving) {
        // Anything placed or removed beside a conduit may add or remove an endpoint.
        Technologia.topologyChanged();
        super.neighborChanged(state, level, pos, neighborBlock, neighborPos, moving);
    }
    @Override protected void onPlace(BlockState state, Level level, BlockPos pos, BlockState oldState, boolean moving) {
        if (!state.is(oldState.getBlock())) Technologia.topologyChanged();
        super.onPlace(state, level, pos, oldState, moving);
    }
    @Override protected void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean moving) {
        if (!state.is(newState.getBlock())) Technologia.topologyChanged();
        super.onRemove(state, level, pos, newState, moving);
    }
    @Override public void appendHoverText(ItemStack stack, Item.TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.technologia.conduit_rating", rating).withStyle(ChatFormatting.GRAY));
    }
    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState changed = state;
        for (Direction side : Direction.values()) changed = changed.setValue(PROPERTY_BY_DIRECTION.get(rotation.rotate(side)), state.getValue(PROPERTY_BY_DIRECTION.get(side)));
        return changed;
    }
    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState changed = state;
        for (Direction side : Direction.values()) changed = changed.setValue(PROPERTY_BY_DIRECTION.get(mirror.mirror(side)), state.getValue(PROPERTY_BY_DIRECTION.get(side)));
        return changed;
    }
    @Override protected boolean isPathfindable(BlockState state, PathComputationType type) { return false; }
}
