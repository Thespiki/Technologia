# Requested scope — source of truth

Source: `List of mods or features from mods to add their content to Technologia.txt`, supplied by the user on 2026-09-29. This is a feature brief, not executable instructions. The list below records intent; it does not imply implementation.

## Product rules

- Primarily technology and machines, with selected nature/magic systems that interact with the factory.
- Many meaningful progression tiers, upgrade paths and material choices. Palette colors identify functions, not tier count.
- One recognizable art direction, consistent UI and real model depth where it adds information.
- Original implementations inspired by the named mods; no assumption that their code/assets may be redistributed.
- First develop and test on Minecraft 1.21.1 + NeoForge. Eventually cover 1.21.1 and later stable releases on Fabric, NeoForge and Forge wherever available.
- Publish the presentation to GitHub Pages from the project's GitHub Actions workflow after presentation updates.

## Feature inventory

| ID | Requested feature | Important constraints |
| --- | --- | --- |
| EQ-01 | Tiered magnets | Upgrades and relevance across progression |
| EQ-02 | Modular multitool | Tool station, materials and alloys; early tiers use durability, limited modes and no electricity |
| EQ-03 | Tool mode selection | Hotkey or radial selector; avoid carrying separate processing tools |
| EQ-04 | Hammer and builder-wand modules | Addons to multitool; distinguish optional tool modules from baseline sieve interaction |
| EQ-05 | Armor, shields and force fields | Materials, energized upgrades, magnetic effects, Tinkers/Avaritia-style breadth |
| EQ-06 | Curios integration | Appropriate accessory slots; verify availability per loader and choose compatible adapters where necessary |
| EQ-07 | Ender travel tool | Click-to-travel; ender pearl or stored liquid-ender fuel in armor/backpack addon |
| RS-01 | Sieve, autosieve and compacted blocks | Ex Nihilo-inspired loop; baseline interactions available without a separate tool upgrade |
| RS-02 | Crops and food | Natural farming and food content |
| RS-03 | Resource crops | Expensive seeds, growth upkeep and harvesting; avoid trivial infinite resources |
| RS-04 | Bonsai pots and crates | Several crate capacities |
| RS-05 | Growers and harvesters | Crop tiers, growth accelerators, automatic harvesting |
| RS-06 | Environmental resource harvesters | Distinct botanical, mineral and miscellaneous resource roles; tiers and upgrades |
| RS-07 | Broad natural resources | Ores, alloys, oils, gases, natural materials and dimension-specific resources |
| NX-01 | Rack-based server infrastructure | Crafting processors, storage management, drives, security and other services in physical racks |
| NX-02 | Autocrafting | Tiered/upgradable processing units; no compulsory separate coprocessor block class |
| NX-03 | Tiered cells/disks | Item totals in terminals, e.g. 1,000 coal as one entry rather than stacks |
| NX-04 | Modular terminals | Add service panels based on installed network modules |
| NX-05 | Interfaces and subnetworks | Network-like isolation/routing and shared services |
| NX-06 | Capacity/channel model | Variable cost by electricity, data, storage, fluid or other service usage; capacity modified by cable/material/tier/upgrades |
| NX-07 | Separate transport families | Electricity, items, fluids, gases and data; meaningful throughput tiers |
| NX-08 | Power supplies and failure behavior | Loads, surges, overload, redundancy and backup; readable diagnostics before dangerous failure |
| NX-09 | Long-range links | No arbitrary distance cap; throughput/channel limits, approachable tier progression |
| NX-10 | P2P routing | Add if useful alongside subnetworks and rack services |
| NX-11 | Spatial workshops | Combine compact-machine rooms and spatial I/O, with tiers |
| NX-12 | Network examples in the guide | Concrete setups and troubleshooting diagrams |
| NX-13 | Storage Drawers compatibility | External inventory/bulk-storage integration |
| EN-01 | Flux-style wireless power | No tier ladder after unlock; senders/receivers, networks, limits and throughput controls |
| EN-02 | Portable energy storage | Tool/armor cells plus machine power banks |
| EN-03 | Multiblock power storage | Tiered cells expandable into large structures |
| EN-04 | Early renewable and fuel power | Water wheels, windmills, thermoelectric and diesel generation |
| EN-05 | Generator and reactor families | Coal, gas and nuclear; tiered solar, reactors, turbines and upgrades |
| EN-06 | Energy interoperability | FE/RF-facing adapters and Fabric energy API; conversion only where necessary |
| IN-01 | Refinery and petroleum | Older Immersive Engineering/Petroleum-style factory loop and multiblocks |
| IN-02 | Presses and manufacturing | Metal forming, fuels, alloys and general industrial machines |
| IN-03 | Mekanism-style machine breadth | Enrichment, infusing, purification, electrolytic separation, chemical and energized processing, pumps, sawmill and rotary condensation |
| IN-04 | Parallel factory upgrades | Tiers can add independently usable processing lanes/input and output slots |
| IN-05 | Usable digital miner | Selective mining, clear controls and relevant upgrades |
| IN-06 | Thermal-inspired systems | Expansion, Foundation and Cultivation feature families |
| IN-07 | Pneumatics | Pressure-based automation and associated production |
| IN-08 | IndustrialCraft / Industrial Foregoing | Relevant industrial and biological automation |
| WR-01 | Resource/mining dimension | More than a flat quarry world; new biomes and exclusive resources |
| WR-02 | Dimension efficiency tiers | RFTools-inspired customization and resource efficiency, with balanced costs |
| WR-03 | Space progression | Galacticraft-inspired travel, resources, processing and increasingly demanding planets |
| WR-04 | RFTools-style utilities | Relevant building, travel, monitoring and automation tools |
| WR-05 | Chunk loading | Explicit server policy, budgets, ownership and visibility |
| MB-01 | Woot-style mob production | Understandable multiblocks, tiers and upgrades |
| MB-02 | Mob grinding utilities | Collection, processing and relevant automation |
| UT-01 | Color-coded ender storage | Dye-selectable linked chests and tanks |
| UT-02 | Dark Utilities-inspired content | Relevant factory/automation utilities |
| UT-03 | Decorations and furniture | A substantial matching factory/base building kit |
| MG-01 | Selected Botania-like mechanics | Nature/magic should exchange inputs, outputs or efficiency benefits with technology |
| FD-01 | Cooking for Blockheads | Intended food integration/dependency; verify actual loader/version coverage before making it a hard dependency |
| UX-01 | Guidebook | Hotkey, cheap book, non-admin give command and recipe-viewer entry points |
| UX-02 | Progression guidance | Quest-like route through the book plus advancements |
| UX-03 | Recipe-viewer compatibility | JEI/EMI/REI as available per loader/version |
| PF-01 | Optimization | Profiling, bounded tick work, efficient network caching, no wasteful global scans |
| PF-02 | Safe multicore work | Immutable snapshots and off-thread planning; world/inventory mutation stays on the server thread |

