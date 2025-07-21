package com.mchivelli.machiavellianminigames.core;

import com.mchivelli.machiavellianminigames.MachiavellianMinigames;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySelector;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.fml.ModList;
import net.minecraftforge.server.ServerLifecycleHooks;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Handles arena restoration after games, including block resets and entity removal.
 */
public class ArenaResetter {

    /**
     * Reset an arena by cleaning up entities, including those from the Corpse mod if installed.
     * @param arena The arena to reset
     */
    public static void resetArena(Arena arena) {
        if (arena == null) {
            MachiavellianMinigames.LOGGER.error("Attempted to reset null arena");
            return;
        }

        MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
        if (server == null) {
            MachiavellianMinigames.LOGGER.error("Server not available for arena reset");
            return;
        }

        // Get level from dimension
        ResourceKey<Level> dimensionKey = ResourceKey.create(Registries.DIMENSION, arena.getDimension());
        ServerLevel level = server.getLevel(dimensionKey);
        if (level == null) {
            MachiavellianMinigames.LOGGER.error("Could not find level for dimension: {}", arena.getDimension());
            return;
        }

        // Check arena bounds
        if (arena.getPos1() == null || arena.getPos2() == null) {
            MachiavellianMinigames.LOGGER.error("Arena {} has invalid bounds", arena.getName());
            return;
        }

        MachiavellianMinigames.LOGGER.info("Resetting arena {}", arena.getName());

        // Clean up entities
        cleanEntities(arena, level);

        // Reset checkpoints
        resetCheckpoints(arena);

        // Set arena state to IDLE
        arena.setState(Arena.ArenaState.IDLE);

        MachiavellianMinigames.LOGGER.info("Arena {} has been reset successfully", arena.getName());
    }

    /**
     * Clean up all entities in an arena, including items, mobs, and mod-specific entities
     * @param arena The arena to clean
     * @param level The level containing the arena
     */
    private static void cleanEntities(Arena arena, ServerLevel level) {
        int minX = Math.min(arena.getPos1().getX(), arena.getPos2().getX());
        int minY = Math.min(arena.getPos1().getY(), arena.getPos2().getY());
        int minZ = Math.min(arena.getPos1().getZ(), arena.getPos2().getZ());
        int maxX = Math.max(arena.getPos1().getX(), arena.getPos2().getX());
        int maxY = Math.max(arena.getPos1().getY(), arena.getPos2().getY());
        int maxZ = Math.max(arena.getPos1().getZ(), arena.getPos2().getZ());

        // Create bounding box for the arena
        AABB boundingBox = new AABB(minX, minY, minZ, maxX + 1, maxY + 1, maxZ + 1);

        // Get all entities in the arena
        List<Entity> entities = level.getEntitiesOfClass(Entity.class, boundingBox, EntitySelector.ENTITY_STILL_ALIVE);
        
        // Track removed entities
        int removedEntities = 0;
        Set<String> entityTypes = new HashSet<>();

        // Remove all entities (except players)
        for (Entity entity : entities) {
            if (!entity.getType().getDescriptionId().equals("entity.minecraft.player")) {
                entityTypes.add(entity.getType().getDescriptionId());
                
                // Special handling for Corpse mod entities if installed
                boolean isCorpse = isCorpseEntity(entity);
                if (isCorpse) {
                    MachiavellianMinigames.LOGGER.debug("Removing Corpse entity at {}", entity.blockPosition());
                }
                
                // Remove entity
                entity.remove(Entity.RemovalReason.DISCARDED);
                removedEntities++;
            }
        }

        // Special check for dropped items
        List<ItemEntity> items = level.getEntitiesOfClass(ItemEntity.class, boundingBox);
        for (ItemEntity item : items) {
            item.remove(Entity.RemovalReason.DISCARDED);
            removedEntities++;
        }

        MachiavellianMinigames.LOGGER.info("Removed {} entities from arena {}: {}", 
            removedEntities, arena.getName(), String.join(", ", entityTypes));
    }

    /**
     * Reset all checkpoint zones in the arena
     * @param arena The arena to reset checkpoints in
     */
    private static void resetCheckpoints(Arena arena) {
        for (CheckpointZone zone : arena.getCheckpointZones()) {
            zone.resetClaim();
            // Controlling team is reset via reflection since there's no direct setter
            try {
                java.lang.reflect.Field field = CheckpointZone.class.getDeclaredField("controllingTeam");
                field.setAccessible(true);
                field.set(zone, null);
            } catch (Exception e) {
                MachiavellianMinigames.LOGGER.error("Failed to reset controlling team for checkpoint {}: {}", 
                    zone.getName(), e.getMessage());
            }
        }
        
        MachiavellianMinigames.LOGGER.info("Reset {} checkpoint zones", arena.getCheckpointZones().size());
    }

    /**
     * Check if an entity is from the Corpse mod
     * @param entity The entity to check
     * @return true if it's a Corpse mod entity
     */
    private static boolean isCorpseEntity(Entity entity) {
        // Check if Corpse mod is loaded first
        boolean corpseModLoaded = ModList.get().isLoaded("corpse");
        
        if (corpseModLoaded) {
            String entityId = entity.getEncodeId();
            // The corpse mod typically uses "corpse:corpse" as the entity ID
            return entityId != null && entityId.contains("corpse");
        }
        
        return false;
    }
}
