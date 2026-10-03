# Delivery plan and quality gates

## Current slice

Alpha.4 adds machine tiers to the workshop. There are 18 machine types: nine processors, four generators, two cells, a miner, a storage core and a terminal. Every powered machine has one of eight tiers, Mk I to Mk VIII, defined as data in `technologia/tiers.json`. Seven tier kits upgrade a placed machine in place. Processors get 1, 1, 2, 3, 4, 5, 6 and 8 parallel lanes by tier, on a fixed layout of 16 ingredient slots and 27 result slots. Machines refuse items they cannot process and move an unusable item already inside to the result slots. The wrench dismantles a machine while keeping its tier and stored energy. Energy conduits come in copper (200 FE/t), gold (1,000 FE/t) and resonite (5,000 FE/t). The new Auto Sieve and five ore fragments feed the crusher. Resonance Blooms add 8% speed each to a nearby processing machine, up to five. Working processors and fuel generators play a working sound and emit particles. The native Field Guide has 38 articles in ten chapters and a Progress page; 25 vanilla advancements record workshop milestones. Content totals: 40 blocks and 37 standalone items.

Gameplay remains shared across three loader adapters; the Fabric, NeoForge and Forge jars are built from the same shared code. Automated server tests run on all three loaders on headless servers: the full GameTest suite on NeoForge, and loader smoke tests on Fabric and Forge. Their results for alpha.4 are recorded in [VALIDATION](VALIDATION.md). See [README](../README.md) for implemented behavior. Building a loader jar and passing headless server tests are separate from testing its client, dedicated server and modpack behavior.

The requested target is **Minecraft 1.21.1 and newer** on **Fabric, NeoForge and Forge**. Development and playtesting happen on one version first: Minecraft 1.21.1, with NeoForge as the first in-game test platform. The loader adapters are prepared early to keep gameplay portable. The cadence for later Minecraft versions and the snapshot policy are not decided yet.

## Next milestones

1. **Harden the workshop:** client and dedicated-server tests on all three loaders; persistence/reload, multiplayer, recipe reload and automation edge cases; guide and screen accessibility, a guide hotkey, and recipe-viewer entry points (JEI first; EMI and REI as candidates to evaluate).
2. **Nexus and logistics:** storage disks, network-aware import/export and autocrafting; server racks with power supplies, overload behavior and rack cooling; the channel system with data cables whose material sets throughput; fluid conduits, per-face setup, richer filter rules, owner/team permissions and a live network inspector. The current terminal limit is four cores/216 slots; Item Transfer handles adjacent vanilla inventories rather than network-wide stock requests.
3. **Production:** fluid tanks, washing/chemistry and gases. Extend the existing counted-ingredient/byproduct recipe system and show dependencies and unavailable steps in the UI. Measure the bronze/steel workshop route and the eight machine tiers in survival before adding more.
4. **Architect and control:** builder previews, blueprint material planning, named travel, dashboard, stock rules and machine configuration copying.
5. **Ascendant:** modular tools, shields, flight, fusion assembly, reactor engineering and multiblock reservoir. Survival progression must have uses at each tier.
6. **Chaos:** original encounters, rifts, exotic fabrication and optional creative-item objectives. The exact reference for the Chaotic Evolution inspiration is to be confirmed first. Add content only after server load and encounter readability are tested.

Delivered in alpha.4 and no longer listed above: factory upgrades (tier kits), parallel processing (lanes), wrench dismantling, input refusal, conduit materials, the sieve line, Resonance Blooms, machine sounds and particles, the Progress page, and a format marker in machine save data.

Planned and not started: rack cooling (needs server racks and network power), mob essence (needs mob farms), the orbital solar relay (needs space and wireless power) and data-cable materials (needs the channel system). They are delivered with those systems, which are not built either. A French translation is not planned for now; English is the only language. An ender chest giving network access and a server-room spatial pocket are not planned. The scope record behind these lines is in [REQUIREMENTS.md](REQUIREMENTS.md).

## Compatibility contracts

