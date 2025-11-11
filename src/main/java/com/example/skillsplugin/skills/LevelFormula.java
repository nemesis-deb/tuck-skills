package com.example.skillsplugin.skills;

import com.example.skillsplugin.config.ConfigManager;

/**
 * Handles level progression calculations using configurable formulas.
 * Calculates the experience required to reach each level based on config settings.
 */
public class LevelFormula {
    
    private final ConfigManager configManager;
    
    // Default values if config is not available
    private static final double DEFAULT_BASE_XP = 100.0;
    private static final double DEFAULT_EXPONENT = 1.2;
    
    /**
     * Creates a LevelFormula with a ConfigManager for dynamic configuration.
     * 
     * @param configManager The configuration manager to use for formula parameters
     */
    public LevelFormula(ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    /**
     * Calculates the experience required to reach the next level from the current level.
     * Uses the formula: baseXP * (level ^ exponent)
     * 
     * @param currentLevel The current level
     * @return The experience required to reach the next level
     */
    public double getRequiredExperience(int currentLevel) {
        if (currentLevel < 1) {
            return 0.0;
        }
        
        double baseXP = getBaseXP();
        double exponent = getExponent();
        
        return baseXP * Math.pow(currentLevel, exponent);
    }
    
    /**
     * Gets the base XP value from config, or uses default if config is unavailable.
     * 
     * @return The base XP value
     */
    private double getBaseXP() {
        if (configManager != null) {
            return configManager.getBaseXP();
        }
        return DEFAULT_BASE_XP;
    }
    
    /**
     * Gets the exponent value from config, or uses default if config is unavailable.
     * 
     * @return The exponent value
     */
    private double getExponent() {
        if (configManager != null) {
            return configManager.getExponent();
        }
        return DEFAULT_EXPONENT;
    }
    
    /**
     * Creates a LevelFormula with default values (for testing without config).
     * 
     * @return A LevelFormula instance with default values
     */
    public static LevelFormula createDefault() {
        return new LevelFormula(null);
    }
}
