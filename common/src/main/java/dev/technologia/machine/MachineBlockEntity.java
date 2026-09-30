package dev.technologia.machine;

import dev.technologia.Technologia;
import dev.technologia.storage.StorageMenu;
import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.TagKey;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import java.util.*;
import java.util.stream.IntStream;

public final class MachineBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    public final MachineKind kind;
    public final EnergyBuffer energy;
    public Object platformEnergy;
    private final NonNullList<ItemStack> items;
    private int progress, fuelTicks, cursor, status;
    private boolean enabled = true;
    private UUID owner;
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));
    private static final TagKey<Block> LEGACY_ORES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("forge", "ores"));
    public final ContainerData data = new ContainerData() {
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.stored() & 65535;
                case 1 -> energy.stored() >>> 16;
                case 2 -> kind.ordinal();
                case 3 -> progress;
                case 4 -> status;
                case 5 -> enabled ? 1 : 0;
                case 6 -> Technologia.BALANCE.processingTicks();
                default -> 0;
            };
        }
        public void set(int index, int value) {}
        public int getCount() { return 7; }
    };

    public MachineBlockEntity(BlockPos pos, BlockState state) {
        super(Technologia.MACHINE_TYPE, pos, state);
        kind = ((MachineBlock) state.getBlock()).kind;
        energy = new EnergyBuffer(Math.max(1, kind.capacity));
        items = NonNullList.withSize(kind == MachineKind.CORE ? 54 : 27, ItemStack.EMPTY);
        if (kind == MachineKind.MINER) enabled = false;
    }
    public void setOwner(UUID id) { owner = id; setChanged(); }
    public boolean mayConfigure(Player player) { return kind != MachineKind.MINER || owner == null || player.getUUID().equals(owner); }
    public void toggle(Player player) {
        if (mayConfigure(player)) {
            if (owner == null) owner = player.getUUID();
            enabled = !enabled; setChanged();
        }
    }
    public void rescan(Player player) { if (mayConfigure(player)) { cursor = 0; status = 0; setChanged(); } }
    public int receiveEnergy(int amount, boolean simulate) {
        if (kind.capacity == 0 || kind == MachineKind.GENERATOR) return 0;
        int received = energy.receive(amount, simulate);
        if (!simulate && received > 0) setChanged();
        return received;
    }
    public int extractEnergy(int amount, boolean simulate) {
        if (!kind.suppliesEnergy()) return 0;
        int extracted = energy.extract(amount, simulate);
        if (!simulate && extracted > 0) setChanged();
        return extracted;
    }

    public static void tick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        if (machine.kind.capacity == 0) return;
        int previousEnergy = machine.energy.stored();
        if (machine.enabled) {
            switch (machine.kind) {
                case GENERATOR -> machine.generate();
                case CRUSHER, FURNACE -> machine.process();
                case MINER -> { if (level.getGameTime() % 10 == 0) machine.mine((ServerLevel) level); }
                default -> machine.status = 0;
            }
        } else if (machine.status != 7) machine.status = 5;
        if (machine.kind.suppliesEnergy()) {
            for (Direction direction : Direction.values()) {
                BlockPos next = pos.relative(direction);
                if (!level.hasChunkAt(next)) continue;
                if (level.getBlockEntity(next) instanceof MachineBlockEntity target &&
                        (!target.kind.suppliesEnergy() || (machine.kind == MachineKind.GENERATOR && target.kind == MachineKind.CELL))) {
                    int moved = target.receiveEnergy(Math.min(200, machine.energy.stored()), false);
                    machine.energy.extract(moved, false);
                } else if (!(level.getBlockEntity(next) instanceof MachineBlockEntity)) Technologia.ENERGY_EXPORT.accept(machine, direction);
            }
        }
        if (previousEnergy != machine.energy.stored()) machine.setChanged();
        boolean active = machine.enabled && machine.status == 1;
        if (state.getValue(MachineBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(MachineBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }
    private void generate() {
        if (energy.stored() == energy.capacity()) { status = 0; return; }
        if (fuelTicks == 0) {
            if (!items.getFirst().is(Items.COAL) && !items.getFirst().is(Items.CHARCOAL)) { status = 4; return; }
            items.getFirst().shrink(1); fuelTicks = 1600; setChanged();
        }
        energy.receive(Technologia.BALANCE.generatorPerTick(), false);
        fuelTicks--; status = 1; setChanged();
    }
    private ItemStack result() {
        ItemStack input = items.getFirst();
        if (input.isEmpty()) return ItemStack.EMPTY;
        if (kind == MachineKind.FURNACE) {
            return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, new SingleRecipeInput(input), level)
                    .map(recipe -> recipe.value().assemble(new SingleRecipeInput(input), level.registryAccess())).orElse(ItemStack.EMPTY);
        }
        for (String material : List.of("iron", "gold", "copper", "tin", "lead")) {
            if (input.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("c", "raw_materials/" + material)))
                    || input.is(TagKey.create(Registries.ITEM, ResourceLocation.fromNamespaceAndPath("forge", "raw_materials/" + material)))) {
                return new ItemStack(Technologia.ITEMS.get(material + "_dust"), 2);
            }
        }
        return ItemStack.EMPTY;
    }
    private void process() {
        ItemStack output = result();
        if (output.isEmpty()) { status = 4; progress = 0; return; }
        if (!fits(List.of(output))) { status = 3; return; }
        int cost = Technologia.BALANCE.machineEnergyPerTick();
        if (energy.stored() < cost) { status = 2; return; }
        energy.extract(cost, false); progress++; status = 1;
        if (progress >= Technologia.BALANCE.processingTicks()) {
            insert(output); items.getFirst().shrink(1); progress = 0;
        }
        setChanged();
    }
    private void mine(ServerLevel level) {
        if (!Technologia.BALANCE.minerEnabled()) { status = 5; return; }
        var player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (player == null || player.level() != level || player.isSpectator() || !player.mayBuild()) { status = 6; return; }
        int cost = Technologia.BALANCE.minerEnergyPerBlock();
        if (energy.stored() < cost) { status = 2; return; }
        int radius = Technologia.BALANCE.minerRadius();
        int depth = Math.min(Technologia.BALANCE.minerDepth(), worldPosition.getY() - level.getMinBuildHeight());
        int width = radius * 2 + 1, total = width * width * Math.max(0, depth);
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        for (int budget = 0; budget < 32 && cursor < total; budget++) {
            int index = cursor++;
            BlockPos target = worldPosition.offset(index % width - radius, -1 - index / (width * width), (index / width) % width - radius);
            if (!level.hasChunkAt(target)) { cursor--; status = 9; setChanged(); return; }
            BlockState block = level.getBlockState(target);
            if ((!block.is(ORES) && !block.is(LEGACY_ORES)) || block.hasBlockEntity() || block.getDestroySpeed(level, target) < 0 || !tool.isCorrectToolForDrops(block)) continue;
            var drops = Block.getDrops(block, level, target, null, player, tool);
            ItemStack filter = items.getFirst();
            if (!filter.isEmpty() && !filter.is(block.getBlock().asItem()) && drops.stream().noneMatch(drop -> drop.is(filter.getItem()))) continue;
            if (!fits(drops)) { cursor--; status = 3; setChanged(); return; }
            if (!level.mayInteract(player, target) || !level.getWorldBorder().isWithinBounds(target) || !Technologia.BREAK_PERMISSION.test(player, target)) {
                status = 8; continue;
            }
            if (level.getBlockState(target) == block && level.destroyBlock(target, false, player)) {
                for (ItemStack drop : drops) insert(drop);
                energy.extract(cost, false); status = 1; setChanged(); return;
            }
        }
        if (cursor >= total) { status = 7; enabled = false; }
        setChanged();
    }
    private boolean fits(List<ItemStack> drops) {
        List<ItemStack> trial = items.stream().map(ItemStack::copy).toList();
        // ArrayList is required: adding to an empty slot must replace the entry.
        var mutable = new ArrayList<>(trial);
        for (ItemStack drop : drops) if (!merge(mutable, drop.copy())) return false;
        return true;
    }
    private void insert(ItemStack stack) { merge(items, stack.copy()); setChanged(); }
    private static boolean merge(List<ItemStack> slots, ItemStack remaining) {
        for (int i = 1; i < slots.size() && !remaining.isEmpty(); i++) {
            ItemStack slot = slots.get(i);
            if (slot.isEmpty()) {
                int count = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                slots.set(i, remaining.copyWithCount(count)); remaining.shrink(count);
            } else if (ItemStack.isSameItemSameComponents(slot, remaining)) {
                int count = Math.min(remaining.getCount(), Math.max(0, slot.getMaxStackSize() - slot.getCount()));
                slot.grow(count); remaining.shrink(count);
            }
        }
        return remaining.isEmpty();
    }
    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Energy", energy.stored()); tag.putInt("Progress", progress);
        tag.putInt("Fuel", fuelTicks); tag.putInt("Cursor", cursor); tag.putBoolean("Enabled", enabled);
        if (owner != null) tag.putUUID("Owner", owner);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear(); ContainerHelper.loadAllItems(tag, items, registries);
        energy.restore(tag.getInt("Energy")); progress = Math.clamp(tag.getInt("Progress"), 0, 12000);
        fuelTicks = Math.clamp(tag.getInt("Fuel"), 0, 1600); cursor = Math.max(0, tag.getInt("Cursor"));
        enabled = tag.getBoolean("Enabled"); owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
    }
    public int getContainerSize() { return items.size(); }
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    public ItemStack getItem(int slot) { return items.get(slot); }
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) { if (slot == 0) progress = 0; setChanged(); }
        return removed;
    }
    public ItemStack removeItemNoUpdate(int slot) { return ContainerHelper.takeItem(items, slot); }
    public void setItem(int slot, ItemStack stack) { items.set(slot, stack); if (slot == 0) progress = 0; setChanged(); }
    public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    public void clearContent() { items.clear(); progress = 0; setChanged(); }
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (kind == MachineKind.CORE) return true;
        if (slot != 0) return false;
        return switch (kind) {
            case GENERATOR -> stack.is(Items.COAL) || stack.is(Items.CHARCOAL);
            case CRUSHER, FURNACE, MINER -> true;
            default -> false;
        };
    }
    public int[] getSlotsForFace(Direction side) { return IntStream.range(0, items.size()).toArray(); }
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return canPlaceItem(slot, stack); }
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return kind == MachineKind.CORE || slot > 0; }
    public Component getDisplayName() { return Component.translatable("block.technologia." + kind.id); }
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return kind == MachineKind.CORE ? new StorageMenu(id, inventory, this) : new MachineMenu(id, inventory, this, data);
    }
}
