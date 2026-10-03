// Structural checks on the generated resources. Fails with a message naming the file and the broken reference.
import fs from 'node:fs';
import path from 'node:path';
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
// An ingredient is {item}, {tag} or a list of those; returns 'ns:path' for items and '#ns:path' for tags.
const ingredientIds=value=>Array.isArray(value)?value.flatMap(ingredientIds):value?.item?[value.item]:value?.tag?['#'+value.tag]:[];
const recipes=[],shaped=new Map(),shapeless=new Map(),advancements=[];
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
 }
 if(f.includes('/models/'))for(const texture of Object.values(json.textures??{}))if(texture.startsWith('technologia:'))assert.ok(fs.existsSync(`${assets}/textures/${texture.split(':')[1]}.png`),`${show(f)}: missing texture ${texture}`);
 if(f.includes('/blockstates/'))for(const variant of [...Object.values(json.variants??{}),...(json.multipart??[]).map(part=>part.apply)].flat())assert.ok(fs.existsSync(`${assets}/models/${variant.model.split(':')[1]}.json`),`${show(f)}: missing model ${variant.model}`);
 if(f.includes('/tags/'))for(const value of json.values??[])if(typeof value==='string'&&value.startsWith('#c:'))assert.ok(fs.existsSync(`${base}/data/c/tags/${f.includes('/block/')?'block':'item'}/${value.slice(3)}.json`),`${show(f)}: missing tag ${value}`);
 if(f.includes('/advancement/')){advancements.push(f);
  if(json.parent){const [ns,parent]=json.parent.split(':');assert.ok(fs.existsSync(`${base}/data/${ns}/advancement/${parent}.json`),`${show(f)}: parent advancement ${json.parent} does not exist`);}
  const icon=json.display.icon.id;if(icon.startsWith('technologia:'))assert.ok(hasItemModel(icon.slice(12)),`${show(f)}: icon ${icon} has no item model`);
  for(const text of [json.display.title,json.display.description])if(text.translate)assert.ok(lang[text.translate],`${show(f)}: no translation for ${text.translate}`);
 }
}
// Every block needs a name and a loot table, or it shows a raw key and drops nothing.
const blockstates=files.filter(f=>f.includes('/blockstates/'));
for(const f of blockstates){
 assert.ok(lang['block.technologia.'+name(f)],`${show(f)}: no translation block.technologia.${name(f)}`);
 assert.ok(fs.existsSync(`${data}/loot_table/blocks/${name(f)}.json`),`${show(f)}: block has no loot table`);
}
const itemModels=files.filter(f=>f.includes('/models/item/'));
for(const f of itemModels)assert.ok(lang['item.technologia.'+name(f)]||lang['block.technologia.'+name(f)],`${show(f)}: no item.technologia.${name(f)} or block.technologia.${name(f)} translation`);
// Machines are the blocks with an 'active' state. With the block's own name masked, no two of their models may match.
const machines=blockstates.filter(f=>Object.keys(read(f).variants??{}).some(key=>key.includes('active='))).map(name);
const distinct=(what,entries)=>{const seen=new Map();for(const [label,content] of entries){assert.ok(!seen.has(content),`${label} is identical to ${seen.get(content)} (${what})`);seen.set(content,label);}};
distinct('machine block models, block name masked',machines.flatMap(machine=>['','_active'].map(suffix=>[`models/block/${machine}${suffix}.json`,fs.readFileSync(`${assets}/models/block/${machine}${suffix}.json`,'utf8').replaceAll(machine,'*')])));
distinct('machine front textures',machines.flatMap(machine=>['','_active'].map(suffix=>[`textures/block/${machine}${suffix}.png`,fs.readFileSync(`${assets}/textures/block/${machine}${suffix}.png`).toString('base64')])));
const pngs=files.filter(f=>f.endsWith('.png'));
distinct('item icons',pngs.filter(f=>f.includes('/textures/item/')).map(f=>[show(f),fs.readFileSync(f).toString('base64')]));
for(const n of ['draconic_core','chaotic_core'])assert.ok(!fs.existsSync(`${data}/recipe/${n}.json`),`${n} is a concept item and must not be craftable`);
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
