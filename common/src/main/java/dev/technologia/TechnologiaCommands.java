package dev.technologia;

import com.mojang.brigadier.CommandDispatcher;
import net.minecraft.commands.*;
import net.minecraft.network.chat.Component;

/** Read-only operator diagnostics. Configuration changes require a server restart. */
public final class TechnologiaCommands {
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("technologia").requires(source -> source.hasPermission(2))
                .then(Commands.literal("status").executes(context -> {
                    var b = Technologia.BALANCE;
                    context.getSource().sendSuccess(() -> Component.literal("Technologia alpha | generator " + b.generatorPerTick()
                            + " FE/t | machines " + b.machineEnergyPerTick() + " FE/t | cycle " + b.processingTicks()
                            + " ticks | miner " + (b.minerEnabled() ? "enabled" : "disabled") + ", radius " + b.minerRadius()
                            + ", depth " + b.minerDepth() + ". Config: config/technologia.json (restart required)."), false);
                    return 1;
                })));
    }
    private TechnologiaCommands() {}
}
