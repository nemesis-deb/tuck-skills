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
            
            // Handle admin subcommands
            if (args.length > 0 && args[0].equalsIgnoreCase("admin")) {
                return handleAdmin(sender, args);
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
            
            // Handle /skills top <skillname>
            if (args.length >= 2 && args[0].equalsIgnoreCase("top")) {
                return handleTop(player, args[1]);
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
            
            // Check if first argument is a player name (for viewing other players)
            Player targetPlayer = plugin.getServer().getPlayer(args[0]);
            if (targetPlayer != null && targetPlayer.isOnline()) {
                // /skills <player> or /skills <player> <skill>
                return handleViewOtherPlayer(player, targetPlayer, args);
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
                player.sendMessage(ChatColor.RED + "Unknown skill or player: " + args[0]);
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
            
            // Handle /skills display off
            if (skillArg.equalsIgnoreCase("off") || skillArg.equalsIgnoreCase("none")) {
                uiManager.removeSkillDisplayName(player, profile);
                playerDataManager.saveProfile(player.getUniqueId()); // Save the preference
                player.sendMessage(ChatColor.GREEN + "Skill display removed from your name.");
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
                
                // Update display name with skill badge and save preference
                uiManager.setSkillDisplayName(player, skill, profile);
                playerDataManager.saveProfile(player.getUniqueId()); // Save the preference
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
     * Handles the /skills top <skillname> subcommand.
     * Displays the top 10 players for a specific skill.
     * 
     * @param player The player executing the command
     * @param skillArg The skill name
     * @return true if the command was handled successfully
     */
    private boolean handleTop(Player player, String skillArg) {
        try {
            // Parse skill type
            String skillName = skillArg.toUpperCase();
            try {
                SkillType skillType = SkillType.valueOf(skillName);
                
                // Get top 10 players
                PlayerDataManager.LeaderboardEntry[] entries = playerDataManager.getTopPlayers(skillType, 10);
                
                // Display leaderboard
                uiManager.sendLeaderboard(player, skillType, entries);
                return true;
                
            } catch (IllegalArgumentException e) {
                player.sendMessage(ChatColor.RED + "Unknown skill: " + skillArg);
                player.sendMessage(ChatColor.GRAY + "Available skills: " + getSkillNamesList());
                return true;
            }
            
        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "An unexpected error occurred.");
            plugin.getLogger().severe("Error in top command: " + e.getMessage());
            e.printStackTrace();
            return true;
        }
    }
    
    /**
     * Handles viewing another player's skills.
     * 
     * @param viewer The player viewing
     * @param target The target player
     * @param args Command arguments
     * @return true if handled successfully
     */
    private boolean handleViewOtherPlayer(Player viewer, Player target, String[] args) {
        try {
            SkillProfile profile = playerDataManager.getProfile(target.getUniqueId());
            if (profile == null) {
                viewer.sendMessage(ChatColor.RED + "Failed to load " + target.getName() + "'s skill profile.");
                return true;
            }
            
            // /skills <player> - show overview
            if (args.length == 1) {
                viewer.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== " + target.getName() + "'s Skills ===");
                viewer.sendMessage("");
                
                for (SkillType type : SkillType.values()) {
                    Skill skill = profile.getSkill(type);
                    if (skill != null) {
                        viewer.sendMessage(uiManager.formatSkillLinePublic(skill));
                    }
                }
                viewer.sendMessage("");
                return true;
            }
            
            // /skills <player> <skill> - show specific skill
            String skillName = args[1].toUpperCase();
            try {
                SkillType skillType = SkillType.valueOf(skillName);
                Skill skill = profile.getSkill(skillType);
                
                if (skill == null) {
                    viewer.sendMessage(ChatColor.RED + "Skill not found: " + skillName);
                    return true;
                }
                
                viewer.sendMessage("");
                viewer.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== " + target.getName() + "'s " + skillType.name() + " ===");
                viewer.sendMessage("");
                viewer.sendMessage(ChatColor.YELLOW + "Level: " + ChatColor.WHITE + skill.getLevel());
                viewer.sendMessage(ChatColor.YELLOW + "Experience: " + ChatColor.WHITE + String.format("%.0f", skill.getExperience()) + " XP");
                viewer.sendMessage("");
                return true;
                
            } catch (IllegalArgumentException e) {
                viewer.sendMessage(ChatColor.RED + "Unknown skill: " + args[1]);
                viewer.sendMessage(ChatColor.GRAY + "Available skills: " + getSkillNamesList());
                return true;
            }
            
        } catch (Exception e) {
            viewer.sendMessage(ChatColor.RED + "An error occurred.");
            plugin.getLogger().severe("Error viewing other player: " + e.getMessage());
            return true;
        }
    }
    
    /**
     * Handles admin subcommands.
     * 
     * @param sender Command sender
     * @param args Command arguments
     * @return true if handled successfully
     */
    private boolean handleAdmin(CommandSender sender, String[] args) {
        // Check permission
        if (!sender.hasPermission("skills.admin")) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to use admin commands.");
            return true;
        }
        
        if (args.length < 2) {
            sender.sendMessage(ChatColor.RED + "Usage:");
            sender.sendMessage(ChatColor.GRAY + "/skills admin set <player> <skill> <level>");
            sender.sendMessage(ChatColor.GRAY + "/skills admin add <player> <skill> <xp>");
            sender.sendMessage(ChatColor.GRAY + "/skills admin reset <player> [skill]");
            return true;
        }
        
        String subCommand = args[1].toLowerCase();
        
        switch (subCommand) {
            case "set":
                return handleAdminSet(sender, args);
            case "add":
                return handleAdminAdd(sender, args);
            case "reset":
                return handleAdminReset(sender, args);
            default:
                sender.sendMessage(ChatColor.RED + "Unknown admin command: " + subCommand);
                return true;
        }
    }
    
    /**
     * Handles /skills admin set command.
     */
    private boolean handleAdminSet(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(ChatColor.RED + "Usage: /skills admin set <player> <skill> <level>");
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[2]);
            return true;
        }
        
        try {
            SkillType skillType = SkillType.valueOf(args[3].toUpperCase());
            int level = Integer.parseInt(args[4]);
            
            if (level < 1) {
                sender.sendMessage(ChatColor.RED + "Level must be at least 1.");
                return true;
            }
            
            SkillProfile profile = playerDataManager.getProfile(target.getUniqueId());
            Skill skill = profile.getSkill(skillType);
            
            if (skill != null) {
                skill.setLevel(level);
                skill.setExperience(0); // Reset XP when setting level
                playerDataManager.saveProfile(target.getUniqueId());
                
                sender.sendMessage(ChatColor.GREEN + "Set " + target.getName() + "'s " + skillType.name() + " to level " + level);
                target.sendMessage(ChatColor.YELLOW + "Your " + skillType.name() + " level has been set to " + level + " by an administrator.");
            }
            
            return true;
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid skill or level.");
            return true;
        }
    }
    
    /**
     * Handles /skills admin add command.
     */
    private boolean handleAdminAdd(CommandSender sender, String[] args) {
        if (args.length < 5) {
            sender.sendMessage(ChatColor.RED + "Usage: /skills admin add <player> <skill> <xp>");
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[2]);
            return true;
        }
        
        try {
            SkillType skillType = SkillType.valueOf(args[3].toUpperCase());
            double xp = Double.parseDouble(args[4]);
            
            if (xp <= 0) {
                sender.sendMessage(ChatColor.RED + "XP must be positive.");
                return true;
            }
            
            PlayerDataManager.ExperienceResult result = playerDataManager.awardExperience(target, skillType, xp);
            playerDataManager.saveProfile(target.getUniqueId());
            
            sender.sendMessage(ChatColor.GREEN + "Gave " + target.getName() + " " + String.format("%.0f", xp) + " " + skillType.name() + " XP");
            
            if (result.isLeveledUp()) {
                SkillProfile profile = playerDataManager.getProfile(target.getUniqueId());
                int newLevel = profile.getSkill(skillType).getLevel();
                sender.sendMessage(ChatColor.GOLD + target.getName() + " leveled up to " + skillType.name() + " " + newLevel + "!");
            }
            
            return true;
        } catch (IllegalArgumentException e) {
            sender.sendMessage(ChatColor.RED + "Invalid skill or XP amount.");
            return true;
        }
    }
    
    /**
     * Handles /skills admin reset command.
     */
    private boolean handleAdminReset(CommandSender sender, String[] args) {
        if (args.length < 3) {
            sender.sendMessage(ChatColor.RED + "Usage: /skills admin reset <player> [skill]");
            return true;
        }
        
        Player target = plugin.getServer().getPlayer(args[2]);
        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Player not found: " + args[2]);
            return true;
        }
        
        SkillProfile profile = playerDataManager.getProfile(target.getUniqueId());
        
        // Reset specific skill
        if (args.length >= 4) {
            try {
                SkillType skillType = SkillType.valueOf(args[3].toUpperCase());
                Skill skill = profile.getSkill(skillType);
                
                if (skill != null) {
                    skill.setLevel(1);
                    skill.setExperience(0);
                    playerDataManager.saveProfile(target.getUniqueId());
                    
                    sender.sendMessage(ChatColor.GREEN + "Reset " + target.getName() + "'s " + skillType.name() + " skill");
                    target.sendMessage(ChatColor.YELLOW + "Your " + skillType.name() + " skill has been reset by an administrator.");
                }
                
                return true;
            } catch (IllegalArgumentException e) {
                sender.sendMessage(ChatColor.RED + "Invalid skill: " + args[3]);
                return true;
            }
        }
        
        // Reset all skills
        for (SkillType type : SkillType.values()) {
            Skill skill = profile.getSkill(type);
            if (skill != null) {
                skill.setLevel(1);
                skill.setExperience(0);
            }
        }
        playerDataManager.saveProfile(target.getUniqueId());
        
        sender.sendMessage(ChatColor.GREEN + "Reset all of " + target.getName() + "'s skills");
        target.sendMessage(ChatColor.YELLOW + "All your skills have been reset by an administrator.");
        
        return true;
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
            
            // First argument: skill names, "reload", "display", "top", "admin", or player names
            if (args.length == 1) {
                // Add all skill names
                for (SkillType skillType : SkillType.values()) {
                    completions.add(skillType.name().toLowerCase());
                }
                
                // Add reload if sender has permission
                if (sender.hasPermission("skills.reload")) {
                    completions.add("reload");
                }
                
                // Add admin if sender has permission
                if (sender.hasPermission("skills.admin")) {
                    completions.add("admin");
                }
                
                // Add subcommands
                completions.add("display");
                completions.add("top");
                
                // Add online player names
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    completions.add(p.getName());
                }
                
                // Filter based on what the user has typed
                String input = args[0].toLowerCase();
                return completions.stream()
                        .filter(s -> s.startsWith(input))
                        .collect(Collectors.toList());
            }
            
            // Second argument for /skills admin <subcommand>
            if (args.length == 2 && args[0].equalsIgnoreCase("admin")) {
                if (sender.hasPermission("skills.admin")) {
                    completions.add("set");
                    completions.add("add");
                    completions.add("reset");
                }
                
                String input = args[1].toLowerCase();
                return completions.stream()
                        .filter(s -> s.startsWith(input))
                        .collect(Collectors.toList());
            }
            
            // Third argument for /skills admin <subcommand> <player>
            if (args.length == 3 && args[0].equalsIgnoreCase("admin")) {
                for (Player p : plugin.getServer().getOnlinePlayers()) {
                    completions.add(p.getName());
                }
                
                String input = args[2].toLowerCase();
                return completions.stream()
                        .filter(s -> s.startsWith(input))
                        .collect(Collectors.toList());
            }
            
            // Fourth argument for /skills admin set/add <player> <skill>
            if (args.length == 4 && args[0].equalsIgnoreCase("admin") && 
                (args[1].equalsIgnoreCase("set") || args[1].equalsIgnoreCase("add") || args[1].equalsIgnoreCase("reset"))) {
                for (SkillType skillType : SkillType.values()) {
                    completions.add(skillType.name().toLowerCase());
                }
                
                String input = args[3].toLowerCase();
                return completions.stream()
                        .filter(s -> s.startsWith(input))
                        .collect(Collectors.toList());
            }
            
            // Second argument for /skills <player> <skill>
            if (args.length == 2) {
                // Check if first arg is a player
                Player target = plugin.getServer().getPlayer(args[0]);
                if (target != null) {
                    for (SkillType skillType : SkillType.values()) {
                        completions.add(skillType.name().toLowerCase());
                    }
                    
                    String input = args[1].toLowerCase();
                    return completions.stream()
                            .filter(s -> s.startsWith(input))
                            .collect(Collectors.toList());
                }
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
            
            // Second argument for /skills top <skillname>
            if (args.length == 2 && args[0].equalsIgnoreCase("top")) {
                // Add all skill names
                for (SkillType skillType : SkillType.values()) {
                    completions.add(skillType.name().toLowerCase());
                }
                
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
