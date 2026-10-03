package dev.technologia.test;

import dev.technologia.Technologia;
import dev.technologia.machine.*;
import dev.technologia.logistics.*;
import dev.technologia.storage.StorageMenu;
import dev.technologia.recipe.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.*;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.gametest.*;

@GameTestHolder(Technologia.MOD_ID)
@PrefixGameTestTemplate(false)
public final class FactoryGameTests {
 private static final int OUT=MachineBlockEntity.FIRST_OUTPUT,END=MachineBlockEntity.SLOTS;
 private static MachineBlockEntity place(GameTestHelper h,String id,int x,int z){var pos=new BlockPos(x,2,z);h.setBlock(pos,Technologia.BLOCKS.get(id));return (MachineBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(pos));}
 private static ItemStack item(String id,int count){return new ItemStack(Technologia.ITEMS.get(id),count);}
 private static void ticks(GameTestHelper h,MachineBlockEntity be,int count){for(int i=0;i<count;i++)MachineBlockEntity.tick(h.getLevel(),be.getBlockPos(),be.getBlockState(),be);}
 private static Player near(GameTestHelper h,BlockPos pos){var p=h.makeMockPlayer(GameType.SURVIVAL);p.setPos(pos.getX()+.5,pos.getY(),pos.getZ()+.5);return p;}
 @GameTest(template="empty") public static void countedAlloysAndSwappedInputs(GameTestHelper h){
  var m=place(h,"alloy_smelter",2,2);m.setItem(0,item("tin_ingot",1));m.setItem(1,new ItemStack(Items.COPPER_INGOT,3));m.receiveEnergy(20000,false);ticks(h,m,160);
  h.assertTrue(m.getItem(0).isEmpty()&&m.getItem(1).isEmpty()&&m.getItem(OUT).is(Technologia.ITEMS.get("bronze_ingot"))&&m.getItem(OUT).getCount()==4,"Swapped alloy slots consume exactly 3 copper and 1 tin");
  h.assertTrue(m.energy.stored()==15200,"Bronze costs exactly 4800 FE");h.succeed();
 }
 @GameTest(template="empty") public static void missingAlloyQuantityDoesNotSpend(GameTestHelper h){
  var m=place(h,"alloy_smelter",2,2);m.setItem(0,new ItemStack(Items.COPPER_INGOT,2));m.setItem(1,item("tin_ingot",1));m.receiveEnergy(20000,false);ticks(h,m,200);
  h.assertTrue(m.energy.stored()==20000&&m.getItem(0).getCount()==2&&m.getItem(OUT).isEmpty(),"Insufficient counted ingredient spends nothing");h.succeed();
 }
 @GameTest(template="empty") public static void alloyFullOutputStopsBeforeConsumption(GameTestHelper h){
  var m=place(h,"alloy_smelter",2,2);m.setItem(0,new ItemStack(Items.COPPER_INGOT,3));m.setItem(1,item("tin_ingot",1));for(int i=OUT;i<END;i++)m.setItem(i,new ItemStack(Items.DIAMOND,64));m.receiveEnergy(20000,false);ticks(h,m,170);
  h.assertTrue(m.energy.stored()==20000&&m.getItem(0).getCount()==3&&m.getItem(1).getCount()==1,"Full output preserves both inputs and power");h.succeed();
 }
 @GameTest(template="empty") public static void sawmillReservesByproductSpace(GameTestHelper h){
  var m=place(h,"sawmill",2,2);m.setItem(0,new ItemStack(Items.OAK_LOG));for(int i=OUT;i<END;i++)m.setItem(i,new ItemStack(Items.DIAMOND,64));m.setItem(OUT,new ItemStack(Items.OAK_PLANKS,58));m.receiveEnergy(20000,false);ticks(h,m,100);
  h.assertTrue(m.getItem(0).getCount()==1&&m.energy.stored()==20000,"Plank space alone does not authorize consuming a log");m.setItem(OUT+1,ItemStack.EMPTY);ticks(h,m,80);
  h.assertTrue(m.getItem(OUT).getCount()==64&&m.getItem(OUT+1).is(Technologia.ITEMS.get("sawdust"))&&m.getItem(0).isEmpty(),"Both results commit once");h.succeed();
 }
 @GameTest(template="empty") public static void processSurvivesReloadWithoutFreeCompletion(GameTestHelper h){
  var m=place(h,"metal_press",2,2);m.setItem(0,new ItemStack(Items.IRON_INGOT));m.receiveEnergy(10000,false);ticks(h,m,30);
  var saved=m.saveWithFullMetadata(h.getLevel().registryAccess());var restored=(MachineBlockEntity)BlockEntity.loadStatic(m.getBlockPos(),m.getBlockState(),saved,h.getLevel().registryAccess());restored.setLevel(h.getLevel());ticks(h,restored,30);
  h.assertTrue(restored.getItem(OUT).is(Technologia.ITEMS.get("iron_plate"))&&restored.energy.stored()==9100,"Reload preserves paid progress and spends remaining energy");h.succeed();
 }
 @GameTest(template="empty") public static void changedInputsResetProgress(GameTestHelper h){
  var m=place(h,"metal_press",2,2);m.setItem(0,new ItemStack(Items.IRON_INGOT));m.receiveEnergy(10000,false);ticks(h,m,59);m.setItem(0,new ItemStack(Items.GOLD_INGOT));ticks(h,m,1);
  h.assertTrue(m.getItem(OUT).isEmpty()&&m.getItem(0).is(Items.GOLD_INGOT),"Changing recipe cannot finish with previously paid progress");ticks(h,m,59);h.assertTrue(m.getItem(OUT).is(Technologia.ITEMS.get("gold_plate")),"New recipe finishes after its own duration");h.succeed();
 }
 @GameTest(template="empty") public static void additionalProcessorsLoadRecipes(GameTestHelper h){
  var compactor=place(h,"compactor",2,2);compactor.setItem(0,new ItemStack(Items.IRON_INGOT,9));compactor.receiveEnergy(10000,false);ticks(h,compactor,100);h.assertTrue(compactor.getItem(OUT).is(Items.IRON_BLOCK),"Compactor loads counted recipe");
  var centrifuge=place(h,"centrifuge",4,2);centrifuge.setItem(0,new ItemStack(Items.GRAVEL));centrifuge.receiveEnergy(10000,false);ticks(h,centrifuge,100);h.assertTrue(centrifuge.getItem(OUT).is(Items.FLINT),"Centrifuge loads separation recipe");
  var recycler=place(h,"recycler",6,2);recycler.setItem(0,new ItemStack(Items.IRON_PICKAXE));recycler.receiveEnergy(10000,false);ticks(h,recycler,80);h.assertTrue(recycler.getItem(OUT).is(Items.IRON_NUGGET)&&recycler.getItem(OUT).getCount()==3,"Recycler returns bounded metal salvage");h.succeed();
 }
 @GameTest(template="empty") public static void advancedCellCapacityAndSolarInput(GameTestHelper h){
  var cell=place(h,"advanced_energy_cell",2,2);h.assertTrue(cell.receiveEnergy(Integer.MAX_VALUE,false)==5000000,"Advanced capacity bounded to 5 million");
  var solar=place(h,"advanced_solar_generator",5,2);h.assertTrue(solar.receiveEnergy(1000,false)==0,"Solar is output-only");
  h.setBlock(new BlockPos(5,3,2),Blocks.STONE);ticks(h,solar,5);h.assertTrue(solar.energy.stored()==0,"Covered solar never generates");h.succeed();
 }
 @GameTest(template="empty") public static void biomassFuelAndNoInputExtraction(GameTestHelper h){
  var biomass=place(h,"biomass_generator",2,2);biomass.setItem(0,item("sawdust",1));ticks(h,biomass,200);h.assertTrue(biomass.energy.stored()==4000&&biomass.getItem(0).isEmpty(),"Sawdust produces bounded 4000 FE at defaults");
  h.assertTrue(!biomass.canPlaceItem(0,new ItemStack(Items.COAL)),"Biomass rejects fossil fuel");h.succeed();
 }
 @GameTest(template="empty") public static void conduitLoopConservesEnergy(GameTestHelper h){
  var cell=place(h,"energy_cell",1,2);var m=place(h,"metal_press",4,2);cell.receiveEnergy(1000,false);
  for(var p:new BlockPos[]{new BlockPos(2,2,2),new BlockPos(3,2,2),new BlockPos(2,2,3),new BlockPos(3,2,3)})h.setBlock(p,Technologia.BLOCKS.get("energy_conduit"));
  int moved=EnergyTransport.transfer(cell);h.assertTrue(moved==200&&cell.energy.stored()==800&&m.energy.stored()==200,"Loop routes only once within shared 200 budget");h.succeed();
 }
 @GameTest(template="empty") public static void cellsDoNotPingPongThroughConduits(GameTestHelper h){
  var a=place(h,"energy_cell",2,2);var b=place(h,"advanced_energy_cell",4,2);h.setBlock(new BlockPos(3,2,2),Technologia.BLOCKS.get("energy_conduit"));a.receiveEnergy(1000,false);
  h.assertTrue(EnergyTransport.transfer(a)==0&&b.energy.stored()==0&&a.energy.stored()==1000,"Basic and advanced buffers do not circulate energy");h.succeed();
 }
 @GameTest(template="empty") public static void filteredTransferAndFullTarget(GameTestHelper h){
  var source=new SimpleContainer(2);source.setItem(0,new ItemStack(Items.IRON_INGOT,16));source.setItem(1,new ItemStack(Items.GOLD_INGOT,5));var target=new SimpleContainer(1);target.setItem(0,new ItemStack(Items.GOLD_INGOT,62));
  int moved=ItemTransferBlockEntity.move(source,Direction.NORTH,target,Direction.SOUTH,new ItemStack(Items.GOLD_INGOT),8);
  h.assertTrue(moved==2&&source.getItem(1).getCount()==3&&target.getItem(0).getCount()==64&&source.getItem(0).getCount()==16,"Only filter matches move into actual available space");
  h.assertTrue(ItemTransferBlockEntity.move(source,Direction.NORTH,target,Direction.SOUTH,ItemStack.EMPTY,8)==0,"Full target preserves source");h.succeed();
 }
 @GameTest(template="empty") public static void sidedTransferProtectsMachineInputs(GameTestHelper h){
  var machine=place(h,"alloy_smelter",2,2);machine.setItem(0,new ItemStack(Items.COPPER_INGOT,3));machine.setItem(1,item("tin_ingot",1));machine.setItem(OUT,item("bronze_ingot",4));var target=new SimpleContainer(2);
  h.assertTrue(ItemTransferBlockEntity.move(machine,Direction.EAST,target,Direction.WEST,ItemStack.EMPTY,8)==4,"Only alloy output is extracted");h.assertTrue(machine.getItem(0).getCount()==3&&machine.getItem(1).getCount()==1,"Both inputs retained");h.succeed();
 }
 @GameTest(template="empty") public static void transferFilterPersistsWithoutOwningSample(GameTestHelper h){
  var p=new BlockPos(2,2,2);h.setBlock(p,Technologia.BLOCKS.get("item_transfer"));var be=(ItemTransferBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));var sample=new ItemStack(Items.COAL,12);be.setFilter(sample);sample.setCount(3);
  var restored=(ItemTransferBlockEntity)BlockEntity.loadStatic(be.getBlockPos(),be.getBlockState(),be.saveWithFullMetadata(h.getLevel().registryAccess()),h.getLevel().registryAccess());h.assertTrue(restored.filter().getCount()==1&&restored.filter().is(Items.COAL),"Ghost filter owns only a copied identity, saved across reload");h.succeed();
 }
 @GameTest(template="empty") public static void multiCoreCatalogueAndTopologyChecks(GameTestHelper h){
  var a=place(h,"storage_core",2,2);var b=place(h,"storage_core",3,2);var terminal=place(h,"storage_terminal",4,2);a.setItem(0,new ItemStack(Items.COAL,40));b.setItem(0,new ItemStack(Items.COAL,40));var player=near(h,terminal.getBlockPos());
  var menu=new StorageMenu(1,player.getInventory(),StorageNetwork.connectedContainer(a,terminal.getBlockPos()));h.assertTrue(menu.capacity()==108&&menu.entries().getFirst().count()==80,"Two cores aggregate with real 108-slot capacity");
  menu.clickMenuButton(player,StorageMenu.actionButton(menu.entries().getFirst(),StorageMenu.Action.STACK));h.assertTrue(menu.getCarried().getCount()==64&&a.getItem(0).getCount()+b.getItem(0).getCount()==16,"Withdrawal across cores conserves item total");
  place(h,"storage_core",3,3);h.assertTrue(!menu.stillValid(player),"Topology changes invalidate existing snapshot");h.succeed();
 }
 @GameTest(template="empty") public static void highStorageSlotsAndPaddingAreSafe(GameTestHelper h){
  var storage=new SimpleContainer(216);storage.setItem(215,new ItemStack(Items.DIAMOND,7));var player=near(h,h.absolutePos(new BlockPos(2,2,2)));var menu=new StorageMenu(1,player.getInventory(),storage);
  h.assertTrue(menu.clickMenuButton(player,StorageMenu.actionButton(menu.entries().getFirst(),StorageMenu.Action.STACK))&&menu.getCarried().getCount()==7&&storage.isEmpty(),"Protocol addresses slot215 without truncation");
  var solo=new SimpleContainer(54);for(int i=0;i<54;i++)solo.setItem(i,new ItemStack(Items.COAL,64));var singleMenu=new StorageMenu(2,player.getInventory(),solo);singleMenu.setCarried(new ItemStack(Items.DIAMOND));h.assertTrue(!singleMenu.clickMenuButton(player,StorageMenu.DEPOSIT_BUTTON)&&singleMenu.getCarried().getCount()==1,"Padding never stores items");h.succeed();
 }
 @GameTest(template="empty") public static void transferDirectionAndRedstonePause(GameTestHelper h){
  var source=place(h,"storage_core",2,2);var target=place(h,"storage_core",4,2);source.setItem(0,new ItemStack(Items.DIAMOND,12));
  var p=new BlockPos(3,2,2);h.setBlock(p,Technologia.BLOCKS.get("item_transfer").defaultBlockState().setValue(ItemTransferBlock.FACING,Direction.EAST));
  var transfer=(ItemTransferBlockEntity)h.getLevel().getBlockEntity(h.absolutePos(p));
  h.assertTrue(transfer.transfer()==8&&target.getItem(0).getCount()==8&&source.getItem(0).getCount()==4,"Arrow direction moves eight items from rear to front");
  h.setBlock(new BlockPos(3,2,3),Blocks.REDSTONE_BLOCK);
  h.assertTrue(transfer.transfer()==0&&source.getItem(0).getCount()==4,"Redstone immediately pauses transfer");h.succeed();
 }
 @GameTest(template="empty") public static void machineMenuMapsAlloyAndSingleInputs(GameTestHelper h){
  var alloy=place(h,"alloy_smelter",2,2);alloy.setItem(1,item("tin_ingot",1));alloy.setItem(OUT,item("bronze_ingot",4));var player=near(h,alloy.getBlockPos());
  var menu=new MachineMenu(1,player.getInventory(),alloy,alloy.data);
  h.assertTrue(menu.slots.get(1).isActive()&&!menu.slots.get(2).isActive()&&menu.slots.get(OUT).getItem().is(Technologia.ITEMS.get("bronze_ingot")),"A Mk I alloy smelter shows two ingredient slots and its results");
  menu.quickMoveStack(player,OUT);h.assertTrue(alloy.getItem(OUT).isEmpty()&&alloy.getItem(1).getCount()==1,"Shift withdrawal preserves second ingredient");
  var press=place(h,"metal_press",3,2);press.setItem(OUT,item("iron_plate",3));var single=new MachineMenu(2,player.getInventory(),press,press.data);
  h.assertTrue(single.slots.get(0).isActive()&&!single.slots.get(1).isActive()&&single.slots.get(OUT).getItem().getCount()==3,"A Mk I press shows one ingredient slot");
  press.setItem(1,item("iron_plate",2));h.assertTrue(single.quickMoveStack(player,1).isEmpty()&&press.getItem(1).getCount()==2,"A hidden slot cannot be used to withdraw");press.setItem(1,ItemStack.EMPTY);
  single.clicked(1,0,net.minecraft.world.inventory.ClickType.PICKUP,player);h.assertTrue(single.getCarried().isEmpty(),"A hidden slot ignores clicks");
  single.quickMoveStack(player,OUT);h.assertTrue(press.getItem(OUT).isEmpty(),"Active output still withdraws");h.succeed();
 }
 @GameTest(template="empty") public static void advancementsAndOpenMachineShapes(GameTestHelper h){
  h.assertTrue(h.getLevel().getServer().getAdvancements().get(Technologia.id("workshop/advanced_solar"))!=null,"Workshop milestones decode into server advancement registry");
  var solar=place(h,"solar_generator",2,2);h.assertTrue(solar.getBlockState().getShape(h.getLevel(),solar.getBlockPos()).max(Direction.Axis.Y)<1,"Panel selection profile follows raised low model");
  var press=place(h,"metal_press",4,2);var shape=press.getBlockState().getShape(h.getLevel(),press.getBlockPos());
  h.assertTrue(!net.minecraft.world.phys.shapes.Shapes.joinIsNotEmpty(net.minecraft.world.phys.shapes.Shapes.box(.22,.65,.35,.3,.75,.4),shape,net.minecraft.world.phys.shapes.BooleanOp.AND),"Press opening remains physically open");h.succeed();
 }
 @GameTest(template="empty") public static void excessiveCoreCountFailsClosed(GameTestHelper h){
  for(int i=1;i<=5;i++)place(h,"storage_core",i,2);var terminal=place(h,"storage_terminal",6,2);h.assertTrue(StorageNetwork.findCores(h.getLevel(),terminal.getBlockPos()).isEmpty(),"Unsupported fifth core cannot hide part of a network");h.succeed();
 }
}
