package com.mchivellian.recruitsaddon.client;

import com.mchivellian.recruitsaddon.ModMain;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MODID, value = Dist.CLIENT)
public class KeyInputHandler {
    
    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.level == null) return;
        
        // Check if raid key was pressed
        if (KeyBindings.SEND_RAID_KEY.consumeClick()) {
            handleRaidCommand(0); // 0 = raid
        }
        
        // Check if assault key was pressed
        if (KeyBindings.SEND_ASSAULT_KEY.consumeClick()) {
            handleRaidCommand(1); // 1 = assault
        }
    }
    
    private static void handleRaidCommand(int raidType) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        
        // Check if player is looking at a valid target
        if (!RaidCommandIntegration.isValidRaidTarget(mc.player.blockPosition())) {
            mc.player.sendSystemMessage(Component.literal("Invalid raid target location"));
            return;
        }
        
        // Send the raid command
        RaidCommandIntegration.sendRaidCommand(raidType);
        
        String commandType = (raidType == 0) ? "raid" : "assault";
        mc.player.sendSystemMessage(Component.literal("Sending recruits on " + commandType + "!"));
    }
}
