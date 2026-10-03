package dev.technologia;

import dev.technologia.machine.*;
import dev.technologia.config.Balance;
import dev.technologia.logistics.EnergyConduitBlock;
import dev.technologia.logistics.ItemTransferBlock;
import dev.technologia.nature.ResonanceBloomBlock;
import dev.technologia.storage.StorageMenu;
import dev.technologia.guide.GuideItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import java.util.*;
import java.util.function.BiPredicate;

/** Loader-neutral content. Initialized by the loader during registration. */
public final class Technologia {
    public static final String MOD_ID = "technologia";
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    /** Blocks with see-through textures; each loader's client entry point gives them a cutout layer. */
    public static final List<String> CUTOUT_BLOCKS = List.of("reinforced_glass", "resonance_bloom");
    public static final List<String> DECOR = List.of("steel_casing", "bronze_casing", "industrial_bricks", "steel_grate", "hazard_block",
            "engineering_lamp", "steel_pillar", "ventilation_grille", "control_panel", "reinforced_glass");
    /** Decor with a distinct front; it faces the player when placed and turns with the wrench. */
    public static final Set<String> DIRECTIONAL_DECOR = Set.of("ventilation_grille", "control_panel");
    public static final List<String> FRAGMENTS = List.of("iron_fragment", "copper_fragment", "gold_fragment", "tin_fragment", "lead_fragment");
    public static BlockEntityType<MachineBlockEntity> MACHINE_TYPE;
    public static BlockEntityType<dev.technologia.logistics.ItemTransferBlockEntity> ITEM_TRANSFER_TYPE;
    public static MenuType<MachineMenu> MACHINE_MENU;
    public static MenuType<StorageMenu> STORAGE_MENU;
    public static MenuType<dev.technologia.guide.GuideMenu> GUIDE_MENU;
    public static CreativeModeTab TAB;
    public static Balance BALANCE = Balance.defaults();
    // Destructive automation fails closed until a loader installs its protection hook.
    public static BiPredicate<ServerPlayer, BlockPos> BREAK_PERMISSION = (player, pos) -> false;
    private static int topology;

    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }

    /** Cached conduit and storage routes are valid only for the value they were built with. */
    public static int topology() { return topology; }
    /** Called whenever a machine, cable or one of their neighbors changes. */
    public static void topologyChanged() { topology++; }

    private static BlockBehaviour.Properties metal(float strength) {
        return BlockBehaviour.Properties.of().strength(strength).sound(SoundType.METAL).mapColor(MapColor.METAL);
    }

    public static void createBlocks() {
        if (!BLOCKS.isEmpty()) return;
        addBlock("machine_frame", new Block(metal(4).requiresCorrectToolForDrops()));
        for (MachineKind kind : MachineKind.values()) {
            addBlock(kind.id, new MachineBlock(kind, metal(4).requiresCorrectToolForDrops().noOcclusion()
                    .lightLevel(state -> state.getValue(MachineBlock.ACTIVE) ? 6 : 0)));
        }
        addBlock("network_cable", new NetworkCableBlock(metal(1).noOcclusion()));
        addBlock("energy_conduit", new EnergyConduitBlock(200, metal(1).noOcclusion()));
        addBlock("gold_energy_conduit", new EnergyConduitBlock(1000, metal(1).noOcclusion()));
        addBlock("resonite_energy_conduit", new EnergyConduitBlock(5000, metal(1).noOcclusion()));
        addBlock("item_transfer", new ItemTransferBlock(metal(3).noOcclusion()));
        for (String name : DECOR) {
            var properties = (name.equals("reinforced_glass") ? BlockBehaviour.Properties.of().strength(4).sound(SoundType.GLASS) : metal(4))
                    .requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> name.equals("engineering_lamp") ? 15 : 0);
            addBlock(name, DIRECTIONAL_DECOR.contains(name) ? new DirectionalFactoryBlock(name, properties) : new FactoryBlock(name, properties));
        }
        for (String ore : List.of("tin_ore", "deepslate_tin_ore", "lead_ore", "deepslate_lead_ore", "resonite_ore")) {
            addBlock(ore, new Block(BlockBehaviour.Properties.of().strength(3, 3).requiresCorrectToolForDrops()
                    .sound(ore.startsWith("deepslate") ? SoundType.DEEPSLATE : SoundType.STONE)
                    .mapColor(ore.startsWith("deepslate") ? MapColor.DEEPSLATE : MapColor.STONE)));
        }
        addBlock("resonance_bloom", new ResonanceBloomBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noCollission()
                .instabreak().sound(SoundType.GRASS).lightLevel(state -> 5).pushReaction(PushReaction.DESTROY)));
    }
    public static void createItems() {
        if (!ITEMS.isEmpty()) return;
        BLOCKS.forEach((name, block) -> ITEMS.put(name, block instanceof MachineBlock
                ? new MachineBlockItem(block, new Item.Properties()) : new BlockItem(block, new Item.Properties())));
        for (String name : List.of("raw_tin", "raw_lead", "tin_ingot", "lead_ingot", "resonite", "iron_dust", "gold_dust", "copper_dust", "tin_dust", "lead_dust", "basic_circuit", "advanced_circuit", "draconic_core", "chaotic_core")) {
            ITEMS.put(name, new Item(new Item.Properties()));
        }
        ITEMS.put("field_guide", new GuideItem(new Item.Properties()));
        ITEMS.put("wrench", new WrenchItem(new Item.Properties()));
        for (String name : List.of("bronze_ingot", "steel_ingot", "iron_plate", "copper_plate", "gold_plate", "bronze_plate", "steel_plate", "sawdust", "coal_dust"))
            ITEMS.put(name, new Item(new Item.Properties()));
        for (String name : FRAGMENTS) ITEMS.put(name, new Item(new Item.Properties()));
        // One installer kit per tier above the first, driven by /technologia/tiers.json.
        for (MachineTier tier : MachineTier.all())
            if (!tier.isBase()) ITEMS.put("tier_kit_" + tier.id(), new TierKitItem(tier.index(), new Item.Properties().stacksTo(16)));
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
