package dev.technologia.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

/** Teleports the holder to the block they look at, for one ender pearl from their inventory. */
public final class TravelStaffItem extends Item {
    public static final int RANGE = 32, COOLDOWN_TICKS = 20;
    public TravelStaffItem(Properties properties) { super(properties.stacksTo(1)); }

    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide) return InteractionResultHolder.success(stack);
        // Only blocks one can stand against stop the ray: it passes through grass, flowers, torches and water.
        Vec3 eye = player.getEyePosition(), end = eye.add(player.getViewVector(1.0F).scale(RANGE));
        BlockHitResult hit = level.clip(new ClipContext(eye, end, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, player));
        BlockPos spot = hit.getType() == HitResult.Type.BLOCK && !hit.isInside() ? standingSpot(level, hit) : null;
        if (spot == null) return fail(player, stack, "message.technologia.staff_no_target");
        boolean free = player.getAbilities().instabuild;
        if (!free && player.getInventory().clearOrCountMatchingItems(item -> item.is(Items.ENDER_PEARL), 1, player.inventoryMenu.getCraftSlots()) < 1)
            return fail(player, stack, "message.technologia.staff_no_pearl");
        player.inventoryMenu.broadcastChanges();
        level.playSound(null, player.blockPosition(), SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.0F);
        if (player.isPassenger()) player.stopRiding();
        player.teleportTo(spot.getX() + 0.5, spot.getY(), spot.getZ() + 0.5);
        player.resetFallDistance();
        level.playSound(null, spot, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.6F, 1.2F);
        player.getCooldowns().addCooldown(this, COOLDOWN_TICKS);
        return InteractionResultHolder.consume(stack);
    }
    private static InteractionResultHolder<ItemStack> fail(Player player, ItemStack stack, String message) {
        player.displayClientMessage(Component.translatable(message), true);
        return InteractionResultHolder.fail(stack);
    }

    /** Where to stand next to the block that was hit: two free blocks inside the world border, or null. */
    static BlockPos standingSpot(Level level, BlockHitResult hit) {
        BlockPos block = hit.getBlockPos();
        Direction face = hit.getDirection();
        BlockPos[] candidates = face == Direction.DOWN ? new BlockPos[] {block.below(2)}
                : new BlockPos[] {block.relative(face), block.above(), block.relative(face).above()};
        for (BlockPos feet : candidates)
            if (level.getWorldBorder().isWithinBounds(feet) && isFree(level, feet) && isFree(level, feet.above())) return feet;
        return null;
    }
    private static boolean isFree(Level level, BlockPos pos) {
        return level.isInWorldBounds(pos) && level.hasChunkAt(pos) && level.getBlockState(pos).getCollisionShape(level, pos).isEmpty();
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.technologia.travel_staff", RANGE).withStyle(ChatFormatting.GRAY));
    }
}
