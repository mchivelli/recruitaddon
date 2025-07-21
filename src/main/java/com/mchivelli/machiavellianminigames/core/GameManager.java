package com.mchivelli.machiavellianminigames.core;

import com.mchivelli.machiavellianminigames.MachiavellianMinigames;
import com.mchivelli.machiavellianminigames.minigames.StormTheFrontGame;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class GameManager {
    private final Map<String, Game> activeGames = new ConcurrentHashMap<>();
    private final Set<String> supportedGameTypes = new HashSet<>();
    
    public GameManager() {
        // Register supported game types
        supportedGameTypes.add("storm");
        supportedGameTypes.add("stormthefront");
        // Add more game types as they are implemented
    }
    
    /**
     * Check if an arena is available for starting a game
     * @param arenaName The name of the arena to check
     * @return true if the arena is available, false otherwise
     */
    public boolean isArenaAvailable(String arenaName, ArenaManager arenaManager) {
        Arena arena = arenaManager.getArena(arenaName);
        if (arena == null) {
            return false;
        }
        
        // Check if arena already has an active game
        if (activeGames.containsKey(arenaName)) {
            return false;
        }
        
        // Check arena state
        return arena.getState() == Arena.ArenaState.IDLE;
    }
    
    /**
     * Check if a game type is supported
     * @param gameType The game type to check
     * @return true if the game type is supported, false otherwise
     */
    public boolean isGameTypeSupported(String gameType) {
        return supportedGameTypes.contains(gameType.toLowerCase());
    }
    
    /**
     * Get a list of all supported game types
     * @return A collection of supported game types
     */
    public Collection<String> getSupportedGameTypes() {
        return new ArrayList<>(supportedGameTypes);
    }
    
    /**
     * Start a game in the specified arena with the specified game type
     * @param arena The arena to start the game in
     * @param gameType The type of game to start
     * @return true if the game was started successfully, false otherwise
     */
    public boolean startGame(Arena arena, String gameType) {
        if (arena.getCurrentGame() != null || arena.getState() != Arena.ArenaState.IDLE) {
            MachiavellianMinigames.LOGGER.warn("Attempted to start game in arena {} but it already has an active game or is not idle", arena.getName());
            return false;
        }
        
        if (!arena.isConfigured()) {
            MachiavellianMinigames.LOGGER.warn("Attempted to start game in unconfigured arena {}", arena.getName());
            return false;
        }
        
        Game game = createGame(gameType, arena);
        if (game == null) {
            MachiavellianMinigames.LOGGER.warn("Failed to create game of type {} for arena {}", gameType, arena.getName());
            return false;
        }
        
        arena.setCurrentGame(game);
        arena.setState(Arena.ArenaState.ACTIVE);
        activeGames.put(arena.getName(), game);
        
        // Add all team players to the game
        for (Team team : arena.getTeams()) {
            for (UUID playerId : team.getPlayers()) {
                ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
                if (player != null) {
                    game.addPlayer(player);
                    MachiavellianMinigames.LOGGER.info("Added player {} to {} game in arena {}", 
                        player.getName().getString(), gameType, arena.getName());
                }
            }
        }
        
        game.start();
        MachiavellianMinigames.LOGGER.info("Started {} game in arena {}", gameType, arena.getName());
        return true;
    }
    
    public boolean endGame(String arenaName) {
        Game game = activeGames.remove(arenaName);
        if (game != null) {
            game.end();
            game.getArena().setCurrentGame(null);
            game.getArena().setState(Arena.ArenaState.IDLE);
            MachiavellianMinigames.LOGGER.info("Ended game in arena {}", arenaName);
            return true;
        }
        return false;
    }
    
    public Game getGame(String arenaName) {
        return activeGames.get(arenaName);
    }
    
    public Collection<Game> getActiveGames() {
        return activeGames.values();
    }
    
    public void tick() {
        // Tick all active games
        for (Game game : activeGames.values()) {
            try {
                game.tick();
            } catch (Exception e) {
                MachiavellianMinigames.LOGGER.error("Error ticking game in arena {}: {}", 
                    game.getArena().getName(), e.getMessage());
            }
        }
    }
    
    /**
     * Create a game of the specified type in the specified arena
     * @param gameType The type of game to create
     * @param arena The arena to create the game in
     * @return The created game, or null if the game type is not supported
     */
    private Game createGame(String gameType, Arena arena) {
        gameType = gameType.toLowerCase();
        
        // Check if game type is supported
        if (!supportedGameTypes.contains(gameType)) {
            MachiavellianMinigames.LOGGER.warn("Unsupported game type: {}", gameType);
            return null;
        }
        
        // Create the appropriate game type
        if ("storm".equals(gameType) || "stormthefront".equals(gameType)) {
            return new StormTheFrontGame(arena);
        }
        
        // Additional game types can be added here
        
        MachiavellianMinigames.LOGGER.error("Failed to create game of type {}", gameType);
        return null;
    }
    
    public boolean addPlayerToGame(ServerPlayer player, String arenaName) {
        Game game = activeGames.get(arenaName);
        if (game != null) {
            if (game.getState() == Game.GameState.PREPARING) {
                game.addPlayer(player);
                player.sendSystemMessage(Component.literal("You have joined the " + game.getGameType() + " game in arena " + arenaName));
                return true;
            } else {
                player.sendSystemMessage(Component.literal("Cannot join game: The game has already started."));
            }
        } else {
            player.sendSystemMessage(Component.literal("Cannot join game: No active game in arena " + arenaName));
        }
        return false;
    }
    
    public void removePlayerFromGame(ServerPlayer player) {
        // Find and remove player from any active game
        removePlayerFromGame(player.getUUID());
    }
    
    /**
     * Remove a player from any game they're in by UUID
     * @param playerId The UUID of the player to remove
     */
    public void removePlayerFromGame(UUID playerId) {
        // Find and remove player from any active game
        for (Game game : activeGames.values()) {
            if (game.hasPlayer(playerId)) {
                game.removePlayer(playerId);
                break;
            }
        }
    }
    
    public Game getPlayerGame(ServerPlayer player) {
        for (Game game : activeGames.values()) {
            if (game.getPlayerData(player.getUUID()) != null) {
                return game;
            }
        }
        return null;
    }
}
