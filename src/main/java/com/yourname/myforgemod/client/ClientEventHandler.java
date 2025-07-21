package com.yourname.myforgemod.client;

import com.talhanation.recruits.client.events.CommandCategoryManager;
import com.yourname.myforgemod.ModMain;
import com.yourname.myforgemod.client.gui.EnhancedRecruitCategory;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

/**
 * Client-side event handler for registering GUI components
 */
@Mod.EventBusSubscriber(modid = ModMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class ClientEventHandler {
    
    @SubscribeEvent
    public static void onClientSetup(FMLClientSetupEvent event) {
        event.enqueueWork(() -> {
            // Register our command categories with the Recruits mod GUI
            CommandCategoryManager.register(new EnhancedRecruitCategory());
            ModMain.LOGGER.info("Registered Enhanced Recruit Category with Recruits GUI");
        });
    }
}
