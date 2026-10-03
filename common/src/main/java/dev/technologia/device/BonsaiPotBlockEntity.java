package dev.technologia.device;

import dev.technologia.Technologia;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.Container;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.WorldlyContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Slot 0 holds the planted sapling; slots 1 to 9 hold what the tree gave. */
public final class BonsaiPotBlockEntity extends BlockEntity implements WorldlyContainer {
    public static final int GROW_TICKS = 1200, RESULT_SLOTS = 9, SLOTS = 1 + RESULT_SLOTS;
    private static final int[] FACE_SLOTS = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    private final NonNullList<ItemStack> items = NonNullList.withSize(SLOTS, ItemStack.EMPTY);
    private int growth;
    public BonsaiPotBlockEntity(BlockPos pos, BlockState state) { super(Technologia.BONSAI_TYPE, pos, state); }

    /** The log a sapling grows into, found by name so trees from other mods work too; null when unknown. */
    public static Item logFor(Item sapling) {
        ResourceLocation id = BuiltInRegistries.ITEM.getKey(sapling);
        String path = id.getPath(), wood;
        if (path.endsWith("_sapling")) wood = path.substring(0, path.length() - "_sapling".length());
        else if (path.endsWith("_propagule")) wood = path.substring(0, path.length() - "_propagule".length());
        else if (path.endsWith("_fungus")) wood = path.substring(0, path.length() - "_fungus".length());
        else if (path.equals("azalea") || path.equals("flowering_azalea")) wood = "oak";
        else return null;
        for (String suffix : new String[] {"_log", "_stem"}) {
            Item log = BuiltInRegistries.ITEM.get(ResourceLocation.fromNamespaceAndPath(id.getNamespace(), wood + suffix));
            if (log != Items.AIR) return log;
        }
        return null;
    }
    public static boolean isSapling(ItemStack stack) {
        return (stack.is(ItemTags.SAPLINGS) || stack.is(Items.CRIMSON_FUNGUS) || stack.is(Items.WARPED_FUNGUS)) && logFor(stack.getItem()) != null;
    }

