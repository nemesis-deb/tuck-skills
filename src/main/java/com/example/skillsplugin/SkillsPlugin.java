package com.example.skillsplugin;

import com.example.skillsplugin.commands.SkillsCommand;
import com.example.skillsplugin.config.ConfigManager;
import com.example.skillsplugin.data.DataStorage;
import com.example.skillsplugin.data.DataStorageException;
import com.example.skillsplugin.data.JsonDataStorage;
import com.example.skillsplugin.data.PlayerDataManager;
import com.example.skillsplugin.events.PlayerConnectionListener;
import com.example.skillsplugin.events.SkillEventListener;
import com.example.skillsplugin.skills.BonusManager;
import com.example.skillsplugin.skills.ExperienceCalculator;
import com.example.skillsplugin.skills.LevelFormula;
import com.example.skillsplugin.ui.UIManager;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

/**
 * Main plugin class for the Skills Plugin
 * Handles plugin lifecycle and initialization of all managers and listeners
 */
public class SkillsPlugin extends JavaPlugin {
    
    private ConfigManager configManager;
    private DataStorage dataStorage;
    private PlayerDataManager playerDataManager;
    private ExperienceCalculator experienceCalculator;
    private LevelFormula levelFormula;
    private BonusManager bonusManager;
    private UIManager uiManager;
    private SkillEventListener skillEventListener;
    private PlayerConnectionListener playerConnectionListener;
    private SkillsCommand skillsCommand;
    private int autoSaveTaskId = -1;

