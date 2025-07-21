package com.yourname.myforgemod.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.talhanation.recruits.entities.AbstractRecruitEntity;
import com.talhanation.recruits.network.MessageMovement;
import com.yourname.myforgemod.ModMain;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.ChatFormatting;

import java.util.List;
import java.util.UUID;

/**
 * Commands that use the EXACT same recruit discovery approach as the official mod
 * Instead of getEntitiesOfClass(), we'll try different approaches used by the official mod
 */
public class OfficialStyleCommands {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("officialtest")
            .then(Commands.literal("findrecruits")
                .executes(OfficialStyleCommands::testRecruitDiscovery)
            )
            .then(Commands.literal("march")
                .then(Commands.argument("groupId", IntegerArgumentType.integer())
                    .then(Commands.argument("x", StringArgumentType.string())
                        .then(Commands.argument("y", StringArgumentType.string())
                            .then(Commands.argument("z", StringArgumentType.string())
                                .executes(OfficialStyleCommands::executeOfficialMarch)
                            )
                        )
                    )
                )
            )
        );
    }
    
    private static int testRecruitDiscovery(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            ServerLevel world = (ServerLevel) player.getCommandSenderWorld();
            
            player.sendSystemMessage(Component.literal("§b=== OFFICIAL-STYLE RECRUIT DISCOVERY TEST ==="));
            
            // Method 1: Try getting all entities, then filtering
            List<Entity> allEntities = world.getEntitiesOfClass(Entity.class, player.getBoundingBox().inflate(500));
            List<AbstractRecruitEntity> recruitEntities = allEntities.stream()
                .filter(entity -> entity instanceof AbstractRecruitEntity)
                .map(entity -> (AbstractRecruitEntity) entity)
                .toList();
            
            player.sendSystemMessage(Component.literal("§7Method 1 - All Entities then Filter:"));
            player.sendSystemMessage(Component.literal("§7  Total entities: " + allEntities.size()));
            player.sendSystemMessage(Component.literal("§7  Recruit entities: " + recruitEntities.size()));
            
            // Method 2: Try different world reference
            if (player.level() instanceof ServerLevel serverLevel) {
                List<AbstractRecruitEntity> worldRecruits = serverLevel.getEntitiesOfClass(
                    AbstractRecruitEntity.class, 
                    player.getBoundingBox().inflate(200) // Try smaller radius first
                );
                player.sendSystemMessage(Component.literal("§7Method 2 - Different world reference (200 blocks):"));
                player.sendSystemMessage(Component.literal("§7  Found: " + worldRecruits.size()));
            }
            
            // Method 3: Try player's tracked entities
            player.sendSystemMessage(Component.literal("§7Method 3 - Player dimension: " + player.level().dimension().location()));
            player.sendSystemMessage(Component.literal("§7  Player UUID: " + player.getUUID()));
            player.sendSystemMessage(Component.literal("§7  Player position: " + player.blockPosition()));
            
            // Method 4: Try to mimic official MessageMovement approach
            List<AbstractRecruitEntity> messageStyleRecruits = world.getEntitiesOfClass(
                AbstractRecruitEntity.class, 
                player.getBoundingBox().inflate(500) // Fixed: Double.MAX_VALUE breaks entity search
            );
            
            player.sendSystemMessage(Component.literal("§7Method 4 - MessageMovement style:"));
            player.sendSystemMessage(Component.literal("§7  Before filtering: " + messageStyleRecruits.size()));
            
            // Apply the same filtering as MessageMovement
            messageStyleRecruits.removeIf(recruit -> !recruit.isEffectedByCommand(player.getUUID(), 0));
            player.sendSystemMessage(Component.literal("§7  After isEffectedByCommand(uuid, 0): " + messageStyleRecruits.size()));
            
            // Show details of any found recruits
            for (int i = 0; i < Math.min(messageStyleRecruits.size(), 3); i++) {
                AbstractRecruitEntity recruit = messageStyleRecruits.get(i);
                player.sendSystemMessage(Component.literal(String.format(
                    "§a  Recruit %d: %s (Group: %d, Owner: %s, Alive: %s)", 
                    i+1, recruit.getName().getString(), recruit.getGroup(), 
                    recruit.getOwnerUUID(), recruit.isAlive()
                )));
            }
            
            player.sendSystemMessage(Component.literal("§b=== END DISCOVERY TEST ==="));
            return 1;
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError in discovery test: " + e.getMessage()));
            ModMain.LOGGER.error("Error in recruit discovery test", e);
            return 0;
        }
    }
    
    private static int executeOfficialMarch(CommandContext<CommandSourceStack> context) {
        try {
            ServerPlayer player = context.getSource().getPlayerOrException();
            int groupId = IntegerArgumentType.getInteger(context, "groupId");
            String xStr = StringArgumentType.getString(context, "x");
            String yStr = StringArgumentType.getString(context, "y");
            String zStr = StringArgumentType.getString(context, "z");
            
            // Parse coordinates
            BlockPos playerPos = player.blockPosition();
            int x = parseCoordinate(xStr, playerPos.getX());
            int y = parseCoordinate(yStr, playerPos.getY());
            int z = parseCoordinate(zStr, playerPos.getZ());
            
            player.sendSystemMessage(Component.literal("§e[OFFICIAL-STYLE] Testing march with group " + groupId + " to " + x + ", " + y + ", " + z));
            
            // Try to send movement using the official mod's approach
            // This mimics what MessageMovement does
            ServerLevel world = (ServerLevel) player.getCommandSenderWorld();
            List<AbstractRecruitEntity> recruits = world.getEntitiesOfClass(
                AbstractRecruitEntity.class, 
                player.getBoundingBox().inflate(500) // Fixed: Double.MAX_VALUE breaks entity search
            );
            
            ModMain.LOGGER.info("OFFICIAL-STYLE: Found {} total entities before filtering", recruits.size());
            
            recruits.removeIf(recruit -> !recruit.isEffectedByCommand(player.getUUID(), groupId));
            
            ModMain.LOGGER.info("OFFICIAL-STYLE: Found {} recruits after filtering for group {}", recruits.size(), groupId);
            
            if (recruits.isEmpty()) {
                player.sendSystemMessage(Component.literal("§c[OFFICIAL-STYLE] No recruits found in group " + groupId).withStyle(ChatFormatting.RED));
                return 0;
            }
            
            player.sendSystemMessage(Component.literal("§a[OFFICIAL-STYLE] Found " + recruits.size() + " recruits in group " + groupId));
            
            return recruits.size();
            
        } catch (Exception e) {
            context.getSource().sendFailure(Component.literal("§cError in official-style march: " + e.getMessage()));
            ModMain.LOGGER.error("Error in official-style march", e);
            return 0;
        }
    }
    
    private static int parseCoordinate(String coordStr, int currentPos) {
        if (coordStr.equals("~")) {
            return currentPos;
        } else if (coordStr.startsWith("~")) {
            try {
                int offset = Integer.parseInt(coordStr.substring(1));
                return currentPos + offset;
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid relative coordinate: " + coordStr);
            }
        } else {
            try {
                return Integer.parseInt(coordStr);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid coordinate: " + coordStr);
            }
        }
    }
}
