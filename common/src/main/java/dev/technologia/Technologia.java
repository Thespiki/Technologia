package dev.technologia;

import dev.technologia.machine.*;
import dev.technologia.config.Balance;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import java.util.*;
import java.util.function.BiPredicate;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;
import net.minecraft.core.Direction;

/** Loader-neutral content. Initialized by the loader during registration. */
public final class Technologia {
    public static final String MOD_ID = "technologia";
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    public static BlockEntityType<MachineBlockEntity> MACHINE_TYPE;
    public static MenuType<MachineMenu> MACHINE_MENU;
    public static CreativeModeTab TAB;
    public static Balance BALANCE = Balance.defaults();
    // Destructive automation fails closed until a loader installs its protection hook.
    public static BiPredicate<ServerPlayer, BlockPos> BREAK_PERMISSION = (player, pos) -> false;
    public static BiConsumer<MachineBlockEntity, Direction> ENERGY_EXPORT = (machine, side) -> {};

    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }

    public static void createBlocks() {
        if (!BLOCKS.isEmpty()) return;
        addBlock("machine_frame", new Block(BlockBehaviour.Properties.of().strength(4).requiresCorrectToolForDrops()));
        for (MachineKind kind : MachineKind.values()) {
            addBlock(kind.id, new MachineBlock(kind, BlockBehaviour.Properties.of().strength(4).requiresCorrectToolForDrops()));
        }
        addBlock("network_cable", new Block(BlockBehaviour.Properties.of().strength(1).requiresCorrectToolForDrops()));
        for (String ore : List.of("tin_ore", "deepslate_tin_ore", "lead_ore", "deepslate_lead_ore", "resonite_ore")) {
            addBlock(ore, new Block(BlockBehaviour.Properties.of().strength(3, 3).requiresCorrectToolForDrops()));
        }
    }
    public static void createItems() {
        if (!ITEMS.isEmpty()) return;
        BLOCKS.forEach((name, block) -> ITEMS.put(name, new BlockItem(block, new Item.Properties())));
        for (String name : List.of("raw_tin", "raw_lead", "tin_ingot", "lead_ingot", "resonite", "iron_dust", "gold_dust", "copper_dust", "tin_dust", "lead_dust", "basic_circuit", "advanced_circuit", "draconic_core", "chaotic_core")) {
            ITEMS.put(name, new Item(new Item.Properties()));
        }
    }
    public static Block[] machineBlocks() { return BLOCKS.values().stream().filter(b -> b instanceof MachineBlock).toArray(Block[]::new); }
    public static void createTab() {
        TAB = CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemGroup.technologia"))
                .icon(() -> new ItemStack(ITEMS.get("storage_core")))
                .displayItems((parameters, output) -> ITEMS.values().forEach(output::accept)).build();
    }

    private static void addBlock(String name, Block block) {
        BLOCKS.put(name, block);
    }
    private Technologia() {}
}
