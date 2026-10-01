package dev.technologia.machine;

public enum MachineKind {
    GENERATOR("coal_generator", 100000), CRUSHER("crusher", 50000),
    FURNACE("electric_furnace", 50000), MINER("digital_miner", 100000),
    CORE("storage_core", 0), TERMINAL("storage_terminal", 0), CELL("energy_cell", 1000000),
    ALLOY_SMELTER("alloy_smelter", 80000), METAL_PRESS("metal_press", 50000),
    SAWMILL("sawmill", 50000), SOLAR_GENERATOR("solar_generator", 20000),
    COMPACTOR("compactor", 60000), CENTRIFUGE("centrifuge", 80000), RECYCLER("recycler", 50000),
    BIOMASS_GENERATOR("biomass_generator", 100000), ADVANCED_CELL("advanced_energy_cell", 5000000),
    ADVANCED_SOLAR("advanced_solar_generator", 100000);
    public final String id;
    public final int capacity;
    MachineKind(String id, int capacity) { this.id = id; this.capacity = capacity; }
    public boolean isCell() { return this == CELL || this == ADVANCED_CELL; }
    public boolean isSolar() { return this == SOLAR_GENERATOR || this == ADVANCED_SOLAR; }
    public boolean isGenerator() { return this == GENERATOR || this == BIOMASS_GENERATOR || isSolar(); }
    public boolean suppliesEnergy() { return isGenerator() || isCell(); }
    public boolean acceptsEnergy() { return capacity > 0 && !isGenerator(); }
    public boolean isProcessor() { return this == CRUSHER || this == FURNACE || this == ALLOY_SMELTER || this == METAL_PRESS || this == SAWMILL || this == COMPACTOR || this == CENTRIFUGE || this == RECYCLER; }
    public int inputCount() { return this == ALLOY_SMELTER ? 2 : isProcessor() || this == GENERATOR || this == BIOMASS_GENERATOR || this == MINER ? 1 : 0; }
    public boolean hasOutput() { return isProcessor() || this == MINER; }
}
