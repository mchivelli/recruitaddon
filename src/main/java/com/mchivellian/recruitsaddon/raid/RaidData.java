package com.mchivellian.recruitsaddon.raid;

import net.minecraft.core.BlockPos;

import java.util.List;
import java.util.UUID;

public class RaidData {
    
    public enum RaidPhase {
        MOVING_TO_TARGET,
        ENGAGING_TARGETS,
        LOOTING,
        RETURNING
    }
    
    private final UUID playerUuid;
    private final List<UUID> recruitIds;
    private final BlockPos targetPos;
    private final int raidType; // 0 = raid, 1 = assault
    private RaidPhase phase;
    private int lootingTime;
    
    public RaidData(UUID playerUuid, List<UUID> recruitIds, BlockPos targetPos, int raidType, RaidPhase phase) {
        this.playerUuid = playerUuid;
        this.recruitIds = recruitIds;
        this.targetPos = targetPos;
        this.raidType = raidType;
        this.phase = phase;
        this.lootingTime = 0;
    }
    
    public UUID getPlayerUuid() {
        return playerUuid;
    }
    
    public List<UUID> getRecruitIds() {
        return recruitIds;
    }
    
    public BlockPos getTargetPos() {
        return targetPos;
    }
    
    public int getRaidType() {
        return raidType;
    }
    
    public RaidPhase getPhase() {
        return phase;
    }
    
    public void setPhase(RaidPhase phase) {
        this.phase = phase;
    }
    
    public int getLootingTime() {
        return lootingTime;
    }
    
    public void incrementLootingTime() {
        this.lootingTime++;
    }
    
    public String getRaidTypeName() {
        return raidType == 0 ? "Raid" : "Assault";
    }
}
