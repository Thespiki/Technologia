package dev.technologia;

import dev.technologia.machine.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import team.reborn.energy.api.EnergyStorage;

/** Persistent per-entity adapter, with rollback for nested Fabric transactions. */
final class FabricEnergy extends SnapshotParticipant<int[]> implements EnergyStorage {
    private final MachineBlockEntity machine;
    FabricEnergy(MachineBlockEntity machine) { this.machine = machine; }
    // The per-tick rate window is part of the state, so an aborted simulation costs no budget.
    protected int[] createSnapshot() { return machine.energySnapshot(); }
    protected void readSnapshot(int[] value) { machine.restoreEnergySnapshot(value); }
    protected void onFinalCommit() { machine.setChanged(); }
    public boolean supportsInsertion() { return machine.kind.acceptsEnergy(); }
    public boolean supportsExtraction() { return machine.kind.suppliesEnergy(); }
    public long insert(long amount, TransactionContext transaction) {
        StoragePreconditions.notNegative(amount);
        if (!supportsInsertion()) return 0;
        int accepted = machine.receiveExternal((int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) { updateSnapshots(transaction); accepted = machine.receiveExternal(accepted, false); }
        return accepted;
    }
    public long extract(long amount, TransactionContext transaction) {
        StoragePreconditions.notNegative(amount);
        if (!supportsExtraction()) return 0;
        int accepted = machine.send((int) Math.min(amount, Integer.MAX_VALUE), true);
        if (accepted > 0) { updateSnapshots(transaction); accepted = machine.send(accepted, false); }
        return accepted;
    }
    public long getAmount() { return machine.energy.stored(); }
    public long getCapacity() { return machine.capacity(); }
}
