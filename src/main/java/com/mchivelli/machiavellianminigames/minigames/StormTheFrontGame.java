package com.mchivelli.machiavellianminigames.minigames;

import com.mchivelli.machiavellianminigames.MachiavellianMinigames;
import com.mchivelli.machiavellianminigames.core.*;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerBossEvent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.BossEvent;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.*;

/**
 * Storm the Front minigame implementation - ISOLATED SYSTEM
 * Players compete to capture and hold checkpoint zones to earn resources
 * This game is completely isolated from base Game checkpoint mechanics
 */
public class StormTheFrontGame extends Game {
    
    // Isolated checkpoint management system
    private Map<String, StormCheckpointData> checkpoints = new HashMap<>();
    private Map<String, ServerBossEvent> bossBars = new HashMap<>();
    
    // Resource distribution tracking
    private long lastMoneyDistribution = 0;
    private long lastUpgradeDistribution = 0;
    
    // Message throttling
    private Map<String, Long> lastContestedMessageTime = new HashMap<>();
    
    public StormTheFrontGame(Arena arena) {
        super(arena);
    }
    
    @Override
    public void start() {
        setState(GameState.ACTIVE);
        startTime = System.currentTimeMillis();
        lastMoneyDistribution = startTime;
        lastUpgradeDistribution = startTime;
        
        // Initialize isolated checkpoint system
        initializeCheckpoints();
        
        // Initialize boss bars for all checkpoints
        initializeBossBars();
        
        // Teleport all players to their team spawns
        teleportPlayersToSpawns();
        
        // Give initial resources to all players
        giveInitialResources();
        
        // Broadcast game start message
        Component startMessage = Component.literal("§6Storm the Front has begun! Capture checkpoints to earn resources!");
        broadcastMessage(startMessage);
        
        MachiavellianMinigames.LOGGER.info("Storm the Front game started in arena {} with {} checkpoints", 
            arena.getName(), checkpoints.size());
    }
    
    @Override
    protected void gameTick() {
        long currentTime = System.currentTimeMillis();
        
        // Update isolated checkpoint system
        updateIsolatedCheckpoints(currentTime);
        
        // Distribute resources at intervals
        distributeResources(currentTime);
    }
    
    @Override
    public String getGameType() {
        return "Storm the Front";
    }
    
    @Override
    protected String getPlayerTeam(UUID playerId) {
        for (Team team : arena.getTeams()) {
            if (team.getPlayers().contains(playerId)) {
                return team.getName();
            }
        }
        return null;
    }
    
    /**
     * Handle king killed event
     */
    public void onKingKilled(UUID killedPlayerId, ServerPlayer killer) {
        // This method is called by MinigameEvents when a king is killed
        // For Storm the Front, we don't have specific king mechanics
        // but we keep this method for compatibility
    }
    
    /**
     * Initialize the isolated checkpoint system
     */
    private void initializeCheckpoints() {
        checkpoints.clear();
        
        for (CheckpointZone zone : arena.getCheckpointZones()) {
            StormCheckpointData checkpoint = new StormCheckpointData(
                zone.getName(), 
                zone.getCheckpointType(), 
                zone
            );
            checkpoints.put(zone.getName(), checkpoint);
            MachiavellianMinigames.LOGGER.info("Initialized checkpoint {} ({}) for Storm the Front", 
                zone.getName(), zone.getCheckpointType());
        }
    }
    
