package dev.technologia.machine;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;

/** A testing tool with no recipe: sets a machine to the top tier, or back to the first when crouching. Never used up. */
public final class CreativeKitItem extends Item {
    public CreativeKitItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override public boolean isFoil(ItemStack stack) { return true; }
    /** Handled here and not by the block: the game skips a block's own use while the player crouches with an item in hand. */
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer(); var level = context.getLevel(); var pos = context.getClickedPos();
        if (player == null || !(level.getBlockState(pos).getBlock() instanceof MachineBlock block)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (!(level.getBlockEntity(pos) instanceof MachineBlockEntity machine) || !player.mayBuild() || !machine.mayConfigure(player)) return InteractionResult.FAIL;
        boolean tierable = block.kind.isTierable(), changed = tierable && machine.setTier(player.isShiftKeyDown() ? 0 : MachineTier.count() - 1);
        player.displayClientMessage(Component.translatable(!tierable ? "message.technologia.kit_no_tier"
                : changed ? "message.technologia.kit_installed" : "message.technologia.kit_already", machine.tier().name()), true);
        return InteractionResult.CONSUME;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.technologia.creative_kit").withStyle(ChatFormatting.LIGHT_PURPLE));
    }
}
