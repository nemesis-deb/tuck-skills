# Data Persistence Layer

This package contains the data persistence implementation for the Skills Plugin.

## Components

### DataStorage Interface
The main interface defining storage operations:
- `save(UUID, SkillProfile)` - Saves a player's skill profile
- `load(UUID)` - Loads a player's skill profile
- `exists(UUID)` - Checks if a profile exists
- `initialize()` - Initializes the storage system

### JsonDataStorage
JSON-based implementation of DataStorage:
- Stores player data as JSON files in `plugins/SkillsPlugin/playerdata/`
- Each player has their own file: `{uuid}.json`
- Uses Gson for serialization/deserialization
- Handles missing skills gracefully by creating defaults

### DataStorageException
Custom exception for storage-related errors.

## JSON File Format

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
  "lastUpdated": 1699651200000
}
```

## Usage Example

```java
// Initialize storage
File dataDirectory = new File(plugin.getDataFolder(), "playerdata");
DataStorage storage = new JsonDataStorage(dataDirectory);
storage.initialize();

// Save a profile
UUID playerId = player.getUniqueId();
SkillProfile profile = new SkillProfile(playerId);
storage.save(playerId, profile);

// Load a profile
SkillProfile loadedProfile = storage.load(playerId);

// Check if profile exists
if (storage.exists(playerId)) {
    // Profile exists
}
```

## Testing

Comprehensive tests are available in `JsonDataStorageTest.java` covering:
- Directory initialization
- Save and load operations
- Data integrity across multiple save/load cycles
- Multiple player support
- High level and experience values
- Missing skill handling
- Error conditions
