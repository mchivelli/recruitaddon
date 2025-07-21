package com.mchivelli.machiavellianminigames;

import com.mchivelli.machiavellianminigames.commands.ModCommands;
import com.mchivelli.machiavellianminigames.core.ArenaManager;
import com.mchivelli.machiavellianminigames.core.GameManager;
import com.mchivelli.machiavellianminigames.events.MinigameEvents;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import net.minecraftforge.eventbus.api.IEventBus;
// Import removed: net.minecraftforge.fml.ModLoadingContext - not needed
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(MachiavellianMinigames.MODID)
public class MachiavellianMinigames {

    public static final String MODID = "machiavellianminigames";
    public static final Logger LOGGER = LogManager.getLogger();
    
    // Core system managers
    public static ArenaManager arenaManager;
    public static GameManager gameManager;

    public MachiavellianMinigames() {
        // We need to use the get() method for Forge 1.20.1 compatibility
        // This is marked deprecated but is the correct way in this version
        // For future reference: in newer versions this would change
        @SuppressWarnings("deprecation")
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // Register mod content
        ModRegistry.BLOCKS.register(eventBus);
        ModRegistry.ITEMS.register(eventBus);
        ModRegistry.TILE_ENTITIES.register(eventBus);
        
        // Initialize configuration
        new ConfigManager();
        
        // Setup event listeners
        eventBus.addListener(this::setup);
        eventBus.addListener(this::setupClient);
        
        // Register server events
        MinecraftForge.EVENT_BUS.register(this);
        
        // Initialize core managers early to avoid null pointers
        arenaManager = new ArenaManager();
        gameManager = new GameManager();
    }

    private void setup(final FMLCommonSetupEvent event) {
        // Core managers are already initialized in the constructor
        // No need to initialize them again
        
        // Register event handlers
        MinecraftForge.EVENT_BUS.register(MinigameEvents.class);
        
        LOGGER.info("Machiavellian Minigames initialized!");
    }

    private void setupClient(final FMLClientSetupEvent event) {
        // Client-side setup - GUI initialization, etc.
    }

    @SubscribeEvent
    public void onServerStarting(ServerStartingEvent event) {
        LOGGER.info("Machiavellian Minigames server starting...");
        // Initialize server-side systems
        if (arenaManager != null) {
            arenaManager.loadArenas();
        }
    }

    @SubscribeEvent
    public void onRegisterCommands(RegisterCommandsEvent event) {
        ModCommands.register(event.getDispatcher());
        LOGGER.info("Machiavellian Minigames commands registered.");
    }
}
