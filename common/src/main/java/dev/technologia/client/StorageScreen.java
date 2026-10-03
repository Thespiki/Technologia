package dev.technologia.client;

import dev.technologia.storage.StorageMenu;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/** Component-aware item catalogue over ordinary server-owned inventory slots. */
public final class StorageScreen extends AbstractContainerScreen<StorageMenu> {
    private static final int COLUMNS = 6;
    private static final int PAGE_SIZE = 12;
    private static final int GRID_X = 14, GRID_Y = 51, CELL_WIDTH = 45, CELL_HEIGHT = 31;
    private EditBox search;
    private Button sort, previous, next, deposit;
    private List<StorageMenu.Entry> filtered = List.of();
    private int page;
    private int totalTypes;
    private boolean sortByCount;
    private boolean cataloguePressed;
    private String query = "";
    private long shownFingerprint = Long.MIN_VALUE;
    private String shownQuery;
    private boolean shownSort;
    private int totalItems, occupiedSlots;

    public StorageScreen(StorageMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageWidth = 300;
        imageHeight = 238;
        inventoryLabelX = 69;
        inventoryLabelY = 149;
    }

    @Override protected void init() {
        super.init();
        search = new EditBox(font, leftPos + 14, topPos + 29, 174, 17, text("search", "Search items"));
        search.setMaxLength(96);
        search.setHint(text("search_hint", "Search name or mod:item"));
        search.setValue(query);
        search.setResponder(value -> { query = value; page = 0; refreshEntries(); });
        addRenderableWidget(search);
        sort = addRenderableWidget(new PanelButton(leftPos + 194, topPos + 27, 92, 20, sortLabel(), button -> {
            sortByCount = !sortByCount;
            sort.setMessage(sortLabel());
            page = 0;
            refreshEntries();
        }));
        previous = addRenderableWidget(new PanelButton(leftPos + 14, topPos + 115, 20, 18, Component.literal("<"), button -> changePage(-1)));
        previous.setTooltip(Tooltip.create(text("previous", "Previous page")));
        next = addRenderableWidget(new PanelButton(leftPos + 94, topPos + 115, 20, 18, Component.literal(">"), button -> changePage(1)));
        next.setTooltip(Tooltip.create(text("next", "Next page")));
        deposit = addRenderableWidget(new PanelButton(leftPos + 194, topPos + 115, 92, 18, text("deposit", "Deposit held"), button -> send(StorageMenu.DEPOSIT_BUTTON)));
        deposit.setTooltip(Tooltip.create(text("deposit_hint", "Store the stack on your cursor. Shift-click your inventory to deposit directly.")));
        refreshEntries();
        setInitialFocus(search);
    }

    /** Rebuilds the catalogue only when the stored items, the search text or the order changed. */
    private void refreshIfChanged() {
        long fingerprint = menu.fingerprint();
        if (fingerprint != shownFingerprint || !query.equals(shownQuery) || sortByCount != shownSort) refreshEntries();
        if (deposit != null) deposit.active = !menu.getCarried().isEmpty();
    }

    private void refreshEntries() {
        shownFingerprint = menu.fingerprint(); shownQuery = query; shownSort = sortByCount;
        totalItems = menu.totalItems(); occupiedSlots = menu.occupiedSlots();
        List<StorageMenu.Entry> all = menu.entries();
        totalTypes = all.size();
        String[] terms = query.trim().toLowerCase(Locale.ROOT).split("\\s+");
        Comparator<StorageMenu.Entry> byName = Comparator
                .comparing((StorageMenu.Entry entry) -> entry.stack().getHoverName().getString().toLowerCase(Locale.ROOT))
                .thenComparing(entry -> BuiltInRegistries.ITEM.getKey(entry.stack().getItem()).toString())
                .thenComparingInt(StorageMenu.Entry::sourceSlot);
        filtered = all.stream().filter(entry -> {
            String haystack = (entry.stack().getHoverName().getString() + " "
                    + BuiltInRegistries.ITEM.getKey(entry.stack().getItem())).toLowerCase(Locale.ROOT);
            for (String term : terms) if (!haystack.contains(term)) return false;
            return true;
        }).sorted(sortByCount ? Comparator.comparingInt(StorageMenu.Entry::count).reversed().thenComparing(byName) : byName).toList();
        page = Math.clamp(page, 0, pageCount() - 1);
        if (previous != null) previous.active = page > 0;
        if (next != null) next.active = page < pageCount() - 1;
        if (deposit != null) deposit.active = !menu.getCarried().isEmpty();
    }

