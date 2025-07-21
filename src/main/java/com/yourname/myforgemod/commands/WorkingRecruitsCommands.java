package com.yourname.myforgemod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.yourname.myforgemod.ModMain;
import com.yourname.myforgemod.integration.RecruitsIntegration;
import com.yourname.myforgemod.integration.RecruitsIntegration.FormationType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;

import java.util.List;

/**
 * Working implementation of recruit commands that directly uses the official mod's approach
 * This bypasses all problematic integration layers
 */
public class WorkingRecruitsCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("workingcommands")
            .then(Commands.literal("march")
                .then(Commands.argument("groupId", IntegerArgumentType.integer())
                    .then(Commands.argument("x", StringArgumentType.string())
                        .then(Commands.argument("y", StringArgumentType.string())
                            .then(Commands.argument("z", StringArgumentType.string())
                                .executes(WorkingRecruitsCommands::executeMarchDirect)
                            )
                        )
                    )
                )
            )
            .then(Commands.literal("test")
                .executes(WorkingRecruitsCommands::testRecruitDiscovery)
            )
        );
    }
    
    private static int executeMarchDirect(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            int groupId = IntegerArgumentType.getInteger(context, "groupId");
            String xStr = StringArgumentType.getString(context, "x");
            String yStr = StringArgumentType.getString(context, "y");
            String zStr = StringArgumentType.getString(context, "z");
            
            // Parse coordinates
            BlockPos playerPos = player.blockPosition();
            int x = parseCoordinate(xStr, playerPos.getX());
            int y = parseCoordinate(yStr, playerPos.getY());
            int z = parseCoordinate(zStr, playerPos.getZ());
            BlockPos targetPos = new BlockPos(x, y, z);
            
            // DIRECT APPROACH - Use the EXACT same code as the official mod's MessageMovement.executeServerSide()
            List<AbstractRecruitEntity> allRecruits = player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class, 
                player.getBoundingBox().inflate(500) // Fixed: Double.MAX_VALUE breaks entity search
            );
            
            ModMain.LOGGER.info("WORKING COMMAND: Found {} total recruits in world for player {}", 
                allRecruits.size(), player.getName().getString());
            
            // Apply the exact same filter as the official mod: isEffectedByCommand
            allRecruits.removeIf(recruit -> !recruit.isEffectedByCommand(player.getUUID(), groupId));
            
            ModMain.LOGGER.info("WORKING COMMAND: After isEffectedByCommand filter with group {}: {} recruits remain", 
                groupId, allRecruits.size());
            
            // Debug each recruit that passed the filter
            for (AbstractRecruitEntity recruit : allRecruits) {
                ModMain.LOGGER.info("WORKING COMMAND: Recruit {} - Group: {}, Owner: {}, Alive: {}, Listen: {}", 
                    recruit.getName().getString(),
                    recruit.getGroup(),
                    recruit.getOwnerUUID(),
                    recruit.isAlive(),
                    recruit.getListen());
            }
            
            if (allRecruits.isEmpty()) {
                player.sendSystemMessage(Component.literal("§cNo recruits found in group " + groupId + "!").withStyle(ChatFormatting.RED));
                return 0;
            }
            
            // Send successful message
            context.getSource().sendSuccess(() -> 
                Component.literal("§a[WORKING] Sending " + allRecruits.size() + " recruits from group " + groupId + " to march to " + x + ", " + y + ", " + z), false);
            
            // Use the integration method to actually move them
            RecruitsIntegration.sendRecruitsToCoordinates(allRecruits, targetPos, true);
            
            return allRecruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError executing working march command: " + e.getMessage()));
            ModMain.LOGGER.error("Error in working march command", e);
            return 0;
        }
    }
    
    private static int testRecruitDiscovery(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            
            // Test recruit discovery with detailed logging
            List<AbstractRecruitEntity> allRecruits = player.getCommandSenderWorld().getEntitiesOfClass(
                AbstractRecruitEntity.class, 
                player.getBoundingBox().inflate(500) // Fixed: Double.MAX_VALUE breaks entity search
            );
            
            player.sendSystemMessage(Component.literal("§b=== RECRUIT DISCOVERY TEST ==="));
            player.sendSystemMessage(Component.literal("§bFound " + allRecruits.size() + " total recruits in world"));
            
            for (int i = 0; i < allRecruits.size() && i < 10; i++) { // Limit to first 10 for chat
                AbstractRecruitEntity recruit = allRecruits.get(i);
                boolean canCommand0 = recruit.isEffectedByCommand(player.getUUID(), 0); // All groups
                boolean canCommand1 = recruit.isEffectedByCommand(player.getUUID(), 1); // Group 1
                boolean canCommand2 = recruit.isEffectedByCommand(player.getUUID(), 2); // Group 2
                
                player.sendSystemMessage(Component.literal(String.format(
                    "§7Recruit %d: %s (Group: %d, Can command: All=%s, G1=%s, G2=%s)", 
                    i+1, recruit.getName().getString(), recruit.getGroup(), canCommand0, canCommand1, canCommand2
                )));
            }
            
            return 1;
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError in test: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int parseCoordinate(String coordStr, int currentPos) {
        if (coordStr.equals("~")) {
            return currentPos;
        } else if (coordStr.startsWith("~")) {
            try {
                int offset = Integer.parseInt(coordStr.substring(1));
                return currentPos + offset;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid relative coordinate: " + coordStr);
            }
        } else {
            try {
                return Integer.parseInt(coordStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid coordinate: " + coordStr);
            }
        }
    }
}
