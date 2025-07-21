package com.yourname.myforgemod.client;

import com.yourname.myforgemod.client.gui.RecruitMovementScreen;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

public class ClientSetup {
    
    public static void init(FMLClientSetupEvent event) {
        // Client-side initialization
        event.enqueueWork(() -> {
            // Register key bindings and client-side handlers here
        });
    }
    
    public static void openRecruitMovementScreen() {
        Minecraft.getInstance().setScreen(new RecruitMovementScreen());
    }
}