    /**
     * Teleport all players to their team spawns
     */
    private void teleportPlayersToSpawns() {
        for (UUID playerId : playerData.keySet()) {
            ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                // Find the player's team
                Team playerTeam = null;
                for (Team team : arena.getTeams()) {
                    if (team.hasPlayer(playerId)) {
                        playerTeam = team;
                        break;
                    }
                }
                
                if (playerTeam != null && playerTeam.getSpawnPoint() != null) {
                    BlockPos spawnPoint = playerTeam.getSpawnPoint();
                    player.teleportTo(spawnPoint.getX() + 0.5, spawnPoint.getY(), spawnPoint.getZ() + 0.5);
                    player.sendSystemMessage(Component.literal("§aTeleported to team spawn!"));
                }
            }
        }
    }
    
    /**
     * ISOLATED CHECKPOINT MANAGEMENT - Main update method
     */
    private void updateIsolatedCheckpoints(long currentTime) {
        for (StormCheckpointData checkpoint : checkpoints.values()) {
            updateSingleCheckpoint(checkpoint, currentTime);
        }
    }
    
    /**
     * Update a single checkpoint's state and progress
     */
    private void updateSingleCheckpoint(StormCheckpointData checkpoint, long currentTime) {
        // Get players currently in this checkpoint zone
        Set<ServerPlayer> playersInZone = getPlayersInCheckpointZone(checkpoint);
        Set<String> teamsInZone = getTeamsFromPlayers(playersInZone);
        
        // Handle checkpoint logic based on player presence
        if (playersInZone.isEmpty()) {
            handleEmptyCheckpoint(checkpoint, currentTime);
        } else if (teamsInZone.size() > 1) {
            handleContestedCheckpoint(checkpoint, teamsInZone);
        } else {
            handleSingleTeamCheckpoint(checkpoint, teamsInZone.iterator().next(), currentTime);
        }
        
        // Update boss bar for this checkpoint
        updateCheckpointBossBar(checkpoint, playersInZone, teamsInZone.size() > 1);
    }
    
    /**
     * Get all players currently in a checkpoint zone
     */
    private Set<ServerPlayer> getPlayersInCheckpointZone(StormCheckpointData checkpoint) {
        Set<ServerPlayer> playersInZone = new HashSet<>();
        
        for (Team team : arena.getTeams()) {
            for (UUID playerId : team.getPlayers()) {
                ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
                if (player != null && isPlayerInZone(player, checkpoint.getZone())) {
                    playersInZone.add(player);
                }
            }
        }
        
        return playersInZone;
    }
    
    /**
     * Get team names from a set of players
     */
    private Set<String> getTeamsFromPlayers(Set<ServerPlayer> players) {
        Set<String> teams = new HashSet<>();
        
        for (ServerPlayer player : players) {
            String teamName = getPlayerTeam(player.getUUID());
            if (teamName != null) {
                teams.add(teamName);
            }
        }
        
        return teams;
    }
    
    /**
     * Handle checkpoint when no players are present
     */
    private void handleEmptyCheckpoint(StormCheckpointData checkpoint, long currentTime) {
        // If checkpoint is being claimed and has been abandoned, reset it
        if (checkpoint.shouldResetDueToAbandonment(currentTime)) {
            MachiavellianMinigames.LOGGER.info("Checkpoint {} abandoned - resetting to unclaimed", 
                checkpoint.getName());
            checkpoint.resetToUnclaimed();
        }
        
        // Continue progress updates even when empty (for countdown)
        if (checkpoint.getState() == StormCheckpointData.State.CLAIMING || 
            checkpoint.getState() == StormCheckpointData.State.CLEARING) {
            checkpoint.updateProgress(currentTime);
        }
    }
    
    /**
     * Handle checkpoint when multiple teams are present (contested)
     */
    private void handleContestedCheckpoint(StormCheckpointData checkpoint, Set<String> teamsInZone) {
        checkpoint.setContested();
        
        // Send contested message (throttled)
        long lastMessageTime = lastContestedMessageTime.getOrDefault(checkpoint.getName(), 0L);
        if (System.currentTimeMillis() - lastMessageTime > StormTheFrontConfig.getContestedMessageCooldownMs()) {
            Component message = Component.literal("§c" + checkpoint.getName() + " is contested!");
            broadcastMessage(message);
            lastContestedMessageTime.put(checkpoint.getName(), System.currentTimeMillis());
        }
    }
    
    /**
     * Handle checkpoint when single team is present
     */
    private void handleSingleTeamCheckpoint(StormCheckpointData checkpoint, String teamName, long currentTime) {
        checkpoint.updatePlayerPresence(currentTime);
        
        // Resume from contested if needed
        if (checkpoint.getState() == StormCheckpointData.State.CONTESTED) {
            checkpoint.resumeFromContested(teamName, currentTime);
            return;
        }
        
        // Handle different checkpoint states
        switch (checkpoint.getState()) {
            case CONTROLLED:
                // Team already controls this checkpoint
                if (teamName.equals(checkpoint.getControllingTeam())) {
                    // Same team controlling - no action needed
                    return;
                } else {
                    // Enemy team - start clearing phase
                    checkpoint.startClearing(teamName, currentTime);
                    notifyCheckpointAction(checkpoint, teamName, "clearing");
                }
                break;
                
            case UNCLAIMED:
                // Start claiming unclaimed checkpoint
                checkpoint.startClaiming(teamName, currentTime);
                notifyCheckpointAction(checkpoint, teamName, "claiming");
                break;
                
            case CLAIMING:
            case CLEARING:
                // Continue existing claim/clear if same team
                if (teamName.equals(checkpoint.getClaimingTeam())) {
                    checkpoint.updateProgress(currentTime);
                    
                    // Check if complete
                    if (checkpoint.isComplete()) {
                        handleCheckpointCompletion(checkpoint, currentTime);
                    }
                } else {
                    // Different team interrupting - start over
                    if (checkpoint.getState() == StormCheckpointData.State.CLAIMING) {
                        checkpoint.startClearing(teamName, currentTime);
                        notifyCheckpointAction(checkpoint, teamName, "clearing");
                    } else {
                        checkpoint.startClaiming(teamName, currentTime);
                        notifyCheckpointAction(checkpoint, teamName, "claiming");
                    }
                }
                break;
        }
    }
    
    /**
     * Handle completion of checkpoint claiming or clearing
     */
    private void handleCheckpointCompletion(StormCheckpointData checkpoint, long currentTime) {
        if (checkpoint.getState() == StormCheckpointData.State.CLEARING) {
            // Clearing completed - start claiming phase
            String attackingTeam = checkpoint.getClaimingTeam();
            MachiavellianMinigames.LOGGER.info("Checkpoint {} cleared by team {} - starting claim phase", 
                checkpoint.getName(), attackingTeam);
            
            checkpoint.startClaiming(attackingTeam, currentTime);
            notifyCheckpointAction(checkpoint, attackingTeam, "claiming after clearing");
            
        } else if (checkpoint.getState() == StormCheckpointData.State.CLAIMING) {
            // Claiming completed - checkpoint captured!
            String capturingTeam = checkpoint.getClaimingTeam();
            MachiavellianMinigames.LOGGER.info("Checkpoint {} captured by team {}", 
                checkpoint.getName(), capturingTeam);
            
            checkpoint.completeCapture(capturingTeam);
            notifyCheckpointCaptured(checkpoint, capturingTeam);
            awardImmediateResources(capturingTeam);
            updateResourceMultipliers();
        }
    }
    
    /**
     * Notify players of checkpoint actions
     */
    private void notifyCheckpointAction(StormCheckpointData checkpoint, String teamName, String action) {
        String checkpointType = checkpoint.getZone().getCheckpointType() == CheckpointZone.CheckpointType.MONEY ? 
            "Money" : "Upgrade";
        
        Component message;
        if (action.equals("claiming")) {
            message = Component.literal(String.format(
                "§%s%s is capturing %s (%s)!", 
                getTeamColor(teamName), teamName, checkpoint.getName(), checkpointType));
        } else if (action.equals("clearing")) {
            String defendingTeam = checkpoint.getControllingTeam();
            message = Component.literal(String.format(
                "§%s%s is clearing %s's control of %s (%s)!", 
                getTeamColor(teamName), teamName, defendingTeam, checkpoint.getName(), checkpointType));
        } else if (action.equals("claiming after clearing")) {
            message = Component.literal(String.format(
                "§%s%s cleared %s (%s) and is now claiming it!", 
                getTeamColor(teamName), teamName, checkpoint.getName(), checkpointType));
        } else {
            message = Component.literal(String.format(
                "§%s%s is %s %s (%s)!", 
                getTeamColor(teamName), teamName, action, checkpoint.getName(), checkpointType));
        }
        
        broadcastMessage(message);
    }
    
    /**
     * Notify players when checkpoint is captured
     */
    private void notifyCheckpointCaptured(StormCheckpointData checkpoint, String teamName) {
        String checkpointType = checkpoint.getZone().getCheckpointType() == CheckpointZone.CheckpointType.MONEY ? 
            "Money" : "Upgrade";
        
        Component message = Component.literal(String.format(
            "§%s%s has captured %s (%s)!", 
            getTeamColor(teamName), teamName, checkpoint.getName(), checkpointType));
        
        broadcastMessage(message);
    }
    
    /**
     * Update boss bar for a specific checkpoint
     */
    private void updateCheckpointBossBar(StormCheckpointData checkpoint, Set<ServerPlayer> playersInZone, boolean isContested) {
        ServerBossEvent bossBar = bossBars.get(checkpoint.getName());
        if (bossBar == null) return;
        
        // Clear existing players
        bossBar.removeAllPlayers();
        
        // Update boss bar based on checkpoint state
        String checkpointType = checkpoint.getZone().getCheckpointType() == CheckpointZone.CheckpointType.MONEY ? 
            "§6(Money)" : "§2(Upgrade)";
        
        switch (checkpoint.getState()) {
            case CONTROLLED:
                String controllingTeam = checkpoint.getControllingTeam();
                bossBar.setName(Component.literal(String.format(
                    "§%s%s controls %s %s", 
                    getTeamColor(controllingTeam), controllingTeam, checkpoint.getName(), checkpointType)));
                bossBar.setColor(getBossBarColor(controllingTeam));
                bossBar.setProgress(1.0F);
                
                // Add all players for visibility
                addAllPlayersToBar(bossBar);
                break;
                
            case CLAIMING:
                String claimingTeam = checkpoint.getClaimingTeam();
                float claimProgress = checkpoint.getProgressPercentage() / 100.0F;
                
                if (isContested) {
                    bossBar.setName(Component.literal(String.format(
                        "§c%s %s (Contested)", checkpoint.getName(), checkpointType)));
                    bossBar.setColor(BossEvent.BossBarColor.RED);
                } else {
                    bossBar.setName(Component.literal(String.format(
                        "§%s%s claiming %s %s (%.0f%%)", 
                        getTeamColor(claimingTeam), claimingTeam, checkpoint.getName(), checkpointType, checkpoint.getProgressPercentage())));
                    bossBar.setColor(getBossBarColor(claimingTeam));
                }
                bossBar.setProgress(claimProgress);
                
                // Add claiming team players and players in zone
                addTeamPlayersToBar(bossBar, claimingTeam);
                for (ServerPlayer player : playersInZone) {
                    bossBar.addPlayer(player);
                }
                break;
                
            case CLEARING:
                String clearingTeam = checkpoint.getClaimingTeam();
                String defendingTeam = checkpoint.getControllingTeam();
                float clearProgress = checkpoint.getProgressPercentage() / 100.0F;
                
                if (isContested) {
                    bossBar.setName(Component.literal(String.format(
                        "§c%s %s (Contested)", checkpoint.getName(), checkpointType)));
                    bossBar.setColor(BossEvent.BossBarColor.RED);
                } else {
                    bossBar.setName(Component.literal(String.format(
                        "§c%s's control being cleared (%.0f%%)", 
                        defendingTeam, checkpoint.getProgressPercentage())));
                    bossBar.setColor(BossEvent.BossBarColor.RED);
                }
                bossBar.setProgress(clearProgress);
                
                // Add clearing team players and players in zone
                addTeamPlayersToBar(bossBar, clearingTeam);
                for (ServerPlayer player : playersInZone) {
                    bossBar.addPlayer(player);
                }
                break;
                
            case CONTESTED:
                bossBar.setName(Component.literal(String.format(
                    "§c%s %s (Contested)", checkpoint.getName(), checkpointType)));
                bossBar.setColor(BossEvent.BossBarColor.RED);
                bossBar.setProgress(0.0F);
                
                // Add all players in zone
                for (ServerPlayer player : playersInZone) {
                    bossBar.addPlayer(player);
                }
                break;
                
            case UNCLAIMED:
            default:
                bossBar.setName(Component.literal(String.format(
                    "§f%s %s (Unclaimed)", checkpoint.getName(), checkpointType)));
                bossBar.setColor(BossEvent.BossBarColor.WHITE);
                bossBar.setProgress(0.0F);
                
                // Add players in zone
                for (ServerPlayer player : playersInZone) {
                    bossBar.addPlayer(player);
                }
                break;
        }
    }
    
    /**
     * Distribute resources based on controlled checkpoints - ISOLATED SYSTEM
     */
    private void distributeResources(long currentTime) {
        // Check if it's time to distribute money
        if (currentTime - lastMoneyDistribution >= StormTheFrontConfig.getMoneyIntervalMs()) {
            distributeMoneyResources();
            lastMoneyDistribution = currentTime;
        }
        
        // Check if it's time to distribute upgrades
        if (currentTime - lastUpgradeDistribution >= StormTheFrontConfig.getUpgradeIntervalMs()) {
            distributeUpgradeResources();
            lastUpgradeDistribution = currentTime;
        }
    }
    
    /**
     * Distribute money resources to all players based on controlled checkpoints
     */
    private void distributeMoneyResources() {
        for (Team team : arena.getTeams()) {
            int moneyCheckpoints = getControlledCheckpointsOfType(team.getName(), CheckpointZone.CheckpointType.MONEY);
            if (moneyCheckpoints > 0) {
                int multiplier = (int) Math.pow(2, moneyCheckpoints);
                int baseAmount = StormTheFrontConfig.getMoneyPerInterval();
                int totalAmount = baseAmount * multiplier;
                
                for (UUID playerId : team.getPlayers()) {
                    PlayerData playerData = getPlayerData(playerId);
                    if (playerData != null) {
                        playerData.awardMoneyCoin(totalAmount);
                    }
                }
                
                // Notify team
                Component message = Component.literal(String.format(
                    "§6+%d money from %d checkpoint%s (x%d multiplier)", 
                    totalAmount, moneyCheckpoints, moneyCheckpoints == 1 ? "" : "s", multiplier));
                broadcastMessageToTeam(team.getName(), message);
            }
        }
    }
    
    /**
     * Distribute upgrade resources to all players based on controlled checkpoints
     */
    private void distributeUpgradeResources() {
        for (Team team : arena.getTeams()) {
            int upgradeCheckpoints = getControlledCheckpointsOfType(team.getName(), CheckpointZone.CheckpointType.UPGRADE);
            if (upgradeCheckpoints > 0) {
                int multiplier = (int) Math.pow(2, upgradeCheckpoints);
                int baseAmount = StormTheFrontConfig.getUpgradesPerInterval();
                int totalAmount = baseAmount * multiplier;
                
                for (UUID playerId : team.getPlayers()) {
                    PlayerData playerData = getPlayerData(playerId);
                    if (playerData != null) {
                        playerData.awardUpgradeItem(totalAmount);
                    }
                }
                
                // Notify team
                Component message = Component.literal(String.format(
                    "§2+%d upgrades from %d checkpoint%s (x%d multiplier)", 
                    totalAmount, upgradeCheckpoints, upgradeCheckpoints == 1 ? "" : "s", multiplier));
                broadcastMessageToTeam(team.getName(), message);
            }
        }
    }
    
    /**
     * Get number of controlled checkpoints of a specific type for a team
     */
    private int getControlledCheckpointsOfType(String teamName, CheckpointZone.CheckpointType type) {
        int count = 0;
        for (StormCheckpointData checkpoint : checkpoints.values()) {
            if (checkpoint.getState() == StormCheckpointData.State.CONTROLLED && 
                teamName.equals(checkpoint.getControllingTeam()) &&
                checkpoint.getZone().getCheckpointType() == type) {
                count++;
            }
        }
        return count;
    }
    
    /**
     * Award immediate resources when checkpoint is captured
     */
    private void awardImmediateResources(String teamName) {
        // Award immediate bonus to capturing team
        for (UUID playerId : getTeamPlayers(teamName)) {
            PlayerData playerData = getPlayerData(playerId);
            if (playerData != null) {
                playerData.awardMoneyCoin(StormTheFrontConfig.getCaptureMoneyBonus());
                playerData.awardUpgradeItem(StormTheFrontConfig.getCaptureUpgradeBonus());
            }
        }
        
        // Notify team of bonus
        if (StormTheFrontConfig.getCaptureMoneyBonus() > 0 || StormTheFrontConfig.getCaptureUpgradeBonus() > 0) {
            Component message = Component.literal(String.format(
                "§eCaptured checkpoint! Bonus: +%d money, +%d upgrades",
                StormTheFrontConfig.getCaptureMoneyBonus(), 
                StormTheFrontConfig.getCaptureUpgradeBonus()));
            broadcastMessageToTeam(teamName, message);
        }
    }
    
    /**
     * Update resource multipliers based on currently controlled checkpoints
     */
    private void updateResourceMultipliers() {
        for (Team team : arena.getTeams()) {
            int moneyCheckpoints = getControlledCheckpointsOfType(team.getName(), CheckpointZone.CheckpointType.MONEY);
            int upgradeCheckpoints = getControlledCheckpointsOfType(team.getName(), CheckpointZone.CheckpointType.UPGRADE);
            
            for (UUID playerId : team.getPlayers()) {
                PlayerData playerData = getPlayerData(playerId);
                if (playerData != null) {
                    playerData.setMoneyCheckpoints(moneyCheckpoints);
                    playerData.setUpgradeCheckpoints(upgradeCheckpoints);
                }
            }
        }
    }
    
    /**
     * ISOLATED SYSTEM - Initialize boss bars for all checkpoints
     */
    private void initializeBossBars() {
        for (CheckpointZone zone : arena.getCheckpointZones()) {
            String zoneName = zone.getName();
            String checkpointType = zone.getCheckpointType() == CheckpointZone.CheckpointType.MONEY ? 
                "§6(Money)" : "§2(Upgrade)";
            
            ServerBossEvent bossBar = new ServerBossEvent(
                Component.literal("§f" + zoneName + " " + checkpointType + " (Unclaimed)"),
                BossEvent.BossBarColor.WHITE,
                BossEvent.BossBarOverlay.PROGRESS
            );
            
            bossBars.put(zoneName, bossBar);
            
            MachiavellianMinigames.LOGGER.info("Initialized boss bar for checkpoint: {}", zoneName);
        }
    }
    
    /**
     * Get team color code for formatting
     */
    private String getTeamColor(String teamName) {
        Team team = arena.getTeam(teamName);
        if (team != null) {
            ChatFormatting color = team.getColor();
            if (color != null) {
                return color.toString().substring(1); // Remove § prefix
            }
        }
        return "f"; // Default to white
    }
    
    /**
     * Get boss bar color for team
     */
    private BossEvent.BossBarColor getBossBarColor(String teamName) {
        Team team = arena.getTeam(teamName);
        if (team != null) {
            ChatFormatting color = team.getColor();
            if (color != null) {
                switch (color) {
                    case BLUE: return BossEvent.BossBarColor.BLUE;
                    case GREEN: return BossEvent.BossBarColor.GREEN;
                    case YELLOW: return BossEvent.BossBarColor.YELLOW;
                    case RED: return BossEvent.BossBarColor.RED;
                    case LIGHT_PURPLE: return BossEvent.BossBarColor.PINK;
                    case AQUA: return BossEvent.BossBarColor.BLUE;
                    default: return BossEvent.BossBarColor.WHITE;
                }
            }
        }
        return BossEvent.BossBarColor.WHITE;
    }
    
    /**
     * Add all players in the game to a boss bar
     */
    private void addAllPlayersToBar(ServerBossEvent bossBar) {
        for (Team team : arena.getTeams()) {
            for (UUID playerId : team.getPlayers()) {
                ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
                if (player != null) {
                    bossBar.addPlayer(player);
                }
            }
        }
    }
    
    /**
     * Add players from a specific team to a boss bar
     */
    private void addTeamPlayersToBar(ServerBossEvent bossBar, String teamName) {
        Team team = arena.getTeam(teamName);
        if (team != null) {
            for (UUID playerId : team.getPlayers()) {
                ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
                if (player != null) {
                    bossBar.addPlayer(player);
                }
            }
        }
    }
    
    /**
     * Broadcast message to specific team
     */
    private void broadcastMessageToTeam(String teamName, Component message) {
        Team team = arena.getTeam(teamName);
        if (team != null) {
            for (UUID playerId : team.getPlayers()) {
                ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
                if (player != null) {
                    player.sendSystemMessage(message);
                }
            }
        }
    }
    
    /**
     * Get list of player UUIDs for a team
     */
    private Set<UUID> getTeamPlayers(String teamName) {
        Team team = arena.getTeam(teamName);
        if (team != null) {
            return team.getPlayers();
        }
        return new HashSet<>();
    }
    
    /**
     * Update checkpoint zones and handle claiming logic
     */
    private void updateCheckpoints() {
        long currentTime = System.currentTimeMillis();
        
        for (CheckpointZone zone : arena.getCheckpointZones()) {
            // Track players in this zone
            Set<ServerPlayer> playersInZone = new HashSet<>();
            Set<String> teamsInZone = new HashSet<>();
            
            // Check which players are in this zone
            for (Team team : arena.getTeams()) {
                for (UUID playerId : team.getPlayers()) {
                    ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
                    if (player != null && isPlayerInZone(player, zone)) {
                        playersInZone.add(player);
                        teamsInZone.add(team.getName());
                    }
                }
            }
            
            // Handle zone logic based on player presence
            handleZoneLogic(zone, playersInZone, teamsInZone, currentTime);
        }
    }
    
    /**
     * Update all boss bars continuously to ensure seamless progress display
     */
    private void updateAllBossBars() {
        for (CheckpointZone zone : arena.getCheckpointZones()) {
            // Only update boss bars for zones that are being claimed or contested
            if (zone.isBeingClaimed() || zone.isControlled()) {
                // Get current players in zone
                Set<ServerPlayer> playersInZone = new HashSet<>();
                Set<String> teamsInZone = new HashSet<>();
                
                for (Team team : arena.getTeams()) {
                    for (UUID playerId : team.getPlayers()) {
                        ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
                        if (player != null && isPlayerInZone(player, zone)) {
                            playersInZone.add(player);
                            teamsInZone.add(team.getName());
                        }
                    }
                }
                
                // Update boss bar with current state
                boolean isContested = teamsInZone.size() > 1;
                updateBossBar(zone, playersInZone, isContested);
            }
        }
    }
    
    /**
     * Check if a player is in a zone using multiple hitbox points
     */
    private boolean isPlayerInZone(ServerPlayer player, CheckpointZone zone) {
        double playerX = player.getX();
        double playerY = player.getY();
        double playerZ = player.getZ();
        
        // Check multiple points around player hitbox for reliable detection
        BlockPos centerPos = new BlockPos((int)Math.floor(playerX), (int)Math.floor(playerY), (int)Math.floor(playerZ));
        BlockPos pos1 = new BlockPos((int)Math.floor(playerX - 0.3), (int)Math.floor(playerY), (int)Math.floor(playerZ - 0.3));
        BlockPos pos2 = new BlockPos((int)Math.floor(playerX + 0.3), (int)Math.floor(playerY), (int)Math.floor(playerZ + 0.3));
        
        return zone.isInZone(centerPos) || zone.isInZone(pos1) || zone.isInZone(pos2);
    }
    
    /**
     * Handle zone logic based on player presence
     */
    private void handleZoneLogic(CheckpointZone zone, Set<ServerPlayer> playersInZone, 
                                Set<String> teamsInZone, long currentTime) {
        
        // Always update boss bar first (shows to players in zone AND claiming team)
        updateBossBar(zone, playersInZone, teamsInZone.size() > 1);
        
        // Handle zone abandonment
        if (playersInZone.isEmpty() && zone.shouldResetDueToPlayerAbsence(currentTime)) {
            handleZoneAbandonment(zone);
            // Update boss bar after abandonment to reflect new state
            updateBossBar(zone, playersInZone, false);
            return;
        }
        
        // Handle zone claiming logic
        if (playersInZone.isEmpty()) {
            // No players in zone - but still need to update claiming progress if zone is being claimed
            if (zone.isBeingClaimed()) {
                zone.updateClaimProgress(currentTime);
                // Update boss bar to show progress to claiming team even when no one is in zone
                updateBossBar(zone, playersInZone, false);
            }
            return;
        }
        else if (teamsInZone.size() > 1) {
            // Contested zone
            handleContestedZone(zone, teamsInZone);
        }
        else {
            // Single team in zone
            String singleTeam = teamsInZone.iterator().next();
            handleSingleTeamZone(zone, singleTeam, currentTime);
        }
        
        // Always update boss bar after handling zone logic to ensure latest state is shown
        updateBossBar(zone, playersInZone, teamsInZone.size() > 1);
    }
    
    /**
     * Update boss bar - shows to players in zone AND claiming team members
     */
    private void updateBossBar(CheckpointZone zone, Set<ServerPlayer> playersInZone, boolean isContested) {
        ServerBossEvent bossBar = bossBars.get(zone.getName());
        if (bossBar == null) return;
        
        String zoneName = zone.getName() + " " + 
            (zone.getCheckpointType() == CheckpointZone.CheckpointType.MONEY ? 
            "§6(Money)" : "§2(Upgrade)");
        
        // Determine who should see the boss bar
        Set<ServerPlayer> bossBarViewers = new HashSet<>(playersInZone);
        
        // Add claiming team members if zone is being claimed (so they can see progress even when not in zone)
        if (zone.isBeingClaimed()) {
            String claimingTeamName = zone.getClaimingTeam();
            Team claimingTeam = arena.getTeam(claimingTeamName);
            if (claimingTeam != null) {
                for (UUID playerId : claimingTeam.getPlayers()) {
                    ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        bossBarViewers.add(player);
                    }
                }
            }
        }
        
        // Update boss bar viewers
        bossBar.removeAllPlayers();
        for (ServerPlayer player : bossBarViewers) {
            bossBar.addPlayer(player);
        }
        
        // Always update boss bar content (even if no viewers - for when players join later)
        float accurateProgress = zone.isBeingClaimed() ? zone.getClaimPercentage() : 0.0F;
        
        // Update boss bar based on state
        if (isContested) {
            // Contested - show current progress
            bossBar.setName(Component.literal("§c" + zoneName + " (CONTESTED)"));
            bossBar.setColor(BossEvent.BossBarColor.RED);
            bossBar.setProgress(accurateProgress / 100.0F);
        }
        else if (zone.isControlled() && !"CLEARING".equals(zone.getControllingTeam())) {
            // Controlled (not in clearing phase)
            Team controllingTeam = arena.getTeam(zone.getControllingTeam());
            if (controllingTeam != null) {
                bossBar.setName(Component.literal(controllingTeam.getColor() + 
                    controllingTeam.getName() + "§f controls " + zoneName));
                bossBar.setColor(getBossBarColorFromChatFormatting(controllingTeam.getColor()));
                bossBar.setProgress(1.0F);
            }
        }
        else if (zone.isBeingClaimed()) {
            // Being claimed - show progress
            if ("CLEARING".equals(zone.getControllingTeam())) {
                // Clearing phase - show clearing progress
                Team claimingTeam = arena.getTeam(zone.getClaimingTeam());
                if (claimingTeam != null) {
                    bossBar.setName(Component.literal("§c" + claimingTeam.getColor() + 
                        claimingTeam.getName() + "§f clearing " + zoneName + 
                        " (" + Math.round(accurateProgress) + "%)"));
                    bossBar.setColor(BossEvent.BossBarColor.RED);
                    bossBar.setProgress(accurateProgress / 100.0F);
                }
            } else {
                // Normal claiming - show claiming progress
                Team claimingTeam = arena.getTeam(zone.getClaimingTeam());
                if (claimingTeam != null) {
                    String progressText = playersInZone.isEmpty() ? 
                        " (LOSING " + Math.round(100 - accurateProgress) + "%)" : 
                        " (" + Math.round(accurateProgress) + "%)";
                    
                    bossBar.setName(Component.literal(claimingTeam.getColor() + 
                        claimingTeam.getName() + "§f claiming " + zoneName + progressText));
                    bossBar.setColor(getBossBarColorFromChatFormatting(claimingTeam.getColor()));
                    bossBar.setProgress(accurateProgress / 100.0F);
                }
            }
        }
        else {
            // Unclaimed - only show to players in zone
            if (!playersInZone.isEmpty()) {
                bossBar.setName(Component.literal("§f" + zoneName + " (Unclaimed)"));
                bossBar.setColor(BossEvent.BossBarColor.WHITE);
                bossBar.setProgress(0.0F);
            }
        }
    }
    
    /**
     * Handle zone abandonment
     */
    private void handleZoneAbandonment(CheckpointZone zone) {
        MachiavellianMinigames.LOGGER.info("Resetting checkpoint {} due to player absence", zone.getName());
        
        // Notify team if claim was interrupted
        if (zone.isBeingClaimed()) {
            String claimingTeam = zone.getClaimingTeam();
            Team team = arena.getTeam(claimingTeam);
            if (team != null) {
                Component message = Component.literal(team.getColor() + 
                    "Your claim on " + zone.getName() + " was interrupted - capture incomplete!");
                
                for (UUID playerId : team.getPlayers()) {
                    ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
                    if (player != null) {
                        player.sendSystemMessage(message);
                    }
                }
            }
        }
        
        // Reset zone
        if ("CLEARING".equals(zone.getControllingTeam())) {
            zone.setControllingTeam(null);
        }
        zone.resetClaim();
    }
    
    /**
     * Handle contested zone
     */
    private void handleContestedZone(CheckpointZone zone, Set<String> teamsInZone) {
        zone.updatePlayerPresence();
        
        // Pause claim progress
        if (zone.isBeingClaimed() && !zone.isClaimPaused()) {
            zone.pauseClaim();
            MachiavellianMinigames.LOGGER.info("Zone {} claim progress PAUSED due to contest", zone.getName());
        }
        
        // Send contested message (throttled)
        long currentTime = System.currentTimeMillis();
        long lastMessageTime = lastContestedMessageTime.getOrDefault(zone.getName(), 0L);
        
        if (currentTime - lastMessageTime > 3000) {
            Component message = Component.literal("§c" + zone.getName() + " is contested!");
            broadcastMessage(message);
            lastContestedMessageTime.put(zone.getName(), currentTime);
        }
    }
    
    /**
     * Handle single team zone
     */
    private void handleSingleTeamZone(CheckpointZone zone, String singleTeam, long currentTime) {
        // If team already controls, just update presence
        if (singleTeam.equals(zone.getControllingTeam())) {
            zone.updatePlayerPresence();
            return;
        }
        
        // If already claiming, continue claim process
        if (zone.isBeingClaimed()) {
            boolean shouldContinue = false;
            
            if ("CLEARING".equals(zone.getControllingTeam())) {
                shouldContinue = singleTeam.equals(zone.getClaimingTeam());
            } else {
                shouldContinue = singleTeam.equals(zone.getClaimingTeam());
            }
            
            if (shouldContinue) {
                // Resume if paused
                if (zone.isClaimPaused()) {
                    zone.resumeClaim();
                    MachiavellianMinigames.LOGGER.info("Zone {} claim progress RESUMED", zone.getName());
                }
                
                zone.updatePlayerPresence();
                zone.updateClaimProgress(currentTime);
                
                // Check completion using accurate percentage calculation
                if (zone.getClaimPercentage() >= 100.0F) {
                    handleClaimCompletion(zone, singleTeam);
                }
            }
        } else {
            // Start new claim
            startNewClaim(zone, singleTeam);
        }
    }
    
    /**
     * Handle claim completion
     */
    private void handleClaimCompletion(CheckpointZone zone, String singleTeam) {
        if ("CLEARING".equals(zone.getControllingTeam())) {
            // Clearing complete - start claiming
            String claimingTeam = zone.getClaimingTeam();
            MachiavellianMinigames.LOGGER.info("Zone {} cleared, now claiming for {}", zone.getName(), claimingTeam);
            
            zone.setControllingTeam(null);
            zone.resetClaim();
            zone.startClaim(claimingTeam);
            
            Team team = arena.getTeam(claimingTeam);
            if (team != null) {
                Component message = Component.literal(team.getColor() + 
                    team.getName() + "§f cleared " + zone.getName() + " and is now claiming it!");
                broadcastMessage(message);
            }
        } else {
            // Normal claim complete - CHECKPOINT CAPTURED!
            String claimingTeam = zone.getClaimingTeam();
            MachiavellianMinigames.LOGGER.info("CHECKPOINT COMPLETION: Zone {} being completed by team {}", zone.getName(), claimingTeam);
            
            zone.completeClaim();
            MachiavellianMinigames.LOGGER.info("CHECKPOINT COMPLETION: Zone {} now controlled by team {}", zone.getName(), zone.getControllingTeam());
            
            processCheckpointCapture(zone, claimingTeam);
            
            // Enhanced notification system for checkpoint capture
            Team capturingTeam = arena.getTeam(claimingTeam);
            if (capturingTeam != null) {
                String zoneType = zone.getCheckpointType() == CheckpointZone.CheckpointType.MONEY ? "Money" : "Upgrade";
                
                // Main capture announcement
                Component captureMessage = Component.literal("§6§l[CHECKPOINT CAPTURED] " + 
                    capturingTeam.getColor() + capturingTeam.getName() + 
                    "§f has captured " + zone.getName() + " (" + zoneType + ")!");
                broadcastMessage(captureMessage);
                
                // Notify capturing team of their success
                Component teamMessage = Component.literal(capturingTeam.getColor() + 
                    "§l[TEAM] §r" + capturingTeam.getColor() + "Excellent work! You now control " + 
                    zone.getName() + " and will receive bonus " + zoneType.toLowerCase() + " resources!");
                notifyTeam(capturingTeam, teamMessage);
                
                // Notify other teams of the capture
                for (Team otherTeam : arena.getTeams()) {
                    if (!otherTeam.getName().equals(claimingTeam)) {
                        Component enemyMessage = Component.literal("§c§l[ALERT] §r§c" + 
                            capturingTeam.getName() + " has captured " + zone.getName() + 
                            "! They will now receive bonus resources from this checkpoint.");
                        notifyTeam(otherTeam, enemyMessage);
                    }
                }
            }
            
            MachiavellianMinigames.LOGGER.info("Team {} captured zone {}", claimingTeam, zone.getName());
        }
    }
    
    /**
     * Start new claim for a team
     */
    private void startNewClaim(CheckpointZone zone, String attackingTeam) {
        String currentController = zone.getControllingTeam();
        
        if (currentController == null) {
            // Unclaimed - start normal claim
            zone.startClaim(attackingTeam);
            
            Team team = arena.getTeam(attackingTeam);
            if (team != null) {
                Component message = Component.literal(team.getColor() + 
                    team.getName() + "§f is capturing " + zone.getName() + "!");
                broadcastMessage(message);
            }
        } else if (!attackingTeam.equals(currentController)) {
            // Enemy controlled - start clearing phase
            zone.setControllingTeam("CLEARING");
            zone.startClaim(attackingTeam);
            
            Team attackingTeamObj = arena.getTeam(attackingTeam);
            Team defendingTeamObj = arena.getTeam(currentController);
            
            if (attackingTeamObj != null && defendingTeamObj != null) {
                Component message = Component.literal(attackingTeamObj.getColor() + 
                    attackingTeamObj.getName() + "§f is clearing " + defendingTeamObj.getName() + 
                    "'s control of " + zone.getName() + "!");
                broadcastMessage(message);
            }
        }
    }
    
    /**
     * Process checkpoint capture and update player counts
     */
    @Override
    protected void processCheckpointCapture(CheckpointZone zone, String controllingTeam) {
        // Call parent method to award immediate capture bonuses
        super.processCheckpointCapture(zone, controllingTeam);
        
        // Update checkpoint counts for ongoing resource multipliers
        updatePlayerCheckpointCounts();
        
        MachiavellianMinigames.LOGGER.info("Checkpoint {} captured by team {} - immediate bonuses awarded and multipliers updated", 
            zone.getName(), controllingTeam);
    }
    
    /**
     * Update player checkpoint counts for resource multipliers
     */
    private void updatePlayerCheckpointCounts() {
        // Reset all counts
        for (PlayerData data : playerData.values()) {
            data.setMoneyCheckpoints(0);
            data.setUpgradeCheckpoints(0);
        }
        
        // Count controlled checkpoints per team
        for (CheckpointZone zone : arena.getCheckpointZones()) {
            String controllingTeam = zone.getControllingTeam();
            if (controllingTeam != null && !controllingTeam.equals("CLEARING")) {
                Team team = arena.getTeam(controllingTeam);
                if (team != null) {
                    for (UUID playerId : team.getPlayers()) {
                        PlayerData data = playerData.get(playerId);
                        if (data != null) {
                            if (zone.getCheckpointType() == CheckpointZone.CheckpointType.MONEY) {
                                data.setMoneyCheckpoints(data.getMoneyCheckpoints() + 1);
                            } else {
                                data.setUpgradeCheckpoints(data.getUpgradeCheckpoints() + 1);
                            }
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Give initial resources to all players
     */
    private void giveInitialResources() {
        for (UUID playerId : playerData.keySet()) {
            ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                PlayerData data = playerData.get(playerId);
                if (data != null) {
                    // Give initial resources
                    data.awardMoneyCoin(1);
                    data.awardUpgradeItem(1);
                }
            }
        }
    }
    
    /**
     * Custom resource distribution is handled by the base Game class
     * The PlayerData.updateRewards() method handles checkpoint multipliers automatically
     */
    
    /**
     * Print debug information
     */
    private void printDebugInfo() {
        MachiavellianMinigames.LOGGER.info("=== Storm the Front Debug Info ===");
        MachiavellianMinigames.LOGGER.info("Active players: {}", playerData.size());
        MachiavellianMinigames.LOGGER.info("Checkpoint zones: {}", arena.getCheckpointZones().size());
        
        for (PlayerData data : playerData.values()) {
            ServerPlayer player = data.getPlayer();
            if (player != null) {
                String teamName = getPlayerTeam(player.getUUID());
                if (teamName == null) teamName = "None";
                MachiavellianMinigames.LOGGER.info("Player {} (Team: {}) at {} - Money checkpoints: {}, Upgrade checkpoints: {}", 
                    player.getName().getString(), teamName, player.blockPosition(), 
                    data.getMoneyCheckpoints(), data.getUpgradeCheckpoints());
            }
        }
        
        for (CheckpointZone zone : arena.getCheckpointZones()) {
            MachiavellianMinigames.LOGGER.info("Zone {} ({}) - Controlling: {}, Claiming: {}, Progress: {}%", 
                zone.getName(), zone.getCheckpointType(), zone.getControllingTeam(), 
                zone.getClaimingTeam(), Math.round(zone.getClaimProgress()));
        }
    }
    
    /**
     * Map chat formatting to boss bar colors
     */
    private BossEvent.BossBarColor getBossBarColorFromChatFormatting(ChatFormatting chatFormatting) {
        return switch (chatFormatting) {
            case RED -> BossEvent.BossBarColor.RED;
            case BLUE -> BossEvent.BossBarColor.BLUE;
            case GREEN -> BossEvent.BossBarColor.GREEN;
            case YELLOW -> BossEvent.BossBarColor.YELLOW;
            case LIGHT_PURPLE -> BossEvent.BossBarColor.PINK;
            case AQUA -> BossEvent.BossBarColor.BLUE;
            default -> BossEvent.BossBarColor.WHITE;
        };
    }
    
    /**
     * Broadcast message to all players
     */
    private void broadcastMessage(Component message) {
        for (Team team : arena.getTeams()) {
            for (UUID playerId : team.getPlayers()) {
                ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
                if (player != null) {
                    player.sendSystemMessage(message);
                }
            }
        }
    }
    
    /**
     * Send message to a specific team
     */
    private void notifyTeam(Team team, Component message) {
        for (UUID playerId : team.getPlayers()) {
            ServerPlayer player = ServerLifecycleHooks.getCurrentServer().getPlayerList().getPlayer(playerId);
            if (player != null) {
                player.sendSystemMessage(message);
            }
        }
    }
    
    @Override
    public void end() {
        setState(GameState.ENDING);
        endTime = System.currentTimeMillis();
        
        // Remove all boss bars
        for (ServerBossEvent bossBar : bossBars.values()) {
            bossBar.removeAllPlayers();
        }
        bossBars.clear();
        
        // Reset all checkpoint zones
        for (CheckpointZone zone : arena.getCheckpointZones()) {
            zone.setControllingTeam(null);
            zone.resetClaim();
        }
        
        // Reset player checkpoint counts
        for (PlayerData data : playerData.values()) {
            data.setMoneyCheckpoints(0);
            data.setUpgradeCheckpoints(0);
        }
        
        // Restore player data and reset arena
        restoreAllPlayerData();
        resetArena();
        
        setState(GameState.ENDED);
    }
}
