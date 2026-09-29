# Technologia

An original Minecraft technology mod inspired by the automation and enormous factories of the 1.12.2 era. **Early development alpha**, with a shared codebase and separate Fabric, NeoForge and Forge adapters.

[Explore the presentation](https://thespiki.github.io/Technologia/) · [Full requested scope](docs/REQUIREMENTS.md) · [18-milestone progression proposal](docs/PROGRESSION.md)

The first development/playtest target is **Minecraft 1.21.1 + NeoForge**. The requested long-term target is **1.21.1 and later stable releases**, across the three loaders where available. Newer versions require tested ports, not just a wider version range in metadata.

## What is implemented in the first slice

- Combustion generator: coal/charcoal → power; pauses when full.
- Energy cell: one million energy units; adjacent transfer and loader energy adapters.
- Ore crusher: raw iron, gold, copper, tin or lead → two dust.
- Electric furnace: reads the game's smelting recipes, including datapacks.
- Survey miner: selective ore mining in a bounded region, one filter slot, start/pause/rescan, permission callbacks and an owner-online requirement. Starts paused.
- Nexus storage core: 54 slots. A terminal opens the connected core through a network of up to 128 blocks. It is not yet searchable digital storage or autocrafting.
- Tin, lead and resonite generation, drops, common material tags and survival crafting/smelting recipes.
- A shared machine screen, status messages, energy/progress indicators, help tooltips and original 32px prototype art.
- Static 3D models with recessed storage drive bays and a recessed terminal screen, pending in-game visual review.
- Server balance config, operator diagnostics, automated resource checks, unit tests and NeoForge game tests.

Draconic and chaotic cores are **creative-only concept items**. Equipment, bosses, fusion, reactors, RFTools-style building and full AE2/Mekanism-scale systems are planned. This project is not an unofficial port of those mods.

## Try the workshop

Install the jar for your Minecraft version and loader on client and server. Fabric additionally requires Fabric API; Team Reborn Energy is bundled. Do not install multiple Technologia loader jars together.

1. Mine tin and smelt it. Craft a machine frame, basic circuits and a combustion generator.
2. Place an ore crusher directly beside the generator. Put coal or charcoal in the generator's upper-left slot. Put raw iron in the crusher's upper-left slot. The other slots receive output.
3. Smelt the dust in a vanilla furnace or powered electric furnace. Hoppers can feed input and collect output.
4. Place a storage core and terminal connected by network cable. Both access the same stored items. Network cable currently carries storage connectivity, not power.
5. Place a Survey Miner above ore and supply power. Its first slot is an optional ore/raw-material filter; leave it empty for all tagged ores. Press Start. Default area: 9×9 horizontally, up to 32 blocks below the machine. Only its owner can start/pause/rescan it, and that owner must be online in the same dimension.

Ore generation affects newly generated chunks. Machine inventories persist across saves and drop when machines are broken. Energy is not retained in a broken machine item in this prototype. The electric furnace does not award smelting XP yet. Machine fronts currently have a fixed orientation; connected cable geometry and active animations are future polish.

## Manage it

`config/technologia.json` is created at startup. Restart the server after changing it. It controls generator output, processing duration/cost, miner energy/radius/depth and whether mining is enabled. Values are clamped to safe implementation limits. Invalid JSON is logged and defaults are used without overwriting your file.

Operators can run `/technologia status` for the active settings. A graphical control dashboard and configuration UI are planned.

Forge/NeoForge expose energy and sided item capabilities; Fabric exposes transactional Team Reborn Energy and uses vanilla sided inventories. These are interoperability surfaces, not evidence that every cable, claim or automation mod has been tested. Create-specific recipes and kinetic conversion are not implemented yet. See the [compatibility plan](docs/ROADMAP.md).

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

`node tools/generate-resources.mjs` reproducibly rebuilds prototype textures, recipes, loot, tags, worldgen and loader metadata. `node tools/generate-test-template.mjs` rebuilds the game-test structure. Edit the generators when changing generated content.

`node tools/build-site.mjs` builds the static presentation into `site/dist`. The Pages workflow publishes presentation changes on `main` using GitHub Actions; set the repository's Pages source to **GitHub Actions**. The build workflow compiles all three loaders and checks NeoForge GameTests for an explicit success summary.

## Design and next steps

- [Art direction, progression and 36 feature proposals](docs/DESIGN.md)
- [Visual board with the actual prototype textures](docs/art-direction.html)
- [Roadmap, version strategy and compatibility requirements](docs/ROADMAP.md)
- [Verification results](docs/VALIDATION.md)
- [In-game acceptance checklist](docs/PLAYTEST.md)

The build structure is based on [Jaredlll08's MultiLoader-Template, 1.21.1 branch](https://github.com/jaredlll08/MultiLoader-Template/tree/1.21.1), with its CC0 license preserved. Original project content has no redistribution license selected yet.
