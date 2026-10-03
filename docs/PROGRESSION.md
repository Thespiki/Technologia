# Expandable progression proposal

The five palette accents describe roles. They do not cap progression. This working proposal has **18 milestones**, T00–T17, and can expand after recipe and playtime balancing. These names and unlocks are proposals for the full mod, not implemented tiers.

| Tier | Name | New capability |
| --- | --- | --- |
| T00 | Salvage | Sieving, manual resource loops, durable basic tools |
| T01 | Workshop | Tool station, material choices, crates and early farming |
| T02 | Mechanical | Water/wind power, presses and starter multiblocks |
| T03 | Electrified | Coal power, cells, basic powered processing |
| T04 | Industrial | Ore doubling, fluids, automatic production lines |
| T05 | Precision | Better circuits, multi-lane factories, efficient routing |
| T06 | Chemical | Refinery, gases, electrolysis, byproduct reuse |
| T07 | Digital | Server racks, item indexing and modular terminals |
| T08 | Automated | Pattern crafting, stock targets and controlled resource production |
| T09 | Nuclear | Fission, turbines, advanced heat and waste handling |
| T10 | Cryogenic | Superconductors, gas liquefaction and advanced materials |
| T11 | Quantum | Long-range network links, spatial workshops and high-density storage |
| T12 | Orbital | First space infrastructure and off-world manufacturing |
| T13 | Planetary | Demanding environments, new resource chains and outposts |
| T14 | Stellar | Fusion, large energy projects and extreme production |
| T15 | Draconic | Original high-end equipment, shields and fusion assembly |
| T16 | Chaotic | Rift control, powerful encounters and exotic transformations |
| T17 | Singularity | Optional mastery projects and creative-scale objectives |

Draconic Evolution and Chaotic Evolution are both named inspirations for the late game. Which mod or addon “Chaotic Evolution” refers to is still to be confirmed. The T15 and T16 rows are placeholders for those inspirations. Their listed capabilities are proposals.

Parallel branches—farming, pneumatics, nature/magic, mob production and building—should have several useful entry points. The route must not force every player through every side system. Expert profiles can add cross-branch requirements without making that the default.

Track global milestones separately from machine class, installed upgrades and material quality. Tier indicators use explicit T-numbers, names and small geometry changes; power/data/processing colors retain their meaning throughout.

## Machine tiers in alpha.4

Machine tiers are a separate scale from the milestones above. Alpha.4 has eight of them, defined in `technologia/tiers.json`. Every powered machine has a tier; storage cores and terminals do not. A tier kit raises a placed machine by one tier.

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

Lanes apply to processors only. Generation applies to generators and solar panels. Energy per operation is the cost of a whole operation compared with Mk I. The number of machine tiers, their names and their values are current implementation choices and can change with balancing.
