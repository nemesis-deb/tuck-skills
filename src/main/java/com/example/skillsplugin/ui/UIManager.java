package com.example.skillsplugin.ui;

import com.example.skillsplugin.skills.Skill;
import com.example.skillsplugin.skills.SkillProfile;
import com.example.skillsplugin.skills.SkillType;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitRunnable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Manages all player-facing UI elements including boss bars, action bars, and chat messages.
 * Handles visual feedback for skill progression, level-ups, and skill information display.
 */
public class UIManager {
    private final Plugin plugin;
    private final Map<UUID, BossBar> activeBossBars;
    private final Map<UUID, Integer> bossBarTaskIds; // Track scheduled tasks for cleanup
    
    /**
     * Creates a new UIManager.
     * 
     * @param plugin The plugin instance for scheduling tasks
     */
    public UIManager(Plugin plugin) {
        this.plugin = plugin;
        this.activeBossBars = new HashMap<>();
        this.bossBarTaskIds = new HashMap<>();
    }
    
    /**
     * Displays a boss bar to celebrate a skill level-up.
     * The boss bar automatically removes itself after the configured duration.
     * 
     * @param player The player who leveled up
     * @param skill The skill type that leveled up
     * @param newLevel The new level achieved
     */
    public void showLevelUpBossBar(Player player, SkillType skill, int newLevel) {
        try {
            if (player == null || !player.isOnline()) {
                return;
            }
            
            UUID playerId = player.getUniqueId();
            
            // Remove any existing boss bar for this player
            removeBossBar(player);
            
            // Create the boss bar with skill name and level
            String title = ChatColor.GOLD + "" + ChatColor.BOLD + skill.name() + " LEVEL UP! " 
                         + ChatColor.YELLOW + "Level " + newLevel;
            BossBar bossBar = Bukkit.createBossBar(title, getColorForSkill(skill), BarStyle.SOLID);
            bossBar.setProgress(1.0);
            bossBar.addPlayer(player);
            
            // Store the boss bar
            activeBossBars.put(playerId, bossBar);
            
            // Schedule removal after configured duration (default 5 seconds = 100 ticks)
            int taskId = new BukkitRunnable() {
                @Override
                public void run() {
                    try {
                        removeBossBar(player);
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error removing boss bar for player " + player.getName() + ": " + e.getMessage());
                    }
                }
            }.runTaskLater(plugin, 100L).getTaskId(); // Use default 100 ticks for now
            
            // Store task ID for cleanup
            bossBarTaskIds.put(playerId, taskId);
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error showing level-up boss bar for player " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Sends an action bar message to notify the player of XP gain.
     * 
     * @param player The player who gained XP
     * @param skill The skill type that gained XP
     * @param amount The amount of XP gained
     */
    public void sendXPGainMessage(Player player, SkillType skill, double amount) {
        try {
            if (player == null || !player.isOnline()) {
                return;
            }
            
            String message = ChatColor.GREEN + "+" + String.format("%.1f", amount) + " " 
                           + ChatColor.GRAY + skill.name() + " XP";
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
        } catch (Exception e) {
            plugin.getLogger().warning("Error sending XP gain message to player " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Sends a formatted overview of all the player's skills.
     * 
     * @param player The player to send the overview to
     * @param profile The player's skill profile
     */
    public void sendSkillsOverview(Player player, SkillProfile profile) {
        try {
            if (player == null || !player.isOnline()) {
                return;
            }
            
            if (profile == null) {
                player.sendMessage(ChatColor.RED + "Error: Could not load your skill profile.");
                return;
            }
            
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== Your Skills ===");
            player.sendMessage("");
            
            for (SkillType type : SkillType.values()) {
                try {
                    Skill skill = profile.getSkill(type);
                    if (skill != null) {
                        String skillLine = formatSkillLine(skill);
                        player.sendMessage(skillLine);
                    }
                } catch (Exception e) {
                    plugin.getLogger().warning("Error formatting skill line for " + type + ": " + e.getMessage());
                    player.sendMessage(ChatColor.RED + type.name() + " - Error loading skill data");
                }
            }
            
            player.sendMessage("");
            player.sendMessage(ChatColor.GRAY + "Use " + ChatColor.WHITE + "/skills <skillname>" 
                             + ChatColor.GRAY + " for details");
                             
        } catch (Exception e) {
            plugin.getLogger().warning("Error sending skills overview to player " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatColor.RED + "An error occurred while displaying your skills.");
        }
    }
    
    /**
     * Sends detailed information about a specific skill including progress bar.
     * 
     * @param player The player to send the details to
     * @param skill The skill to display details for
     */
    public void sendSkillDetails(Player player, Skill skill) {
        try {
            if (player == null || !player.isOnline()) {
                return;
            }
            
            if (skill == null) {
                player.sendMessage(ChatColor.RED + "Error: Skill data not found.");
                return;
            }
            
            SkillType type = skill.getType();
            int level = skill.getLevel();
            double currentXP = skill.getExperience();
            double requiredXP = skill.getRequiredExperience();
            double progress = requiredXP > 0 ? currentXP / requiredXP : 0;
            
            player.sendMessage("");
            player.sendMessage(ChatColor.GOLD + "" + ChatColor.BOLD + "=== " + type.name() + " ===");
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "Level: " + ChatColor.WHITE + level);
            player.sendMessage(ChatColor.YELLOW + "Experience: " + ChatColor.WHITE 
                             + String.format("%.1f", currentXP) + " / " + String.format("%.1f", requiredXP));
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "Progress: " + createProgressBar(progress));
            player.sendMessage("");
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error sending skill details to player " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatColor.RED + "An error occurred while displaying skill details.");
        }
    }
    
    /**
     * Removes and cleans up the boss bar for a player.
     * Called on player disconnect or when boss bar expires.
     * Cancels any scheduled removal tasks to prevent memory leaks.
     * 
     * @param player The player whose boss bar should be removed
     */
    public void removeBossBar(Player player) {
        try {
            if (player == null) {
                return;
            }
            
            UUID playerId = player.getUniqueId();
            
            // Cancel scheduled task if exists
            Integer taskId = bossBarTaskIds.remove(playerId);
            if (taskId != null) {
                try {
                    Bukkit.getScheduler().cancelTask(taskId);
                } catch (Exception e) {
                    plugin.getLogger().fine("Error canceling boss bar task: " + e.getMessage());
                }
            }
            
            // Remove and cleanup boss bar
            BossBar bossBar = activeBossBars.remove(playerId);
            if (bossBar != null) {
                try {
                    bossBar.removePlayer(player);
                } catch (Exception e) {
                    plugin.getLogger().fine("Error removing player from boss bar: " + e.getMessage());
                }
                try {
                    bossBar.removeAll();
                } catch (Exception e) {
                    plugin.getLogger().fine("Error removing all players from boss bar: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            plugin.getLogger().warning("Error in removeBossBar: " + e.getMessage());
        }
    }
    
    /**
     * Cleans up all UI elements for a specific player.
     * Called when a player disconnects to ensure proper cleanup.
     * 
     * @param player The player to clean up UI elements for
     */
    public void cleanupPlayer(Player player) {
        try {
            removeBossBar(player);
        } catch (Exception e) {
            plugin.getLogger().warning("Error cleaning up UI for player " + (player != null ? player.getName() : "null") + ": " + e.getMessage());
        }
    }
    
    /**
     * Cleans up all boss bars and cancels all scheduled tasks (called on plugin disable).
     * Prevents memory leaks by ensuring all resources are properly released.
     */
    public void cleanup() {
        try {
            // Cancel all scheduled tasks
            for (Integer taskId : bossBarTaskIds.values()) {
                try {
                    if (taskId != null) {
                        Bukkit.getScheduler().cancelTask(taskId);
                    }
                } catch (Exception e) {
                    plugin.getLogger().fine("Error canceling task during cleanup: " + e.getMessage());
                }
            }
            bossBarTaskIds.clear();
            
            // Remove all boss bars
            for (BossBar bossBar : activeBossBars.values()) {
                try {
                    if (bossBar != null) {
                        bossBar.removeAll();
                    }
                } catch (Exception e) {
                    plugin.getLogger().fine("Error removing boss bar during cleanup: " + e.getMessage());
                }
            }
            activeBossBars.clear();
        } catch (Exception e) {
            plugin.getLogger().warning("Error during UI cleanup: " + e.getMessage());
        }
    }
    
    /**
     * Formats a single skill line for the overview display.
     * 
     * @param skill The skill to format
     * @return A formatted string with skill name, level, and mini progress bar
     */
    private String formatSkillLine(Skill skill) {
        SkillType type = skill.getType();
        int level = skill.getLevel();
        double progress = skill.getExperience() / skill.getRequiredExperience();
        
        String skillName = ChatColor.AQUA + type.name();
        String levelText = ChatColor.YELLOW + "Lvl " + level;
        String progressBar = createMiniProgressBar(progress);
        
        return skillName + " " + levelText + " " + progressBar;
    }
    
    /**
     * Creates a visual progress bar for detailed skill display.
     * 
     * @param progress The progress as a decimal (0.0 to 1.0)
     * @return A formatted progress bar string
     */
    private String createProgressBar(double progress) {
        int totalBars = 20;
        int filledBars = (int) (progress * totalBars);
        
        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.GREEN);
        
        for (int i = 0; i < totalBars; i++) {
            if (i < filledBars) {
                bar.append("█");
            } else if (i == 0) {
                bar.append(ChatColor.GRAY);
                bar.append("█");
            } else {
                bar.append("█");
            }
        }
        
        bar.append(ChatColor.WHITE + " " + String.format("%.1f", progress * 100) + "%");
        
        return bar.toString();
    }
    
    /**
     * Creates a mini progress bar for skill overview display.
     * 
     * @param progress The progress as a decimal (0.0 to 1.0)
     * @return A formatted mini progress bar string
     */
    private String createMiniProgressBar(double progress) {
        int totalBars = 10;
        int filledBars = (int) (progress * totalBars);
        
        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.GRAY + "[");
        bar.append(ChatColor.GREEN);
        
        for (int i = 0; i < totalBars; i++) {
            if (i < filledBars) {
                bar.append("|");
            } else {
                bar.append(ChatColor.DARK_GRAY + "|");
            }
        }
        
        bar.append(ChatColor.GRAY + "]");
        
        return bar.toString();
    }
    
    /**
     * Gets the appropriate boss bar color for a skill type.
     * 
     * @param skill The skill type
     * @return The bar color for this skill
     */
    private BarColor getColorForSkill(SkillType skill) {
        switch (skill) {
            case MINING:
                return BarColor.BLUE;
            case WOODCUTTING:
                return BarColor.GREEN;
            case COMBAT:
                return BarColor.RED;
            case FARMING:
                return BarColor.YELLOW;
            case FISHING:
                return BarColor.BLUE;
            case ENCHANTING:
                return BarColor.PURPLE;
            case TRADING:
                return BarColor.WHITE;
            default:
                return BarColor.WHITE;
        }
    }
}
