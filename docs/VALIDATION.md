# Verification record — 2026-09-29

This record distinguishes compilation, automated behavior checks and hands-on playtesting. Passing a build is not a guarantee of compatibility or a finished game experience.

| Check | Result |
| --- | --- |
| Shared Java compilation | Passed with Java 21 |
| Energy core unit tests | 2 passed; includes 20,000 simulated/committed transfer cycles and integer boundary tests |
| Resource structural validation | Passed: 165 shared JSON files, model/texture references, translations, local common tag references and loader metadata |
| Fabric 1.21.1 build | Passed; final alpha jar rebuilt after registry and model changes |
| NeoForge 1.21.1 build | Passed; final alpha jar rebuilt after registry and model changes |
| Forge 1.21.1 build | Passed; final alpha jar rebuilt after registry and model changes |
| NeoForge game tests | All 8 required server tests passed |
| Presentation site | Static build and JavaScript syntax passed; search, status filters, empty results and combined filtering checked in browser; mobile and desktop layouts reviewed |
| Minecraft client visual review | Not performed |
| Fabric / Forge in-game tests | Not performed |
| Create, external cables, claim mods and modpacks | Not tested in a running pack |
| Later Minecraft releases | Ports not started |

The game-test suite exercises coal-generator-to-crusher processing, full-output backpressure, a data-driven smelting recipe, block-entity save/load, network disconnection, a paused miner, FE simulation and sided inventory access.

Reproduce with the commands in README. The test report is generated under `common/build/reports/tests/test`; server-run diagnostics are in the NeoForge run directory. See PLAYTEST.md for remaining acceptance checks.

The final local three-loader build succeeded after the deferred registry initialization fix and recessed model additions. An earlier server startup failure returned a successful Gradle exit despite a registry error; CI therefore requires an explicit successful GameTest summary in addition to the process exit code. The fixed server run reports all eight tests passing. Javadoc warnings about missing API comments remain.

Known implementation limits are intentional for this first slice: fixed machine-facing textures, a plain storage inventory without search/autocrafting, adjacent power transfer rather than a built-in energy cable network, a single miner filter with configured radius/depth, hardcoded crusher material families, no furnace XP, no energy retained in broken machine items, no final equipment/boss systems and no Create kinetic bridge.
