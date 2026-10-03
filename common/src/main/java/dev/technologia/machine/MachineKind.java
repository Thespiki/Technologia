package dev.technologia.machine;

/** Machine families. Constants are only ever appended: the ordinal is synced to open menus. */
public enum MachineKind {
    GENERATOR("coal_generator", 100000), CRUSHER("crusher", 50000),
    FURNACE("electric_furnace", 50000), MINER("digital_miner", 100000),
    CORE("storage_core", 0), TERMINAL("storage_terminal", 0), CELL("energy_cell", 1000000),
    ALLOY_SMELTER("alloy_smelter", 80000), METAL_PRESS("metal_press", 50000),
    SAWMILL("sawmill", 50000), SOLAR_GENERATOR("solar_generator", 20000),
    COMPACTOR("compactor", 60000), CENTRIFUGE("centrifuge", 80000), RECYCLER("recycler", 50000),
    BIOMASS_GENERATOR("biomass_generator", 100000), ADVANCED_CELL("advanced_energy_cell", 5000000),
    ADVANCED_SOLAR("advanced_solar_generator", 100000), SIEVE("auto_sieve", 50000),
    // Alpha.5: early power from the surroundings, wireless power, more processors and area machines.
    WATER_WHEEL("water_wheel", 20000), WINDMILL("windmill", 20000), THERMO_GENERATOR("thermoelectric_generator", 40000),
    CREATIVE_SOURCE("creative_energy_source", 100000000),
    WIRELESS_SENDER("wireless_sender", 200000), WIRELESS_RECEIVER("wireless_receiver", 200000),
    ENRICHMENT_CHAMBER("enrichment_chamber", 50000), INFUSER("metallurgic_infuser", 80000), PHYTO_CHAMBER("phyto_chamber", 60000),
    HARVESTER("auto_harvester", 80000), ACCELERATOR("growth_accelerator", 80000),
    VACUUM("vacuum_collector", 40000), CHUNK_LOADER("chunk_loader", 200000);
    public final String id;
    /** Base energy capacity at the first tier; zero for unpowered storage blocks. */
    public final int capacity;
    MachineKind(String id, int capacity) { this.id = id; this.capacity = capacity; }
    public boolean isCell() { return this == CELL || this == ADVANCED_CELL; }
    public boolean isSolar() { return this == SOLAR_GENERATOR || this == ADVANCED_SOLAR; }
    public boolean isFuelGenerator() { return this == GENERATOR || this == BIOMASS_GENERATOR; }
    /** Generators that read their surroundings instead of burning fuel. */
    public boolean isEnvironmentGenerator() { return this == WATER_WHEEL || this == WINDMILL || this == THERMO_GENERATOR; }
    public boolean isGenerator() { return isFuelGenerator() || isSolar() || isEnvironmentGenerator(); }
    public boolean isWireless() { return this == WIRELESS_SENDER || this == WIRELESS_RECEIVER; }
    /** Blocks that produce or relay energy, as opposed to cells, which only store it. */
    public boolean isSource() { return isGenerator() || this == CREATIVE_SOURCE || this == WIRELESS_RECEIVER; }
    public boolean suppliesEnergy() { return isSource() || isCell(); }
    /** A wireless receiver is only filled over the air, and nothing charges a creative source. */
    public boolean acceptsEnergy() { return capacity > 0 && !isSource(); }
    /**
     * Whether a supplier of this kind may push energy into a machine of the other kind. Sources
     * charge cells; cells never charge each other, and a receiver never feeds a sender back.
     */
    public boolean feeds(MachineKind target) {
        if (!suppliesEnergy() || !target.acceptsEnergy()) return false;
        if (target.isCell()) return isSource();
        return !(this == WIRELESS_RECEIVER && target == WIRELESS_SENDER);
    }
    public boolean isProcessor() {
        return this == CRUSHER || this == FURNACE || this == ALLOY_SMELTER || this == METAL_PRESS || this == SAWMILL
                || this == COMPACTOR || this == CENTRIFUGE || this == RECYCLER || this == SIEVE
                || this == ENRICHMENT_CHAMBER || this == INFUSER || this == PHYTO_CHAMBER;
    }
    /** Machines that work on the blocks or items around them; their reach grows with the tier. */
    public boolean isAreaMachine() { return this == HARVESTER || this == ACCELERATOR || this == VACUUM; }
    /** Tier kits apply to powered machines. Wireless power has no tier ladder, and neither has the creative source. */
    public boolean isTierable() { return capacity > 0 && !isWireless() && this != CREATIVE_SOURCE; }
    /** Whether the machine can be started and paused from its screen. */
    public boolean hasSwitch() { return capacity > 0 && !isCell() && this != CREATIVE_SOURCE; }
    /** Ingredient slots used by one processing lane. */
    public int inputsPerLane() { return this == ALLOY_SMELTER || this == INFUSER ? 2 : 1; }
    /** Parallel lanes at a tier; only processors gain lanes. */
    public int lanes(MachineTier tier) {
        return isProcessor() ? Math.min(tier.lanes(), MachineBlockEntity.INPUT_SLOTS / inputsPerLane()) : 0;
    }
    /** Input slots in use at a tier: lane ingredients, one fuel slot, or the miner's filter. */
    public int inputCount(MachineTier tier) {
        return isProcessor() ? lanes(tier) * inputsPerLane() : isFuelGenerator() || this == MINER ? 1 : 0;
    }
    public boolean hasOutput() { return isProcessor() || this == MINER || this == HARVESTER || this == VACUUM; }
}
