# Technologia — design direction

An original technology mod for Minecraft 1.21.1. The ambition is the satisfying scale of 1.12.2 automation, with one coherent visual language and understandable tools. Fabric, NeoForge and Forge receive separate jars built around shared gameplay. They are separate ecosystems; one world/server cannot run all three loaders simultaneously.

This is a prototype and design proposal, not a complete replacement for AE2, Mekanism, RFTools or Draconic Evolution. Their code, branding and art are not being repackaged. Their current projects also continue to exist on newer versions. Here, “chaotic” means the requested top-tier endgame; a separate addon can be identified later.

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

The storage core now has six physically recessed drive bays, and the terminal has a recessed screen and projecting control ledge, using static vanilla block-model cuboids. These are original geometry prototypes, pending client visual review. Larger framed multiblocks and moving components remain planned.

Every final machine UI should answer: What goes in? What comes out? How much power does it need? Why has it stopped? Keep basic operation on the first screen. Put side routing, redstone rules and upgrades in consistent secondary panels. Show recipes, missing ingredients and costs before starting. Tooltips must describe actions in player language. Keyboard navigation, readable contrast, small GUI scales and localization are release requirements.

The prototype uses a common machine screen and vanilla storage screens. A unified searchable storage UI, side configuration UI, guidebook and control dashboard are still planned.

## Main requested systems

**Nexus storage:** Start with connected storage and terminals. Add searchable unified inventory, disks, bulk vaults, import/export buses, pattern autocrafting, machine scheduling, stock targets and wireless access. Simple networking should work without channel arithmetic; an expert mode can make bandwidth and routing into puzzles. Crafting failures must point to their cause, such as missing tin or an unpowered furnace.

**Industrial processing:** Start with raw ore → two dust → smelting. Add an alloy furnace, metal press, washer, separator, electrolyzer, chemical reactor, crystallizer and multi-recipe factories. Later ore multiplication should reward infrastructure, with clear yields and reusable byproducts. Do not create new copies of every vanilla metal.

**Survey Miner:** Preserve the old digital miner's selective excavation, radius/height settings, filters, replacement and silk-touch choices. The prototype has a bounded region below the miner, one inventory filter, pause/rescan, owner checks and permission callbacks. Later UI should preview matching blocks and total energy, offer named presets and explain protection failures. Chunk loading must be opt-in, quota-limited and compatible with server policy.

**Architect utilities:** A preview-first builder for rooms, tunnels, spheres and roads; saved blueprints; named teleport pads; factory monitors; and readable automation rules. Start with presets before introducing a full logic editor. A build preview should show exact material and energy requirements and make its operation cancellable.

**Ascendant / Chaos:** Original fusion assembly, a visible multiblock energy reservoir, modular shields/flight/mining equipment and repeatable boss encounters. Chaos should unlock new capabilities, not only multiply damage. Draconic and chaotic cores in the prototype are creative inventory concepts, without survival recipes or powers.

## More content to consider

| Proposal | Why it belongs |
| --- | --- |
| Compact item/fluid/power conduits | Dense, tidy factories with inspectable routes |
| Parallel factory upgrades | Expand throughput while retaining configuration |
| Alloy furnace and forming press | Make useful conductors, plates and structural alloys |
| Brine wells and salt flats | Give chemistry its own exploration loop |
| Sulfur vents and geothermal taps | Turn location into a power/processing choice |
| Resin trees and rubber processing | Link farming to insulation and flexible components |
| Oil seeps and fractionation | Fuels, polymers and reusable chemical byproducts |
| Bauxite deposits and alumina refining | Lightweight machinery through a distinctive processing chain |
| Biomass digesters | A reason to reuse farm waste |
| Hydroponics and tree farms | Renewable food, wood and industrial crops |
| Livestock collection and controlled spawning | An automation branch beyond mining |
| Resource cultivators | Optional skyblock-friendly deterministic materials |
| Recycling and salvage | Recover value from obsolete equipment |
| Prospecting scanner | Explore resource regions before automated mining |
| Blueprint library | Reuse favorite factory layouts with a material bill |
| Building wand and exchange tool | Reduce repetitive base construction |
| Elevators, travel anchors and teleport pads | Make large bases pleasant to navigate |
| Named wireless power links | Connect remote outposts with visible throughput limits |
| Multiblock turbines and reactors | Give large builds functional purpose |
| Energy reservoir with projected runtime | A visible base centerpiece and useful diagnostic |
| Modular tools and equipment | Choose flight, shields, magnetism and area mining |
| Keep-in-stock crafting | Maintain reserves with one understandable rule |
| Factory dashboard | One place for power trends, blocked machines and tasks |
| Factory black box | Explain what caused a production line to stop |
| Network route overlay | Highlight disconnected or overloaded components |
| Production contracts | Optional goals for sustained output and efficiency |
| Closed-loop challenges | Reward reuse and self-sustaining factories |
| Ancient industrial ruins | Find blueprints, damaged machinery and salvage |
| Rift stabilizer | Control anomalies to obtain exotic resources |
| Modular pocket workshops | Compact automation spaces with explicit performance limits |
| Custom industrial dimensions | Much later: curated resource worlds with clear costs |
| Classic and Expert profiles | Open-ended building or more demanding production chains |
| Creative-item milestones | Optional long-term goals after the main progression |
| Shared guide and quest chapters | Teach systems through usable mini-factories |
| Computer control API | Allow advanced players to script diagnostics and requests |
| Cosmetic factory kit | Matching catwalks, windows, lamps, tanks and control panels |

## Why these proposals fit the era

[Enigmatica 2 Expert](https://www.curseforge.com/minecraft/modpacks/enigmatica2expert) explicitly combined AE2, Mekanism, Thermal Expansion, Ender IO, Immersive Engineering and NuclearCraft, with extensive quests, technology branches and creative-item goals. That supports a coherent progression with several valid routes through it.

[SkyFactory 4](https://www.curseforge.com/minecraft/modpacks/skyfactory-4) foregrounded automation, chosen resource production and optional progression. [StoneBlock](https://www.curseforge.com/minecraft/modpacks/stoneblock) offered an underground sandbox with guiding quests. [FTB Revelation](https://www.curseforge.com/minecraft/modpacks/ftb-revelation) is a useful reference for open-ended kitchen-sink play. These are examples of pack design, not a measured survey or a historical download ranking. Present-day lifetime downloads cannot establish what was most downloaded in 2018.

Mechanical reference points: [AE2's archived introduction](https://appliedenergistics.org/ae2-site-archive/Getting-Started/), [Mekanism Digital Miner](https://wiki.aidancbrady.com/wiki/Digital_Miner), [Mekanism processing](https://wiki.aidancbrady.com/wiki/Ore_Processing), [Thermal Expansion's 1.12 machines](https://teamcofh.com/docs/1.12/thermal-expansion/machines/), [RFTools](https://www.curseforge.com/minecraft/mc-mods/rftools), [Industrial Foregoing](https://www.curseforge.com/minecraft/mc-mods/industrial-foregoing), and [Draconic Evolution](https://www.curseforge.com/minecraft/mc-mods/draconic-evolution). The proposed black box, contracts and rift systems are original design directions, not claims about those mods.
