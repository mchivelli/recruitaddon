package com.mchivelli.machiavellianminigames.events;

import com.mchivelli.machiavellianminigames.MachiavellianMinigames;
import com.mchivelli.machiavellianminigames.core.Arena;
import com.mchivelli.machiavellianminigames.core.ArenaManager;
import com.mchivelli.machiavellianminigames.core.Game;
import com.mchivelli.machiavellianminigames.minigames.StormTheFrontGame;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EnderChestBlock;
import net.minecraftforge.event.entity.item.ItemTossEvent;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.event.entity.player.EntityItemPickupEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.level.BlockEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

@Mod.EventBusSubscriber(modid = MachiavellianMinigames.MODID)
public class MinigameEvents {
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerDropItem(ItemTossEvent event) {
        Player player = event.getPlayer();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        ArenaManager arenaManager = ArenaManager.get();
        Arena arena = arenaManager.getArenaContainingPlayer(serverPlayer.blockPosition(), 
                                                           serverPlayer.level().dimension().location());
        
        if (arena != null && arena.getCurrentGame() != null) {
            // Block item dropping during games
            event.setCanceled(true);
            serverPlayer.displayClientMessage(Component.literal("§cYou cannot drop items during a minigame!"), true);
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerPickupItem(EntityItemPickupEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        ArenaManager arenaManager = ArenaManager.get();
        Arena arena = arenaManager.getArenaContainingPlayer(serverPlayer.blockPosition(), 
                                                           serverPlayer.level().dimension().location());
        
        if (arena != null && arena.getCurrentGame() != null) {
            // Only allow pickup of minigame items (resources)
            Game game = arena.getCurrentGame();
            if (game instanceof StormTheFrontGame) {
                // Allow pickup of game resources - let it through
                return;
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerRightClickBlock(PlayerInteractEvent.RightClickBlock event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        ArenaManager arenaManager = ArenaManager.get();
        Arena arena = arenaManager.getArenaContainingPlayer(serverPlayer.blockPosition(), 
                                                           serverPlayer.level().dimension().location());
        
        if (arena != null && arena.getCurrentGame() != null) {
            BlockPos pos = event.getPos();
            
            // Block ender chest interactions
            if (serverPlayer.level().getBlockState(pos).getBlock() instanceof EnderChestBlock) {
                event.setCanceled(true);
                serverPlayer.displayClientMessage(Component.literal("§cEnder chests are disabled during minigames!"), true);
                return;
            }
            
            // Block specific container interactions that could bypass inventory isolation
            if (serverPlayer.level().getBlockState(pos).is(Blocks.CHEST) ||
                serverPlayer.level().getBlockState(pos).is(Blocks.TRAPPED_CHEST) ||
                serverPlayer.level().getBlockState(pos).is(Blocks.BARREL) ||
                serverPlayer.level().getBlockState(pos).is(Blocks.SHULKER_BOX)) {
                
                event.setCanceled(true);
                serverPlayer.displayClientMessage(Component.literal("§cContainer access is disabled during minigames!"), true);
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onBlockPlace(BlockEvent.EntityPlaceEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        
        ArenaManager arenaManager = ArenaManager.get();
        Arena arena = arenaManager.getArenaContainingPlayer(serverPlayer.blockPosition(), 
                                                           serverPlayer.level().dimension().location());
        
        if (arena != null && arena.getCurrentGame() != null) {
            // Block placing containers and special blocks
            if (event.getPlacedBlock().is(Blocks.CHEST) ||
                event.getPlacedBlock().is(Blocks.TRAPPED_CHEST) ||
                event.getPlacedBlock().is(Blocks.ENDER_CHEST) ||
                event.getPlacedBlock().is(Blocks.BARREL) ||
                event.getPlacedBlock().is(Blocks.SHULKER_BOX) ||
                event.getPlacedBlock().is(Blocks.HOPPER)) {
                
                event.setCanceled(true);
                serverPlayer.displayClientMessage(Component.literal("§cYou cannot place containers during minigames!"), true);
            }
        }
    }
    
    @SubscribeEvent(priority = EventPriority.HIGH)
    public static void onPlayerDeath(LivingDeathEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer serverPlayer)) return;
        
        ArenaManager arenaManager = ArenaManager.get();
        Arena arena = arenaManager.getArenaContainingPlayer(serverPlayer.blockPosition(), 
                                                           serverPlayer.level().dimension().location());
        
        if (arena != null && arena.getCurrentGame() != null) {
            Game game = arena.getCurrentGame();
            
            // Handle Storm the Front king deaths
            if (game instanceof StormTheFrontGame stormGame) {
                // Check if this player was a king
                for (var team : arena.getTeams()) {
                    if (team.hasKing(serverPlayer.getUUID())) {
                        // Find the killer
                        ServerPlayer killer = null;
                        if (event.getSource().getEntity() instanceof ServerPlayer) {
                            killer = (ServerPlayer) event.getSource().getEntity();
                        }
                        
                        if (killer != null) {
                            stormGame.onKingKilled(serverPlayer.getUUID(), killer);
                        }
                        break;
                    }
                }
            }
            
            // Keep items on death during minigames
            event.setCanceled(true);
            
            // Teleport back to team spawn after a delay
            java.util.Timer timer = new java.util.Timer();
            timer.schedule(new java.util.TimerTask() {
                @Override
                public void run() {
                    // Find player's team and teleport to spawn
                    for (var team : arena.getTeams()) {
                        if (team.hasPlayer(serverPlayer.getUUID()) && team.getSpawnPoint() != null) {
                            BlockPos spawn = team.getSpawnPoint();
                            serverPlayer.teleportTo(spawn.getX() + 0.5, spawn.getY(), spawn.getZ() + 0.5);
                            serverPlayer.setHealth(serverPlayer.getMaxHealth());
                            serverPlayer.getFoodData().setFoodLevel(20);
                            serverPlayer.displayClientMessage(Component.literal("§aRespawned at team base!"), true);
                            break;
                        }
                    }
                }
            }, 3000); // 3 second delay
        }
    }
    
    @SubscribeEvent
    public static void onPlayerLoggedOut(PlayerEvent.PlayerLoggedOutEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        // Remove player from any active games
        ArenaManager arenaManager = ArenaManager.get();
        for (Arena arena : arenaManager.getArenas()) {
            if (arena.getCurrentGame() != null) {
                Game game = arena.getCurrentGame();
                if (game.hasPlayer(serverPlayer.getUUID())) {
                    MachiavellianMinigames.gameManager.removePlayerFromGame(serverPlayer.getUUID());
                    break;
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onPlayerChangedDimension(PlayerEvent.PlayerChangedDimensionEvent event) {
        Player player = event.getEntity();
        if (!(player instanceof ServerPlayer serverPlayer)) return;
        
        // Check if player left an arena dimension during a game
        ArenaManager arenaManager = ArenaManager.get();
        for (Arena arena : arenaManager.getArenas()) {
            if (arena.getCurrentGame() != null && arena.getDimension().equals(event.getFrom())) {
                Game game = arena.getCurrentGame();
                if (game.hasPlayer(serverPlayer.getUUID())) {
                    // Player left arena dimension during game - remove them
                    MachiavellianMinigames.gameManager.removePlayerFromGame(serverPlayer.getUUID());
                    serverPlayer.sendSystemMessage(Component.literal("§cYou have been removed from the minigame for leaving the arena dimension."));
                    break;
                }
            }
        }
    }
    
    @SubscribeEvent
    public static void onServerTick(TickEvent.ServerTickEvent event) {
        // Only tick on the end phase to avoid double ticking
        if (event.phase == TickEvent.Phase.END) {
            if (MachiavellianMinigames.gameManager != null) {
                MachiavellianMinigames.gameManager.tick();
            }
        }
    }
}
