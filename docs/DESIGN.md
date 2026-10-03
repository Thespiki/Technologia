# Technologia — design direction

An original technology mod for Minecraft 1.21.1. The ambition is the satisfying scale of 1.12.2 automation, with one coherent visual language and understandable tools. Fabric, NeoForge and Forge receive separate jars built around shared gameplay. They are separate ecosystems; one world/server cannot run all three loaders simultaneously.

This is a prototype and design proposal, not a complete replacement for AE2, Mekanism, RFTools or Draconic Evolution. Their code, branding and art are not being repackaged. Their current projects also continue to exist on newer versions. Draconic Evolution and Chaotic Evolution are both named inspirations for the late game. Which mod or addon “Chaotic Evolution” refers to is still to be confirmed. Until then, the “Chaos” and “Chaotic” entries in this document are placeholders for that inspiration, not a definition of it.

## An expandable progression

The working proposal is **18 milestones, T00–T17**, detailed in [PROGRESSION.md](PROGRESSION.md). This is not a fixed maximum. The following five broad themes describe the journey; they are not five machine tiers or five palette-based levels.

| Theme | Player's new capability | Main systems | Visual evolution |
| --- | --- | --- | --- |
| Workshop | Stop repeating hand work | Coal power, crushing, electric smelting, basic storage | Ceramic housings, brushed metal, amber instruments |
| Industry | Build a self-sustaining base | Factories, farming, fluid handling, selective mining | Larger modular frames, readable pipes and ports |
| Network | Ask the factory for what you need | Searchable storage, patterns, stock targets, blueprints | Cyan data paths, compact panels, persistent labels |
| Ascendant | Build spectacular infrastructure | Fusion assembly, shields, modular equipment, giant batteries | Open energy structures, restrained violet accents |
| Chaos | Master unusual world rules | Rift stabilization, original bosses, exotic fabrication | Fractured geometry and controlled purple-white energy |

Existing workshop prototype content is enumerated in the README. Everything else here is a proposed expansion.

Machine tiers are a separate scale from these themes and from the 18 milestones. Since alpha.4 there are eight machine tiers, Mk I to Mk VIII, defined as data in `technologia/tiers.json`. Each tier sets a machine's lanes, speed, energy capacity, efficiency, generation and transfer rate. Since alpha.5 a machine above Mk I shows its tier on its front as a coloured band with pips.

## Art direction and interface rules

**Industrial precision with an exotic late game.** The common chassis survives each tier; it evolves instead of being replaced by unrelated art styles. Avoid opaque neon boxes and noisy rainbow interfaces.

| Role | Color | Use |
| --- | --- | --- |
| Recess | `#0C121B` | Slots, gaps and deep screen surfaces |
| Ceramic | `#1B2735` | Main housings and panels |
| Metal | `#415267` | Edges, brackets and inactive routes |
| Legible text | `#EDF4FC` | Primary labels |
| Muted text | `#AEBED1` | Help and secondary readings |
| Power / data | `#55D9D0` | Energy bars, storage indicators |
| Processing | `#E8B46A` | Input ports, process progress |
| Exotic | `#B08DF5` | Ascendant/chaotic accents |

Use 32 px textures, a consistent screw/vent grid, and recognizable silhouettes for each machine family. Inputs, outputs and status need shapes and words as well as color. The first textures are deterministic original prototype assets generated from this palette; production art requires another review for silhouette, orientation, animation and readability in-world.

Since alpha.3 every machine type has its own static model built from vanilla block-model cuboids; alpha.5 has 31 of them. Examples: six recessed drive bays on the storage core, a freestanding terminal with a recessed screen, open press frames, saw housings, raised solar panels and exposed cell coils. Collision and selection shapes follow the model geometry. Working machines emit particles, except solar generators; processors, fuel generators and the Water Wheel also play a quiet working sound. Machines, cables and building blocks use metal sounds and reinforced glass uses glass sounds. Since alpha.5 a tier band is added to a machine above Mk I: a strip at the bottom of its front in the colour of its tier kit, with one pip per tier number (two for Mk II, eight for Mk VIII). The rest of the model does not change with the tier. Larger framed multiblocks and moving components remain planned.

Every final machine UI should answer: What goes in? What comes out? How much power does it need? Why has it stopped? Keep basic operation on the first screen. Put side routing, redstone rules and upgrades in consistent secondary panels. Show recipes, missing ingredients and costs before starting. Tooltips must describe actions in player language. Keyboard navigation, readable contrast, small GUI scales and localization are release requirements.

