package dev.technologia.machine;

import dev.technologia.Technologia;
import dev.technologia.storage.StorageMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.*;

/** Bounded, loaded-only traversal. A terminal views real inventories without moving ownership. */
public final class StorageNetwork {
    public static final int MAX_CORES = 4;
    public static final int MAX_NODES = 128;

    public static List<MachineBlockEntity> findCores(Level level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        List<MachineBlockEntity> cores = new ArrayList<>();
        if (!level.hasChunkAt(start) || !isNetwork(level, start)) return List.of();
        queue.add(start.immutable()); seen.add(start.immutable());
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            if (!level.hasChunkAt(pos)) return List.of();
            if (level.getBlockEntity(pos) instanceof MachineBlockEntity be && be.kind == MachineKind.CORE) {
                cores.add(be);
                if (cores.size() > MAX_CORES) return List.of();
            }
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (seen.contains(next) || !level.hasChunkAt(next) || !isNetwork(level, next)) continue;
                if (seen.size() >= MAX_NODES) return List.of();
                seen.add(next); queue.add(next);
            }
        }
        cores.sort(Comparator.comparingLong(core -> core.getBlockPos().asLong()));
        return List.copyOf(cores);
    }

    private static boolean isNetwork(Level level, BlockPos pos) {
        var block = level.getBlockState(pos).getBlock();
        return block == Technologia.BLOCKS.get("network_cable") || block == Technologia.BLOCKS.get("storage_core")
                || block == Technologia.BLOCKS.get("storage_terminal");
    }

    /** Retained for callers that only need to check whether a usable network exists. */
    public static MachineBlockEntity findCore(Level level, BlockPos start) {
        var cores = findCores(level, start);
        return cores.isEmpty() ? null : cores.getFirst();
    }

    public static MenuProvider access(MachineBlockEntity core, BlockPos terminal) {
        return new SimpleMenuProvider((id, inventory, player) -> new StorageMenu(id, inventory, connectedContainer(core, terminal)),
                Component.translatable("block.technologia.storage_terminal"));
    }

    public static Container connectedContainer(MachineBlockEntity core, BlockPos terminal) {
        Level level = Objects.requireNonNull(core.getLevel());
        List<MachineBlockEntity> cores = findCores(level, terminal);
        if (!cores.contains(core)) throw new IllegalArgumentException("Core is not in the terminal network");
        BlockPos anchor = terminal.immutable();
        return new Container() {
            private MachineBlockEntity coreAt(int slot) { return cores.get(slot / 54); }
            public int getContainerSize() { return cores.size() * 54; }
            public boolean isEmpty() { return cores.stream().allMatch(MachineBlockEntity::isEmpty); }
            public ItemStack getItem(int slot) { return coreAt(slot).getItem(slot % 54); }
            public ItemStack removeItem(int slot, int count) { return coreAt(slot).removeItem(slot % 54, count); }
            public ItemStack removeItemNoUpdate(int slot) { return coreAt(slot).removeItemNoUpdate(slot % 54); }
            public void setItem(int slot, ItemStack stack) { coreAt(slot).setItem(slot % 54, stack); }
            public boolean canPlaceItem(int slot, ItemStack stack) { return coreAt(slot).canPlaceItem(slot % 54, stack); }
            public void setChanged() { cores.forEach(MachineBlockEntity::setChanged); }
            public void clearContent() { cores.forEach(MachineBlockEntity::clearContent); }
            public boolean stillValid(Player viewer) {
                return viewer.level() == level && level.hasChunkAt(anchor)
                        && viewer.distanceToSqr(anchor.getX() + .5, anchor.getY() + .5, anchor.getZ() + .5) <= 64
                        && level.getBlockState(anchor).is(Technologia.BLOCKS.get("storage_terminal"))
                        && cores.stream().allMatch(c -> !c.isRemoved() && level.hasChunkAt(c.getBlockPos())
                            && level.getBlockEntity(c.getBlockPos()) == c)
                        && findCores(level, anchor).equals(cores);
            }
        };
    }
    private StorageNetwork() {}
}
