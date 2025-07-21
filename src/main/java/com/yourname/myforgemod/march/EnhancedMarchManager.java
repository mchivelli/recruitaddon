package com.yourname.myforgemod.march;

import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.yourname.myforgemod.integration.RecruitsIntegration;
import com.yourname.myforgemod.ModMain;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages the enhanced march commands.
 * This class is now fully static and simplified to prevent AI conflicts
 * with the Recruits mod. The complex state-tracking was causing recruits
 * to reset their positions.
 */
public class EnhancedMarchManager {

    /**
     * Executes an enhanced march command for currently selected recruits.
     * This method is called directly from the GUI button.
     */
    public static void executeEnhancedMarch() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        List<AbstractRecruitEntity> selectedRecruits = RecruitsIntegration.getSelectedRecruits(player);
        if (selectedRecruits.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cNo recruits selected."));
            return;
        }

        BlockPos targetPos = getPlayerLookAtPos(player);
        if (targetPos == null) {
            player.sendSystemMessage(Component.literal("§cNo target position in sight."));
            return;
        }

        ModMain.LOGGER.info("Executing Enhanced March for {} recruits to {}", selectedRecruits.size(), targetPos);

        int successCount = 0;
        for (int i = 0; i < selectedRecruits.size(); i++) {
            Entity recruit = selectedRecruits.get(i);
            BlockPos finalPos = calculateMarchPosition(targetPos, i, selectedRecruits.size());
            if (RecruitsIntegration.sendMarchCommand(recruit, finalPos)) {
                successCount++;
            }
        }
        
        player.sendSystemMessage(Component.literal(
            String.format("§aMarching %d/%d recruits to %s", successCount, selectedRecruits.size(), targetPos.toShortString())
        ));
    }

    /**
     * Executes a march command to a specific set of coordinates.
     * This is called from the new /recruitsmarch command.
     */
    public static void executeMarchToCoordinates(Player player, BlockPos targetPos) {
        if (player == null) return;

        List<AbstractRecruitEntity> selectedRecruits = RecruitsIntegration.getSelectedRecruits(player);
        if (selectedRecruits.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cNo recruits selected."));
            return;
        }

        ModMain.LOGGER.info("Executing coordinate March for {} recruits to {}", selectedRecruits.size(), targetPos);

        int successCount = 0;
        for (int i = 0; i < selectedRecruits.size(); i++) {
            Entity recruit = selectedRecruits.get(i);
            BlockPos finalPos = calculateMarchPosition(targetPos, i, selectedRecruits.size());
            if (RecruitsIntegration.sendMarchCommand(recruit, finalPos)) {
                successCount++;
            }
        }
        
        player.sendSystemMessage(Component.literal(
            String.format("§aMarching %d/%d recruits to %s", successCount, selectedRecruits.size(), targetPos.toShortString())
        ));
    }

    /**
     * Executes a formation march command.
     */
    public static void executeFormationMarch() {
        Player player = Minecraft.getInstance().player;
        if (player == null) return;

        List<AbstractRecruitEntity> selectedRecruits = RecruitsIntegration.getSelectedRecruits(player);
        if (selectedRecruits.isEmpty()) {
            player.sendSystemMessage(Component.literal("§cNo recruits selected."));
            return;
        }
        
        BlockPos targetPos = getPlayerLookAtPos(player);
        if (targetPos == null) {
            player.sendSystemMessage(Component.literal("§cNo target position in sight."));
            return;
        }

        ModMain.LOGGER.info("Executing Formation March for {} recruits to {}", selectedRecruits.size(), targetPos);
        
        int successCount = 0;
        for (int i = 0; i < selectedRecruits.size(); i++) {
            Entity recruit = selectedRecruits.get(i);
            // Simple line formation perpendicular to player's facing direction
            int offset = i - (selectedRecruits.size() - 1) / 2;
            BlockPos finalPos = targetPos.relative(player.getDirection().getCounterClockWise(), offset * 2);

            if (RecruitsIntegration.sendMarchCommand(recruit, finalPos)) {
                successCount++;
            }
        }
        
        player.sendSystemMessage(Component.literal(
            String.format("§aForming %d/%d recruits at %s", successCount, selectedRecruits.size(), targetPos.toShortString())
        ));
    }

    private static BlockPos getPlayerLookAtPos(Player player) {
        HitResult hitResult = player.pick(100.0D, 0.0F, false);
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            return ((BlockHitResult) hitResult).getBlockPos();
        }
        return null;
    }

    private static BlockPos calculateMarchPosition(BlockPos basePos, int index, int total) {
        if (total == 1) return basePos;
        
        int spacing = 3;
        int cols = (int) Math.ceil(Math.sqrt(total));
        
        int row = index / cols;
        int col = index % cols;
        
        int offsetX = (col - cols / 2) * spacing;
        int offsetZ = (row - (int)Math.ceil((double)total / cols) / 2) * spacing;
        
        return basePos.offset(offsetX, 0, offsetZ);
    }
} 