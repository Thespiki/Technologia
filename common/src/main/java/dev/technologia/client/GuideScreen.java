package dev.technologia.client;

import dev.technologia.Technologia;
import dev.technologia.guide.FieldGuide;
import dev.technologia.guide.GuideMenu;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/** Original chapter atlas and readable articles, drawn from Technologia's machine palette. */
public final class GuideScreen extends AbstractContainerScreen<GuideMenu> {
    private static final int PANEL = 0xff101923, EDGE = 0xff344658, TEXT = 0xffedf4fc;
    private static final int MUTED = 0xffaebed1, CYAN = 0xff55d9d0, AMBER = 0xffe8b46a;
    private final List<FieldGuide.Chapter> chapters = FieldGuide.chapters();
    private final List<FieldGuide.Page> pages = FieldGuide.pages();
    private final List<PanelButton> chapterButtons = new ArrayList<>();
    private PanelButton previous, next;
    private int page = -1, scroll, maxScroll, sideWidth, contentX, contentY, contentWidth, contentHeight;
    private boolean draggingScroll;

    public GuideScreen(GuideMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
    }

    @Override protected void init() {
        imageWidth = Math.min(680, width - 16);
        imageHeight = Math.min(430, height - 16);
        super.init();
        sideWidth = imageWidth >= 480 ? 126 : 86;
        contentX = leftPos + sideWidth + 28;
        contentY = topPos + 50;
        contentWidth = imageWidth - sideWidth - 42;
        contentHeight = imageHeight - 86;
        chapterButtons.clear();
        addRenderableWidget(new PanelButton(leftPos + imageWidth - 80, topPos + 12, 42, 20,
                Component.literal("Atlas"), button -> showPage(-1)));
        addRenderableWidget(new PanelButton(leftPos + imageWidth - 32, topPos + 12, 20, 20,
                Component.literal("X"), button -> onClose()));
        int rowHeight = Math.min(26, contentHeight / chapters.size());
        for (int i = 0; i < chapters.size(); i++) {
            int chapter = i;
            PanelButton button = new PanelButton(leftPos + 10, contentY + i * rowHeight,
                    sideWidth, rowHeight - 2, Component.literal(chapters.get(i).title()),
                    clicked -> showPage(chapters.get(chapter).firstPage()));
            chapterButtons.add(addRenderableWidget(button));
        }
        previous = addRenderableWidget(new PanelButton(contentX, topPos + imageHeight - 27, 46, 18,
                Component.literal("< Back"), button -> showPage(page - 1)));
        next = addRenderableWidget(new PanelButton(leftPos + imageWidth - 62, topPos + imageHeight - 27,
                46, 18, Component.literal("Next >"), button -> showPage(page + 1)));
        updateButtons();
        scroll = 0;
    }

    private void showPage(int selected) {
        page = Math.clamp(selected, -1, pages.size() - 1);
        scroll = 0;
        draggingScroll = false;
        updateButtons();
    }

