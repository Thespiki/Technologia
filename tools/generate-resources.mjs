// Deterministic, original 32px prototype art and Minecraft 1.21.1 data.
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
const base='common/src/main/resources';
const write=(p,s)=>{fs.mkdirSync(path.dirname(p),{recursive:true});fs.writeFileSync(p,s)};
const json=(p,o)=>write(`${base}/${p}.json`,JSON.stringify(o,null,2)+'\n');
const id=n=>'technologia:'+n;
const blocks=['machine_frame','coal_generator','crusher','electric_furnace','digital_miner','storage_core','storage_terminal','energy_cell','network_cable','tin_ore','deepslate_tin_ore','lead_ore','deepslate_lead_ore','resonite_ore'];
const items=['raw_tin','raw_lead','tin_ingot','lead_ingot','resonite','iron_dust','gold_dust','copper_dust','tin_dust','lead_dust','basic_circuit','advanced_circuit','draconic_core','chaotic_core'];
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
png('block/drive_bay',r=>{r(0,0,32,32,palette.dark);r(1,3,30,26,palette.rim);r(3,5,26,22,palette.base);r(5,11,17,3,palette.light);r(5,17,12,2,palette.rim);r(25,10,3,5,palette.cyan);r(25,18,3,4,palette.light)});
png('block/terminal_screen',r=>{r(0,0,32,32,palette.dark);r(1,1,30,30,[25,59,66]);for(let y=5;y<26;y+=6){r(4,y,3,3,palette.cyan);r(10,y,15-(y%4),2,palette.light)}r(27,4,1,24,palette.rim);r(27,4,1,9,palette.cyan)});
// Static cuboids give real depth without a per-frame block entity renderer.
const cuboid=(from,to,texture='#casing',front=null)=>({from,to,faces:Object.fromEntries(['down','up','north','south','west','east'].map(face=>[face,{texture:face==='north'&&front?front:texture,...(face==='north'&&front?{uv:[0,0,16,16]}:{})}]))});
function recessedModel(name){
 const elements=[cuboid([0,0,2.5],[16,16,16]),cuboid([0,0,0],[2,16,2.5]),cuboid([14,0,0],[16,16,2.5]),cuboid([2,0,0],[14,2,2.5]),cuboid([2,14,0],[14,16,2.5])];
 if(name==='storage_core')for(const x of [2.5,8.5])for(const y of [2.5,6.5,10.5])elements.push(cuboid([x,y,1],[x+5,y+3,2.5],'#casing','#detail'));
 else {elements.push(cuboid([2,5,1.7],[14,14,2.5],'#casing','#detail'));elements.push(cuboid([3,2.5,0.5],[13,4,2.5],'#casing'));}
 return {parent:'minecraft:block/block',ambientocclusion:true,textures:{particle:'technologia:block/casing',casing:'technologia:block/casing',detail:`technologia:block/${name==='storage_core'?'drive_bay':'terminal_screen'}`},elements};
}
for(const n of blocks){
 const ore=n.includes('_ore');
 png('block/'+n,r=>{
  if(ore){const deep=n.startsWith('deepslate');for(let y=0;y<32;y++)for(let x=0;x<32;x++){let q=(x*17+y*31+x*y*3)%19+(deep?43:100);r(x,y,1,1,[q,q+2,q+3])}const c=n.includes('tin')?[180,213,209]:n.includes('lead')?[136,133,172]:palette.cyan;for(const [x,y] of [[7,6],[22,9],[12,18],[25,24],[3,26]]){r(x-1,y-1,5,4,palette.dark);r(x,y,4,3,c);r(x,y,2,1,palette.light)}return;}
  casing(r);const c=['crusher','electric_furnace','coal_generator'].includes(n)?palette.amber:palette.cyan;
  r(5,6,22,18,palette.dark);r(6,26,16,2,c);r(25,26,2,2,c);
  if(n==='crusher'){for(let y=9;y<23;y+=4){r(8,y,7,2,c);r(17,y+1,7,2,palette.light)}}
  else if(n==='electric_furnace'||n==='coal_generator'){r(9,12,14,10,palette.rim);r(11,15,10,5,c);r(14,10,4,10,c)}
  else if(n==='digital_miner'){r(15,8,2,14,c);r(9,14,14,2,c);r(11,10,10,2,palette.rim);r(11,19,10,2,palette.rim)}
  else if(n==='energy_cell'){r(10,9,12,13,palette.rim);for(let y=11;y<22;y+=4)r(12,y,8,2,c)}
  else if(n==='storage_core'){for(let y=9;y<23;y+=5){r(8,y,16,3,palette.rim);r(21,y,2,2,c)}}
  else if(n==='storage_terminal'){r(8,9,16,12,[34,76,83]);for(let y=11;y<19;y+=3)r(10,y,7+(y%2)*4,1,c);r(11,23,10,1,palette.light)}
  else {r(12,10,8,10,palette.rim);r(14,12,4,6,c)}
 });
 json(`assets/technologia/blockstates/${n}`,{variants:{'':{model:`technologia:block/${n}`}}});
 json(`assets/technologia/models/block/${n}`,['storage_core','storage_terminal'].includes(n)?recessedModel(n):ore?{parent:'minecraft:block/cube_all',textures:{all:`technologia:block/${n}`}}:{parent:'minecraft:block/cube',textures:{particle:`technologia:block/${n}`,down:'technologia:block/casing',up:'technologia:block/top',north:`technologia:block/${n}`,south:'technologia:block/casing',east:'technologia:block/casing',west:'technologia:block/casing'}});
 json(`assets/technologia/models/item/${n}`,{parent:`technologia:block/${n}`});
 const drop=ore?(n.includes('tin')?'raw_tin':n.includes('lead')?'raw_lead':'resonite'):n;
 const entry=ore?{type:'minecraft:alternatives',children:[{type:'minecraft:item',name:id(n),conditions:[{condition:'minecraft:match_tool',predicate:{predicates:{'minecraft:enchantments':[{enchantments:'minecraft:silk_touch',levels:{min:1}}]}}}]},{type:'minecraft:item',name:id(drop),functions:[{function:'minecraft:apply_bonus',enchantment:'minecraft:fortune',formula:'minecraft:ore_drops'},{function:'minecraft:explosion_decay'}]}]}:{type:'minecraft:item',name:id(drop)};
 json(`data/technologia/loot_table/blocks/${n}`,{type:'minecraft:block',pools:[{rolls:1,entries:[entry],...(!ore?{conditions:[{condition:'minecraft:survives_explosion'}]}:{})}]});
}
for(const n of items){
 let c=n.includes('gold')?palette.amber:n.includes('copper')?[221,139,99]:n.includes('lead')?[148,138,186]:n.includes('chaotic')?palette.violet:n.includes('draconic')?[236,136,73]:n.includes('circuit')||n==='resonite'?palette.cyan:palette.light;
 png('item/'+n,r=>{
  if(n.endsWith('dust')){r(7,21,18,4,palette.dark);r(9,17,14,6,c);r(13,13,6,5,c);r(5,24,4,2,c);r(25,21,3,2,c)}
  else if(n.includes('circuit')){r(6,6,20,20,palette.dark);r(8,8,16,16,[37,75,72]);r(12,12,8,8,c);for(let x=10;x<25;x+=5){r(x,4,2,3,palette.amber);r(x,25,2,3,palette.amber)}}
  else if(n.endsWith('core')){r(11,4,10,24,palette.dark);r(5,10,22,12,palette.dark);r(10,8,12,16,c);r(7,12,18,8,c);r(13,12,6,8,[246,244,240])}
  else if(n.endsWith('ingot')){r(5,14,22,10,palette.dark);r(7,13,18,8,c);r(10,10,14,4,c);r(10,11,12,2,[225,233,238])}
  else {r(12,5,8,22,palette.dark);r(7,12,18,10,palette.dark);r(13,7,6,18,c);r(9,13,14,7,c);r(13,8,2,9,[226,238,244])}
 });
 json(`assets/technologia/models/item/${n}`,{parent:'minecraft:item/generated',textures:{layer0:`technologia:item/${n}`}});
}
const names={coal_generator:'Combustion Generator',crusher:'Ore Crusher',electric_furnace:'Electric Furnace',digital_miner:'Survey Miner',storage_core:'Nexus Storage Core',storage_terminal:'Nexus Terminal',energy_cell:'Energy Cell',resonite:'Resonite Crystal',draconic_core:'Draconic Core (Concept)',chaotic_core:'Chaotic Core (Concept)'};
const title=n=>n.split('_').map(s=>s[0].toUpperCase()+s.slice(1)).join(' ');
const lang={'itemGroup.technologia':'Technologia','message.technologia.no_core':'No storage core found. Connect it with Network Cable.', 'ui.technologia.toggle':'Start / Pause','ui.technologia.pause':'Pause','ui.technologia.start':'Start','ui.technologia.rescan':'Rescan'};
for(const n of blocks)lang['block.technologia.'+n]=names[n]??title(n);
for(const n of items)lang['item.technologia.'+n]=names[n]??title(n);
['Ready','Working','Needs power','Output is full','Needs input','Paused','Owner unavailable','Scan complete','Protected block skipped','Waiting for loaded chunk'].forEach((s,i)=>lang['status.technologia.'+i]=s);
Object.assign(lang,{'hint.technologia.coal_generator':'Input: coal or charcoal. Outputs power to adjacent blocks.','hint.technologia.crusher':'Input: raw metal. Produces two dust per raw material.','hint.technologia.electric_furnace':'Input: any vanilla smelting recipe. Output slots follow.','hint.technologia.digital_miner':'Filter: ore or raw material. Empty = all ores. Starts paused.','hint.technologia.energy_cell':'Stores power. Connect consumers on any face.'});
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
for(const material of ['iron','gold','copper','tin','lead'])for(const ns of ['c','forge'])tag(ns,'item','dusts/'+material,[id(material+'_dust')]);
for(const ns of ['c','forge']){
 tag(ns,'item','ingots',['iron','gold','copper','tin','lead'].map(n=>'#'+ns+':ingots/'+n));
 tag(ns,'item','raw_materials',['iron','gold','copper','tin','lead'].map(n=>'#'+ns+':raw_materials/'+n));
 tag(ns,'item','dusts',['iron','gold','copper','tin','lead'].map(n=>'#'+ns+':dusts/'+n));
}
const ingredient=s=>s.startsWith('#')?{tag:s.slice(1)}:{item:s.includes(':')?s:id(s)};
const craft=(n,pattern,key,count=1)=>json(`data/technologia/recipe/${n}`,{type:'minecraft:crafting_shaped',category:'misc',pattern,key:Object.fromEntries(Object.entries(key).map(([k,v])=>[k,ingredient(v)])),result:{id:id(n),count}});
craft('machine_frame',['ICI','C C','ICI'],{I:'minecraft:iron_ingot',C:'minecraft:copper_ingot'});
craft('basic_circuit',[' R ','CTC',' R '],{R:'minecraft:redstone',C:'minecraft:copper_ingot',T:'#c:ingots/tin'},2);
craft('advanced_circuit',['RGR','CBC','RGR'],{R:'resonite',G:'minecraft:gold_ingot',C:'basic_circuit',B:'minecraft:redstone_block'});
craft('coal_generator',['III','CFC','IRI'],{I:'minecraft:iron_ingot',C:'minecraft:coal',F:'machine_frame',R:'minecraft:furnace'});
craft('crusher',['IPI','CFC','IRI'],{I:'minecraft:iron_ingot',P:'minecraft:piston',C:'basic_circuit',F:'machine_frame',R:'minecraft:redstone'});
craft('electric_furnace',['III','CFC','IRI'],{I:'minecraft:copper_ingot',C:'basic_circuit',F:'machine_frame',R:'minecraft:furnace'});
craft('energy_cell',['TLT','RFR','TLT'],{T:'#c:ingots/tin',L:'#c:ingots/lead',R:'minecraft:redstone_block',F:'machine_frame'});
craft('storage_core',['ICI','BFB','ICI'],{I:'minecraft:iron_ingot',C:'basic_circuit',B:'minecraft:chest',F:'machine_frame'});
craft('storage_terminal',['GGG','CFC','IRI'],{G:'minecraft:glass',C:'basic_circuit',F:'machine_frame',I:'minecraft:iron_ingot',R:'resonite'});
craft('network_cable',[' T ','CRC',' T '],{T:'#c:ingots/tin',C:'minecraft:copper_ingot',R:'minecraft:redstone'},8);
craft('digital_miner',['DAD','EFP','CRC'],{D:'minecraft:diamond',A:'advanced_circuit',E:'minecraft:ender_pearl',F:'machine_frame',P:'minecraft:iron_pickaxe',C:'basic_circuit',R:'resonite'});
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
