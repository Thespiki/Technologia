package dev.technologia.machine;

import dev.technologia.Technologia;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AttachedStemBlock;
import net.minecraft.world.level.block.BambooSaplingBlock;
import net.minecraft.world.level.block.BambooStalkBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CactusBlock;
import net.minecraft.world.level.block.CocoaBlock;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.MangrovePropaguleBlock;
import net.minecraft.world.level.block.NetherWartBlock;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.SugarCaneBlock;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Area machines: the harvester, the growth accelerator and the vacuum collector. Each works on a
 * square around the machine, three blocks tall, whose reach grows with the tier.
 */
final class FarmWork {
    static final int HARVEST_ENERGY = 40, GROWTH_ENERGY = 15, COLLECT_ENERGY = 4;
    /** Passes without a plant to grow (one every five ticks) before the accelerator shows as idle. */
    static final int IDLE_PASSES = 4;
    /** What a ripe plant gives and what is left standing; {@code after} is null when the block is removed. */
    record Harvest(List<ItemStack> drops, BlockState after) {}

    private static int cost(MachineBlockEntity machine, int base) {
        return (int) Math.max(1, Math.round(base * machine.tier().efficiency()));
    }
    private static BlockPos target(MachineBlockEntity machine, int index, int reach) {
        int width = reach * 2 + 1;
        // Top layer first, so tall plants are cut from above.
        return machine.getBlockPos().offset(index % width - reach, 1 - index / (width * width), (index / width) % width - reach);
    }

    /** Harvests ripe crops and replants them. Like the miner, it acts for its owner, who must be present. */
    static void harvest(MachineBlockEntity machine, ServerLevel level) {
        var player = machine.owner() == null ? null : level.getServer().getPlayerList().getPlayer(machine.owner());
        if (player == null || player.level() != level || player.isSpectator() || !player.mayBuild()) { machine.status(6); return; }
        int cost = cost(machine, HARVEST_ENERGY);
        if (machine.energy.stored() < cost) { machine.status(2); return; }
        int reach = machine.reach(), width = reach * 2 + 1, total = width * width * 3;
        machine.status(0);
        for (int looked = 0, budget = 24 + 12 * machine.tier().index(); looked < budget; looked++) {
            if (machine.cursor >= total || machine.cursor < 0) machine.cursor = 0;
            BlockPos target = target(machine, machine.cursor++, reach);
            if (target.equals(machine.getBlockPos()) || !level.hasChunkAt(target)) continue;
            BlockState state = level.getBlockState(target);
            Harvest harvest = plan(level, target, state);
            if (harvest == null) continue;
            if (!machine.fits(harvest.drops())) { machine.cursor--; machine.status(3); return; }
            if (!level.mayInteract(player, target) || !level.getWorldBorder().isWithinBounds(target) || !Technologia.BREAK_PERMISSION.test(player, target)) {
                machine.status(8);
                continue;
            }
            if (harvest.after() == null) level.destroyBlock(target, false, player);
            else level.setBlock(target, harvest.after(), Block.UPDATE_ALL);
            for (ItemStack drop : harvest.drops()) machine.insert(drop);
            machine.energy.extract(cost, false);
            machine.status(1);
            if (machine.energy.stored() < cost) break;
        }
        machine.setChanged();
    }

