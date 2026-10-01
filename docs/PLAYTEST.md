# Alpha.3 acceptance checklist

Record Minecraft, Java, loader, modpack and Technologia versions with each result. These are acceptance cases, not completed-test claims. Put results and remaining gaps in [VALIDATION.md](VALIDATION.md). Run on Fabric, Forge and NeoForge independently, on clients and dedicated servers.

## Survival workshop and recipes

1. In a fresh survival world, locate tin, lead and resonite at their configured heights, including negative Y. Test fortune, silk touch and wrong-tier tools. Obtain the first circuit, frame and generator through recipes.
2. Power a crusher beside the combustion generator. Check one raw metal → two dust for iron, gold, copper, tin and lead; smelt the dust to ingots. Confirm coal → coal dust and reject charcoal for that processing recipe.
3. Craft an Alloy Smelter. Three copper plus one tin must yield four bronze; one iron plus two coal dust must yield one steel. Test both slot orders, insufficient quantities, larger stacks and unrelated items. Inputs are consumed only when a complete cycle finishes.
4. Press iron, copper, gold, bronze and steel ingots into matching plates. Follow the recipes from bronze into conduits and solar, and from steel into the compactor, miner and advanced power. Check that no recipe depends on its own unobtainable output.
5. Process all ten wood families, including stripped logs/wood and crimson/warped stems. Each sawmill cycle must produce six matching planks plus one sawdust. Fill all available space for the byproduct: the machine must stop without consuming input or spending more power. Clear space and resume.
6. Check compactor recipes for bulk metals, coal, redstone, lapis, diamonds, emeralds, sandstone and clay. Check centrifuge recipes: gravel → flint, clay → four clay balls, two moss → bone meal, four cactus → slime.
7. Recycle iron/gold tools and armor into three matching nuggets. Test damaged, named and enchanted items in a disposable test world; the whole item and its components are intentionally consumed. Other equipment must be rejected.
8. Pause, remove input, replace it with another item/component variant and reload recipes mid-process. Progress must reset for a changed recipe/input identity, but survive adding more identical input. Change datapack duration, energy, output and byproduct; confirm the next cycle uses the loaded recipe.
9. Fill outputs, then free capacity. Test partial stacks as well as empty slots. Break a machine while running, unload its chunk and restart the server: inventory drops exactly once on break; saved inventory, fuel, energy and valid processing progress survive reload. Broken machine items intentionally lose stored energy.

## Power and conduits

1. Check default combustion output at 40 FE/t and biomass at 20 FE/t. Test biomass fuel durations for sawdust, saplings, wheat, kelp and sugar cane; coal must not work in the biomass generator. Confirm fuel stops burning at full buffer.
2. Check solar output at 16 FE/t and advanced solar at 64 FE/t. Test day/night transitions, rain, a solid roof and a dimension without skylight. Removing the obstruction or restoring clear daylight should resume generation.
3. Route power through straight lines, branches and loops, then mix conduit endpoints with direct neighbors. A Technologia supplier must spend at most 200 FE/t across all recipients, including multiple faces of the same recipient. Check total energy before/after simulated and actual transfer.
4. Charge both the one-million-FE and five-million-FE cells. Generators may charge cells; cells must not charge one another through adjacency or conduit loops. A paused generator may still export its stored energy.
5. Disconnect branches and unload adjacent chunks. Routing must remain bounded to 128 conduits without forcing chunks to load. Test a continuously hungry endpoint alongside another consumer for starvation.
6. On each loader, connect representative external sources and sinks directly to machine energy adapters, then test conduit delivery from a Technologia source to an external sink. Conduits have no general external input buffer. Check FE simulation and cancelled/nested Fabric transactions.

## Nexus and item automation

