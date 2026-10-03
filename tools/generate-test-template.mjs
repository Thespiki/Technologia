// Rebuilds the empty 8x8x8 GameTest template used by the NeoForge and Forge test source sets.
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import {fileURLToPath} from 'node:url';
const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const short=n=>{const b=Buffer.alloc(2);b.writeUInt16BE(n);return b};
const int=n=>{const b=Buffer.alloc(4);b.writeInt32BE(n);return b};
const string=s=>Buffer.concat([short(Buffer.byteLength(s)),Buffer.from(s)]);
const tag=(type,name,data)=>Buffer.concat([Buffer.from([type]),string(name),data]);
const list=(type,values)=>Buffer.concat([Buffer.from([type]),int(values.length),...values]);
const nbt=Buffer.concat([Buffer.from([10,0,0]),tag(3,'DataVersion',int(3955)),tag(9,'size',list(3,[int(8),int(8),int(8)])),tag(9,'palette',list(10,[Buffer.concat([tag(8,'Name',string('minecraft:air')),Buffer.from([0])])])),tag(9,'blocks',list(10,[])),tag(9,'entities',list(10,[])),Buffer.from([0])]);
// Tests are dev-only: the template lives beside them, never in src/main, so it cannot reach a release jar.
for(const loader of ['neoforge','forge']){
 const directory=path.join(root,loader,'src/gametest/resources/data/technologia/structure');
 fs.mkdirSync(directory,{recursive:true});fs.writeFileSync(path.join(directory,'empty.nbt'),zlib.gzipSync(nbt));
}
