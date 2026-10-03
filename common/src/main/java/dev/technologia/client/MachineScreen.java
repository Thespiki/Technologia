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
    private static final int CONTROLS_Y = 122, CHANNEL_Y = 90;
    private Button toggle, rescan, redstone, eject, limit;
    private final Button[] channel = new Button[4];
    public MachineScreen(MachineMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 280; imageHeight = 238; inventoryLabelX = MachineMenu.INVENTORY_X; inventoryLabelY = 149;
    }
    @Override protected void init() {
        super.init();
        // Three controls share the row under the results, each wide enough for its longest label; the miner's rescan sits under its filter slot.
        toggle = addRenderableWidget(new PanelButton(leftPos + 100, topPos + CONTROLS_Y, 46, 20, Component.translatable("ui.technologia.toggle"), b -> send(MachineMenu.BUTTON_TOGGLE)));
        redstone = addRenderableWidget(new PanelButton(leftPos + 149, topPos + CONTROLS_Y, 52, 20, Component.empty(), b -> send(MachineMenu.BUTTON_REDSTONE)));
        eject = addRenderableWidget(new PanelButton(leftPos + 204, topPos + CONTROLS_Y, 58, 20, Component.empty(), b -> send(MachineMenu.BUTTON_EJECT)));
        rescan = addRenderableWidget(new PanelButton(leftPos + MachineMenu.INPUT_X - 1, topPos + 86, 72, 20, Component.translatable("ui.technologia.rescan"), b -> send(MachineMenu.BUTTON_RESCAN)));
        int[] ids = {MachineMenu.BUTTON_CHANNEL_DOWN_10, MachineMenu.BUTTON_CHANNEL_DOWN, MachineMenu.BUTTON_CHANNEL_UP, MachineMenu.BUTTON_CHANNEL_UP_10};
        String[] labels = {"-10", "-1", "+1", "+10"};
        for (int i = 0; i < 4; i++) {
            final int id = ids[i];
            channel[i] = addRenderableWidget(new PanelButton(leftPos + 16 + i * 34, topPos + CHANNEL_Y, 30, 20, Component.literal(labels[i]), b -> send(id)));
        }
        limit = addRenderableWidget(new PanelButton(leftPos + 160, topPos + CHANNEL_Y, 102, 20, Component.empty(), b -> send(MachineMenu.BUTTON_LIMIT)));
    }
    private void send(int id) { if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id); }
    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        MachineKind kind = menu.kind();
        toggle.setMessage(Component.translatable(menu.enabled() ? "ui.technologia.pause" : "ui.technologia.start"));
        toggle.visible = kind.hasSwitch();
        rescan.visible = kind == MachineKind.MINER;
        redstone.visible = kind.hasSwitch();
        redstone.setMessage(Component.translatable("ui.technologia.redstone." + menu.redstoneMode()));
        eject.visible = kind.hasOutput();
        eject.setMessage(Component.translatable(menu.ejects() ? "ui.technologia.eject_on" : "ui.technologia.eject_off"));
        for (Button button : channel) button.visible = kind.isWireless();
        limit.visible = kind == MachineKind.WIRELESS_SENDER;
        limit.setMessage(Component.translatable("ui.technologia.wireless_limit", menu.wirelessLimit()));
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        if (mouseX >= leftPos + BAR_LEFT && mouseX <= leftPos + BAR_RIGHT && mouseY >= topPos + BAR_TOP && mouseY <= topPos + BAR_BOTTOM + 1) {
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable("ui.technologia.energy", menu.energy(), menu.capacity()));
            lines.add(Component.translatable("ui.technologia.transfer_rate", MachineBlockEntity.transferRate(kind, menu.tier())));
            if (menu.rate() > 0) lines.add(Component.translatable("ui.technologia.rate", menu.rate()));
            if (menu.boost() > 0) lines.add(Component.translatable("ui.technologia.bloom_boost", menu.boost() * MachineBlockEntity.BLOOM_BONUS_PERCENT));
            graphics.renderComponentTooltip(font, lines, mouseX, mouseY);
        }
        if (redstone.visible && redstone.isHovered()) graphics.renderTooltip(font, Component.translatable("ui.technologia.redstone_hint"), mouseX, mouseY);
        if (eject.visible && eject.isHovered()) graphics.renderTooltip(font, Component.translatable("ui.technologia.eject_hint"), mouseX, mouseY);
        if (menu.hasInput() && menu.slots.getFirst().getItem().isEmpty() && menu.getCarried().isEmpty()
                && mouseX >= leftPos + MachineMenu.INPUT_X - 1 && mouseX < leftPos + MachineMenu.INPUT_X + 17
                && mouseY >= topPos + MachineMenu.GRID_Y - 1 && mouseY < topPos + MachineMenu.GRID_Y + 17)
            graphics.renderTooltip(font, Component.translatable("hint.technologia." + kind.id), mouseX, mouseY);
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
        MachineKind kind = menu.kind();
        g.drawString(font, title, 16, 14, 0xffedf4fc, false);
        if (kind.isTierable()) {
            String tier = menu.tier().name();
            g.drawString(font, tier, BAR_RIGHT - font.width(tier), 14, 0xff55d9d0, false);
        }
        g.drawString(font, Component.translatable("status.technologia." + menu.status()), 16, 44, 0xffaebed1, false);
        if (menu.hasInput()) g.drawString(font, Component.translatable(kind == MachineKind.MINER ? "ui.technologia.filter" : "ui.technologia.input"), MachineMenu.INPUT_X, 52, 0xffe8b46a, false);
        if (menu.hasOutput()) g.drawString(font, Component.translatable("ui.technologia.output"), MachineMenu.OUTPUT_X, 52, 0xffaebed1, false);
        if (kind == MachineKind.SIEVE) {
            Component mesh = Component.translatable("ui.technologia.mesh", Component.translatable("ui.technologia.mesh." + menu.mesh()));
            g.drawString(font, mesh, BAR_RIGHT - font.width(mesh), 44, 0xffe8b46a, false);
        }
        if (kind.isWireless()) {
            g.drawString(font, Component.translatable("ui.technologia.channel", menu.channel()), 16, CHANNEL_Y - 12, 0xff55d9d0, false);
            // Two lines at most fit between the status line and the channel controls.
            g.drawWordWrap(font, Component.translatable("hint.technologia." + kind.id), 16, 56, BAR_RIGHT - 16, 0xffaebed1);
        } else if (menu.hasOutput() && !menu.hasInput()) {
            g.drawWordWrap(font, Component.translatable("hint.technologia." + kind.id), MachineMenu.INPUT_X, 58, 82, 0xffaebed1);
        } else if (!menu.hasInput() && !menu.hasOutput() || kind.isFuelGenerator()) {
            // Machines without a grid explain themselves in the free space.
            int tx = menu.hasInput() ? 40 : 16;
            g.drawWordWrap(font, Component.translatable("hint.technologia." + kind.id), tx, 67, BAR_RIGHT - tx, 0xffaebed1);
        }
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xffaebed1, false);
    }
}
