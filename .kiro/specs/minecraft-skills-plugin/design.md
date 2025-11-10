# Design Document

## Overview

This design document outlines the architecture and implementation details for a Minecraft skills plugin built on the Spigot/Paper API. The plugin implements a comprehensive skill leveling system with 7 distinct skills, experience tracking, level progression, data persistence, and visual feedback through boss bars and messages.

The plugin follows a modular architecture with clear separation between event handling, skill logic, data management, and player feedback systems.

## Architecture

### High-Level Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                      Main Plugin Class                       │
│  (Initialization, Command Registration, Config Loading)     │
└──────────────────┬──────────────────────────────────────────┘
                   │
        ┌──────────┴──────────┬──────────────┬────────────────┐
        │                     │              │                │
┌───────▼────────┐  ┌────────▼────────┐  ┌──▼──────┐  ┌─────▼──────┐
│ Event Listener │  │  Skill Manager  │  │ Config  │  │  Commands  │
│    System      │  │                 │  │ Manager │  │            │
└───────┬────────┘  └────────┬────────┘  └─────────┘  └─────┬──────┘
        │                    │                               │
        │           ┌────────▼────────┐                      │
        │           │  Player Data    │                      │
        └──────────►│    Manager      │◄─────────────────────┘
                    └────────┬────────┘
                             │
                    ┌────────▼────────┐
                    │   Data Storage  │
                    │  (JSON/SQLite)  │
                    └─────────────────┘
```

### Core Components

1. **Main Plugin Class**: Entry point, handles plugin lifecycle
2. **Event Listener System**: Captures Minecraft events and triggers skill experience
3. **Skill Manager**: Manages skill definitions, experience calculations, and level-ups
4. **Player Data Manager**: Handles player skill profiles and data operations
5. **Data Storage Layer**: Persists player data to disk
6. **Config Manager**: Loads and manages plugin configuration
7. **Command Handler**: Processes player commands for viewing skills
8. **UI Manager**: Handles boss bars, action bars, and chat messages

## Components and Interfaces

### 1. Skill System

#### Skill Enum
```java
public enum SkillType {
    MINING,
    WOODCUTTING,
    COMBAT,
    FARMING,
    FISHING,
    ENCHANTING,
    TRADING
}
```

#### Skill Class
Represents a single skill with its current state:
- `SkillType type`: The skill identifier
- `int level`: Current skill level (starts at 1)
- `double experience`: Current experience points
- `double getRequiredExperience()`: Calculates XP needed for next level
- `boolean addExperience(double amount)`: Adds XP and returns true if leveled up
- `void levelUp()`: Handles level-up logic

#### SkillProfile Class
Represents a player's complete skill set:
- `UUID playerId`: Player identifier
- `Map<SkillType, Skill> skills`: All skills for this player
- `Skill getSkill(SkillType type)`: Retrieves specific skill
- `void initializeSkills()`: Creates all skills at level 1

### 2. Experience System

#### ExperienceCalculator Class
Handles experience award calculations:
- `double calculateMiningXP(Material block)`: Returns XP for mining blocks
- `double calculateWoodcuttingXP(Material log)`: Returns XP for chopping wood
- `double calculateCombatXP(EntityType mob)`: Returns XP for killing mobs
- `double calculateFarmingXP(Material crop)`: Returns XP for harvesting crops
- `double calculateFishingXP(ItemStack catch)`: Returns XP for fishing
- `double calculateEnchantingXP(int enchantLevel)`: Returns XP for enchanting
- `double calculateTradingXP(MerchantRecipe trade)`: Returns XP for trading

All calculations are configurable through the config file with base values and multipliers.

#### LevelFormula Class
Calculates experience requirements for levels:
- `double getRequiredExperience(int level)`: Returns XP needed for a specific level
- Default formula: `baseXP * (level ^ exponent)` where baseXP = 100, exponent = 1.5
- Configurable through config file

### 3. Event Handling

#### SkillEventListener Class
Listens to Minecraft events and awards experience:
- `onBlockBreak(BlockBreakEvent)`: Mining and Woodcutting
- `onEntityDeath(EntityDeathEvent)`: Combat
- `onPlayerHarvest(BlockBreakEvent)`: Farming (crop-specific)
- `onPlayerFish(PlayerFishEvent)`: Fishing
- `onEnchantItem(EnchantItemEvent)`: Enchanting
- `onVillagerTrade(VillagerAcquireTradeEvent)`: Trading

Each handler:
1. Validates the event (not cancelled, valid action)
2. Determines XP amount via ExperienceCalculator
3. Awards XP to player via PlayerDataManager
4. Triggers UI feedback

### 4. Data Management

#### PlayerDataManager Class
Central manager for player skill data:
- `SkillProfile getProfile(UUID playerId)`: Gets or creates player profile
- `void saveProfile(UUID playerId)`: Saves profile to storage
- `void loadProfile(UUID playerId)`: Loads profile from storage
- `void awardExperience(Player player, SkillType skill, double amount)`: Awards XP and handles level-ups
- `Map<UUID, SkillProfile> cache`: In-memory cache of active players

#### DataStorage Interface
Abstract storage layer with implementations:
- `void save(UUID playerId, SkillProfile profile)`: Persists data
- `SkillProfile load(UUID playerId)`: Retrieves data
- `boolean exists(UUID playerId)`: Checks if data exists

**Implementations:**
- `JsonDataStorage`: Stores data in JSON files (default)
- `SQLiteDataStorage`: Stores data in SQLite database (optional)

File structure for JSON:
```
plugins/SkillsPlugin/playerdata/{uuid}.json
```

### 5. Configuration

#### Config Structure (config.yml)
```yaml
# Experience multipliers per skill
experience:
  mining: 1.0
  woodcutting: 1.0
  combat: 1.0
  farming: 1.0
  fishing: 1.0
  enchanting: 1.0
  trading: 1.0

