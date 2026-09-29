# Delivery plan and quality gates

## Current slice

Shared Java gameplay, three loader adapters, first machine UI and original prototype art. Coal power, buffering, crusher doubling, electric smelting, bounded selective mining, a core/terminal storage connection, new ores and basic recipes. See README and VALIDATION for actual verification status.

The agreed rollout is to develop and playtest on one version first: **Minecraft 1.21.1, NeoForge as the first in-game test platform**. The eventual coverage requested is **1.21.1 and every later stable Minecraft release**, across Fabric, NeoForge and Forge wherever those loaders are available. The loader adapters are prepared early to keep gameplay portable; simultaneous release on every target is not a prerequisite for the first playable milestone. Snapshots are excluded unless requested.

## Next milestones

1. **Harden the workshop:** client and dedicated-server tests on all three loaders; persistence/reload, multiplayer, full inventory, recipe reload and automation edge cases; facing and active block states; guide pages; accessible screens and localization.
2. **Nexus and logistics:** searchable storage, multiple cores/disks, import/export, energy/fluid conduits, side setup, sorting, item filters, owner/team permissions and a live network inspector.
3. **Production:** data-driven crusher recipes, alloys, fluid tanks, washing/chemistry, factory upgrades and first autocrafting. Show recipe dependencies and unavailable steps in the UI.
4. **Architect and control:** builder previews, blueprint material planning, named travel, dashboard, stock rules and machine configuration copying.
5. **Ascendant:** modular tools, shields, flight, fusion assembly, reactor engineering and multiblock reservoir. Survival progression must have uses at each tier.
6. **Chaos:** original encounters, rifts, exotic fabrication and optional creative-item objectives. Add content only after server load and encounter readability are tested.

## Compatibility contracts

| System | Foundation | Next verification/work |
| --- | --- | --- |
| Energy | Forge/NeoForge FE capability adapters; Fabric Team Reborn Energy adapter | Test push/pull directions, simulation, nested transaction rollback and representative cable mods |
| Units | One internal unit maps to one FE or one Fabric E | Balance remains a gameplay choice; no hidden voltage conversion |
| Inventory | Vanilla sided containers; Forge/NeoForge item capabilities | Exercise external pipes and Create funnels; prevent output insertion and input extraction |
| Materials | Shared `c:` tags and legacy `forge:` exports | Check tag conventions with actual pack dependencies |
| Recipes | Vanilla JSON crafting and smelting; furnace reads recipe manager | Move crusher mapping to a custom recipe serializer, then JEI/EMI/REI displays |
| Create | Common inventory/material surfaces are provided | Optional crushing/mixing/pressing recipes and a dedicated kinetic bridge; no direct Create integration is claimed yet |
| Mining protection | Owner-online restriction and loader break callbacks; no forced chunks | Verify each claim mod; add dedicated integration where callback coverage is insufficient |
| Pack management | Restart-required server balance config, bounded values, invalid-file fallback | Config UI, per-world profiles, diagnostics command, pack overrides and migration policy |
| Scripting | Datapacks can change standard recipes and tags | Explicit KubeJS/CraftTweaker hooks only after recipe/API stabilization |

Create's kinetic stress/rotation is not FE. A generator/motor bridge needs its own speed, stress, efficiency and feedback-loop design. Availability of a Create version or addon on one loader does not imply availability on the other two. Pin and test actual versions before advertising support.

The baseline versions are Minecraft 1.21.1, Java 21, Fabric Loader 0.16.9/API 0.109.0, Forge 52.0.28 and NeoForge 21.1.80. Broader version coverage is a delivery requirement, not a claim of existing binary support. Each port needs a separate compatibility matrix and tested release. Keep the oldest supported line maintained; group compatible minor releases only after verification. When a requested loader does not exist for a release, record that gap explicitly rather than implying compatibility.

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
- Save migrations have versioned fixtures before released data layouts change.

Prefer many small, complete and tested milestones. The desired final mod is large; the first alpha must not describe planned content as already playable.
