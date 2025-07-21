package com.mchivelli.machiavellianminigames.core;

import com.mchivelli.machiavellianminigames.MachiavellianMinigames;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

import java.util.*;

public abstract class Game {
    protected final Arena arena;
    protected final Map<UUID, PlayerData> playerData;
    protected GameState state;
    protected long startTime;
    protected long endTime;
    
    // Reward intervals (in milliseconds)
    protected static final long MONEY_COIN_INTERVAL = 60000; // 1 minute
    protected static final long UPGRADE_ITEM_INTERVAL = 300000; // 5 minutes
    protected long lastRewardUpdateTime;
    
    public enum GameState {
        PREPARING,
        ACTIVE,
        ENDING,
        ENDED
    }
    
    public Game(Arena arena) {
        this.arena = arena;
        this.playerData = new HashMap<>();
        this.state = GameState.PREPARING;
        this.lastRewardUpdateTime = 0;
    }
    
    public abstract void start();
    public abstract void end();
    public void tick() {
        // Common game tick logic
        if (state == GameState.ACTIVE) {
            // Update rewards for all players
            updatePlayerRewards();
            
            // Run game-specific tick logic
            gameTick();
        }
    }
    
    /**
     * Game-specific tick logic to be implemented by subclasses
     */
    protected abstract void gameTick();
    public abstract String getGameType();
    
    public void addPlayer(ServerPlayer player) {
        // Save player data and clear inventory for minigame
        PlayerData data = new PlayerData(player);
        data.save(); // Save their current inventory/state
        data.clearForMinigame(); // Clear inventory immediately when joining
        playerData.put(player.getUUID(), data);
        
        // Notify player
        player.sendSystemMessage(Component.literal("§6Your inventory has been saved and will be restored when you leave."));
    }
    
    public void removePlayer(UUID playerId) {
        PlayerData data = playerData.remove(playerId);
        if (data != null) {
            data.restore();
        }
    }
    
    public PlayerData getPlayerData(UUID playerId) {
        return playerData.get(playerId);
    }
    
    protected void saveAllPlayerData() {
        for (PlayerData data : playerData.values()) {
            data.save();
        }
    }
    
    /**
     * Restore all player data and reset the arena state
     */
    protected void restoreAllPlayerData() {
        // First restore all players' data
        for (PlayerData data : playerData.values()) {
            data.restore();
        }
        
        // Notify players about arena cleanup
        for (UUID playerId : playerData.keySet()) {
            ServerPlayer player = net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer()
                .getPlayerList().getPlayer(playerId);
            if (player != null) {
                player.sendSystemMessage(Component.literal("§6Arena is being reset..."));
            }
        }
    }
    
    /**
     * Update rewards for all players in the game
     */
    protected void updatePlayerRewards() {
        long currentTime = System.currentTimeMillis();
        
        // Don't update too frequently - check every second
        if (lastRewardUpdateTime > 0 && currentTime - lastRewardUpdateTime < 1000) {
            return;
        }
        
        // Update rewards for all players
        for (PlayerData data : playerData.values()) {
            try {
                data.updateRewards(currentTime, MONEY_COIN_INTERVAL, UPGRADE_ITEM_INTERVAL);
            } catch (Exception e) {
                MachiavellianMinigames.LOGGER.error("Error updating rewards for player: {}", e.getMessage());
            }
        }
        
        lastRewardUpdateTime = currentTime;
    }
    
    /**
     * Process the capture of a checkpoint by a team
     * This awards checkpoints and immediate bonuses to players on the capturing team
     * 
     * @param zone The checkpoint zone that was captured
     * @param teamName The name of the team that captured the checkpoint
     */
    protected void processCheckpointCapture(CheckpointZone zone, String teamName) {
        if (zone == null || teamName == null || teamName.isEmpty()) {
            return;
        }
        
        // Iterate through all players on the capturing team
        for (UUID playerId : playerData.keySet()) {
            PlayerData playerData = getPlayerData(playerId);
            
            // Only process players on the capturing team
            if (teamName.equals(getPlayerTeam(playerId))) {
                // Add the checkpoint to the player's owned checkpoints
                playerData.addCheckpoint(zone.getCheckpointType());
                
                // Give an immediate bonus based on checkpoint type
                if (zone.getCheckpointType() == CheckpointZone.CheckpointType.MONEY) {
                    playerData.awardMoneyCoin(2); // Bonus 2 coins for capturing money checkpoint
                } else if (zone.getCheckpointType() == CheckpointZone.CheckpointType.UPGRADE) {
                    playerData.awardUpgradeItem(1); // Bonus 1 upgrade for capturing upgrade checkpoint
                }
            }
        }
        
        MachiavellianMinigames.LOGGER.info("Team {} captured checkpoint {}", teamName, zone.getName());
    }
    
    /**
     * Reset the arena to its original state by cleaning entities and resetting blocks
     * Called when the game ends
     */
    protected void resetArena() {
        MachiavellianMinigames.LOGGER.info("Resetting arena {} after game {}", arena.getName(), getGameType());
        ArenaResetter.resetArena(arena);
        MachiavellianMinigames.LOGGER.info("Arena {} reset complete", arena.getName());
    }
    
    /**
     * Get the team name for a player
     * @param playerId The UUID of the player
     * @return The team name, or null if the player isn't on a team
     */
    protected abstract String getPlayerTeam(UUID playerId);
    
    // Getters
    public Arena getArena() { return arena; }
    public GameState getState() { return state; }
    public long getStartTime() { return startTime; }
    public long getEndTime() { return endTime; }
    public Collection<PlayerData> getPlayerData() { return playerData.values(); }
    
    /**
     * Check if a player with the given UUID is in this game
     * @param playerId The UUID of the player to check
     * @return true if the player is in this game, false otherwise
     */
    public boolean hasPlayer(UUID playerId) {
        return playerData.containsKey(playerId);
    }
    
    protected void setState(GameState state) {
        this.state = state;
    }
}
