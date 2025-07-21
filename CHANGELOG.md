# RecruitsAddon - Advanced Recruit Command System

## Version 1.0.0 - Major Release

### 🎯 **New Features**

#### **Advanced Command System**
- **Group-Based Commands**: Commands now target specific recruit groups instead of all nearby recruits
- **Formation Support**: Added LINE, SQUARE, CIRCLE, and WEDGE tactical formations for organized movement
- **Unlimited Range**: All recruit commands work at any distance - no more proximity limitations!
- **Enhanced GUI Integration**: Seamless integration with the existing Recruits mod GUI system

#### **New Commands Added**
- `/recruits march x y z [group groupId] [formation]` - March recruits to coordinates with optional group targeting and formations
- `/recruits raid x y z [group groupId] [formation]` - Send recruits on aggressive raids with formation support
- `/recruits follow [player] [formation]` - Make recruits follow you or another player in formation
- `/recruits attack <player>` - Command recruits to attack a specific player (configurable max distance: 125 blocks)
- `/recruits cancel` - Cancel all current operations and make recruits return to you
- `/recruits stop` - Make recruits hold defensive positions with shields raised

#### **Advanced Raid System**
- **Two-Phase Raids**: March → Raid phases with automatic transitions
- **Entity Blacklist**: Configurable protection for villagers, pets, and other entities
- **Smart Targeting**: Recruits intelligently engage hostiles while avoiding blacklisted entities
- **Real-time Notifications**: Get detailed updates about your recruit missions

#### **Group Retaliation System**
- **Coordinated Defense**: When one recruit is attacked during march, the entire group retaliates
- **Auto-Resume**: After dealing with threats, recruits automatically resume their original mission
- **Tactical Engagement**: 5-second combat engagement before resuming march operations

### 🛡️ **Enhanced Defensive Behavior**

#### **Stop Command Improvements**
- **Shield Integration**: Multiple methods to ensure recruits raise shields when ordered to stop
- **Defensive States**: Proper guard mode activation with retaliation-only behavior
- **Visual Feedback**: Enhanced status messages for better command awareness

#### **Cancel Command Reliability**
- **Comprehensive AI Clearing**: Fully clears all movement and combat AI states
- **Guaranteed Return**: Multiple fallback methods to ensure recruits pathfind back to player
- **Error Resilience**: Robust null-checking and error handling prevents command failures

### 🎮 **GUI Enhancements**

#### **Enhanced Recruit Command GUI**
- **Improved Spacing**: Increased button spacing to prevent overlap with menu buttons
- **Group Selection**: Commands automatically target active recruit groups
- **Quick Commands**: 
  - "March Here" - Send groups to your look-at position
  - "Raid Here" - Send groups to raid your look-at position  
  - "March to..." - Prefill chat with march command for easy coordinate entry
  - "Raid at..." - Prefill chat with raid command for easy coordinate entry
  - "Cancel" - Instantly cancel operations and recall recruits
  - "Stop" - Make recruits hold defensive positions

#### **Smart Coordinate Suggestions**
- **Player Position**: Suggests your exact X, Y, Z coordinates individually
- **Relative Positioning**: Full support for `~ ~ ~` relative coordinates
- **Formation Auto-complete**: Suggests all available formations (LINE, SQUARE, CIRCLE, WEDGE)

### ⚙️ **Configuration System**

#### **Lagging Recruit Teleportation**
- **Auto-Teleport**: Detects recruits who fall behind during march/raid approach and teleports them to the group centre
- **Configurable Thresholds**: `teleportationDistanceThreshold` and `teleportationProgressThreshold` in `recruits-addon.toml`

#### **Comprehensive Config Options**
- **Attack Distance**: Configure max distance for attack commands (default: 125 blocks)
- **Follow Distance**: Set minimum distance for follow commands (default: 50 blocks)
- **Entity Blacklists**: Customize which entities recruits should avoid during raids
- **Notification Settings**: Fine-tune what notifications you receive and when
- **Formation Behavior**: Adjust how recruits maintain formations during movement

### 🔧 **Technical Improvements**

#### **Performance Optimizations**
- **Efficient Group Detection**: Smart recruit discovery system for better performance
- **Memory Management**: Proper cleanup of completed missions and abandoned operations
- **Network Optimization**: Reduced packet overhead for multiplayer compatibility

#### **Integration Enhancements**
- **RecruitsIntegration Class**: Comprehensive API for recruit control and management
- **Formation Utils**: Advanced positioning calculations for tactical formations
- **Advanced Raid Manager**: Sophisticated mission tracking and coordination system

#### **Error Handling & Reliability**
- **Robust Null Checking**: Comprehensive protection against null pointer exceptions
- **Graceful Degradation**: Commands continue working even if some features fail
- **Clear Error Messages**: Helpful feedback when commands can't be executed

### 🐛 **Bug Fixes**

#### **Critical Fixes**
- **Oscillation Bug**: Fixed recruits bouncing between march and retreat states
- **Cancel Reliability**: Resolved null pointer exceptions in cancel command
- **Group Selection**: Fixed GUI sending commands to all recruits instead of selected groups
- **Shield Activation**: Implemented multiple methods to ensure shields raise on stop command

#### **Stability Improvements**
- **Command Registration**: Fixed compilation errors and method signature mismatches
- **Distance Calculations**: Removed all arbitrary distance limitations
- **State Management**: Improved AI state clearing and command override behavior

### 📋 **Breaking Changes**
- **Distance Limitations Removed**: Commands now work at unlimited range (previously 50 blocks)
- **Group-Based Targeting**: Commands prioritize recruit groups over proximity-based selection
- **Enhanced Command Syntax**: Additional optional parameters for group and formation targeting

### 🔄 **Migration Notes**
- **Existing Commands**: All existing basic commands continue to work as before
- **New Capabilities**: Additional features are opt-in and don't affect existing workflows  
- **Configuration**: New config file `recruits-addon.toml` created automatically

### 🎯 **Usage Examples**

```
# Basic Commands (work at any distance)
/recruits march 100 64 -200
/recruits raid -50 70 300
/recruits cancel
/recruits stop

# Advanced Group Commands
/recruits march 0 64 0 group 1 formation LINE
/recruits raid 100 80 -100 group 2 formation SQUARE

# Social Commands
/recruits follow PlayerName formation WEDGE
/recruits attack EnemyPlayer
```

### 🏆 **What's Next**
This release represents a complete overhaul of the recruit command system, providing tactical control, reliability, and unlimited operational range. Future updates will focus on additional formations, advanced AI behaviors, and integration with other popular mods.

---

*For detailed usage instructions, see README.md*
*For configuration options, see the generated `recruits-addon.toml` file*
