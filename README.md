# RecruitsAddon - Advanced Recruit Command System

**A comprehensive addon for the Recruits mod that provides advanced tactical control over your recruit units.**

![Minecraft](https://img.shields.io/badge/Minecraft-1.20.1-green.svg)
![Forge](https://img.shields.io/badge/Forge-47.3.0-orange.svg)
![License](https://img.shields.io/badge/License-MIT-blue.svg)

## 🎯 Overview

RecruitsAddon transforms the way you command your recruits by providing:
- **Unlimited Range Commands** - Control your recruits from anywhere in the world
- **Group-Based Operations** - Target specific recruit groups for tactical precision
- **Formation System** - Organize recruits in LINE, SQUARE, CIRCLE, and WEDGE formations
- **Advanced Combat AI & Auto-Teleport** - Coordinated group retaliation, smart defensive behavior, and auto-teleportation for lagging recruits
- **Enhanced GUI Integration** - Seamless integration with the existing Recruits mod interface

## 📋 Requirements

- **Minecraft**: 1.20.1
- **Minecraft Forge**: 47.3.0+
- **TalhaNation's Recruits Mod**: Latest version for 1.20.1

## 🚀 Installation

1. Download the latest release from the [Releases](../../releases) page
2. Place the `.jar` file in your `mods` folder
3. Ensure you have the Recruits mod installed
4. Launch Minecraft with Forge

## 🎮 How to Use

### Basic Commands

All commands work at **unlimited distance** - no need to be near your recruits!

#### March Commands
```
/recruits march <x> <y> <z> [group <groupId>] [formation <type>]
```
- **Basic**: `/recruits march 100 64 -200` - March all recruits to coordinates
- **Group**: `/recruits march 0 64 0 group 1` - March only group 1
- **Formation**: `/recruits march 50 70 -100 formation LINE` - March in line formation
- **Advanced**: `/recruits march -50 80 150 group 2 formation SQUARE` - Group 2 in square formation

#### Raid Commands
```
/recruits raid <x> <y> <z> [group <groupId>] [formation <type>]
```
- **Basic**: `/recruits raid -100 65 300` - Send recruits to raid coordinates
- **Group**: `/recruits raid 200 70 -50 group 3` - Send group 3 to raid
- **Formation**: `/recruits raid 0 64 -200 formation WEDGE` - Raid in wedge formation

#### Control Commands
```
/recruits cancel     # Cancel all operations, recruits return to you
/recruits stop       # Make recruits hold defensive positions with shields
/recruits follow [player] [formation]  # Follow you or another player
/recruits attack <player>  # Attack a specific player (max 125 blocks)
```

### Formation Types

- **LINE** - Single line formation, perfect for narrow passages
- **SQUARE** - Grid formation, balanced defense and offense
- **CIRCLE** - Circular formation, excellent for area defense
- **WEDGE** - V-shaped formation, ideal for breakthrough attacks

### Coordinate System

**Absolute Coordinates**: Use exact numbers
```
/recruits march 100 64 -200
```

**Relative Coordinates**: Use `~` for relative positioning
```
/recruits march ~ ~5 ~10    # 5 blocks up, 10 blocks forward from your position
/recruits march ~-20 ~ ~    # 20 blocks to the left of your position
```

## 🎛️ GUI Features

### Enhanced Recruit Command GUI

Access through the existing Recruits mod interface to find new "Group Commands" section:

**Quick Action Buttons:**
- **March Here** - Send active groups to your crosshair target
- **Raid Here** - Send active groups to raid your crosshair target
- **Cancel** - Instantly recall all recruits to you
- **Stop** - Make recruits hold defensive positions

**Chat Prefill Buttons:**
- **March to...** - Prefills `/recruits march group X ~ ~ ~` for easy editing
- **Raid at...** - Prefills `/recruits raid group X ~ ~ ~` for easy editing

*Note: Buttons automatically detect your active recruit groups and target them specifically.*

## ⚔️ Combat Features

### March vs Raid Behavior

**March Mode (Defensive)**:
- Recruits move to destination in neutral state
- Will defend if attacked but continue mission
- Maintains formation during movement
- Suitable for repositioning and defensive movements

**Raid Mode (Aggressive)**:
- Two-phase operation: March → Raid
- Recruits march to destination, then become aggressive
- Attacks hostile mobs and players at destination
- Respects entity blacklist configuration

### Group Retaliation System

When any recruit in a marching group is attacked:
1. **Entire group** immediately targets the attacker
2. Group coordinates to eliminate the threat
3. After 5 seconds, automatically **resumes original march**
4. You receive notifications about the engagement

### Defensive Commands

**Stop Command**:
- Recruits raise shields and enter guard mode
- Only retaliate when directly attacked
- Multiple shield activation methods ensure reliability
- Perfect for holding strategic positions

**Cancel Command**:
- Comprehensive AI state clearing
- Multiple pathfinding methods ensure recruits return
- Cancels any active raid missions
- Robust error handling prevents failures

## ⚙️ Configuration

Configuration file: `config/recruits-addon.toml` (auto-generated on first run)

### Key Settings

```toml
[commands]
# Maximum distance for attack commands (blocks)
attackCommandMaxDistance = 125
# Minimum distance required for follow commands (blocks) 
followCommandMinDistance = 50

[raids]
# Enable raid notifications
enableRaidNotifications = true
# Notify when recruits reach raid destination
notifyOnDestinationReached = true
# Notify when recruits engage in combat
notifyOnCombatStart = true

[blacklist]
# Protect villagers from raid attacks
blacklistVillagers = true
# Protect pets from raid attacks
blacklistPets = true
# Prevent recruits from attacking creepers (explosion protection)
blacklistCreepers = true
```

### Entity Blacklist

Customize which entities recruits should avoid during raids:
```toml
[blacklist.entities]
entityBlacklist = [
    "minecraft:villager",
    "minecraft:iron_golem", 
    "minecraft:cat",
    "minecraft:wolf",
    "minecraft:creeper"
]
```

## 🎯 Advanced Usage Examples

### Tactical Scenarios

**Base Defense Setup**:
```
/recruits march -100 64 -100 group 1 formation CIRCLE
/recruits march 100 64 100 group 2 formation SQUARE  
/recruits stop
```

**Coordinated Attack**:
```
/recruits march 0 64 -200 group 1 formation WEDGE
/recruits march 50 64 -200 group 2 formation LINE
/recruits raid 0 64 -150 group 1
/recruits raid 0 64 -150 group 2
```

**Escort Mission**:
```
/recruits follow @p formation CIRCLE
/recruits march ~ ~ ~20 formation LINE
```

### Group Management

**Multi-Group Operations**:
```
# Send different groups to different locations
/recruits march 100 64 0 group 1 formation LINE
/recruits march -100 64 0 group 2 formation SQUARE
/recruits raid 0 64 200 group 3 formation WEDGE

# Recall specific group
/recruits cancel group 1

# Stop all groups
/recruits stop
```

## 🛠️ Troubleshooting

### Common Issues

**Q: Recruits not responding to commands**
- Ensure recruits are actually owned by you
- Check that groups are not disabled in the GUI
- Verify recruits are not stuck or in combat

**Q: GUI buttons not working**
- Make sure you have active recruit groups
- Check that recruits are alive and not disabled
- Restart the GUI if buttons appear grayed out

**Q: Distance limitations**
- This mod removes ALL distance limitations
- Commands work from anywhere in the world
- If recruits seem unresponsive, they may be in unloaded chunks

**Q: Recruits not raising shields on stop**
- The mod tries multiple methods to activate shields
- Ensure recruits have shields equipped
- Some recruit types may not support shield mechanics

### Debug Information

Enable debug logging in your `log4j2.xml`:
```xml
<Logger level="debug" name="com.yourname.myforgemod"/>
```

## 🤝 Compatibility

**Compatible Mods:**
- TalhaNation's Recruits (required)
- Most other recruit/army mods
- Performance optimization mods

**Potential Conflicts:**
- Other mods that modify recruit AI
- Mods that override command registration

## 📝 Version History

See [CHANGELOG.md](CHANGELOG.md) for detailed version history and update notes.

## 🐛 Bug Reports & Suggestions

Found a bug or have a feature request?
- Check existing [Issues](../../issues)
- Create a new issue with detailed information
- Include your Minecraft and Forge versions
- Provide log files if experiencing crashes

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- **TalhaNation** for the excellent base Recruits mod
- The Minecraft Forge team for the modding framework
- The community for feedback and testing

---

**Enjoy commanding your recruits with tactical precision! ⚔️** 
