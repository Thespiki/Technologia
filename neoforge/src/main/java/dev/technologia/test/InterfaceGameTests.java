package dev.technologia.test;

import com.mojang.brigadier.CommandDispatcher;
import dev.technologia.Technologia;
import dev.technologia.TechnologiaCommands;
import dev.technologia.guide.GuideItem;
import dev.technologia.machine.*;
import dev.technologia.storage.StorageMenu;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.gametest.framework.*;
import net.minecraft.network.chat.Component;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.PipeBlock;
import net.minecraft.world.level.block.Rotation;
import net.neoforged.neoforge.gametest.*;

/** Server-side conservation and lifecycle tests for the shared interfaces. */
@GameTestHolder(Technologia.MOD_ID)
@PrefixGameTestTemplate(false)
public final class InterfaceGameTests {
    private static Player player(GameTestHelper helper) { return helper.makeMockPlayer(GameType.SURVIVAL); }
    private static StorageMenu menu(Player player, SimpleContainer storage) { return new StorageMenu(1, player.getInventory(), storage); }
    private static int button(StorageMenu menu, StorageMenu.Action action) { return StorageMenu.actionButton(menu.entries().getFirst(), action); }

    @GameTest(template = "empty")
    public static void storageAggregatesWithoutOwningItems(GameTestHelper h) {
        var storage = new SimpleContainer(54);
        for (int i = 0; i < 15; i++) storage.setItem(i, new ItemStack(Items.COAL, 64));
        storage.setItem(15, new ItemStack(Items.COAL, 40));
        ItemStack named = new ItemStack(Items.COAL, 3);
        named.set(DataComponents.CUSTOM_NAME, Component.literal("Special coal"));
        storage.setItem(16, named);
        var menu = menu(player(h), storage);
        h.assertTrue(menu.entries().size() == 2 && menu.entries().getFirst().count() == 1000, "Identical items aggregate; components stay distinct");
        menu.entries().getFirst().stack().setCount(1);
        h.assertTrue(menu.totalItems() == 1003 && storage.getItem(0).getCount() == 64, "Catalogue snapshots must not mutate stored stacks");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void withdrawalsAndDepositsConserveItems(GameTestHelper h) {
        var player = player(h); var storage = new SimpleContainer(54);
        storage.setItem(0, new ItemStack(Items.COAL, 40)); storage.setItem(1, new ItemStack(Items.COAL, 40));
        var menu = menu(player, storage);
        h.assertTrue(menu.clickMenuButton(player, button(menu, StorageMenu.Action.ONE)), "Withdraw one");
        h.assertTrue(menu.getCarried().getCount() == 1 && menu.totalItems() == 79, "One-item withdrawal conserves total");
        menu.clickMenuButton(player, button(menu, StorageMenu.Action.STACK));
        h.assertTrue(menu.getCarried().getCount() == 64 && menu.totalItems() == 16, "Withdrawal fills cursor only to stack limit across slots");
        menu.clickMenuButton(player, StorageMenu.DEPOSIT_BUTTON);
        h.assertTrue(menu.getCarried().isEmpty() && menu.totalItems() == 80, "Deposit returns all carried items");
        menu.clickMenuButton(player, button(menu, StorageMenu.Action.TO_INVENTORY));
        h.assertTrue(player.getInventory().countItem(Items.COAL) == 64 && menu.totalItems() == 16, "Shift withdrawal moves one stack into inventory");
        for (int i = 54; i < menu.slots.size(); i++) if (menu.slots.get(i).hasItem()) menu.quickMoveStack(player, i);
        h.assertTrue(menu.totalItems() == 80 && player.getInventory().countItem(Items.COAL) == 0, "Shift deposit conserves items");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullStoragePreservesCursor(GameTestHelper h) {
        var player = player(h); var storage = new SimpleContainer(54);
        for (int i = 0; i < 54; i++) storage.setItem(i, new ItemStack(Items.COAL, 64));
        var menu = menu(player, storage); menu.setCarried(new ItemStack(Items.COAL, 12));
        h.assertTrue(!menu.clickMenuButton(player, StorageMenu.DEPOSIT_BUTTON) && menu.getCarried().getCount() == 12, "Full core must leave cursor untouched");
        storage.getItem(0).shrink(3);
        menu.clickMenuButton(player, StorageMenu.DEPOSIT_BUTTON);
        h.assertTrue(menu.getCarried().getCount() == 9 && menu.totalItems() == 3456, "Partial deposit takes only available room");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void fullInventoryAndDifferentCursorRejectWithdrawal(GameTestHelper h) {
        var player = player(h); var storage = new SimpleContainer(54);
        storage.setItem(0, new ItemStack(Items.COAL, 40)); var menu = menu(player, storage);
        for (int i = 0; i < 36; i++) player.getInventory().setItem(i, new ItemStack(Items.DIAMOND, 64));
        h.assertTrue(!menu.clickMenuButton(player, button(menu, StorageMenu.Action.TO_INVENTORY)), "Full inventory rejects withdrawal");
        menu.setCarried(new ItemStack(Items.DIAMOND, 7));
        h.assertTrue(!menu.clickMenuButton(player, button(menu, StorageMenu.Action.STACK)), "Different cursor identity must not be replaced");
        h.assertTrue(menu.totalItems() == 40 && menu.getCarried().getCount() == 7, "Failed actions preserve both inventories");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void staleAndForgedStorageActionsAreRejected(GameTestHelper h) {
        var player = player(h); var storage = new SimpleContainer(54);
        storage.setItem(0, new ItemStack(Items.COAL, 40)); var menu = menu(player, storage);
        int stale = button(menu, StorageMenu.Action.STACK);
        storage.setItem(0, new ItemStack(Items.DIAMOND, 4));
        h.assertTrue(!menu.clickMenuButton(player, stale), "A stale coal click must never take replacement diamonds");
        int current = button(menu, StorageMenu.Action.STACK);
        h.assertTrue(!menu.clickMenuButton(player, (current & ~63) | 63), "Invalid backing slot rejected");
        h.assertTrue(!menu.clickMenuButton(player, current | 192), "Unknown action rejected");
        menu.clicked(0, 0, ClickType.PICKUP, player);
        h.assertTrue(menu.quickMoveStack(player, 0).isEmpty() && menu.getCarried().isEmpty() && menu.totalItems() == 4, "Raw slot packets cannot bypass catalogue actions");
        h.succeed();
    }

    @GameTest(template = "empty")
    public static void disconnectedTerminalRejectsOpenMenuActions(GameTestHelper h) {
        var corePos = new BlockPos(2, 2, 2); var cablePos = new BlockPos(3, 2, 2); var terminalPos = new BlockPos(4, 2, 2);
        h.setBlock(corePos, Technologia.BLOCKS.get("storage_core")); h.setBlock(cablePos, Technologia.BLOCKS.get("network_cable"));
        h.setBlock(terminalPos, Technologia.BLOCKS.get("storage_terminal"));
        var core = (MachineBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(corePos)); core.setItem(0, new ItemStack(Items.COAL, 8));
        var player = player(h); var absolute = h.absolutePos(terminalPos); player.setPos(absolute.getX() + .5, absolute.getY(), absolute.getZ() + .5);
        var menu = new StorageMenu(1, player.getInventory(), StorageNetwork.connectedContainer(core, absolute));
        h.assertTrue(menu.stillValid(player), "Connected terminal opens nearby"); int button = button(menu, StorageMenu.Action.STACK);
        h.setBlock(cablePos, Blocks.AIR);
        h.assertTrue(!menu.stillValid(player) && !menu.clickMenuButton(player, button), "Disconnect invalidates even an already open menu");
        h.assertTrue(core.getItem(0).getCount() == 8, "Disconnected core items remain intact"); h.succeed();
    }

    @GameTest(template = "empty")
    public static void machineStateChangesKeepInventory(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2); h.setBlock(pos, Technologia.BLOCKS.get("crusher"));
        var absolute = h.absolutePos(pos); var machine = (MachineBlockEntity) h.getLevel().getBlockEntity(absolute);
        machine.setItem(0, new ItemStack(Items.RAW_IRON, 7)); machine.receiveEnergy(45000, false);
        var state = machine.getBlockState().setValue(MachineBlock.ACTIVE, true).rotate(Rotation.CLOCKWISE_90);
        h.getLevel().setBlockAndUpdate(absolute, state);
        h.assertTrue(h.getLevel().getBlockEntity(absolute) == machine && machine.getItem(0).getCount() == 7 && machine.energy.stored() == 45000, "Changing facing or activity must preserve the original block entity");
        h.assertTrue(state.getValue(MachineBlock.FACING) == Direction.EAST, "North front rotates east"); h.succeed();
    }

    @GameTest(template = "empty", timeoutTicks = 30)
    public static void activityTracksRealProcessing(GameTestHelper h) {
        var pos = new BlockPos(2, 2, 2); h.setBlock(pos, Technologia.BLOCKS.get("crusher"));
        var machine = (MachineBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
        machine.setItem(0, new ItemStack(Items.RAW_IRON)); machine.receiveEnergy(10000, false);
        h.runAtTickTime(5, () -> {
            h.assertTrue(machine.getBlockState().getValue(MachineBlock.ACTIVE), "Powered processing lights the front");
            machine.toggle(player(h));
        });
        h.runAtTickTime(10, () -> {
            h.assertTrue(!machine.getBlockState().getValue(MachineBlock.ACTIVE) && machine.getItem(0).getCount() == 1, "Pausing clears activity and retains unfinished input"); h.succeed();
        });
    }

    @GameTest(template = "empty")
    public static void cableGeometryFollowsNeighbors(GameTestHelper h) {
        var cable = new BlockPos(3, 2, 2); h.setBlock(cable, Technologia.BLOCKS.get("network_cable"));
        h.setBlock(cable.east(), Technologia.BLOCKS.get("storage_core"));
        h.assertTrue(h.getBlockState(cable).getValue(PipeBlock.EAST), "Cable connects to core");
        h.setBlock(cable.east(), Blocks.AIR);
        h.assertTrue(!h.getBlockState(cable).getValue(PipeBlock.EAST), "Cable arm disappears after core removal");
        h.setBlock(cable.west(), Technologia.BLOCKS.get("crusher"));
        h.assertTrue(!h.getBlockState(cable).getValue(PipeBlock.WEST), "Data cable does not pretend to power a crusher"); h.succeed();
    }

    @GameTest(template = "empty")
    public static void guideIsCraftableAndPublic(GameTestHelper h) {
        var stack = new ItemStack(Technologia.ITEMS.get("field_guide"));
        h.assertTrue(stack.getItem() instanceof GuideItem && stack.get(DataComponents.WRITTEN_BOOK_CONTENT).pages().size() == 18, "Guide has readable book pages");
        var articles = dev.technologia.guide.FieldGuide.pages();
        var chapters = dev.technologia.guide.FieldGuide.chapters();
        h.assertTrue(articles.size() == 17 && chapters.size() == 9, "Native atlas exposes all articles and chapters");
        for (int i = 0; i < articles.size(); i++) {
            var article = articles.get(i); var chapter = chapters.get(article.chapter());
            h.assertTrue(!article.title().isBlank() && !article.paragraphs().isEmpty() && i >= chapter.firstPage() && i < chapter.lastPage(), "Every article belongs to its visible chapter");
        }
        var reader = player(h); var guideMenu = new dev.technologia.guide.GuideMenu(1, reader.getInventory());
        h.assertTrue(guideMenu.slots.isEmpty() && guideMenu.quickMoveStack(reader, 0).isEmpty(), "Reading the guide must expose no inventory transfers");
        h.assertTrue(h.getLevel().getRecipeManager().byKey(Technologia.id("field_guide")).isPresent(), "Guide crafting recipe loads");
        var dispatcher = new CommandDispatcher<CommandSourceStack>(); TechnologiaCommands.register(dispatcher);
        var source = h.getLevel().getServer().createCommandSourceStack().withPermission(0);
        var root = dispatcher.getRoot().getChild("technologia");
        h.assertTrue(root.canUse(source) && root.getChild("guide").canUse(source) && !root.getChild("status").canUse(source), "Guide is public; diagnostics remain operator-only"); h.succeed();
    }
}
