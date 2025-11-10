package com.example.skillsplugin.data;

import com.example.skillsplugin.skills.Skill;
import com.example.skillsplugin.skills.SkillProfile;
import com.example.skillsplugin.skills.SkillType;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * JSON-based implementation of DataStorage.
 * Stores player skill profiles as JSON files in the playerdata directory.
 */
public class JsonDataStorage implements DataStorage {
    
    private final File dataDirectory;
    private final Gson gson;
    
    /**
     * Creates a new JSON data storage instance.
     * 
     * @param dataDirectory The directory where player data files will be stored
     */
    public JsonDataStorage(File dataDirectory) {
        this.dataDirectory = dataDirectory;
        this.gson = new GsonBuilder()
                .setPrettyPrinting()
                .create();
    }
    
    @Override
    public void initialize() throws DataStorageException {
        if (!dataDirectory.exists()) {
            if (!dataDirectory.mkdirs()) {
                throw new DataStorageException("Failed to create data directory: " + dataDirectory.getAbsolutePath());
            }
        }
        
        if (!dataDirectory.isDirectory()) {
            throw new DataStorageException("Data path exists but is not a directory: " + dataDirectory.getAbsolutePath());
        }
    }
    
    @Override
    public void save(UUID playerId, SkillProfile profile) throws DataStorageException {
        if (playerId == null) {
            throw new DataStorageException("Cannot save profile: playerId is null");
        }
        if (profile == null) {
            throw new DataStorageException("Cannot save profile: profile is null for player " + playerId);
        }
        
        File playerFile = getPlayerFile(playerId);
        File tempFile = new File(playerFile.getAbsolutePath() + ".tmp");
        File backupFile = new File(playerFile.getAbsolutePath() + ".bak");
        
        try {
            // Create JSON object
            JsonObject root = new JsonObject();
            root.addProperty("playerId", playerId.toString());
            
            // Add skills
            JsonObject skillsJson = new JsonObject();
            for (Map.Entry<SkillType, Skill> entry : profile.getSkills().entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    continue; // Skip null entries
                }
                JsonObject skillJson = new JsonObject();
                Skill skill = entry.getValue();
                skillJson.addProperty("level", skill.getLevel());
                skillJson.addProperty("experience", skill.getExperience());
                skillsJson.add(entry.getKey().name(), skillJson);
            }
            root.add("skills", skillsJson);
            
            // Add timestamp
            root.addProperty("lastUpdated", System.currentTimeMillis());
            
            // Write to temporary file first
            try (FileWriter writer = new FileWriter(tempFile)) {
                gson.toJson(root, writer);
                writer.flush();
            }
            
            // Create backup of existing file if it exists
            if (playerFile.exists()) {
                if (backupFile.exists()) {
                    backupFile.delete();
                }
                if (!playerFile.renameTo(backupFile)) {
                    throw new IOException("Failed to create backup file");
                }
            }
            
            // Rename temp file to actual file
            if (!tempFile.renameTo(playerFile)) {
                // Restore backup if rename failed
                if (backupFile.exists()) {
                    backupFile.renameTo(playerFile);
                }
                throw new IOException("Failed to rename temporary file to player file");
            }
            
            // Delete backup on successful save
            if (backupFile.exists()) {
                backupFile.delete();
            }
            
        } catch (IOException e) {
            // Clean up temp file if it exists
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw new DataStorageException("Failed to save player data for " + playerId, e);
        } catch (Exception e) {
            // Clean up temp file if it exists
            if (tempFile.exists()) {
                tempFile.delete();
            }
            throw new DataStorageException("Unexpected error saving player data for " + playerId, e);
        }
    }
    
    @Override
    public SkillProfile load(UUID playerId) throws DataStorageException {
        if (playerId == null) {
            throw new DataStorageException("Cannot load profile: playerId is null");
        }
        
        File playerFile = getPlayerFile(playerId);
        File backupFile = new File(playerFile.getAbsolutePath() + ".bak");
        
        if (!playerFile.exists()) {
            // Check if backup exists
            if (backupFile.exists()) {
                return loadFromFile(playerId, backupFile, true);
            }
            return null;
        }
        
        try {
            return loadFromFile(playerId, playerFile, false);
        } catch (DataStorageException e) {
            // Try loading from backup if main file fails
            if (backupFile.exists()) {
                try {
                    return loadFromFile(playerId, backupFile, true);
                } catch (DataStorageException backupError) {
                    // Both files failed, throw original error
                    throw e;
                }
            }
            throw e;
        }
    }
    
    /**
     * Loads a skill profile from a specific file.
     * 
     * @param playerId The player's UUID
     * @param file The file to load from
     * @param isBackup Whether this is a backup file
     * @return The loaded skill profile
     * @throws DataStorageException If loading fails
     */
    private SkillProfile loadFromFile(UUID playerId, File file, boolean isBackup) throws DataStorageException {
        try (FileReader reader = new FileReader(file)) {
            JsonObject root = JsonParser.parseReader(reader).getAsJsonObject();
            
            // Parse player ID
            UUID loadedPlayerId;
            try {
                loadedPlayerId = UUID.fromString(root.get("playerId").getAsString());
            } catch (Exception e) {
                throw new DataStorageException("Invalid or missing playerId in data file" + (isBackup ? " (backup)" : ""), e);
            }
            
            // Verify player ID matches
            if (!loadedPlayerId.equals(playerId)) {
                throw new DataStorageException("Player ID mismatch: expected " + playerId + ", found " + loadedPlayerId);
            }
            
            // Parse skills
            Map<SkillType, Skill> skills = new HashMap<>();
            JsonObject skillsJson = root.getAsJsonObject("skills");
            
            if (skillsJson == null) {
                throw new DataStorageException("Missing skills data in profile" + (isBackup ? " (backup)" : ""));
            }
            
            for (SkillType type : SkillType.values()) {
                try {
                    if (skillsJson.has(type.name())) {
                        JsonObject skillJson = skillsJson.getAsJsonObject(type.name());
                        int level = skillJson.get("level").getAsInt();
                        double experience = skillJson.get("experience").getAsDouble();
                        
                        // Validate values
                        if (level < 1) {
                            level = 1;
                        }
                        if (experience < 0) {
                            experience = 0;
                        }
                        
                        skills.put(type, new Skill(type, level, experience));
                    } else {
                        // If skill is missing, create default
                        skills.put(type, new Skill(type));
                    }
                } catch (Exception e) {
                    // If individual skill parsing fails, use default
                    skills.put(type, new Skill(type));
                }
            }
            
            return new SkillProfile(loadedPlayerId, skills);
            
        } catch (IOException e) {
            throw new DataStorageException("Failed to load player data for " + playerId + (isBackup ? " from backup" : ""), e);
        } catch (DataStorageException e) {
            throw e;
        } catch (Exception e) {
            throw new DataStorageException("Failed to parse player data for " + playerId + (isBackup ? " from backup" : ""), e);
        }
    }
    
    @Override
    public boolean exists(UUID playerId) {
        return getPlayerFile(playerId).exists();
    }
    
    /**
     * Gets the file for a specific player's data.
     * 
     * @param playerId The player's UUID
     * @return The file where the player's data is stored
     */
    private File getPlayerFile(UUID playerId) {
        return new File(dataDirectory, playerId.toString() + ".json");
    }
}
