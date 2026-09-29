import fs from 'node:fs';
import path from 'node:path';
import assert from 'node:assert/strict';
const base='common/src/main/resources';
function walk(p){return fs.readdirSync(p,{withFileTypes:true}).flatMap(e=>e.isDirectory()?walk(path.join(p,e.name)):[path.join(p,e.name)])}
const files=walk(base); let count=0;
for(const f of files.filter(f=>f.endsWith('.json'))){const data=JSON.parse(fs.readFileSync(f,'utf8')); count++;
 if(f.includes(`${path.sep}recipe${path.sep}`)){assert.ok(data.type);assert.ok(data.result.id);assert.ok(data.result.count>0)}
 if(f.includes(`${path.sep}models${path.sep}`))for(const texture of Object.values(data.textures??{}))if(texture.startsWith('technologia:'))assert.ok(fs.existsSync(`${base}/assets/technologia/textures/${texture.split(':')[1]}.png`),`${f}: ${texture}`);
 if(f.includes(`${path.sep}blockstates${path.sep}`))for(const variant of Object.values(data.variants))assert.ok(fs.existsSync(`${base}/assets/technologia/models/${variant.model.split(':')[1]}.json`),f);
 if(f.includes(`${path.sep}tags${path.sep}`))for(const value of data.values??[])if(typeof value==='string'&&value.startsWith('#c:'))assert.ok(fs.existsSync(`${base}/data/c/tags/${f.includes(`${path.sep}block${path.sep}`)?'block':'item'}/${value.slice(3)}.json`),`${f}: ${value}`);
}
const lang=JSON.parse(fs.readFileSync(`${base}/assets/technologia/lang/en_us.json`,'utf8'));
for(const f of files.filter(f=>f.includes(`${path.sep}blockstates${path.sep}`)))assert.ok(lang['block.technologia.'+path.basename(f,'.json')]);
for(const n of ['draconic_core','chaotic_core'])assert.ok(!fs.existsSync(path.join(base,'data','technologia','recipe',`${n}.json`)));
for(const module of ['fabric','forge','neoforge'])for(const f of walk(`${module}/src/main/resources`).filter(f=>f.endsWith('.json')))JSON.parse(fs.readFileSync(f,'utf8'));
console.log(`PASS: ${count} shared JSON files, translations, texture/model references, tag references and loader JSON.`);
