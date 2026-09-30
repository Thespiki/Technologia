package dev.technologia.guide;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/** Opens the native field guide without client imports on dedicated servers. */
public final class GuideItem extends Item {
    public GuideItem(Properties properties) {
        super(properties.stacksTo(1).component(DataComponents.WRITTEN_BOOK_CONTENT, FieldGuide.content()));
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player instanceof ServerPlayer serverPlayer) open(serverPlayer, hand);
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide());
    }

    public static void open(ServerPlayer player, InteractionHand hand) {
        ItemStack guide = player.getItemInHand(hand);
        if (!(guide.getItem() instanceof GuideItem)) return;
        // Keep old copies' metadata current; reading uses our own chapter interface.
        guide.set(DataComponents.WRITTEN_BOOK_CONTENT, FieldGuide.content());
        player.openMenu(new SimpleMenuProvider((id, inventory, reader) -> new GuideMenu(id, inventory),
                Component.translatableWithFallback("ui.technologia.field_guide", "Technologia Field Guide")));
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context,
                                List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatableWithFallback("tooltip.technologia.field_guide",
                "Use to read the workshop guide").withStyle(ChatFormatting.GRAY));
    }
}
