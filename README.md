# ZoneMusicPlayer Plugin

⚠️ **Alert: Performance Warning** ⚠️  
This plugin currently reacts to every player movement, which can lead to performance issues on large servers or with many players. To improve efficiency, the plugin should ideally trigger events only when a player **changes region or biome**, instead of checking on every movement. Future updates may address this limitation.

---

## ZoneMusicPlayer

ZoneMusicPlayer is a Minecraft plugin that allows playing custom music based on the region, biome, or biome group a player is in. If no specific music is found, a global fallback music will play.

## Features

- 🎵 **Regional Music**: Play specific music when a player enters a defined region.
- 🌍 **Biome Music**: Assign specific music to individual biomes.
- 🌳 **Biome Groups**: Group biomes together and assign shared music.
- 🎧 **Global Music**: Play default music when no other matches are found.
- 🔧 **Administrative Commands**: Reload the configuration, play sounds for players, or toggle debug mode.

---

## Dependencies

- **WorldGuard**
- **WorldEdit/FAWE (Fast Async WorldEdit)**

---

## Installation

1. Download the plugin JAR file.
2. Place the JAR file into your server's `plugins` folder.
3. Start or restart your server to generate the default configuration file.

---

## Configuration

The configuration file is located at `plugins/ZoneMusicPlayer/config.json`. Below is an example:

```json
{
  "min_delay": 5,
  "max_delay": 20,
  "biome_groups": {
    "FOREST_GROUP": ["FOREST", "TAIGA", "DARK_FOREST"],
    "DESERT_GROUP": ["DESERT", "BADLANDS"]
  },
  "musics": [
    {
      "name": "custom:region_spawn_music",
      "duration": 120,
      "region_id": "spawn"
    },
    {
      "name": "custom:forest_theme",
      "duration": 100,
      "biome": "FOREST"
    },
    {
      "name": "custom:desert_theme",
      "duration": 120,
      "biome_group": "DESERT_GROUP"
    },
    {
      "name": "custom:global_theme",
      "duration": 150
    }
  ]
}
```

---

## Commands

| Command | Description |
|---------|-------------|
| `/zonemusic help` | Show a list of available commands. |
| `/zonemusic reload` | Reload the plugin configuration. |
| `/zonemusic debug` | Toggle debug mode for the executing player. |
| `/zonemusic play <player> <sound> <category>` | Play a custom sound for a specific player. |

### Examples:
1. **Reload configuration**:
   ```
   /zonemusic reload
   ```
2. **Toggle debug mode**:
   ```
   /zonemusic debug
   ```
3. **Play custom sound**:
   ```
   /zonemusic play PlayerName custom:my_sound music
   ```

---

## Permissions

| Permission              | Description                                |
|--------------------------|--------------------------------------------|
| `zonemusic.reload`       | Allows reloading the plugin configuration. |
| `zonemusic.debug`        | Allows toggling debug mode.                |
| `zonemusic.play`         | Allows playing custom sounds for players.  |

---

## How It Works

1. **Region Detection**:
    - Music is prioritized if the player is in a specific region.

2. **Biome and Biome Group Detection**:
    - If no region music exists, the plugin checks for music assigned to the player's biome or biome group.

3. **Global Fallback**:
    - If no region, biome, or group music is found, a global track is played.

4. **Debug Mode**:
    - Provides detailed messages about which context and music are being used for testing and troubleshooting.

---

## Known Issues

⚠️ **Performance Warning**
- The plugin currently reacts to every player movement (`PlayerMoveEvent`). This can impact server performance, especially on servers with many players.
- **Improvement Suggestion**: Create events to detect when a player **changes region** or **enters a new biome** instead of reacting to every block movement.

---

## Contributing and Forking

ZoneMusicPlayer is licensed under the **MIT License**, which means it is 100% free and open source. You are encouraged to:
- 🎉 **Fork** the repository.
- 🔧 **Modify** the code to fit your needs.
- 🔄 **Contribute** improvements back to the community via pull requests.

There are no restrictions on usage, distribution, or modification. Feel free to customize and adapt this plugin as you wish!

---

## License

ZoneMusicPlayer is licensed under the **MIT License**, which allows for free use, modification, and distribution. A full copy of the license can be found in the project's repository.
