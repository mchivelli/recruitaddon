package com.yourname.myforgemod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import com.yourname.myforgemod.client.ClientSetup;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;

public class MarchCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("march")
            .requires(source -> source.getEntity() instanceof ServerPlayer)
            .then(Commands.literal("gui")
                .executes(MarchCommands::openMarchGui))
            .then(Commands.literal("help")
                .executes(MarchCommands::showHelp)));
    }
    
    private static int openMarchGui(CommandContext<CommandSourceStack> context) {
        try {
            // Open GUI on client side
            DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> {
                ClientSetup.openRecruitMovementScreen();
            });
            
            context.getSource().sendSuccess(() -> Component.literal("Opening recruit movement GUI..."), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error opening GUI: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int showHelp(CommandContext<CommandSourceStack> context) {
        context.getSource().sendSuccess(() -> Component.literal(
            "March Commands:\n" +
            "/march gui - Open recruit movement GUI\n" +
            "/recruits move all <x> <y> <z> - Move all recruits to coordinates\n" +
            "/recruits march all <x> <y> <z> <formation> - March all recruits in formation\n" +
            "/recruits recall all - Recall all recruits to your position\n" +
            "/recruits status - Get recruit status"
        ), false);
        return 1;
    }
}