# Level formula settings
leveling:
  base-xp: 100
  exponent: 1.5

# Skill bonuses
bonuses:
  enabled: true
  mining:
    double-drop-chance-per-level: 0.5  # 0.5% per level
  woodcutting:
    double-drop-chance-per-level: 0.5
  combat:
    damage-bonus-per-level: 0.5  # 0.5% per level
  farming:
    double-crop-chance-per-level: 0.5
  fishing:
    treasure-chance-per-level: 0.3
  enchanting:
    cost-reduction-per-level: 0.5  # 0.5% per level
  trading:
    discount-per-level: 0.3  # 0.3% per level

# UI settings
ui:
  boss-bar-duration: 5  # seconds
  show-xp-gain-messages: true
  
# Enabled skills
enabled-skills:
  - MINING
  - WOODCUTTING
  - COMBAT
  - FARMING
  - FISHING
  - ENCHANTING
  - TRADING

# Data storage type
storage:
  type: JSON  # JSON or SQLITE
```

### 6. Skill Bonuses

#### BonusManager Class
Applies skill-based bonuses during gameplay:
- `void applyMiningBonus(Player player, Block block)`: Chance for double drops
- `void applyWoodcuttingBonus(Player player, Block block)`: Chance for extra logs
- `double getCombatDamageMultiplier(Player player)`: Returns damage multiplier
- `void applyFarmingBonus(Player player, Block crop)`: Chance for extra crops
- `void applyFishingBonus(Player player, ItemStack catch)`: Chance for better loot
- `int getEnchantingCostReduction(Player player, int baseCost)`: Reduces XP cost
- `double getTradingDiscount(Player player)`: Returns price multiplier

Each bonus method:
1. Gets player's skill level
2. Calculates bonus percentage from config
3. Applies random chance or direct multiplier
4. Returns result or modifies event

### 7. User Interface

#### UIManager Class
Handles all player-facing UI elements:
- `void showLevelUpBossBar(Player player, SkillType skill, int newLevel)`: Displays boss bar
- `void sendXPGainMessage(Player player, SkillType skill, double amount)`: Action bar message
- `void sendSkillsOverview(Player player)`: Sends formatted skills list
- `void sendSkillDetails(Player player, SkillType skill)`: Sends detailed skill info

**Boss Bar Implementation:**
- Creates BossBar with skill name and level
- Sets color based on skill type
- Schedules removal after 5 seconds using BukkitRunnable
- Automatically cleans up on player disconnect

**Message Formatting:**
- Uses color codes for visual appeal
- Progress bars for XP visualization
- Consistent formatting across all messages

### 8. Commands

#### SkillsCommand Class
Handles `/skills` command with subcommands:
- `/skills` - Shows all skills overview
- `/skills <skillname>` - Shows detailed info for specific skill
- `/skills reload` - Reloads config (admin only)

Command structure uses Bukkit's command framework with tab completion.

## Data Models

### SkillProfile JSON Structure
```json
{
  "playerId": "uuid-string",
  "skills": {
    "MINING": {
      "level": 15,
      "experience": 450.5
    },
    "WOODCUTTING": {
      "level": 12,
      "experience": 230.0
    },
    ...
  },
  "lastUpdated": "2025-11-10T12:00:00Z"
}
```

### SQLite Schema (Optional)
```sql
CREATE TABLE player_skills (
    player_id TEXT NOT NULL,
    skill_type TEXT NOT NULL,
    level INTEGER NOT NULL DEFAULT 1,
    experience REAL NOT NULL DEFAULT 0,
    PRIMARY KEY (player_id, skill_type)
);