Since alpha.4 one machine screen serves every powered machine: a 4×4 grid of ingredient slots on the left, a 9×3 grid of result slots on the right, the tier name at the top right and one progress line per lane. Only the slots a tier uses are shown. Alpha.5 adds a redstone button, an Eject button on machines with result slots, channel and limit buttons on the wireless blocks, the installed mesh on the Auto Sieve, and what a generator or wireless block moves right now in the energy tooltip. A machine without ingredient slots explains itself in the free space of its screen. The storage catalogue is searchable, shows item totals and, since alpha.3, combines up to four cores. The native Field Guide has twelve chapters and 53 articles. The guide takes inspiration from quest-book navigation while using original Technologia visuals. Its Progress page shows the workshop advancements as a route with done, next and locked states, read from the advancements the server has revealed to the player. Quest rewards and team mechanics are not specified and not present. Side configuration, a control dashboard, a guide hotkey and storage drives are still planned.

## Main requested systems

These paragraphs combine the request with design proposals. The requested scope itself is listed in [REQUIREMENTS.md](REQUIREMENTS.md); where a proposal here differs from it, the request applies.

**Nexus storage:** Start with connected storage and terminals; alpha.3 terminals already combine up to four cores. The next step was decided on 2026-10-03 and is planned for alpha.6: a server rack is one block, a cabinet with bays for modules (drives, storage manager, power supply, fan, security), and its front shows the installed modules. Cabinets side by side form the server room. When the network asks for too much power there is a visible warning, then a breaker shuts the network down cleanly; nothing is destroyed and the breaker must be re-armed. Racks replace Storage Cores: existing cores keep working in old worlds but are no longer craftable, and players must not lose items. None of this is built yet. Beyond that, the proposals are: add searchable unified inventory, disks, bulk vaults, import/export buses, pattern autocrafting, machine scheduling, stock targets and wireless access. Simple networking should work without channel arithmetic; an expert mode can make bandwidth and routing into puzzles. Crafting failures must point to their cause, such as missing tin or an unpowered furnace.

**Industrial processing:** Start with raw ore → two dust → smelting. The Alloy Smelter, Metal Press, Sawmill, Compactor, Centrifuge and Recycler exist since alpha.3, the Auto Sieve since alpha.4, and the Enrichment Chamber, Metallurgic Infuser and Phyto Chamber since alpha.5. Still to add: a washer, separator, electrolyzer, chemical reactor, crystallizer and multi-recipe factories. Later ore multiplication should reward infrastructure, with clear yields and reusable byproducts. Do not create new copies of every vanilla metal.

**Survey Miner:** Preserve the old digital miner's selective excavation, radius/height settings, filters, replacement and silk-touch choices. The prototype has a bounded region below the miner, one filter slot that only a player can set, pause/rescan, owner checks and permission callbacks. Since alpha.4 only the owner can take results or change the filter from the screen (automation and block breaking are not owner-checked), and higher machine tiers scan and mine faster. Later UI should preview matching blocks and total energy, offer named presets and explain protection failures. The miner itself loads no chunks. Alpha.5 adds a separate Chunk Loader block that needs power and that a server can switch off or limit in the config; per-player quotas are not built.

**Architect utilities:** A preview-first builder for rooms, tunnels, spheres and roads; saved blueprints; named teleport pads; factory monitors; and readable automation rules. Start with presets before introducing a full logic editor. A build preview should show exact material and energy requirements and make its operation cancellable.

**Ascendant / Chaos:** Original fusion assembly, a visible multiblock energy reservoir, modular shields/flight/mining equipment and repeatable boss encounters. These are proposals; what the Chaotic Evolution inspiration should bring is still to be confirmed. Draconic and chaotic cores in the prototype are creative inventory concepts, without survival recipes or powers.

## More content to consider

The Status column separates what a released version already contains from what is still a proposal. A "partly delivered" row lists what exists; the rest of that row is still a proposal.

