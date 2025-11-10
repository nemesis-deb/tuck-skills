package com.example.skillsplugin.integration;

import com.example.skillsplugin.SkillsPlugin;
import com.example.skillsplugin.config.ConfigManager;
import com.example.skillsplugin.data.DataStorage;
import com.example.skillsplugin.data.DataStorageException;
import com.example.skillsplugin.data.JsonDataStorage;
import com.example.skillsplugin.data.PlayerDataManager;
import com.example.skillsplugin.events.SkillEventListener;
import com.example.skillsplugin.skills.*;
import com.example.skillsplugin.ui.UIManager;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.block.Block;
import org.bukkit.command.PluginCommand;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Zombie;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.PluginManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Comprehensive integration tests for the Skills Plugin.
 * Tests complete workflows including XP gain, data persistence, bonuses, concurrency, and config reload.
 */
public class SkillsPluginIntegrationTest {
    
    private SkillsPlugin mockPlugin;
    private File tempDataFolder;
    private JsonDataStorage storage;
    private PlayerDataManager playerDataManager;
    private ConfigManager configManager;
    private ExperienceCalculator experienceCalculator;
    private LevelFormula levelFormula;
    private BonusManager bonusManager;
    private UIManager uiManager;
    private SkillEventListener eventListener;
    
    @Before
    public void setUp() throws IOException, DataStorageException {
        // Create temporary directory for test data
        tempDataFolder = Files.createTempDirectory("skillsplugin-test").toFile();
        
        // Set up mock plugin
        mockPlugin = mock(SkillsPlugin.class);
        Logger mockLogger = mock(Logger.class);
        Server mockServer = mock(Server.class);
        PluginManager mockPluginManager = mock(PluginManager.class);
        FileConfiguration mockConfig = new YamlConfiguration();
        
        when(mockPlugin.getLogger()).thenReturn(mockLogger);
        when(mockPlugin.getDataFolder()).thenReturn(tempDataFolder);
        when(mockPlugin.getServer()).thenReturn(mockServer);
        when(mockServer.getPluginManager()).thenReturn(mockPluginManager);
        when(mockPlugin.getConfig()).thenReturn(mockConfig);
        
        // Initialize real components
        // Note: ConfigManager.loadConfig() calls final methods, so we initialize it but don't call loadConfig
        configManager = new ConfigManager(mockPlugin);
        
        File dataDirectory = new File(tempDataFolder, "playerdata");
        storage = new JsonDataStorage(dataDirectory);
        storage.initialize();
        
        playerDataManager = new PlayerDataManager(mockPlugin, storage);
        levelFormula = new LevelFormula(configManager);
        experienceCalculator = new ExperienceCalculator(configManager);
        bonusManager = new BonusManager(playerDataManager, configManager);
        uiManager = mock(UIManager.class); // Mock UI for verification
        
        eventListener = new SkillEventListener(
            playerDataManager,
            experienceCalculator,
            uiManager,
            bonusManager,
            mockLogger
        );
    }
    
    @After
    public void tearDown() {
        // Clean up temporary files
        deleteDirectory(tempDataFolder);
    }
    
