/** Original static geometry. No per-tick renderer; each machine has its own readable silhouette. */
export function buildFactoryModels({json,png,palette,machines}) {
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
 const faceNames=['down','up','north','south','west','east'];
 const box=(a,b,t='metal',front=null,rotation=null)=>({from:a,to:b,...(rotation?{rotation}:{}),faces:Object.fromEntries(faceNames.map(f=>[f,{texture:'#'+(f==='north'&&front?front:t)}]))});
 const textures={metal:'technologia:block/factory_metal',dark:'technologia:block/factory_dark',copper:'technologia:block/factory_copper',bolt:'technologia:block/factory_bolt',vent:'technologia:block/factory_vent',hazard:'technologia:block/factory_hazard',panel:'technologia:block/solar_top'};
 for(const name of machines)for(const active of [false,true]){
  let e=[];const add=(a,b,t,f,rot)=>e.push(box(a,b,t,f,rot));const beam=(x,z)=>add([x,2,z],[x+2,14,z+2]);
  const foot=()=>{add([0,0,1],[16,2,15]);add([1,0,0],[4,3,3],'dark');add([12,0,0],[15,3,3],'dark');};
  const body=()=>{foot();add([1,2,3],[15,14,15],'dark');add([1,14,2],[15,16,15]);beam(0,1);beam(14,1);add([3,3,2],[13,13,3],'dark','front');};
  if(name==='metal_press'||name==='compactor'){
   foot();for(const x of [1,13])for(const z of [2,12])beam(x,z);add([0,13,1],[16,16,15]);add([6,8,5],[10,13,11],'copper');add([3,7,3],[13,9,13]);add([2,2,1],[14,4,15],'hazard');add([4,4,4],[12,5,12],'dark');add([12,10,1],[14,12,2],'signal');
   if(name==='compactor'){add([2,4,12],[14,13,14],'dark');add([2,5,2],[3,12,12],'metal');}
  } else if(name==='sawmill'){
   foot();add([0,2,2],[16,7,15],'dark');add([0,7,0],[16,9,16]);for(const x of [1,13])add([x,9,0],[x+2,10,16],'copper');add([7,8,4],[9,14,12],'bolt');add([6,10,5],[10,12,11],'metal');add([6,12,7],[10,14,9],'metal');add([3,10,12],[13,13,15],'dark');add([3,10,11],[5,12,12],'signal');
  } else if(name.includes('solar')){
   add([2,0,2],[14,5,14],'dark');add([5,5,5],[11,11,11],'metal');add([0,11,0],[16,13,16],'panel');for(const x of [0,15])add([x,10,0],[x+1,14,16]);for(const z of [0,15])add([0,10,z],[16,14,z+1]);add([5,2,1],[11,4,2],'signal');
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
  } else {
   body();if(name==='crusher'||name==='recycler'){add([3,5,0.6],[13,12,2],'dark');for(const x of [4,9])for(let y=6;y<12;y+=2)add([x,y,0.5],[x+3,y+1,1.5],'bolt');add([3,2,0],[13,3,2],'hazard');}
   else if(name==='alloy_smelter'){for(const x of [3,9]){add([x,5,1],[x+4,11,3],'metal');add([x+.5,6,.6],[x+3.5,10,1],'heat');}add([5,13,0],[11,14,2],'copper');}
   else {add([4,4,1],[12,11,2],'metal');add([5,5,.7],[11,10,1],name==='biomass_generator'?'signal':'heat');add([4,11,1],[12,12,2],'bolt');}
   add([4,14,3],[12,15,13],'vent');add([14,5,5],[16,12,11],'copper');add([1,3,15],[15,13,16],'vent');
  }
  // A visible socket and status lamp establish a shared family across distinct bodies.
  add([14,3,7],[16,6,10],'dark');add([15.5,4,8],[16,5,9],'signal');
  json(`assets/technologia/models/block/${name}${active?'_active':''}`,{parent:'minecraft:block/block',ambientocclusion:true,textures:{...textures,particle:'technologia:block/factory_metal',front:`technologia:block/${name}${active?'_active':''}`,detail:`technologia:block/${name==='storage_core'?'drive_bay':'terminal_screen'}${active?'_active':''}`,signal:`technologia:block/${active?'factory_glow':'factory_dark'}`,heat:`technologia:block/${active?'factory_heat':'factory_dark'}`},elements:e});
 }
 const decor=['steel_casing','bronze_casing','industrial_bricks','steel_grate','hazard_block','engineering_lamp','steel_pillar','ventilation_grille','control_panel','reinforced_glass'];
 for(const n of decor){let e=[];const add=(a,b,t)=>e.push(box(a,b,t));let render_type;
  if(n==='steel_pillar'){add([0,0,0],[16,2,16],'metal');add([0,14,0],[16,16,16],'metal');add([4,2,4],[12,14,12],'metal');for(const x of [2,12])for(const z of [2,12])add([x,2,z],[x+2,14,z+2],'copper');}
  else if(n==='steel_grate'){for(let x=0;x<16;x+=4)add([x,0,0],[x+1.5,16,16],'metal');for(let y=0;y<16;y+=4)add([0,y,0],[16,y+1,16],'dark');}
  else if(n==='ventilation_grille'){add([0,0,2],[16,16,16],'dark');for(let y=1;y<16;y+=3)add([1,y,0],[15,y+1.5,3],'metal');}
  else if(n==='engineering_lamp'){add([0,0,0],[16,16,16],'signal');for(const x of [0,14])add([x,0,-.05],[x+2,16,16.05],'metal');for(const y of [0,14])add([0,y,-.05],[16,y+2,16.05],'metal');}
  else if(n==='control_panel'){add([0,0,1],[16,16,16],'dark');add([2,5,0],[14,14,1],'screen');for(let x=3;x<14;x+=3)add([x,2,0],[x+2,4,1],'signal');}
  else if(n==='reinforced_glass'){add([0,0,0],[16,16,16],'glass');render_type='minecraft:cutout';}
  else if(n==='industrial_bricks'){for(let y=0;y<16;y+=4)for(let x=0;x<16;x+=8)add([x+.2,y+.2,0],[x+7.8,y+3.8,16],'metal');}
  else {add([0,0,0],[16,16,16],n==='bronze_casing'?'copper':n==='hazard_block'?'hazard':'metal');}
  json(`assets/technologia/models/block/${n}`,{parent:'minecraft:block/block',...(render_type?{render_type}:{}),textures:{...textures,particle:'technologia:block/factory_metal',signal:'technologia:block/factory_glow',screen:'technologia:block/terminal_screen_active',glass:'technologia:block/factory_glass'},elements:e});
 }
}
