package com.mchivelli.machiavellianminigames.commands;

import com.mchivelli.machiavellianminigames.MachiavellianMinigames;
import com.mchivelli.machiavellianminigames.core.Arena;
import com.mchivelli.machiavellianminigames.core.ArenaManager;
import com.mchivelli.machiavellianminigames.core.Game;
import com.mchivelli.machiavellianminigames.core.GameManager;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;

public class GameCommands {
    
    public static int startGame(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String arenaName = StringArgumentType.getString(ctx, "arena");
        String gameType = StringArgumentType.getString(ctx, "type");
        CommandSourceStack source = ctx.getSource();
        
        ArenaManager arenaManager = ArenaManager.get();
        Arena arena = arenaManager.getArena(arenaName);
        
        if (arena == null) {
            source.sendFailure(Component.literal("§cArena " + arenaName + " does not exist!"));
            return 0;
        }
        
        if (!arena.isConfigured()) {
            source.sendFailure(Component.literal("§cArena " + arenaName + " is not properly configured!"));
            return 0;
        }
        
        GameManager gameManager = MachiavellianMinigames.gameManager;
        
        // Check if arena is available
        if (!gameManager.isArenaAvailable(arenaName, arenaManager)) {
            source.sendFailure(Component.literal("§cArena " + arenaName + " is currently unavailable or has an active game!"));
            return 0;
        }
        
        // Validate game type
        if (!gameManager.isGameTypeSupported(gameType)) {
            source.sendFailure(Component.literal("§cGame type '" + gameType + "' is not supported!"));
            source.sendSuccess(() -> Component.literal("§eAvailable game types: " + String.join(", ", gameManager.getSupportedGameTypes())), false);
            return 0;
        }
        
        // Check if all teams have at least one player
        boolean hasPlayers = false;
        for (var team : arena.getTeams()) {
            if (team.getPlayerCount() > 0) {
                hasPlayers = true;
                break;
            }
        }
        
        if (!hasPlayers) {
            source.sendFailure(Component.literal("§cNo players assigned to teams in arena " + arenaName + "!"));
            return 0;
        }
        
        boolean started = gameManager.startGame(arena, gameType);
        
        if (started) {
            source.sendSuccess(() -> Component.literal("§aStarted " + gameType + " game in arena " + arenaName), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§cFailed to start game. Check console for details."));
            return 0;
        }
    }
    
    public static int endGame(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String arenaName = StringArgumentType.getString(ctx, "arena");
        CommandSourceStack source = ctx.getSource();
        
        ArenaManager arenaManager = ArenaManager.get();
        Arena arena = arenaManager.getArena(arenaName);
        
        if (arena == null) {
            source.sendFailure(Component.literal("§cArena " + arenaName + " does not exist!"));
            return 0;
        }
        
        if (arena.getCurrentGame() == null) {
            source.sendFailure(Component.literal("§cNo active game in arena " + arenaName + "!"));
            return 0;
        }
        
        GameManager gameManager = MachiavellianMinigames.gameManager;
        boolean ended = gameManager.endGame(arenaName);
        
        if (ended) {
            source.sendSuccess(() -> Component.literal("§aEnded game in arena " + arenaName), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§cFailed to end game."));
            return 0;
        }
    }
    
    public static int gameStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        GameManager gameManager = MachiavellianMinigames.gameManager;
        
        source.sendSuccess(() -> Component.literal("§6=== Available Game Types ==="), false);
        for (String gameType : gameManager.getSupportedGameTypes()) {
            source.sendSuccess(() -> Component.literal("§e- " + gameType), false);
        }
        
        source.sendSuccess(() -> Component.literal("§6=== Active Games ==="), false);
        
        if (gameManager.getActiveGames().isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7No active games."), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("§6=== Active Games ==="), false);
        for (Game game : gameManager.getActiveGames()) {
            Arena arena = game.getArena();
            String status = "§f" + arena.getName() + " - " + game.getGameType() + 
                           " (§a" + game.getState() + "§f)";
            source.sendSuccess(() -> Component.literal(status), false);
            
            // Show player count
            int playerCount = game.getPlayerData().size();
            source.sendSuccess(() -> Component.literal("  §7Players: " + playerCount), false);
            
            // Show time remaining if active
            if (game.getState() == Game.GameState.ACTIVE) {
                long timeRemaining = (game.getEndTime() - System.currentTimeMillis()) / 1000;
                if (timeRemaining > 0) {
                    source.sendSuccess(() -> Component.literal("  §7Time remaining: " + formatTime(timeRemaining)), false);
                }
                
                source.sendSuccess(() -> Component.literal("§aGame in " + arena.getName() + ": " + game.getGameType() 
                    + ", State: " + game.getState() + ", Time left: " + formatTime(timeRemaining)), false);
            }
        }
        return 1;
    }
    
    private static String formatTime(long seconds) {
        long minutes = seconds / 60;
        long remainingSeconds = seconds % 60;
        return String.format("%d:%02d", minutes, remainingSeconds);
    }
}
