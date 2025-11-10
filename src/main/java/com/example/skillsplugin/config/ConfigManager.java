package com.example.skillsplugin.config;

import com.example.skillsplugin.SkillsPlugin;
import com.example.skillsplugin.skills.SkillType;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Manages plugin configuration loading, parsing, and reloading
 */
public class ConfigManager {
    
    private final SkillsPlugin plugin;
    private FileConfiguration config;
    
    // Cached configuration values for performance
    private Map<SkillType, Double> experienceMultipliers;
    private double baseXP;
    private double exponent;
    private boolean bonusesEnabled;
    private Map<String, Double> bonusSettings;
    private int bossBarDuration;
    private boolean showXPGainMessages;
    private List<String> enabledSkills;
    private String storageType;
    
    // Additional cached values for hot paths
    private Map<SkillType, Boolean> skillEnabledCache;
    private long bossBarDurationTicks; // Pre-calculated ticks for boss bar duration
    
    public ConfigManager(SkillsPlugin plugin) {
        this.plugin = plugin;
        this.experienceMultipliers = new HashMap<>();
        this.bonusSettings = new HashMap<>();
        this.skillEnabledCache = new HashMap<>();
    }
    
    /**
     * Loads the configuration from config.yml
     * Creates default config if it doesn't exist
     * Falls back to default values if config loading fails
     */
    public void loadConfig() {
        try {
            // Save default config if it doesn't exist
            plugin.saveDefaultConfig();
            
            // Reload config from disk
            plugin.reloadConfig();
            config = plugin.getConfig();
            
            // Parse and cache configuration values
            parseConfig();
            
            plugin.getLogger().log(Level.INFO, "Configuration loaded successfully");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load configuration, using default values", e);
            // Initialize with default values
            initializeDefaults();
        }
    }
    
    /**
     * Reloads the configuration from disk
     * Falls back to previous values if reload fails
     */
    public void reloadConfig() {
        try {
            plugin.reloadConfig();
            config = plugin.getConfig();
            parseConfig();
            plugin.getLogger().log(Level.INFO, "Configuration reloaded successfully");
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to reload configuration, keeping previous values", e);
            // Keep existing cached values - don't reinitialize to defaults
        }
    }
    
