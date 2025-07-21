package com.mchivellian.recruitsaddon.raid;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.mchivellian.recruitsaddon.config.RaidConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Advanced raid manager that handles sophisticated raid mechanics:
 * - March to destination in formation with neutral state
 * - Switch to aggressive when reaching destination
 * - Entity blacklist support
 * - Player notifications for various events
 */
@Mod.EventBusSubscriber
public class AdvancedRaidManager {
    
    // Active raid tracking
    private static final Map<UUID, RaidMission> activeRaids = new ConcurrentHashMap<>();
    private static final Map<UUID, Long> lastNotificationTime = new ConcurrentHashMap<>();
    private static final Map<UUID, BlockPos> lastKnownPositions = new ConcurrentHashMap<>();
    
    // Timing constants
    private static final int STUCK_CHECK_INTERVAL = 100; // 5 seconds
    private static final int NOTIFICATION_COOLDOWN = 60; // 3 seconds
    private static final int POSITION_CHECK_DISTANCE = 2; // blocks
    
    /**
     * Start a raid mission for a group of recruits
     */
    public static void startRaidMission(List<AbstractRecruitEntity> recruits, BlockPos destination, String formation, ServerPlayer commander) {
        UUID missionId = UUID.randomUUID();
        RaidMission mission = new RaidMission(missionId, recruits, destination, formation, commander);
        activeRaids.put(missionId, mission);
        
        // Initialize recruit tracking
        for (AbstractRecruitEntity recruit : recruits) {
            lastKnownPositions.put(recruit.getUUID(), recruit.blockPosition());
        }
        
        notifyPlayer(commander, "§aRaid mission started with " + recruits.size() + " recruits marching to " + 
                    destination.getX() + ", " + destination.getY() + ", " + destination.getZ());
    }
    
    /**
     * Cancel a raid mission
     */
    public static void cancelRaidMission(UUID missionId) {
        RaidMission mission = activeRaids.remove(missionId);
        if (mission != null) {
            // Reset all recruits to neutral and make them follow
            for (AbstractRecruitEntity recruit : mission.recruits) {
                if (recruit.isAlive()) {
                    recruit.setState(0); // Neutral
                    recruit.setFollowState(1); // Follow player
                }
            }
            notifyPlayer(mission.commander, "§cRaid mission cancelled. Recruits returning to you.");
        }
    }
    
    /**
     * Cancel all raid missions for a player
     */
    public static void cancelAllRaidMissions(ServerPlayer player) {
        List<UUID> toRemove = new ArrayList<>();
        for (Map.Entry<UUID, RaidMission> entry : activeRaids.entrySet()) {
            if (entry.getValue().commander.equals(player)) {
                toRemove.add(entry.getKey());
            }
        }
        for (UUID missionId : toRemove) {
            cancelRaidMission(missionId);
        }
    }
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        // Process all active raids
        Iterator<Map.Entry<UUID, RaidMission>> iterator = activeRaids.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, RaidMission> entry = iterator.next();
            RaidMission mission = entry.getValue();
            
            // Remove completed or invalid missions
            if (mission.isCompleted() || !mission.commander.isAlive()) {
                iterator.remove();
                continue;
            }
            
