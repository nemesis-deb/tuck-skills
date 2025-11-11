package com.example.skillsplugin.events;

import com.example.skillsplugin.data.PlayerDataManager;
import com.example.skillsplugin.skills.BonusManager;
import com.example.skillsplugin.skills.ExperienceCalculator;
import com.example.skillsplugin.skills.SkillProfile;
import com.example.skillsplugin.skills.SkillType;
import com.example.skillsplugin.ui.UIManager;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.enchantment.EnchantItemEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityPickupItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.inventory.MerchantRecipe;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Listens to Minecraft events and awards skill experience to players.
 * Handles all skill-related gameplay actions including mining, woodcutting,
 * combat, farming, fishing, enchanting, and trading.
 */
public class SkillEventListener implements Listener {
    
    private final PlayerDataManager playerDataManager;
    private final ExperienceCalculator experienceCalculator;
    private final UIManager uiManager;
    private final BonusManager bonusManager;
    private final Logger logger;
    
    /**
     * Creates a new skill event listener.
     * 
     * @param playerDataManager The player data manager for awarding XP
     * @param experienceCalculator The calculator for determining XP amounts
     * @param uiManager The UI manager for player feedback
     * @param bonusManager The bonus manager for applying skill bonuses
     * @param logger The logger for error handling
     */
    public SkillEventListener(PlayerDataManager playerDataManager, 
                             ExperienceCalculator experienceCalculator,
                             UIManager uiManager,
                             BonusManager bonusManager,
                             Logger logger) {
        this.playerDataManager = playerDataManager;
        this.experienceCalculator = experienceCalculator;
        this.uiManager = uiManager;
        this.bonusManager = bonusManager;
        this.logger = logger;
    }
    
