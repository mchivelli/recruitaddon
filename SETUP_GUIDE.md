# Quick Setup Guide for Machiavellian Minigames

## Prerequisites
- Minecraft Forge 1.20.1 server
- OP permissions (level 2+)

## Step-by-Step Setup

### 1. Create an Arena
```
/mm arena create myarena
```

### 2. Set Arena Boundaries
Stand at one corner of your arena area, then at the opposite corner:
```
/mm arena setbounds myarena ~ ~ ~ ~20 ~10 ~20
```
(This creates a 20x10x20 arena from your current position)

### 3. Create Teams
```
/mm team create myarena red red
/mm team create myarena blue blue
```

### 4. Add Players to Teams
```
/mm team addplayer myarena red PlayerName1
/mm team addplayer myarena blue PlayerName2
```

### 5. Set Team Spawn Points
Stand where you want each team to spawn:
```
/mm team setspawn myarena red ~ ~ ~
/mm team setspawn myarena blue ~ ~ ~
```

### 6. Add Checkpoints (for Storm the Front)
```
/mm arena checkpoint myarena checkpoint1 100 64 100 105 69 105 money
/mm arena checkpoint myarena checkpoint2 120 64 120 125 69 125 upgrade
```

### 7. Start the Game
```
/mm game start myarena storm
```

### 8. End the Game
```
/mm game end myarena
```

## Useful Commands

### Arena Management
- `/mm arena list` - List all arenas
- `/mm arena info <arena>` - Show arena details
- `/mm arena delete <arena>` - Delete an arena

### Team Management
- `/mm team list <arena>` - List teams in arena
- `/mm team removeplayer <arena> <team> <player>` - Remove player from team

### Game Management
- `/mm game status` - Check current game status
- `/mm reload` - Reload configuration

## Configuration

The mod uses TOML configuration files in `config/machiavellianminigames/`:
- `main.toml` - Main configuration
- `storm.toml` - Storm the Front game settings

Key settings:
- `storm_duration` - Game duration in minutes (default: 10)
- `storm_claim_time` - Time to capture checkpoint in seconds (default: 30)
- `storm_resource_interval` - Resource generation interval in seconds (default: 60)

## Troubleshooting

1. **Commands not working**: Ensure you have OP level 2+ permissions
2. **Players can't join teams**: Make sure the arena exists and teams are created
3. **Checkpoints not working**: Verify checkpoint coordinates are correct
4. **Game won't start**: Ensure teams have players and spawn points are set

## Tips

- Use `/mm arena info <arena>` to verify your setup before starting
- Test with a small arena first to understand the mechanics
- Checkpoints should be strategically placed for balanced gameplay
- Monitor the console for any error messages during setup