    @Override
    public void onEnable() {
        try {
            getLogger().log(Level.INFO, "SkillsPlugin is enabling...");
            
            // Initialize configuration manager
            try {
                configManager = new ConfigManager(this);
                configManager.loadConfig();
                getLogger().log(Level.INFO, "Configuration manager initialized successfully");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to initialize configuration manager", e);
                getLogger().log(Level.SEVERE, "Plugin will be disabled due to configuration failure");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Initialize data storage
            try {
                File dataDirectory = new File(getDataFolder(), "playerdata");
                dataStorage = new JsonDataStorage(dataDirectory);
                dataStorage.initialize();
                getLogger().log(Level.INFO, "Data storage initialized successfully");
            } catch (DataStorageException e) {
                getLogger().log(Level.SEVERE, "Failed to initialize data storage", e);
                getLogger().log(Level.SEVERE, "Plugin will be disabled due to data storage failure");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Initialize player data manager
            try {
                playerDataManager = new PlayerDataManager(this, dataStorage);
                getLogger().log(Level.INFO, "Player data manager initialized successfully");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to initialize player data manager", e);
                getLogger().log(Level.SEVERE, "Plugin will be disabled due to player data manager failure");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Initialize level formula
            try {
                levelFormula = new LevelFormula(configManager);
                getLogger().log(Level.INFO, "Level formula initialized successfully");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to initialize level formula", e);
                getLogger().log(Level.SEVERE, "Plugin will be disabled due to level formula failure");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Initialize experience calculator
            try {
                experienceCalculator = new ExperienceCalculator(configManager);
                getLogger().log(Level.INFO, "Experience calculator initialized successfully");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to initialize experience calculator", e);
                getLogger().log(Level.SEVERE, "Plugin will be disabled due to experience calculator failure");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Initialize bonus manager
            try {
                bonusManager = new BonusManager(playerDataManager, configManager);
                getLogger().log(Level.INFO, "Bonus manager initialized successfully");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to initialize bonus manager", e);
                getLogger().log(Level.SEVERE, "Plugin will be disabled due to bonus manager failure");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Initialize UI manager
            try {
                uiManager = new UIManager(this);
                uiManager.setConfigManager(configManager); // Set config manager for sound settings
                getLogger().log(Level.INFO, "UI manager initialized successfully");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to initialize UI manager", e);
                getLogger().log(Level.SEVERE, "Plugin will be disabled due to UI manager failure");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Register event listeners
            try {
                skillEventListener = new SkillEventListener(
                    playerDataManager, 
                    experienceCalculator, 
                    uiManager, 
                    bonusManager,
                    getLogger()
                );
                getServer().getPluginManager().registerEvents(skillEventListener, this);
                
                playerConnectionListener = new PlayerConnectionListener(
                    playerDataManager,
                    uiManager,
                    getLogger()
                );
                getServer().getPluginManager().registerEvents(playerConnectionListener, this);
                
                getLogger().log(Level.INFO, "Event listeners registered successfully");
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to register event listeners", e);
                getLogger().log(Level.SEVERE, "Plugin will be disabled due to event listener registration failure");
                getServer().getPluginManager().disablePlugin(this);
                return;
            }
            
            // Register commands
            try {
                skillsCommand = new SkillsCommand(this, playerDataManager, uiManager);
                if (getCommand("skills") != null) {
                    getCommand("skills").setExecutor(skillsCommand);
                    getCommand("skills").setTabCompleter(skillsCommand);
                    getLogger().log(Level.INFO, "Commands registered successfully");
                } else {
                    getLogger().log(Level.WARNING, "Skills command not found in plugin.yml - command registration skipped");
                }
            } catch (Exception e) {
                getLogger().log(Level.SEVERE, "Failed to register commands", e);
                getLogger().log(Level.WARNING, "Plugin will continue without command support");
            }
            
            // Start auto-save task (every 5 minutes = 6000 ticks)
            try {
                autoSaveTaskId = getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
                    @Override
                    public void run() {
                        try {
                            getLogger().log(Level.INFO, "Running auto-save for all player profiles...");
                            playerDataManager.saveAllProfiles();
                            getLogger().log(Level.INFO, "Auto-save completed successfully");
                        } catch (Exception e) {
                            getLogger().log(Level.SEVERE, "Error during auto-save", e);
                        }
                    }
                }, 6000L, 6000L); // Start after 5 minutes, repeat every 5 minutes
                getLogger().log(Level.INFO, "Auto-save task started (every 5 minutes)");
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to start auto-save task - data will only be saved on player quit and server shutdown", e);
            }
            
            getLogger().log(Level.INFO, "SkillsPlugin has been enabled successfully!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Unexpected error during plugin initialization", e);
            getLogger().log(Level.SEVERE, "Plugin will be disabled");
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        try {
            getLogger().log(Level.INFO, "SkillsPlugin is disabling...");
            
            // Cancel auto-save task
            if (autoSaveTaskId != -1) {
                try {
                    getServer().getScheduler().cancelTask(autoSaveTaskId);
                    getLogger().log(Level.INFO, "Auto-save task cancelled");
                } catch (Exception e) {
                    getLogger().log(Level.WARNING, "Error cancelling auto-save task", e);
                }
            }
            
            // Save all cached player profiles
            if (playerDataManager != null) {
                try {
                    getLogger().log(Level.INFO, "Saving all player profiles...");
                    playerDataManager.saveAllProfiles();
                    getLogger().log(Level.INFO, "All player profiles saved successfully");
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Error occurred while saving player profiles during shutdown", e);
                    getLogger().log(Level.SEVERE, "Some player data may not have been saved!");
                }
            } else {
                getLogger().log(Level.WARNING, "Player data manager was null during shutdown - no profiles to save");
            }
            
            // Cleanup boss bars
            if (uiManager != null) {
                try {
                    getLogger().log(Level.INFO, "Cleaning up UI elements...");
                    uiManager.cleanup();
                    getLogger().log(Level.INFO, "UI cleanup completed successfully");
                } catch (Exception e) {
                    getLogger().log(Level.SEVERE, "Error occurred while cleaning up UI elements", e);
                }
            } else {
                getLogger().log(Level.WARNING, "UI manager was null during shutdown - no UI elements to clean up");
            }
            
            // Clear references to help with garbage collection
            configManager = null;
            dataStorage = null;
            playerDataManager = null;
            experienceCalculator = null;
            levelFormula = null;
            bonusManager = null;
            uiManager = null;
            skillEventListener = null;
            playerConnectionListener = null;
            skillsCommand = null;
            
            getLogger().log(Level.INFO, "SkillsPlugin has been disabled successfully!");
            
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Unexpected error during plugin shutdown", e);
            getLogger().log(Level.SEVERE, "Plugin shutdown may not have completed cleanly");
        }
    }
    
    /**
     * Gets the configuration manager instance.
     * 
     * @return The configuration manager instance
     */
    public ConfigManager getConfigManager() {
        return configManager;
    }
    
    /**
     * Gets the data storage instance.
     * 
     * @return The data storage instance
     */
    public DataStorage getDataStorage() {
        return dataStorage;
    }
    
    /**
     * Gets the player data manager instance.
     * 
     * @return The player data manager instance
     */
    public PlayerDataManager getPlayerDataManager() {
        return playerDataManager;
    }
    
    /**
     * Gets the bonus manager instance.
     * 
     * @return The bonus manager instance
     */
    public BonusManager getBonusManager() {
        return bonusManager;
    }
    
    /**
     * Gets the UI manager instance.
     * 
     * @return The UI manager instance
     */
    public UIManager getUIManager() {
        return uiManager;
    }
}
