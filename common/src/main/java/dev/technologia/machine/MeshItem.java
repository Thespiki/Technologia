package dev.technologia.machine;

import dev.technologia.Technologia;
import java.util.List;
import java.util.function.IntUnaryOperator;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

/**
 * A sieve mesh. Every sieve starts with a plain string mesh (level 0, no item); using a better
 * mesh on a sieve installs it and hands back the one it replaces.
 */
public final class MeshItem extends Item {
    /** Item names by level, starting at level 1. */
    public static final List<String> NAMES = List.of("flint_mesh", "iron_mesh", "diamond_mesh");
    public final int level;
    public MeshItem(int level, Properties properties) { super(properties.stacksTo(16)); this.level = level; }

    /** The item for a mesh level, or an empty stack for the built-in string mesh. */
    public static ItemStack stack(int level) {
        return level < 1 || level > NAMES.size() ? ItemStack.EMPTY : new ItemStack(Technologia.ITEMS.get(NAMES.get(level - 1)));
    }

    /** Shared by the Auto Sieve and the Hand Sieve. {@code installer} stores the new level and returns the old one. */
    public static void install(ItemStack held, int current, IntUnaryOperator installer, Player player, Level level, BlockPos pos) {
        if (!(held.getItem() instanceof MeshItem mesh)) return;
        if (mesh.level == current) {
            player.displayClientMessage(Component.translatable("message.technologia.mesh_same"), true);
            return;
        }
        ItemStack previous = stack(installer.applyAsInt(mesh.level));
        if (!player.getAbilities().instabuild) held.shrink(1);
        if (!previous.isEmpty()) Technologia.giveOrDrop(player, previous, pos);
        level.playSound(null, pos, SoundEvents.ITEM_FRAME_ADD_ITEM, SoundSource.BLOCKS, 0.8F, 1.0F);
        player.displayClientMessage(Component.translatable("message.technologia.mesh_installed", mesh.getDescription()), true);
    }

    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        tooltip.add(Component.translatable("tooltip.technologia.mesh").withStyle(ChatFormatting.GRAY));
    }
}
