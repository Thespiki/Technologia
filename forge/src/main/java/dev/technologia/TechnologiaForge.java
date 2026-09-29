package dev.technologia;

import dev.technologia.config.Balance;
import dev.technologia.machine.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.capabilities.*;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.items.wrapper.SidedInvWrapper;
import net.minecraftforge.registries.RegisterEvent;

@Mod(Technologia.MOD_ID)
public final class TechnologiaForge {
    public TechnologiaForge(FMLJavaModLoadingContext context) {
        MinecraftForge.EVENT_BUS.addListener((net.minecraftforge.event.RegisterCommandsEvent event) -> TechnologiaCommands.register(event.getDispatcher()));
        Technologia.BALANCE = Balance.load(FMLPaths.CONFIGDIR.get());
        context.getModEventBus().addListener(this::register);
        MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, this::attach);
        Technologia.BREAK_PERMISSION = (player, pos) -> !MinecraftForge.EVENT_BUS.post(new BlockEvent.BreakEvent(player.level(), pos, player.level().getBlockState(pos), player));
        Technologia.ENERGY_EXPORT = (machine, side) -> {
            var target = machine.getLevel().getBlockEntity(machine.getBlockPos().relative(side));
            if (target != null) target.getCapability(ForgeCapabilities.ENERGY, side.getOpposite()).ifPresent(storage ->
                    machine.extractEnergy(storage.receiveEnergy(Math.min(200, machine.energy.stored()), false), false));
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
    private void attach(AttachCapabilitiesEvent<BlockEntity> event) {
        if (!(event.getObject() instanceof MachineBlockEntity machine)) return;
        LazyOptional<IEnergyStorage> energy = LazyOptional.of(() -> new IEnergyStorage() {
            public int receiveEnergy(int max, boolean simulate) { return machine.receiveEnergy(Math.min(200, max), simulate); }
            public int extractEnergy(int max, boolean simulate) { return machine.extractEnergy(Math.min(200, max), simulate); }
            public int getEnergyStored() { return machine.energy.stored(); }
            public int getMaxEnergyStored() { return machine.kind.capacity; }
            public boolean canExtract() { return machine.kind.suppliesEnergy(); }
            public boolean canReceive() { return machine.kind.capacity > 0 && machine.kind != MachineKind.GENERATOR; }
        });
        var items = LazyOptional.of(() -> new SidedInvWrapper(machine, Direction.DOWN));
        event.addCapability(Technologia.id("machine"), new ICapabilityProvider() {
            public <T> LazyOptional<T> getCapability(Capability<T> capability, Direction side) {
                if (capability == ForgeCapabilities.ENERGY && machine.kind.capacity > 0) return energy.cast();
                if (capability == ForgeCapabilities.ITEM_HANDLER) return items.cast();
                return LazyOptional.empty();
            }
        });
        event.addListener(energy::invalidate); event.addListener(items::invalidate);
    }
}
