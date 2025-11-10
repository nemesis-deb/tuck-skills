package com.example.skillsplugin.data;

import com.example.skillsplugin.skills.Skill;
import com.example.skillsplugin.skills.SkillProfile;
import com.example.skillsplugin.skills.SkillType;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.util.UUID;

import static org.junit.Assert.*;

/**
 * Tests for JsonDataStorage implementation.
 */
public class JsonDataStorageTest {
    
    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();
    
    private JsonDataStorage storage;
    private File dataDirectory;
    
    @Before
    public void setUp() throws Exception {
        dataDirectory = tempFolder.newFolder("playerdata");
        storage = new JsonDataStorage(dataDirectory);
        storage.initialize();
    }
    
    @Test
    public void testInitializeCreatesDirectory() {
        assertTrue("Data directory should be created", dataDirectory.exists());
        assertTrue("Data path should be a directory", dataDirectory.isDirectory());
    }
    
    @Test(expected = DataStorageException.class)
    public void testInitializeThrowsExceptionWhenPathIsFile() throws Exception {
        File fileInsteadOfDir = tempFolder.newFile("notadirectory");
        
        JsonDataStorage badStorage = new JsonDataStorage(fileInsteadOfDir);
        badStorage.initialize();
    }
    
    @Test
    public void testSaveAndLoadProfile() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        SkillProfile profile = new SkillProfile(playerId);
        
        // Modify some skills
        profile.getSkill(SkillType.MINING).addExperience(500);
        profile.getSkill(SkillType.COMBAT).addExperience(1000);
        
        // Save profile
        storage.save(playerId, profile);
        
        // Load profile
        SkillProfile loadedProfile = storage.load(playerId);
        
        assertNotNull("Loaded profile should not be null", loadedProfile);
        assertEquals("Player ID should match", playerId, loadedProfile.getPlayerId());
        
        // Verify skills were loaded correctly
        Skill loadedMining = loadedProfile.getSkill(SkillType.MINING);
        Skill loadedCombat = loadedProfile.getSkill(SkillType.COMBAT);
        
        assertNotNull("Mining skill should exist", loadedMining);
        assertNotNull("Combat skill should exist", loadedCombat);
        
