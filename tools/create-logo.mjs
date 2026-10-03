// Draws the Technologia logo as a 32x32 texture, in the palette and style of the mod's block textures,
// and writes it at 32, 128 and 512 pixels (enlarged without smoothing) into branding/.
// Run from the repository root: node tools/create-logo.mjs
import fs from 'node:fs';
import path from 'node:path';
import zlib from 'node:zlib';
import {fileURLToPath} from 'node:url';

const root=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'..');
const SIZE=32,CENTER=(SIZE-1)/2;
// The palette of the block textures (tools/generate-resources.mjs), with lighter and darker steps for shading.
// Each material: top edge, left edge, two body tones, right edge, bottom edge.
const ceramic=[[23,34,47],[27,39,53],[31,45,61]],outline=[12,18,27],rim=[65,82,103],shade=[17,25,35];
const steel={top:[214,226,240],left:[168,184,204],body:[[124,142,164],[134,152,174]],right:[96,114,136],bottom:[66,82,104]};
const brass={top:[252,226,176],left:[240,196,124],body:[[214,158,88],[224,168,96]],right:[172,120,60],bottom:[128,86,42]};
const cyan={top:[204,255,248],left:[150,240,230],body:[[85,217,208],[85,217,208]],right:[52,170,168],bottom:[36,136,140]};
const glow=[[26,84,90],[17,52,60]],pit=[9,14,21];
// Two versions: a brass gear with steel rivets, and a steel gear with brass rivets.
const versions=[['technologia-logo',brass,steel],['technologia-logo-steel',steel,brass]];

const inside=(x,y)=>x>=0&&y>=0&&x<SIZE&&y<SIZE;
// The same pattern on every run: a small hash of the position instead of a random number.
const speck=(x,y)=>((x*73856093)^(y*19349663)^((x+y)*83492791))>>>0;
// Shapes are decided per pixel, then shaded from their own outline: light from the top left.
const gearAt=(x,y)=>{
 const dx=x-CENTER,dy=y-CENTER,r=Math.hypot(dx,dy);
 if(r<=8)return false;
 if(r<=10.7)return true;
 // Eight teeth of the same length: straight ones four pixels wide, diagonal ones a little narrower so they look the same size.
 for(let k=0;k<8;k++){
  const a=k*Math.PI/4,along=dx*Math.cos(a)+dy*Math.sin(a),across=-dx*Math.sin(a)+dy*Math.cos(a);
  if(along>0&&along<=13.2&&Math.abs(across)<=(k%2?1.5:2))return true;
 }
 return false;
};
const holeAt=(x,y)=>Math.hypot(x-CENTER,y-CENTER)<=8;
const letterAt=(x,y)=>(y>=10&&y<=12&&x>=10&&x<=21)||(y>=13&&y<=21&&x>=14&&x<=17);
const is=(shape,x,y)=>inside(x,y)&&shape(x,y);
const bevel=(shape,x,y,tones)=>!is(shape,x,y-1)?tones.top:!is(shape,x,y+1)?tones.bottom:!is(shape,x-1,y)?tones.left:!is(shape,x+1,y)?tones.right:tones.body[speck(x,y)%3===0?1:0];

function draw(gear,rivet){
 const pixels=Array.from({length:SIZE},()=>Array(SIZE).fill(null));
 // The casing plate: a framed ceramic panel with a lit top-left edge, as on the machine blocks.
 for(let y=0;y<SIZE;y++)for(let x=0;x<SIZE;x++){
  const edge=x===0||y===0||x===SIZE-1||y===SIZE-1;
  pixels[y][x]=edge?outline:x===SIZE-2||y===SIZE-2?shade:x===1||y===1?rim:ceramic[speck(x,y)%7===0?2:speck(x,y)%5===0?0:1];
 }
 for(const [rx,ry] of [[3,3],[27,3],[3,27],[27,27]]){
  pixels[ry][rx]=rivet.top;pixels[ry][rx+1]=rivet.body[0];pixels[ry+1][rx]=rivet.body[0];pixels[ry+1][rx+1]=rivet.bottom;
 }
 for(let y=0;y<SIZE;y++)for(let x=0;x<SIZE;x++){
  if(gearAt(x,y)){pixels[y][x]=bevel(gearAt,x,y,gear);continue;}
  if(holeAt(x,y)){
   if(letterAt(x,y)){pixels[y][x]=bevel(letterAt,x,y,cyan);continue;}
   // The letter lights the pit around it.
   let near=3;
   for(let oy=-2;oy<=2;oy++)for(let ox=-2;ox<=2;ox++)if(is(letterAt,x+ox,y+oy))near=Math.min(near,Math.max(Math.abs(ox),Math.abs(oy)));
   pixels[y][x]=near===1?glow[0]:near===2?glow[1]:pit;
   continue;
  }
  // A dark line around the gear separates it from the plate.
  if(x>1&&y>1&&x<SIZE-2&&y<SIZE-2&&[[1,0],[-1,0],[0,1],[0,-1]].some(([ox,oy])=>is(gearAt,x+ox,y+oy)))pixels[y][x]=outline;
 }
 return pixels;
}

// PNG writer: 8-bit RGB, no filter.
const crcTable=Array.from({length:256},(_,n)=>{let c=n;for(let k=0;k<8;k++)c=c&1?0xedb88320^(c>>>1):c>>>1;return c>>>0});
const crc=buffer=>{let c=0xffffffff;for(const byte of buffer)c=crcTable[(c^byte)&255]^(c>>>8);return (c^0xffffffff)>>>0};
const chunk=(type,body)=>{const head=Buffer.alloc(4);head.writeUInt32BE(body.length);const typed=Buffer.concat([Buffer.from(type),body]),tail=Buffer.alloc(4);tail.writeUInt32BE(crc(typed));return Buffer.concat([head,typed,tail])};
function write(file,pixels,scale){
 const side=SIZE*scale,raw=Buffer.alloc(side*(side*3+1));
 for(let y=0;y<side;y++){const row=y*(side*3+1);raw[row]=0;for(let x=0;x<side;x++){const [r,g,b]=pixels[Math.floor(y/scale)][Math.floor(x/scale)];raw.set([r,g,b],row+1+x*3);}}
 const header=Buffer.alloc(13);header.writeUInt32BE(side,0);header.writeUInt32BE(side,4);header.set([8,2,0,0,0],8);
 fs.writeFileSync(file,Buffer.concat([Buffer.from([137,80,78,71,13,10,26,10]),chunk('IHDR',header),chunk('IDAT',zlib.deflateSync(raw,{level:9})),chunk('IEND',Buffer.alloc(0))]));
}
const out=path.join(root,'branding');
fs.mkdirSync(out,{recursive:true});
for(const [name,gear,rivet] of versions){
 const pixels=draw(gear,rivet);
 for(const [size,scale] of [[32,1],[128,4],[512,16]])write(path.join(out,`${name}-${size}.png`),pixels,scale);
}
console.log('Wrote two versions of the logo at 32, 128 and 512 pixels to branding/');
