package dev.technologia;
import dev.technologia.client.MachineScreen;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RegisterMenuScreensEvent;
@EventBusSubscriber(modid = Technologia.MOD_ID, bus = EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public final class TechnologiaNeoForgeClient {
    @SubscribeEvent public static void screens(RegisterMenuScreensEvent event) {
        event.register(Technologia.MACHINE_MENU, MachineScreen::new);
        event.register(Technologia.STORAGE_MENU, dev.technologia.client.StorageScreen::new);
        event.register(Technologia.GUIDE_MENU, dev.technologia.client.GuideScreen::new);
    }
}
