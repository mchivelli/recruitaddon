package com.yourname.myforgemod.network;

import com.yourname.myforgemod.raid.RaidManager;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.UUID;
import java.util.function.Supplier;

public class MessageRaidCommand {
    
    private UUID playerUuid;
    private BlockPos targetPos;
    private int groupId;
    private int raidType; // 0 = raid, 1 = assault
    
    public MessageRaidCommand() {
    }
    
    public MessageRaidCommand(UUID playerUuid, BlockPos targetPos, int groupId, int raidType) {
        this.playerUuid = playerUuid;
        this.targetPos = targetPos;
        this.groupId = groupId;
        this.raidType = raidType;
    }
    
    public static void encode(MessageRaidCommand packet, FriendlyByteBuf buf) {
        buf.writeUUID(packet.playerUuid);
        buf.writeBlockPos(packet.targetPos);
        buf.writeInt(packet.groupId);
        buf.writeInt(packet.raidType);
    }
    
    public static MessageRaidCommand decode(FriendlyByteBuf buf) {
        UUID playerUuid = buf.readUUID();
        BlockPos targetPos = buf.readBlockPos();
        int groupId = buf.readInt();
        int raidType = buf.readInt();
        return new MessageRaidCommand(playerUuid, targetPos, groupId, raidType);
    }
    
    public static void handle(MessageRaidCommand message, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player != null && player.getUUID().equals(message.playerUuid)) {
                RaidManager.startRaid(player, message.targetPos, message.groupId, message.raidType);
            }
        });
        context.setPacketHandled(true);
    }
}
