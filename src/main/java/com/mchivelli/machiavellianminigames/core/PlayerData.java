package com.mchivelli.machiavellianminigames.core;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.item.Item;

public class PlayerData {
    private final ServerPlayer player;
    private ItemStack[] savedInventory;
    private ItemStack[] savedArmor;
    private ItemStack savedOffHand;
    private int savedXP;
    private int savedFoodLevel;
    private float savedSaturation;
    private float savedHealth;
    private BlockPos savedPosition;
    private GameType savedGameMode;
    private boolean dataSaved = false;
    
    // Game-specific data
    private int moneyCoins = 0; // Diamond count
    private int upgradeItems = 0; // Emerald count
    private long lastMoneyCoinTime = 0;
    private long lastUpgradeTime = 0;
    
    // Acquired checkpoints for rewards
    private int moneyCheckpoints = 0;
    private int upgradeCheckpoints = 0;
    
    public PlayerData(ServerPlayer player) {
        this.player = player;
    }
    
    public void save() {
        if (dataSaved) return;
        
        // Save inventory
        savedInventory = new ItemStack[player.getInventory().items.size()];
        for (int i = 0; i < player.getInventory().items.size(); i++) {
            savedInventory[i] = player.getInventory().items.get(i).copy();
        }
        
        // Save armor
        savedArmor = new ItemStack[player.getInventory().armor.size()];
        for (int i = 0; i < player.getInventory().armor.size(); i++) {
            savedArmor[i] = player.getInventory().armor.get(i).copy();
        }
        
        // Save offhand
        savedOffHand = player.getOffhandItem().copy();
        
        // Save other stats
        savedXP = player.totalExperience;
        savedFoodLevel = player.getFoodData().getFoodLevel();
        savedSaturation = player.getFoodData().getSaturationLevel();
        savedHealth = player.getHealth();
        savedPosition = player.blockPosition();
        savedGameMode = player.gameMode.getGameModeForPlayer();
        
        dataSaved = true;
    }
    
    public void restore() {
        if (!dataSaved) return;
        
        // Clear current inventory
        player.getInventory().clearContent();
        
        // Restore inventory
        for (int i = 0; i < savedInventory.length && i < player.getInventory().items.size(); i++) {
            player.getInventory().items.set(i, savedInventory[i].copy());
        }
        
        // Restore armor
        for (int i = 0; i < savedArmor.length && i < player.getInventory().armor.size(); i++) {
            player.getInventory().armor.set(i, savedArmor[i].copy());
        }
        
        // Restore offhand
        player.getInventory().offhand.set(0, savedOffHand.copy());
        
        // Restore other stats
        player.giveExperiencePoints(savedXP - player.totalExperience);
        player.getFoodData().setFoodLevel(savedFoodLevel);
        player.getFoodData().setSaturation(savedSaturation);
        player.setHealth(savedHealth);
        player.setGameMode(savedGameMode);
        
        // Teleport back to saved position
        if (savedPosition != null) {
            player.teleportTo(savedPosition.getX() + 0.5, savedPosition.getY(), savedPosition.getZ() + 0.5);
        }
        
        dataSaved = false;
    }
    
    public void clearForMinigame() {
        // Clear inventory and reset stats for minigame
        player.getInventory().clearContent();
        player.setExperienceLevels(0);
        player.setExperiencePoints(0);
        player.getFoodData().setFoodLevel(20);
        player.getFoodData().setSaturation(20.0f);
        player.setHealth(player.getMaxHealth());
    }
    
    /**
     * Award money coins (diamonds) to the player
     * 
     * @param amount The number of coins to award
     */
    public void awardMoneyCoin(int amount) {
        moneyCoins += amount;
        giveItem(Items.DIAMOND, amount);
    }
    
    /**
     * Award a single money coin (diamond) to the player
     */
    public void awardMoneyCoin() {
        awardMoneyCoin(1);
    }
    
    /**
     * Award upgrade items (emeralds) to the player
     * 
     * @param amount The number of items to award
     */
    public void awardUpgradeItem(int amount) {
        upgradeItems += amount;
        giveItem(Items.EMERALD, amount);
    }
    
