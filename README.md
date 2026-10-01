# Technologia

An original Minecraft technology mod inspired by the automation and enormous factories of the 1.12.2 era. **Early development alpha**, with a shared codebase and separate Fabric, NeoForge and Forge adapters.

[Explore the presentation](https://thespiki.github.io/Technologia/) · [Full requested scope](docs/REQUIREMENTS.md) · [18-milestone progression proposal](docs/PROGRESSION.md)

The first development/playtest target is **Minecraft 1.21.1 + NeoForge**. The requested long-term target is **1.21.1 and later stable releases**, across the three loaders where available. Newer versions require tested ports, not just a wider version range in metadata.

## What is implemented in 0.1.0-alpha.3

The workshop now contains **17 machine types, 10 factory building blocks, 32 guide articles and 20 advancement milestones**, with separate Fabric, NeoForge and Forge builds. See the [alpha.3 release notes](docs/releases/0.1.0-alpha.3.md) for the additions and upgrade details.

- Production: Ore Crusher, Electric Furnace, Alloy Smelter, Metal Press, Sawmill, Compactor, Centrifuge and Recycler. Bronze, steel, five metal plates, coal dust and sawdust connect the recipes into a longer workshop progression.
- Power: coal/charcoal and biomass generators, 16 FE/t and 64 FE/t solar generators, and cells storing one million or five million FE. Energy Conduits route power from Technologia generators and cells through loaded connections.
- Logistics: directional Item Transfers move up to eight items every eight ticks, with a component-aware ghost filter, redstone pause and sided inventory rules. A wrench rotates machines and transfer arrows without breaking them.
- Nexus: searchable combined item counts, name/count sorting, pages and cursor/shift-click transfers. Terminals combine up to four connected cores, giving 54–216 backing slots. Opening a core directly still shows that core's 54 slots.
- Survey Miner: bounded selective ore mining, an optional retained filter, start/pause/rescan, permission callbacks and an owner-online requirement. Starts paused.
- Resources and building: tin, lead and resonite ores; shared material tags; survival recipes; steel/bronze casings, industrial bricks, grates, hazard blocks, lamps, pillars, grilles, control panels and reinforced glass. Engineering lamps emit level-15 light; the other building-kit blocks are decorative.
- Presentation: original 32px textures and distinct three-dimensional models across all machine types, including open press frames, saw housings, solar panels, exposed coils, storage racks and a freestanding terminal. Machine and decoration collision/selection shapes follow their model geometry. Connected cables and conduits follow their routes. Active fronts change appearance; moving mechanical animations remain future work.
- Guidance and management: a native nine-chapter Field Guide with 32 articles, scrolling and keyboard navigation; 20 persistent vanilla advancements; reloadable processing recipes; restart-required server balance config and operator diagnostics.

Draconic and chaotic cores are **creative-only concept items**. Equipment, bosses, fusion, reactors, RFTools-style building and full AE2/Mekanism-scale systems are planned. This project is not an unofficial port of those mods.

All three loader builds passed, and the local NeoForge server suite passed all **38 required GameTests**. Targeted NeoForge client checks passed for the models, alloy UI, conduit power, item transfer, combined storage and wrench; see the [verification record](docs/VALIDATION.md). Earlier NeoForge playtesting covered the original workshop; it does not certify the new systems. Multiplayer, Fabric/Forge runtime and representative modpack checks remain required.

## Try the workshop

Install the jar for your Minecraft version and loader on client and server. Fabric additionally requires Fabric API; Team Reborn Energy is bundled. Do not install multiple Technologia loader jars together.

Download builds from [GitHub Releases](https://github.com/Thespiki/Technologia/releases). Each mod release contains separate Fabric, NeoForge and Forge jars.

1. Craft a Field Guide from a book and copper ingot, or use `/technologia guide`. Mine tin and smelt it. Craft a machine frame, basic circuits and a combustion generator.
2. Place an ore crusher directly beside the generator. Put coal or charcoal in the generator's upper-left slot. Put raw iron in the crusher's upper-left slot. The other slots receive output.
3. Smelt the dust in a vanilla furnace or powered Electric Furnace. Craft an Alloy Smelter: three copper ingots plus one tin ingot make four bronze ingots. Either input order works. Crush coal into coal dust; one iron ingot plus two coal dust make steel.
4. Use bronze to craft a Metal Press. Press ingots into plates, then build conduits, solar generation and more production machines. A Sawmill makes six planks and one sawdust per log; sawdust can fuel the Biomass Generator or be crafted into charcoal in batches of nine.
5. Place Energy Conduits between a generator/cell and consumers. Each supplier shares a 200 FE/t budget across direct neighbors and conduit routes. Generators can charge cells; cells do not charge each other. Solar generation requires clear daytime, a skylit dimension and open sky above the panel.
6. Place an Item Transfer between two inventories, with its arrow pointing toward the destination. Right-click with a sample item to filter it, or empty-handed to clear the filter. The sample is retained. Use the wrench to cycle all six directions; redstone pauses transfer. Hoppers also feed machine inputs and collect outputs.
7. Connect one to four Storage Cores and a terminal using Network Cable or direct contact. Search by item name or registry ID; left-click a result for a stack, right-click for one item, or shift-click to withdraw into your inventory. Shift-click your inventory to deposit, or use **Deposit held** for the cursor stack. Reopen the terminal after changing its connected cores. Network Cable carries storage connectivity; use Energy Conduits for power.
8. Place a Survey Miner above ore and supply power. Its first slot is an optional ore/raw-material filter; leave it empty for all tagged ores. Press Start. Default area: 9×9 horizontally, up to 32 blocks below the machine. Only its owner can start/pause/rescan it, and that owner must be online in the same dimension.

Ore generation affects newly generated chunks. Machine inventories persist across saves and drop when machines are broken. Energy is not retained in a broken machine item in this prototype. The Electric Furnace does not award smelting XP yet. Recycling consumes the entire input item, including its enchantments and components. Advancements record obtaining equipment; they neither gate recipes nor give repeatable item rewards.

## Manage it

`config/technologia.json` is created at startup. Restart the server after changing it. It controls combustion-generator output, biomass output at half that value, Electric Furnace duration/cost, miner energy/radius/depth and whether mining is enabled. Values are clamped to implementation limits. Invalid JSON is logged and defaults are used without overwriting your file.

The Crusher and new processing machines use datapack recipes of type `technologia:processing`, with counted item/tag ingredients, a result, an optional byproduct, cycle time in ticks and energy per tick. This replaces the old crusher mapping: its cost and duration now come from recipes, rather than the furnace settings. For example, `data/technologia/recipe/alloying/bronze.json` contains:

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

Recipes reload through the standard datapack workflow. All ingredients and space for both output and byproduct are checked before energy is spent. Changing a recipe or input identity resets progress; adding more of the same input preserves it. Pause stops production while stored power may still be exported.

Operators can run `/technologia status` for the active settings. A graphical control dashboard and configuration UI are planned.

Forge/NeoForge expose energy and sided item capabilities; Fabric exposes transactional Team Reborn Energy and uses vanilla sided inventories. Conduits can route Technologia suppliers to external energy endpoints through these adapters. Conduits do not expose a general input buffer for another mod's power cable. Item Transfers currently support vanilla `Container` and sided-container inventories; capability-only external inventories need an adapter. Create-specific recipes, kinetic conversion and JEI/EMI/REI recipe displays are not implemented yet. See the [compatibility plan](docs/ROADMAP.md) for remaining pack tests.

## Build and test

Install a **Java 21 JDK**, set `JAVA_HOME`, and use the included Gradle wrapper. Node.js is only needed for asset generation/validation. The first build downloads Minecraft and loader development dependencies and can take several minutes.

```powershell
.\gradlew.bat :common:test :neoforge:build
.\gradlew.bat :neoforge:runGameTestServer
.\gradlew.bat :neoforge:runClient
```

Other loader artifacts:

```powershell
.\gradlew.bat :fabric:build :forge:build
node tools/verify-resources.mjs
```

On Linux/macOS use `./gradlew`. Installable jars are under `fabric/build/libs`, `neoforge/build/libs` or `forge/build/libs`; choose the main jar, not a `sources` or `javadoc` jar. The `common` module is not an installable mod.

To build all three loaders and collect their installable jars together:

```powershell
.\gradlew.bat packageRelease
```

This runs the shared unit tests and all three loader builds, then puts the three jars in `releases/<mod version>/`, using `version` from `gradle.properties`. Each folder contains the Fabric, NeoForge and Forge jars for Minecraft 1.21.1. Delivered versions are preserved: packaging refuses to overwrite an existing jar with different bytes. Increment the version for a changed build. Generated release jars stay local and are excluded from Git; Git tags preserve the corresponding source.

Pushing a `v<mod version>` tag publishes those three variants to GitHub Releases after build/resource checks and server GameTests pass. See [release packaging and publishing](releases/README.md) for the versioned notes and tag workflow.

`node tools/generate-resources.mjs` reproducibly rebuilds prototype textures, recipes, loot, tags, worldgen and loader metadata. `node tools/generate-test-template.mjs` rebuilds the game-test structure. Edit the generators when changing generated content.

`node tools/build-site.mjs` builds the static presentation into `site/dist`. The Pages workflow publishes presentation changes on `main` using GitHub Actions; set the repository's Pages source to **GitHub Actions**. The build workflow compiles all three loaders and checks NeoForge GameTests for an explicit success summary.

## Design and next steps

- [Art direction, progression and 36 feature proposals](docs/DESIGN.md)
- [Visual board with the actual prototype textures](docs/art-direction.html)
- [Roadmap, version strategy and compatibility requirements](docs/ROADMAP.md)
- [Verification results](docs/VALIDATION.md)
- [In-game acceptance checklist](docs/PLAYTEST.md)

The build structure is based on [Jaredlll08's MultiLoader-Template, 1.21.1 branch](https://github.com/jaredlll08/MultiLoader-Template/tree/1.21.1), with its CC0 license preserved. Original project content has no redistribution license selected yet.
