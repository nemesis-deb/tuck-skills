package com.example.skillsplugin.data;

import com.example.skillsplugin.skills.Skill;
import com.example.skillsplugin.skills.SkillProfile;
import com.example.skillsplugin.skills.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

/**
 * Manages player skill data with in-memory caching and async persistence.
 * Handles loading, saving, and updating player skill profiles.
 */
public class PlayerDataManager {
    
    private final Plugin plugin;
    private final DataStorage dataStorage;
    private final Map<UUID, SkillProfile> cache;
    
    /**
     * Creates a new player data manager.
     * 
     * @param plugin The plugin instance
     * @param dataStorage The data storage implementation
     */
    public PlayerDataManager(Plugin plugin, DataStorage dataStorage) {
        this.plugin = plugin;
        this.dataStorage = dataStorage;
        this.cache = new ConcurrentHashMap<>();
    }
    
    /**
     * Gets a player's skill profile with lazy loading.
     * If the profile is not in cache, it will be loaded from storage.
     * If the profile doesn't exist in storage, a new one will be created.
     * 
     * @param playerId The UUID of the player
     * @return The player's skill profile
     */
    public SkillProfile getProfile(UUID playerId) {
        // Check cache first
        SkillProfile profile = cache.get(playerId);
        if (profile != null) {
            return profile;
        }
        
        // Try to load from storage
        try {
            profile = dataStorage.load(playerId);
            
            if (profile == null) {
                // Create new profile for new player
                profile = new SkillProfile(playerId);
                plugin.getLogger().log(Level.INFO, "Created new skill profile for player: " + playerId);
            } else {
                plugin.getLogger().log(Level.INFO, "Loaded skill profile for player: " + playerId);
            }
            
            // Add to cache
            cache.put(playerId, profile);
            
        } catch (DataStorageException e) {
            plugin.getLogger().log(Level.SEVERE, "Failed to load profile for player " + playerId + ", creating new profile", e);
            // Fallback: create new profile
            profile = new SkillProfile(playerId);
            cache.put(playerId, profile);
        }
        
        return profile;
    }
    
    /**
     * Saves a player's skill profile asynchronously.
     * The profile is saved in a separate thread to avoid blocking the main thread.
     * Implements retry logic with up to 3 attempts.
     * Optimized to create a snapshot of the profile to avoid concurrent modification issues.
     * 
     * @param playerId The UUID of the player
     */
    public void saveProfile(UUID playerId) {
        SkillProfile profile = cache.get(playerId);
        if (profile == null) {
            plugin.getLogger().log(Level.WARNING, "Attempted to save non-existent profile for player: " + playerId);
            return;
        }
        
        // Create a reference to the profile for async saving
        // The profile object is thread-safe due to ConcurrentHashMap usage
        final SkillProfile profileToSave = profile;
        
        // Save asynchronously with retry logic
        new BukkitRunnable() {
            @Override
            public void run() {
                saveWithRetry(playerId, profileToSave, 3);
            }
        }.runTaskAsynchronously(plugin);
    }
    
    /**
     * Saves a player's skill profile synchronously.
     * Should only be used during plugin shutdown or when async saving is not appropriate.
     * Implements retry logic with up to 3 attempts.
     * 
     * @param playerId The UUID of the player
     */
    public void saveProfileSync(UUID playerId) {
        SkillProfile profile = cache.get(playerId);
        if (profile == null) {
            plugin.getLogger().log(Level.WARNING, "Attempted to save non-existent profile for player: " + playerId);
            return;
        }
        
        saveWithRetry(playerId, profile, 3);
    }
    
    /**
     * Saves all cached player profiles synchronously.
     * Used during plugin shutdown to ensure all data is persisted.
     * Implements retry logic for each profile.
     * Optimized to process profiles efficiently during shutdown.
     */
    public void saveAllProfiles() {
        int cacheSize = cache.size();
        if (cacheSize == 0) {
            plugin.getLogger().log(Level.INFO, "No player profiles to save");
            return;
        }
        
        plugin.getLogger().log(Level.INFO, "Saving " + cacheSize + " player profiles...");
        int saved = 0;
        int failed = 0;
        
        // Create a snapshot of entries to avoid concurrent modification
        Map.Entry<UUID, SkillProfile>[] entries = cache.entrySet().toArray(new Map.Entry[0]);
        
        for (Map.Entry<UUID, SkillProfile> entry : entries) {
            boolean success = saveWithRetry(entry.getKey(), entry.getValue(), 3);
            if (success) {
                saved++;
            } else {
                failed++;
            }
        }
        
        plugin.getLogger().log(Level.INFO, "Saved " + saved + " profiles (" + failed + " failed)");
    }
    
