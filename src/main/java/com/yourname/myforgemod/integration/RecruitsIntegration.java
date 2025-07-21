package com.yourname.myforgemod.integration;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.util.FormationUtils;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.fml.ModList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Integration class for interacting with the Recruits mod
 * Provides methods to send recruits to coordinates while maintaining formations
 * or marching them to specific positions
 */
public class RecruitsIntegration {
    
    private static final String RECRUITS_MOD_ID = "recruits";
    private static final double DEFAULT_SEARCH_RADIUS = 100.0;
    
    /**
     * Check if the Recruits mod is loaded
     */
    public static boolean isRecruitsLoaded() {
        return ModList.get().isLoaded(RECRUITS_MOD_ID);
    }
    
    /**
     * Get all recruits owned by a player within a certain radius (Player version)
     */
    public static List<AbstractRecruitEntity> getPlayerRecruits(Player player, double radius) {
        if (player instanceof ServerPlayer serverPlayer) {
            return getPlayerRecruits(serverPlayer, radius);
        }
        return new ArrayList<>();
    }
    
    /**
     * Get all recruits owned by a player within a certain radius (ServerPlayer version)
     */
    public static List<AbstractRecruitEntity> getPlayerRecruits(ServerPlayer player, double radius) {
        if (!isRecruitsLoaded()) {
            return List.of();
        }
        
        Level level = player.level();
        AABB searchArea = player.getBoundingBox().inflate(radius);
        
        return level.getEntitiesOfClass(AbstractRecruitEntity.class, searchArea)
                .stream()
                .filter(recruit -> recruit.getOwnerUUID() != null && recruit.getOwnerUUID().equals(player.getUUID()))
                .collect(Collectors.toList());
    }
    
    /**
     * Get all recruits owned by a player within default radius
     */
    public static List<AbstractRecruitEntity> getPlayerRecruits(ServerPlayer player) {
        return getPlayerRecruits(player, DEFAULT_SEARCH_RADIUS);
    }
    
    /**
     * Get recruits in a specific group owned by a player
     * NOW PROPERLY FILTERS BY GROUP using official mod's isEffectedByCommand method
     */
    public static List<AbstractRecruitEntity> getPlayerRecruitsInGroup(ServerPlayer player, int group, double radius) {
        if (!isRecruitsLoaded()) {
            return List.of();
        }
        
        // FIXED: Use reasonable search radius instead of Double.MAX_VALUE which breaks entity search
        Level level = player.level();
        AABB searchArea = player.getBoundingBox().inflate(500);
        
        List<AbstractRecruitEntity> groupRecruits = level.getEntitiesOfClass(AbstractRecruitEntity.class, searchArea)
                .stream()
                .filter(recruit -> {
                    // Use official mod's filtering method to match EXACTLY what the official mod does
                    return recruit.isEffectedByCommand(player.getUUID(), group);
                })
                .collect(Collectors.toList());
        
        System.out.println("DEBUG EMERGENCY: Found " + groupRecruits.size() + " recruits for player " + player.getName().getString() + " in group " + group);
        for (AbstractRecruitEntity recruit : groupRecruits) {
            System.out.println("DEBUG EMERGENCY: Recruit: " + recruit.getName().getString() + " Group: " + recruit.getGroup());
        }
        
        return groupRecruits;
    }
    
    /**
     * Get recruits in a specific group owned by a player within default radius
     */
    public static List<AbstractRecruitEntity> getPlayerRecruitsInGroup(ServerPlayer player, int group) {
        return getPlayerRecruitsInGroup(player, group, DEFAULT_SEARCH_RADIUS);
    }
    
    /**
     * Send recruits to specific coordinates while maintaining their formation
     * @param recruits List of recruits to move
     * @param targetPos Target position to move to
     * @param maintainFormation Whether to maintain formation during movement
     */
    public static void sendRecruitsToCoordinates(List<AbstractRecruitEntity> recruits, BlockPos targetPos, boolean maintainFormation) {
        sendRecruitsToCoordinates(recruits, new Vec3(targetPos.getX(), targetPos.getY(), targetPos.getZ()), maintainFormation);
    }
    
