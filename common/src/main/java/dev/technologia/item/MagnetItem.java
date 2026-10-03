package dev.technologia.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;

/**
 * Pulls dropped items and experience to its holder while switched on. Use toggles it; the glint
 * shows that it is on. Crouching pauses it, so items can still be dropped on purpose.
 */
public final class MagnetItem extends Item {
    public final int range;
    public MagnetItem(int range, Properties properties) { super(properties.stacksTo(1)); this.range = range; }

    public static boolean isOn(ItemStack stack) { return Boolean.TRUE.equals(stack.get(DataComponents.ENCHANTMENT_GLINT_OVERRIDE)); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (!level.isClientSide) {
            boolean on = !isOn(stack);
            if (on) stack.set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true); else stack.remove(DataComponents.ENCHANTMENT_GLINT_OVERRIDE);
            level.playSound(null, player.blockPosition(), SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.4F, on ? 1.4F : 0.8F);
            player.displayClientMessage(Component.translatable(on ? "message.technologia.magnet_on" : "message.technologia.magnet_off"), true);
        }
        return InteractionResultHolder.sidedSuccess(stack, level.isClientSide);
    }

    @Override public void inventoryTick(ItemStack stack, Level level, Entity entity, int slot, boolean selected) {
        if (level.isClientSide || !(entity instanceof Player player) || !isOn(stack) || player.isSpectator() || player.isShiftKeyDown()
                || (level.getGameTime() & 3) != 0) return;
        AABB box = player.getBoundingBox().inflate(range);
        // Items that cannot be picked up yet are left where they are, so a freshly dropped item stays down.
        for (ItemEntity item : level.getEntitiesOfClass(ItemEntity.class, box, found -> found.isAlive() && !found.hasPickUpDelay()))
            item.setPos(player.getX(), player.getY(), player.getZ());
        for (ExperienceOrb orb : level.getEntitiesOfClass(ExperienceOrb.class, box, Entity::isAlive))
            orb.setPos(player.getX(), player.getY(), player.getZ());
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.technologia.magnet", range).withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable(isOn(stack) ? "tooltip.technologia.magnet_on" : "tooltip.technologia.magnet_off")
                .withStyle(isOn(stack) ? ChatFormatting.AQUA : ChatFormatting.DARK_GRAY));
    }
}
