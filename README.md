# Technologia

An original Minecraft technology mod inspired by the automation and enormous factories of the 1.12.2 era. **Early development alpha**, with a shared codebase and separate Fabric, NeoForge and Forge adapters.

[Explore the presentation](https://thespiki.github.io/Technologia/) · [Requested scope](docs/REQUIREMENTS.md) · [18-milestone progression proposal](docs/PROGRESSION.md)

The first development/playtest target is **Minecraft 1.21.1 + NeoForge**. The requested target is **Minecraft 1.21.1 and newer** on Fabric, NeoForge and Forge. The cadence for later Minecraft versions and the snapshot policy are not decided yet. Newer versions require tested ports, not just a wider version range in metadata.

## What is implemented in 0.1.0-alpha.4

The workshop contains **18 machine types, eight machine tiers, 40 blocks, 37 standalone items, 38 guide articles and 25 advancement milestones**. The Fabric, NeoForge and Forge jars are built from the same shared code. See the [alpha.4 release notes](docs/releases/0.1.0-alpha.4.md) for the additions and upgrade details.

- Machine tiers: every powered machine has a tier from Mk I to Mk VIII. The tiers are data, read from `technologia/tiers.json` in the jar. From Mk I to Mk VIII, work speed rises from 1× to 5×, the energy buffer from 1× to 32×, the energy moved per tick from 200 to 25,600 FE/t, and generator and solar output from 1× to 12×. From Mk III on, an operation costs less energy, down to 70% at Mk VIII. Storage cores and terminals have no tier.
- Tier kits: seven items, one per tier above the first (Bronze Mk II to Stellar Mk VIII). Use a kit on a placed machine to upgrade it in place. Kits install in order. Inventory, energy and settings are kept.
- Machines built from machines: Crafting an Advanced Energy Cell or an Advanced Solar Generator uses other machines as ingredients. The result keeps the lowest tier among those machines and their combined stored energy, up to its own capacity. Kits on a higher-tier ingredient beyond that lowest tier are not refunded.
- Production: Ore Crusher, Electric Furnace, Alloy Smelter, Metal Press, Sawmill, Compactor, Centrifuge, Recycler and Auto Sieve. Processors have 1, 1, 2, 3, 4, 5, 6 and 8 parallel lanes by tier. Each lane has its own ingredient slot (two for the Alloy Smelter) and its own progress. All lanes work at the same time and share the energy buffer and the 27 result slots. When a lane is empty and another lane holds at least two operations' worth of ingredients, the machine moves half of that stack to the empty lane, so the input does not need to be sorted.
- Input rules: a machine refuses items it cannot process. The crusher takes only crushable items, the Electric Furnace only smeltable items, fuel generators only their fuel, and an Alloy Smelter lane only an ingredient that pairs with what it already holds. A hopper or Item Transfer keeps a refused item. An unusable item already inside is moved to the result slots; if they are full the machine shows "Unsupported item".
- Sieving: the Auto Sieve turns gravel, sand and coarse dirt into iron, copper and lead ore fragments, with a chance of a second output (35% tin fragment from gravel, 10% gold fragment from sand, 20% flint from coarse dirt). A cycle takes 80 ticks at 15 FE/t. Four matching fragments crush into one dust. The crusher also turns cobblestone into gravel and gravel into sand.
- Power: coal/charcoal and biomass generators, 16 FE/t and 64 FE/t solar generators, and cells storing one million or five million FE at Mk I. A supplier sends 200 FE/t at Mk I multiplied by its tier's transfer value; that limit is per tick across everything it feeds, including other mods' cables. Energy Conduits (copper) carry 200 FE/t per supplier, Gold Energy Conduits 1,000 FE/t and Resonite Energy Conduits 5,000 FE/t. The weakest conduit on a network limits what flows through that network; a block directly beside a supplier is not limited by conduits.
- Logistics: directional Item Transfers move up to eight items every eight ticks, with a component-aware ghost filter, redstone pause and sided inventory rules. Extraction follows hopper rules, so a transfer on any side of a vanilla furnace takes the smelted items and leaves the fuel. Using the wrench rotates machines, Item Transfers, control panels and ventilation grilles. Crouch-using the wrench dismantles a machine into the player's inventory, keeping its tier and stored energy; its contents drop.
- Nexus: searchable combined item counts, name/count sorting, pages and cursor/shift-click transfers. Terminals combine up to four connected cores, giving 54–216 backing slots. Opening a core directly still shows that core's 54 slots.
- Survey Miner: bounded selective ore mining, an optional retained filter that only a player can set, start/pause/rescan, permission callbacks and an owner-online requirement. Only the owner can take results or change the filter from its screen; hoppers, Item Transfers and breaking the block are not owner-checked. Starts paused; Start after a completed scan begins a new scan. Higher tiers scan and mine faster and spend less energy per block.
- Resonance Bloom: a plant crafted from a Resonite Crystal, any small flower and glowstone dust, planted on soil. Each bloom within two blocks of a processing machine, in any direction, adds 8% speed, up to five blooms and 40%.
- Resources and building: tin, lead and resonite ores; five ore fragments; shared material tags; survival recipes; steel/bronze casings, industrial bricks, grates, hazard blocks, lamps, pillars, grilles, control panels and reinforced glass, each with a distinct recipe. Engineering lamps emit level-15 light; the other building-kit blocks are decorative. Control panels and ventilation grilles face the player when placed.
- Presentation: original 32px textures and a separate three-dimensional model for every machine type. Machine and decoration collision/selection shapes follow their model geometry. Connected cables and conduits follow their routes. An active machine lights its front. Working processors and fuel generators also emit particles and play a quiet working sound, and the Survey Miner emits particles. Machines, cables and building blocks use metal sounds; reinforced glass uses glass sounds. A tier does not change a machine's model yet. Moving mechanical animations remain future work.
- Guidance and management: a native ten-chapter Field Guide with 38 articles, scrolling and keyboard navigation, plus a Progress page that shows the workshop advancements the game has revealed so far as a route with done, next and locked states; 25 persistent vanilla advancements, each earned by obtaining an item; reloadable processing recipes; restart-required server balance config and operator diagnostics.

Draconic and chaotic cores are **creative-only concept items**. Not in alpha.4: fluids, gases, autocrafting, storage disks, server racks, network channels, multiblocks, tools and armor, crops, dimensions, recipe-viewer integration, per-face configuration, a hotkey for the guide, and any language other than English. Rack cooling, mob essence, an orbital solar relay and data-cable materials are planned for later, together with the systems they depend on; none of them is started. Equipment, bosses, fusion, reactors, RFTools-style building and full AE2/Mekanism-scale systems are planned. This project is not an unofficial port of those mods.

Automated server tests run on all three loaders, on headless servers without a game client: the full GameTest suite on NeoForge, and loader smoke tests on Fabric and Forge. Their results for alpha.4 are recorded in [docs/VALIDATION.md](docs/VALIDATION.md). These tests do not show how the mod behaves in a game client or in a modpack. Fabric and Forge client play, multiplayer and representative modpack checks remain required.

## Try the workshop

Download builds from [GitHub Releases](https://github.com/Thespiki/Technologia/releases). Each mod release contains three jars, one per loader. For 0.1.0-alpha.4:

| Loader | Jar | Also needed |
| --- | --- | --- |
| Fabric | `technologia-fabric-1.21.1-0.1.0-alpha.4.jar` | Fabric Loader 0.16.9 or newer and Fabric API 0.109.0+1.21.1 or newer; Team Reborn Energy is bundled |
| NeoForge | `technologia-neoforge-1.21.1-0.1.0-alpha.4.jar` | NeoForge 21.1.80 or newer |
| Forge | `technologia-forge-1.21.1-0.1.0-alpha.4.jar` | Forge 52.0.28 or newer |

Use Minecraft 1.21.1 and Java 21. Put the one jar that matches your loader in the `mods` folder of the client and of the server. Do not install two Technologia loader jars together. Back up a world before opening it with a new alpha.

1. Craft a Field Guide from a book and copper ingot, or use `/technologia guide`. Mine tin and smelt it. Craft a machine frame, basic circuits and a combustion generator.
2. Place an ore crusher directly beside the generator. Put coal or charcoal in the generator's fuel slot. Put raw iron in the crusher's ingredient slot (amber, on the left). Results appear in the 27-slot grid on the right. A machine refuses items it cannot process.
3. Smelt the dust in a vanilla furnace or powered Electric Furnace. Craft an Alloy Smelter: three copper ingots plus one tin ingot make four bronze ingots. Either input order works; a lane only accepts an ingredient that pairs with what it already holds. Crush coal into coal dust; one iron ingot plus two coal dust make steel.
4. Use bronze to craft a Metal Press. Press ingots into plates, then build conduits, solar generation and more production machines. A Sawmill makes six planks and one sawdust per log; sawdust can fuel the Biomass Generator or be crafted into charcoal in batches of nine.
5. Place Energy Conduits between a generator/cell and consumers. A Mk I supplier sends at most 200 FE/t in total across direct neighbors and conduit routes; tier kits raise that limit. Copper conduits carry 200 FE/t per supplier, gold 1,000 FE/t and resonite 5,000 FE/t, and the weakest conduit on a run sets the limit for that run. Runs that only meet at the supplier are separate and keep their own limit. A block directly beside a supplier is not limited by conduits. Generators can charge cells; cells do not charge each other. Solar generation requires clear daytime, a skylit dimension and open sky above the panel.
6. Place an Item Transfer between two inventories, with its arrow pointing toward the destination. Right-click with a sample item to filter it, or empty-handed to clear the filter. The sample is retained. Use the wrench to cycle all six directions; redstone pauses transfer. Hoppers also feed machine inputs and collect outputs.
7. Connect one to four Storage Cores and a terminal using Network Cable or direct contact. Search by item name or registry ID; left-click a result for a stack, right-click for one item, or shift-click to withdraw into your inventory. Shift-click your inventory to deposit, or use **Deposit held** for the cursor stack. Reopen the terminal after changing its connected cores. Network Cable carries storage connectivity; use Energy Conduits for power.
8. Place a Survey Miner above ore and supply power. Its filter slot is optional and takes an ore or raw material; leave it empty for all tagged ores. Only a player can set the filter, not a hopper. Press Start. Default area: 9×9 horizontally, up to 32 blocks below the machine. Only its owner can start/pause/rescan it, and take its results or change its filter from its screen; that owner must be online in the same dimension. A hopper or Item Transfer on the miner extracts for anyone. When a scan completes the miner pauses; Start begins a new scan.
9. Craft a Bronze Tier Kit (four bronze plates around a basic circuit) and use it on a placed machine to raise it from Mk I to Mk II. Kits install in order up to Mk VIII; the machine keeps its inventory, energy and settings. From Mk III a processor has two or more lanes, each with its own ingredient slot and progress line. Only the slots a tier uses are shown.
10. Use the wrench on a machine to rotate it. Crouch-use the wrench to dismantle it: the machine goes to your inventory with its tier and stored energy, and its contents drop. Place it again to continue with the same tier and energy.
11. Craft an Auto Sieve and feed it gravel, sand or coarse dirt. Crush four matching fragments into one dust, then smelt the dust. Feed a crusher cobblestone to make gravel, and gravel to make sand.
12. Craft a Resonance Bloom from a Resonite Crystal, any small flower and glowstone dust, and plant it on soil within two blocks of a processing machine. Each bloom adds 8% speed, up to five blooms. Faster work draws proportionally more power per tick; the energy per operation is unchanged.

Ore generation affects newly generated chunks. Machine inventories persist across saves and drop when machines are broken. A broken or dismantled machine item keeps its tier and stored energy. The Electric Furnace does not award smelting XP yet. Recycling consumes the entire input item, including its enchantments and components. Advancements record obtaining equipment; they neither gate recipes nor give repeatable item rewards.

Worlds from alpha.3 load in alpha.4: machine data now carries a format marker, results of existing machines move to the new result slots, and registry ids are unchanged. Back up a world before upgrading an alpha.

## Manage it

`config/technologia.json` is created at startup. Restart the server after changing it. It controls combustion-generator output, biomass output at half that value, Electric Furnace duration/cost, miner energy/radius/depth, whether mining is enabled, and `machineSounds` (set it to `false` to turn working sounds off). Values are clamped to implementation limits. A key missing from a partial file keeps its default. Invalid JSON is logged and defaults are used without overwriting your file.

Machine tiers are read at start-up from `technologia/tiers.json` inside the mod jar. Each tier has `lanes`, `speed`, `capacity`, `efficiency`, `generation`, `transfer` and, above the first tier, a `kit` recipe. One tier kit item is registered per tier above the first, so the client and the server must use the same file. It is not a datapack file and is not reloaded while the game runs.

The Crusher, the Auto Sieve and the other processing machines use datapack recipes of type `technologia:processing`, with counted item/tag ingredients, a result, an optional byproduct with an optional `byproduct_chance` (0 to 1, default 1), cycle time in ticks and energy per tick. The Electric Furnace reads vanilla smelting recipes and takes its cost and duration from the config. For example, `data/technologia/recipe/alloying/bronze.json` contains:

```json
{
  "type": "technologia:processing",
  "machine": "alloy_smelter",
  "ingredients": [
    {"ingredient": {"tag": "c:ingots/copper"}, "count": 3},
    {"ingredient": {"tag": "c:ingots/tin"}, "count": 1}
  ],
  "result": {"id": "technologia:bronze_ingot", "count": 4},
  "time": 160,
  "energy": 30
}
```

Recipes reload through the standard datapack workflow. All ingredients and space for both output and byproduct are checked before energy is spent; space for a byproduct is reserved even when its chance is below 1. Changing a recipe or input identity resets that lane's progress; adding more of the same input preserves it. Pause stops production while stored power may still be exported.

Operators can run `/technologia status` for the active settings. A graphical control dashboard and configuration UI are planned.

Forge/NeoForge expose energy and sided item capabilities; Fabric exposes transactional Team Reborn Energy and uses vanilla sided inventories. Energy that another mod's cable pulls from a supplier counts against the same per-tick send limit as conduit routing, and energy pushed into a machine by another mod's cable is limited to that machine's transfer rate per tick. Conduits can route Technologia suppliers to external energy endpoints through these adapters. Conduits do not expose a general input buffer for another mod's power cable. Item Transfers currently support vanilla `Container` and sided-container inventories; capability-only external inventories need an adapter. Create-specific recipes, kinetic conversion and recipe-viewer entry points are not implemented yet. The request names JEI or other recipe viewers; JEI comes first, with EMI and REI as candidates to evaluate. See the [compatibility plan](docs/ROADMAP.md) for remaining pack tests.

Akashic Tome: the Field Guide's item id (`technologia:field_guide`) contains "guide", so the NeoForge 1.21.1 build of Akashic Tome should accept it with its default settings. This was read from that mod's source and has not been tested in game. No Forge or Fabric build of Akashic Tome for 1.21.1 was found.

## Build and test

Install a **Java 21 JDK**, set `JAVA_HOME`, and use the included Gradle wrapper. Node.js is only needed for asset generation/validation. The first build downloads Minecraft and loader development dependencies and can take several minutes.

```powershell
.\gradlew.bat :common:test :neoforge:build
.\gradlew.bat :neoforge:runGameTestServer
.\gradlew.bat :neoforge:runClient
```

Other loader artifacts and their server smoke tests:

```powershell
.\gradlew.bat :fabric:build :forge:build
.\gradlew.bat :fabric:runGametest
.\gradlew.bat :forge:runGameTestServer
node tools/verify-resources.mjs
```

On Linux/macOS use `./gradlew`. Installable jars are under `fabric/build/libs`, `neoforge/build/libs` or `forge/build/libs`; choose the main jar, not a `sources` or `javadoc` jar. The `common` module is not an installable mod.

The full GameTest suite lives in `neoforge/src/gametest`. Fabric and Forge have loader smoke tests in `fabric/src/gametest` and `forge/src/gametest`. These are development-only source sets: the tests and their test structure are not packaged in any jar. Each test task starts a headless server, runs the tests and stops.

To build all three loaders and collect their installable jars together:

```powershell
.\gradlew.bat packageRelease
```

This runs the shared unit tests and all three loader builds, then puts the three jars in `releases/<mod version>/`, using `version` from `gradle.properties`. Each folder contains the Fabric, NeoForge and Forge jars for Minecraft 1.21.1. Archives are built without file timestamps and with a stable entry order, so the same source gives the same jar. Every jar carries the project licence, the two template licences and a third-party notice; packaging fails if one of them or the tiers file is missing, or if test classes are found in a jar. Loader version ranges in the mod metadata come from `gradle.properties`. Delivered versions are preserved: packaging refuses to overwrite an existing jar with different bytes. Increment the version for a changed build. Generated release jars stay local and are excluded from Git; Git tags preserve the corresponding source.

Pushing a `v<mod version>` tag publishes those three variants to GitHub Releases after the build, the resource checks and the server tests of all three loaders pass. See [release packaging and publishing](releases/README.md) for the versioned notes and tag workflow.

`node tools/generate-resources.mjs` reproducibly rebuilds prototype textures, recipes, loot, tags, worldgen and loader metadata; it reads the same `tiers.json` as the mod. `node tools/generate-test-template.mjs` rebuilds the game-test structure. Edit the generators when changing generated content.

`node tools/build-site.mjs` builds the static presentation into `site/dist`. The Pages workflow publishes presentation changes on `main` using GitHub Actions; set the repository's Pages source to **GitHub Actions**. The build workflow compiles all three loaders and requires an explicit success summary from each loader's server tests.

## Design and next steps

- [Art direction, progression and 36 feature proposals](docs/DESIGN.md)
- [Visual board with a sample of the current generated textures](docs/art-direction.html)
- [Roadmap, version targets and compatibility requirements](docs/ROADMAP.md)
- [Verification results](docs/VALIDATION.md)
- [In-game acceptance checklist](docs/PLAYTEST.md)

The build structure is based on [Jaredlll08's MultiLoader-Template, 1.21.1 branch](https://github.com/jaredlll08/MultiLoader-Template/tree/1.21.1), with its CC0 license preserved. Original project content has no redistribution license selected yet.
