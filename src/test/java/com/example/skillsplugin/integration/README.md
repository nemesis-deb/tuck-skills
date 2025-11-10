# Integration Tests

This directory contains integration tests for the Skills Plugin. Due to the complexity of mocking Bukkit's final classes and methods (JavaPlugin, PluginCommand, etc.), these tests are designed to be run in a test server environment or with PowerMock/MockBukkit.

## Test Coverage

The integration tests cover the following scenarios:

### 1. Complete XP Gain Flow from Event to Level-Up
- Tests the full pipeline: Event → XP Calculation → Data Update → Level-up → UI Feedback
- Verifies that players gain XP when performing skill-related actions
- Confirms level-ups occur when XP thresholds are reached
- Validates UI feedback (messages and boss bars) are triggered correctly

### 2. Data Persistence Across Save/Load Cycles
- Tests that player skill data is correctly saved to disk
- Verifies data integrity after multiple save/load cycles
- Confirms that XP and levels are preserved across server restarts
- Tests the PlayerDataManager cache and persistence mechanisms

### 3. Bonus Application During Events
- Verifies that skill bonuses are correctly calculated based on player levels
- Tests that bonuses scale linearly with skill levels
- Confirms bonus multipliers are applied during gameplay
- Validates that config changes affect bonus calculations

### 4. Multiple Players with Concurrent Actions
- Tests thread safety when multiple players perform actions simultaneously
- Verifies data integrity under concurrent load
- Confirms that player profiles don't interfere with each other
- Tests cache management with multiple concurrent players

### 5. Config Reload Applying Changes
- Tests that configuration changes are applied without server restart
- Verifies that XP multipliers, level formulas, and bonus rates can be reloaded
- Confirms that existing player data is not affected by config changes
- Tests that new calculations use updated config values

## Running Integration Tests

Due to Bukkit's architecture, these tests require special setup:

### Option 1: MockBukkit (Recommended)
Add MockBukkit to your test dependencies:
```xml
<dependency>
    <groupId>com.github.seeseemelk</groupId>
    <artifactId>MockBukkit-v1.20</artifactId>
    <version>3.9.0</version>
    <scope>test</scope>
</dependency>
```

### Option 2: Test Server
Run tests on an actual test Minecraft server with the plugin loaded.

### Option 3: PowerMock
Use PowerMock to mock final classes, though this is more complex and slower.

## Manual Testing Checklist

Until automated integration tests are fully implemented, use this checklist for manual testing:

- [ ] Join server and verify new profile creation
- [ ] Perform each skill action and verify XP gain
- [ ] Level up each skill and verify boss bar display
- [ ] Test `/skills` command and all subcommands
- [ ] Reload config and verify changes apply
- [ ] Disconnect and reconnect to verify data persistence
- [ ] Test bonuses for each skill at various levels
- [ ] Test with multiple players simultaneously
- [ ] Verify data integrity after server restart
- [ ] Test config reload with players online

## Test Implementation Notes

The `SkillsPluginIntegrationTest.java` file contains the test structure, but may require MockBukkit or similar framework to run successfully. The tests are written to be comprehensive once the mocking framework is properly configured.

Key challenges:
- `JavaPlugin` has many final methods that cannot be mocked with standard Mockito
- `PluginCommand` is a final class
- `FileConfiguration` methods may be final
- Bukkit's scheduler and event system require special handling

## Future Improvements

- Integrate MockBukkit for full test automation
- Add performance benchmarks for concurrent operations
- Add stress tests with many players and actions
- Add tests for edge cases and error conditions
- Add tests for data migration scenarios
