package com.example.skillsplugin.events;

import com.example.skillsplugin.data.PlayerDataManager;
import com.example.skillsplugin.skills.SkillProfile;
import com.example.skillsplugin.ui.UIManager;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;
import java.util.logging.Logger;

import static org.mockito.Mockito.*;

/**
 * Tests for PlayerConnectionListener.
 * Verifies player join/quit handling, profile loading/saving, and cache management.
 */
public class PlayerConnectionListenerTest {
    
    private PlayerConnectionListener listener;
    private PlayerDataManager playerDataManager;
    private UIManager uiManager;
    private Logger logger;
    
    private Player mockPlayer;
    private UUID playerId;
    
    @Before
    public void setUp() {
        playerDataManager = mock(PlayerDataManager.class);
        uiManager = mock(UIManager.class);
        logger = mock(Logger.class);
        
        listener = new PlayerConnectionListener(playerDataManager, uiManager, logger);
        
        // Set up mock player
        mockPlayer = mock(Player.class);
        playerId = UUID.randomUUID();
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        when(mockPlayer.getName()).thenReturn("TestPlayer");
    }
    
    @Test
    public void testPlayerJoinLoadsProfile() {
        // Arrange
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, "TestPlayer joined");
        SkillProfile mockProfile = mock(SkillProfile.class);
        when(mockProfile.isNew()).thenReturn(false);
        when(playerDataManager.getProfile(playerId)).thenReturn(mockProfile);
        when(playerDataManager.isCached(playerId)).thenReturn(true);
        
        // Act
        listener.onPlayerJoin(event);
        
        // Assert
        verify(playerDataManager).getProfile(playerId);
        verify(logger).log(any(), contains("Player joined"));
    }
    
    @Test
    public void testFirstTimePlayerJoinCreatesNewProfile() {
        // Arrange
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, "TestPlayer joined");
        SkillProfile mockProfile = mock(SkillProfile.class);
        when(mockProfile.isNew()).thenReturn(true);
        when(playerDataManager.getProfile(playerId)).thenReturn(mockProfile);
        when(playerDataManager.isCached(playerId)).thenReturn(false);
        
        // Act
        listener.onPlayerJoin(event);
        
        // Assert
        verify(playerDataManager).getProfile(playerId);
        verify(logger).log(any(), contains("First-time player joined"));
    }
    
    @Test
    public void testPlayerJoinHandlesException() {
        // Arrange
        PlayerJoinEvent event = new PlayerJoinEvent(mockPlayer, "TestPlayer joined");
        when(playerDataManager.getProfile(playerId)).thenThrow(new RuntimeException("Test exception"));
        
        // Act - should not throw exception
        listener.onPlayerJoin(event);
        
        // Assert
        verify(logger).log(any(), anyString(), any(Exception.class));
    }
    
    @Test
    public void testPlayerQuitSavesProfile() {
        // Arrange
        PlayerQuitEvent event = new PlayerQuitEvent(mockPlayer, "TestPlayer left");
        
        // Act
        listener.onPlayerQuit(event);
        
        // Assert
        verify(playerDataManager).saveProfile(playerId);
        verify(logger).log(any(), contains("Saved profile"));
    }
    
    @Test
    public void testPlayerQuitCleansUpUI() {
        // Arrange
        PlayerQuitEvent event = new PlayerQuitEvent(mockPlayer, "TestPlayer left");
        
        // Act
        listener.onPlayerQuit(event);
        
        // Assert
        verify(uiManager).cleanupPlayer(mockPlayer);
    }
    
    @Test
    public void testPlayerQuitRemovesFromCache() {
        // Arrange
        PlayerQuitEvent event = new PlayerQuitEvent(mockPlayer, "TestPlayer left");
        
        // Act
        listener.onPlayerQuit(event);
        
        // Assert
        verify(playerDataManager).removeFromCache(playerId);
    }
    
    @Test
    public void testPlayerQuitHandlesException() {
        // Arrange
        PlayerQuitEvent event = new PlayerQuitEvent(mockPlayer, "TestPlayer left");
        doThrow(new RuntimeException("Test exception")).when(playerDataManager).saveProfile(playerId);
        
        // Act - should not throw exception
        listener.onPlayerQuit(event);
        
        // Assert
        verify(logger).log(any(), anyString(), any(Exception.class));
        // Should still attempt cleanup even if save fails
        verify(uiManager).cleanupPlayer(mockPlayer);
        verify(playerDataManager).removeFromCache(playerId);
    }
    
    @Test
    public void testPlayerQuitCompletesAllSteps() {
        // Arrange
        PlayerQuitEvent event = new PlayerQuitEvent(mockPlayer, "TestPlayer left");
        
        // Act
        listener.onPlayerQuit(event);
        
        // Assert - verify all steps are executed in order
        verify(playerDataManager).saveProfile(playerId);
        verify(uiManager).cleanupPlayer(mockPlayer);
        verify(playerDataManager).removeFromCache(playerId);
    }
    
    @Test
    public void testMultiplePlayersJoinAndQuit() {
        // Arrange
        Player player1 = mock(Player.class);
        Player player2 = mock(Player.class);
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();
        
        when(player1.getUniqueId()).thenReturn(id1);
        when(player1.getName()).thenReturn("Player1");
        when(player2.getUniqueId()).thenReturn(id2);
        when(player2.getName()).thenReturn("Player2");
        
        SkillProfile profile1 = mock(SkillProfile.class);
        SkillProfile profile2 = mock(SkillProfile.class);
        when(profile1.isNew()).thenReturn(false);
        when(profile2.isNew()).thenReturn(false);
        
        when(playerDataManager.getProfile(id1)).thenReturn(profile1);
        when(playerDataManager.getProfile(id2)).thenReturn(profile2);
        when(playerDataManager.isCached(id1)).thenReturn(true);
        when(playerDataManager.isCached(id2)).thenReturn(true);
        
        PlayerJoinEvent joinEvent1 = new PlayerJoinEvent(player1, "Player1 joined");
        PlayerJoinEvent joinEvent2 = new PlayerJoinEvent(player2, "Player2 joined");
        PlayerQuitEvent quitEvent1 = new PlayerQuitEvent(player1, "Player1 left");
        PlayerQuitEvent quitEvent2 = new PlayerQuitEvent(player2, "Player2 left");
        
        // Act
        listener.onPlayerJoin(joinEvent1);
        listener.onPlayerJoin(joinEvent2);
        listener.onPlayerQuit(quitEvent1);
        listener.onPlayerQuit(quitEvent2);
        
        // Assert
        verify(playerDataManager).getProfile(id1);
        verify(playerDataManager).getProfile(id2);
        verify(playerDataManager).saveProfile(id1);
        verify(playerDataManager).saveProfile(id2);
        verify(playerDataManager).removeFromCache(id1);
        verify(playerDataManager).removeFromCache(id2);
        verify(uiManager).cleanupPlayer(player1);
        verify(uiManager).cleanupPlayer(player2);
    }
}
