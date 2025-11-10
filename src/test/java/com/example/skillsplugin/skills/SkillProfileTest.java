package com.example.skillsplugin.skills;

import org.junit.Before;
import org.junit.Test;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import static org.junit.Assert.*;

/**
 * Unit tests for the SkillProfile class.
 * Tests player skill profile creation and management.
 */
public class SkillProfileTest {
    
    private UUID playerId;
    private SkillProfile profile;
    
    @Before
    public void setUp() {
        playerId = UUID.randomUUID();
        profile = new SkillProfile(playerId);
    }
    
    @Test
    public void testProfileInitialization() {
        assertEquals("Profile should have correct player ID", playerId, profile.getPlayerId());
        assertNotNull("Profile should have skills map", profile.getSkills());
        assertEquals("Profile should have 7 skills", 7, profile.getSkills().size());
    }
    
    @Test
    public void testAllSkillTypesInitialized() {
        // Verify all 7 skill types are present
        for (SkillType type : SkillType.values()) {
            Skill skill = profile.getSkill(type);
            assertNotNull("Skill " + type + " should be initialized", skill);
            assertEquals("Skill " + type + " should start at level 1", 1, skill.getLevel());
            assertEquals("Skill " + type + " should start with 0 XP", 0.0, skill.getExperience(), 0.001);
        }
    }
    
    @Test
    public void testGetSpecificSkill() {
        Skill miningSkill = profile.getSkill(SkillType.MINING);
        assertNotNull("Mining skill should exist", miningSkill);
        assertEquals("Mining skill should be MINING type", SkillType.MINING, miningSkill.getType());
        
        Skill combatSkill = profile.getSkill(SkillType.COMBAT);
        assertNotNull("Combat skill should exist", combatSkill);
        assertEquals("Combat skill should be COMBAT type", SkillType.COMBAT, combatSkill.getType());
    }
    
    @Test
    public void testGetSkillsReturnsAllSkills() {
        Map<SkillType, Skill> skills = profile.getSkills();
        
        assertEquals("Should have 7 skills", 7, skills.size());
        assertTrue("Should contain MINING", skills.containsKey(SkillType.MINING));
        assertTrue("Should contain WOODCUTTING", skills.containsKey(SkillType.WOODCUTTING));
        assertTrue("Should contain COMBAT", skills.containsKey(SkillType.COMBAT));
        assertTrue("Should contain FARMING", skills.containsKey(SkillType.FARMING));
        assertTrue("Should contain FISHING", skills.containsKey(SkillType.FISHING));
        assertTrue("Should contain ENCHANTING", skills.containsKey(SkillType.ENCHANTING));
        assertTrue("Should contain TRADING", skills.containsKey(SkillType.TRADING));
    }
    
    @Test
    public void testProfileWithExistingSkills() {
        // Create a profile with pre-existing skills (simulating loading from storage)
        Map<SkillType, Skill> existingSkills = new HashMap<>();
        existingSkills.put(SkillType.MINING, new Skill(SkillType.MINING, 10, 500.0));
        existingSkills.put(SkillType.COMBAT, new Skill(SkillType.COMBAT, 5, 200.0));
        
        SkillProfile loadedProfile = new SkillProfile(playerId, existingSkills);
        
        assertEquals("Profile should have correct player ID", playerId, loadedProfile.getPlayerId());
        assertEquals("Profile should have 2 skills", 2, loadedProfile.getSkills().size());
        
        Skill miningSkill = loadedProfile.getSkill(SkillType.MINING);
        assertEquals("Mining skill should be level 10", 10, miningSkill.getLevel());
        assertEquals("Mining skill should have 500 XP", 500.0, miningSkill.getExperience(), 0.001);
        
        Skill combatSkill = loadedProfile.getSkill(SkillType.COMBAT);
        assertEquals("Combat skill should be level 5", 5, combatSkill.getLevel());
        assertEquals("Combat skill should have 200 XP", 200.0, combatSkill.getExperience(), 0.001);
    }
    
    @Test
    public void testAddSkill() {
        // Create a new profile with existing skills
        Map<SkillType, Skill> existingSkills = new HashMap<>();
        SkillProfile loadedProfile = new SkillProfile(playerId, existingSkills);
        
        assertEquals("Profile should start with 0 skills", 0, loadedProfile.getSkills().size());
        
        // Add a skill
        Skill fishingSkill = new Skill(SkillType.FISHING, 3, 150.0);
        loadedProfile.addSkill(fishingSkill);
        
        assertEquals("Profile should now have 1 skill", 1, loadedProfile.getSkills().size());
        
        Skill retrievedSkill = loadedProfile.getSkill(SkillType.FISHING);
        assertNotNull("Fishing skill should exist", retrievedSkill);
        assertEquals("Fishing skill should be level 3", 3, retrievedSkill.getLevel());
        assertEquals("Fishing skill should have 150 XP", 150.0, retrievedSkill.getExperience(), 0.001);
    }
    
    @Test
    public void testSkillModificationPersistsInProfile() {
        // Get a skill and modify it
        Skill miningSkill = profile.getSkill(SkillType.MINING);
        miningSkill.addExperience(250.0);
        
        // Retrieve the same skill again and verify changes persisted
        Skill retrievedSkill = profile.getSkill(SkillType.MINING);
        assertTrue("Mining skill should have leveled up", retrievedSkill.getLevel() > 1);
        assertTrue("Mining skill should have experience", retrievedSkill.getExperience() > 0);
    }
    
    @Test
    public void testMultiplePlayersHaveSeparateProfiles() {
        UUID player1Id = UUID.randomUUID();
        UUID player2Id = UUID.randomUUID();
        
        SkillProfile profile1 = new SkillProfile(player1Id);
        SkillProfile profile2 = new SkillProfile(player2Id);
        
        // Modify player 1's mining skill
        profile1.getSkill(SkillType.MINING).addExperience(200.0);
        
        // Verify player 2's mining skill is unaffected
        assertEquals("Player 2's mining skill should still be level 1", 
                     1, profile2.getSkill(SkillType.MINING).getLevel());
        assertEquals("Player 2's mining skill should still have 0 XP", 
                     0.0, profile2.getSkill(SkillType.MINING).getExperience(), 0.001);
    }
}
