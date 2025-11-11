# Skills Plugin

A comprehensive skill leveling system for Minecraft servers running Spigot/Paper 1.20+.

## Features

- 7 different skills: Mining, Woodcutting, Combat, Farming, Fishing, Enchanting, Trading
- Experience gain through gameplay activities
- Level progression with configurable formulas
- Skill bonuses that scale with level
- Visual feedback with boss bars and messages
- Persistent data storage
- Fully configurable

## Building

This project uses Maven. To build:

```bash
mvn clean package
```

The compiled JAR will be in the `target/` directory as `SkillsPlugin-1.0.0.jar`.

### Build Requirements

- Java 17 or higher
- Maven 3.6 or higher

### Build Options

- **Standard build**: `mvn clean package`
- **Skip tests**: `mvn clean package -DskipTests`
- **Clean only**: `mvn clean`

The build process:
1. Compiles Java source files with Java 17 compatibility
2. Processes resources (plugin.yml, config.yml)
3. Runs unit tests (unless skipped)
4. Creates a shaded JAR with all dependencies included
5. Outputs the final distributable JAR to `target/SkillsPlugin-1.0.0.jar`

## Installation

1. Build the plugin or download the latest release
2. Place the JAR file in your server's `plugins/` directory
3. Restart the server
4. Configure the plugin in `plugins/SkillsPlugin/config.yml`

## Commands

- `/skills` - View all your skills
- `/skills <skillname>` - View details for a specific skill
- `/skills top <skillname>` - View the top 10 players for a specific skill
- `/skills display <skillname>` - Display your skill level in your name (e.g., `[⛏ 15] PlayerName`)
- `/skills display off` - Remove skill display from your name
- `/skills reload` - Reload the configuration (requires `skills.reload` permission)

## Permissions

- `skills.use` - Allows using the skills command (default: true)
- `skills.reload` - Allows reloading the config (default: op)

## Configuration

The plugin is highly configurable through `plugins/SkillsPlugin/config.yml`.

### Leveling Difficulty

The XP required per level is calculated using: `baseXP * (level ^ exponent)`

**Exponent values and their effects:**
- `1.0` = Linear (100 XP per level, very easy)
- `1.2` = Gentle curve (casual servers)
- `1.25` = Moderate curve (default, balanced)
- `1.3` = Steeper curve (competitive servers)
- `1.5` = Very steep (hardcore servers)
- `2.0` = Extreme difficulty

**Example XP requirements with different exponents:**

| Level | Exponent 1.2 | Exponent 1.25 | Exponent 1.3 | Exponent 1.5 |
|-------|--------------|---------------|--------------|--------------|
| 1→2   | 100 XP       | 100 XP        | 100 XP       | 100 XP       |
| 10→11 | 251 XP       | 298 XP        | 355 XP       | 3,162 XP     |
| 20→21 | 464 XP       | 595 XP        | 764 XP       | 8,944 XP     |
| 50→51 | 1,096 XP     | 1,778 XP      | 2,885 XP     | 35,355 XP    |

### Other Settings

- **XP Multipliers**: Adjust XP gain per skill (1.0 = normal, 2.0 = double XP)
- **Skill Bonuses**: Configure bonus percentages for each skill
- **UI Settings**: Customize boss bar duration and XP messages
- **Enabled Skills**: Enable/disable specific skills

## Requirements

- Java 17 or higher
- Spigot/Paper 1.20.1 or higher
