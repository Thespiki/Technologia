package dev.technologia.machine;

import dev.technologia.Technologia;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

/**
 * Chunk loaders keep their area loaded with the game's own forced chunks, which survive a restart.
 * A loader remembers the radius it holds and which chunks it forced itself, and gives those back
 * when it loses power, is switched off, changes tier or is removed. A chunk that was already forced
 * by a command or another mod is left alone. Claims of several loaders on one chunk are counted in
 * memory and rebuilt as loaders tick; a chunk is only released when no running loader needs it.
 * <p>
 * The game is only asked to force or release a chunk when that changes something: asking again for
 * a chunk in the state it already has would mark the game's saved list as unchanged, and a change
 * that was waiting to be saved would be lost.
 */
final class ChunkLoading {
    static final int ENERGY_PER_CHUNK = 10, REFRESH_TICKS = 100;
    /**
     * A loader that holds nothing takes its area only with this many ticks of energy in store, and
     * after giving an area back it first runs this many ticks. A weak supply or a redstone clock can
     * therefore not make the game load and unload the chunks every few ticks.
     */
    static final int RESTART_TICKS = 100;
    private static final Map<ServerLevel, Map<Long, Set<BlockPos>>> CLAIMS = new WeakHashMap<>();

    /** Mk I holds its own chunk, Mk III a 3x3 area and Mk VI a 5x5 area, up to the configured limit. */
    static int radius(MachineBlockEntity machine) {
        int byTier = machine.tier().index() >= 5 ? 2 : machine.tier().index() >= 2 ? 1 : 0;
        return Math.min(byTier, Technologia.BALANCE.chunkLoaderRadius());
    }

    static void keep(MachineBlockEntity machine, ServerLevel level, long time) {
        if (!Technologia.BALANCE.chunkLoading()) { release(machine, level); machine.status(18); return; }
        int radius = radius(machine), side = radius * 2 + 1;
        int cost = (int) Math.max(1, Math.round(side * side * ENERGY_PER_CHUNK * machine.tier().efficiency()));
        if (machine.loadedRadius < 0) {
            if (machine.energy.stored() < Math.min((long) cost * RESTART_TICKS, machine.energy.capacity())) { release(machine, level); machine.status(2); return; }
            if (machine.restartWait > 0) { machine.restartWait--; machine.status(19); return; }
        }
        if (machine.energy.stored() < cost) { release(machine, level); machine.status(2); return; }
        machine.energy.extract(cost, false);
        machine.status(1);
        // The area follows the tier and the configured limit: the old one is given back first.
        if (machine.loadedRadius != radius) release(machine, level);
        if (!machine.claimed || time % REFRESH_TICKS == 0) claim(machine, level, radius);
    }

    private static void claim(MachineBlockEntity machine, ServerLevel level, int radius) {
        ChunkPos center = new ChunkPos(machine.getBlockPos());
        Map<Long, Set<BlockPos>> claims = CLAIMS.computeIfAbsent(level, key -> new HashMap<>());
        boolean changed = machine.loadedRadius != radius;
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            long key = ChunkPos.asLong(center.x + dx, center.z + dz);
            claims.computeIfAbsent(key, k -> new HashSet<>()).add(machine.getBlockPos());
            // A chunk this loader forces itself is one it must release; one that is forced already is not touched.
            if (!level.getForcedChunks().contains(key) && level.setChunkForced(center.x + dx, center.z + dz, true))
                changed |= machine.forcedChunks.add(key);
        }
        machine.loadedRadius = radius;
        machine.claimed = true;
        if (changed) machine.setChanged();
    }

    /** Gives back the chunks this loader forced, unless another running loader still claims them. */
    static void release(MachineBlockEntity machine, ServerLevel level) {
        if (machine.loadedRadius < 0 && machine.forcedChunks.isEmpty()) return;
        Map<Long, Set<BlockPos>> claims = CLAIMS.computeIfAbsent(level, key -> new HashMap<>());
        ChunkPos center = new ChunkPos(machine.getBlockPos());
        for (int dx = -machine.loadedRadius; dx <= machine.loadedRadius; dx++) for (int dz = -machine.loadedRadius; dz <= machine.loadedRadius; dz++) {
            long key = ChunkPos.asLong(center.x + dx, center.z + dz);
            Set<BlockPos> holders = claims.get(key);
            if (holders != null) { holders.remove(machine.getBlockPos()); if (holders.isEmpty()) claims.remove(key); }
        }
        for (long key : machine.forcedChunks) {
            Set<BlockPos> holders = claims.get(key);
            MachineBlockEntity heir = null;
            if (holders != null) for (BlockPos pos : holders)
                if (level.isLoaded(pos) && level.getBlockEntity(pos) instanceof MachineBlockEntity other && other.kind == MachineKind.CHUNK_LOADER && other != machine) { heir = other; break; }
            // Another loader still needs the chunk: it becomes the one responsible for releasing it.
            if (heir != null) { heir.forcedChunks.add(key); heir.setChanged(); }
            else if (level.getForcedChunks().contains(key)) level.setChunkForced(ChunkPos.getX(key), ChunkPos.getZ(key), false);
        }
        machine.forcedChunks.clear();
        machine.loadedRadius = -1;
        machine.claimed = false;
        machine.restartWait = RESTART_TICKS;
        machine.setChanged();
    }
    private ChunkLoading() {}
}
