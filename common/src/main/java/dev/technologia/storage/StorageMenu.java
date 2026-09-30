package dev.technologia.storage;

import dev.technologia.Technologia;
import net.minecraft.world.Container;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import java.util.ArrayList;
import java.util.List;

/**
 * A view of a real core inventory: the 54 backing slots use vanilla synchronization,
 * and aggregation never owns or copies stored items. Only the server moves items.
 */
public final class StorageMenu extends AbstractContainerMenu {
    public static final int STORAGE_SLOTS = 54;
    public static final int DEPOSIT_BUTTON = -1;
    private static final int REVISION_MASK = 0x7fffff;
    private final Container storage;
    private final boolean clientSide;
    private final List<ItemStack> identities = new ArrayList<>(STORAGE_SLOTS);
    private int revision = 1;

    public enum Action { STACK, ONE, TO_INVENTORY }

    /** A snapshot for rendering, including the revision that identifies its source slot. */
    public record Entry(ItemStack stack, int count, int sourceSlot, int revision) {}

    public StorageMenu(int id, Inventory player) {
        this(id, player, new SimpleContainer(STORAGE_SLOTS));
    }

    public StorageMenu(int id, Inventory player, Container storage) {
        super(Technologia.STORAGE_MENU, id);
        checkContainerSize(storage, STORAGE_SLOTS);
        this.storage = storage;
        this.clientSide = player.player.level().isClientSide;
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            identities.add(identity(storage.getItem(i)));
            final int index = i;
            addSlot(new Slot(storage, index, -10000, -10000) {
                @Override public boolean isActive() { return false; }
                @Override public boolean mayPlace(ItemStack stack) { return storage.canPlaceItem(index, stack); }
                @Override public boolean mayPickup(Player player) { return false; }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++)
            addSlot(new Slot(player, col + row * 9 + 9, 69 + col * 18, 161 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 69 + col * 18, 219));
        // Vanilla container data packets carry signed shorts. Split the token explicitly.
        addDataSlot(new DataSlot() {
            @Override public int get() { return revision & 0xffff; }
            @Override public void set(int value) { revision = (revision & 0x7f0000) | (value & 0xffff); }
        });
        addDataSlot(new DataSlot() {
            @Override public int get() { return (revision >>> 16) & 0x7f; }
            @Override public void set(int value) { revision = (revision & 0xffff) | ((value & 0x7f) << 16); }
        });
    }

    public List<Entry> entries() {
        refreshRevision();
        List<Entry> result = new ArrayList<>();
        for (int slot = 0; slot < STORAGE_SLOTS; slot++) {
            ItemStack stack = storage.getItem(slot);
            if (stack.isEmpty()) continue;
            int match = -1;
            for (int i = 0; i < result.size(); i++) {
                if (ItemStack.isSameItemSameComponents(result.get(i).stack(), stack)) { match = i; break; }
            }
            if (match == -1) result.add(new Entry(identity(stack), stack.getCount(), slot, revision));
            else {
                Entry previous = result.get(match);
                result.set(match, new Entry(previous.stack(), previous.count() + stack.getCount(), previous.sourceSlot(), revision));
            }
        }
        return List.copyOf(result);
    }

    public int occupiedSlots() {
        int result = 0;
        for (int slot = 0; slot < STORAGE_SLOTS; slot++) if (!storage.getItem(slot).isEmpty()) result++;
        return result;
    }

    public int totalItems() {
        int result = 0;
        for (int slot = 0; slot < STORAGE_SLOTS; slot++) result += storage.getItem(slot).getCount();
        return result;
    }

    /** Button IDs are VAR_INT in Minecraft 1.21.1. No client supplied item or count is trusted. */
    public static int actionButton(Entry entry, Action action) {
        if (entry.sourceSlot() < 0 || entry.sourceSlot() >= STORAGE_SLOTS) throw new IllegalArgumentException("Invalid storage slot");
        return ((entry.revision() & REVISION_MASK) << 8) | (action.ordinal() << 6) | entry.sourceSlot();
    }

    @Override public boolean stillValid(Player player) { return storage.stillValid(player); }

    @Override public void broadcastChanges() {
        refreshRevision();
        super.broadcastChanges();
    }

    @Override public boolean clickMenuButton(Player player, int button) {
        if (clientSide || !stillValid(player)) return false;
        refreshRevision();
        if (button == DEPOSIT_BUTTON) {
            ItemStack carried = getCarried();
            if (carried.isEmpty() || !moveItemStackTo(carried, 0, STORAGE_SLOTS, false)) return false;
            if (carried.isEmpty()) setCarried(ItemStack.EMPTY);
            storage.setChanged();
            broadcastChanges();
            return true;
        }
        if (button < 0 || (button >>> 8) != revision) return false;
        int sourceSlot = button & 63;
        int actionId = (button >>> 6) & 3;
        if (sourceSlot >= STORAGE_SLOTS || actionId >= Action.values().length) return false;
        ItemStack source = storage.getItem(sourceSlot);
        if (source.isEmpty()) return false;
        ItemStack expected = identity(source);
        Action action = Action.values()[actionId];
        ItemStack carried = getCarried();
        if (action != Action.TO_INVENTORY && !carried.isEmpty()
                && !ItemStack.isSameItemSameComponents(expected, carried)) return false;
        int room = action == Action.ONE ? 1 : expected.getMaxStackSize();
        if (action != Action.TO_INVENTORY) room = Math.min(room, expected.getMaxStackSize() - carried.getCount());
        if (room <= 0) return false;
        int available = 0;
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            ItemStack stack = storage.getItem(i);
            if (ItemStack.isSameItemSameComponents(expected, stack)) available += stack.getCount();
        }
        int moved = Math.min(room, available);
        if (action == Action.TO_INVENTORY) {
            ItemStack transfer = expected.copyWithCount(moved);
            if (!moveItemStackTo(transfer, STORAGE_SLOTS, slots.size(), true)) return false;
            moved -= transfer.getCount();
        } else if (carried.isEmpty()) setCarried(expected.copyWithCount(moved));
        else carried.grow(moved);
        int remaining = moved;
        for (int i = 0; i < STORAGE_SLOTS && remaining > 0; i++) {
            ItemStack stack = storage.getItem(i);
            if (!ItemStack.isSameItemSameComponents(expected, stack)) continue;
            int removed = Math.min(remaining, stack.getCount());
            storage.removeItem(i, removed);
            remaining -= removed;
        }
        storage.setChanged();
        broadcastChanges();
        return moved > 0;
    }