    /**
     * Send recruits to specific coordinates with optional formation maintenance (Vec3 version)
     */
    public static void sendRecruitsToCoordinates(List<AbstractRecruitEntity> recruits, Vec3 targetPos, boolean maintainFormation) {
        if (!isRecruitsLoaded() || recruits.isEmpty()) {
            return;
        }
        
        if (maintainFormation) {
            // Calculate formation positions around the target
            applyFormationAtPosition(recruits, targetPos, FormationType.SQUARE);
        } else {
            // Send all recruits to the same position
            for (AbstractRecruitEntity recruit : recruits) {
                setRecruitMovePosition(recruit, targetPos);
            }
        }
    }
    
    /**
     * March recruits to a position in formation
     * @param recruits List of recruits to march
     * @param targetPos Target position
     * @param formationType Type of formation to use
     */
    public static void marchRecruitsToPosition(List<AbstractRecruitEntity> recruits, Vec3 targetPos, FormationType formationType) {
        if (!isRecruitsLoaded() || recruits.isEmpty()) {
            return;
        }
        
        applyFormationAtPosition(recruits, targetPos, formationType);
    }
    
    /**
     * Send all recruits owned by a player to coordinates
     */
    public static void sendAllRecruitsToCoordinates(ServerPlayer player, Vec3 targetPos, boolean maintainFormation) {
        List<AbstractRecruitEntity> recruits = getPlayerRecruits(player);
        sendRecruitsToCoordinates(recruits, targetPos, maintainFormation);
    }
    
    /**
     * Send recruits in a specific group to coordinates
     */
    public static void sendGroupToCoordinates(ServerPlayer player, int group, Vec3 targetPos, boolean maintainFormation) {
        List<AbstractRecruitEntity> recruits = getPlayerRecruitsInGroup(player, group);
        sendRecruitsToCoordinates(recruits, targetPos, maintainFormation);
    }
    
    /**
     * Make recruits hold their current positions
     */
    public static void makeRecruitsHoldPosition(List<AbstractRecruitEntity> recruits) {
        if (!isRecruitsLoaded()) {
            return;
        }
        
        for (AbstractRecruitEntity recruit : recruits) {
            recruit.setShouldHoldPos(true);
            recruit.setShouldFollow(false);
            recruit.setShouldMovePos(false);
        }
    }
    
    /**
     * Make recruits follow the player
     */
    public static void makeRecruitsFollow(List<AbstractRecruitEntity> recruits) {
        if (!isRecruitsLoaded()) {
            return;
        }
        
        for (AbstractRecruitEntity recruit : recruits) {
            recruit.setShouldFollow(true);
            recruit.setShouldHoldPos(false);
            recruit.setShouldMovePos(false);
        }
    }
    
    /**
     * Stop all movement commands for recruits
     */
    public static void stopRecruits(List<AbstractRecruitEntity> recruits) {
        if (!isRecruitsLoaded()) {
            return;
        }
        
        for (AbstractRecruitEntity recruit : recruits) {
            recruit.setShouldFollow(false);
            recruit.setShouldHoldPos(false);
            recruit.setShouldMovePos(false);
        }
    }
    
    /**
     * Set a recruit's move position
     */
    private static void setRecruitMovePosition(AbstractRecruitEntity recruit, Vec3 position) {
        BlockPos blockPos = new BlockPos((int) position.x, (int) position.y, (int) position.z);
        
        // Find a safe position on the surface if needed
        if (recruit.level() instanceof ServerLevel serverLevel) {
            blockPos = FormationUtils.getPositionOrSurface(serverLevel, blockPos);
        }
        
        recruit.setMovePos(blockPos);
        recruit.setShouldMovePos(true);
        recruit.setShouldFollow(false);
        recruit.setShouldHoldPos(false);
    }
    
    /**
     * Apply formation at a specific position
     */
    private static void applyFormationAtPosition(List<AbstractRecruitEntity> recruits, Vec3 centerPos, FormationType formationType) {
        if (recruits.isEmpty()) {
            return;
        }
        
        List<Vec3> positions = calculateFormationPositions(recruits.size(), centerPos, formationType);
        
        for (int i = 0; i < Math.min(recruits.size(), positions.size()); i++) {
            setRecruitMovePosition(recruits.get(i), positions.get(i));
        }
    }
    
