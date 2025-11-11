package com.example.skillsplugin.config;

import com.example.skillsplugin.SkillsPlugin;
import com.example.skillsplugin.skills.SkillType;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for the ConfigManager class.
 * Tests configuration loading, parsing, default values, and reload functionality.
 */
public class ConfigManagerTest {
    
    private ConfigManager configManager;
    private SkillsPlugin mockPlugin;
    private FileConfiguration testConfig;
    private File tempConfigFile;
    
    @Before
    public void setUp() throws IOException {
        // Create mock plugin
        mockPlugin = mock(SkillsPlugin.class);
        when(mockPlugin.getLogger()).thenReturn(Logger.getLogger("TestLogger"));
        
        // Create a temporary config file for testing
        tempConfigFile = File.createTempFile("test-config", ".yml");
        tempConfigFile.deleteOnExit();
        
        // Load default config from resources
        testConfig = new YamlConfiguration();
        loadDefaultConfig();
        
        // Mock plugin config methods
        when(mockPlugin.getConfig()).thenReturn(testConfig);
        doNothing().when(mockPlugin).saveDefaultConfig();
        doNothing().when(mockPlugin).reloadConfig();
        
        // Create ConfigManager instance
        configManager = new ConfigManager(mockPlugin);
    }
    
    /**
     * Loads default configuration values for testing
     */
    private void loadDefaultConfig() {
        // Experience multipliers
        testConfig.set("experience.mining", 1.0);
        testConfig.set("experience.woodcutting", 1.0);
        testConfig.set("experience.combat", 1.0);
        testConfig.set("experience.farming", 1.0);
        testConfig.set("experience.fishing", 1.0);
        testConfig.set("experience.enchanting", 1.0);
        testConfig.set("experience.trading", 1.0);
        
        // Leveling formula
        testConfig.set("leveling.base-xp", 100.0);
        testConfig.set("leveling.exponent", 1.25);
        
        // Bonuses
        testConfig.set("bonuses.enabled", true);
        testConfig.set("bonuses.mining.double-drop-chance-per-level", 0.5);
        testConfig.set("bonuses.woodcutting.double-drop-chance-per-level", 0.5);
        testConfig.set("bonuses.combat.damage-bonus-per-level", 0.5);
        testConfig.set("bonuses.farming.double-crop-chance-per-level", 0.5);
        testConfig.set("bonuses.fishing.treasure-chance-per-level", 0.3);
        testConfig.set("bonuses.enchanting.cost-reduction-per-level", 0.5);
        testConfig.set("bonuses.trading.discount-per-level", 0.3);
        
        // UI settings
        testConfig.set("ui.boss-bar-duration", 5);
        testConfig.set("ui.show-xp-gain-messages", true);
        
        // Enabled skills
        testConfig.set("enabled-skills", java.util.Arrays.asList(
            "MINING", "WOODCUTTING", "COMBAT", "FARMING", 
            "FISHING", "ENCHANTING", "TRADING"
        ));
        
        // Storage type
        testConfig.set("storage.type", "JSON");
    }
    
    @Test
    public void testConfigManagerInitialization() {
        assertNotNull("ConfigManager should be initialized", configManager);
    }
    
    @Test
    public void testLoadConfig() {
        configManager.loadConfig();
        
        verify(mockPlugin).saveDefaultConfig();
        verify(mockPlugin).reloadConfig();
        verify(mockPlugin, atLeastOnce()).getConfig();
    }
    
    @Test
    public void testDefaultExperienceMultipliers() {
        configManager.loadConfig();
        
        assertEquals("Mining multiplier should be 1.0", 
            1.0, configManager.getExperienceMultiplier(SkillType.MINING), 0.001);
        assertEquals("Woodcutting multiplier should be 1.0", 
            1.0, configManager.getExperienceMultiplier(SkillType.WOODCUTTING), 0.001);
        assertEquals("Combat multiplier should be 1.0", 
            1.0, configManager.getExperienceMultiplier(SkillType.COMBAT), 0.001);
        assertEquals("Farming multiplier should be 1.0", 
            1.0, configManager.getExperienceMultiplier(SkillType.FARMING), 0.001);
        assertEquals("Fishing multiplier should be 1.0", 
            1.0, configManager.getExperienceMultiplier(SkillType.FISHING), 0.001);
        assertEquals("Enchanting multiplier should be 1.0", 
            1.0, configManager.getExperienceMultiplier(SkillType.ENCHANTING), 0.001);
        assertEquals("Trading multiplier should be 1.0", 
            1.0, configManager.getExperienceMultiplier(SkillType.TRADING), 0.001);
    }
    
