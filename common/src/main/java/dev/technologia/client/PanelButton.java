package dev.technologia.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;

/** Shared controls match the machine panels, including visible keyboard focus. */
public final class PanelButton extends Button {
    public PanelButton(int x, int y, int width, int height, Component label, OnPress action) {
        super(x, y, width, height, label, action, DEFAULT_NARRATION);
    }
    @Override protected void renderWidget(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        boolean focus = active && isHoveredOrFocused();
        int x = getX(), y = getY();
        g.fill(x, y, x + width, y + height, focus ? 0xff55d9d0 : 0xff415267);
        g.fill(x + 1, y + 1, x + width - 1, y + height - 1, focus ? 0xff284449 : 0xff101923);
        renderScrollingString(g, Minecraft.getInstance().font, 4, active ? 0xffedf4fc : 0xff697b8d);
    }
}
