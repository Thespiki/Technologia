# Alpha acceptance checklist

Record Minecraft, Java, loader, modpack and Technologia versions with each result. Build success is not a playtest pass. Run on Fabric, Forge and NeoForge independently, on clients and dedicated servers.

## Workshop

1. Create a fresh survival world. Locate tin, lead and resonite at their configured heights, also at negative Y. Test fortune, silk touch and wrong-tier tools.
2. Obtain the first circuit and generator through recipes. Place generator next to crusher. Insert coal in the upper-left slot, raw iron in the crusher. Confirm 1 raw iron → 2 iron dust, then smelt both to ingots.
3. Pause, remove input, change input and reload recipes mid-process. No duplication, no output for a different recipe's progress.
4. Fill every output slot. The machine stops without consuming the input. Clear one slot and confirm recovery.
5. Break/re-place a machine, unload its chunk and restart the server while running. Inventory drops exactly once when broken; inventory, fuel, energy and progress persist across reloads. Stored energy is intentionally lost when broken in this prototype.

## Storage and automation

1. Connect core → cable → terminal. Access the same items from core and terminal. Insert/extract with two players and hoppers simultaneously; total count must be conserved.
2. Break cable/core/terminal with the menu open. Moving out of range or losing connectivity closes access. Networks stop traversing beyond 128 connected blocks and never load chunks.
3. Insert into machine input via hopper, extract output below it. Pipes must not insert into output slots or pull the miner's filter.
4. Test a real FE cable/source/sink on Forge and NeoForge and a Team Reborn Energy source/sink on Fabric. Test simulation and cancelled nested transactions, not only successful transfer.
5. With Create installed on a supported loader/version, test funnels and chutes. This does not test a kinetic bridge: none is implemented yet.

## Miner

1. Place miner above known ore. It starts paused. Power it, choose a filter (ore/raw material in first slot; empty means all ores), then start. Default area: 9×9, up to 32 blocks below it.
2. Confirm owner-only start/pause/rescan, owner-online requirement, dimension check, world border and spawn-protection behavior. Try break-cancellation and actual claimed regions from a protection mod.
3. Fill output, then free it. Ore must remain until all drops fit. Run with fortune-modifying loot datapacks and large stack counts.
4. Disconnect the owner, restart, rescan, and unload an adjacent chunk. It must not force-load chunks or mine protected blocks.
5. Configure minerEnabled=false and verify it cannot resume. Corrupt the config; verify fallback is logged and the original file is preserved.

## Interface and server load

1. Test small/large GUI scale, keyboard navigation, tooltips, status strings and simultaneous viewers.
   Check the separate machine input/output areas and that cells do not show unused item slots. Read every Field Guide page and its contents links. Craft a guide, request it without operator permissions, repeat the request and try with a full inventory.
   Search Nexus by name and registry ID, switch sorting and pages, and test stack/one/shift withdrawals, cursor deposits and repeated fast clicks while a second player or hopper changes the core. Search typing must not trigger hotbar shortcuts.
   Place machines facing each direction; check active/inactive fronts, recessed bays, terminal depth and all six cable arms. Existing alpha.1 worlds should preserve inventories and acquire default facing after upgrade; back up a test world before upgrading.
2. Confirm display of stored energy above 32,767 and 65,535; vanilla menu synchronization uses split integer parts.
3. Profile an active factory before setting advertised scale limits. Record tick time and memory for 10, 100 and 500 machines.
4. Test with all optional mods absent, then each integration individually. Do not claim blanket compatibility based only on registration.
