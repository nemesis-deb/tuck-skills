package com.example.skillsplugin.commands;

import com.example.skillsplugin.SkillsPlugin;
import com.example.skillsplugin.config.ConfigManager;
import com.example.skillsplugin.data.PlayerDataManager;
import com.example.skillsplugin.skills.Skill;
import com.example.skillsplugin.skills.SkillProfile;
import com.example.skillsplugin.skills.SkillType;
import com.example.skillsplugin.ui.UIManager;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.UUID;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Tests for the SkillsCommand class.
 */
@RunWith(MockitoJUnitRunner.class)
public class SkillsCommandTest {
    
    @Mock
    private SkillsPlugin plugin;
    
    @Mock
    private PlayerDataManager playerDataManager;
    
    @Mock
    private UIManager uiManager;
    
    @Mock
    private ConfigManager configManager;
    
    @Mock
    private Player player;
    
    @Mock
    private CommandSender consoleSender;
    
    @Mock
    private Command command;
    
    @Mock
    private java.util.logging.Logger logger;
    
    private SkillsCommand skillsCommand;
    private SkillProfile testProfile;
    private UUID testPlayerId;
    
    @Before
    public void setUp() {
        skillsCommand = new SkillsCommand(plugin, playerDataManager, uiManager);
        testPlayerId = UUID.randomUUID();
        testProfile = new SkillProfile(testPlayerId);
        
        // Setup player mock
        when(player.getUniqueId()).thenReturn(testPlayerId);
        when(player.hasPermission("skills.use")).thenReturn(true);
        when(player.hasPermission("skills.reload")).thenReturn(false);
        
        // Setup player data manager mock
        when(playerDataManager.getProfile(testPlayerId)).thenReturn(testProfile);
        
        // Setup plugin mock
        when(plugin.getConfigManager()).thenReturn(configManager);
        when(plugin.getLogger()).thenReturn(logger);
    }
    
    @Test
    public void testSkillsOverviewCommand() {
        // Execute /skills command
        boolean result = skillsCommand.onCommand(player, command, "skills", new String[]{});
        
        // Verify command was successful
        assertTrue(result);
        
        // Verify UI manager was called to show overview
        verify(uiManager).sendSkillsOverview(player, testProfile);
        verify(playerDataManager).getProfile(testPlayerId);
    }
    
    @Test
    public void testSkillDetailsCommand() {
        // Execute /skills mining command
        boolean result = skillsCommand.onCommand(player, command, "skills", new String[]{"mining"});
        
        // Verify command was successful
        assertTrue(result);
        
        // Verify UI manager was called to show skill details
        Skill miningSkill = testProfile.getSkill(SkillType.MINING);
        verify(uiManager).sendSkillDetails(player, miningSkill);
        verify(playerDataManager).getProfile(testPlayerId);
    }
    
    @Test
    public void testSkillDetailsCommandCaseInsensitive() {
        // Execute /skills WOODCUTTING command (uppercase)
        boolean result = skillsCommand.onCommand(player, command, "skills", new String[]{"WOODCUTTING"});
        
        // Verify command was successful
        assertTrue(result);
        
        // Verify UI manager was called to show skill details
        Skill woodcuttingSkill = testProfile.getSkill(SkillType.WOODCUTTING);
        verify(uiManager).sendSkillDetails(player, woodcuttingSkill);
    }
    
    @Test
    public void testInvalidSkillName() {
        // Execute /skills invalidskill command
        boolean result = skillsCommand.onCommand(player, command, "skills", new String[]{"invalidskill"});
        
        // Verify command was successful (handled gracefully)
        assertTrue(result);
        
        // Verify error message was sent to player
        verify(player).sendMessage(contains("Unknown skill"));
        
        // Verify UI manager was NOT called
        verify(uiManager, never()).sendSkillDetails(any(), any());
    }
    
    @Test
    public void testReloadCommandWithPermission() {
        // Give player reload permission
        when(player.hasPermission("skills.reload")).thenReturn(true);
        
        // Execute /skills reload command
        boolean result = skillsCommand.onCommand(player, command, "skills", new String[]{"reload"});
        
        // Verify command was successful
        assertTrue(result);
        
        // Verify config was reloaded
        verify(configManager).reloadConfig();
        
        // Verify success message was sent
        verify(player).sendMessage(contains("reloaded successfully"));
    }
    
    @Test
    public void testReloadCommandWithoutPermission() {
        // Execute /skills reload command without permission
        boolean result = skillsCommand.onCommand(player, command, "skills", new String[]{"reload"});
        
        // Verify command was successful (handled gracefully)
        assertTrue(result);
        
        // Verify config was NOT reloaded
        verify(configManager, never()).reloadConfig();
        
        // Verify permission error message was sent
        verify(player).sendMessage(contains("don't have permission"));
    }
    
