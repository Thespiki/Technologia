package dev.technologia;

import dev.technologia.machine.*;
import dev.technologia.config.Balance;
import dev.technologia.device.*;
import dev.technologia.item.MagnetItem;
import dev.technologia.item.TravelStaffItem;
import dev.technologia.logistics.EnergyConduitBlock;
import dev.technologia.logistics.ItemTransferBlock;
import dev.technologia.logistics.ItemTransferBlockEntity;
import dev.technologia.nature.ResonanceBloomBlock;
import dev.technologia.storage.StorageMenu;
import dev.technologia.guide.GuideItem;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.*;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Supplier;

/** Loader-neutral content. Initialized by the loader during registration. */
public final class Technologia {
    public static final String MOD_ID = "technologia";
    public static final Map<String, Block> BLOCKS = new LinkedHashMap<>();
    public static final Map<String, Item> ITEMS = new LinkedHashMap<>();
    /** Blocks with see-through textures; each loader's client entry point gives them a cutout layer. */
    public static final List<String> CUTOUT_BLOCKS = List.of("reinforced_glass", "resonance_bloom", "catwalk", "bonsai_pot", "hand_sieve");
    public static final List<String> DECOR = List.of("steel_casing", "bronze_casing", "industrial_bricks", "steel_grate", "hazard_block",
            "engineering_lamp", "steel_pillar", "ventilation_grille", "control_panel", "reinforced_glass",
            "floor_panel", "catwalk", "steel_table", "steel_stool", "metal_shelf", "tool_cabinet", "warning_light");
    /** Decor with a distinct front; it faces the player when placed and turns with the wrench. */
    public static final Set<String> DIRECTIONAL_DECOR = Set.of("ventilation_grille", "control_panel", "metal_shelf", "tool_cabinet");
    public static final List<String> ORES = List.of("tin_ore", "deepslate_tin_ore", "lead_ore", "deepslate_lead_ore", "resonite_ore",
            "silver_ore", "deepslate_silver_ore", "nickel_ore", "deepslate_nickel_ore");
    /** One placed feature per ore family; Fabric adds them to the overworld in code. */
    public static final List<String> ORE_FEATURES = List.of("tin_ore", "lead_ore", "resonite_ore", "silver_ore", "nickel_ore");
    public static final List<String> COMPRESSED = List.of("compressed_cobblestone", "compressed_gravel", "compressed_sand");
    public static final List<String> FRAGMENTS = List.of("iron_fragment", "copper_fragment", "gold_fragment", "tin_fragment", "lead_fragment",
            "silver_fragment", "nickel_fragment", "redstone_fragment", "lapis_fragment", "diamond_fragment");
    /** Plain crafting materials added in alpha.5. */
    public static final List<String> MATERIALS = List.of("raw_silver", "raw_nickel", "silver_ingot", "nickel_ingot", "electrum_ingot", "invar_ingot",
            "constantan_ingot", "silver_dust", "nickel_dust", "invar_plate", "electrum_plate", "enriched_carbon", "enriched_redstone",
            "enriched_diamond", "enriched_resonite", "infused_alloy", "reinforced_alloy", "resonant_alloy");
    /** Crate name to rows of nine slots. */
    public static final Map<String, Integer> CRATES = crates();
    /** Magnet name to reach in blocks. */
    public static final Map<String, Integer> MAGNETS = magnets();

    public static BlockEntityType<MachineBlockEntity> MACHINE_TYPE;
    public static BlockEntityType<ItemTransferBlockEntity> ITEM_TRANSFER_TYPE;
    public static BlockEntityType<CrateBlockEntity> CRATE_TYPE;
    public static BlockEntityType<BonsaiPotBlockEntity> BONSAI_TYPE;
    public static BlockEntityType<HandSieveBlockEntity> HAND_SIEVE_TYPE;
    public static MenuType<MachineMenu> MACHINE_MENU;
    public static MenuType<StorageMenu> STORAGE_MENU;
    public static MenuType<dev.technologia.guide.GuideMenu> GUIDE_MENU;
    public static CreativeModeTab TAB;
    public static Balance BALANCE = Balance.defaults();
    // Destructive automation fails closed until a loader installs its protection hook.
    public static BiPredicate<ServerPlayer, BlockPos> BREAK_PERMISSION = (player, pos) -> false;
    private static int topology;

    /**
     * A block entity type to register. The loaders build the type themselves, because the vanilla
     * factory interface is not public in every environment, then hand it back through {@code store}.
     */
    public record BlockEntityEntry<T extends BlockEntity>(String id, BiFunction<BlockPos, BlockState, T> factory,
                                                         Supplier<Block[]> blocks, Consumer<BlockEntityType<T>> store) {}
    public static List<BlockEntityEntry<?>> blockEntities() {
        return List.of(
                new BlockEntityEntry<>("machine", MachineBlockEntity::new, Technologia::machineBlocks, type -> MACHINE_TYPE = type),
                new BlockEntityEntry<>("item_transfer", ItemTransferBlockEntity::new, () -> new Block[] {BLOCKS.get("item_transfer")}, type -> ITEM_TRANSFER_TYPE = type),
                new BlockEntityEntry<>("crate", CrateBlockEntity::new, () -> CRATES.keySet().stream().map(BLOCKS::get).toArray(Block[]::new), type -> CRATE_TYPE = type),
                new BlockEntityEntry<>("bonsai_pot", BonsaiPotBlockEntity::new, () -> new Block[] {BLOCKS.get("bonsai_pot")}, type -> BONSAI_TYPE = type),
                new BlockEntityEntry<>("hand_sieve", HandSieveBlockEntity::new, () -> new Block[] {BLOCKS.get("hand_sieve")}, type -> HAND_SIEVE_TYPE = type));
    }

