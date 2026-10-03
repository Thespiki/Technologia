// Deterministic, original 32px prototype art and Minecraft 1.21.1 data.
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
import {buildFactoryModels,cull} from './factory-models.mjs';
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
const decor=['steel_casing','bronze_casing','industrial_bricks','steel_grate','hazard_block','engineering_lamp','steel_pillar','ventilation_grille','control_panel','reinforced_glass'];
// Decor with a front face: placed facing the player, so its blockstate carries a 'facing' property.
const directionalDecor=['ventilation_grille','control_panel'];
const conduits=['energy_conduit','gold_energy_conduit','resonite_energy_conduit'];
blocks.push(...extraMachines,...decor,...conduits.slice(1),'resonance_bloom');machines.push(...extraMachines);
const ores=blocks.filter(n=>n.endsWith('_ore'));
const fragments=['iron_fragment','copper_fragment','gold_fragment','tin_fragment','lead_fragment'];
// One installer kit per tier after the first, straight from tiers.json.
const kits=tiers.slice(1).map(tier=>'tier_kit_'+tier.id);
const items=['wrench','raw_tin','raw_lead','tin_ingot','lead_ingot','bronze_ingot','steel_ingot','resonite','iron_dust','gold_dust','copper_dust','tin_dust','lead_dust','coal_dust','sawdust','iron_plate','copper_plate','gold_plate','bronze_plate','steel_plate','basic_circuit','advanced_circuit','draconic_core','chaotic_core','field_guide',...fragments,...kits];
const palette={base:[27,39,53],dark:[12,18,27],rim:[65,82,103],light:[174,190,209],cyan:[85,217,208],amber:[232,180,106],violet:[176,141,245]};
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
// Ores: speckled stone with five mineral nuggets. Resonite only generates in the deepslate layer, so it shares the dark base.
const paintOre=n=>r=>{const deep=n.startsWith('deepslate')||n==='resonite_ore';for(let y=0;y<32;y++)for(let x=0;x<32;x++){let q=(x*17+y*31+x*y*3)%19+(deep?43:100);r(x,y,1,1,[q,q+2,q+3])}const c=n.includes('tin')?[180,213,209]:n.includes('lead')?[136,133,172]:palette.cyan;for(const [x,y] of [[7,6],[22,9],[12,18],[25,24],[3,26]]){r(x-1,y-1,5,4,palette.dark);r(x,y,4,3,c);r(x,y,2,1,palette.light)}};
// Processing machines glow amber while working; power and data machines glow cyan.
const heated=['coal_generator','crusher','electric_furnace','alloy_smelter','metal_press','sawmill','compactor','centrifuge','recycler','auto_sieve'];
const leaf=[86,160,84];
// One instrument per machine: every front texture is different, idle and active.
const paintFront=(n,active=false)=>r=>{
 casing(r);const c=active?(heated.includes(n)?palette.amber:palette.cyan):palette.rim;
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
 else {r(12,10,8,10,palette.rim);r(14,12,4,6,c)}
};
const turns={north:0,east:90,south:180,west:270};
// Cables, conduits, the item transfer and the bloom get their blockstate and model in their own sections below.
const custom=['network_cable',...conduits,'item_transfer','resonance_bloom'];
for(const n of blocks){
 const ore=ores.includes(n),model=`technologia:block/${n}`;
 if(machines.includes(n)) {
  // Models come from factory-models.mjs; only the instrument textures and the facing x active states are made here.
  for(const active of [false,true])png('block/'+n+(active?'_active':''),paintFront(n,active));
  const variants={};
  for(const [facing,y] of Object.entries(turns))for(const active of [false,true])variants[`active=${active},facing=${facing}`]={model:model+(active?'_active':''),...(y?{y}:{})};
  json(`assets/technologia/blockstates/${n}`,{variants});
 } else if(directionalDecor.includes(n))json(`assets/technologia/blockstates/${n}`,{variants:Object.fromEntries(Object.entries(turns).map(([facing,y])=>[`facing=${facing}`,{model,...(y?{y}:{})}]))});
 else if(!custom.includes(n))json(`assets/technologia/blockstates/${n}`,{variants:{'':{model}}});
 if(ore){png('block/'+n,paintOre(n));json(`assets/technologia/models/block/${n}`,{parent:'minecraft:block/cube_all',textures:{all:model}});}
 if(n==='machine_frame'){png('block/'+n,paintFront(n));json(`assets/technologia/models/block/${n}`,{parent:'minecraft:block/cube',textures:{particle:model,down:'technologia:block/casing',up:'technologia:block/top',north:model,south:'technologia:block/casing',east:'technologia:block/casing',west:'technologia:block/casing'}});}
 json(`assets/technologia/models/item/${n}`,n==='resonance_bloom'?{parent:'minecraft:item/generated',textures:{layer0:model}}:{parent:model});
 const drop=ore?(n.includes('tin')?'raw_tin':n.includes('lead')?'raw_lead':'resonite'):n;
 const entry=ore?{type:'minecraft:alternatives',children:[{type:'minecraft:item',name:id(n),conditions:[{condition:'minecraft:match_tool',predicate:{predicates:{'minecraft:enchantments':[{enchantments:'minecraft:silk_touch',levels:{min:1}}]}}}]},{type:'minecraft:item',name:id(drop),functions:[{function:'minecraft:apply_bonus',enchantment:'minecraft:fortune',formula:'minecraft:ore_drops'},{function:'minecraft:explosion_decay'}]}]}:{type:'minecraft:item',name:id(drop)};
 json(`data/technologia/loot_table/blocks/${n}`,{type:'minecraft:block',pools:[{rolls:1,entries:[entry],...(!ore&&!machines.includes(n)?{conditions:[{condition:'minecraft:survives_explosion'}]}:{})}]});
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
// Conduit grades share one shape; the two jacket stripes tell them apart: amber, warm yellow, cyan and violet.
const conduitStripes={energy_conduit:[palette.amber,palette.amber],gold_energy_conduit:[[255,208,64],[255,232,138]],resonite_energy_conduit:[palette.cyan,palette.violet]};
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
const tint=n=>n.includes('coal')?[70,78,88]:n==='sawdust'?[188,137,78]:n.includes('steel')?[132,151,170]:n.includes('bronze')?[201,144,76]:n.includes('gold')?palette.amber:n.includes('copper')?[221,139,99]:n.includes('lead')?[148,138,186]:n.includes('tin')?[180,213,209]:n.includes('iron')?[214,202,190]:n.includes('chaotic')?palette.violet:n.includes('draconic')?[236,136,73]:n==='advanced_circuit'?palette.violet:n.includes('circuit')||n==='resonite'?palette.cyan:palette.light;
// Rock chips per fragment as [x,y,width,height]: two or three, placed differently for every metal.
const chips={iron_fragment:[[6,16,8,7],[17,9,7,6],[18,20,6,5]],copper_fragment:[[6,10,9,8],[16,19,9,7]],gold_fragment:[[5,9,7,6],[13,17,9,8],[22,8,5,5]],tin_fragment:[[8,17,10,8],[19,8,7,7]],lead_fragment:[[6,7,8,7],[7,19,7,6],[17,13,9,8]]};
const kitAccent={bronze:[201,144,76],steel:[150,168,186],resonant:palette.cyan,hardened:[126,200,255],tempered:[226,112,68],entangled:palette.violet,stellar:[250,236,160]};
for(const n of items){
 const kit=kits.indexOf(n),c=kit<0?tint(n):kitAccent[tiers[kit+1].id]??palette.light;
 png('item/'+n,r=>{
  if(n==='wrench'){r(13,13,6,15,palette.rim);r(8,4,5,10,palette.light);r(19,4,5,10,palette.light);r(9,11,14,6,palette.light);r(14,22,4,3,palette.cyan)}
  else if(n==='field_guide'){r(5,4,22,25,palette.dark);r(7,5,19,21,palette.rim);r(9,7,15,16,palette.base);r(7,26,18,2,palette.light);r(7,5,2,21,palette.amber);r(15,10,4,10,palette.cyan);r(12,13,10,4,palette.cyan)}
  else if(n.endsWith('dust')){r(7,21,18,4,palette.dark);r(9,17,14,6,c);r(13,13,6,5,c);r(5,24,4,2,c);r(25,21,3,2,c)}
  else if(n.endsWith('plate')){r(5,9,22,16,palette.dark);r(7,10,18,12,c);r(8,10,16,2,palette.light);r(9,22,16,2,palette.rim);for(const x of [8,22])for(const y of [13,19])r(x,y,1,1,palette.dark)}
  else if(n.endsWith('fragment')){const low=c.map(v=>Math.round(v*.68));for(const [x,y,w,h] of chips[n]){r(x-1,y-1,w+2,h+2,palette.dark);r(x,y,w,h,c);r(x+2,y+h-2,w-2,2,low);r(x,y,w-3,1,[246,244,240]);r(x+w-2,y,2,2,palette.dark);r(x,y+h-1,1,1,palette.dark)}}
  // A module plate with a tier-coloured header; the tier number (Mk II = 2) is shown as that many pips.
  else if(kit>=0){r(4,5,24,22,palette.dark);r(5,6,22,20,palette.rim);r(7,8,18,16,palette.base);r(7,8,18,3,c);for(const x of [8,14,20])r(x,27,4,2,palette.light);for(let i=0;i<kit+2;i++)r(9+4*(i%4),14+4*Math.floor(i/4),3,3,c)}
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
const names={coal_generator:'Combustion Generator',crusher:'Ore Crusher',electric_furnace:'Electric Furnace',alloy_smelter:'Alloy Smelter',metal_press:'Metal Press',sawmill:'Sawmill',solar_generator:'Solar Generator',energy_conduit:'Energy Conduit',item_transfer:'Item Transfer',digital_miner:'Survey Miner',storage_core:'Nexus Storage Core',storage_terminal:'Nexus Terminal',energy_cell:'Energy Cell',resonite:'Resonite Crystal',draconic_core:'Draconic Core (Concept)',chaotic_core:'Chaotic Core (Concept)'};
for(const n of fragments)names[n]=title(n.replace('_fragment',''))+' Ore Fragment';
for(const tier of tiers.slice(1))names['tier_kit_'+tier.id]=`${title(tier.id)} Tier Kit (${tier.name})`;
// Every translation key is assigned exactly once; a repeated key is a generator bug.
const lang={};
const say=entries=>{for(const [key,value] of Object.entries(entries)){assert.ok(!(key in lang),`lang key ${key} is assigned twice`);lang[key]=value}};
say({'itemGroup.technologia':'Technologia','ui.technologia.toggle':'Start / Pause','ui.technologia.pause':'Pause','ui.technologia.start':'Start','ui.technologia.rescan':'Rescan'});
for(const n of blocks)say({['block.technologia.'+n]:names[n]??title(n)});
for(const n of items)say({['item.technologia.'+n]:names[n]??title(n)});
say({'item.technologia.tiered':'%s %s'});
['Ready','Working','Needs power','Output is full','Needs input','Paused','Owner unavailable','Scan complete','Protected block skipped','Waiting for loaded chunk','Needs clear daylight','Conduit route too long','Unsupported item'].forEach((s,i)=>say({['status.technologia.'+i]:s}));
say({
 'hint.technologia.coal_generator':'Coal/charcoal fuel. Use energy conduits to reach distant machines.',
 'hint.technologia.crusher':'Input: raw metal. Produces two dust per raw material.',
 'hint.technologia.electric_furnace':'Input: any vanilla smelting recipe. Output slots follow.',
 'hint.technologia.digital_miner':'Filter: ore or raw material. Empty = all ores. Starts paused.',
 'hint.technologia.energy_cell':'Stores 1,000,000 FE at Mk I. Sends up to 200 FE/t; tier kits raise both.',
 'hint.technologia.alloy_smelter':'Two inputs: 3 copper + 1 tin makes 4 bronze. 1 iron + 2 coal dust makes steel.',
 'hint.technologia.metal_press':'Input: iron, copper, gold, bronze or steel ingot. Produces one plate.',
 'hint.technologia.sawmill':'Input: logs or wood. Produces six planks and one sawdust.',
 'hint.technologia.solar_generator':'Produces 16 energy/tick in daylight under open sky. No fuel required.',
 'hint.technologia.compactor':'Packs materials into blocks; also presses sand into sandstone.',
 'hint.technologia.centrifuge':'Separates clay and gravel; extract bone meal and slime from biological inputs.',
 'hint.technologia.recycler':'Salvages metal nuggets from iron/gold tools and armor. Contents and enchantments are lost.',
 'hint.technologia.biomass_generator':'Fuel: sawdust, saplings, wheat, kelp or sugar cane. 20 FE/t at default settings.',
 'hint.technologia.advanced_energy_cell':'Stores 5,000,000 FE at Mk I. Sends up to 200 FE/t; tier kits raise both.',
 'hint.technologia.advanced_solar_generator':'Generates 64 FE/t in clear daylight with sky access.',
 'hint.technologia.auto_sieve':'Input: gravel, sand or coarse dirt. Shakes out ore fragments.'
});
say({'ui.technologia.input':'Input','ui.technologia.output':'Output','ui.technologia.filter':'Filter','ui.technologia.energy':'%s / %s FE','ui.technologia.transfer_rate':'Transfers up to %s FE/t','ui.technologia.bloom_boost':'Resonance Blooms: +%s%% speed','ui.technologia.field_guide':'Technologia Field Guide'});
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
// Everything solid is a pickaxe block; only the ores ask for a tool tier. The bloom breaks by hand.
tag('minecraft','block','mineable/pickaxe',blocks.filter(n=>n!=='resonance_bloom').map(id));
tag('minecraft','block','needs_stone_tool',ores.filter(n=>!n.includes('resonite')).map(id));
tag('minecraft','block','needs_iron_tool',[id('resonite_ore')]);
for(const type of ['block','item']){
 // Add vanilla values so the common recipe/miner logic also works on bare Fabric.
 tag('c',type,'ores',[...ores.map(id),...['coal','iron','gold','copper','redstone','lapis','diamond','emerald'].flatMap(n=>['minecraft:'+n+'_ore','minecraft:deepslate_'+n+'_ore']), 'minecraft:nether_gold_ore','minecraft:nether_quartz_ore']);
 for(const material of ['tin','lead','resonite'])tag('c',type,'ores/'+material,ores.filter(n=>n.includes(material)).map(id));
 for(const material of ['iron','gold','copper'])tag('c',type,'ores/'+material,[`minecraft:${material}_ore`,`minecraft:deepslate_${material}_ore`]);
 // Forge 1.21 packs may still use forge tags; expose both namespaces.
 tag('forge',type,'ores',ores.map(id));
}
for(const material of ['iron','gold','copper'])for(const ns of ['c','forge']){
 tag(ns,'item','ingots/'+material,['minecraft:'+material+'_ingot']);
 tag(ns,'item','raw_materials/'+material,['minecraft:raw_'+material]);
}
for(const material of ['tin','lead'])for(const [category,item] of [['raw_materials','raw_'+material],['ingots',material+'_ingot']])for(const ns of ['c','forge'])tag(ns,'item',`${category}/${material}`,[id(item)]);
for(const material of ['iron','gold','copper','tin','lead','coal'])for(const ns of ['c','forge'])tag(ns,'item','dusts/'+material,[id(material+'_dust')]);
for(const material of ['bronze','steel'])for(const ns of ['c','forge'])tag(ns,'item','ingots/'+material,[id(material+'_ingot')]);
for(const material of ['iron','gold','copper','bronze','steel'])for(const ns of ['c','forge'])tag(ns,'item','plates/'+material,[id(material+'_plate')]);
for(const ns of ['c','forge'])tag(ns,'item','dusts/wood',[id('sawdust')]);
for(const ns of ['c','forge']){
 tag(ns,'item','ingots',['iron','gold','copper','tin','lead','bronze','steel'].map(n=>'#'+ns+':ingots/'+n));
 tag(ns,'item','raw_materials',['iron','gold','copper','tin','lead'].map(n=>'#'+ns+':raw_materials/'+n));
 tag(ns,'item','dusts',['iron','gold','copper','tin','lead','coal','wood'].map(n=>'#'+ns+':dusts/'+n));
 tag(ns,'item','plates',['iron','gold','copper','bronze','steel'].map(n=>'#'+ns+':plates/'+n));
}
// '#' marks a tag; a bare name is one of this mod's items.
const item=s=>s.includes(':')?s:id(s);
const ingredient=s=>s.startsWith('#')?{tag:s.slice(1)}:{item:item(s)};
const craft=(n,pattern,key,count=1,type='minecraft:crafting_shaped')=>json(`data/technologia/recipe/${n}`,{type,category:'misc',pattern,key:Object.fromEntries(Object.entries(key).map(([k,v])=>[k,ingredient(v)])),result:{id:id(n),count}});
const shapeless=(n,inputs,result=n)=>json(`data/technologia/recipe/${n}`,{type:'minecraft:crafting_shapeless',category:'misc',ingredients:inputs.map(ingredient),result:{id:item(result),count:1}});
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
// A byproduct may be any namespaced item; its chance is only written when it is below 1.
const processing=(name,machine,inputs,result,count,time,energy,byproduct,chance=1)=>json(`data/technologia/recipe/${name}`,{type:'technologia:processing',machine,ingredients:inputs.map(([input,count])=>({ingredient:ingredient(input),count})),result:{id:item(result),count},time,energy,...(byproduct?{byproduct:{id:item(byproduct),count:1},...(chance<1?{byproduct_chance:chance}:{})}:{})});
for(const material of ['iron','gold','copper','tin','lead'])processing(`crushing/raw_${material}`,'crusher',[[`#c:raw_materials/${material}`,1]],`${material}_dust`,2,100,20);
processing('crushing/coal','crusher',[['minecraft:coal',1]],'coal_dust',1,60,20);
processing('crushing/cobblestone','crusher',[['minecraft:cobblestone',1]],'minecraft:gravel',1,80,20);
processing('crushing/gravel','crusher',[['minecraft:gravel',1]],'minecraft:sand',1,80,20);
for(const material of ['iron','copper','gold','tin','lead'])processing(`crushing/${material}_fragment`,'crusher',[[`${material}_fragment`,4]],`${material}_dust`,1,60,20);
processing('sieving/gravel','auto_sieve',[['minecraft:gravel',1]],'iron_fragment',1,80,15,'tin_fragment',.35);
processing('sieving/sand','auto_sieve',[['minecraft:sand',1]],'copper_fragment',1,80,15,'gold_fragment',.1);
processing('sieving/coarse_dirt','auto_sieve',[['minecraft:coarse_dirt',1]],'lead_fragment',1,80,15,'minecraft:flint',.2);
processing('alloying/bronze','alloy_smelter',[['#c:ingots/copper',3],['#c:ingots/tin',1]],'bronze_ingot',4,160,30);
processing('alloying/steel','alloy_smelter',[['#c:ingots/iron',1],['#c:dusts/coal',2]],'steel_ingot',1,240,30);
for(const material of ['iron','copper','gold','bronze','steel'])processing(`pressing/${material}`,'metal_press',[[`#c:ingots/${material}`,1]],`${material}_plate`,1,60,15);
for(const wood of ['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','crimson','warped'])processing(`sawing/${wood}`,'sawmill',[[`#minecraft:${wood}_${['crimson','warped'].includes(wood)?'stems':'logs'}`,1]],`minecraft:${wood}_planks`,6,80,15,'sawdust');
shapeless('charcoal_from_sawdust',Array.from({length:9},()=>'sawdust'),'minecraft:charcoal');
// A larger, connected survival workshop.
craft('wrench',[' I ',' CI','C  '],{I:'minecraft:iron_ingot',C:'minecraft:copper_ingot'});
craft('compactor',['SPS','CFC','SPS'],{S:'#c:plates/steel',P:'minecraft:piston',C:'basic_circuit',F:'machine_frame'});
craft('centrifuge',['BIB','CFC','BRB'],{B:'#c:plates/bronze',I:'minecraft:iron_bars',C:'basic_circuit',F:'machine_frame',R:'resonite'});
craft('recycler',['BIB','CFC','BHB'],{B:'#c:plates/bronze',I:'minecraft:iron_bars',C:'basic_circuit',F:'machine_frame',H:'minecraft:hopper'});
craft('biomass_generator',['IBI','CFC','IRI'],{I:'minecraft:iron_ingot',B:'minecraft:composter',C:'basic_circuit',F:'machine_frame',R:'minecraft:furnace'});
// Built from other machines: this recipe type keeps their lowest tier and their stored energy on the result.
craft('advanced_energy_cell',['SAS','ECE','SAS'],{S:'#c:plates/steel',A:'advanced_circuit',E:'energy_cell',C:'resonite'},1,'technologia:machine_crafting');
craft('advanced_solar_generator',['SSS','ACA','PFP'],{S:'solar_generator',A:'advanced_circuit',C:'resonite',P:'#c:plates/steel',F:'machine_frame'},1,'technologia:machine_crafting');
// Construction blocks: one distinct recipe each, eight blocks per craft.
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
 reinforced_glass:[['GPG','PGP','GPG'],{G:'minecraft:glass',P:'#c:plates/iron'}]
};
for(const n of decor)craft(n,...decorRecipes[n],8);
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
// Titles and descriptions are translation keys, so a language file can replace them.
for(const [key,icon,heading,description,parent] of milestones){
 const text='advancement.technologia.'+key;say({[text+'.title']:heading,[text+'.description']:description});
 json('data/technologia/advancement/workshop/'+key,{...(parent?{parent:'technologia:workshop/'+parent}:{}),display:{icon:{id:id(icon)},title:{translate:text+'.title'},description:{translate:text+'.description'},frame:'task',show_toast:true,announce_to_chat:false,hidden:false,...(!parent?{background:'minecraft:textures/gui/advancements/backgrounds/stone.png'}:{})},criteria:{obtained:{trigger:'minecraft:inventory_changed',conditions:{items:[{items:id(icon)}]}}},requirements:[['obtained']]});
}
json('assets/technologia/lang/en_us',lang);
buildFactoryModels({json,png,palette,machines,decor});
buildFactoryShapes({write,base,machines,decor});
// Ore and raw metal give ore experience; dust was already paid for when the ore was mined, so it gives the small dust value.
for(const material of ['tin','lead'])for(const input of ['raw_'+material,material+'_ore','deepslate_'+material+'_ore',material+'_dust'])for(const type of ['smelting','blasting'])json(`data/technologia/recipe/${input}_${type}`,{type:'minecraft:'+type,category:'misc',ingredient:ingredient(input),result:{id:id(material+'_ingot'),count:1},experience:input.endsWith('_dust')?.1:.5,cookingtime:type==='smelting'?200:100});
for(const material of ['iron','gold','copper'])for(const type of ['smelting','blasting'])json(`data/technologia/recipe/${material}_dust_${type}`,{type:'minecraft:'+type,category:'misc',ingredient:ingredient(material+'_dust'),result:{id:'minecraft:'+material+'_ingot',count:1},experience:.1,cookingtime:type==='smelting'?200:100});
for(const [n,size,count,min,max] of [['tin',8,10,-32,80],['lead',6,7,-48,32],['resonite',4,4,-56,0]]){
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
console.log(`Generated ${blocks.length} blocks, ${items.length} items, ${written.size} files, three ores and loader metadata.`);