    /**
     * Calculate formation positions based on formation type
     */
    private static List<Vec3> calculateFormationPositions(int recruitCount, Vec3 centerPos, FormationType formationType) {
        List<Vec3> positions = new java.util.ArrayList<>();
        
        switch (formationType) {
            case LINE -> {
                // Arrange recruits in a line
                double spacing = 2.0;
                double startOffset = -(recruitCount - 1) * spacing / 2.0;
                
                for (int i = 0; i < recruitCount; i++) {
                    double xOffset = startOffset + i * spacing;
                    positions.add(centerPos.add(xOffset, 0, 0));
                }
            }
            case SQUARE -> {
                // Arrange recruits in a square formation
                int sideLength = (int) Math.ceil(Math.sqrt(recruitCount));
                double spacing = 2.0;
                double startX = -(sideLength - 1) * spacing / 2.0;
                double startZ = -(sideLength - 1) * spacing / 2.0;
                
                for (int i = 0; i < recruitCount; i++) {
                    int row = i / sideLength;
                    int col = i % sideLength;
                    
                    double x = startX + col * spacing;
                    double z = startZ + row * spacing;
                    positions.add(centerPos.add(x, 0, z));
                }
            }
            case CIRCLE -> {
                // Arrange recruits in a circle
                double radius = Math.max(2.0, recruitCount * 0.5);
                
                for (int i = 0; i < recruitCount; i++) {
                    double angle = 2 * Math.PI * i / recruitCount;
                    double x = radius * Math.cos(angle);
                    double z = radius * Math.sin(angle);
                    positions.add(centerPos.add(x, 0, z));
                }
            }
            case WEDGE -> {
                // Arrange recruits in a wedge/V formation
                double spacing = 2.0;
                int center = recruitCount / 2;
                
                for (int i = 0; i < recruitCount; i++) {
                    double xOffset = (i - center) * spacing;
                    double zOffset = Math.abs(i - center) * spacing * 0.5;
                    positions.add(centerPos.add(xOffset, 0, -zOffset));
                }
            }
            default -> {
                // Default to line formation
                double spacing = 2.0;
                double startOffset = -(recruitCount - 1) * spacing / 2.0;
                
                for (int i = 0; i < recruitCount; i++) {
                    double xOffset = startOffset + i * spacing;
                    positions.add(centerPos.add(xOffset, 0, 0));
                }
            }
        }
        
        return positions;
    }
    
    /**
     * Get the current position of a recruit
     */
    public static Vec3 getRecruitPosition(AbstractRecruitEntity recruit) {
        return recruit.position();
    }
    
    /**
     * Check if a recruit is currently moving to a position
     */
    public static boolean isRecruitMovingToPosition(AbstractRecruitEntity recruit) {
        return recruit.getShouldMovePos() && recruit.getMovePos() != null;
    }
    
    /**
     * Check if a recruit is currently following
     */
    public static boolean isRecruitFollowing(AbstractRecruitEntity recruit) {
        return recruit.getShouldFollow();
    }
    
    /**
     * Check if a recruit is holding position
     */
    public static boolean isRecruitHoldingPosition(AbstractRecruitEntity recruit) {
        return recruit.getShouldHoldPos();
    }
    
    /**
     * Get the distance between a recruit and a target position
     */
    public static double getDistanceToPosition(AbstractRecruitEntity recruit, Vec3 targetPos) {
        return recruit.position().distanceTo(targetPos);
    }
    
    /**
     * Check if a recruit has reached its target position (within tolerance)
     */
    public static boolean hasRecruitReachedTarget(AbstractRecruitEntity recruit, Vec3 targetPos, double tolerance) {
        return getDistanceToPosition(recruit, targetPos) <= tolerance;
    }
    
    /**
     * Check if a recruit has reached its target position (default tolerance of 2 blocks)
     */
    public static boolean hasRecruitReachedTarget(AbstractRecruitEntity recruit, Vec3 targetPos) {
        return hasRecruitReachedTarget(recruit, targetPos, 2.0);
    }
    