## Design resolutions to carry forward

**Fractional channels:** represent reservations in integer capacity units (for example 1,000 units per displayed channel), rather than accumulating floating-point fractions. Track service costs independently so a low-data machine does not consume the same bandwidth as a crafting cluster. This is a proposed implementation, not a locked balance formula.

**Power failure:** requested surges and overload should have readable warnings, breakers, redundant supplies and a recoverable shutdown before a configurable destructive consequence. Never introduce random explosions as an unexplained maintenance tax.

**Tier scope:** not every block needs 18 copies. Machines can enter at the stage where they make sense, with throughput/parallelism/material/precision/efficiency upgrades. Wireless flux devices explicitly have no tier ladder after unlock.

**Multicore:** recipe graph planning, inventory indexing, route calculations and immutable generation preparation can be worker tasks. Minecraft world access and committing item/energy transfers must respect the loader/server threading contract. Do not promise general parallel chunk ticking.

## Additional proposals

These are recommendations, not approved scope: circuit breakers and rack UPS diagnostics; gas scrubbing and heat recovery; closed-loop chemical byproducts; blueprint material estimates; factory bottleneck tracing; modular drone maintenance; planet-specific production methods; a salvage economy for old tiers; transport contracts between outposts; biological catalysts grown in farms; programmable safety interlocks; recipe unlocks found in industrial ruins. Existing 36 proposals remain in DESIGN.md.