    private void deleteDirectory(File directory) {
        if (directory.exists()) {
            File[] files = directory.listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.isDirectory()) {
                        deleteDirectory(file);
                    } else {
                        file.delete();
                    }
                }
            }
            directory.delete();
        }
    }
    
    // ========== Test 1: Complete XP Gain Flow from Event to Level-Up ==========
    
    @Test
    public void testCompleteXPGainFlowWithoutLevelUp() {
        // Arrange
        Player mockPlayer = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        
        Block mockBlock = mock(Block.class);
        when(mockBlock.getType()).thenReturn(Material.DIAMOND_ORE);
        BlockBreakEvent event = new BlockBreakEvent(mockBlock, mockPlayer);
        
        // Act
        eventListener.onBlockBreak(event);
        
        // Assert
        SkillProfile profile = playerDataManager.getProfile(playerId);
        assertNotNull("Profile should be created", profile);
        
        Skill miningSkill = profile.getSkill(SkillType.MINING);
        assertTrue("Mining skill should have gained XP", miningSkill.getExperience() > 0);
        assertEquals("Mining skill should still be level 1", 1, miningSkill.getLevel());
        
        verify(uiManager).sendXPGainMessage(eq(mockPlayer), eq(SkillType.MINING), anyDouble());
        verify(uiManager, never()).showLevelUpBossBar(any(), any(), anyInt());
    }
    
    @Test
    public void testCompleteXPGainFlowWithLevelUp() {
        // Arrange
        Player mockPlayer = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        
        // Give player XP close to level-up
        SkillProfile profile = playerDataManager.getProfile(playerId);
        Skill miningSkill = profile.getSkill(SkillType.MINING);
        miningSkill.addExperience(90.0); // Close to 100 XP needed for level 2
        
        Block mockBlock = mock(Block.class);
        when(mockBlock.getType()).thenReturn(Material.DIAMOND_ORE);
        BlockBreakEvent event = new BlockBreakEvent(mockBlock, mockPlayer);
        
        // Act
        eventListener.onBlockBreak(event);
        
        // Assert
        assertEquals("Mining skill should be level 2", 2, miningSkill.getLevel());
        assertTrue("Mining skill should have overflow XP", miningSkill.getExperience() > 0);
        
        verify(uiManager).sendXPGainMessage(eq(mockPlayer), eq(SkillType.MINING), anyDouble());
        verify(uiManager).showLevelUpBossBar(mockPlayer, SkillType.MINING, 2);
    }
    
    @Test
    public void testXPGainForAllSkillTypes() {
        // Test that all skill types can gain XP
        Player mockPlayer = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        
        SkillProfile profile = playerDataManager.getProfile(playerId);
        
        // Mining
        Block stoneBlock = mock(Block.class);
        when(stoneBlock.getType()).thenReturn(Material.STONE);
        eventListener.onBlockBreak(new BlockBreakEvent(stoneBlock, mockPlayer));
        assertTrue("Mining should have XP", profile.getSkill(SkillType.MINING).getExperience() > 0);
        
        // Woodcutting
        Block logBlock = mock(Block.class);
        when(logBlock.getType()).thenReturn(Material.OAK_LOG);
        eventListener.onBlockBreak(new BlockBreakEvent(logBlock, mockPlayer));
        assertTrue("Woodcutting should have XP", profile.getSkill(SkillType.WOODCUTTING).getExperience() > 0);
        
        // Combat
        Zombie zombie = mock(Zombie.class);
        when(zombie.getKiller()).thenReturn(mockPlayer);
        when(zombie.getType()).thenReturn(EntityType.ZOMBIE);
        eventListener.onEntityDeath(new EntityDeathEvent(zombie, null));
        assertTrue("Combat should have XP", profile.getSkill(SkillType.COMBAT).getExperience() > 0);
    }
    
    // ========== Test 2: Data Persistence Across Save/Load Cycles ==========
    
    @Test
    public void testBasicSaveAndLoad() throws DataStorageException {
        // Arrange
        UUID playerId = UUID.randomUUID();
        SkillProfile originalProfile = new SkillProfile(playerId);
        originalProfile.getSkill(SkillType.MINING).addExperience(150.0);
        originalProfile.getSkill(SkillType.COMBAT).addExperience(75.0);
        
        // Act
        storage.save(playerId, originalProfile);
        SkillProfile loadedProfile = storage.load(playerId);
        
        // Assert
        assertNotNull("Loaded profile should not be null", loadedProfile);
        assertEquals("Player ID should match", playerId, loadedProfile.getPlayerId());
        
        Skill miningSkill = loadedProfile.getSkill(SkillType.MINING);
        assertEquals("Mining level should match",
                     originalProfile.getSkill(SkillType.MINING).getLevel(),
                     miningSkill.getLevel());
        assertEquals("Mining XP should match",
                     originalProfile.getSkill(SkillType.MINING).getExperience(),
                     miningSkill.getExperience(), 0.01);
    }
    
    @Test
    public void testPlayerDataManagerCacheAndPersistence() throws DataStorageException {
        // Arrange
        UUID playerId = UUID.randomUUID();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        
        // Act
        SkillProfile profile = playerDataManager.getProfile(playerId);
        playerDataManager.awardExperience(mockPlayer, SkillType.MINING, 150.0);
        playerDataManager.saveProfileSync(playerId);
        
        // Clear cache to force reload from disk
        playerDataManager.removeFromCache(playerId);
        SkillProfile reloadedProfile = playerDataManager.getProfile(playerId);
        
        // Assert
        assertEquals("Mining XP should be persisted", 150.0,
                     reloadedProfile.getSkill(SkillType.MINING).getExperience(), 0.01);
    }
    
    @Test
    public void testMultipleSaveLoadCycles() throws DataStorageException {
        // Test data consistency across multiple cycles
        UUID playerId = UUID.randomUUID();
        SkillProfile profile = new SkillProfile(playerId);
        
        // Cycle 1
        profile.getSkill(SkillType.MINING).addExperience(50.0);
        storage.save(playerId, profile);
        profile = storage.load(playerId);
        assertEquals("Mining XP after cycle 1", 50.0,
                     profile.getSkill(SkillType.MINING).getExperience(), 0.01);
        
        // Cycle 2
        profile.getSkill(SkillType.MINING).addExperience(75.0);
        storage.save(playerId, profile);
        profile = storage.load(playerId);
        assertEquals("Mining XP after cycle 2", 125.0,
                     profile.getSkill(SkillType.MINING).getExperience(), 0.01);
    }
    
    // ========== Test 3: Bonus Application During Events ==========
    
    @Test
    public void testBonusManagerIntegration() {
        // Test that bonus manager correctly calculates bonuses based on skill levels
        Player mockPlayer = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        
        SkillProfile profile = playerDataManager.getProfile(playerId);
        
        // Level up mining to level 10
        Skill miningSkill = profile.getSkill(SkillType.MINING);
        while (miningSkill.getLevel() < 10) {
            miningSkill.addExperience(1000.0);
        }
        
        // Test combat damage multiplier
        Skill combatSkill = profile.getSkill(SkillType.COMBAT);
        while (combatSkill.getLevel() < 20) {
            combatSkill.addExperience(1000.0);
        }
        
        double damageMultiplier = bonusManager.getCombatDamageMultiplier(mockPlayer);
        assertTrue("Combat damage should be boosted", damageMultiplier > 1.0);
        
        // Test enchanting cost reduction
        int reducedCost = bonusManager.getEnchantingCostReduction(mockPlayer, 30);
        assertTrue("Enchanting cost should be reduced", reducedCost < 30);
        assertTrue("Enchanting cost should not be negative", reducedCost > 0);
    }
    
    @Test
    public void testBonusScalesWithLevel() {
        // Test that bonuses scale linearly with level
        Player mockPlayer = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        
        SkillProfile profile = playerDataManager.getProfile(playerId);
        Skill combatSkill = profile.getSkill(SkillType.COMBAT);
        
        double multiplierLevel1 = bonusManager.getCombatDamageMultiplier(mockPlayer);
        
        combatSkill.addExperience(1000.0); // Level up
        double multiplierLevel2 = bonusManager.getCombatDamageMultiplier(mockPlayer);
        
        assertTrue("Damage multiplier should increase with level",
                   multiplierLevel2 > multiplierLevel1);
    }
    
    // ========== Test 4: Multiple Players with Concurrent Actions ==========
    
    @Test
    public void testMultiplePlayersConcurrentXPGains() throws Exception {
        int playerCount = 10;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(playerCount);
        
        List<Player> players = new ArrayList<>();
        List<UUID> playerIds = new ArrayList<>();
        
        // Create mock players
        for (int i = 0; i < playerCount; i++) {
            Player mockPlayer = mock(Player.class);
            UUID playerId = UUID.randomUUID();
            when(mockPlayer.getUniqueId()).thenReturn(playerId);
            players.add(mockPlayer);
            playerIds.add(playerId);
        }
        
        // Create threads for each player to gain XP
        for (int i = 0; i < playerCount; i++) {
            final Player player = players.get(i);
            new Thread(() -> {
                try {
                    startLatch.await();
                    
                    Block mockBlock = mock(Block.class);
                    when(mockBlock.getType()).thenReturn(Material.DIAMOND_ORE);
                    BlockBreakEvent event = new BlockBreakEvent(mockBlock, player);
                    eventListener.onBlockBreak(event);
                    
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
        assertTrue("All threads should complete", doneLatch.await(10, TimeUnit.SECONDS));
        
        // Verify all players gained XP
        for (int i = 0; i < playerCount; i++) {
            SkillProfile profile = playerDataManager.getProfile(playerIds.get(i));
            assertNotNull("Profile should exist for player " + i, profile);
            
            Skill miningSkill = profile.getSkill(SkillType.MINING);
            assertTrue("Player " + i + " should have gained mining XP",
                       miningSkill.getExperience() > 0);
        }
    }
    
    @Test
    public void testSamePlayerConcurrentXPGains() throws Exception {
        // Test that a single player can gain XP from multiple threads safely
        Player mockPlayer = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        
        int actionCount = 50;
        double xpPerAction = 10.0;
        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(actionCount);
        
        // Create multiple threads that award XP to the same player
        for (int i = 0; i < actionCount; i++) {
            new Thread(() -> {
                try {
                    startLatch.await();
                    playerDataManager.awardExperience(mockPlayer, SkillType.MINING, xpPerAction);
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
        assertTrue("All threads should complete", doneLatch.await(10, TimeUnit.SECONDS));
        
        // Verify player gained XP or levels
        SkillProfile profile = playerDataManager.getProfile(playerId);
        Skill miningSkill = profile.getSkill(SkillType.MINING);
        
        assertTrue("Player should have gained levels or XP",
                   miningSkill.getLevel() > 1 || miningSkill.getExperience() > 0);
    }
    
    // ========== Test 5: Config Reload Applying Changes ==========
    
    @Test
    public void testConfigReloadAffectsNewCalculations() {
        // Test that config changes affect subsequent calculations
        FileConfiguration config = mockPlugin.getConfig();
        
        // Set initial multiplier
        config.set("experience.mining", 1.0);
        configManager.reloadConfig();
        
        ExperienceCalculator calculator1 = new ExperienceCalculator(configManager);
        double xp1 = calculator1.calculateMiningXP(Material.DIAMOND_ORE);
        
        // Change multiplier
        config.set("experience.mining", 2.0);
        configManager.reloadConfig();
        
        ExperienceCalculator calculator2 = new ExperienceCalculator(configManager);
        double xp2 = calculator2.calculateMiningXP(Material.DIAMOND_ORE);
        
        // Assert XP is doubled
        assertEquals("XP should be doubled after config reload", xp1 * 2.0, xp2, 0.01);
    }
    
    @Test
    public void testConfigReloadDoesNotAffectExistingData() {
        // Test that reloading config doesn't corrupt existing player data
        Player mockPlayer = mock(Player.class);
        UUID playerId = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        
        // Give player some XP
        playerDataManager.awardExperience(mockPlayer, SkillType.MINING, 150.0);
        playerDataManager.awardExperience(mockPlayer, SkillType.COMBAT, 75.0);
        
        SkillProfile profile = playerDataManager.getProfile(playerId);
        double miningXP = profile.getSkill(SkillType.MINING).getExperience();
        double combatXP = profile.getSkill(SkillType.COMBAT).getExperience();
        
        // Reload config
        FileConfiguration config = mockPlugin.getConfig();
        config.set("experience.mining", 5.0);
        configManager.reloadConfig();
        
        // Verify existing data is unchanged
        assertEquals("Mining XP should be unchanged", miningXP,
                     profile.getSkill(SkillType.MINING).getExperience(), 0.01);
        assertEquals("Combat XP should be unchanged", combatXP,
                     profile.getSkill(SkillType.COMBAT).getExperience(), 0.01);
    }
    
    @Test
    public void testLevelFormulaReload() {
        // Test that level formula changes are applied
        FileConfiguration config = mockPlugin.getConfig();
        
        config.set("leveling.base-xp", 100.0);
        configManager.reloadConfig();
        LevelFormula formula1 = new LevelFormula(configManager);
        double req1 = formula1.getRequiredExperience(1);
        
        config.set("leveling.base-xp", 200.0);
        configManager.reloadConfig();
        LevelFormula formula2 = new LevelFormula(configManager);
        double req2 = formula2.getRequiredExperience(1);
        
        assertEquals("Level requirement should be doubled", req1 * 2.0, req2, 0.01);
    }
    
    // ========== Additional Integration Tests ==========
    
    @Test
    public void testDataIntegrityAfterMultipleOperations() throws DataStorageException {
        // Test complex scenario with multiple operations
        UUID playerId = UUID.randomUUID();
        Player mockPlayer = mock(Player.class);
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        
        // Operation 1: Create profile and add XP
        playerDataManager.awardExperience(mockPlayer, SkillType.MINING, 50.0);
        playerDataManager.awardExperience(mockPlayer, SkillType.COMBAT, 75.0);
        playerDataManager.saveProfileSync(playerId);
        
        // Operation 2: Load, modify, save
        playerDataManager.removeFromCache(playerId);
        playerDataManager.awardExperience(mockPlayer, SkillType.MINING, 100.0);
        playerDataManager.saveProfileSync(playerId);
        
        // Operation 3: Load, modify multiple skills, save
        playerDataManager.removeFromCache(playerId);
        playerDataManager.awardExperience(mockPlayer, SkillType.WOODCUTTING, 60.0);
        playerDataManager.awardExperience(mockPlayer, SkillType.FISHING, 40.0);
        playerDataManager.saveProfileSync(playerId);
        
        // Final verification
        playerDataManager.removeFromCache(playerId);
        SkillProfile finalProfile = playerDataManager.getProfile(playerId);
        
        assertEquals("Mining XP should be cumulative", 150.0,
                     finalProfile.getSkill(SkillType.MINING).getExperience(), 0.01);
        assertEquals("Combat XP should be preserved", 75.0,
                     finalProfile.getSkill(SkillType.COMBAT).getExperience(), 0.01);
        assertEquals("Woodcutting XP should be saved", 60.0,
                     finalProfile.getSkill(SkillType.WOODCUTTING).getExperience(), 0.01);
        assertEquals("Fishing XP should be saved", 40.0,
                     finalProfile.getSkill(SkillType.FISHING).getExperience(), 0.01);
    }
    
    @Test
    public void testSaveAllProfilesIntegration() throws DataStorageException {
        // Test saving multiple cached profiles at once
        UUID player1 = UUID.randomUUID();
        UUID player2 = UUID.randomUUID();
        UUID player3 = UUID.randomUUID();
        
        Player mockPlayer1 = mock(Player.class);
        Player mockPlayer2 = mock(Player.class);
        Player mockPlayer3 = mock(Player.class);
        
        when(mockPlayer1.getUniqueId()).thenReturn(player1);
        when(mockPlayer2.getUniqueId()).thenReturn(player2);
        when(mockPlayer3.getUniqueId()).thenReturn(player3);
        
        // Load and modify profiles
        playerDataManager.awardExperience(mockPlayer1, SkillType.MINING, 100.0);
        playerDataManager.awardExperience(mockPlayer2, SkillType.COMBAT, 150.0);
        playerDataManager.awardExperience(mockPlayer3, SkillType.FISHING, 75.0);
        
        // Save all at once
        playerDataManager.saveAllProfiles();
        
        // Clear cache and reload
        playerDataManager.clearCache();
        
        // Verify all were saved
        SkillProfile loaded1 = playerDataManager.getProfile(player1);
        SkillProfile loaded2 = playerDataManager.getProfile(player2);
        SkillProfile loaded3 = playerDataManager.getProfile(player3);
        
        assertEquals("Player 1 data should be saved", 100.0,
                     loaded1.getSkill(SkillType.MINING).getExperience(), 0.01);
        assertEquals("Player 2 data should be saved", 150.0,
                     loaded2.getSkill(SkillType.COMBAT).getExperience(), 0.01);
        assertEquals("Player 3 data should be saved", 75.0,
                     loaded3.getSkill(SkillType.FISHING).getExperience(), 0.01);
    }
}
