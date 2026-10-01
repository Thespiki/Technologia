package dev.technologia;

import dev.technologia.machine.*;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.fabricmc.fabric.api.transfer.v1.storage.StoragePreconditions;
import team.reborn.energy.api.EnergyStorage;

/** Persistent per-entity adapter, with rollback for nested Fabric transactions. */
final class FabricEnergy extends SnapshotParticipant<Integer> implements EnergyStorage {
    private final MachineBlockEntity machine;
    FabricEnergy(MachineBlockEntity machine) { this.machine = machine; }
    protected Integer createSnapshot() { return machine.energy.stored(); }
    protected void readSnapshot(Integer value) { machine.energy.restore(value); }
    protected void onFinalCommit() { machine.setChanged(); }
    public boolean supportsInsertion() { return machine.kind.acceptsEnergy(); }
    public boolean supportsExtraction() { return machine.kind.suppliesEnergy(); }
    public long insert(long amount, TransactionContext transaction) {
        StoragePreconditions.notNegative(amount);
        if (!supportsInsertion()) return 0;
        int accepted = machine.energy.receive((int) Math.min(amount, 200), true);
        if (accepted > 0) { updateSnapshots(transaction); machine.energy.receive(accepted, false); }
        return accepted;
    }
    public long extract(long amount, TransactionContext transaction) {
        StoragePreconditions.notNegative(amount);
        if (!supportsExtraction()) return 0;
        int accepted = machine.energy.extract((int) Math.min(amount, 200), true);
        if (accepted > 0) { updateSnapshots(transaction); machine.energy.extract(accepted, false); }
        return accepted;
    }
    public long getAmount() { return machine.energy.stored(); }
    public long getCapacity() { return machine.kind.capacity; }
}
