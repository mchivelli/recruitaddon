package com.mchivellian.recruitsaddon.client;

import com.mojang.blaze3d.platform.InputConstants;
import com.mchivellian.recruitsaddon.ModMain;
import net.minecraft.client.KeyMapping;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = ModMain.MODID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class KeyBindings {
    
    public static final String KEY_CATEGORY_RECRUITS_RAID = "key.category.recruits_raid";
    public static final String KEY_SEND_RAID = "key.recruits_raid.send_raid";
    public static final String KEY_SEND_ASSAULT = "key.recruits_raid.send_assault";
    
    public static final KeyMapping SEND_RAID_KEY = new KeyMapping(
        KEY_SEND_RAID,
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_R, // Default to 'R' key
        KEY_CATEGORY_RECRUITS_RAID
    );
    
    public static final KeyMapping SEND_ASSAULT_KEY = new KeyMapping(
        KEY_SEND_ASSAULT,
        InputConstants.Type.KEYSYM,
        InputConstants.KEY_T, // Default to 'T' key
        KEY_CATEGORY_RECRUITS_RAID
    );
    
    @SubscribeEvent
    public static void onRegisterKeyMappings(RegisterKeyMappingsEvent event) {
        event.register(SEND_RAID_KEY);
        event.register(SEND_ASSAULT_KEY);
    }
}
