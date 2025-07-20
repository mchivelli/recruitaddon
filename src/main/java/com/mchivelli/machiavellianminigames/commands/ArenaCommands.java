package com.mchivelli.machiavellianminigames.commands;


import com.mchivelli.machiavellianminigames.core.Arena;
import com.mchivelli.machiavellianminigames.core.ArenaManager;
import com.mchivelli.machiavellianminigames.core.CheckpointZone;
import com.mchivelli.machiavellianminigames.core.Team;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;

import java.util.concurrent.CompletableFuture;

public class ArenaCommands {
    
    public static int createArena(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String arenaName = StringArgumentType.getString(ctx, "name");
        CommandSourceStack source = ctx.getSource();
        ServerLevel level = source.getLevel();
        
        ArenaManager manager = ArenaManager.get();
        Arena arena = manager.createArena(arenaName, level.dimension().location());
        
        if (arena != null) {
            source.sendSuccess(() -> Component.literal("§aCreated arena: " + arenaName), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§cArena " + arenaName + " already exists!"));
            return 0;
        }
    }
    
    public static int deleteArena(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String arenaName = StringArgumentType.getString(ctx, "name");
        CommandSourceStack source = ctx.getSource();
        
        ArenaManager manager = ArenaManager.get();
        boolean deleted = manager.deleteArena(arenaName);
        
        if (deleted) {
            source.sendSuccess(() -> Component.literal("§aDeleted arena: " + arenaName), false);
            return 1;
        } else {
            source.sendFailure(Component.literal("§cArena " + arenaName + " does not exist!"));
            return 0;
        }
    }
    
    public static int setBounds(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String arenaName = StringArgumentType.getString(ctx, "name");
        BlockPos pos1 = BlockPosArgument.getBlockPos(ctx, "pos1");
        BlockPos pos2 = BlockPosArgument.getBlockPos(ctx, "pos2");
        CommandSourceStack source = ctx.getSource();
        
        ArenaManager manager = ArenaManager.get();
        Arena arena = manager.getArena(arenaName);
        
        if (arena == null) {
            source.sendFailure(Component.literal("§cArena " + arenaName + " does not exist!"));
            return 0;
        }
        
        arena.setBounds(pos1, pos2);
        source.sendSuccess(() -> Component.literal("§aSet bounds for arena " + arenaName + 
            " from " + pos1.toShortString() + " to " + pos2.toShortString()), false);
        return 1;
    }
    
    public static int listArenas(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ArenaManager manager = ArenaManager.get();
        
        if (manager.getArenas().isEmpty()) {
            source.sendSuccess(() -> Component.literal("§7No arenas configured."), false);
            return 1;
        }
        
        source.sendSuccess(() -> Component.literal("§6=== Arenas ==="), false);
        for (Arena arena : manager.getArenas()) {
            String status = arena.isConfigured() ? "§aReady" : "§cIncomplete";
            String gameStatus = arena.getCurrentGame() != null ? " §7(Game Active)" : "";
            source.sendSuccess(() -> Component.literal("§f" + arena.getName() + " " + status + gameStatus), false);
        }
        return 1;
    }
    
    public static int arenaInfo(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String arenaName = StringArgumentType.getString(ctx, "name");
        CommandSourceStack source = ctx.getSource();
        
        ArenaManager manager = ArenaManager.get();
        Arena arena = manager.getArena(arenaName);
        
        if (arena == null) {
            source.sendFailure(Component.literal("§cArena " + arenaName + " does not exist!"));
            return 0;
        }
        
        source.sendSuccess(() -> Component.literal("§6=== Arena: " + arena.getName() + " ==="), false);
        source.sendSuccess(() -> Component.literal("§fDimension: " + arena.getDimension().toString()), false);
        
        if (arena.getPos1() != null && arena.getPos2() != null) {
            source.sendSuccess(() -> Component.literal("§fBounds: " + arena.getPos1().toShortString() + 
                " to " + arena.getPos2().toShortString()), false);
        } else {
            source.sendSuccess(() -> Component.literal("§cBounds: Not set"), false);
        }
        
        source.sendSuccess(() -> Component.literal("§fTeams: " + arena.getTeams().size()), false);
        for (Team team : arena.getTeams()) {
            source.sendSuccess(() -> Component.literal("  " + team.getFormattedName() + 
                " (" + team.getPlayerCount() + " players)"), false);
        }
        
        source.sendSuccess(() -> Component.literal("§fCheckpoints: " + arena.getCheckpointZones().size()), false);
        for (CheckpointZone zone : arena.getCheckpointZones()) {
            source.sendSuccess(() -> Component.literal("  §f" + zone.getName()), false);
        }
        
        final String statusText;
        if (arena.getCurrentGame() != null) {
            statusText = "§eGame in progress: " + arena.getCurrentGame().getGameType();
        } else {
            statusText = arena.isConfigured() ? "§aReady for games" : "§cIncomplete configuration";
        }
        source.sendSuccess(() -> Component.literal("§fStatus: " + statusText), false);
        
        return 1;
    }
    
    public static int addCheckpoint(CommandContext<CommandSourceStack> ctx) throws CommandSyntaxException {
        String arenaName = StringArgumentType.getString(ctx, "arena");
        String checkpointName = StringArgumentType.getString(ctx, "name");
        BlockPos pos1 = BlockPosArgument.getBlockPos(ctx, "pos1");
        BlockPos pos2 = BlockPosArgument.getBlockPos(ctx, "pos2");
        String typeStr = StringArgumentType.getString(ctx, "type").toLowerCase();
        CommandSourceStack source = ctx.getSource();
        
        ArenaManager manager = ArenaManager.get();
        Arena arena = manager.getArena(arenaName);
        
        if (arena == null) {
            source.sendFailure(Component.literal("§cArena " + arenaName + " does not exist!"));
            return 0;
        }
        
        // Parse the checkpoint type
        CheckpointZone.CheckpointType type;
        if (typeStr.equals("money")) {
            type = CheckpointZone.CheckpointType.MONEY;
        } else if (typeStr.equals("upgrade")) {
            type = CheckpointZone.CheckpointType.UPGRADE;
        } else {
            source.sendFailure(Component.literal("§cInvalid checkpoint type! Use 'money' or 'upgrade'"));
            return 0;
        }
        
        // Add checkpoint with the specified type
        arena.addCheckpointZone(checkpointName, pos1, pos2, type);
        
        // Mark the data as dirty so it gets saved
        manager.setDirty();
        
        source.sendSuccess(() -> Component.literal("§aAdded " + typeStr + " checkpoint " + checkpointName + 
            " to arena " + arenaName), false);
        return 1;
    }
    
    /**
     * Provides arena name suggestions for command auto-completion
     */
    public static SuggestionProvider<CommandSourceStack> suggestArenaNames() {
        return (context, builder) -> {
            ArenaManager manager = ArenaManager.get();
            for (Arena arena : manager.getArenas()) {
                builder.suggest(arena.getName());
            }
            return CompletableFuture.completedFuture(builder.build());
        };
    }
}
