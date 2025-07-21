package com.yourname.myforgemod.network;

import com.yourname.myforgemod.integration.RecruitsIntegration;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkEvent;

import java.util.List;
import java.util.function.Supplier;

/**
 * Network packet for recruit movement commands
 */
public class PacketRecruitMovement {
    
    public enum Action {
        MOVE_ALL,
        MOVE_GROUP,
        MARCH_ALL,
        MARCH_GROUP,
        RECALL_ALL,
        RECALL_GROUP,
        HOLD_ALL,
        HOLD_GROUP,
        FOLLOW_ALL,
        FOLLOW_GROUP,
        STOP_ALL,
        STOP_GROUP,
        STATUS
    }
    
    private final Action action;
    private final Vec3 targetPos;
    private final int groupId;
    private final RecruitsIntegration.FormationType formation;
    
    public PacketRecruitMovement(Action action, Vec3 targetPos, int groupId, RecruitsIntegration.FormationType formation) {
        this.action = action;
        this.targetPos = targetPos;
        this.groupId = groupId;
        this.formation = formation;
    }
    
    public static void encode(PacketRecruitMovement packet, FriendlyByteBuf buf) {
        buf.writeEnum(packet.action);
        buf.writeDouble(packet.targetPos.x);
        buf.writeDouble(packet.targetPos.y);
        buf.writeDouble(packet.targetPos.z);
        buf.writeInt(packet.groupId);
        buf.writeEnum(packet.formation);
    }
    
    public static PacketRecruitMovement decode(FriendlyByteBuf buf) {
        Action action = buf.readEnum(Action.class);
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        int groupId = buf.readInt();
        RecruitsIntegration.FormationType formation = buf.readEnum(RecruitsIntegration.FormationType.class);
        
        return new PacketRecruitMovement(action, new Vec3(x, y, z), groupId, formation);
    }
    
    public static void handle(PacketRecruitMovement packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context context = contextSupplier.get();
        context.enqueueWork(() -> {
            ServerPlayer player = context.getSender();
            if (player == null) return;
            
            if (!RecruitsIntegration.isRecruitsLoaded()) {
                return;
            }
            
            switch (packet.action) {
                case MOVE_ALL -> {
                    List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(player);
                    RecruitsIntegration.sendRecruitsToCoordinates(recruits, packet.targetPos, false);
                }
                case MOVE_GROUP -> {
                    List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruitsInGroup(player, packet.groupId);
                    RecruitsIntegration.sendRecruitsToCoordinates(recruits, packet.targetPos, false);
                }
                case MARCH_ALL -> {
                    List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(player);
                    RecruitsIntegration.marchRecruitsToPosition(recruits, packet.targetPos, packet.formation);
                }
                case MARCH_GROUP -> {
                    List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruitsInGroup(player, packet.groupId);
                    RecruitsIntegration.marchRecruitsToPosition(recruits, packet.targetPos, packet.formation);
                }
                case RECALL_ALL -> {
                    RecruitsIntegration.RecruitCommands.recallAllRecruits(player);
                }
                case RECALL_GROUP -> {
                    RecruitsIntegration.RecruitCommands.recallGroup(player, packet.groupId);
                }
                case HOLD_ALL -> {
                    RecruitsIntegration.RecruitCommands.holdAllPositions(player);
                }
                case HOLD_GROUP -> {
                    RecruitsIntegration.RecruitCommands.holdGroupPositions(player, packet.groupId);
                }
                case FOLLOW_ALL -> {
                    RecruitsIntegration.RecruitCommands.followAll(player);
                }
                case FOLLOW_GROUP -> {
                    RecruitsIntegration.RecruitCommands.followGroup(player, packet.groupId);
                }
                case STOP_ALL -> {
                    List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(player);
                    RecruitsIntegration.stopRecruits(recruits);
                }
                case STOP_GROUP -> {
                    List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruitsInGroup(player, packet.groupId);
                    RecruitsIntegration.stopRecruits(recruits);
                }
                case STATUS -> {
                    // Status is handled by the command system, just trigger a status check
                    List<AbstractRecruitEntity> recruits = RecruitsIntegration.getPlayerRecruits(player);
                    int following = 0, holding = 0, moving = 0, idle = 0;
                    
                    for (AbstractRecruitEntity recruit : recruits) {
                        if (RecruitsIntegration.isRecruitFollowing(recruit)) {
                            following++;
                        } else if (RecruitsIntegration.isRecruitHoldingPosition(recruit)) {
                            holding++;
                        } else if (RecruitsIntegration.isRecruitMovingToPosition(recruit)) {
                            moving++;
                        } else {
                            idle++;
                        }
                    }
                    
                    player.sendSystemMessage(net.minecraft.network.chat.Component.literal(
                        "Recruit Status - Total: " + recruits.size() + 
                        " | Following: " + following + 
                        " | Holding: " + holding + 
                        " | Moving: " + moving + 
                        " | Idle: " + idle));
                }
            }
        });
        context.setPacketHandled(true);
    }
}
