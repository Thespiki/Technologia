// Structural checks on the generated resources. Fails with a message naming the file and the broken reference.
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import assert from 'node:assert/strict';
import {fileURLToPath} from 'node:url';
// Paths are resolved from this file, so the checks run from any working directory.
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..').replaceAll('\\','/');
const base=`${root}/common/src/main/resources`;
const assets=`${base}/assets/technologia`,data=`${base}/data/technologia`;
function walk(p){return fs.readdirSync(p,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(`${p}/${e.name}`):[`${p}/${e.name}`])}
const read=f=>JSON.parse(fs.readFileSync(f,'utf8'));
const show=f=>f.slice(root.length+1);
const name=f=>path.basename(f,path.extname(f));
const files=walk(base); let count=0;
const lang=read(`${assets}/lang/en_us.json`);
const hasItemModel=item=>fs.existsSync(`${assets}/models/item/${item}.json`);
const hasBlockstate=block=>fs.existsSync(`${assets}/blockstates/${block}.json`);
// The registries live in Java. Block names are read from the source, so a block that is registered without
// resources fails here instead of showing up in game as a missing model that drops nothing.
const sources=['common','fabric','forge','neoforge'].flatMap(module=>walk(`${root}/${module}/src/main/java`)).filter(f=>f.endsWith('.java'));
const java=f=>fs.readFileSync(`${root}/common/src/main/java/dev/technologia/${f}`,'utf8');
const quoted=text=>[...text.matchAll(/"([a-z0-9_]+)"/g)].map(match=>match[1]);
const registry=java('Technologia.java');
const listed=constant=>quoted(registry.match(new RegExp(`\\b${constant} = List\\.of\\(([^;]*)\\);`))?.[1]??'');
const machineIds=[...java('machine/MachineKind.java').matchAll(/\b[A-Z_]+\("([a-z0-9_]+)", *\d+\)/g)].map(match=>match[1]);
// The recipe codec accepts the processors of MachineKind.isProcessor() except the Electric Furnace, which uses vanilla smelting.
const kindIds=Object.fromEntries([...java('machine/MachineKind.java').matchAll(/\b([A-Z_]+)\("([a-z0-9_]+)", *\d+\)/g)].map(match=>[match[1],match[2]]));
const recipeMachines=[...(java('machine/MachineKind.java').match(/isProcessor\(\) \{([^}]*)\}/)?.[1]??'').matchAll(/this == ([A-Z_]+)/g)].map(match=>kindIds[match[1]]).filter(machine=>machine&&machine!=='electric_furnace');
assert.ok(recipeMachines.length>=8,'No processors found in MachineKind.isProcessor(): the pattern above no longer matches the source');
const registered=[...new Set([...[...registry.matchAll(/addBlock\("([a-z0-9_]+)"/g)].map(match=>match[1]),...machineIds,...listed('DECOR'),...listed('ORES'),...[...registry.matchAll(/crates\.put\("([a-z0-9_]+)"/g)].map(match=>match[1])])];
assert.ok(machineIds.length&&registered.length>machineIds.length,'No block names found in Technologia.java and MachineKind.java: the patterns above no longer match the source');
const tierCount=read(`${base}/technologia/tiers.json`).tiers.length;
// Machines with two ingredients per lane (MachineKind.inputsPerLane), and the sieve mesh names by level, from level 1.
const twoInputs=['alloy_smelter','metallurgic_infuser'],meshNames=['flint','iron','diamond'];
// An ingredient is {item}, {tag} or a list of those; returns 'ns:path' for items and '#ns:path' for tags.
const ingredientIds=value=>Array.isArray(value)?value.flatMap(ingredientIds):value?.item?[value.item]:value?.tag?['#'+value.tag]:[];
const recipes=[],shaped=new Map(),shapeless=new Map(),processing=new Map(),advancements=[];
for(const f of files.filter(f=>f.endsWith('.json'))){const json=read(f); count++;
 if(f.includes('/recipe/')){recipes.push(f);assert.ok(json.type,`${show(f)}: recipe has no type`);assert.ok(json.result.id,`${show(f)}: recipe has no result`);assert.ok(json.result.count>0,`${show(f)}: recipe result count must be positive`);
  const inputs=[...Object.values(json.key??{}),...(json.ingredients??[]).map(entry=>entry.ingredient??entry),...(json.ingredient?[json.ingredient]:[])].flatMap(ingredientIds);
  for(const used of [...inputs,json.result.id,...(json.byproduct?[json.byproduct.id]:[])]){
   if(used.startsWith('technologia:'))assert.ok(hasItemModel(used.slice(12)),`${show(f)}: ${used} has no item model, so the item does not exist`);
   if(used.startsWith('#c:')||used.startsWith('#technologia:')){const [ns,tag]=used.slice(1).split(':');assert.ok(fs.existsSync(`${base}/data/${ns}/tags/item/${tag}.json`),`${show(f)}: ingredient tag ${used} has no tag file`);}
  }
  // Two crafting recipes with the same grid, or the same shapeless ingredients, would shadow each other.
  if(json.type==='minecraft:crafting_shaped'||json.type==='technologia:machine_crafting'){const grid=JSON.stringify(json.pattern.map(row=>[...row].map(symbol=>symbol===' '?'':ingredientIds(json.key[symbol]).join('|'))));assert.ok(!shaped.has(grid),`${show(f)}: same pattern and key as ${shaped.get(grid)}`);shaped.set(grid,show(f));}
  if(json.type==='minecraft:crafting_shapeless'){const set=JSON.stringify(json.ingredients.map(entry=>ingredientIds(entry).join('|')).sort());assert.ok(!shapeless.has(set),`${show(f)}: same shapeless ingredients as ${shapeless.get(set)}`);shapeless.set(set,show(f));}
  // The game drops a processing recipe that its codec refuses, and says so only in the log. The same limits are checked here.
  if(json.type==='technologia:processing'){
   const whole=(value,min,max)=>Number.isInteger(value)&&value>=min&&value<=max,mesh=json.mesh??0;
   assert.ok(recipeMachines.includes(json.machine),`${show(f)}: ${json.machine} is not a machine that takes processing recipes`);
   assert.ok(whole(json.result?.count??1,1,64),`${show(f)}: result count must be 1 to 64`);
   assert.equal(json.ingredients.length,twoInputs.includes(json.machine)?2:1,`${show(f)}: wrong number of ingredients for ${json.machine}`);
   for(const entry of json.ingredients)assert.ok(whole(entry.count,1,64),`${show(f)}: ingredient count must be 1 to 64`);
   assert.ok(whole(json.time,1,12000)&&whole(json.energy,1,100000),`${show(f)}: time must be 1 to 12000 and energy 1 to 100000`);
   if(json.byproduct)assert.ok(whole(json.byproduct.count,1,64),`${show(f)}: byproduct count must be 1 to 64`);
   if('byproduct_chance' in json)assert.ok(json.byproduct&&json.byproduct_chance>0&&json.byproduct_chance<=1,`${show(f)}: byproduct_chance needs a byproduct and a value above 0, at most 1`);
   // Sieve meshes: level 0 is the built-in string mesh; a recipe for a better mesh carries the mesh name.
   assert.ok(whole(mesh,0,meshNames.length),`${show(f)}: mesh must be a whole number from 0 to ${meshNames.length}`);
   assert.ok(mesh===0||json.machine==='auto_sieve',`${show(f)}: only sieve recipes take a mesh`);
   if(mesh>0){assert.ok(hasItemModel(meshNames[mesh-1]+'_mesh'),`${show(f)}: mesh ${mesh} has no ${meshNames[mesh-1]}_mesh item`);assert.ok(name(f).endsWith('_'+meshNames[mesh-1]),`${show(f)}: a mesh ${mesh} recipe is named <block>_${meshNames[mesh-1]}`);}
   // Same machine, same counted ingredients and same mesh: the machine could not tell the two recipes apart.
   const key=JSON.stringify([json.machine,mesh,...json.ingredients.map(entry=>ingredientIds(entry.ingredient).join('|')+'x'+entry.count).sort()]);
   assert.ok(!processing.has(key),`${show(f)}: same machine, ingredients and mesh as ${processing.get(key)}`);processing.set(key,show(f));
  }
 }
 if(f.includes('/models/'))for(const texture of Object.values(json.textures??{}))if(texture.startsWith('technologia:'))assert.ok(fs.existsSync(`${assets}/textures/${texture.split(':')[1]}.png`),`${show(f)}: missing texture ${texture}`);
 // Covers both kinds of blockstate: every model named by a variant or by a multipart part must exist.
 if(f.includes('/blockstates/'))for(const variant of [...Object.values(json.variants??{}),...(json.multipart??[]).map(part=>part.apply)].flat())assert.ok(fs.existsSync(`${assets}/models/${variant.model.split(':')[1]}.json`),`${show(f)}: missing model ${variant.model}`);
 if(f.includes('/tags/'))for(const value of json.values??[])if(typeof value==='string'){const block=f.includes('/tags/block/');
  if(value.startsWith('#c:'))assert.ok(fs.existsSync(`${base}/data/c/tags/${block?'block':'item'}/${value.slice(3)}.json`),`${show(f)}: missing tag ${value}`);
  if(value.startsWith('technologia:'))assert.ok(block?hasBlockstate(value.slice(12)):hasItemModel(value.slice(12)),`${show(f)}: ${value} does not exist`);
 }
 if(f.includes('/loot_table/'))for(const [,drop] of JSON.stringify(json).matchAll(/"name":"technologia:([a-z0-9_]+)"/g))assert.ok(hasItemModel(drop),`${show(f)}: drops technologia:${drop}, which has no item model`);
 if(f.includes('/advancement/')){advancements.push(f);
  if(json.parent){const [ns,parent]=json.parent.split(':');assert.ok(fs.existsSync(`${base}/data/${ns}/advancement/${parent}.json`),`${show(f)}: parent advancement ${json.parent} does not exist`);}
  const icon=json.display.icon.id;if(icon.startsWith('technologia:'))assert.ok(hasItemModel(icon.slice(12)),`${show(f)}: icon ${icon} has no item model`);
  for(const text of [json.display.title,json.display.description])if(text.translate)assert.ok(lang[text.translate],`${show(f)}: no translation for ${text.translate}`);
 }
}
// Every block in the game needs a blockstate, a name, a loot table and an item model, or it shows a raw key, a missing model, or drops nothing.
const blockstates=files.filter(f=>f.includes('/blockstates/'));
for(const block of registered){
 assert.ok(hasBlockstate(block),`${block} is registered in Java but has no blockstate`);
 assert.ok(lang['block.technologia.'+block],`${block}: no translation block.technologia.${block}`);
 assert.ok(fs.existsSync(`${data}/loot_table/blocks/${block}.json`),`${block}: block has no loot table`);
 assert.ok(hasItemModel(block),`${block}: block has no item model`);
}
for(const f of blockstates)assert.ok(registered.includes(name(f)),`${show(f)}: no block of that name is registered (names are read from addBlock, MachineKind, DECOR, ORES and crates() in Java)`);
const itemModels=files.filter(f=>f.includes('/models/item/'));
for(const f of itemModels)assert.ok(lang['item.technologia.'+name(f)]||lang['block.technologia.'+name(f)],`${show(f)}: no item.technologia.${name(f)} or block.technologia.${name(f)} translation`);
// Every state of a block needs a model. Machines carry facing, active and tier, so their blockstate is multipart:
// exactly one base part per facing and active, and one tier band part per tier above the first and facing.
const facings=['north','east','south','west'],turn={north:0,east:90,south:180,west:270};
const machines=blockstates.filter(f=>(read(f).multipart??[]).some(part=>part.when&&'active' in part.when)).map(name);
for(const machine of machineIds)assert.ok(machines.includes(machine),`blockstates/${machine}.json: a machine has facing, active and tier, so its blockstate must be multipart with parts for facing and active`);
assert.equal(machines.length,machineIds.length,`${machines.find(machine=>!machineIds.includes(machine))} has an active state but is not a machine of MachineKind`);
for(const machine of machines){const f=`${assets}/blockstates/${machine}.json`,parts=read(f).multipart;
 const matching=when=>parts.filter(part=>Object.entries(when).every(([property,value])=>part.when?.[property]===value)&&Object.keys(part.when).length===Object.keys(when).length);
 for(const facing of facings){
  for(const active of ['false','true']){const found=matching({facing,active});assert.equal(found.length,1,`${show(f)}: needs exactly one part for facing=${facing},active=${active}`);
   assert.equal(found[0].apply.model,`technologia:block/${machine}${active==='true'?'_active':''}`,`${show(f)}: wrong model for facing=${facing},active=${active}`);
   assert.equal(found[0].apply.y??0,turn[facing],`${show(f)}: wrong turn for facing=${facing},active=${active}`);}
  for(let tier=1;tier<tierCount;tier++){const found=matching({facing,tier:String(tier)});assert.equal(found.length,1,`${show(f)}: needs exactly one tier band part for tier=${tier},facing=${facing}`);
   assert.equal(found[0].apply.model,`technologia:block/tier_band_${tier}`,`${show(f)}: wrong band model for tier=${tier},facing=${facing}`);
   assert.equal(found[0].apply.y??0,turn[facing],`${show(f)}: wrong band turn for tier=${tier},facing=${facing}`);}
 }
 assert.equal(parts.length,facings.length*(tierCount+1),`${show(f)}: has parts that belong to no facing, active or tier value`);
}
const stateValues={bonsai_pot:['stage',[0,1,2,3]],hand_sieve:['fill',[0,1,2,3,4]],vector_plate:['facing',facings],fast_vector_plate:['facing',facings]};
for(const [block,[property,values]] of Object.entries(stateValues))for(const value of values)assert.ok(read(`${assets}/blockstates/${block}.json`).variants?.[`${property}=${value}`],`blockstates/${block}.json: no variant for ${property}=${value}`);
// The tier band is drawn between the front feet. No machine model may reach into that box, or the two would overlap.
const band=read(`${assets}/models/block/tier_band_1.json`).elements[0];
for(const machine of machines)for(const suffix of ['','_active'])for(const element of read(`${assets}/models/block/${machine}${suffix}.json`).elements)
 assert.ok(![0,1,2].every(axis=>element.from[axis]<band.to[axis]&&element.to[axis]>band.from[axis]),`models/block/${machine}${suffix}.json: an element enters the tier band space`);
// With the block's own name masked, no two machine models may match, and no two front textures.
const distinct=(what,entries)=>{const seen=new Map();for(const [label,content] of entries){assert.ok(!seen.has(content),`${label} is identical to ${seen.get(content)} (${what})`);seen.set(content,label);}};
distinct('machine block models, block name masked',machines.flatMap(machine=>['','_active'].map(suffix=>[`models/block/${machine}${suffix}.json`,fs.readFileSync(`${assets}/models/block/${machine}${suffix}.json`,'utf8').replaceAll(machine,'*')])));
distinct('machine front textures',machines.flatMap(machine=>['','_active'].map(suffix=>[`textures/block/${machine}${suffix}.png`,fs.readFileSync(`${assets}/textures/block/${machine}${suffix}.png`).toString('base64')])));
const pngs=files.filter(f=>f.endsWith('.png'));
distinct('item icons',pngs.filter(f=>f.includes('/textures/item/')).map(f=>[show(f),fs.readFileSync(f).toString('base64')]));
for(const n of ['draconic_core','chaotic_core'])assert.ok(!fs.existsSync(`${data}/recipe/${n}.json`),`${n} is a concept item and must not be craftable`);
// A texture with see-through pixels only looks right on the cutout layer. Forge and NeoForge read that from the model's
// 'render_type'; Fabric reads the list Technologia.CUTOUT_BLOCKS. Both must name every block that needs it.
const seeThrough=new Map();
const hasHoles=texture=>{
 if(!seeThrough.has(texture)){const png=fs.readFileSync(`${assets}/textures/${texture}.png`),chunks=[];let width=0;
  for(let at=8;at<png.length;at+=12+png.readUInt32BE(at)){const type=png.toString('latin1',at+4,at+8),body=png.subarray(at+8,at+8+png.readUInt32BE(at));if(type==='IHDR')width=body.readUInt32BE(0);if(type==='IDAT')chunks.push(body);}
  // The generator writes unfiltered 8-bit RGBA rows: one filter byte (0), then four bytes per pixel, alpha last.
  const rows=zlib.inflateSync(Buffer.concat(chunks)),stride=width*4+1;let holes=false;
  for(let at=0;at<rows.length;at++){const column=at%stride;
   if(column===0)assert.equal(rows[at],0,`textures/${texture}.png: not written by the generator (filtered rows)`);
   else if(column%4===0&&rows[at]<255)holes=true;}
  seeThrough.set(texture,holes);}
 return seeThrough.get(texture);
};
const cutout=listed('CUTOUT_BLOCKS');
for(const block of cutout)assert.ok(registered.includes(block),`Technologia.CUTOUT_BLOCKS names ${block}, which is not a block`);
for(const block of registered){const state=read(`${assets}/blockstates/${block}.json`);
 for(const {model} of [...Object.values(state.variants??{}),...(state.multipart??[]).map(part=>part.apply)].flat()){
  if(!model.startsWith('technologia:'))continue;
  const json=read(`${assets}/models/${model.slice(12)}.json`),textures=json.textures??{};
  // Only the textures a face uses count; a model without elements takes its faces from its parent and uses them all.
  const keys=json.elements?[...new Set(json.elements.flatMap(element=>Object.values(element.faces).map(face=>face.texture.slice(1))))]:Object.keys(textures);
  const resolve=key=>textures[key]?.startsWith('#')?resolve(textures[key].slice(1)):textures[key];
  const holes=keys.map(resolve).filter(texture=>texture?.startsWith('technologia:')&&hasHoles(texture.slice(12)));
  if(holes.length){
   assert.equal(json.render_type,'minecraft:cutout',`models/${model.slice(12)}.json: ${holes[0]} has see-through pixels, so the model needs "render_type": "minecraft:cutout"`);
   assert.ok(cutout.includes(block),`${block}: ${holes[0]} has see-through pixels, so Technologia.CUTOUT_BLOCKS must name the block`);
  }
 }
}
// Each block is dug with one tool; only the listed blocks break by hand and need no tool tag.
const tools=['pickaxe','axe','shovel'].map(tool=>read(`${base}/data/minecraft/tags/block/mineable/${tool}.json`).values);
for(const block of registered){const tagged=tools.filter(values=>values.includes('technologia:'+block)).length;
 assert.equal(tagged,['resonance_bloom','bonsai_pot'].includes(block)?0:1,`${block}: must be in exactly one mineable tag, or in none if it breaks by hand`);}
// Ore generation: each placed feature points at a configured feature that places registered blocks, Forge and NeoForge
// each add it with a biome modifier, and Fabric adds the names in Technologia.ORE_FEATURES in code.
const features=fs.readdirSync(`${data}/worldgen/placed_feature`).map(f=>name(f));
for(const feature of features){
 const configured=`${data}/worldgen/configured_feature/${read(`${data}/worldgen/placed_feature/${feature}.json`).feature.split(':')[1]}.json`;
 assert.ok(fs.existsSync(configured),`worldgen/placed_feature/${feature}.json: its configured feature does not exist`);
 for(const target of read(configured).config.targets)assert.ok(registered.includes(target.state.Name.split(':')[1]),`${show(configured)}: places ${target.state.Name}, which is not a block`);
 for(const loader of ['forge','neoforge']){const modifier=`${root}/${loader}/src/main/resources/data/technologia/${loader}/biome_modifier/${feature}.json`;
  assert.ok(fs.existsSync(modifier)&&read(modifier).features==='technologia:'+feature,`${loader}: no biome modifier adds technologia:${feature}`);}
}
assert.deepEqual([...listed('ORE_FEATURES')].sort(),[...features].sort(),'Technologia.ORE_FEATURES must name exactly the placed features');
// Translation keys used by the Java code: a whole key must exist; a key built at run time ends in '.' and needs at least one entry.
for(const f of sources)for(const [,key] of fs.readFileSync(f,'utf8').matchAll(/"((?:block|item|itemGroup|status|ui|hint|message|tooltip|advancement)\.technologia[a-z0-9_.]*)"/g))
 assert.ok(key.endsWith('.')?Object.keys(lang).some(entry=>entry.startsWith(key)):lang[key],`${show(f)}: no translation for ${key}`);
// Numbered texts are built at run time, so each number the code can produce needs its own entry.
const machineSource=java('machine/MachineBlockEntity.java'),numbered=(prefix,count)=>{for(let i=0;i<count;i++)assert.ok(lang[prefix+i],`no translation for ${prefix}${i}`)};
const statusTexts=Object.keys(lang).filter(key=>key.startsWith('status.technologia.')).length;
numbered('status.technologia.',statusTexts);
for(const f of sources)for(const [,value] of fs.readFileSync(f,'utf8').matchAll(/\bstatus(?:\(| = )(\d+)\b/g))assert.ok(lang['status.technologia.'+value],`${show(f)}: no translation for status ${value}`);
numbered('ui.technologia.redstone.',Number(machineSource.match(/REDSTONE_MODES = (\d+)/)[1]));
numbered('ui.technologia.mesh.',Number(java('recipe/MachineRecipe.java').match(/MAX_MESH = (\d+)/)[1])+1);
// The machine screen shows a hint for its machine. The Nexus core and terminal open the storage screen instead.
for(const machine of machines)if(!machine.startsWith('storage_'))assert.ok(lang['hint.technologia.'+machine],`${machine}: no translation hint.technologia.${machine}`);
// The wireless screens have room for two lines of about 45 characters under the status line.
for(const machine of ['wireless_sender','wireless_receiver'])assert.ok(lang['hint.technologia.'+machine].length<=90,`hint.technologia.${machine} is longer than two lines`);
// Loader metadata is a Gradle template: every ${placeholder} must be a gradle.properties key, or the build stops in processResources.
const properties=new Set(fs.readFileSync(`${root}/gradle.properties`,'utf8').split(/\r?\n/).filter(line=>line.includes('=')&&!line.startsWith('#')).map(line=>line.slice(0,line.indexOf('=')).trim()));
const metadata=['fabric/src/main/resources/fabric.mod.json','forge/src/main/resources/META-INF/mods.toml','neoforge/src/main/resources/META-INF/neoforge.mods.toml'];
for(const f of metadata){const text=fs.readFileSync(`${root}/${f}`,'utf8');
 for(const [,key] of text.matchAll(/\$\{([^}]*)\}/g))assert.ok(properties.has(key),`${f}: placeholder \${${key}} is not a gradle.properties key`);
 assert.equal(text.split('$').length,text.split('${').length,`${f}: a '$' outside a \${placeholder} breaks Gradle's expand()`);
 if(f.endsWith('.toml')){assert.ok(text.includes('modId="technologia"'),`${f}: lacks modId="technologia"`);assert.ok(text.includes('version="${version}"'),`${f}: lacks version="\${version}"`);}
 else assert.equal(JSON.parse(text).version,'${version}',`${f}: version must be "\${version}"`);
}
for(const module of ['fabric','forge','neoforge'])for(const f of walk(`${root}/${module}/src/main/resources`).filter(f=>f.endsWith('.json')))read(f);
console.log(`PASS: ${count} shared JSON files; ${blockstates.length} blocks, ${itemModels.length} item models, ${recipes.length} recipes, ${advancements.length} advancements, ${Object.keys(lang).length} translations, ${pngs.length} PNGs, ${machines.length} distinct machines; references, tags, loot tables, duplicate recipes and loader metadata checked.`);
