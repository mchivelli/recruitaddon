package com.mchivellian.recruitsaddon.network;

import com.mchivellian.recruitsaddon.integration.RecruitsIntegration;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraftforge.network.NetworkEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class PacketMarchCommand {

    private final BlockPos targetPos;
    private final List<UUID> recruitUuids;

    public PacketMarchCommand(BlockPos targetPos, List<UUID> recruitUuids) {
        this.targetPos = targetPos;
        this.recruitUuids = recruitUuids;
    }

    public PacketMarchCommand(FriendlyByteBuf buf) {
        this.targetPos = buf.readBlockPos();
        this.recruitUuids = buf.readList(FriendlyByteBuf::readUUID);
    }

    public void encode(FriendlyByteBuf buf) {
        buf.writeBlockPos(this.targetPos);
        buf.writeCollection(this.recruitUuids, FriendlyByteBuf::writeUUID);
    }

    public void handle(Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player == null) return;

            Level world = player.level();
            for (UUID uuid : this.recruitUuids) {
                Entity recruit = ((net.minecraft.server.level.ServerLevel) world).getEntity(uuid);
                if (recruit != null) {
                    // Find the highest ground for the target position
                    int groundY = world.getHeight(Heightmap.Types.MOTION_BLOCKING, this.targetPos.getX(), this.targetPos.getZ());
                    BlockPos groundPos = new BlockPos(this.targetPos.getX(), groundY, this.targetPos.getZ());
                    RecruitsIntegration.sendMarchCommand(recruit, groundPos);
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }
} 