    /**
     * Get selected recruits (alias for getPlayerRecruits for backward compatibility)
     */
    public static List<AbstractRecruitEntity> getSelectedRecruits(Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            return getPlayerRecruits(serverPlayer);
        }
        return List.of();
    }
    
    /**
     * Find nearby recruits (alias for getPlayerRecruits for backward compatibility)
     */
    public static List<AbstractRecruitEntity> findNearbyRecruits(Player player, double radius) {
        if (player instanceof ServerPlayer serverPlayer) {
            return getPlayerRecruits(serverPlayer, radius);
        }
        return List.of();
    }
    
    /**
     * Send march command to a recruit (backward compatibility method)
     */
    public static boolean sendMarchCommand(Entity recruit, BlockPos targetPos) {
        if (recruit instanceof AbstractRecruitEntity abstractRecruit) {
            setRecruitMovePosition(abstractRecruit, new Vec3(targetPos.getX(), targetPos.getY(), targetPos.getZ()));
            return true;
        }
        return false;
    }
    
    /**
     * Get the geometric center of a group of recruits
     */
    public static Vec3 getRecruitsCenter(List<AbstractRecruitEntity> recruits) {
        if (recruits.isEmpty()) {
            return Vec3.ZERO;
        }
        
        if (recruits.size() == 1) {
            return recruits.get(0).position();
        }
        
        // Use FormationUtils if available, otherwise calculate manually
        if (recruits.get(0).level() instanceof ServerLevel serverLevel) {
            return FormationUtils.getGeometricMedian(recruits, serverLevel);
        } else {
            // Manual calculation
            double totalX = 0, totalY = 0, totalZ = 0;
            for (AbstractRecruitEntity recruit : recruits) {
                Vec3 pos = recruit.position();
                totalX += pos.x;
                totalY += pos.y;
                totalZ += pos.z;
            }
            
            int count = recruits.size();
            return new Vec3(totalX / count, totalY / count, totalZ / count);
        }
    }
    
    /**
     * Formation types for organizing recruits
     */
    public enum FormationType {
        LINE,      // Single line formation
        SQUARE,    // Square grid formation
        CIRCLE,    // Circular formation
        WEDGE      // V-shaped wedge formation
    }
    
    /**
     * Utility class for recruit movement commands
     */
    public static class RecruitCommands {
        
        /**
         * Send all recruits to player's current position
         */
        public static void recallAllRecruits(ServerPlayer player) {
            List<AbstractRecruitEntity> recruits = getPlayerRecruits(player);
            sendRecruitsToCoordinates(recruits, player.position(), true);
        }
        
        /**
         * Send specific group to player's current position
         */
        public static void recallGroup(ServerPlayer player, int group) {
            List<AbstractRecruitEntity> recruits = getPlayerRecruitsInGroup(player, group);
            sendRecruitsToCoordinates(recruits, player.position(), true);
        }
        
        /**
         * Make all recruits hold their current positions
         */
        public static void holdAllPositions(ServerPlayer player) {
            List<AbstractRecruitEntity> recruits = getPlayerRecruits(player);
            makeRecruitsHoldPosition(recruits);
        }
        
        /**
         * Make specific group hold their positions
         */
        public static void holdGroupPositions(ServerPlayer player, int group) {
            List<AbstractRecruitEntity> recruits = getPlayerRecruitsInGroup(player, group);
            makeRecruitsHoldPosition(recruits);
        }
        
        /**
         * Make all recruits follow the player
         */
        public static void followAll(ServerPlayer player) {
            List<AbstractRecruitEntity> recruits = getPlayerRecruits(player);
            makeRecruitsFollow(recruits);
        }
        
        /**
         * Make specific group follow the player
         */
        public static void followGroup(ServerPlayer player, int group) {
            List<AbstractRecruitEntity> recruits = getPlayerRecruitsInGroup(player, group);
            makeRecruitsFollow(recruits);
        }
    
    /**
     * Formation types for recruit marching
     * Based on formations available in FormationUtils
     */
    public enum FormationType {
        LINE,           // Basic line formation
        LINEUP,         // Line up formation (wider spacing)
        SQUARE,         // Square/grid formation
        CIRCLE,         // Single circle formation
        TRIANGLE,       // Triangle/wedge formation
        WEDGE           // Alias for triangle
    }
}
}
