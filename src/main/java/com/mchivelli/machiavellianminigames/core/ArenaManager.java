package com.mchivelli.machiavellianminigames.core;

import com.mchivelli.machiavellianminigames.MachiavellianMinigames;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraftforge.server.ServerLifecycleHooks;

import javax.annotation.Nonnull;
import java.util.*;

public class ArenaManager extends SavedData {
    private static final String DATA_NAME = MachiavellianMinigames.MODID + "_arenas";
    
    private final Map<String, Arena> arenas = new HashMap<>();
    
    public ArenaManager() {
        super();
    }
    
    public static ArenaManager get() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return new ArenaManager();
        
        ServerLevel overworld = server.getLevel(ServerLevel.OVERWORLD);
        if (overworld == null) return new ArenaManager();
        
        return overworld.getDataStorage().computeIfAbsent(ArenaManager::load, ArenaManager::new, DATA_NAME);
    }
    
    public static ArenaManager load(CompoundTag tag) {
        ArenaManager manager = new ArenaManager();
        
        if (tag.contains("arenas")) {
            CompoundTag arenasTag = tag.getCompound("arenas");
            for (String arenaName : arenasTag.getAllKeys()) {
                Arena arena = Arena.fromNBT(arenasTag.getCompound(arenaName));
                manager.arenas.put(arenaName, arena);
            }
        }
        
        return manager;
    }
    
    @Override
    @Nonnull
    public CompoundTag save(@Nonnull CompoundTag tag) {
        CompoundTag arenasTag = new CompoundTag();
        for (Arena arena : arenas.values()) {
            arenasTag.put(arena.getName(), arena.toNBT());
        }
        tag.put("arenas", arenasTag);
        return tag;
    }
    
    public Arena createArena(String name, ResourceLocation dimension) {
        if (arenas.containsKey(name)) {
            return null; // Arena already exists
        }
        
        Arena arena = new Arena(name, dimension);
        arenas.put(name, arena);
        setDirty();
        return arena;
    }
    
    public boolean deleteArena(String name) {
        Arena arena = arenas.remove(name);
        if (arena != null) {
            // End any active game
            if (arena.getCurrentGame() != null) {
                arena.getCurrentGame().end();
            }
            setDirty();
            return true;
        }
        return false;
    }
    
    public Arena getArena(String name) {
        return arenas.get(name);
    }
    
    public Collection<Arena> getArenas() {
        return arenas.values();
    }
    
    public Arena getArenaAt(ServerPlayer player) {
        return getArenaAt(player.blockPosition(), player.level().dimension().location());
    }
    
    public Arena getArenaAt(BlockPos pos, ResourceLocation dimension) {
        for (Arena arena : arenas.values()) {
            if (arena.getDimension().equals(dimension) && arena.isInBounds(pos)) {
                return arena;
            }
        }
        return null;
    }
    
    public void loadArenas() {
        MachiavellianMinigames.LOGGER.info("Loaded {} arenas", arenas.size());
    }
    
    /**
     * Gets the arena containing the player's current position
     * @param pos The player's position
     * @param dimension The dimension resource location
     * @return The arena containing the player, or null if none
     */
    public Arena getArenaContainingPlayer(BlockPos pos, ResourceLocation dimension) {
        for (Arena arena : arenas.values()) {
            if (arena.getDimension().equals(dimension) && arena.isInBounds(pos)) {
                return arena;
            }
        }
        return null;
    }
    
    public void teleportPlayerToTeamSpawn(ServerPlayer player, Arena arena) {
        // Find the player's team
        Team playerTeam = null;
        for (Team team : arena.getTeams()) {
            if (team.hasPlayer(player.getUUID())) {
                playerTeam = team;
                break;
            }
        }
        
        if (playerTeam != null && playerTeam.getSpawnPoint() != null) {
            BlockPos spawnPoint = playerTeam.getSpawnPoint();
            MinecraftServer server = player.getServer();
            if (server != null) {
                ResourceKey<Level> dimensionKey = ResourceKey.create(net.minecraft.core.registries.Registries.DIMENSION, arena.getDimension());
                ServerLevel level = server.getLevel(dimensionKey);
                if (level != null) {
                    player.teleportTo(level, spawnPoint.getX() + 0.5, spawnPoint.getY(), spawnPoint.getZ() + 0.5, 
                                    player.getYRot(), player.getXRot());
                }
            }
        }
    }
    
    public boolean isPlayerInArena(ServerPlayer player) {
        return getArenaAt(player) != null;
    }
    
    public void checkArenaViolations() {
        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) return;
        
        // Check all players and teleport them if they're outside arena bounds during a game
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            Arena arena = getArenaAt(player);
            if (arena != null && arena.getState() == Arena.ArenaState.ACTIVE) {
                if (!arena.isInBounds(player.blockPosition())) {
                    teleportPlayerToTeamSpawn(player, arena);
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "§cTeleported back to spawn - you left the arena bounds!"));
                }
            }
        }
    }
}
