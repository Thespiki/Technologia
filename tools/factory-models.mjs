/** Original static geometry. No per-tick renderer; each machine has its own readable silhouette. */
import assert from 'node:assert/strict';
// A face lying exactly on the block boundary is hidden by a full neighbour, so it is culled against that side.
export const cull=(face,from,to)=>({down:from[1]===0,up:to[1]===16,north:from[2]===0,south:to[2]===16,west:from[0]===0,east:to[0]===16})[face]?{cullface:face}:{};
// The tier band overlay is drawn in this box, between the two front feet. Machine models leave it empty.
export const tierBand={from:[4,0,0],to:[12,2,1]};
const entersBand=e=>[0,1,2].every(i=>e.from[i]<tierBand.to[i]&&e.to[i]>tierBand.from[i]);
export function buildFactoryModels({json,png,palette,machines,decor}) {
 const tex=(name,paint)=>png('block/'+name,paint);
 tex('factory_metal',r=>{r(0,0,32,32,[53,69,82]);for(let y=0;y<32;y+=4)r(0,y,32,1,[64,81,94]);for(const x of [2,28])for(const y of [2,28]){r(x,y,2,2,[176,195,203]);r(x,y,1,1,[224,230,222]);}});
 tex('factory_dark',r=>{r(0,0,32,32,[13,23,31]);for(let y=1;y<32;y+=4)r(0,y,32,1,[22,34,44]);});
 tex('factory_copper',r=>{r(0,0,32,32,[149,91,47]);for(let y=0;y<32;y+=4){r(0,y,32,2,[212,146,76]);r(0,y+2,32,1,[93,64,40]);}});
 tex('factory_bolt',r=>{r(0,0,32,32,[103,123,137]);r(3,3,26,26,[161,180,186]);r(8,14,16,4,[45,64,78]);});
 tex('factory_glow',r=>{r(0,0,32,32,[25,79,81]);r(2,2,28,28,palette.cyan);r(6,6,20,20,[185,245,225]);});
 tex('factory_heat',r=>{r(0,0,32,32,[99,47,27]);r(2,2,28,28,palette.amber);r(6,6,20,20,[253,224,149]);});
 tex('factory_vent',r=>{r(0,0,32,32,[13,23,31]);for(let y=2;y<31;y+=5){r(2,y,28,2,[99,123,138]);r(4,y+2,24,1,[45,62,72]);}});
 tex('factory_hazard',r=>{r(0,0,32,32,[20,29,36]);for(let y=0;y<32;y++)for(let x=0;x<32;x++)if((x+y)%16<7)r(x,y,1,1,palette.amber);});
 tex('factory_glass',r=>{for(const x of [0,30])r(x,0,2,32,palette.rim);for(const y of [0,30])r(0,y,32,2,palette.rim);for(let t=0;t<8;t++){r(6+t,4+t,1,1,[143,206,218]);r(18+t,20+t,1,1,[99,149,167]);}});
 // Alpha.5 fittings. Grate, mesh and leaves leave pixels unpainted: blocks that use them render as cutout.
 const speckle=(r,a,b,mix)=>{for(let y=0;y<32;y++)for(let x=0;x<32;x++)r(x,y,1,1,mix(x,y)?b:a);};
 tex('factory_arc',r=>{r(0,0,32,32,[52,40,92]);r(2,2,28,28,palette.violet);r(6,6,20,20,[226,212,255]);});
 tex('factory_wood',r=>{r(0,0,32,32,[150,110,68]);for(let y=0;y<32;y+=8){r(0,y,32,1,[98,70,42]);r(0,y+1,32,1,[176,134,86]);}for(const [x,y] of [[9,3],[22,11],[5,19],[17,27]])r(x,y,1,4,[112,80,48]);});
 tex('factory_canvas',r=>{r(0,0,32,32,[222,214,196]);for(let y=3;y<32;y+=6)r(0,y,32,1,[186,176,156]);for(let x=5;x<32;x+=10)r(x,0,1,32,[200,190,170]);});
 tex('factory_frost',r=>{r(0,0,32,32,[176,206,226]);for(let y=0;y<32;y+=4){r(0,y,32,1,[226,240,250]);r(0,y+3,32,1,[122,156,184]);}});
 tex('factory_pane',r=>{r(0,0,32,32,[30,62,72]);for(const x of [0,30])r(x,0,2,32,palette.rim);for(const y of [0,30])r(0,y,32,2,palette.rim);for(let t=0;t<8;t++){r(6+t,4+t,1,1,[143,206,218]);r(18+t,20+t,1,1,[99,149,167]);}});
 tex('factory_leaf',r=>speckle(r,[58,132,70],[86,160,84],(x,y)=>(x*7+y*13)%11<3));
 tex('factory_soil',r=>speckle(r,[72,50,34],[98,70,48],(x,y)=>(x*5+y*11)%13<3));
 tex('factory_gravel',r=>{for(let y=0;y<32;y++)for(let x=0;x<32;x++){const q=(x*17+y*31+x*y*3)%23;r(x,y,1,1,[112+q,106+q,104+q]);}});
 tex('factory_terracotta',r=>{r(0,0,32,32,[156,90,60]);for(let y=0;y<32;y+=6)r(0,y,32,1,[128,70,46]);r(0,1,32,1,[186,116,82]);});
 tex('factory_tile',r=>{r(0,0,32,32,[45,58,70]);for(const x of [0,16])for(const y of [0,16]){r(x+1,y+1,14,14,[78,96,110]);r(x+1,y+1,14,1,[110,130,144]);r(x+3,y+3,1,1,[176,195,203]);r(x+12,y+12,1,1,[30,40,50]);}});
 tex('factory_grate',r=>{for(let t=0;t<32;t+=4){r(t,0,2,32,[99,123,138]);r(0,t,32,2,[122,146,160]);}});
 tex('factory_mesh',r=>{for(let t=1;t<32;t+=4){r(t,0,1,32,[214,206,188]);r(0,t,32,1,[214,206,188]);}});
 tex('factory_leaves',r=>{for(let y=0;y<32;y++)for(let x=0;x<32;x++)if((x*7+y*13+x*y)%9<7)r(x,y,1,1,(x+y)%3?[58,132,70]:[86,160,84]);});
 const faceNames=['down','up','north','south','west','east'];
 const box=(a,b,t='metal',front=null)=>({from:a,to:b,faces:Object.fromEntries(faceNames.map(f=>[f,{texture:'#'+(f==='north'&&front?front:t),...cull(f,a,b)}]))});
 const textures={metal:'technologia:block/factory_metal',dark:'technologia:block/factory_dark',copper:'technologia:block/factory_copper',bolt:'technologia:block/factory_bolt',vent:'technologia:block/factory_vent',hazard:'technologia:block/factory_hazard',panel:'technologia:block/solar_top'};
 // Extra texture keys are only written into the models that use them.
 const pick=(extras,e)=>{const used=new Set(e.flatMap(el=>Object.values(el.faces).map(face=>face.texture.slice(1))));return Object.fromEntries(Object.entries(extras).filter(([key])=>used.has(key)).map(([key,file])=>[key,'technologia:block/'+file]));};
 for(const name of machines)for(const active of [false,true]){
  let e=[];const add=(a,b,t,f)=>e.push(box(a,b,t,f));const beam=(x,z)=>add([x,2,z],[x+2,14,z+2]);
  const foot=()=>{add([0,0,1],[16,2,15]);add([1,0,0],[4,3,3],'dark');add([12,0,0],[15,3,3],'dark');};
  const body=()=>{foot();add([1,2,3],[15,14,15],'dark');add([1,14,2],[15,16,15]);beam(0,1);beam(14,1);add([3,3,2],[13,13,3],'dark','front');};
  // A front plate of any size that shows the whole instrument drawing (texture pixels 5..27 by 6..24).
  const panel=(a,b)=>{add(a,b,'dark');e.at(-1).faces.north={texture:'#front',uv:[2.5,3,13.5,12],...cull('north',a,b)};};
  if(name==='metal_press'||name==='compactor'){
   foot();for(const x of [1,13])for(const z of [2,12])beam(x,z);add([0,13,1],[16,16,15]);add([6,8,5],[10,13,11],'copper');add([3,7,3],[13,9,13]);add([2,2,1],[14,4,15],'hazard');add([4,4,4],[12,5,12],'dark');add([12,10,1],[14,12,2],'signal');
   if(name==='compactor'){add([2,4,12],[14,13,14],'dark');add([2,5,2],[3,12,12],'metal');}
  } else if(name==='sawmill'){
   foot();add([0,2,2],[16,7,15],'dark');add([0,7,0],[16,9,16]);for(const x of [1,13])add([x,9,0],[x+2,10,16],'copper');add([7,8,4],[9,14,12],'bolt');add([6,10,5],[10,12,11],'metal');add([6,12,7],[10,14,9],'metal');add([3,10,12],[13,13,15],'dark');add([3,10,11],[5,12,12],'signal');
  } else if(name==='auto_sieve'){
   // An open frame: four posts and a top ring hold a mesh tray above the collection bin.
   foot();for(const x of [1,13])for(const z of [2,12])beam(x,z);for(const z of [2,12])add([1,14,z],[15,15,z+2]);for(const x of [1,13])add([x,14,4],[x+2,15,12]);
   add([3,9,3],[13,10,13],'vent');for(const z of [2,13])add([2,9,z],[14,11,z+1],'copper');for(const x of [2,13])add([x,9,3],[x+1,11,13],'copper');
   add([3,2,3],[13,6,13],'dark');add([4,2,2],[12,6,3],'dark','front');add([1.5,11,1.5],[2.5,13,2],'signal');
  } else if(name.includes('solar')){
   // The panels have no foot slab, so a small plinth backs the tier band.
   add([2,0,2],[14,5,14],'dark');add([4,0,1],[12,2,2],'dark');add([5,5,5],[11,11,11],'metal');add([0,11,0],[16,13,16],'panel');for(const x of [0,15])add([x,10,0],[x+1,14,16]);for(const z of [0,15])add([0,10,z],[16,14,z+1]);add([5,2,1],[11,4,2],'signal');
   if(name.startsWith('advanced')){add([2,5,3],[4,11,13],'copper');add([12,5,3],[14,11,13],'copper');}
  } else if(name==='storage_core'){
   body();e=e.filter(el=>!(el.from[0]===3&&el.from[1]===3));for(const x of [3,8.5])for(const y of [3,7,11]){add([x,y,1],[x+4.5,y+3,3],'dark','detail');add([x+.4,y+1,0.5],[x+3,y+1.5,1],'metal');}add([4,14,0],[12,15,2],'vent');
  } else if(name==='storage_terminal'){
   foot();add([5,2,8],[11,6,13]);add([1,6,5],[15,16,9],'metal');add([2,7,4.5],[14,15,5],'dark','detail');add([2,3,1],[14,5,9],'dark');for(let x=3;x<13;x+=2)add([x,5,2],[x+1,5.3,6],'metal');
  } else if(name.includes('energy_cell')){
   foot();add([0,14,1],[16,16,15]);for(const x of [0,14])for(const z of [1,13])beam(x,z);
   for(const x of [3,9]){add([x,3,4],[x+4,13,12],'dark');for(const y of [4,7,10])add([x,y,3],[x+4,y+1.5,13],name.startsWith('advanced')?'copper':'metal');add([x+1,5,2],[x+3,11,3],'signal');}
  } else if(name==='centrifuge'){
   foot();add([2,2,2],[14,12,14],'dark');for(const [x,z] of [[2,2],[10,2],[2,10],[10,10]]){add([x,4,z],[x+4,12,z+4],'copper');add([x,12,z],[x+4,13,z+4],'metal');}add([5,13,5],[11,15,11],'metal');add([4,3,1],[12,6,2],'dark','front');
  } else if(name==='digital_miner'){
   foot();for(const x of [0,14])for(const z of [1,13])beam(x,z);add([1,13,2],[15,16,14],'metal');add([6,3,6],[10,14,10],'copper');for(let y=3;y<12;y+=3)add([4,y,4],[12,y+1,12],'bolt');add([5,1,5],[11,3,11],'hazard');add([2,6,0],[5,12,3],'dark','front');
  } else if(name==='water_wheel'){
   // A paddle wheel of crossed boards turns in front of the housing.
   foot();add([1,2,5],[15,13,15],'dark');add([1,13,4],[15,15,15]);panel([2.5,3.5,4.5],[13.5,12.5,5]);add([0,3,7],[1,9,13],'copper');
   add([7,7,1],[9,9,4.5],'bolt');add([7.5,3.5,2],[8.5,12.5,3],'wood');add([2.5,7.5,2],[13.5,8.5,3],'wood');
   add([5,12,1],[11,13,4],'wood');add([5,3,1],[11,4,4],'wood');add([2,5,1],[3,11,4],'wood');add([13,5,1],[14,11,4],'wood');
   for(const x of [3,11.5])for(const y of [4,10.5])add([x,y,1.5],[x+1.5,y+1.5,3.5],'wood');
   add([7.5,7.5,.5],[8.5,8.5,1],'signal');
  } else if(name==='windmill'){
   // A short tower; four spars with offset sails make a pinwheel on its front.
   foot();add([4,2,5],[12,8,13],'dark');add([5,8,6],[11,14,12]);add([4,14,4],[12,16,13],'copper');for(const x of [2,12])add([x,2,7],[x+2,5,11]);
   panel([5.25,2.75,4.6],[10.75,7.25,5]);add([7,10.5,.5],[9,12.5,6],'bolt');add([7.5,11,.2],[8.5,12,.5],'signal');
   add([7.5,7,1],[8.5,16,2]);add([3.5,11,1],[12.5,12,2]);
   add([8.5,13,1.2],[10.5,16,1.8],'canvas');add([9.5,9,1.2],[12.5,11,1.8],'canvas');add([5.5,7,1.2],[7.5,10,1.8],'canvas');add([3.5,12,1.2],[6.5,14,1.8],'canvas');
   add([7.5,10,12],[8.5,13,15.5],'canvas');
  } else if(name==='thermoelectric_generator'){
   // Hot copper half on the west, cold finned half on the east, one divider between them.
   foot();add([1,2,3],[8,13,15],'copper');add([8,2,3],[15,13,15],'frost');add([7.5,2,2.5],[8.5,14,15.5],'dark');panel([2.5,3,2],[13.5,12,3]);
   for(const z of [4,7,10,13]){add([2,13,z],[7,15,z+1],'heat');add([9,13,z],[14,16,z+1],'frost');}
   add([0,4,5],[1,11,13],'heat');for(const y of [8,10])add([15,y,5],[16,y+1,13],'frost');
  } else if(name==='creative_energy_source'){
   // A lantern: the core is always lit, caged between two caps.
   foot();add([1,2,2],[15,7,15],'dark');add([1,13,2],[15,16,15]);add([4,7,5],[12,13,12],'core');panel([5.25,2.25,1.6],[10.75,6.75,2]);
   for(const x of [2,6,9,13])for(const z of [3,13])add([x,7,z],[x+1,13,z+1],'bolt');for(const x of [2,13])for(const z of [6.5,9.5])add([x,7,z],[x+1,13,z+1],'bolt');
  } else if(name==='wireless_sender'){
   // A mast with two cross arms points up from the deck.
   foot();add([1,2,2],[15,8,15],'dark');add([0,8,1],[16,9,15]);panel([5.25,2.75,1.6],[10.75,7.25,2]);
   add([6,9,6],[10,11,10],'copper');add([7.25,11,7.25],[8.75,15,8.75]);add([7,15,7],[9,16,9],'signal');
   add([3.5,12,7.5],[12.5,13,8.5],'bolt');add([7.5,13.75,5],[8.5,14.5,11],'bolt');
   for(const x of [2,12])for(const z of [3,11]){add([x,9,z],[x+2,11,z+2],'copper');add([x+.5,11,z+.5],[x+1.5,11.5,z+1.5],'signal');}
  } else if(name==='wireless_receiver'){
   // A dish on a pedestal; a ring around the feed catches the signal.
   foot();add([2,2,3],[14,8,14],'dark');panel([5.25,2.75,2.6],[10.75,7.25,3]);
   add([5,8,5],[11,9,11]);add([3,9,3],[13,10,13],'bolt');for(const z of [3,12])add([3,10,z],[13,11,z+1]);for(const x of [3,12])add([x,10,4],[x+1,11,12]);
   add([7.5,10,7.5],[8.5,14,8.5]);add([7,14,7],[9,15.5,9],'signal');
   for(const z of [5,10])add([5,12.5,z],[11,13.5,z+1],'copper');for(const x of [5,10])add([x,12.5,6],[x+1,13.5,10],'copper');add([6,12.75,7.75],[10,13.25,8.25],'bolt');
  } else if(name==='enrichment_chamber'){
   // An upright pressure drum with two flanges, held in a gantry.
   foot();beam(0,1);beam(14,1);add([0,14,1],[16,15,3]);
   add([3,2,3],[13,13,14],'copper');add([2,2,5],[14,13,12],'copper');add([5,2,2],[11,13,15],'copper');
   for(const y of [4,10])add([1.75,y,1.75],[14.25,y+1,15.25],'bolt');
   panel([5.25,5.25,1.6],[10.75,9.75,2]);add([4,13,4],[12,14,13]);add([7,14,7],[9,15.5,9],'bolt');add([5.5,15.5,7.5],[10.5,16,8.5]);add([7.5,15.5,5.5],[8.5,16,10.5]);
  } else if(name==='metallurgic_infuser'){
   // Two hoppers, one per ingredient, feed a crucible.
   foot();add([1,2,14],[15,16,15],'dark');add([2,2,3],[14,7,14],'dark');add([1,7,2],[15,8,14]);add([3,8,4],[13,8.5,12],'heat');panel([5.25,2.25,2.6],[10.75,6.75,3]);
   for(const [x,t] of [[1,'copper'],[9,'metal']]){add([x,13,3],[x+6,16,14],t);add([x+1,11,4],[x+5,13,13],t);add([x+2,8.5,6],[x+4,11,10],t);}
  } else if(name==='phyto_chamber'){
   // A grow box: lamp above, plant in the middle, open at the front.
   foot();add([1,2,2],[15,7,15],'dark');panel([5.25,2.25,1.6],[10.75,6.75,2]);add([2,7,3],[14,7.5,14],'soil');
   for(const x of [1,13])for(const z of [2,13])add([x,7,z],[x+2,14,z+2]);add([1,14,2],[15,16,15]);add([3,13.5,4],[13,14,13],'heat');
   add([3,7,14],[13,14,14.5],'pane');for(const x of [1.5,14])add([x,7,4],[x+.5,14,13],'pane');
   add([7.5,7.5,7.5],[8.5,10,8.5],'leaf');add([5.5,9.5,5.5],[10.5,13,10.5],'leaf');
  } else if(name==='growth_accelerator'){
   // Four emitter posts stand around a growth pad on a low base.
   foot();add([1,2,2],[15,7,15],'dark');add([0,7,1],[16,8,15]);panel([5.25,2.25,1.6],[10.75,6.75,2]);
   for(const x of [1,12])for(const z of [2,11]){add([x,8,z],[x+3,13,z+3]);add([x+.5,13,z+.5],[x+2.5,15,z+2.5],'signal');}
   add([5,8,5.5],[11,9,11.5],'leaf');add([6.5,9,7],[9.5,10,10],'signal');
  } else if(name==='vacuum_collector'){
   // A wide funnel, open at the top, over a compact body.
   foot();add([2,2,3],[14,9,14],'dark');panel([4,2.25,2.6],[12,8.75,3]);add([0,3,5],[2,8,9],'copper');
   for(const z of [2,14])add([1,9,z],[15,12,z+1]);for(const x of [1,14])add([x,9,3],[x+1,12,14]);
   for(const z of [1,15])add([0,12,z],[16,16,z+1]);for(const x of [0,15])add([x,12,2],[x+1,16,15]);
   add([2,9,3],[14,9.5,14],'vent');add([6,9.5,6.5],[10,10,10.5],'signal');
  } else if(name==='chunk_loader'){
   // An open frame cube; the core hangs in the middle above a walled base. Each post stands on a dark plinth:
   // its side lies in the same plane as the side of a front foot, and two different textures there would flicker.
   foot();for(const x of [1,13])for(const z of [2,12]){add([x,2,z],[x+2,3,z+2],'dark');add([x,3,z],[x+2,16,z+2]);}
   for(const z of [2,12])add([3,14,z],[13,16,z+2]);for(const x of [1,13])add([x,14,4],[x+2,16,12]);
   for(const x of [1,13])add([x,2,4],[x+2,7,12],'dark');for(const z of [2,12])add([3,2,z],[13,7,z+2],'dark');panel([5.25,2.25,1.6],[10.75,6.75,2]);
   add([5,7,5],[11,12,11],'arc');add([7,2,7],[9,7,9],'dark');add([7.5,12,7.5],[8.5,14.5,8.5],'bolt');add([3,14.5,7.5],[13,15.5,8.5],'bolt');
  } else {
   body();
   if(name==='crusher'){add([3,5,0.6],[13,12,2],'dark');for(const x of [4,9])for(let y=6;y<12;y+=2)add([x,y,0.5],[x+3,y+1,1.5],'bolt');add([3,2,0],[13,3,2],'hazard');}
   else if(name==='recycler'){add([3,5,0.6],[13,12,2],'dark');add([4,7,0.2],[12,10,1.6],'bolt');add([3,12,0],[13,13,2],'copper');add([3,2,0],[13,3,2],'hazard');}
   else if(name==='alloy_smelter'){for(const x of [3,9]){add([x,5,1],[x+4,11,3],'metal');add([x+.5,6,.6],[x+3.5,10,1],'heat');}add([5,13,0],[11,14,2],'copper');}
   // A cutter bar with teeth hangs between two arms in front of the body.
   else if(name==='auto_harvester'){for(const x of [2,13])add([x,3,0],[x+1,7,2],'copper');add([3,3.5,.25],[13,5,1.75]);for(let x=4.5;x<11;x+=2)add([x,2.25,.5],[x+1,3.5,1.5],'bolt');}
   else {
    // Firebox family: a framed window, then one distinguishing fitting per machine.
    add([4,4,1],[12,11,2],'metal');add([5,5,.7],[11,10,1],name==='biomass_generator'?'signal':'heat');
    if(name==='coal_generator'){add([3,11,0],[13,13,2],'copper');add([5,3,0],[11,4,2],'dark');}
    else {add([4,11,1],[12,12,2],'bolt');if(name==='electric_furnace')for(const x of [3,12])add([x,4,1],[x+1,12,2],'copper');if(name==='biomass_generator')add([4,2,0],[12,4,1.5],'copper');}
   }
   add([4,14,3],[12,15,13],'vent');add([14,5,5],[16,12,11],'copper');add([1,3,15],[15,13,16],'vent');
  }
  // A visible socket and status lamp establish a shared family across distinct bodies.
  add([14,3,7],[16,6,10],'dark');add([15.5,4,8],[16,5,9],'signal');
  assert.ok(!e.some(entersBand),`${name}: a model element enters the tier band space`);
  // 'arc' is the violet glow of a working machine; 'core' is lit all the time.
  const fittings=pick({wood:'factory_wood',canvas:'factory_canvas',frost:'factory_frost',pane:'factory_pane',leaf:'factory_leaf',soil:'factory_soil',arc:active?'factory_arc':'factory_dark',core:'factory_arc'},e);
  json(`assets/technologia/models/block/${name}${active?'_active':''}`,{parent:'minecraft:block/block',ambientocclusion:true,textures:{...textures,particle:'technologia:block/factory_metal',front:`technologia:block/${name}${active?'_active':''}`,detail:`technologia:block/${name==='storage_core'?'drive_bay':'terminal_screen'}${active?'_active':''}`,signal:`technologia:block/${active?'factory_glow':'factory_dark'}`,heat:`technologia:block/${active?'factory_heat':'factory_dark'}`,...fittings},elements:e});
 }
 for(const n of decor){let e=[];const add=(a,b,t)=>e.push(box(a,b,t));let render_type;
  if(n==='steel_pillar'){add([0,0,0],[16,2,16],'metal');add([0,14,0],[16,16,16],'metal');add([4,2,4],[12,14,12],'metal');for(const x of [2,12])for(const z of [2,12])add([x,2,z],[x+2,14,z+2],'copper');}
  else if(n==='steel_grate'){for(let x=0;x<16;x+=4)add([x,0,0],[x+1.5,16,16],'metal');for(let y=0;y<16;y+=4)add([0,y,0],[16,y+1,16],'dark');}
  else if(n==='ventilation_grille'){add([0,0,2],[16,16,16],'dark');for(let y=1;y<16;y+=3)add([1,y,0],[15,y+1.5,3],'metal');}
  else if(n==='engineering_lamp'){add([0,0,0],[16,16,16],'signal');for(const x of [0,14])add([x,0,-.05],[x+2,16,16.05],'metal');for(const y of [0,14])add([0,y,-.05],[16,y+2,16.05],'metal');}
  else if(n==='control_panel'){add([0,0,1],[16,16,16],'dark');add([2,5,0],[14,14,1],'screen');for(let x=3;x<14;x+=3)add([x,2,0],[x+2,4,1],'signal');}
  else if(n==='reinforced_glass'){add([0,0,0],[16,16,16],'glass');render_type='minecraft:cutout';}
  // The dark core is the mortar: it closes the gaps between the raised bricks.
  else if(n==='industrial_bricks'){add([0,0,.5],[16,16,15.5],'dark');for(let y=0;y<16;y+=4)for(let x=0;x<16;x+=8)add([x+.2,y+.2,0],[x+7.8,y+3.8,16],'metal');}
  else if(n==='floor_panel'){add([0,0,0],[16,16,16],'tile');}
  // A grated deck in a frame, with a low rail on its west and east edges.
  else if(n==='catwalk'){for(const z of [0,15])add([0,0,z],[16,2,z+1],'metal');for(const x of [0,15])add([x,0,1],[x+1,2,15],'metal');add([1,1,1],[15,2,15],'grate');
   for(const x of [0,15]){add([x,4,0],[x+1,5,16],'metal');for(const z of [0,7.5,15])add([x,2,z],[x+1,4,z+1],'metal');}render_type='minecraft:cutout';}
  else if(n==='steel_table'){add([0,13,0],[16,16,16],'metal');for(const x of [1,13])for(const z of [1,13])add([x,0,z],[x+2,13,z+2],'dark');for(const x of [1.5,13.5])add([x,3,3],[x+1,4,13],'bolt');}
  else if(n==='steel_stool'){add([3,8,3],[13,10,13],'metal');add([4,10,4],[12,11,12],'copper');for(const x of [4,10.5])for(const z of [4,10.5])add([x,0,z],[x+1.5,8,z+1.5],'dark');add([4.5,3,4.5],[11.5,4,11.5],'bolt');}
  // Open at the front: two compartments between side panels, with a few things left on the shelves.
  else if(n==='metal_shelf'){for(const x of [0,15])add([x,0,2],[x+1,16,16],'metal');add([1,0,15],[15,16,16],'dark');for(const y of [0,7.5,15])add([1,y,2],[15,y+1,15],'metal');add([3,1,6],[7,5,11],'copper');add([9,8.5,7],[13,11.5,12],'bolt');add([3,8.5,5],[6,10,13],'hazard');}
  else if(n==='tool_cabinet'){add([0,0,1],[16,16,16],'metal');for(const y of [1,6,11]){add([1,y,.5],[15,y+4,1],'dark');add([6,y+1.5,0],[10,y+2.5,.5],'bolt');}}
  else if(n==='warning_light'){add([4,0,4],[12,2,12],'dark');add([5,2,5],[11,3,11],'metal');add([6,3,6],[10,8,10],'heat');add([5.5,8,5.5],[10.5,9,10.5],'metal');for(const x of [5.5,10])for(const z of [5.5,10])add([x,3,z],[x+.5,8,z+.5],'bolt');}
  else {add([0,0,0],[16,16,16],n==='bronze_casing'?'copper':n==='hazard_block'?'hazard':'metal');}
  json(`assets/technologia/models/block/${n}`,{parent:'minecraft:block/block',...(render_type?{render_type}:{}),textures:{...textures,particle:'technologia:block/factory_metal',signal:'technologia:block/factory_glow',screen:'technologia:block/terminal_screen_active',glass:'technologia:block/factory_glass',...pick({tile:'factory_tile',grate:'factory_grate',heat:'factory_heat'},e)},elements:e});
 }
 // Blocks with one model per state. The first model is the item model and the collision shape; the cutout layer lets mesh and leaves show through.
 const device=(n,particle,keys,e)=>json(`assets/technologia/models/block/${n}`,{parent:'minecraft:block/block',render_type:'minecraft:cutout',textures:{particle:'technologia:block/'+particle,...pick(keys,e)},elements:e});
 // Hand sieve: four legs, a frame, a string mesh, and a layer that sinks with every shake.
 for(let fill=0;fill<5;fill++){const e=[];const add=(a,b,t)=>e.push(box(a,b,t));
  for(const x of [1,13])for(const z of [1,13])add([x,0,z],[x+2,10,z+2],'wood');
  for(const z of [1,14])add([1,10,z],[15,14,z+1],'wood');for(const x of [1,14])add([x,10,2],[x+1,14,14],'wood');
  add([2,10.5,2],[14,11,14],'mesh');if(fill)add([2,11,2],[14,10.75+fill*.75,14],'gravel');
  device('hand_sieve'+(fill?'_'+fill:''),'factory_wood',{wood:'factory_wood',mesh:'factory_mesh',gravel:'factory_gravel'},e);
 }
 // Bonsai pot: empty, a sprout, a small tree, a full small tree.
 for(let stage=0;stage<4;stage++){const e=[];const add=(a,b,t)=>e.push(box(a,b,t));
  add([3,0,3],[13,4,13],'pot');for(const z of [2.5,12.5])add([2.5,4,z],[13.5,6,z+1],'pot');for(const x of [2.5,12.5])add([x,4,3.5],[x+1,6,12.5],'pot');add([3.5,4,3.5],[12.5,5,12.5],'soil');
  if(stage===1){add([7.5,5,7.5],[8.5,8,8.5],'leaf');add([6,7,7.5],[10,8,8.5],'leaf');}
  if(stage===2){add([7,5,7],[9,10,9],'wood');add([5,9,5],[11,13,11],'leaves');}
  if(stage===3){add([7,5,7],[9,11,9],'wood');add([4,10,4],[12,14,12],'leaves');add([5.5,14,5.5],[10.5,16,10.5],'leaves');for(const x of [3,12])add([x,11,6],[x+1,13,10],'leaves');}
  device('bonsai_pot'+(stage?'_'+stage:''),'factory_terracotta',{pot:'factory_terracotta',soil:'factory_soil',leaf:'factory_leaf',wood:'factory_wood',leaves:'factory_leaves'},e);
 }
}
