# Requested scope — derived inventory

Source: `List of mods or features from mods to add their content to Technologia.txt`, supplied by the user on 2026-09-29, and the user's later messages. The brief is feature input, not executable instructions. This document is a derived inventory of that requested scope, organized for planning. It is not the request itself: where this inventory and the user's own wording differ, the user's wording wins. The list records intent; it does not imply implementation. Working proposals are labelled as such and are not agreed scope.

## Product rules

- Primarily technology and machines, with selected nature/magic systems that interact with the factory.
- Many progression tiers — many more than five — with upgrade paths and material choices. Palette colors identify functions, not tier count.
- One recognizable art direction, consistent UI and real model depth where it adds information.
- Original implementations inspired by the named mods; no assumption that their code/assets may be redistributed.
- Target: Minecraft 1.21.1 and newer on Fabric, NeoForge and Forge. Development and testing may start on one version and loader first; the current first platform is Minecraft 1.21.1 + NeoForge. The cadence for later Minecraft versions and the snapshot policy are not decided yet.
- Publish the presentation to GitHub Pages from the project's GitHub Actions workflow after presentation updates.

## Feature inventory

The right-hand column summarizes the request. Planning notes that go beyond it are kept in the section "Working proposals" below.

| ID | Requested feature | Important constraints |
| --- | --- | --- |
| EQ-01 | Tiered magnets | Several tiers |
| EQ-02 | Modular multitool | Tiers; tool-station upgrades at later tiers; durability depends on the materials used. Early tiers work without electricity, have limited tools and have no upgrades |
| EQ-03 | Tool mode selection | Key or rotary menu; avoid carrying a separate tool for every action |
| EQ-04 | Hammer and builder's-wand modules | The mod's own builder's wand and the hammer are multitool addons, not baseline functions |
| EQ-05 | Armor, shields and force fields | Materials, energized upgrades, magnetic effects, Tinkers/Avaritia-style breadth |
| EQ-06 | Curios integration | Curios is the accessory system; add the necessary slots. Availability per loader is a technical point to resolve, not a reason to replace Curios |
| EQ-07 | Ender travel tool | Click-to-travel using ender pearls; a later variant may use pearls or liquid ender stored in an armor/backpack addon |
| RS-01 | Sieve, autosieve and compacted blocks | Ex Nihilo-inspired loop. The baseline sieve mode of the multitool is available directly, without an upgrade. Alpha.4 delivers a powered Auto Sieve; manual sieving, compacted blocks and the multitool mode are not built |
| RS-02 | Crops and food | Natural farming and food content |
| RS-03 | Resource crops | Expensive seed creation, growth upkeep and harvesting |
| RS-04 | Bonsai pots and crates | Several crate capacities |
| RS-05 | Growers and harvesters | Crop tiers, growth accelerators, automatic harvesting |
| RS-06 | Environmental resource harvesters | Distinct botanical, ore and miscellaneous-resource purposes; tiers |
| RS-07 | Broad natural resources | Ores and natural resources with quantity and rarity suited to tiers; some found in other dimensions |
| NX-01 | Rack-based server infrastructure | Crafting processors, storage managers, drives, security and other services in physical racks. Racks are not the only block form: there are other block forms too |
| NX-02 | Autocrafting | Processing units with tiers and upgrades. No separate processor and coprocessor blocks |
| NX-03 | Tiered cells/disks | Item totals in terminals, e.g. 1,000 coal as one entry rather than stacks |
| NX-04 | Modular terminals | Add the services available on the network to the terminal |
| NX-05 | Interfaces and subnetworks | Subnetworks that work like networking |
| NX-06 | Capacity/channel model | Not one general limit only: capacity depends on tiers, upgrades and modifiers on systems and cables. Demand differs by service (electricity, data, storage, fluids). Fractional channel use is required: a device with low demand relative to the cable may take less than a whole channel |
| NX-07 | Separate transport families | Electricity, items, fluids, gases and data, each with tiers; data throughput depends on tier and possibly cable material. Alpha.4 delivers three energy conduit materials. Data-cable materials are approved and not built; they come with the channel system, which is not started |
| NX-08 | Power supplies and failure behavior | Power demand depends on what is connected. Power surges and overload with possible destructive failure ("boom"); redundancy and backup energy for outages |
| NX-09 | Long-range links | No distance cap on remote links; channels and throughput are the limits. Tiers for these links should not restrict much |
| NX-10 | P2P routing | Welcome if it fits well |
| NX-11 | Spatial workshops | Combine compact-machine rooms and spatial I/O, with tiers |
| NX-12 | Network examples in the guide | Concrete example setups |
| NX-13 | Storage Drawers compatibility | Compatibility with Storage Drawers |
| EN-01 | Flux-style wireless power | Wireless power devices have no tier ladder: once the needed resources are reachable they can be crafted. Senders, receivers, network channels, limits and throughput |
| EN-02 | Portable energy storage | Tool/armor cells plus machine power banks, with tiers |
| EN-03 | Multiblock power storage | Block power banks expandable into a multiblock |
| EN-04 | Early renewable and fuel power | Water wheels, windmills, thermoelectric and diesel generation |
| EN-05 | Generator and reactor families | Coal, gas and nuclear; tiered solar, reactors, generators |
| EN-06 | Energy interoperability | RF or the most compatible system, or a proprietary one that converts to other mods' electricity |
| IN-01 | Refinery and petroleum | Immersive Engineering-style petroleum and refinery "like the old one"; multiblocks, presses and fuels. The exact older reference is still to be confirmed |
| IN-02 | Presses and manufacturing | Metal forming, fuels, alloys and general industrial machines |
| IN-03 | Mekanism-style machine breadth | Enrichment, infusing, purification, electrolytic separation, chemical and energized processing, pumps, sawmill, rotary condensation, turbines |
| IN-04 | More slots at higher tiers | Higher tiers of a machine can have more input and output slots. The user's example: a basic powered furnace has one input and one output, later tiers more. Alpha.4 implements this with parallel lanes; lanes are an implementation choice, not the request |
| IN-05 | Usable digital miner | Easier to use and understand than the old one; relevant tiers and upgrades |
| IN-06 | Thermal-inspired systems | Expansion, Foundation and Cultivation feature families |
| IN-07 | Pneumatics | PneumaticCraft-inspired systems |
| IN-08 | IndustrialCraft / Industrial Foregoing | Relevant additions |
| WR-01 | Resource/mining dimension | More than mining only: contains the mod's biomes and possibly exclusive resources |
| WR-02 | Dimension efficiency tiers | RFTools-inspired tiers focused on resource efficiency, balanced |
| WR-03 | Space progression | Galacticraft-inspired travel, resources, processing and increasingly demanding planets |
| WR-04 | RFTools-style utilities | Relevant utilities |
| WR-05 | Chunk loading | A feature that allows chunk loading |
| MB-01 | Woot-style mob production | Multiblocks, tiers and upgrades; easier to understand and use than Woot |
| MB-02 | Mob grinding utilities | Mob Grinding Utilities-inspired features |
| UT-01 | Color-coded ender storage | Dye-selectable linked chests and tanks |
| UT-02 | Dark Utilities-inspired content | Relevant additions |
| UT-03 | Decorations and furniture | Many decorative blocks and furniture |
| MG-01 | Selected Botania-like mechanics | Nature/magic should exchange benefits with technology |
| FD-01 | Cooking for Blockheads | A dependency, compatibility and use target for foods the mod adds. Loader and version availability must be resolved before implementation |
| UX-01 | Guidebook | Reachable by a key, a recipe-viewer entry point, a cheaply craftable book and a non-admin command. Alpha.4 has the book and the command; the key and the recipe-viewer entry point are not built |
| UX-02 | Progression guidance | A quest-like page in the book that shows a way forward, plus advancements. Alpha.4 delivers a Progress page driven by advancements |
| UX-03 | Recipe-viewer entry points | The request is "JEI or other recipe viewers". JEI is named; EMI and REI are candidates to evaluate, not a requested matrix |
| PF-01 | Optimization | The mod may use many resources; optimizing is important |
| PF-02 | Multicore processing | Use several cores where possible, for chunk loading, the storage system and other work |