| Proposal | Why it belongs | Status |
| --- | --- | --- |
| Compact item/fluid/power conduits | Dense, tidy factories with inspectable routes | Partly delivered: Energy Conduits and Item Transfer in alpha.3, gold and resonite conduit materials in alpha.4, a superconducting conduit in alpha.5. Fluid conduits remain a proposal |
| Parallel factory upgrades | Expand throughput while retaining configuration | Delivered in alpha.4: tier kits upgrade a placed machine in place and add parallel lanes |
| Alloy furnace and forming press | Make useful conductors, plates and structural alloys | Delivered in alpha.3: Alloy Smelter and Metal Press |
| Brine wells and salt flats | Give chemistry its own exploration loop | Proposal |
| Sulfur vents and geothermal taps | Turn location into a power/processing choice | Proposal. Alpha.5 already makes location matter for power with the Water Wheel, the Windmill and the Thermoelectric Generator; vents and taps are not built |
| Resin trees and rubber processing | Link farming to insulation and flexible components | Proposal |
| Oil seeps and fractionation | Fuels, polymers and reusable chemical byproducts | Proposal |
| Bauxite deposits and alumina refining | Lightweight machinery through a distinctive processing chain | Proposal |
| Biomass digesters | A reason to reuse farm waste | Partly delivered in alpha.3: the Biomass Generator burns sawdust, saplings, wheat, kelp and sugar cane. A digester remains a proposal |
| Hydroponics and tree farms | Renewable food, wood and industrial crops | Partly delivered in alpha.5: the Phyto Chamber grows crops and saplings from one seed, the Bonsai Pot grows wood, and the Auto Harvester and Growth Accelerator work on fields. Industrial crops remain a proposal |
| Livestock collection and controlled spawning | An automation branch beyond mining | Proposal |
| Resource cultivators | Optional skyblock-friendly deterministic materials | Proposal |
| Recycling and salvage | Recover value from obsolete equipment | Partly delivered in alpha.3: the Recycler turns iron and gold tools and armor into nuggets. Salvage of machines remains a proposal |
| Prospecting scanner | Explore resource regions before automated mining | Proposal |
| Blueprint library | Reuse favorite factory layouts with a material bill | Proposal |
| Building wand and exchange tool | Reduce repetitive base construction | Proposal |
| Elevators, travel anchors and teleport pads | Make large bases pleasant to navigate | Proposal. Alpha.5 has a Travel Staff (travel to the block you look at) and Vector Plates; elevators, anchors and pads are not built |
| Named wireless power links | Connect remote outposts with visible throughput limits | Partly delivered in alpha.5: a Wireless Energy Sender and Receiver with numbered public channels and a sender limit. Names and private links remain a proposal |
| Multiblock turbines and reactors | Give large builds functional purpose | Proposal |
| Energy reservoir with projected runtime | A visible base centerpiece and useful diagnostic | Proposal |
| Modular tools and equipment | Choose flight, shields, magnetism and area mining | Proposal. Alpha.5 has three magnets as separate items; they are not modules |
| Keep-in-stock crafting | Maintain reserves with one understandable rule | Proposal |
| Factory dashboard | One place for power trends, blocked machines and tasks | Proposal |
| Factory black box | Explain what caused a production line to stop | Proposal |
| Network route overlay | Highlight disconnected or overloaded components | Proposal |
| Production contracts | Optional goals for sustained output and efficiency | Proposal |
| Closed-loop challenges | Reward reuse and self-sustaining factories | Proposal |
| Ancient industrial ruins | Find blueprints, damaged machinery and salvage | Proposal |
| Rift stabilizer | Control anomalies to obtain exotic resources | Proposal |
| Modular pocket workshops | Compact automation spaces with explicit performance limits | Proposal |
| Custom industrial dimensions | Much later: curated resource worlds with clear costs | Proposal |
| Classic and Expert profiles | Open-ended building or more demanding production chains | Proposal |
| Creative-item milestones | Optional long-term goals after the main progression | Proposal |
| Shared guide and quest chapters | Teach systems through usable mini-factories | Partly delivered: the Field Guide since alpha.2, and a Progress page driven by advancements in alpha.4. Shared and team progress remain a proposal |
| Computer control API | Allow advanced players to script diagnostics and requests | Proposal |
| Cosmetic factory kit | Matching catwalks, windows, lamps, tanks and control panels | Partly delivered: ten building blocks in alpha.3, including lamps, reinforced glass, grilles and control panels, and seven more in alpha.5, including catwalks, floor panels, tables, shelves and warning lights. Tanks remain a proposal |

## Why these proposals fit the era

[Enigmatica 2 Expert](https://www.curseforge.com/minecraft/modpacks/enigmatica2expert) explicitly combined AE2, Mekanism, Thermal Expansion, Ender IO, Immersive Engineering and NuclearCraft, with extensive quests, technology branches and creative-item goals. That supports a coherent progression with several valid routes through it.

[SkyFactory 4](https://www.curseforge.com/minecraft/modpacks/skyfactory-4) foregrounded automation, chosen resource production and optional progression. [StoneBlock](https://www.curseforge.com/minecraft/modpacks/stoneblock) offered an underground sandbox with guiding quests. [FTB Revelation](https://www.curseforge.com/minecraft/modpacks/ftb-revelation) is a useful reference for open-ended kitchen-sink play. These are examples of pack design, not a measured survey or a historical download ranking. Present-day lifetime downloads cannot establish what was most downloaded in 2018.

Mechanical reference points: [AE2's archived introduction](https://appliedenergistics.org/ae2-site-archive/Getting-Started/), [Mekanism Digital Miner](https://wiki.aidancbrady.com/wiki/Digital_Miner), [Mekanism processing](https://wiki.aidancbrady.com/wiki/Ore_Processing), [Thermal Expansion's 1.12 machines](https://teamcofh.com/docs/1.12/thermal-expansion/machines/), [RFTools](https://www.curseforge.com/minecraft/mc-mods/rftools), [Industrial Foregoing](https://www.curseforge.com/minecraft/mc-mods/industrial-foregoing), and [Draconic Evolution](https://www.curseforge.com/minecraft/mc-mods/draconic-evolution). The proposed black box, contracts and rift systems are original design directions, not claims about those mods.