CREATE INDEX idx_player_id ON player_skills(player_id);
```

## Error Handling

### Exception Hierarchy
- `SkillPluginException`: Base exception
  - `DataLoadException`: Failed to load player data
  - `DataSaveException`: Failed to save player data
  - `ConfigException`: Configuration errors

### Error Handling Strategy
1. **Data Loading Failures**: Create new profile with defaults, log warning
2. **Data Saving Failures**: Retry up to 3 times, keep data in memory, log error
3. **Config Errors**: Use default values, log error, notify admins
4. **Event Processing Errors**: Log error, continue operation (don't break gameplay)

### Logging
- Use Bukkit's logger with appropriate levels
- INFO: Plugin lifecycle events, successful operations
- WARNING: Recoverable errors, fallback to defaults
- SEVERE: Critical errors that affect functionality

## Testing Strategy

### Unit Tests
1. **Skill Logic Tests**
   - Test experience addition and level-up calculations
   - Test level formula with various inputs
   - Test skill initialization

2. **Experience Calculator Tests**
   - Test XP calculations for all skill types
   - Test config multiplier application
   - Test edge cases (null materials, invalid types)

3. **Bonus Calculator Tests**
   - Test bonus percentage calculations
   - Test random chance applications
   - Test config-based bonus scaling

4. **Data Serialization Tests**
   - Test JSON serialization/deserialization
   - Test data integrity after save/load cycle
   - Test handling of corrupted data

### Integration Tests
1. **Event Flow Tests**
   - Mock Bukkit events and verify XP is awarded
   - Test complete flow from event to level-up
   - Test bonus application during events

2. **Data Persistence Tests**
   - Test save/load cycle with real files
   - Test concurrent access scenarios
   - Test data migration between storage types

3. **Command Tests**
   - Test command execution and output
   - Test permission checks
   - Test tab completion

### Manual Testing Checklist
- [ ] Join server and verify new profile creation
- [ ] Perform each skill action and verify XP gain
- [ ] Level up each skill and verify boss bar display
- [ ] Test `/skills` command and all subcommands
- [ ] Reload config and verify changes apply
- [ ] Disconnect and reconnect to verify data persistence
- [ ] Test bonuses for each skill at various levels
- [ ] Test with multiple players simultaneously

## Performance Considerations

1. **Caching**: Keep active player profiles in memory, only load from disk on join
2. **Async Operations**: Save data asynchronously to avoid blocking main thread
3. **Event Optimization**: Minimize calculations in event handlers
4. **Boss Bar Cleanup**: Properly remove boss bars to prevent memory leaks
5. **Batch Saves**: Consider batching saves during server shutdown

## Dependencies

- **Spigot/Paper API**: 1.20+ (or configurable version)
- **Java**: 17+ (modern Minecraft server requirement)
- **Gson**: For JSON serialization (included in Spigot)
- **SQLite JDBC** (optional): For SQLite storage implementation

## Plugin Metadata (plugin.yml)

```yaml
name: SkillsPlugin
version: 1.0.0
main: com.example.skillsplugin.SkillsPlugin
api-version: 1.20
author: [Author]
description: A comprehensive skill leveling system

commands:
  skills:
    description: View your skills
    usage: /skills [skillname]
    aliases: [skill, sk]

permissions:
  skills.use:
    description: Allows using the skills command
    default: true
  skills.reload:
    description: Allows reloading the config
    default: op
```
