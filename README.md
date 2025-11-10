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
- `/skills reload` - Reload the configuration (requires `skills.reload` permission)

## Permissions

- `skills.use` - Allows using the skills command (default: true)
- `skills.reload` - Allows reloading the config (default: op)

## Requirements

- Java 17 or higher
- Spigot/Paper 1.20.1 or higher