    public ItemStack sapling() { return items.getFirst(); }
    public boolean isGrown() { return growth >= GROW_TICKS; }
    public int percent() { return Math.min(100, growth * 100 / GROW_TICKS); }
    public void plant(ItemStack sapling) { items.set(0, sapling); growth = 0; setChanged(); refreshStage(); }
    public void feed() { growth = Math.min(GROW_TICKS, growth + GROW_TICKS / 4); setChanged(); refreshStage(); }
    public ItemStack uproot() {
        ItemStack sapling = items.getFirst();
        items.set(0, ItemStack.EMPTY); growth = 0; setChanged(); refreshStage();
        return sapling;
    }
    /** Hands every result to the player; false when there was nothing to take. */
    public boolean giveResults(Player player) {
        boolean any = false;
        for (int slot = 1; slot < SLOTS; slot++) {
            if (items.get(slot).isEmpty()) continue;
            Technologia.giveOrDrop(player, items.get(slot), worldPosition);
            items.set(slot, ItemStack.EMPTY);
            any = true;
        }
        if (any) setChanged();
        return any;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, BonsaiPotBlockEntity pot) {
        if (pot.sapling().isEmpty()) return;
        if (pot.growth < GROW_TICKS) {
            pot.growth++;
            if (pot.growth % 100 == 0 || pot.growth == GROW_TICKS) { pot.setChanged(); pot.refreshStage(); }
            return;
        }
        // A grown tree waits until its whole yield fits, then is cut back to a sapling.
        if (level.getGameTime() % 20 != 0) return;
        List<ItemStack> yield = pot.yield();
        if (!pot.fits(yield)) return;
        for (ItemStack stack : yield) pot.store(stack);
        pot.growth = 0;
        pot.setChanged();
        pot.refreshStage();
    }
    private List<ItemStack> yield() {
        List<ItemStack> result = new ArrayList<>();
        Item log = logFor(sapling().getItem());
        if (log == null) return result;
        var random = level.random;
        result.add(new ItemStack(log, 2 + random.nextInt(2)));
        if (random.nextInt(3) > 0) result.add(new ItemStack(Items.STICK, 1 + random.nextInt(2)));
        if (random.nextInt(5) == 0) result.add(sapling().copyWithCount(1));
        if ((sapling().is(Items.OAK_SAPLING) || sapling().is(Items.DARK_OAK_SAPLING)) && random.nextInt(20) == 0) result.add(new ItemStack(Items.APPLE));
        return result;
    }
    private boolean fits(List<ItemStack> stacks) {
        int free = 0;
        for (int slot = 1; slot < SLOTS; slot++) if (items.get(slot).isEmpty()) free++;
        // Conservative: one free slot per stack that cannot be merged completely.
        for (ItemStack stack : stacks) {
            int remaining = stack.getCount();
            for (int slot = 1; slot < SLOTS && remaining > 0; slot++) {
                ItemStack held = items.get(slot);
                if (!held.isEmpty() && ItemStack.isSameItemSameComponents(held, stack)) remaining -= held.getMaxStackSize() - held.getCount();
            }
            if (remaining > 0 && free-- <= 0) return false;
        }
        return true;
    }
    private void store(ItemStack stack) {
        ItemStack remaining = stack.copy();
        for (int slot = 1; slot < SLOTS && !remaining.isEmpty(); slot++) {
            ItemStack held = items.get(slot);
            if (held.isEmpty() || !ItemStack.isSameItemSameComponents(held, remaining)) continue;
            int moved = Math.min(remaining.getCount(), held.getMaxStackSize() - held.getCount());
            held.grow(moved); remaining.shrink(moved);
        }
        for (int slot = 1; slot < SLOTS && !remaining.isEmpty(); slot++)
            if (items.get(slot).isEmpty()) { items.set(slot, remaining.copy()); remaining.setCount(0); }
    }
    private void refreshStage() {
        if (level == null || level.isClientSide) return;
        int stage = sapling().isEmpty() ? 0 : growth >= GROW_TICKS ? 3 : growth >= GROW_TICKS / 2 ? 2 : 1;
        BlockState state = getBlockState();
        if (state.hasProperty(BonsaiPotBlock.STAGE) && state.getValue(BonsaiPotBlock.STAGE) != stage)
            level.setBlock(worldPosition, state.setValue(BonsaiPotBlock.STAGE, stage), Block.UPDATE_CLIENTS);
    }

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Growth", growth);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear();
        ContainerHelper.loadAllItems(tag, items, registries);
        growth = Math.clamp(tag.getInt("Growth"), 0, GROW_TICKS);
    }

    @Override public int getContainerSize() { return SLOTS; }
    @Override public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    @Override public ItemStack getItem(int slot) { return items.get(slot); }
    @Override public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) { if (slot == 0) growth = 0; setChanged(); refreshStage(); }
        return removed;
    }
    @Override public ItemStack removeItemNoUpdate(int slot) {
        ItemStack removed = ContainerHelper.takeItem(items, slot);
        if (slot == 0) growth = 0;
        return removed;
    }
    @Override public void setItem(int slot, ItemStack stack) {
        if (slot == 0 && !ItemStack.isSameItemSameComponents(items.getFirst(), stack)) growth = 0;
        items.set(slot, stack);
        setChanged();
        refreshStage();
    }
    @Override public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    @Override public void clearContent() { items.clear(); growth = 0; setChanged(); refreshStage(); }
    @Override public boolean canPlaceItem(int slot, ItemStack stack) { return slot == 0 && items.getFirst().isEmpty() && isSapling(stack); }
    /** Only the sapling slot accepts items, and it takes a single sapling however many are offered. */
    @Override public int getMaxStackSize() { return 1; }
    /** Comparator reading from how full the result slots are. */
    public int comparatorSignal() {
        int total = 0;
        for (int slot = 1; slot < SLOTS; slot++) total += items.get(slot).getCount();
        return total == 0 ? 0 : 1 + Math.min(14, 14 * total / (RESULT_SLOTS * 64));
    }
    @Override public int[] getSlotsForFace(Direction side) { return FACE_SLOTS; }
    @Override public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return canPlaceItem(slot, stack); }
    @Override public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return slot > 0; }
}
