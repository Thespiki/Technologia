package dev.technologia.machine;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/** Installs one tier on a placed machine. The machine must already be at the tier below. */
public final class TierKitItem extends Item {
    public final int tier;
    public TierKitItem(int tier, Properties properties) { super(properties); this.tier = tier; }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        MachineTier target = MachineTier.get(tier), previous = MachineTier.get(tier - 1);
        tooltip.add(Component.translatable("tooltip.technologia.tier_kit", previous.name(), target.name()).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.technologia.tier_stats", target.lanes(), percent(target.speed()),
                percent(target.capacity()), percent(target.transfer())).withStyle(ChatFormatting.DARK_AQUA));
    }
    static String percent(double factor) { return Math.round(factor * 100) + "%"; }
}
