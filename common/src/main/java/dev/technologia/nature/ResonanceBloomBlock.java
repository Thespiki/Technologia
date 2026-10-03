package dev.technologia.nature;

import com.mojang.serialization.MapCodec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/**
 * A resonite-fed plant. Processing machines count the blooms growing around them and work
 * faster for each one, up to a small cap. The plant itself holds no state.
 */
public final class ResonanceBloomBlock extends BushBlock {
    private static final MapCodec<ResonanceBloomBlock> CODEC = simpleCodec(ResonanceBloomBlock::new);
    private static final VoxelShape SHAPE = Block.box(4, 0, 4, 12, 11, 12);
    public ResonanceBloomBlock(Properties properties) { super(properties); }
    @Override protected MapCodec<? extends BushBlock> codec() { return CODEC; }
    @Override protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) { return SHAPE; }
    @Override public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (random.nextInt(5) != 0) return;
        level.addParticle(ParticleTypes.END_ROD, pos.getX() + 0.3 + random.nextDouble() * 0.4, pos.getY() + 0.6,
                pos.getZ() + 0.3 + random.nextDouble() * 0.4, 0, 0.01, 0);
    }
}
