package com.yourname.myforgemod.raid;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.yourname.myforgemod.ModMain;
import com.yourname.myforgemod.config.RaidConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Mod.EventBusSubscriber(modid = ModMain.MODID)
public class RaidManager {
    
    /**
     * Cool-down tracker so the player does not get chat-spam when recruits are attacked
     */
    private static final Map<UUID, Long> LAST_ATTACK_MESSAGE = new ConcurrentHashMap<>();
    private static final int ATTACK_MESSAGE_COOLDOWN_TICKS = 100; // 5 seconds
    
    private static final Map<UUID, RaidData> activeRaids = new ConcurrentHashMap<>();
    private static final double RECRUIT_SEARCH_RADIUS = 500; // Fixed: Double.MAX_VALUE breaks entity search
    private static final double TARGET_ARRIVAL_RADIUS = 8.0;
    private static final double COMBAT_ENGAGEMENT_RADIUS = 16.0;
    private static final double LOOT_SEARCH_RADIUS = 12.0;
    private static final int RAID_TICK_INTERVAL = 20; // Check every second
    private static int tickCounter = 0;
    
    public static boolean startRaid(ServerPlayer player, BlockPos targetPos, int groupId, int raidType) {
        if (player.level() instanceof ServerLevel serverLevel) {
            List<AbstractRecruitEntity> groupRecruits = findGroupRecruits(player, serverLevel, groupId);
            
            if (groupRecruits.isEmpty()) {
                player.sendSystemMessage(net.minecraft.network.chat.Component.literal("No recruits found in group " + groupId));
                return false;
            }
            
            // Stop any existing raid for this player
            stopRaid(player);
            
            List<UUID> recruitIds = new ArrayList<>();
            for (AbstractRecruitEntity recruit : groupRecruits) {
                recruitIds.add(recruit.getUUID());
                // Set recruits to move to target position
                setRecruitRaidTarget(recruit, targetPos, raidType);
            }
            
            RaidData raidData = new RaidData(
                player.getUUID(),
                recruitIds,
                targetPos,
                raidType,
                RaidData.RaidPhase.MOVING_TO_TARGET
            );
            
            activeRaids.put(player.getUUID(), raidData);
            
            String raidTypeName = (raidType == 0) ? "raid" : "assault";
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                "Warband mustered for " + raidTypeName + " (" + groupRecruits.size() + " strong) - advancing to (" +
                targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ() + ")"));
            
