package dev.technologia.machine;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.FluidState;

/**
 * Early generators that read the world around them and consume nothing. Rates are FE per tick at
 * the first tier; the machine multiplies them by its tier.
 */
final class EnvironmentPower {
    static final int WATER_FLOWING = 6, WATER_FALLING = 10, WIND_MIN = 4, WIND_MAX = 32;

    static int rate(Level level, BlockPos pos, MachineKind kind) {
        return switch (kind) {
            case WATER_WHEEL -> water(level, pos);
            case WINDMILL -> wind(level, pos);
            case THERMO_GENERATOR -> heat(level, pos);
            default -> 0;
        };
    }

    /** Moving water on the four sides turns the wheel; still water does nothing. */
    private static int water(Level level, BlockPos pos) {
        int total = 0;
        for (Direction side : Direction.Plane.HORIZONTAL) {
            BlockPos near = pos.relative(side);
            if (!level.hasChunkAt(near)) continue;
            FluidState fluid = level.getFluidState(near);
            if (!fluid.is(FluidTags.WATER) || fluid.isSource()) continue;
            total += fluid.hasProperty(FlowingFluid.FALLING) && fluid.getValue(FlowingFluid.FALLING) ? WATER_FALLING : WATER_FLOWING;
        }
        return total;
    }

    /** Needs open sky and nothing solid beside or above it. Higher is windier, and so is bad weather. */
    private static int wind(Level level, BlockPos pos) {
        if (!level.dimensionType().hasSkyLight() || !level.canSeeSky(pos.above())) return 0;
        for (BlockPos near : BlockPos.betweenClosed(pos.offset(-1, 0, -1), pos.offset(1, 1, 1))) {
            if (near.equals(pos)) continue;
            if (!level.hasChunkAt(near) || !level.getBlockState(near).getCollisionShape(level, near).isEmpty()) return 0;
        }
        int base = Math.clamp(WIND_MIN + (pos.getY() - 64) / 6, WIND_MIN, WIND_MAX);
        return level.isThundering() ? base * 2 : level.isRaining() ? base * 3 / 2 : base;
    }

    /** The hottest neighbour times the coldest one; both are needed and neither is used up. */
    private static int heat(Level level, BlockPos pos) {
        int hot = 0;
        double cold = 0;
        for (Direction side : Direction.values()) {
            BlockPos near = pos.relative(side);
            if (!level.hasChunkAt(near)) continue;
            BlockState state = level.getBlockState(near);
            hot = Math.max(hot, hot(state));
            cold = Math.max(cold, cold(state));
        }
        return hot == 0 || cold == 0 ? 0 : (int) Math.round(hot * cold);
    }
    static int hot(BlockState state) {
        FluidState fluid = state.getFluidState();
        if (fluid.is(FluidTags.LAVA)) return fluid.isSource() ? 16 : 12;
        if (state.is(BlockTags.FIRE)) return 8;
        if (state.is(Blocks.MAGMA_BLOCK)) return 6;
        if (state.is(BlockTags.CAMPFIRES) && state.hasProperty(CampfireBlock.LIT) && state.getValue(CampfireBlock.LIT)) return 4;
        return 0;
    }
    static double cold(BlockState state) {
        if (state.is(Blocks.BLUE_ICE)) return 3;
        if (state.is(Blocks.PACKED_ICE)) return 2.5;
        if (state.is(BlockTags.ICE)) return 2;
        if (state.is(Blocks.SNOW_BLOCK) || state.is(Blocks.POWDER_SNOW)) return 1.5;
        if (state.getFluidState().is(FluidTags.WATER)) return 1;
        return 0;
    }
    private EnvironmentPower() {}
}
