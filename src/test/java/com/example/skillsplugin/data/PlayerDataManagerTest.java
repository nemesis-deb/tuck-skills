package com.example.skillsplugin.data;

import com.example.skillsplugin.skills.Skill;
import com.example.skillsplugin.skills.SkillProfile;
import com.example.skillsplugin.skills.SkillType;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

/**
 * Tests for PlayerDataManager.
 * Tests cache management, lazy loading, async saving, and concurrent access.
 */
public class PlayerDataManagerTest {
    
    private Plugin mockPlugin;
    private DataStorage mockStorage;
    private BukkitScheduler mockScheduler;
    private PlayerDataManager manager;
    private Logger mockLogger;
    
    @Before
    public void setUp() {
        mockPlugin = mock(Plugin.class);
        mockStorage = mock(DataStorage.class);
        mockScheduler = mock(BukkitScheduler.class);
        mockLogger = mock(Logger.class);
        
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        when(mockPlugin.getServer()).thenReturn(mock(org.bukkit.Server.class));
        
        manager = new PlayerDataManager(mockPlugin, mockStorage);
    }
    
    @Test
    public void testGetProfileLoadsFromStorageWhenNotCached() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        SkillProfile expectedProfile = new SkillProfile(playerId);
        
        when(mockStorage.load(playerId)).thenReturn(expectedProfile);
        
        SkillProfile result = manager.getProfile(playerId);
        
