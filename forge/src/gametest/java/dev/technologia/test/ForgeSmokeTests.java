package dev.technologia.test;

import dev.technologia.Technologia;
import dev.technologia.machine.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;

/**
 * Runs on a headless Forge server (gradlew :forge:runGameTestServer). The full behaviour suite lives
 * in the NeoForge module; these checks prove the shared code and the Forge adapters load and work.
 */
@GameTestHolder(Technologia.MOD_ID)
public final class ForgeSmokeTests {
    private static final String EMPTY = "technologia:empty";
    private static final int OUT = MachineBlockEntity.FIRST_OUTPUT;
    private static MachineBlockEntity place(GameTestHelper h, String id, int x, int z) {
        var pos = new BlockPos(x, 2, z);
        h.setBlock(pos, Technologia.BLOCKS.get(id));
        return (MachineBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
    }

    @GameTest(template = EMPTY)
    public static void contentTiersAndWorldgenLoad(GameTestHelper h) {
        h.assertTrue(MachineTier.count() >= 6, "Tier data is read from the mod jar");
        for (MachineTier tier : MachineTier.all()) {
            if (tier.isBase()) continue;
            h.assertTrue(Technologia.ITEMS.get("tier_kit_" + tier.id()) instanceof TierKitItem, "Tier kit item is registered");
            h.assertTrue(h.getLevel().getRecipeManager().byKey(Technologia.id("tier_kit_" + tier.id())).isPresent(), "Tier kit recipe loads");
        }
        h.assertTrue(h.getLevel().getRecipeManager().byKey(Technologia.id("crushing/raw_iron")).isPresent(), "Processing recipes decode");
        h.assertTrue(h.getLevel().getServer().getAdvancements().get(Technologia.id("workshop/root")) != null, "Advancements load");
        var registries = h.getLevel().registryAccess();
        var tin = registries.registryOrThrow(Registries.PLACED_FEATURE).getHolderOrThrow(ResourceKey.create(Registries.PLACED_FEATURE, Technologia.id("tin_ore")));
        var plains = registries.registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.PLAINS).value();
        h.assertTrue(plains.getGenerationSettings().features().stream().anyMatch(step -> step.contains(tin)), "Tin ore is added to overworld biomes");
        h.succeed();
    }

    @GameTest(template = EMPTY, timeoutTicks = 300)
    public static void generatorPowersCrusherThroughConduit(GameTestHelper h) {
        var generator = place(h, "coal_generator", 2, 2);
        h.setBlock(new BlockPos(3, 2, 2), Technologia.BLOCKS.get("energy_conduit"));
        var crusher = place(h, "crusher", 4, 2);
        generator.setItem(0, new ItemStack(Items.COAL));
        crusher.setItem(0, new ItemStack(Items.RAW_IRON));
        h.succeedWhen(() -> h.assertTrue(crusher.getItem(0).isEmpty() && crusher.getItem(OUT).is(Technologia.ITEMS.get("iron_dust"))
                && crusher.getItem(OUT).getCount() == 2, "Power crosses a conduit and one raw iron becomes two dust"));
    }

    @GameTest(template = EMPTY)
    public static void energyAndItemCapabilitiesFollowMachineRules(GameTestHelper h) {
        var cell = place(h, "energy_cell", 2, 2);
        var energy = cell.getCapability(ForgeCapabilities.ENERGY, Direction.UP).resolve().orElse(null);
        h.assertTrue(energy != null && energy.canReceive() && energy.getMaxEnergyStored() == 1000000, "Forge Energy is exposed with the tier capacity");
        h.assertTrue(energy.receiveEnergy(5000, true) == 200 && cell.energy.stored() == 0, "Simulation is bounded and stores nothing");
        h.assertTrue(energy.receiveEnergy(5000, false) == 200 && energy.receiveEnergy(5000, false) == 0, "The receive limit is per tick");
        var crusher = place(h, "crusher", 4, 2);
        var north = crusher.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.NORTH).resolve().orElse(null);
        var unsided = crusher.getCapability(ForgeCapabilities.ITEM_HANDLER, null).resolve().orElse(null);
        h.assertTrue(north != null && unsided != null, "The item capability answers on a face and without one");
        h.assertTrue(north.insertItem(0, new ItemStack(Items.DIAMOND), false).getCount() == 1, "An item the crusher cannot process is refused");
        h.assertTrue(north.insertItem(0, new ItemStack(Items.RAW_IRON), false).isEmpty() && crusher.getItem(0).is(Items.RAW_IRON), "A real ingredient is accepted");
        h.assertTrue(north.extractItem(0, 1, false).isEmpty(), "Automation cannot take an ingredient back out");
        h.succeed();
    }

    @GameTest(template = EMPTY)
    public static void droppedMachineKeepsTierAndProtectionHookAnswers(GameTestHelper h) {
        var press = place(h, "metal_press", 2, 2);
        h.assertTrue(press.upgradeTo(1), "A tier installs");
        press.receiveEnergy(900, false);
        var drops = Block.getDrops(press.getBlockState(), h.getLevel(), press.getBlockPos(), press);
        h.assertTrue(drops.size() == 1 && MachineBlockItem.tier(drops.get(0)).index() == 1 && MachineBlockItem.energy(drops.get(0)) == 900, "The dropped machine keeps tier and energy");
        var player = h.makeMockServerPlayerInLevel();
        h.assertTrue(Technologia.BREAK_PERMISSION.test(player, press.getBlockPos()), "The Forge break-permission hook is installed and allows an unprotected block");
        h.getLevel().getServer().getPlayerList().remove(player);
        h.succeed();
    }
}
