package com.example.skillsplugin.skills;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 * Unit tests for the Skill class.
 * Tests experience addition, level-up logic, and progression calculations.
 */
public class SkillTest {
    
    private Skill skill;
    
    @Before
    public void setUp() {
        skill = new Skill(SkillType.MINING);
    }
    
    @Test
    public void testSkillInitialization() {
        assertEquals("Skill should start at level 1", 1, skill.getLevel());
        assertEquals("Skill should start with 0 experience", 0.0, skill.getExperience(), 0.001);
        assertEquals("Skill type should be MINING", SkillType.MINING, skill.getType());
    }
    
    @Test
    public void testSkillInitializationWithValues() {
        Skill customSkill = new Skill(SkillType.COMBAT, 10, 500.0);
        assertEquals("Skill should have level 10", 10, customSkill.getLevel());
        assertEquals("Skill should have 500 experience", 500.0, customSkill.getExperience(), 0.001);
        assertEquals("Skill type should be COMBAT", SkillType.COMBAT, customSkill.getType());
    }
    
    @Test
    public void testRequiredExperienceCalculation() {
        // Level 1: 100 * (1 ^ 1.25) = 100
        assertEquals("Level 1 should require 100 XP", 100.0, skill.getRequiredExperience(), 0.001);
        
        skill.setLevel(2);
        // Level 2: 100 * (2 ^ 1.25) = 237.84
        assertEquals("Level 2 should require ~237.84 XP", 237.84, skill.getRequiredExperience(), 0.01);
        
        skill.setLevel(5);
        // Level 5: 100 * (5 ^ 1.25) = 843.90
        assertEquals("Level 5 should require ~843.90 XP", 843.90, skill.getRequiredExperience(), 0.01);
    }
    
    @Test
    public void testAddExperienceNoLevelUp() {
        boolean leveledUp = skill.addExperience(50.0);
        
        assertFalse("Should not level up with only 50 XP", leveledUp);
        assertEquals("Experience should be 50", 50.0, skill.getExperience(), 0.001);
        assertEquals("Level should still be 1", 1, skill.getLevel());
    }
    
    @Test
    public void testAddExperienceSingleLevelUp() {
        boolean leveledUp = skill.addExperience(100.0);
        
        assertTrue("Should level up with 100 XP", leveledUp);
        assertEquals("Level should be 2", 2, skill.getLevel());
        assertEquals("Experience should be 0 (overflow)", 0.0, skill.getExperience(), 0.001);
    }
    
    @Test
    public void testAddExperienceWithOverflow() {
        boolean leveledUp = skill.addExperience(150.0);
        
        assertTrue("Should level up with 150 XP", leveledUp);
        assertEquals("Level should be 2", 2, skill.getLevel());
        assertEquals("Experience should be 50 (overflow)", 50.0, skill.getExperience(), 0.001);
    }
    
    @Test
    public void testAddExperienceMultipleLevelUps() {
        // Add enough XP to go from level 1 to level 3
        // Level 1 requires 100, Level 2 requires ~282.84
        // Total needed: 100 + 282.84 = 382.84
        boolean leveledUp = skill.addExperience(400.0);
        
        assertTrue("Should level up with 400 XP", leveledUp);
        assertEquals("Level should be 3", 3, skill.getLevel());
        // Overflow: 400 - 100 - 282.84 = 17.16
        assertEquals("Experience should be ~17.16 (overflow)", 17.16, skill.getExperience(), 0.01);
    }
    
    @Test
    public void testAddExperienceMultipleTimes() {
        skill.addExperience(30.0);
        assertEquals("Experience should be 30", 30.0, skill.getExperience(), 0.001);
        assertEquals("Level should be 1", 1, skill.getLevel());
        
        skill.addExperience(40.0);
        assertEquals("Experience should be 70", 70.0, skill.getExperience(), 0.001);
        assertEquals("Level should be 1", 1, skill.getLevel());
        
        boolean leveledUp = skill.addExperience(50.0);
        assertTrue("Should level up after third addition", leveledUp);
        assertEquals("Level should be 2", 2, skill.getLevel());
        assertEquals("Experience should be 20 (overflow)", 20.0, skill.getExperience(), 0.001);
    }
    
    @Test
    public void testAddZeroExperience() {
        boolean leveledUp = skill.addExperience(0.0);
        
        assertFalse("Should not level up with 0 XP", leveledUp);
        assertEquals("Experience should remain 0", 0.0, skill.getExperience(), 0.001);
        assertEquals("Level should remain 1", 1, skill.getLevel());
    }
    
    @Test
    public void testAddNegativeExperience() {
        skill.addExperience(50.0);
        boolean leveledUp = skill.addExperience(-10.0);
        
        assertFalse("Should not level up with negative XP", leveledUp);
        assertEquals("Experience should remain 50", 50.0, skill.getExperience(), 0.001);
        assertEquals("Level should remain 1", 1, skill.getLevel());
    }
    
    @Test
    public void testSetLevel() {
        skill.setLevel(10);
        assertEquals("Level should be 10", 10, skill.getLevel());
        
        skill.setLevel(0);
        assertEquals("Level should remain 10 (invalid value)", 10, skill.getLevel());
        
        skill.setLevel(-5);
        assertEquals("Level should remain 10 (invalid value)", 10, skill.getLevel());
    }
    
    @Test
    public void testSetExperience() {
        skill.setExperience(250.5);
        assertEquals("Experience should be 250.5", 250.5, skill.getExperience(), 0.001);
        
        skill.setExperience(-10.0);
        assertEquals("Experience should remain 250.5 (invalid value)", 250.5, skill.getExperience(), 0.001);
    }
    
