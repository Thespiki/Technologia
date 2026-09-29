const features = [
 ['prototype','Power & processing','Coal power, energy cells, ore crushing and electric smelting form the first factory loop.','Workshop'],
 ['prototype','Survey Miner','A bounded selective miner with an ore/material filter, pause, rescan and owner checks.','Extraction'],
 ['prototype','Nexus core & terminal','Access a shared 54-slot inventory through connected terminals. Search and item aggregation come later.','Storage'],
 ['prototype','Tin, lead & resonite','New resources, world generation, drops, crafting recipes and common material tags.','Resources'],
 ['prototype','Shared machine interface','Common energy/progress display, inventory layout, status messages and input hints.','Interface'],
 ['prototype','Management & adapters','Server balance config, operator diagnostics, FE and Fabric energy adapters, sided inventories.','Compatibility'],
 ['planned','A real server room','Physical racks for storage managers, disks, crafting processors, power supplies and security services.','Nexus'],
 ['planned','Flexible channels','Capacity budgets depend on service use, cable materials, tiers and upgrades, including fractional displayed costs.','Nexus'],
 ['planned','Modular terminals','Item totals rather than stacks; install panels for crafting, fluids, gases and other network services.','Nexus'],
 ['planned','Autocrafting & stock','Tiered processors with upgrades, patterns, external-machine scheduling and understandable missing-input reports.','Nexus'],
 ['planned','Cables & subnetworks','Distinct item, fluid, gas, power and data routes; interfaces, P2P routing and isolated subnetworks.','Logistics'],
 ['planned','Redundant infrastructure','Power supplies, surge protection, backup storage and clear overload behavior.','Nexus'],
 ['planned','Distance without an arbitrary cap','Quantum-style links limited by throughput and capacity. Flux-style energy links have no tier ladder after unlock.','Wireless'],
 ['planned','Spatial workshops','Tiered compact rooms and spatial storage, with useful connections to the outside factory.','Spatial'],
 ['planned','Modular multitool','Material-based tool station, early durability, powered upgrades, radial modes, magnets, hammer and builder-wand modules.','Equipment'],
 ['planned','Armor & travel','Upgradeable equipment, shields and force fields, accessory slots, portable energy and ender-fueled travel.','Equipment'],
 ['planned','Sieves & compacted resources','Manual and automatic sieving, compacted blocks and accessible baseline tool interactions.','Resources'],
 ['planned','Farms & resource crops','Crop tiers, bonsai pots, crates, growers and harvesters. Resource seeds and upkeep remain expensive.','Cultivation'],
 ['planned','Purpose-built harvesters','Environmental-style multiblocks with distinct botanical, mineral and miscellaneous resource roles.','Resources'],
 ['planned','Chemistry & refining','Petroleum, refinery towers, electrolysis, enrichment, infusing, purification, condensation and gas processing.','Industry'],
 ['planned','Parallel factories','Upgrade production lanes, input/output capacity, precision and efficiency without rebuilding every workflow.','Industry'],
 ['planned','Pressure engineering','Pneumatic production and useful pressure-based automation alongside electrical machinery.','Industry'],
 ['planned','Power at every scale','Water, wind, thermoelectric and diesel generation; solar, gas, fission, turbines and later fusion.','Energy'],
 ['planned','Resource worlds','Mining dimensions with their own biomes/resources and balanced dimensional-efficiency upgrades.','Exploration'],
 ['planned','Space industry','Orbital and planetary progression, demanding environments, exclusive resources and off-world processing.','Exploration'],
 ['planned','Mob production','Approachable Woot-style multiblocks, mob processing, grinding utilities and meaningful upgrades.','Automation'],
 ['planned','Factory utility kit','RFTools-style builders, dye-linked ender chests/tanks, decorations, furniture and controlled chunk loading.','Utilities'],
 ['planned','Nature meets machinery','Selected Botania-inspired interactions, biological catalysts and mutually useful tech–nature production chains.','Nature'],
 ['planned','A guide that gets you there','Hotkey/book access, a non-admin book command, example setups, quest-like guidance and advancements.','Learning'],
 ['planned','Integration, by platform','Create, Curios, Storage Drawers, Cooking for Blockheads and recipe viewers, verified per loader and game version.','Compatibility'],
 ['planned','Draconic & chaotic endgame','Original modular equipment, fusion assembly, massive energy storage, rifts and powerful encounters.','Endgame'],
 ['proposed','Factory black box','Trace a stopped production line to its first bottleneck instead of guessing which machine failed.','Diagnostics'],
 ['proposed','Heat recovery & gas scrubbing','Turn waste streams into useful heat, materials or biological inputs across industrial branches.','Closed loops'],
 ['proposed','Outpost contracts','Set delivery targets between planetary factories and let logistics report missed or blocked shipments.','Space logistics'],
 ['proposed','Salvage & industrial ruins','Find damaged machinery and blueprints; recover useful materials from obsolete equipment.','Exploration'],
 ['proposed','Safety interlocks & rack UPS','Preview load, rehearse outages and protect critical services with readable priorities and breakers.','Engineering'],
 ['proposed','Maintenance drones','Small utility drones for inspection and repair, with server budgets and visible work queues.','Automation']
];
const tiers=[['Salvage','Sieving, recovered materials and manual tools.'],['Workshop','Tool stations, crates and material choices.'],['Mechanical','Water, wind, presses and starter multiblocks.'],['Electrified','Generators, portable cells and powered machines.'],['Industrial','Ore processing, fluids and production lines.'],['Precision','Circuits, parallel machines and better routing.'],['Chemical','Refining, gases, electrolysis and byproducts.'],['Digital','Server racks, storage indexes and modular terminals.'],['Automated','Autocrafting, stock targets and resource control.'],['Nuclear','Fission, turbines and engineered heat handling.'],['Cryogenic','Liquefaction, superconductors and advanced materials.'],['Quantum','Remote networks and spatial workshops.'],['Orbital','Space infrastructure and off-world industry.'],['Planetary','Demanding worlds and specialized outposts.'],['Stellar','Fusion and extreme manufacturing.'],['Draconic','Powerful equipment, shields and fusion assembly.'],['Chaotic','Rifts, encounters and exotic transformations.'],['Singularity','Optional mastery and creative-scale projects.']];
const grid=document.querySelector('#feature-grid');
let filter='all';
function element(tag,text,className){const el=document.createElement(tag);el.textContent=text;if(className)el.className=className;return el}
function render(){const query=document.querySelector('#search').value.toLowerCase().trim();const selected=features.filter(f=>(filter==='all'||f[0]===filter)&&f.join(' ').toLowerCase().includes(query));grid.replaceChildren();for(const [status,title,description,group] of selected){const card=element('article','','feature');card.append(element('span',status==='proposed'?'IDEA':status.toUpperCase(),'status '+status),element('h3',title),element('p',description),element('small',group));grid.append(card)}if(!selected.length)grid.append(element('p','No systems match this search. Try a different term or status.','empty'));document.querySelector('#results-count').textContent=`${selected.length} of ${features.length} systems and ideas`}
document.querySelector('#search').addEventListener('input',render);for(const button of document.querySelectorAll('[data-filter]'))button.addEventListener('click',()=>{filter=button.dataset.filter;for(const other of document.querySelectorAll('[data-filter]')){other.classList.toggle('selected',other===button);other.setAttribute('aria-pressed',String(other===button))}render()});
tiers.forEach(([name,description],i)=>{const card=element('article','','tier');card.append(element('span','T'+String(i).padStart(2,'0')),element('h3',name),element('p',description));document.querySelector('#tier-grid').append(card)});
for(const [id,name] of [['coal_generator','Generator'],['crusher','Crusher'],['electric_furnace','Furnace'],['digital_miner','Miner'],['storage_core','Nexus Core'],['storage_terminal','Terminal'],['energy_cell','Energy Cell']]){const card=element('div','','texture');const img=document.createElement('img');img.src=`./assets/block/${id}.png`;img.alt=`${name} prototype front texture`;img.width=80;img.height=80;img.loading='lazy';card.append(img,element('p',name));document.querySelector('#texture-gallery').append(card)}
render();
