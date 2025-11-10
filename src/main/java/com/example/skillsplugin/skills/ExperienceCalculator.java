package com.example.skillsplugin.skills;

import com.example.skillsplugin.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;

/**
 * Calculates experience points for various skill-related actions.
 * All calculations are configurable through the ConfigManager.
 */
public class ExperienceCalculator {
    
    private final ConfigManager configManager;
    
    public ExperienceCalculator(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    /**
     * Calculates Mining XP based on block material
     * @param block The material of the block that was mined
     * @return The experience points to award
     */
    public double calculateMiningXP(Material block) {
        if (block == null) {
            return 0.0;
        }
        
        double baseXP = 0.0;
        
        // Stone and basic materials
        switch (block) {
            case STONE:
            case COBBLESTONE:
            case ANDESITE:
            case DIORITE:
            case GRANITE:
                baseXP = 1.0;
                break;
                
            // Coal
            case COAL_ORE:
            case DEEPSLATE_COAL_ORE:
                baseXP = 5.0;
                break;
                
            // Iron
            case IRON_ORE:
            case DEEPSLATE_IRON_ORE:
            case RAW_IRON_BLOCK:
                baseXP = 10.0;
                break;
                
            // Gold
            case GOLD_ORE:
            case DEEPSLATE_GOLD_ORE:
            case RAW_GOLD_BLOCK:
            case NETHER_GOLD_ORE:
                baseXP = 15.0;
                break;
                
            // Redstone
            case REDSTONE_ORE:
            case DEEPSLATE_REDSTONE_ORE:
                baseXP = 12.0;
                break;
                
            // Lapis
            case LAPIS_ORE:
            case DEEPSLATE_LAPIS_ORE:
                baseXP = 12.0;
                break;
                
            // Diamond
            case DIAMOND_ORE:
            case DEEPSLATE_DIAMOND_ORE:
                baseXP = 25.0;
                break;
                
            // Emerald
            case EMERALD_ORE:
            case DEEPSLATE_EMERALD_ORE:
                baseXP = 30.0;
                break;
                
            // Netherite/Ancient Debris
            case ANCIENT_DEBRIS:
                baseXP = 50.0;
                break;
                
            // Copper
            case COPPER_ORE:
            case DEEPSLATE_COPPER_ORE:
            case RAW_COPPER_BLOCK:
                baseXP = 8.0;
                break;
                
            // Quartz
            case NETHER_QUARTZ_ORE:
                baseXP = 10.0;
                break;
                
            // Obsidian
            case OBSIDIAN:
            case CRYING_OBSIDIAN:
                baseXP = 20.0;
                break;
                
            default:
                baseXP = 0.0;
        }
        
        return baseXP * configManager.getExperienceMultiplier(SkillType.MINING);
    }
    
    /**
     * Calculates Woodcutting XP based on log type
     * @param log The material of the log that was chopped
     * @return The experience points to award
     */
    public double calculateWoodcuttingXP(Material log) {
        if (log == null) {
            return 0.0;
        }
        
        double baseXP = 0.0;
        
        switch (log) {
            case OAK_LOG:
            case OAK_WOOD:
            case STRIPPED_OAK_LOG:
            case STRIPPED_OAK_WOOD:
                baseXP = 5.0;
                break;
                
            case SPRUCE_LOG:
            case SPRUCE_WOOD:
            case STRIPPED_SPRUCE_LOG:
            case STRIPPED_SPRUCE_WOOD:
                baseXP = 5.0;
                break;
                
            case BIRCH_LOG:
            case BIRCH_WOOD:
            case STRIPPED_BIRCH_LOG:
            case STRIPPED_BIRCH_WOOD:
                baseXP = 5.0;
                break;
                
            case JUNGLE_LOG:
            case JUNGLE_WOOD:
            case STRIPPED_JUNGLE_LOG:
            case STRIPPED_JUNGLE_WOOD:
                baseXP = 7.0;
                break;
                
            case ACACIA_LOG:
            case ACACIA_WOOD:
            case STRIPPED_ACACIA_LOG:
            case STRIPPED_ACACIA_WOOD:
                baseXP = 6.0;
                break;
                
            case DARK_OAK_LOG:
            case DARK_OAK_WOOD:
            case STRIPPED_DARK_OAK_LOG:
            case STRIPPED_DARK_OAK_WOOD:
                baseXP = 8.0;
                break;
                
            case MANGROVE_LOG:
            case MANGROVE_WOOD:
            case STRIPPED_MANGROVE_LOG:
            case STRIPPED_MANGROVE_WOOD:
                baseXP = 7.0;
                break;
                
            case CHERRY_LOG:
            case CHERRY_WOOD:
            case STRIPPED_CHERRY_LOG:
            case STRIPPED_CHERRY_WOOD:
                baseXP = 7.0;
                break;
                
            case CRIMSON_STEM:
            case CRIMSON_HYPHAE:
            case STRIPPED_CRIMSON_STEM:
            case STRIPPED_CRIMSON_HYPHAE:
                baseXP = 10.0;
                break;
                
            case WARPED_STEM:
            case WARPED_HYPHAE:
            case STRIPPED_WARPED_STEM:
            case STRIPPED_WARPED_HYPHAE:
                baseXP = 10.0;
                break;
                
            default:
                baseXP = 0.0;
        }
        
        return baseXP * configManager.getExperienceMultiplier(SkillType.WOODCUTTING);
    }
    
    /**
     * Calculates Combat XP based on entity type
     * @param mob The type of entity that was killed
     * @return The experience points to award
     */
    public double calculateCombatXP(EntityType mob) {
        if (mob == null) {
            return 0.0;
        }
        
        double baseXP = 0.0;
        
        switch (mob) {
            // Passive mobs (low XP)
            case CHICKEN:
            case COW:
            case PIG:
            case SHEEP:
            case RABBIT:
                baseXP = 2.0;
                break;
                
            // Hostile mobs (medium XP)
            case ZOMBIE:
            case SKELETON:
            case CREEPER:
            case SPIDER:
            case CAVE_SPIDER:
                baseXP = 10.0;
                break;
                
            case ENDERMAN:
            case WITCH:
                baseXP = 15.0;
                break;
                
            // Nether mobs
            case BLAZE:
            case GHAST:
            case MAGMA_CUBE:
            case WITHER_SKELETON:
                baseXP = 20.0;
                break;
                
            case PIGLIN:
            case PIGLIN_BRUTE:
            case HOGLIN:
                baseXP = 15.0;
                break;
                
            // Stronger mobs
            case GUARDIAN:
            case ELDER_GUARDIAN:
                baseXP = 30.0;
                break;
                
            case SHULKER:
                baseXP = 25.0;
                break;
                
            case PHANTOM:
                baseXP = 12.0;
                break;
                
            case DROWNED:
                baseXP = 12.0;
                break;
                
            case PILLAGER:
            case VINDICATOR:
            case EVOKER:
            case RAVAGER:
                baseXP = 18.0;
                break;
                
            // Boss mobs (high XP)
            case ENDER_DRAGON:
                baseXP = 500.0;
                break;
                
            case WITHER:
                baseXP = 300.0;
                break;
                
            case WARDEN:
                baseXP = 200.0;
                break;
                
            default:
                baseXP = 5.0; // Default for any other hostile entity
        }
        
        return baseXP * configManager.getExperienceMultiplier(SkillType.COMBAT);
    }
    
    /**
     * Calculates Farming XP based on crop type
     * @param crop The material of the crop that was harvested
     * @return The experience points to award
     */
    public double calculateFarmingXP(Material crop) {
        if (crop == null) {
            return 0.0;
        }
        
        double baseXP = 0.0;
        
        switch (crop) {
            case WHEAT:
                baseXP = 5.0;
                break;
                
            case CARROTS:
            case POTATOES:
                baseXP = 5.0;
                break;
                
            case BEETROOTS:
                baseXP = 5.0;
                break;
                
            case MELON:
                baseXP = 3.0;
                break;
                
            case PUMPKIN:
                baseXP = 4.0;
                break;
                
            case SUGAR_CANE:
                baseXP = 2.0;
                break;
                
            case CACTUS:
                baseXP = 2.0;
                break;
                
            case BAMBOO:
                baseXP = 1.0;
                break;
                
            case SWEET_BERRY_BUSH:
                baseXP = 3.0;
                break;
                
            case COCOA:
                baseXP = 4.0;
                break;
                
            case NETHER_WART:
                baseXP = 6.0;
                break;
                
            default:
                baseXP = 0.0;
        }
        
        return baseXP * configManager.getExperienceMultiplier(SkillType.FARMING);
    }
    
    /**
     * Calculates Fishing XP
     * @param caughtItem The item that was caught (can be used for bonus XP for treasure)
     * @return The experience points to award
     */
    public double calculateFishingXP(ItemStack caughtItem) {
        double baseXP = 10.0; // Base XP for any catch
        
        if (caughtItem != null) {
            Material material = caughtItem.getType();
            
            // Bonus XP for treasure items
            switch (material) {
                case ENCHANTED_BOOK:
                case NAME_TAG:
                case SADDLE:
                case NAUTILUS_SHELL:
                    baseXP = 25.0;
                    break;
                    
                case BOW:
                case FISHING_ROD:
                    baseXP = 20.0;
                    break;
                    
                // Regular fish
                case COD:
                case SALMON:
                    baseXP = 10.0;
                    break;
                    
                case TROPICAL_FISH:
                    baseXP = 12.0;
                    break;
                    
                case PUFFERFISH:
                    baseXP = 15.0;
                    break;
                    
                // Junk items
                case LEATHER_BOOTS:
                case LEATHER:
                case BONE:
                case STRING:
                case BOWL:
                case STICK:
                case INK_SAC:
                case TRIPWIRE_HOOK:
                case LILY_PAD:
                case ROTTEN_FLESH:
                case BAMBOO:
                    baseXP = 5.0;
                    break;
            }
        }
        
        return baseXP * configManager.getExperienceMultiplier(SkillType.FISHING);
    }
    
    /**
     * Calculates Enchanting XP based on enchantment level
     * @param enchantLevel The level of the enchantment (1-30)
     * @return The experience points to award
     */
    public double calculateEnchantingXP(int enchantLevel) {
        if (enchantLevel <= 0) {
            return 0.0;
        }
        
        // XP scales with enchantment level
        // Level 1 = 5 XP, Level 30 = 150 XP
        double baseXP = enchantLevel * 5.0;
        
        return baseXP * configManager.getExperienceMultiplier(SkillType.ENCHANTING);
    }
    
    /**
     * Calculates Trading XP based on trade value
     * @param trade The merchant recipe that was used
     * @return The experience points to award
     */
    public double calculateTradingXP(MerchantRecipe trade) {
        if (trade == null) {
            return 0.0;
        }
        
        // Base XP for any trade
        double baseXP = 10.0;
        
        // Add bonus XP based on the result item value
        ItemStack result = trade.getResult();
        if (result != null) {
            Material material = result.getType();
            
            // Higher value items give more XP
            switch (material) {
                case DIAMOND:
                case DIAMOND_SWORD:
                case DIAMOND_PICKAXE:
                case DIAMOND_AXE:
                case DIAMOND_SHOVEL:
                case DIAMOND_HOE:
                case DIAMOND_HELMET:
                case DIAMOND_CHESTPLATE:
                case DIAMOND_LEGGINGS:
                case DIAMOND_BOOTS:
                    baseXP = 30.0;
                    break;
                    
                case EMERALD:
                case EMERALD_BLOCK:
                    baseXP = 25.0;
                    break;
                    
                case IRON_INGOT:
                case GOLD_INGOT:
                case IRON_SWORD:
                case IRON_PICKAXE:
                case IRON_AXE:
                case IRON_HELMET:
                case IRON_CHESTPLATE:
                case IRON_LEGGINGS:
                case IRON_BOOTS:
                    baseXP = 15.0;
                    break;
                    
                case ENCHANTED_BOOK:
                    baseXP = 35.0;
                    break;
                    
                case ENDER_PEARL:
                case GLOWSTONE:
                case REDSTONE:
                    baseXP = 12.0;
                    break;
                    
                default:
                    baseXP = 10.0;
            }
            
            // Multiply by quantity
            baseXP *= result.getAmount();
        }
        
        return baseXP * configManager.getExperienceMultiplier(SkillType.TRADING);
    }
}
