package com.example.skillsplugin.events;

import com.example.skillsplugin.data.PlayerDataManager;
import com.example.skillsplugin.skills.SkillProfile;
import com.example.skillsplugin.ui.UIManager;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Handles player connection events (join/quit) for the skills plugin.
 * Manages loading player profiles on join and saving/cleanup on quit.
 */
public class PlayerConnectionListener implements Listener {
    
    private final PlayerDataManager playerDataManager;
    private final UIManager uiManager;
    private final Logger logger;
    
    /**
     * Creates a new player connection listener.
     * 
     * @param playerDataManager The player data manager instance
     * @param uiManager The UI manager instance
     * @param logger The logger instance
     */
    public PlayerConnectionListener(PlayerDataManager playerDataManager, UIManager uiManager, Logger logger) {
        this.playerDataManager = playerDataManager;
        this.uiManager = uiManager;
        this.logger = logger;
    }
    
    /**
     * Handles player join events.
     * Loads the player's skill profile from storage or creates a new one for first-time players.
     * 
     * @param event The player join event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        try {
            // Load or create player profile
            SkillProfile profile = playerDataManager.getProfile(playerId);
            
            // Check if this is a first-time player
            boolean isNewPlayer = !playerDataManager.isCached(playerId) && profile.isNew();
            
            if (isNewPlayer) {
                logger.log(Level.INFO, "First-time player joined: " + player.getName() + " (" + playerId + ")");
                // Profile is automatically initialized with default skills in SkillProfile constructor
            } else {
                logger.log(Level.INFO, "Player joined: " + player.getName() + " (" + playerId + ")");
            }
            
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error loading profile for player " + player.getName() + " (" + playerId + ")", e);
            // Profile will be created with defaults by PlayerDataManager.getProfile()
        }
    }
    
    /**
     * Handles player quit events.
     * Saves the player's skill profile and removes it from cache.
     * Also cleans up any UI elements like boss bars.
     * 
     * @param event The player quit event
     */
    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        UUID playerId = player.getUniqueId();
        
        // Save player profile
        try {
            playerDataManager.saveProfile(playerId);
            logger.log(Level.INFO, "Saved profile for player: " + player.getName() + " (" + playerId + ")");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error saving profile for player " + player.getName() + " (" + playerId + ")", e);
        }
        
        // Clean up UI elements (boss bars) - always execute even if save fails
        try {
            uiManager.cleanupPlayer(player);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error cleaning up UI for player " + player.getName() + " (" + playerId + ")", e);
        }
        
        // Remove from cache - always execute even if previous steps fail
        try {
            playerDataManager.removeFromCache(playerId);
            logger.log(Level.FINE, "Removed player from cache: " + player.getName() + " (" + playerId + ")");
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Error removing player from cache " + player.getName() + " (" + playerId + ")", e);
        }
    }
}
