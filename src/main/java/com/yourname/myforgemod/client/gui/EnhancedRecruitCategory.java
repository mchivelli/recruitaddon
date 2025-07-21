package com.yourname.myforgemod.client.gui;

import com.talhanation.recruits.client.gui.CommandScreen;
import com.talhanation.recruits.client.gui.commandscreen.ICommandCategory;
import com.talhanation.recruits.client.gui.group.RecruitsGroup;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import java.util.List;
import java.util.ArrayList;

/**
 * Group Commands category with look-at targeting and chat prefill buttons
 * Integrates with the Recruits mod's CommandScreen to provide additional functionality
 */
public class EnhancedRecruitCategory implements ICommandCategory {
    
    @Override
    public Component getToolTipName() {
        return Component.literal("Group Commands");
    }
    
    @Override
    public ItemStack getIcon() {
        return new ItemStack(Items.DIAMOND_SWORD);
    }
    
    @Override
    public void createButtons(CommandScreen screen, int centerX, int centerY, List<RecruitsGroup> groups, Player player) {
        int buttonWidth = 100;
        int buttonHeight = 20;
        int buttonSpacing = 8; // Increased spacing between buttons
        int x = centerX - buttonWidth / 2;
        int startY = centerY - 90; // Start higher to prevent overlap
        
        // March to Look-At Position button
        Button marchButton = Button.builder(
            Component.literal("March Here"),
            button -> marchToLookAtPosition(groups)
        )
        .bounds(x, startY, buttonWidth, buttonHeight)
        .tooltip(Tooltip.create(Component.literal("Send selected recruits to march to the position you're looking at")))
        .build();
        screen.addRenderableWidget(marchButton);
        
        // Raid to Look-At Position button
        Button raidButton = Button.builder(
            Component.literal("Raid Here"),
            button -> raidToLookAtPosition(groups)
        )
        .bounds(x, startY + (buttonHeight + buttonSpacing), buttonWidth, buttonHeight)
        .tooltip(Tooltip.create(Component.literal("Send selected recruits to raid the position you're looking at")))
        .build();
        screen.addRenderableWidget(raidButton);
        
        // Get the first enabled group ID for prefill commands
        int tempGroupId = -1;
        for (RecruitsGroup group : groups) {
            if (!group.isDisabled() && group.getCount() > 0) {
                tempGroupId = group.getId();
                break;
            }
        }
        final int firstEnabledGroupId = tempGroupId;
        
        if (firstEnabledGroupId != -1) {
            // Prefill March Command button
            Button marchCmdButton = Button.builder(
                Component.literal("March to..."),
                button -> prefillMarchCommand(firstEnabledGroupId)
            )
            .bounds(x, startY + (buttonHeight + buttonSpacing) * 2, buttonWidth, buttonHeight)
            .tooltip(Tooltip.create(Component.literal("Type '/recruits march group ID ~ ~ ~' in chat for easy coordinate entry")))
            .build();
            screen.addRenderableWidget(marchCmdButton);
            
            // Prefill Raid Command button
            Button raidCmdButton = Button.builder(
                Component.literal("Raid at..."),
                button -> prefillRaidCommand(firstEnabledGroupId)
            )
            .bounds(x, startY + (buttonHeight + buttonSpacing) * 3, buttonWidth, buttonHeight)
            .tooltip(Tooltip.create(Component.literal("Type '/recruits raid group ID ~ ~ ~' in chat for easy coordinate entry")))
            .build();
            screen.addRenderableWidget(raidCmdButton);
            
            // Cancel button
            Button cancelButton = Button.builder(
                Component.literal("Cancel"),
                button -> executeCancelCommand(firstEnabledGroupId)
            )
            .bounds(x, startY + (buttonHeight + buttonSpacing) * 4, buttonWidth, buttonHeight)
            .tooltip(Tooltip.create(Component.literal("Cancel current group commands and return recruits to you")))
            .build();
            screen.addRenderableWidget(cancelButton);
            
            // Stop button
            Button stopButton = Button.builder(
                Component.literal("Stop"),
                button -> executeStopCommand(firstEnabledGroupId)
            )
            .bounds(x, startY + (buttonHeight + buttonSpacing) * 5, buttonWidth, buttonHeight)
            .tooltip(Tooltip.create(Component.literal("Make recruits stand ground with shields up, only retaliate when attacked")))
            .build();
            screen.addRenderableWidget(stopButton);
        }
    }
    
    /**
     * March recruits to the position the player is looking at
     */
    private void marchToLookAtPosition(List<RecruitsGroup> groups) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        HitResult hitResult = mc.player.pick(100.0, 0.0f, false);
        
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            BlockPos pos = blockHit.getBlockPos();
            
            // Get selected groups or use all if none selected
            if (groups.isEmpty()) {
                mc.player.sendSystemMessage(Component.literal("§cNo recruit groups selected. Please select a group first."));
                return;
            }
            
            // Get all enabled groups and send command for each
            List<Integer> enabledGroupIds = new ArrayList<>();
            for (RecruitsGroup group : groups) {
                if (!group.isDisabled() && group.getCount() > 0) {
                    enabledGroupIds.add(group.getId());
                }
            }
            
