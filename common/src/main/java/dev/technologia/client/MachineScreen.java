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
        imageWidth = 246; imageHeight = 238; inventoryLabelX = 42; inventoryLabelY = 149;
    }
    @Override protected void init() {
        super.init();
        toggle = addRenderableWidget(new PanelButton(leftPos + 16, topPos + 125, 104, 20, Component.translatable("ui.technologia.toggle"), b -> send(0)));
        rescan = addRenderableWidget(new PanelButton(leftPos + 126, topPos + 125, 104, 20, Component.translatable("ui.technologia.rescan"), b -> send(1)));
    }
    private void send(int id) { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        toggle.setMessage(Component.translatable(menu.enabled() ? "ui.technologia.pause" : "ui.technologia.start"));
        toggle.visible = menu.kind().capacity > 0 && !menu.kind().isCell();
        rescan.visible = menu.kind() == MachineKind.MINER;
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + 16 && mouseX <= leftPos + 230 && mouseY >= topPos + 31 && mouseY <= topPos + 40)
            graphics.renderTooltip(font, Component.literal(menu.energy() + " / " + menu.kind().capacity + " FE"), mouseX, mouseY);
        if (menu.hasInput() && menu.slots.getFirst().getItem().isEmpty() && mouseX >= leftPos + 17 && mouseX <= leftPos + 35 && mouseY >= topPos + 63 && mouseY <= topPos + 81)
            graphics.renderTooltip(font, Component.translatable("hint.technologia." + menu.kind().id), mouseX, mouseY);
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xff0c121b);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xff1b2735);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 4, 0xff55d9d0);
        g.fill(x + 16, y + 31, x + 230, y + 39, 0xff0c121b);
        int fill = (int) (214L * menu.energy() / Math.max(1, menu.kind().capacity));
        g.fill(x + 16, y + 31, x + 16 + fill, y + 39, 0xff55d9d0);
        for (var slot : menu.slots) {
            if (!slot.isActive()) continue;
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, slot.index < 2 && slot.container == menu.slots.getFirst().container ? 0xffe8b46a : 0xff415267);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xff101923);
        }
        if (menu.hasOutput()) {
            g.fill(x + 65, y + 119, x + 227, y + 121, 0xff101923);
            g.fill(x + 65, y + 119, x + 65 + Math.min(162, 162 * menu.progress() / menu.duration()), y + 121, 0xffe8b46a);
        }
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 16, 14, 0xffedf4fc, false);
        g.drawString(font, Component.translatable("status.technologia." + menu.status()), 16, 44, 0xffaebed1, false);
        if (menu.hasInput()) g.drawString(font, Component.translatable(menu.kind() == MachineKind.MINER ? "ui.technologia.filter" : "ui.technologia.input"), 17, 52, 0xffe8b46a, false);
        if (menu.hasOutput()) g.drawString(font, Component.translatable("ui.technologia.output"), 65, 52, 0xffaebed1, false);
        if (menu.kind().isCell() || menu.kind().isGenerator()) {
            int tx = menu.hasInput() ? 54 : 16;
            g.drawWordWrap(font, Component.translatable("hint.technologia." + menu.kind().id), tx, 67, 230 - tx, 0xffaebed1);
        }
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xffaebed1, false);
    }
}
