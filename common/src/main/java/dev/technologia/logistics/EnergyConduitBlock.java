package dev.technologia.logistics;

import com.mojang.serialization.MapCodec;
import dev.technologia.machine.MachineBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;

/** Connected static geometry. Routing is performed by suppliers, never by cable block entities. */
public final class EnergyConduitBlock extends PipeBlock {
    public EnergyConduitBlock(Properties properties) {
        super(3.0F / 16.0F, properties.noOcclusion());
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
                || !(neighbor.getBlock() instanceof MachineBlock) && neighbor.hasBlockEntity();
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
        return state.setValue(PROPERTY_BY_DIRECTION.get(side), connects(neighbor));
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
