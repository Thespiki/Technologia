package dev.technologia.test;

import dev.technologia.Technologia;
import dev.technologia.device.*;
import dev.technologia.logistics.EnergyTransport;
import dev.technologia.machine.*;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.transfer.v1.item.ItemStorage;
import net.fabricmc.fabric.api.transfer.v1.item.ItemVariant;
import net.fabricmc.fabric.api.transfer.v1.transaction.Transaction;
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
import team.reborn.energy.api.EnergyStorage;

/**
 * Runs on a headless Fabric server (gradlew :fabric:runGametest). The full behaviour suite lives in
 * the NeoForge module; these checks prove the shared code and the Fabric adapters load and work.
 */
public final class FabricSmokeTests implements FabricGameTest {
    private static final int OUT = MachineBlockEntity.FIRST_OUTPUT;
    private static MachineBlockEntity place(GameTestHelper h, String id, int x, int z) {
        var pos = new BlockPos(x, 2, z);
        h.setBlock(pos, Technologia.BLOCKS.get(id));
        return (MachineBlockEntity) h.getLevel().getBlockEntity(h.absolutePos(pos));
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void contentTiersAndWorldgenLoad(GameTestHelper h) {
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

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 300)
    public void generatorPowersCrusherThroughConduit(GameTestHelper h) {
        var generator = place(h, "coal_generator", 2, 2);
        h.setBlock(new BlockPos(3, 2, 2), Technologia.BLOCKS.get("energy_conduit"));
        var crusher = place(h, "crusher", 4, 2);
        generator.setItem(0, new ItemStack(Items.COAL));
        crusher.setItem(0, new ItemStack(Items.RAW_IRON));
        h.succeedWhen(() -> h.assertTrue(crusher.getItem(0).isEmpty() && crusher.getItem(OUT).is(Technologia.ITEMS.get("iron_dust"))
                && crusher.getItem(OUT).getCount() == 2, "Power crosses a conduit and one raw iron becomes two dust"));
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void energyAndItemApisFollowMachineRules(GameTestHelper h) {
        var cell = place(h, "energy_cell", 2, 2);
        EnergyStorage energy = EnergyStorage.SIDED.find(h.getLevel(), cell.getBlockPos(), Direction.UP);
        h.assertTrue(energy != null && energy.supportsInsertion() && energy.getCapacity() == 1000000, "Team Reborn Energy is exposed with the tier capacity");
        try (Transaction simulation = Transaction.openOuter()) {
            h.assertTrue(energy.insert(5000, simulation) == 200, "One tick accepts at most the machine's transfer rate");
        }
        h.assertTrue(cell.energy.stored() == 0, "An aborted transaction leaves no energy behind");
        try (Transaction transaction = Transaction.openOuter()) {
            h.assertTrue(energy.insert(5000, transaction) == 200, "An aborted simulation does not use up the tick's budget");
            transaction.commit();
        }
        h.assertTrue(cell.energy.stored() == 200, "A committed transaction stores the energy");
        var crusher = place(h, "crusher", 4, 2);
        var items = ItemStorage.SIDED.find(h.getLevel(), crusher.getBlockPos(), Direction.UP);
        h.assertTrue(items != null, "Fabric's transfer API sees the machine inventory");
        try (Transaction transaction = Transaction.openOuter()) {
            h.assertTrue(items.insert(ItemVariant.of(Items.DIAMOND), 1, transaction) == 0, "An item the crusher cannot process is refused");
            h.assertTrue(items.insert(ItemVariant.of(Items.RAW_IRON), 1, transaction) == 1, "A real ingredient is accepted");
            transaction.commit();
        }
        h.assertTrue(crusher.getItem(0).is(Items.RAW_IRON), "The ingredient is in the first lane");
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void droppedMachineKeepsTierAndProtectionHookAnswers(GameTestHelper h) {
        var press = place(h, "metal_press", 2, 2);
        h.assertTrue(press.upgradeTo(1), "A tier installs");
        press.receiveEnergy(900, false);
        var drops = Block.getDrops(press.getBlockState(), h.getLevel(), press.getBlockPos(), press);
        h.assertTrue(drops.size() == 1 && MachineBlockItem.tier(drops.getFirst()).index() == 1 && MachineBlockItem.energy(drops.getFirst()) == 900, "The dropped machine keeps tier and energy");
        var player = h.makeMockServerPlayerInLevel();
        h.assertTrue(Technologia.BREAK_PERMISSION.test(player, press.getBlockPos()), "The Fabric break-permission hook is installed and allows an unprotected block");
        h.getLevel().getServer().getPlayerList().remove(player);
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void alpha5DevicesAndPowerAreRegistered(GameTestHelper h) {
        String[] devices = {"wooden_crate", "bonsai_pot", "hand_sieve"};
        Class<?>[] types = {CrateBlockEntity.class, BonsaiPotBlockEntity.class, HandSieveBlockEntity.class};
        for (int i = 0; i < devices.length; i++) {
            var pos = new BlockPos(1 + 2 * i, 2, 6);
            h.setBlock(pos, Technologia.BLOCKS.get(devices[i]));
            h.assertTrue(types[i].isInstance(h.getLevel().getBlockEntity(h.absolutePos(pos))), "Placing a " + devices[i] + " creates its block entity");
        }
        var source = place(h, "creative_energy_source", 1, 1); var cell = place(h, "energy_cell", 2, 1);
        source.energy.restore(source.capacity());
        h.assertTrue(EnergyTransport.transfer(source) == 1000000 && cell.energy.stored() == 1000000, "A creative source fills the cell beside it in one tick");
        // A channel of its own: the wireless grid is shared by every test on the server.
        var sender = place(h, "wireless_sender", 5, 1); var receiver = place(h, "wireless_receiver", 5, 4);
        sender.changeChannel(7); receiver.changeChannel(7); sender.receiveEnergy(1000, false);
        MachineBlockEntity.tick(h.getLevel(), receiver.getBlockPos(), receiver.getBlockState(), receiver);
        MachineBlockEntity.tick(h.getLevel(), sender.getBlockPos(), sender.getBlockState(), sender);
        h.assertTrue(receiver.energy.stored() == 1000 && sender.energy.stored() == 0, "A wireless sender reaches a receiver on its channel");
        h.succeed();
    }
}
