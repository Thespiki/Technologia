package dev.technologia.machine;

import dev.technologia.Technologia;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

public final class MachineMenu extends AbstractContainerMenu {
    private final Container machine;
    private final ContainerData data;
    public MachineMenu(int id, Inventory player) { this(id, player, new SimpleContainer(27), new SimpleContainerData(7)); }
    public MachineMenu(int id, Inventory player, Container machine, ContainerData data) {
        super(Technologia.MACHINE_MENU, id);
        checkContainerSize(machine, 27); checkContainerDataCount(data, 7);
        this.machine = machine; this.data = data;
        for (int i = 0; i < 2; i++) {
            final int input = i;
            addSlot(new Slot(machine, i, 18, 64 + i * 24) {
                @Override public boolean isActive() { return input < kind().inputCount(); }
                @Override public boolean mayPlace(ItemStack stack) { return isActive() && machine.canPlaceItem(input, stack); }
                @Override public boolean mayPickup(Player player) { return isActive(); }
            });
        }
        Container outputView = player.player.level().isClientSide ? new SimpleContainer(26) : new Container() {
            private int index(int slot) { return slot + kind().inputCount(); }
            private boolean present(int slot) { return kind().hasOutput() && index(slot) < 27; }
            public int getContainerSize() { return 26; }
            public boolean isEmpty() { for(int i=0;i<26;i++) if(!getItem(i).isEmpty()) return false; return true; }
            public ItemStack getItem(int slot) { return present(slot) ? machine.getItem(index(slot)) : ItemStack.EMPTY; }
            public ItemStack removeItem(int slot,int count) { return present(slot) ? machine.removeItem(index(slot),count) : ItemStack.EMPTY; }
            public ItemStack removeItemNoUpdate(int slot) { return present(slot) ? machine.removeItemNoUpdate(index(slot)) : ItemStack.EMPTY; }
            public void setItem(int slot,ItemStack stack) { if(present(slot)) machine.setItem(index(slot),stack); }
            public boolean canPlaceItem(int slot,ItemStack stack) { return false; }
            public void setChanged() { machine.setChanged(); }
            public void clearContent() { for(int i=0;i<26;i++) if(present(i)) machine.setItem(index(i),ItemStack.EMPTY); }
            public boolean stillValid(Player viewer) { return machine.stillValid(viewer); }
        };
        for (int i = 0; i < 26; i++) {
            final int output = i;
            addSlot(new Slot(outputView, i, 66 + i % 9 * 18, 64 + i / 9 * 18) {
                @Override public boolean isActive() { return hasOutput() && output < 27 - kind().inputCount(); }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return isActive(); }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(player, col + row * 9 + 9, 42 + col * 18, 161 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, 42 + col * 18, 219));
        addDataSlots(data);
    }
    public int energy() { return (data.get(0) & 65535) | (data.get(1) << 16); }
    public MachineKind kind() { return MachineKind.values()[Math.clamp(data.get(2), 0, MachineKind.values().length - 1)]; }
    public int progress() { return data.get(3); }
    public int duration() { return Math.max(1, data.get(6)); }
    public int status() { return data.get(4); }
    public boolean enabled() { return data.get(5) != 0; }
    public boolean hasInput() { return kind().inputCount() > 0; }
    public boolean hasOutput() { return kind().hasOutput(); }
    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!stillValid(player) || !(machine instanceof MachineBlockEntity be) || !be.mayConfigure(player)) return false;
        if (button == 0) be.toggle(player);
        else if (button == 1 && be.kind == MachineKind.MINER) be.rescan(player);
        else return false;
        return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.isActive() || !slot.mayPickup(player) || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack current = slot.getItem(), original = current.copy();
        if (index < 28) {
            if (!moveItemStackTo(current, 28, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(current, 0, kind().inputCount(), false)) return ItemStack.EMPTY;
        if (current.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, current);
        return original;
    }
    @Override public void clicked(int slotId, int button, ClickType type, Player player) {
        if (!stillValid(player) || (slotId >= 0 && slotId < 28 && !slots.get(slotId).isActive())) return;
        super.clicked(slotId, button, type, player);
    }
}
