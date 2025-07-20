package com.mchivelli.machiavellianminigames.core;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;

import java.util.*;

public class Arena {
    private final String name;
    private final ResourceLocation dimension;
    private BlockPos pos1;
    private BlockPos pos2;
    private final Map<String, Team> teams;
    private Game currentGame;
    private ArenaState state;
    
    // Checkpoint zones for Storm the Front
    private final Map<String, CheckpointZone> checkpointZones;
    
    public enum ArenaState {
        IDLE,
        PREPARING,
        ACTIVE,
        ENDING
    }
    
    public Arena(String name, ResourceLocation dimension) {
        this.name = name;
        this.dimension = dimension;
        this.teams = new HashMap<>();
        this.checkpointZones = new HashMap<>();
        this.state = ArenaState.IDLE;
    }
    
    public void setBounds(BlockPos pos1, BlockPos pos2) {
        this.pos1 = pos1;
        this.pos2 = pos2;
    }
    
    public boolean isInBounds(BlockPos pos) {
        if (pos1 == null || pos2 == null) return false;
        
        int minX = Math.min(pos1.getX(), pos2.getX());
        int maxX = Math.max(pos1.getX(), pos2.getX());
        int minY = Math.min(pos1.getY(), pos2.getY());
        int maxY = Math.max(pos1.getY(), pos2.getY());
        int minZ = Math.min(pos1.getZ(), pos2.getZ());
        int maxZ = Math.max(pos1.getZ(), pos2.getZ());
        
        return pos.getX() >= minX && pos.getX() <= maxX &&
               pos.getY() >= minY && pos.getY() <= maxY &&
               pos.getZ() >= minZ && pos.getZ() <= maxZ;
    }
    
    public void addTeam(Team team) {
        teams.put(team.getName(), team);
    }
    
    public void removeTeam(String teamName) {
        teams.remove(teamName);
    }
    
    public Team getTeam(String name) {
        return teams.get(name);
    }
    
    public Collection<Team> getTeams() {
        return teams.values();
    }
    
    /**
     * @deprecated Use addCheckpointZone with type parameter instead
     */
    @Deprecated
    public void addCheckpointZone(String name, BlockPos pos1, BlockPos pos2) {
        addCheckpointZone(name, pos1, pos2, CheckpointZone.CheckpointType.MONEY);
    }
    
    /**
     * Add a checkpoint zone to this arena with a specific type
     * @param name Zone name
     * @param pos1 First corner position
     * @param pos2 Second corner position
     * @param type Checkpoint type (MONEY or UPGRADE) - determines rewards
     */
    public void addCheckpointZone(String name, BlockPos pos1, BlockPos pos2, CheckpointZone.CheckpointType type) {
        checkpointZones.put(name, new CheckpointZone(name, pos1, pos2, type));
    }
    
    public CheckpointZone getCheckpointZone(String name) {
        return checkpointZones.get(name);
    }
    
    public Collection<CheckpointZone> getCheckpointZones() {
        return checkpointZones.values();
    }
    
    // Getters and setters
    public String getName() { return name; }
    public ResourceLocation getDimension() { return dimension; }
    public BlockPos getPos1() { return pos1; }
    public BlockPos getPos2() { return pos2; }
    public Game getCurrentGame() { return currentGame; }
    public void setCurrentGame(Game game) { this.currentGame = game; }
    public ArenaState getState() { return state; }
    public void setState(ArenaState state) { this.state = state; }
    
    public boolean isConfigured() {
        return pos1 != null && pos2 != null && !teams.isEmpty();
    }
    
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putString("dimension", dimension.toString());
        
        if (pos1 != null) {
            tag.putLong("pos1", pos1.asLong());
        }
        if (pos2 != null) {
            tag.putLong("pos2", pos2.asLong());
        }
        
        // Save teams
        CompoundTag teamsTag = new CompoundTag();
        for (Team team : teams.values()) {
            teamsTag.put(team.getName(), team.toNBT());
        }
        tag.put("teams", teamsTag);
        
        // Save checkpoint zones
        CompoundTag checkpointsTag = new CompoundTag();
        for (CheckpointZone zone : checkpointZones.values()) {
            checkpointsTag.put(zone.getName(), zone.toNBT());
        }
        tag.put("checkpoints", checkpointsTag);
        
        return tag;
    }
    
    public static Arena fromNBT(CompoundTag tag) {
        String name = tag.getString("name");
        ResourceLocation dimension = new ResourceLocation(tag.getString("dimension"));
        Arena arena = new Arena(name, dimension);
        
        if (tag.contains("pos1")) {
            arena.pos1 = BlockPos.of(tag.getLong("pos1"));
        }
        if (tag.contains("pos2")) {
            arena.pos2 = BlockPos.of(tag.getLong("pos2"));
        }
        
        // Load teams
        if (tag.contains("teams")) {
            CompoundTag teamsTag = tag.getCompound("teams");
            for (String teamName : teamsTag.getAllKeys()) {
                Team team = Team.fromNBT(teamsTag.getCompound(teamName));
                arena.teams.put(teamName, team);
            }
        }
        
        // Load checkpoint zones
        if (tag.contains("checkpoints")) {
            CompoundTag checkpointsTag = tag.getCompound("checkpoints");
            for (String zoneName : checkpointsTag.getAllKeys()) {
                CheckpointZone zone = CheckpointZone.fromNBT(checkpointsTag.getCompound(zoneName));
                arena.checkpointZones.put(zoneName, zone);
            }
        }
        
        return arena;
    }
}
