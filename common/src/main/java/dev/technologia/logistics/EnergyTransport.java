package dev.technologia.logistics;

import dev.technologia.machine.MachineBlockEntity;
import dev.technologia.machine.MachineKind;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.*;

/** Bounded, loaded-chunk-only routing. Conduits contain no energy and cannot duplicate it. */
public final class EnergyTransport {
    public static final int MAX_NODES = 128;
    public static final int TRANSFER_PER_TICK = 200;
    @FunctionalInterface public interface ExternalReceiver {
        int receive(Level level, BlockPos target, Direction inputSide, int amount, boolean simulate);
    }
    public static ExternalReceiver EXTERNAL_RECEIVER = (level, pos, side, amount, simulate) -> 0;
    private record Endpoint(BlockPos pos, Direction inputSide) {}

    /** Call once per supplier tick; direct neighbors and conduit endpoints share the same limit. */
    public static int transfer(MachineBlockEntity source) {
        Level level = source.getLevel();
        if (level == null || level.isClientSide || source.isRemoved() || !source.kind.suppliesEnergy() || source.energy.stored() <= 0) return 0;
        var queue = new ArrayDeque<BlockPos>();
        var conduits = new HashSet<BlockPos>();
        var endpoints = new LinkedHashSet<Endpoint>();
        collect(level, source.getBlockPos(), source.getBlockPos(), queue, conduits, endpoints);
        while (!queue.isEmpty()) collect(level, queue.removeFirst(), source.getBlockPos(), queue, conduits, endpoints);
        var targets = new ArrayList<>(endpoints);
        int budget = Math.min(TRANSFER_PER_TICK, source.energy.stored()), moved = 0;
        int first = targets.isEmpty() ? 0 : (int) Math.floorMod(level.getGameTime(), targets.size());
        // Rotate first priority each tick so one perpetually hungry machine cannot starve the rest.
        for (int i = 0; i < targets.size() && moved < budget; i++) {
            Endpoint endpoint = targets.get((first + i) % targets.size());
            if (!level.hasChunkAt(endpoint.pos)) continue;
            int amount = budget - moved, accepted;
            if (level.getBlockEntity(endpoint.pos) instanceof MachineBlockEntity target) {
                if (target.kind.suppliesEnergy() && !(source.kind.isGenerator() && target.kind.isCell())) continue;
                accepted = target.receiveEnergy(amount, false);
            } else {
                int offered = Math.clamp(EXTERNAL_RECEIVER.receive(level, endpoint.pos, endpoint.inputSide, amount, true), 0, amount);
                accepted = offered == 0 ? 0 : Math.clamp(EXTERNAL_RECEIVER.receive(level, endpoint.pos, endpoint.inputSide, offered, false), 0, offered);
            }
            if (accepted > 0) {
                source.extractEnergy(accepted, false);
                moved += accepted;
            }
        }
        return moved;
    }

    private static void collect(Level level, BlockPos pos, BlockPos source, ArrayDeque<BlockPos> queue,
                                Set<BlockPos> conduits, Set<Endpoint> endpoints) {
        for (Direction side : Direction.values()) {
            BlockPos next = pos.relative(side);
            if (next.equals(source) || !level.hasChunkAt(next)) continue;
            if (level.getBlockState(next).getBlock() instanceof EnergyConduitBlock) {
                if (conduits.size() < MAX_NODES && conduits.add(next)) queue.addLast(next);
            } else if (level.getBlockState(next).hasBlockEntity()) {
                endpoints.add(new Endpoint(next, side.getOpposite()));
            }
        }
    }
    private EnergyTransport() {}
}