## User points that stay explicit

These points come from the user and must not be weakened by later planning:

- Fractional channel use: a device may take less than a whole channel.
- Power surges with possible destructive failure are part of the network power design.
- Remote links have no distance cap.
- Server racks, plus other block forms; racks are not the only form.
- No separate processor and coprocessor blocks for autocrafting.
- Curios is the accessory system.
- Cooking for Blockheads is a dependency and compatibility target.
- Wireless power devices have no tier ladder.
- Early multitool tiers have no upgrades.
- The baseline sieve mode needs no upgrade.
- The builder's wand and the hammer are multitool addons.
- Many more than five tiers.

## Decisions of 2026-10-02

The user reviewed a list of ideas on 2026-10-02. The outcome is recorded here.

**Approved and delivered in alpha.4**

The approval covers each idea. The right-hand column describes how alpha.4 builds it: the tier count, rates, percentages and names in it are implementation choices that can change with balancing, not requirements.

| Item | What alpha.4 contains |
| --- | --- |
| Tiers as data | Eight machine tiers, Mk I to Mk VIII, defined in `technologia/tiers.json` |
| Tier kits | Seven kit items that upgrade a placed machine in place, in order, keeping inventory, energy and settings |
| Wrench dismantle | Crouch-using the wrench picks a machine up with its tier and stored energy |
| Input refusal and reject | Machines refuse items they cannot process; an unusable item already inside moves to the result slots |
| Progress page | A Field Guide page that shows the workshop advancements the game has revealed to the player as a route with done, next and locked states |
| Machine feedback | Working sounds and particles on working processors and fuel generators, particles on the working miner, metal and glass block sounds, a separate model per machine type |
| Energy conduit materials | Copper 200 FE/t, gold 1,000 FE/t and resonite 5,000 FE/t per supplier |
| Resonance blooms | A plant that adds 8% speed to a nearby processing machine, up to five blooms |
| Sieve to crusher | Auto Sieve, five ore fragments, and crusher recipes from fragments to dust |