            // Update mission state
            updateRaidMission(mission);
        }
    }
    
    private static void updateRaidMission(RaidMission mission) {
        List<AbstractRecruitEntity> aliveRecruits = new ArrayList<>();
        List<AbstractRecruitEntity> atDestination = new ArrayList<>();
        
        // Check recruit status
        for (AbstractRecruitEntity recruit : mission.recruits) {
            if (recruit.isAlive()) {
                aliveRecruits.add(recruit);
                
                // Check if recruit reached destination
                double distance = recruit.blockPosition().distSqr(mission.destination);
                if (distance <= RaidConfig.getDestinationReachDistance() * RaidConfig.getDestinationReachDistance()) {
                    atDestination.add(recruit);
                }
                
                // Check if recruit is stuck
                checkIfRecruitStuck(recruit, mission.commander);
            }
        }
        
        // Update mission phase based on recruit positions
        if (mission.phase == RaidPhase.MARCHING && !atDestination.isEmpty()) {
            // Some recruits reached destination - switch to raid phase
            mission.phase = RaidPhase.RAIDING;
            
            // Switch recruits at destination to aggressive mode
            for (AbstractRecruitEntity recruit : atDestination) {
                recruit.setState(1); // Aggressive
                recruit.setFollowState(0); // Stop following, start attacking
            }
            
            notifyPlayer(mission.commander, "§c" + atDestination.size() + " recruits have reached the raid destination and are engaging enemies!");
        }
        
        // Handle raiding phase
        if (mission.phase == RaidPhase.RAIDING) {
            handleRaidingPhase(mission, atDestination);
        }
        
        // Check if mission should be completed
        if (aliveRecruits.isEmpty()) {
            notifyPlayer(mission.commander, "§4Raid mission failed - all recruits have been defeated!");
            mission.completed = true;
        }
    }
    
    private static void handleRaidingPhase(RaidMission mission, List<AbstractRecruitEntity> raiders) {
        if (raiders.isEmpty()) return;
        
        // Find hostile entities near the destination
        BlockPos dest = mission.destination;
        AABB searchArea = new AABB(dest).inflate(15.0); // 15 block radius
        
        List<LivingEntity> hostiles = new ArrayList<>();
        for (AbstractRecruitEntity recruit : raiders) {
            List<Entity> nearbyEntities = recruit.level().getEntities(recruit, searchArea);
            for (Entity entity : nearbyEntities) {
                if (entity instanceof LivingEntity living && isValidRaidTarget(living, recruit)) {
                    hostiles.add(living);
                }
            }
        }
        
        // Notify about combat if hostiles found
        if (!hostiles.isEmpty() && shouldNotify(mission.commander, "combat")) {
            notifyPlayer(mission.commander, "§eYour recruits are engaging " + hostiles.size() + " hostile entities!");
        }
    }
    
    private static boolean isValidRaidTarget(LivingEntity target, AbstractRecruitEntity recruit) {
        // Don't attack other recruits or the player
        if (target instanceof AbstractRecruitEntity || target.equals(recruit.getOwner())) {
            return false;
        }
        
        // Check entity blacklist
        String entityType = target.getType().toString();
        if (RaidConfig.isEntityBlacklisted(entityType)) {
            return false;
        }
        
        // Check specific blacklist settings
        if (target.getType().toString().contains("creeper") && RaidConfig.shouldAvoidCreepers()) {
            return false;
        }
        if (target.getType().toString().contains("enderman") && RaidConfig.shouldAvoidEndermen()) {
            return false;
        }
        if (target instanceof Villager && RaidConfig.shouldAvoidVillagers()) {
            return false;
        }
        
        // Target monsters and hostile entities
        return target instanceof Monster || target.getLastHurtByMob() != null;
    }
    
    private static void checkIfRecruitStuck(AbstractRecruitEntity recruit, ServerPlayer commander) {
        UUID recruitId = recruit.getUUID();
        BlockPos currentPos = recruit.blockPosition();
        BlockPos lastPos = lastKnownPositions.get(recruitId);
        
        if (lastPos != null) {
            double distance = currentPos.distSqr(lastPos);
            if (distance < POSITION_CHECK_DISTANCE * POSITION_CHECK_DISTANCE) {
                // Recruit hasn't moved much - might be stuck
                if (shouldNotify(commander, "stuck")) {
                    notifyPlayer(commander, "§eA recruit appears to be stuck at " + 
                               currentPos.getX() + ", " + currentPos.getY() + ", " + currentPos.getZ());
                }
            }
        }
        
        lastKnownPositions.put(recruitId, currentPos);
    }
    
    @SubscribeEvent
    public static void onRecruitDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof AbstractRecruitEntity recruit)) return;
        
        // Find the mission this recruit belongs to
        for (RaidMission mission : activeRaids.values()) {
            if (mission.recruits.contains(recruit)) {
                if (RaidConfig.NOTIFY_ON_RECRUIT_DEATH.get()) {
                    notifyPlayer(mission.commander, "§4A recruit has fallen in battle!");
                }
                break;
            }
        }
    }
    
    @SubscribeEvent
    public static void onRecruitHurt(LivingHurtEvent event) {
        if (!(event.getEntity() instanceof AbstractRecruitEntity recruit)) return;
        
        Entity attacker = event.getSource().getEntity();
        if (attacker == null) return;
        
        // Find the mission this recruit belongs to
        for (RaidMission mission : activeRaids.values()) {
            if (mission.recruits.contains(recruit) && mission.phase == RaidPhase.MARCHING) {
                // Recruit under attack during march - coordinate group retaliation
                if (shouldNotify(mission.commander, "combat")) {
                    notifyPlayer(mission.commander, "§eYour recruits are under attack while marching! Group is retaliating!");
                }
                
                // Make the entire group retaliate against the attacker
                coordinateGroupRetaliation(mission, attacker);
                break;
            }
        }
    }
    
    /**
     * Coordinate group retaliation during march - all recruits target attacker, then resume march
     */
    private static void coordinateGroupRetaliation(RaidMission mission, Entity attacker) {
        if (mission.phase != RaidPhase.MARCHING) return;
        
        // Store original march target for later resumption
        Vec3 originalTarget = Vec3.atCenterOf(mission.destination);
        
        for (AbstractRecruitEntity recruit : mission.recruits) {
            if (recruit.isAlive()) {
                try {
                    // Set attacker as target for retaliation (only if it's a living entity)
                    if (attacker instanceof LivingEntity livingAttacker) {
                        recruit.setTarget(livingAttacker);
                        recruit.setAggressive(true);
                        
                        // Clear current movement to focus on combat
                        recruit.setShouldMovePos(false);
                        recruit.getNavigation().stop();
                        
                        // Start pathfinding to attacker
                        recruit.getNavigation().moveTo(attacker, 1.5);
                    }
                    
                } catch (Exception e) {
                    // Individual recruit retaliation failed, continue with others
                }
            }
        }
        
        // Schedule resumption of march after combat (5 seconds delay)
        scheduleMarchResumption(mission, originalTarget);
    }
    
    /**
     * Schedule march resumption after combat delay
     */
    private static void scheduleMarchResumption(RaidMission mission, Vec3 originalTarget) {
        // Use server scheduler to resume march after 5 seconds (100 ticks)
        try {
            java.util.concurrent.CompletableFuture.runAsync(() -> {
                try {
                    Thread.sleep(5000); // 5 second delay
                    
                    // Check if mission is still active and in marching phase
                    if (activeRaids.containsValue(mission) && mission.phase == RaidPhase.MARCHING) {
                        resumeMarchAfterCombat(mission, originalTarget);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        } catch (Exception e) {
            // Scheduling failed, try immediate resumption
            resumeMarchAfterCombat(mission, originalTarget);
        }
    }
    
    /**
     * Resume march after combat delay
     */
    private static void resumeMarchAfterCombat(RaidMission mission, Vec3 originalTarget) {
        for (AbstractRecruitEntity recruit : mission.recruits) {
            if (recruit.isAlive()) {
                try {
                    // Clear combat targets and return to neutral march state
                    recruit.setTarget(null);
                    recruit.setAggressive(false);
                    recruit.setState(0); // Neutral state for marching
                    
                    // Resume march to original destination
                    BlockPos targetPos = new BlockPos((int) originalTarget.x, (int) originalTarget.y, (int) originalTarget.z);
                    recruit.setMovePos(targetPos);
                    recruit.setShouldMovePos(true);
                    recruit.setShouldFollow(false);
                    recruit.setShouldHoldPos(false);
                    
                    // Start pathfinding to resume march
                    recruit.getNavigation().moveTo(originalTarget.x, originalTarget.y, originalTarget.z, 1.0);
                    
                } catch (Exception e) {
                    // Individual recruit resumption failed, continue with others
                }
            }
        }
        
        // Notify player that march has resumed
        if (shouldNotify(mission.commander, "march_resume")) {
            notifyPlayer(mission.commander, "§aYour recruits have dealt with the threat and resumed marching!");
        }
    }
    
    private static boolean shouldNotify(ServerPlayer player, String type) {
        if (!RaidConfig.ENABLE_RAID_NOTIFICATIONS.get()) return false;
        
        long currentTime = System.currentTimeMillis();
        UUID key = UUID.nameUUIDFromBytes((player.getUUID() + "_" + type).getBytes());
        Long lastTime = lastNotificationTime.get(key);
        
        if (lastTime == null || currentTime - lastTime > NOTIFICATION_COOLDOWN * 1000) {
            lastNotificationTime.put(key, currentTime);
            return true;
        }
        return false;
    }
    
    private static void notifyPlayer(ServerPlayer player, String message) {
        if (player != null && player.isAlive()) {
            player.sendSystemMessage(Component.literal(message));
        }
    }
    
    /**
     * Represents an active raid mission
     */
    private static class RaidMission {
        final UUID id;
        final List<AbstractRecruitEntity> recruits;
        final BlockPos destination;
        final String formation;
        final ServerPlayer commander;
        RaidPhase phase;
        boolean completed;
        
        RaidMission(UUID id, List<AbstractRecruitEntity> recruits, BlockPos destination, String formation, ServerPlayer commander) {
            this.id = id;
            this.recruits = new ArrayList<>(recruits);
            this.destination = destination;
            this.formation = formation;
            this.commander = commander;
            this.phase = RaidPhase.MARCHING;
            this.completed = false;
        }
        
        boolean isCompleted() {
            return completed || recruits.stream().noneMatch(AbstractRecruitEntity::isAlive);
        }
    }
    
    /**
     * Phases of a raid mission
     */
    private enum RaidPhase {
        MARCHING,  // Moving to destination in formation, neutral state
        RAIDING    // At destination, aggressive state, attacking enemies
    }
}
