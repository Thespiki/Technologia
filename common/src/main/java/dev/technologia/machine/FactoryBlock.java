package dev.technologia.machine;

import net.minecraft.core.*;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.*;

/** Workshop construction blocks use the same physical profile as their baked models. */
public class FactoryBlock extends Block {
    protected final String model;
    public FactoryBlock(String model, Properties properties) { super(properties); this.model = model; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return FactoryShapes.get(model, Direction.NORTH);
    }
}
