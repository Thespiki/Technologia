package dev.technologia.machine;

import dev.technologia.Technologia;
import dev.technologia.storage.StorageMenu;
import dev.technologia.recipe.MachineRecipe;
import dev.technologia.recipe.MachineRecipeInput;
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
    private int recipeDuration = 1, recipeEnergy;
    private ResourceLocation activeRecipe;
    private Recipe<?> liveRecipe;
    private final NonNullList<ItemStack> processingInputs = NonNullList.withSize(2, ItemStack.EMPTY);
    private final NonNullList<ItemStack> processingOutputs = NonNullList.withSize(2, ItemStack.EMPTY);
    private int[] processingConsumption = new int[0];
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
                case 6 -> recipeDuration;
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
        if (!kind.acceptsEnergy()) return 0;
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
                case GENERATOR, BIOMASS_GENERATOR -> machine.generate();
                case SOLAR_GENERATOR, ADVANCED_SOLAR -> machine.generateSolar();
                case CRUSHER, FURNACE, ALLOY_SMELTER, METAL_PRESS, SAWMILL, COMPACTOR, CENTRIFUGE, RECYCLER -> machine.process();
                case MINER -> { if (level.getGameTime() % 10 == 0) machine.mine((ServerLevel) level); }
                default -> machine.status = 0;
            }
        } else if (machine.status != 7) machine.status = 5;
        if (machine.kind.suppliesEnergy()) dev.technologia.logistics.EnergyTransport.transfer(machine);
        if (previousEnergy != machine.energy.stored()) machine.setChanged();
        boolean active = machine.enabled && machine.status == 1;
        if (state.getValue(MachineBlock.ACTIVE) != active) {
            level.setBlock(pos, state.setValue(MachineBlock.ACTIVE, active), Block.UPDATE_CLIENTS);
        }
    }
    private void generate() {
        if (energy.stored() == energy.capacity()) { status = 0; return; }
        if (fuelTicks == 0) {
            int burn = fuelDuration(items.getFirst());
            if (burn == 0) { status = 4; return; }
            items.getFirst().shrink(1); fuelTicks = burn; setChanged();
        }
        energy.receive(kind == MachineKind.BIOMASS_GENERATOR ? Math.max(1, Technologia.BALANCE.generatorPerTick() / 2) : Technologia.BALANCE.generatorPerTick(), false);
        fuelTicks--; status = 1; setChanged();
    }
    private void generateSolar() {
        if (energy.stored() == energy.capacity()) { status = 0; return; }
        if (!level.dimensionType().hasSkyLight() || !level.isDay() || level.isRaining()
                || level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        worldPosition.getX(), worldPosition.getZ()) > worldPosition.getY() + 1
                || !level.canSeeSky(worldPosition.above())) { status = 10; return; }
        energy.receive(kind == MachineKind.ADVANCED_SOLAR ? 64 : 16, false); status = 1; setChanged();
    }
    public int fuelDuration(ItemStack stack) {
        if (kind == MachineKind.GENERATOR) return stack.is(Items.COAL) || stack.is(Items.CHARCOAL) ? 1600 : 0;
        if (kind != MachineKind.BIOMASS_GENERATOR) return 0;
        if (stack.is(Technologia.ITEMS.get("sawdust"))) return 200;
        if (stack.is(net.minecraft.tags.ItemTags.SAPLINGS)) return 100;
        if (stack.is(Items.WHEAT) || stack.is(Items.SUGAR_CANE) || stack.is(Items.KELP)) return 80;
        return 0;
    }
    private record ProcessPlan(ResourceLocation id, Recipe<?> recipe, int[] consumption, List<ItemStack> outputs, int time, int energy) {}
    private ProcessPlan findProcess() {
        if (items.getFirst().isEmpty()) return null;
        if (kind == MachineKind.FURNACE) {
            var input = new SingleRecipeInput(items.getFirst());
            return level.getRecipeManager().getRecipeFor(RecipeType.SMELTING, input, level)
                    .map(holder -> new ProcessPlan(holder.id(), holder.value(), new int[]{1},
                            List.of(holder.value().assemble(input, level.registryAccess())),
                            Technologia.BALANCE.processingTicks(), Technologia.BALANCE.machineEnergyPerTick())).orElse(null);
        }
        var input = new MachineRecipeInput(kind, items.subList(0, kind.inputCount()));
        return level.getRecipeManager().getRecipeFor(MachineRecipe.TYPE, input, level)
                .map(holder -> new ProcessPlan(holder.id(), holder.value(), holder.value().consumption(input),
                        holder.value().outputs(), holder.value().time(), holder.value().energy())).orElse(null);
    }
    private boolean sameProcess(ProcessPlan plan) {
        if (!plan.id().equals(activeRecipe) || (liveRecipe != null && liveRecipe != plan.recipe())
                || recipeDuration != plan.time() || recipeEnergy != plan.energy()
                || !Arrays.equals(processingConsumption, plan.consumption())) return false;
        for (int i = 0; i < kind.inputCount(); i++) if (!ItemStack.isSameItemSameComponents(items.get(i), processingInputs.get(i))) return false;
        for (int i = 0; i < 2; i++) {
            ItemStack expected = i < plan.outputs().size() ? plan.outputs().get(i) : ItemStack.EMPTY;
            if (!ItemStack.matches(expected, processingOutputs.get(i))) return false;
        }
        return true;
    }
    private void selectProcess(ProcessPlan plan) {
        if (!sameProcess(plan)) { progress = 0; setChanged(); }
        activeRecipe = plan.id(); liveRecipe = plan.recipe(); recipeDuration = plan.time(); recipeEnergy = plan.energy();
        processingConsumption = plan.consumption().clone();
        for (int i = 0; i < 2; i++) {
            processingInputs.set(i, i < kind.inputCount() ? items.get(i).copyWithCount(1) : ItemStack.EMPTY);
            processingOutputs.set(i, i < plan.outputs().size() ? plan.outputs().get(i).copy() : ItemStack.EMPTY);
        }
    }
    private void resetProcess() {
        if (progress != 0 || activeRecipe != null) setChanged();
        progress = 0; activeRecipe = null; liveRecipe = null;
        processingInputs.clear(); processingOutputs.clear(); processingConsumption = new int[0];
    }
    private void process() {
        ProcessPlan plan = findProcess();
        if (plan == null || plan.outputs().stream().anyMatch(ItemStack::isEmpty)) { status = 4; resetProcess(); return; }
        selectProcess(plan);
        // Check the complete result and byproduct together before spending any resource.
        if (!fits(plan.outputs())) { status = 3; return; }
        if (energy.stored() < plan.energy()) { status = 2; return; }
        energy.extract(plan.energy(), false); progress++; status = 1;
        if (progress >= plan.time()) {
            plan.outputs().forEach(this::insert);
            for (int i = 0; i < plan.consumption().length; i++) items.get(i).shrink(plan.consumption()[i]);
            resetProcess();
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
        for (ItemStack drop : drops) if (!merge(mutable, drop.copy(), kind.inputCount())) return false;
        return true;
    }
    private void insert(ItemStack stack) { merge(items, stack.copy(), kind.inputCount()); setChanged(); }
    private static boolean merge(List<ItemStack> slots, ItemStack remaining, int firstOutput) {
        for (int i = firstOutput; i < slots.size() && !remaining.isEmpty(); i++) {
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
        if (activeRecipe != null) {
            CompoundTag processing = new CompoundTag();
            processing.putString("Recipe", activeRecipe.toString()); processing.putInt("Time", recipeDuration); processing.putInt("Cost", recipeEnergy);
            processing.putIntArray("Consumption", processingConsumption);
            CompoundTag inputs = new CompoundTag(), outputs = new CompoundTag();
            ContainerHelper.saveAllItems(inputs, processingInputs, registries); ContainerHelper.saveAllItems(outputs, processingOutputs, registries);
            processing.put("Inputs", inputs); processing.put("Outputs", outputs); tag.put("Processing", processing);
        }
        if (owner != null) tag.putUUID("Owner", owner);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear(); ContainerHelper.loadAllItems(tag, items, registries);
        energy.restore(tag.getInt("Energy")); progress = Math.clamp(tag.getInt("Progress"), 0, 12000);
        fuelTicks = Math.clamp(tag.getInt("Fuel"), 0, 1600); cursor = Math.max(0, tag.getInt("Cursor"));
        enabled = tag.getBoolean("Enabled"); owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        CompoundTag processing = tag.getCompound("Processing"); liveRecipe = null;
        activeRecipe = ResourceLocation.tryParse(processing.getString("Recipe"));
        recipeDuration = Math.clamp(processing.getInt("Time"), 1, 12000); recipeEnergy = Math.clamp(processing.getInt("Cost"), 1, 100000);
        processingConsumption = processing.getIntArray("Consumption"); processingInputs.clear(); processingOutputs.clear();
        ContainerHelper.loadAllItems(processing.getCompound("Inputs"), processingInputs, registries);
        ContainerHelper.loadAllItems(processing.getCompound("Outputs"), processingOutputs, registries);
    }
    public int getContainerSize() { return items.size(); }
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    public ItemStack getItem(int slot) { return items.get(slot); }
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) { if (slot < kind.inputCount() && items.get(slot).isEmpty()) resetProcess(); setChanged(); }
        return removed;
    }
    public ItemStack removeItemNoUpdate(int slot) { ItemStack removed = ContainerHelper.takeItem(items, slot); if (slot < kind.inputCount()) resetProcess(); return removed; }
    public void setItem(int slot, ItemStack stack) {
        if (slot < kind.inputCount() && !ItemStack.isSameItemSameComponents(items.get(slot), stack)) resetProcess();
        items.set(slot, stack); setChanged();
    }
    public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    public void clearContent() { items.clear(); resetProcess(); setChanged(); }
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (kind == MachineKind.CORE) return true;
        if (slot < 0 || slot >= kind.inputCount()) return false;
        return switch (kind) {
            case GENERATOR, BIOMASS_GENERATOR -> fuelDuration(stack) > 0;
            case CRUSHER, FURNACE, MINER, ALLOY_SMELTER, METAL_PRESS, SAWMILL, COMPACTOR, CENTRIFUGE, RECYCLER -> true;
            default -> false;
        };
    }
    public int[] getSlotsForFace(Direction side) { return IntStream.range(0, items.size()).toArray(); }
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return canPlaceItem(slot, stack); }
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return kind == MachineKind.CORE || kind.hasOutput() && slot >= kind.inputCount(); }
    public Component getDisplayName() { return Component.translatable("block.technologia." + kind.id); }
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return kind == MachineKind.CORE ? new StorageMenu(id, inventory, this) : new MachineMenu(id, inventory, this, data);
    }
}
