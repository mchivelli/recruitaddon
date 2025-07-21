package com.mchivellian.recruitsaddon.march;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.mchivellian.recruitsaddon.ModMain;
import com.mchivellian.recruitsaddon.config.RaidConfig;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = ModMain.MODID)
public class MarchManager {
    private static final Map<UUID, MarchData> activeMarchData = new ConcurrentHashMap<>();
    private static final double RECRUIT_SEARCH_RADIUS = 500; // Fixed: Double.MAX_VALUE breaks entity search
    private static final double COMBAT_ENGAGEMENT_RADIUS = 16.0;
    private static final int MARCH_TICK_INTERVAL = 20; // Check every second
    private static int tickCounter = 0;
    
    public static boolean startMarch(Player player, BlockPos targetPosition) {
        if (player.level() instanceof ServerLevel serverLevel) {
            List<Entity> nearbyRecruits = findNearbyRecruits(player, serverLevel);
            
            if (nearbyRecruits.isEmpty()) {
                return false;
            }
            
            // Stop any existing march
            stopMarch(player);
            
            List<UUID> recruitIds = new ArrayList<>();
            for (Entity recruit : nearbyRecruits) {
                recruitIds.add(recruit.getUUID());
                // Set recruits to march formation and set target
                setRecruitMarchTarget(recruit, targetPosition);
            }
            
            MarchData marchData = new MarchData(
                player.getUUID(),
                recruitIds,
                player.blockPosition(),
                targetPosition
            );
            
            activeMarchData.put(player.getUUID(), marchData);
            ModMain.LOGGER.info("Started march for player {} with {} recruits to {}", 
                player.getName().getString(), recruitIds.size(), targetPosition);
            
            return true;
        }
        
        return false;
    }
    
    public static boolean stopMarch(Player player) {
        MarchData marchData = activeMarchData.remove(player.getUUID());
        if (marchData != null) {
            marchData.setActive(false);
            
            if (player.level() instanceof ServerLevel serverLevel) {
                // Set recruits to hold position
                for (UUID recruitId : marchData.getRecruitIds()) {
                    Entity recruit = serverLevel.getEntity(recruitId);
                    if (recruit != null) {
                        stopRecruit(recruit);
                    }
                }
            }
            
            ModMain.LOGGER.info("Stopped march for player {}", player.getName().getString());
            return true;
        }
        return false;
    }
    
