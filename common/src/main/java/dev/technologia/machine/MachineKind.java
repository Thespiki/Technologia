package dev.technologia.machine;

public enum MachineKind {
    GENERATOR("coal_generator", 100000), CRUSHER("crusher", 50000),
    FURNACE("electric_furnace", 50000), MINER("digital_miner", 100000),
    CORE("storage_core", 0), TERMINAL("storage_terminal", 0), CELL("energy_cell", 1000000);
    public final String id;
    public final int capacity;
    MachineKind(String id, int capacity) { this.id = id; this.capacity = capacity; }
    public boolean suppliesEnergy() { return this == GENERATOR || this == CELL; }
}
