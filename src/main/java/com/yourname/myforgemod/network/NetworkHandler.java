package com.yourname.myforgemod.network;

import com.yourname.myforgemod.ModMain;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.simple.SimpleChannel;

public class NetworkHandler {
    
    private static final String PROTOCOL_VERSION = "1";
    
    public static final SimpleChannel INSTANCE = NetworkRegistry.newSimpleChannel(
        new ResourceLocation(ModMain.MODID, "main"),
        () -> PROTOCOL_VERSION,
        PROTOCOL_VERSION::equals,
        PROTOCOL_VERSION::equals
    );
    
    private static int packetId = 0;
    
    public static void registerMessages() {
        INSTANCE.registerMessage(
            packetId++,
            MessageRaidCommand.class,
            MessageRaidCommand::encode,
            MessageRaidCommand::decode,
            MessageRaidCommand::handle
        );
        
        INSTANCE.registerMessage(
            packetId++,
            PacketRecruitMovement.class,
            PacketRecruitMovement::encode,
            PacketRecruitMovement::decode,
            PacketRecruitMovement::handle
        );
    }
    
    public static <MSG> void sendToServer(MSG message) {
        INSTANCE.sendToServer(message);
    }
    
    private static int nextId() {
        return packetId++;
    }
}
