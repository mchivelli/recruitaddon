package com.yourname.myforgemod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.yourname.myforgemod.integration.RecruitsIntegration;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.coordinates.Vec3Argument;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Commands for controlling recruit movement to specific coordinates
 */
public class RecruitMovementCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("recruits")
            .requires(source -> source.getEntity() instanceof ServerPlayer)
            .then(Commands.literal("move")
                .then(Commands.literal("all")
                    .then(Commands.argument("position", Vec3Argument.vec3())
                        .executes(RecruitMovementCommands::moveAllRecruits)
                        .then(Commands.literal("formation")
                            .then(Commands.literal("line")
                                .executes(context -> moveAllRecruitsWithFormation(context, RecruitsIntegration.FormationType.LINE)))
                            .then(Commands.literal("square")
                                .executes(context -> moveAllRecruitsWithFormation(context, RecruitsIntegration.FormationType.SQUARE)))
                            .then(Commands.literal("circle")
                                .executes(context -> moveAllRecruitsWithFormation(context, RecruitsIntegration.FormationType.CIRCLE)))
                            .then(Commands.literal("wedge")
                                .executes(context -> moveAllRecruitsWithFormation(context, RecruitsIntegration.FormationType.WEDGE))))))
                .then(Commands.literal("group")
                    .then(Commands.argument("groupId", IntegerArgumentType.integer(0))
                        .then(Commands.argument("position", Vec3Argument.vec3())
                            .executes(RecruitMovementCommands::moveGroupRecruits)
                            .then(Commands.literal("formation")
                                .then(Commands.literal("line")
                                    .executes(context -> moveGroupRecruitsWithFormation(context, RecruitsIntegration.FormationType.LINE)))
                                .then(Commands.literal("square")
                                    .executes(context -> moveGroupRecruitsWithFormation(context, RecruitsIntegration.FormationType.SQUARE)))
                                .then(Commands.literal("circle")
                                    .executes(context -> moveGroupRecruitsWithFormation(context, RecruitsIntegration.FormationType.CIRCLE)))
                                .then(Commands.literal("wedge")
                                    .executes(context -> moveGroupRecruitsWithFormation(context, RecruitsIntegration.FormationType.WEDGE))))))))
            .then(Commands.literal("march")
                .then(Commands.literal("all")
                    .then(Commands.argument("position", Vec3Argument.vec3())
                        .then(Commands.literal("line")
                            .executes(context -> marchAllRecruits(context, RecruitsIntegration.FormationType.LINE)))
                        .then(Commands.literal("square")
                            .executes(context -> marchAllRecruits(context, RecruitsIntegration.FormationType.SQUARE)))
                        .then(Commands.literal("circle")
                            .executes(context -> marchAllRecruits(context, RecruitsIntegration.FormationType.CIRCLE)))
                        .then(Commands.literal("wedge")
                            .executes(context -> marchAllRecruits(context, RecruitsIntegration.FormationType.WEDGE)))))
                .then(Commands.literal("group")
                    .then(Commands.argument("groupId", IntegerArgumentType.integer(0))
                        .then(Commands.argument("position", Vec3Argument.vec3())
                            .then(Commands.literal("line")
                                .executes(context -> marchGroupRecruits(context, RecruitsIntegration.FormationType.LINE)))
                            .then(Commands.literal("square")
                                .executes(context -> marchGroupRecruits(context, RecruitsIntegration.FormationType.SQUARE)))
                            .then(Commands.literal("circle")
                                .executes(context -> marchGroupRecruits(context, RecruitsIntegration.FormationType.CIRCLE)))
                            .then(Commands.literal("wedge")
                                .executes(context -> marchGroupRecruits(context, RecruitsIntegration.FormationType.WEDGE)))))))
            .then(Commands.literal("recall")
                .then(Commands.literal("all")
                    .executes(RecruitMovementCommands::recallAllRecruits))
                .then(Commands.literal("group")
                    .then(Commands.argument("groupId", IntegerArgumentType.integer(0))
                        .executes(RecruitMovementCommands::recallGroupRecruits))))
            .then(Commands.literal("hold")
                .then(Commands.literal("all")
                    .executes(RecruitMovementCommands::holdAllRecruits))
                .then(Commands.literal("group")
                    .then(Commands.argument("groupId", IntegerArgumentType.integer(0))
                        .executes(RecruitMovementCommands::holdGroupRecruits))))
            .then(Commands.literal("follow")
                .then(Commands.literal("all")
                    .executes(RecruitMovementCommands::followAllRecruits))
                .then(Commands.literal("group")
                    .then(Commands.argument("groupId", IntegerArgumentType.integer(0))
                        .executes(RecruitMovementCommands::followGroupRecruits))))
            .then(Commands.literal("stop")
                .then(Commands.literal("all")
                    .executes(RecruitMovementCommands::stopAllRecruits))
                .then(Commands.literal("group")
                    .then(Commands.argument("groupId", IntegerArgumentType.integer(0))
                        .executes(RecruitMovementCommands::stopGroupRecruits))))
            .then(Commands.literal("status")
                .executes(RecruitMovementCommands::getRecruitStatus)));
    }
    
    private static int moveAllRecruits(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            Vec3 targetPos = Vec3Argument.getVec3(context, "position");
            
            if (!RecruitsIntegration.isRecruitsLoaded()) {
                context.getSource().sendFailure(Component.literal("Recruits mod is not loaded!"));
                return 0;
            }
            
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(player);
            if (recruits.isEmpty()) {
                context.getSource().sendFailure(Component.literal("No recruits found nearby!"));
                return 0;
            }
            
            RecruitsIntegration.sendRecruitsToCoordinates(recruits, targetPos, false);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "Sent " + recruits.size() + " recruits to position " + 
                String.format("%.1f, %.1f, %.1f", targetPos.x, targetPos.y, targetPos.z)), true);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error moving recruits: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int moveAllRecruitsWithFormation(CommandContext<CommandSourceStack> context, RecruitsIntegration.FormationType formation) {
        try {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            Vec3 targetPos = Vec3Argument.getVec3(context, "position");
            
            if (!RecruitsIntegration.isRecruitsLoaded()) {
                context.getSource().sendFailure(Component.literal("Recruits mod is not loaded!"));
                return 0;
            }
            
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(player);
            if (recruits.isEmpty()) {
                context.getSource().sendFailure(Component.literal("No recruits found nearby!"));
                return 0;
            }
            
            RecruitsIntegration.sendRecruitsToCoordinates(recruits, targetPos, true);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "Sent " + recruits.size() + " recruits to position " + 
                String.format("%.1f, %.1f, %.1f", targetPos.x, targetPos.y, targetPos.z) + 
                " in " + formation.name().toLowerCase() + " formation"), true);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error moving recruits: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int moveGroupRecruits(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            int groupId = IntegerArgumentType.getInteger(context, "groupId");
            Vec3 targetPos = Vec3Argument.getVec3(context, "position");
            
            if (!RecruitsIntegration.isRecruitsLoaded()) {
                context.getSource().sendFailure(Component.literal("Recruits mod is not loaded!"));
                return 0;
            }
            
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruitsInGroup(player, groupId);
            if (recruits.isEmpty()) {
                context.getSource().sendFailure(Component.literal("No recruits found in group " + groupId + "!"));
                return 0;
            }
            
            RecruitsIntegration.sendRecruitsToCoordinates(recruits, targetPos, false);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "Sent " + recruits.size() + " recruits from group " + groupId + " to position " + 
                String.format("%.1f, %.1f, %.1f", targetPos.x, targetPos.y, targetPos.z)), true);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error moving group recruits: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int moveGroupRecruitsWithFormation(CommandContext<CommandSourceStack> context, RecruitsIntegration.FormationType formation) {
        try {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            int groupId = IntegerArgumentType.getInteger(context, "groupId");
            Vec3 targetPos = Vec3Argument.getVec3(context, "position");
            
            if (!RecruitsIntegration.isRecruitsLoaded()) {
                context.getSource().sendFailure(Component.literal("Recruits mod is not loaded!"));
                return 0;
            }
            
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruitsInGroup(player, groupId);
            if (recruits.isEmpty()) {
                context.getSource().sendFailure(Component.literal("No recruits found in group " + groupId + "!"));
                return 0;
            }
            
            RecruitsIntegration.marchRecruitsToPosition(recruits, targetPos, formation);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "Sent " + recruits.size() + " recruits from group " + groupId + " to position " + 
                String.format("%.1f, %.1f, %.1f", targetPos.x, targetPos.y, targetPos.z) + 
                " in " + formation.name().toLowerCase() + " formation"), true);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error moving group recruits: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int marchAllRecruits(CommandContext<CommandSourceStack> context, RecruitsIntegration.FormationType formation) {
        try {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            Vec3 targetPos = Vec3Argument.getVec3(context, "position");
            
            if (!RecruitsIntegration.isRecruitsLoaded()) {
                context.getSource().sendFailure(Component.literal("Recruits mod is not loaded!"));
                return 0;
            }
            
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(player);
            if (recruits.isEmpty()) {
                context.getSource().sendFailure(Component.literal("No recruits found nearby!"));
                return 0;
            }
            
            RecruitsIntegration.marchRecruitsToPosition(recruits, targetPos, formation);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "Marching " + recruits.size() + " recruits to position " + 
                String.format("%.1f, %.1f, %.1f", targetPos.x, targetPos.y, targetPos.z) + 
                " in " + formation.name().toLowerCase() + " formation"), true);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error marching recruits: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int marchGroupRecruits(CommandContext<CommandSourceStack> context, RecruitsIntegration.FormationType formation) {
        try {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            int groupId = IntegerArgumentType.getInteger(context, "groupId");
            Vec3 targetPos = Vec3Argument.getVec3(context, "position");
            
            if (!RecruitsIntegration.isRecruitsLoaded()) {
                context.getSource().sendFailure(Component.literal("Recruits mod is not loaded!"));
                return 0;
            }
            
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruitsInGroup(player, groupId);
            if (recruits.isEmpty()) {
                context.getSource().sendFailure(Component.literal("No recruits found in group " + groupId + "!"));
                return 0;
            }
            
            RecruitsIntegration.marchRecruitsToPosition(recruits, targetPos, formation);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "Marching " + recruits.size() + " recruits from group " + groupId + " to position " + 
                String.format("%.1f, %.1f, %.1f", targetPos.x, targetPos.y, targetPos.z) + 
                " in " + formation.name().toLowerCase() + " formation"), true);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error marching group recruits: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int recallAllRecruits(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            
            if (!RecruitsIntegration.isRecruitsLoaded()) {
                context.getSource().sendFailure(Component.literal("Recruits mod is not loaded!"));
                return 0;
            }
            
            RecruitsIntegration.RecruitCommands.recallAllRecruits(player);
            
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(player);
            context.getSource().sendSuccess(() -> Component.literal(
                "Recalled " + recruits.size() + " recruits to your position"), true);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error recalling recruits: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int recallGroupRecruits(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            int groupId = IntegerArgumentType.getInteger(context, "groupId");
            
            if (!RecruitsIntegration.isRecruitsLoaded()) {
                context.getSource().sendFailure(Component.literal("Recruits mod is not loaded!"));
                return 0;
            }
            
            RecruitsIntegration.RecruitCommands.recallGroup(player, groupId);
            
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruitsInGroup(player, groupId);
            context.getSource().sendSuccess(() -> Component.literal(
                "Recalled " + recruits.size() + " recruits from group " + groupId + " to your position"), true);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error recalling group recruits: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int holdAllRecruits(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            
            if (!RecruitsIntegration.isRecruitsLoaded()) {
                context.getSource().sendFailure(Component.literal("Recruits mod is not loaded!"));
                return 0;
            }
            
            RecruitsIntegration.RecruitCommands.holdAllPositions(player);
            
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(player);
            context.getSource().sendSuccess(() -> Component.literal(
                "Ordered " + recruits.size() + " recruits to hold their positions"), true);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error ordering recruits to hold: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int holdGroupRecruits(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            int groupId = IntegerArgumentType.getInteger(context, "groupId");
            
            if (!RecruitsIntegration.isRecruitsLoaded()) {
                context.getSource().sendFailure(Component.literal("Recruits mod is not loaded!"));
                return 0;
            }
            
            RecruitsIntegration.RecruitCommands.holdGroupPositions(player, groupId);
            
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruitsInGroup(player, groupId);
            context.getSource().sendSuccess(() -> Component.literal(
                "Ordered " + recruits.size() + " recruits from group " + groupId + " to hold their positions"), true);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error ordering group recruits to hold: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int followAllRecruits(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            
            if (!RecruitsIntegration.isRecruitsLoaded()) {
                context.getSource().sendFailure(Component.literal("Recruits mod is not loaded!"));
                return 0;
            }
            
            RecruitsIntegration.RecruitCommands.followAll(player);
            
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(player);
            context.getSource().sendSuccess(() -> Component.literal(
                "Ordered " + recruits.size() + " recruits to follow you"), true);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error ordering recruits to follow: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int followGroupRecruits(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            int groupId = IntegerArgumentType.getInteger(context, "groupId");
            
            if (!RecruitsIntegration.isRecruitsLoaded()) {
                context.getSource().sendFailure(Component.literal("Recruits mod is not loaded!"));
                return 0;
            }
            
            RecruitsIntegration.RecruitCommands.followGroup(player, groupId);
            
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruitsInGroup(player, groupId);
            context.getSource().sendSuccess(() -> Component.literal(
                "Ordered " + recruits.size() + " recruits from group " + groupId + " to follow you"), true);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error ordering group recruits to follow: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int stopAllRecruits(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            
            if (!RecruitsIntegration.isRecruitsLoaded()) {
                context.getSource().sendFailure(Component.literal("Recruits mod is not loaded!"));
                return 0;
            }
            
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(player);
            RecruitsIntegration.stopRecruits(recruits);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "Stopped " + recruits.size() + " recruits"), true);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error stopping recruits: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int stopGroupRecruits(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            int groupId = IntegerArgumentType.getInteger(context, "groupId");
            
            if (!RecruitsIntegration.isRecruitsLoaded()) {
                context.getSource().sendFailure(Component.literal("Recruits mod is not loaded!"));
                return 0;
            }
            
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruitsInGroup(player, groupId);
            RecruitsIntegration.stopRecruits(recruits);
            
            context.getSource().sendSuccess(() -> Component.literal(
                "Stopped " + recruits.size() + " recruits from group " + groupId), true);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error stopping group recruits: " + e.getMessage()));
            return 0;
        }
    }
    
    private static int getRecruitStatus(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = (ServerPlayer) context.getSource().getEntity();
            
            if (!RecruitsIntegration.isRecruitsLoaded()) {
                context.getSource().sendFailure(Component.literal("Recruits mod is not loaded!"));
                return 0;
            }
            
            List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(player);
            if (recruits.isEmpty()) {
                context.getSource().sendSuccess(() -> Component.literal("No recruits found nearby"), false);
                return 0;
            }
            
            final int[] counts = {0, 0, 0, 0}; // following, holding, moving, idle
            
            for (AbstractRecruitEntity recruit : recruits) {
                if (RecruitsIntegration.isRecruitFollowing(recruit)) {
                    counts[0]++;
                } else if (RecruitsIntegration.isRecruitHoldingPosition(recruit)) {
                    counts[1]++;
                } else if (RecruitsIntegration.isRecruitMovingToPosition(recruit)) {
                    counts[2]++;
                } else {
                    counts[3]++;
                }
            }
            
            context.getSource().sendSuccess(() -> Component.literal(
                "Recruit Status - Total: " + recruits.size() + 
                " | Following: " + counts[0] + 
                " | Holding: " + counts[1] + 
                " | Moving: " + counts[2] + 
                " | Idle: " + counts[3]), false);
            
            return recruits.size();
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("Error getting recruit status: " + e.getMessage()));
            return 0;
        }
    }
}
