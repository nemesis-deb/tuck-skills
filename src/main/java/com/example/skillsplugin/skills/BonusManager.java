package com.example.skillsplugin.skills;

import com.example.skillsplugin.config.ConfigManager;
import com.example.skillsplugin.data.PlayerDataManager;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

import java.util.Random;

/**
 * Manages skill-based bonuses that are applied during gameplay.
 * Each skill provides different bonuses that scale with the player's level.
 */
public class BonusManager {
    
    private final PlayerDataManager playerDataManager;
    private final ConfigManager configManager;
    private final Random random;
    
    /**
     * Creates a new bonus manager.
     * 
     * @param playerDataManager The player data manager for accessing skill levels
     * @param configManager The config manager for bonus rates
     */
    public BonusManager(PlayerDataManager playerDataManager, ConfigManager configManager) {
        this.playerDataManager = playerDataManager;
        this.configManager = configManager;
        this.random = new Random();
    }
    
    /**
     * Applies Mining bonus: chance for double drops based on level.
     * 
     * @param player The player mining
     * @param block The block being mined
     */
    public void applyMiningBonus(Player player, Block block) {
        if (!configManager.areBonusesEnabled()) {
            return;
        }
        
        SkillProfile profile = playerDataManager.getProfile(player.getUniqueId());
        Skill miningSkill = profile.getSkill(SkillType.MINING);
        
        double chancePerLevel = configManager.getBonusSetting("mining.double-drop-chance-per-level");
        double totalChance = miningSkill.getLevel() * chancePerLevel;
        
        // Cap at 100% chance
        totalChance = Math.min(totalChance, 100.0);
        
        // Roll for bonus drop
        if (random.nextDouble() * 100 < totalChance) {
            // Drop an extra copy of the block's drops
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block.getType()));
        }
    }
    
    /**
     * Applies Woodcutting bonus: chance for extra log drops based on level.
     * 
     * @param player The player chopping wood
     * @param block The log block being chopped
     */
    public void applyWoodcuttingBonus(Player player, Block block) {
        if (!configManager.areBonusesEnabled()) {
            return;
        }
        
        SkillProfile profile = playerDataManager.getProfile(player.getUniqueId());
        Skill woodcuttingSkill = profile.getSkill(SkillType.WOODCUTTING);
        
        double chancePerLevel = configManager.getBonusSetting("woodcutting.double-drop-chance-per-level");
        double totalChance = woodcuttingSkill.getLevel() * chancePerLevel;
        
        // Cap at 100% chance
        totalChance = Math.min(totalChance, 100.0);
        
        // Roll for bonus drop
        if (random.nextDouble() * 100 < totalChance) {
            // Drop an extra log
            block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(block.getType()));
        }
    }
    
    /**
     * Gets the Combat damage multiplier based on level.
     * 
     * @param player The player in combat
     * @return The damage multiplier (1.0 = no bonus, 1.5 = 50% more damage)
     */
    public double getCombatDamageMultiplier(Player player) {
        if (!configManager.areBonusesEnabled()) {
            return 1.0;
        }
        
        SkillProfile profile = playerDataManager.getProfile(player.getUniqueId());
        Skill combatSkill = profile.getSkill(SkillType.COMBAT);
        
        double bonusPerLevel = configManager.getBonusSetting("combat.damage-bonus-per-level");
        double bonusPercent = combatSkill.getLevel() * bonusPerLevel;
        
        // Convert percentage to multiplier (e.g., 10% = 1.10)
        return 1.0 + (bonusPercent / 100.0);
    }
    
    /**
     * Applies Farming bonus: chance for extra crop yield based on level.
     * 
     * @param player The player harvesting crops
     * @param block The crop block being harvested
     */
    public void applyFarmingBonus(Player player, Block block) {
        if (!configManager.areBonusesEnabled()) {
            return;
        }
        
        SkillProfile profile = playerDataManager.getProfile(player.getUniqueId());
        Skill farmingSkill = profile.getSkill(SkillType.FARMING);
        
        double chancePerLevel = configManager.getBonusSetting("farming.double-crop-chance-per-level");
        double totalChance = farmingSkill.getLevel() * chancePerLevel;
        
        // Cap at 100% chance
        totalChance = Math.min(totalChance, 100.0);
        
        // Roll for bonus drop
        if (random.nextDouble() * 100 < totalChance) {
            // Drop extra crops based on the crop type
            Material cropMaterial = getCropDropMaterial(block.getType());
            if (cropMaterial != null) {
                block.getWorld().dropItemNaturally(block.getLocation(), new ItemStack(cropMaterial));
            }
        }
    }
    
    /**
     * Applies Fishing bonus: chance for treasure based on level.
     * 
     * @param player The player fishing
     * @param caught The item that was caught
     * @return The item to give (may be upgraded to treasure)
     */
    public ItemStack applyFishingBonus(Player player, ItemStack caught) {
        if (!configManager.areBonusesEnabled() || caught == null) {
            return caught;
        }
        
        SkillProfile profile = playerDataManager.getProfile(player.getUniqueId());
        Skill fishingSkill = profile.getSkill(SkillType.FISHING);
        
        double chancePerLevel = configManager.getBonusSetting("fishing.treasure-chance-per-level");
        double totalChance = fishingSkill.getLevel() * chancePerLevel;
        
        // Cap at reasonable chance
        totalChance = Math.min(totalChance, 50.0);
        
        // Roll for treasure
        if (random.nextDouble() * 100 < totalChance) {
            // Return a treasure item
            return getTreasureItem();
        }
        
        return caught;
    }
    
    /**
     * Gets the Enchanting XP cost reduction based on level.
     * 
     * @param player The player enchanting
     * @param baseCost The base XP cost
     * @return The reduced XP cost
     */
    public int getEnchantingCostReduction(Player player, int baseCost) {
        if (!configManager.areBonusesEnabled()) {
            return baseCost;
        }
        
        SkillProfile profile = playerDataManager.getProfile(player.getUniqueId());
        Skill enchantingSkill = profile.getSkill(SkillType.ENCHANTING);
        
        double reductionPerLevel = configManager.getBonusSetting("enchanting.cost-reduction-per-level");
        double totalReduction = enchantingSkill.getLevel() * reductionPerLevel;
        
        // Cap at 80% reduction
        totalReduction = Math.min(totalReduction, 80.0);
        
        // Calculate reduced cost
        int reducedCost = (int) (baseCost * (1.0 - totalReduction / 100.0));
        
        // Ensure minimum cost of 1
        return Math.max(reducedCost, 1);
    }
    
    /**
     * Gets the Trading discount multiplier based on level.
     * 
     * @param player The player trading
     * @return The price multiplier (1.0 = no discount, 0.9 = 10% discount)
     */
    public double getTradingDiscount(Player player) {
        if (!configManager.areBonusesEnabled()) {
            return 1.0;
        }
        
        SkillProfile profile = playerDataManager.getProfile(player.getUniqueId());
        Skill tradingSkill = profile.getSkill(SkillType.TRADING);
        
        double discountPerLevel = configManager.getBonusSetting("trading.discount-per-level");
        double totalDiscount = tradingSkill.getLevel() * discountPerLevel;
        
        // Cap at 50% discount
        totalDiscount = Math.min(totalDiscount, 50.0);
        
        // Convert to multiplier (e.g., 10% discount = 0.90 multiplier)
        return 1.0 - (totalDiscount / 100.0);
    }
    
    /**
     * Gets the crop drop material for a given crop block type.
     * 
     * @param cropType The crop block type
     * @return The material that drops from the crop, or null if not a crop
     */
    private Material getCropDropMaterial(Material cropType) {
        switch (cropType) {
            case WHEAT:
                return Material.WHEAT;
            case CARROTS:
                return Material.CARROT;
            case POTATOES:
                return Material.POTATO;
            case BEETROOTS:
                return Material.BEETROOT;
            case NETHER_WART:
                return Material.NETHER_WART;
            case MELON:
                return Material.MELON_SLICE;
            case PUMPKIN:
                return Material.PUMPKIN;
            case SUGAR_CANE:
                return Material.SUGAR_CANE;
            case CACTUS:
                return Material.CACTUS;
            case BAMBOO:
                return Material.BAMBOO;
            case COCOA:
                return Material.COCOA_BEANS;
            case SWEET_BERRY_BUSH:
                return Material.SWEET_BERRIES;
            default:
                return null;
        }
    }
    
    /**
     * Gets a random treasure item for fishing bonus.
     * 
     * @return A treasure ItemStack
     */
    private ItemStack getTreasureItem() {
        Material[] treasures = {
            Material.NAME_TAG,
            Material.SADDLE,
            Material.BOW,
            Material.FISHING_ROD,
            Material.BOOK,
            Material.NAUTILUS_SHELL,
            Material.LILY_PAD
        };
        
        Material treasure = treasures[random.nextInt(treasures.length)];
        return new ItemStack(treasure);
    }
}
