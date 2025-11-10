# Event Listeners

## SkillEventListener

The `SkillEventListener` class handles all Minecraft events related to skill progression. It listens to various gameplay events and awards experience points to players based on their actions.

### Implemented Event Handlers

1. **BlockBreakEvent** - Handles Mining, Woodcutting, and Farming XP
   - Mining: Awards XP for breaking ores and stone
   - Woodcutting: Awards XP for breaking logs
   - Farming: Awards XP for harvesting mature crops

2. **EntityDeathEvent** - Handles Combat XP
   - Awards XP when a player kills a mob
   - Only awards XP if the entity was killed by a player

3. **PlayerFishEvent** - Handles Fishing XP
   - Awards XP for successful catches (CAUGHT_FISH state)
   - Different XP amounts based on what was caught

4. **EnchantItemEvent** - Handles Enchanting XP
   - Awards XP based on the enchantment level cost

5. **InventoryClickEvent** - Handles Trading XP
   - Awards XP when a player completes a trade with a villager
   - Detects merchant inventory and result slot clicks

### Features

- **Error Handling**: All event handlers are wrapped in try-catch blocks to prevent crashes
- **UI Feedback**: Automatically triggers XP gain messages and level-up boss bars
- **Mature Crop Detection**: Only awards farming XP for fully grown crops
- **Integration**: Seamlessly integrates with PlayerDataManager, ExperienceCalculator, and UIManager

### Registration

The event listener needs to be registered in the main plugin class during the `onEnable()` phase:

```java
SkillEventListener eventListener = new SkillEventListener(
    playerDataManager,
    experienceCalculator,
    uiManager,
    getLogger()
);
getServer().getPluginManager().registerEvents(eventListener, this);
```

### Testing

Comprehensive integration tests are provided in `SkillEventListenerTest.java` covering:
- All skill types and their respective events
- Level-up scenarios
- Error handling
- Edge cases (immature crops, non-player kills, etc.)
