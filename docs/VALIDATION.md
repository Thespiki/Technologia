# Verification record — 0.1.0-alpha.2

This record distinguishes compilation, automated behavior checks and hands-on playtesting. Passing a build is not a guarantee of compatibility or a finished game experience.

| Check | Result |
| --- | --- |
| Shared Java compilation | Passed with Java 21 |
| Energy core unit tests | 2 passed; includes 20,000 simulated/committed transfer cycles and integer boundary tests |
| Resource structural validation | Passed: 181 shared JSON files, model/texture references, translations, local common tag references and loader metadata |
| Fabric 1.21.1 build | Final alpha.2 build passed as part of local release packaging |
| NeoForge 1.21.1 build | Final alpha.2 build passed as part of local release packaging |
| Forge 1.21.1 build | Final alpha.2 build passed as part of local release packaging |
| NeoForge game tests | All 18 required server tests passed on 2026-09-30, including native guide content and read-only menu checks |
| GitHub CI | Final alpha.2 run pending |
| Versioned packaging | Local packageRelease succeeded on 2026-09-30 and produced the three alpha.2 loader jars; overwrite protection added |
| Presentation site | Static build and JavaScript syntax passed; search, status filters, empty results and combined filtering checked in browser; mobile and desktop layouts reviewed; added guide screenshot checked at a 1265px viewport |
| Minecraft client visual review | Initial NeoForge checks at 854×480 and 1920×1009 client sizes covered the guide, storage catalogue/empty search and furnace layout; details below |
| Field Guide content | Native nine-chapter atlas derives 17 articles from shared content; archival book metadata retained. Responsive screen uses wrapped, scrollable text |
| Fabric / Forge in-game tests | Not performed |
| Create, external cables, claim mods and modpacks | Not tested in a running pack |
| Later Minecraft releases | Ports not started |

The original eight GameTests cover generator-to-crusher processing, full-output backpressure, data-driven smelting, block-entity save/load, network disconnection, a paused miner, FE simulation and sided inventory access. Ten new tests cover aggregation of 1,000 identical items while separating named/component variants; stack/one/shift withdrawals and cursor/shift deposits; full inventories and partial space; stale/invalid catalogue requests and hidden-slot packets; disconnected open menus; inventory preservation across facing/activity changes; actual processing indicators; cable arm updates; and guide content, recipes and command permissions.

Reproduce with the commands in README. The test report is generated under `common/build/reports/tests/test`; server-run diagnostics are in the NeoForge run directory. See PLAYTEST.md for remaining acceptance checks.

An earlier alpha.1 server startup failure returned a successful Gradle exit despite a registry error; CI therefore requires an explicit successful GameTest summary in addition to the process exit code. Javadoc warnings about missing API comments remain.

Remaining limits: one core per terminal and no autocrafting, adjacent power transfer without a built-in energy cable network, a single miner filter with configured radius/depth, hardcoded crusher material families, no furnace XP, no energy retained in broken machine items, no final equipment/boss systems and no Create kinetic bridge. Multiplayer stress, existing-world upgrades, other loaders' runtime behavior and external compatibility still need playtests.

User playtest on NeoForge 1.21.1: reported working core/terminal synchronization via both cable and direct contact, energy-cell transfer, generator, furnace, crusher and miner filters for iron, coal and unfiltered ores. Their screenshots exposed a progress-bar overlap and an empty-storage grid/text overlap; both layouts have been corrected in source. This is a first singleplayer playtest, not multiplayer or modpack certification.

Client checks on 2026-09-30 confirmed all nine atlas cards fit in the maximized guide, the Nexus sidebar chapter opens, and Next advances to its next article. In the small window, clicking the scrollbar exposed the final “One core per terminal” text without overlapping the footer. Automated mouse-wheel and Page Down inputs were inconclusive, so those navigation paths are not recorded as verified.

The live storage catalogue showed 13 item types and 2,033 items, including a combined count of 1,000 coal. A search with no match displayed the corrected empty-results panel. The furnace showed 64 output ingots, with its progress bar below the output grid and clear of the controls. The world saved and exited normally. These checks supplement the server tests; they do not cover every interface action or GUI scale.
