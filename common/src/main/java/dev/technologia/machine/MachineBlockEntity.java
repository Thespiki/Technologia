package dev.technologia.machine;

import dev.technologia.Technologia;
import dev.technologia.logistics.EnergyTransport;
import dev.technologia.nature.ResonanceBloomBlock;
import dev.technologia.recipe.MachineRecipe;
import dev.technologia.recipe.MachineRecipeInput;
import dev.technologia.recipe.RecipeIndex;
import dev.technologia.storage.StorageMenu;
import net.minecraft.core.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.TagKey;
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

/**
 * One block entity serves every machine family. The slot layout is fixed for all tiers so an
 * upgrade never has to move items: {@link #INPUT_SLOTS} ingredient slots, then {@link #OUTPUT_SLOTS}
 * result slots. A tier only decides how many ingredient slots are in use.
 */
public final class MachineBlockEntity extends BlockEntity implements WorldlyContainer, MenuProvider {
    /** Saved-data layout version. Alpha.3 wrote no marker and kept outputs directly after the inputs. */
    public static final int FORMAT = 2;
    public static final int INPUT_SLOTS = 16, OUTPUT_SLOTS = 27, FIRST_OUTPUT = INPUT_SLOTS, SLOTS = INPUT_SLOTS + OUTPUT_SLOTS;
    public static final int CORE_SLOTS = 54;
    public static final int DATA_COUNT = 7 + MachineTier.MAX_LANES;
    /** Energy a first-tier machine may move per tick; tiers multiply it. */
    public static final int BASE_TRANSFER = 200;
    public static final int MAX_BLOOMS = 5, BLOOM_BONUS_PERCENT = 8;
    private static final int WORK_UNITS = 100, ALPHA3_SLOTS = 27;
    private static final int[] ONE = {1};
    private static final TagKey<Block> ORES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("c", "ores"));
    private static final TagKey<Block> LEGACY_ORES = TagKey.create(Registries.BLOCK, ResourceLocation.fromNamespaceAndPath("forge", "ores"));

    private static final class Lane {
        int progress, signature, duration = 1;
        ResourceLocation recipe;
        RecipeHolder<?> hint;
    }
    private record Plan(ResourceLocation id, RecipeHolder<?> holder, int[] consumption, List<ItemStack> outputs,
                        float byproductChance, int time, int energy, int signature) {}

    public final MachineKind kind;
    public final EnergyBuffer energy;
    public Object platformEnergy;
    /** Cached conduit route, owned by {@link EnergyTransport}. */
    public EnergyTransport.Route route;
    private final NonNullList<ItemStack> items;
    private final Lane[] lanes = new Lane[MachineTier.MAX_LANES];
    private final int phase;
    private MachineTier tier = MachineTier.get(0);
    private int fuelTicks, cursor, status, boost, activeHold;
    private boolean enabled = true;
    private UUID owner;
    private Object recipeToken;
    private int[] faceSlots;
    private long rateTick = Long.MIN_VALUE;
    private int receivedThisTick, extractedThisTick;

    public final ContainerData data = new ContainerData() {
        public int get(int index) {
            return switch (index) {
                case 0 -> energy.stored() & 65535;
                case 1 -> energy.stored() >>> 16;
                case 2 -> kind.ordinal();
                case 3 -> status;
                case 4 -> enabled ? 1 : 0;
                case 5 -> tier.index();
                case 6 -> boost;
                default -> index < DATA_COUNT ? laneProgress(index - 7) : 0;
            };
        }
        public void set(int index, int value) {}
        public int getCount() { return DATA_COUNT; }
    };

    public MachineBlockEntity(BlockPos pos, BlockState state) {
        super(Technologia.MACHINE_TYPE, pos, state);
        kind = ((MachineBlock) state.getBlock()).kind;
        energy = new EnergyBuffer(Math.max(1, kind.capacity));
        items = NonNullList.withSize(kind == MachineKind.CORE ? CORE_SLOTS : SLOTS, ItemStack.EMPTY);
        for (int i = 0; i < lanes.length; i++) lanes[i] = new Lane();
        phase = Math.floorMod(pos.hashCode(), 8000);
        if (kind == MachineKind.MINER) enabled = false;
    }

    // ---- Tier ----------------------------------------------------------------------------------

    public MachineTier tier() { return tier; }
    public int capacity() { return scaledCapacity(kind, tier); }
    public static int scaledCapacity(MachineKind kind, MachineTier tier) {
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, Math.round(kind.capacity * tier.capacity())));
    }
    /** Energy this machine may send or accept through cables in one tick. */
    public int transferRate() { return scaledTransfer(tier); }
    public static int scaledTransfer(MachineTier tier) { return (int) Math.max(1, Math.min(Integer.MAX_VALUE, Math.round(BASE_TRANSFER * tier.transfer()))); }
    public int inputCount() { return kind.inputCount(tier); }

    /** Installs the next tier in place. Inventory, energy and settings are untouched. */
    public boolean upgradeTo(int index) {
        if (!kind.isTierable() || index != tier.index() + 1 || index >= MachineTier.count()) return false;
        applyTier(MachineTier.get(index));
        setChanged();
        if (level != null) level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        return true;
    }
    private void applyTier(MachineTier value) {
        tier = value;
        if (kind.capacity > 0) energy.resize(capacity());
        faceSlots = null;
        route = null;
    }
    /** Clients only need the tier (for pick-block and future visuals); energy stays server-side. */
    @Override public CompoundTag getUpdateTag(HolderLookup.Provider registries) {
        CompoundTag tag = new CompoundTag();
        tag.putInt("Format", FORMAT);
        tag.putInt("Tier", tier.index());
        return tag;
    }
    @Override public net.minecraft.network.protocol.Packet<net.minecraft.network.protocol.game.ClientGamePacketListener> getUpdatePacket() {
        return net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket.create(this);
    }

    // ---- Ownership and controls ----------------------------------------------------------------

    public void setOwner(UUID id) { owner = id; setChanged(); }
    public boolean mayConfigure(Player player) { return kind != MachineKind.MINER || owner == null || player.getUUID().equals(owner); }
    public boolean isEnabled() { return enabled; }
    public int status() { return status; }
    public void toggle(Player player) {
        if (!mayConfigure(player)) return;
        if (owner == null) owner = player.getUUID();
        enabled = !enabled;
        // Starting a miner whose scan had finished begins a new scan instead of stopping again at once.
        if (enabled && kind == MachineKind.MINER && level != null && cursor >= scanTotal()) cursor = 0;
        if (!enabled) activeHold = 0;
        status = enabled ? 0 : 5;
        setChanged();
    }
    public void rescan(Player player) { if (mayConfigure(player)) { cursor = 0; status = enabled ? 0 : 5; setChanged(); } }

    // ---- Energy --------------------------------------------------------------------------------

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
    private void rollRateWindow() {
        long now = level == null ? 0 : level.getGameTime();
        if (now != rateTick) { rateTick = now; receivedThisTick = 0; extractedThisTick = 0; }
    }
    /** Remaining energy this machine may still send during the current tick. */
    public int sendBudget() {
        rollRateWindow();
        return Math.max(0, Math.min(transferRate() - extractedThisTick, energy.stored()));
    }
    /** Internal routing and other mods' cables share one per-tick send limit. */
    public int send(int amount, boolean simulate) {
        int extracted = extractEnergy(Math.min(amount, sendBudget()), simulate);
        if (!simulate) extractedThisTick += extracted;
        return extracted;
    }
    /** Energy pushed in by another mod's cable, limited per tick rather than per call. */
    public int receiveExternal(int amount, boolean simulate) {
        rollRateWindow();
        int received = receiveEnergy(Math.min(amount, Math.max(0, transferRate() - receivedThisTick)), simulate);
        if (!simulate) receivedThisTick += received;
        return received;
    }

    /** Stored energy plus this tick's transfer counters, for transactional energy APIs. */
    public int[] energySnapshot() { rollRateWindow(); return new int[] {energy.stored(), receivedThisTick, extractedThisTick}; }
    public void restoreEnergySnapshot(int[] snapshot) {
        energy.restore(snapshot[0]); receivedThisTick = snapshot[1]; extractedThisTick = snapshot[2];
    }

    // ---- Tick ----------------------------------------------------------------------------------

    public static void tick(Level level, BlockPos pos, BlockState state, MachineBlockEntity machine) {
        if (machine.kind.capacity == 0) return;
        int previousEnergy = machine.energy.stored();
        long time = level.getGameTime() + machine.phase;
        if (machine.enabled) {
            switch (machine.kind) {
                case GENERATOR, BIOMASS_GENERATOR -> machine.generate();
                case SOLAR_GENERATOR, ADVANCED_SOLAR -> machine.generateSolar();
                case MINER -> { if (level.getGameTime() % 10 == 0) machine.mine((ServerLevel) level); }
                default -> {
                    if (machine.kind.isProcessor()) {
                        if (time % 40 == 0) machine.countBlooms();
                        machine.process();
                        if (time % 20 == 0) { machine.clearUnusedInputs(); machine.balanceLanes(); }
                    } else machine.status = 0;
                }
            }
        } else machine.status = machine.kind == MachineKind.MINER && machine.cursor > 0 && machine.cursor >= machine.scanTotal() ? 7 : 5;
        boolean working = machine.enabled && machine.status == 1;
        if (machine.kind.suppliesEnergy()) {
            EnergyTransport.transfer(machine);
            if (machine.enabled && machine.route != null && machine.route.truncated()) machine.status = 11;
        }
        if (previousEnergy != machine.energy.stored()) machine.setChanged();
        // A short hold keeps the lit front steady when power arrives slower than it is spent.
        if (working) machine.activeHold = 10; else if (machine.activeHold > 0) machine.activeHold--;
        boolean active = machine.enabled && machine.activeHold > 0;
        // KNOWN_SHAPE: a lit front never changes a neighbour, so neighbours are not asked to re-check.
        if (state.getValue(MachineBlock.ACTIVE) != active) level.setBlock(pos, state.setValue(MachineBlock.ACTIVE, active), Block.UPDATE_CLIENTS | Block.UPDATE_KNOWN_SHAPE);
        if (working && time % 80 == 0 && Technologia.BALANCE.machineSounds()) {
            SoundEvent sound = machine.workSound();
            if (sound != null) level.playSound(null, pos, sound, SoundSource.BLOCKS, 0.2F, 0.9F + level.random.nextFloat() * 0.2F);
        }
    }

    private SoundEvent workSound() {
        return switch (kind) {
            case GENERATOR, BIOMASS_GENERATOR -> SoundEvents.FURNACE_FIRE_CRACKLE;
            case FURNACE, ALLOY_SMELTER -> SoundEvents.BLASTFURNACE_FIRE_CRACKLE;
            case CRUSHER, RECYCLER -> SoundEvents.GRINDSTONE_USE;
            case METAL_PRESS -> SoundEvents.PISTON_EXTEND;
            case COMPACTOR -> SoundEvents.PISTON_CONTRACT;
            case SAWMILL -> SoundEvents.UI_STONECUTTER_TAKE_RESULT;
            case CENTRIFUGE -> SoundEvents.BEACON_AMBIENT;
            case SIEVE -> SoundEvents.SAND_BREAK;
            default -> null;
        };
    }

    private int generationPerTick() {
        int base = switch (kind) {
            case BIOMASS_GENERATOR -> Math.max(1, Technologia.BALANCE.generatorPerTick() / 2);
            case SOLAR_GENERATOR -> 16;
            case ADVANCED_SOLAR -> 64;
            default -> Technologia.BALANCE.generatorPerTick();
        };
        return (int) Math.max(1, Math.round(base * tier.generation()));
    }
    private void generate() {
        int output = generationPerTick();
        // Burn only when a whole tick of output fits, so no fuel is spent on clipped energy.
        if (energy.capacity() - energy.stored() < Math.min(output, energy.capacity())) { status = 0; return; }
        if (fuelTicks == 0) {
            int burn = fuelDuration(items.getFirst());
            if (burn == 0) { status = 4; return; }
            items.getFirst().shrink(1); fuelTicks = burn;
        }
        energy.receive(output, false);
        fuelTicks--; status = 1; setChanged();
    }
    private void generateSolar() {
        if (energy.stored() == energy.capacity()) { status = 0; return; }
        if (!level.dimensionType().hasSkyLight() || !level.isDay() || level.isRaining()
                || level.getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.MOTION_BLOCKING,
                        worldPosition.getX(), worldPosition.getZ()) > worldPosition.getY() + 1
                || !level.canSeeSky(worldPosition.above())) { status = 10; return; }
        energy.receive(generationPerTick(), false); status = 1; setChanged();
    }
    public int fuelDuration(ItemStack stack) {
        if (kind == MachineKind.GENERATOR) return stack.is(Items.COAL) || stack.is(Items.CHARCOAL) ? 1600 : 0;
        if (kind != MachineKind.BIOMASS_GENERATOR) return 0;
        if (stack.is(Technologia.ITEMS.get("sawdust"))) return 200;
        if (stack.is(net.minecraft.tags.ItemTags.SAPLINGS)) return 100;
        if (stack.is(Items.WHEAT) || stack.is(Items.SUGAR_CANE) || stack.is(Items.KELP)) return 80;
        return 0;
    }

    // ---- Processing ----------------------------------------------------------------------------

    private double speedFactor() { return tier.speed() * (100 + boost * BLOOM_BONUS_PERCENT) / 100.0; }
    private int workPerTick() { return (int) Math.max(1, Math.round(WORK_UNITS * speedFactor())); }
    /** Faster work costs proportionally more per tick; efficiency lowers the cost of a whole operation. */
    private int costPerTick(int recipeEnergy) {
        return (int) Math.max(1, Math.min(Integer.MAX_VALUE, Math.round(recipeEnergy * speedFactor() * tier.efficiency())));
    }
    private int laneProgress(int lane) {
        if (lane < 0 || lane >= lanes.length || lanes[lane].recipe == null) return 0;
        return (int) Math.min(1000L, 1000L * lanes[lane].progress / Math.max(1, lanes[lane].duration * WORK_UNITS));
    }
    public int boost() { return boost; }

    private void countBlooms() {
        int found = 0;
        for (BlockPos near : BlockPos.betweenClosed(worldPosition.offset(-2, -2, -2), worldPosition.offset(2, 2, 2))) {
            if (!level.hasChunkAt(near)) continue;
            if (level.getBlockState(near).getBlock() instanceof ResonanceBloomBlock && ++found >= MAX_BLOOMS) break;
        }
        if (found != boost) { boost = found; setChanged(); }
    }

    private void process() {
        Object token = RecipeIndex.token(level);
        if (token != recipeToken) { recipeToken = token; for (Lane lane : lanes) lane.hint = null; }
        int count = kind.lanes(tier), result = 4;
        boolean working = false;
        for (int lane = 0; lane < count; lane++) {
            int state = processLane(lane);
            if (state == 1) working = true;
            else if (rank(state) > rank(result)) result = state;
        }
        status = working ? 1 : result;
    }
    /** When no lane works, the most actionable reason is shown: power, then space, then bad input. */
    private static int rank(int state) { return switch (state) { case 2 -> 4; case 3 -> 3; case 12 -> 2; default -> 1; }; }

    private int processLane(int lane) {
        int perLane = kind.inputsPerLane(), first = lane * perLane;
        Lane state = lanes[lane];
        boolean empty = true;
        for (int i = 0; i < perLane; i++) if (!items.get(first + i).isEmpty()) empty = false;
        if (empty) { resetLane(lane); return 4; }
        Plan plan = findPlan(lane);
        if (plan == null) { resetLane(lane); return rejectUnsupported(lane); }
        if (!plan.id().equals(state.recipe) || state.signature != plan.signature()) {
            state.progress = 0; state.recipe = plan.id(); state.signature = plan.signature(); setChanged();
        }
        state.hint = plan.holder(); state.duration = plan.time();
        // Check the complete result and byproduct together before spending any resource.
        if (!fits(plan.outputs())) return 3;
        // The last tick of an operation only pays for the work it still needs.
        int perTick = workPerTick(), work = Math.min(perTick, plan.time() * WORK_UNITS - state.progress);
        int cost = Math.min(energy.capacity(), (int) Math.max(1, Math.ceil((double) costPerTick(plan.energy()) * work / perTick)));
        if (energy.stored() < cost) return 2;
        energy.extract(cost, false);
        state.progress += work;
        if (state.progress >= plan.time() * WORK_UNITS) {
            for (int i = 0; i < plan.outputs().size(); i++) {
                if (i == 1 && plan.byproductChance() < 1 && level.random.nextFloat() >= plan.byproductChance()) continue;
                insert(plan.outputs().get(i));
            }
            for (int i = 0; i < plan.consumption().length; i++) items.get(first + i).shrink(plan.consumption()[i]);
            state.progress = 0; state.recipe = null; state.signature = 0;
        }
        setChanged();
        return 1;
    }

    @SuppressWarnings("unchecked")
    private Plan findPlan(int lane) {
        int perLane = kind.inputsPerLane(), first = lane * perLane;
        RecipeHolder<?> hint = lanes[lane].hint;
        RecipeManager manager = level.getRecipeManager();
        if (kind == MachineKind.FURNACE) {
            var input = new SingleRecipeInput(items.get(first));
            var found = manager.getRecipeFor(RecipeType.SMELTING, input, level,
                    hint != null && hint.value() instanceof SmeltingRecipe ? (RecipeHolder<SmeltingRecipe>) hint : null);
            if (found.isEmpty()) return null;
            ItemStack result = found.get().value().assemble(input, level.registryAccess());
            if (result.isEmpty()) return null;
            int time = Technologia.BALANCE.processingTicks(), cost = Technologia.BALANCE.machineEnergyPerTick();
            return new Plan(found.get().id(), found.get(), ONE, List.of(result), 1, time, cost, signature(time, cost, ONE, List.of(result), 1));
        }
        var input = new MachineRecipeInput(kind, items.subList(first, first + perLane));
        var found = manager.getRecipeFor(MachineRecipe.TYPE, input, level,
                hint != null && hint.value() instanceof MachineRecipe ? (RecipeHolder<MachineRecipe>) hint : null);
        if (found.isEmpty()) return null;
        MachineRecipe recipe = found.get().value();
        int[] consumption = recipe.consumption(input);
        List<ItemStack> outputs = recipe.outputs();
        if (consumption.length != perLane || outputs.stream().anyMatch(ItemStack::isEmpty)) return null;
        return new Plan(found.get().id(), found.get(), consumption, outputs, recipe.byproductChance(), recipe.time(), recipe.energy(),
                signature(recipe.time(), recipe.energy(), consumption, outputs, recipe.byproductChance()));
    }
    /** Stable across restarts, so paid progress survives a save but not a changed recipe. */
    private static int signature(int time, int energy, int[] consumption, List<ItemStack> outputs, float chance) {
        int hash = 31 * (31 * time + energy) + Arrays.hashCode(consumption);
        for (ItemStack output : outputs) hash = 31 * (31 * hash + BuiltInRegistries.ITEM.getKey(output.getItem()).hashCode()) + output.getCount();
        return 31 * hash + Float.floatToIntBits(chance);
    }
    private void resetLane(int lane) {
        Lane state = lanes[lane];
        if (state.progress != 0 || state.recipe != null) setChanged();
        state.progress = 0; state.recipe = null; state.signature = 0;
    }
    private void resetLaneOfSlot(int slot) {
        if (!kind.isProcessor() || slot < 0 || slot >= INPUT_SLOTS) return;
        int lane = slot / kind.inputsPerLane();
        if (lane < lanes.length) resetLane(lane);
    }

    /**
     * Items a machine can never process are moved to the result slots, where automation can take
     * them back. A lane waiting for more of a valid ingredient is left alone.
     */
    private int rejectUnsupported(int lane) {
        int perLane = kind.inputsPerLane(), first = lane * perLane, result = 4;
        for (int i = 0; i < perLane; i++) {
            ItemStack stack = items.get(first + i);
            if (stack.isEmpty()) continue;
            boolean supported = RecipeIndex.isIngredient(level, kind, stack);
            if (supported && perLane == 2 && i == 1) {
                ItemStack partner = items.get(first);
                if (!partner.isEmpty() && RecipeIndex.isIngredient(level, kind, partner) && !RecipeIndex.isPair(level, kind, partner, stack)) supported = false;
            }
            if (supported) continue;
            ItemStack remaining = stack.copy();
            merge(items, remaining, FIRST_OUTPUT, SLOTS);
            items.set(first + i, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
            if (!remaining.isEmpty()) result = 12;
            setChanged();
        }
        return result;
    }

    /**
     * Ingredient slots beyond the tier's lanes are hidden and unreachable. If tier data ever shrinks,
     * anything left in them moves to the results instead of being stranded.
     */
    private void clearUnusedInputs() {
        for (int slot = inputCount(); slot < INPUT_SLOTS; slot++) {
            ItemStack stack = items.get(slot);
            if (stack.isEmpty()) continue;
            ItemStack remaining = stack.copy();
            merge(items, remaining, FIRST_OUTPUT, SLOTS);
            items.set(slot, remaining.isEmpty() ? ItemStack.EMPTY : remaining);
            setChanged();
        }
    }

    /** Spreads a large stack into an idle lane so extra lanes work without sorted input. */
    private void balanceLanes() {
        int count = kind.lanes(tier), perLane = kind.inputsPerLane();
        if (count < 2) return;
        int target = -1;
        for (int lane = 0; lane < count && target < 0; lane++) {
            boolean empty = true;
            for (int i = 0; i < perLane; i++) if (!items.get(lane * perLane + i).isEmpty()) empty = false;
            if (empty) target = lane;
        }
        if (target < 0) return;
        for (int lane = 0; lane < count; lane++) {
            if (lane == target) continue;
            Plan plan = findPlan(lane);
            if (plan == null) continue;
            // Move whole operations only, so neither lane is left with a remainder it can never use.
            int operations = Integer.MAX_VALUE;
            for (int i = 0; i < perLane; i++) operations = Math.min(operations, items.get(lane * perLane + i).getCount() / plan.consumption()[i]);
            if (operations < 2) continue;
            for (int i = 0; i < perLane; i++) {
                ItemStack source = items.get(lane * perLane + i);
                items.set(target * perLane + i, source.split(operations / 2 * plan.consumption()[i]));
            }
            setChanged();
            return;
        }
    }

    // ---- Mining --------------------------------------------------------------------------------

    private int scanTotal() {
        if (level == null) return Integer.MAX_VALUE;
        int width = Technologia.BALANCE.minerRadius() * 2 + 1;
        int depth = Math.min(Technologia.BALANCE.minerDepth(), worldPosition.getY() - level.getMinBuildHeight());
        return width * width * Math.max(0, depth);
    }
    private void mine(ServerLevel level) {
        if (!Technologia.BALANCE.minerEnabled()) { status = 5; return; }
        var player = owner == null ? null : level.getServer().getPlayerList().getPlayer(owner);
        if (player == null || player.level() != level || player.isSpectator() || !player.mayBuild()) { status = 6; return; }
        int cost = (int) Math.max(1, Math.min(energy.capacity(), Math.round(Technologia.BALANCE.minerEnergyPerBlock() * tier.efficiency())));
        if (energy.stored() < cost) { status = 2; return; }
        int radius = Technologia.BALANCE.minerRadius(), width = radius * 2 + 1, total = scanTotal();
        int positions = (int) Math.min(512, Math.round(32 * tier.speed())), blocks = (int) Math.max(1, Math.round(tier.speed()));
        ItemStack tool = new ItemStack(Items.IRON_PICKAXE);
        // Scanning counts as working; a stale "paused" or "needs power" line must not linger.
        status = 1;
        for (int budget = 0; budget < positions && cursor < total && blocks > 0; budget++) {
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
                energy.extract(cost, false); blocks--;
                if (energy.stored() < cost) break;
            }
        }
        if (cursor >= total) { status = 7; enabled = false; activeHold = 0; }
        setChanged();
    }

    // ---- Output space --------------------------------------------------------------------------

    /** Simulates {@link #insert} for every stack at once without copying the inventory. */
    private boolean fits(List<ItemStack> stacks) {
        ItemStack[] held = new ItemStack[OUTPUT_SLOTS];
        int[] counts = new int[OUTPUT_SLOTS];
        for (int i = 0; i < OUTPUT_SLOTS; i++) { held[i] = items.get(FIRST_OUTPUT + i); counts[i] = held[i].getCount(); }
        for (ItemStack stack : stacks) {
            int remaining = stack.getCount();
            for (int i = 0; i < OUTPUT_SLOTS && remaining > 0; i++) {
                if (counts[i] == 0) {
                    int moved = Math.min(remaining, stack.getMaxStackSize());
                    held[i] = stack; counts[i] = moved; remaining -= moved;
                } else if (ItemStack.isSameItemSameComponents(held[i], stack)) {
                    int moved = Math.min(remaining, Math.max(0, held[i].getMaxStackSize() - counts[i]));
                    counts[i] += moved; remaining -= moved;
                }
            }
            if (remaining > 0) return false;
        }
        return true;
    }
    private void insert(ItemStack stack) { merge(items, stack.copy(), FIRST_OUTPUT, SLOTS); setChanged(); }
    /** Fills slots in order; whatever does not fit is left in {@code remaining}. */
    private static void merge(List<ItemStack> slots, ItemStack remaining, int from, int to) {
        for (int i = from; i < to && !remaining.isEmpty(); i++) {
            ItemStack slot = slots.get(i);
            if (slot.isEmpty()) {
                int count = Math.min(remaining.getCount(), remaining.getMaxStackSize());
                slots.set(i, remaining.copyWithCount(count)); remaining.shrink(count);
            } else if (ItemStack.isSameItemSameComponents(slot, remaining)) {
                int count = Math.min(remaining.getCount(), Math.max(0, slot.getMaxStackSize() - slot.getCount()));
                slot.grow(count); remaining.shrink(count);
            }
        }
    }

    // ---- Persistence ---------------------------------------------------------------------------

    @Override protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        ContainerHelper.saveAllItems(tag, items, registries);
        tag.putInt("Format", FORMAT);
        tag.putInt("Tier", tier.index());
        tag.putInt("Energy", energy.stored());
        tag.putInt("Fuel", fuelTicks); tag.putInt("Cursor", cursor); tag.putBoolean("Enabled", enabled);
        ListTag saved = new ListTag();
        for (int i = 0; i < lanes.length; i++) {
            if (lanes[i].recipe == null) continue;
            CompoundTag lane = new CompoundTag();
            lane.putInt("Lane", i); lane.putString("Recipe", lanes[i].recipe.toString());
            lane.putInt("Progress", lanes[i].progress); lane.putInt("Signature", lanes[i].signature); lane.putInt("Duration", lanes[i].duration);
            saved.add(lane);
        }
        if (!saved.isEmpty()) tag.put("Lanes", saved);
        if (owner != null) tag.putUUID("Owner", owner);
    }
    @Override protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        items.clear(); ContainerHelper.loadAllItems(tag, items, registries);
        applyTier(kind.isTierable() ? MachineTier.get(tag.getInt("Tier")) : MachineTier.get(0));
        energy.restore(tag.getInt("Energy"));
        fuelTicks = Math.clamp(tag.getInt("Fuel"), 0, 100000); cursor = Math.max(0, tag.getInt("Cursor"));
        enabled = tag.contains("Enabled") ? tag.getBoolean("Enabled") : kind != MachineKind.MINER;
        owner = tag.hasUUID("Owner") ? tag.getUUID("Owner") : null;
        for (Lane lane : lanes) { lane.progress = 0; lane.recipe = null; lane.signature = 0; lane.hint = null; lane.duration = 1; }
        if (tag.getInt("Format") < FORMAT) { migrateAlpha3(); return; }
        ListTag saved = tag.getList("Lanes", Tag.TAG_COMPOUND);
        for (int i = 0; i < saved.size(); i++) {
            CompoundTag lane = saved.getCompound(i);
            int index = lane.getInt("Lane");
            ResourceLocation recipe = ResourceLocation.tryParse(lane.getString("Recipe"));
            if (index < 0 || index >= lanes.length || recipe == null || lane.getString("Recipe").isEmpty()) continue;
            lanes[index].recipe = recipe; lanes[index].signature = lane.getInt("Signature");
            lanes[index].duration = Math.clamp(lane.getInt("Duration"), 1, 12000);
            lanes[index].progress = Math.clamp(lane.getInt("Progress"), 0, 12000 * WORK_UNITS);
        }
    }
    /**
     * Alpha.3 stored results directly after the one or two inputs of a 27-slot inventory. They move
     * to the fixed result region, which is large enough for every old result slot.
     */
    private void migrateAlpha3() {
        if (kind == MachineKind.CORE) return;
        int oldInputs = kind == MachineKind.ALLOY_SMELTER ? 2 : kind.isProcessor() || kind.isFuelGenerator() || kind == MachineKind.MINER ? 1 : 0;
        List<ItemStack> results = new ArrayList<>();
        for (int slot = oldInputs; slot < ALPHA3_SLOTS; slot++) {
            if (!items.get(slot).isEmpty()) results.add(items.get(slot));
            items.set(slot, ItemStack.EMPTY);
        }
        for (int i = 0; i < results.size(); i++) items.set(FIRST_OUTPUT + i, results.get(i));
    }

    /** Tier and stored energy travel with the dropped block. */
    public void saveToItem(ItemStack stack) {
        CompoundTag tag = new CompoundTag();
        if (!tier.isBase()) tag.putInt("Tier", tier.index());
        if (energy.stored() > 0 && kind.capacity > 0) tag.putInt("Energy", energy.stored());
        BlockItem.setBlockEntityData(stack, Technologia.MACHINE_TYPE, tag);
    }

    // ---- Container -----------------------------------------------------------------------------

    public int getContainerSize() { return items.size(); }
    public boolean isEmpty() { return items.stream().allMatch(ItemStack::isEmpty); }
    public ItemStack getItem(int slot) { return items.get(slot); }
    public ItemStack removeItem(int slot, int amount) {
        ItemStack removed = ContainerHelper.removeItem(items, slot, amount);
        if (!removed.isEmpty()) { if (items.get(slot).isEmpty()) resetLaneOfSlot(slot); setChanged(); }
        return removed;
    }
    public ItemStack removeItemNoUpdate(int slot) { ItemStack removed = ContainerHelper.takeItem(items, slot); resetLaneOfSlot(slot); return removed; }
    public void setItem(int slot, ItemStack stack) {
        if (!ItemStack.isSameItemSameComponents(items.get(slot), stack)) resetLaneOfSlot(slot);
        items.set(slot, stack); setChanged();
    }
    public boolean stillValid(Player player) { return Container.stillValidBlockEntity(this, player); }
    public void clearContent() { items.clear(); for (int i = 0; i < lanes.length; i++) resetLane(i); setChanged(); }

    /** Machines refuse what they cannot use, so a wrong item never blocks a production line. */
    public boolean canPlaceItem(int slot, ItemStack stack) {
        if (kind == MachineKind.CORE) return true;
        if (slot < 0 || slot >= inputCount()) return false;
        if (kind.isFuelGenerator()) return fuelDuration(stack) > 0;
        if (kind == MachineKind.MINER) return true;
        if (level == null) return true;
        if (!RecipeIndex.isIngredient(level, kind, stack)) return false;
        if (kind.inputsPerLane() == 2 && !ItemStack.isSameItemSameComponents(items.get(slot), stack)) {
            ItemStack partner = items.get(slot ^ 1);
            if (!partner.isEmpty() && !RecipeIndex.isPair(level, kind, partner, stack)) return false;
        }
        return true;
    }
    public int[] getSlotsForFace(Direction side) {
        if (faceSlots == null) {
            if (kind == MachineKind.CORE) faceSlots = IntStream.range(0, items.size()).toArray();
            else faceSlots = IntStream.concat(IntStream.range(0, inputCount()),
                    kind.hasOutput() ? IntStream.range(FIRST_OUTPUT, SLOTS) : IntStream.empty()).toArray();
        }
        return faceSlots;
    }
    /** The miner's filter is a setting: only a player places it, never a hopper. */
    public boolean canPlaceItemThroughFace(int slot, ItemStack stack, Direction side) { return kind != MachineKind.MINER && canPlaceItem(slot, stack); }
    public boolean canTakeItemThroughFace(int slot, ItemStack stack, Direction side) { return kind == MachineKind.CORE || kind.hasOutput() && slot >= FIRST_OUTPUT; }
    public Component getDisplayName() { return Component.translatable("block.technologia." + kind.id); }
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return kind == MachineKind.CORE ? new StorageMenu(id, inventory, this) : new MachineMenu(id, inventory, this, data);
    }
}