    @Test
    public void testCustomExperienceMultipliers() {
        testConfig.set("experience.mining", 2.0);
        testConfig.set("experience.combat", 0.5);
        
        configManager.loadConfig();
        
        assertEquals("Mining multiplier should be 2.0", 
            2.0, configManager.getExperienceMultiplier(SkillType.MINING), 0.001);
        assertEquals("Combat multiplier should be 0.5", 
            0.5, configManager.getExperienceMultiplier(SkillType.COMBAT), 0.001);
    }
    
    @Test
    public void testDefaultLevelingFormula() {
        configManager.loadConfig();
        
        assertEquals("Base XP should be 100.0", 100.0, configManager.getBaseXP(), 0.001);
        assertEquals("Exponent should be 1.25", 1.25, configManager.getExponent(), 0.001);
    }
    
    @Test
    public void testCustomLevelingFormula() {
        testConfig.set("leveling.base-xp", 200.0);
        testConfig.set("leveling.exponent", 2.0);
        
        configManager.loadConfig();
        
        assertEquals("Base XP should be 200.0", 200.0, configManager.getBaseXP(), 0.001);
        assertEquals("Exponent should be 2.0", 2.0, configManager.getExponent(), 0.001);
    }
    
    @Test
    public void testBonusesEnabled() {
        configManager.loadConfig();
        
        assertTrue("Bonuses should be enabled by default", configManager.areBonusesEnabled());
    }
    
    @Test
    public void testBonusesDisabled() {
        testConfig.set("bonuses.enabled", false);
        
        configManager.loadConfig();
        
        assertFalse("Bonuses should be disabled", configManager.areBonusesEnabled());
    }
    
    @Test
    public void testDefaultBonusSettings() {
        configManager.loadConfig();
        
        assertEquals("Mining double drop chance should be 0.5", 
            0.5, configManager.getBonusSetting("mining.double-drop-chance-per-level"), 0.001);
        assertEquals("Combat damage bonus should be 0.5", 
            0.5, configManager.getBonusSetting("combat.damage-bonus-per-level"), 0.001);
        assertEquals("Fishing treasure chance should be 0.3", 
            0.3, configManager.getBonusSetting("fishing.treasure-chance-per-level"), 0.001);
    }
    
    @Test
    public void testCustomBonusSettings() {
        testConfig.set("bonuses.mining.double-drop-chance-per-level", 1.0);
        testConfig.set("bonuses.fishing.treasure-chance-per-level", 0.8);
        
        configManager.loadConfig();
        
        assertEquals("Mining double drop chance should be 1.0", 
            1.0, configManager.getBonusSetting("mining.double-drop-chance-per-level"), 0.001);
        assertEquals("Fishing treasure chance should be 0.8", 
            0.8, configManager.getBonusSetting("fishing.treasure-chance-per-level"), 0.001);
    }
    
    @Test
    public void testDefaultUISettings() {
        configManager.loadConfig();
        
        assertEquals("Boss bar duration should be 5 seconds", 
            5, configManager.getBossBarDuration());
        assertTrue("XP gain messages should be shown", 
            configManager.shouldShowXPGainMessages());
    }
    
    @Test
    public void testCustomUISettings() {
        testConfig.set("ui.boss-bar-duration", 10);
        testConfig.set("ui.show-xp-gain-messages", false);
        
        configManager.loadConfig();
        
        assertEquals("Boss bar duration should be 10 seconds", 
            10, configManager.getBossBarDuration());
        assertFalse("XP gain messages should not be shown", 
            configManager.shouldShowXPGainMessages());
    }
    
    @Test
    public void testAllSkillsEnabledByDefault() {
        configManager.loadConfig();
        
        assertTrue("Mining should be enabled", 
            configManager.isSkillEnabled(SkillType.MINING));
        assertTrue("Woodcutting should be enabled", 
            configManager.isSkillEnabled(SkillType.WOODCUTTING));
        assertTrue("Combat should be enabled", 
            configManager.isSkillEnabled(SkillType.COMBAT));
        assertTrue("Farming should be enabled", 
            configManager.isSkillEnabled(SkillType.FARMING));
        assertTrue("Fishing should be enabled", 
            configManager.isSkillEnabled(SkillType.FISHING));
        assertTrue("Enchanting should be enabled", 
            configManager.isSkillEnabled(SkillType.ENCHANTING));
        assertTrue("Trading should be enabled", 
            configManager.isSkillEnabled(SkillType.TRADING));
    }
    
