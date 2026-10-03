// Deterministic, original 32px prototype art and Minecraft 1.21.1 data.
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {buildFactoryModels,cull,tierBand} from './factory-models.mjs';
import {buildFactoryShapes} from './factory-shapes.mjs';
// Paths are resolved from this file, so the generator gives the same output from any working directory.
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const base='common/src/main/resources';
// Every output is written exactly once per run: a second write would mean an earlier one was dead code.
const written=new Set();
const write=(p,s)=>{assert.ok(!written.has(p),`${p} is written twice`);written.add(p);const file=path.join(root,p);fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,s)};
const json=(p,o)=>write(`${base}/${p}.json`,JSON.stringify(o,null,2)+'\n');
const id=n=>'technologia:'+n;
const tiers=JSON.parse(fs.readFileSync(path.join(root,base,'technologia/tiers.json'),'utf8')).tiers;
const blocks=['machine_frame','coal_generator','crusher','electric_furnace','alloy_smelter','metal_press','sawmill','solar_generator','digital_miner','storage_core','storage_terminal','energy_cell','network_cable','energy_conduit','item_transfer','tin_ore','deepslate_tin_ore','lead_ore','deepslate_lead_ore','resonite_ore'];
const machines=['coal_generator','crusher','electric_furnace','alloy_smelter','metal_press','sawmill','solar_generator','digital_miner','storage_core','storage_terminal','energy_cell'];
const extraMachines=['compactor','centrifuge','recycler','biomass_generator','advanced_energy_cell','advanced_solar_generator','auto_sieve'];
// Alpha.5: power from the surroundings, wireless power, more processors and machines that work on the area around them.
const newMachines=['water_wheel','windmill','thermoelectric_generator','creative_energy_source','wireless_sender','wireless_receiver','enrichment_chamber','metallurgic_infuser','phyto_chamber','auto_harvester','growth_accelerator','vacuum_collector','chunk_loader'];
const decor=['steel_casing','bronze_casing','industrial_bricks','steel_grate','hazard_block','engineering_lamp','steel_pillar','ventilation_grille','control_panel','reinforced_glass','floor_panel','catwalk','steel_table','steel_stool','metal_shelf','tool_cabinet','warning_light'];
// Decor with a front face: placed facing the player, so its blockstate carries a 'facing' property.
const directionalDecor=['ventilation_grille','control_panel','metal_shelf','tool_cabinet'];
const conduits=['energy_conduit','gold_energy_conduit','resonite_energy_conduit','superconducting_energy_conduit'];
const compressed=['compressed_cobblestone','compressed_gravel','compressed_sand'];
const crates=['wooden_crate','bronze_crate','steel_crate','resonant_crate'];
const vectorPlates=['vector_plate','fast_vector_plate'];
// Blocks with one model per value of a numbered property; factory-models.mjs builds the models.
const devices={bonsai_pot:['stage',4],hand_sieve:['fill',5]};
blocks.push(...extraMachines,...newMachines,...decor,...conduits.slice(1),'resonance_bloom','silver_ore','deepslate_silver_ore','nickel_ore','deepslate_nickel_ore',...compressed,...crates,...Object.keys(devices),...vectorPlates);machines.push(...extraMachines,...newMachines);
const ores=blocks.filter(n=>n.endsWith('_ore'));
// Metals of this mod that are mined as ore, and metals that only come out of the Alloy Smelter.
const ownMetals=['tin','lead','silver','nickel'],alloys=['bronze','steel','electrum','invar','constantan'],vanillaMetals=['iron','gold','copper'];
const metalFragments=['iron_fragment','copper_fragment','gold_fragment','tin_fragment','lead_fragment','silver_fragment','nickel_fragment'];
const fragments=[...metalFragments,'redstone_fragment','lapis_fragment','diamond_fragment'];
// Sieve meshes by level, starting at level 1; level 0 is the string mesh every sieve starts with.
const meshes=['flint_mesh','iron_mesh','diamond_mesh'];
// One installer kit per tier after the first, straight from tiers.json.
const kits=tiers.slice(1).map(tier=>'tier_kit_'+tier.id);
const items=['wrench','raw_tin','raw_lead','tin_ingot','lead_ingot','bronze_ingot','steel_ingot','resonite','iron_dust','gold_dust','copper_dust','tin_dust','lead_dust','coal_dust','sawdust','iron_plate','copper_plate','gold_plate','bronze_plate','steel_plate','basic_circuit','advanced_circuit','draconic_core','chaotic_core','field_guide',...fragments,...kits,
 'raw_silver','raw_nickel','silver_ingot','nickel_ingot','electrum_ingot','invar_ingot','constantan_ingot','silver_dust','nickel_dust','invar_plate','electrum_plate','enriched_carbon','enriched_redstone','enriched_diamond','enriched_resonite','infused_alloy','reinforced_alloy','resonant_alloy',...meshes,'magnet','reinforced_magnet','resonant_magnet','travel_staff','creative_tier_kit'];
