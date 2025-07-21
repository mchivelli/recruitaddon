package com.mchivelli.machiavellianminigames.core;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import java.util.*;

public class Team {
    private final String name;
    private ChatFormatting color;
    private BlockPos spawnPoint;
    private final Set<UUID> players;
    private final Set<UUID> kings; // For king-based minigames
    
    public Team(String name, ChatFormatting color) {
        this.name = name;
        this.color = color;
        this.players = new HashSet<>();
        this.kings = new HashSet<>();
    }
    
    public void addPlayer(UUID playerId) {
        players.add(playerId);
    }
    
    public void removePlayer(UUID playerId) {
        players.remove(playerId);
    }
    
    public boolean hasPlayer(UUID playerId) {
        return players.contains(playerId);
    }
    
    public void addKing(UUID kingId) {
        kings.add(kingId);
    }
    
    public void removeKing(UUID kingId) {
        kings.remove(kingId);
    }
    
    public boolean hasKing(UUID kingId) {
        return kings.contains(kingId);
    }
    
    public String getFormattedName() {
        return color + name + ChatFormatting.RESET;
    }
    
    // Getters and setters
    public String getName() { return name; }
    public ChatFormatting getColor() { return color; }
    public void setColor(ChatFormatting color) { this.color = color; }
    public BlockPos getSpawnPoint() { return spawnPoint; }
    public void setSpawnPoint(BlockPos spawnPoint) { this.spawnPoint = spawnPoint; }
    public Set<UUID> getPlayers() { return new HashSet<>(players); }
    public Set<UUID> getKings() { return new HashSet<>(kings); }
    public int getPlayerCount() { return players.size(); }
    
    public CompoundTag toNBT() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putString("color", color.getName());
        
        if (spawnPoint != null) {
            tag.putLong("spawnPoint", spawnPoint.asLong());
        }
        
        // Save players
        ListTag playersTag = new ListTag();
        for (UUID playerId : players) {
            playersTag.add(StringTag.valueOf(playerId.toString()));
        }
        tag.put("players", playersTag);
        
        // Save kings
        ListTag kingsTag = new ListTag();
        for (UUID kingId : kings) {
            kingsTag.add(StringTag.valueOf(kingId.toString()));
        }
        tag.put("kings", kingsTag);
        
        return tag;
    }
    
    public static Team fromNBT(CompoundTag tag) {
        String name = tag.getString("name");
        ChatFormatting color = ChatFormatting.getByName(tag.getString("color"));
        if (color == null) color = ChatFormatting.WHITE;
        
        Team team = new Team(name, color);
        
        if (tag.contains("spawnPoint")) {
            team.spawnPoint = BlockPos.of(tag.getLong("spawnPoint"));
        }
        
        // Load players
        if (tag.contains("players")) {
            ListTag playersTag = tag.getList("players", 8); // 8 = STRING
            for (int i = 0; i < playersTag.size(); i++) {
                try {
                    UUID playerId = UUID.fromString(playersTag.getString(i));
                    team.players.add(playerId);
                } catch (IllegalArgumentException e) {
                    // Skip invalid UUIDs
                }
            }
        }
        
        // Load kings
        if (tag.contains("kings")) {
            ListTag kingsTag = tag.getList("kings", 8); // 8 = STRING
            for (int i = 0; i < kingsTag.size(); i++) {
                try {
                    UUID kingId = UUID.fromString(kingsTag.getString(i));
                    team.kings.add(kingId);
                } catch (IllegalArgumentException e) {
                    // Skip invalid UUIDs
                }
            }
        }
        
        return team;
    }
}
