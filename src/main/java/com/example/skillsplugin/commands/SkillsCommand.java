package com.example.skillsplugin.commands;

import com.example.skillsplugin.SkillsPlugin;
import com.example.skillsplugin.data.PlayerDataManager;
import com.example.skillsplugin.skills.Skill;
import com.example.skillsplugin.skills.SkillProfile;
import com.example.skillsplugin.skills.SkillType;
import com.example.skillsplugin.ui.UIManager;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Handles the /skills command and its subcommands.
 * Provides skill overview, detailed skill information, and config reload functionality.
 */
public class SkillsCommand implements CommandExecutor, TabCompleter {
    
    private final SkillsPlugin plugin;
    private final PlayerDataManager playerDataManager;
    private final UIManager uiManager;
    
    /**
     * Creates a new SkillsCommand handler.
     * 
     * @param plugin The plugin instance
     * @param playerDataManager The player data manager
     * @param uiManager The UI manager
     */
    public SkillsCommand(SkillsPlugin plugin, PlayerDataManager playerDataManager, UIManager uiManager) {
        this.plugin = plugin;
        this.playerDataManager = playerDataManager;
        this.uiManager = uiManager;
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        try {
            // Handle reload subcommand
            if (args.length > 0 && args[0].equalsIgnoreCase("reload")) {
                return handleReload(sender);
            }
            
            // All other commands require the sender to be a player
            if (!(sender instanceof Player)) {
                sender.sendMessage(ChatColor.RED + "This command can only be used by players.");
                return true;
            }
            
            Player player = (Player) sender;
            
            // Check permission
            if (!player.hasPermission("skills.use")) {
                player.sendMessage(ChatColor.RED + "You don't have permission to use this command.");
                return true;
            }
            
            // Handle /skills display <skillname> or /skills display off
            if (args.length >= 2 && args[0].equalsIgnoreCase("display")) {
                return handleDisplay(player, args[1]);
            }
            
            // Get player's skill profile
            SkillProfile profile;
            try {
                profile = playerDataManager.getProfile(player.getUniqueId());
                if (profile == null) {
                    player.sendMessage(ChatColor.RED + "Failed to load your skill profile. Please try again.");
                    plugin.getLogger().severe("Failed to load profile for player " + player.getName());
                    return true;
                }
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "An error occurred while loading your skills. Please contact an administrator.");
                plugin.getLogger().severe("Error loading profile for player " + player.getName() + ": " + e.getMessage());
                e.printStackTrace();
                return true;
            }
            
            // Handle /skills (show overview)
            if (args.length == 0) {
                try {
                    uiManager.sendSkillsOverview(player, profile);
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "An error occurred while displaying your skills.");
                    plugin.getLogger().severe("Error displaying skills overview for player " + player.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
                return true;
            }
            
            // Handle /skills <skillname> (show specific skill details)
            String skillName = args[0].toUpperCase();
            
            try {
                SkillType skillType = SkillType.valueOf(skillName);
                Skill skill = profile.getSkill(skillType);
                
                if (skill == null) {
                    player.sendMessage(ChatColor.RED + "Skill not found: " + skillName);
                    return true;
                }
                
                try {
                    uiManager.sendSkillDetails(player, skill);
                } catch (Exception e) {
                    player.sendMessage(ChatColor.RED + "An error occurred while displaying skill details.");
                    plugin.getLogger().severe("Error displaying skill details for player " + player.getName() + ": " + e.getMessage());
                    e.printStackTrace();
                }
                return true;
                
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Unknown skill: " + args[0]);
                player.sendMessage(ChatColor.GRAY + "Available skills: " + getSkillNamesList());
                return true;
            }
            
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "An unexpected error occurred. Please contact an administrator.");
            plugin.getLogger().severe("Unexpected error in skills command: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }
    
    /**
     * Handles the /skills reload subcommand.
     * 
     * @param sender The command sender
     * @return true if the command was handled successfully
     */
    private boolean handleReload(CommandSender sender) {
        // Check permission
        if (!sender.hasPermission("skills.reload")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to reload the configuration.");
            return true;
        }
        
        try {
            plugin.getConfigManager().reloadConfig();
            sender.sendMessage(ChatColor.GREEN + "Configuration reloaded successfully!");
            return true;
        } catch (Exception e) {
            sender.sendMessage(ChatColor.RED + "Failed to reload configuration. Check console for errors.");
            plugin.getLogger().severe("Failed to reload configuration: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }
    
    /**
     * Handles the /skills display <skillname> subcommand.
     * Changes the player's display name to show their skill level.
     * 
     * @param player The player executing the command
     * @param skillArg The skill name or "off" to remove display
     * @return true if the command was handled successfully
     */
    private boolean handleDisplay(Player player, String skillArg) {
        try {
            // Handle /skills display off
            if (skillArg.equalsIgnoreCase("off") || skillArg.equalsIgnoreCase("none")) {
                player.setDisplayName(player.getName());
                player.setPlayerListName(player.getName());
                player.sendMessage(ChatColor.GREEN + "Skill display removed from your name.");
                return true;
            }
            
            // Get player's skill profile
            SkillProfile profile;
            try {
                profile = playerDataManager.getProfile(player.getUniqueId());
                if (profile == null) {
                    player.sendMessage(ChatColor.RED + "Failed to load your skill profile. Please try again.");
                    return true;
                }
            } catch (Exception e) {
                player.sendMessage(ChatColor.RED + "An error occurred while loading your skills.");
                plugin.getLogger().severe("Error loading profile for player " + player.getName() + ": " + e.getMessage());
                return true;
            }
            
            // Parse skill type
            String skillName = skillArg.toUpperCase();
            try {
                SkillType skillType = SkillType.valueOf(skillName);
                Skill skill = profile.getSkill(skillType);
                
                if (skill == null) {
                    player.sendMessage(ChatColor.RED + "Skill not found: " + skillName);
                    return true;
                }
                
                // Update display name with skill badge
                uiManager.setSkillDisplayName(player, skill);
                player.sendMessage(ChatColor.GREEN + "Your name now displays your " + skillType.name() + " level!");
                return true;
                
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Unknown skill: " + skillArg);
                player.sendMessage(ChatColor.GRAY + "Available skills: " + getSkillNamesList());
                player.sendMessage(ChatColor.GRAY + "Use 'off' to remove skill display");
                return true;
            }
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "An unexpected error occurred.");
            plugin.getLogger().severe("Error in display command: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }
    
    /**
     * Gets a comma-separated list of all skill names.
     * 
     * @return A string with all skill names
     */
    private String getSkillNamesList() {
        return Arrays.stream(SkillType.values())
                .map(SkillType::name)
                .map(String::toLowerCase)
                .collect(Collectors.joining(", "));
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        try {
            List<String> completions = new ArrayList<>();
            
            // First argument: skill names, "reload", or "display"
            if (args.length == 1) {
                // Add all skill names
                for (SkillType skillType : SkillType.values()) {
                    completions.add(skillType.name().toLowerCase());
                }
                
                // Add reload if sender has permission
                if (sender.hasPermission("skills.reload")) {
                    completions.add("reload");
                }
                
                // Add display subcommand
                completions.add("display");
                
                // Filter based on what the user has typed
                String input = args[0].toLowerCase();
                return completions.stream()
                        .filter(s -> s.startsWith(input))
                        .collect(Collectors.toList());
            }
            
            // Second argument for /skills display <skillname>
            if (args.length == 2 && args[0].equalsIgnoreCase("display")) {
                // Add all skill names
                for (SkillType skillType : SkillType.values()) {
                    completions.add(skillType.name().toLowerCase());
                }
                
                // Add "off" option
                completions.add("off");
                
                // Filter based on what the user has typed
                String input = args[1].toLowerCase();
                return completions.stream()
                        .filter(s -> s.startsWith(input))
                        .collect(Collectors.toList());
            }
            
            // No completions for additional arguments
            return completions;
            
        } catch (Exception e) {
            plugin.getLogger().severe("Error in tab completion: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}
