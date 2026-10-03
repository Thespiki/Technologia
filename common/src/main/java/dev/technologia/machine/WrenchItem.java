package dev.technologia.machine;

import dev.technologia.Technologia;
import dev.technologia.logistics.ItemTransferBlock;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/** Use turns installed equipment; crouch-use dismantles a machine, keeping its tier and energy. */
public final class WrenchItem extends Item {
    public WrenchItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer(); var level = context.getLevel(); var pos = context.getClickedPos();
        if (player == null || !player.mayBuild() || !level.mayInteract(player, pos)) return InteractionResult.FAIL;
        var state = level.getBlockState(pos);
        boolean machineBlock = state.getBlock() instanceof MachineBlock;
        if (!machineBlock && !(state.getBlock() instanceof ItemTransferBlock) && !(state.getBlock() instanceof DirectionalFactoryBlock)
                && !(state.getBlock() instanceof dev.technologia.device.VectorPlateBlock)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            if (!machine.mayConfigure(player)) return InteractionResult.FAIL;
            if (player.isShiftKeyDown()) {
                // Dismantling removes a block, so it obeys the same protection hook as the miner.
                if (!(player instanceof ServerPlayer server) || !Technologia.BREAK_PERMISSION.test(server, pos)) return InteractionResult.FAIL;
                ItemStack item = new ItemStack(state.getBlock());
                machine.saveToItem(item);
                // Removing the block drops the inventory through MachineBlock.onRemove.
                if (!level.removeBlock(pos, false)) return InteractionResult.FAIL;
                Technologia.giveOrDrop(player, item, pos);
                level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, 1.4F);
                player.displayClientMessage(Component.translatable("message.technologia.dismantled"), true);
                return InteractionResult.CONSUME;
            }
        }
        if (state.hasProperty(ItemTransferBlock.FACING)) {
            Direction next = Direction.values()[(state.getValue(ItemTransferBlock.FACING).ordinal() + 1) % Direction.values().length];
            state = state.setValue(ItemTransferBlock.FACING, next);
        } else state = state.setValue(MachineBlock.FACING, state.getValue(MachineBlock.FACING).getClockWise());
        level.setBlock(pos, state, Block.UPDATE_ALL);
        player.displayClientMessage(Component.translatable("message.technologia.rotated"), true);
        return InteractionResult.CONSUME;
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.technologia.wrench").withStyle(ChatFormatting.GRAY));
    }
}
