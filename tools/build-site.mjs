import fs from 'node:fs';
import path from 'node:path';
const output='site/dist';
fs.mkdirSync(output,{recursive:true});
for(const name of ['index.html','style.css','app.js','favicon.svg'])fs.copyFileSync(path.join('site',name),path.join(output,name));
fs.cpSync('common/src/main/resources/assets/technologia/textures/block',`${output}/assets/block`,{recursive:true});
fs.writeFileSync(`${output}/.nojekyll`,'');
console.log('Built static Pages presentation in site/dist');
