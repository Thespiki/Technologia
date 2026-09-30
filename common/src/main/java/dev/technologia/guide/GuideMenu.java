package dev.technologia.guide;

import dev.technologia.Technologia;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;

/** A read-only document menu: no inventory slots, transfers, rewards or world mutations. */
public final class GuideMenu extends AbstractContainerMenu {
    public GuideMenu(int containerId, Inventory inventory) {
        super(Technologia.GUIDE_MENU, containerId);
    }

    @Override public boolean stillValid(Player player) { return true; }
    @Override public ItemStack quickMoveStack(Player player, int slot) { return ItemStack.EMPTY; }
    @Override public void clicked(int slot, int button, ClickType type, Player player) {}
}
