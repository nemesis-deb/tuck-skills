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
    private com.example.skillsplugin.config.ConfigManager configManager;
    
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
     * Sets the config manager for sound settings.
     * 
     * @param configManager The config manager
     */
    public void setConfigManager(com.example.skillsplugin.config.ConfigManager configManager) {
        this.configManager = configManager;
    }
    
    /**
     * Displays a boss bar to celebrate a skill level-up.
     * The boss bar is shown to all online players and automatically removes itself after 5 seconds.
     * The boss bar health decreases smoothly over the duration.
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
            
            // Play level-up sound
            playLevelUpSound(player);
            
            UUID playerId = player.getUniqueId();
            
            // Remove any existing boss bar for this player
            removeBossBar(player);
            
            // Create the boss bar with player name, skill icon, and level
            ChatColor skillColor = getChatColorForSkill(skill);
            String skillIcon = getIconForSkill(skill);
            String title = ChatColor.WHITE + player.getName() 
                         + ChatColor.GRAY + " reached " 
                         + skillColor + "" + ChatColor.BOLD + skillIcon + " " + skill.name() 
                         + ChatColor.YELLOW + " Level " + ChatColor.WHITE + "" + ChatColor.BOLD + newLevel;
            BossBar bossBar = Bukkit.createBossBar(title, getColorForSkill(skill), BarStyle.SOLID);
            bossBar.setProgress(1.0);
            
            // Add all online players to see the boss bar
            for (Player onlinePlayer : Bukkit.getOnlinePlayers()) {
                bossBar.addPlayer(onlinePlayer);
            }
            
            // Store the boss bar
            activeBossBars.put(playerId, bossBar);
            
            // Schedule boss bar health decrease every tick (5 seconds = 100 ticks)
            final int totalTicks = 100;
            int taskId = new BukkitRunnable() {
                int ticksElapsed = 0;
                
                @Override
                public void run() {
                    try {
                        ticksElapsed++;
                        
                        // Calculate remaining progress (decreases from 1.0 to 0.0)
                        double progress = 1.0 - ((double) ticksElapsed / totalTicks);
                        
                        if (progress <= 0 || ticksElapsed >= totalTicks) {
                            // Time's up, remove the boss bar
                            removeBossBar(player);
                            this.cancel();
                        } else {
                            // Update boss bar progress
                            BossBar bar = activeBossBars.get(playerId);
                            if (bar != null) {
                                bar.setProgress(Math.max(0.0, progress));
                            } else {
                                // Boss bar was removed externally, cancel task
                                this.cancel();
                            }
                        }
                    } catch (Exception e) {
                        plugin.getLogger().warning("Error updating boss bar for player " + player.getName() + ": " + e.getMessage());
                        this.cancel();
                    }
                }
            }.runTaskTimer(plugin, 1L, 1L).getTaskId(); // Run every tick starting after 1 tick
            
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
            
            // Play XP gain sound
            playXPGainSound(player);
            
            ChatColor skillColor = getChatColorForSkill(skill);
            String skillIcon = getIconForSkill(skill);
            String message = ChatColor.GREEN + "+" + String.format("%.1f", amount) + " " 
                           + skillColor + skillIcon + " " + skill.name() + " XP";
            player.spigot().sendMessage(net.md_5.bungee.api.ChatMessageType.ACTION_BAR, 
                                        net.md_5.bungee.api.chat.TextComponent.fromLegacyText(message));
        } catch (Exception e) {
            plugin.getLogger().warning("Error sending XP gain message to player " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Sends a leaderboard for a specific skill.
     * 
     * @param player The player to send the leaderboard to
     * @param skillType The skill type to show leaderboard for
     * @param entries The leaderboard entries
     */
    public void sendLeaderboard(Player player, SkillType skillType, com.example.skillsplugin.data.PlayerDataManager.LeaderboardEntry[] entries) {
        try {
            if (player == null || !player.isOnline()) {
                return;
            }
            
            ChatColor skillColor = getChatColorForSkill(skillType);
            String skillIcon = getIconForSkill(skillType);
            
            player.sendMessage("");
            player.sendMessage(skillColor + "" + ChatColor.BOLD + "=== " + skillIcon + " " + skillType.name() + " LEADERBOARD ===");
            player.sendMessage("");
            
            if (entries.length == 0) {
                player.sendMessage(ChatColor.GRAY + "No players found.");
                player.sendMessage("");
                return;
            }
            
            for (int i = 0; i < entries.length; i++) {
                com.example.skillsplugin.data.PlayerDataManager.LeaderboardEntry entry = entries[i];
                int rank = i + 1;
                
                // Get player name (try to get from online players first, otherwise use UUID)
                String playerName = null;
                Player targetPlayer = Bukkit.getPlayer(entry.getPlayerId());
                if (targetPlayer != null) {
                    playerName = targetPlayer.getName();
                } else {
                    // Try to get from offline player
                    org.bukkit.OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(entry.getPlayerId());
                    if (offlinePlayer.hasPlayedBefore()) {
                        playerName = offlinePlayer.getName();
                    }
                }
                
                // Fallback to UUID if name not found
                if (playerName == null) {
                    playerName = entry.getPlayerId().toString().substring(0, 8) + "...";
                }
                
                // Format rank with medal emojis for top 3
                String rankDisplay;
                if (rank == 1) {
                    rankDisplay = ChatColor.GOLD + "🥇 #1";
                } else if (rank == 2) {
                    rankDisplay = ChatColor.GRAY + "🥈 #2";
                } else if (rank == 3) {
                    rankDisplay = ChatColor.GOLD + "🥉 #3";
                } else {
                    rankDisplay = ChatColor.GRAY + "#" + rank;
                }
                
                // Check if this is the viewing player
                boolean isViewingPlayer = entry.getPlayerId().equals(player.getUniqueId());
                ChatColor nameColor = isViewingPlayer ? ChatColor.YELLOW : ChatColor.WHITE;
                
                String line = rankDisplay + " " + nameColor + playerName + " " 
                            + ChatColor.GRAY + "- " + skillColor + "Level " + entry.getLevel() 
                            + ChatColor.GRAY + " (" + String.format("%.0f", entry.getExperience()) + " XP)";
                
                player.sendMessage(line);
            }
            
            player.sendMessage("");
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error sending leaderboard to player " + player.getName() + ": " + e.getMessage());
            player.sendMessage(ChatColor.RED + "An error occurred while displaying the leaderboard.");
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
     * Sets the player's display name to show their skill level.
     * Format: [icon level] PlayerName
     * Also saves the preference in the player's profile.
     * 
     * @param player The player whose display name to update
     * @param skill The skill to display
     * @param profile The player's skill profile to save the preference
     */
    public void setSkillDisplayName(Player player, Skill skill, SkillProfile profile) {
        try {
            if (player == null || !player.isOnline()) {
                return;
            }
            
            if (skill == null) {
                plugin.getLogger().warning("Attempted to set display name with null skill for player " + player.getName());
                return;
            }
            
            SkillType type = skill.getType();
            int level = skill.getLevel();
            ChatColor skillColor = getChatColorForSkill(type);
            String skillIcon = getIconForSkill(type);
            
            // Create the display name: [icon level] PlayerName
            String displayName = ChatColor.GRAY + "[" + skillColor + skillIcon + " " + level + ChatColor.GRAY + "] " 
                               + ChatColor.WHITE + player.getName();
            
            player.setDisplayName(displayName);
            player.setPlayerListName(displayName);
            
            // Save the preference in the profile
            if (profile != null) {
                profile.setDisplayedSkill(type);
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error setting skill display name for player " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Removes the skill display from a player's name and clears the preference.
     * 
     * @param player The player whose display name to reset
     * @param profile The player's skill profile to clear the preference
     */
    public void removeSkillDisplayName(Player player, SkillProfile profile) {
        try {
            if (player == null || !player.isOnline()) {
                return;
            }
            
            player.setDisplayName(player.getName());
            player.setPlayerListName(player.getName());
            
            // Clear the preference in the profile
            if (profile != null) {
                profile.setDisplayedSkill(null);
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error removing skill display name for player " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Restores a player's display name based on their saved preference.
     * Called on player join to restore their chosen skill display.
     * 
     * @param player The player whose display name to restore
     * @param profile The player's skill profile containing the preference
     */
    public void restoreDisplayName(Player player, SkillProfile profile) {
        try {
            if (player == null || !player.isOnline() || profile == null) {
                return;
            }
            
            SkillType displayedSkill = profile.getDisplayedSkill();
            if (displayedSkill != null) {
                Skill skill = profile.getSkill(displayedSkill);
                if (skill != null) {
                    setSkillDisplayName(player, skill, profile);
                    plugin.getLogger().fine("Restored display name for player " + player.getName() + " with " + displayedSkill);
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error restoring display name for player " + player.getName() + ": " + e.getMessage());
        }
    }
    
    /**
     * Updates a player's display name if they have a skill displayed and it leveled up.
     * Called after a level-up to keep the display name current.
     * 
     * @param player The player whose display name to update
     * @param profile The player's skill profile
     * @param leveledUpSkill The skill that just leveled up
     */
    public void updateDisplayNameIfNeeded(Player player, SkillProfile profile, SkillType leveledUpSkill) {
        try {
            if (player == null || !player.isOnline() || profile == null) {
                return;
            }
            
            SkillType displayedSkill = profile.getDisplayedSkill();
            if (displayedSkill != null && displayedSkill == leveledUpSkill) {
                Skill skill = profile.getSkill(displayedSkill);
                if (skill != null) {
                    setSkillDisplayName(player, skill, profile);
                    plugin.getLogger().fine("Updated display name for player " + player.getName() + " after leveling up " + leveledUpSkill);
                }
            }
            
        } catch (Exception e) {
            plugin.getLogger().warning("Error updating display name for player " + player.getName() + ": " + e.getMessage());
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
            
            ChatColor skillColor = getChatColorForSkill(type);
            String skillIcon = getIconForSkill(type);
            
            player.sendMessage("");
            player.sendMessage(skillColor + "" + ChatColor.BOLD + "=== " + skillIcon + " " + type.name() + " ===");
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "Level: " + ChatColor.WHITE + level);
            player.sendMessage(ChatColor.YELLOW + "Experience: " + ChatColor.WHITE 
                             + String.format("%.1f", currentXP) + " / " + String.format("%.1f", requiredXP));
            player.sendMessage("");
            player.sendMessage(ChatColor.YELLOW + "Progress: " + createProgressBar(progress, skillColor));
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
        
        ChatColor skillColor = getChatColorForSkill(type);
        String skillIcon = getIconForSkill(type);
        String skillName = skillColor + "" + ChatColor.BOLD + skillIcon + " " + type.name();
        String levelText = ChatColor.YELLOW + "Lvl " + level;
        String progressBar = createMiniProgressBar(progress, skillColor);
        
        return skillName + " " + levelText + " " + progressBar;
    }
    
    /**
     * Formats a single skill line for public viewing (other players).
     * 
     * @param skill The skill to format
     * @return A formatted string with skill name and level
     */
    public String formatSkillLinePublic(Skill skill) {
        SkillType type = skill.getType();
        int level = skill.getLevel();
        
        ChatColor skillColor = getChatColorForSkill(type);
        String skillIcon = getIconForSkill(type);
        String skillName = skillColor + "" + ChatColor.BOLD + skillIcon + " " + type.name();
        String levelText = ChatColor.YELLOW + "Lvl " + level;
        
        return skillName + " " + levelText;
    }
    
    /**
     * Creates a visual progress bar for detailed skill display.
     * 
     * @param progress The progress as a decimal (0.0 to 1.0)
     * @param skillColor The color to use for the filled portion
     * @return A formatted progress bar string
     */
    private String createProgressBar(double progress, ChatColor skillColor) {
        int totalBars = 20;
        int filledBars = (int) (progress * totalBars);
        
        StringBuilder bar = new StringBuilder();
        
        for (int i = 0; i < totalBars; i++) {
            if (i < filledBars) {
                if (i == 0) {
                    bar.append(skillColor);
                }
                bar.append("█");
            } else {
                if (i == filledBars) {
                    bar.append(ChatColor.GRAY);
                }
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
     * @param skillColor The color to use for the filled portion
     * @return A formatted mini progress bar string
     */
    private String createMiniProgressBar(double progress, ChatColor skillColor) {
        int totalBars = 10;
        int filledBars = (int) (progress * totalBars);
        
        StringBuilder bar = new StringBuilder();
        bar.append(ChatColor.GRAY + "[");
        bar.append(skillColor);
        
        for (int i = 0; i < totalBars; i++) {
            if (i < filledBars) {
                bar.append("█");
            } else {
                bar.append(ChatColor.DARK_GRAY + "█");
            }
        }
        
        bar.append(ChatColor.GRAY + "]");
        
        return bar.toString();
    }
    
    /**
     * Plays the level-up sound for a player.
     * 
     * @param player The player to play the sound for
     */
    private void playLevelUpSound(Player player) {
        try {
            if (configManager == null || !configManager.areSoundsEnabled()) {
                return;
            }
            
            String soundName = configManager.getLevelUpSound();
            if (soundName == null || soundName.equalsIgnoreCase("none")) {
                return;
            }
            
            try {
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, 1.0f, 1.0f);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid level-up sound: " + soundName);
            }
        } catch (Exception e) {
            plugin.getLogger().fine("Error playing level-up sound: " + e.getMessage());
        }
    }
    
    /**
     * Plays the XP gain sound for a player.
     * 
     * @param player The player to play the sound for
     */
    private void playXPGainSound(Player player) {
        try {
            if (configManager == null || !configManager.areSoundsEnabled()) {
                return;
            }
            
            String soundName = configManager.getXPGainSound();
            if (soundName == null || soundName.equalsIgnoreCase("none")) {
                return;
            }
            
            try {
                org.bukkit.Sound sound = org.bukkit.Sound.valueOf(soundName);
                player.playSound(player.getLocation(), sound, 0.5f, 1.2f);
            } catch (IllegalArgumentException e) {
                plugin.getLogger().warning("Invalid XP gain sound: " + soundName);
            }
        } catch (Exception e) {
            plugin.getLogger().fine("Error playing XP gain sound: " + e.getMessage());
        }
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
    
    /**
     * Gets the appropriate chat color for a skill type.
     * 
     * @param skill The skill type
     * @return The chat color for this skill
     */
    private ChatColor getChatColorForSkill(SkillType skill) {
        switch (skill) {
            case MINING:
                return ChatColor.DARK_AQUA;
            case WOODCUTTING:
                return ChatColor.DARK_GREEN;
            case COMBAT:
                return ChatColor.RED;
            case FARMING:
                return ChatColor.GOLD;
            case FISHING:
                return ChatColor.AQUA;
            case ENCHANTING:
                return ChatColor.LIGHT_PURPLE;
            case TRADING:
                return ChatColor.GREEN;
            default:
                return ChatColor.WHITE;
        }
    }
    
    /**
     * Gets the appropriate icon/symbol for a skill type.
     * 
     * @param skill The skill type
     * @return The icon string for this skill
     */
    private String getIconForSkill(SkillType skill) {
        switch (skill) {
            case MINING:
                return "⛏"; // Pickaxe
            case WOODCUTTING:
                return "🪓"; // Axe
            case COMBAT:
                return "⚔"; // Sword
            case FARMING:
                return "🌾"; // Wheat
            case FISHING:
                return "🎣"; // Fishing rod
            case ENCHANTING:
                return "✨"; // Sparkles
            case TRADING:
                return "💰"; // Money bag
            default:
                return "⭐"; // Star
        }
    }
}
