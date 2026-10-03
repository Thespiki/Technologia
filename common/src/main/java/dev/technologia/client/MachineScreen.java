package dev.technologia.client;

import dev.technologia.machine.MachineBlockEntity;
import dev.technologia.machine.MachineMenu;
import dev.technologia.machine.MachineKind;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;

/** Drawn from the shared palette; no loader-specific UI or external texture dependency. */
public final class MachineScreen extends AbstractContainerScreen<MachineMenu> {
    private static final int BAR_LEFT = 16, BAR_RIGHT = 264, BAR_TOP = 31, BAR_BOTTOM = 39;
    private Button toggle;
    private Button rescan;
    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 280; imageHeight = 238; inventoryLabelX = MachineMenu.INVENTORY_X; inventoryLabelY = 149;
    }
    @Override protected void init() {
        super.init();
        toggle = addRenderableWidget(new PanelButton(leftPos + 100, topPos + 122, 78, 20, Component.translatable("ui.technologia.toggle"), b -> send(0)));
        rescan = addRenderableWidget(new PanelButton(leftPos + 184, topPos + 122, 78, 20, Component.translatable("ui.technologia.rescan"), b -> send(1)));
    }
    private void send(int id) { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        toggle.setMessage(Component.translatable(menu.enabled() ? "ui.technologia.pause" : "ui.technologia.start"));
        toggle.visible = menu.kind().capacity > 0 && !menu.kind().isCell();
        rescan.visible = menu.kind() == MachineKind.MINER;
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + BAR_LEFT && mouseX <= leftPos + BAR_RIGHT && mouseY >= topPos + BAR_TOP && mouseY <= topPos + BAR_BOTTOM + 1) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("ui.technologia.energy", menu.energy(), menu.capacity()));
            lines.add(Component.translatable("ui.technologia.transfer_rate", MachineBlockEntity.scaledTransfer(menu.tier())));
            if (menu.boost() > 0) lines.add(Component.translatable("ui.technologia.bloom_boost", menu.boost() * MachineBlockEntity.BLOOM_BONUS_PERCENT));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
        if (menu.hasInput() && menu.slots.getFirst().getItem().isEmpty() && menu.getCarried().isEmpty()
                && mouseX >= leftPos + MachineMenu.INPUT_X - 1 && mouseX < leftPos + MachineMenu.INPUT_X + 17
                && mouseY >= topPos + MachineMenu.GRID_Y - 1 && mouseY < topPos + MachineMenu.GRID_Y + 17)
            graphics.renderTooltip(font, Component.translatable("hint.technologia." + menu.kind().id), mouseX, mouseY);
    }
    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xff0c121b);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xff1b2735);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 4, 0xff55d9d0);
        g.fill(x + BAR_LEFT, y + BAR_TOP, x + BAR_RIGHT, y + BAR_BOTTOM, 0xff0c121b);
        int width = BAR_RIGHT - BAR_LEFT;
        int fill = (int) Math.min(width, (long) width * menu.energy() / Math.max(1, menu.capacity()));
        g.fill(x + BAR_LEFT, y + BAR_TOP, x + BAR_LEFT + fill, y + BAR_BOTTOM, 0xff55d9d0);
        for (var slot : menu.slots) {
            if (!slot.isActive()) continue;
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, slot.index < MachineBlockEntity.INPUT_SLOTS ? 0xffe8b46a : 0xff415267);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xff101923);
        }
        // One progress line per lane, drawn along the bottom edge of that lane's ingredient slots.
        int perLane = menu.kind().inputsPerLane();
        for (int lane = 0; lane < menu.lanes(); lane++) {
            int progress = menu.laneProgress(lane);
            if (progress <= 0) continue;
            int first = lane * perLane, span = perLane * 18 - 2;
            int px = x + MachineMenu.inputX(first), py = y + MachineMenu.inputY(first) + 15;
            g.fill(px, py, px + span, py + 2, 0xff0c121b);
            g.fill(px, py, px + Math.max(1, span * progress / 1000), py + 2, 0xff55d9d0);
        }
    }
    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, title, 16, 14, 0xffedf4fc, false);
        if (menu.kind().isTierable()) {
            String tier = menu.tier().name();
            g.drawString(font, tier, BAR_RIGHT - font.width(tier), 14, 0xff55d9d0, false);
        }
        g.drawString(font, Component.translatable("status.technologia." + menu.status()), 16, 44, 0xffaebed1, false);
        if (menu.hasInput()) g.drawString(font, Component.translatable(menu.kind() == MachineKind.MINER ? "ui.technologia.filter" : "ui.technologia.input"), MachineMenu.INPUT_X, 52, 0xffe8b46a, false);
        if (menu.hasOutput()) g.drawString(font, Component.translatable("ui.technologia.output"), MachineMenu.OUTPUT_X, 52, 0xffaebed1, false);
        if (menu.kind().isCell() || menu.kind().isGenerator()) {
            int tx = menu.hasInput() ? 40 : 16;
            g.drawWordWrap(font, Component.translatable("hint.technologia." + menu.kind().id), tx, 67, BAR_RIGHT - tx, 0xffaebed1);
        }
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xffaebed1, false);
    }
}