| System | Foundation | Next verification/work |
| --- | --- | --- |
| Energy | Forge/NeoForge FE and Fabric Team Reborn Energy adapters. A supplier sends 200 FE/t at Mk I multiplied by its tier's transfer value; the limit is per tick across direct neighbors, conduit routes and other mods' cables. Conduits carry 200 (copper), 1,000 (gold) or 5,000 (resonite) FE/t per supplier; the weakest conduit on a network limits that network; a block directly beside a supplier is not limited by conduits. Routes cover at most 128 conduits, are cached, and are rebuilt when blocks change or after 100 ticks | Test external endpoints, push/pull directions, simulation, nested transaction rollback and representative cable mods; conduits have no external input buffer |
| Units | One internal unit maps to one FE or one Fabric E | Balance remains a gameplay choice; no hidden voltage conversion |
| Inventory | Vanilla sided containers; Forge/NeoForge item capabilities; filtered adjacent Item Transfers. Machines expose the ingredient slots their tier uses and the 27 result slots. Insertion is refused for items the machine cannot process; extraction takes results only; the miner's filter cannot be set by automation | Add adapters for capability-only transfer endpoints; exercise external pipes and Create funnels, component filters and full targets |
| Storage | Up to four cores/216 slots per terminal, bounded to 128 loaded network blocks; stale actions and topology changes invalidate access. The network walk of an open terminal is repeated only after a block change or once a second | Multiplayer contention, chunk-boundary changes, storage disks and network stock management |
| Materials | Shared `c:` tags and legacy `forge:` exports | Check tag conventions with actual pack dependencies |
| Recipes | Vanilla crafting/smelting plus `technologia:processing` for crusher, alloys, plates, sawmill, compactor, centrifuge, recycler and sieve; counted ingredients, optional byproducts and an optional byproduct chance | Recipe-viewer entry points (JEI first; EMI and REI as candidates to evaluate), overlapping recipe rules, pack reload tests and progression balancing |
| Create | Common inventory/material surfaces are provided | Optional crushing/mixing/pressing recipes and a dedicated kinetic bridge; no direct Create integration is claimed yet |
| Akashic Tome | The Field Guide's item id (`technologia:field_guide`) contains "guide", so the NeoForge 1.21.1 build of Akashic Tome should accept it with its default settings. This was read from that mod's source and has not been tested in game | Test in game on NeoForge. No Forge or Fabric build of Akashic Tome for 1.21.1 was found |
| Mining protection | Owner-online restriction and loader break callbacks; no forced chunks. Wrench dismantling uses the same break callback | Verify each claim mod; add dedicated integration where callback coverage is insufficient |
| Pack management | Restart-required server balance config with bounded values, defaults for missing keys, invalid-file fallback and a `machineSounds` switch; operator status command; processing datapacks; tier data in `technologia/tiers.json` inside the jar | Config UI, per-world profiles, network diagnostics and a way to change tiers without editing the jar |
| Save data | Machine data carries a format marker since alpha.4. Alpha.3 machine data has no marker and is migrated on load: results move to the new result slots. Registry ids are unchanged | Versioned fixtures for each released layout and a written migration policy |
| Scripting | Datapacks can change standard and custom processing recipes and tags | Explicit KubeJS/CraftTweaker hooks only after recipe/API stabilization |

Create's kinetic stress/rotation is not FE. A generator/motor bridge needs its own speed, stress, efficiency and feedback-loop design. Availability of a Create version or addon on one loader does not imply availability on the other two. Pin and test actual versions before advertising support.

The baseline versions are Minecraft 1.21.1, Java 21, Fabric Loader 0.16.9/API 0.109.0, Forge 52.0.28 and NeoForge 21.1.80. Coverage of newer Minecraft versions is part of the requested target, not a claim of existing binary support. Each port needs a separate compatibility matrix and tested release. How older lines are maintained and how minor releases are grouped is not decided yet. When a requested loader does not exist for a release, record that gap explicitly rather than implying compatibility.

## Release requirements

- No item duplication or disappearance on full output, rapid shift-click, block break, reconnect, chunk unload or server restart.
- Menu actions validate container ownership/distance and execute on the server. No client-authoritative energy, recipes or mining.
- Both simulated FE calls and aborted/nested Fabric transactions preserve conservation of energy.
- Mining respects loaded chunks, world borders, spawn/claim protection and configurable tick budgets. Test with actual protection mods; no blanket compatibility promise.
- Dedicated servers never initialize client screens or rendering classes.
- Resource definitions resolve; recipes are obtainable; ore distribution and progression are measured in survival.
- Four-player concurrent usage and a large factory workload are profiled before balance/performance claims.
- Load without every optional integration. Then test each integration separately and in representative packs.
- Interface review at supported GUI scales: readable status, usable keyboard focus, meaningful tooltips, no color-only information.
- Save migrations have versioned fixtures before released data layouts change. Alpha.4 changed the machine slot layout and added the format marker. A server test checks the migration with constructed alpha.3-layout data; a fixture saved by a real alpha.3 world is still to be added.

Prefer many small, complete and tested milestones. The desired final mod is large; an alpha must not describe planned content as already playable.

## Visual development

The current models establish one industrial language: dark steel, copper conductors, recessed panels, readable status lights and visible machinery. Presses, sawmills, sieves, generators, cells, racks and terminals have different silhouettes; each of the 18 machine types has its own model. Machine and decoration collision/selection shapes are generated from their model geometry. The ten-block factory building kit carries the same materials into the surrounding base. In alpha.4 working processors and fuel generators also emit particles and play a working sound, and gold and resonite conduits have their own textures.

Next visual work includes moving mechanisms, a visible difference between machine tiers, more informative ports, visual multiblock assembly guides, richer machine dashboards and accessibility checks at multiple GUI scales. Visible sockets are not yet configurable side modes, and decorative control panels do not operate other machines. Palette colors describe functions; they do not limit the number of progression tiers.
