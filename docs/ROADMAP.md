# Delivery plan and quality gates

## Current slice

Alpha.5 widens the workshop. There are 31 machine types: twelve processors, seven generators, two cells, a Creative Energy Source, a wireless sender and receiver, three area machines (Auto Harvester, Growth Accelerator, Vacuum Collector), a Chunk Loader, a miner, a storage core and a terminal. Every powered machine except the wireless blocks and the creative source has one of eight tiers, Mk I to Mk VIII, defined as data in `technologia/tiers.json`, and above Mk I shows it as a band on its front. Seven tier kits upgrade a placed machine in place. Processors get 1, 1, 2, 3, 4, 5, 6 and 8 parallel lanes by tier, on a fixed layout of 16 ingredient slots and 27 result slots.

New in alpha.5: the Water Wheel, Windmill and Thermoelectric Generator, which use no fuel; wireless power on 256 public channels; a fourth conduit at 32,000 FE/t; the Enrichment Chamber and Metallurgic Infuser with their enriched materials and alloys; silver, nickel, electrum, invar and constantan; the Phyto Chamber, Auto Harvester, Growth Accelerator and Bonsai Pot; three sieve meshes, the Hand Sieve and compressed blocks; the Vacuum Collector, Chunk Loader, four crates and Vector Plates; three magnets and the Travel Staff; seven more building blocks; and, on every machine, redstone control, comparator output, result ejection and the tier band. The native Field Guide has 53 articles in twelve chapters and a Progress page; 45 vanilla advancements record workshop milestones. Content totals: 76 blocks and 68 standalone items. The numbers and recipes are in the [alpha.5 release notes](releases/0.1.0-alpha.5.md).

Gameplay remains shared across three loader adapters; the Fabric, NeoForge and Forge jars are built from the same shared code. Automated server tests run on all three loaders on headless servers: the full GameTest suite on NeoForge, and loader smoke tests on Fabric and Forge. Their results are recorded in [VALIDATION](VALIDATION.md). See [README](../README.md) for implemented behavior. Building a loader jar and passing headless server tests are separate from testing its client, dedicated server and modpack behavior. For Fabric, the owner reported a short client launch of alpha.4 that seemed to work; real play testing on Fabric and Forge is still to do.

The requested target is **Minecraft 1.21.1 and newer** on **Fabric, NeoForge and Forge**. Development and playtesting happen on one version first: Minecraft 1.21.1, with NeoForge as the first in-game test platform. The loader adapters are prepared early to keep gameplay portable. The cadence for later Minecraft versions and the snapshot policy are not decided yet.

## Next slice: alpha.6, server racks

Decided by the owner on 2026-10-03 (recorded in [REQUIREMENTS.md](REQUIREMENTS.md)). Nothing of it is built yet.

- A server rack is one block: a cabinet with bays into which modules are inserted (drives, storage manager, power supply, fan, security). Its front shows the installed modules. Several cabinets side by side form the server room.
- When the network asks for too much power there is a visible warning, and then a breaker shuts the network down cleanly. Nothing is destroyed and there is no explosion. The breaker must be re-armed.
- Racks replace Storage Cores. Existing cores keep working in old worlds but are no longer craftable. Players must not lose items.

How the modules are crafted, what each one stores or costs, how the warning looks and how the breaker is re-armed are not decided; they are implementation work for alpha.6.

## Later milestones

1. **Harden the workshop:** client and dedicated-server tests on all three loaders; persistence/reload, multiplayer, recipe reload and automation edge cases; guide and screen accessibility, a guide hotkey, and recipe-viewer entry points (JEI first; EMI and REI as candidates to evaluate).
2. **Nexus and logistics, after the racks:** network-aware import/export and autocrafting; rack cooling; the channel system with data cables whose material sets throughput; fluid conduits, per-face setup, richer filter rules, owner/team permissions and a live network inspector. The current terminal limit is four cores/216 slots; Item Transfer handles adjacent vanilla inventories rather than network-wide stock requests.
3. **Production:** fluid tanks, washing/chemistry and gases. Extend the existing counted-ingredient/byproduct recipe system and show dependencies and unavailable steps in the UI. Measure the workshop route, the new alloys and the eight machine tiers in survival before adding more.
4. **Architect and control:** builder previews, blueprint material planning, named travel, dashboard, stock rules and machine configuration copying.
5. **Ascendant:** the multitool and modular tools, armor, shields, flight, Curios accessories, fusion assembly, reactor engineering and a multiblock reservoir. Survival progression must have uses at each tier.
6. **Chaos:** original encounters, rifts, exotic fabrication and optional creative-item objectives. The exact reference for the Chaotic Evolution inspiration is to be confirmed first. Add content only after server load and encounter readability are tested.

