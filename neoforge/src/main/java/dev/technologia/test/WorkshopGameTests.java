package dev.technologia.test;

import dev.technologia.Technologia;
import dev.technologia.machine.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.capabilities.Capabilities;

@GameTestHolder(Technologia.MOD_ID)
@PrefixGameTestTemplate(false)
public final class WorkshopGameTests {
    private static MachineBlockEntity place(GameTestHelper helper, String id, int x) {
        BlockPos pos = new BlockPos(x, 2, 2);
        helper.setBlock(pos, Technologia.BLOCKS.get(id));
        return (MachineBlockEntity) helper.getLevel().getBlockEntity(helper.absolutePos(pos));
    }
    @GameTest(template = "empty", timeoutTicks = 220)
    public static void generatorPowersCrusher(GameTestHelper helper) {
        var gen = place(helper, "coal_generator", 2);
        var crusher = place(helper, "crusher", 3);
        gen.setItem(0, new ItemStack(Items.COAL));
        crusher.setItem(0, new ItemStack(Items.RAW_IRON));
        helper.succeedWhen(() -> {
            helper.assertTrue(crusher.getItem(0).isEmpty(), "Input must be consumed once");
            helper.assertTrue(crusher.getItem(1).is(Technologia.ITEMS.get("iron_dust")) && crusher.getItem(1).getCount() == 2, "One raw iron must produce exactly two dust");
        });
    }
    @GameTest(template = "empty", timeoutTicks = 180)
    public static void fullOutputDoesNotConsume(GameTestHelper helper) {
        var crusher = place(helper, "crusher", 2);
        crusher.setItem(0, new ItemStack(Items.RAW_IRON));
        for (int i = 1; i < 27; i++) crusher.setItem(i, new ItemStack(Technologia.ITEMS.get("iron_dust"), 64));
        crusher.receiveEnergy(10000, false);
        helper.runAtTickTime(120, () -> {
            helper.assertTrue(crusher.getItem(0).getCount() == 1, "Full output must preserve input");
            helper.assertTrue(crusher.energy.stored() == 10000, "Full output must not drain energy");
            helper.succeed();
        });
    }
    @GameTest(template = "empty", timeoutTicks = 180)
    public static void furnaceUsesLoadedRecipe(GameTestHelper helper) {
        var furnace = place(helper, "electric_furnace", 2);
        furnace.setItem(0, new ItemStack(Technologia.ITEMS.get("raw_tin")));
        furnace.receiveEnergy(10000, false);
        helper.succeedWhen(() -> helper.assertTrue(furnace.getItem(1).is(Technologia.ITEMS.get("tin_ingot")), "Data-pack tin smelting recipe must load"));
    }
    @GameTest(template = "empty")
    public static void persistenceRoundTrip(GameTestHelper helper) {
        var machine = place(helper, "crusher", 2);
        machine.setItem(0, new ItemStack(Items.RAW_IRON, 7));
        machine.receiveEnergy(45000, false);
        var saved = machine.saveWithFullMetadata(helper.getLevel().registryAccess());
        var restored = (MachineBlockEntity) BlockEntity.loadStatic(machine.getBlockPos(), machine.getBlockState(), saved, helper.getLevel().registryAccess());
        helper.assertTrue(restored != null && restored.getItem(0).getCount() == 7 && restored.energy.stored() == 45000, "Inventory and energy must survive save/load");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void networkDisconnects(GameTestHelper helper) {
        var core = place(helper, "storage_core", 2);
        helper.setBlock(new BlockPos(3, 2, 2), Technologia.BLOCKS.get("network_cable"));
        var terminal = place(helper, "storage_terminal", 4);
        helper.assertTrue(StorageNetwork.findCore(helper.getLevel(), terminal.getBlockPos()) == core, "Cable must expose core");
        helper.setBlock(new BlockPos(3, 2, 2), Blocks.AIR);
        helper.assertTrue(StorageNetwork.findCore(helper.getLevel(), terminal.getBlockPos()) == null, "Removing cable must disconnect core");
        helper.succeed();
    }
    @GameTest(template = "empty", timeoutTicks = 40)
    public static void minerStartsPaused(GameTestHelper helper) {
        var miner = place(helper, "digital_miner", 2);
        miner.receiveEnergy(10000, false);
        helper.setBlock(new BlockPos(2, 1, 2), Blocks.IRON_ORE);
        helper.runAtTickTime(20, () -> {
            helper.assertBlockPresent(Blocks.IRON_ORE, new BlockPos(2, 1, 2));
            helper.assertTrue(miner.energy.stored() == 10000, "Paused miner must not spend energy");
            helper.succeed();
        });
    }
    @GameTest(template = "empty")
    public static void energyCapabilitySimulation(GameTestHelper helper) {
        var cell = place(helper, "energy_cell", 2);
        var storage = helper.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, cell.getBlockPos(), Direction.UP);
        helper.assertTrue(storage != null, "FE capability must be available");
        int accepted = storage.receiveEnergy(5000, true);
        helper.assertTrue(accepted == 200 && storage.getEnergyStored() == 0, "Simulated receipt must be bounded and not mutate");
        storage.receiveEnergy(accepted, false);
        helper.assertTrue(storage.extractEnergy(200, true) == 200 && storage.getEnergyStored() == 200, "Simulated extraction must not mutate");
        storage.extractEnergy(200, false);
        helper.assertTrue(storage.getEnergyStored() == 0, "Committed extraction must conserve energy");
        helper.succeed();
    }
    @GameTest(template = "empty")
    public static void itemCapabilityProtectsInputAndOutput(GameTestHelper helper) {
        var crusher = place(helper, "crusher", 2);
        var storage = helper.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, crusher.getBlockPos(), Direction.DOWN);
        helper.assertTrue(storage != null, "Item capability must be available");
        storage.insertItem(0, new ItemStack(Items.RAW_IRON), false);
        helper.assertTrue(storage.extractItem(0, 1, false).isEmpty(), "Automation must not extract process input");
        helper.assertTrue(!storage.insertItem(1, new ItemStack(Items.DIAMOND), false).isEmpty(), "Automation must not insert into output");
        crusher.setItem(1, new ItemStack(Technologia.ITEMS.get("iron_dust"), 2));
        helper.assertTrue(storage.extractItem(1, 2, false).getCount() == 2, "Automation must extract output");
        helper.succeed();
    }
}
