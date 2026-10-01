package dev.technologia;
import dev.technologia.client.MachineScreen;
import net.fabricmc.api.ClientModInitializer;
import net.minecraft.client.gui.screens.MenuScreens;
public final class TechnologiaFabricClient implements ClientModInitializer {
    public void onInitializeClient() {
        net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap.INSTANCE.putBlock(Technologia.BLOCKS.get("reinforced_glass"), net.minecraft.client.renderer.RenderType.cutout());
        MenuScreens.register(Technologia.MACHINE_MENU, MachineScreen::new);
        MenuScreens.register(Technologia.STORAGE_MENU, dev.technologia.client.StorageScreen::new);
        MenuScreens.register(Technologia.GUIDE_MENU, dev.technologia.client.GuideScreen::new);
    }
}
