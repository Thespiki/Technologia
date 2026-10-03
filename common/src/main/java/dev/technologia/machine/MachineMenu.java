package dev.technologia.machine;

import dev.technologia.Technologia;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/**
 * Every machine shares one slot layout: a 4x4 ingredient grid and a 9x3 result grid. The tier
 * decides how many ingredient slots are active; unused ones are hidden and refuse clicks.
 */
public final class MachineMenu extends AbstractContainerMenu {
    public static final int INPUT_X = 13, OUTPUT_X = 100, GRID_Y = 62, INVENTORY_X = 59;
    public static final int BUTTON_TOGGLE = 0, BUTTON_RESCAN = 1, BUTTON_REDSTONE = 2, BUTTON_EJECT = 3, BUTTON_CHANNEL_DOWN = 4,
            BUTTON_CHANNEL_UP = 5, BUTTON_CHANNEL_DOWN_10 = 6, BUTTON_CHANNEL_UP_10 = 7, BUTTON_LIMIT = 8;
    private static final int MACHINE_SLOTS = MachineBlockEntity.SLOTS;
    private final Container machine;
    private final ContainerData data;
    private final Player viewer;
    public MachineMenu(int id, Inventory player) { this(id, player, new SimpleContainer(MACHINE_SLOTS), new SimpleContainerData(MachineBlockEntity.DATA_COUNT)); }
    public MachineMenu(int id, Inventory player, Container machine, ContainerData data) {
        super(Technologia.MACHINE_MENU, id);
        checkContainerSize(machine, MACHINE_SLOTS); checkContainerDataCount(data, MachineBlockEntity.DATA_COUNT);
        this.machine = machine; this.data = data; this.viewer = player.player;
        for (int i = 0; i < MachineBlockEntity.INPUT_SLOTS; i++) {
            final int input = i;
            addSlot(new Slot(machine, i, inputX(i), inputY(i)) {
                @Override public boolean isActive() { return input < inputCount(); }
                @Override public boolean mayPlace(ItemStack stack) { return isActive() && accepts(input, stack); }
                @Override public boolean mayPickup(Player player) { return isActive() && mayUse(player); }
            });
        }
        for (int i = 0; i < MachineBlockEntity.OUTPUT_SLOTS; i++) {
            addSlot(new Slot(machine, MachineBlockEntity.FIRST_OUTPUT + i, OUTPUT_X + i % 9 * 18, GRID_Y + i / 9 * 18) {
                @Override public boolean isActive() { return hasOutput(); }
                @Override public boolean mayPlace(ItemStack stack) { return false; }
                @Override public boolean mayPickup(Player player) { return isActive() && mayUse(player); }
            });
        }
        for (int row = 0; row < 3; row++) for (int col = 0; col < 9; col++) addSlot(new Slot(player, col + row * 9 + 9, INVENTORY_X + col * 18, 161 + row * 18));
        for (int col = 0; col < 9; col++) addSlot(new Slot(player, col, INVENTORY_X + col * 18, 219));
        addDataSlots(data);
    }
    /** Lanes sit side by side; a two-ingredient lane keeps its pair adjacent. */
    public static int inputX(int slot) { return INPUT_X + slot % 4 * 18; }
    public static int inputY(int slot) { return GRID_Y + slot / 4 * 18; }

