package dev.technologia;
import dev.technologia.client.MachineScreen;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
@Mod.EventBusSubscriber(modid = Technologia.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TechnologiaForgeClient {
    @SubscribeEvent public static void setup(FMLClientSetupEvent event) { event.enqueueWork(() -> {
        MenuScreens.register(Technologia.MACHINE_MENU, MachineScreen::new);
        MenuScreens.register(Technologia.STORAGE_MENU, dev.technologia.client.StorageScreen::new);
        MenuScreens.register(Technologia.GUIDE_MENU, dev.technologia.client.GuideScreen::new);
    }); }
}
