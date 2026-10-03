package dev.technologia.logistics;

import dev.technologia.Technologia;
import dev.technologia.machine.MachineBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;

import java.util.*;

/** Bounded, loaded-chunk-only routing. Conduits contain no energy and cannot duplicate it. */
public final class EnergyTransport {
    public static final int MAX_NODES = 128;
    /** Rate of a first-tier supplier and of a copper conduit. */
    public static final int TRANSFER_PER_TICK = MachineBlockEntity.BASE_TRANSFER;
    /** A cached route is rebuilt at the latest after this many ticks, which covers chunk loading. */
    public static final int ROUTE_LIFETIME = 100;
    @FunctionalInterface public interface ExternalReceiver {
        int receive(Level level, BlockPos target, Direction inputSide, int amount, boolean simulate);
    }
    public static ExternalReceiver EXTERNAL_RECEIVER = (level, pos, side, amount, simulate) -> 0;
    /** {@code run} is the conduit run that reaches the block, or -1 when it touches the supplier. */
    public record Endpoint(BlockPos pos, Direction inputSide, int run) {}
    /**
     * What one supplier can reach. Conduit runs that only meet at the supplier are separate: each
     * has its own entry in {@code ratings}, the weakest conduit on that run. A rating limits what
     * flows through that run, never what goes to a directly adjacent block.
     */
    public record Route(List<Endpoint> endpoints, int[] ratings, boolean truncated, int topology, long builtAt) {}

    /** Call once per supplier tick; direct neighbors and conduit endpoints share the supplier's limit. */
    public static int transfer(MachineBlockEntity source) {
        Level level = source.getLevel();
        if (level == null || level.isClientSide || source.isRemoved() || !source.kind.suppliesEnergy()) return 0;
        int budget = source.sendBudget();
        if (budget <= 0) return 0;
        long now = level.getGameTime();
        Route route = source.route;
        if (route == null || route.topology != Technologia.topology() || now - route.builtAt >= ROUTE_LIFETIME || now < route.builtAt)
            route = source.route = build(level, source.getBlockPos(), now);
        List<Endpoint> targets = route.endpoints;
        if (targets.isEmpty()) return 0;
        int moved = 0;
        int[] left = route.ratings.clone();
        int first = (int) Math.floorMod(now, (long) targets.size());
        // Rotate first priority each tick so one perpetually hungry machine cannot starve the rest.
        for (int i = 0; i < targets.size() && moved < budget; i++) {
            Endpoint endpoint = targets.get((first + i) % targets.size());
            int amount = endpoint.run < 0 ? budget - moved : Math.min(budget - moved, left[endpoint.run]);
            if (amount <= 0 || !level.hasChunkAt(endpoint.pos)) continue;
            int accepted;
            if (level.getBlockEntity(endpoint.pos) instanceof MachineBlockEntity target) {
                if (target.kind.suppliesEnergy() && !(source.kind.isGenerator() && target.kind.isCell())) continue;
                accepted = target.receiveEnergy(amount, false);
            } else {
                int offered = Math.clamp(EXTERNAL_RECEIVER.receive(level, endpoint.pos, endpoint.inputSide, amount, true), 0, amount);
                accepted = offered == 0 ? 0 : Math.clamp(EXTERNAL_RECEIVER.receive(level, endpoint.pos, endpoint.inputSide, offered, false), 0, offered);
            }
            if (accepted > 0) {
                // Trust what the supplier really gave up, not what the receiver reported.
                int sent = source.send(accepted, false);
                moved += sent;
                if (endpoint.run >= 0) left[endpoint.run] -= sent;
            }
        }
        return moved;
    }

    private static Route build(Level level, BlockPos source, long now) {
        var conduits = new HashSet<BlockPos>();
        var endpoints = new LinkedHashMap<Object, Endpoint>();
        var ratings = new ArrayList<Integer>();
        boolean[] truncated = {false};
        // Blocks touching the supplier first, then one walk per conduit run that starts at it.
        var starts = new ArrayDeque<BlockPos>();
        collect(level, source, source, -1, starts, new HashSet<>(), endpoints, new int[] {Integer.MAX_VALUE}, truncated);
        for (BlockPos start : starts) {
            if (conduits.contains(start)) continue;
            if (conduits.size() >= MAX_NODES) { truncated[0] = true; break; }
            if (!(level.getBlockState(start).getBlock() instanceof EnergyConduitBlock first)) continue;
            int[] rating = {first.rating};
            var queue = new ArrayDeque<BlockPos>();
            conduits.add(start); queue.add(start);
            // The node bound is shared by all runs of one supplier.
            while (!queue.isEmpty()) collect(level, queue.removeFirst(), source, ratings.size(), queue, conduits, endpoints, rating, truncated);
            ratings.add(rating[0]);
        }
        return new Route(List.copyOf(endpoints.values()), ratings.stream().mapToInt(Integer::intValue).toArray(), truncated[0], Technologia.topology(), now);
    }

    private static void collect(Level level, BlockPos pos, BlockPos source, int run, ArrayDeque<BlockPos> queue,
                                Set<BlockPos> conduits, Map<Object, Endpoint> endpoints, int[] rating, boolean[] truncated) {
        for (Direction side : Direction.values()) {
            BlockPos next = pos.relative(side);
            if (next.equals(source) || !level.hasChunkAt(next)) continue;
            var state = level.getBlockState(next);
            if (state.getBlock() instanceof EnergyConduitBlock conduit) {
                if (conduits.contains(next)) continue;
                if (conduits.size() >= MAX_NODES) { truncated[0] = true; continue; }
                conduits.add(next.immutable()); queue.addLast(next.immutable());
                rating[0] = Math.min(rating[0], conduit.rating);
            } else if (state.hasBlockEntity()) {
                // Technologia machines ignore the face, so each is listed once. Another mod's block may
                // accept energy on some faces only, so every touching face is kept for it.
                BlockPos target = next.immutable();
                Object key = level.getBlockEntity(next) instanceof MachineBlockEntity ? target : List.of(target, side);
                // A block touching the supplier directly is never limited by a conduit it also touches.
                Endpoint known = endpoints.get(key);
                if (known == null || known.run >= 0 && run < 0) endpoints.put(key, new Endpoint(target, side.getOpposite(), run));
            }
        }
    }
    private EnergyTransport() {}
}
