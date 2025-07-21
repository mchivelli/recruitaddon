package com.mchivelli.machiavellianminigames.core;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;

public class CheckpointZone {
    public enum CheckpointType {
        MONEY,
        UPGRADE
    }
    
    private final String name;
    private final BlockPos pos1;
    private final BlockPos pos2;
    private String controllingTeam;
    private long claimStartTime;
    private String claimingTeam;
    private int claimProgress; // 0-100
    private CheckpointType checkpointType;
    
    // Timer fields for the capture process
    private static final long CLAIM_DURATION_MILLIS = 10000; // 10 seconds in milliseconds
    private static final long EXIT_RESET_DELAY_MILLIS = 3000; // 3 seconds before resetting progress
    private long claimEndTime;
    private long lastPlayerPresence; // Track when players were last in the zone
    
    // Pause functionality for contested zones
    private boolean isPaused = false;
    private long pausedTime = 0; // Total time spent paused
    private long pauseStartTime = 0; // When the current pause started
    
    public CheckpointZone(String name, BlockPos pos1, BlockPos pos2) {
        this(name, pos1, pos2, CheckpointType.MONEY);
    }
    
    public CheckpointZone(String name, BlockPos pos1, BlockPos pos2, CheckpointType type) {
        this.name = name;
        this.pos1 = pos1;
        this.pos2 = pos2;
        this.controllingTeam = null;
        this.claimStartTime = 0;
        this.claimingTeam = null;
        this.claimProgress = 0;
        this.checkpointType = type;
    }
    
    public boolean isInZone(BlockPos pos) {
        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());
        
        boolean inZone = pos.getX() >= minX && pos.getX() <= maxX &&
                      pos.getY() >= minY && pos.getY() <= maxY &&
                      pos.getZ() >= minZ && pos.getZ() <= maxZ;
                      
        // Log detailed position info for debugging
        // Debug logging removed to prevent spam
        