    /** The plan for one block, or null when there is nothing ripe to take. */
    static Harvest plan(ServerLevel level, BlockPos pos, BlockState state) {
        Block block = state.getBlock();
        if (block instanceof CropBlock crop) {
            if (!crop.isMaxAge(state)) return null;
            List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, null));
            // One seed goes back into the ground; without a seed in the drops the plant is simply removed.
            return new Harvest(drops, takeOne(drops, crop.getCloneItemStack(level, pos, state).getItem()) ? crop.getStateForAge(0) : null);
        }
        if (block instanceof NetherWartBlock) {
            if (state.getValue(NetherWartBlock.AGE) < NetherWartBlock.MAX_AGE) return null;
            List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, null));
            return new Harvest(drops, takeOne(drops, Items.NETHER_WART) ? state.setValue(NetherWartBlock.AGE, 0) : null);
        }
        if (block instanceof CocoaBlock) {
            if (state.getValue(CocoaBlock.AGE) < CocoaBlock.MAX_AGE) return null;
            List<ItemStack> drops = new ArrayList<>(Block.getDrops(state, level, pos, null));
            return new Harvest(drops, takeOne(drops, Items.COCOA_BEANS) ? state.setValue(CocoaBlock.AGE, 0) : null);
        }
        if (block instanceof SweetBerryBushBlock) {
            int age = state.getValue(SweetBerryBushBlock.AGE);
            if (age < 2) return null;
            // Same yield as picking by hand; the bush stays.
            return new Harvest(List.of(new ItemStack(Items.SWEET_BERRIES, 1 + level.random.nextInt(2) + (age == SweetBerryBushBlock.MAX_AGE ? 1 : 0))),
                    state.setValue(SweetBerryBushBlock.AGE, 1));
        }
        if (state.is(Blocks.PUMPKIN) || state.is(Blocks.MELON)) {
            // Only fruit still attached to a stem: a pumpkin used as decoration is left alone.
            for (Direction side : Direction.Plane.HORIZONTAL) {
                BlockState near = level.getBlockState(pos.relative(side));
                if (near.getBlock() instanceof AttachedStemBlock && near.getValue(AttachedStemBlock.FACING) == side.getOpposite())
                    return new Harvest(Block.getDrops(state, level, pos, null), null);
            }
            return null;
        }
        if (block instanceof SugarCaneBlock || block instanceof CactusBlock || block instanceof BambooStalkBlock) {
            // The base stays and regrows; everything above it is cut.
            return level.getBlockState(pos.below()).is(block) ? new Harvest(Block.getDrops(state, level, pos, null), null) : null;
        }
        return null;
    }
    private static boolean takeOne(List<ItemStack> drops, Item seed) {
        for (ItemStack drop : drops) {
            if (!drop.is(seed)) continue;
            drop.shrink(1);
            drops.removeIf(ItemStack::isEmpty);
            return true;
        }
        return false;
    }

    /** Gives plants in reach extra growth ticks; each one costs energy. */
    static void accelerate(MachineBlockEntity machine, ServerLevel level) {
        int cost = cost(machine, GROWTH_ENERGY), reach = machine.reach(), width = reach * 2 + 1;
        if (machine.energy.stored() < cost) { machine.status(2); return; }
        boolean worked = false;
        for (int attempt = 0, attempts = 2 + machine.tier().index(); attempt < attempts; attempt++) {
            BlockPos target = target(machine, level.random.nextInt(width * width * 3), reach);
            if (!level.hasChunkAt(target)) continue;
            BlockState state = level.getBlockState(target);
            if (!growable(level, target, state)) continue;
            if (machine.energy.stored() < cost) break;
            state.randomTick(level, target, level.random);
            machine.energy.extract(cost, false);
            worked = true;
        }
        // The scan cursor is unused here, so it counts passes that found nothing to grow.
        if (worked) { machine.cursor = 0; machine.status(1); machine.setChanged(); }
        else if (++machine.cursor >= IDLE_PASSES) { machine.cursor = IDLE_PASSES; machine.status(0); }
    }
    /** A plant a growth tick can do something for; energy is only spent on these. */
    static boolean growable(ServerLevel level, BlockPos pos, BlockState state) {
        if (!state.isRandomlyTicking()) return false;
        Block block = state.getBlock();
        // Cane and cactus only grow from the top block of a column shorter than three.
        if (block instanceof SugarCaneBlock || block instanceof CactusBlock)
            return level.isEmptyBlock(pos.above()) && !(level.getBlockState(pos.below()).is(block) && level.getBlockState(pos.below(2)).is(block));
        if (block instanceof BambooStalkBlock || block instanceof BambooSaplingBlock) return level.isEmptyBlock(pos.above()) && bright(level, pos.above());
        if (block instanceof CropBlock || block instanceof StemBlock) return bright(level, pos);
        if (block instanceof PitcherCropBlock) return level.getRawBrightness(pos, 0) >= 8;
        // Saplings count the darkness of night; a propagule is taken as it is.
        if (block instanceof SaplingBlock && !(block instanceof MangrovePropaguleBlock)) return level.getMaxLocalRawBrightness(pos.above()) >= 9;
        if (block instanceof SweetBerryBushBlock) return bright(level, pos.above());
        // Other mods' crops and saplings keep their own rules.
        if (state.is(BlockTags.CROPS) || state.is(BlockTags.SAPLINGS)) return true;
        return block instanceof NetherWartBlock || block instanceof CocoaBlock;
    }
    /** The light most plants need before a growth tick does anything. */
    private static boolean bright(ServerLevel level, BlockPos pos) { return level.getRawBrightness(pos, 0) >= 9; }

    /** Pulls dropped items in reach into the result slots. */
    static void collect(MachineBlockEntity machine, ServerLevel level) {
        int reach = machine.reach(), cost = cost(machine, COLLECT_ENERGY);
        List<ItemEntity> found = level.getEntitiesOfClass(ItemEntity.class, new AABB(machine.getBlockPos()).inflate(reach),
                // Like a player, it leaves items that cannot be picked up yet, or ever (display items).
                entity -> entity.isAlive() && !entity.hasPickUpDelay() && !entity.getItem().isEmpty());
        boolean moved = false, full = false;
        for (ItemEntity entity : found) {
            if (machine.energy.stored() < cost) { if (!moved) machine.status(2); return; }
            ItemStack stack = entity.getItem(), rest = machine.insertPartly(stack);
            if (rest.getCount() == stack.getCount()) { full = true; continue; }
            if (rest.isEmpty()) entity.discard(); else entity.setItem(rest);
            machine.energy.extract(cost, false);
            moved = true;
        }
        machine.status(moved ? 1 : full ? 3 : 0);
    }
    private FarmWork() {}
}
