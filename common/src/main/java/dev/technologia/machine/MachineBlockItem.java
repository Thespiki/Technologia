package dev.technologia.machine;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.block.Block;

/** A machine in item form remembers its tier and stored energy. */
public final class MachineBlockItem extends BlockItem {
    public MachineBlockItem(Block block, Properties properties) { super(block, properties); }

    private static CompoundTag data(ItemStack stack) {
        return stack.getOrDefault(DataComponents.BLOCK_ENTITY_DATA, CustomData.EMPTY).getUnsafe();
    }
    public static MachineTier tier(ItemStack stack) { return MachineTier.get(data(stack).getInt("Tier")); }
    public static int energy(ItemStack stack) { return Math.max(0, data(stack).getInt("Energy")); }

    @Override public Component getName(ItemStack stack) {
        MachineTier tier = tier(stack);
        return tier.isBase() ? super.getName(stack) : Component.translatable("item.technologia.tiered", super.getName(stack), tier.name());
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        super.appendHoverText(stack, context, tooltip, flag);
        if (!(getBlock() instanceof MachineBlock machine) || !machine.kind.isTierable()) return;
        MachineTier tier = tier(stack);
        tooltip.add(Component.translatable("tooltip.technologia.machine_tier", tier.name()).withStyle(ChatFormatting.DARK_AQUA));
        tooltip.add(Component.translatable("tooltip.technologia.machine_energy", energy(stack),
                MachineBlockEntity.scaledCapacity(machine.kind, tier)).withStyle(ChatFormatting.GRAY));
    }
}
