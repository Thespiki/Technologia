// Deterministic, original 32px prototype art and Minecraft 1.21.1 data.
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import {buildFactoryModels} from './factory-models.mjs';
import {buildFactoryShapes} from './factory-shapes.mjs';
const base='common/src/main/resources';
const write=(p,s)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,s)};
const json=(p,o)=>write(`${base}/${p}.json`,JSON.stringify(o,null,2)+'\n');
const id=n=>'technologia:'+n;
const blocks=['machine_frame','coal_generator','crusher','electric_furnace','alloy_smelter','metal_press','sawmill','solar_generator','digital_miner','storage_core','storage_terminal','energy_cell','network_cable','energy_conduit','item_transfer','tin_ore','deepslate_tin_ore','lead_ore','deepslate_lead_ore','resonite_ore'];
const machines=['coal_generator','crusher','electric_furnace','alloy_smelter','metal_press','sawmill','solar_generator','digital_miner','storage_core','storage_terminal','energy_cell'];
const extraMachines=['compactor','centrifuge','recycler','biomass_generator','advanced_energy_cell','advanced_solar_generator'];
const decor=['steel_casing','bronze_casing','industrial_bricks','steel_grate','hazard_block','engineering_lamp','steel_pillar','ventilation_grille','control_panel','reinforced_glass'];
blocks.push(...extraMachines,...decor);machines.push(...extraMachines);
const items=['wrench','raw_tin','raw_lead','tin_ingot','lead_ingot','bronze_ingot','steel_ingot','resonite','iron_dust','gold_dust','copper_dust','tin_dust','lead_dust','coal_dust','sawdust','iron_plate','copper_plate','gold_plate','bronze_plate','steel_plate','basic_circuit','advanced_circuit','draconic_core','chaotic_core','field_guide'];
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
const cuboid=(from,to,texture='#casing',front=null)=>({from,to,faces:Object.fromEntries(['down','up','north','south','west','east'].map(face=>[face,{texture:face==='north'&&front?front:texture,...(face==='north'&&front?{uv:[0,0,16,16]}:{})}]))});
function recessedModel(name,active=false){
 const elements=[cuboid([0,0,2.5],[16,16,16]),cuboid([0,0,0],[2,16,2.5]),cuboid([14,0,0],[16,16,2.5]),cuboid([2,0,0],[14,2,2.5]),cuboid([2,14,0],[14,16,2.5])];
 if(name==='storage_core')for(const x of [2.5,8.5])for(const y of [2.5,6.5,10.5])elements.push(cuboid([x,y,1],[x+5,y+3,2.5],'#casing','#detail'));
 else {elements.push(cuboid([2,5,1.7],[14,14,2.5],'#casing','#detail'));elements.push(cuboid([3,2.5,0.5],[13,4,2.5],'#casing'));}
 return {parent:'minecraft:block/block',ambientocclusion:true,textures:{particle:'technologia:block/casing',casing:'technologia:block/casing',detail:`technologia:block/${name==='storage_core'?'drive_bay':'terminal_screen'}${active?'_active':''}`},elements};
}
function productionModel(name,active=false){
 const elements=[cuboid([0,0,2],[16,16,16]),cuboid([1,1,0],[3,15,2]),cuboid([13,1,0],[15,15,2]),cuboid([3,1,0],[13,3,2]),cuboid([3,13,0],[13,15,2]),cuboid([3,3,1],[13,13,2],'#casing','#detail')];
 if(name==='metal_press')elements.push(cuboid([5,3,0],[11,4,2],'#casing'));
 if(name==='sawmill')elements.push(cuboid([4,3,0],[12,5,2],'#casing'));
 return {parent:'minecraft:block/block',ambientocclusion:true,textures:{particle:`technologia:block/${name}`,casing:'technologia:block/casing',detail:`technologia:block/${name}${active?'_active':''}`},elements};
}
const productionModels=['alloy_smelter','metal_press','sawmill'];
for(const n of blocks){
 const ore=n.includes('_ore');
 const paintFront=(active=false)=>r=>{
  if(ore){const deep=n.startsWith('deepslate');for(let y=0;y<32;y++)for(let x=0;x<32;x++){let q=(x*17+y*31+x*y*3)%19+(deep?43:100);r(x,y,1,1,[q,q+2,q+3])}const c=n.includes('tin')?[180,213,209]:n.includes('lead')?[136,133,172]:palette.cyan;for(const [x,y] of [[7,6],[22,9],[12,18],[25,24],[3,26]]){r(x-1,y-1,5,4,palette.dark);r(x,y,4,3,c);r(x,y,2,1,palette.light)}return;}
  casing(r);const c=active?(['crusher','electric_furnace','coal_generator','alloy_smelter','metal_press','sawmill'].includes(n)?palette.amber:palette.cyan):palette.rim;
  r(5,6,22,18,palette.dark);r(6,26,16,2,c);r(25,26,2,2,c);
  if(n==='crusher'){for(let y=9;y<23;y+=4){r(8,y,7,2,c);r(17,y+1,7,2,palette.light)}}
  else if(n==='alloy_smelter'){r(7,9,7,8,palette.rim);r(18,9,7,8,palette.rim);r(9,11,3,4,c);r(20,11,3,4,c);r(12,19,8,3,c);r(15,17,2,5,palette.light)}
  else if(n==='metal_press'){r(7,8,3,16,palette.light);r(22,8,3,16,palette.light);r(10,9,12,4,palette.rim);r(14,13,4,5,c);r(10,20,12,3,c);r(11,18,10,2,palette.light)}
  else if(n==='sawmill'){r(7,19,18,4,[155,105,68]);r(8,20,16,1,palette.amber);r(12,9,8,10,palette.light);r(9,12,14,4,palette.rim);r(15,10,2,8,palette.dark);r(23,9,2,7,c)}
  else if(n==='solar_generator'){r(9,10,14,8,palette.rim);r(11,11,10,5,c);r(15,7,2,3,palette.light);r(15,18,2,3,palette.light);r(6,13,3,2,palette.light);r(23,13,3,2,palette.light)}
  else if(n==='item_transfer'){r(7,11,18,10,palette.rim);r(10,14,12,4,palette.amber);r(17,11,3,10,palette.amber);r(20,13,3,6,palette.amber);r(23,15,2,2,palette.amber)}
  else if(n==='electric_furnace'||n==='coal_generator'){r(9,12,14,10,palette.rim);r(11,15,10,5,c);r(14,10,4,10,c)}
  else if(n==='digital_miner'){r(15,8,2,14,c);r(9,14,14,2,c);r(11,10,10,2,palette.rim);r(11,19,10,2,palette.rim)}
  else if(n==='energy_cell'){r(10,9,12,13,palette.rim);for(let y=11;y<22;y+=4)r(12,y,8,2,c)}
  else if(n==='storage_core'){for(let y=9;y<23;y+=5){r(8,y,16,3,palette.rim);r(21,y,2,2,c)}}
  else if(n==='storage_terminal'){r(8,9,16,12,[34,76,83]);for(let y=11;y<19;y+=3)r(10,y,7+(y%2)*4,1,c);r(11,23,10,1,palette.light)}
  else {r(12,10,8,10,palette.rim);r(14,12,4,6,c)}
 };
 png('block/'+n,paintFront());
 if(machines.includes(n)) {
  png('block/'+n+'_active',paintFront(true));
  const variants={};
  for(const [facing,y] of Object.entries({north:0,east:90,south:180,west:270}))for(const active of [false,true])variants[`active=${active},facing=${facing}`]={model:`technologia:block/${n}${active?'_active':''}`,...(y?{y}:{})};
  json(`assets/technologia/blockstates/${n}`,{variants});
  json(`assets/technologia/models/block/${n}_active`,['storage_core','storage_terminal'].includes(n)?recessedModel(n,true):productionModels.includes(n)?productionModel(n,true):{parent:`technologia:block/${n}`,textures:{north:`technologia:block/${n}_active`,particle:`technologia:block/${n}_active`}});
 } else json(`assets/technologia/blockstates/${n}`,{variants:{'':{model:`technologia:block/${n}`}}});
 json(`assets/technologia/models/block/${n}`,['storage_core','storage_terminal'].includes(n)?recessedModel(n):productionModels.includes(n)?productionModel(n):ore?{parent:'minecraft:block/cube_all',textures:{all:`technologia:block/${n}`}}:{parent:'minecraft:block/cube',textures:{particle:`technologia:block/${n}`,down:'technologia:block/casing',up:`technologia:block/${n==='solar_generator'?'solar_top':'top'}`,north:`technologia:block/${n}`,south:'technologia:block/casing',east:'technologia:block/casing',west:'technologia:block/casing'}});
 json(`assets/technologia/models/item/${n}`,{parent:`technologia:block/${n}`});
 const drop=ore?(n.includes('tin')?'raw_tin':n.includes('lead')?'raw_lead':'resonite'):n;
 const entry=ore?{type:'minecraft:alternatives',children:[{type:'minecraft:item',name:id(n),conditions:[{condition:'minecraft:match_tool',predicate:{predicates:{'minecraft:enchantments':[{enchantments:'minecraft:silk_touch',levels:{min:1}}]}}}]},{type:'minecraft:item',name:id(drop),functions:[{function:'minecraft:apply_bonus',enchantment:'minecraft:fortune',formula:'minecraft:ore_drops'},{function:'minecraft:explosion_decay'}]}]}:{type:'minecraft:item',name:id(drop)};
 json(`data/technologia/loot_table/blocks/${n}`,{type:'minecraft:block',pools:[{rolls:1,entries:[entry],...(!ore?{conditions:[{condition:'minecraft:survives_explosion'}]}:{})}]});
}
// Six small arm models avoid a dynamic renderer and match PipeBlock's cached shapes.
const cableElement=(from,to,texture)=>({from,to,faces:Object.fromEntries(['down','up','north','south','west','east'].map(face=>[face,{texture,uv:[0,0,16,16]}]))});
const cableModel=elements=>({parent:'minecraft:block/block',textures:{particle:'technologia:block/cable_jacket',jacket:'technologia:block/cable_jacket',hub:'technologia:block/cable_hub'},elements});
const cableHub=cableElement([5,5,5],[11,11,11],'#hub');
const cableArms={north:[[5,5,0],[11,11,5]],south:[[5,5,11],[11,11,16]],east:[[11,5,5],[16,11,11]],west:[[0,5,5],[5,11,11]],up:[[5,11,5],[11,16,11]],down:[[5,0,5],[11,5,11]]};
json('assets/technologia/models/block/network_cable_core',cableModel([cableHub]));
for(const [direction,[from,to]] of Object.entries(cableArms))json(`assets/technologia/models/block/network_cable_${direction}`,cableModel([cableElement(from,to,'#jacket')]));
json('assets/technologia/models/block/network_cable',cableModel([cableHub,...['north','south'].map(direction=>cableElement(...cableArms[direction],'#jacket'))]));
json('assets/technologia/blockstates/network_cable',{multipart:[{apply:{model:'technologia:block/network_cable_core'}},...Object.keys(cableArms).map(direction=>({when:{[direction]:'true'},apply:{model:`technologia:block/network_cable_${direction}`}}))]});
png('block/conduit_jacket',r=>{r(0,0,32,32,palette.dark);r(3,0,26,32,palette.base);for(const x of [7,22])r(x,0,3,32,palette.amber);for(const y of [3,25]){r(2,y,28,4,palette.rim);r(12,y,8,4,palette.light)}});
png('block/conduit_hub',r=>{casing(r);r(6,6,20,20,palette.dark);r(14,7,7,3,palette.amber);r(11,10,7,7,palette.amber);r(14,16,7,3,palette.amber);r(12,19,5,6,palette.amber)});
const conduitModel=elements=>({parent:'minecraft:block/block',textures:{particle:'technologia:block/conduit_jacket',jacket:'technologia:block/conduit_jacket',hub:'technologia:block/conduit_hub'},elements});
json('assets/technologia/models/block/energy_conduit_core',conduitModel([cableHub]));
for(const [direction,[from,to]] of Object.entries(cableArms))json(`assets/technologia/models/block/energy_conduit_${direction}`,conduitModel([cableElement(from,to,'#jacket')]));
json('assets/technologia/models/block/energy_conduit',conduitModel([cableHub,...['north','south'].map(direction=>cableElement(...cableArms[direction],'#jacket'))]));
json('assets/technologia/blockstates/energy_conduit',{multipart:[{apply:{model:'technologia:block/energy_conduit_core'}},...Object.keys(cableArms).map(direction=>({when:{[direction]:'true'},apply:{model:`technologia:block/energy_conduit_${direction}`}}))]});
json('assets/technologia/models/block/item_transfer',{parent:'minecraft:block/block',textures:{particle:'technologia:block/casing',casing:'technologia:block/casing',detail:'technologia:block/item_transfer'},elements:[cuboid([3,3,3],[13,13,13]),cuboid([5,5,13],[11,11,16]),cuboid([4,4,0],[12,12,3],'#casing','#detail')]});
json('assets/technologia/blockstates/item_transfer',{variants:Object.fromEntries(Object.entries({north:{},east:{y:90},south:{y:180},west:{y:270},up:{x:270},down:{x:90}}).map(([facing,rotation])=>[`facing=${facing}`,{model:'technologia:block/item_transfer',...rotation}]))});
for(const n of items){
 let c=n.includes('coal')?[70,78,88]:n==='sawdust'?[188,137,78]:n.includes('steel')?[132,151,170]:n.includes('bronze')?[201,144,76]:n.includes('gold')?palette.amber:n.includes('copper')?[221,139,99]:n.includes('lead')?[148,138,186]:n.includes('chaotic')?palette.violet:n.includes('draconic')?[236,136,73]:n.includes('circuit')||n==='resonite'?palette.cyan:palette.light;
 png('item/'+n,r=>{
  if(n==='wrench'){r(13,13,6,15,palette.rim);r(8,4,5,10,palette.light);r(19,4,5,10,palette.light);r(9,11,14,6,palette.light);r(14,22,4,3,palette.cyan)}
  else if(n==='field_guide'){r(5,4,22,25,palette.dark);r(7,5,19,21,palette.rim);r(9,7,15,16,palette.base);r(7,26,18,2,palette.light);r(7,5,2,21,palette.amber);r(15,10,4,10,palette.cyan);r(12,13,10,4,palette.cyan)}
  else if(n.endsWith('dust')){r(7,21,18,4,palette.dark);r(9,17,14,6,c);r(13,13,6,5,c);r(5,24,4,2,c);r(25,21,3,2,c)}
  else if(n.endsWith('plate')){r(5,9,22,16,palette.dark);r(7,10,18,12,c);r(8,10,16,2,palette.light);r(9,22,16,2,palette.rim);for(const x of [8,22])for(const y of [13,19])r(x,y,1,1,palette.dark)}
  else if(n.includes('circuit')){r(6,6,20,20,palette.dark);r(8,8,16,16,[37,75,72]);r(12,12,8,8,c);for(let x=10;x<25;x+=5){r(x,4,2,3,palette.amber);r(x,25,2,3,palette.amber)}}
  else if(n.endsWith('core')){r(11,4,10,24,palette.dark);r(5,10,22,12,palette.dark);r(10,8,12,16,c);r(7,12,18,8,c);r(13,12,6,8,[246,244,240])}
  else if(n.endsWith('ingot')){r(5,14,22,10,palette.dark);r(7,13,18,8,c);r(10,10,14,4,c);r(10,11,12,2,[225,233,238])}
  else {r(12,5,8,22,palette.dark);r(7,12,18,10,palette.dark);r(13,7,6,18,c);r(9,13,14,7,c);r(13,8,2,9,[226,238,244])}
 });
 json(`assets/technologia/models/item/${n}`,{parent:'minecraft:item/generated',textures:{layer0:`technologia:item/${n}`}});
}
const names={coal_generator:'Combustion Generator',crusher:'Ore Crusher',electric_furnace:'Electric Furnace',alloy_smelter:'Alloy Smelter',metal_press:'Metal Press',sawmill:'Sawmill',solar_generator:'Solar Generator',energy_conduit:'Energy Conduit',item_transfer:'Item Transfer',digital_miner:'Survey Miner',storage_core:'Nexus Storage Core',storage_terminal:'Nexus Terminal',energy_cell:'Energy Cell',resonite:'Resonite Crystal',draconic_core:'Draconic Core (Concept)',chaotic_core:'Chaotic Core (Concept)'};
const title=n=>n.split('_').map(s=>s[0].toUpperCase()+s.slice(1)).join(' ');
const lang={'itemGroup.technologia':'Technologia','message.technologia.no_core':'No usable Nexus network. Connect 1-4 cores within 128 network blocks.', 'ui.technologia.toggle':'Start / Pause','ui.technologia.pause':'Pause','ui.technologia.start':'Start','ui.technologia.rescan':'Rescan'};
for(const n of blocks)lang['block.technologia.'+n]=names[n]??title(n);
for(const n of items)lang['item.technologia.'+n]=names[n]??title(n);
['Ready','Working','Needs power','Output is full','Needs input','Paused','Owner unavailable','Scan complete','Protected block skipped','Waiting for loaded chunk','Requires daylight and open sky'].forEach((s,i)=>lang['status.technologia.'+i]=s);
Object.assign(lang,{'hint.technologia.coal_generator':'Input: coal or charcoal. Outputs power to adjacent blocks.','hint.technologia.crusher':'Input: raw metal. Produces two dust per raw material.','hint.technologia.electric_furnace':'Input: any vanilla smelting recipe. Output slots follow.','hint.technologia.digital_miner':'Filter: ore or raw material. Empty = all ores. Starts paused.','hint.technologia.energy_cell':'Stores power. Connect consumers on any face.'});
Object.assign(lang,{'hint.technologia.alloy_smelter':'Two inputs: 3 copper + 1 tin makes 4 bronze. 1 iron + 2 coal dust makes steel.','hint.technologia.metal_press':'Input: iron, copper, gold, bronze or steel ingot. Produces one plate.','hint.technologia.sawmill':'Input: logs or wood. Produces six planks and one sawdust.','hint.technologia.solar_generator':'Produces 16 energy/tick in daylight under open sky. No fuel required.'});
Object.assign(lang, {'ui.technologia.input':'Input','ui.technologia.output':'Output','ui.technologia.filter':'Filter','tooltip.technologia.field_guide':'Use to read the workshop guide'});
const storageText={title:'Nexus storage',search:'Search items',search_hint:'Search name or mod:item',previous:'Previous page',next:'Next page',deposit:'Deposit held',deposit_hint:'Store the stack on your cursor. Shift-click your inventory to deposit directly.',sort_count:'Count (high)',sort_name:'Name (A-Z)',stored:'%s stored',withdraw_hint:'Left: stack | Right: one | Shift: to inventory',page:'%s / %s',status:'%s/%s types | %s items | %s/%s slots',empty:'Storage is empty',no_matches:'No matching items'};
for(const [key,value] of Object.entries(storageText))lang['ui.technologia.storage.'+key]=value;
lang['ui.technologia.storage.empty_hint']='Shift-click inventory items to deposit.';
lang['ui.technologia.storage.no_matches_hint']='Try another name or mod:item.';
Object.assign(lang,{
 'message.technologia.no_core':'No usable Nexus network. Connect 1-4 cores within 128 network blocks.',
 'message.technologia.transfer_filter':'Item Transfer filter: %s (sample retained)',
 'message.technologia.transfer_unfiltered':'Item Transfer: all items. Arrow points toward output; redstone pauses.',
 'message.technologia.inspect':'%s: %s / %s FE', 'message.technologia.rotated':'Facing changed; inventory preserved.',
 'status.technologia.10':'Needs clear daylight',
 'hint.technologia.compactor':'Packs materials into blocks; also presses sand into sandstone.',
 'hint.technologia.centrifuge':'Separates clay and gravel; extract bone meal and slime from biological inputs.',
 'hint.technologia.recycler':'Salvages metal nuggets from iron/gold tools and armor. Contents and enchantments are lost.',
 'hint.technologia.biomass_generator':'Fuel: sawdust, saplings, wheat, kelp or sugar cane. 20 FE/t at default settings.',
 'hint.technologia.advanced_energy_cell':'Stores 5,000,000 FE. One shared 200 FE/t output budget.',
 'hint.technologia.advanced_solar_generator':'Generates 64 FE/t in clear daylight with sky access.',
 'hint.technologia.energy_cell':'Stores 1,000,000 FE. Adjacent sinks and conduits share 200 FE/t.',
 'hint.technologia.coal_generator':'Coal/charcoal fuel. Use energy conduits to reach distant machines.'
});
json('assets/technologia/lang/en_us',lang);
const tag=(ns,type,n,values)=>json(`data/${ns}/tags/${type}/${n}`,{replace:false,values});
tag('minecraft','block','mineable/pickaxe',blocks.map(id));
tag('minecraft','block','needs_stone_tool',blocks.filter(n=>!n.includes('resonite')).map(id));
tag('minecraft','block','needs_iron_tool',[id('resonite_ore')]);
for(const type of ['block','item']){
 tag('c',type,'ores',blocks.filter(n=>n.endsWith('_ore')).map(id));
 for(const material of ['tin','lead','resonite'])tag('c',type,'ores/'+material,blocks.filter(n=>n.includes(material)&&n.endsWith('_ore')).map(id));
 // Forge 1.21 packs may still use forge tags; expose both namespaces.
 tag('forge',type,'ores',blocks.filter(n=>n.endsWith('_ore')).map(id));
}
// Add vanilla values so the common recipe/miner logic also works on bare Fabric.
for(const material of ['iron','gold','copper']){
 tag('c','item','raw_materials/'+material,['minecraft:raw_'+material]);
 for(const ns of ['c','forge']) {
  tag(ns,'item','ingots/'+material,['minecraft:'+material+'_ingot']);
  tag(ns,'item','raw_materials/'+material,['minecraft:raw_'+material]);
 }
 for(const type of ['block','item'])tag('c',type,'ores/'+material,[`minecraft:${material}_ore`,`minecraft:deepslate_${material}_ore`]);
}
for(const type of ['block','item'])tag('c',type,'ores',[...blocks.filter(n=>n.endsWith('_ore')).map(id),...['coal','iron','gold','copper','redstone','lapis','diamond','emerald'].flatMap(n=>['minecraft:'+n+'_ore','minecraft:deepslate_'+n+'_ore']), 'minecraft:nether_gold_ore','minecraft:nether_quartz_ore']);
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
const ingredient=s=>s.startsWith('#')?{tag:s.slice(1)}:{item:s.includes(':')?s:id(s)};
const craft=(n,pattern,key,count=1)=>json(`data/technologia/recipe/${n}`,{type:'minecraft:crafting_shaped',category:'misc',pattern,key:Object.fromEntries(Object.entries(key).map(([k,v])=>[k,ingredient(v)])),result:{id:id(n),count}});
json('data/technologia/recipe/field_guide',{type:'minecraft:crafting_shapeless',category:'misc',ingredients:[ingredient('minecraft:book'),ingredient('minecraft:copper_ingot')],result:{id:id('field_guide'),count:1}});
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
craft('energy_conduit',[' G ','PRP',' G '],{G:'minecraft:glass',P:'#c:plates/copper',R:'minecraft:redstone'},8);
craft('item_transfer',[' B ','PHP',' R '],{B:'#c:plates/bronze',P:'minecraft:piston',H:'minecraft:hopper',R:'minecraft:redstone'},2);
craft('energy_cell',['TLT','RFR','TLT'],{T:'#c:ingots/tin',L:'#c:ingots/lead',R:'minecraft:redstone_block',F:'machine_frame'});
craft('storage_core',['ICI','BFB','ICI'],{I:'minecraft:iron_ingot',C:'basic_circuit',B:'minecraft:chest',F:'machine_frame'});
craft('storage_terminal',['GGG','CFC','IRI'],{G:'minecraft:glass',C:'basic_circuit',F:'machine_frame',I:'minecraft:iron_ingot',R:'resonite'});
craft('network_cable',[' T ','CRC',' T '],{T:'#c:ingots/tin',C:'minecraft:copper_ingot',R:'minecraft:redstone'},8);
craft('digital_miner',['DAD','EFP','CRC'],{D:'#c:plates/steel',A:'advanced_circuit',E:'minecraft:ender_pearl',F:'machine_frame',P:'minecraft:diamond_pickaxe',C:'basic_circuit',R:'resonite'});
const processing=(name,machine,inputs,result,count,time,energy,byproduct)=>json(`data/technologia/recipe/${name}`,{type:'technologia:processing',machine,ingredients:inputs.map(([input,count])=>({ingredient:ingredient(input),count})),result:{id:result.includes(':')?result:id(result),count},time,energy,...(byproduct?{byproduct:{id:id(byproduct),count:1}}:{})});
for(const material of ['iron','gold','copper','tin','lead'])processing(`crushing/raw_${material}`,'crusher',[[`#c:raw_materials/${material}`,1]],`${material}_dust`,2,100,20);
processing('crushing/coal','crusher',[['minecraft:coal',1]],'coal_dust',1,60,20);
processing('alloying/bronze','alloy_smelter',[['#c:ingots/copper',3],['#c:ingots/tin',1]],'bronze_ingot',4,160,30);
processing('alloying/steel','alloy_smelter',[['#c:ingots/iron',1],['#c:dusts/coal',2]],'steel_ingot',1,240,30);
for(const material of ['iron','copper','gold','bronze','steel'])processing(`pressing/${material}`,'metal_press',[[`#c:ingots/${material}`,1]],`${material}_plate`,1,60,15);
for(const wood of ['oak','spruce','birch','jungle','acacia','dark_oak','mangrove','cherry','crimson','warped'])processing(`sawing/${wood}`,'sawmill',[[`#minecraft:${wood}_${['crimson','warped'].includes(wood)?'stems':'logs'}`,1]],`minecraft:${wood}_planks`,6,80,15,'sawdust');
json('data/technologia/recipe/charcoal_from_sawdust',{type:'minecraft:crafting_shapeless',category:'misc',ingredients:Array.from({length:9},()=>ingredient('sawdust')),result:{id:'minecraft:charcoal',count:1}});
// A larger, connected survival workshop.
craft('wrench',[' I ',' CI','C  '],{I:'minecraft:iron_ingot',C:'minecraft:copper_ingot'});
craft('compactor',['SPS','CFC','SPS'],{S:'#c:plates/steel',P:'minecraft:piston',C:'basic_circuit',F:'machine_frame'});
craft('centrifuge',['BIB','CFC','BRB'],{B:'#c:plates/bronze',I:'minecraft:iron_bars',C:'basic_circuit',F:'machine_frame',R:'resonite'});
craft('recycler',['BIB','CFC','BHB'],{B:'#c:plates/bronze',I:'minecraft:iron_bars',C:'basic_circuit',F:'machine_frame',H:'minecraft:hopper'});
craft('biomass_generator',['IBI','CFC','IRI'],{I:'minecraft:iron_ingot',B:'minecraft:composter',C:'basic_circuit',F:'machine_frame',R:'minecraft:furnace'});
craft('advanced_energy_cell',['SAS','ECE','SAS'],{S:'#c:plates/steel',A:'advanced_circuit',E:'energy_cell',C:'resonite'});
craft('advanced_solar_generator',['SSS','ACA','PFP'],{S:'solar_generator',A:'advanced_circuit',C:'resonite',P:'#c:plates/steel',F:'machine_frame'});
for(const n of decor)craft(n,['IPI','PGP','IPI'],{I:n==='bronze_casing'?'#c:ingots/bronze':'minecraft:iron_nugget',P:n==='hazard_block'?'minecraft:yellow_dye':'#c:plates/iron',G:n==='engineering_lamp'?'minecraft:glowstone':n==='reinforced_glass'?'minecraft:glass':'minecraft:stone'},8);
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
const milestones=[['root','machine_frame','A workshop worth building','Craft a Machine Frame.',null],['power','coal_generator','First spark','Build a combustion generator.','root'],['crushing','crusher','Break it down','Double raw metals into dust.','power'],['smelting','electric_furnace','Close the loop','Smelt dust into useful ingots.','crushing'],['alloys','alloy_smelter','Better together','Combine counted ingredients into alloys.','smelting'],['bronze','bronze_ingot','Age of bronze','Alloy three copper with one tin.','alloys'],['steel','steel_ingot','Steelworks','Alloy iron and coal dust.','alloys'],['press','metal_press','Under pressure','Form metal plates.','bronze'],['sawmill','sawmill','Waste becomes fuel','Saw logs into planks and sawdust.','press'],['biomass','biomass_generator','Circular workshop','Power equipment with renewable biomass.','sawmill'],['solar','solar_generator','Clear skies','Generate power without fuel.','press'],['conduits','energy_conduit','Wire the workshop','Route power through conduits.','press'],['transfer','item_transfer','Keep it moving','Transfer and filter adjacent inventories.','press'],['nexus','storage_terminal','One catalogue','Connect up to four storage cores.','root'],['compacting','compactor','Less space, more material','Compress bulk materials.','steel'],['centrifuge','centrifuge','Separate the useful','Process mineral and biological inputs.','press'],['recycler','recycler','Recover the metal','Salvage nuggets from old equipment.','press'],['advanced_power','advanced_energy_cell','Power reserve','Build a five-million-FE battery.','steel'],['advanced_solar','advanced_solar_generator','Solar field','Build a 64-FE/t solar array.','advanced_power'],['miner','digital_miner','Selective extraction','Build and configure the Survey Miner.','steel']];
for(const [key,item,title,description,parent] of milestones)json('data/technologia/advancement/workshop/'+key,{...(parent?{parent:'technologia:workshop/'+parent}:{}),display:{icon:{id:id(item)},title,description,frame:'task',show_toast:true,announce_to_chat:false,hidden:false,...(!parent?{background:'minecraft:textures/gui/advancements/backgrounds/stone.png'}:{})},criteria:{obtained:{trigger:'minecraft:inventory_changed',conditions:{items:[{items:id(item)}]}}},requirements:[['obtained']]});
buildFactoryModels({json,png,palette,machines});
buildFactoryShapes({fs,write,base,machines,decor});
for(const material of ['tin','lead'])for(const input of ['raw_'+material,material+'_ore','deepslate_'+material+'_ore',material+'_dust'])for(const type of ['smelting','blasting'])json(`data/technologia/recipe/${input}_${type}`,{type:'minecraft:'+type,category:'misc',ingredient:ingredient(input),result:{id:id(material+'_ingot'),count:1},experience:.5,cookingtime:type==='smelting'?200:100});
for(const material of ['iron','gold','copper'])for(const type of ['smelting','blasting'])json(`data/technologia/recipe/${material}_dust_${type}`,{type:'minecraft:'+type,category:'misc',ingredient:ingredient(material+'_dust'),result:{id:'minecraft:'+material+'_ingot',count:1},experience:.1,cookingtime:type==='smelting'?200:100});
for(const [n,size,count,min,max] of [['tin',8,10,-32,80],['lead',6,7,-48,32],['resonite',4,4,-56,0]]){
 const targets=['stone','deepslate'].map(stone=>({target:{predicate_type:'minecraft:tag_match',tag:`minecraft:${stone}_ore_replaceables`},state:{Name:id((stone==='deepslate'&&n!=='resonite'?'deepslate_':'')+n+'_ore')}}));
 json(`data/technologia/worldgen/configured_feature/${n}_ore`,{type:'minecraft:ore',config:{size,discard_chance_on_air_exposure:0,targets}});
 json(`data/technologia/worldgen/placed_feature/${n}_ore`,{feature:id(n+'_ore'),placement:[{type:'minecraft:count',count},{type:'minecraft:in_square'},{type:'minecraft:height_range',height:{type:'minecraft:trapezoid',min_inclusive:{absolute:min},max_inclusive:{absolute:max}}},{type:'minecraft:biome'}]});
 for(const loader of ['forge','neoforge'])write(`${loader}/src/main/resources/data/technologia/${loader}/biome_modifier/${n}_ore.json`,JSON.stringify({type:loader+':add_features',biomes:'#minecraft:is_overworld',features:id(n+'_ore'),step:'underground_ores'},null,2)+'\n');
}
write(`${base}/pack.mcmeta`,JSON.stringify({pack:{pack_format:34,description:'Technologia resources'}})+'\n');
write('fabric/src/main/resources/fabric.mod.json',JSON.stringify({schemaVersion:1,id:'technologia',version:'${version}',name:'Technologia',description:'Industrial automation with a unified visual language.',authors:['Technologia'],license:'All Rights Reserved',environment:'*',accessWidener:'technologia.accesswidener',entrypoints:{main:['dev.technologia.TechnologiaFabric'],client:['dev.technologia.TechnologiaFabricClient']},depends:{fabricloader:'>=0.16.9',minecraft:'1.21.1',java:'>=21','fabric-api':'*','team_reborn_energy':'>=4.1.0'}},null,2)+'\n');
for(const loader of ['forge','neoforge'])write(`${loader}/src/main/resources/META-INF/${loader==='forge'?'mods':'neoforge.mods'}.toml`, `modLoader="javafml"\nloaderVersion="${loader==='forge'?'[52,)':'[4,)'}"\nlicense="All Rights Reserved"\n[[mods]]\nmodId="technologia"\nversion="\${version}"\ndisplayName="Technologia"\nauthors="Technologia"\ndescription='''Industrial automation with a unified visual language.'''\n[[dependencies.technologia]]\nmodId="${loader}"\n${loader==='forge'?'mandatory=true':'type="required"'}\nversionRange="${loader==='forge'?'[52.0.28,)':'[21.1.80,)'}"\nordering="NONE"\nside="BOTH"\n[[dependencies.technologia]]\nmodId="minecraft"\n${loader==='forge'?'mandatory=true':'type="required"'}\nversionRange="[1.21.1,1.21.2)"\nordering="NONE"\nside="BOTH"\n`);
console.log(`Generated ${blocks.length} blocks, ${items.length} items, three ores and loader metadata.`);
