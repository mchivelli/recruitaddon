package com.mchivelli.machiavellianminigames;

import com.electronwill.nightconfig.core.file.CommentedFileConfig;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.common.ForgeConfigSpec.*;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

@Mod.EventBusSubscriber
public class ConfigManager {

    public static ForgeConfigSpec CONFIG;
    
    // Storm the Front configuration
    public static IntValue STORM_DURATION;
    public static IntValue STORM_RESOURCE_INTERVAL;
    public static ConfigValue<String> STORM_MONEY_ITEM;
    public static ConfigValue<String> STORM_UPGRADE_ITEM;
    public static IntValue STORM_CLAIM_TIME;
    public static IntValue STORM_RESET_TIMER;
    
    // Resource distribution configuration
    public static IntValue STORM_MONEY_AMOUNT;
    public static IntValue STORM_UPGRADE_AMOUNT;
    public static IntValue STORM_MONEY_INTERVAL;
    public static IntValue STORM_UPGRADE_INTERVAL;
    
    // General configuration
    public static BooleanValue ENABLE_DEBUG_LOGGING;
    public static IntValue MAX_PLAYERS_PER_TEAM;
    public static BooleanValue ENABLE_FRIENDLY_FIRE;
    
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    
    static {
        BUILDER.push("General");
        ENABLE_DEBUG_LOGGING = BUILDER
            .comment("Enable debug logging for troubleshooting")
            .define("enableDebugLogging", false);
        MAX_PLAYERS_PER_TEAM = BUILDER
            .comment("Maximum players per team (0 = unlimited)")
            .defineInRange("maxPlayersPerTeam", 0, 0, 100);
        ENABLE_FRIENDLY_FIRE = BUILDER
            .comment("Allow team members to damage each other")
            .define("enableFriendlyFire", false);
        BUILDER.pop();

        BUILDER.push("StormTheFront");
        STORM_DURATION = BUILDER
            .comment("Default game duration in seconds (min: 300, max: 7200)")
            .defineInRange("duration", 1800, 300, 7200);
        STORM_RESOURCE_INTERVAL = BUILDER
            .comment("Resource distribution interval in seconds")
            .defineInRange("resourceInterval", 60, 10, 300);
        STORM_MONEY_ITEM = BUILDER
            .comment("Item to use as money resource")
            .define("moneyItem", "minecraft:diamond");
        STORM_UPGRADE_ITEM = BUILDER
            .comment("Item to use as upgrade resource")
            .define("upgradeItem", "minecraft:emerald");
        STORM_CLAIM_TIME = BUILDER
            .comment("Time required to claim a checkpoint in seconds")
            .defineInRange("claimTime", 10, 5, 60);
        STORM_RESET_TIMER = BUILDER
            .comment("Time before checkpoint progress resets when left alone")
            .defineInRange("resetTimer", 3, 1, 30);
        BUILDER.pop();
        
        BUILDER.push("ResourceDistribution");
        STORM_MONEY_AMOUNT = BUILDER
            .comment("Amount of money items to give per resource distribution")
            .defineInRange("moneyAmount", 1, 1, 64);
        STORM_UPGRADE_AMOUNT = BUILDER
            .comment("Amount of upgrade items to give per resource distribution")
            .defineInRange("upgradeAmount", 1, 1, 64);
        STORM_MONEY_INTERVAL = BUILDER
            .comment("Money resource distribution interval in seconds")
            .defineInRange("moneyInterval", 60, 5, 300);
        STORM_UPGRADE_INTERVAL = BUILDER
            .comment("Upgrade resource distribution interval in seconds")
            .defineInRange("upgradeInterval", 300, 5, 600);
        BUILDER.pop();
        
        CONFIG = BUILDER.build();
    }

    public ConfigManager() {
        loadConfig(CONFIG, MachiavellianMinigames.MODID + ".toml");
    }

    public static void loadConfig(ForgeConfigSpec config, String path) {
        final Path configPath = FMLPaths.CONFIGDIR.get().resolve(path);
        final CommentedFileConfig file = CommentedFileConfig.builder(configPath)
                .sync()
                .autosave()
                .preserveInsertionOrder()
                .build();

        file.load();
        config.setConfig(file);
    }
}
