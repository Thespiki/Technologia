package dev.technologia.machine;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.pathfinder.PathComputationType;

/** Static multipart geometry and cached collision shapes; no block entity or ticking required. */
public final class NetworkCableBlock extends PipeBlock {
    public NetworkCableBlock(Properties properties) {
        super(3.0F / 16.0F, properties.noOcclusion());
        BlockState state = stateDefinition.any();
        for (Direction direction : Direction.values()) state = state.setValue(PROPERTY_BY_DIRECTION.get(direction), false);
        registerDefaultState(state);
    }

    @Override protected MapCodec<? extends PipeBlock> codec() { return MapCodec.unit(this); }

    @Override protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(NORTH, EAST, SOUTH, WEST, UP, DOWN);
    }

    public static boolean connectsTo(BlockState state) {
        return state.getBlock() instanceof NetworkCableBlock
                || state.getBlock() instanceof MachineBlock machine
                && (machine.kind == MachineKind.CORE || machine.kind == MachineKind.TERMINAL);
    }

    @Override public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = defaultBlockState();
        for (Direction direction : Direction.values()) {
            state = state.setValue(PROPERTY_BY_DIRECTION.get(direction),
                    connectsTo(context.getLevel().getBlockState(context.getClickedPos().relative(direction))));
        }
        return state;
    }

    @Override protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
                                               LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
        return state.setValue(PROPERTY_BY_DIRECTION.get(direction), connectsTo(neighbor));
    }

    @Override protected BlockState rotate(BlockState state, Rotation rotation) {
        BlockState rotated = state;
        for (Direction direction : Direction.values()) {
            rotated = rotated.setValue(PROPERTY_BY_DIRECTION.get(rotation.rotate(direction)),
                    state.getValue(PROPERTY_BY_DIRECTION.get(direction)));
        }
        return rotated;
    }

    @Override protected BlockState mirror(BlockState state, Mirror mirror) {
        BlockState mirrored = state;
        for (Direction direction : Direction.values()) {
            mirrored = mirrored.setValue(PROPERTY_BY_DIRECTION.get(mirror.mirror(direction)),
                    state.getValue(PROPERTY_BY_DIRECTION.get(direction)));
        }
        return mirrored;
    }

    @Override protected boolean isPathfindable(BlockState state, PathComputationType type) { return false; }
}
