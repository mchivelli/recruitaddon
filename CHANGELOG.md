# Machiavellian Minigames - Changelog

## About
**Machiavellian Minigames** is a comprehensive Minecraft 1.20.1 Forge mod that provides a robust minigame system with arena management, team-based gameplay, and advanced checkpoint capture mechanics.

**Author:** mchivelli  
**Mod ID:** `machiavellianminigames`  
**Minecraft Version:** 1.20.1  
**Forge Version:** Compatible with latest 1.20.1 Forge builds

---

## 🎮 Core Features

### **Arena Management System**
- **Multi-Arena Support**: Create and manage multiple independent game arenas
- **Flexible Boundaries**: Set custom arena boundaries using coordinate-based bounds
- **Persistent Storage**: Arenas automatically save and persist across server restarts
- **Arena Commands**: Full command suite for arena creation, deletion, and management

### **Team Management System**
- **Dynamic Teams**: Create teams with custom names and colors
- **Player Assignment**: Add/remove players to teams with full permission handling
- **Team Spawning**: Set custom spawn points for each team
- **Color Coding**: Full ChatFormatting color support for team identification
- **Cross-Arena Teams**: Teams can be configured per-arena for maximum flexibility

### **Advanced Command System**
- **Hierarchical Commands**: Organized under `/mm` prefix for consistency
- **Auto-Completion**: Intelligent command suggestions for arena names, teams, colors, and game types
- **Permission Handling**: Proper permission checks and error handling
- **Comprehensive Help**: Built-in help system and command documentation

---

## 🏰 Storm the Front Minigame

### **Checkpoint Capture System**
- **Two-Phase Capture**: Innovative clearing → claiming system for strategic gameplay
  - **Phase 1 (Clearing)**: Remove enemy control from occupied checkpoints
  - **Phase 2 (Claiming)**: Capture cleared checkpoints for your team
- **Contested Zones**: Automatic pause system when multiple teams are present
- **Abandonment Detection**: Smart reset system when players leave capture zones
- **Multi-Point Hit Detection**: Reliable player detection using multiple hitbox points

### **Dynamic Boss Bar System**
- **Real-Time Progress**: Live progress updates showing capture percentage
- **Team-Specific Colors**: Boss bars match team colors for easy identification
- **State-Aware Display**: Different displays for controlled, contested, claiming, and clearing states
- **Smart Visibility**: Boss bars show to relevant players (in zone + claiming team)
- **Smooth Transitions**: Seamless progress updates with no visual glitches

### **Resource Distribution System**
- **Interval-Based Rewards**: Configurable resource distribution at set intervals
  - Money resources: Every 60 seconds (configurable)
  - Upgrade resources: Every 300 seconds (configurable)
- **Exponential Multipliers**: Resource amounts scale exponentially with controlled checkpoints (2^checkpoints)
- **Immediate Bonuses**: Instant rewards when capturing checkpoints
- **Type-Specific Checkpoints**: Money checkpoints and Upgrade checkpoints provide different resources

### **Isolated Checkpoint Engine**
- **State Machine**: Clean state management (UNCLAIMED → CLAIMING → CONTROLLED → CONTESTED)
- **Progress Tracking**: Accurate progress calculation with pause/resume functionality
- **Atomic Transitions**: Reliable state transitions without race conditions
- **Performance Optimized**: Efficient tick-based updates only when necessary

---

## ⚙️ Configuration System

### **Per-Game Configuration**
- **Storm the Front Config**: Dedicated configuration class with ForgeConfigSpec
- **Runtime Reloadable**: Configuration changes apply without server restart
- **Comprehensive Settings**:
  - Claim times (default: 10 seconds)
  - Resource intervals (money: 60s, upgrades: 300s)
  - Resource amounts and capture bonuses
  - Boss bar display options
  - Message cooldowns and formatting

### **TOML Configuration Files**
- **User-Friendly**: Human-readable configuration files
- **Validation**: Built-in validation and error checking
- **Documentation**: Inline comments explaining each setting
- **Backup-Safe**: Configuration system handles missing or corrupted files gracefully

---

## 🛡️ Anti-Cheat & Security Features

### **Inventory Isolation**
- **Complete Inventory Backup**: Full player state preservation before minigames
- **Automatic Restoration**: Perfect restoration of inventory, armor, XP, health, and position
- **Ender Chest Protection**: Disabled ender chest access within arenas
- **Item Transfer Prevention**: Blocks inventory transfers between players in arenas

### **Arena Security**
- **Boundary Enforcement**: Strict boundary checking and enforcement
- **State Protection**: Game states protected from external interference
- **Resource Protection**: Anti-duplication measures for minigame resources

---

## 🎯 Advanced Gameplay Features

### **Strategic Depth**
- **Team Coordination**: Requires teamwork for effective checkpoint control
- **Resource Management**: Strategic decisions about which checkpoints to prioritize
- **Risk vs Reward**: Exponential rewards encourage aggressive expansion
- **Comeback Mechanics**: Losing teams can still recover through strategic play

### **Real-Time Feedback**
- **Action Notifications**: Immediate feedback for all checkpoint actions
- **Progress Visibility**: Always-visible progress for active captures
- **Team Communication**: Built-in messaging system for team coordination
- **Status Updates**: Clear communication of game state changes

### **Scalable Gameplay**
- **Multiple Checkpoints**: Support for unlimited checkpoints per arena
- **Flexible Team Sizes**: Works with any number of teams and players
- **Configurable Durations**: Customizable game lengths and capture times