    private void updateButtons() {
        if (previous == null || next == null) return;
        previous.active = page >= 0;
        next.active = page >= 0 && page < pages.size() - 1;
        for (int i = 0; i < chapterButtons.size(); i++) {
            boolean selected = page >= 0 && pages.get(page).chapter() == i;
            chapterButtons.get(i).setMessage(Component.literal(chapters.get(i).title())
                    .withStyle(selected ? ChatFormatting.AQUA : ChatFormatting.WHITE));
        }
    }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x + 4, y + 5, x + imageWidth + 4, y + imageHeight + 5, 0x88000000);
        g.fill(x, y, x + imageWidth, y + imageHeight, EDGE);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, PANEL);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 3, CYAN);
        g.fill(x + 1, y + 39, x + imageWidth - 1, y + 40, EDGE);
        g.fill(x + sideWidth + 17, y + 40, x + sideWidth + 18, y + imageHeight - 1, EDGE);
        g.drawString(font, Component.literal("TECHNOLOGIA").withStyle(ChatFormatting.BOLD), x + 12, y + 12, TEXT, false);
        g.drawString(font, "FIELD GUIDE", x + 12, y + 25, CYAN, false);
        if (imageWidth >= 480) g.drawString(font, "A workshop worth building", x + 140, y + 21, MUTED, false);
        g.fill(contentX, y + imageHeight - 33, x + imageWidth - 15, y + imageHeight - 32, EDGE);
        if (page >= 0) {
            String count = (page + 1) + " / " + pages.size();
            g.drawCenteredString(font, count, (contentX + x + imageWidth - 16) / 2, y + imageHeight - 21, MUTED);
        } else {
            g.drawCenteredString(font, "9 chapters", (contentX + x + imageWidth - 16) / 2, y + imageHeight - 21, MUTED);
        }
        if (page >= 0) {
            int rowHeight = Math.min(26, contentHeight / chapters.size());
            int markerY = contentY + pages.get(page).chapter() * rowHeight;
            g.fill(x + 6, markerY, x + 8, markerY + rowHeight - 2, CYAN);
        }
        g.enableScissor(contentX, contentY, contentX + contentWidth, contentY + contentHeight);
        if (page < 0) renderAtlas(g, mouseX, mouseY);
        else renderArticle(g);
        g.disableScissor();
        drawScrollbar(g);
    }

    private void renderAtlas(GuiGraphics g, int mouseX, int mouseY) {
        int y = contentY - scroll;
        g.drawString(font, Component.literal("WORKSHOP ATLAS").withStyle(ChatFormatting.BOLD), contentX + 2, y + 2, TEXT, false);
        int introBottom = drawWrapped(g, "Choose a chapter. Build the workshop, then explore what comes next.",
                contentX + 2, y + 18, contentWidth - 14, MUTED);
        int gridY = introBottom + 12;
        int columns = contentWidth >= 330 ? 3 : 2;
        int cardWidth = (contentWidth - 14 - (columns - 1) * 8) / columns;
        for (int i = 0; i < chapters.size(); i++) {
            int cardX = contentX + (i % columns) * (cardWidth + 8);
            int cardY = gridY + (i / columns) * 65;
            if (i % columns != columns - 1 && i + 1 < chapters.size())
                g.fill(cardX + cardWidth, cardY + 28, cardX + cardWidth + 8, cardY + 29, EDGE);
            if (i + columns < chapters.size())
                g.fill(cardX + cardWidth / 2, cardY + 57, cardX + cardWidth / 2 + 1, cardY + 65, EDGE);
            boolean hovered = withinContent(mouseX, mouseY) && inside(mouseX, mouseY, cardX, cardY, cardWidth, 57);
            var chapter = chapters.get(i);
            int accent = chapter.planned() ? AMBER : CYAN;
            g.fill(cardX, cardY, cardX + cardWidth, cardY + 57, hovered ? accent : EDGE);
            g.fill(cardX + 1, cardY + 1, cardX + cardWidth - 1, cardY + 56, hovered ? 0xff213842 : 0xff16232f);
            g.fill(cardX + 1, cardY + 1, cardX + cardWidth - 1, cardY + 3, accent);
            g.renderItem(icon(chapter), cardX + 6, cardY + 8);
            String number = String.format("%02d", i + 1);
            g.drawString(font, number, cardX + cardWidth - font.width(number) - 7, cardY + 12, accent, false);
            g.drawString(font, chapter.title(), cardX + 6, cardY + 30, TEXT, false);
            int count = chapter.lastPage() - chapter.firstPage();
            String caption = chapter.planned() ? "PLANNED" : count + (count == 1 ? " article" : " articles");
            g.drawString(font, caption, cardX + 6, cardY + 43, chapter.planned() ? AMBER : MUTED, false);
        }
        int rows = (chapters.size() + columns - 1) / columns;
        maxScroll = Math.max(0, gridY + rows * 65 - 8 - (contentY - scroll) - contentHeight);
        scroll = Math.min(scroll, maxScroll);
    }

    private void renderArticle(GuiGraphics g) {
        var article = pages.get(page);
        var chapter = chapters.get(article.chapter());
        int y = contentY - scroll;
        int accent = chapter.planned() ? AMBER : CYAN;
        String tag = chapter.planned() ? "PLANNED / NOT IMPLEMENTED" : chapter.title().toUpperCase(java.util.Locale.ROOT);
        y = drawWrapped(g, tag, contentX + 2, y + 1, contentWidth - 14, accent) + 10;
        g.fill(contentX + 2, y - 2, contentX + 24, y + 20, EDGE);
        g.fill(contentX + 3, y - 1, contentX + 23, y + 19, 0xff172a36);
        g.renderItem(icon(chapter), contentX + 5, y + 1);
        int headingBottom = drawWrapped(g, Component.literal(article.title()).withStyle(ChatFormatting.BOLD),
                contentX + 33, y + 2, contentWidth - 46, TEXT);
        y = Math.max(y + 31, headingBottom + 12);
        g.fill(contentX + 2, y - 5, contentX + contentWidth - 14, y - 4, EDGE);
        for (String paragraph : article.paragraphs()) {
            y = drawWrapped(g, paragraph, contentX + 2, y + 5, contentWidth - 16, TEXT) + 8;
        }
        if (article.chapter() == 0) {
            y += 4;
            g.fill(contentX + 2, y, contentX + contentWidth - 14, y + 2, AMBER);
            y = drawWrapped(g, "WORKSHOP ROUTE", contentX + 2, y + 10, contentWidth - 16, AMBER) + 5;
            y = drawWrapped(g, "Generator > Crusher > Furnace > Ingots", contentX + 2, y, contentWidth - 16, MUTED) + 8;
        }
        if (chapter.planned())
            y = drawWrapped(g, "Future content will arrive through later updates.", contentX + 2, y + 8,
                    contentWidth - 16, MUTED) + 8;
        maxScroll = Math.max(0, y - (contentY - scroll) - contentHeight);
        scroll = Math.min(scroll, maxScroll);
    }

    private int drawWrapped(GuiGraphics g, String text, int x, int y, int lineWidth, int color) {
        return drawWrapped(g, Component.literal(text), x, y, lineWidth, color);
    }

    private int drawWrapped(GuiGraphics g, Component text, int x, int y, int lineWidth, int color) {
        for (FormattedCharSequence line : font.split(text, Math.max(24, lineWidth))) {
            g.drawString(font, line, x, y, color, false);
            y += 12;
        }
        return y;
    }

    private ItemStack icon(FieldGuide.Chapter chapter) {
        return new ItemStack(Technologia.ITEMS.getOrDefault(chapter.icon(), Items.BOOK));
    }

    private void drawScrollbar(GuiGraphics g) {
        if (maxScroll == 0) return;
        int x = contentX + contentWidth - 5;
        int thumb = Math.max(18, contentHeight * contentHeight / (contentHeight + maxScroll));
        int y = contentY + (contentHeight - thumb) * scroll / maxScroll;
        g.fill(x, contentY, x + 3, contentY + contentHeight, EDGE);
        g.fill(x, y, x + 3, y + thumb, CYAN);
    }

    @Override protected void renderLabels(GuiGraphics graphics, int mouseX, int mouseY) {}

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 0 && maxScroll > 0 && inside(mouseX, mouseY, contentX + contentWidth - 9, contentY, 9, contentHeight)) {
            draggingScroll = true;
            scrollTo(mouseY);
            return true;
        }
        if (button == 0 && page < 0 && withinContent(mouseX, mouseY)) {
            int introLines = font.split(Component.literal("Choose a chapter. Build the workshop, then explore what comes next."), contentWidth - 14).size();
            int gridY = contentY - scroll + 18 + introLines * 12 + 12;
            int columns = contentWidth >= 330 ? 3 : 2;
            int cardWidth = (contentWidth - 14 - (columns - 1) * 8) / columns;
            for (int i = 0; i < chapters.size(); i++) {
                if (inside(mouseX, mouseY, contentX + (i % columns) * (cardWidth + 8), gridY + (i / columns) * 65, cardWidth, 57)) {
                    showPage(chapters.get(i).firstPage());
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double horizontal, double vertical) {
        if (withinContent(mouseX, mouseY)) {
            scroll = Math.clamp(scroll - (int) (vertical * 24), 0, maxScroll);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, horizontal, vertical);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
        if (draggingScroll && button == 0) { scrollTo(mouseY); return true; }
        return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (draggingScroll) { draggingScroll = false; return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private void scrollTo(double mouseY) {
        scroll = Math.clamp((int) ((mouseY - contentY) * maxScroll / Math.max(1, contentHeight)), 0, maxScroll);
    }

    @Override public boolean keyPressed(int key, int scanCode, int modifiers) {
        if (key == 266 || key == 267) {
            scroll = Math.clamp(scroll + (key == 266 ? -1 : 1) * Math.max(24, contentHeight - 24), 0, maxScroll);
            return true;
        }
        if (key == 268) { showPage(-1); return true; }
        if (key == 263 && page >= 0) { showPage(page - 1); return true; }
        if (key == 262 && page >= 0 && page < pages.size() - 1) { showPage(page + 1); return true; }
        return super.keyPressed(key, scanCode, modifiers);
    }

    private boolean withinContent(double x, double y) {
        return inside(x, y, contentX, contentY, contentWidth, contentHeight);
    }

    private static boolean inside(double x, double y, int left, int top, int width, int height) {
        return x >= left && x < left + width && y >= top && y < top + height;
    }
}