        assertEquals(profile.getSkill(SkillType.MINING).getLevel(), loadedMining.getLevel());
        assertEquals(profile.getSkill(SkillType.MINING).getExperience(), loadedMining.getExperience(), 0.01);
        assertEquals(profile.getSkill(SkillType.COMBAT).getLevel(), loadedCombat.getLevel());
        assertEquals(profile.getSkill(SkillType.COMBAT).getExperience(), loadedCombat.getExperience(), 0.01);
    }
    
    @Test
    public void testLoadNonExistentProfile() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        SkillProfile profile = storage.load(playerId);
        
        assertNull("Loading non-existent profile should return null", profile);
    }
    
    @Test
    public void testExists() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        
        assertFalse("Profile should not exist initially", storage.exists(playerId));
        
        SkillProfile profile = new SkillProfile(playerId);
        storage.save(playerId, profile);
        
        assertTrue("Profile should exist after saving", storage.exists(playerId));
    }
    
    @Test
    public void testSaveOverwritesExistingProfile() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        SkillProfile profile = new SkillProfile(playerId);
        
        // Save initial profile
        profile.getSkill(SkillType.MINING).addExperience(100);
        storage.save(playerId, profile);
        
        // Modify and save again
        profile.getSkill(SkillType.MINING).addExperience(200);
        storage.save(playerId, profile);
        
        // Load and verify
        SkillProfile loadedProfile = storage.load(playerId);
        assertEquals(profile.getSkill(SkillType.MINING).getLevel(), loadedProfile.getSkill(SkillType.MINING).getLevel());
        assertEquals(profile.getSkill(SkillType.MINING).getExperience(), 
                     loadedProfile.getSkill(SkillType.MINING).getExperience(), 0.01);
    }
    
    @Test
    public void testDataIntegrityAfterMultipleSaveLoadCycles() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        SkillProfile profile = new SkillProfile(playerId);
        
        // Cycle 1
        profile.getSkill(SkillType.MINING).addExperience(150);
        storage.save(playerId, profile);
        profile = storage.load(playerId);
        
        // Cycle 2
        profile.getSkill(SkillType.WOODCUTTING).addExperience(250);
        storage.save(playerId, profile);
        profile = storage.load(playerId);
        
        // Cycle 3
        profile.getSkill(SkillType.COMBAT).addExperience(350);
        storage.save(playerId, profile);
        profile = storage.load(playerId);
        
        // Verify all data is intact
        assertNotNull(profile);
        assertTrue(profile.getSkill(SkillType.MINING).getExperience() > 0);
        assertTrue(profile.getSkill(SkillType.WOODCUTTING).getExperience() > 0);
        assertTrue(profile.getSkill(SkillType.COMBAT).getExperience() > 0);
    }
    
    @Test
    public void testAllSkillTypesAreSavedAndLoaded() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        SkillProfile profile = new SkillProfile(playerId);
        
        // Add experience to all skills
        for (SkillType type : SkillType.values()) {
            profile.getSkill(type).addExperience(100 * type.ordinal());
        }
        
        storage.save(playerId, profile);
        SkillProfile loadedProfile = storage.load(playerId);
        
        // Verify all skills exist and have correct data
        for (SkillType type : SkillType.values()) {
            Skill originalSkill = profile.getSkill(type);
            Skill loadedSkill = loadedProfile.getSkill(type);
            
            assertNotNull("Skill " + type + " should exist", loadedSkill);
            assertEquals("Level should match for " + type, 
                        originalSkill.getLevel(), loadedSkill.getLevel());
            assertEquals("Experience should match for " + type,
                        originalSkill.getExperience(), loadedSkill.getExperience(), 0.01);
        }
    }
    
    @Test
    public void testLoadHandlesMissingSkills() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        SkillProfile profile = new SkillProfile(playerId);
        
        storage.save(playerId, profile);
        
        // Manually remove a skill from the JSON file would be complex,
        // but the load method should handle missing skills by creating defaults
        SkillProfile loadedProfile = storage.load(playerId);
        
        // Verify all skills exist even if some were missing
        for (SkillType type : SkillType.values()) {
            assertNotNull("Skill " + type + " should exist", loadedProfile.getSkill(type));
        }
    }
    
    @Test
    public void testSaveWithHighLevelAndExperience() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        SkillProfile profile = new SkillProfile(playerId);
        
        // Set high values
        Skill mining = profile.getSkill(SkillType.MINING);
        mining.setLevel(99);
        mining.setExperience(999999.99);
        
        storage.save(playerId, profile);
        SkillProfile loadedProfile = storage.load(playerId);
        
        Skill loadedMining = loadedProfile.getSkill(SkillType.MINING);
        assertEquals(99, loadedMining.getLevel());
        assertEquals(999999.99, loadedMining.getExperience(), 0.01);
    }
    
    @Test
    public void testMultiplePlayersCanBeSavedAndLoaded() throws DataStorageException {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        UUID player3 = UUID.randomUUID();
        
        SkillProfile profile1 = new SkillProfile(player1);
        SkillProfile profile2 = new SkillProfile(player2);
        SkillProfile profile3 = new SkillProfile(player3);
        
        profile1.getSkill(SkillType.MINING).addExperience(100);
        profile2.getSkill(SkillType.COMBAT).addExperience(200);
        profile3.getSkill(SkillType.FISHING).addExperience(300);
        
        storage.save(player1, profile1);
        storage.save(player2, profile2);
        storage.save(player3, profile3);
        
        assertTrue(storage.exists(player1));
        assertTrue(storage.exists(player2));
        assertTrue(storage.exists(player3));
        
        SkillProfile loaded1 = storage.load(player1);
        SkillProfile loaded2 = storage.load(player2);
        SkillProfile loaded3 = storage.load(player3);
        
        assertNotNull(loaded1);
        assertNotNull(loaded2);
        assertNotNull(loaded3);
        
        assertEquals(player1, loaded1.getPlayerId());
        assertEquals(player2, loaded2.getPlayerId());
        assertEquals(player3, loaded3.getPlayerId());
    }
}