    @Override public ItemStack quickMoveStack(Player player, int index) {
        // Core slots are intentionally only actionable through the revision-checked buttons.
        if (!stillValid(player) || index < STORAGE_SLOTS || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.hasItem() || !slot.mayPickup(player)) return ItemStack.EMPTY;
        ItemStack current = slot.getItem();
        ItemStack original = current.copy();
        if (!moveItemStackTo(current, 0, STORAGE_SLOTS, false)) return ItemStack.EMPTY;
        if (current.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, current);
        storage.setChanged();
        broadcastChanges();
        return original;
    }

    @Override public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (!stillValid(player) || (slotId >= 0 && slotId < STORAGE_SLOTS)) return;
        super.clicked(slotId, button, clickType, player);
    }

    @Override public boolean canDragTo(Slot slot) { return slot.index >= STORAGE_SLOTS; }
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.index >= STORAGE_SLOTS; }

    private void refreshRevision() {
        if (clientSide) return;
        boolean changed = false;
        for (int i = 0; i < STORAGE_SLOTS; i++) {
            ItemStack current = storage.getItem(i);
            if (!ItemStack.isSameItemSameComponents(identities.get(i), current)) {
                identities.set(i, identity(current));
                changed = true;
            }
        }
        if (changed) revision = revision == REVISION_MASK ? 1 : revision + 1;
    }

    private static ItemStack identity(ItemStack stack) { return stack.isEmpty() ? ItemStack.EMPTY : stack.copyWithCount(1); }
}
