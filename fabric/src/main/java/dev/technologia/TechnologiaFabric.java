package dev.technologia;

import dev.technologia.config.Balance;
import dev.technologia.machine.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.fabric.api.biome.v1.*;
import net.fabricmc.fabric.api.event.player.PlayerBlockBreakEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.*;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.level.levelgen.GenerationStep;
import team.reborn.energy.api.EnergyStorage;
import team.reborn.energy.api.EnergyStorageUtil;

public final class TechnologiaFabric implements ModInitializer {
    public void onInitialize() {
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> TechnologiaCommands.register(dispatcher));
        Technologia.BALANCE = Balance.load(FabricLoader.getInstance().getConfigDir());
        Technologia.createBlocks();
        Technologia.BLOCKS.forEach((id, block) -> Registry.register(BuiltInRegistries.BLOCK, Technologia.id(id), block));
        Technologia.createItems();
        Technologia.ITEMS.forEach((id, item) -> Registry.register(BuiltInRegistries.ITEM, Technologia.id(id), item));
        Technologia.MACHINE_TYPE = net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.create(MachineBlockEntity::new, Technologia.machineBlocks()).build();
        Technologia.MACHINE_MENU = new net.minecraft.world.inventory.MenuType<>(MachineMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS);
        Technologia.createTab();
        Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Technologia.id("machine"), Technologia.MACHINE_TYPE);
        Registry.register(BuiltInRegistries.MENU, Technologia.id("machine"), Technologia.MACHINE_MENU);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Technologia.id("technologia"), Technologia.TAB);
        for (String ore : new String[]{"tin_ore", "lead_ore", "resonite_ore"}) {
            BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES,
                    ResourceKey.create(Registries.PLACED_FEATURE, Technologia.id(ore)));
        }
        Technologia.BREAK_PERMISSION = (player, pos) -> PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(
                player.level(), player, pos, player.level().getBlockState(pos), player.level().getBlockEntity(pos));
        EnergyStorage.SIDED.registerForBlockEntity((machine, side) -> machine.kind.capacity == 0 ? null : energy(machine), Technologia.MACHINE_TYPE);
        Technologia.ENERGY_EXPORT = (machine, side) -> {
            var target = EnergyStorage.SIDED.find(machine.getLevel(), machine.getBlockPos().relative(side), side.getOpposite());
            if (target != null) EnergyStorageUtil.move(energy(machine), target, 200, null);
        };
    }
    private static FabricEnergy energy(MachineBlockEntity machine) {
        if (!(machine.platformEnergy instanceof FabricEnergy)) machine.platformEnergy = new FabricEnergy(machine);
        return (FabricEnergy) machine.platformEnergy;
    }
}
