# Verification record — 0.1.0-alpha.3

Checks run on 2026-09-30 and 2026-10-01 with Minecraft 1.21.1 and Java 21. Build coverage, server behavior and client review are recorded separately.

| Check | Result |
| --- | --- |
| Shared energy unit tests | 2 passed, including 20,000 simulation/commit cycles and integer boundaries |
| Resource structure | 415 shared JSON files passed translation, texture/model, tag and loader metadata checks |
| Fabric / NeoForge / Forge | All three loader builds passed; final release packaging pending |
| NeoForge server GameTests | All 38 required tests passed locally |
| Recipe / advancement loading | 1,404 recipes and 1,420 server advancements loaded without a Technologia decoding error |
| Machine shapes | Cached collision and selection profiles generated from original model elements; open press and solar profile tested |
| Client review | NeoForge: new models, alloy UI and processing, conduit charge, transfer destination, multi-core capacity and wrench rotation checked |
| Presentation site | Static build, JavaScript syntax, local HTML and model screenshot HTTP 200 passed; fresh browser preview could not attach |
| Publication | GitHub CI, release assets and Pages verification pending |
| Fabric / Forge client playtests | Not performed |
| External mods / multiplayer stress | Not performed |

The 38 server tests include the previous 18 generator, processing, mining, energy, storage, menu and guide checks, plus 20 checks for counted/swapped alloy inputs, insufficient quantity, blocked outputs and byproducts, process save/load and input changes, new processors, solar obstruction and advanced cell capacity, biomass, conduit loops and energy conservation, cell feedback prevention, filtered/full/sided item transfer, filter persistence, actual direction/redstone behavior, multi-core aggregation and topology changes, slot 215 and padded storage safety, five-core rejection, machine menu mapping, loaded advancements and physical model profiles.

The first expanded run caught solar generation immediately after a roof was placed, before Minecraft's skylight update had caught up. Generation now checks the synchronous obstruction heightmap as well as sky light. The regression passes. Builds and CI require an explicit `All N required tests passed` summary, not just a zero Gradle exit code.

New processing recipes use datapack-defined costs. The furnace still reads vanilla smelting recipes and config costs. External energy adapters and sided inventory surfaces are implemented, but this does not establish compatibility with an untested cable, claim mod, recipe viewer or modpack.

Current limits: four cores/216 slots per terminal; no disks or autocrafting; 128 loaded blocks per storage traversal and 128 conduits per power route; one miner filter; adjacent Item Transfer supports vanilla containers rather than capability-only inventories; no furnace XP; broken machines lose stored energy; no fluids/gases, machine upgrades, moving mechanisms, Create kinetic bridge, advanced equipment or bosses. See PLAYTEST.md for remaining acceptance work.

## Alpha.3 client checks

The existing NeoForge test world loaded successfully. The two-input Alloy Smelter displayed separate inputs and outputs, a clear progress bar and working Start/Pause controls. A conduit charged its buffer from 40,000 to 80,000 FE. Six copper and two tin produced eight bronze ingots, which Item Transfer moved into the destination chest; the chest contents were checked in-game. A terminal combined two cores containing 32 diamonds each into a count of 64 and correctly displayed 108 backing slots. Wrench use rotated the terminal through its real geometry without opening the menu.

The model gallery showed presses, saw housing, centrifuge, alloy smelter, solar panels, cells, miner, storage and terminal geometry with no missing textures observed. The screenshot at `site/media/factory-alpha3.png` is an unedited Minecraft capture. These are targeted singleplayer checks, not a full GUI-scale, multiplayer or performance certification. The development PC logged startup/reload lag; no factory throughput benchmark has been established.

A normal `/reload` completed and loaded processing recipes and advancements without a Technologia decoding error. A mid-cycle recipe edit and every guide navigation path still need dedicated client testing.

## Earlier playtest evidence

For alpha.2, the user reported working core/terminal synchronization through cable or direct contact, energy-cell transfer, generator, furnace, crusher and miner filters. Screenshots led to corrections for overlapping output progress and empty-storage text. Initial NeoForge client checks covered the native guide at small/maximized sizes, chapter/next navigation, storage aggregation of 1,000 coal, empty search, and separated furnace progress. These are historical alpha.2 observations, not a substitute for testing alpha.3 changes.

Alpha.2 remains available at https://github.com/Thespiki/Technologia/releases/tag/v0.1.0-alpha.2, with its earlier validation recorded in that source tag. Alpha.1 remains preserved as well.
