package dev.technologia.device;

import dev.technologia.Technologia;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.ContainerHelper;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ChestMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.RandomizableContainerBlockEntity;
import net.minecraft.world.level.block.state.BlockState;

/** Plain storage on the vanilla container base, so hoppers, comparators and other mods' pipes treat it like a chest. */
public final class CrateBlockEntity extends RandomizableContainerBlockEntity {
    private final int rows;
    private NonNullList<ItemStack> items;
    public CrateBlockEntity(BlockPos pos, BlockState state) {
        super(Technologia.CRATE_TYPE, pos, state);
        rows = state.getBlock() instanceof CrateBlock crate ? Math.clamp(crate.rows, 3, 6) : 3;
        items = NonNullList.withSize(rows * 9, ItemStack.EMPTY);
    }
    @Override public int getContainerSize() { return rows * 9; }
    @Override protected Component getDefaultName() { return Component.translatable(getBlockState().getBlock().getDescriptionId()); }
    @Override protected NonNullList<ItemStack> getItems() { return items; }
    @Override protected void setItems(NonNullList<ItemStack> items) { this.items = items; }
    @Override protected AbstractContainerMenu createMenu(int id, Inventory inventory) {
        MenuType<?> type = switch (rows) { case 4 -> MenuType.GENERIC_9x4; case 5 -> MenuType.GENERIC_9x5; case 6 -> MenuType.GENERIC_9x6; default -> MenuType.GENERIC_9x3; };
        return new ChestMenu(type, id, inventory, this, rows);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items = NonNullList.withSize(getContainerSize(), ItemStack.EMPTY);
        if (!tryLoadLootTable(tag)) ContainerHelper.loadAllItems(tag, items, registries);
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        if (!trySaveLootTable(tag)) ContainerHelper.saveAllItems(tag, items, registries);
    }
}
