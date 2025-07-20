package com.mchivelli.machiavellianminigames.commands;

import com.mchivelli.machiavellianminigames.core.Arena;
import com.mchivelli.machiavellianminigames.core.ArenaManager;
import com.mchivelli.machiavellianminigames.core.Team;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;

import java.util.concurrent.CompletableFuture;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public class TeamCommands {
    
    public static int createTeam(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String arenaName = StringArgumentType.getString(ctx, "arena");
        String teamName = StringArgumentType.getString(ctx, "team");
        String colorName = StringArgumentType.getString(ctx, "color");
        CommandSourceStack source = ctx.getSource();
        
        ArenaManager manager = ArenaManager.get();
        Arena arena = manager.getArena(arenaName);
        
        if (arena == null) {
            source.sendFailure(Component.literal("§cArena " + arenaName + " does not exist!"));
            return 0;
        }
        
        if (arena.getTeam(teamName) != null) {
            source.sendFailure(Component.literal("§cTeam " + teamName + " already exists in arena " + arenaName + "!"));
            return 0;
        }
        
        ChatFormatting color = parseColor(colorName);
        if (color == null) {
            source.sendFailure(Component.literal("§cInvalid color: " + colorName + 
                ". Valid colors: red, blue, green, yellow, purple, aqua, white, gray, black"));
            return 0;
        }
        
        Team team = new Team(teamName, color);
        arena.addTeam(team);
        
        source.sendSuccess(() -> Component.literal("§aCreated team " + team.getFormattedName() + 
            " in arena " + arenaName), false);
        return 1;
    }
    
    public static int addPlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String arenaName = StringArgumentType.getString(ctx, "arena");
        String teamName = StringArgumentType.getString(ctx, "team");
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        CommandSourceStack source = ctx.getSource();
        
        ArenaManager manager = ArenaManager.get();
        Arena arena = manager.getArena(arenaName);
        
        if (arena == null) {
            source.sendFailure(Component.literal("§cArena " + arenaName + " does not exist!"));
            return 0;
        }
        
        Team team = arena.getTeam(teamName);
        if (team == null) {
            source.sendFailure(Component.literal("§cTeam " + teamName + " does not exist in arena " + arenaName + "!"));
            return 0;
        }
        
        // Remove player from other teams in this arena
        for (Team otherTeam : arena.getTeams()) {
            if (otherTeam.hasPlayer(player.getUUID())) {
                otherTeam.removePlayer(player.getUUID());
                source.sendSuccess(() -> Component.literal("§7Removed " + player.getName().getString() + 
                    " from team " + otherTeam.getName()), false);
            }
        }
        
        team.addPlayer(player.getUUID());
        source.sendSuccess(() -> Component.literal("§aAdded " + player.getName().getString() + 
            " to team " + team.getFormattedName()), false);
        
        player.sendSystemMessage(Component.literal("§aYou have been added to team " + team.getFormattedName() + 
            " in arena " + arenaName));
        return 1;
    }
    
    public static int removePlayer(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String arenaName = StringArgumentType.getString(ctx, "arena");
        String teamName = StringArgumentType.getString(ctx, "team");
        ServerPlayer player = EntityArgument.getPlayer(ctx, "player");
        CommandSourceStack source = ctx.getSource();
        
        ArenaManager manager = ArenaManager.get();
        Arena arena = manager.getArena(arenaName);
        
        if (arena == null) {
            source.sendFailure(Component.literal("§cArena " + arenaName + " does not exist!"));
            return 0;
        }
        
        Team team = arena.getTeam(teamName);
        if (team == null) {
            source.sendFailure(Component.literal("§cTeam " + teamName + " does not exist in arena " + arenaName + "!"));
            return 0;
        }
        
        if (!team.hasPlayer(player.getUUID())) {
            source.sendFailure(Component.literal("§c" + player.getName().getString() + 
                " is not on team " + teamName + "!"));
            return 0;
        }
        
        team.removePlayer(player.getUUID());
        source.sendSuccess(() -> Component.literal("§aRemoved " + player.getName().getString() + 
            " from team " + team.getFormattedName()), false);
        
        player.sendSystemMessage(Component.literal("§7You have been removed from team " + team.getFormattedName() + 
            " in arena " + arenaName));
        return 1;
    }
    
    public static int setSpawn(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String arenaName = StringArgumentType.getString(ctx, "arena");
        String teamName = StringArgumentType.getString(ctx, "team");
        BlockPos pos = BlockPosArgument.getBlockPos(ctx, "pos");
        CommandSourceStack source = ctx.getSource();
        
        ArenaManager manager = ArenaManager.get();
        Arena arena = manager.getArena(arenaName);
        
        if (arena == null) {
            source.sendFailure(Component.literal("§cArena " + arenaName + " does not exist!"));
            return 0;
        }
        
        Team team = arena.getTeam(teamName);
        if (team == null) {
            source.sendFailure(Component.literal("§cTeam " + teamName + " does not exist in arena " + arenaName + "!"));
            return 0;
        }
        
        team.setSpawnPoint(pos);
        source.sendSuccess(() -> Component.literal("§aSet spawn point for team " + team.getFormattedName() + 
            " to " + pos.toShortString()), false);
        return 1;
    }
    
    public static int listTeams(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String arenaName = StringArgumentType.getString(ctx, "arena");
        CommandSourceStack source = ctx.getSource();
        
        ArenaManager manager = ArenaManager.get();
        Arena arena = manager.getArena(arenaName);
        
        if (arena == null) {
            source.sendFailure(Component.literal("§cArena " + arenaName + " does not exist!"));
            return 0;
        }
        
        if (arena.getTeams().isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7No teams in arena " + arenaName), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("§6=== Teams in " + arenaName + " ==="), false);
        for (Team team : arena.getTeams()) {
            String spawnStatus = team.getSpawnPoint() != null ? "§a✓" : "§c✗";
            source.sendSuccess(() -> Component.literal(team.getFormattedName() + 
                " (" + team.getPlayerCount() + " players) " + spawnStatus + " spawn"), false);
        }
        return 1;
    }
    
    private static ChatFormatting parseColor(String colorName) {
        return switch (colorName.toLowerCase()) {
            case "red" -> ChatFormatting.RED;
            case "blue" -> ChatFormatting.BLUE;
            case "green" -> ChatFormatting.GREEN;
            case "yellow" -> ChatFormatting.YELLOW;
            case "purple" -> ChatFormatting.DARK_PURPLE;
            case "aqua" -> ChatFormatting.AQUA;
            case "white" -> ChatFormatting.WHITE;
            case "gray" -> ChatFormatting.GRAY;
            case "black" -> ChatFormatting.BLACK;
            default -> null;
        };
    }
    
    /**
     * Provides team name suggestions for command auto-completion
     * @param arenaName The arena to get teams from
     * @return A suggestion provider for team names
     */
    public static SuggestionProvider<CommandSourceStack> suggestTeamNames() {
        return (context, builder) -> {
            String arenaName;
            try {
                // Get the arena name from the command context
                arenaName = StringArgumentType.getString(context, "arena");
            } catch (Exception e) {
                // If we can't get the arena name, just return an empty list
                return CompletableFuture.completedFuture(builder.build());
            }
            
            ArenaManager manager = ArenaManager.get();
            Arena arena = manager.getArena(arenaName);
            
            if (arena != null) {
                // Add all teams from this arena
                for (Team team : arena.getTeams()) {
                    builder.suggest(team.getName());
                }
            }
            
            return CompletableFuture.completedFuture(builder.build());
        };
    }
}