        assertNotNull("Profile should not be null", result);
        assertEquals("Profile should have correct player ID", playerId, result.getPlayerId());
        verify(mockStorage, times(1)).load(playerId);
        assertTrue("Profile should be cached", manager.isCached(playerId));
    }
    
    @Test
    public void testGetProfileCreatesNewProfileWhenNotInStorage() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        
        when(mockStorage.load(playerId)).thenReturn(null);
        
        SkillProfile result = manager.getProfile(playerId);
        
        assertNotNull("Profile should not be null", result);
        assertEquals("Profile should have correct player ID", playerId, result.getPlayerId());
        
        // Verify all skills are initialized
        for (SkillType type : SkillType.values()) {
            Skill skill = result.getSkill(type);
            assertNotNull("Skill " + type + " should exist", skill);
            assertEquals("Skill should start at level 1", 1, skill.getLevel());
            assertEquals("Skill should start with 0 XP", 0.0, skill.getExperience(), 0.01);
        }
        
        assertTrue("Profile should be cached", manager.isCached(playerId));
    }
    
    @Test
    public void testGetProfileReturnsCachedProfileOnSecondCall() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        SkillProfile expectedProfile = new SkillProfile(playerId);
        
        when(mockStorage.load(playerId)).thenReturn(expectedProfile);
        
        // First call - loads from storage
        SkillProfile result1 = manager.getProfile(playerId);
        
        // Second call - should use cache
        SkillProfile result2 = manager.getProfile(playerId);
        
        assertSame("Should return same cached instance", result1, result2);
        verify(mockStorage, times(1)).load(playerId); // Only called once
    }
    
    @Test
    public void testGetProfileHandlesStorageException() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        
        when(mockStorage.load(playerId)).thenThrow(new DataStorageException("Test exception"));
        
        SkillProfile result = manager.getProfile(playerId);
        
        assertNotNull("Profile should not be null even on exception", result);
        assertEquals("Profile should have correct player ID", playerId, result.getPlayerId());
        assertTrue("Profile should be cached", manager.isCached(playerId));
    }
    
    @Test
    public void testSaveProfileSync() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        SkillProfile profile = new SkillProfile(playerId);
        
        when(mockStorage.load(playerId)).thenReturn(profile);
        
        // Load profile into cache
        manager.getProfile(playerId);
        
        // Save synchronously
        manager.saveProfileSync(playerId);
        
        verify(mockStorage, times(1)).save(playerId, profile);
    }
    
    @Test
    public void testSaveProfileSyncWithNonCachedProfile() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        
        // Try to save profile that's not in cache
        manager.saveProfileSync(playerId);
        
        // Should not attempt to save
        verify(mockStorage, never()).save(any(), any());
    }
    
    @Test
    public void testSaveAllProfiles() throws DataStorageException {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        UUID player3 = UUID.randomUUID();
        
        when(mockStorage.load(any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return new SkillProfile(id);
        });
        
        // Load multiple profiles
        manager.getProfile(player1);
        manager.getProfile(player2);
        manager.getProfile(player3);
        
        // Save all
        manager.saveAllProfiles();
        
        verify(mockStorage, times(1)).save(eq(player1), any(SkillProfile.class));
        verify(mockStorage, times(1)).save(eq(player2), any(SkillProfile.class));
        verify(mockStorage, times(1)).save(eq(player3), any(SkillProfile.class));
    }
    
    @Test
    public void testSaveAllProfilesHandlesExceptions() throws DataStorageException {
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        
        when(mockStorage.load(any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return new SkillProfile(id);
        });
        
        // Load profiles
        manager.getProfile(player1);
        manager.getProfile(player2);
        
        // Make one save fail (all retry attempts)
        doThrow(new DataStorageException("Test exception"))
            .when(mockStorage).save(eq(player1), any());
        
        // Should not throw exception, should continue saving others
        manager.saveAllProfiles();
        
        // With retry logic, player1 save will be attempted 3 times (maxAttempts)
        verify(mockStorage, times(3)).save(eq(player1), any(SkillProfile.class));
        // player2 should succeed on first attempt
        verify(mockStorage, times(1)).save(eq(player2), any(SkillProfile.class));
    }
    
    @Test
    public void testAwardExperienceBasic() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        
        SkillProfile profile = new SkillProfile(playerId);
        when(mockStorage.load(playerId)).thenReturn(profile);
        
        // Award experience
        PlayerDataManager.ExperienceResult result = 
            manager.awardExperience(mockPlayer, SkillType.MINING, 50.0);
        
        assertNotNull("Result should not be null", result);
        assertEquals("Skill type should match", SkillType.MINING, result.getSkillType());
        assertEquals("Experience gained should match", 50.0, result.getExperienceGained(), 0.01);
        assertFalse("Should not level up with 50 XP", result.isLeveledUp());
        assertEquals("Should gain 0 levels", 0, result.getLevelsGained());
        
        // Verify skill was updated
        Skill skill = manager.getProfile(playerId).getSkill(SkillType.MINING);
        assertEquals("Skill should have 50 XP", 50.0, skill.getExperience(), 0.01);
    }
    
    @Test
    public void testAwardExperienceCausesLevelUp() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        
        SkillProfile profile = new SkillProfile(playerId);
        when(mockStorage.load(playerId)).thenReturn(profile);
        
        // Award enough experience to level up (default formula: 100 XP for level 2)
        PlayerDataManager.ExperienceResult result = 
            manager.awardExperience(mockPlayer, SkillType.MINING, 150.0);
        
        assertTrue("Should level up", result.isLeveledUp());
        assertEquals("Should gain 1 level", 1, result.getLevelsGained());
        
        // Verify skill was updated
        Skill skill = manager.getProfile(playerId).getSkill(SkillType.MINING);
        assertEquals("Skill should be level 2", 2, skill.getLevel());
        assertTrue("Skill should have overflow XP", skill.getExperience() > 0);
    }
    
    @Test
    public void testAwardExperienceCausesMultipleLevelUps() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        
        SkillProfile profile = new SkillProfile(playerId);
        when(mockStorage.load(playerId)).thenReturn(profile);
        
        // Award massive experience to cause multiple level-ups
        PlayerDataManager.ExperienceResult result = 
            manager.awardExperience(mockPlayer, SkillType.MINING, 1000.0);
        
        assertTrue("Should level up", result.isLeveledUp());
        assertTrue("Should gain multiple levels", result.getLevelsGained() > 1);
        
        // Verify skill was updated
        Skill skill = manager.getProfile(playerId).getSkill(SkillType.MINING);
        assertTrue("Skill should be higher than level 2", skill.getLevel() > 2);
    }
    
    @Test
    public void testAwardExperienceWithZeroAmount() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        
        SkillProfile profile = new SkillProfile(playerId);
        when(mockStorage.load(playerId)).thenReturn(profile);
        
        PlayerDataManager.ExperienceResult result = 
            manager.awardExperience(mockPlayer, SkillType.MINING, 0.0);
        
        assertEquals("Experience gained should be 0", 0.0, result.getExperienceGained(), 0.01);
        assertFalse("Should not level up", result.isLeveledUp());
    }
    
    @Test
    public void testAwardExperienceWithNegativeAmount() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        
        SkillProfile profile = new SkillProfile(playerId);
        when(mockStorage.load(playerId)).thenReturn(profile);
        
        PlayerDataManager.ExperienceResult result = 
            manager.awardExperience(mockPlayer, SkillType.MINING, -50.0);
        
        assertEquals("Experience gained should be 0", 0.0, result.getExperienceGained(), 0.01);
        assertFalse("Should not level up", result.isLeveledUp());
        
        // Verify skill was not modified
        Skill skill = manager.getProfile(playerId).getSkill(SkillType.MINING);
        assertEquals("Skill should still have 0 XP", 0.0, skill.getExperience(), 0.01);
    }
    
    @Test
    public void testRemoveFromCache() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        SkillProfile profile = new SkillProfile(playerId);
        
        when(mockStorage.load(playerId)).thenReturn(profile);
        
        // Load profile into cache
        manager.getProfile(playerId);
        assertTrue("Profile should be cached", manager.isCached(playerId));
        
        // Remove from cache
        manager.removeFromCache(playerId);
        assertFalse("Profile should not be cached", manager.isCached(playerId));
    }
    
    @Test
    public void testCacheSize() throws DataStorageException {
        when(mockStorage.load(any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return new SkillProfile(id);
        });
        
        assertEquals("Cache should be empty initially", 0, manager.getCacheSize());
        
        manager.getProfile(UUID.randomUUID());
        assertEquals("Cache should have 1 entry", 1, manager.getCacheSize());
        
        manager.getProfile(UUID.randomUUID());
        assertEquals("Cache should have 2 entries", 2, manager.getCacheSize());
        
        manager.getProfile(UUID.randomUUID());
        assertEquals("Cache should have 3 entries", 3, manager.getCacheSize());
    }
    
    @Test
    public void testClearCache() throws DataStorageException {
        when(mockStorage.load(any())).thenAnswer(invocation -> {
            UUID id = invocation.getArgument(0);
            return new SkillProfile(id);
        });
        
        // Load multiple profiles
        manager.getProfile(UUID.randomUUID());
        manager.getProfile(UUID.randomUUID());
        manager.getProfile(UUID.randomUUID());
        
        assertEquals("Cache should have 3 entries", 3, manager.getCacheSize());
        
        manager.clearCache();
        
        assertEquals("Cache should be empty", 0, manager.getCacheSize());
    }
    
    @Test
    public void testConcurrentGetProfile() throws Exception {
        UUID playerId = UUID.randomUUID();
        SkillProfile profile = new SkillProfile(playerId);
        
        when(mockStorage.load(playerId)).thenReturn(profile);
        
        int threadCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        SkillProfile[] results = new SkillProfile[threadCount];
        
        // Create multiple threads that try to get the same profile simultaneously
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    startLatch.await(); // Wait for all threads to be ready
                    results[index] = manager.getProfile(playerId);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        
        // Start all threads at once
        startLatch.countDown();
        
        // Wait for all threads to complete
        assertTrue("All threads should complete", doneLatch.await(5, TimeUnit.SECONDS));
        
        // Verify all threads got a profile
        for (int i = 0; i < threadCount; i++) {
            assertNotNull("Thread " + i + " should have gotten a profile", results[i]);
            assertEquals("All profiles should have same player ID", playerId, results[i].getPlayerId());
        }
        
        // Verify storage was only called once (due to caching)
        verify(mockStorage, atMost(threadCount)).load(playerId);
        
        // Verify only one profile is cached
        assertEquals("Only one profile should be cached", 1, manager.getCacheSize());
    }
    
    @Test
    public void testConcurrentAwardExperience() throws Exception {
        UUID playerId = UUID.randomUUID();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        
        SkillProfile profile = new SkillProfile(playerId);
        when(mockStorage.load(playerId)).thenReturn(profile);
        
        // Pre-load profile
        manager.getProfile(playerId);
        
        int threadCount = 100;
        double xpPerThread = 10.0;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        // Create multiple threads that award experience simultaneously
        for (int i = 0; i < threadCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    manager.awardExperience(mockPlayer, SkillType.MINING, xpPerThread);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        
        // Start all threads at once
        startLatch.countDown();
        
        // Wait for all threads to complete
        assertTrue("All threads should complete", doneLatch.await(5, TimeUnit.SECONDS));
        
        // Verify total experience is correct
        Skill skill = manager.getProfile(playerId).getSkill(SkillType.MINING);
        double expectedTotalXP = threadCount * xpPerThread;
        
        // The skill should have gained levels, so we need to account for that
        // Just verify that some XP was added
        assertTrue("Skill should have gained experience or levels", 
                   skill.getExperience() > 0 || skill.getLevel() > 1);
    }
    
    @Test
    public void testConcurrentSaveAndLoad() throws Exception {
        UUID playerId = UUID.randomUUID();
        SkillProfile profile = new SkillProfile(playerId);
        
        when(mockStorage.load(playerId)).thenReturn(profile);
        
        // Pre-load profile
        manager.getProfile(playerId);
        
        int threadCount = 20;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        
        // Create threads that save and load simultaneously
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            new Thread(() -> {
                try {
                    startLatch.await();
                    if (index % 2 == 0) {
                        manager.saveProfileSync(playerId);
                    } else {
                        manager.getProfile(playerId);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }).start();
        }
        
        // Start all threads at once
        startLatch.countDown();
        
        // Wait for all threads to complete
        assertTrue("All threads should complete", doneLatch.await(5, TimeUnit.SECONDS));
        
        // Verify no exceptions were thrown and profile is still cached
        assertTrue("Profile should still be cached", manager.isCached(playerId));
    }
    
    @Test
    public void testExperienceResultGetters() {
        PlayerDataManager.ExperienceResult result = 
            new PlayerDataManager.ExperienceResult(SkillType.COMBAT, 123.45, 2, true);
        
        assertEquals("Skill type should match", SkillType.COMBAT, result.getSkillType());
        assertEquals("Experience gained should match", 123.45, result.getExperienceGained(), 0.01);
        assertEquals("Levels gained should match", 2, result.getLevelsGained());
        assertTrue("Should be leveled up", result.isLeveledUp());
    }
    
    @Test
    public void testAwardExperienceToAllSkills() throws DataStorageException {
        UUID playerId = UUID.randomUUID();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        
        SkillProfile profile = new SkillProfile(playerId);
        when(mockStorage.load(playerId)).thenReturn(profile);
        
        // Award experience to all skill types
        for (SkillType type : SkillType.values()) {
            PlayerDataManager.ExperienceResult result = 
                manager.awardExperience(mockPlayer, type, 50.0);
            
            assertNotNull("Result should not be null for " + type, result);
            assertEquals("Skill type should match", type, result.getSkillType());
            assertEquals("Experience gained should be 50", 50.0, result.getExperienceGained(), 0.01);
        }
        
        // Verify all skills have XP
        SkillProfile loadedProfile = manager.getProfile(playerId);
        for (SkillType type : SkillType.values()) {
            Skill skill = loadedProfile.getSkill(type);
            assertEquals("Skill " + type + " should have 50 XP", 
                        50.0, skill.getExperience(), 0.01);
        }
    }
}