        return inZone;
    }
    
    /**
     * Start a claim attempt by a team
     * Sets up the 10-second timer for claiming
     * @param teamName The team attempting to claim this zone
     */
    public void startClaim(String teamName) {
        this.claimingTeam = teamName;
        this.claimStartTime = System.currentTimeMillis();
        this.claimEndTime = claimStartTime + CLAIM_DURATION_MILLIS; // 10 seconds from now
        this.claimProgress = 0;
    }
    
    /**
     * Reset the claiming process
     * Called when no teams are in zone or multiple teams contest it
     */
    public void resetClaim() {
        this.claimingTeam = null;
        this.claimStartTime = 0;
        this.claimEndTime = 0;
        this.claimProgress = 0;
        
        // Reset pause state
        this.isPaused = false;
        this.pausedTime = 0;
        this.pauseStartTime = 0;
    }
    
    /**
     * Update the last time a player from the claiming team was in this zone
     */
    public void updatePlayerPresence() {
        this.lastPlayerPresence = System.currentTimeMillis();
    }
    
    /**
     * Check if the claiming process should be reset due to player absence
     * @param currentTime Current system time in milliseconds
     * @return True if the claim should be reset due to timeout
     */
    public boolean shouldResetDueToPlayerAbsence(long currentTime) {
        // Only check for player absence if there's an active claim
        if (isBeingClaimed()) {
            return (currentTime - lastPlayerPresence) > EXIT_RESET_DELAY_MILLIS;
        }
        return false;
    }
    
    /**
     * Complete the claim process
     * Called when a team successfully claims the zone
     */
    public void completeClaim() {
        this.controllingTeam = this.claimingTeam;
        resetClaim();
    }
    
    /**
     * Get the remaining time in milliseconds until this zone is captured
     * @return Milliseconds remaining, or 0 if not being claimed
     */
    public long getRemainingClaimTimeMillis() {
        if (!isBeingClaimed()) return 0;
        
        long currentTime = System.currentTimeMillis();
        long remaining = claimEndTime - currentTime;
        return Math.max(0, remaining);
    }
    
    /**
     * Get the claim progress as a percentage (0-100)
     * @return Claim progress percentage
     */
    public float getClaimPercentage() {
        if (!isBeingClaimed()) return 0;
        
        long currentTime = System.currentTimeMillis();
        long elapsed = currentTime - claimStartTime;
        
        // Subtract paused time from elapsed time
        long totalPausedTime = pausedTime;
        if (isPaused && pauseStartTime > 0) {
            totalPausedTime += (currentTime - pauseStartTime);
        }
        
        long effectiveElapsed = elapsed - totalPausedTime;
        float percentage = (float)effectiveElapsed / CLAIM_DURATION_MILLIS * 100;
        return Math.min(100, Math.max(0, percentage));
    }
    
    /**
     * Pause the claim progress (for contested zones)
     */
    public void pauseClaim() {
        if (!isPaused && isBeingClaimed()) {
            isPaused = true;
            pauseStartTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Resume the claim progress (when no longer contested)
     */
    public void resumeClaim() {
        if (isPaused) {
            if (pauseStartTime > 0) {
                pausedTime += (System.currentTimeMillis() - pauseStartTime);
            }
            isPaused = false;
            pauseStartTime = 0;
        }
    }
    
    /**
     * Check if the claim is currently paused
     * @return true if paused, false otherwise
     */
    public boolean isClaimPaused() {
        return isPaused;
    }
    
    /**
     * Update the claim progress based on elapsed time
     * @param currentTime The current time in milliseconds
     */
    public void updateClaimProgress(long currentTime) {
        if (claimingTeam == null) {
            return;
        }
        
        // Calculate claim progress percentage
        float percentage = getClaimPercentage();
        this.claimProgress = (int)percentage;
        
        // Check if enough time has passed for complete capture
        if (currentTime - claimStartTime >= CLAIM_DURATION_MILLIS) {
            // Complete the claim - this sets controlling team to claiming team
            completeClaim();
            // Checkpoint captured - logging handled by game logic
        } else {
            // Progress logging handled by game logic
        }
    }
    
    // Getters
    public String getName() { return name; }
    public BlockPos getPos1() { return pos1; }
    public BlockPos getPos2() { return pos2; }
    public String getControllingTeam() { return controllingTeam; }
    public String getClaimingTeam() { return claimingTeam; }
    public int getClaimProgress() { return claimProgress; }
    public long getClaimStartTime() { return claimStartTime; }
    public CheckpointType getCheckpointType() { return checkpointType; }
    public void setCheckpointType(CheckpointType type) { this.checkpointType = type; }
    public void setControllingTeam(String team) { this.controllingTeam = team; }
    
    public boolean isBeingClaimed() { return claimingTeam != null; }
    public boolean isControlled() { return controllingTeam != null; }
    
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putLong("pos1", pos1.asLong());
        tag.putLong("pos2", pos2.asLong());
        tag.putInt("checkpointType", checkpointType.ordinal());
        
        if (controllingTeam != null) {
            tag.putString("controllingTeam", controllingTeam);
        }
        if (claimingTeam != null) {
            tag.putString("claimingTeam", claimingTeam);
            tag.putLong("claimStartTime", claimStartTime);
            tag.putInt("claimProgress", claimProgress);
        }
        
        return tag;
    }
    
    public static CheckpointZone fromNBT(CompoundTag tag) {
        String name = tag.getString("name");
        BlockPos pos1 = BlockPos.of(tag.getLong("pos1"));
        BlockPos pos2 = BlockPos.of(tag.getLong("pos2"));
        
        CheckpointType type = CheckpointType.values()[tag.contains("checkpointType") ? 
            tag.getInt("checkpointType") : CheckpointType.MONEY.ordinal()];
        CheckpointZone zone = new CheckpointZone(name, pos1, pos2, type);
        
        if (tag.contains("controllingTeam")) {
            zone.controllingTeam = tag.getString("controllingTeam");
        }
        if (tag.contains("claimingTeam")) {
            zone.claimingTeam = tag.getString("claimingTeam");
            zone.claimStartTime = tag.getLong("claimStartTime");
            zone.claimProgress = tag.getInt("claimProgress");
        }
        
        return zone;
    }
}