    /**
     * Award a single upgrade item (emerald) to the player
     */
    public void awardUpgradeItem() {
        awardUpgradeItem(1);
    }
    
    /**
     * Add a checkpoint of the specified type to the player's data
     * @param type The type of checkpoint acquired
     */
    public void addCheckpoint(CheckpointZone.CheckpointType type) {
        if (type == CheckpointZone.CheckpointType.MONEY) {
            moneyCheckpoints++;
        } else if (type == CheckpointZone.CheckpointType.UPGRADE) {
            upgradeCheckpoints++;
        }
    }
    
    /**
     * Helper method to give an item to the player
     * @param item The item to give
     * @param count The number of items to give
     */
    private void giveItem(Item item, int count) {
        ItemStack stack = new ItemStack(item, count);
        if (!player.getInventory().add(stack)) {
            player.drop(stack, false);
        }
    }
    
    /**
     * Update the reward timers and give periodic rewards
     * @param currentTime Current system time in milliseconds
     * @param moneyInterval Interval in milliseconds for money rewards
     * @param upgradeInterval Interval in milliseconds for upgrade rewards
     */
    public void updateRewards(long currentTime, long moneyInterval, long upgradeInterval) {
        // Check if it's time for money rewards
        if (lastMoneyCoinTime == 0 || currentTime - lastMoneyCoinTime >= moneyInterval) {
            // Award base amount (1)
            int moneyReward = 1;
            
            // Double reward for each money checkpoint controlled
            // 0 checkpoints = 1 diamond (base)
            // 1 checkpoint = 2 diamonds (doubled)
            // 2 checkpoints = 4 diamonds (doubled again)
            for (int i = 0; i < moneyCheckpoints; i++) {
                moneyReward *= 2; // Double for each checkpoint
            }
            
            // Award the diamonds
            awardMoneyCoin(moneyReward);
            lastMoneyCoinTime = currentTime;
            
            // Debug log
            if (player != null) {
                player.sendSystemMessage(Component.literal("§bYou received " + moneyReward + 
                    " diamonds from controlling " + moneyCheckpoints + " money checkpoint(s)."));
            }
        }
        
        // Check if it's time for upgrade rewards
        if (lastUpgradeTime == 0 || currentTime - lastUpgradeTime >= upgradeInterval) {
            // Award base amount (1)
            int upgradeReward = 1;
            
            // Double reward for each upgrade checkpoint controlled
            // 0 checkpoints = 1 emerald (base)
            // 1 checkpoint = 2 emeralds (doubled)
            // 2 checkpoints = 4 emeralds (doubled again)
            for (int i = 0; i < upgradeCheckpoints; i++) {
                upgradeReward *= 2; // Double for each checkpoint
            }
            
            // Award the emeralds
            awardUpgradeItem(upgradeReward);
            lastUpgradeTime = currentTime;
            
            // Debug log
            if (player != null) {
                player.sendSystemMessage(Component.literal("§2You received " + upgradeReward + 
                    " emeralds from controlling " + upgradeCheckpoints + " upgrade checkpoint(s)."));
            }
        }
    }
    
    // Getters and setters
    public ServerPlayer getPlayer() { return player; }
    public boolean isDataSaved() { return dataSaved; }
    public int getMoneyCoins() { return moneyCoins; }
    public int getUpgradeItems() { return upgradeItems; }
    public int getMoneyCheckpoints() { return moneyCheckpoints; }
    public int getUpgradeCheckpoints() { return upgradeCheckpoints; }
    public long getLastMoneyCoinTime() { return lastMoneyCoinTime; }
    public long getLastUpgradeTime() { return lastUpgradeTime; }
    public void setLastMoneyCoinTime(long time) { this.lastMoneyCoinTime = time; }
    public void setLastUpgradeTime(long time) { this.lastUpgradeTime = time; }
    public void setMoneyCheckpoints(int count) { this.moneyCheckpoints = count; }
    public void setUpgradeCheckpoints(int count) { this.upgradeCheckpoints = count; }
}
