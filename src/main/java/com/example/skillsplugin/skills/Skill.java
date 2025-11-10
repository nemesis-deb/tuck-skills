package com.example.skillsplugin.skills;

/**
 * Represents a single skill with its current state including level and experience.
 * Handles experience addition and level-up logic with configurable level formulas.
 */
public class Skill {
    private final SkillType type;
    private int level;
    private double experience;
    private LevelFormula levelFormula;
    
    /**
     * Creates a new skill starting at level 1 with 0 experience.
     * Uses default level formula.
     * 
     * @param type The type of skill
     */
    public Skill(SkillType type) {
        this.type = type;
        this.level = 1;
        this.experience = 0.0;
        this.levelFormula = LevelFormula.createDefault();
    }
    
    /**
     * Creates a skill with specified level and experience (for loading from storage).
     * Uses default level formula.
     * 
     * @param type The type of skill
     * @param level The current level
     * @param experience The current experience
     */
    public Skill(SkillType type, int level, double experience) {
        this.type = type;
        this.level = level;
        this.experience = experience;
        this.levelFormula = LevelFormula.createDefault();
    }
    
    /**
     * Creates a skill with a custom level formula.
     * 
     * @param type The type of skill
     * @param levelFormula The level formula to use for XP calculations
     */
    public Skill(SkillType type, LevelFormula levelFormula) {
        this.type = type;
        this.level = 1;
        this.experience = 0.0;
        this.levelFormula = levelFormula;
    }
    
    /**
     * Creates a skill with specified level, experience, and custom level formula.
     * 
     * @param type The type of skill
     * @param level The current level
     * @param experience The current experience
     * @param levelFormula The level formula to use for XP calculations
     */
    public Skill(SkillType type, int level, double experience, LevelFormula levelFormula) {
        this.type = type;
        this.level = level;
        this.experience = experience;
        this.levelFormula = levelFormula;
    }
    
    /**
     * Gets the skill type.
     * 
     * @return The skill type
     */
    public SkillType getType() {
        return type;
    }
    
    /**
     * Gets the current level.
     * 
     * @return The current level
     */
    public int getLevel() {
        return level;
    }
    
    /**
     * Gets the current experience.
     * 
     * @return The current experience
     */
    public double getExperience() {
        return experience;
    }
    
    /**
     * Calculates the experience required to reach the next level.
     * Uses the configured level formula.
     * 
     * @return The experience required for the next level
     */
    public double getRequiredExperience() {
        return levelFormula.getRequiredExperience(level);
    }
    
    /**
     * Sets the level formula for this skill.
     * 
     * @param levelFormula The level formula to use
     */
    public void setLevelFormula(LevelFormula levelFormula) {
        if (levelFormula != null) {
            this.levelFormula = levelFormula;
        }
    }
    
    /**
     * Gets the level formula used by this skill.
     * 
     * @return The level formula
     */
    public LevelFormula getLevelFormula() {
        return levelFormula;
    }
    
    /**
     * Adds experience to this skill and processes level-ups if thresholds are reached.
     * Supports multiple level-ups in a single call if enough experience is gained.
     * 
     * @param amount The amount of experience to add
     * @return true if at least one level-up occurred, false otherwise
     */
    public boolean addExperience(double amount) {
        if (amount <= 0) {
            return false;
        }
        
        experience += amount;
        boolean leveledUp = false;
        
        // Process multiple level-ups if enough experience was gained
        while (experience >= getRequiredExperience()) {
            levelUp();
            leveledUp = true;
        }
        
        return leveledUp;
    }
    
    /**
     * Handles the level-up logic by incrementing the level and carrying over overflow experience.
     */
    private void levelUp() {
        double required = getRequiredExperience();
        experience -= required;
        level++;
    }
    
    /**
     * Sets the level (used for testing or admin commands).
     * 
     * @param level The new level
     */
    public void setLevel(int level) {
        if (level > 0) {
            this.level = level;
        }
    }
    
    /**
     * Sets the experience (used for testing or admin commands).
     * 
     * @param experience The new experience amount
     */
    public void setExperience(double experience) {
        if (experience >= 0) {
            this.experience = experience;
        }
    }
}
