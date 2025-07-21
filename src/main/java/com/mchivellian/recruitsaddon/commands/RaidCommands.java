package com.mchivellian.recruitsaddon.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mchivellian.recruitsaddon.raid.RaidManager;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class RaidCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("recruits-raid")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("start")
                .then(Commands.argument("position", BlockPosArgument.blockPos())
                    .then(Commands.argument("groupId", IntegerArgumentType.integer(-1))
                        .then(Commands.argument("raidType", IntegerArgumentType.integer(0, 1))
                            .executes(RaidCommands::startRaid)))))
            .then(Commands.literal("stop")
                .executes(RaidCommands::stopRaid))
            .then(Commands.literal("status")
                .executes(RaidCommands::raidStatus)));
    }
    
    private static int startRaid(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("Only players can start raids"));
                return 0;
            }
            
            BlockPos targetPos = BlockPosArgument.getBlockPos(context, "position");
            int groupId = IntegerArgumentType.getInteger(context, "groupId");
            int raidType = IntegerArgumentType.getInteger(context, "raidType");
            
            boolean success = RaidManager.startRaid(player, targetPos, groupId, raidType);
            
            if (success) {
                String raidTypeName = (raidType == 0) ? "raid" : "assault";
                String groupText = (groupId == -1) ? "all groups" : "group " + groupId;
                source.sendSuccess(() -> Component.literal(
                    "Started " + raidTypeName + " with " + groupText + " to position " + 
                    targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ()), true);
                return 1;
            } else {
                source.sendFailure(Component.literal("Failed to start raid - no valid recruits found"));
                return 0;
            }
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error starting raid: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int stopRaid(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("Only players can stop raids"));
                return 0;
            }
            
            RaidManager.stopRaid(player);
            source.sendSuccess(() -> Component.literal("Raid stopped"), true);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error stopping raid: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int raidStatus(CommandContext<CommandSourceStack> context) {
        try {
            CommandSourceStack source = context.getSource();
            if (!(source.getEntity() instanceof ServerPlayer player)) {
                source.sendFailure(Component.literal("Only players can check raid status"));
                return 0;
            }
            
            // TODO: Implement raid status checking
            source.sendSuccess(() -> Component.literal("Raid status: Not implemented yet"), false);
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error checking raid status: " + e.getMessage()));
            return 0;
        }
    }
}