    @Test
    public void testReloadCommandFromConsole() {
        // Give console sender reload permission
        when(consoleSender.hasPermission("skills.reload")).thenReturn(true);
        
        // Execute /skills reload command from console
        boolean result = skillsCommand.onCommand(consoleSender, command, "skills", new String[]{"reload"});
        
        // Verify command was successful
        assertTrue(result);
        
        // Verify config was reloaded
        verify(configManager).reloadConfig();
        
        // Verify success message was sent
        verify(consoleSender).sendMessage(contains("reloaded successfully"));
    }
    
    @Test
    public void testReloadCommandFailure() {
        // Give player reload permission
        when(player.hasPermission("skills.reload")).thenReturn(true);
        
        // Make config reload throw exception
        doThrow(new RuntimeException("Config error")).when(configManager).reloadConfig();
        
        // Execute /skills reload command
        boolean result = skillsCommand.onCommand(player, command, "skills", new String[]{"reload"});
        
        // Verify command was successful (handled gracefully)
        assertTrue(result);
        
        // Verify error message was sent
        verify(player).sendMessage(contains("Failed to reload"));
    }
    
    @Test
    public void testCommandWithoutPermission() {
        // Remove permission
        when(player.hasPermission("skills.use")).thenReturn(false);
        
        // Execute /skills command
        boolean result = skillsCommand.onCommand(player, command, "skills", new String[]{});
        
        // Verify command was successful (handled gracefully)
        assertTrue(result);
        
        // Verify permission error message was sent
        verify(player).sendMessage(contains("don't have permission"));
        
        // Verify UI manager was NOT called
        verify(uiManager, never()).sendSkillsOverview(any(), any());
    }
    
    @Test
    public void testCommandFromConsoleWithoutReload() {
        // Execute /skills command from console (not reload)
        boolean result = skillsCommand.onCommand(consoleSender, command, "skills", new String[]{});
        
        // Verify command was successful (handled gracefully)
        assertTrue(result);
        
        // Verify error message was sent
        verify(consoleSender).sendMessage(contains("only be used by players"));
    }
    
    @Test
    public void testTabCompletionFirstArgument() {
        // Test tab completion for first argument
        List<String> completions = skillsCommand.onTabComplete(player, command, "skills", new String[]{""});
        
        // Verify all skill names are included
        assertNotNull(completions);
        assertTrue(completions.contains("mining"));
        assertTrue(completions.contains("woodcutting"));
        assertTrue(completions.contains("combat"));
        assertTrue(completions.contains("farming"));
        assertTrue(completions.contains("fishing"));
        assertTrue(completions.contains("enchanting"));
        assertTrue(completions.contains("trading"));
        
        // Verify reload is NOT included (player doesn't have permission)
        assertFalse(completions.contains("reload"));
    }
    
    @Test
    public void testTabCompletionWithReloadPermission() {
        // Give player reload permission
        when(player.hasPermission("skills.reload")).thenReturn(true);
        
        // Test tab completion for first argument
        List<String> completions = skillsCommand.onTabComplete(player, command, "skills", new String[]{""});
        
        // Verify reload is included
        assertNotNull(completions);
        assertTrue(completions.contains("reload"));
    }
    
    @Test
    public void testTabCompletionFiltering() {
        // Test tab completion with partial input "mi"
        List<String> completions = skillsCommand.onTabComplete(player, command, "skills", new String[]{"mi"});
        
        // Verify only matching skills are included
        assertNotNull(completions);
        assertTrue(completions.contains("mining"));
        assertFalse(completions.contains("woodcutting"));
        assertFalse(completions.contains("combat"));
    }
    
    @Test
    public void testTabCompletionFilteringCaseInsensitive() {
        // Test tab completion with partial input "FI"
        List<String> completions = skillsCommand.onTabComplete(player, command, "skills", new String[]{"FI"});
        
        // Verify matching skills are included (case insensitive)
        assertNotNull(completions);
        assertTrue(completions.contains("fishing"));
        assertFalse(completions.contains("mining"));
    }
    
    @Test
    public void testTabCompletionSecondArgument() {
        // Test tab completion for second argument (should be empty)
        List<String> completions = skillsCommand.onTabComplete(player, command, "skills", new String[]{"mining", ""});
        
        // Verify no completions for second argument
        assertNotNull(completions);
        assertTrue(completions.isEmpty());
    }
    
    @Test
    public void testAllSkillTypesCanBeQueried() {
        // Test that all skill types can be queried successfully
        for (SkillType skillType : SkillType.values()) {
            String skillName = skillType.name().toLowerCase();
            boolean result = skillsCommand.onCommand(player, command, "skills", new String[]{skillName});
            
            assertTrue("Failed to query skill: " + skillName, result);
        }
        
        // Verify UI manager was called for each skill
        verify(uiManager, times(SkillType.values().length)).sendSkillDetails(eq(player), any(Skill.class));
    }
}