    public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }

    /** Cached conduit and storage routes are valid only for the value they were built with. */
    public static int topology() { return topology; }
    /** Called whenever a machine, cable or one of their neighbors changes. */
    public static void topologyChanged() { topology++; }

    /**
     * Puts a stack in the player's inventory, and what does not fit on the ground at {@code pos}.
     * In creative mode the inventory reports success and discards whatever it has no room for, so
     * there it is only offered the stack when a whole slot is free.
     */
    public static void giveOrDrop(Player player, ItemStack stack, BlockPos pos) {
        if (stack.isEmpty()) return;
        var inventory = player.getInventory();
        if (!player.hasInfiniteMaterials() || inventory.getFreeSlot() >= 0) inventory.add(stack);
        drop(player.level(), pos, stack);
    }
    /**
     * Leaves an item on the ground in the middle of a block space. These are items handed back or
     * made by a device, not the drops of a broken block, so the doTileDrops game rule does not apply.
     */
    public static void drop(Level level, BlockPos pos, ItemStack stack) {
        if (level.isClientSide || stack.isEmpty()) return;
        ItemEntity entity = new ItemEntity(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, stack);
        entity.setDefaultPickUpDelay();
        level.addFreshEntity(entity);
    }

    private static Map<String, Integer> crates() {
        Map<String, Integer> crates = new LinkedHashMap<>();
        crates.put("wooden_crate", 3); crates.put("bronze_crate", 4); crates.put("steel_crate", 5); crates.put("resonant_crate", 6);
        return Collections.unmodifiableMap(crates);
    }
    private static Map<String, Integer> magnets() {
        Map<String, Integer> magnets = new LinkedHashMap<>();
        magnets.put("magnet", 5); magnets.put("reinforced_magnet", 8); magnets.put("resonant_magnet", 12);
        return Collections.unmodifiableMap(magnets);
    }

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
        addBlock("superconducting_energy_conduit", new EnergyConduitBlock(32000, metal(1).noOcclusion()));
        addBlock("item_transfer", new ItemTransferBlock(metal(3).noOcclusion()));
        for (String name : DECOR) {
            int light = name.equals("engineering_lamp") ? 15 : name.equals("warning_light") ? 12 : 0;
            var properties = (name.equals("reinforced_glass") ? BlockBehaviour.Properties.of().strength(4).sound(SoundType.GLASS) : metal(4))
                    .requiresCorrectToolForDrops().noOcclusion().lightLevel(state -> light);
            addBlock(name, DIRECTIONAL_DECOR.contains(name) ? new DirectionalFactoryBlock(name, properties) : new FactoryBlock(name, properties));
        }
        for (String ore : ORES) {
            addBlock(ore, new Block(BlockBehaviour.Properties.of().strength(3, 3).requiresCorrectToolForDrops()
                    .sound(ore.startsWith("deepslate") ? SoundType.DEEPSLATE : SoundType.STONE)
                    .mapColor(ore.startsWith("deepslate") ? MapColor.DEEPSLATE : MapColor.STONE)));
        }
        addBlock("resonance_bloom", new ResonanceBloomBlock(BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_CYAN).noCollission()
                .instabreak().sound(SoundType.GRASS).lightLevel(state -> 5).pushReaction(PushReaction.DESTROY)));
        // Nine blocks in one. They are solid storage blocks and do not fall.
        addBlock("compressed_cobblestone", new Block(BlockBehaviour.Properties.of().strength(3, 8).requiresCorrectToolForDrops().sound(SoundType.STONE).mapColor(MapColor.STONE)));
        addBlock("compressed_gravel", new Block(BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.GRAVEL).mapColor(MapColor.STONE)));
        addBlock("compressed_sand", new Block(BlockBehaviour.Properties.of().strength(1.5F).sound(SoundType.SAND).mapColor(MapColor.SAND)));
        CRATES.forEach((name, rows) -> addBlock(name, new CrateBlock(rows, name.equals("wooden_crate")
                ? BlockBehaviour.Properties.of().strength(2.5F).sound(SoundType.WOOD).mapColor(MapColor.WOOD) : metal(3))));
        addBlock("bonsai_pot", new BonsaiPotBlock(BlockBehaviour.Properties.of().strength(1).sound(SoundType.DECORATED_POT).mapColor(MapColor.TERRACOTTA_ORANGE).noOcclusion()));
        addBlock("hand_sieve", new HandSieveBlock(BlockBehaviour.Properties.of().strength(2).sound(SoundType.WOOD).mapColor(MapColor.WOOD).noOcclusion()));
        addBlock("vector_plate", new VectorPlateBlock(0.12, metal(1).noOcclusion()));
        addBlock("fast_vector_plate", new VectorPlateBlock(0.3, metal(1).noOcclusion()));
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
        for (String name : MATERIALS) ITEMS.put(name, new Item(new Item.Properties()));
        for (int level = 1; level <= MeshItem.NAMES.size(); level++) ITEMS.put(MeshItem.NAMES.get(level - 1), new MeshItem(level, new Item.Properties()));
        MAGNETS.forEach((name, range) -> ITEMS.put(name, new MagnetItem(range, new Item.Properties())));
        ITEMS.put("travel_staff", new TravelStaffItem(new Item.Properties()));
        ITEMS.put("creative_tier_kit", new CreativeKitItem(new Item.Properties().rarity(Rarity.EPIC)));
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