    public int energy() { return (data.get(0) & 65535) | (data.get(1) << 16); }
    public MachineKind kind() { return MachineKind.values()[Math.clamp(data.get(2), 0, MachineKind.values().length - 1)]; }
    public int status() { return data.get(3); }
    public boolean enabled() { return data.get(4) != 0; }
    public MachineTier tier() { return MachineTier.get(data.get(5)); }
    public int boost() { return Math.clamp(data.get(6), 0, MachineBlockEntity.MAX_BLOOMS); }
    /** Progress of one lane in thousandths. */
    public int laneProgress(int lane) { return lane < 0 || lane >= MachineTier.MAX_LANES ? 0 : Math.clamp(data.get(7 + lane), 0, 1000); }
    public int capacity() { return MachineBlockEntity.scaledCapacity(kind(), tier()); }
    public int lanes() { return kind().lanes(tier()); }
    public int inputCount() { return kind().inputCount(tier()); }
    public int redstoneMode() { return Math.clamp(data.get(MachineBlockEntity.DATA_REDSTONE), 0, MachineBlockEntity.REDSTONE_MODES - 1); }
    public boolean ejects() { return data.get(MachineBlockEntity.DATA_EJECT) != 0; }
    public int channel() { return Math.clamp(data.get(MachineBlockEntity.DATA_CHANNEL), 0, MachineBlockEntity.MAX_CHANNEL); }
    /** Energy made or relayed during the last tick. */
    public int rate() { return (data.get(MachineBlockEntity.DATA_RATE) & 65535) | (data.get(MachineBlockEntity.DATA_RATE_HIGH) << 16); }
    public int mesh() { return Math.clamp(data.get(MachineBlockEntity.DATA_MESH), 0, dev.technologia.recipe.MachineRecipe.MAX_MESH); }
    public int wirelessLimit() { return MachineBlockEntity.WIRELESS_LIMITS[Math.clamp(data.get(MachineBlockEntity.DATA_LIMIT), 0, MachineBlockEntity.WIRELESS_LIMITS.length - 1)]; }
    public boolean hasInput() { return inputCount() > 0; }
    public boolean hasOutput() { return kind().hasOutput(); }
    @Override public boolean stillValid(Player player) { return machine.stillValid(player); }
    /**
     * A miner's screen belongs to its owner: other players may look, not take results or change the
     * filter here. Hoppers, Item Transfers and breaking the block are not owner-checked; that is the
     * job of a protection mod.
     */
    private boolean mayUse(Player player) { return !(machine instanceof MachineBlockEntity be) || be.mayConfigure(player); }
    @Override public boolean clickMenuButton(Player player, int button) {
        if (!stillValid(player) || !(machine instanceof MachineBlockEntity be) || !be.mayConfigure(player)) return false;
        switch (button) {
            case BUTTON_TOGGLE -> { if (!be.kind.hasSwitch()) return false; be.toggle(player); }
            case BUTTON_RESCAN -> { if (be.kind != MachineKind.MINER) return false; be.rescan(player); }
            case BUTTON_REDSTONE -> { if (!be.kind.hasSwitch()) return false; be.cycleRedstone(); }
            case BUTTON_EJECT -> { if (!be.kind.hasOutput()) return false; be.toggleEject(); }
            case BUTTON_CHANNEL_DOWN, BUTTON_CHANNEL_UP, BUTTON_CHANNEL_DOWN_10, BUTTON_CHANNEL_UP_10 -> {
                if (!be.kind.isWireless()) return false;
                be.changeChannel(button == BUTTON_CHANNEL_DOWN ? -1 : button == BUTTON_CHANNEL_UP ? 1 : button == BUTTON_CHANNEL_DOWN_10 ? -10 : 10);
            }
            case BUTTON_LIMIT -> { if (be.kind != MachineKind.WIRELESS_SENDER) return false; be.cycleLimit(); }
            default -> { return false; }
        }
        return true;
    }
    @Override public ItemStack quickMoveStack(Player player, int index) {
        if (!stillValid(player) || !mayUse(player) || index < 0 || index >= slots.size()) return ItemStack.EMPTY;
        Slot slot = slots.get(index);
        if (!slot.isActive() || !slot.mayPickup(player) || !slot.hasItem()) return ItemStack.EMPTY;
        ItemStack current = slot.getItem(), original = current.copy();
        if (index < MACHINE_SLOTS) {
            if (!moveItemStackTo(current, MACHINE_SLOTS, slots.size(), true)) return ItemStack.EMPTY;
        } else if (!moveItemStackTo(current, 0, inputCount(), false)) return ItemStack.EMPTY;
        if (current.isEmpty()) slot.setByPlayer(ItemStack.EMPTY); else slot.setChanged();
        slot.onTake(player, current);
        return original;
    }
    @Override public void clicked(int slotId, int button, ClickType type, Player player) {
        if (!stillValid(player)) return;
        if (slotId >= 0 && slotId < MACHINE_SLOTS && (!slots.get(slotId).isActive() || !mayUse(player))) return;
        super.clicked(slotId, button, type, player);
    }
    @Override public boolean canDragTo(Slot slot) { return slot.index >= MACHINE_SLOTS || slot.isActive(); }
    /** Double-click collection must respect hidden slots and the miner's owner too. */
    @Override public boolean canTakeItemForPickAll(ItemStack stack, Slot slot) { return slot.index >= MACHINE_SLOTS || slot.isActive() && mayUse(viewer); }
    /**
     * The server asks the machine. The client has no machine, only the synced recipes, so it runs
     * the same ingredient test to avoid showing an item the server is about to refuse.
     */
    private boolean accepts(int slot, ItemStack stack) {
        if (!viewer.level().isClientSide || !kind().isProcessor()) return machine.canPlaceItem(slot, stack);
        return dev.technologia.recipe.RecipeIndex.isIngredient(viewer.level(), kind(), stack);
    }
}