            return true;
        }
        return false;
    }
    
    public static void stopRaid(ServerPlayer player) {
        RaidData raidData = activeRaids.remove(player.getUUID());
        if (raidData != null) {
            // Reset recruit behavior
            if (player.level() instanceof ServerLevel serverLevel) {
                for (UUID recruitId : raidData.getRecruitIds()) {
                    AbstractRecruitEntity recruit = findRecruitById(serverLevel, recruitId);
                    if (recruit != null) {
                        resetRecruitBehavior(recruit);
                    }
                }
            }
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Warband recalled — mission aborted."));
        }
    }
    
    private static List<AbstractRecruitEntity> findGroupRecruits(ServerPlayer player, ServerLevel level, int groupId) {
        AABB searchArea = new AABB(player.blockPosition()).inflate(RECRUIT_SEARCH_RADIUS);
        List<AbstractRecruitEntity> allRecruits = level.getEntitiesOfClass(AbstractRecruitEntity.class, searchArea);
        
        List<AbstractRecruitEntity> groupRecruits = new ArrayList<>();
        for (AbstractRecruitEntity recruit : allRecruits) {
            if (recruit.getOwnerUUID() != null && 
                recruit.getOwnerUUID().equals(player.getUUID()) && 
                (groupId == -1 || recruit.getGroup() == groupId)) {
                groupRecruits.add(recruit);
            }
        }
        
        return groupRecruits;
    }
    
    private static void setRecruitRaidTarget(AbstractRecruitEntity recruit, BlockPos targetPos, int raidType) {
        // Clear any existing commands that might interfere
        recruit.clearHoldPos();
        recruit.setFollowState(0); // Set to wander/free state
        
        // Use FormationUtils to find a safe position on the surface
        BlockPos safePos = com.talhanation.recruits.util.FormationUtils.getPositionOrSurface(
            recruit.getCommandSenderWorld(), targetPos);
        
        // Set recruit to move to target position using proper movement system
        recruit.setMovePos(safePos);
        recruit.setShouldMovePos(true);
        recruit.setFollowState(6); // Move command
        
        // Set aggressive behavior for raids
        if (raidType == 0) { // Raid - more aggressive
            recruit.setAggressive(true);
        } else { // Assault - focused attack
            recruit.setAggressive(true);
        }
        
        ModMain.LOGGER.info("Set raid target for recruit {} to safe position {}", recruit.getUUID(), safePos);
    }
    
    private static AbstractRecruitEntity findRecruitById(ServerLevel level, UUID recruitId) {
        for (AbstractRecruitEntity recruit : level.getEntitiesOfClass(AbstractRecruitEntity.class, 
                new AABB(level.getWorldBorder().getMinX(), level.getMinBuildHeight(), level.getWorldBorder().getMinZ(),
                        level.getWorldBorder().getMaxX(), level.getMaxBuildHeight(), level.getWorldBorder().getMaxZ()))) {
            if (recruit.getUUID().equals(recruitId)) {
                return recruit;
            }
        }
        return null;
    }
    
    private static void resetRecruitBehavior(AbstractRecruitEntity recruit) {
        // Reset recruit to default behavior
        recruit.setFollowState(0); // Wander command
        recruit.setAggressive(false);
        recruit.setShouldMovePos(false);
        recruit.clearMovePos();
        recruit.setFollowState(0); // Reset to wander state
        
        ModMain.LOGGER.info("Reset behavior for recruit {}", recruit.getUUID());
    }
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        
        tickCounter++;
        if (tickCounter < RAID_TICK_INTERVAL) return;
        tickCounter = 0;
        
        if (activeRaids.isEmpty()) return;
        
        Iterator<Map.Entry<UUID, RaidData>> iterator = activeRaids.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<UUID, RaidData> entry = iterator.next();
            UUID playerUuid = entry.getKey();
            RaidData raidData = entry.getValue();
            
            // Find the player
            ServerPlayer player = null;
            for (ServerLevel level : event.getServer().getAllLevels()) {
                Player foundPlayer = level.getPlayerByUUID(playerUuid);
                if (foundPlayer instanceof ServerPlayer) {
                    player = (ServerPlayer) foundPlayer;
                    break;
                }
            }
            
            if (player == null) {
                iterator.remove();
                continue;
            }
            
            updateRaidProgress(player, raidData, iterator);
        }
    }
    
    private static void updateRaidProgress(ServerPlayer player, RaidData raidData, Iterator<Map.Entry<UUID, RaidData>> iterator) {
        ServerLevel level = (ServerLevel) player.level();
        List<AbstractRecruitEntity> activeRecruits = new ArrayList<>();
        
        // Find active recruits
        for (UUID recruitId : raidData.getRecruitIds()) {
            AbstractRecruitEntity recruit = findRecruitById(level, recruitId);
            if (recruit != null && recruit.isAlive()) {
                activeRecruits.add(recruit);
            }
        }
        
        if (activeRecruits.isEmpty()) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Warband lost — raid aborted."));
            iterator.remove();
            return;
        }
        
        switch (raidData.getPhase()) {
            case MOVING_TO_TARGET:
                handleMovingToTarget(player, raidData, activeRecruits, level);
                break;
            case ENGAGING_TARGETS:
                handleEngagingTargets(player, raidData, activeRecruits, level);
                break;
            case LOOTING:
                handleLooting(player, raidData, activeRecruits, level);
                break;
            case RETURNING:
                handleReturning(player, raidData, activeRecruits, level, iterator);
                break;
        }
    }
    
    private static void handleMovingToTarget(ServerPlayer player, RaidData raidData, List<AbstractRecruitEntity> recruits, ServerLevel level) {
        BlockPos targetPos = raidData.getTargetPos();
        boolean allArrived = true;
        
        // Check for lagging recruits and teleport them if needed
        teleportLaggingRecruits(recruits, targetPos, level, "raid");
        
        for (AbstractRecruitEntity recruit : recruits) {
            double distance = recruit.distanceToSqr(targetPos.getX(), targetPos.getY(), targetPos.getZ());
            if (distance > TARGET_ARRIVAL_RADIUS * TARGET_ARRIVAL_RADIUS) {
                allArrived = false;
                // Continue moving to target
                setRecruitRaidTarget(recruit, targetPos, raidData.getRaidType());
            }
        }
        
        if (allArrived) {
            raidData.setPhase(RaidData.RaidPhase.ENGAGING_TARGETS);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Warband has reached the battlefield — commence the assault!"));
        }
    }
    
    private static void handleEngagingTargets(ServerPlayer player, RaidData raidData, List<AbstractRecruitEntity> recruits, ServerLevel level) {
        BlockPos targetPos = raidData.getTargetPos();
        AABB combatArea = new AABB(targetPos).inflate(COMBAT_ENGAGEMENT_RADIUS);
        
        // Find hostile entities in the area
        List<LivingEntity> hostiles = level.getEntitiesOfClass(LivingEntity.class, combatArea, entity -> {
            return entity instanceof Monster || 
                   (raidData.getRaidType() == 0 && entity instanceof Villager); // Raids can target villagers
        });
        
        if (hostiles.isEmpty()) {
            raidData.setPhase(RaidData.RaidPhase.LOOTING);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Enemy routed — commence plunder."));
            return;
        }
        
        // Assign targets to recruits
        for (int i = 0; i < recruits.size() && i < hostiles.size(); i++) {
            AbstractRecruitEntity recruit = recruits.get(i);
            LivingEntity target = hostiles.get(i);
            recruit.setTarget(target);
        }
    }
    
    private static void handleLooting(ServerPlayer player, RaidData raidData, List<AbstractRecruitEntity> recruits, ServerLevel level) {
        // Simple looting phase - just wait a bit then return
        raidData.incrementLootingTime();
        
        if (raidData.getLootingTime() > 100) { // 5 seconds of looting
            raidData.setPhase(RaidData.RaidPhase.RETURNING);
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Spoils secured — returning to commander."));
            
            // Set recruits to return to player
            for (AbstractRecruitEntity recruit : recruits) {
                recruit.setFollowState(1); // Follow command
                recruit.setTarget(null);
            }
        }
    }
    
    private static void handleReturning(ServerPlayer player, RaidData raidData, List<AbstractRecruitEntity> recruits, ServerLevel level, Iterator<Map.Entry<UUID, RaidData>> iterator) {
        boolean allReturned = true;
        BlockPos playerPos = player.blockPosition();
        
        for (AbstractRecruitEntity recruit : recruits) {
            double distance = recruit.distanceToSqr(player.getX(), player.getY(), player.getZ());
            if (distance > 100) { // 10 block radius
                allReturned = false;
                // Command recruit to return to player using safe positioning
                BlockPos safePlayerPos = com.talhanation.recruits.util.FormationUtils.getPositionOrSurface(
                    level, playerPos);
                recruit.clearHoldPos();
                recruit.setMovePos(safePlayerPos);
                recruit.setShouldMovePos(true);
                recruit.setFollowState(6); // Move command
            }
        }
        
        if (allReturned) {
            // Reset all recruits to normal behavior
            for (AbstractRecruitEntity recruit : recruits) {
                resetRecruitBehavior(recruit);
            }
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("Warband returns victorious."));
            iterator.remove();
        }
    }

    /**
     * When any recruit in an active raid is hurt, command the whole raid group to attack the aggressor
     * and notify the commander with a short cooldown to prevent spam.
     */
    @SubscribeEvent
    public static void onRecruitHurt(net.minecraftforge.event.entity.living.LivingHurtEvent event) {
        if (!(event.getEntity() instanceof AbstractRecruitEntity victim)) return;
        net.minecraft.world.entity.Entity src = event.getSource().getEntity();
        if (!(src instanceof net.minecraft.world.entity.LivingEntity attacker)) return;

        UUID victimId = victim.getUUID();
        for (java.util.Map.Entry<java.util.UUID, RaidData> entry : activeRaids.entrySet()) {
            RaidData data = entry.getValue();
            if (!data.getRecruitIds().contains(victimId)) continue;

            // Order retaliation
            if (victim.level() instanceof net.minecraft.server.level.ServerLevel level) {
                for (UUID id : data.getRecruitIds()) {
                    AbstractRecruitEntity recruit = findRecruitById(level, id);
                    if (recruit != null && recruit.isAlive()) {
                        recruit.setTarget(attacker);
                    }
                }
            }

            // Cool-down gated commander message
            long now = victim.level().getGameTime();
            long last = LAST_ATTACK_MESSAGE.getOrDefault(entry.getKey(), 0L);
            if (now - last >= ATTACK_MESSAGE_COOLDOWN_TICKS) {
                LAST_ATTACK_MESSAGE.put(entry.getKey(), now);
                net.minecraft.server.level.ServerPlayer commander = victim.level().getServer().getPlayerList().getPlayer(entry.getKey());
                if (commander != null) {
                    commander.sendSystemMessage(net.minecraft.network.chat.Component.literal("Warband under attack — weapons free!"));
                }
            }
            break; // a recruit can belong to only one raid entry
        }
    }

    /**
     * Teleports recruits who have fallen too far behind the main group during movement phase.
     * Only applies during movement, not combat/looting phases.
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
                setRecruitRaidTarget(recruit, targetPos, 0); // Use default raid type for movement
                
                ModMain.LOGGER.info("Teleported lagging {} recruit {} to group position ({}, {}, {})", 
                    operationType, recruit.getUUID(), teleportPos.getX(), teleportPos.getY(), teleportPos.getZ());
            }
        }
    }
}