    public static MarchData getMarchData(Player player) {
        return activeMarchData.get(player.getUUID());
    }
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase == TickEvent.Phase.END) {
            tickCounter++;
            if (tickCounter >= MARCH_TICK_INTERVAL) {
                tickCounter = 0;
                updateActiveMarchData();
            }
        }
    }
    
    private static void updateActiveMarchData() {
        Iterator<Map.Entry<UUID, MarchData>> iterator = activeMarchData.entrySet().iterator();
        
        while (iterator.hasNext()) {
            Map.Entry<UUID, MarchData> entry = iterator.next();
            MarchData marchData = entry.getValue();
            
            // Update march status
            if (updateMarchStatus(marchData)) {
                // March completed or should be removed
                iterator.remove();
            }
        }
    }
    
    /**
     * Tick update for a single active march. Handles arrival detection and final defensive posture.
     */
    private static boolean updateMarchStatus(MarchData marchData) {
        // If march was externally flagged inactive, remove it.
        if (!marchData.isActive()) {
            return true;
        }

        ServerLevel level = null;
        List<Entity> validRecruits = new ArrayList<>();

        // Fetch recruit entities and determine level from the first one
        for (UUID id : marchData.getRecruitIds()) {
            // If we don't yet have a level reference, try to resolve it via server-level iteration
            if (level == null) {
                for (ServerLevel srvLevel : net.minecraftforge.server.ServerLifecycleHooks.getCurrentServer().getAllLevels()) {
                    Entity maybe = srvLevel.getEntity(id);
                    if (maybe != null) {
                        level = srvLevel;
                        break;
                    }
                }
            }
            if (level == null) {
                continue;
            }
            Entity e = level.getEntity(id);
            if (e instanceof AbstractRecruitEntity recruit && recruit.isAlive()) {
                validRecruits.add(recruit);
            }
        }

        if (validRecruits.isEmpty()) {
            marchData.setActive(false);
            return true; // Nothing left to manage
        }

        // Check for lagging recruits and teleport them if needed (only during movement)
        List<AbstractRecruitEntity> recruitList = new ArrayList<>();
        for (Entity ent : validRecruits) {
            if (ent instanceof AbstractRecruitEntity recruit) {
                recruitList.add(recruit);
            }
        }
        teleportLaggingRecruits(recruitList, marchData.getTargetPosition(), level, "march");

        // Check if all recruits have reached the destination
        boolean allArrived = true;
        for (Entity ent : validRecruits) {
            double dx = ent.getX() - (marchData.getTargetPosition().getX() + 0.5);
            double dz = ent.getZ() - (marchData.getTargetPosition().getZ() + 0.5);
            double dy = ent.getY() - marchData.getTargetPosition().getY();
            if ((dx*dx + dy*dy + dz*dz) > 9) {
                allArrived = false;
                break;
            }
        }

        if (!allArrived) {
            return false; // Still marching
        }

        // All arrived – issue defensive posture once and mark completed
        if (!marchData.isCompleted()) {
            marchData.setCompleted(true);

            for (Entity ent : validRecruits) {
                AbstractRecruitEntity recruit = (AbstractRecruitEntity) ent;
                // Stop movement
                recruit.setShouldMovePos(false);
                recruit.clearMovePos();
                recruit.setFollowState(0);

                // Defensive (not fully aggressive)
                recruit.setAggressive(false);
                recruit.setState(2); // Guard/defensive

                raiseShieldIfPossible(recruit);
            }

            // Notify commander
            net.minecraft.server.level.ServerPlayer commander = level.getServer().getPlayerList().getPlayer(marchData.getPlayerId());
            if (commander != null) {
                commander.sendSystemMessage(net.minecraft.network.chat.Component.literal("Shield wall formed — hold the line."));
            }
        }

        // After arrival, keep marchData for a short time so retaliation handling could reference it; here we just mark inactive after posture set
        marchData.setActive(false);
        return true; // Remove on next tick
    }

    /**
     * Attempt to raise a recruit's shield using several possible method names.
     */
    private static void raiseShieldIfPossible(AbstractRecruitEntity recruit) {
        boolean done = false;
        String[] methods = {"setShieldUp", "setDefensive", "setGuarding", "setBlocking", "setUsedShield"};
        for (String m : methods) {
            if (done) break;
            try {
                java.lang.reflect.Method met = recruit.getClass().getMethod(m, boolean.class);
                met.invoke(recruit, true);
                done = true;
            } catch (Exception ignored) {}
        }
        if (!done) {
            try {
                if (recruit.getOffhandItem().getItem() instanceof net.minecraft.world.item.ShieldItem) {
                    recruit.startUsingItem(net.minecraft.world.InteractionHand.OFF_HAND);
                }
            } catch (Exception ignored) {}
        }
    }

    /**
     * Teleports recruits who have fallen too far behind the main group during movement phase.
     * Only applies during movement, not after arrival.
     */
    private static void teleportLaggingRecruits(List<AbstractRecruitEntity> recruits, BlockPos targetPos, ServerLevel level, String operationType) {
        if (!RaidConfig.ENABLE_TELEPORTATION.get() || recruits.size() <= 1) return; // Need at least 2 recruits to determine who's lagging
        
        // Calculate average position of all recruits
        double avgX = 0, avgY = 0, avgZ = 0;
        int validRecruits = 0;
        
        for (AbstractRecruitEntity recruit : recruits) {
            if (recruit.isAlive()) {
                avgX += recruit.getX();
                avgY += recruit.getY();
                avgZ += recruit.getZ();
                validRecruits++;
            }
        }
        
        if (validRecruits == 0) return;
        
        avgX /= validRecruits;
        avgY /= validRecruits;
        avgZ /= validRecruits;
        
        int lagDistance = RaidConfig.TELEPORTATION_DISTANCE_THRESHOLD.get();
        int progressDistance = RaidConfig.TELEPORTATION_PROGRESS_THRESHOLD.get();
        final double LAG_THRESHOLD = lagDistance * lagDistance; // Convert to squared for distance comparison
        final double MIN_PROGRESS_THRESHOLD = progressDistance * progressDistance; // Convert to squared
        
        // Check if the group has moved significantly from spawn (avoid teleporting at start)
        double groupProgressToTarget = (avgX - targetPos.getX()) * (avgX - targetPos.getX()) + 
                                     (avgZ - targetPos.getZ()) * (avgZ - targetPos.getZ());
        
        for (AbstractRecruitEntity recruit : recruits) {
            if (!recruit.isAlive()) continue;
            
            double distanceFromGroup = (recruit.getX() - avgX) * (recruit.getX() - avgX) + 
                                     (recruit.getY() - avgY) * (recruit.getY() - avgY) + 
                                     (recruit.getZ() - avgZ) * (recruit.getZ() - avgZ);
            
            // Only teleport if recruit is far from group AND group has made progress
            if (distanceFromGroup > LAG_THRESHOLD && groupProgressToTarget > MIN_PROGRESS_THRESHOLD) {
                // Find a safe position near the group center
                BlockPos teleportPos = com.talhanation.recruits.util.FormationUtils.getPositionOrSurface(
                    level, new BlockPos((int)avgX, (int)avgY, (int)avgZ));
                
                // Teleport the lagging recruit
                recruit.teleportTo(teleportPos.getX() + 0.5, teleportPos.getY(), teleportPos.getZ() + 0.5);
                recruit.clearMovePos();
                recruit.setShouldMovePos(false);
                
                // Reset their target to continue the mission
                setRecruitMarchTarget(recruit, targetPos);
                
                ModMain.LOGGER.info("Teleported lagging {} recruit {} to group position ({}, {}, {})", 
                    operationType, recruit.getUUID(), teleportPos.getX(), teleportPos.getY(), teleportPos.getZ());
            }
        }
    }
    
    private static List<Entity> findNearbyRecruits(Player player, ServerLevel level) {
        // Try to get selected recruits first, then nearby
        List<AbstractRecruitEntity> selectedRecruits = com.mchivellian.recruitsaddon.integration.RecruitsIntegration.getSelectedRecruits(player);
        if (!selectedRecruits.isEmpty()) {
            return new ArrayList<>(selectedRecruits);
        }
        List<AbstractRecruitEntity> nearbyRecruits = com.mchivellian.recruitsaddon.integration.RecruitsIntegration.findNearbyRecruits(player, 500);
        return new ArrayList<>(nearbyRecruits);
    }
    
    private static boolean isPlayerOwned(Entity entity, Player player) {
        // This would need proper Recruits mod integration to check ownership
        // Placeholder implementation
        return true;
    }
    
    private static void setRecruitMarchTarget(Entity recruit, BlockPos target) {
        // Use the new integration to send march command
        com.mchivellian.recruitsaddon.integration.RecruitsIntegration.sendMarchCommand(recruit, target);
    }
    
    private static void stopRecruit(Entity recruit) {
        // Send hold position command (march to current position)
        com.mchivellian.recruitsaddon.integration.RecruitsIntegration.sendMarchCommand(recruit, recruit.blockPosition());
    }
    
    private static List<Entity> findNearbyEnemies(Entity recruit, ServerLevel level) {
        List<Entity> enemies = new ArrayList<>();
        
        AABB searchArea = new AABB(
            recruit.getX() - COMBAT_ENGAGEMENT_RADIUS,
            recruit.getY() - 5,
            recruit.getZ() - COMBAT_ENGAGEMENT_RADIUS,
            recruit.getX() + COMBAT_ENGAGEMENT_RADIUS,
            recruit.getY() + 5,
            recruit.getZ() + COMBAT_ENGAGEMENT_RADIUS
        );
        
        List<Entity> entities = level.getEntities((Entity) null, searchArea, entity -> {
            if (!(entity instanceof LivingEntity)) return false;
            
            // Don't attack vanilla mobs during march
            String className = entity.getClass().getSimpleName().toLowerCase();
            if (isVanillaMob(className)) return false;
            
            // Check if entity is hostile to recruits
            return isHostileToRecruits(entity, recruit);
        });
        
        enemies.addAll(entities);
        return enemies;
    }
    
    private static boolean isVanillaMob(String className) {
        // List of vanilla Minecraft mobs to avoid attacking during march
        return className.contains("cow") || className.contains("pig") || className.contains("chicken") ||
               className.contains("sheep") || className.contains("horse") || className.contains("villager") ||
               className.contains("cat") || className.contains("dog") || className.contains("wolf");
    }
    
    private static boolean isHostileToRecruits(Entity entity, Entity recruit) {
        // This would need proper Recruits mod integration to determine hostility
        // Placeholder implementation
        return entity instanceof LivingEntity && !isVanillaMob(entity.getClass().getSimpleName().toLowerCase());
    }
} 