1. Connect one, two and four cores to a terminal. Capacity must show 54, 108 and 216 slots respectively. Place items in the last slot of the fourth core and withdraw them through the terminal. Opening a core directly must show only that core's inventory.
2. Use two players and hoppers concurrently. Exercise stack/one/shift withdrawals, cursor deposits, full player inventories and repeated fast clicks while another viewer changes sorting or contents. Total item count must remain constant; named/component variants stay distinct.
3. Add, replace or remove a core with the terminal open, then reopen it. Old actions must be rejected and the new view must reflect the current capacity. Break cable/core/terminal, move out of range or unload a connection. No stale access is allowed. Five connected cores or more than 128 network blocks must fail closed without moving or deleting their contents.
4. Place Item Transfers between chests and machines in all six directions. Check rear extraction and arrow-side insertion, at most eight items per eight ticks. Test double chests, partially full slots and a completely full target; nothing may disappear or remain in an invisible transfer buffer.
5. Set a filter with a held sample, confirm the sample count is unchanged, and clear it with an empty hand. Test exact component matching with named items, then save/reload the filter. Apply redstone to pause transfer and remove it to resume.
6. Feed both alloy inputs with filtered transfers or hoppers and extract only outputs. Automatic insertion must not enter outputs; automatic extraction must not pull processor inputs or the miner's retained filter. Test separate input/output layouts after reopening each machine screen.
7. Use the wrench to rotate stocked machines and Item Transfers; inventories, energy and filters must remain intact. Crouch-use a machine to inspect energy. Verify miner ownership still applies. The wrench is not a dismantling tool.
8. Exercise representative external pipes and Create funnels/chutes on supported versions. Item Transfer currently supports vanilla `Container` and sided-container inventories, not capability-only third-party inventories. A kinetic bridge is not implemented.

## Miner and management

1. Place a miner above known ore. It starts paused. Power it, choose an optional ore/drop filter and start. Default area: 9×9, up to 32 blocks below it. Confirm its iron-pickaxe harvest limits and retained filter sample.
2. Confirm owner-only start/pause/rescan, owner-online requirement, dimension check, world border and spawn-protection behavior. Try break-cancellation and actual claimed regions from a protection mod.
3. Fill output, then free it. Ore must remain until every drop fits. Test changed loot tables and large stack counts. Disconnect the owner, restart, rescan and unload an adjacent chunk; the miner must not force-load chunks or mine protected blocks.
4. Set `minerEnabled=false` and verify it cannot resume. Corrupt the config and confirm the original file is preserved with logged fallback. Check `/technologia status` access and restart-required settings. Furnace config must not silently replace datapack processing costs.

## Models, interface and upgrades

1. Review all 17 machine types from every side, with active/inactive states and each facing. Inspect press openings, saw housing, solar panels, coils, miner assembly, recessed storage bays and terminal screen/keyboard. Check textures, depth, selection outlines, collision and item forms without missing models or obstructing the player's view unexpectedly.
2. Build with all ten decoration blocks. Check reinforced glass transparency, grille/grate depth, lamp level-15 light and consistent material colors. Decorative controls must not be presented as functional machine controls.
3. At small and large GUI scales, check both Alloy Smelter inputs, the output grid, inactive slots, progress bars, long status text, hover tooltips and keyboard focus. Confirm displayed energy above 32,767 and 65,535 and at the advanced cell's five-million-FE capacity.
4. Read all 32 Field Guide articles across nine chapters, including scrolling and keyboard navigation. Craft a guide, request it without operator permissions, repeat the request and try with a full inventory. Nexus search typing must not trigger hotbar shortcuts; empty searches should remain readable.
5. Open Minecraft Advancements, obtain each of the 20 workshop milestone items and verify persistence after rejoining. Milestones record possession, do not lock recipes and do not grant repeatable item rewards.
6. Back up alpha.1 and alpha.2 fixture worlds before upgrading. Existing IDs and saved inventory layout should remain intact; alpha.1 blocks acquire default facing. An older unfinished cycle may restart under the new processing state format without consuming another input. Verify no missing registry warnings or changed existing item counts.

## Server load and pack coverage

1. Profile tick time and memory for 10, 100 and 500 active machines, including branched conduits, multiple terminal viewers and Item Transfers. Record four-player contention before advertising factory scale.
2. Load dedicated servers without client rendering classes. Test all optional integrations absent, then individually, then in representative packs. Build success and interface checks do not establish blanket mod compatibility.
