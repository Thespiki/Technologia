# Expandable progression proposal

The five palette accents describe roles. They do not cap progression. This working proposal has **18 milestones**, T00–T17, and can expand after recipe and playtime balancing. These names and unlocks are proposals for the full mod, not implemented tiers.

The last column lists what the current version, alpha.5, already contains for a milestone's capability. "Built" means that the blocks and items exist and are obtained through ordinary recipes. The game has no milestone gates, and a milestone is not complete because one of its items exists.

| Tier | Name | New capability | Built so far (alpha.5) |
| --- | --- | --- | --- |
| T00 | Salvage | Sieving, manual resource loops, durable basic tools | Hand Sieve, three sieve meshes and compressed cobblestone, gravel and sand. Durable basic tools are not built |
| T01 | Workshop | Tool station, material choices, crates and early farming | Four crates and the Bonsai Pot. The tool station and material choices are not built |
| T02 | Mechanical | Water/wind power, presses and starter multiblocks | Water Wheel and Windmill. The Metal Press exists as a powered machine. Starter multiblocks are not built |
| T03 | Electrified | Coal power, cells, basic powered processing | Combustion and Biomass Generators, two solar generators, two energy cells, the Ore Crusher, the Electric Furnace and the other processors |
| T04 | Industrial | Ore doubling, fluids, automatic production lines | Ore doubling in the crusher; Item Transfers, hoppers and result ejection for production lines. Fluids are not built |
| T05 | Precision | Better circuits, multi-lane factories, efficient routing | Advanced Circuits, parallel lanes with the eight machine tiers, four conduit materials |
| T06 | Chemical | Refinery, gases, electrolysis, byproduct reuse | Not started. The only byproduct reuse so far is sawdust as biomass fuel |
| T07 | Digital | Server racks, item indexing and modular terminals | Storage Cores and a searchable terminal for up to four cores. Server racks are planned for alpha.6; modular terminals are not built |
| T08 | Automated | Pattern crafting, stock targets and controlled resource production | Not started |
| T09 | Nuclear | Fission, turbines, advanced heat and waste handling | Not started |
| T10 | Cryogenic | Superconductors, gas liquefaction and advanced materials | A Superconducting Energy Conduit exists. The rest is not started |
| T11 | Quantum | Long-range network links, spatial workshops and high-density storage | Not started. Wireless power exists since alpha.5, for energy only |
| T12 | Orbital | First space infrastructure and off-world manufacturing | Not started |
| T13 | Planetary | Demanding environments, new resource chains and outposts | Not started |
| T14 | Stellar | Fusion, large energy projects and extreme production | Not started |
| T15 | Draconic | Original high-end equipment, shields and fusion assembly | Not started. A Draconic Core exists as a creative-only concept item |
| T16 | Chaotic | Rift control, powerful encounters and exotic transformations | Not started. A Chaotic Core exists as a creative-only concept item |
| T17 | Singularity | Optional mastery projects and creative-scale objectives | Not started |

Draconic Evolution and Chaotic Evolution are both named inspirations for the late game. Which mod or addon “Chaotic Evolution” refers to is still to be confirmed. The T15 and T16 rows are placeholders for those inspirations. Their listed capabilities are proposals.

Parallel branches—farming, pneumatics, nature/magic, mob production and building—should have several useful entry points. The route must not force every player through every side system. Expert profiles can add cross-branch requirements without making that the default. Of these branches, alpha.5 has the first farming machines (Phyto Chamber, Auto Harvester, Growth Accelerator), the Resonance Bloom on the nature side, and 17 building blocks. Pneumatics and mob production are not started.

Alpha.5 content that the table does not name: the Thermoelectric Generator, the Enrichment Chamber and Metallurgic Infuser with their alloys, silver and nickel, the Chunk Loader, the Vacuum Collector, Vector Plates, three magnets and the Travel Staff. Where they sit in the milestone order is not decided.

Track global milestones separately from machine class, installed upgrades and material quality. Tier indicators use explicit T-numbers, names and small geometry changes; power/data/processing colors retain their meaning throughout.

## Machine tiers

Machine tiers are a separate scale from the milestones above. There are eight of them since alpha.4, defined in `technologia/tiers.json`; alpha.5 did not change their values. Every powered machine has a tier, except the Wireless Energy Sender and Receiver and the Creative Energy Source; storage cores and terminals have none either. A tier kit raises a placed machine by one tier. Since alpha.5 a machine above Mk I shows a band at the bottom of its front, in the colour of its tier kit, with one pip per tier number.

| Tier | Kit | Lanes | Speed | Energy capacity | Energy per operation | Generation | Transfer per tick |
| --- | --- | --- | --- | --- | --- | --- | --- |
| Mk I | none | 1 | 1× | 1× | 100% | 1× | 200 FE/t |
| Mk II | Bronze | 1 | 1.5× | 2× | 100% | 1.5× | 400 FE/t |
| Mk III | Steel | 2 | 1.5× | 3× | 95% | 2× | 800 FE/t |
| Mk IV | Resonant | 3 | 2× | 5× | 90% | 3× | 1,600 FE/t |
| Mk V | Hardened | 4 | 2.5× | 8× | 85% | 4× | 3,200 FE/t |
| Mk VI | Tempered | 5 | 3× | 12× | 80% | 6× | 6,400 FE/t |
| Mk VII | Entangled | 6 | 4× | 20× | 75% | 8× | 12,800 FE/t |
| Mk VIII | Stellar | 8 | 5× | 32× | 70% | 12× | 25,600 FE/t |

Lanes apply to processors only. Generation applies to generators, solar panels, the Water Wheel, the Windmill and the Thermoelectric Generator. Energy per operation is the cost of a whole operation compared with Mk I; it also applies to a harvest, a growth step, a collected stack and a loaded chunk. The number of machine tiers, their names and their values are current implementation choices and can change with balancing.

For the area machines of alpha.5 the tier also sets the area:

| Tier | Auto Harvester and Growth Accelerator | Vacuum Collector reach | Chunk Loader |
| --- | --- | --- | --- |
| Mk I | 5×5 blocks | 3 blocks | 1 chunk |
| Mk II | 5×5 blocks | 4 blocks | 1 chunk |
| Mk III | 7×7 blocks | 5 blocks | 3×3 chunks |
| Mk IV | 7×7 blocks | 6 blocks | 3×3 chunks |
| Mk V | 9×9 blocks | 7 blocks | 3×3 chunks |
| Mk VI | 9×9 blocks | 8 blocks | 5×5 chunks |
| Mk VII | 11×11 blocks | 9 blocks | 5×5 chunks |
| Mk VIII | 11×11 blocks | 10 blocks | 5×5 chunks |

The Auto Harvester and the Growth Accelerator work on three layers: their own level and one block above and below. A server can lower the Chunk Loader's area with `chunkLoaderRadius`. These areas are implementation choices too.
