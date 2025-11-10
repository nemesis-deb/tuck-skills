package com.example.skillsplugin.skills;

import com.example.skillsplugin.config.ConfigManager;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantRecipe;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ExperienceCalculatorTest {
    
    @Mock
    private ConfigManager configManager;
    
    private ExperienceCalculator calculator;
    
    @Before
    public void setUp() {
        calculator = new ExperienceCalculator(configManager);
        
        // Default multipliers to 1.0 for all skills
        when(configManager.getExperienceMultiplier(any(SkillType.class))).thenReturn(1.0);
    }
    
    // ========== Mining XP Tests ==========
    
    @Test
    public void testCalculateMiningXP_Stone() {
        double xp = calculator.calculateMiningXP(Material.STONE);
        assertEquals(1.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateMiningXP_CoalOre() {
        double xp = calculator.calculateMiningXP(Material.COAL_ORE);
        assertEquals(5.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateMiningXP_IronOre() {
        double xp = calculator.calculateMiningXP(Material.IRON_ORE);
        assertEquals(10.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateMiningXP_DiamondOre() {
        double xp = calculator.calculateMiningXP(Material.DIAMOND_ORE);
        assertEquals(25.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateMiningXP_AncientDebris() {
        double xp = calculator.calculateMiningXP(Material.ANCIENT_DEBRIS);
        assertEquals(50.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateMiningXP_DeepslateOres() {
        double coalXP = calculator.calculateMiningXP(Material.DEEPSLATE_COAL_ORE);
        double ironXP = calculator.calculateMiningXP(Material.DEEPSLATE_IRON_ORE);
        double diamondXP = calculator.calculateMiningXP(Material.DEEPSLATE_DIAMOND_ORE);
        
        assertEquals(5.0, coalXP, 0.01);
        assertEquals(10.0, ironXP, 0.01);
        assertEquals(25.0, diamondXP, 0.01);
    }
    
    @Test
    public void testCalculateMiningXP_Obsidian() {
        double xp = calculator.calculateMiningXP(Material.OBSIDIAN);
        assertEquals(20.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateMiningXP_NullMaterial() {
        double xp = calculator.calculateMiningXP(null);
        assertEquals(0.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateMiningXP_NonOreMaterial() {
        double xp = calculator.calculateMiningXP(Material.DIRT);
        assertEquals(0.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateMiningXP_WithMultiplier() {
        when(configManager.getExperienceMultiplier(SkillType.MINING)).thenReturn(2.0);
        
        double xp = calculator.calculateMiningXP(Material.DIAMOND_ORE);
        assertEquals(50.0, xp, 0.01); // 25.0 * 2.0
    }
    
    // ========== Woodcutting XP Tests ==========
    
    @Test
    public void testCalculateWoodcuttingXP_OakLog() {
        double xp = calculator.calculateWoodcuttingXP(Material.OAK_LOG);
        assertEquals(5.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateWoodcuttingXP_SpruceLog() {
        double xp = calculator.calculateWoodcuttingXP(Material.SPRUCE_LOG);
        assertEquals(5.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateWoodcuttingXP_DarkOakLog() {
        double xp = calculator.calculateWoodcuttingXP(Material.DARK_OAK_LOG);
        assertEquals(8.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateWoodcuttingXP_CrimsonStem() {
        double xp = calculator.calculateWoodcuttingXP(Material.CRIMSON_STEM);
        assertEquals(10.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateWoodcuttingXP_StrippedLogs() {
        double oakXP = calculator.calculateWoodcuttingXP(Material.STRIPPED_OAK_LOG);
        double birchXP = calculator.calculateWoodcuttingXP(Material.STRIPPED_BIRCH_LOG);
        
        assertEquals(5.0, oakXP, 0.01);
        assertEquals(5.0, birchXP, 0.01);
    }
    
    @Test
    public void testCalculateWoodcuttingXP_NullMaterial() {
        double xp = calculator.calculateWoodcuttingXP(null);
        assertEquals(0.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateWoodcuttingXP_NonLogMaterial() {
        double xp = calculator.calculateWoodcuttingXP(Material.DIRT);
        assertEquals(0.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateWoodcuttingXP_WithMultiplier() {
        when(configManager.getExperienceMultiplier(SkillType.WOODCUTTING)).thenReturn(1.5);
        
        double xp = calculator.calculateWoodcuttingXP(Material.DARK_OAK_LOG);
        assertEquals(12.0, xp, 0.01); // 8.0 * 1.5
    }
    
    // ========== Combat XP Tests ==========
    
    @Test
    public void testCalculateCombatXP_PassiveMobs() {
        double chickenXP = calculator.calculateCombatXP(EntityType.CHICKEN);
        double cowXP = calculator.calculateCombatXP(EntityType.COW);
        double pigXP = calculator.calculateCombatXP(EntityType.PIG);
        
        assertEquals(2.0, chickenXP, 0.01);
        assertEquals(2.0, cowXP, 0.01);
        assertEquals(2.0, pigXP, 0.01);
    }
    
    @Test
    public void testCalculateCombatXP_BasicHostileMobs() {
        double zombieXP = calculator.calculateCombatXP(EntityType.ZOMBIE);
        double skeletonXP = calculator.calculateCombatXP(EntityType.SKELETON);
        double creeperXP = calculator.calculateCombatXP(EntityType.CREEPER);
        
        assertEquals(10.0, zombieXP, 0.01);
        assertEquals(10.0, skeletonXP, 0.01);
        assertEquals(10.0, creeperXP, 0.01);
    }
    
    @Test
    public void testCalculateCombatXP_NetherMobs() {
        double blazeXP = calculator.calculateCombatXP(EntityType.BLAZE);
        double ghastXP = calculator.calculateCombatXP(EntityType.GHAST);
        double witherSkeletonXP = calculator.calculateCombatXP(EntityType.WITHER_SKELETON);
        
        assertEquals(20.0, blazeXP, 0.01);
        assertEquals(20.0, ghastXP, 0.01);
        assertEquals(20.0, witherSkeletonXP, 0.01);
    }
    
    @Test
    public void testCalculateCombatXP_BossMobs() {
        double enderDragonXP = calculator.calculateCombatXP(EntityType.ENDER_DRAGON);
        double witherXP = calculator.calculateCombatXP(EntityType.WITHER);
        double wardenXP = calculator.calculateCombatXP(EntityType.WARDEN);
        
        assertEquals(500.0, enderDragonXP, 0.01);
        assertEquals(300.0, witherXP, 0.01);
        assertEquals(200.0, wardenXP, 0.01);
    }
    
    @Test
    public void testCalculateCombatXP_ElderGuardian() {
        double xp = calculator.calculateCombatXP(EntityType.ELDER_GUARDIAN);
        assertEquals(30.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateCombatXP_NullEntity() {
        double xp = calculator.calculateCombatXP(null);
        assertEquals(0.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateCombatXP_WithMultiplier() {
        when(configManager.getExperienceMultiplier(SkillType.COMBAT)).thenReturn(2.5);
        
        double xp = calculator.calculateCombatXP(EntityType.ZOMBIE);
        assertEquals(25.0, xp, 0.01); // 10.0 * 2.5
    }
    
    // ========== Farming XP Tests ==========
    
    @Test
    public void testCalculateFarmingXP_Wheat() {
        double xp = calculator.calculateFarmingXP(Material.WHEAT);
        assertEquals(5.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateFarmingXP_Carrots() {
        double xp = calculator.calculateFarmingXP(Material.CARROTS);
        assertEquals(5.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateFarmingXP_NetherWart() {
        double xp = calculator.calculateFarmingXP(Material.NETHER_WART);
        assertEquals(6.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateFarmingXP_Melon() {
        double xp = calculator.calculateFarmingXP(Material.MELON);
        assertEquals(3.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateFarmingXP_Pumpkin() {
        double xp = calculator.calculateFarmingXP(Material.PUMPKIN);
        assertEquals(4.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateFarmingXP_NullMaterial() {
        double xp = calculator.calculateFarmingXP(null);
        assertEquals(0.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateFarmingXP_NonCropMaterial() {
        double xp = calculator.calculateFarmingXP(Material.STONE);
        assertEquals(0.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateFarmingXP_WithMultiplier() {
        when(configManager.getExperienceMultiplier(SkillType.FARMING)).thenReturn(1.2);
        
        double xp = calculator.calculateFarmingXP(Material.WHEAT);
        assertEquals(6.0, xp, 0.01); // 5.0 * 1.2
    }
    
    // ========== Fishing XP Tests ==========
    
    @Test
    public void testCalculateFishingXP_RegularFish() {
        ItemStack cod = new ItemStack(Material.COD);
        ItemStack salmon = new ItemStack(Material.SALMON);
        
        double codXP = calculator.calculateFishingXP(cod);
        double salmonXP = calculator.calculateFishingXP(salmon);
        
        assertEquals(10.0, codXP, 0.01);
        assertEquals(10.0, salmonXP, 0.01);
    }
    
    @Test
    public void testCalculateFishingXP_TropicalFish() {
        ItemStack tropicalFish = new ItemStack(Material.TROPICAL_FISH);
        double xp = calculator.calculateFishingXP(tropicalFish);
        assertEquals(12.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateFishingXP_Pufferfish() {
        ItemStack pufferfish = new ItemStack(Material.PUFFERFISH);
        double xp = calculator.calculateFishingXP(pufferfish);
        assertEquals(15.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateFishingXP_TreasureItems() {
        ItemStack enchantedBook = new ItemStack(Material.ENCHANTED_BOOK);
        ItemStack nameTag = new ItemStack(Material.NAME_TAG);
        ItemStack saddle = new ItemStack(Material.SADDLE);
        
        double bookXP = calculator.calculateFishingXP(enchantedBook);
        double nameTagXP = calculator.calculateFishingXP(nameTag);
        double saddleXP = calculator.calculateFishingXP(saddle);
        
        assertEquals(25.0, bookXP, 0.01);
        assertEquals(25.0, nameTagXP, 0.01);
        assertEquals(25.0, saddleXP, 0.01);
    }
    
    @Test
    public void testCalculateFishingXP_JunkItems() {
        ItemStack leatherBoots = new ItemStack(Material.LEATHER_BOOTS);
        ItemStack stick = new ItemStack(Material.STICK);
        
        double bootsXP = calculator.calculateFishingXP(leatherBoots);
        double stickXP = calculator.calculateFishingXP(stick);
        
        assertEquals(5.0, bootsXP, 0.01);
        assertEquals(5.0, stickXP, 0.01);
    }
    
    @Test
    public void testCalculateFishingXP_NullItem() {
        double xp = calculator.calculateFishingXP(null);
        assertEquals(10.0, xp, 0.01); // Base XP
    }
    
    @Test
    public void testCalculateFishingXP_WithMultiplier() {
        when(configManager.getExperienceMultiplier(SkillType.FISHING)).thenReturn(1.5);
        
        ItemStack cod = new ItemStack(Material.COD);
        double xp = calculator.calculateFishingXP(cod);
        assertEquals(15.0, xp, 0.01); // 10.0 * 1.5
    }
    
    // ========== Enchanting XP Tests ==========
    
    @Test
    public void testCalculateEnchantingXP_Level1() {
        double xp = calculator.calculateEnchantingXP(1);
        assertEquals(5.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateEnchantingXP_Level10() {
        double xp = calculator.calculateEnchantingXP(10);
        assertEquals(50.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateEnchantingXP_Level30() {
        double xp = calculator.calculateEnchantingXP(30);
        assertEquals(150.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateEnchantingXP_ZeroLevel() {
        double xp = calculator.calculateEnchantingXP(0);
        assertEquals(0.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateEnchantingXP_NegativeLevel() {
        double xp = calculator.calculateEnchantingXP(-5);
        assertEquals(0.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateEnchantingXP_WithMultiplier() {
        when(configManager.getExperienceMultiplier(SkillType.ENCHANTING)).thenReturn(2.0);
        
        double xp = calculator.calculateEnchantingXP(10);
        assertEquals(100.0, xp, 0.01); // 50.0 * 2.0
    }
    
    // ========== Trading XP Tests ==========
    
    @Test
    public void testCalculateTradingXP_BasicTrade() {
        MerchantRecipe trade = mock(MerchantRecipe.class);
        ItemStack result = new ItemStack(Material.BREAD, 1);
        when(trade.getResult()).thenReturn(result);
        
        double xp = calculator.calculateTradingXP(trade);
        assertEquals(10.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateTradingXP_DiamondTrade() {
        MerchantRecipe trade = mock(MerchantRecipe.class);
        ItemStack result = new ItemStack(Material.DIAMOND, 1);
        when(trade.getResult()).thenReturn(result);
        
        double xp = calculator.calculateTradingXP(trade);
        assertEquals(30.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateTradingXP_EmeraldTrade() {
        MerchantRecipe trade = mock(MerchantRecipe.class);
        ItemStack result = new ItemStack(Material.EMERALD, 1);
        when(trade.getResult()).thenReturn(result);
        
        double xp = calculator.calculateTradingXP(trade);
        assertEquals(25.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateTradingXP_EnchantedBookTrade() {
        MerchantRecipe trade = mock(MerchantRecipe.class);
        ItemStack result = new ItemStack(Material.ENCHANTED_BOOK, 1);
        when(trade.getResult()).thenReturn(result);
        
        double xp = calculator.calculateTradingXP(trade);
        assertEquals(35.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateTradingXP_MultipleItems() {
        MerchantRecipe trade = mock(MerchantRecipe.class);
        ItemStack result = new ItemStack(Material.EMERALD, 5);
        when(trade.getResult()).thenReturn(result);
        
        double xp = calculator.calculateTradingXP(trade);
        assertEquals(125.0, xp, 0.01); // 25.0 * 5
    }
    
    @Test
    public void testCalculateTradingXP_NullTrade() {
        double xp = calculator.calculateTradingXP(null);
        assertEquals(0.0, xp, 0.01);
    }
    
    @Test
    public void testCalculateTradingXP_WithMultiplier() {
        when(configManager.getExperienceMultiplier(SkillType.TRADING)).thenReturn(1.5);
        
        MerchantRecipe trade = mock(MerchantRecipe.class);
        ItemStack result = new ItemStack(Material.DIAMOND, 1);
        when(trade.getResult()).thenReturn(result);
        
        double xp = calculator.calculateTradingXP(trade);
        assertEquals(45.0, xp, 0.01); // 30.0 * 1.5
    }
    
    // ========== Config Multiplier Integration Tests ==========
    
    @Test
    public void testAllSkillsWithDifferentMultipliers() {
        when(configManager.getExperienceMultiplier(SkillType.MINING)).thenReturn(2.0);
        when(configManager.getExperienceMultiplier(SkillType.WOODCUTTING)).thenReturn(1.5);
        when(configManager.getExperienceMultiplier(SkillType.COMBAT)).thenReturn(3.0);
        when(configManager.getExperienceMultiplier(SkillType.FARMING)).thenReturn(1.2);
        when(configManager.getExperienceMultiplier(SkillType.FISHING)).thenReturn(0.8);
        when(configManager.getExperienceMultiplier(SkillType.ENCHANTING)).thenReturn(2.5);
        when(configManager.getExperienceMultiplier(SkillType.TRADING)).thenReturn(1.1);
        
        assertEquals(50.0, calculator.calculateMiningXP(Material.DIAMOND_ORE), 0.01);
        assertEquals(12.0, calculator.calculateWoodcuttingXP(Material.DARK_OAK_LOG), 0.01);
        assertEquals(30.0, calculator.calculateCombatXP(EntityType.ZOMBIE), 0.01);
        assertEquals(6.0, calculator.calculateFarmingXP(Material.WHEAT), 0.01);
        
        ItemStack cod = new ItemStack(Material.COD);
        assertEquals(8.0, calculator.calculateFishingXP(cod), 0.01);
        
        assertEquals(125.0, calculator.calculateEnchantingXP(10), 0.01);
        
        MerchantRecipe trade = mock(MerchantRecipe.class);
        when(trade.getResult()).thenReturn(new ItemStack(Material.BREAD));
        assertEquals(11.0, calculator.calculateTradingXP(trade), 0.01);
    }
}