    @Test
    public void testDisableSpecificSkills() {
        testConfig.set("enabled-skills", java.util.Arrays.asList(
            "MINING", "COMBAT", "FISHING"
        ));
        
        configManager.loadConfig();
        
        assertTrue("Mining should be enabled", 
            configManager.isSkillEnabled(SkillType.MINING));
        assertFalse("Woodcutting should be disabled", 
            configManager.isSkillEnabled(SkillType.WOODCUTTING));
        assertTrue("Combat should be enabled", 
            configManager.isSkillEnabled(SkillType.COMBAT));
        assertFalse("Farming should be disabled", 
            configManager.isSkillEnabled(SkillType.FARMING));
        assertTrue("Fishing should be enabled", 
            configManager.isSkillEnabled(SkillType.FISHING));
        assertFalse("Enchanting should be disabled", 
            configManager.isSkillEnabled(SkillType.ENCHANTING));
        assertFalse("Trading should be disabled", 
            configManager.isSkillEnabled(SkillType.TRADING));
    }
    
    @Test
    public void testDefaultStorageType() {
        configManager.loadConfig();
        
        assertEquals("Storage type should be JSON", "JSON", configManager.getStorageType());
    }
    
    @Test
    public void testCustomStorageType() {
        testConfig.set("storage.type", "SQLITE");
        
        configManager.loadConfig();
        
        assertEquals("Storage type should be SQLITE", "SQLITE", configManager.getStorageType());
    }
    
    @Test
    public void testReloadConfig() {
        configManager.loadConfig();
        
        // Verify initial values
        assertEquals("Initial mining multiplier should be 1.0", 
            1.0, configManager.getExperienceMultiplier(SkillType.MINING), 0.001);
        
        // Change config values
        testConfig.set("experience.mining", 3.0);
        testConfig.set("leveling.base-xp", 500.0);
        
        // Reload config
        configManager.reloadConfig();
        
        // Verify updated values
        assertEquals("Mining multiplier should be updated to 3.0", 
            3.0, configManager.getExperienceMultiplier(SkillType.MINING), 0.001);
        assertEquals("Base XP should be updated to 500.0", 
            500.0, configManager.getBaseXP(), 0.001);
        
        verify(mockPlugin, times(2)).reloadConfig();
    }
    
    @Test
    public void testMissingConfigValuesUseDefaults() {
        // Create config with missing values
        testConfig = new YamlConfiguration();
        when(mockPlugin.getConfig()).thenReturn(testConfig);
        
        configManager.loadConfig();
        
        // Should use default values
        assertEquals("Missing mining multiplier should default to 1.0", 
            1.0, configManager.getExperienceMultiplier(SkillType.MINING), 0.001);
        assertEquals("Missing base XP should default to 100.0", 
            100.0, configManager.getBaseXP(), 0.001);
        assertEquals("Missing exponent should default to 1.25", 
            1.25, configManager.getExponent(), 0.001);
        assertTrue("Missing bonuses enabled should default to true", 
            configManager.areBonusesEnabled());
        assertEquals("Missing boss bar duration should default to 5", 
            5, configManager.getBossBarDuration());
        assertTrue("Missing show XP messages should default to true", 
            configManager.shouldShowXPGainMessages());
        assertEquals("Missing storage type should default to JSON", 
            "JSON", configManager.getStorageType());
    }
    
    @Test
    public void testGetRawConfig() {
        configManager.loadConfig();
        
        FileConfiguration rawConfig = configManager.getConfig();
        
        assertNotNull("Raw config should not be null", rawConfig);
        assertEquals("Raw config should match test config", testConfig, rawConfig);
    }
    
    @Test
    public void testNonExistentBonusSetting() {
        configManager.loadConfig();
        
        double value = configManager.getBonusSetting("nonexistent.setting");
        
        assertEquals("Non-existent bonus setting should return 0.0", 0.0, value, 0.001);
    }
    
    @Test
    public void testEmptyEnabledSkillsList() {
        testConfig.set("enabled-skills", java.util.Collections.emptyList());
        
        configManager.loadConfig();
        
        // When list is empty, all skills should be enabled by default
        assertTrue("Mining should be enabled when list is empty", 
            configManager.isSkillEnabled(SkillType.MINING));
        assertTrue("Combat should be enabled when list is empty", 
            configManager.isSkillEnabled(SkillType.COMBAT));
    }
}
