package com.mchivellian.recruitsaddon.march;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;

public class MarchData {
    private final UUID playerId;
    private final List<UUID> recruitIds;
    private List<Entity> recruitEntities; // Store actual entities for faster access
    private final BlockPos startPosition;
    private final BlockPos targetPosition;
    private BlockPos currentPosition;
    private boolean isEngaged;
    private boolean isActive;
    private boolean isCompleted;
    private long startTime;
    private int aliveRecruits;
    private int progressPercentage;
    
    public MarchData(UUID playerId, List<UUID> recruitIds, BlockPos startPosition, BlockPos targetPosition) {
        this.playerId = playerId;
        this.recruitIds = recruitIds;
        this.recruitEntities = new ArrayList<>(); // Initialize empty, will be set later
        this.startPosition = startPosition;
        this.targetPosition = targetPosition;
        this.currentPosition = startPosition;
        this.isEngaged = false;
        this.isActive = true;
        this.isCompleted = false;
        this.startTime = System.currentTimeMillis();
        this.aliveRecruits = recruitIds.size();
        this.progressPercentage = 0;
    }
    
    public UUID getPlayerId() {
        return playerId;
    }
    
    public List<UUID> getRecruitIds() {
        return recruitIds;
    }
    
    public List<Entity> getRecruitEntities() {
        return recruitEntities;
    }
    
    public void setRecruitEntities(List<Entity> recruitEntities) {
        this.recruitEntities = recruitEntities;
    }
    
    public BlockPos getStartPosition() {
        return startPosition;
    }
    
    public BlockPos getTargetPosition() {
        return targetPosition;
    }
    
    public BlockPos getCurrentPosition() {
        return currentPosition;
    }
    
    public void setCurrentPosition(BlockPos currentPosition) {
        this.currentPosition = currentPosition;
    }
    
    public boolean isEngaged() {
        return isEngaged;
    }
    
    public void setEngaged(boolean engaged) {
        this.isEngaged = engaged;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        this.isActive = active;
    }
    
    public boolean isCompleted() {
        return isCompleted;
    }
    
    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public int getAliveRecruits() {
        return aliveRecruits;
    }
    
    public void setAliveRecruits(int aliveRecruits) {
        this.aliveRecruits = aliveRecruits;
    }
    
    public int getProgressPercentage() {
        return progressPercentage;
    }
    
    public void setProgressPercentage(int progressPercentage) {
        this.progressPercentage = Math.max(0, Math.min(100, progressPercentage));
    }
    
    public int calculateProgressPercentage() {
        double totalDistance = startPosition.distSqr(targetPosition);
        double remainingDistance = currentPosition.distSqr(targetPosition);
        
        if (totalDistance == 0) return 100;
        
        double progress = 1.0 - (remainingDistance / totalDistance);
        return Math.max(0, Math.min(100, (int) (progress * 100)));
    }
    
    public double getDistanceToTarget() {
        return Math.sqrt(currentPosition.distSqr(targetPosition));
    }
    
    public boolean hasReachedTarget() {
        return currentPosition.distSqr(targetPosition) <= 9; // Within 3 blocks
    }
} 
