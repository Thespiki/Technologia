package dev.technologia.test;

import dev.technologia.Technologia;
import dev.technologia.logistics.*;
import dev.technologia.machine.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.*;

import java.util.List;

/** Tiers, lanes, input refusal, drops, save migration and the alpha.4 additions. */
@GameTestHolder(Technologia.MOD_ID)
@PrefixGameTestTemplate(false)
public final class TierGameTests {
    private static final int OUT = MachineBlockEntity.FIRST_OUTPUT;
    private static MachineBlockEntity place(GameTestHelper h, String id, int x, int z) {
        var pos = new BlockPos(x, 2, z);
        h.setBlock(pos, Technologia.BLOCKS.get(id));
        return (MachineBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
    }
    private static ItemStack item(String id, int count) { return new ItemStack(Technologia.ITEMS.get(id), count); }
    private static void ticks(GameTestHelper h, MachineBlockEntity be, int count) {
        for (int i = 0; i < count; i++) MachineBlockEntity.tick(h.getLevel(), be.getBlockPos(), be.getBlockState(), be);
    }
    private static void upgrade(MachineBlockEntity machine, int tier) { for (int i = machine.tier().index() + 1; i <= tier; i++) machine.upgradeTo(i); }

    @GameTest(template = "empty")
    public static void tierDataLoadsAndKitsAreRegistered(GameTestHelper h) {
        h.assertTrue(MachineTier.count() >= 6, "The tier ladder has many more than five tiers");
        for (MachineTier tier : MachineTier.all()) {
            h.assertTrue(tier.index() == 0 || Technologia.ITEMS.get("tier_kit_" + tier.id()) instanceof TierKitItem kit && kit.tier == tier.index(), "Every tier above the first has its kit item");
            h.assertTrue(tier.index() == 0 || h.getLevel().getRecipeManager().byKey(Technologia.id("tier_kit_" + tier.id())).isPresent(), "Every kit is craftable");
            h.assertTrue(MachineKind.ALLOY_SMELTER.inputCount(tier) <= MachineBlockEntity.INPUT_SLOTS && MachineKind.CRUSHER.lanes(tier) >= 1, "Lanes always fit the ingredient grid");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void tierKitUpgradesInPlaceAndInOrder(GameTestHelper h) {
        var crusher = place(h, "crusher", 2, 2);
        crusher.setItem(0, new ItemStack(Items.RAW_IRON, 5)); crusher.setItem(OUT, item("iron_dust", 3)); crusher.receiveEnergy(40000, false);
        h.assertTrue(!crusher.upgradeTo(2) && crusher.tier().index() == 0, "A tier cannot be skipped");
        h.assertTrue(crusher.upgradeTo(1) && crusher.tier().index() == 1, "The next tier installs");
        h.assertTrue(crusher.getItem(0).getCount() == 5 && crusher.getItem(OUT).getCount() == 3 && crusher.energy.stored() == 40000, "An upgrade keeps inventory and energy");
        h.assertTrue(crusher.capacity() == MachineBlockEntity.scaledCapacity(MachineKind.CRUSHER, MachineTier.get(1)) && crusher.capacity() > 50000, "Capacity follows the tier data");
        var player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Technologia.ITEMS.get("tier_kit_" + MachineTier.get(2).id())));
        h.useBlock(new BlockPos(2, 2, 2), player);
        h.assertTrue(crusher.tier().index() == 2 && player.getMainHandItem().isEmpty(), "Using the kit on the block installs it and consumes the kit");
        var core = place(h, "storage_core", 4, 2);
        h.assertTrue(!core.upgradeTo(1), "Storage cores have no tier");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void lanesWorkInParallelAndShareOutputs(GameTestHelper h) {
        var crusher = place(h, "crusher", 2, 2);
        upgrade(crusher, 2);
        h.assertTrue(crusher.inputCount() == 2, "Mk III has two lanes");
        crusher.setItem(0, new ItemStack(Items.RAW_IRON)); crusher.setItem(1, new ItemStack(Items.RAW_COPPER)); crusher.receiveEnergy(50000, false);
        // Mk III works at 150 percent, so a 100-tick recipe needs 67 ticks.
        ticks(h, crusher, 66);
        h.assertTrue(crusher.getItem(OUT).isEmpty(), "Neither lane finishes early");
        ticks(h, crusher, 1);
        h.assertTrue(crusher.getItem(0).isEmpty() && crusher.getItem(1).isEmpty(), "Both lanes consumed their ingredient in the same tick");
        h.assertTrue(crusher.getItem(OUT).getCount() == 2 && crusher.getItem(OUT + 1).getCount() == 2 && !ItemStack.isSameItem(crusher.getItem(OUT), crusher.getItem(OUT + 1)), "Both results are present");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 100)
    public static void idleLaneReceivesHalfOfALargeStack(GameTestHelper h) {
        var crusher = place(h, "crusher", 2, 2);
        upgrade(crusher, 2);
        crusher.setItem(0, new ItemStack(Items.RAW_IRON, 8));
        h.succeedWhen(() -> {
            h.assertTrue(crusher.getItem(0).getCount() + crusher.getItem(1).getCount() == 8, "Balancing conserves items");
            h.assertTrue(crusher.getItem(1).getCount() == 4, "Half of the stack moved to the idle lane");
        });
    }

    @GameTest(template = "empty")
    public static void machinesRefuseAndRejectUnusableItems(GameTestHelper h) {
        var crusher = place(h, "crusher", 1, 2);
        h.assertTrue(crusher.canPlaceItem(0, new ItemStack(Items.RAW_IRON)) && !crusher.canPlaceItem(0, new ItemStack(Items.DIAMOND)), "A crusher only accepts what it can crush");
        h.assertTrue(!crusher.canPlaceItem(1, new ItemStack(Items.RAW_IRON)), "A slot beyond the tier's lanes accepts nothing");
        var furnace = place(h, "electric_furnace", 3, 2);
        h.assertTrue(furnace.canPlaceItem(0, new ItemStack(Items.COBBLESTONE)) && !furnace.canPlaceItem(0, new ItemStack(Items.DIAMOND)), "A furnace only accepts what it can smelt");
        var alloy = place(h, "alloy_smelter", 5, 2);
        alloy.setItem(0, new ItemStack(Items.COPPER_INGOT, 3));
        h.assertTrue(alloy.canPlaceItem(1, item("tin_ingot", 1)) && !alloy.canPlaceItem(1, new ItemStack(Items.COPPER_INGOT)), "The second alloy slot only takes a matching partner");
        h.assertTrue(alloy.canPlaceItem(0, new ItemStack(Items.COPPER_INGOT)), "Topping up the same ingredient is allowed");
        crusher.setItem(0, new ItemStack(Items.DIAMOND, 3)); crusher.receiveEnergy(5000, false); ticks(h, crusher, 1);
        h.assertTrue(crusher.getItem(0).isEmpty() && crusher.getItem(OUT).is(Items.DIAMOND) && crusher.getItem(OUT).getCount() == 3, "An unusable item already inside moves to the results");
        h.assertTrue(crusher.energy.stored() == 5000, "Rejecting costs no energy");
        crusher.setItem(0, new ItemStack(Items.IRON_INGOT)); // not crushable either; results full of something else
        for (int i = OUT; i < MachineBlockEntity.SLOTS; i++) crusher.setItem(i, new ItemStack(Items.DIAMOND, 64));
        ticks(h, crusher, 1);
        h.assertTrue(crusher.getItem(0).getCount() == 1 && crusher.status() == 12, "With no room to reject, the item stays and the status says why");
        var compactor = place(h, "compactor", 1, 4);
        compactor.setItem(0, new ItemStack(Items.IRON_INGOT, 5)); compactor.receiveEnergy(5000, false); ticks(h, compactor, 5);
        h.assertTrue(compactor.getItem(0).getCount() == 5 && compactor.getItem(OUT).isEmpty() && compactor.status() == 4, "Too few of a valid ingredient waits; it is not rejected");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void droppedMachineKeepsTierAndEnergy(GameTestHelper h) {
        var cell = place(h, "energy_cell", 2, 2);
        upgrade(cell, 1); cell.receiveEnergy(123456, false);
        var drops = Block.getDrops(cell.getBlockState(), h.getLevel(), cell.getBlockPos(), cell);
        h.assertTrue(drops.size() == 1 && drops.getFirst().has(DataComponents.BLOCK_ENTITY_DATA), "The dropped block carries its data");
        ItemStack stack = drops.getFirst();
        h.assertTrue(MachineBlockItem.tier(stack).index() == 1 && MachineBlockItem.energy(stack) == 123456, "Tier and energy are on the item");
        var roundTrip = ItemStack.parse(h.getLevel().registryAccess(), stack.save(h.getLevel().registryAccess())).orElseThrow();
        h.assertTrue(MachineBlockItem.tier(roundTrip).index() == 1, "The item survives being saved");
        var target = place(h, "energy_cell", 4, 2);
        BlockItem.updateCustomBlockEntityTag(h.getLevel(), null, target.getBlockPos(), stack);
        h.assertTrue(target.tier().index() == 1 && target.energy.stored() == 123456, "Placing the item restores tier and energy");
        var plain = Block.getDrops(target.getBlockState(), h.getLevel(), target.getBlockPos(), place(h, "energy_cell", 6, 2));
        h.assertTrue(!plain.getFirst().has(DataComponents.BLOCK_ENTITY_DATA), "A fresh machine drops a plain, stackable item");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void wrenchDismantlesIntoInventory(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        var pos = new BlockPos(2, 2, 2);
        var press = place(h, "metal_press", 2, 2);
        upgrade(press, 1); press.receiveEnergy(7000, false); press.setItem(0, new ItemStack(Items.IRON_INGOT, 2));
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Technologia.ITEMS.get("wrench")));
        player.setShiftKeyDown(true);
        h.useBlock(pos, player);
        h.assertBlockPresent(Blocks.AIR, pos);
        ItemStack held = ItemStack.EMPTY;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) if (player.getInventory().getItem(i).is(Technologia.ITEMS.get("metal_press"))) held = player.getInventory().getItem(i);
        h.assertTrue(!held.isEmpty() && MachineBlockItem.tier(held).index() == 1 && MachineBlockItem.energy(held) == 7000, "The dismantled machine is in the inventory with its tier and energy");
        h.assertItemEntityCountIs(Items.IRON_INGOT, pos, 2, 2);
        h.getLevel().getServer().getPlayerList().remove(player);
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void alpha3SavesMoveResultsToTheNewRegion(GameTestHelper h) {
        var crusher = place(h, "crusher", 2, 2);
        var registries = h.getLevel().registryAccess();
        CompoundTag old = crusher.saveWithFullMetadata(registries);
        old.remove("Format"); old.remove("Tier"); old.remove("Lanes");
        ListTag items = new ListTag();
        int[] slots = {0, 1, 26}; ItemStack[] stacks = {new ItemStack(Items.RAW_IRON, 6), item("iron_dust", 5), new ItemStack(Items.DIAMOND, 2)};
        for (int i = 0; i < slots.length; i++) { CompoundTag entry = (CompoundTag) stacks[i].save(registries); entry.putByte("Slot", (byte) slots[i]); items.add(entry); }
        old.put("Items", items); old.putInt("Energy", 1234);
        var loaded = (MachineBlockEntity) BlockEntity.loadStatic(crusher.getBlockPos(), crusher.getBlockState(), old, registries);
        h.assertTrue(loaded.getItem(0).getCount() == 6 && loaded.getItem(1).isEmpty(), "The ingredient stays; the old result slot is cleared");
        h.assertTrue(loaded.getItem(OUT).getCount() == 5 && loaded.getItem(OUT + 1).is(Items.DIAMOND) && loaded.getItem(26).isEmpty(), "Old results move to the result region");
        h.assertTrue(loaded.energy.stored() == 1234 && loaded.tier().index() == 0, "Energy is kept and the machine is Mk I");
        int total = 0; for (int i = 0; i < loaded.getContainerSize(); i++) total += loaded.getItem(i).getCount();
        h.assertTrue(total == 13, "Migration neither loses nor creates items");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void conduitMaterialSetsTheNetworkLimit(GameTestHelper h) {
        var copperCell = place(h, "energy_cell", 1, 1); var copperTarget = place(h, "compactor", 3, 1);
        var goldCell = place(h, "energy_cell", 1, 4); var goldTarget = place(h, "compactor", 3, 4);
        var directCell = place(h, "energy_cell", 5, 6); var directTarget = place(h, "metal_press", 6, 6);
        h.setBlock(new BlockPos(2, 2, 1), Technologia.BLOCKS.get("energy_conduit"));
        h.setBlock(new BlockPos(2, 2, 4), Technologia.BLOCKS.get("gold_energy_conduit"));
        for (var cell : new MachineBlockEntity[] {copperCell, goldCell, directCell}) { upgrade(cell, 3); cell.receiveEnergy(100000, false); }
        h.assertTrue(copperCell.transferRate() == 1600, "A Mk IV supplier can send 1,600 FE/t");
        h.assertTrue(EnergyTransport.transfer(copperCell) == 200 && copperTarget.energy.stored() == 200, "A copper conduit caps the network at 200 FE/t");
        h.assertTrue(EnergyTransport.transfer(goldCell) == 1000 && goldTarget.energy.stored() == 1000, "A gold conduit carries 1,000 FE/t");
        h.assertTrue(EnergyTransport.transfer(directCell) == 1600 && directTarget.energy.stored() == 1600, "A directly adjacent machine is limited only by the supplier");
        h.assertTrue(EnergyTransport.transfer(directCell) == 0, "The supplier's limit is per tick");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void generatorDoesNotBurnFuelIntoAFullBuffer(GameTestHelper h) {
        var generator = place(h, "coal_generator", 2, 2);
        generator.energy.restore(generator.capacity() - 10);
        generator.setItem(0, new ItemStack(Items.COAL, 3));
        ticks(h, generator, 20);
        h.assertTrue(generator.getItem(0).getCount() == 3 && generator.energy.stored() == generator.capacity() - 10, "No fuel is burnt when a full tick of output does not fit");
        generator.energy.restore(0); ticks(h, generator, 1);
        h.assertTrue(generator.getItem(0).getCount() == 2 && generator.energy.stored() == 40, "With room, one fuel item starts burning at the configured rate");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 120)
    public static void resonanceBloomsSpeedUpNearbyProcessors(GameTestHelper h) {
        var crusher = place(h, "crusher", 3, 3);
        for (int x : new int[] {2, 4}) { h.setBlock(new BlockPos(x, 1, 3), Blocks.DIRT); h.setBlock(new BlockPos(x, 2, 3), Technologia.BLOCKS.get("resonance_bloom")); }
        h.succeedWhen(() -> {
            h.assertBlockPresent(Technologia.BLOCKS.get("resonance_bloom"), new BlockPos(2, 2, 3));
            h.assertTrue(crusher.boost() == 2, "Two blooms within two blocks are counted");
        });
    }

    @GameTest(template = "empty")
    public static void autoSieveYieldsFragmentsThatCrushIntoDust(GameTestHelper h) {
        var sieve = place(h, "auto_sieve", 2, 2);
        sieve.setItem(0, new ItemStack(Items.GRAVEL)); sieve.receiveEnergy(5000, false); ticks(h, sieve, 80);
        h.assertTrue(sieve.getItem(0).isEmpty() && sieve.getItem(OUT).is(Technologia.ITEMS.get("iron_fragment")), "Gravel sieves into an iron fragment");
        var crusher = place(h, "crusher", 4, 2);
        crusher.setItem(0, item("iron_fragment", 4)); crusher.receiveEnergy(5000, false); ticks(h, crusher, 60);
        h.assertTrue(crusher.getItem(0).isEmpty() && crusher.getItem(OUT).is(Technologia.ITEMS.get("iron_dust")) && crusher.getItem(OUT).getCount() == 1, "Four fragments crush into one dust");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void onlyTheOwnerUsesAMinerMenu(GameTestHelper h) {
        var miner = place(h, "digital_miner", 2, 2);
        var owner = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL); var visitor = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        for (var p : new net.minecraft.world.entity.player.Player[] {owner, visitor}) p.setPos(miner.getBlockPos().getX() + .5, miner.getBlockPos().getY(), miner.getBlockPos().getZ() + .5);
        miner.setOwner(owner.getUUID()); miner.setItem(OUT, new ItemStack(Items.RAW_IRON, 9));
        var menu = new MachineMenu(1, visitor.getInventory(), miner, miner.data);
        h.assertTrue(menu.quickMoveStack(visitor, OUT).isEmpty() && miner.getItem(OUT).getCount() == 9, "A visitor cannot take the miner's results");
        h.assertTrue(!menu.clickMenuButton(visitor, 0) && !miner.isEnabled(), "A visitor cannot start the miner");
        var own = new MachineMenu(2, owner.getInventory(), miner, miner.data);
        own.quickMoveStack(owner, OUT);
        h.assertTrue(miner.getItem(OUT).isEmpty() && owner.getInventory().countItem(Items.RAW_IRON) == 9, "The owner can");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void directionalDecorAndTransferShapes(GameTestHelper h) {
        var panel = Technologia.BLOCKS.get("control_panel");
        h.assertTrue(panel instanceof DirectionalFactoryBlock && panel.defaultBlockState().hasProperty(DirectionalFactoryBlock.FACING), "Control panels can face a direction");
        var transfer = Technologia.BLOCKS.get("item_transfer").defaultBlockState();
        h.setBlock(new BlockPos(2, 2, 2), transfer);
        h.assertTrue(h.getBlockState(new BlockPos(2, 2, 2)).getShape(h.getLevel(), h.absolutePos(new BlockPos(2, 2, 2))).bounds().getYsize() < 0.7, "The Item Transfer outline follows its body, not a full cube");
        h.assertTrue(((dev.technologia.logistics.EnergyConduitBlock) Technologia.BLOCKS.get("resonite_energy_conduit")).rating == 5000, "Resonite conduits carry 5,000 FE/t");
        h.assertTrue(Direction.NORTH == transfer.getValue(dev.technologia.logistics.ItemTransferBlock.FACING), "Default facing is unchanged");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void laneBalancingMovesWholeOperations(GameTestHelper h) {
        var compactor = place(h, "compactor", 2, 2);
        upgrade(compactor, 2);
        // Nine ingots make one block: 27 must split into 18 and 9, never 14 and 13.
        compactor.setItem(0, new ItemStack(Items.IRON_INGOT, 27));
        var alloy = place(h, "alloy_smelter", 4, 2);
        upgrade(alloy, 2);
        alloy.setItem(0, new ItemStack(Items.COPPER_INGOT, 9)); alloy.setItem(1, item("tin_ingot", 3));
        h.succeedWhen(() -> {
            h.assertTrue(compactor.getItem(0).getCount() == 18 && compactor.getItem(1).getCount() == 9, "Each lane holds a whole number of operations");
            h.assertTrue(alloy.getItem(0).getCount() == 6 && alloy.getItem(1).getCount() == 2 && alloy.getItem(2).getCount() == 3 && alloy.getItem(3).getCount() == 1, "Both alloy ingredients move together, one operation's worth");
        });
    }

    @GameTest(template = "empty")
    public static void hiddenSlotsAreSafeForCommands(GameTestHelper h) {
        var crusher = place(h, "crusher", 2, 2);
        crusher.setItem(10, new ItemStack(Items.STONE, 3));
        h.assertTrue(crusher.getItem(10).getCount() == 3, "Setting a hidden ingredient slot directly does not fail");
        crusher.removeItemNoUpdate(12);
        h.succeedWhen(() -> h.assertTrue(crusher.getItem(10).isEmpty() && crusher.getItem(OUT).is(Items.STONE) && crusher.getItem(OUT).getCount() == 3, "Anything put there moves to the results"));
    }

    @GameTest(template = "empty")
    public static void separateConduitRunsKeepTheirOwnLimit(GameTestHelper h) {
        var cell = place(h, "energy_cell", 3, 3);
        var copperTarget = place(h, "compactor", 1, 3); var goldTarget = place(h, "compactor", 5, 3);
        h.setBlock(new BlockPos(2, 2, 3), Technologia.BLOCKS.get("energy_conduit"));
        h.setBlock(new BlockPos(4, 2, 3), Technologia.BLOCKS.get("gold_energy_conduit"));
        // A copper stub that leads nowhere must not throttle the other runs either.
        h.setBlock(new BlockPos(3, 2, 4), Technologia.BLOCKS.get("energy_conduit"));
        upgrade(cell, 3); cell.receiveEnergy(100000, false);
        int moved = EnergyTransport.transfer(cell);
        h.assertTrue(copperTarget.energy.stored() == 200, "The copper run carries 200 FE/t");
        h.assertTrue(goldTarget.energy.stored() == 1000, "The gold run on the same supplier still carries 1,000 FE/t");
        h.assertTrue(moved == 1200 && cell.energy.stored() == 100000 - 1200, "The supplier gives up exactly what arrived");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void transferTakesFurnaceResultsNotFuel(GameTestHelper h) {
        var furnacePos = new BlockPos(2, 2, 2);
        h.setBlock(furnacePos, Blocks.FURNACE);
        var furnace = (Container) h.getLevel().getBlockEntity(h.absolutePos(furnacePos));
        furnace.setItem(1, new ItemStack(Items.COAL, 8)); furnace.setItem(2, new ItemStack(Items.IRON_INGOT, 5));
        var target = place(h, "storage_core", 4, 2);
        var p = new BlockPos(3, 2, 2);
        h.setBlock(p, Technologia.BLOCKS.get("item_transfer").defaultBlockState().setValue(ItemTransferBlock.FACING, Direction.EAST));
        var transfer = (ItemTransferBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(p));
        h.assertTrue(transfer.transfer() == 5, "A transfer on the side of a furnace takes the smelted items");
        h.assertTrue(furnace.getItem(1).getCount() == 8 && furnace.getItem(2).isEmpty() && target.getItem(0).is(Items.IRON_INGOT), "The fuel stays in the furnace");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void transferFillsOneItemSlots(GameTestHelper h) {
        var source = place(h, "storage_core", 2, 2);
        source.setItem(0, new ItemStack(Items.BOOK, 16));
        var shelfPos = new BlockPos(4, 2, 2);
        h.setBlock(shelfPos, Blocks.CHISELED_BOOKSHELF);
        var shelf = (Container) h.getLevel().getBlockEntity(h.absolutePos(shelfPos));
        var p = new BlockPos(3, 2, 2);
        h.setBlock(p, Technologia.BLOCKS.get("item_transfer").defaultBlockState().setValue(ItemTransferBlock.FACING, Direction.EAST));
        var transfer = (ItemTransferBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(p));
        h.assertTrue(transfer.transfer() == 6 && source.getItem(0).getCount() == 10, "A stack of books fills a chiseled bookshelf one book per slot");
        for (int slot = 0; slot < 6; slot++) h.assertTrue(shelf.getItem(slot).getCount() == 1, "Each shelf slot holds one book");
        h.succeed();
    }

    private static ItemStack cellItem(GameTestHelper h, int x, int tier, int energy) {
        var cell = place(h, "energy_cell", x, 2);
        upgrade(cell, tier); cell.receiveEnergy(energy, false);
        ItemStack stack = new ItemStack(Technologia.ITEMS.get("energy_cell"));
        cell.saveToItem(stack);
        return stack;
    }

    @GameTest(template = "empty")
    public static void craftingFromMachinesKeepsTierAndEnergy(GameTestHelper h) {
        var recipe = (CraftingRecipe) h.getLevel().getRecipeManager().byKey(Technologia.id("advanced_energy_cell")).orElseThrow().value();
        ItemStack steel = item("steel_plate", 1), circuit = item("advanced_circuit", 1), resonite = item("resonite", 1);
        var upgraded = CraftingInput.of(3, 3, List.of(steel, circuit, steel.copy(), cellItem(h, 1, 3, 30000), resonite, cellItem(h, 3, 2, 12000), steel.copy(), circuit.copy(), steel.copy()));
        h.assertTrue(recipe.matches(upgraded, h.getLevel()), "Upgraded cells are valid ingredients");
        ItemStack result = recipe.assemble(upgraded, h.getLevel().registryAccess());
        h.assertTrue(result.is(Technologia.ITEMS.get("advanced_energy_cell")), "The recipe still makes an Advanced Energy Cell");
        h.assertTrue(MachineBlockItem.tier(result).index() == 2, "The result keeps the lowest tier among the machines used");
        h.assertTrue(MachineBlockItem.energy(result) == 42000, "The result keeps their stored energy");
        var plain = CraftingInput.of(3, 3, List.of(steel, circuit, steel.copy(), item("energy_cell", 1), resonite, item("energy_cell", 1), steel.copy(), circuit.copy(), steel.copy()));
        ItemStack fresh = recipe.assemble(plain, h.getLevel().registryAccess());
        h.assertTrue(ItemStack.isSameItemSameComponents(fresh, item("advanced_energy_cell", 1)), "New cells make a plain result that stacks with others");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeWrenchNeverDeletesTheMachine(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        var cell = place(h, "energy_cell", 2, 2);
        upgrade(cell, 1); cell.receiveEnergy(5000, false);
        var player = h.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.CREATIVE);
        for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, new ItemStack(Items.DIRT, 64));
        player.getInventory().setItem(player.getInventory().selected, new ItemStack(Technologia.ITEMS.get("wrench")));
        player.setShiftKeyDown(true);
        h.useBlock(pos, player);
        h.assertTrue(h.getBlockState(pos).isAir(), "The machine was dismantled");
        var dropped = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, new net.minecraft.world.phys.AABB(h.absolutePos(pos)).inflate(3));
        h.assertTrue(dropped.stream().anyMatch(e -> e.getItem().is(Technologia.ITEMS.get("energy_cell")) && MachineBlockItem.tier(e.getItem()).index() == 1 && MachineBlockItem.energy(e.getItem()) == 5000),
                "With a full creative inventory the machine item drops instead of vanishing");
        h.getLevel().getServer().getPlayerList().remove(player);
        h.succeed();
    }
}