Delivered in alpha.5 and no longer listed above: water, wind and thermoelectric power; wireless power; the superconducting conduit; enrichment and infusion; silver, nickel and three alloys; the Phyto Chamber, Auto Harvester, Growth Accelerator and Bonsai Pot; sieve meshes, the Hand Sieve and compressed blocks; the Vacuum Collector, Chunk Loader, crates and Vector Plates; magnets and the Travel Staff; seven building blocks; redstone control, comparator output, result ejection and the tier band.

Delivered in alpha.4: factory upgrades (tier kits), parallel processing (lanes), wrench dismantling, input refusal, conduit materials, the sieve line, Resonance Blooms, machine sounds and particles, the Progress page, and a format marker in machine save data.

## Still not built

None of the following is started. Each is planned unless it says otherwise.

- Server racks, modules, network power supplies and the breaker (alpha.6, see above).
- Fluids and gases, with their tanks, pipes and machines.
- Autocrafting.
- Storage drives.
- The modular multitool, its tool station, the hammer and builder's-wand addons and the multitool sieve mode.
- Armor, shields and force fields; Curios accessories.
- Reactors, turbines, diesel and gas generators, fusion.
- Dimensions and the resource dimension; space.
- Mob farms and mob-grinding utilities.
- Linked (dye-coded) chests and tanks.
- Crops, food and Cooking for Blockheads integration; resource crops and crop tiers.
- The channel system, data cables, subnetworks, P2P routing, long-range links and spatial workshops.
- Refinery and petroleum, pneumatics, and the remaining Mekanism-, Thermal- and Industrial-style machines.
- Portable energy storage and multiblock power storage.
- Per-face machine configuration, a guide hotkey, recipe-viewer entry points, Create and Storage Drawers integration, a control dashboard and a config UI.
- Multiblocks, moving machine animations, bosses and the draconic/chaotic endgame.
- A French translation. It was declined for now on 2026-10-02 and waits for the owner's request; English is the only language.
- Planned together with the systems they depend on: rack cooling (needs server racks and network power), mob essence (needs mob farms), the orbital solar relay (needs space; wireless power exists since alpha.5) and data-cable materials (needs the channel system).

An ender chest giving network access and a server-room spatial pocket are not planned. The scope record behind these lines is in [REQUIREMENTS.md](REQUIREMENTS.md).

## Compatibility contracts

| System | Foundation | Next verification/work |
| --- | --- | --- |
| Energy | Forge/NeoForge FE and Fabric Team Reborn Energy adapters. A supplier sends 200 FE/t at Mk I multiplied by its tier's transfer value; the limit is per tick across direct neighbors, conduit routes and other mods' cables. Wireless blocks move up to 50,000 FE/t and the Creative Energy Source up to 1,000,000 FE/t, whatever the tier. Conduits carry 200 (copper), 1,000 (gold), 5,000 (resonite) or 32,000 (superconducting) FE/t per supplier; the weakest conduit on a network limits that network; a block directly beside a supplier is not limited by conduits. Generators, the creative source and wireless receivers charge cells; cells do not charge each other; a wireless receiver accepts no cable energy. Routes cover at most 128 conduits, are cached, and are rebuilt when blocks change or after 100 ticks | Test external endpoints, push/pull directions, simulation, nested transaction rollback and representative cable mods; conduits have no external input buffer |
| Units | One internal unit maps to one FE or one Fabric E | Balance remains a gameplay choice; no hidden voltage conversion |
| Inventory | Vanilla sided containers; Forge/NeoForge item capabilities for machines, crates and the Bonsai Pot; filtered adjacent Item Transfers. Machines expose the ingredient slots their tier uses and the 27 result slots. Insertion is refused for items the machine cannot process; extraction takes results only; the miner's filter cannot be set by automation. The Eject button pushes results into the vanilla-style inventory behind the machine | Add adapters for capability-only transfer endpoints; exercise external pipes and Create funnels, component filters and full targets |
| Storage | Up to four cores/216 slots per terminal, bounded to 128 loaded network blocks; stale actions and topology changes invalidate access. The network walk of an open terminal is repeated only after a block change or once a second | Multiplayer contention, chunk-boundary changes, and the move from Storage Cores to racks in alpha.6 without loss of items |
| Materials | Shared `c:` tags and legacy `forge:` exports, including silver, nickel, electrum, invar and constantan since alpha.5 | Check tag conventions with actual pack dependencies |
| Recipes | Vanilla crafting/smelting plus `technologia:processing` for crusher, alloys, plates, sawmill, compactor, centrifuge, recycler, sieve, enrichment, infusion and growing; counted ingredients, optional byproducts, an optional byproduct chance and, for sieve recipes, an optional mesh level | Recipe-viewer entry points (JEI first; EMI and REI as candidates to evaluate), overlapping recipe rules, pack reload tests and progression balancing |
| Create | Common inventory/material surfaces are provided | Optional crushing/mixing/pressing recipes and a dedicated kinetic bridge; no direct Create integration is claimed yet |
| Akashic Tome | The Field Guide's item id (`technologia:field_guide`) contains "guide", so the NeoForge 1.21.1 build of Akashic Tome should accept it with its default settings. This was read from that mod's source and has not been tested in game | Test in game on NeoForge. No Forge or Fabric build of Akashic Tome for 1.21.1 was found |
| Mining and harvesting protection | The Survey Miner and the Auto Harvester need their owner online in the same dimension and ask the loader's break callback; neither loads chunks. Wrench dismantling uses the same break callback | Verify each claim mod; add dedicated integration where callback coverage is insufficient |
| Chunk loading | The Chunk Loader uses the game's forced chunks while it has power and is switched on. It releases only the chunks it forced itself; a chunk already forced by a command or another mod is left alone. `chunkLoading` and `chunkLoaderRadius` switch it off or limit its area | Per-player budgets, ownership and visibility; behaviour next to other chunk-loading mods; server load |
| Wireless power | Public numbered channels, 0 to 255; a sender reaches loaded receivers in any dimension; nothing about the grid is saved, it rebuilds as receivers tick | Private or named networks, a view of who uses a channel, multiplayer abuse cases |
| Pack management | Restart-required server balance config with bounded values, defaults for missing keys, invalid-file fallback, a `machineSounds` switch and the two chunk-loading keys; operator status command; processing datapacks; tier data in `technologia/tiers.json` inside the jar | Config UI, per-world profiles, network diagnostics and a way to change tiers without editing the jar |
| Save data | Machine data carries a format marker since alpha.4, and alpha.5 did not change the format. Alpha.3 machine data has no marker and is migrated on load: results move to the new result slots. Registry ids are unchanged. The tier band is a new blockstate value that a machine sets on its first tick | Versioned fixtures for each released layout and a written migration policy, before alpha.6 retires the Storage Core recipe |
| Scripting | Datapacks can change standard and custom processing recipes and tags | Explicit KubeJS/CraftTweaker hooks only after recipe/API stabilization |

