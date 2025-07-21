package com.mchivelli.machiavellianminigames.minigames;

import com.mchivelli.machiavellianminigames.core.CheckpointZone;

/**
 * Isolated checkpoint data for Storm the Front game
 * Manages checkpoint state without relying on base CheckpointZone complex logic
 */
public class StormCheckpointData {
    
    public enum State {
        UNCLAIMED,      // No one controls it
        CLAIMING,       // Team is claiming (0-100% progress)
        CLEARING,       // Team is clearing enemy control (0-100% progress)  
        CONTROLLED,     // Team fully controls it
        CONTESTED       // Multiple teams present (paused)
    }
    
    private final String name;
    private final CheckpointZone.CheckpointType type;
    private final CheckpointZone zone; // Reference for position checking only
    
    private State state;
    private String controllingTeam;
    private String claimingTeam;
    private float progress;           // 0.0 to 100.0
    private long stateStartTime;
    private boolean isPaused;
    private long pausedTime;         // Total time spent paused
    private long pauseStartTime;     // When current pause started
    private long lastPlayerPresence; // Last time claiming team was present
    
    // Configuration
    private static final long CLAIM_DURATION_MS = 10000; // 10 seconds
    private static final long ABANDON_TIMEOUT_MS = 3000; // 3 seconds
    
    public StormCheckpointData(String name, CheckpointZone.CheckpointType type, CheckpointZone zone) {
        this.name = name;
        this.type = type;
        this.zone = zone;
        this.state = State.UNCLAIMED;
        this.controllingTeam = null;
        this.claimingTeam = null;
        this.progress = 0.0F;
        this.stateStartTime = 0;
        this.isPaused = false;
        this.pausedTime = 0;
        this.pauseStartTime = 0;
        this.lastPlayerPresence = 0;
    }
    
    /**
     * Start claiming process for a team
     */
    public void startClaiming(String teamName, long currentTime) {
        this.state = State.CLAIMING;
        this.claimingTeam = teamName;
        this.progress = 0.0F;
        this.stateStartTime = currentTime;
        this.lastPlayerPresence = currentTime;
        this.isPaused = false;
        this.pausedTime = 0;
        this.pauseStartTime = 0;
    }
    
    /**
     * Start clearing process (attacking enemy-controlled checkpoint)
     */
    public void startClearing(String attackingTeam, long currentTime) {
        this.state = State.CLEARING;
        this.claimingTeam = attackingTeam;
        this.progress = 0.0F;
        this.stateStartTime = currentTime;
        this.lastPlayerPresence = currentTime;
        this.isPaused = false;
        this.pausedTime = 0;
        this.pauseStartTime = 0;
    }
    
    /**
     * Complete the current action (claiming or clearing)
     */
    public void completeAction(long currentTime) {
        if (state == State.CLEARING) {
            // Clearing complete - start claiming for attacking team
            this.controllingTeam = null;
            startClaiming(this.claimingTeam, currentTime);
        } else if (state == State.CLAIMING) {
            // Claiming complete - set as controlled
            this.controllingTeam = this.claimingTeam;
            this.claimingTeam = null;
            this.state = State.CONTROLLED;
            this.progress = 100.0F;
        }
    }
    
    /**
     * Set contested state when multiple teams are present
     */
    public void setContested() {
        if (state == State.CLAIMING || state == State.CLEARING) {
            pauseProgress();
            this.state = State.CONTESTED;
        }
    }
    
    /**
     * Resume from contested state
     */
    public void resumeFromContested(String teamName, long currentTime) {
        if (state == State.CONTESTED) {
            resumeProgress();
            if (controllingTeam == null) {
                this.state = State.CLAIMING;
            } else if (!teamName.equals(controllingTeam)) {
                this.state = State.CLEARING;
            } else {
                this.state = State.CLAIMING;
            }
            this.claimingTeam = teamName;
            this.lastPlayerPresence = currentTime;
        }
    }
    
    /**
     * Update progress based on time elapsed
     */
    public void updateProgress(long currentTime) {
        if (state != State.CLAIMING && state != State.CLEARING) {
            return;
        }
        
        if (isPaused) {
            return;
        }
        
        long elapsed = currentTime - stateStartTime;
        long totalPausedTime = pausedTime;
        if (isPaused && pauseStartTime > 0) {
            totalPausedTime += (currentTime - pauseStartTime);
        }
        
        long effectiveElapsed = elapsed - totalPausedTime;
        this.progress = Math.min(100.0F, Math.max(0.0F, (float)effectiveElapsed / CLAIM_DURATION_MS * 100.0F));
    }
    
    /**
     * Update player presence timestamp
     */
    public void updatePlayerPresence(long currentTime) {
        this.lastPlayerPresence = currentTime;
    }
    
    /**
     * Check if should reset due to player abandonment
     */
    public boolean shouldResetDueToAbandonment(long currentTime) {
        if (state == State.CLAIMING || state == State.CLEARING) {
            return (currentTime - lastPlayerPresence) > ABANDON_TIMEOUT_MS;
        }
        return false;
    }
    
    /**
     * Reset to unclaimed state
     */
    public void resetToUnclaimed() {
        this.state = State.UNCLAIMED;
        this.controllingTeam = null;
        this.claimingTeam = null;
        this.progress = 0.0F;
        this.stateStartTime = 0;
        this.isPaused = false;
        this.pausedTime = 0;
        this.pauseStartTime = 0;
        this.lastPlayerPresence = 0;
    }
    
    /**
     * Pause progress during contested state
     */
    private void pauseProgress() {
        if (!isPaused && (state == State.CLAIMING || state == State.CLEARING)) {
            isPaused = true;
            pauseStartTime = System.currentTimeMillis();
        }
    }
    
    /**
     * Resume progress after contested state ends
     */
    private void resumeProgress() {
        if (isPaused) {
            if (pauseStartTime > 0) {
                pausedTime += (System.currentTimeMillis() - pauseStartTime);
            }
            isPaused = false;
            pauseStartTime = 0;
        }
    }
    
    /**
     * Check if progress is complete (100%)
     */
    public boolean isComplete() {
        return progress >= 100.0F;
    }
    
    /**
     * Complete the capture process and set controlling team
     */
    public void completeCapture(String capturingTeam) {
        this.state = State.CONTROLLED;
        this.controllingTeam = capturingTeam;
        this.claimingTeam = null;
        this.progress = 100.0F;
        this.stateStartTime = System.currentTimeMillis();
        this.isPaused = false;
        this.pausedTime = 0;
        this.pauseStartTime = 0;
    }
    
    /**
     * Get progress as percentage (0-100)
     */
    public float getProgressPercentage() {
        return progress;
    }
    
    // Getters
    public String getName() { return name; }
    public CheckpointZone.CheckpointType getType() { return type; }
    public CheckpointZone getZone() { return zone; }
    public State getState() { return state; }
    public String getControllingTeam() { return controllingTeam; }
    public String getClaimingTeam() { return claimingTeam; }
    public float getProgress() { return progress; }
    public boolean isPaused() { return isPaused; }
    public long getStateStartTime() { return stateStartTime; }
}
