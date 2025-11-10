package com.example.skillsplugin.ui;

import com.example.skillsplugin.skills.Skill;
import com.example.skillsplugin.skills.SkillProfile;
import com.example.skillsplugin.skills.SkillType;
import net.md_5.bungee.api.chat.BaseComponent;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for UIManager class.
 * Tests boss bar creation, action bar messages, and skill display formatting.
 */
public class UIManagerTest {
    
    private UIManager uiManager;
    private Plugin mockPlugin;
    private Player mockPlayer;
    private Player.Spigot mockSpigot;
    private UUID playerId;
    
    @Before
    public void setUp() {
        mockPlugin = mock(Plugin.class);
        mockPlayer = mock(Player.class);
        mockSpigot = mock(Player.Spigot.class);
        playerId = UUID.randomUUID();
        
        when(mockPlayer.getUniqueId()).thenReturn(playerId);
        when(mockPlayer.spigot()).thenReturn(mockSpigot);
        when(mockPlayer.isOnline()).thenReturn(true);
        
        uiManager = new UIManager(mockPlugin);
    }
    
    @Test
    public void testSendXPGainMessage() {
        // Test that XP gain message is sent via action bar
        uiManager.sendXPGainMessage(mockPlayer, SkillType.MINING, 25.5);
        
        // Verify spigot().sendMessage was called
        verify(mockSpigot).sendMessage(any(net.md_5.bungee.api.ChatMessageType.class), 
                                       any(BaseComponent[].class));
    }
    
    @Test
    public void testSendSkillsOverview() {
        // Create a skill profile with some skills
        SkillProfile profile = new SkillProfile(playerId);
        Skill miningSkill = profile.getSkill(SkillType.MINING);
        miningSkill.addExperience(150);
        
        // Send overview
        uiManager.sendSkillsOverview(mockPlayer, profile);
        
        // Verify messages were sent
        verify(mockPlayer, atLeast(10)).sendMessage(anyString());
    }
    
    @Test
    public void testSendSkillDetails() {
        // Create a skill with some progress
        Skill skill = new Skill(SkillType.COMBAT);
        skill.addExperience(75);
        
        // Send details
        uiManager.sendSkillDetails(mockPlayer, skill);
        
        // Verify multiple messages were sent (header, level, experience, progress bar)
        verify(mockPlayer, atLeast(5)).sendMessage(anyString());
    }
    
    @Test
    public void testCleanup() {
        // Cleanup should not throw any exceptions
        try {
            uiManager.cleanup();
        } catch (Exception e) {
            fail("Cleanup should not throw exceptions");
        }
    }
    
    @Test
    public void testRemoveBossBarWithNoActiveBossBar() {
        // Should not throw exception when removing non-existent boss bar
        try {
            uiManager.removeBossBar(mockPlayer);
        } catch (Exception e) {
            fail("Remove boss bar should not throw exceptions");
        }
    }
    
    @Test
    public void testSendXPGainMessageWithDifferentSkills() {
        // Test XP messages for different skills
        for (SkillType type : SkillType.values()) {
            uiManager.sendXPGainMessage(mockPlayer, type, 10.0);
        }
        
        // Verify message was sent for each skill
        verify(mockSpigot, times(SkillType.values().length))
            .sendMessage(any(net.md_5.bungee.api.ChatMessageType.class), 
                        any(BaseComponent[].class));
    }
    
    @Test
    public void testSendSkillDetailsShowsCorrectLevel() {
        Skill skill = new Skill(SkillType.FISHING);
        skill.setLevel(5);
        skill.setExperience(50);
        
        uiManager.sendSkillDetails(mockPlayer, skill);
        
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockPlayer, atLeast(1)).sendMessage(messageCaptor.capture());
        
        // Check that level 5 appears in one of the messages
        boolean foundLevel = false;
        for (String msg : messageCaptor.getAllValues()) {
            if (msg.contains("5")) {
                foundLevel = true;
                break;
            }
        }
        assertTrue("Level should be displayed in skill details", foundLevel);
    }
    
    @Test
    public void testSendSkillsOverviewContainsAllSkills() {
        SkillProfile profile = new SkillProfile(playerId);
        
        uiManager.sendSkillsOverview(mockPlayer, profile);
        
        ArgumentCaptor<String> messageCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockPlayer, atLeast(1)).sendMessage(messageCaptor.capture());
        
        // Verify that all skill types appear in the messages
        String allMessages = String.join(" ", messageCaptor.getAllValues());
        for (SkillType type : SkillType.values()) {
            assertTrue("Overview should contain " + type.name(), 
                      allMessages.contains(type.name()));
        }
    }
}
