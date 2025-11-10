package com.example.skillsplugin.skills;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Represents a player's complete skill profile containing all their skills.
 * Each player has one SkillProfile that holds all 7 skills.
 */
public class SkillProfile {
    private final UUID playerId;
    private final Map<SkillType, Skill> skills;
    private boolean isNew;
    
    /**
     * Creates a new skill profile for a player with all skills initialized at level 1.
     * 
     * @param playerId The UUID of the player
     */
    public SkillProfile(UUID playerId) {
        this.playerId = playerId;
        this.skills = new HashMap<>();
        this.isNew = true;
        initializeSkills();
    }
    
    /**
     * Creates a skill profile with existing skills (for loading from storage).
     * 
     * @param playerId The UUID of the player
     * @param skills The map of existing skills
     */
    public SkillProfile(UUID playerId, Map<SkillType, Skill> skills) {
        this.playerId = playerId;
        this.skills = skills;
        this.isNew = false;
    }
    
    /**
     * Initializes all skills at level 1 with 0 experience.
     */
    private void initializeSkills() {
        for (SkillType type : SkillType.values()) {
            skills.put(type, new Skill(type));
        }
    }
    
    /**
     * Gets the player's UUID.
     * 
     * @return The player's UUID
     */
    public UUID getPlayerId() {
        return playerId;
    }
    
    /**
     * Gets a specific skill by type.
     * 
     * @param type The skill type to retrieve
     * @return The skill, or null if not found
     */
    public Skill getSkill(SkillType type) {
        return skills.get(type);
    }
    
    /**
     * Gets all skills in this profile.
     * 
     * @return A map of all skills
     */
    public Map<SkillType, Skill> getSkills() {
        return skills;
    }
    
    /**
     * Adds a skill to the profile (used when loading from storage).
     * 
     * @param skill The skill to add
     */
    public void addSkill(Skill skill) {
        skills.put(skill.getType(), skill);
    }
    
    /**
     * Checks if this is a newly created profile (first-time player).
     * 
     * @return true if this is a new profile, false if loaded from storage
     */
    public boolean isNew() {
        return isNew;
    }
    
    /**
     * Marks this profile as no longer new (used after first save).
     */
    public void markAsExisting() {
        this.isNew = false;
    }
}