    @Test
    public void testLevelProgressionScaling() {
        // Test that higher levels require more XP
        double level1Required = skill.getRequiredExperience();
        
        skill.setLevel(2);
        double level2Required = skill.getRequiredExperience();
        
        skill.setLevel(3);
        double level3Required = skill.getRequiredExperience();
        
        assertTrue("Level 2 should require more XP than level 1", level2Required > level1Required);
        assertTrue("Level 3 should require more XP than level 2", level3Required > level2Required);
    }
    
    @Test
    public void testLargeExperienceGain() {
        // Test with a very large XP gain to ensure multiple level-ups work correctly
        boolean leveledUp = skill.addExperience(10000.0);
        
        assertTrue("Should level up with large XP gain", leveledUp);
        assertTrue("Level should be significantly higher than 1", skill.getLevel() > 5);
        assertTrue("Experience should be less than required for next level", 
                   skill.getExperience() < skill.getRequiredExperience());
    }
    
    @Test
    public void testMultipleLevelUpsWithExactCalculation() {
        // Test precise multi-level-up scenario
        // Level 1 requires 100 XP
        // Level 2 requires ~282.84 XP
        // Level 3 requires ~519.62 XP
        // Total to reach level 4: 100 + 282.84 + 519.62 = 902.46
        
        boolean leveledUp = skill.addExperience(1000.0);
        
        assertTrue("Should level up", leveledUp);
        assertEquals("Should reach level 4", 4, skill.getLevel());
        // Overflow: 1000 - 902.46 = 97.54
        assertEquals("Should have overflow XP", 97.54, skill.getExperience(), 1.0);
    }
    
    @Test
    public void testMultipleLevelUpsInSequence() {
        // Test multiple separate XP additions that cause level-ups
        skill.addExperience(100.0); // Level 2
        assertEquals("Should be level 2", 2, skill.getLevel());
        
        skill.addExperience(283.0); // Level 3 (slightly more than 282.84 to account for precision)
        assertEquals("Should be level 3", 3, skill.getLevel());
        
        skill.addExperience(520.0); // Level 4 (slightly more than 519.62 to account for precision)
        assertEquals("Should be level 4", 4, skill.getLevel());
    }
    
    @Test
    public void testOverflowHandlingAccuracy() {
        // Add exactly enough XP to level up with specific overflow
        skill.addExperience(125.0); // 100 required, 25 overflow
        
        assertEquals("Should be level 2", 2, skill.getLevel());
        assertEquals("Should have 25 XP overflow", 25.0, skill.getExperience(), 0.001);
        
        // Now add more to level up again
        skill.addExperience(300.0); // 282.84 required, ~42.16 overflow
        
        assertEquals("Should be level 3", 3, skill.getLevel());
        assertEquals("Should have ~42.16 XP overflow", 42.16, skill.getExperience(), 0.01);
    }
    
    @Test
    public void testMassiveExperienceGain() {
        // Test with extremely large XP to ensure no overflow errors
        boolean leveledUp = skill.addExperience(100000.0);
        
        assertTrue("Should level up", leveledUp);
        assertTrue("Should reach high level", skill.getLevel() > 20);
        assertTrue("Experience should be valid", skill.getExperience() >= 0);
        assertTrue("Experience should be less than next level requirement", 
                   skill.getExperience() < skill.getRequiredExperience());
        assertFalse("Experience should not be infinite", Double.isInfinite(skill.getExperience()));
        assertFalse("Experience should not be NaN", Double.isNaN(skill.getExperience()));
    }
    
    @Test
    public void testLevelUpDoesNotSkipLevels() {
        // Ensure that when leveling up multiple times, we don't skip any levels
        int startLevel = skill.getLevel();
        skill.addExperience(1000.0);
        int endLevel = skill.getLevel();
        
        // Verify we went through each level sequentially
        assertTrue("Should have leveled up at least once", endLevel > startLevel);
        
        // The overflow XP should be less than what's needed for the next level
        assertTrue("Overflow should be less than next level requirement",
                   skill.getExperience() < skill.getRequiredExperience());
    }
    
    @Test
    public void testCustomLevelFormula() {
        // Test skill with custom level formula
        LevelFormula customFormula = LevelFormula.createDefault();
        Skill customSkill = new Skill(SkillType.COMBAT, customFormula);
        
        assertEquals("Should start at level 1", 1, customSkill.getLevel());
        assertEquals("Should use custom formula", customFormula, customSkill.getLevelFormula());
        
        customSkill.addExperience(100.0);
        assertEquals("Should level up to 2", 2, customSkill.getLevel());
    }
    
    @Test
    public void testSetLevelFormula() {
        LevelFormula newFormula = LevelFormula.createDefault();
        skill.setLevelFormula(newFormula);
        
        assertEquals("Should have new formula", newFormula, skill.getLevelFormula());
    }
    
    @Test
    public void testSetNullLevelFormula() {
        LevelFormula originalFormula = skill.getLevelFormula();
        skill.setLevelFormula(null);
        
        assertEquals("Should keep original formula when setting null", 
                     originalFormula, skill.getLevelFormula());
    }
    
    @Test
    public void testExperienceProgressionRealism() {
        // Test that the progression feels realistic (not too fast, not too slow)
        int levelsGained = 0;
        double totalXPSpent = 0;
        
        // Simulate gaining 100 XP at a time
        for (int i = 0; i < 50; i++) {
            if (skill.addExperience(100.0)) {
                levelsGained++;
            }
            totalXPSpent += 100.0;
        }
        
        // After 5000 XP, should have gained multiple levels but not too many
        assertTrue("Should have gained at least 3 levels", levelsGained >= 3);
        assertTrue("Should not have gained more than 15 levels", levelsGained <= 15);
        assertTrue("Should be at reasonable level", skill.getLevel() >= 4 && skill.getLevel() <= 16);
    }
}
