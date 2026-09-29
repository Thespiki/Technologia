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
        Technologia.ENERGY_EXPORT = (machine, side) -> {
            var target = machine.getLevel().getCapability(Capabilities.EnergyStorage.BLOCK, machine.getBlockPos().relative(side), side.getOpposite());
            if (target != null) machine.extractEnergy(target.receiveEnergy(Math.min(200, machine.energy.stored()), false), false);
        };
    }
    private void register(RegisterEvent event) {
        event.register(Registries.BLOCK, helper -> { Technologia.createBlocks(); Technologia.BLOCKS.forEach((id, block) -> helper.register(Technologia.id(id), block)); });
        event.register(Registries.ITEM, helper -> { Technologia.createItems(); Technologia.ITEMS.forEach((id, item) -> helper.register(Technologia.id(id), item)); });
        event.register(Registries.BLOCK_ENTITY_TYPE, helper -> {
            Technologia.MACHINE_TYPE = net.minecraft.world.level.block.entity.BlockEntityType.Builder.of(MachineBlockEntity::new, Technologia.machineBlocks()).build(null);
            helper.register(Technologia.id("machine"), Technologia.MACHINE_TYPE);
        });
        event.register(Registries.MENU, helper -> {
            Technologia.MACHINE_MENU = new net.minecraft.world.inventory.MenuType<>(MachineMenu::new, net.minecraft.world.flag.FeatureFlags.DEFAULT_FLAGS);
            helper.register(Technologia.id("machine"), Technologia.MACHINE_MENU);
        });
        event.register(Registries.CREATIVE_MODE_TAB, helper -> { Technologia.createTab(); helper.register(Technologia.id("technologia"), Technologia.TAB); });
    }
    private void capabilities(RegisterCapabilitiesEvent event) {
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, Technologia.MACHINE_TYPE, (machine, side) -> machine.kind.capacity == 0 ? null : new IEnergyStorage() {
            public int receiveEnergy(int max, boolean simulate) { return machine.receiveEnergy(Math.min(200, max), simulate); }
            public int extractEnergy(int max, boolean simulate) { return machine.extractEnergy(Math.min(200, max), simulate); }
            public int getEnergyStored() { return machine.energy.stored(); }
            public int getMaxEnergyStored() { return machine.kind.capacity; }
            public boolean canExtract() { return machine.kind.suppliesEnergy(); }
            public boolean canReceive() { return machine.kind.capacity > 0 && machine.kind != MachineKind.GENERATOR; }
        });
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, Technologia.MACHINE_TYPE, (machine, side) -> new SidedInvWrapper(machine, side == null ? Direction.DOWN : side));
    }
}
