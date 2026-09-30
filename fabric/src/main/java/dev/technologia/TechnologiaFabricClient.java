package dev.technologia;
import dev.technologia.client.MachineScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
public final class TechnologiaFabricClient implements ClientModInitializer {
    public void onInitializeClient() {
        MenuScreens.register(Technologia.MACHINE_MENU, MachineScreen::new);
        MenuScreens.register(Technologia.STORAGE_MENU, dev.technologia.client.StorageScreen::new);
        MenuScreens.register(Technologia.GUIDE_MENU, dev.technologia.client.GuideScreen::new);
    }
}