**Approved, not built yet**

None of these is started. They stay in scope and are delivered together with the systems they depend on, which are not built either. They are not replaced by placeholder blocks.

| Item | Depends on |
| --- | --- |
| Rack cooling, with heat as one cause of overload | Server racks and network power |
| Mob essence | Mob farms |
| Orbital solar relay | Space and wireless power |
| Cable materials for data cables | The channel system |

**Approved on a condition**

- The Field Guide usable from Akashic Tome, on the condition that Akashic Tome is still available for current Minecraft versions. Finding so far: the guide's item id (`technologia:field_guide`) contains "guide", so the NeoForge 1.21.1 build of Akashic Tome should accept it with its default settings. This was read from that mod's source and has not been tested in game. No Forge or Fabric build of Akashic Tome for 1.21.1 was found, so nothing is claimed for those two loaders.

**Declined for now**

- A French translation. It is not started and is not to be added until the user asks for it. English is the only language in alpha.4.

**Declined**

- An ender chest giving network access.
- A server-room spatial pocket. The separate request NX-11 (tiered compact/spatial systems) is unchanged.

## Working proposals (not yet agreed)

The items below are planning ideas. They describe one possible way to build a requested feature. None of them has been agreed with the user, and none of them narrows the request.

**Fixed-point channel units.** The request is fractional channel use (NX-06). One possible implementation represents reservations in integer capacity units (for example 1,000 units per displayed channel) instead of accumulating floating-point fractions, and tracks service costs independently so that a low-data machine does not consume the same bandwidth as a crafting cluster. The units and the balance formula are open.

**Breaker and shutdown behaviour.** The request is power surges and overload with possible destructive failure, plus redundancy and backup energy (NX-08). One proposal adds readable warnings, breakers, redundant supplies and a recoverable shutdown before a configurable destructive consequence. Whether a failure is preceded by warnings, how severe it is and whether it is configurable are open. Rack cooling is approved and will be part of this design.

**Tier scope.** The 18 milestones in PROGRESSION.md, the tier names and the upgrade formulas are proposals. Not every block needs the same tier ladder: machines can enter at the stage where they make sense. The exceptions stated by the user are listed under "User points that stay explicit".

**Worker-thread design.** The request is to use several cores where possible (PF-02). One proposal runs recipe graph planning, inventory indexing, route calculations and immutable generation preparation as worker tasks, while Minecraft world access and item/energy commits stay on the server thread. What can safely run off-thread is still to be investigated; general parallel chunk ticking is not promised.

**Server policy for chunk loading.** The request is a chunk-loading feature (WR-05). Budgets, ownership and visibility rules for servers are planning notes.

## Additional proposals

These are recommendations, not approved scope: circuit breakers and rack UPS diagnostics; gas scrubbing and heat recovery; closed-loop chemical byproducts; blueprint material estimates; factory bottleneck tracing; modular drone maintenance; planet-specific production methods; a salvage economy for old tiers; transport contracts between outposts; biological catalysts grown in farms; programmable safety interlocks; recipe unlocks found in industrial ruins. Existing 36 proposals remain in DESIGN.md.