    /**
     * Parses configuration values and caches them
     * Uses default values for any missing or invalid configuration entries
     */
    private void parseConfig() {
        try {
            // Parse experience multipliers
            experienceMultipliers.clear();
            for (SkillType skillType : SkillType.values()) {
                try {
                    String path = "experience." + skillType.name().toLowerCase();
                    double multiplier = config.getDouble(path, 1.0);
                    // Validate multiplier is positive
                    if (multiplier <= 0) {
                        plugin.getLogger().log(Level.WARNING, "Invalid experience multiplier for " + skillType + ": " + multiplier + ", using default 1.0");
                        multiplier = 1.0;
                    }
                    experienceMultipliers.put(skillType, multiplier);
                } catch (Exception e) {
                    plugin.getLogger().log(Level.WARNING, "Error parsing experience multiplier for " + skillType + ", using default 1.0", e);
                    experienceMultipliers.put(skillType, 1.0);
                }
            }
            
            // Parse leveling formula settings
            try {
                baseXP = config.getDouble("leveling.base-xp", 100.0);
                if (baseXP <= 0) {
                    plugin.getLogger().log(Level.WARNING, "Invalid base XP value: " + baseXP + ", using default 100.0");
                    baseXP = 100.0;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error parsing base XP, using default 100.0", e);
                baseXP = 100.0;
            }
            
            try {
                exponent = config.getDouble("leveling.exponent", 1.5);
                if (exponent <= 0) {
                    plugin.getLogger().log(Level.WARNING, "Invalid exponent value: " + exponent + ", using default 1.5");
                    exponent = 1.5;
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error parsing exponent, using default 1.5", e);
                exponent = 1.5;
            }
            
            // Parse bonus settings
            try {
                bonusesEnabled = config.getBoolean("bonuses.enabled", true);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error parsing bonuses.enabled, using default true", e);
                bonusesEnabled = true;
            }
            
            bonusSettings.clear();
            parseBonusSetting("mining.double-drop-chance-per-level", 0.5);
            parseBonusSetting("woodcutting.double-drop-chance-per-level", 0.5);
            parseBonusSetting("combat.damage-bonus-per-level", 0.5);
            parseBonusSetting("farming.double-crop-chance-per-level", 0.5);
            parseBonusSetting("fishing.treasure-chance-per-level", 0.3);
            parseBonusSetting("enchanting.cost-reduction-per-level", 0.5);
            parseBonusSetting("trading.discount-per-level", 0.3);
            
            // Parse UI settings
            try {
                bossBarDuration = config.getInt("ui.boss-bar-duration", 5);
                if (bossBarDuration <= 0) {
                    plugin.getLogger().log(Level.WARNING, "Invalid boss bar duration: " + bossBarDuration + ", using default 5");
                    bossBarDuration = 5;
                }
                // Pre-calculate ticks for performance (1 second = 20 ticks)
                bossBarDurationTicks = bossBarDuration * 20L;
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error parsing boss bar duration, using default 5", e);
                bossBarDuration = 5;
                bossBarDurationTicks = 100L;
            }
            
            try {
                showXPGainMessages = config.getBoolean("ui.show-xp-gain-messages", true);
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error parsing show-xp-gain-messages, using default true", e);
                showXPGainMessages = true;
            }
            
            // Parse enabled skills
            try {
                enabledSkills = config.getStringList("enabled-skills");
                if (enabledSkills.isEmpty()) {
                    // Default to all skills if not specified
                    for (SkillType skillType : SkillType.values()) {
                        enabledSkills.add(skillType.name());
                    }
                }
                
                // Build skill enabled cache for O(1) lookups
                skillEnabledCache.clear();
                for (SkillType skillType : SkillType.values()) {
                    skillEnabledCache.put(skillType, enabledSkills.contains(skillType.name()));
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error parsing enabled skills, enabling all skills by default", e);
                enabledSkills.clear();
                skillEnabledCache.clear();
                for (SkillType skillType : SkillType.values()) {
                    enabledSkills.add(skillType.name());
                    skillEnabledCache.put(skillType, true);
                }
            }
            
            // Parse storage type
            try {
                storageType = config.getString("storage.type", "JSON");
                if (storageType == null || storageType.isEmpty()) {
                    plugin.getLogger().log(Level.WARNING, "Invalid storage type, using default JSON");
                    storageType = "JSON";
                }
            } catch (Exception e) {
                plugin.getLogger().log(Level.WARNING, "Error parsing storage type, using default JSON", e);
                storageType = "JSON";
            }
            
        } catch (Exception e) {
            plugin.getLogger().log(Level.SEVERE, "Critical error parsing configuration, falling back to defaults", e);
            initializeDefaults();
        }
    }
    
    /**
     * Parses a single bonus setting with error handling
     */
    private void parseBonusSetting(String key, double defaultValue) {
        try {
            String configPath = "bonuses." + key;
            double value = config.getDouble(configPath, defaultValue);
            if (value < 0) {
                plugin.getLogger().log(Level.WARNING, "Invalid bonus setting for " + key + ": " + value + ", using default " + defaultValue);
                value = defaultValue;
            }
            bonusSettings.put(key, value);
        } catch (Exception e) {
            plugin.getLogger().log(Level.WARNING, "Error parsing bonus setting " + key + ", using default " + defaultValue, e);
            bonusSettings.put(key, defaultValue);
        }
    }
    
    /**
     * Initializes all configuration values to their defaults
     * Used as a fallback when configuration loading fails completely
     */
    private void initializeDefaults() {
        plugin.getLogger().log(Level.INFO, "Initializing default configuration values");
        
        // Default experience multipliers
        experienceMultipliers.clear();
        for (SkillType skillType : SkillType.values()) {
            experienceMultipliers.put(skillType, 1.0);
        }
        
        // Default leveling formula
        baseXP = 100.0;
        exponent = 1.5;
        
        // Default bonus settings
        bonusesEnabled = true;
        bonusSettings.clear();
        bonusSettings.put("mining.double-drop-chance-per-level", 0.5);
        bonusSettings.put("woodcutting.double-drop-chance-per-level", 0.5);
        bonusSettings.put("combat.damage-bonus-per-level", 0.5);
        bonusSettings.put("farming.double-crop-chance-per-level", 0.5);
        bonusSettings.put("fishing.treasure-chance-per-level", 0.3);
        bonusSettings.put("enchanting.cost-reduction-per-level", 0.5);
        bonusSettings.put("trading.discount-per-level", 0.3);
        
        // Default UI settings
        bossBarDuration = 5;
        bossBarDurationTicks = 100L;
        showXPGainMessages = true;
        
        // Default enabled skills (all)
        enabledSkills.clear();
        skillEnabledCache.clear();
        for (SkillType skillType : SkillType.values()) {
            enabledSkills.add(skillType.name());
            skillEnabledCache.put(skillType, true);
        }
        
        // Default storage type
        storageType = "JSON";
        
        plugin.getLogger().log(Level.INFO, "Default configuration values initialized");
    }
    
    /**
     * Gets the experience multiplier for a specific skill
     */
    public double getExperienceMultiplier(SkillType skillType) {
        return experienceMultipliers.getOrDefault(skillType, 1.0);
    }
    
    /**
     * Gets the base XP value for level calculations
     */
    public double getBaseXP() {
        return baseXP;
    }
    
    /**
     * Gets the exponent for level calculations
     */
    public double getExponent() {
        return exponent;
    }
    
    /**
     * Checks if bonuses are enabled
     */
    public boolean areBonusesEnabled() {
        return bonusesEnabled;
    }
    
    /**
     * Gets a bonus setting value
     */
    public double getBonusSetting(String key) {
        return bonusSettings.getOrDefault(key, 0.0);
    }
    
    /**
     * Gets the boss bar duration in seconds
     */
    public int getBossBarDuration() {
        return bossBarDuration;
    }
    
    /**
     * Checks if XP gain messages should be shown
     */
    public boolean shouldShowXPGainMessages() {
        return showXPGainMessages;
    }
    
    /**
     * Checks if a skill is enabled (optimized with cache for O(1) lookup)
     */
    public boolean isSkillEnabled(SkillType skillType) {
        return skillEnabledCache.getOrDefault(skillType, true);
    }
    
    /**
     * Gets the boss bar duration in ticks (pre-calculated for performance)
     */
    public long getBossBarDurationTicks() {
        return bossBarDurationTicks;
    }
    
    /**
     * Gets the storage type (JSON or SQLITE)
     */
    public String getStorageType() {
        return storageType;
    }
    
    /**
     * Gets the raw FileConfiguration object
     */
    public FileConfiguration getConfig() {
        return config;
    }
}
