package dev.technologia.client;

import dev.technologia.machine.MachineMenu;
import dev.technologia.machine.MachineKind;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Drawn from the shared palette; no loader-specific UI or external texture dependency. */
public final class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private Button toggle;
    private Button rescan;
    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 230; imageHeight = 246; inventoryLabelX = 34; inventoryLabelY = 152;
    }
    @Override protected void init() {
        super.init();
        toggle = addRenderableWidget(Button.builder(Component.translatable("ui.technologia.toggle"), b -> send(0))
                .bounds(leftPos + 16, topPos + 127, 96, 20).build());
        rescan = addRenderableWidget(Button.builder(Component.translatable("ui.technologia.rescan"), b -> send(1))
                .bounds(leftPos + 118, topPos + 127, 96, 20).build());
    }
    private void send(int id) { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        toggle.setMessage(Component.translatable(menu.enabled() ? "ui.technologia.pause" : "ui.technologia.start"));
        rescan.active = menu.kind() == MachineKind.MINER;
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + 16 && mouseX <= leftPos + 214 && mouseY >= topPos + 31 && mouseY <= topPos + 40)
            graphics.renderTooltip(font, Component.literal(menu.energy() + " / " + menu.kind().capacity + " FE"), mouseX, mouseY);
        if (mouseX >= leftPos + 33 && mouseX <= leftPos + 51 && mouseY >= topPos + 65 && mouseY <= topPos + 83)
            graphics.renderTooltip(font, Component.translatable("hint.technologia." + menu.kind().id), mouseX, mouseY);
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xff0c121b);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xff1b2735);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 4, 0xff55d9d0);
        g.fill(x + 16, y + 31, x + 214, y + 39, 0xff0c121b);
        int fill = (int) (198L * menu.energy() / Math.max(1, menu.kind().capacity));
        g.fill(x + 16, y + 31, x + 16 + fill, y + 39, 0xff55d9d0);
        for (var slot : menu.slots) {
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, slot.index == 0 && slot.container == menu.slots.getFirst().container ? 0xffe8b46a : 0xff415267);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xff101923);
        }
        g.fill(x + 34, y + 121, x + 196, y + 123, 0xff101923);
        g.fill(x + 34, y + 121, x + 34 + Math.min(162, 162 * menu.progress() / menu.duration()), y + 123, 0xffe8b46a);
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 16, 14, 0xffedf4fc, false);
        g.drawString(font, Component.translatable("status.technologia." + menu.status()), 16, 44, 0xffaebed1, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xffaebed1, false);
    }
}
