package com.mchivelli.machiavellianminigames.minigames;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.config.ModConfigEvent;

/**
 * Configuration for Storm the Front minigame
 * Isolated configuration system for all Storm the Front specific settings
 */
@Mod.EventBusSubscriber(modid = "machiavellianminigames", bus = Mod.EventBusSubscriber.Bus.MOD)
public class StormTheFrontConfig {
    
    private static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;
    
    // Checkpoint claiming settings
    public static final ForgeConfigSpec.IntValue CLAIM_TIME_SECONDS;
    public static final ForgeConfigSpec.IntValue ABANDON_TIMEOUT_SECONDS;
    
    // Resource distribution settings
    public static final ForgeConfigSpec.IntValue MONEY_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.IntValue UPGRADE_INTERVAL_SECONDS;
    public static final ForgeConfigSpec.IntValue BASE_MONEY_AMOUNT;
    public static final ForgeConfigSpec.IntValue BASE_UPGRADE_AMOUNT;
    
    // Capture bonuses
    public static final ForgeConfigSpec.IntValue MONEY_CAPTURE_BONUS;
    public static final ForgeConfigSpec.IntValue UPGRADE_CAPTURE_BONUS;
    
    // Boss bar settings
    public static final ForgeConfigSpec.BooleanValue SHOW_PROGRESS_PERCENTAGE;
    public static final ForgeConfigSpec.BooleanValue SHOW_LOSING_INDICATOR;
    public static final ForgeConfigSpec.IntValue CONTESTED_MESSAGE_COOLDOWN_SECONDS;
    
    // Game duration
    public static final ForgeConfigSpec.IntValue GAME_DURATION_MINUTES;
    
    static {
        BUILDER.push("Storm the Front Configuration");
        
        BUILDER.push("Checkpoint Claiming");
        CLAIM_TIME_SECONDS = BUILDER
            .comment("Time in seconds to claim a checkpoint")
            .defineInRange("claimTimeSeconds", 10, 1, 60);
        ABANDON_TIMEOUT_SECONDS = BUILDER
            .comment("Time in seconds before abandoning claim when no players present")
            .defineInRange("abandonTimeoutSeconds", 3, 1, 10);
        BUILDER.pop();
        
        BUILDER.push("Resource Distribution");
        MONEY_INTERVAL_SECONDS = BUILDER
            .comment("Interval in seconds for money resource distribution")
            .defineInRange("moneyIntervalSeconds", 60, 10, 300);
        UPGRADE_INTERVAL_SECONDS = BUILDER
            .comment("Interval in seconds for upgrade resource distribution")
            .defineInRange("upgradeIntervalSeconds", 300, 30, 600);
        BASE_MONEY_AMOUNT = BUILDER
            .comment("Base amount of money resources per distribution")
            .defineInRange("baseMoneyAmount", 1, 1, 10);
        BASE_UPGRADE_AMOUNT = BUILDER
            .comment("Base amount of upgrade resources per distribution")
            .defineInRange("baseUpgradeAmount", 1, 1, 5);
        BUILDER.pop();
        
        BUILDER.push("Capture Bonuses");
        MONEY_CAPTURE_BONUS = BUILDER
            .comment("Immediate money bonus when capturing a money checkpoint")
            .defineInRange("moneyCaptureBonus", 2, 0, 10);
        UPGRADE_CAPTURE_BONUS = BUILDER
            .comment("Immediate upgrade bonus when capturing an upgrade checkpoint")
            .defineInRange("upgradeCaptureBonus", 1, 0, 5);
        BUILDER.pop();
        
        BUILDER.push("Boss Bar Settings");
        SHOW_PROGRESS_PERCENTAGE = BUILDER
            .comment("Show progress percentage in boss bar")
            .define("showProgressPercentage", true);
        SHOW_LOSING_INDICATOR = BUILDER
            .comment("Show 'LOSING' indicator when players abandon claim")
            .define("showLosingIndicator", true);
        CONTESTED_MESSAGE_COOLDOWN_SECONDS = BUILDER
            .comment("Cooldown in seconds between contested messages")
            .defineInRange("contestedMessageCooldownSeconds", 3, 1, 10);
        BUILDER.pop();
        
        BUILDER.push("Game Settings");
        GAME_DURATION_MINUTES = BUILDER
            .comment("Game duration in minutes (0 = unlimited)")
            .defineInRange("gameDurationMinutes", 0, 0, 120);
        BUILDER.pop();
        
        BUILDER.pop();
        SPEC = BUILDER.build();
    }
    
    // Cached values for performance
    private static long claimTimeMs = 10000;
    private static long abandonTimeoutMs = 3000;
    private static long moneyIntervalMs = 60000;
    private static long upgradeIntervalMs = 300000;
    private static int baseMoneyAmount = 1;
    private static int baseUpgradeAmount = 1;
    private static int moneyCaptureBonus = 2;
    private static int upgradeCaptureBonus = 1;
    private static boolean showProgressPercentage = true;
    private static boolean showLosingIndicator = true;
    private static long contestedMessageCooldownMs = 3000;
    private static long gameDurationMs = 0;
    
    @SubscribeEvent
    static void onLoad(final ModConfigEvent event) {
        // Update cached values when config is loaded/reloaded
        claimTimeMs = CLAIM_TIME_SECONDS.get() * 1000L;
        abandonTimeoutMs = ABANDON_TIMEOUT_SECONDS.get() * 1000L;
        moneyIntervalMs = MONEY_INTERVAL_SECONDS.get() * 1000L;
        upgradeIntervalMs = UPGRADE_INTERVAL_SECONDS.get() * 1000L;
        baseMoneyAmount = BASE_MONEY_AMOUNT.get();
        baseUpgradeAmount = BASE_UPGRADE_AMOUNT.get();
        moneyCaptureBonus = MONEY_CAPTURE_BONUS.get();
        upgradeCaptureBonus = UPGRADE_CAPTURE_BONUS.get();
        showProgressPercentage = SHOW_PROGRESS_PERCENTAGE.get();
        showLosingIndicator = SHOW_LOSING_INDICATOR.get();
        contestedMessageCooldownMs = CONTESTED_MESSAGE_COOLDOWN_SECONDS.get() * 1000L;
        gameDurationMs = GAME_DURATION_MINUTES.get() * 60000L;
    }
    
    // Getters for cached values (for performance)
    public static long getClaimTimeMs() { return claimTimeMs; }
    public static long getAbandonTimeoutMs() { return abandonTimeoutMs; }
    public static long getMoneyIntervalMs() { return moneyIntervalMs; }
    public static long getUpgradeIntervalMs() { return upgradeIntervalMs; }
    public static int getBaseMoneyAmount() { return baseMoneyAmount; }
    public static int getBaseUpgradeAmount() { return baseUpgradeAmount; }
    public static int getMoneyCaptureBonus() { return moneyCaptureBonus; }
    public static int getUpgradeCaptureBonus() { return upgradeCaptureBonus; }
    public static boolean showProgressPercentage() { return showProgressPercentage; }
    public static boolean showLosingIndicator() { return showLosingIndicator; }
    public static long getContestedMessageCooldownMs() { return contestedMessageCooldownMs; }
    public static long getGameDurationMs() { return gameDurationMs; }
    
    // Additional getters for isolated system
    public static int getMoneyPerInterval() { return baseMoneyAmount; }
    public static int getUpgradesPerInterval() { return baseUpgradeAmount; }
    public static int getCaptureMoneyBonus() { return moneyCaptureBonus; }
    public static int getCaptureUpgradeBonus() { return upgradeCaptureBonus; }
}
