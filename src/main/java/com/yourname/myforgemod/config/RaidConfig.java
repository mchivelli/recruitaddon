package com.yourname.myforgemod.config;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;

import java.util.Arrays;
import java.util.List;

/**
 * Configuration for raid behavior and entity blacklists
 */
public class RaidConfig {
    
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    // Raid behavior settings
    public static final ForgeConfigSpec.BooleanValue ENABLE_RAID_NOTIFICATIONS;
    public static final ForgeConfigSpec.IntValue NOTIFICATION_RANGE;
    public static final ForgeConfigSpec.IntValue DESTINATION_REACH_DISTANCE;
    
    // Entity blacklist settings
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RAID_ENTITY_BLACKLIST;
    public static final ForgeConfigSpec.BooleanValue BLACKLIST_CREEPERS;
    public static final ForgeConfigSpec.BooleanValue BLACKLIST_ENDERMEN;
    public static final ForgeConfigSpec.BooleanValue BLACKLIST_VILLAGERS;
    
    // Notification settings
    public static final ForgeConfigSpec.BooleanValue NOTIFY_ON_DESTINATION_REACHED;
    public static final ForgeConfigSpec.BooleanValue NOTIFY_ON_COMBAT_START;
    public static final ForgeConfigSpec.BooleanValue NOTIFY_ON_RECRUIT_STUCK;
    public static final ForgeConfigSpec.BooleanValue NOTIFY_ON_RECRUIT_DEATH;
    
    // Command distance settings
    public static final ForgeConfigSpec.IntValue ATTACK_COMMAND_MAX_DISTANCE;
    public static final ForgeConfigSpec.IntValue FOLLOW_COMMAND_MIN_DISTANCE;
    
    // Teleportation settings
    public static final ForgeConfigSpec.BooleanValue ENABLE_TELEPORTATION;
    public static final ForgeConfigSpec.IntValue TELEPORTATION_DISTANCE_THRESHOLD;
    public static final ForgeConfigSpec.IntValue TELEPORTATION_PROGRESS_THRESHOLD;
    
    static {
        BUILDER.comment("Raid / March behaviour settings. Adjust movement, notifications and arrival detection parameters.").push("Raid Behavior");
        
        ENABLE_RAID_NOTIFICATIONS = BUILDER
            .comment("Enable notifications for raid events")
            .define("enableRaidNotifications", true);
            
        NOTIFICATION_RANGE = BUILDER
            .comment("Range in blocks for raid notifications")
            .defineInRange("notificationRange", 100, 10, 500);
            
        DESTINATION_REACH_DISTANCE = BUILDER
            .comment("Distance in blocks to consider destination reached")
            .defineInRange("destinationReachDistance", 5, 1, 20);
        
        BUILDER.pop();
        
        BUILDER.comment("Entity blacklist – recruits will ignore or avoid these entities during raids.").push("Entity Blacklist");
        
        RAID_ENTITY_BLACKLIST = BUILDER
            .comment("List of entity types that recruits should not attack during raids",
                     "Format: modid:entity_name (e.g., minecraft:creeper, minecraft:villager)")
            .defineList("raidEntityBlacklist", 
                Arrays.asList(
                    "minecraft:creeper",
                    "minecraft:villager", 
                    "minecraft:iron_golem",
                    "minecraft:cat",
                    "minecraft:wolf"
                ), 
                obj -> obj instanceof String);
        
        BLACKLIST_CREEPERS = BUILDER
            .comment("Prevent recruits from attacking creepers (to avoid explosions)")
            .define("blacklistCreepers", true);
            
        BLACKLIST_ENDERMEN = BUILDER
            .comment("Prevent recruits from attacking endermen (to avoid teleportation issues)")
            .define("blacklistEndermen", true);
            
        BLACKLIST_VILLAGERS = BUILDER
            .comment("Prevent recruits from attacking villagers and other peaceful NPCs")
            .define("blacklistVillagers", true);
        
        BUILDER.pop();
        
        BUILDER.comment("Toggles for in-game chat notifications sent to the commander player.").push("Notifications");
        
        NOTIFY_ON_DESTINATION_REACHED = BUILDER
            .comment("Notify player when recruits reach their destination")
            .define("notifyOnDestinationReached", true);
            
        NOTIFY_ON_COMBAT_START = BUILDER
            .comment("Notify player when recruits start combat")
            .define("notifyOnCombatStart", true);
            
        NOTIFY_ON_RECRUIT_STUCK = BUILDER
            .comment("Notify player when recruits appear to be stuck")
            .define("notifyOnRecruitStuck", true);
            
        NOTIFY_ON_RECRUIT_DEATH = BUILDER
            .comment("Notify player when a recruit dies")
            .define("notifyOnRecruitDeath", true);
        
        BUILDER.pop();
        
        BUILDER.comment("Distance thresholds used by various /recruits commands.").push("Commands");
        
        ATTACK_COMMAND_MAX_DISTANCE = BUILDER
            .comment("Maximum distance to target player for attack command (in blocks)")
            .defineInRange("attackCommandMaxDistance", 125, 10, 500);
        
        FOLLOW_COMMAND_MIN_DISTANCE = BUILDER
            .comment("Minimum distance required for follow command (in blocks)")
            .defineInRange("followCommandMinDistance", 50, 5, 200);
        
        BUILDER.pop();
        
        BUILDER.comment("Automatic teleportation of lagging recruits during march / raid approach phase. Disable or fine-tune here.").push("Teleportation");
        
        ENABLE_TELEPORTATION = BUILDER
            .comment("Enable automatic teleportation of lagging recruits during movement")
            .define("enableTeleportation", true);
            
        TELEPORTATION_DISTANCE_THRESHOLD = BUILDER
            .comment("Distance (in blocks) behind the group's average position at which a recruit is considered 'lagging'.  If exceeded, the recruit will be teleported to the group centre.")
            .defineInRange("teleportationDistanceThreshold", 30, 10, 100);
            
        TELEPORTATION_PROGRESS_THRESHOLD = BUILDER
            .comment("Safety margin (in blocks) the group must have progressed from their initial position before any teleportation can occur. Prevents recruits warping at mission start.")
            .defineInRange("teleportationProgressThreshold", 50, 20, 200);
        
        BUILDER.pop();
        
        SPEC = BUILDER.build();
    }
    
    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.COMMON, SPEC, "recruits-addon.toml");
    }
    
    /**
     * Check if an entity type is blacklisted from being attacked during raids
     */
    public static boolean isEntityBlacklisted(String entityType) {
        List<? extends String> blacklist = RAID_ENTITY_BLACKLIST.get();
        return blacklist.contains(entityType);
    }
    
    /**
     * Check if creepers should be avoided
     */
    public static boolean shouldAvoidCreepers() {
        return BLACKLIST_CREEPERS.get();
    }
    
    /**
     * Check if endermen should be avoided
     */
    public static boolean shouldAvoidEndermen() {
        return BLACKLIST_ENDERMEN.get();
    }
    
    /**
     * Check if villagers should be avoided
     */
    public static boolean shouldAvoidVillagers() {
        return BLACKLIST_VILLAGERS.get();
    }
    
    /**
     * Get the distance to consider destination reached
     */
    public static int getDestinationReachDistance() {
        return DESTINATION_REACH_DISTANCE.get();
    }
    
    /**
     * Get the notification range
     */
    public static int getNotificationRange() {
        return NOTIFICATION_RANGE.get();
    }
}
