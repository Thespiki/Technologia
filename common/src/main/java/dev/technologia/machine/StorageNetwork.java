package dev.technologia.machine;

import dev.technologia.Technologia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import java.util.*;

/** Bounded traversal; never loads chunks. Inventory stays on the core, never copied. */
public final class StorageNetwork {
    public static MachineBlockEntity findCore(Level level, BlockPos start) {
        Set<BlockPos> seen = new HashSet<>();
        ArrayDeque<BlockPos> queue = new ArrayDeque<>();
        queue.add(start); seen.add(start);
        while (!queue.isEmpty()) {
            BlockPos pos = queue.removeFirst();
            if (!level.hasChunkAt(pos)) continue;
            if (level.getBlockEntity(pos) instanceof MachineBlockEntity be && be.kind == MachineKind.CORE) return be;
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (seen.size() >= 128 || seen.contains(next) || !level.hasChunkAt(next)) continue;
                var block = level.getBlockState(next).getBlock();
                if (block == Technologia.BLOCKS.get("network_cable") || block == Technologia.BLOCKS.get("storage_core") || block == Technologia.BLOCKS.get("storage_terminal")) { seen.add(next); queue.add(next); }
            }
        }
        return null;
    }

    public static MenuProvider access(MachineBlockEntity core, BlockPos terminal) {
        return new SimpleMenuProvider((id, inventory, player) -> ChestMenu.sixRows(id, inventory, new Container() {
            public int getContainerSize() { return core.getContainerSize(); }
            public boolean isEmpty() { return core.isEmpty(); }
            public ItemStack getItem(int slot) { return core.getItem(slot); }
            public ItemStack removeItem(int slot, int count) { return core.removeItem(slot, count); }
            public ItemStack removeItemNoUpdate(int slot) { return core.removeItemNoUpdate(slot); }
            public void setItem(int slot, ItemStack stack) { core.setItem(slot, stack); }
            public void setChanged() { core.setChanged(); }
            public void clearContent() { core.clearContent(); }
            public boolean stillValid(Player viewer) {
                Level level = core.getLevel();
                return !core.isRemoved() && level != null && viewer.level() == level
                        && viewer.distanceToSqr(terminal.getX() + .5, terminal.getY() + .5, terminal.getZ() + .5) <= 64
                        && level.getBlockState(terminal).is(Technologia.BLOCKS.get("storage_terminal"))
                        && findCore(level, terminal) == core;
            }
        }), Component.translatable("block.technologia.storage_core"));
    }
    private StorageNetwork() {}
}
