package com.example.skillsplugin.data;

import com.example.skillsplugin.skills.SkillProfile;

import java.util.UUID;

/**
 * Interface for data storage implementations.
 * Provides methods to save, load, and check existence of player skill profiles.
 */
public interface DataStorage {
    
    /**
     * Saves a player's skill profile to persistent storage.
     * 
     * @param playerId The UUID of the player
     * @param profile The skill profile to save
     * @throws DataStorageException if the save operation fails
     */
    void save(UUID playerId, SkillProfile profile) throws DataStorageException;
    
    /**
     * Loads a player's skill profile from persistent storage.
     * 
     * @param playerId The UUID of the player
     * @return The loaded skill profile, or null if not found
     * @throws DataStorageException if the load operation fails
     */
    SkillProfile load(UUID playerId) throws DataStorageException;
    
    /**
     * Checks if a player's skill profile exists in storage.
     * 
     * @param playerId The UUID of the player
     * @return true if the profile exists, false otherwise
     */
    boolean exists(UUID playerId);
    
    /**
     * Initializes the storage system (creates directories, tables, etc.).
     * 
     * @throws DataStorageException if initialization fails
     */
    void initialize() throws DataStorageException;
}