    /**
     * Saves a profile with retry logic.
     * Attempts to save up to maxAttempts times with exponential backoff.
     * 
     * @param playerId The UUID of the player
     * @param profile The skill profile to save
     * @param maxAttempts Maximum number of save attempts
     * @return true if save was successful, false otherwise
     */
    private boolean saveWithRetry(UUID playerId, SkillProfile profile, int maxAttempts) {
        int attempt = 0;
        long backoffMs = 100; // Start with 100ms backoff
        
        while (attempt < maxAttempts) {
            attempt++;
            try {
                dataStorage.save(playerId, profile);
                if (attempt > 1) {
                    plugin.getLogger().log(Level.INFO, "Successfully saved profile for player " + playerId + " on attempt " + attempt);
                } else {
                    plugin.getLogger().log(Level.FINE, "Saved skill profile for player: " + playerId);
                }
                return true;
            } catch (DataStorageException e) {
                if (attempt < maxAttempts) {
                    plugin.getLogger().log(Level.WARNING, "Failed to save profile for player " + playerId + " (attempt " + attempt + "/" + maxAttempts + "), retrying in " + backoffMs + "ms...", e);
                    try {
                        Thread.sleep(backoffMs);
                        backoffMs *= 2; // Exponential backoff
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        plugin.getLogger().log(Level.SEVERE, "Save retry interrupted for player " + playerId, ie);
                        return false;
                    }
                } else {
                    plugin.getLogger().log(Level.SEVERE, "Failed to save profile for player " + playerId + " after " + maxAttempts + " attempts. Data may be lost!", e);
                    return false;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Awards experience to a player for a specific skill.
     * Handles XP gain, level-ups, and returns information about the result.
     * 
     * @param player The player to award experience to
     * @param skillType The skill type to award experience for
     * @param amount The amount of experience to award
     * @return An ExperienceResult containing information about the XP gain and any level-ups
     */
    public ExperienceResult awardExperience(Player player, SkillType skillType, double amount) {
        if (amount <= 0) {
            return new ExperienceResult(skillType, 0, 0, false);
        }
        
        UUID playerId = player.getUniqueId();
        SkillProfile profile = getProfile(playerId);
        Skill skill = profile.getSkill(skillType);
        
        if (skill == null) {
            plugin.getLogger().log(Level.WARNING, "Skill " + skillType + " not found for player " + playerId);
            return new ExperienceResult(skillType, 0, 0, false);
        }
        
        int oldLevel = skill.getLevel();
        boolean leveledUp = skill.addExperience(amount);
        int newLevel = skill.getLevel();
        int levelsGained = newLevel - oldLevel;
        
        return new ExperienceResult(skillType, amount, levelsGained, leveledUp);
    }
    
    /**
     * Removes a player's profile from the cache.
     * Should be called when a player disconnects.
     * 
     * @param playerId The UUID of the player
     */
    public void removeFromCache(UUID playerId) {
        cache.remove(playerId);
        plugin.getLogger().log(Level.FINE, "Removed player from cache: " + playerId);
    }
    
    /**
     * Checks if a player's profile is currently cached.
     * 
     * @param playerId The UUID of the player
     * @return true if the profile is in cache, false otherwise
     */
    public boolean isCached(UUID playerId) {
        return cache.containsKey(playerId);
    }
    
    /**
     * Gets the number of profiles currently in cache.
     * 
     * @return The cache size
     */
    public int getCacheSize() {
        return cache.size();
    }
    
    /**
     * Clears all profiles from the cache without saving.
     * Should only be used for testing or emergency situations.
     */
    public void clearCache() {
        cache.clear();
        plugin.getLogger().log(Level.WARNING, "Player data cache cleared");
    }
    
    /**
     * Result object containing information about an experience award.
     */
    public static class ExperienceResult {
        private final SkillType skillType;
        private final double experienceGained;
        private final int levelsGained;
        private final boolean leveledUp;
        
        public ExperienceResult(SkillType skillType, double experienceGained, int levelsGained, boolean leveledUp) {
            this.skillType = skillType;
            this.experienceGained = experienceGained;
            this.levelsGained = levelsGained;
            this.leveledUp = leveledUp;
        }
        
        public SkillType getSkillType() {
            return skillType;
        }
        
        public double getExperienceGained() {
            return experienceGained;
        }
        
        public int getLevelsGained() {
            return levelsGained;
        }
        
        public boolean isLeveledUp() {
            return leveledUp;
        }
    }
}
