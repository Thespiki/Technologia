package dev.technologia.test;

import dev.technologia.Technologia;
import dev.technologia.device.*;
import dev.technologia.item.MagnetItem;
import dev.technologia.logistics.*;
import dev.technologia.machine.*;
import dev.technologia.recipe.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.ForcedChunksSavedData;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.gametest.*;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * The alpha.5 additions: tier bands, machine settings, free and wireless power, farm machines,
 * devices, tools and recipes. The numbers are those of docs/releases/0.1.0-alpha.5.md.
 * <p>
 * Machines that only act every few ticks (eject, harvester, accelerator, vacuum) are left to the
 * game's own ticking, because calling tick() many times inside one game tick repeats the same instant.
 */
@GameTestHolder(Technologia.MOD_ID)
@PrefixGameTestTemplate(false)
public final class Alpha5GameTests {
    private static final int OUT = MachineBlockEntity.FIRST_OUTPUT, END = MachineBlockEntity.SLOTS, RATE = MachineBlockEntity.DATA_RATE;

    private static MachineBlockEntity place(GameTestHelper h, String id, int x, int z) {
        return (MachineBlockEntity) device(h, id, new BlockPos(x, 2, z));
    }
    private static BlockEntity device(GameTestHelper h, String id, BlockPos pos) {
        h.setBlock(pos, Technologia.BLOCKS.get(id));
        return h.getLevel().getBlockEntity(h.absolutePos(pos));
    }
    private static ItemStack item(String id, int count) { return new ItemStack(Technologia.ITEMS.get(id), count); }
    private static ItemStack item(Item item, int count) { return new ItemStack(item, count); }
    private static void ticks(GameTestHelper h, MachineBlockEntity be, int count) {
        for (int i = 0; i < count; i++) MachineBlockEntity.tick(h.getLevel(), be.getBlockPos(), be.getBlockState(), be);
    }
    private static void upgrade(MachineBlockEntity machine, int tier) { for (int i = machine.tier().index() + 1; i <= tier; i++) machine.upgradeTo(i); }
    private static Player near(GameTestHelper h, BlockPos absolute) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setPos(absolute.getX() + .5, absolute.getY(), absolute.getZ() + .5);
        return player;
    }
    /** A screen button, pressed through the real menu by a player standing at the machine. */
    private static boolean click(GameTestHelper h, MachineBlockEntity machine, int button) {
        var player = near(h, machine.getBlockPos());
        return new MachineMenu(1, player.getInventory(), machine, machine.data).clickMenuButton(player, button);
    }
    private static int band(GameTestHelper h, BlockPos pos) {
        BlockState state = h.getBlockState(pos);
        return state.hasProperty(MachineBlock.TIER) ? state.getValue(MachineBlock.TIER) : -1;
    }
    private static void wheat(GameTestHelper h, BlockPos pos, int age) {
        h.setBlock(pos.below(), Blocks.FARMLAND.defaultBlockState().setValue(FarmBlock.MOISTURE, 7));
        h.setBlock(pos, Blocks.WHEAT.defaultBlockState().setValue(CropBlock.AGE, age));
    }
    private static int age(GameTestHelper h, BlockPos pos) {
        BlockState state = h.getBlockState(pos);
        return state.is(Blocks.WHEAT) ? state.getValue(CropBlock.AGE) : -1;
    }
    /** A dropped item lying still at a position inside the test box. */
    private static ItemEntity drop(GameTestHelper h, ItemStack stack, double x, double y, double z) {
        Vec3 at = h.absoluteVec(new Vec3(x, y, z));
        var entity = new ItemEntity(h.getLevel(), at.x, at.y, at.z, stack);
        entity.setDeltaMovement(Vec3.ZERO);
        h.getLevel().addFreshEntity(entity);
        return entity;
    }

    // ---- Machine framework ---------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void tierBandFollowsUpgradesLoadingAndPlacement(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        var crusher = place(h, "crusher", 2, 2);
        crusher.setItem(0, item(Items.RAW_IRON, 5));
        h.assertTrue(band(h, pos) == 0, "A new machine shows no tier band");
        h.assertTrue(crusher.upgradeTo(1) && band(h, pos) == 1 && crusher.upgradeTo(2) && band(h, pos) == 2, "Each upgrade changes the band at once");
        ticks(h, crusher, 1);
        h.assertTrue(band(h, pos) == 2 && h.getLevel().getBlockEntity(h.absolutePos(pos)) == crusher && crusher.getItem(0).getCount() == 5, "The band stays, and the machine keeps its contents");
        // A machine saved before alpha.5, or filled from its item, knows its tier before its block shows it.
        upgrade(crusher, 3);
        ItemStack stack = item("crusher", 1);
        crusher.saveToItem(stack);
        var loaded = place(h, "crusher", 4, 2);
        BlockItem.updateCustomBlockEntityTag(h.getLevel(), null, loaded.getBlockPos(), stack);
        ticks(h, loaded, 1);
        h.assertTrue(loaded.tier().index() == 3 && band(h, new BlockPos(4, 2, 2)) == 3, "A machine that loads a tier shows it after one tick");
        var floor = new BlockPos(6, 1, 2);
        h.setBlock(floor, Blocks.STONE);
        var hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(floor)), Direction.UP, h.absolutePos(floor), false);
        var placed = ((BlockItem) stack.getItem()).place(new BlockPlaceContext(h.getLevel(), null, InteractionHand.MAIN_HAND, stack, hit));
        h.assertTrue(placed.consumesAction() && band(h, floor.above()) == 3, "A Mk IV machine placed from its item shows the band at once");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeKitSetsAnyTier(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        var press = place(h, "metal_press", 2, 2);
        int top = MachineTier.count() - 1;
        h.assertTrue(press.setTier(top) && press.tier().index() == top && band(h, pos) == top, "A creative tool jumps straight to the top tier");
        h.assertTrue(!press.setTier(top), "Setting the tier a machine already has changes nothing");
        press.energy.restore(press.capacity());
        h.assertTrue(press.setTier(0) && band(h, pos) == 0 && press.energy.stored() == 50000, "Going back to Mk I clips the energy to the smaller buffer");
        // Through the game's own use path: while the player crouches it skips the block and only asks the item.
        var player = h.makeMockServerPlayerInLevel();
        try {
            player.setGameMode(GameType.CREATIVE);
            player.setItemInHand(InteractionHand.MAIN_HAND, item("creative_tier_kit", 1));
            var hit = new BlockHitResult(Vec3.atCenterOf(h.absolutePos(pos)), Direction.UP, h.absolutePos(pos), false);
            player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            h.assertTrue(press.tier().index() == top && player.getMainHandItem().getCount() == 1, "Using the Creative Tier Kit installs the top tier and keeps the kit");
            player.setShiftKeyDown(true);
            player.gameMode.useItemOn(player, h.getLevel(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
            h.assertTrue(press.tier().index() == 0 && player.getMainHandItem().getCount() == 1, "Crouch-use puts the first tier back");
        } finally {
            h.getLevel().getServer().getPlayerList().remove(player);
        }
        h.assertTrue(!place(h, "wireless_sender", 4, 2).setTier(top) && !place(h, "creative_energy_source", 6, 2).setTier(1), "Blocks without tiers ignore it");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void redstoneModesPauseAndResume(GameTestHelper h) {
        var press = place(h, "metal_press", 2, 2);
        press.setItem(0, item(Items.IRON_INGOT, 1)); press.receiveEnergy(10000, false);
        h.assertTrue(click(h, press, MachineMenu.BUTTON_REDSTONE) && press.data.get(MachineBlockEntity.DATA_REDSTONE) == 1, "The redstone button selects 'only with a signal'");
        ticks(h, press, 10);
        h.assertTrue(press.status() == 13 && press.energy.stored() == 10000 && press.getItem(0).getCount() == 1, "Without a signal the press is paused by redstone and spends nothing");
        var signal = new BlockPos(2, 2, 3);
        h.setBlock(signal, Blocks.REDSTONE_BLOCK);
        ticks(h, press, 10);
        h.assertTrue(press.status() == 1 && press.energy.stored() == 9850, "A signal starts it");
        press.cycleRedstone();
        ticks(h, press, 10);
        h.assertTrue(press.redstoneMode() == 2 && press.status() == 13 && press.energy.stored() == 9850, "'Only without a signal' pauses it while the signal is on");
        h.setBlock(signal, Blocks.AIR);
        ticks(h, press, 50);
        h.assertTrue(press.getItem(OUT).is(Technologia.ITEMS.get("iron_plate")) && press.energy.stored() == 9100, "Work resumes where it stopped: the plate costs 900 FE in all");
        press.cycleRedstone();
        h.assertTrue(press.redstoneMode() == 0, "The third press goes back to ignoring redstone");
        var cell = place(h, "energy_cell", 5, 2);
        h.assertTrue(!click(h, cell, MachineMenu.BUTTON_REDSTONE) && cell.redstoneMode() == 0, "An energy cell has no redstone control");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void comparatorReadsEnergyAndResults(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        var cell = place(h, "energy_cell", 2, 2);
        h.assertTrue(cell.comparatorSignal() == 0, "An empty cell reads 0");
        cell.energy.restore(1);
        h.assertTrue(cell.comparatorSignal() == 1, "Any energy at all reads 1");
        cell.energy.restore(cell.capacity() / 2);
        h.assertTrue(cell.comparatorSignal() == 8, "A half-full cell reads 8");
        cell.energy.restore(cell.capacity());
        h.assertTrue(h.getBlockState(pos).hasAnalogOutputSignal() && h.getBlockState(pos).getAnalogOutputSignal(h.getLevel(), h.absolutePos(pos)) == 15, "A comparator beside a full cell reads 15");
        var crusher = place(h, "crusher", 4, 2);
        crusher.receiveEnergy(50000, false);
        h.assertTrue(crusher.comparatorSignal() == 0, "A processor reports its results, not its energy");
        crusher.setItem(OUT, item("iron_dust", 1));
        h.assertTrue(crusher.comparatorSignal() == 1, "One item in the results reads 1");
        for (int slot = OUT; slot < OUT + 14; slot++) crusher.setItem(slot, item("iron_dust", 64));
        h.assertTrue(crusher.comparatorSignal() == 8, "Results about half full read 8");
        for (int slot = OUT; slot < END; slot++) crusher.setItem(slot, item("iron_dust", 64));
        h.assertTrue(crusher.comparatorSignal() == 15, "Full results read 15");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void autoEjectPushesResultsBehindTheMachine(GameTestHelper h) {
        // Machines face north when set down like this, so "behind" is one block south.
        var press = place(h, "metal_press", 1, 2); var core = place(h, "storage_core", 1, 3);
        var crusher = place(h, "crusher", 3, 2); var sawmill = place(h, "sawmill", 5, 2);
        h.setBlock(new BlockPos(3, 2, 3), Blocks.CHEST); h.setBlock(new BlockPos(5, 2, 3), Blocks.CHEST);
        var chest = (Container) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(3, 2, 3)));
        var untouched = (Container) h.getLevel().getBlockEntity(h.absolutePos(new BlockPos(5, 2, 3)));
        press.setItem(OUT, item("iron_plate", 20)); crusher.setItem(OUT, item("iron_dust", 20)); sawmill.setItem(OUT, item(Items.OAK_PLANKS, 20));
        h.assertTrue(click(h, press, MachineMenu.BUTTON_EJECT) && press.data.get(MachineBlockEntity.DATA_EJECT) == 1, "The eject button switches it on");
        crusher.toggleEject();
        var cell = place(h, "energy_cell", 7, 5);
        cell.toggleEject();
        h.assertTrue(!cell.ejects() && !click(h, cell, MachineMenu.BUTTON_EJECT), "A block without results has nothing to eject");
        // Eight items leave every ten ticks, so twenty are gone well before this check.
        h.runAtTickTime(45, () -> {
            h.assertTrue(press.getItem(OUT).isEmpty() && core.getItem(0).is(Technologia.ITEMS.get("iron_plate")) && core.getItem(0).getCount() == 20, "Results move into a Storage Core behind the machine");
            h.assertTrue(crusher.getItem(OUT).isEmpty() && chest.getItem(0).is(Technologia.ITEMS.get("iron_dust")) && chest.getItem(0).getCount() == 20, "Results move into a chest behind the machine");
            h.assertTrue(sawmill.getItem(OUT).getCount() == 20 && untouched.isEmpty(), "With eject off the results stay in the machine");
            h.succeed();
        });
    }

    // ---- Power from the surroundings -----------------------------------------------------------

    @GameTest(template = "empty")
    public static void waterWheelNeedsFlowingWater(GameTestHelper h) {
        BlockState flowing = Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 3), falling = Blocks.WATER.defaultBlockState().setValue(LiquidBlock.LEVEL, 8);
        BlockPos[] water = {new BlockPos(2, 2, 1), new BlockPos(2, 2, 5), new BlockPos(4, 2, 3), new BlockPos(6, 2, 3)};
        try {
            // A wheel reads its surroundings on its first tick, so the water goes in before it.
            h.setBlock(water[0], flowing); h.setBlock(water[1], Blocks.WATER); h.setBlock(water[2], flowing); h.setBlock(water[3], falling);
            var wheel = place(h, "water_wheel", 1, 1); var still = place(h, "water_wheel", 1, 5); var strong = place(h, "water_wheel", 5, 3);
            ticks(h, wheel, 1); ticks(h, still, 1); ticks(h, strong, 1);
            h.assertTrue(wheel.status() == 1 && wheel.energy.stored() == 6 && wheel.data.get(RATE) == 6, "Flowing water on one side gives 6 FE/t");
            h.assertTrue(still.status() == 15 && still.energy.stored() == 0, "Still water gives nothing and the wheel says what it needs");
            h.assertTrue(strong.energy.stored() == 16, "Falling water gives 10 FE/t, and sides add up");
            h.assertTrue(wheel.receiveEnergy(100, false) == 0, "A wheel is not charged by cable");
            upgrade(wheel, 1);
            ticks(h, wheel, 1);
            h.assertTrue(wheel.energy.stored() == 6 + Math.round(6 * MachineTier.get(1).generation()), "A better tier multiplies the output");
        } finally {
            for (BlockPos pos : water) h.setBlock(pos, Blocks.AIR);
        }
        h.succeed();
    }

    @GameTest(template = "empty", skyAccess = true)
    public static void windmillNeedsOpenAir(GameTestHelper h) {
        var level = h.getLevel();
        var open = place(h, "windmill", 2, 2); var blocked = place(h, "windmill", 5, 5);
        h.setBlock(new BlockPos(6, 2, 5), Blocks.STONE);
        h.assertTrue(level.canSeeSky(open.getBlockPos().above()), "The test area is under open sky");
        ticks(h, open, 1); ticks(h, blocked, 1);
        int base = Math.clamp(4 + (open.getBlockPos().getY() - 64) / 6, 4, 32);
        int expected = level.isThundering() ? base * 2 : level.isRaining() ? base * 3 / 2 : base;
        h.assertTrue(open.status() == 1 && open.energy.stored() == expected && open.data.get(RATE) == expected, "In open air the windmill makes at least 4 FE/t, more with height and bad weather");
        h.assertTrue(blocked.status() == 16 && blocked.energy.stored() == 0, "A solid block beside it stops the windmill and it says what it needs");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void thermoelectricNeedsHotAndCold(GameTestHelper h) {
        BlockPos[] lava = {new BlockPos(2, 2, 1), new BlockPos(2, 2, 5)};
        try {
            h.setBlock(lava[0], Blocks.LAVA); h.setBlock(new BlockPos(0, 2, 1), Blocks.BLUE_ICE); h.setBlock(lava[1], Blocks.LAVA);
            h.setBlock(new BlockPos(5, 1, 3), Blocks.MAGMA_BLOCK); h.setBlock(new BlockPos(5, 3, 3), Blocks.PACKED_ICE);
            var both = place(h, "thermoelectric_generator", 1, 1); var hotOnly = place(h, "thermoelectric_generator", 1, 5); var stacked = place(h, "thermoelectric_generator", 5, 3);
            ticks(h, both, 1); ticks(h, hotOnly, 1); ticks(h, stacked, 1);
            h.assertTrue(both.status() == 1 && both.energy.stored() == 48 && both.data.get(RATE) == 48, "Lava (16) and blue ice (3) give 48 FE/t");
            h.assertTrue(hotOnly.status() == 17 && hotOnly.energy.stored() == 0, "Lava alone gives nothing: a cold side is needed too");
            h.assertTrue(stacked.energy.stored() == 15, "Top and bottom count: magma (6) and packed ice (2.5) give 15 FE/t");
            h.assertBlockPresent(Blocks.LAVA, lava[0]);
        } finally {
            for (BlockPos pos : lava) h.setBlock(pos, Blocks.AIR);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void creativeSourceStaysFullAndChargesACell(GameTestHelper h) {
        var source = place(h, "creative_energy_source", 2, 2);
        h.assertTrue(source.receiveEnergy(1000, false) == 0 && !source.upgradeTo(1), "Nothing charges or upgrades a creative source");
        ticks(h, source, 1);
        h.assertTrue(source.capacity() == 100000000 && source.energy.stored() == source.capacity() && source.comparatorSignal() == 15, "It fills itself");
        var cell = place(h, "advanced_energy_cell", 3, 2);
        h.assertTrue(EnergyTransport.transfer(source) == 1000000 && cell.energy.stored() == 1000000, "It sends 1,000,000 FE in one tick to the cell beside it");
        h.assertTrue(EnergyTransport.transfer(source) == 0, "That is its limit for the tick");
        ticks(h, source, 1);
        h.assertTrue(source.energy.stored() == source.capacity(), "It is full again on its next tick");
        var second = place(h, "creative_energy_source", 2, 5); var far = place(h, "advanced_energy_cell", 4, 5);
        h.setBlock(new BlockPos(3, 2, 5), Technologia.BLOCKS.get("superconducting_energy_conduit"));
        second.energy.restore(second.capacity());
        h.assertTrue(EnergyTransport.transfer(second) == 32000 && far.energy.stored() == 32000, "A superconducting conduit carries 32,000 FE/t");
        h.succeed();
    }

    // ---- Wireless power. Each test keeps to its own channels: the grid is shared by the whole server. ----

    @GameTest(template = "empty")
    public static void wirelessSenderReachesItsChannelOnly(GameTestHelper h) {
        var sender = place(h, "wireless_sender", 1, 1); var receiver = place(h, "wireless_receiver", 6, 6); var cell = place(h, "energy_cell", 1, 2);
        cell.receiveEnergy(1000, false);
        h.assertTrue(EnergyTransport.transfer(cell) == 200 && sender.energy.stored() == 200, "A sender is charged through cables like any machine");
        h.assertTrue(receiver.receiveEnergy(1000, false) == 0, "A receiver cannot be charged by cable");
        for (int i = 0; i < 4; i++) click(h, sender, MachineMenu.BUTTON_CHANNEL_UP_10);
        h.assertTrue(click(h, sender, MachineMenu.BUTTON_CHANNEL_UP) && sender.data.get(MachineBlockEntity.DATA_CHANNEL) == 41, "The channel buttons step by ten and by one");
        receiver.changeChannel(-1);
        h.assertTrue(receiver.channel() == 255, "Channels wrap around below zero");
        receiver.changeChannel(43);
        cell.changeChannel(5);
        h.assertTrue(receiver.channel() == 42 && cell.channel() == 0 && !click(h, cell, MachineMenu.BUTTON_CHANNEL_UP), "Only wireless blocks have a channel");
        ticks(h, receiver, 1); ticks(h, sender, 1);
        h.assertTrue(sender.status() == 14 && sender.energy.stored() == 200 && receiver.energy.stored() == 0, "Nothing crosses to another channel, and the sender says no receiver is listening");
        receiver.changeChannel(-1);
        ticks(h, receiver, 1); ticks(h, sender, 1);
        h.assertTrue(receiver.energy.stored() == 200 && sender.energy.stored() == 0 && sender.status() == 1 && sender.data.get(RATE) == 200, "On the same channel the energy arrives");
        ticks(h, receiver, 1);
        h.assertTrue(receiver.status() == 1 && receiver.data.get(RATE) == 200, "The receiver shows what came in");
        ItemStack stack = item("wireless_receiver", 1);
        receiver.saveToItem(stack);
        var replaced = place(h, "wireless_receiver", 6, 1);
        BlockItem.updateCustomBlockEntityTag(h.getLevel(), null, replaced.getBlockPos(), stack);
        h.assertTrue(replaced.channel() == 41, "A receiver picked up and set down again keeps its channel");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void wirelessLimitCapsAndSharesEvenly(GameTestHelper h) {
        var sender = place(h, "wireless_sender", 1, 1); var first = place(h, "wireless_receiver", 6, 1); var second = place(h, "wireless_receiver", 6, 6);
        for (var machine : new MachineBlockEntity[] {sender, first, second}) machine.changeChannel(43);
        sender.receiveEnergy(100000, false);
        ticks(h, first, 1); ticks(h, second, 1);
        h.assertTrue(sender.wirelessLimit() == 50000, "A new sender allows 50,000 FE/t");
        h.assertTrue(click(h, sender, MachineMenu.BUTTON_LIMIT) && sender.wirelessLimit() == 100 && sender.data.get(MachineBlockEntity.DATA_LIMIT) == 0, "The limit button steps round to 100 FE/t");
        ticks(h, sender, 1);
        h.assertTrue(first.energy.stored() == 50 && second.energy.stored() == 50 && sender.energy.stored() == 99900 && sender.data.get(RATE) == 100, "100 FE/t is shared equally between two receivers");
        sender.cycleLimit();
        ticks(h, sender, 1);
        h.assertTrue(first.energy.stored() == 300 && second.energy.stored() == 300, "The next step is 500 FE/t");
        for (int step : new int[] {2000, 10000, 50000, 100}) { sender.cycleLimit(); h.assertTrue(sender.wirelessLimit() == step, "The limit cycles 100, 500, 2,000, 10,000 and 50,000 FE/t"); }
        first.energy.restore(first.capacity());
        ticks(h, sender, 1);
        h.assertTrue(second.energy.stored() == 400, "A full receiver leaves its share to the others");
        second.toggle(h.makeMockPlayer(GameType.SURVIVAL));
        ticks(h, sender, 1);
        h.assertTrue(second.energy.stored() == 400 && sender.energy.stored() == 99300 && sender.status() != 1, "A receiver that is switched off takes nothing");
        h.assertTrue(!click(h, first, MachineMenu.BUTTON_LIMIT), "Only senders have a limit");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void receiverFeedsMachinesAndCellsButNotSenders(GameTestHelper h) {
        var receiver = place(h, "wireless_receiver", 3, 3); var press = place(h, "metal_press", 4, 3); var cell = place(h, "energy_cell", 2, 3);
        var sender = place(h, "wireless_sender", 3, 4); var crusher = place(h, "crusher", 3, 1);
        h.setBlock(new BlockPos(3, 2, 2), Technologia.BLOCKS.get("energy_conduit"));
        // Leave little room in each target, so the result does not depend on which one is served first.
        receiver.energy.restore(60000); press.energy.restore(press.capacity() - 2000); cell.energy.restore(cell.capacity() - 1000);
        int moved = EnergyTransport.transfer(receiver);
        h.assertTrue(press.energy.stored() == press.capacity() && cell.energy.stored() == cell.capacity(), "A receiver feeds the machine and the cell beside it");
        h.assertTrue(crusher.energy.stored() == 200, "It feeds conduits too, at the conduit's rate");
        h.assertTrue(sender.energy.stored() == 0 && moved == 3200 && receiver.energy.stored() == 56800, "It never feeds a sender back, and gives up exactly what arrived");
        h.assertTrue(!MachineKind.CELL.feeds(MachineKind.WIRELESS_RECEIVER) && !MachineKind.GENERATOR.feeds(MachineKind.WIRELESS_RECEIVER) && !MachineKind.CELL.feeds(MachineKind.CREATIVE_SOURCE), "Nothing charges a receiver or a creative source through cables");
        h.assertTrue(MachineKind.CREATIVE_SOURCE.feeds(MachineKind.CELL) && MachineKind.CELL.feeds(MachineKind.WIRELESS_SENDER) && !MachineKind.CELL.feeds(MachineKind.ADVANCED_CELL), "Sources charge cells, cells charge senders, cells do not charge each other");
        h.succeed();
    }

    // ---- Sieves --------------------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void flintMeshDoublesGravelFragments(GameTestHelper h) {
        var sieve = place(h, "auto_sieve", 2, 2);
        h.assertTrue(sieve.installMesh(1) == 0 && sieve.mesh() == 1 && sieve.data.get(MachineBlockEntity.DATA_MESH) == 1, "Installing a flint mesh hands back the string mesh level");
        sieve.setItem(0, item(Items.GRAVEL, 1)); sieve.receiveEnergy(5000, false); ticks(h, sieve, 80);
        h.assertTrue(sieve.getItem(0).isEmpty() && sieve.getItem(OUT).is(Technologia.ITEMS.get("iron_fragment")) && sieve.getItem(OUT).getCount() == 2, "With a flint mesh gravel gives two iron fragments");
        h.assertTrue(sieve.energy.stored() == 3800, "A finer mesh costs no more energy");
        var drops = Block.getDrops(sieve.getBlockState(), h.getLevel(), sieve.getBlockPos(), sieve);
        h.assertTrue(drops.size() == 1 && drops.getFirst().has(DataComponents.BLOCK_ENTITY_DATA) && drops.getFirst().get(DataComponents.BLOCK_ENTITY_DATA).copyTag().getInt("Mesh") == 1, "The dropped sieve carries its mesh");
        var other = place(h, "auto_sieve", 4, 2);
        BlockItem.updateCustomBlockEntityTag(h.getLevel(), null, other.getBlockPos(), drops.getFirst());
        h.assertTrue(other.mesh() == 1, "Placing that item installs the mesh again");
        h.assertTrue(sieve.installMesh(3) == 1 && sieve.mesh() == 3, "A new mesh hands back the one it replaces");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void meshItemsInstallAndComeBack(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        var autoPos = new BlockPos(2, 2, 2); var handPos = new BlockPos(5, 2, 2); var pressPos = new BlockPos(2, 2, 5);
        var auto = place(h, "auto_sieve", 2, 2); var hand = (HandSieveBlockEntity) device(h, "hand_sieve", handPos); var press = place(h, "metal_press", 2, 5);
        player.setItemInHand(InteractionHand.MAIN_HAND, item("flint_mesh", 2));
        h.useBlock(autoPos, player);
        h.assertTrue(auto.mesh() == 1 && player.getMainHandItem().getCount() == 1, "Using a mesh on the Auto Sieve installs one");
        h.useBlock(autoPos, player);
        h.assertTrue(player.getMainHandItem().getCount() == 1, "The mesh already installed is not taken twice");
        h.useBlock(pressPos, player);
        h.assertTrue(press.mesh() == 0 && player.getMainHandItem().getCount() == 1, "Other machines take no mesh");
        h.useBlock(handPos, player);
        h.assertTrue(hand.mesh() == 1 && player.getMainHandItem().isEmpty(), "The Hand Sieve takes the same meshes");
        player.setItemInHand(InteractionHand.MAIN_HAND, item("diamond_mesh", 1));
        h.useBlock(autoPos, player);
        h.assertTrue(auto.mesh() == 3 && player.getInventory().countItem(Technologia.ITEMS.get("flint_mesh")) == 1, "A better mesh replaces the old one, which comes back to the player");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void handSieveShakesOutFragmentsAndDropsWhatItHolds(GameTestHelper h) {
        var pos = new BlockPos(3, 2, 3);
        var sieve = (HandSieveBlockEntity) device(h, "hand_sieve", pos);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        Item fragment = Technologia.ITEMS.get("iron_fragment");
        player.setItemInHand(InteractionHand.MAIN_HAND, item(Items.DIAMOND, 3));
        h.useBlock(pos, player);
        h.assertTrue(sieve.isEmpty() && player.getMainHandItem().getCount() == 3, "The sieve refuses an item it cannot sieve");
        player.setItemInHand(InteractionHand.MAIN_HAND, item(Items.COBBLESTONE, 3));
        h.useBlock(pos, player);
        h.assertTrue(sieve.isEmpty() && player.getMainHandItem().getCount() == 3, "A refused block stays in the hand");
        for (Direction side : Direction.values()) h.assertBlockNotPresent(Blocks.COBBLESTONE, pos.relative(side));
        player.setItemInHand(InteractionHand.MAIN_HAND, item(Items.GRAVEL, 5));
        h.useBlock(pos, player);
        h.assertTrue(!sieve.isEmpty() && player.getMainHandItem().getCount() == 4 && h.getBlockState(pos).getValue(HandSieveBlock.FILL) == 4, "It takes one block of gravel");
        for (int shake = 0; shake < 3; shake++) h.useBlock(pos, player);
        h.assertTrue(player.getMainHandItem().getCount() == 4 && h.getBlockState(pos).getValue(HandSieveBlock.FILL) == 1, "While it is full, using it shakes it and takes nothing more");
        h.assertItemEntityNotPresent(fragment, pos, 2);
        h.useBlock(pos, player);
        h.assertTrue(sieve.isEmpty() && h.getBlockState(pos).getValue(HandSieveBlock.FILL) == 0, "The fourth shake empties the tray");
        h.assertItemEntityCountIs(fragment, pos.above(), 1.5, 1);
        h.assertTrue(sieve.installMesh(2) == 0 && sieve.fill(item(Items.GRAVEL, 1)), "An iron mesh goes in, then more gravel");
        h.setBlock(pos, Blocks.AIR);
        h.assertItemEntityCountIs(Items.GRAVEL, pos, 1.5, 1);
        h.assertItemEntityCountIs(Technologia.ITEMS.get("iron_mesh"), pos, 1.5, 1);
        h.succeed();
    }

    // ---- New processors ------------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void enrichmentChamberEnriches(GameTestHelper h) {
        var chamber = place(h, "enrichment_chamber", 2, 2);
        h.assertTrue(chamber.canPlaceItem(0, item(Items.COAL, 1)) && !chamber.canPlaceItem(0, item(Items.DIRT, 1)), "It only accepts what it can enrich");
        chamber.setItem(0, item(Items.COAL, 1)); chamber.receiveEnergy(5000, false); ticks(h, chamber, 100);
        h.assertTrue(chamber.getItem(0).isEmpty() && chamber.getItem(OUT).is(Technologia.ITEMS.get("enriched_carbon")) && chamber.getItem(OUT).getCount() == 1 && chamber.energy.stored() == 3000, "Coal becomes enriched carbon for 2,000 FE");
        chamber.setItem(0, item(Items.REDSTONE, 3)); ticks(h, chamber, 100);
        h.assertTrue(chamber.getItem(0).getCount() == 3 && chamber.energy.stored() == 3000, "Three redstone wait for a fourth");
        chamber.setItem(0, item(Items.REDSTONE, 4)); ticks(h, chamber, 100);
        h.assertTrue(chamber.getItem(0).isEmpty() && chamber.countItem(Technologia.ITEMS.get("enriched_redstone")) == 1 && chamber.energy.stored() == 1000, "Four redstone make one enriched redstone");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void infuserTakesItsPairInEitherOrder(GameTestHelper h) {
        var first = place(h, "metallurgic_infuser", 2, 2); var second = place(h, "metallurgic_infuser", 4, 2);
        h.assertTrue(first.inputCount() == 2, "A Mk I infuser has one lane of two ingredients");
        first.setItem(0, item(Items.IRON_INGOT, 2));
        h.assertTrue(first.canPlaceItem(1, item("enriched_carbon", 1)) && first.canPlaceItem(1, item("enriched_redstone", 1)) && !first.canPlaceItem(1, item("enriched_diamond", 1)), "The second slot only takes a matching partner");
        first.setItem(1, item("enriched_carbon", 1));
        second.setItem(0, item("enriched_carbon", 1)); second.setItem(1, item(Items.IRON_INGOT, 2));
        for (var infuser : new MachineBlockEntity[] {first, second}) {
            infuser.receiveEnergy(20000, false); ticks(h, infuser, 160);
            h.assertTrue(infuser.getItem(0).isEmpty() && infuser.getItem(1).isEmpty(), "Both ingredients are used up, whichever slot they are in");
            h.assertTrue(infuser.getItem(OUT).is(Technologia.ITEMS.get("steel_ingot")) && infuser.getItem(OUT).getCount() == 2 && infuser.energy.stored() == 15200, "Two iron and one enriched carbon make two steel for 4,800 FE");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void phytoChamberGrowsFromOneSeed(GameTestHelper h) {
        var chamber = place(h, "phyto_chamber", 2, 2);
        chamber.setItem(0, item(Items.WHEAT_SEEDS, 1)); chamber.receiveEnergy(20000, false); ticks(h, chamber, 399);
        h.assertTrue(chamber.getItem(OUT).isEmpty(), "Growing takes 400 ticks");
        ticks(h, chamber, 1);
        h.assertTrue(chamber.getItem(0).isEmpty() && chamber.getItem(OUT).is(Items.WHEAT) && chamber.getItem(OUT).getCount() == 2 && chamber.getItem(OUT + 1).is(Items.WHEAT_SEEDS), "One seed gives two wheat and the seed back");
        h.assertTrue(chamber.energy.stored() == 12000, "A crop costs 8,000 FE");
        chamber.setItem(0, item(Items.OAK_SAPLING, 1)); ticks(h, chamber, 400);
        h.assertTrue(chamber.countItem(Items.OAK_LOG) == 4 && chamber.countItem(Items.OAK_SAPLING) == 1, "A sapling gives four logs and comes back");
        h.succeed();
    }

    // ---- Area machines -------------------------------------------------------------------------

    /** The owner is a real online player, as for the miner; the loader's own protection hook answers for the crop. */
    @GameTest(template = "empty", timeoutTicks = 200)
    public static void harvesterReplantsRipeWheatAndLeavesTheRest(GameTestHelper h) {
        var owner = h.makeMockServerPlayerInLevel();
        var harvester = place(h, "auto_harvester", 3, 3);
        harvester.setOwner(owner.getUUID()); harvester.receiveEnergy(1000, false);
        BlockPos ripe = new BlockPos(4, 2, 3), young = new BlockPos(2, 2, 3);
        wheat(h, ripe, 7); wheat(h, young, 3);
        h.assertTrue(Technologia.BREAK_PERMISSION.test(owner, h.absolutePos(ripe)), "The protection hook allows an unprotected crop");
        h.succeedWhen(() -> {
            h.assertTrue(age(h, ripe) == 0, "Ripe wheat is cut and replanted as a seedling");
            h.assertTrue(harvester.countItem(Items.WHEAT) == 1 && harvester.energy.stored() == 960 && harvester.status() == 1, "The wheat is in the results and one harvest cost 40 FE");
            h.assertTrue(age(h, young) >= 3, "Wheat that is not ripe is left to grow");
            h.getLevel().getServer().getPlayerList().remove(owner);
        });
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void harvesterWaitsForItsOwner(GameTestHelper h) {
        var harvester = place(h, "auto_harvester", 3, 3);
        harvester.setOwner(UUID.randomUUID()); harvester.receiveEnergy(1000, false);
        BlockPos ripe = new BlockPos(4, 2, 3);
        wheat(h, ripe, 7);
        ServerPlayer[] owner = new ServerPlayer[1];
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(harvester.status() == 6, "A harvester whose owner is away says so"))
                .thenIdle(30)
                .thenExecute(() -> {
                    h.assertTrue(age(h, ripe) == 7 && harvester.energy.stored() == 1000 && harvester.countItem(Items.WHEAT) == 0, "Without its owner it harvests nothing and spends nothing");
                    owner[0] = h.makeMockServerPlayerInLevel();
                    harvester.setOwner(owner[0].getUUID());
                })
                .thenWaitUntil(() -> h.assertTrue(age(h, ripe) == 0 && harvester.countItem(Items.WHEAT) == 1, "Once the owner is online in the same world it harvests"))
                .thenExecute(() -> h.getLevel().getServer().getPlayerList().remove(owner[0]))
                .thenSucceed();
    }

    /**
     * The accelerator picks plants at random and a growth tick only sometimes makes wheat older, so
     * this test waits for it. No player is near a headless test, so nothing else makes the wheat grow.
     */
    @GameTest(template = "empty", timeoutTicks = 1000)
    public static void acceleratorAgesWheatOnlyWithEnergy(GameTestHelper h) {
        var accelerator = place(h, "growth_accelerator", 3, 3);
        List<BlockPos> field = new ArrayList<>();
        for (int x = 1; x <= 5; x++) for (int z = 1; z <= 5; z++) if (x != 3 || z != 3) field.add(new BlockPos(x, 2, z));
        for (BlockPos crop : field) wheat(h, crop, 0);
        h.assertTrue(field.stream().allMatch(crop -> age(h, crop) == 0), "The wheat field is planted");
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(accelerator.status() == 2, "Without energy the accelerator finds a plant and asks for power"))
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(accelerator.energy.stored() == 0 && field.stream().allMatch(crop -> age(h, crop) == 0), "Without energy no plant grows");
                    accelerator.receiveEnergy(3000, false);
                })
                .thenWaitUntil(() -> {
                    int spent = 3000 - accelerator.energy.stored();
                    h.assertTrue(spent > 0 && spent % 15 == 0, "Every growth tick it gives costs 15 FE");
                    h.assertTrue(field.stream().anyMatch(crop -> age(h, crop) > 0) && accelerator.status() == 1, "Wheat in reach grows older");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void vacuumCollectsDropsUnlessFull(GameTestHelper h) {
        var vacuum = place(h, "vacuum_collector", 3, 3);
        vacuum.receiveEnergy(1000, false);
        for (int slot = OUT; slot < END; slot++) vacuum.setItem(slot, item(Items.COBBLESTONE, 64));
        // Floors keep the items where they were dropped: one in reach, one just beyond three blocks.
        h.setBlock(new BlockPos(5, 1, 3), Blocks.STONE); h.setBlock(new BlockPos(7, 1, 3), Blocks.STONE);
        var dropped = drop(h, item(Items.DIAMOND, 5), 5.5, 2, 3.5);
        var beyond = drop(h, item(Items.EMERALD, 1), 7.5, 2, 3.5);
        h.startSequence()
                .thenWaitUntil(() -> h.assertTrue(vacuum.status() == 3, "With full results the collector reports that it has no room"))
                .thenExecute(() -> {
                    h.assertTrue(dropped.isAlive() && dropped.getItem().getCount() == 5 && vacuum.energy.stored() == 1000, "The item is left on the ground and no energy is spent");
                    vacuum.setItem(OUT, item(Items.DIAMOND, 62));
                })
                .thenWaitUntil(() -> h.assertTrue(vacuum.getItem(OUT).getCount() == 64 && dropped.isAlive() && dropped.getItem().getCount() == 3 && vacuum.energy.stored() == 996, "It takes what fits for 4 FE and leaves the rest"))
                .thenExecute(() -> vacuum.setItem(OUT + 1, ItemStack.EMPTY))
                .thenWaitUntil(() -> h.assertTrue(!dropped.isAlive() && vacuum.getItem(OUT + 1).is(Items.DIAMOND) && vacuum.getItem(OUT + 1).getCount() == 3 && vacuum.energy.stored() == 992, "A free slot lets it pull in the rest of the stack"))
                .thenExecute(() -> h.assertTrue(beyond.isAlive() && vacuum.countItem(Items.EMERALD) == 0, "An item beyond its reach stays where it is"))
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void chunkLoaderHoldsItsChunksWhilePoweredAndRunning(GameTestHelper h) {
        var level = h.getLevel();
        var pos = new BlockPos(3, 2, 3);
        var loader = place(h, "chunk_loader", 3, 3);
        ChunkPos chunk = new ChunkPos(loader.getBlockPos());
        long own = ChunkPos.asLong(chunk.x, chunk.z);
        // The test framework forces the chunks of its own structures. They are let go here so the
        // loader's claim can be seen, and put back at the end whatever happens.
        Set<Long> before = new HashSet<>(level.getForcedChunks());
        List<ChunkPos> area = ChunkPos.rangeClosed(chunk, 1).toList();
        area.forEach(held -> level.setChunkForced(held.x, held.z, false));
        try {
            ticks(h, loader, 1);
            h.assertTrue(loader.status() == 2 && !level.getForcedChunks().contains(own), "Without power nothing is loaded");
            loader.receiveEnergy(999, false); ticks(h, loader, 1);
            h.assertTrue(loader.status() == 2 && loader.energy.stored() == 999 && !level.getForcedChunks().contains(own), "It takes its area only with 100 ticks of energy in store");
            loader.receiveEnergy(1, false); ticks(h, loader, 1);
            h.assertTrue(loader.status() == 1 && loader.energy.stored() == 990 && level.getForcedChunks().contains(own), "A powered loader forces its own chunk for 10 FE/t");
            h.assertTrue(area.stream().filter(held -> level.getForcedChunks().contains(held.toLong())).count() == 1, "A Mk I loader holds one chunk only");
            ticks(h, loader, 98);
            h.assertTrue(loader.status() == 1 && loader.energy.stored() == 10 && level.getForcedChunks().contains(own), "Once it runs, it keeps the area down to its last tick of energy");
            var player = h.makeMockPlayer(GameType.SURVIVAL);
            loader.energy.restore(2000);
            loader.toggle(player); ticks(h, loader, 1);
            h.assertTrue(!level.getForcedChunks().contains(own) && loader.energy.stored() == 2000, "Switched off, it gives the chunk back and spends nothing");
            loader.toggle(player); ticks(h, loader, 100);
            h.assertTrue(loader.status() == 19 && !level.getForcedChunks().contains(own) && loader.energy.stored() == 2000,
                    "Switched on again, it first runs 100 ticks without an area, so a redstone clock cannot make chunks load and unload every few ticks");
            ticks(h, loader, 1);
            h.assertTrue(loader.status() == 1 && level.getForcedChunks().contains(own) && loader.energy.stored() == 1990, "Then it takes the chunk back");
            loader.cycleRedstone(); ticks(h, loader, 1);
            h.assertTrue(loader.status() == 13 && !level.getForcedChunks().contains(own), "Paused by redstone, it lets go too");
            loader.cycleRedstone(); loader.cycleRedstone();
            loader.energy.restore(5); ticks(h, loader, 1);
            h.assertTrue(loader.status() == 2 && !level.getForcedChunks().contains(own) && loader.energy.stored() == 5, "It does not start on too little energy");
            upgrade(loader, 2);
            int cost = (int) Math.round(9 * 10 * MachineTier.get(2).efficiency());
            loader.energy.restore(cost * 100); ticks(h, loader, 101);
            h.assertTrue(area.stream().allMatch(held -> level.getForcedChunks().contains(held.toLong())) && loader.energy.stored() == cost * 99, "A Mk III loader holds 3x3 chunks and pays for nine");
            upgrade(loader, 3); ticks(h, loader, 1);
            h.assertTrue(loader.status() == 1 && area.stream().allMatch(held -> level.getForcedChunks().contains(held.toLong())), "A new tier with the same area keeps every chunk without a pause");
            h.assertTrue(loader.setTier(0), "The loader is set back to Mk I");
            ticks(h, loader, 1);
            h.assertTrue(loader.status() == 1 && level.getForcedChunks().contains(own) && area.stream().filter(held -> level.getForcedChunks().contains(held.toLong())).count() == 1,
                    "With a smaller area it swaps the old one for its own chunk in the same tick");
            h.setBlock(pos, Blocks.AIR);
            h.assertTrue(area.stream().noneMatch(held -> level.getForcedChunks().contains(held.toLong())), "Removing the block releases every chunk");
        } finally {
            area.forEach(held -> level.setChunkForced(held.x, held.z, before.contains(held.toLong())));
        }
        h.succeed();
    }

    // ---- Devices -------------------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void cratesHoldTheirSizeAndDropTheirContents(GameTestHelper h) {
        String[] ids = {"wooden_crate", "bronze_crate", "steel_crate", "resonant_crate"};
        int[] sizes = {27, 36, 45, 54};
        Item[] cargo = {Items.DIAMOND, Items.EMERALD, Items.GOLD_INGOT, Items.IRON_INGOT};
        var registries = h.getLevel().registryAccess();
        for (int i = 0; i < ids.length; i++) {
            var pos = new BlockPos(1 + 2 * i, 2, 3);
            var crate = (CrateBlockEntity) device(h, ids[i], pos);
            h.assertTrue(crate != null && crate.getContainerSize() == sizes[i], ids[i] + " holds " + sizes[i] + " slots");
            crate.setItem(sizes[i] - 1, item(cargo[i], 3 + i));
            var handler = h.getLevel().getCapability(Capabilities.ItemHandler.BLOCK, crate.getBlockPos(), Direction.UP);
            h.assertTrue(handler != null && handler.getSlots() == sizes[i] && handler.getStackInSlot(sizes[i] - 1).getCount() == 3 + i, ids[i] + " is open to pipes and hoppers");
            h.assertTrue(h.getBlockState(pos).getAnalogOutputSignal(h.getLevel(), h.absolutePos(pos)) == 1, ids[i] + " gives a comparator reading");
            var restored = (CrateBlockEntity) BlockEntity.loadStatic(crate.getBlockPos(), crate.getBlockState(), crate.saveWithFullMetadata(registries), registries);
            h.assertTrue(restored != null && restored.getContainerSize() == sizes[i] && restored.getItem(sizes[i] - 1).getCount() == 3 + i, ids[i] + " keeps its contents when saved");
            h.setBlock(pos, Blocks.AIR);
            h.assertItemEntityCountIs(cargo[i], pos, 1.5, 3 + i);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void bonsaiPotGrowsLogsFromOneSapling(GameTestHelper h) {
        var level = h.getLevel();
        var pos = new BlockPos(3, 2, 3);
        var pot = (BonsaiPotBlockEntity) device(h, "bonsai_pot", pos);
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        h.assertTrue(pot.canPlaceItem(0, item(Items.OAK_SAPLING, 1)) && !pot.canPlaceItem(0, item(Items.DIRT, 1)) && !pot.canPlaceItem(0, item(Items.OAK_LOG, 1)), "The pot takes saplings and nothing else");
        var hopper = new SimpleContainer(item(Items.OAK_LOG, 8), item(Items.BIRCH_SAPLING, 4));
        h.assertTrue(ItemTransferBlockEntity.move(hopper, Direction.DOWN, pot, Direction.UP, ItemStack.EMPTY, 8) == 1 && pot.sapling().is(Items.BIRCH_SAPLING) && pot.sapling().getCount() == 1, "Automation plants exactly one sapling");
        for (int slot = 1; slot < BonsaiPotBlockEntity.SLOTS; slot++)
            h.assertTrue(pot.getItem(slot).isEmpty() && !pot.canPlaceItemThroughFace(slot, item(Items.OAK_LOG, 1), Direction.UP) && !pot.canPlaceItemThroughFace(slot, item(Items.OAK_SAPLING, 1), Direction.UP), "Nothing can be pushed into the result slots");
        var handler = level.getCapability(Capabilities.ItemHandler.BLOCK, pot.getBlockPos(), Direction.UP);
        h.assertTrue(handler != null && handler.insertItem(1, item(Items.OAK_LOG, 1), false).getCount() == 1, "A pipe is refused there as well");
        h.assertTrue(h.getBlockState(pos).getValue(BonsaiPotBlock.STAGE) == 1 && pot.percent() == 0, "A planted pot shows a sprout");
        player.setItemInHand(InteractionHand.MAIN_HAND, item(Items.BONE_MEAL, 2));
        h.useBlock(pos, player);
        h.assertTrue(pot.percent() == 25 && player.getMainHandItem().getCount() == 1, "Bone meal adds a quarter of the growth");
        for (int i = 0; i < BonsaiPotBlockEntity.GROW_TICKS / 4; i++) BonsaiPotBlockEntity.tick(level, pot.getBlockPos(), pot.getBlockState(), pot);
        h.assertTrue(pot.percent() == 50 && h.getBlockState(pos).getValue(BonsaiPotBlock.STAGE) == 2, "Half grown, it shows a small tree");
        for (int i = 0; i < BonsaiPotBlockEntity.GROW_TICKS / 2; i++) BonsaiPotBlockEntity.tick(level, pot.getBlockPos(), pot.getBlockState(), pot);
        h.assertTrue(pot.isGrown() && h.getBlockState(pos).getValue(BonsaiPotBlock.STAGE) == 3 && pot.getItem(1).isEmpty(), "After 1,200 ticks the tree is grown");
        player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
        // A grown tree is cut once a second, so the wood arrives through the game's own ticking.
        h.startSequence()
                .thenWaitUntil(() -> {
                    h.assertTrue(pot.getItem(1).is(Items.BIRCH_LOG) && pot.getItem(1).getCount() >= 2 && pot.getItem(1).getCount() <= 3, "A grown tree gives two or three logs of its own wood");
                    h.assertTrue(pot.sapling().is(Items.BIRCH_SAPLING) && !pot.isGrown() && h.getBlockState(pos).getValue(BonsaiPotBlock.STAGE) == 1, "The sapling stays and starts again");
                })
                .thenExecute(() -> {
                    h.assertTrue(pot.comparatorSignal() >= 1 && pot.canTakeItemThroughFace(1, pot.getItem(1), Direction.DOWN) && !pot.canTakeItemThroughFace(0, pot.sapling(), Direction.DOWN), "A hopper below takes the wood, never the sapling");
                    h.useBlock(pos, player);
                    h.assertTrue(player.getInventory().countItem(Items.BIRCH_LOG) >= 2 && pot.getItem(1).isEmpty() && !pot.sapling().isEmpty(), "An empty hand takes the wood and leaves the sapling");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void vectorPlatePushesAlongItsFacing(GameTestHelper h) {
        var level = h.getLevel();
        var pos = new BlockPos(3, 2, 3);
        h.setBlock(pos.below(), Blocks.STONE);
        BlockState plate = Technologia.BLOCKS.get("vector_plate").defaultBlockState().setValue(VectorPlateBlock.FACING, Direction.EAST);
        BlockState fast = Technologia.BLOCKS.get("fast_vector_plate").defaultBlockState();
        h.setBlock(pos, plate);
        BlockPos absolute = h.absolutePos(pos);
        double speed = ((VectorPlateBlock) plate.getBlock()).speed;
        // The plate acts on whatever is inside its block space; the item need not be in the world for that.
        var rider = new ItemEntity(level, absolute.getX() + .5, absolute.getY() + .1, absolute.getZ() + .5, item(Items.DIAMOND, 1));
        rider.setDeltaMovement(0, 0, 0.05);
        plate.entityInside(level, absolute, rider);
        h.assertTrue(Math.abs(rider.getDeltaMovement().x - speed / 2) < 1e-9 && rider.getDeltaMovement().y == 0 && rider.getDeltaMovement().z == 0.05, "The plate pushes an item the way it points and leaves its other motion alone");
        for (int i = 0; i < 40; i++) plate.entityInside(level, absolute, rider);
        h.assertTrue(rider.getDeltaMovement().x > speed * 0.99 && rider.getDeltaMovement().x <= speed + 1e-9, "The push settles at the plate's speed and never goes past it");
        rider.setDeltaMovement(Vec3.ZERO);
        fast.entityInside(level, absolute, rider);
        h.assertTrue(rider.getDeltaMovement().z < -speed / 2 && rider.getDeltaMovement().x == 0, "A fast plate pushes harder; set down plainly it points north");
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        player.setShiftKeyDown(true);
        plate.entityInside(level, absolute, player);
        h.assertTrue(player.getDeltaMovement().x == 0, "A crouching player is not carried");
        player.setShiftKeyDown(false);
        plate.entityInside(level, absolute, player);
        h.assertTrue(player.getDeltaMovement().x > 0, "A standing player is");
        player.setItemInHand(InteractionHand.MAIN_HAND, item("wrench", 1));
        h.useBlock(pos, player);
        h.assertTrue(h.getBlockState(pos).getValue(VectorPlateBlock.FACING) == Direction.SOUTH, "A wrench turns the plate a quarter turn");
        // A plate needs a solid block under it.
        h.setBlock(pos.below(), Blocks.AIR);
        h.assertBlockPresent(Blocks.AIR, pos);
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void vectorPlatesCarryADroppedItem(GameTestHelper h) {
        BlockState plate = Technologia.BLOCKS.get("vector_plate").defaultBlockState().setValue(VectorPlateBlock.FACING, Direction.EAST);
        for (int x = 1; x <= 6; x++) h.setBlock(new BlockPos(x, 1, 3), Blocks.STONE);
        for (int x = 1; x <= 4; x++) h.setBlock(new BlockPos(x, 2, 3), plate);
        var rider = drop(h, item(Items.DIAMOND, 1), 1.5, 2.1, 3.5);
        double startX = rider.getX(), startZ = rider.getZ();
        h.succeedWhen(() -> h.assertTrue(rider.isAlive() && rider.getX() > startX + 2 && Math.abs(rider.getZ() - startZ) < 0.3, "A dropped item rides a row of plates in the direction they point"));
    }

    // ---- Tools ---------------------------------------------------------------------------------

    @GameTest(template = "empty")
    public static void magnetPullsItemsOnlyWhenSwitchedOn(GameTestHelper h) {
        var level = h.getLevel();
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        Vec3 feet = h.absoluteVec(new Vec3(1.5, 2, 1.5));
        player.setPos(feet.x, feet.y, feet.z);
        ItemStack magnet = item("magnet", 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, magnet);
        h.setBlock(new BlockPos(4, 1, 1), Blocks.STONE); h.setBlock(new BlockPos(1, 1, 7), Blocks.STONE);
        var close = drop(h, item(Items.DIAMOND, 1), 4.5, 2, 1.5);
        var beyond = drop(h, item(Items.EMERALD, 1), 1.5, 2, 7.5);
        Runnable carried = () -> magnet.inventoryTick(level, player, 0, true);
        h.assertTrue(!MagnetItem.isOn(magnet), "A new magnet is off");
        // The magnet only looks every fourth tick, so each state is held for four ticks.
        h.startSequence()
                .thenExecuteFor(4, () -> { carried.run(); h.assertTrue(close.distanceTo(player) > 2, "A magnet that is off pulls nothing"); })
                .thenExecute(() -> {
                    magnet.use(level, player, InteractionHand.MAIN_HAND);
                    h.assertTrue(MagnetItem.isOn(magnet), "Using it switches it on");
                    player.setShiftKeyDown(true);
                })
                .thenExecuteFor(4, () -> { carried.run(); h.assertTrue(close.distanceTo(player) > 2, "Crouching pauses the magnet"); })
                .thenExecute(() -> player.setShiftKeyDown(false))
                .thenWaitUntil(() -> { carried.run(); h.assertTrue(close.distanceTo(player) < 0.01, "Switched on, it brings the item to its holder"); })
                .thenExecute(() -> {
                    h.assertTrue(beyond.distanceTo(player) > 5, "An item beyond five blocks stays where it is");
                    magnet.use(level, player, InteractionHand.MAIN_HAND);
                    h.assertTrue(!MagnetItem.isOn(magnet), "Using it again switches it off");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void travelStaffGoesToTheBlockInSightForOnePearl(GameTestHelper h) {
        var level = h.getLevel();
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        Vec3 feet = h.absoluteVec(new Vec3(1.5, 2, 3.5));
        player.setPos(feet.x, feet.y, feet.z);
        // Looking east, level: the eyes are in the block row above the feet.
        player.setYRot(-90); player.setYHeadRot(-90); player.setXRot(0);
        ItemStack staff = item("travel_staff", 1);
        player.setItemInHand(InteractionHand.MAIN_HAND, staff);
        h.setBlock(new BlockPos(5, 3, 3), Blocks.STONE);
        // Grass at eye height between the player and the stone: it has an outline but nothing to stand against.
        h.setBlock(new BlockPos(3, 2, 3), Blocks.DIRT); h.setBlock(new BlockPos(3, 3, 3), Blocks.SHORT_GRASS);
        h.assertTrue(staff.use(level, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.FAIL && player.position().distanceTo(feet) < 1e-6, "Without an ender pearl the staff does nothing");
        player.getInventory().add(item(Items.ENDER_PEARL, 2));
        h.assertTrue(staff.use(level, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.CONSUME, "With a pearl it works");
        h.assertTrue(player.position().distanceTo(h.absoluteVec(new Vec3(4.5, 3, 3.5))) < 1e-6, "It looks through the grass and stands the player in the free space in front of the block they looked at");
        h.assertTrue(player.getInventory().countItem(Items.ENDER_PEARL) == 1 && player.getCooldowns().isOnCooldown(staff.getItem()), "One pearl is used and the staff needs a moment");
        h.setBlock(new BlockPos(4, 7, 3), Blocks.STONE);
        player.setXRot(-90);
        h.assertTrue(staff.use(level, player, InteractionHand.MAIN_HAND).getResult() == InteractionResult.CONSUME && player.position().distanceTo(h.absoluteVec(new Vec3(4.5, 5, 3.5))) < 1e-6, "Looking up at a ceiling puts the player two blocks below it");
        h.assertTrue(player.getInventory().countItem(Items.ENDER_PEARL) == 0, "Each journey costs one pearl");
        h.succeed();
    }

    // ---- Recipes -------------------------------------------------------------------------------

    private static final String[] CRAFTED = {
            "water_wheel", "windmill", "thermoelectric_generator", "enrichment_chamber", "metallurgic_infuser", "phyto_chamber", "auto_harvester",
            "growth_accelerator", "vacuum_collector", "chunk_loader", "wireless_sender", "wireless_receiver", "superconducting_energy_conduit",
            "compressed_cobblestone", "compressed_gravel", "compressed_sand", "wooden_crate", "bronze_crate", "steel_crate", "resonant_crate",
            "bonsai_pot", "hand_sieve", "flint_mesh", "iron_mesh", "diamond_mesh", "vector_plate", "fast_vector_plate", "magnet", "reinforced_magnet",
            "resonant_magnet", "travel_staff", "floor_panel", "catwalk", "steel_table", "steel_stool", "metal_shelf", "tool_cabinet", "warning_light"};
    /** Recipes that make more than one item. */
    private static final Map<String, Integer> BATCHES = Map.of("superconducting_energy_conduit", 8, "vector_plate", 8, "fast_vector_plate", 4,
            "floor_panel", 8, "catwalk", 8, "steel_table", 4, "steel_stool", 4, "metal_shelf", 4, "tool_cabinet", 2, "warning_light", 4);

    @GameTest(template = "empty")
    public static void craftingAndSmeltingRecipesLoad(GameTestHelper h) {
        var recipes = h.getLevel().getRecipeManager(); var registries = h.getLevel().registryAccess();
        for (String id : CRAFTED) {
            var recipe = recipes.byKey(Technologia.id(id));
            h.assertTrue(recipe.isPresent(), "The crafting recipe " + id + " loads");
            ItemStack result = recipe.get().value().getResultItem(registries);
            h.assertTrue(result.is(Technologia.ITEMS.get(id)) && result.getCount() == BATCHES.getOrDefault(id, 1), "The recipe " + id + " makes " + BATCHES.getOrDefault(id, 1) + " of that item");
        }
        Item[] loose = {Items.COBBLESTONE, Items.GRAVEL, Items.SAND};
        for (Item block : loose) {
            String id = BuiltInRegistries.ITEM.getKey(block).getPath() + "_from_compressed";
            var recipe = recipes.byKey(Technologia.id(id));
            h.assertTrue(recipe.isPresent() && recipe.get().value().getResultItem(registries).is(block) && recipe.get().value().getResultItem(registries).getCount() == 9, "The recipe " + id + " gives nine blocks back");
        }
        for (String creative : new String[] {"creative_energy_source", "creative_tier_kit"})
            h.assertTrue(Technologia.ITEMS.containsKey(creative) && recipes.byKey(Technologia.id(creative)).isEmpty(), creative + " exists and has no recipe");
        for (String metal : new String[] {"silver", "nickel"}) for (String source : new String[] {"raw_" + metal, metal + "_ore", "deepslate_" + metal + "_ore", metal + "_dust"}) {
            var input = new SingleRecipeInput(item(source, 1));
            Item ingot = Technologia.ITEMS.get(metal + "_ingot");
            var smelted = recipes.getRecipeFor(RecipeType.SMELTING, input, h.getLevel()); var blasted = recipes.getRecipeFor(RecipeType.BLASTING, input, h.getLevel());
            h.assertTrue(smelted.isPresent() && smelted.get().value().getResultItem(registries).is(ingot), source + " smelts into " + metal + "_ingot");
            h.assertTrue(blasted.isPresent() && blasted.get().value().getResultItem(registries).is(ingot), source + " blasts into " + metal + "_ingot");
        }
        h.succeed();
    }

    private static boolean crafts(GameTestHelper h, String id, int width, ItemStack... grid) {
        var recipe = h.getLevel().getRecipeManager().byKey(Technologia.id(id));
        return recipe.isPresent() && recipe.get().value() instanceof CraftingRecipe crafting
                && crafting.matches(CraftingInput.of(width, grid.length / width, List.of(grid)), h.getLevel());
    }

    @GameTest(template = "empty")
    public static void shapedRecipesFollowTheManifest(GameTestHelper h) {
        ItemStack none = ItemStack.EMPTY, planks = item(Items.BIRCH_PLANKS, 1), stick = item(Items.STICK, 1), string = item(Items.STRING, 1), flint = item(Items.FLINT, 1);
        ItemStack iron = item(Items.IRON_INGOT, 1), redstone = item(Items.REDSTONE, 1), frame = item("machine_frame", 1), brick = item(Items.BRICK, 1);
        h.assertTrue(crafts(h, "wooden_crate", 3, planks, planks, planks, planks, item(Items.CHEST, 1), planks, planks, planks, planks), "Wooden Crate: any planks around a chest");
        h.assertTrue(crafts(h, "hand_sieve", 3, planks, string, planks, planks, string, planks, stick, none, stick), "Hand Sieve: planks, string and two stick legs");
        h.assertTrue(crafts(h, "flint_mesh", 3, string, flint, string, flint, string, flint, string, flint, string), "Flint Mesh: string and flint in a checker");
        h.assertTrue(crafts(h, "iron_mesh", 3, item(Items.IRON_NUGGET, 1), string, item(Items.IRON_NUGGET, 1), string, item("flint_mesh", 1), string, item(Items.IRON_NUGGET, 1), string, item(Items.IRON_NUGGET, 1)), "Iron Mesh: built on a flint mesh");
        h.assertTrue(crafts(h, "magnet", 3, redstone, none, item(Items.LAPIS_LAZULI, 1), iron, none, iron, iron, iron, iron), "Item Magnet: a horseshoe of iron with redstone and lapis tips");
        h.assertTrue(crafts(h, "bonsai_pot", 3, brick, none, brick, brick, item(Items.DIRT, 1), brick, none, brick, none), "Bonsai Pot: bricks around dirt");
        h.assertTrue(crafts(h, "vector_plate", 3, item("iron_plate", 1), item(Items.SLIME_BALL, 1), item("iron_plate", 1), redstone, item("gold_plate", 1), redstone), "Vector Plate: two rows");
        h.assertTrue(crafts(h, "water_wheel", 3, planks, stick, planks, stick, frame, stick, planks, item(Items.COPPER_INGOT, 1), planks), "Water Wheel");
        h.assertTrue(crafts(h, "superconducting_energy_conduit", 3, none, item(Items.BLUE_ICE, 1), none, item("electrum_plate", 1), item("resonant_alloy", 1), item("electrum_plate", 1), none, item(Items.BLUE_ICE, 1), none), "Superconducting Energy Conduit");
        h.assertTrue(crafts(h, "chunk_loader", 3, item("reinforced_alloy", 1), item(Items.ENDER_EYE, 1), item("reinforced_alloy", 1), item("advanced_circuit", 1), frame, item("advanced_circuit", 1), item("reinforced_alloy", 1), item("resonite", 1), item("reinforced_alloy", 1)), "Chunk Loader");
        ItemStack cobble = item(Items.COBBLESTONE, 1);
        h.assertTrue(crafts(h, "compressed_cobblestone", 3, cobble, cobble, cobble, cobble, cobble, cobble, cobble, cobble, cobble) && crafts(h, "cobblestone_from_compressed", 1, item("compressed_cobblestone", 1)), "Nine cobblestone pack into one block and back");
        ItemStack eye = item(Items.ENDER_EYE, 1), rod = item(Items.BLAZE_ROD, 1);
        h.assertTrue(crafts(h, "travel_staff", 3, none, none, eye, none, rod, none, rod, none, none), "Travel Staff: an ender eye on two blaze rods, set diagonally");
        h.succeed();
    }

    /** Finds a recipe by what goes in and what comes out, so the test does not depend on recipe file names. */
    private static MachineRecipe makes(GameTestHelper h, MachineKind kind, ItemStack result, int time, int energy, ItemStack... in) {
        var input = new MachineRecipeInput(kind, List.of(in));
        String name = kind.id + ": " + in[0].getItem() + " -> " + result.getItem();
        MachineRecipe found = null;
        for (var holder : h.getLevel().getRecipeManager().getAllRecipesFor(MachineRecipe.TYPE)) {
            MachineRecipe recipe = holder.value();
            if (recipe.machine() == kind && recipe.mesh() == 0 && recipe.matches(input, h.getLevel()) && recipe.outputs().getFirst().is(result.getItem())) found = recipe;
        }
        h.assertTrue(found != null, "The recipe loads, " + name);
        h.assertTrue(found.outputs().getFirst().getCount() == result.getCount() && found.time() == time && found.energy() == energy, "Result count, time and energy are as listed, " + name);
        int[] used = found.consumption(input);
        for (int i = 0; i < in.length; i++) h.assertTrue(used[i] == in[i].getCount(), "Ingredient counts are as listed, " + name);
        return found;
    }
    private static void also(GameTestHelper h, MachineRecipe recipe, ItemStack extra, double chance) {
        var outputs = recipe.outputs();
        h.assertTrue(outputs.size() == 2 && outputs.get(1).is(extra.getItem()) && outputs.get(1).getCount() == extra.getCount() && Math.abs(recipe.byproductChance() - chance) < 0.001,
                "The byproduct is as listed, " + recipe.machine().id + ": " + extra.getItem());
    }
    private static Item vanilla(String id) { return BuiltInRegistries.ITEM.get(ResourceLocation.withDefaultNamespace(id)); }

    @GameTest(template = "empty")
    public static void processingRecipesMatchTheManifest(GameTestHelper h) {
        MachineKind crusher = MachineKind.CRUSHER, alloy = MachineKind.ALLOY_SMELTER, press = MachineKind.METAL_PRESS, compactor = MachineKind.COMPACTOR;
        MachineKind enrichment = MachineKind.ENRICHMENT_CHAMBER, infuser = MachineKind.INFUSER, phyto = MachineKind.PHYTO_CHAMBER;
        makes(h, crusher, item("silver_dust", 2), 100, 20, item("raw_silver", 1));
        makes(h, crusher, item("nickel_dust", 2), 100, 20, item("raw_nickel", 1));
        makes(h, crusher, item("silver_dust", 1), 60, 20, item("silver_fragment", 4));
        makes(h, crusher, item("nickel_dust", 1), 60, 20, item("nickel_fragment", 4));
        makes(h, crusher, item(Items.REDSTONE, 2), 60, 20, item("redstone_fragment", 4));
        makes(h, crusher, item(Items.LAPIS_LAZULI, 2), 60, 20, item("lapis_fragment", 4));
        makes(h, crusher, item(Items.DIAMOND, 1), 120, 30, item("diamond_fragment", 4));
        makes(h, alloy, item("electrum_ingot", 2), 160, 30, item(Items.GOLD_INGOT, 1), item("silver_ingot", 1));
        makes(h, alloy, item("invar_ingot", 3), 200, 30, item(Items.IRON_INGOT, 2), item("nickel_ingot", 1));
        makes(h, alloy, item("constantan_ingot", 2), 160, 30, item(Items.COPPER_INGOT, 1), item("nickel_ingot", 1));
        makes(h, press, item("invar_plate", 1), 60, 15, item("invar_ingot", 1));
        makes(h, press, item("electrum_plate", 1), 60, 15, item("electrum_ingot", 1));
        makes(h, compactor, item("compressed_cobblestone", 1), 100, 20, item(Items.COBBLESTONE, 9));
        makes(h, compactor, item("compressed_gravel", 1), 100, 20, item(Items.GRAVEL, 9));
        // Compressed sand is crafted only, so the compactor has a single recipe for sand: sandstone.
        makes(h, compactor, item(Items.SANDSTONE, 1), 60, 15, item(Items.SAND, 4));
        makes(h, enrichment, item("enriched_carbon", 1), 100, 20, item(Items.COAL, 1));
        makes(h, enrichment, item("enriched_carbon", 1), 100, 20, item(Items.CHARCOAL, 1));
        makes(h, enrichment, item("enriched_redstone", 1), 100, 20, item(Items.REDSTONE, 4));
        makes(h, enrichment, item("enriched_diamond", 1), 100, 20, item(Items.DIAMOND, 1));
        makes(h, enrichment, item("enriched_resonite", 1), 100, 20, item("resonite", 1));
        makes(h, enrichment, item(Items.GLOWSTONE_DUST, 4), 100, 20, item(Items.GLOWSTONE, 1));
        makes(h, enrichment, item(Items.QUARTZ, 4), 100, 20, item(Items.QUARTZ_BLOCK, 1));
        makes(h, infuser, item("steel_ingot", 2), 160, 30, item(Items.IRON_INGOT, 2), item("enriched_carbon", 1));
        makes(h, infuser, item("infused_alloy", 1), 160, 30, item(Items.IRON_INGOT, 1), item("enriched_redstone", 1));
        makes(h, infuser, item("reinforced_alloy", 1), 160, 30, item("infused_alloy", 1), item("enriched_diamond", 1));
        makes(h, infuser, item("resonant_alloy", 2), 160, 30, item(Items.COPPER_INGOT, 2), item("enriched_resonite", 1));
        also(h, makes(h, phyto, item(Items.WHEAT, 2), 400, 20, item(Items.WHEAT_SEEDS, 1)), item(Items.WHEAT_SEEDS, 1), 1);
        also(h, makes(h, phyto, item(Items.BEETROOT, 2), 400, 20, item(Items.BEETROOT_SEEDS, 1)), item(Items.BEETROOT_SEEDS, 1), 1);
        also(h, makes(h, phyto, item(Items.MELON_SLICE, 4), 400, 20, item(Items.MELON_SEEDS, 1)), item(Items.MELON_SEEDS, 1), 0.5);
        also(h, makes(h, phyto, item(Items.PUMPKIN, 1), 400, 20, item(Items.PUMPKIN_SEEDS, 1)), item(Items.PUMPKIN_SEEDS, 1), 1);
        for (Item crop : new Item[] {Items.CARROT, Items.POTATO, Items.SUGAR_CANE, Items.CACTUS, Items.NETHER_WART, Items.SWEET_BERRIES, Items.KELP, Items.COCOA_BEANS})
            makes(h, phyto, item(crop, 3), 400, 20, item(crop, 1));
        makes(h, phyto, item(Items.BAMBOO, 4), 400, 20, item(Items.BAMBOO, 1));
        for (String wood : new String[] {"oak", "spruce", "birch", "jungle", "acacia", "dark_oak", "cherry"})
            also(h, makes(h, phyto, item(vanilla(wood + "_log"), 4), 400, 20, item(vanilla(wood + "_sapling"), 1)), item(vanilla(wood + "_sapling"), 1), 1);
        makes(h, phyto, item("resonance_bloom", 2), 600, 30, item("resonance_bloom", 1));
        h.succeed();
    }

    private static void sieves(GameTestHelper h, Item block, String id, int mesh, int time, ItemStack result, ItemStack extra, double chance) {
        var found = RecipeIndex.best(h.getLevel(), new MachineRecipeInput(MachineKind.SIEVE, List.of(new ItemStack(block)), mesh));
        h.assertTrue(found.isPresent() && found.get().id().equals(Technologia.id("sieving/" + id)), "Mesh " + mesh + " uses the recipe sieving/" + id);
        MachineRecipe recipe = found.get().value();
        h.assertTrue(recipe.time() == time && recipe.energy() == 15, "sieving/" + id + " takes " + time + " ticks at 15 FE/t");
        h.assertTrue(recipe.outputs().getFirst().is(result.getItem()) && recipe.outputs().getFirst().getCount() == result.getCount(), "sieving/" + id + " gives " + result.getCount() + " " + result.getItem());
        also(h, recipe, extra, chance);
    }

    @GameTest(template = "empty")
    public static void sieveRecipesFollowTheMesh(GameTestHelper h) {
        sieves(h, Items.GRAVEL, "gravel", 0, 80, item("iron_fragment", 1), item("tin_fragment", 1), 0.35);
        sieves(h, Items.GRAVEL, "gravel_flint", 1, 80, item("iron_fragment", 2), item("nickel_fragment", 1), 0.30);
        sieves(h, Items.GRAVEL, "gravel_iron", 2, 80, item("iron_fragment", 2), item("redstone_fragment", 1), 0.35);
        sieves(h, Items.GRAVEL, "gravel_diamond", 3, 80, item("iron_fragment", 3), item("diamond_fragment", 1), 0.08);
        sieves(h, Items.SAND, "sand", 0, 80, item("copper_fragment", 1), item("gold_fragment", 1), 0.10);
        sieves(h, Items.SAND, "sand_flint", 1, 80, item("copper_fragment", 2), item("silver_fragment", 1), 0.25);
        sieves(h, Items.SAND, "sand_iron", 2, 80, item("gold_fragment", 1), item("lapis_fragment", 1), 0.30);
        sieves(h, Items.SAND, "sand_diamond", 3, 80, item("gold_fragment", 2), item("lapis_fragment", 1), 0.50);
        sieves(h, Items.COARSE_DIRT, "coarse_dirt", 0, 80, item("lead_fragment", 1), item(Items.FLINT, 1), 0.20);
        sieves(h, Items.COARSE_DIRT, "coarse_dirt_flint", 1, 80, item("lead_fragment", 2), item("tin_fragment", 1), 0.50);
        sieves(h, Items.COARSE_DIRT, "coarse_dirt_iron", 2, 80, item("tin_fragment", 2), item("nickel_fragment", 1), 0.40);
        sieves(h, Items.COARSE_DIRT, "coarse_dirt_diamond", 3, 80, item("silver_fragment", 2), item("redstone_fragment", 1), 0.40);
        // Compressed blocks have one recipe, whatever mesh is installed.
        sieves(h, Technologia.ITEMS.get("compressed_gravel"), "compressed_gravel", 3, 480, item("iron_fragment", 9), item("tin_fragment", 3), 1);
        sieves(h, Technologia.ITEMS.get("compressed_sand"), "compressed_sand", 0, 480, item("copper_fragment", 9), item("gold_fragment", 1), 1);
        h.assertTrue(RecipeIndex.best(h.getLevel(), new MachineRecipeInput(MachineKind.SIEVE, List.of(item("compressed_cobblestone", 1)), 3)).isEmpty(), "Compressed cobblestone is not sieved");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void handSieveLeavesCompressedBlocksToTheAutoSieve(GameTestHelper h) {
        var sieve = (HandSieveBlockEntity) device(h, "hand_sieve", new BlockPos(2, 2, 2));
        h.assertTrue(!sieve.fill(item("compressed_gravel", 1)) && sieve.isEmpty(), "A compressed block is refused: nine blocks for four shakes would be free work");
        h.assertTrue(sieve.fill(item(Items.GRAVEL, 1)) && !sieve.isEmpty(), "Plain gravel is accepted");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void chunkLoaderLeavesChunksForcedBySomeoneElse(GameTestHelper h) {
        var loader = place(h, "chunk_loader", 2, 2);
        long chunk = new ChunkPos(loader.getBlockPos()).toLong();
        // The test framework keeps this chunk forced, as a /forceload command would.
        h.assertTrue(h.getLevel().getForcedChunks().contains(chunk), "The chunk is already forced before the loader runs");
        loader.receiveEnergy(50000, false);
        ticks(h, loader, 2);
        h.assertTrue(loader.status() == 1, "The loader runs");
        h.assertTrue(click(h, loader, MachineMenu.BUTTON_TOGGLE) && !loader.isEnabled(), "The loader is switched off");
        ticks(h, loader, 2);
        h.assertTrue(h.getLevel().getForcedChunks().contains(chunk), "A chunk the loader did not force itself stays forced");
        h.setBlock(new BlockPos(2, 2, 2), Blocks.AIR);
        h.assertTrue(h.getLevel().getForcedChunks().contains(chunk), "Removing the loader does not release it either");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void receiverPausedByRedstoneTakesNoPower(GameTestHelper h) {
        var sender = place(h, "wireless_sender", 1, 2);
        var receiver = place(h, "wireless_receiver", 5, 2);
        for (int i = 0; i < 45; i++) { sender.changeChannel(1); receiver.changeChannel(1); }
        sender.receiveEnergy(100000, false);
        ticks(h, receiver, 1);
        ticks(h, sender, 1);
        int first = receiver.energy.stored();
        h.assertTrue(first > 0, "A running receiver on the channel is powered");
        // Mode 1 runs only with a redstone signal, and there is none.
        receiver.cycleRedstone();
        ticks(h, sender, 1);
        h.assertTrue(receiver.energy.stored() == first, "A receiver held back by redstone takes nothing");
        receiver.cycleRedstone(); receiver.cycleRedstone();
        ticks(h, sender, 1);
        h.assertTrue(receiver.energy.stored() > first, "Back on 'any', it is powered again");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void chunkLoaderNeverHidesAPendingSaveOfForcedChunks(GameTestHelper h) {
        var level = h.getLevel();
        var registries = level.registryAccess();
        var loader = place(h, "chunk_loader", 2, 2);
        long chunk = new ChunkPos(loader.getBlockPos()).toLong();
        h.assertTrue(level.getForcedChunks().contains(chunk), "The test framework keeps this chunk forced");
        var saved = level.getDataStorage().computeIfAbsent(ForcedChunksSavedData.factory(), "chunks");
        // As after a /forceload command that has not been written to disk yet. Asking the game to
        // force a chunk that is forced already would mark this list as saved, and the command would be lost.
        saved.setDirty(true);
        loader.receiveEnergy(50000, false);
        ticks(h, loader, 2);
        h.assertTrue(loader.status() == 1 && saved.isDirty(), "Claiming a chunk that is already forced leaves the pending save alone");
        // A reloaded loader claims its area again, as its regular refresh does.
        loader.loadWithComponents(loader.saveWithFullMetadata(registries), registries);
        ticks(h, loader, 1);
        h.assertTrue(loader.status() == 1 && saved.isDirty(), "Claiming again leaves it alone too");
        h.assertTrue(click(h, loader, MachineMenu.BUTTON_TOGGLE) && !loader.isEnabled(), "The loader is switched off");
        ticks(h, loader, 1);
        h.assertTrue(saved.isDirty() && level.getForcedChunks().contains(chunk), "Letting go of a chunk it never forced changes nothing");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void idleReceiverIsNotShownAsWorking(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        var receiver = place(h, "wireless_receiver", 2, 2);
        ticks(h, receiver, 12);
        h.assertTrue(receiver.status() == 0 && !h.getBlockState(pos).getValue(MachineBlock.ACTIVE), "A receiver that never got power is ready, with a dark front");
        h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 200)
    public static void vacuumLeavesItemsThatCannotBePickedUp(GameTestHelper h) {
        var vacuum = place(h, "vacuum_collector", 3, 3);
        vacuum.receiveEnergy(1000, false);
        h.setBlock(new BlockPos(5, 1, 3), Blocks.STONE);
        var display = drop(h, item(Items.DIAMOND_BLOCK, 1), 5.5, 2, 3.5);
        display.setNeverPickUp();
        h.startSequence()
                .thenIdle(40)
                .thenExecute(() -> {
                    h.assertTrue(display.isAlive() && vacuum.countItem(Items.DIAMOND_BLOCK) == 0 && vacuum.energy.stored() == 1000, "A display item that can never be picked up stays where it is");
                    display.setNoPickUpDelay();
                })
                .thenWaitUntil(() -> h.assertTrue(!display.isAlive() && vacuum.countItem(Items.DIAMOND_BLOCK) == 1, "Once a player could pick it up, the collector takes it"))
                .thenSucceed();
    }

    /** Cactus blocks tick at random at any height, but only the top of a column shorter than three can grow. */
    @GameTest(template = "empty", timeoutTicks = 600)
    public static void acceleratorDoesNotChargeForPlantsThatCannotGrow(GameTestHelper h) {
        var accelerator = (MachineBlockEntity) device(h, "growth_accelerator", new BlockPos(3, 3, 3));
        List<BlockPos> columns = new ArrayList<>();
        // No cactus may touch another block sideways, so they stand on every second space.
        for (int x = 1; x <= 5; x++) for (int z = 1; z <= 5; z++) if ((x + z) % 2 == 0 && (x != 3 || z != 3)) columns.add(new BlockPos(x, 1, z));
        for (BlockPos base : columns) {
            h.setBlock(base, Blocks.STONE); h.setBlock(base.above(), Blocks.SAND);
            for (int up = 2; up <= 4; up++) h.setBlock(base.above(up), Blocks.CACTUS);
        }
        accelerator.receiveEnergy(3000, false);
        h.startSequence()
                .thenIdle(60)
                .thenExecute(() -> {
                    h.assertTrue(columns.stream().allMatch(base -> h.getBlockState(base.above(4)).is(Blocks.CACTUS)), "The cactus field stands three high");
                    h.assertTrue(accelerator.energy.stored() == 3000 && accelerator.status() == 0, "Full-grown columns in reach cost nothing and leave the accelerator ready");
                    for (BlockPos base : columns) h.setBlock(base.above(4), Blocks.AIR);
                })
                .thenWaitUntil(() -> {
                    int spent = 3000 - accelerator.energy.stored();
                    h.assertTrue(spent > 0 && spent % 15 == 0 && accelerator.status() == 1, "Cut back to two high, the columns are worked on again for 15 FE a growth tick");
                })
                .thenSucceed();
    }

    @GameTest(template = "empty")
    public static void handedBackItemsAreNeverLost(GameTestHelper h) {
        var level = h.getLevel();
        var pos = new BlockPos(2, 2, 2);
        var sievePos = new BlockPos(5, 2, 5);
        var player = h.makeMockServerPlayerInLevel();
        var rule = level.getGameRules().getRule(GameRules.RULE_DOBLOCKDROPS);
        boolean before = rule.get();
        try {
            // A creative inventory without a free slot, but with room for one more log in a stack: the
            // game would take that one log and throw the others away.
            player.setGameMode(GameType.CREATIVE);
            for (int slot = 0; slot < 36; slot++) player.getInventory().setItem(slot, item(Items.DIRT, 64));
            player.getInventory().setItem(5, item(Items.BIRCH_LOG, 63));
            Technologia.giveOrDrop(player, item(Items.BIRCH_LOG, 3), h.absolutePos(pos));
            h.assertTrue(player.getInventory().countItem(Items.BIRCH_LOG) == 63, "A full creative inventory is left as it is");
            h.assertItemEntityCountIs(Items.BIRCH_LOG, pos, 1.5, 3);
            // These items are handed back or made by a device; they are not the drops of a broken block.
            rule.set(false, level.getServer());
            player.setGameMode(GameType.SURVIVAL);
            Technologia.giveOrDrop(player, item(Items.GOLD_INGOT, 2), h.absolutePos(pos));
            h.assertItemEntityCountIs(Items.GOLD_INGOT, pos, 1.5, 2);
            var sieve = (HandSieveBlockEntity) device(h, "hand_sieve", sievePos);
            h.assertTrue(sieve.fill(item(Items.GRAVEL, 1)), "The sieve takes gravel");
            for (int shake = 0; shake < HandSieveBlockEntity.SHAKES; shake++) sieve.shake();
            h.assertItemEntityCountIs(Technologia.ITEMS.get("iron_fragment"), sievePos.above(), 1.5, 1);
        } finally {
            rule.set(before, level.getServer());
            level.getServer().getPlayerList().remove(player);
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void crateKeepsItsCustomNameWhenBroken(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2);
        var crate = (CrateBlockEntity) device(h, "steel_crate", pos);
        crate.applyComponents(DataComponentMap.builder().set(DataComponents.CUSTOM_NAME, Component.literal("Ores")).build(), DataComponentPatch.EMPTY);
        var drops = Block.getDrops(h.getBlockState(pos), h.getLevel(), h.absolutePos(pos), crate);
        h.assertTrue(drops.size() == 1 && drops.getFirst().is(Technologia.ITEMS.get("steel_crate")), "A broken crate drops itself");
        h.assertTrue(Component.literal("Ores").equals(drops.getFirst().get(DataComponents.CUSTOM_NAME)), "A crate renamed in an anvil keeps its name when it is broken");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void chunkLoaderOnlyTrustsItsOwnSavedArea(GameTestHelper h) {
        var registries = h.getLevel().registryAccess();
        var loader = place(h, "chunk_loader", 2, 2);
        ChunkPos own = new ChunkPos(loader.getBlockPos());
        long near = own.toLong(), far = ChunkPos.asLong(own.x + 40, own.z);
        var tag = loader.saveWithFullMetadata(registries);
        tag.putInt("Loaded", 0);
        tag.putLongArray("Forced", new long[] {far, near});
        // Data put on a loader that is already in the world, as a command or a pasted block does.
        loader.loadWithComponents(tag, registries);
        var live = loader.saveWithFullMetadata(registries);
        h.assertTrue(!live.contains("Loaded") && !live.contains("Forced"), "A loader in the world does not take an area from pasted data");
        // The same data read with the chunk, as after a restart.
        var restored = (MachineBlockEntity) BlockEntity.loadStatic(loader.getBlockPos(), loader.getBlockState(), tag, registries);
        var saved = restored.saveWithFullMetadata(registries);
        h.assertTrue(saved.getInt("Loaded") == 0 && java.util.Arrays.equals(saved.getLongArray("Forced"), new long[] {near}),
                "A loaded loader keeps the chunks near itself and drops one it could never have forced");
        h.succeed();
    }
}
