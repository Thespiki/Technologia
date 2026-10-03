package dev.technologia;

import com.mojang.brigadier.CommandDispatcher;
import dev.technologia.guide.GuideItem;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Public onboarding and read-only operator diagnostics. */
public final class TechnologiaCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("technologia")
                .then(Commands.literal("guide").executes(context -> {
                    ServerPlayer player = context.getSource().getPlayerOrException();
                    for (InteractionHand hand : InteractionHand.values()) {
                        if (player.getItemInHand(hand).getItem() instanceof GuideItem) {
                            GuideItem.open(player, hand);
                            return 1;
                        }
                    }
                    for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                        if (player.getInventory().getItem(slot).getItem() instanceof GuideItem) {
                            context.getSource().sendSuccess(() -> Component.translatableWithFallback(
                                    "message.technologia.guide_owned",
                                    "Your Field Guide is in your inventory. Hold it and use it to read."), false);
                            return 1;
                        }
                    }
                    // Checking first also avoids creative-mode Inventory.add silently deleting it.
                    if (player.getInventory().getFreeSlot() < 0) {
                        context.getSource().sendFailure(Component.translatableWithFallback(
                                "message.technologia.guide_full",
                                "Make one inventory slot free, then use /technologia guide again."));
                        return 0;
                    }
                    ItemStack guide = new ItemStack(Technologia.ITEMS.get("field_guide"));
                    if (!player.getInventory().add(guide)) return 0;
                    player.inventoryMenu.broadcastChanges();
                    context.getSource().sendSuccess(() -> Component.translatableWithFallback(
                            "message.technologia.guide_received",
                            "Field Guide added. Hold it and use it to read. No operator permissions needed."), false);
                    return 1;
                }))
                .then(Commands.literal("status").requires(source -> source.hasPermission(2)).executes(context -> {
                    var b = Technologia.BALANCE;
                    context.getSource().sendSuccess(() -> Component.literal("Technologia alpha | generator " + b.generatorPerTick()
                            + " FE/t | electric furnace " + b.machineEnergyPerTick() + " FE/t, " + b.processingTicks()
                            + " ticks | " + dev.technologia.machine.MachineTier.count() + " machine tiers | miner " + (b.minerEnabled() ? "enabled" : "disabled") + ", radius " + b.minerRadius()
                            + ", depth " + b.minerDepth() + ". Config: config/technologia.json (restart required)."), false);
                    return 1;
                })));
    }
    private TechnologiaCommands() {}
}
