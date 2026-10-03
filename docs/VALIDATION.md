# Verification record — 0.1.0-alpha.4

Checks run on 2026-10-02 and 2026-10-03 with Minecraft 1.21.1 and Java 21, on the development PC (Windows). What was run on a headless server, what was only read, and what was not done are listed separately. The alpha.3 record is kept below.

| Check | Result |
| --- | --- |
| Shared energy unit tests | 2 passed |
| Resource structure | `node tools/verify-resources.mjs` passed: 485 shared JSON files, 40 blocks, 77 item models, 134 recipes, 25 advancements, 205 translations, 105 textures, 18 machine types |
| Generator output | Regenerating gives the committed files (text output compared; an independent copy was byte-identical for all 600 files) |
| Fabric / NeoForge / Forge build | `packageRelease` passed on 2026-10-03; three jars collected in `releases/0.1.0-alpha.4/` |
| NeoForge server GameTests | All 59 required tests passed |
| Fabric server GameTests | All 4 required tests passed. First time the mod has been started on Fabric |
| Forge server GameTests | All 4 required tests passed. First time the mod has been started on Forge |
| One release command | `packageRelease` plus the three test runs in a single Gradle invocation, as the release workflow does it: passed, exactly three pass summaries |
| Recipe / advancement loading | 1,425 recipes and 1,425 advancements loaded on each loader's test server without a Technologia decoding error |
| Jar contents | No test class, test structure or test mod descriptor in any jar; licence files and `technologia/tiers.json` present; loader metadata placeholders expanded |
| Earlier releases | The nine alpha.1 to alpha.3 jars in `releases/` have unchanged SHA-256 digests |
| Code review | Five areas reviewed, each finding checked by a second reader instructed to refute it; see below |
| NeoForge client start | Development client started to the title screen on 2026-10-03. The resource reload included Technologia and the log shows no missing model or texture warning for it. No world was opened |
| Client playtest of alpha.4 features | Not performed |
| Fabric / Forge client start | Not performed |
| Other mods, multiplayer, performance | Not performed |
| GitHub verification | [Three-loader build and server tests](https://github.com/Thespiki/Technologia/actions/runs/37103671596) passed on Linux for release source `e4c7bbf` before it was merged; the [release build and server tests](https://github.com/Thespiki/Technologia/actions/runs/37106437355) passed for the tag |
| Publication | [Alpha.4 prerelease](https://github.com/Thespiki/Technologia/releases/tag/v0.1.0-alpha.4) published with exactly three jars. The three public jars were downloaded; their SHA-256 digests equal those of the jars built on the development PC |
| Pages | [Deployment passed](https://github.com/Thespiki/Technologia/actions/runs/37106106567); the live page shows alpha.4 |
| Earlier releases online | The alpha.1, alpha.2 and alpha.3 releases are still listed; their tags point at the same commits as before |

SHA-256 of the alpha.4 jars. The release workflow builds the jars again from the tag on Linux; the result is byte-identical to the Windows build, so these digests apply to the published files:

| Jar | SHA-256 |
| --- | --- |
| `technologia-fabric-1.21.1-0.1.0-alpha.4.jar` | `30cd6414267c77456548dda1c88e43733ab7b703115b40a72b9d02b9cb2f6037` |
| `technologia-forge-1.21.1-0.1.0-alpha.4.jar` | `536b72fdb8314b978e023e43c5de88f0dd40ded6bd4d8a39928d7099f6696694` |
| `technologia-neoforge-1.21.1-0.1.0-alpha.4.jar` | `f736a888ea46b4d61cdf6f5b7b8cdcdf7af8a23b98bba20f0ca27058491f18f9` |

## What the server tests cover

The NeoForge suite has 59 tests in four groups: workshop basics (8), factory processing, energy, transfer and storage (20), menus and the guide (10), and the alpha.4 additions (21). The alpha.4 group checks:

- tier data and kit recipes load; a kit upgrades in place, in order, keeping inventory and energy; storage cores have no tier;
- lanes work in parallel and share results; an idle lane receives whole operations only (27 ingots split 18 and 9; alloy ingredients move together);
- machines refuse unusable items and move unusable inputs to the results; slots hidden at the current tier are safe to address;
- broken and dismantled machines keep tier and energy; the wrench dismantles into the inventory, and in creative mode with a full inventory the item drops instead of vanishing;
- an alpha.3 save converts without losing or creating items;
- conduit materials limit a run; two runs on one supplier keep their own limit; a directly adjacent machine is limited only by the supplier; the limit is per tick;
- a generator does not burn fuel into a full buffer; Resonance Blooms speed up a processor; the Auto Sieve works; only the owner uses a miner's screen;
- an Item Transfer on the side of a furnace takes results and leaves fuel, and fills a chiseled bookshelf one book per slot;
- crafting an Advanced Energy Cell from upgraded cells keeps the lowest tier and the stored energy.

The Fabric and Forge tests are smoke tests: content, tier data and world generation load; a generator powers a crusher through a conduit; the loader's energy and item adapters follow the machine rules; a dropped machine keeps its tier and the loader's block-break protection hook answers. They do not repeat the full suite.

## Review findings

Confirmed and fixed before release: lane balancing left remainders that no lane could use; a command addressing a hidden slot failed; two conduit runs on one supplier shared the weaker rating; the Item Transfer asked a target about the whole source stack instead of the amount moved, and pulled fuel from the side of a vanilla furnace; the wrench deleted the machine item in creative mode with a full inventory; crafting a machine from upgraded machines discarded their tier and energy; the bloom scan was narrower vertically than the guide said; the guide's Progress page showed a misleading empty state and cut long descriptions; the guide claimed a datapack could change tier statistics; the docs and guide overstated the miner's owner check (it covers the miner's screen, not hoppers or block breaking); the release workflow did not compare the tagged resources with the generator.

Checked and not changed: a route built while a neighbouring chunk loads (the reported case cannot occur; a conduit run crossing two or more chunk borders may take up to five seconds to appear after a far chunk loads); pick-block returns the machine with its tier, as a shulker box does, so in survival it only selects an inventory item with the same data.

## Limits of this record

- Sounds, particles, tooltips, the 280×238 machine screen, the Progress page layout and every model were checked by reading code and generated files and by the resource validator. No alpha.4 feature has been looked at in a running client beyond the start described in the table.
- The Fabric and Forge jars have been run only as headless test servers in the development environment, not as installed jars and not with a client.
- Akashic Tome: its source for NeoForge 1.21.1 accepts item ids containing "guide" by default, so the Field Guide should be accepted. This was read, not tested in game. No Fabric or Forge 1.21.1 build of that mod was found.
- No other mod's cables, pipes, claims or recipe viewer was tested. No multiplayer or performance test was run.
- The conversion of an alpha.3 world is covered by a server test on saved machine data, not by loading a real alpha.3 world.

See PLAYTEST.md for the client checklist that remains.

## Alpha.3 record (2026-09-30 and 2026-10-01)

Checks run on 2026-09-30 and 2026-10-01 with Minecraft 1.21.1 and Java 21. Build coverage, server behavior and client review are recorded separately.

| Check | Result |
| --- | --- |
| Shared energy unit tests | 2 passed, including 20,000 simulation/commit cycles and integer boundaries |
| Resource structure | 415 shared JSON files passed translation, texture/model, tag and loader metadata checks |
| Fabric / NeoForge / Forge | Final packageRelease passed on 2026-10-01; all three installable jars collected |
| NeoForge server GameTests | All 38 required tests passed locally |
| Recipe / advancement loading | 1,404 recipes and 1,420 server advancements loaded without a Technologia decoding error |
| Machine shapes | Cached collision and selection profiles generated from original model elements; open press and solar profile tested |
| Client review | NeoForge: new models, alloy UI and processing, conduit charge, transfer destination, multi-core capacity and wrench rotation checked |
| Presentation site | Static build, JavaScript syntax, local HTML and model screenshot HTTP 200 passed; fresh browser preview could not attach |
| GitHub verification | [Three-loader matrix](https://github.com/Thespiki/Technologia/actions/runs/36843197515) and [release build/server tests](https://github.com/Thespiki/Technologia/actions/runs/36843273215) passed for release source `c547a8c` |
| Publication | [Alpha.3 prerelease](https://github.com/Thespiki/Technologia/releases/tag/v0.1.0-alpha.3) published with exactly three jars; all public SHA256 digests verified |
| Pages | [Deployment passed](https://github.com/Thespiki/Technologia/actions/runs/36843197593); live alpha.3 HTML and model screenshot returned HTTP 200 |
| Fabric / Forge client playtests | Not performed |
| External mods / multiplayer stress | Not performed |

The 38 server tests include the previous 18 generator, processing, mining, energy, storage, menu and guide checks, plus 20 checks for counted/swapped alloy inputs, insufficient quantity, blocked outputs and byproducts, process save/load and input changes, new processors, solar obstruction and advanced cell capacity, biomass, conduit loops and energy conservation, cell feedback prevention, filtered/full/sided item transfer, filter persistence, actual direction/redstone behavior, multi-core aggregation and topology changes, slot 215 and padded storage safety, five-core rejection, machine menu mapping, loaded advancements and physical model profiles.

The first expanded run caught solar generation immediately after a roof was placed, before Minecraft's skylight update had caught up. Generation now checks the synchronous obstruction heightmap as well as sky light. The regression passes. Builds and CI require an explicit `All N required tests passed` summary, not just a zero Gradle exit code.

New processing recipes use datapack-defined costs. The furnace still reads vanilla smelting recipes and config costs. External energy adapters and sided inventory surfaces are implemented, but this does not establish compatibility with an untested cable, claim mod, recipe viewer or modpack.

Current limits: four cores/216 slots per terminal; no disks or autocrafting; 128 loaded blocks per storage traversal and 128 conduits per power route; one miner filter; adjacent Item Transfer supports vanilla containers rather than capability-only inventories; no furnace XP; broken machines lose stored energy; no fluids/gases, machine upgrades, moving mechanisms, Create kinetic bridge, advanced equipment or bosses. See PLAYTEST.md for remaining acceptance work.

### Alpha.3 client checks

All public jars were downloaded and their SHA256 digests verified. Every uncompressed entry matched the corresponding local package: 572 Fabric, 575 Forge and 578 NeoForge entries. All six alpha.1/alpha.2 jar hashes and both previous source tags remain unchanged.

The existing NeoForge test world loaded successfully. The two-input Alloy Smelter displayed separate inputs and outputs, a clear progress bar and working Start/Pause controls. A conduit charged its buffer from 40,000 to 80,000 FE. Six copper and two tin produced eight bronze ingots, which Item Transfer moved into the destination chest; the chest contents were checked in-game. A terminal combined two cores containing 32 diamonds each into a count of 64 and correctly displayed 108 backing slots. Wrench use rotated the terminal through its real geometry without opening the menu.

The model gallery showed presses, saw housing, centrifuge, alloy smelter, solar panels, cells, miner, storage and terminal geometry with no missing textures observed. The screenshot at `site/media/factory-alpha3.png` is an unedited Minecraft capture. These are targeted singleplayer checks, not a full GUI-scale, multiplayer or performance certification. The development PC logged startup/reload lag; no factory throughput benchmark has been established.

A normal `/reload` completed and loaded processing recipes and advancements without a Technologia decoding error. A mid-cycle recipe edit and every guide navigation path still need dedicated client testing.

### Earlier playtest evidence

For alpha.2, the user reported working core/terminal synchronization through cable or direct contact, energy-cell transfer, generator, furnace, crusher and miner filters. Screenshots led to corrections for overlapping output progress and empty-storage text. Initial NeoForge client checks covered the native guide at small/maximized sizes, chapter/next navigation, storage aggregation of 1,000 coal, empty search, and separated furnace progress. These are historical alpha.2 observations, not a substitute for testing alpha.3 changes.

Alpha.2 remains available at https://github.com/Thespiki/Technologia/releases/tag/v0.1.0-alpha.2, with its earlier validation recorded in that source tag. Alpha.1 remains preserved as well.
