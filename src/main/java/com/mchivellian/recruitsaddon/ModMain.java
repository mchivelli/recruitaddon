package com.mchivellian.recruitsaddon;

import com.mchivellian.recruitsaddon.client.ClientSetup;
import com.mchivellian.recruitsaddon.commands.MarchCommands;
import com.mchivellian.recruitsaddon.commands.RaidCommands;
import com.mchivellian.recruitsaddon.commands.RecruitMovementCommands;
import com.mchivellian.recruitsaddon.commands.SimpleRecruitsCommands;
import com.mchivellian.recruitsaddon.commands.WorkingRecruitsCommands;
import com.mchivellian.recruitsaddon.commands.OfficialStyleCommands;
import com.mchivellian.recruitsaddon.network.NetworkHandler;
import com.mchivellian.recruitsaddon.raid.RaidManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.server.ServerStartingEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/* [Guide: ModMain.java is the entry point of your Forge mod.
   - The @Mod annotation registers this class with Forge using the unique mod ID "examplemod".
   - The constructor sets up the mod by registering items, blocks, tile entities, and configuration.
   - It also adds event listeners for both common (server and client) and client-specific setup.
   - Use this file to initialize your mod's core functionality without modifying the critical steps.
] */
@Mod(ModMain.MODID)
public class ModMain {

  public static final String MODID = "recruitsaddon"; // Changed to match mods.toml
  public static final Logger LOGGER = LogManager.getLogger();

  public ModMain() {
    // [Guide: Retrieve the mod event bus for registering events during mod loading.]
    IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

    // [Guide: Register your mod's blocks, items, and tile entities so they are initialized correctly.]
    // ItemInit.ITEMS.register(modEventBus);

    // [Guide: Initialize the configuration settings for your mod.]
    new ConfigManager();

    // [Guide: Add listeners for common and client-specific setup events.]
    modEventBus.addListener(this::commonSetup);
    DistExecutor.unsafeRunWhenOn(Dist.CLIENT, () -> () -> modEventBus.addListener(ClientSetup::init));

    // Register ourselves for server and other game events we are interested in
    MinecraftForge.EVENT_BUS.register(this);
    MinecraftForge.EVENT_BUS.register(RaidManager.class);
    MinecraftForge.EVENT_BUS.register(com.mchivellian.recruitsaddon.raid.AdvancedRaidManager.class);
    
    // Register configuration
    com.mchivellian.recruitsaddon.config.RaidConfig.register();
  }

  private void commonSetup(final FMLCommonSetupEvent event) {
    // [Guide: Common setup method. Use this to initialize logic that should run on both client and server.]
    LOGGER.info("Enhanced March Mod setup.");
    
    // Initialize network handler
    event.enqueueWork(() -> {
      NetworkHandler.registerMessages();
    });
  }

  @SubscribeEvent
  public void onRegisterCommands(RegisterCommandsEvent event) {
    // Disabled conflicting command registrations - using only SimpleRecruitsCommands
    // RaidCommands.register(event.getDispatcher());
    // MarchCommands.register(event.getDispatcher());
    // RecruitMovementCommands.register(event.getDispatcher());
    SimpleRecruitsCommands.register(event.getDispatcher());
    WorkingRecruitsCommands.register(event.getDispatcher()); // NEW WORKING IMPLEMENTATION FOR TESTING
    OfficialStyleCommands.register(event.getDispatcher()); // OFFICIAL-STYLE RECRUIT DISCOVERY TESTING
  }

  /**
   * Method to handle server starting event.
   */
  @SubscribeEvent
  public void onServerStarting(ServerStartingEvent event) {
    // Do something when the server starts
    LOGGER.info("Enhanced March Mod is ready.");
    // Initialize Mod Systems
    //TaxManager.initialize(event.getServer());
  }
}