            if (!enabledGroupIds.isEmpty()) {
                // Send command with new structure: /recruits march group ID x y z
                for (Integer groupId : enabledGroupIds) {
                    String command = "/recruits march group " + groupId + " " + pos.getX() + " " + pos.getY() + " " + pos.getZ();
                    mc.player.connection.sendCommand(command.substring(1)); // Remove the '/' prefix
                }
            }
            
            mc.player.sendSystemMessage(Component.literal("§aMarching selected recruit groups to " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()));
        } else {
            mc.player.sendSystemMessage(Component.literal("§cNo valid position found. Look at a block to march there."));
        }
    }
    
    /**
     * Raid the position the player is looking at
     */
    private void raidToLookAtPosition(List<RecruitsGroup> groups) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        HitResult hitResult = mc.player.pick(100.0, 0.0f, true); // Include entities for raids
        
        BlockPos targetPos = null;
        
        if (hitResult.getType() == HitResult.Type.BLOCK) {
            BlockHitResult blockHit = (BlockHitResult) hitResult;
            targetPos = blockHit.getBlockPos();
        } else if (hitResult.getType() == HitResult.Type.ENTITY) {
            EntityHitResult entityHit = (EntityHitResult) hitResult;
            Entity entity = entityHit.getEntity();
            targetPos = entity.blockPosition();
        }
        
        if (targetPos != null) {
            // Get selected groups or use all if none selected
            if (groups.isEmpty()) {
                mc.player.sendSystemMessage(Component.literal("§cNo recruit groups selected. Please select a group first."));
                return;
            }
            
            // Get all enabled groups and send command for each
            List<Integer> enabledGroupIds = new ArrayList<>();
            for (RecruitsGroup group : groups) {
                if (!group.isDisabled() && group.getCount() > 0) {
                    enabledGroupIds.add(group.getId());
                }
            }
            
            if (!enabledGroupIds.isEmpty()) {
                // Send command with new structure: /recruits raid group ID x y z
                for (Integer groupId : enabledGroupIds) {
                    String command = "/recruits raid group " + groupId + " " + targetPos.getX() + " " + targetPos.getY() + " " + targetPos.getZ();
                    mc.player.connection.sendCommand(command.substring(1)); // Remove the '/' prefix
                }
            }
            
            mc.player.sendSystemMessage(Component.literal("§cStarting raid with selected groups at " + targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ()));
        } else {
            mc.player.sendSystemMessage(Component.literal("§cNo valid target found. Look at a block or entity to raid there."));
        }
    }
    
    /**
     * Prefill march command in chat with group ID
     */
    private void prefillMarchCommand(int groupId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // Open chat with prefilled command including group ID and ~ ~ ~
        String command = "/recruits march group " + groupId + " ~ ~ ~";
        mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen(command));
    }
    
    /**
     * Prefill raid command in chat with group ID
     */
    private void prefillRaidCommand(int groupId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // Open chat with prefilled command including group ID and ~ ~ ~
        String command = "/recruits raid group " + groupId + " ~ ~ ~";
        mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen(command));
    }
    
    /**
     * Execute cancel command for the specified group
     */
    private void executeCancelCommand(int groupId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // Send cancel command
        String command = "/recruits cancel";
        mc.player.connection.sendCommand(command.substring(1)); // Remove the '/' prefix
    }
    
    /**
     * Execute stop command for the specified group
     */
    private void executeStopCommand(int groupId) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // Send stop command
        String command = "/recruits stop";
        mc.player.connection.sendCommand(command.substring(1)); // Remove the '/' prefix
    }
    
    /**
     * Add chat prefill buttons that put the new command structure into chat for easy editing
     */
    public static void addChatPrefillButtons(CommandScreen screen, List<RecruitsGroup> groups) {
        if (groups == null || groups.isEmpty()) {
            return;
        }
        
        // Get the first enabled group ID for the prefill
        final int firstEnabledGroupId;
        int tempGroupId = -1;
        for (RecruitsGroup group : groups) {
            if (!group.isDisabled() && group.getCount() > 0) {
                tempGroupId = group.getId();
                break;
            }
        }
        firstEnabledGroupId = tempGroupId;
        
        if (firstEnabledGroupId == -1) {
            return; // No enabled groups
        }
        
        // Create march prefill button
        Button marchPrefillButton = Button.builder(
            Component.literal("March to..."),
            button -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    // Prefill chat with: /recruits march group ID ~ ~ ~
                    String prefillCommand = "/recruits march group " + firstEnabledGroupId + " ~ ~ ~";
                    mc.gui.getChat().addRecentChat(prefillCommand);
                    
                    // Open chat with the prefilled command
                    mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen(prefillCommand));
                }
            }
        ).bounds(10, 200, 100, 20).build();
        
        // Create raid prefill button
        Button raidPrefillButton = Button.builder(
            Component.literal("Raid at..."),
            button -> {
                Minecraft mc = Minecraft.getInstance();
                if (mc.player != null) {
                    // Prefill chat with: /recruits raid group ID ~ ~ ~
                    String prefillCommand = "/recruits raid group " + firstEnabledGroupId + " ~ ~ ~";
                    mc.gui.getChat().addRecentChat(prefillCommand);
                    
                    // Open chat with the prefilled command
                    mc.setScreen(new net.minecraft.client.gui.screens.ChatScreen(prefillCommand));
                }
            }
        ).bounds(120, 200, 100, 20).build();
        
        // Add buttons to screen (this would need to be integrated into the actual CommandScreen)
        // For now, this is a placeholder for the integration point
    }
}