const palette={base:[27,39,53],dark:[12,18,27],rim:[65,82,103],light:[174,190,209],cyan:[85,217,208],amber:[232,180,106],violet:[176,141,245]};
// One accent colour per tier above the first: its kit icon and the tier band on a machine both use it.
const kitAccent={bronze:[201,144,76],steel:[150,168,186],resonant:palette.cyan,hardened:[126,200,255],tempered:[226,112,68],entangled:palette.violet,stellar:[250,236,160]};
const crc=b=>{let n=0xffffffff;for(const v of b){n^=v;for(let i=0;i<8;i++)n=(n>>>1)^((n&1)?0xedb88320:0)}return (n^0xffffffff)>>>0};
function png(name,paint){
 const px=Buffer.alloc(32*32*4); const rect=(x,y,w,h,c)=>{for(let yy=Math.max(0,y);yy<Math.min(32,y+h);yy++)for(let xx=Math.max(0,x);xx<Math.min(32,x+w);xx++){let i=(yy*32+xx)*4;px[i]=c[0];px[i+1]=c[1];px[i+2]=c[2];px[i+3]=c[3]??255}};
 paint(rect); const data=Buffer.alloc(32*(32*4+1));for(let y=0;y<32;y++)px.copy(data,y*129+1,y*128,(y+1)*128);
 const chunk=(name,b)=>{const type=Buffer.from(name),size=Buffer.alloc(4),check=Buffer.alloc(4);size.writeUInt32BE(b.length);check.writeUInt32BE(crc(Buffer.concat([type,b])));return Buffer.concat([size,type,b,check])};
 const ihdr=Buffer.alloc(13);ihdr.writeUInt32BE(32);ihdr.writeUInt32BE(32,4);ihdr[8]=8;ihdr[9]=6;
 write(`${base}/assets/technologia/textures/${name}.png`,Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',ihdr),chunk('IDAT',zlib.deflateSync(data)),chunk('IEND',Buffer.alloc(0))]));
}
const casing=(r)=>{r(0,0,32,32,palette.dark);r(1,1,30,30,palette.rim);r(2,2,28,28,palette.base);for(const x of [3,27])for(const y of [3,27])r(x,y,2,2,palette.light)};
png('block/casing',r=>{casing(r);for(let y=10;y<25;y+=4)r(8,y,16,2,palette.dark)});
png('block/top',r=>{casing(r);r(8,8,16,16,palette.dark);for(let x=9;x<24;x+=4)r(x,9,2,14,palette.rim)});
png('block/solar_top',r=>{casing(r);r(4,4,24,24,palette.dark);for(let y=5;y<28;y+=6)for(let x=5;x<28;x+=6){r(x,y,5,5,[32,73,96]);r(x,y,5,1,palette.cyan);r(x,y,1,5,[82,122,143])}});
for(const active of [false,true]) {
 const suffix=active?'_active':'';
 png('block/drive_bay'+suffix,r=>{r(0,0,32,32,palette.dark);r(1,3,30,26,palette.rim);r(3,5,26,22,palette.base);r(5,11,17,3,palette.light);r(5,17,12,2,palette.rim);r(25,10,3,5,active?palette.cyan:palette.rim);r(25,18,3,4,active?palette.light:palette.base)});
 png('block/terminal_screen'+suffix,r=>{r(0,0,32,32,palette.dark);r(1,1,30,30,active?[25,59,66]:[18,30,39]);for(let y=5;y<26;y+=6){r(4,y,3,3,active?palette.cyan:palette.rim);r(10,y,15-(y%4),2,active?palette.light:palette.rim)}r(27,4,1,24,palette.rim);r(27,4,1,9,active?palette.cyan:palette.base)});
}
png('block/cable_jacket',r=>{r(0,0,32,32,palette.dark);r(2,0,28,32,palette.base);r(4,0,3,32,palette.rim);r(24,0,3,32,palette.rim);r(14,0,4,32,palette.cyan);for(const y of [3,25]){r(2,y,28,4,palette.rim);r(14,y,4,4,palette.light)}});
png('block/cable_hub',r=>{casing(r);r(8,8,16,16,palette.dark);r(12,12,8,8,palette.cyan);r(14,14,4,4,palette.base)});
// Static cuboids give real depth without a per-frame block entity renderer.
const cuboid=(from,to,texture='#casing',front=null)=>({from,to,faces:Object.fromEntries(['down','up','north','south','west','east'].map(face=>[face,{texture:face==='north'&&front?front:texture,...(face==='north'&&front?{uv:[0,0,16,16]}:{}),...cull(face,from,to)}]))});
// Ores: speckled stone with mineral nuggets. Resonite only generates in the deepslate layer, so it shares the dark base.
// Silver and nickel are both pale, so each also has its own nugget layout.
const oreTint={tin:[180,213,209],lead:[136,133,172],silver:[226,234,252],nickel:[200,216,150]};
const oreSpots={silver:[[5,7],[17,4],[24,14],[10,16],[19,24],[4,25]],nickel:[[9,5],[23,7],[5,15],[16,14],[25,21],[11,25]]};
const oreMaterial=n=>n.replace('deepslate_','').replace('_ore','');
const paintOre=n=>r=>{const deep=n.startsWith('deepslate')||n==='resonite_ore';for(let y=0;y<32;y++)for(let x=0;x<32;x++){let q=(x*17+y*31+x*y*3)%19+(deep?43:100);r(x,y,1,1,[q,q+2,q+3])}const c=oreTint[oreMaterial(n)]??palette.cyan;for(const [x,y] of oreSpots[oreMaterial(n)]??[[7,6],[22,9],[12,18],[25,24],[3,26]]){r(x-1,y-1,5,4,palette.dark);r(x,y,4,3,c);r(x,y,2,1,palette.light)}};
// Processing machines glow amber while working; power and data machines glow cyan; wireless and creative blocks glow violet.
const heated=['coal_generator','crusher','electric_furnace','alloy_smelter','metal_press','sawmill','compactor','centrifuge','recycler','auto_sieve','thermoelectric_generator','enrichment_chamber','metallurgic_infuser','phyto_chamber','auto_harvester'];
const arcane=['creative_energy_source','wireless_sender','wireless_receiver','chunk_loader'];
const leaf=[86,160,84],frost=[176,206,226];
// One instrument per machine: every front texture is different, idle and active.
const paintFront=(n,active=false)=>r=>{
 casing(r);const c=active?(heated.includes(n)?palette.amber:arcane.includes(n)?palette.violet:palette.cyan):palette.rim;
 r(5,6,22,18,palette.dark);r(6,26,16,2,c);r(25,26,2,2,c);
 if(n==='crusher'){for(let y=9;y<23;y+=4){r(8,y,7,2,c);r(17,y+1,7,2,palette.light)}}
 else if(n==='alloy_smelter'){r(7,9,7,8,palette.rim);r(18,9,7,8,palette.rim);r(9,11,3,4,c);r(20,11,3,4,c);r(12,19,8,3,c);r(15,17,2,5,palette.light)}
 else if(n==='metal_press'){r(7,8,3,16,palette.light);r(22,8,3,16,palette.light);r(10,9,12,4,palette.rim);r(14,13,4,5,c);r(10,20,12,3,c);r(11,18,10,2,palette.light)}
 else if(n==='sawmill'){r(7,19,18,4,[155,105,68]);r(8,20,16,1,palette.amber);r(12,9,8,10,palette.light);r(9,12,14,4,palette.rim);r(15,10,2,8,palette.dark);r(23,9,2,7,c)}
 else if(n==='solar_generator'){r(9,10,14,8,palette.rim);r(11,11,10,5,c);r(15,7,2,3,palette.light);r(15,18,2,3,palette.light);r(6,13,3,2,palette.light);r(23,13,3,2,palette.light)}
 else if(n==='advanced_solar_generator'){for(const x of [8,17])for(const y of [8,16]){r(x,y,7,6,palette.rim);r(x+1,y+1,5,4,c)}r(15,7,2,16,palette.light)}
 else if(n==='item_transfer'){r(7,11,18,10,palette.rim);r(10,14,12,4,palette.amber);r(17,11,3,10,palette.amber);r(20,13,3,6,palette.amber);r(23,15,2,2,palette.amber)}
 else if(n==='coal_generator'){r(9,12,14,10,palette.rim);r(11,15,10,5,c);r(14,10,4,10,c);for(const x of [11,15,19])r(x,21,2,1,palette.dark);r(12,8,8,2,palette.light)}
 else if(n==='electric_furnace'){r(8,9,16,13,palette.rim);r(10,11,12,9,palette.dark);for(const y of [12,15,18])r(11,y,10,1,c);r(20,13,1,2,c);r(11,16,1,2,c);r(8,22,16,1,palette.light)}
 else if(n==='biomass_generator'){r(9,10,14,12,palette.rim);r(13,11,6,2,leaf);r(11,13,10,5,leaf);r(13,18,6,2,leaf);r(15,11,2,10,c)}
 else if(n==='digital_miner'){r(15,8,2,14,c);r(9,14,14,2,c);r(11,10,10,2,palette.rim);r(11,19,10,2,palette.rim)}
 else if(n==='energy_cell'){r(10,9,12,13,palette.rim);for(let y=11;y<22;y+=4)r(12,y,8,2,c)}
 else if(n==='advanced_energy_cell'){for(const x of [7,17]){r(x,8,8,14,palette.rim);for(let y=10;y<21;y+=4)r(x+2,y,4,2,c)}r(15,12,2,6,palette.violet)}
 else if(n==='storage_core'){for(let y=9;y<23;y+=5){r(8,y,16,3,palette.rim);r(21,y,2,2,c)}}
 else if(n==='storage_terminal'){r(8,9,16,12,[34,76,83]);for(let y=11;y<19;y+=3)r(10,y,7+(y%2)*4,1,c);r(11,23,10,1,palette.light)}
 else if(n==='compactor'){r(7,8,18,3,palette.light);r(7,21,18,3,palette.light);r(10,11,12,3,palette.rim);r(10,18,12,3,palette.rim);r(13,14,6,4,c)}
 else if(n==='centrifuge'){r(11,8,10,15,palette.rim);r(8,11,16,9,palette.rim);r(12,11,8,9,palette.dark);r(15,9,2,13,c);r(9,15,14,2,c);r(15,15,2,2,palette.light)}
 else if(n==='recycler'){r(8,9,14,2,c);r(20,11,2,6,c);r(18,15,6,2,c);r(10,20,14,2,palette.light);r(10,14,2,6,palette.light);r(8,14,6,2,palette.light);r(14,13,4,4,palette.rim)}
 else if(n==='auto_sieve'){r(7,8,18,2,palette.light);for(let x=8;x<25;x+=4)r(x,10,1,7,palette.rim);for(const y of [12,15])r(7,y,18,1,palette.rim);for(const [x,y] of [[10,19],[15,21],[20,19]])r(x,y,2,2,c)}
 // Alpha.5 instruments. Wheel over water; pinwheel; cold bars beside hot bars, on the same sides as the two halves of the model; an endless loop.
 else if(n==='water_wheel'){r(10,8,12,2,palette.rim);r(10,19,12,2,palette.rim);r(8,10,2,9,palette.rim);r(22,10,2,9,palette.rim);r(15,10,2,9,c);r(10,14,12,1,c);r(14,13,4,3,palette.light);for(const x of [6,12,18,24])r(x,22,3,1,c)}
 else if(n==='windmill'){r(15,8,2,6,c);r(17,8,3,4,palette.light);r(18,14,6,2,c);r(20,16,4,3,palette.light);r(15,16,2,6,c);r(12,18,3,4,palette.light);r(8,14,6,2,c);r(8,11,4,3,palette.light);r(14,13,4,4,palette.rim);r(15,14,2,2,c)}
 else if(n==='thermoelectric_generator'){for(const y of [9,12,15,18]){r(7,y,7,2,frost);r(18,y,7,2,c)}r(15,8,2,13,palette.light);r(8,21,5,1,frost);r(19,21,5,1,c)}
 else if(n==='creative_energy_source'){for(const x of [7,17]){r(x,10,8,9,c);r(x+2,12,4,5,palette.dark)}r(14,13,4,3,palette.light);r(15,8,2,2,c);r(15,20,2,2,c)}
 // Mast with waves going up; dish with a ring around its feed.
 else if(n==='wireless_sender'){r(15,13,2,8,palette.light);r(11,21,10,2,palette.rim);r(15,12,2,1,c);r(13,10,6,1,c);r(12,11,1,2,c);r(19,11,1,2,c);r(11,8,10,1,c);r(10,9,1,3,c);r(21,9,1,3,c)}
 else if(n==='wireless_receiver'){r(8,14,16,2,palette.light);r(10,16,12,2,palette.light);r(13,18,6,2,palette.rim);r(15,20,2,3,palette.rim);r(15,10,2,4,c);r(12,8,8,1,c);r(12,12,3,1,c);r(17,12,3,1,c);r(11,9,1,3,c);r(20,9,1,3,c)}
 // Banded drum with a pellet in its window; two hoppers dripping into a crucible; a plant under a lamp.
 else if(n==='enrichment_chamber'){r(9,8,14,15,palette.rim);r(8,10,16,1,palette.light);r(8,20,16,1,palette.light);r(12,12,8,7,palette.dark);r(13,13,6,5,c);r(15,14,2,3,palette.light)}
 else if(n==='metallurgic_infuser'){for(const x of [7,18]){r(x,8,7,3,palette.light);r(x+2,11,3,2,palette.light);r(x+3,13,1,3,c)}r(8,16,16,7,palette.rim);r(10,17,12,3,c)}
 else if(n==='phyto_chamber'){r(8,8,16,2,c);r(11,20,10,3,palette.rim);r(15,12,2,8,leaf);r(11,13,4,3,leaf);r(17,15,4,3,leaf);r(7,8,1,15,palette.light);r(24,8,1,15,palette.light)}
 // Wheat in front of a cutter bar; a sprout between two arrows; a funnel over a bin; a chunk map with the middle lit.
 else if(n==='auto_harvester'){for(const x of [8,13,18]){r(x+1,11,1,8,leaf);r(x,8,3,4,palette.amber)}r(7,20,18,2,palette.light);for(let x=8;x<24;x+=4)r(x,19,2,1,c);r(22,9,3,7,c)}
 else if(n==='growth_accelerator'){r(9,21,14,2,palette.rim);r(15,14,2,7,leaf);r(12,15,3,2,leaf);r(17,13,3,2,leaf);for(const x of [8,22]){r(x,10,2,9,c);r(x-1,11,4,1,c)}}
 else if(n==='vacuum_collector'){r(7,8,18,2,palette.light);r(9,10,14,2,palette.light);r(11,12,10,2,palette.light);r(14,14,4,4,palette.rim);r(10,19,12,4,palette.rim);r(12,20,8,2,c);r(15,8,2,6,c)}
 else if(n==='chunk_loader'){for(let i=0;i<9;i++)r(8+6*(i%3),8+5*Math.floor(i/3),5,4,i===4?c:palette.rim);r(16,14,1,2,palette.light)}
 else {r(12,10,8,10,palette.rim);r(14,12,4,6,c)}
};
// Compressed blocks: the vanilla material, darker, held in a riveted metal frame so it reads as packed.
const packed={compressed_cobblestone:[84,84,88],compressed_gravel:[92,84,82],compressed_sand:[168,150,104]};
const paintCompressed=n=>r=>{
 // Cobblestone is drawn as small stones with dark joints, gravel as coarse speckle, sand as fine ripples.
 const grain=n.endsWith('cobblestone')?(x,y)=>x%4===0||y%4===(x>>2)%2*2?-22:((x>>2)*5+(y>>2)*9+(x>>2)*(y>>2))%7*4:n.endsWith('gravel')?(x,y)=>(x*17+y*31+x*y*3)%23:(x,y)=>(x*3+y*7)%5*3+(y%4===0?-10:0);
 for(let y=0;y<32;y++)for(let x=0;x<32;x++)r(x,y,1,1,packed[n].map(v=>v+grain(x,y)));
 for(const t of [0,30]){r(0,t,32,2,palette.rim);r(t,0,2,32,palette.rim)}
 for(const t of [2,29]){r(2,t,28,1,palette.dark);r(t,2,1,28,palette.dark)}
 for(const x of [0,27])for(const y of [0,27]){r(x,y,5,5,palette.rim);r(x+1,y+1,3,3,palette.light);r(x+2,y+2,1,1,palette.dark)}
};
// Crates: plank or metal panels in a braced frame, with a lid line and a latch; the top shows the lid with a cross brace.
const crateSkin={
 wooden_crate:{panel:[150,110,68],seam:[112,80,48],brace:[98,70,42],rivet:[176,134,86]},
 bronze_crate:{panel:[168,118,62],seam:[120,84,44],brace:[201,144,76],rivet:[240,206,150]},
 steel_crate:{panel:[104,122,140],seam:[72,88,104],brace:[150,168,186],rivet:[224,230,238]},
 resonant_crate:{panel:[30,44,60],seam:[20,30,42],brace:palette.cyan,rivet:palette.violet}
};
const paintCrate=(n,top)=>r=>{
 const {panel,seam,brace,rivet}=crateSkin[n];
 r(0,0,32,32,panel);for(let y=7;y<32;y+=6)r(0,y,32,1,seam);
 if(top){r(13,0,6,32,brace);r(0,13,32,6,brace);r(14,14,4,4,rivet)}
 else {r(3,9,26,1,palette.dark);r(14,7,4,6,rivet);r(15,9,2,2,palette.dark)}
 for(const t of [0,29]){r(0,t,32,3,brace);r(t,0,3,32,brace)}
 for(const x of [0,26])for(const y of [0,26]){r(x,y,6,6,brace);r(x+2,y+2,2,2,rivet)}
};
const turns={north:0,east:90,south:180,west:270};
// Blocks that only turn: their one model is rotated to the 'facing' property.
const facingBlocks=[...directionalDecor,...vectorPlates];
// Cables, conduits, the item transfer and the bloom get their blockstate and model in their own sections below.
const custom=['network_cable',...conduits,'item_transfer','resonance_bloom'];
const bands=tiers.slice(1).map((tier,index)=>index+1);
const part=(when,model,y)=>({when,apply:{model,...(y?{y}:{})}});
for(const n of blocks){
 const ore=ores.includes(n),model=`technologia:block/${n}`;
 if(machines.includes(n)) {
  // Models come from factory-models.mjs; only the instrument textures and the blockstate are made here.
  for(const active of [false,true])png('block/'+n+(active?'_active':''),paintFront(n,active));
  // Every machine block has a 'tier' property, so a variants list would need every combination of facing, active
  // and tier. Multipart keeps it to one base part per facing and active, plus one tier band part per tier and facing.
  json(`assets/technologia/blockstates/${n}`,{multipart:[
   ...Object.entries(turns).flatMap(([facing,y])=>[false,true].map(active=>part({facing,active:String(active)},model+(active?'_active':''),y))),
   ...bands.flatMap(band=>Object.entries(turns).map(([facing,y])=>part({tier:String(band),facing},`technologia:block/tier_band_${band}`,y)))]});
 } else if(facingBlocks.includes(n))json(`assets/technologia/blockstates/${n}`,{variants:Object.fromEntries(Object.entries(turns).map(([facing,y])=>[`facing=${facing}`,{model,...(y?{y}:{})}]))});
 else if(n in devices){const [property,count]=devices[n];json(`assets/technologia/blockstates/${n}`,{variants:Object.fromEntries(Array.from({length:count},(_,value)=>[`${property}=${value}`,{model:model+(value?'_'+value:'')}]))});}
 else if(!custom.includes(n))json(`assets/technologia/blockstates/${n}`,{variants:{'':{model}}});
 if(ore){png('block/'+n,paintOre(n));json(`assets/technologia/models/block/${n}`,{parent:'minecraft:block/cube_all',textures:{all:model}});}
 if(compressed.includes(n)){png('block/'+n,paintCompressed(n));json(`assets/technologia/models/block/${n}`,{parent:'minecraft:block/cube_all',textures:{all:model}});}
 if(crates.includes(n)){png('block/'+n,paintCrate(n,false));png('block/'+n+'_top',paintCrate(n,true));json(`assets/technologia/models/block/${n}`,{parent:'minecraft:block/cube_column',textures:{end:model+'_top',side:model}});}
 if(n==='machine_frame'){png('block/'+n,paintFront(n));json(`assets/technologia/models/block/${n}`,{parent:'minecraft:block/cube',textures:{particle:model,down:'technologia:block/casing',up:'technologia:block/top',north:model,south:'technologia:block/casing',east:'technologia:block/casing',west:'technologia:block/casing'}});}
 json(`assets/technologia/models/item/${n}`,n==='resonance_bloom'?{parent:'minecraft:item/generated',textures:{layer0:model}}:{parent:model});
 const drop=!ore?n:n==='resonite_ore'?'resonite':'raw_'+oreMaterial(n);
 const entry=ore?{type:'minecraft:alternatives',children:[{type:'minecraft:item',name:id(n),conditions:[{condition:'minecraft:match_tool',predicate:{predicates:{'minecraft:enchantments':[{enchantments:'minecraft:silk_touch',levels:{min:1}}]}}}]},{type:'minecraft:item',name:id(drop),functions:[{function:'minecraft:apply_bonus',enchantment:'minecraft:fortune',formula:'minecraft:ore_drops'},{function:'minecraft:explosion_decay'}]}]}:{type:'minecraft:item',name:id(drop),...(crates.includes(n)?{functions:[{function:'minecraft:copy_components',source:'block_entity',include:['minecraft:custom_name']}]}:{})};
 json(`data/technologia/loot_table/blocks/${n}`,{type:'minecraft:block',pools:[{rolls:1,entries:[entry],...(!ore&&!machines.includes(n)?{conditions:[{condition:'minecraft:survives_explosion'}]}:{})}]});
}
// Tier band: a strip in the tier's colour between the front feet of a machine. It carries the tier number as
// pips (Mk II shows two). The front is drawn at twice the usual pixel density so that eight pips fit.
for(const band of bands){
 const c=kitAccent[tiers[band].id]??palette.light,name=`technologia:block/tier_band_${band}`;
 png(`block/tier_band_${band}`,r=>{r(0,0,32,32,c);r(0,0,32,1,c.map(v=>Math.round(v+(255-v)*.5)));r(0,7,32,1,c.map(v=>Math.round(v*.6)));for(let i=0;i<=band;i++)r(15-2*band+4*i,2,2,4,palette.dark)});
 const face=(side,uv)=>({texture:'#band',uv,...cull(side,tierBand.from,tierBand.to)});
 // No south face: the foot of the machine is right behind the band.
 json(`assets/technologia/models/block/tier_band_${band}`,{textures:{particle:name,band:name},elements:[{...tierBand,faces:{down:face('down',[0,8,16,10]),up:face('up',[0,8,16,10]),north:face('north',[0,0,16,4]),west:face('west',[0,8,2,12]),east:face('east',[0,8,2,12])}}]});
}
// Vector plates: a thin plate whose chevrons point north in the model, so the blockstate turns them to 'facing'.
for(const n of vectorPlates){
 const fast=n.startsWith('fast'),c=fast?palette.amber:palette.cyan,name=`technologia:block/${n}`,from=[0,0,0],to=[16,1,16];
 png('block/'+n,r=>{r(0,0,32,32,palette.dark);r(1,1,30,30,fast?[104,80,50]:palette.rim);r(3,3,26,26,fast?[52,42,32]:palette.base);for(const y of fast?[4,8,16,20]:[8,18])for(let i=0;i<7;i++){r(15-i,y+i,2,2,c);r(15+i,y+i,2,2,c)}});
 // The four edges show the bottom rows of the texture, the rim of the plate.
 const face=(side,uv)=>({texture:'#plate',uv,...cull(side,from,to)});
 json(`assets/technologia/models/block/${n}`,{parent:'minecraft:block/thin_block',textures:{particle:name,plate:name},elements:[{from,to,faces:{down:face('down',[0,0,16,16]),up:face('up',[0,0,16,16]),...Object.fromEntries(['north','south','west','east'].map(side=>[side,face(side,[0,15,16,16])]))}}]});
}
// Six small arm models avoid a dynamic renderer and match PipeBlock's cached shapes.
const cableElement=(from,to,texture)=>({from,to,faces:Object.fromEntries(['down','up','north','south','west','east'].map(face=>[face,{texture,uv:[0,0,16,16],...cull(face,from,to)}]))});
const cableHub=cableElement([5,5,5],[11,11,11],'#hub');
const cableArms={north:[[5,5,0],[11,11,5]],south:[[5,5,11],[11,11,16]],east:[[11,5,5],[16,11,11]],west:[[0,5,5],[5,11,11]],up:[[5,11,5],[11,16,11]],down:[[5,0,5],[11,5,11]]};
// One core model, six arm models, an inventory model and the multipart blockstate for a cable-shaped block.
const pipe=(n,skin)=>{
 const model=elements=>({parent:'minecraft:block/block',textures:{particle:`technologia:block/${skin}_jacket`,jacket:`technologia:block/${skin}_jacket`,hub:`technologia:block/${skin}_hub`},elements});
 json(`assets/technologia/models/block/${n}_core`,model([cableHub]));
 for(const [direction,[from,to]] of Object.entries(cableArms))json(`assets/technologia/models/block/${n}_${direction}`,model([cableElement(from,to,'#jacket')]));
 json(`assets/technologia/models/block/${n}`,model([cableHub,...['north','south'].map(direction=>cableElement(...cableArms[direction],'#jacket'))]));
 json(`assets/technologia/blockstates/${n}`,{multipart:[{apply:{model:`technologia:block/${n}_core`}},...Object.keys(cableArms).map(direction=>({when:{[direction]:'true'},apply:{model:`technologia:block/${n}_${direction}`}}))]});
};
pipe('network_cable','cable');
// Conduit grades share one shape; the two jacket stripes tell them apart: amber, warm yellow, cyan and violet, pale blue and white.
const conduitStripes={energy_conduit:[palette.amber,palette.amber],gold_energy_conduit:[[255,208,64],[255,232,138]],resonite_energy_conduit:[palette.cyan,palette.violet],superconducting_energy_conduit:[[150,204,255],[244,250,255]]};
for(const n of conduits){
 const skin=n.replace('energy_conduit','conduit'),[first,second]=conduitStripes[n];
 png(`block/${skin}_jacket`,r=>{r(0,0,32,32,palette.dark);r(3,0,26,32,palette.base);r(7,0,3,32,first);r(22,0,3,32,second);for(const y of [3,25]){r(2,y,28,4,palette.rim);r(12,y,8,4,palette.light)}});
 png(`block/${skin}_hub`,r=>{casing(r);r(6,6,20,20,palette.dark);r(14,7,7,3,first);r(11,10,7,7,first);r(14,16,7,3,second);r(12,19,5,6,second)});
 pipe(n,skin);
}
png('block/item_transfer',paintFront('item_transfer'));
json('assets/technologia/models/block/item_transfer',{parent:'minecraft:block/block',textures:{particle:'technologia:block/casing',casing:'technologia:block/casing',detail:'technologia:block/item_transfer'},elements:[cuboid([3,3,3],[13,13,13]),cuboid([5,5,13],[11,11,16]),cuboid([4,4,0],[12,12,3],'#casing','#detail')]});
json('assets/technologia/blockstates/item_transfer',{variants:Object.fromEntries(Object.entries({north:{},east:{y:90},south:{y:180},west:{y:270},up:{x:270},down:{x:90}}).map(([facing,rotation])=>[`facing=${facing}`,{model:'technologia:block/item_transfer',...rotation}]))});
// A crossed-plane plant on a transparent background: green stem and leaves, a glowing cyan flower and a bud.
png('block/resonance_bloom',r=>{
 const stem=[58,132,70],shade=[42,104,58],glow=[214,255,248];
 r(15,14,2,17,stem);r(16,14,1,17,shade);r(9,23,6,3,leaf);r(7,21,4,3,leaf);r(8,22,5,1,stem);r(17,25,7,3,leaf);r(22,23,4,3,leaf);r(18,26,6,1,stem);
 r(23,13,1,6,stem);r(22,10,3,3,palette.cyan);r(23,11,1,1,glow);
 r(13,3,6,2,palette.cyan);r(11,5,10,7,palette.cyan);r(13,12,6,2,palette.cyan);r(10,7,1,3,palette.cyan);r(21,7,1,3,palette.cyan);
 for(const [x,y] of [[11,5],[19,5],[11,10],[19,10]])r(x,y,2,2,[150,240,232]);r(14,6,4,5,glow);r(15,7,2,3,palette.violet);
});
json('assets/technologia/blockstates/resonance_bloom',{variants:{'':{model:'technologia:block/resonance_bloom'}}});
json('assets/technologia/models/block/resonance_bloom',{parent:'minecraft:block/cross',render_type:'minecraft:cutout',textures:{cross:'technologia:block/resonance_bloom'}});
// Each metal has its own colour, so no two icons of one shape come out the same.
const steel=[132,151,170],white=[246,244,240];
const tint=n=>n.includes('electrum')?[244,226,128]:n.includes('invar')?[152,172,154]:n.includes('constantan')?[228,156,150]:n.includes('silver')?oreTint.silver:n.includes('nickel')?oreTint.nickel:n.includes('redstone')?[214,52,40]:n.includes('lapis')?[52,92,196]:n.includes('diamond')?[110,226,222]:n.includes('carbon')?[58,64,74]
 :n.includes('coal')?[70,78,88]:n==='sawdust'?[188,137,78]:n.includes('steel')?steel:n.includes('bronze')?[201,144,76]:n.includes('gold')?palette.amber:n.includes('copper')?[221,139,99]:n.includes('lead')?[148,138,186]:n.includes('tin')?[180,213,209]:n.includes('iron')?[214,202,190]:n.includes('chaotic')?palette.violet:n.includes('draconic')?[236,136,73]:n==='advanced_circuit'?palette.violet:n.includes('circuit')||n.endsWith('resonite')?palette.cyan:palette.light;
// Rock chips per fragment as [x,y,width,height]: two to four, placed differently for every material.
const chips={iron_fragment:[[6,16,8,7],[17,9,7,6],[18,20,6,5]],copper_fragment:[[6,10,9,8],[16,19,9,7]],gold_fragment:[[5,9,7,6],[13,17,9,8],[22,8,5,5]],tin_fragment:[[8,17,10,8],[19,8,7,7]],lead_fragment:[[6,7,8,7],[7,19,7,6],[17,13,9,8]],
 silver_fragment:[[9,6,8,6],[5,16,7,7],[16,17,9,8]],nickel_fragment:[[5,12,8,8],[16,6,8,6],[17,17,7,7]],redstone_fragment:[[12,5,7,6],[6,13,6,6],[16,14,8,7],[9,22,6,5]],lapis_fragment:[[7,7,10,7],[19,16,7,8],[8,18,7,6]],diamond_fragment:[[11,6,9,9],[7,19,6,6],[18,18,7,7]]};
// What an enriched pellet holds at its heart, and the two halves of an alloy's core.
const hearts={enriched_carbon:[20,24,30],enriched_redstone:[255,170,150],enriched_diamond:[240,252,252],enriched_resonite:palette.violet};
const alloyCores={infused_alloy:[[214,52,40],[255,150,120]],reinforced_alloy:[[150,204,255],[244,250,255]],resonant_alloy:[palette.cyan,palette.violet]};
const meshStyle={flint_mesh:[[86,92,100],6],iron_mesh:[[204,208,212],4],diamond_mesh:[[110,226,222],3]};
const magnetStyle={magnet:[[206,58,52],[230,236,240]],reinforced_magnet:[steel,palette.amber],resonant_magnet:[palette.cyan,palette.violet]};
for(const n of items){
 const kit=kits.indexOf(n),creative=n==='creative_tier_kit',c=creative?[236,96,214]:kit<0?tint(n):kitAccent[tiers[kit+1].id]??palette.light;
 png('item/'+n,r=>{
  if(n==='wrench'){r(13,13,6,15,palette.rim);r(8,4,5,10,palette.light);r(19,4,5,10,palette.light);r(9,11,14,6,palette.light);r(14,22,4,3,palette.cyan)}
  else if(n==='field_guide'){r(5,4,22,25,palette.dark);r(7,5,19,21,palette.rim);r(9,7,15,16,palette.base);r(7,26,18,2,palette.light);r(7,5,2,21,palette.amber);r(15,10,4,10,palette.cyan);r(12,13,10,4,palette.cyan)}
  else if(n.endsWith('dust')){r(7,21,18,4,palette.dark);r(9,17,14,6,c);r(13,13,6,5,c);r(5,24,4,2,c);r(25,21,3,2,c)}
  else if(n.endsWith('plate')){r(5,9,22,16,palette.dark);r(7,10,18,12,c);r(8,10,16,2,palette.light);r(9,22,16,2,palette.rim);for(const x of [8,22])for(const y of [13,19])r(x,y,1,1,palette.dark)}
  else if(n.endsWith('fragment')){const low=c.map(v=>Math.round(v*.68));for(const [x,y,w,h] of chips[n]){r(x-1,y-1,w+2,h+2,palette.dark);r(x,y,w,h,c);r(x+2,y+h-2,w-2,2,low);r(x,y,w-3,1,white);r(x+w-2,y,2,2,palette.dark);r(x,y+h-1,1,1,palette.dark)}}
  // A module plate with a tier-coloured header; the tier number (Mk II = 2) is shown as that many pips.
  // The creative kit has every pip, in magenta under a violet header.
  else if(kit>=0||creative){r(4,5,24,22,palette.dark);r(5,6,22,20,palette.rim);r(7,8,18,16,palette.base);r(7,8,18,3,creative?palette.violet:c);for(const x of [8,14,20])r(x,27,4,2,palette.light);for(let i=0;i<(creative?8:kit+2);i++)r(9+4*(i%4),14+4*Math.floor(i/4),3,3,c)}
  // Enriched materials: a cut gem in the material colour around its heart.
  else if(n.startsWith('enriched')){for(let i=0;i<6;i++)r(14-2*i,5+2*i,4+4*i,22-4*i,palette.dark);for(let i=0;i<5;i++)r(15-2*i,6+2*i,2+4*i,20-4*i,c);r(13,13,6,6,hearts[n]);r(11,15,10,2,hearts[n]);r(15,7,2,3,white);r(10,13,2,2,white)}
  // Alloys: a steel ingot with a window on its coloured core.
  else if(n.endsWith('alloy')){r(5,14,22,10,palette.dark);r(7,13,18,8,steel);r(10,10,14,4,steel);r(10,11,12,2,[225,233,238]);r(10,15,12,4,palette.dark);r(11,16,5,2,alloyCores[n][0]);r(16,16,5,2,alloyCores[n][1])}
  // Meshes: a wooden frame around a grid; a better mesh has a finer grid.
  else if(n.endsWith('mesh')){const [wire,pitch]=meshStyle[n];r(3,3,26,26,palette.dark);r(4,4,24,24,[150,110,68]);r(4,4,24,1,[176,134,86]);r(7,7,18,18,palette.dark);for(let t=8;t<25;t+=pitch){r(t,7,1,18,wire);r(7,t,18,1,wire)}}
  // Horseshoe magnets: coloured arms with bare pole tips.
  else if(n.endsWith('magnet')){const [arm,tip]=magnetStyle[n];r(5,4,9,23,palette.dark);r(18,4,9,23,palette.dark);r(5,18,22,10,palette.dark);r(6,5,7,21,arm);r(19,5,7,21,arm);r(6,19,20,8,arm);r(8,24,16,1,arm.map(v=>Math.round(v*.68)));for(const x of [6,19]){r(x,5,7,5,tip);r(x,10,7,1,palette.dark)}}
  // A blaze-rod staff from corner to corner, with an ender-green head.
  else if(n==='travel_staff'){for(let i=0;i<9;i++)r(4+2*i,24-2*i,4,4,palette.dark);for(let i=0;i<9;i++)r(5+2*i,25-2*i,2,2,palette.amber);r(19,3,10,10,palette.dark);r(20,4,8,8,[46,170,130]);r(22,6,4,4,[150,240,200]);r(23,7,1,1,white)}
  // The advanced board is violet with two chips and pins on all four sides.
  else if(n==='advanced_circuit'){r(5,5,22,22,palette.dark);r(7,7,18,18,[52,44,84]);r(9,9,6,6,c);r(17,17,6,6,c);r(17,10,5,4,palette.cyan);r(10,18,4,4,palette.amber);r(15,12,2,8,palette.light);for(let t=8;t<24;t+=4){r(3,t,2,2,palette.amber);r(27,t,2,2,palette.amber);r(t,3,2,2,palette.amber);r(t,27,2,2,palette.amber)}}
  else if(n.includes('circuit')){r(6,6,20,20,palette.dark);r(8,8,16,16,[37,75,72]);r(12,12,8,8,c);for(let x=10;x<25;x+=5){r(x,4,2,3,palette.amber);r(x,25,2,3,palette.amber)}}
  else if(n.endsWith('core')){r(11,4,10,24,palette.dark);r(5,10,22,12,palette.dark);r(10,8,12,16,c);r(7,12,18,8,c);r(13,12,6,8,[246,244,240])}
  else if(n.endsWith('ingot')){r(5,14,22,10,palette.dark);r(7,13,18,8,c);r(10,10,14,4,c);r(10,11,12,2,[225,233,238])}
  else {r(12,5,8,22,palette.dark);r(7,12,18,10,palette.dark);r(13,7,6,18,c);r(9,13,14,7,c);r(13,8,2,9,[226,238,244])}
 });
 json(`assets/technologia/models/item/${n}`,{parent:'minecraft:item/generated',textures:{layer0:`technologia:item/${n}`}});
}
const title=n=>n.split('_').map(s=>s[0].toUpperCase()+s.slice(1)).join(' ');
const names={coal_generator:'Combustion Generator',crusher:'Ore Crusher',electric_furnace:'Electric Furnace',alloy_smelter:'Alloy Smelter',metal_press:'Metal Press',sawmill:'Sawmill',solar_generator:'Solar Generator',energy_conduit:'Energy Conduit',item_transfer:'Item Transfer',digital_miner:'Survey Miner',storage_core:'Nexus Storage Core',storage_terminal:'Nexus Terminal',energy_cell:'Energy Cell',resonite:'Resonite Crystal',draconic_core:'Draconic Core (Concept)',chaotic_core:'Chaotic Core (Concept)',wireless_sender:'Wireless Energy Sender',wireless_receiver:'Wireless Energy Receiver',magnet:'Item Magnet'};
for(const n of metalFragments)names[n]=title(n.replace('_fragment',''))+' Ore Fragment';
for(const tier of tiers.slice(1))names['tier_kit_'+tier.id]=`${title(tier.id)} Tier Kit (${tier.name})`;
// Every translation key is assigned exactly once; a repeated key is a generator bug.
const lang={};
const say=entries=>{for(const [key,value] of Object.entries(entries)){assert.ok(!(key in lang),`lang key ${key} is assigned twice`);lang[key]=value}};
say({'itemGroup.technologia':'Technologia','ui.technologia.toggle':'Start / Pause','ui.technologia.pause':'Pause','ui.technologia.start':'Start','ui.technologia.rescan':'Rescan'});
for(const n of blocks)say({['block.technologia.'+n]:names[n]??title(n)});
for(const n of items)say({['item.technologia.'+n]:names[n]??title(n)});
say({'item.technologia.tiered':'%s %s'});
['Ready','Working','Needs power','Output is full','Needs input','Paused','Owner unavailable','Scan complete','Protected block skipped','Waiting for loaded chunk','Needs clear daylight','Conduit route too long','Unsupported item',
 'Paused by redstone','No receiver on this channel','Needs flowing water beside it','Needs open air and sky','Needs a hot side and a cold side','Chunk loading is disabled on this server','Starting up'].forEach((s,i)=>say({['status.technologia.'+i]:s}));
say({
 'hint.technologia.coal_generator':'Coal/charcoal fuel. Use energy conduits to reach distant machines.',
 'hint.technologia.crusher':'Input: raw metal. Produces two dust per raw material.',
 'hint.technologia.electric_furnace':'Input: any vanilla smelting recipe. Output slots follow.',
 'hint.technologia.digital_miner':'Filter: ore or raw material. Empty = all ores. Starts paused.',
 'hint.technologia.energy_cell':'Stores 1,000,000 FE at Mk I. Sends up to 200 FE/t; tier kits raise both.',
 'hint.technologia.alloy_smelter':'Two inputs: 3 copper + 1 tin makes 4 bronze. 1 iron + 2 coal dust makes steel.',
 'hint.technologia.metal_press':'Input: iron, copper, gold, bronze, steel, invar or electrum ingot. Produces one plate.',
 'hint.technologia.sawmill':'Input: logs or wood. Produces six planks and one sawdust.',
 'hint.technologia.solar_generator':'Produces 16 energy/tick in daylight under open sky. No fuel required.',
 'hint.technologia.compactor':'Packs materials into blocks; also presses sand into sandstone.',
 'hint.technologia.centrifuge':'Separates clay and gravel; extract bone meal and slime from biological inputs.',
 'hint.technologia.recycler':'Salvages metal nuggets from iron/gold tools and armor. Contents and enchantments are lost.',
 'hint.technologia.biomass_generator':'Fuel: sawdust, saplings, wheat, kelp or sugar cane. 20 FE/t at default settings.',
 'hint.technologia.advanced_energy_cell':'Stores 5,000,000 FE at Mk I. Sends up to 200 FE/t; tier kits raise both.',
 'hint.technologia.advanced_solar_generator':'Generates 64 FE/t in clear daylight with sky access.',
 'hint.technologia.auto_sieve':'Input: gravel, sand or coarse dirt. Shakes out ore fragments. A better mesh finds more.',
 // Machines without slots show their hint as a paragraph on the screen, so these may run to a few lines.
 'hint.technologia.water_wheel':'Put flowing water against its sides. Each side gives 6 FE/t, or 10 FE/t when the water falls. Still water gives nothing. No fuel needed.',
 'hint.technologia.windmill':'Needs open sky above and nothing solid beside or above it; run the cable from below. Gives 4 FE/t at Y 64 and more the higher it stands, up to 32 FE/t. Rain and storms add to that.',
 'hint.technologia.thermoelectric_generator':'Needs a hot block on one side and a cold block on another: lava, fire or magma against ice, snow or water. Lava and water give 16 FE/t, lava and blue ice 48 FE/t. Nothing is used up.',
 'hint.technologia.creative_energy_source':'Always full. Sends up to 1,000,000 FE/t to the machines, cells and conduits next to it. Creative mode only: it has no recipe.',
 'hint.technologia.growth_accelerator':'Makes crops, saplings and other plants around it grow about seven times faster. Each extra growth step costs 15 FE. Reaches 2 blocks at Mk I and one more every two tiers.',
 'hint.technologia.chunk_loader':'Keeps its chunk loaded while it has power and is switched on. Mk III holds 3 by 3 chunks, Mk VI 5 by 5. Costs 10 FE/t for each chunk.',
 // The wireless screen has room for two lines.
 'hint.technologia.wireless_sender':'Feed it by cable. It powers every loaded receiver on this channel, in any dimension.',
 'hint.technologia.wireless_receiver':'Takes power from senders on this channel and feeds the blocks next to it.',
 'hint.technologia.enrichment_chamber':'Input: coal, redstone, a diamond or resonite. Makes the enriched form the infuser needs.',
 'hint.technologia.metallurgic_infuser':'Two inputs: a metal and an enriched material. Iron and enriched carbon make steel.',
 'hint.technologia.phyto_chamber':'Input: one seed, sapling or other plant. Grows it with energy and gives the seed back.',
 'hint.technologia.auto_harvester':'Harvests and replants ripe crops around it. It works for its owner, who must be online.',
 'hint.technologia.vacuum_collector':'Pulls dropped items within 3 blocks into its slots. Each tier adds one block of reach.'
});
say({'ui.technologia.input':'Input','ui.technologia.output':'Output','ui.technologia.filter':'Filter','ui.technologia.energy':'%s / %s FE','ui.technologia.transfer_rate':'Transfers up to %s FE/t','ui.technologia.bloom_boost':'Resonance Blooms: +%s%% speed','ui.technologia.field_guide':'Technologia Field Guide'});
say({
 'ui.technologia.redstone.0':'RS: any','ui.technologia.redstone.1':'RS: high','ui.technologia.redstone.2':'RS: low',
 'ui.technologia.redstone_hint':'Redstone control: run always, only with a signal, or only without one.',
 'ui.technologia.eject_on':'Eject on','ui.technologia.eject_off':'Eject off',
 'ui.technologia.eject_hint':'Push results into the inventory behind the machine.',
 'ui.technologia.channel':'Channel %s','ui.technologia.wireless_limit':'Limit %s FE/t','ui.technologia.rate':'Now: %s FE/t',
 'ui.technologia.mesh':'Mesh: %s'
});
['String','Flint','Iron','Diamond'].forEach((s,i)=>say({['ui.technologia.mesh.'+i]:s}));
const storageText={title:'Nexus storage',search:'Search items',search_hint:'Search name or mod:item',previous:'Previous page',next:'Next page',deposit:'Deposit held',deposit_hint:'Store the stack on your cursor. Shift-click your inventory to deposit directly.',sort_count:'Count (high)',sort_name:'Name (A-Z)',stored:'%s stored',withdraw_hint:'Left: stack | Right: one | Shift: to inventory',page:'%s / %s',status:'%s/%s types | %s items | %s/%s slots',empty:'Storage is empty',no_matches:'No matching items',empty_hint:'Shift-click inventory items to deposit.',no_matches_hint:'Try another name or mod:item.'};
for(const [key,value] of Object.entries(storageText))say({['ui.technologia.storage.'+key]:value});
say({
 'tooltip.technologia.field_guide':'Use to read the workshop guide',
 'tooltip.technologia.conduit_rating':'Carries up to %s FE/t per supplier',
 'tooltip.technologia.tier_kit':'Upgrades a %s machine to %s',
 'tooltip.technologia.tier_stats':'Lanes %s | speed %s | capacity %s | transfer %s',
 'tooltip.technologia.machine_tier':'Tier: %s',
 'tooltip.technologia.machine_energy':'%s / %s FE stored',
 'tooltip.technologia.wrench':'Use: rotate. Crouch-use: dismantle a machine, keeping its tier and energy.',
 'tooltip.technologia.mesh':'Use on a sieve to install it. The old mesh comes back.',
 'tooltip.technologia.creative_kit':'Use: top tier. Crouch-use: first tier. Not used up.',
 'tooltip.technologia.magnet':'Pulls items and experience within %s blocks. Use to switch.',
 'tooltip.technologia.magnet_on':'On','tooltip.technologia.magnet_off':'Off',
 'tooltip.technologia.travel_staff':'Use: travel to the block you look at, up to %s blocks. Costs one ender pearl.',
 'message.technologia.mesh_same':'That mesh is already installed.',
 'message.technologia.mesh_installed':'%s installed.',
 'message.technologia.magnet_on':'Magnet on.','message.technologia.magnet_off':'Magnet off.',
 'message.technologia.staff_no_target':'No block in reach to travel to.',
 'message.technologia.staff_no_pearl':'The Travel Staff needs an ender pearl in your inventory.',
 'message.technologia.bonsai_empty':'Plant a sapling in the pot.',
 'message.technologia.bonsai_growing':'Growing: %s%%',
 'message.technologia.sieve_refused':'The sieve cannot use that.',
 'message.technologia.sieve_empty':'Put gravel, sand or coarse dirt in the sieve first.',
 'message.technologia.no_core':'No usable Nexus network. Connect 1-4 cores within 128 network blocks.',
 'message.technologia.transfer_filter':'Item Transfer filter: %s (sample retained)',
 'message.technologia.transfer_unfiltered':'Item Transfer: all items. Arrow points toward output; redstone pauses.',
 'message.technologia.rotated':'Facing changed; inventory preserved.',
 'message.technologia.kit_no_tier':'This block has no tiers.',
 'message.technologia.kit_already':'Already %s or higher.',
 'message.technologia.kit_order':'Install tier kits in order: this machine must be %s first.',
 'message.technologia.kit_installed':'Upgraded to %s. Inventory and energy kept.',
 'message.technologia.dismantled':'Machine dismantled. Tier and energy stay on the item.',
 'message.technologia.guide_owned':'Your Field Guide is in your inventory. Hold it and use it to read.',
 'message.technologia.guide_full':'Make one inventory slot free, then use /technologia guide again.',
 'message.technologia.guide_received':'Field Guide added. Hold it and use it to read. No operator permissions needed.'
});
const tag=(ns,type,n,values)=>json(`data/${ns}/tags/${type}/${n}`,{replace:false,values});
// Everything solid is a pickaxe block, except the wooden blocks (axe) and the loose compressed blocks (shovel).
// Only the ores ask for a tool tier. The bloom and the bonsai pot break by hand.
const axeBlocks=['wooden_crate','hand_sieve'],shovelBlocks=['compressed_gravel','compressed_sand'],handBlocks=['resonance_bloom','bonsai_pot'];
tag('minecraft','block','mineable/pickaxe',blocks.filter(n=>![...axeBlocks,...shovelBlocks,...handBlocks].includes(n)).map(id));
tag('minecraft','block','mineable/axe',axeBlocks.map(id));
tag('minecraft','block','mineable/shovel',shovelBlocks.map(id));
tag('minecraft','block','needs_stone_tool',ores.filter(n=>!n.includes('resonite')).map(id));
tag('minecraft','block','needs_iron_tool',[id('resonite_ore')]);
for(const type of ['block','item']){
 // Add vanilla values so the common recipe/miner logic also works on bare Fabric.
 tag('c',type,'ores',[...ores.map(id),...['coal','iron','gold','copper','redstone','lapis','diamond','emerald'].flatMap(n=>['minecraft:'+n+'_ore','minecraft:deepslate_'+n+'_ore']), 'minecraft:nether_gold_ore','minecraft:nether_quartz_ore']);
 for(const material of vanillaMetals)tag('c',type,'ores/'+material,[`minecraft:${material}_ore`,`minecraft:deepslate_${material}_ore`]);
 // Forge 1.21 packs may still use forge tags; expose both namespaces.
 tag('forge',type,'ores',ores.map(id));
 for(const material of [...ownMetals,'resonite'])for(const ns of ['c','forge'])tag(ns,type,'ores/'+material,ores.filter(n=>oreMaterial(n)===material).map(id));
}
const plateMetals=[...vanillaMetals,'bronze','steel','invar','electrum'],dustMetals=[...vanillaMetals,...ownMetals,'coal'];
for(const material of vanillaMetals)for(const ns of ['c','forge']){
 tag(ns,'item','ingots/'+material,['minecraft:'+material+'_ingot']);
 tag(ns,'item','raw_materials/'+material,['minecraft:raw_'+material]);
}
for(const material of ownMetals)for(const [category,item] of [['raw_materials','raw_'+material],['ingots',material+'_ingot']])for(const ns of ['c','forge'])tag(ns,'item',`${category}/${material}`,[id(item)]);
for(const material of dustMetals)for(const ns of ['c','forge'])tag(ns,'item','dusts/'+material,[id(material+'_dust')]);
for(const material of alloys)for(const ns of ['c','forge'])tag(ns,'item','ingots/'+material,[id(material+'_ingot')]);
for(const material of plateMetals)for(const ns of ['c','forge'])tag(ns,'item','plates/'+material,[id(material+'_plate')]);
for(const ns of ['c','forge'])tag(ns,'item','dusts/wood',[id('sawdust')]);
for(const ns of ['c','forge']){
 tag(ns,'item','ingots',[...vanillaMetals,...ownMetals,...alloys].map(n=>'#'+ns+':ingots/'+n));
 tag(ns,'item','raw_materials',[...vanillaMetals,...ownMetals].map(n=>'#'+ns+':raw_materials/'+n));
 tag(ns,'item','dusts',[...dustMetals,'wood'].map(n=>'#'+ns+':dusts/'+n));
 tag(ns,'item','plates',plateMetals.map(n=>'#'+ns+':plates/'+n));
}
// '#' marks a tag; a bare name is one of this mod's items.
const item=s=>s.includes(':')?s:id(s);
const ingredient=s=>s.startsWith('#')?{tag:s.slice(1)}:{item:item(s)};
const craft=(n,pattern,key,count=1,type='minecraft:crafting_shaped')=>json(`data/technologia/recipe/${n}`,{type,category:'misc',pattern,key:Object.fromEntries(Object.entries(key).map(([k,v])=>[k,ingredient(v)])),result:{id:id(n),count}});
const shapeless=(n,inputs,result=n,count=1)=>json(`data/technologia/recipe/${n}`,{type:'minecraft:crafting_shapeless',category:'misc',ingredients:inputs.map(ingredient),result:{id:item(result),count}});
const nine=input=>Array.from({length:9},()=>input);
shapeless('field_guide',['minecraft:book','minecraft:copper_ingot']);
craft('machine_frame',['ICI','C C','ICI'],{I:'minecraft:iron_ingot',C:'minecraft:copper_ingot'});
craft('basic_circuit',[' R ','CTC',' R '],{R:'minecraft:redstone',C:'minecraft:copper_ingot',T:'#c:ingots/tin'},2);
craft('advanced_circuit',['RGR','CBC','RGR'],{R:'resonite',G:'#c:plates/gold',C:'basic_circuit',B:'minecraft:redstone_block'});
craft('coal_generator',['III','CFC','IRI'],{I:'minecraft:iron_ingot',C:'minecraft:coal',F:'machine_frame',R:'minecraft:furnace'});
craft('crusher',['IPI','CFC','IRI'],{I:'minecraft:iron_ingot',P:'minecraft:piston',C:'basic_circuit',F:'machine_frame',R:'minecraft:redstone'});
craft('electric_furnace',['III','CFC','IRI'],{I:'minecraft:copper_ingot',C:'basic_circuit',F:'machine_frame',R:'minecraft:furnace'});
craft('alloy_smelter',['ITI','CFC','IRI'],{I:'minecraft:iron_ingot',T:'#c:ingots/tin',C:'basic_circuit',F:'machine_frame',R:'minecraft:furnace'});
craft('metal_press',['BPB','CFC','BIB'],{B:'#c:ingots/bronze',P:'minecraft:piston',C:'basic_circuit',F:'machine_frame',I:'minecraft:iron_block'});
craft('sawmill',['IBI','CFC','IPI'],{I:'minecraft:iron_ingot',B:'#c:plates/bronze',C:'basic_circuit',F:'machine_frame',P:'minecraft:stonecutter'});
craft('solar_generator',['GGG','PCP','PFP'],{G:'minecraft:glass',P:'#c:plates/copper',C:'basic_circuit',F:'machine_frame'});
craft('auto_sieve',['ISI','CFC','IHI'],{I:'minecraft:iron_ingot',S:'minecraft:string',C:'basic_circuit',F:'machine_frame',H:'minecraft:hopper'});
craft('energy_conduit',[' G ','PRP',' G '],{G:'minecraft:glass',P:'#c:plates/copper',R:'minecraft:redstone'},8);
craft('gold_energy_conduit',[' G ','PRP',' G '],{G:'minecraft:glass',P:'#c:plates/gold',R:'minecraft:redstone'},8);
craft('resonite_energy_conduit',[' G ','PRP',' G '],{G:'minecraft:glass',P:'#c:plates/steel',R:'resonite'},8);
craft('item_transfer',[' B ','PHP',' R '],{B:'#c:plates/bronze',P:'minecraft:piston',H:'minecraft:hopper',R:'minecraft:redstone'},2);
craft('energy_cell',['TLT','RFR','TLT'],{T:'#c:ingots/tin',L:'#c:ingots/lead',R:'minecraft:redstone_block',F:'machine_frame'});
craft('storage_core',['ICI','BFB','ICI'],{I:'minecraft:iron_ingot',C:'basic_circuit',B:'minecraft:chest',F:'machine_frame'});
craft('storage_terminal',['GGG','CFC','IRI'],{G:'minecraft:glass',C:'basic_circuit',F:'machine_frame',I:'minecraft:iron_ingot',R:'resonite'});
craft('network_cable',[' T ','CRC',' T '],{T:'#c:ingots/tin',C:'minecraft:copper_ingot',R:'minecraft:redstone'},8);
craft('digital_miner',['DAD','EFP','CRC'],{D:'#c:plates/steel',A:'advanced_circuit',E:'minecraft:ender_pearl',F:'machine_frame',P:'minecraft:diamond_pickaxe',C:'basic_circuit',R:'resonite'});
shapeless('resonance_bloom',['resonite','#minecraft:small_flowers','minecraft:glowstone_dust']);
// Tier kit recipes are data: the pattern and key come from each tier's "kit" entry in tiers.json.
for(const tier of tiers.slice(1))if(tier.kit)craft('tier_kit_'+tier.id,tier.kit.pattern,tier.kit.key);
// A byproduct may be any namespaced item, alone or as [item, count]; its chance is only written when it is below 1.
// 'mesh' is the sieve mesh a recipe needs at least; it is only written above 0, the string mesh.
const processing=(name,machine,inputs,result,count,time,energy,byproduct,chance=1,mesh=0)=>{
 const [extra,extraCount=1]=[byproduct??[]].flat();
 json(`data/technologia/recipe/${name}`,{type:'technologia:processing',machine,ingredients:inputs.map(([input,count])=>({ingredient:ingredient(input),count})),result:{id:item(result),count},time,energy,...(extra?{byproduct:{id:item(extra),count:extraCount},...(chance<1?{byproduct_chance:chance}:{})}:{}),...(mesh?{mesh}:{})});
};
for(const material of [...vanillaMetals,...ownMetals])processing(`crushing/raw_${material}`,'crusher',[[`#c:raw_materials/${material}`,1]],`${material}_dust`,2,100,20);
processing('crushing/coal','crusher',[['minecraft:coal',1]],'coal_dust',1,60,20);
processing('crushing/cobblestone','crusher',[['minecraft:cobblestone',1]],'minecraft:gravel',1,80,20);
processing('crushing/gravel','crusher',[['minecraft:gravel',1]],'minecraft:sand',1,80,20);
for(const n of metalFragments)processing(`crushing/${n}`,'crusher',[[n,4]],n.replace('fragment','dust'),1,60,20);
processing('crushing/redstone_fragment','crusher',[['redstone_fragment',4]],'minecraft:redstone',2,60,20);
processing('crushing/lapis_fragment','crusher',[['lapis_fragment',4]],'minecraft:lapis_lazuli',2,60,20);
processing('crushing/diamond_fragment','crusher',[['diamond_fragment',4]],'minecraft:diamond',1,120,30);
// Sieving: one table per mesh, as block -> [result, count, byproduct, chance]. A finer mesh replaces the coarser recipe for the same block.
const sieving=[
 {gravel:['iron_fragment',1,'tin_fragment',.35],sand:['copper_fragment',1,'gold_fragment',.1],coarse_dirt:['lead_fragment',1,'minecraft:flint',.2]},
 {gravel:['iron_fragment',2,'nickel_fragment',.3],sand:['copper_fragment',2,'silver_fragment',.25],coarse_dirt:['lead_fragment',2,'tin_fragment',.5]},
 {gravel:['iron_fragment',2,'redstone_fragment',.35],sand:['gold_fragment',1,'lapis_fragment',.3],coarse_dirt:['tin_fragment',2,'nickel_fragment',.4]},
 {gravel:['iron_fragment',3,'diamond_fragment',.08],sand:['gold_fragment',2,'lapis_fragment',.5],coarse_dirt:['silver_fragment',2,'redstone_fragment',.4]}
];
sieving.forEach((table,mesh)=>{for(const [block,[result,count,byproduct,chance]] of Object.entries(table))processing(`sieving/${block}`+(mesh?'_'+meshes[mesh-1].replace('_mesh',''):''),'auto_sieve',[['minecraft:'+block,1]],result,count,80,15,byproduct,chance,mesh)});
// A compressed block is nine blocks sieved in one long cycle, whatever the mesh.
processing('sieving/compressed_gravel','auto_sieve',[['compressed_gravel',1]],'iron_fragment',9,480,15,['tin_fragment',3]);
processing('sieving/compressed_sand','auto_sieve',[['compressed_sand',1]],'copper_fragment',9,480,15,'gold_fragment');
processing('alloying/bronze','alloy_smelter',[['#c:ingots/copper',3],['#c:ingots/tin',1]],'bronze_ingot',4,160,30);
processing('alloying/steel','alloy_smelter',[['#c:ingots/iron',1],['#c:dusts/coal',2]],'steel_ingot',1,240,30);
processing('alloying/electrum','alloy_smelter',[['#c:ingots/gold',1],['#c:ingots/silver',1]],'electrum_ingot',2,160,30);
processing('alloying/invar','alloy_smelter',[['#c:ingots/iron',2],['#c:ingots/nickel',1]],'invar_ingot',3,200,30);
processing('alloying/constantan','alloy_smelter',[['#c:ingots/copper',1],['#c:ingots/nickel',1]],'constantan_ingot',2,160,30);
for(const material of plateMetals)processing(`pressing/${material}`,'metal_press',[[`#c:ingots/${material}`,1]],`${material}_plate`,1,60,15);
for(const wood of ['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','crimson','warped'])processing(`sawing/${wood}`,'sawmill',[[`#minecraft:${wood}_${['crimson','warped'].includes(wood)?'stems':'logs'}`,1]],`minecraft:${wood}_planks`,6,80,15,'sawdust');
// Enriching packs a material for the infuser; it also splits glowstone and quartz blocks back into their parts.
for(const [input,count,result,amount] of [['minecraft:coal',1,'enriched_carbon',1],['minecraft:charcoal',1,'enriched_carbon',1],['minecraft:redstone',4,'enriched_redstone',1],['minecraft:diamond',1,'enriched_diamond',1],['resonite',1,'enriched_resonite',1],['minecraft:glowstone',1,'minecraft:glowstone_dust',4],['minecraft:quartz_block',1,'minecraft:quartz',4]])
 processing('enriching/'+input.split(':').pop(),'enrichment_chamber',[[input,count]],result,amount,100,20);
processing('infusing/steel','metallurgic_infuser',[['#c:ingots/iron',2],['enriched_carbon',1]],'steel_ingot',2,160,30);
processing('infusing/infused_alloy','metallurgic_infuser',[['#c:ingots/iron',1],['enriched_redstone',1]],'infused_alloy',1,160,30);
processing('infusing/reinforced_alloy','metallurgic_infuser',[['infused_alloy',1],['enriched_diamond',1]],'reinforced_alloy',1,160,30);
processing('infusing/resonant_alloy','metallurgic_infuser',[['#c:ingots/copper',2],['enriched_resonite',1]],'resonant_alloy',2,160,30);
// Growing: one seed in, the crop out; where the seed is not the crop, it comes back as the byproduct.
for(const [seed,crop,count,chance] of [['wheat_seeds','wheat',2],['beetroot_seeds','beetroot',2],['melon_seeds','melon_slice',4,.5],['pumpkin_seeds','pumpkin',1]])processing('growing/'+crop,'phyto_chamber',[['minecraft:'+seed,1]],'minecraft:'+crop,count,400,20,'minecraft:'+seed,chance);
for(const [plant,count] of [['carrot',3],['potato',3],['sugar_cane',3],['cactus',3],['bamboo',4],['nether_wart',3],['sweet_berries',3],['kelp',3],['cocoa_beans',3]])processing('growing/'+plant,'phyto_chamber',[['minecraft:'+plant,1]],'minecraft:'+plant,count,400,20);
for(const wood of ['oak','spruce','birch','jungle','acacia','dark_oak','cherry'])processing('growing/'+wood,'phyto_chamber',[[`minecraft:${wood}_sapling`,1]],`minecraft:${wood}_log`,4,400,20,`minecraft:${wood}_sapling`);
processing('growing/resonance_bloom','phyto_chamber',[['resonance_bloom',1]],'resonance_bloom',2,600,30);
shapeless('charcoal_from_sawdust',nine('sawdust'),'minecraft:charcoal');
// A larger, connected survival workshop.
craft('wrench',[' I ',' CI','C  '],{I:'minecraft:iron_ingot',C:'minecraft:copper_ingot'});
craft('compactor',['SPS','CFC','SPS'],{S:'#c:plates/steel',P:'minecraft:piston',C:'basic_circuit',F:'machine_frame'});
craft('centrifuge',['BIB','CFC','BRB'],{B:'#c:plates/bronze',I:'minecraft:iron_bars',C:'basic_circuit',F:'machine_frame',R:'resonite'});
craft('recycler',['BIB','CFC','BHB'],{B:'#c:plates/bronze',I:'minecraft:iron_bars',C:'basic_circuit',F:'machine_frame',H:'minecraft:hopper'});
craft('biomass_generator',['IBI','CFC','IRI'],{I:'minecraft:iron_ingot',B:'minecraft:composter',C:'basic_circuit',F:'machine_frame',R:'minecraft:furnace'});
// Built from other machines: this recipe type keeps their lowest tier and their stored energy on the result.
craft('advanced_energy_cell',['SAS','ECE','SAS'],{S:'#c:plates/steel',A:'advanced_circuit',E:'energy_cell',C:'resonite'},1,'technologia:machine_crafting');
craft('advanced_solar_generator',['SSS','ACA','PFP'],{S:'solar_generator',A:'advanced_circuit',C:'resonite',P:'#c:plates/steel',F:'machine_frame'},1,'technologia:machine_crafting');
// Alpha.5 machines. The creative source has no recipe.
craft('water_wheel',['PSP','SFS','PCP'],{P:'#minecraft:planks',S:'minecraft:stick',F:'machine_frame',C:'minecraft:copper_ingot'});
craft('windmill',['WSW','SFS','WCW'],{W:'#minecraft:wool',S:'minecraft:stick',F:'machine_frame',C:'minecraft:copper_ingot'});
craft('thermoelectric_generator',['KPK','PFP','KBK'],{K:'#c:ingots/constantan',P:'#c:plates/copper',F:'machine_frame',B:'basic_circuit'});
craft('enrichment_chamber',['BRB','CFC','BRB'],{B:'#c:plates/bronze',R:'minecraft:redstone_block',C:'basic_circuit',F:'machine_frame'});
craft('metallurgic_infuser',['SRS','CFC','SUS'],{S:'#c:plates/steel',R:'minecraft:redstone',C:'basic_circuit',F:'machine_frame',U:'minecraft:furnace'});
craft('phyto_chamber',['GGG','CFC','IDI'],{G:'minecraft:glass',C:'basic_circuit',F:'machine_frame',I:'#c:plates/invar',D:'minecraft:dirt'});
craft('auto_harvester',['IHI','CFC','ISI'],{I:'#c:plates/invar',H:'minecraft:iron_hoe',C:'basic_circuit',F:'machine_frame',S:'minecraft:shears'});
craft('growth_accelerator',['IBI','CFC','IRI'],{I:'#c:plates/invar',B:'resonance_bloom',C:'basic_circuit',F:'machine_frame',R:'resonite'});
craft('vacuum_collector',['PEP','CFC','PHP'],{P:'#c:plates/iron',E:'minecraft:ender_pearl',C:'basic_circuit',F:'machine_frame',H:'minecraft:hopper'});
craft('chunk_loader',['RER','AFA','RNR'],{R:'reinforced_alloy',E:'minecraft:ender_eye',A:'advanced_circuit',F:'machine_frame',N:'resonite'});
// Sender and receiver share one key; the alloy sits below in the sender and on top in the receiver.
const wirelessKey={E:'#c:plates/electrum',A:'advanced_circuit',P:'minecraft:ender_pearl',F:'machine_frame',R:'resonant_alloy'};
craft('wireless_sender',['EAE','PFP','ERE'],wirelessKey);
craft('wireless_receiver',['ERE','PFP','EAE'],wirelessKey);
craft('superconducting_energy_conduit',[' I ','ERE',' I '],{I:'minecraft:blue_ice',E:'#c:plates/electrum',R:'resonant_alloy'},8);
// Compressed blocks pack nine into one and unpack again.
for(const n of compressed){const loose=n.replace('compressed_','');shapeless(n,nine('minecraft:'+loose));shapeless(loose+'_from_compressed',[n],'minecraft:'+loose,9);}
craft('wooden_crate',['PPP','PCP','PPP'],{P:'#minecraft:planks',C:'minecraft:chest'});
craft('bronze_crate',['BPB','PCP','BPB'],{B:'#c:plates/bronze',P:'#minecraft:planks',C:'wooden_crate'});
craft('steel_crate',['SPS','PCP','SPS'],{S:'#c:plates/steel',P:'#c:plates/bronze',C:'bronze_crate'});
craft('resonant_crate',['RSR','SCS','RSR'],{R:'resonant_alloy',S:'#c:plates/steel',C:'steel_crate'});
craft('bonsai_pot',['B B','BDB',' B '],{B:'minecraft:brick',D:'minecraft:dirt'});
craft('hand_sieve',['PSP','PSP','T T'],{P:'#minecraft:planks',S:'minecraft:string',T:'minecraft:stick'});
craft('flint_mesh',['SFS','FSF','SFS'],{S:'minecraft:string',F:'minecraft:flint'});
craft('iron_mesh',['NSN','SMS','NSN'],{N:'minecraft:iron_nugget',S:'minecraft:string',M:'flint_mesh'});
craft('diamond_mesh',['DSD','SMS','DSD'],{D:'minecraft:diamond',S:'minecraft:string',M:'iron_mesh'});
craft('vector_plate',['ISI','RGR'],{I:'#c:plates/iron',S:'minecraft:slime_ball',R:'minecraft:redstone',G:'#c:plates/gold'},8);
craft('fast_vector_plate',['VGV','VRV'],{V:'vector_plate',G:'#c:plates/gold',R:'minecraft:redstone_block'},4);
craft('magnet',['R L','I I','III'],{R:'minecraft:redstone',L:'minecraft:lapis_lazuli',I:'minecraft:iron_ingot'});
craft('reinforced_magnet',['S S','SMS',' I '],{S:'#c:plates/steel',M:'magnet',I:'infused_alloy'});
craft('resonant_magnet',['R R','RMR',' A '],{R:'resonant_alloy',M:'reinforced_magnet',A:'advanced_circuit'});
craft('travel_staff',['  E',' B ','B  '],{E:'minecraft:ender_eye',B:'minecraft:blaze_rod'});
// Construction blocks: one distinct recipe each, eight blocks per craft unless a count is given.
const decorRecipes={
 steel_casing:[['NPN','PSP','NPN'],{N:'minecraft:iron_nugget',P:'#c:plates/steel',S:'minecraft:stone'}],
 bronze_casing:[['NPN','PSP','NPN'],{N:'minecraft:iron_nugget',P:'#c:plates/bronze',S:'minecraft:stone'}],
 industrial_bricks:[['BPB','BSB','BPB'],{B:'minecraft:bricks',P:'#c:plates/iron',S:'minecraft:stone'}],
 steel_grate:[['B B',' P ','B B'],{B:'minecraft:iron_bars',P:'#c:plates/steel'}],
 hazard_block:[['YPY','PSP','YPY'],{Y:'minecraft:yellow_dye',P:'#c:plates/iron',S:'minecraft:stone'}],
 engineering_lamp:[['PGP','GLG','PGP'],{P:'#c:plates/iron',G:'minecraft:glass',L:'minecraft:glowstone'}],
 steel_pillar:[['PCP','PCP','PCP'],{P:'#c:plates/steel',C:'minecraft:copper_ingot'}],
 ventilation_grille:[['PBP','BSB','PBP'],{P:'#c:plates/iron',B:'minecraft:iron_bars',S:'minecraft:stone'}],
 control_panel:[['PGP','RCR','PSP'],{P:'#c:plates/steel',G:'minecraft:glass',R:'minecraft:redstone',C:'basic_circuit',S:'minecraft:stone'}],
 reinforced_glass:[['GPG','PGP','GPG'],{G:'minecraft:glass',P:'#c:plates/iron'}],
 floor_panel:[['SPS','PSP','SPS'],{S:'minecraft:stone',P:'#c:plates/iron'}],
 catwalk:[['P P','BBB'],{P:'#c:plates/steel',B:'minecraft:iron_bars'}],
 steel_table:[['PPP','B B','B B'],{P:'#c:plates/steel',B:'minecraft:iron_bars'},4],
 steel_stool:[['PPP','B B'],{P:'#c:plates/steel',B:'minecraft:iron_bars'},4],
 metal_shelf:[['SPS','S S','SPS'],{S:'#c:plates/steel',P:'#c:plates/iron'},4],
 tool_cabinet:[['SPS','SPS','STS'],{S:'#c:plates/steel',P:'#c:plates/iron',T:'minecraft:stone'},2],
 warning_light:[[' D ','GLG',' P '],{D:'minecraft:orange_dye',G:'minecraft:glass',L:'minecraft:glowstone',P:'#c:plates/iron'},4]
};
for(const n of decor){const [pattern,key,count=8]=decorRecipes[n];craft(n,pattern,key,count);}
// Sand is left out: the compactor already turns four sand into sandstone, and two recipes for one input would compete.
for(const n of compressed)if(n!=='compressed_sand')processing('compacting/'+n,'compactor',[['minecraft:'+n.replace('compressed_',''),9]],n,1,100,20);
for(const material of ['iron','gold','copper'])processing('compacting/'+material,'compactor',[["#c:ingots/"+material,9]],'minecraft:'+material+'_block',1,100,20);
for(const material of ['coal','redstone','lapis_lazuli','diamond','emerald'])processing('compacting/'+material,'compactor',[["minecraft:"+material,9]],'minecraft:'+(material==='lapis_lazuli'?'lapis':material)+'_block',1,100,20);
processing('compacting/sandstone','compactor',[['minecraft:sand',4]],'minecraft:sandstone',1,60,15);
processing('compacting/clay','compactor',[['minecraft:clay_ball',4]],'minecraft:clay',1,60,15);
processing('separating/gravel','centrifuge',[['minecraft:gravel',1]],'minecraft:flint',1,100,25);
processing('separating/clay','centrifuge',[['minecraft:clay',1]],'minecraft:clay_ball',4,80,15);
processing('separating/moss','centrifuge',[['minecraft:moss_block',2]],'minecraft:bone_meal',1,120,25);
processing('separating/cactus','centrifuge',[['minecraft:cactus',4]],'minecraft:slime_ball',1,160,30);
for(const material of ['iron','golden'])for(const tool of ['helmet','chestplate','leggings','boots','pickaxe','axe','shovel','hoe','sword'])processing('recycling/'+material+'_'+tool,'recycler',[["minecraft:"+material+'_'+tool,1]],'minecraft:'+(material==='golden'?'gold':material)+'_nugget',3,80,15);
// Vanilla advancements provide actual persisted milestones rather than decorative checkmarks.
const milestones=[['root','machine_frame','A workshop worth building','Craft a Machine Frame.',null],['power','coal_generator','First spark','Build a combustion generator.','root'],['crushing','crusher','Break it down','Double raw metals into dust.','power'],['smelting','electric_furnace','Close the loop','Smelt dust into useful ingots.','crushing'],['alloys','alloy_smelter','Better together','Combine counted ingredients into alloys.','smelting'],['bronze','bronze_ingot','Age of bronze','Alloy three copper with one tin.','alloys'],['steel','steel_ingot','Steelworks','Alloy iron and coal dust.','alloys'],['press','metal_press','Under pressure','Form metal plates.','bronze'],['sawmill','sawmill','Waste becomes fuel','Saw logs into planks and sawdust.','press'],['biomass','biomass_generator','Circular workshop','Power equipment with renewable biomass.','sawmill'],['solar','solar_generator','Clear skies','Generate power without fuel.','press'],['conduits','energy_conduit','Wire the workshop','Route power through conduits.','press'],['transfer','item_transfer','Keep it moving','Transfer and filter adjacent inventories.','press'],['nexus','storage_terminal','One catalogue','Connect up to four storage cores.','root'],['compacting','compactor','Less space, more material','Compress bulk materials.','steel'],['centrifuge','centrifuge','Separate the useful','Process mineral and biological inputs.','press'],['recycler','recycler','Recover the metal','Salvage nuggets from old equipment.','press'],['advanced_power','advanced_energy_cell','Power reserve','Build a five-million-FE battery.','steel'],['advanced_solar','advanced_solar_generator','Solar field','Build a 64-FE/t solar array.','advanced_power'],['miner','digital_miner','Selective extraction','Build and configure the Survey Miner.','steel'],['sieve','auto_sieve','Shaken, not mined','Sieve ore fragments out of gravel and sand.','crushing'],['tier_kit','tier_kit_bronze','Upgrade path','Craft a tier kit; use it on a placed machine to upgrade it.','press'],['resonant_kit','tier_kit_resonant','In resonance','Craft a Mk IV tier kit.','tier_kit'],['gold_conduit','gold_energy_conduit','Better conductors','Carry more power with gold conduits.','conduits'],['bloom','resonance_bloom','Growing efficiency','Craft a Resonance Bloom; planted near a machine it speeds it up.','press']];
// Alpha.5 milestones: [key, icon, title, what the item is for, parent].
milestones.push(
 ['early_power','water_wheel','Free energy','Build a Water Wheel; flowing water beside it makes power without fuel.','root'],
 ['thermo','thermoelectric_generator','Hot and cold','Build a Thermoelectric Generator; it makes power between a hot block and a cold one.','early_power'],
 ['enrichment','enrichment_chamber','Enriched','Build an Enrichment Chamber; it prepares coal, redstone and diamonds for the infuser.','alloys'],
 ['infusion','metallurgic_infuser','Infusion','Build a Metallurgic Infuser; it joins a metal and an enriched material into an alloy.','enrichment'],
 ['reinforced','reinforced_alloy','Reinforced','Infuse an Infused Alloy with Enriched Diamond; the Chunk Loader is built from it.','infusion'],
 ['phyto','phyto_chamber','Greenhouse in a box','Build a Phyto Chamber; it grows crops and trees from one seed with energy.','press'],
 ['harvester','auto_harvester','Hands-free harvest','Build an Auto Harvester; it harvests and replants the crops around it.','phyto'],
 ['accelerator','growth_accelerator','Hurry up','Build a Growth Accelerator; it makes the plants around it grow faster.','harvester'],
 ['wireless','wireless_sender','No strings attached','Build a Wireless Energy Sender; it powers the receivers on its channel without cables.','reinforced'],
 ['chunk_loader','chunk_loader','Always on','Build a Chunk Loader; it keeps your workshop running while you are away.','reinforced'],
 ['mesh','flint_mesh','Finer and finer','Craft a Flint Mesh; use it on a sieve to find more fragments.','sieve'],
 ['diamond_mesh','diamond_mesh','Nothing gets through','Craft a Diamond Mesh; a sieve with it can find diamond fragments.','mesh'],
 ['hand_sieve','hand_sieve','By hand first','Craft a Hand Sieve; it shakes ore fragments out of gravel and sand without power.','root'],
 ['bonsai','bonsai_pot','A forest on a shelf','Craft a Bonsai Pot; a sapling planted in it gives wood again and again.','root'],
 ['crate','wooden_crate','Somewhere to put it','Craft a Wooden Crate; it stores 27 stacks in one block.','root'],
 ['magnet','magnet','Attractive','Craft an Item Magnet; it pulls nearby items and experience to you.','root'],
 ['travel_staff','travel_staff','Point and go','Craft a Travel Staff; it takes you to the block you look at for one ender pearl.','root'],
 ['superconductor','superconducting_energy_conduit','No resistance','Craft Superconducting Energy Conduits; each carries up to 32,000 FE/t.','wireless'],
 ['vector_plate','vector_plate','This way','Craft Vector Plates; they carry items, mobs and players the way their arrow points.','press'],
 ['vacuum','vacuum_collector','Tidy floor','Build a Vacuum Collector; it picks up the dropped items around it.','press']
);
// Titles and descriptions are translation keys, so a language file can replace them.
for(const [key,icon,heading,description,parent] of milestones){
 const text='advancement.technologia.'+key;say({[text+'.title']:heading,[text+'.description']:description});
 json('data/technologia/advancement/workshop/'+key,{...(parent?{parent:'technologia:workshop/'+parent}:{}),display:{icon:{id:id(icon)},title:{translate:text+'.title'},description:{translate:text+'.description'},frame:'task',show_toast:true,announce_to_chat:false,hidden:false,...(!parent?{background:'minecraft:textures/gui/advancements/backgrounds/stone.png'}:{})},criteria:{obtained:{trigger:'minecraft:inventory_changed',conditions:{items:[{items:id(icon)}]}}},requirements:[['obtained']]});
}
json('assets/technologia/lang/en_us',lang);
buildFactoryModels({json,png,palette,machines,decor});
buildFactoryShapes({write,base,machines,decor,devices:Object.keys(devices)});
// Ore and raw metal give ore experience; dust was already paid for when the ore was mined, so it gives the small dust value.
for(const material of ownMetals)for(const input of ['raw_'+material,material+'_ore','deepslate_'+material+'_ore',material+'_dust'])for(const type of ['smelting','blasting'])json(`data/technologia/recipe/${input}_${type}`,{type:'minecraft:'+type,category:'misc',ingredient:ingredient(input),result:{id:id(material+'_ingot'),count:1},experience:input.endsWith('_dust')?.1:.5,cookingtime:type==='smelting'?200:100});
for(const material of vanillaMetals)for(const type of ['smelting','blasting'])json(`data/technologia/recipe/${material}_dust_${type}`,{type:'minecraft:'+type,category:'misc',ingredient:ingredient(material+'_dust'),result:{id:'minecraft:'+material+'_ingot',count:1},experience:.1,cookingtime:type==='smelting'?200:100});
// One ore feature per material as [name, vein size, veins per chunk, lowest Y, highest Y]. Fabric adds the same names in code.
const oreFeatures=[['tin',8,10,-32,80],['lead',6,7,-48,32],['resonite',4,4,-56,0],['silver',6,5,-48,40],['nickel',6,6,-32,64]];
for(const [n,size,count,min,max] of oreFeatures){
 const targets=['stone','deepslate'].map(stone=>({target:{predicate_type:'minecraft:tag_match',tag:`minecraft:${stone}_ore_replaceables`},state:{Name:id((stone==='deepslate'&&n!=='resonite'?'deepslate_':'')+n+'_ore')}}));
 json(`data/technologia/worldgen/configured_feature/${n}_ore`,{type:'minecraft:ore',config:{size,discard_chance_on_air_exposure:0,targets}});
 json(`data/technologia/worldgen/placed_feature/${n}_ore`,{feature:id(n+'_ore'),placement:[{type:'minecraft:count',count},{type:'minecraft:in_square'},{type:'minecraft:height_range',height:{type:'minecraft:trapezoid',min_inclusive:{absolute:min},max_inclusive:{absolute:max}}},{type:'minecraft:biome'}]});
 for(const loader of ['forge','neoforge'])write(`${loader}/src/main/resources/data/technologia/${loader}/biome_modifier/${n}_ore.json`,JSON.stringify({type:loader+':add_features',biomes:'#minecraft:is_overworld',features:id(n+'_ore'),step:'underground_ores'},null,2)+'\n');
}
write(`${base}/pack.mcmeta`,JSON.stringify({pack:{pack_format:34,description:'Technologia resources'}})+'\n');
// Loader metadata carries ${...} placeholders; Gradle's expand() fills them from gradle.properties at build time.
write('fabric/src/main/resources/fabric.mod.json',JSON.stringify({schemaVersion:1,id:'technologia',version:'${version}',name:'Technologia',description:'${description}',authors:['Technologia'],license:'All Rights Reserved',environment:'*',accessWidener:'technologia.accesswidener',entrypoints:{main:['dev.technologia.TechnologiaFabric'],client:['dev.technologia.TechnologiaFabricClient']},depends:{fabricloader:'>=${fabric_loader_version}',minecraft:'${minecraft_version}',java:'>=${java_version}','fabric-api':'>=${fabric_version}','team_reborn_energy':'>=4.1.0'}},null,2)+'\n');
for(const loader of ['forge','neoforge']){
 const required=loader==='forge'?'mandatory=true':'type="required"';
 write(`${loader}/src/main/resources/META-INF/${loader==='forge'?'mods':'neoforge.mods'}.toml`,['modLoader="javafml"','loaderVersion="${'+loader+'_loader_version_range}"','license="All Rights Reserved"','[[mods]]','modId="technologia"','version="${version}"','displayName="Technologia"','authors="Technologia"',"description='''${description}'''",'[[dependencies.technologia]]',`modId="${loader}"`,required,'versionRange="[${'+loader+'_version},)"','ordering="NONE"','side="BOTH"','[[dependencies.technologia]]','modId="minecraft"',required,'versionRange="${minecraft_version_range}"','ordering="NONE"','side="BOTH"',''].join('\n'));
}
// The generator owns its output folders: a file it did not write this run is left over from an older
// version (a removed recipe would otherwise keep shipping). Only the two hand-written inputs are kept.
const handWritten=new Set([base+'/technologia.accesswidener',base+'/technologia/tiers.json']);
const walk=dir=>fs.existsSync(path.join(root,dir))?fs.readdirSync(path.join(root,dir),{withFileTypes:true}).flatMap(entry=>entry.isDirectory()?walk(dir+'/'+entry.name):[dir+'/'+entry.name]):[];
const stale=[base,'fabric/src/main/resources','forge/src/main/resources','neoforge/src/main/resources'].flatMap(walk).filter(file=>!written.has(file)&&!handWritten.has(file)).sort();
for(const file of stale){fs.rmSync(path.join(root,file));console.log('Removed stale '+file);}
console.log(`Generated ${blocks.length} blocks, ${items.length} items, ${written.size} files, ${oreFeatures.length} ore features and loader metadata.`);
