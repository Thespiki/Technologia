package dev.technologia;

import dev.technologia.config.Balance;
import dev.technologia.machine.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLPaths;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.level.BlockEvent;
import net.neoforged.neoforge.registries.RegisterEvent;
import net.neoforged.neoforge.capabilities.*;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.wrapper.SidedInvWrapper;

@Mod(Technologia.MOD_ID)
public final class TechnologiaNeoForge {
    public TechnologiaNeoForge(IEventBus bus) {
        NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.RegisterCommandsEvent event) -> TechnologiaCommands.register(event.getDispatcher()));
        Technologia.BALANCE = Balance.load(FMLPaths.CONFIGDIR.get());
        bus.addListener(this::register);
        bus.addListener(this::capabilities);
        Technologia.BREAK_PERMISSION = (player, pos) -> !NeoForge.EVENT_BUS.post(new BlockEvent.BreakEvent(player.level(), pos, player.level().getBlockState(pos), player)).isCanceled();
        dev.technologia.logistics.EnergyTransport.EXTERNAL_RECEIVER = (level, pos, side, amount, simulate) -> {
            var target = level.getCapability(Capabilities.EnergyStorage.BLOCK, pos, side);
            return target == null ? 0 : target.receiveEnergy(amount, simulate);
        };
    }
    private void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> { Technologia.createBlocks(); Technologia.BLOCKS.forEach((id, block) -> helper.register(Technologia.id(id), block)); });
        event.register(Registries.ITEM, helper -> { Technologia.createItems(); Technologia.ITEMS.forEach((id, item) -> helper.register(Technologia.id(id), item)); });
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            for (Technologia.BlockEntityEntry<?> entry : Technologia.blockEntities()) helper.register(Technologia.id(entry.id()), build(entry));
        });
        event.register(Registries.RECIPE_TYPE, helper -> helper.register(Technologia.id("processing"), dev.technologia.recipe.MachineRecipe.TYPE));
        event.register(Registries.RECIPE_SERIALIZER, helper -> {
            helper.register(Technologia.id("processing"), dev.technologia.recipe.MachineRecipe.SERIALIZER);
            helper.register(Technologia.id("machine_crafting"), dev.technologia.recipe.MachineCraftingRecipe.SERIALIZER);
        });
        event.register(Registries.MENU, helper -> {
            Technologia.MACHINE_MENU = new net.minecraft.world.inventory.MenuType<>(MachineMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS);
            helper.register(Technologia.id("machine"), Technologia.MACHINE_MENU);
            Technologia.STORAGE_MENU = new net.minecraft.world.inventory.MenuType<>(dev.technologia.storage.StorageMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS);
            helper.register(Technologia.id("storage"), Technologia.STORAGE_MENU);
            Technologia.GUIDE_MENU = new net.minecraft.world.inventory.MenuType<>(dev.technologia.guide.GuideMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS);
            helper.register(Technologia.id("guide"), Technologia.GUIDE_MENU);
        });
        event.register(Registries.CREATIVE_MODE_TAB, helper -> { Technologia.createTab(); helper.register(Technologia.id("technologia"), Technologia.TAB); });
    }
    private static <T extends net.minecraft.world.level.block.entity.BlockEntity> net.minecraft.world.level.block.entity.BlockEntityType<T> build(Technologia.BlockEntityEntry<T> entry) {
        var type = net.minecraft.world.level.block.entity.BlockEntityType.Builder.<T>of(entry.factory()::apply, entry.blocks().get()).build(null);
        entry.store().accept(type);
        return type;
    }
    private void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Technologia.MACHINE_TYPE, (machine, side) -> machine.kind.capacity == 0 ? null : new IEnergyStorage() {
            public int receiveEnergy(int max, boolean simulate) { return machine.receiveExternal(max, simulate); }
            public int extractEnergy(int max, boolean simulate) { return machine.send(max, simulate); }
            public int getEnergyStored() { return machine.energy.stored(); }
            public int getMaxEnergyStored() { return machine.capacity(); }
            public boolean canExtract() { return machine.kind.suppliesEnergy(); }
            public boolean canReceive() { return machine.kind.acceptsEnergy(); }
        });
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Technologia.MACHINE_TYPE, (machine, side) -> new SidedInvWrapper(machine, side == null ? Direction.DOWN : side));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Technologia.CRATE_TYPE, (crate, side) -> new net.neoforged.neoforge.items.wrapper.InvWrapper(crate));
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Technologia.BONSAI_TYPE, (pot, side) -> new SidedInvWrapper(pot, side == null ? Direction.DOWN : side));
    }
}