    /**
     * Handles block break events for Mining, Woodcutting, and Farming skills.
     * Awards XP based on the type of block broken and applies skill bonuses.
     * Optimized with early returns and minimal object creation.
     * 
     * @param event The block break event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        try {
            Player player = event.getPlayer();
            if (player == null) {
                return; // Early return for invalid player
            }
            
            Block block = event.getBlock();
            Material material = block.getType();
            
            // Check for Mining XP (most common, check first)
            double miningXP = experienceCalculator.calculateMiningXP(material);
            if (miningXP > 0) {
                awardExperienceAndNotify(player, SkillType.MINING, miningXP);
                bonusManager.applyMiningBonus(player, block);
                return; // Only award one skill type per action
            }
            
            // Check for Woodcutting XP
            double woodcuttingXP = experienceCalculator.calculateWoodcuttingXP(material);
            if (woodcuttingXP > 0) {
                awardExperienceAndNotify(player, SkillType.WOODCUTTING, woodcuttingXP);
                bonusManager.applyWoodcuttingBonus(player, block);
                return;
            }
            
            // Check for Farming XP (only mature crops) - check maturity first to avoid calculation
            if (isMatureCrop(block)) {
                double farmingXP = experienceCalculator.calculateFarmingXP(material);
                if (farmingXP > 0) {
                    awardExperienceAndNotify(player, SkillType.FARMING, farmingXP);
                    bonusManager.applyFarmingBonus(player, block);
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling block break event", e);
        }
    }
    
    /**
     * Handles entity damage events for Combat skill bonus.
     * Applies damage multiplier based on Combat skill level.
     * Optimized with early return and minimal calculations.
     * 
     * @param event The entity damage event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEntityDamage(EntityDamageByEntityEvent event) {
        try {
            // Early return if not player damage
            if (!(event.getDamager() instanceof Player)) {
                return;
            }
            
            Player player = (Player) event.getDamager();
            
            // Apply combat damage multiplier
            double multiplier = bonusManager.getCombatDamageMultiplier(player);
            
            // Only modify damage if there's actually a bonus (avoid unnecessary setDamage calls)
            if (multiplier != 1.0) {
                event.setDamage(event.getDamage() * multiplier);
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling entity damage event", e);
        }
    }
    
    /**
     * Handles entity death events for Combat skill.
     * Awards XP when a player kills a mob.
     * Optimized with early returns.
     * 
     * @param event The entity death event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onEntityDeath(EntityDeathEvent event) {
        try {
            // Early return if not killed by a player
            Player killer = event.getEntity().getKiller();
            if (killer == null) {
                return;
            }
            
            // Calculate and award XP
            double combatXP = experienceCalculator.calculateCombatXP(event.getEntityType());
            if (combatXP > 0) {
                awardExperienceAndNotify(killer, SkillType.COMBAT, combatXP);
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling entity death event", e);
        }
    }
    
    /**
     * Handles player fish events for Fishing skill.
     * Awards XP when a player successfully catches something and applies fishing bonus.
     * Optimized with early returns and reduced object creation.
     * 
     * @param event The player fish event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onPlayerFish(PlayerFishEvent event) {
        try {
            // Early return if not a successful catch
            if (event.getState() != PlayerFishEvent.State.CAUGHT_FISH) {
                return;
            }
            
            Player player = event.getPlayer();
            if (player == null) {
                return;
            }
            
            // Get the caught item from the entity
            ItemStack caught = null;
            if (event.getCaught() instanceof org.bukkit.entity.Item) {
                org.bukkit.entity.Item itemEntity = (org.bukkit.entity.Item) event.getCaught();
                caught = itemEntity.getItemStack();
                
                // Apply fishing bonus (may upgrade to treasure)
                ItemStack bonusItem = bonusManager.applyFishingBonus(player, caught);
                if (bonusItem != null && !bonusItem.equals(caught)) {
                    itemEntity.setItemStack(bonusItem);
                }
            }
            
            // Calculate and award XP
            double fishingXP = experienceCalculator.calculateFishingXP(caught);
            if (fishingXP > 0) {
                awardExperienceAndNotify(player, SkillType.FISHING, fishingXP);
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling player fish event", e);
        }
    }
    
    /**
     * Handles enchant item events for Enchanting skill.
     * Awards XP when a player enchants an item and applies cost reduction bonus.
     * Optimized with early validation.
     * 
     * @param event The enchant item event
     */
    @EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
    public void onEnchantItem(EnchantItemEvent event) {
        try {
            Player player = event.getEnchanter();
            if (player == null) {
                return;
            }
            
            int originalCost = event.getExpLevelCost();
            
            // Apply enchanting cost reduction
            int reducedCost = bonusManager.getEnchantingCostReduction(player, originalCost);
            if (reducedCost != originalCost) {
                event.setExpLevelCost(reducedCost);
            }
            
            // Calculate and award XP
            double enchantingXP = experienceCalculator.calculateEnchantingXP(originalCost);
            if (enchantingXP > 0) {
                awardExperienceAndNotify(player, SkillType.ENCHANTING, enchantingXP);
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling enchant item event", e);
        }
    }
    
    /**
     * Handles inventory click events for Trading skill.
     * Awards XP when a player completes a trade with a villager.
     * Optimized with early returns to minimize processing.
     * 
     * @param event The inventory click event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent event) {
        try {
            // Early return: Check if player is clicking the result slot (slot 2 in merchant inventory)
            if (event.getRawSlot() != 2) {
                return;
            }
            
            // Early return: Check if this is a merchant/villager trade
            Inventory inventory = event.getInventory();
            if (!(inventory instanceof MerchantInventory)) {
                return;
            }
            
            // Early return: Ensure it's a player
            if (!(event.getWhoClicked() instanceof Player)) {
                return;
            }
            
            Player player = (Player) event.getWhoClicked();
            MerchantInventory merchantInventory = (MerchantInventory) inventory;
            MerchantRecipe selectedRecipe = merchantInventory.getSelectedRecipe();
            
            // Award XP if a valid trade was made
            if (selectedRecipe != null) {
                double tradingXP = experienceCalculator.calculateTradingXP(selectedRecipe);
                if (tradingXP > 0) {
                    awardExperienceAndNotify(player, SkillType.TRADING, tradingXP);
                }
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling inventory click event for trading", e);
        }
    }
    
    /**
     * Handles item pickup events for Mining and Woodcutting skills.
     * Awards XP based on items picked up - this works with veinminer datapacks
     * that break blocks without firing BlockBreakEvent.
     * 
     * @param event The entity pickup item event
     */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onItemPickup(EntityPickupItemEvent event) {
        try {
            // Only handle player pickups
            if (!(event.getEntity() instanceof Player)) {
                return;
            }
            
            Player player = (Player) event.getEntity();
            ItemStack item = event.getItem().getItemStack();
            Material material = item.getType();
            int amount = item.getAmount();
            
            // Check for Mining XP from ore drops
            double miningXPPerItem = experienceCalculator.calculateMiningXP(material);
            if (miningXPPerItem > 0) {
                double totalXP = miningXPPerItem * amount;
                awardExperienceAndNotify(player, SkillType.MINING, totalXP);
                return;
            }
            
            // Check for Woodcutting XP from log drops
            double woodcuttingXPPerItem = experienceCalculator.calculateWoodcuttingXP(material);
            if (woodcuttingXPPerItem > 0) {
                double totalXP = woodcuttingXPPerItem * amount;
                awardExperienceAndNotify(player, SkillType.WOODCUTTING, totalXP);
                return;
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error handling item pickup event", e);
        }
    }
    
    /**
     * Awards experience to a player and triggers UI feedback.
     * Handles both XP gain messages and level-up notifications.
     * Saves player data after level-ups to prevent data loss.
     * 
     * @param player The player to award XP to
     * @param skillType The skill type to award XP for
     * @param amount The amount of XP to award
     */
    private void awardExperienceAndNotify(Player player, SkillType skillType, double amount) {
        PlayerDataManager.ExperienceResult result = 
            playerDataManager.awardExperience(player, skillType, amount);
        
        // Show XP gain message
        uiManager.sendXPGainMessage(player, skillType, result.getExperienceGained());
        
        // Show level-up boss bar if leveled up
        if (result.isLeveledUp()) {
            SkillProfile profile = playerDataManager.getProfile(player.getUniqueId());
            int newLevel = profile.getSkill(skillType).getLevel();
            uiManager.showLevelUpBossBar(player, skillType, newLevel);
            
            // Update display name if this skill is currently displayed
            uiManager.updateDisplayNameIfNeeded(player, profile, skillType);
            
            // Save player data immediately after level-up to prevent data loss
            playerDataManager.saveProfile(player.getUniqueId());
            logger.log(Level.FINE, "Saved profile for player " + player.getName() + " after leveling up " + skillType);
        }
    }
    
    /**
     * Checks if a block is a mature crop ready for harvest.
     * 
     * @param block The block to check
     * @return true if the block is a mature crop, false otherwise
     */
    private boolean isMatureCrop(Block block) {
        Material material = block.getType();
        BlockData blockData = block.getBlockData();
        
        // Check if block is an ageable crop
        if (blockData instanceof Ageable) {
            Ageable ageable = (Ageable) blockData;
            // Only award XP if crop is fully grown
            return ageable.getAge() == ageable.getMaximumAge();
        }
        
        // Special cases for non-ageable crops
        switch (material) {
            case MELON:
            case PUMPKIN:
            case SUGAR_CANE:
            case CACTUS:
            case BAMBOO:
                return true;
            default:
                return false;
        }
    }
}
