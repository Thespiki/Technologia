package dev.technologia.machine;

import dev.technologia.Technologia;
import dev.technologia.logistics.ItemTransferBlock;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Block;

/** Rotates installed equipment without breaking inventories; crouching inspects it. */
public final class WrenchItem extends Item {
    public WrenchItem(Properties properties) { super(properties.stacksTo(1)); }
    @Override public InteractionResult useOn(UseOnContext context) {
        var player = context.getPlayer(); var level = context.getLevel(); var pos = context.getClickedPos();
        if (player == null || !player.mayBuild() || !level.mayInteract(player, pos)) return InteractionResult.FAIL;
        var state = level.getBlockState(pos);
        if (!(state.getBlock() instanceof MachineBlock) && !(state.getBlock() instanceof ItemTransferBlock)) return InteractionResult.PASS;
        if (level.isClientSide) return InteractionResult.SUCCESS;
        if (level.getBlockEntity(pos) instanceof MachineBlockEntity machine) {
            if (!machine.mayConfigure(player)) return InteractionResult.FAIL;
            if (player.isShiftKeyDown()) {
                player.displayClientMessage(Component.translatable("message.technologia.inspect", machine.getDisplayName(), machine.energy.stored(), machine.kind.capacity), true);
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
}
