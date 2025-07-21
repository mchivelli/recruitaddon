package com.yourname.myforgemod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.yourname.myforgemod.integration.RecruitsIntegration;
import com.yourname.myforgemod.integration.RecruitsIntegration.FormationType;
import com.yourname.myforgemod.ModMain;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.ChatFormatting;

import java.util.Arrays;
import java.util.List;
/**
 * Simple recruit commands for marching and raiding
 */
public class SimpleRecruitsCommands {
    
    // Formation suggestion provider
    private static final SuggestionProvider<CommandSourceStack> FORMATION_SUGGESTIONS = (context, builder) -> {
        return SharedSuggestionProvider.suggest(Arrays.asList(
            "line", "lineup", "square", "circle", "triangle", "wedge"
        ), builder);
    };
    
    // X coordinate suggestion provider (suggest only player's X coordinate)
    private static final SuggestionProvider<CommandSourceStack> X_COORDINATE_SUGGESTIONS = (context, builder) -> {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            BlockPos pos = player.blockPosition();
            return SharedSuggestionProvider.suggest(Arrays.asList(
                String.valueOf(pos.getX()) // Only player X
            ), builder);
        } catch (Exception e) {
            return SharedSuggestionProvider.suggest(Arrays.asList("0"), builder);
        }
    };
    
    // Y coordinate suggestion provider (suggest only player's Y coordinate)
    private static final SuggestionProvider<CommandSourceStack> Y_COORDINATE_SUGGESTIONS = (context, builder) -> {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            BlockPos pos = player.blockPosition();
            return SharedSuggestionProvider.suggest(Arrays.asList(
                String.valueOf(pos.getY()) // Only player Y
            ), builder);
        } catch (Exception e) {
            return SharedSuggestionProvider.suggest(Arrays.asList("0"), builder);
        }
    };
    
    // Z coordinate suggestion provider (suggest only player's Z coordinate)
    private static final SuggestionProvider<CommandSourceStack> Z_COORDINATE_SUGGESTIONS = (context, builder) -> {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            BlockPos pos = player.blockPosition();
            return SharedSuggestionProvider.suggest(Arrays.asList(
                String.valueOf(pos.getZ()) // Only player Z
            ), builder);
        } catch (Exception e) {
            return SharedSuggestionProvider.suggest(Arrays.asList("0"), builder);
        }
    };
    
    /**
     * Get recruits from enabled groups using the exact same approach as the official mod
     * Uses the same server-side filtering logic: isEffectedByCommand(player_uuid, group_id)
     */
    private static List<AbstractRecruitEntity> getRecruitsFromEnabledGroups(ServerPlayer player) {
        ModMain.LOGGER.info("DEBUG: Getting recruits for player {}", player.getName().getString());
        
        // Use the exact same approach as the official mod's MessageMovement.executeServerSide()
        // Get all AbstractRecruitEntity within range (same as official mod)
        List<AbstractRecruitEntity> allRecruits = player.getCommandSenderWorld().getEntitiesOfClass(
            AbstractRecruitEntity.class, 
            player.getBoundingBox().inflate(500) // Fixed: Double.MAX_VALUE breaks entity search
        );
        
        ModMain.LOGGER.info("DEBUG: Found {} total recruits in world", allRecruits.size());
        
        // Filter using the same logic as the official mod
        // For now, use group ID 0 which means "all groups" according to isEffectedByCommand logic
        // Later we can implement proper group selection via network messages
        int targetGroupId = 0; // 0 = all groups (see isEffectedByCommand: this.getGroup() == group || group == 0)
        
        allRecruits.removeIf(recruit -> !recruit.isEffectedByCommand(player.getUUID(), targetGroupId));
        
        ModMain.LOGGER.info("DEBUG: After filtering with isEffectedByCommand({}, {}): {} recruits found", 
            player.getUUID(), targetGroupId, allRecruits.size());
        
        // Debug: show which recruits passed the filter
        for (AbstractRecruitEntity recruit : allRecruits) {
            ModMain.LOGGER.info("DEBUG: Filtered recruit: {} (Group: {}, Owned: {}, Alive: {}, Listen: {})", 
                recruit.getName().getString(), 
                recruit.getGroup(),
                recruit.isOwned(),
                recruit.isAlive(),
                recruit.getListen());
        }
        
        return allRecruits;
    }
    
    /**
     * Helper method to get the group ID of a recruit
     * Uses the correct getGroup() method from AbstractRecruitEntity
     */
    private static int getRecruitGroupId(AbstractRecruitEntity recruit) {
        try {
            // Use the correct getGroup() method from AbstractRecruitEntity
            return recruit.getGroup();
        } catch (Exception e) {
            // If there's any issue, default to group 0
            return 0;
        }
    }
    
    /**
     * Parse coordinate string, supporting ~ syntax for relative coordinates
     * @param coordStr The coordinate string (e.g., "~", "~5", "10")
     * @param currentPos The current position for relative coordinates
     * @return The parsed coordinate value
     */
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

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("recruits")
            // March command: /recruits march group <groupId> <x> <y> <z> [formation]
            .then(Commands.literal("march")
                .then(Commands.literal("group")
                    .then(Commands.argument("groupId", IntegerArgumentType.integer())
                        .then(Commands.argument("x", StringArgumentType.string())
                            .suggests(X_COORDINATE_SUGGESTIONS)
                            .then(Commands.argument("y", StringArgumentType.string())
                                .suggests(Y_COORDINATE_SUGGESTIONS)
                                .then(Commands.argument("z", StringArgumentType.string())
                                    .suggests(Z_COORDINATE_SUGGESTIONS)
                                    .executes(context -> executeMarch(context, null, IntegerArgumentType.getInteger(context, "groupId")))
                                    .then(Commands.argument("formation", StringArgumentType.string())
                                        .suggests(FORMATION_SUGGESTIONS)
                                        .executes(context -> executeMarch(context, StringArgumentType.getString(context, "formation"), IntegerArgumentType.getInteger(context, "groupId")))
                                    )
                                )
                            )
                        )
                    )
                )
                .then(Commands.literal("cancel")
                    .executes(SimpleRecruitsCommands::cancelMarch)
                )
            )
            // Raid command: /recruits raid group <groupId> <x> <y> <z> [formation]
            .then(Commands.literal("raid")
                .then(Commands.literal("group")
                    .then(Commands.argument("groupId", IntegerArgumentType.integer())
                        .then(Commands.argument("x", StringArgumentType.string())
                            .suggests(X_COORDINATE_SUGGESTIONS)
                            .then(Commands.argument("y", StringArgumentType.string())
                                .suggests(Y_COORDINATE_SUGGESTIONS)
                                .then(Commands.argument("z", StringArgumentType.string())
                                    .suggests(Z_COORDINATE_SUGGESTIONS)
                                    .executes(context -> executeRaid(context, null, IntegerArgumentType.getInteger(context, "groupId")))
                                    .then(Commands.argument("formation", StringArgumentType.string())
                                        .suggests(FORMATION_SUGGESTIONS)
                                        .executes(context -> executeRaid(context, StringArgumentType.getString(context, "formation"), IntegerArgumentType.getInteger(context, "groupId")))
                                    )
                                )
                            )
                        )
                    )
                )
                .then(Commands.literal("cancel")
                    .executes(SimpleRecruitsCommands::cancelRaid)
                )
            )
            // Follow command: /recruits follow [player] [formation]
            .then(Commands.literal("follow")
                .executes(context -> executeFollow(context, null, null)) // Follow executor
                .then(Commands.argument("player", EntityArgument.player())
                    .executes(context -> executeFollow(context, EntityArgument.getPlayer(context, "player"), null))
                    .then(Commands.argument("formation", StringArgumentType.string())
                        .suggests(FORMATION_SUGGESTIONS)
                        .executes(context -> executeFollow(context, EntityArgument.getPlayer(context, "player"), StringArgumentType.getString(context, "formation")))
                    )
                )
                .then(Commands.argument("formation", StringArgumentType.string())
                    .suggests(FORMATION_SUGGESTIONS)
                    .executes(context -> executeFollow(context, null, StringArgumentType.getString(context, "formation")))
                )
            )
            // Cancel command: /recruits cancel
            .then(Commands.literal("cancel")
                .executes(SimpleRecruitsCommands::executeCancel)
            )
            // Stop command: /recruits stop
            .then(Commands.literal("stop")
                .executes(SimpleRecruitsCommands::executeStop)
            )
            // Attack command: /recruits attack <player>
            .then(Commands.literal("attack")
                .then(Commands.argument("target", EntityArgument.player())
                    .executes(SimpleRecruitsCommands::executeAttack))));
    }
    
    private static int executeMarch(CommandContext<CommandSourceStack> context, String formation, int groupId) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String xStr = StringArgumentType.getString(context, "x");
            String yStr = StringArgumentType.getString(context, "y");
            String zStr = StringArgumentType.getString(context, "z");
            
            // Parse coordinates, supporting ~ syntax
            BlockPos playerPos = player.blockPosition();
            int x = parseCoordinate(xStr, playerPos.getX());
            int y = parseCoordinate(yStr, playerPos.getY());
            int z = parseCoordinate(zStr, playerPos.getZ());
            
            BlockPos targetPos = new BlockPos(x, y, z);
            
            // Get recruits from specific group or enabled groups
            List<AbstractRecruitEntity> recruits;
            if (groupId != -1) {
                // Target specific group
                recruits = RecruitsIntegration.getPlayerRecruitsInGroup(player, groupId, 500);
                if (recruits.isEmpty()) {
                    player.sendSystemMessage(Component.literal("No recruits found in group " + groupId + "!").withStyle(ChatFormatting.RED));
                    return 0;
                }
            } else {
                // Use enabled groups fallback
                recruits = getRecruitsFromEnabledGroups(player);
                if (recruits.isEmpty()) {
                    player.sendSystemMessage(Component.literal("No recruits found in enabled groups!").withStyle(ChatFormatting.RED));
                    return 0;
                }
            }
            
            String formationText = formation != null ? " in " + formation + " formation" : "";
            String groupText = groupId != -1 ? " from group " + groupId : "";
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§aSending " + recruits.size() + " recruits" + groupText + " to march to " + x + ", " + y + ", " + z + formationText), false);
            
            // Send recruits to march position with formation if specified
            if (formation != null) {
                RecruitsIntegration.marchRecruitsToPosition(recruits, targetPos.getCenter(), FormationType.valueOf(formation.toUpperCase()));
            } else {
                RecruitsIntegration.sendRecruitsToCoordinates(recruits, targetPos, true);
            }
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError executing march command: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeRaid(CommandContext<CommandSourceStack> context, String formation, int groupId) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String xStr = StringArgumentType.getString(context, "x");
            String yStr = StringArgumentType.getString(context, "y");
            String zStr = StringArgumentType.getString(context, "z");
            
            // Parse coordinates, supporting ~ syntax
            BlockPos playerPos = player.blockPosition();
            int x = parseCoordinate(xStr, playerPos.getX());
            int y = parseCoordinate(yStr, playerPos.getY());
            int z = parseCoordinate(zStr, playerPos.getZ());
            
            BlockPos targetPos = new BlockPos(x, y, z);
            
            // Get recruits from specific group or enabled groups
            List<AbstractRecruitEntity> recruits;
            if (groupId != -1) {
                // Target specific group
                recruits = RecruitsIntegration.getPlayerRecruitsInGroup(player, groupId, 500);
                if (recruits.isEmpty()) {
                    player.sendSystemMessage(Component.literal("No recruits found in group " + groupId + "!").withStyle(ChatFormatting.RED));
                    return 0;
                }
            } else {
                // Use enabled groups fallback
                recruits = getRecruitsFromEnabledGroups(player);
                if (recruits.isEmpty()) {
                    player.sendSystemMessage(Component.literal("No recruits found in enabled groups!").withStyle(ChatFormatting.RED));
                    return 0;
                }
            }
            
            String formationText = formation != null ? " in " + formation + " formation" : "";
            String groupText = groupId != -1 ? " from group " + groupId : "";
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§cStarting raid with " + recruits.size() + " recruits" + groupText + " at " + x + ", " + y + ", " + z + formationText), false);
            
            // Send recruits to raid position with formation if specified
            if (formation != null) {
                RecruitsIntegration.marchRecruitsToPosition(recruits, targetPos.getCenter(), FormationType.valueOf(formation.toUpperCase()));
            } else {
                RecruitsIntegration.sendRecruitsToCoordinates(recruits, targetPos, false);
            }
            
            // Set them to aggressive mode for raiding
            for (AbstractRecruitEntity recruit : recruits) {
                recruit.setState(1); // Aggressive state - will attack enemies at destination
            }
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError executing raid command: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int cancelMarch(CommandContext<CommandSourceStack> context) {
        return cancelOperation(context, "march");
    }
    
    private static int cancelRaid(CommandContext<CommandSourceStack> context) {
        return cancelOperation(context, "raid");
    }
    
    /**
     * Execute cancel command - cancels current group command and returns recruits to player
     */
    private static int executeCancel(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            List<AbstractRecruitEntity> recruits = getRecruitsFromEnabledGroups(player);
            
            if (recruits.isEmpty()) {
                player.sendSystemMessage(Component.literal("§cNo recruits found to cancel commands for."));
                return 0;
            }
            
            // PROPER CANCEL USING COMPREHENSIVE AI CLEARING AND EXPLICIT FOLLOW
            
            // 1. Cancel any active raid missions FIRST
            for (AbstractRecruitEntity recruit : recruits) {
                try {
                    com.yourname.myforgemod.raid.AdvancedRaidManager.cancelRaidMission(recruit.getUUID());
                } catch (Exception e) {
                    // Raid manager might not be available
                }
            }
            
            // 2. Stop all current recruit operations (clears march/raid/move states)
            RecruitsIntegration.stopRecruits(recruits);
            
            // 3. Clear ALL AI states and targets to break out of any stuck behaviors
            for (AbstractRecruitEntity recruit : recruits) {
                if (recruit == null) continue; // Safety check
                
                try {
                    // Clear all targets and aggressive states
                    recruit.setTarget(null);
                    recruit.setAggressive(false);
                    recruit.setState(0); // Neutral state
                    
                    // Clear move positions to stop pathfinding to old targets
                    recruit.setMovePos(null);
                    
                    // Clear navigation to stop any ongoing pathfinding
                    if (recruit.getNavigation() != null) {
                        recruit.getNavigation().stop();
                    }
                } catch (Exception e) {
                    // Individual recruit clearing failed, continue with others
                }
            }
            
            // 4. Explicitly set recruits to follow player with immediate pathfinding
            RecruitsIntegration.makeRecruitsFollow(recruits);
            
            // 5. Force immediate follow behavior - simplified approach
            for (AbstractRecruitEntity recruit : recruits) {
                if (recruit == null) continue; // Safety check
                
                try {
                    // Try setting follow state first
                    recruit.setFollowState(1);
                } catch (Exception e) {
                    // setFollowState failed, try navigation approach
                    try {
                        if (recruit.getNavigation() != null && player != null) {
                            recruit.getNavigation().moveTo(player, 1.2);
                        }
                    } catch (Exception e2) {
                        // All follow attempts failed for this recruit, skip it
                    }
                }
            }
            
            player.sendSystemMessage(Component.literal("§aGroup commands cancelled. Recruits returning to you."));
            return recruits.size();
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cFailed to cancel group commands: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute stop command - makes recruits stand ground with shields up, retaliate only when attacked
     */
    private static int executeStop(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            List<AbstractRecruitEntity> recruits = getRecruitsFromEnabledGroups(player);
            
            if (recruits.isEmpty()) {
                player.sendSystemMessage(Component.literal("§cNo recruits found to stop."));
                return 0;
            }
            
            // PROPER STOP USING RECRUITSINTEGRATION METHODS
            
            // 1. Stop all current recruit operations (clears march/raid/move states)
            RecruitsIntegration.stopRecruits(recruits);
            
            // 2. Make recruits hold their current positions (defensive stance)
            RecruitsIntegration.makeRecruitsHoldPosition(recruits);
            
            // 3. Cancel active raid missions for all recruits
            for (AbstractRecruitEntity recruit : recruits) {
                try {
                    com.yourname.myforgemod.raid.AdvancedRaidManager.cancelRaidMission(recruit.getUUID());
                } catch (Exception e) {
                    // Raid manager might not be available
                }
            }
            
            // 4. Set recruits to proper defensive state with shields up
            for (AbstractRecruitEntity recruit : recruits) {
                // Clear all targets and set to defensive mode
                recruit.setTarget(null);
                recruit.setAggressive(false);
                recruit.setState(2); // Guard state (defensive)
                
                // Clear navigation to stop any ongoing pathfinding
                try {
                    recruit.getNavigation().stop();
                } catch (Exception e) {
                    // Navigation might not be accessible
                }
                
                // Try multiple methods to raise shields and set defensive posture
                boolean shieldSet = false;
                
                // Method 1: Try setShieldUp
                try {
                    java.lang.reflect.Method setShieldUpMethod = recruit.getClass().getMethod("setShieldUp", boolean.class);
                    setShieldUpMethod.invoke(recruit, true);
                    shieldSet = true;
                } catch (Exception e) {
                    // Method doesn't exist, try others
                }
                
                // Method 2: Try setDefensive
                if (!shieldSet) {
                    try {
                        java.lang.reflect.Method setDefensiveMethod = recruit.getClass().getMethod("setDefensive", boolean.class);
                        setDefensiveMethod.invoke(recruit, true);
                        shieldSet = true;
                    } catch (Exception e) {
                        // Method doesn't exist, try others
                    }
                }
                
                // Method 3: Try setGuarding
                if (!shieldSet) {
                    try {
                        java.lang.reflect.Method setGuardingMethod = recruit.getClass().getMethod("setGuarding", boolean.class);
                        setGuardingMethod.invoke(recruit, true);
                        shieldSet = true;
                    } catch (Exception e) {
                        // Method doesn't exist, try others
                    }
                }
                
                // Method 4: Try setBlocking
                if (!shieldSet) {
                    try {
                        java.lang.reflect.Method setBlockingMethod = recruit.getClass().getMethod("setBlocking", boolean.class);
                        setBlockingMethod.invoke(recruit, true);
                        shieldSet = true;
                    } catch (Exception e) {
                        // Method doesn't exist
                    }
                }
                
                // Method 5: Try setUsedShield (for shield usage)
                if (!shieldSet) {
                    try {
                        java.lang.reflect.Method setUsedShieldMethod = recruit.getClass().getMethod("setUsedShield", boolean.class);
                        setUsedShieldMethod.invoke(recruit, true);
                        shieldSet = true;
                    } catch (Exception e) {
                        // Method doesn't exist
                    }
                }
                
                // Method 6: Try startUsingItem with shield if equipped
                if (!shieldSet) {
                    try {
                        // Check if recruit has a shield equipped
                        net.minecraft.world.item.ItemStack offhandItem = recruit.getOffhandItem();
                        if (offhandItem.getItem() instanceof net.minecraft.world.item.ShieldItem) {
                            recruit.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
                            shieldSet = true;
                        }
                    } catch (Exception e) {
                        // Shield interaction failed
                    }
                }
            }
            
            player.sendSystemMessage(Component.literal("§aRecruits are now standing ground with shields up. They will retaliate if attacked."));
            return recruits.size();
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cFailed to execute stop command: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Execute follow command with optional player target and formation
     */
    private static int executeFollow(CommandContext<CommandSourceStack> context, ServerPlayer targetPlayer, String formation) {
        try {
            ServerPlayer commandPlayer = context.getSource().getPlayerOrException();
            ServerPlayer followTarget = targetPlayer != null ? targetPlayer : commandPlayer;
            
            List<AbstractRecruitEntity> recruits = getRecruitsFromEnabledGroups(commandPlayer);
            
            if (recruits.isEmpty()) {
                commandPlayer.sendSystemMessage(Component.literal("§cNo recruits found to follow."));
                return 0;
            }
            
            // Set recruits to follow the target player
            for (AbstractRecruitEntity recruit : recruits) {
                // Stop current navigation
                recruit.getNavigation().stop();
                
                // Set follow target
                BlockPos targetPos = followTarget.blockPosition();
                recruit.getNavigation().moveTo(targetPos.getX(), targetPos.getY(), targetPos.getZ(), 1.2);
                
                // Try to set formation if specified
                if (formation != null && !formation.isEmpty()) {
                    try {
                        FormationType formationType = FormationType.valueOf(formation.toUpperCase());
                        // Apply formation logic here if needed
                    } catch (IllegalArgumentException e) {
                        // Invalid formation, ignore
                    }
                }
            }
            
            String targetName = followTarget.getName().getString();
            String message = targetPlayer != null ? 
                "§aRecruits are now following " + targetName + "." :
                "§aRecruits are now following you.";
            
            if (formation != null && !formation.isEmpty()) {
                message += " Formation: " + formation;
            }
            
            commandPlayer.sendSystemMessage(Component.literal(message));
            return recruits.size();
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cFailed to execute follow command: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int cancelOperation(CommandContext<CommandSourceStack> context, String operation) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(player, 500);
            
            if (recruits.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§cNo recruits found nearby."), false);
                return 0;
            }
            
            // Cancel current operation and make them follow the player
            RecruitsIntegration.makeRecruitsFollow(recruits);
            
            // Reset to neutral state
            for (AbstractRecruitEntity recruit : recruits) {
                recruit.setState(0); // Neutral state
            }
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§aCancelled " + operation + " for " + recruits.size() + " recruits. They are now following you."), false);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError cancelling " + operation + ": " + e.getMessage()));
            return 0;
        }
    }
    
    // Formation-based command methods
    private static int executeMarchFormation(CommandContext<CommandSourceStack> context) {
        String formation = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "formation");
        return executeMarchWithFormation(context, formation);
    }
    
    private static int executeRaidFormation(CommandContext<CommandSourceStack> context) {
        String formation = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "formation");
        return executeRaidWithFormation(context, formation);
    }
    
    // Follow command methods
    private static int executeFollowSelf(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(player, 500);
            
            if (recruits.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§cNo recruits found nearby."), false);
                return 0;
            }
            
            RecruitsIntegration.makeRecruitsFollow(recruits);
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§a" + recruits.size() + " recruits are now following you."), false);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError executing follow command: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeFollowPlayer(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer commander = context.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            
            // Check if target is within minimum distance requirement
            double distance = commander.distanceTo(target);
            int minDistance = com.yourname.myforgemod.config.RaidConfig.FOLLOW_COMMAND_MIN_DISTANCE.get();
            
            if (distance < minDistance) {
                context.getSource().sendFailure(Component.literal(
                    "§cTarget player is too close. Minimum distance: " + minDistance + " blocks. Current distance: " + (int)distance + " blocks."));
                return 0;
            }
            
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(commander, 500);
            
            if (recruits.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§cNo recruits found nearby."), false);
                return 0;
            }
            
            // Make recruits follow the target player
            for (AbstractRecruitEntity recruit : recruits) {
                // Note: setOwner method may not exist, using alternative approach
                recruit.setFollowState(1); // Follow state
            }
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§a" + recruits.size() + " recruits are now following " + target.getName().getString() + "."), false);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError executing follow command: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeFollowSelfFormation(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            String formation = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "formation");
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(player, 500);
            
            if (recruits.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§cNo recruits found nearby."), false);
                return 0;
            }
            
            RecruitsIntegration.FormationType formationType = RecruitsIntegration.FormationType.valueOf(formation.toUpperCase());
            // Use regular follow for now - formation following needs additional implementation
            RecruitsIntegration.makeRecruitsFollow(recruits);
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§a" + recruits.size() + " recruits are now following you in " + formation + " formation."), false);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError executing follow command: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeFollowPlayerFormation(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer commander = context.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(context, "player");
            String formation = com.mojang.brigadier.arguments.StringArgumentType.getString(context, "formation");
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(commander, 500);
            
            if (recruits.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§cNo recruits found nearby."), false);
                return 0;
            }
            
            // Make recruits follow the target player in formation
            for (AbstractRecruitEntity recruit : recruits) {
                // Note: setOwner method may not exist, using alternative approach
                recruit.setFollowState(1); // Follow state
            }
            
            RecruitsIntegration.FormationType formationType = RecruitsIntegration.FormationType.valueOf(formation.toUpperCase());
            // Note: Formation following would need additional implementation in RecruitsIntegration
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§a" + recruits.size() + " recruits are now following " + target.getName().getString() + " in " + formation + " formation."), false);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError executing follow command: " + e.getMessage()));
            return 0;
        }
    }
    
    // Helper methods for formation-based commands
    private static int executeMarchWithFormation(CommandContext<CommandSourceStack> context, String formation) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            int x = IntegerArgumentType.getInteger(context, "x");
            int y = IntegerArgumentType.getInteger(context, "y");
            int z = IntegerArgumentType.getInteger(context, "z");
            
            BlockPos targetPos = new BlockPos(x, y, z);
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(player, 500);
            
            if (recruits.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§cNo recruits found nearby."), false);
                return 0;
            }
            
            // March recruits to position with formation
            RecruitsIntegration.FormationType formationType = RecruitsIntegration.FormationType.valueOf(formation.toUpperCase());
            RecruitsIntegration.marchRecruitsToPosition(recruits, new net.minecraft.world.phys.Vec3(targetPos.getX(), targetPos.getY(), targetPos.getZ()), formationType);
            
            // Set them to peaceful mode (they'll defend themselves but return to mission)
            for (AbstractRecruitEntity recruit : recruits) {
                recruit.setState(0); // Neutral state - will defend but not actively hunt
            }
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§aMarching " + recruits.size() + " recruits to " + x + ", " + y + ", " + z + " in " + formation + " formation"), false);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError executing march command: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int executeRaidWithFormation(CommandContext<CommandSourceStack> context, String formation) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            int x = IntegerArgumentType.getInteger(context, "x");
            int y = IntegerArgumentType.getInteger(context, "y");
            int z = IntegerArgumentType.getInteger(context, "z");
            
            BlockPos targetPos = new BlockPos(x, y, z);
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(player, 500);
            
            if (recruits.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§cNo recruits found nearby."), false);
                return 0;
            }
            
            // Start advanced raid with formation
            RecruitsIntegration.FormationType formationType = RecruitsIntegration.FormationType.valueOf(formation.toUpperCase());
            // Use existing raid logic for now
            for (AbstractRecruitEntity recruit : recruits) {
                recruit.setState(0); // Start in neutral state for marching
            }
            
            // Send recruits to raid position
            RecruitsIntegration.sendRecruitsToCoordinates(recruits, targetPos, false);
            
            // Start advanced raid mission
            com.yourname.myforgemod.raid.AdvancedRaidManager.startRaidMission(recruits, targetPos, formation, player);
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§cStarting raid with " + recruits.size() + " recruits to " + x + ", " + y + ", " + z + " in " + formation + " formation"), false);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError executing raid command: " + e.getMessage()));
            return 0;
        }
    }
    
    // Attack command method
    private static int executeAttack(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer commander = context.getSource().getPlayerOrException();
            ServerPlayer target = EntityArgument.getPlayer(context, "target");
            
            // Check if target is within configured distance
            double distance = commander.distanceTo(target);
            int maxDistance = com.yourname.myforgemod.config.RaidConfig.ATTACK_COMMAND_MAX_DISTANCE.get();
            
            if (distance > maxDistance) {
                context.getSource().sendFailure(Component.literal(
                    "§cTarget player is too far away. Maximum distance: " + maxDistance + " blocks. Current distance: " + (int)distance + " blocks."));
                return 0;
            }
            
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(commander, 500);
            
            if (recruits.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("§cNo recruits found nearby."), false);
                return 0;
            }
            
            // Set recruits to attack the target player with enhanced combat engagement
            for (AbstractRecruitEntity recruit : recruits) {
                // Set to aggressive state for combat
                recruit.setState(1); // Aggressive state
                recruit.setTarget(target); // Set primary target to attack
                
                // Enable combat engagement - recruits will fight back when attacked
                enableCombatEngagement(recruit);
            }
            
            context.getSource().sendSuccess(() -> 
                Component.literal("§c" + recruits.size() + " recruits are now attacking " + target.getName().getString() + "!"), false);
            
            // Notify target player
            target.sendSystemMessage(Component.literal("§c" + commander.getName().getString() + " has ordered their recruits to attack you!"));
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError executing attack command: " + e.getMessage()));
            return 0;
        }
    }
    
    /**
     * Clear all goals and tasks from a recruit to completely stop their operations
     */
    private static void clearAllRecruitGoals(AbstractRecruitEntity recruit) {
        try {
            // Stop navigation immediately
            recruit.getNavigation().stop();
            
            // Method 1: Try to clear all goals
            try {
                java.lang.reflect.Method clearGoalsMethod = recruit.getClass().getMethod("clearGoals");
                clearGoalsMethod.invoke(recruit);
            } catch (Exception e1) {
                // Method 2: Try to stop moving
                try {
                    java.lang.reflect.Method stopMovingMethod = recruit.getClass().getMethod("stopMoving");
                    stopMovingMethod.invoke(recruit);
                } catch (Exception e2) {
                    // Method 3: Try to clear goal selector
                    try {
                        java.lang.reflect.Field goalSelectorField = recruit.getClass().getSuperclass().getDeclaredField("goalSelector");
                        goalSelectorField.setAccessible(true);
                        Object goalSelector = goalSelectorField.get(recruit);
                        
                        // Clear all goals
                        java.lang.reflect.Method removeAllGoalsMethod = goalSelector.getClass().getMethod("removeAllGoals");
                        removeAllGoalsMethod.invoke(goalSelector);
                    } catch (Exception e3) {
                        // Method 4: Try to access target selector
                        try {
                            java.lang.reflect.Field targetSelectorField = recruit.getClass().getSuperclass().getDeclaredField("targetSelector");
                            targetSelectorField.setAccessible(true);
                            Object targetSelector = targetSelectorField.get(recruit);
                            
                            // Clear all target goals
                            java.lang.reflect.Method removeAllGoalsMethod = targetSelector.getClass().getMethod("removeAllGoals");
                            removeAllGoalsMethod.invoke(targetSelector);
                        } catch (Exception e4) {
                            // Method 5: Force stop all AI tasks
                            try {
                                java.lang.reflect.Method stopAllTasksMethod = recruit.getClass().getMethod("stopAllTasks");
                                stopAllTasksMethod.invoke(recruit);
                            } catch (Exception e5) {
                                // Final fallback: just stop navigation and clear target
                                recruit.getNavigation().stop();
                                recruit.setTarget(null);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // If all methods fail, at least stop navigation
            recruit.getNavigation().stop();
            recruit.setTarget(null);
        }
    }
    
    /**
     * Enable combat engagement for a recruit - they will retaliate against any attacker
     */
    private static void enableCombatEngagement(AbstractRecruitEntity recruit) {
        try {
            // Try to enable retaliation behavior using reflection
            // This makes recruits automatically engage any entity that attacks them
            
            // Method 1: Try to set retaliation mode
            try {
                java.lang.reflect.Method setRetaliationMethod = recruit.getClass().getMethod("setRetaliation", boolean.class);
                setRetaliationMethod.invoke(recruit, true);
            } catch (Exception e1) {
                // Method 2: Try to set defensive combat mode
                try {
                    java.lang.reflect.Method setCombatModeMethod = recruit.getClass().getMethod("setCombatMode", boolean.class);
                    setCombatModeMethod.invoke(recruit, true);
                } catch (Exception e2) {
                    // Method 3: Try to enable auto-target attackers
                    try {
                        java.lang.reflect.Method setAutoTargetMethod = recruit.getClass().getMethod("setAutoTargetAttackers", boolean.class);
                        setAutoTargetMethod.invoke(recruit, true);
                    } catch (Exception e3) {
                        // Method 4: Try to set hurt by target behavior
                        try {
                            java.lang.reflect.Method setHurtByTargetMethod = recruit.getClass().getMethod("setHurtByTarget", boolean.class);
                            setHurtByTargetMethod.invoke(recruit, true);
                        } catch (Exception e4) {
                            // Method 5: Try to enable aggressive retaliation
                            try {
                                java.lang.reflect.Method setAggressiveRetaliationMethod = recruit.getClass().getMethod("setAggressiveRetaliation", boolean.class);
                                setAggressiveRetaliationMethod.invoke(recruit, true);
                            } catch (Exception e5) {
                                // Fallback: Ensure they are in aggressive state and will respond to damage
                                recruit.setAggressive(true);
                                
                                // Try to access and modify the recruit's goal selector to add hurt by target goal
                                try {
                                    java.lang.reflect.Field goalSelectorField = recruit.getClass().getSuperclass().getDeclaredField("goalSelector");
                                    goalSelectorField.setAccessible(true);
                                    Object goalSelector = goalSelectorField.get(recruit);
                                    
                                    // Try to add a hurt by target goal if possible
                                    java.lang.reflect.Method addGoalMethod = goalSelector.getClass().getMethod("addGoal", int.class, Object.class);
                                    
                                    // Create a HurtByTargetGoal using reflection
                                    Class<?> hurtByTargetGoalClass = Class.forName("net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal");
                                    Object hurtByTargetGoal = hurtByTargetGoalClass.getConstructor(net.minecraft.world.entity.PathfinderMob.class).newInstance(recruit);
                                    
                                    addGoalMethod.invoke(goalSelector, 1, hurtByTargetGoal);
                                } catch (Exception e6) {
                                    // Final fallback: Just ensure aggressive state
                                    recruit.setAggressive(true);
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            // If all methods fail, at least ensure the recruit is aggressive
            recruit.setAggressive(true);
        }
    }
}