Create's kinetic stress/rotation is not FE. A generator/motor bridge needs its own speed, stress, efficiency and feedback-loop design. Availability of a Create version or addon on one loader does not imply availability on the other two. Pin and test actual versions before advertising support.

The baseline versions are Minecraft 1.21.1, Java 21, Fabric Loader 0.16.9/API 0.109.0, Forge 52.0.28 and NeoForge 21.1.80. Coverage of newer Minecraft versions is part of the requested target, not a claim of existing binary support. Each port needs a separate compatibility matrix and tested release. How older lines are maintained and how minor releases are grouped is not decided yet. When a requested loader does not exist for a release, record that gap explicitly rather than implying compatibility.

## Release requirements

- No item duplication or disappearance on full output, rapid shift-click, block break, reconnect, chunk unload or server restart.
- Menu actions validate container ownership/distance and execute on the server. No client-authoritative energy, recipes or mining.
- Both simulated FE calls and aborted/nested Fabric transactions preserve conservation of energy.
- Mining and harvesting respect loaded chunks, world borders, spawn/claim protection and configurable tick budgets. Test with actual protection mods; no blanket compatibility promise.
- Dedicated servers never initialize client screens or rendering classes.
- Resource definitions resolve; recipes are obtainable; ore distribution and progression are measured in survival.
- Four-player concurrent usage and a large factory workload are profiled before balance/performance claims.
- Load without every optional integration. Then test each integration separately and in representative packs.
- Interface review at supported GUI scales: readable status, usable keyboard focus, meaningful tooltips, no color-only information.
- Save migrations have versioned fixtures before released data layouts change. Alpha.4 changed the machine slot layout and added the format marker. A server test checks the migration with constructed alpha.3-layout data; a fixture saved by a real alpha.3 world is still to be added. Alpha.5 did not change the layout.

Prefer many small, complete and tested milestones. The desired final mod is large; an alpha must not describe planned content as already playable.

## Visual development

The current models establish one industrial language: dark steel, copper conductors, recessed panels, readable status lights and visible machinery. Each of the 31 machine types has its own model and its own front texture. Machine and decoration collision/selection shapes are generated from their model geometry. The 17-block factory building kit carries the same materials into the surrounding base. Working machines emit particles, except solar generators, and processors, fuel generators and the Water Wheel play a working sound. Since alpha.5 a machine above Mk I carries a tier band at the bottom of its front, in the colour of its tier kit, with one pip per tier number; each of the four conduit materials has its own texture.

Next visual work includes moving mechanisms, more informative ports, visual multiblock assembly guides, richer machine dashboards and accessibility checks at multiple GUI scales. Visible sockets are not yet configurable side modes, and decorative control panels do not operate other machines. Palette colors describe functions; they do not limit the number of progression tiers.
