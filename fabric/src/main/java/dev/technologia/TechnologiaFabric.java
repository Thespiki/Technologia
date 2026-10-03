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

public final class TechnologiaFabric implements ModInitializer {
    public void onInitialize() {
        net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback.EVENT.register((dispatcher, registry, environment) -> TechnologiaCommands.register(dispatcher));
        Technologia.BALANCE = Balance.load(FabricLoader.getInstance().getConfigDir());
        Technologia.createBlocks();
        Technologia.BLOCKS.forEach((id, block) -> Registry.register(BuiltInRegistries.BLOCK, Technologia.id(id), block));
        Technologia.createItems();
        Technologia.ITEMS.forEach((id, item) -> Registry.register(BuiltInRegistries.ITEM, Technologia.id(id), item));
        for (Technologia.BlockEntityEntry<?> entry : Technologia.blockEntities()) Registry.register(BuiltInRegistries.BLOCK_ENTITY_TYPE, Technologia.id(entry.id()), build(entry));
        Technologia.MACHINE_MENU = new net.minecraft.world.inventory.MenuType<>(MachineMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS);
        Technologia.STORAGE_MENU = new net.minecraft.world.inventory.MenuType<>(dev.technologia.storage.StorageMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS);
        Technologia.GUIDE_MENU = new net.minecraft.world.inventory.MenuType<>(dev.technologia.guide.GuideMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS);
        Registry.register(BuiltInRegistries.RECIPE_TYPE, Technologia.id("processing"), dev.technologia.recipe.MachineRecipe.TYPE);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Technologia.id("processing"), dev.technologia.recipe.MachineRecipe.SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Technologia.id("machine_crafting"), dev.technologia.recipe.MachineCraftingRecipe.SERIALIZER);
        Technologia.createTab();
        Registry.register(BuiltInRegistries.MENU, Technologia.id("machine"), Technologia.MACHINE_MENU);
        Registry.register(BuiltInRegistries.MENU, Technologia.id("storage"), Technologia.STORAGE_MENU);
        Registry.register(BuiltInRegistries.MENU, Technologia.id("guide"), Technologia.GUIDE_MENU);
        Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, Technologia.id("technologia"), Technologia.TAB);
        for (String ore : Technologia.ORE_FEATURES) {
            BiomeModifications.addFeature(BiomeSelectors.foundInOverworld(), GenerationStep.Decoration.UNDERGROUND_ORES,
                    ResourceKey.create(Registries.PLACED_FEATURE, Technologia.id(ore)));
        }
        Technologia.BREAK_PERMISSION = (player, pos) -> PlayerBlockBreakEvents.BEFORE.invoker().beforeBlockBreak(
                player.level(), player, pos, player.level().getBlockState(pos), player.level().getBlockEntity(pos));
        EnergyStorage.SIDED.registerForBlockEntity((machine, side) -> machine.kind.capacity == 0 ? null : energy(machine), Technologia.MACHINE_TYPE);
        dev.technologia.logistics.EnergyTransport.EXTERNAL_RECEIVER = (level, pos, side, amount, simulate) -> {
            var target = EnergyStorage.SIDED.find(level, pos, side);
            if (target == null) return 0;
            try (var transaction = net.fabricmc.fabric.api.transfer.v1.transaction.Transaction.openOuter()) {
                int accepted = (int) target.insert(amount, transaction);
                if (!simulate) transaction.commit();
                return accepted;
            }
        };
    }
    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityType<T> build(Technologia.BlockEntityEntry<T> entry) {
        var type = net.fabricmc.fabric.api.object.builder.v1.block.entity.FabricBlockEntityTypeBuilder.<T>create(entry.factory()::apply, entry.blocks().get()).build();
        entry.store().accept(type);
        return type;
    }
    private static FabricEnergy energy(MachineBlockEntity machine) {
        if (!(machine.platformEnergy instanceof FabricEnergy)) machine.platformEnergy = new FabricEnergy(machine);
        return (FabricEnergy) machine.platformEnergy;
    }
}
