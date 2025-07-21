package com.yourname.myforgemod.client;

import com.yourname.myforgemod.ModMain;
import com.yourname.myforgemod.network.MessageRaidCommand;
import com.yourname.myforgemod.network.NetworkHandler;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@OnlyIn(Dist.CLIENT)
@Mod.EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public class RaidCommandIntegration {
    
    private static int selectedGroupId = -1; // -1 means all groups
    
    public static void setSelectedGroup(int groupId) {
        selectedGroupId = groupId;
    }
    
    public static int getSelectedGroup() {
        return selectedGroupId;
    }
    
    public static void sendRaidCommand(int raidType) {
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        
        if (player == null) return;
        
        // Get the position the player is looking at
        HitResult hitResult = mc.hitResult;
        if (hitResult == null || hitResult.getType() != HitResult.Type.BLOCK) {
            player.sendSystemMessage(net.minecraft.network.chat.Component.literal("You must be looking at a block to set raid target"));
            return;
        }
        
        BlockHitResult blockHitResult = (BlockHitResult) hitResult;
        BlockPos targetPos = blockHitResult.getBlockPos();
        
        // Send the raid command to server
        MessageRaidCommand message = new MessageRaidCommand(
            player.getUUID(),
            targetPos,
            selectedGroupId,
            raidType
        );
        
        // Note: This would need to be sent through the main mod's network channel
        // For now, we'll create a simple integration point
        sendRaidMessage(message);
        
        String raidTypeName = (raidType == 0) ? "raid" : "assault";
        String groupText = (selectedGroupId == -1) ? "all groups" : "group " + selectedGroupId;
        player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
            "Sending " + groupText + " on " + raidTypeName + " to " + 
            targetPos.getX() + ", " + targetPos.getY() + ", " + targetPos.getZ()));
    }
    
    private static void sendRaidMessage(MessageRaidCommand message) {
        // Send the message through our network handler
        NetworkHandler.INSTANCE.sendToServer(message);
        ModMain.LOGGER.info("Sent raid command: " + message.toString());
    }
    
    public static boolean isValidRaidTarget(BlockPos pos) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return false;
        
        // Check if the position is within reasonable distance
        LocalPlayer player = mc.player;
        if (player == null) return false;
        
        double distance = player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ());
        return distance <= 10000; // 100 block radius
    }
}