    private int pageCount() { return Math.max(1, (filtered.size() + PAGE_SIZE - 1) / PAGE_SIZE); }
    private void changePage(int delta) { page = Math.clamp(page + delta, 0, pageCount() - 1); refreshEntries(); }
    private Component sortLabel() { return sortByCount ? text("sort_count", "Count (high)") : text("sort_name", "Name (A-Z)"); }
    private void send(int id) {
        if (minecraft != null && minecraft.gameMode != null) minecraft.gameMode.handleInventoryButtonClick(menu.containerId, id);
    }

    @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        refreshIfChanged();
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
        StorageMenu.Entry hovered = entryAt(mouseX, mouseY);
        if (hovered != null && menu.getCarried().isEmpty()) {
            List<Component> tooltip = new ArrayList<>(getTooltipFromContainerItem(hovered.stack()));
            tooltip.add(text("stored", "%s stored", hovered.count()).withStyle(ChatFormatting.AQUA));
            tooltip.add(text("withdraw_hint", "Left: stack | Right: one | Shift: to inventory").withStyle(ChatFormatting.GRAY));
            graphics.renderTooltip(font, tooltip, hovered.stack().getTooltipImage(), mouseX, mouseY);
        }
    }

    @Override protected void renderBg(GuiGraphics g, float partialTick, int mouseX, int mouseY) {
        int x = leftPos, y = topPos;
        g.fill(x, y, x + imageWidth, y + imageHeight, 0xff0c121b);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + imageHeight - 1, 0xff1b2735);
        g.fill(x + 1, y + 1, x + imageWidth - 1, y + 4, 0xff55d9d0);
        if (filtered.isEmpty()) {
            int panelX = x + GRID_X, panelY = y + GRID_Y;
            int panelWidth = COLUMNS * CELL_WIDTH - 2, panelHeight = 2 * CELL_HEIGHT - 2;
            g.fill(panelX, panelY, panelX + panelWidth, panelY + panelHeight, 0xff415267);
            g.fill(panelX + 1, panelY + 1, panelX + panelWidth - 1, panelY + panelHeight - 1, 0xff101923);
            int center = panelX + panelWidth / 2;
            g.fill(center - 10, panelY + 10, center + 10, panelY + 12, 0xff55d9d0);
        } else for (int index = 0; index < PAGE_SIZE; index++) {
            int cx = x + GRID_X + (index % COLUMNS) * CELL_WIDTH;
            int cy = y + GRID_Y + (index / COLUMNS) * CELL_HEIGHT;
            boolean hovered = mouseX >= cx && mouseX < cx + CELL_WIDTH - 2 && mouseY >= cy && mouseY < cy + CELL_HEIGHT - 2;
            g.fill(cx, cy, cx + CELL_WIDTH - 2, cy + CELL_HEIGHT - 2, hovered ? 0xff55d9d0 : 0xff415267);
            g.fill(cx + 1, cy + 1, cx + CELL_WIDTH - 3, cy + CELL_HEIGHT - 3, 0xff101923);
            int entryIndex = page * PAGE_SIZE + index;
            if (entryIndex >= filtered.size()) continue;
            StorageMenu.Entry entry = filtered.get(entryIndex);
            ItemStack stack = entry.stack();
            g.renderItem(stack, cx + 4, cy + 3);
            g.renderItemDecorations(font, stack, cx + 4, cy + 3, "");
            String count = Integer.toString(entry.count());
            g.drawString(font, count, cx + CELL_WIDTH - 5 - font.width(count), cy + 19, 0xffedf4fc, false);
        }
        for (var slot : menu.slots) {
            if (!slot.isActive()) continue;
            int sx = x + slot.x - 1, sy = y + slot.y - 1;
            g.fill(sx, sy, sx + 18, sy + 18, 0xff415267);
            g.fill(sx + 1, sy + 1, sx + 17, sy + 17, 0xff101923);
        }
    }

    @Override protected void renderLabels(GuiGraphics g, int mouseX, int mouseY) {
        g.drawString(font, text("title", "Nexus storage"), 14, 12, 0xffedf4fc, false);
        Component pageLabel = text("page", "%s / %s", page + 1, pageCount());
        g.drawCenteredString(font, pageLabel, 64, 120, 0xffedf4fc);
        Component status = text("status", "%s/%s types | %s items | %s/%s slots", filtered.size(), totalTypes, totalItems, occupiedSlots, menu.capacity());
        g.drawString(font, status, 14, 138, 0xffaebed1, false);
        g.drawString(font, playerInventoryTitle, inventoryLabelX, inventoryLabelY, 0xffaebed1, false);
        if (filtered.isEmpty()) {
            Component empty = query.isBlank() ? text("empty", "Storage is empty") : text("no_matches", "No matching items");
            Component hint = query.isBlank()
                    ? text("empty_hint", "Shift-click inventory items to deposit.")
                    : text("no_matches_hint", "Try another name or mod:item.");
            int center = GRID_X + (COLUMNS * CELL_WIDTH - 2) / 2;
            g.drawCenteredString(font, empty, center, GRID_Y + 20, 0xffedf4fc);
            var lines = font.split(hint, COLUMNS * CELL_WIDTH - 26);
            for (int i = 0; i < Math.min(2, lines.size()); i++)
                g.drawCenteredString(font, lines.get(i), center, GRID_Y + 35 + i * font.lineHeight, 0xffaebed1);
        }
    }

    private StorageMenu.Entry entryAt(double mouseX, double mouseY) {
        double x = mouseX - leftPos - GRID_X, y = mouseY - topPos - GRID_Y;
        if (x < 0 || y < 0 || x >= COLUMNS * CELL_WIDTH || y >= 2 * CELL_HEIGHT) return null;
        int col = (int) x / CELL_WIDTH, row = (int) y / CELL_HEIGHT;
        if ((int) x % CELL_WIDTH >= CELL_WIDTH - 2 || (int) y % CELL_HEIGHT >= CELL_HEIGHT - 2) return null;
        int index = page * PAGE_SIZE + row * COLUMNS + col;
        return index < filtered.size() ? filtered.get(index) : null;
    }

    @Override public boolean mouseClicked(double mouseX, double mouseY, int button) {
        StorageMenu.Entry entry = entryAt(mouseX, mouseY);
        if (entry != null && (button == 0 || button == 1)) {
            cataloguePressed = true;
            search.setFocused(false);
            setFocused(null);
            StorageMenu.Action action = hasShiftDown() ? StorageMenu.Action.TO_INVENTORY
                    : button == 1 ? StorageMenu.Action.ONE : StorageMenu.Action.STACK;
            send(StorageMenu.actionButton(entry, action));
            return true;
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (cataloguePressed) { cataloguePressed = false; return true; }
        return super.mouseReleased(mouseX, mouseY, button);
    }

    @Override public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY) {
        return cataloguePressed || super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override public boolean mouseScrolled(double mouseX, double mouseY, double deltaX, double deltaY) {
        if (mouseX >= leftPos + GRID_X && mouseX < leftPos + GRID_X + COLUMNS * CELL_WIDTH
                && mouseY >= topPos + GRID_Y && mouseY < topPos + GRID_Y + 2 * CELL_HEIGHT && deltaY != 0) {
            changePage(deltaY < 0 ? 1 : -1);
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, deltaX, deltaY);
    }

    @Override public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (search.isFocused() && keyCode != GLFW.GLFW_KEY_ESCAPE && keyCode != GLFW.GLFW_KEY_TAB) {
            // Typing an inventory/hotbar shortcut in search must never move an item or close the menu.
            search.keyPressed(keyCode, scanCode, modifiers);
            return true;
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    private static net.minecraft.network.chat.MutableComponent text(String key, String fallback, Object... args) {
        return Component.translatableWithFallback("ui.technologia.storage." + key, fallback, args);
    }
}