---

## 🔧 Technical Features

### **Modern Architecture**
- **Event-Driven**: Built on Forge's event system for reliable operation
- **Modular Design**: Cleanly separated components for maintainability
- **Memory Efficient**: Optimized data structures and minimal memory footprint
- **Thread Safe**: Proper synchronization for multiplayer environments

### **Robust Error Handling**
- **Graceful Failures**: System continues operating even when individual components fail
- **Comprehensive Logging**: Detailed logging for debugging and monitoring
- **Recovery Systems**: Automatic recovery from common error conditions
- **Validation**: Input validation and sanitization throughout

### **Performance Optimized**
- **Efficient Updates**: Smart update scheduling to minimize server impact
- **Lazy Loading**: Resources loaded only when needed
- **Caching**: Intelligent caching of frequently accessed data
- **Batch Operations**: Grouped operations for better performance

---

## 📋 Command Reference

### **Arena Commands**
```
/mm arena create <name>              - Create a new arena
/mm arena delete <name>              - Delete an existing arena
/mm arena setbounds <name> <pos1> <pos2>  - Set arena boundaries
/mm arena list                       - List all arenas
/mm arena info <name>                - Show detailed arena information
```

### **Checkpoint Commands**
```
/mm checkpoint add <arena> <name> <pos1> <pos2> <type>  - Add checkpoint to arena
```

### **Team Commands**
```
/mm team create <arena> <team> <color>     - Create a team in an arena
/mm team addplayer <arena> <team> <player> - Add player to team
/mm team removeplayer <arena> <team> <player> - Remove player from team
/mm team setspawn <arena> <team> <pos>     - Set team spawn point
/mm team list <arena>                      - List teams in arena
```

### **Game Commands**
```
/mm game start <arena> <type>        - Start a minigame (type: storm)
/mm game end <arena>                 - End current game in arena
/mm game status                      - Show current game status
```

### **System Commands**
```
/mm reload                          - Reload configuration files
```

---

## 🚀 Recent Improvements

### **v1.0 - Initial Release**
- Complete arena and team management system
- Storm the Front minigame with full feature set
- Comprehensive command system with auto-completion
- TOML-based configuration system
- Anti-cheat and security features

### **v1.1 - Enhanced Checkpoint System**
- Implemented isolated checkpoint capture engine
- Added two-phase capture mechanics (clearing → claiming)
- Enhanced boss bar system with real-time updates
- Improved contested zone handling with pause/resume
- Added exponential resource multiplier system

### **v1.2 - Performance & Stability**
- Optimized tick-based update system
- Enhanced error handling and recovery
- Improved memory management and performance
- Added comprehensive logging and debugging tools
- Fixed compilation issues and improved code organization

---

## 🎯 Future Roadmap

### **Planned Features**
- Additional minigame types beyond Storm the Front
- Advanced statistics and leaderboard system
- Tournament bracket system for competitive play
- Enhanced spectator mode with camera controls
- Integration with economy plugins
- Custom item rewards and progression system

### **Technical Improvements**
- Database integration for persistent statistics
- Web-based admin panel for server management
- API for third-party plugin integration
- Advanced anti-cheat measures
- Performance monitoring and optimization tools

---

## 🐛 Known Issues & Limitations

### **Current Limitations**
- Single minigame type (Storm the Front) - more planned
- Basic resource types (money/upgrades) - extensible system in place
- Manual checkpoint placement - GUI tools planned

### **Resolved Issues**
- ✅ Boss bar progress not updating during clearing phase
- ✅ Contested checkpoints spam prevention
- ✅ Checkpoint completion logic accuracy
- ✅ Resource awarding on capture completion
- ✅ Server shutdown cleanup and arena reset

---

## 📦 Installation & Setup

### **Requirements**
- Minecraft 1.20.1
- Minecraft Forge 47.x.x or higher
- Java 17 or higher

### **Quick Setup Guide**
1. Install mod in `mods/` folder
2. Start server to generate configuration files
3. Create an arena: `/mm arena create testArena`
4. Set arena bounds: `/mm arena setbounds testArena ~ ~ ~ ~50 ~ ~50`
5. Add checkpoints: `/mm checkpoint add testArena cp1 ~ ~ ~ ~5 ~3 ~5 MONEY`
6. Create teams: `/mm team create testArena Red RED`
7. Add players: `/mm team addplayer testArena Red PlayerName`
8. Start game: `/mm game start testArena storm`

### **Configuration**
- Main config: `config/machiavellianminigames-server.toml`
- Arena data: `world/data/arena_manager.dat`
- Per-game configs: Auto-generated on first run

---

## 🤝 Support & Contributing

### **Getting Help**
- Check this changelog for feature documentation
- Review command reference for usage instructions
- Check configuration files for customization options
- Use `/mm` commands with tab completion for guidance

### **Reporting Issues**
- Provide detailed reproduction steps
- Include relevant log files and configuration
- Specify Minecraft, Forge, and mod versions
- Include information about other installed mods

---

## 📄 License & Credits

**Author:** mchivelli  
**License:** All rights reserved  
**Built with:** Minecraft Forge, Java 17, Gradle

Special thanks to the Minecraft Forge team and the modding community for providing the tools and documentation that made this mod possible.

---

*Last Updated: 2025-01-20*
*Version: 1.2.0*
