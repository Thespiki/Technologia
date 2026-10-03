package dev.technologia.logistics;

import dev.technologia.Technologia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.Container;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.WorldlyContainerHolder;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.ChestType;
import java.util.Arrays;
import java.util.stream.IntStream;

/** No item buffer: a persisted ghost filter is the only state owned by this transport. */
public final class ItemTransferBlockEntity extends BlockEntity {
    public static final int ITEMS_PER_OPERATION = 8;
    public static final int INTERVAL_TICKS = 8;
    private ItemStack filter = ItemStack.EMPTY;
    public ItemTransferBlockEntity(BlockPos pos, BlockState state) { super(Technologia.ITEM_TRANSFER_TYPE, pos, state); }
    public ItemStack filter() { return filter.copy(); }
    public void setFilter(ItemStack stack) { filter = stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1); setChanged(); }

    public static void tick(Level level, BlockPos pos, BlockState state, ItemTransferBlockEntity transfer) {
        // The position offsets each transfer so a large factory does not move everything on one tick.
        if (!level.isClientSide && (level.getGameTime() + Math.floorMod(pos.hashCode(), INTERVAL_TICKS)) % INTERVAL_TICKS == 0) transfer.transfer();
    }
    public int transfer() {
        if (level == null || level.isClientSide || isRemoved() || level.hasNeighborSignal(worldPosition)) return 0;
        Direction facing = getBlockState().getValue(ItemTransferBlock.FACING);
        Container source = containerAt(level, worldPosition.relative(facing.getOpposite()));
        Container target = containerAt(level, worldPosition.relative(facing));
        if (source == null || target == null || source == target) return 0;
        // Extraction follows hopper rules: vanilla sided blocks such as the furnace only describe what
        // may leave them for the bottom face, so a transfer on any side takes results, not fuel.
        return move(source, Direction.DOWN, target, facing.getOpposite(), filter, ITEMS_PER_OPERATION);
    }

    /** Public for deterministic tests and future devices; all transfers happen on the server thread. */
    public static int move(Container source, Direction sourceFace, Container target, Direction targetFace, ItemStack filter, int limit) {
        if (source == target || limit <= 0) return 0;
        int moved = 0;
        int[] destinationSlots = slots(target, targetFace);
        for (int sourceSlot : slots(source, sourceFace)) {
            if (moved >= Math.min(ITEMS_PER_OPERATION, limit)) break;
            ItemStack available = source.getItem(sourceSlot);
            if (available.isEmpty() || !filter.isEmpty() && !ItemStack.isSameItemSameComponents(filter, available)
                    || !source.canTakeItem(target, sourceSlot, available)
                    || source instanceof WorldlyContainer sided && !sided.canTakeItemThroughFace(sourceSlot, available, sourceFace)) continue;
            for (int targetSlot : destinationSlots) {
                if (available.isEmpty() || moved >= Math.min(ITEMS_PER_OPERATION, limit)) break;
                ItemStack existing = target.getItem(targetSlot);
                if (!existing.isEmpty() && !ItemStack.isSameItemSameComponents(existing, available)) continue;
                int room = Math.max(0, target.getMaxStackSize(available) - existing.getCount());
                int count = Math.min(Math.min(ITEMS_PER_OPERATION, limit) - moved, Math.min(room, available.getCount()));
                if (count == 0) continue;
                // Ask about the stack that would really arrive: some blocks only accept one item at a time.
                ItemStack offered = available.copyWithCount(count);
                if (!target.canPlaceItem(targetSlot, offered)
                        || target instanceof WorldlyContainer sided && !sided.canPlaceItemThroughFace(targetSlot, offered, targetFace)) continue;
                ItemStack extracted = source.removeItem(sourceSlot, count);
                if (extracted.isEmpty()) continue;
                ItemStack result = existing.isEmpty() ? extracted.copy() : existing.copyWithCount(existing.getCount() + extracted.getCount());
                target.setItem(targetSlot, result);
                moved += extracted.getCount();
                available = source.getItem(sourceSlot);
            }
        }
        if (moved > 0) { source.setChanged(); target.setChanged(); }
        return moved;
    }
    private static int[] slots(Container container, Direction face) {
        return container instanceof WorldlyContainer sided ? Arrays.stream(sided.getSlotsForFace(face))
                .filter(slot -> slot >= 0 && slot < container.getContainerSize()).distinct().toArray()
                : IntStream.range(0, container.getContainerSize()).toArray();
    }
    private static Container containerAt(Level level, BlockPos pos) {
        if (!level.hasChunkAt(pos)) return null;
        BlockState state = level.getBlockState(pos);
        if (state.getBlock() instanceof WorldlyContainerHolder holder) return holder.getContainer(state, level, pos);
        if (!(level.getBlockEntity(pos) instanceof Container container)) return null;
        if (container instanceof ChestBlockEntity && state.getBlock() instanceof ChestBlock chest) {
            if (state.getValue(ChestBlock.TYPE) != ChestType.SINGLE && !level.hasChunkAt(pos.relative(ChestBlock.getConnectedDirection(state)))) return null;
            return ChestBlock.getContainer(chest, state, level, pos, true);
        }
        return container;
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!filter.isEmpty()) tag.put("Filter", filter.save(registries));
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        setFilter(ItemStack.parseOptional(registries, tag.getCompound("Filter")));
    }
}
