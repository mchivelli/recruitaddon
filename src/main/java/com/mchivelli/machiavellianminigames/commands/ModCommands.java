package com.mchivelli.machiavellianminigames.commands;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.BlockPosArgument;

public class ModCommands {
    
    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        // Register all commands under /mm prefix
        dispatcher.register(Commands.literal("mm")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("reload")
                .executes(ctx -> {
                    ctx.getSource().sendSuccess(() -> net.minecraft.network.chat.Component.literal("§aConfiguration reloaded!"), false);
                    return 1;
                }))
            
            // /mm arena commands
            .then(Commands.literal("arena")
                .then(Commands.literal("create")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .executes(ArenaCommands::createArena)))
                .then(Commands.literal("delete")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(ArenaCommands.suggestArenaNames())
                        .executes(ArenaCommands::deleteArena)))
                .then(Commands.literal("setbounds")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(ArenaCommands.suggestArenaNames())
                        .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                            .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                .executes(ArenaCommands::setBounds)))))
                .then(Commands.literal("list")
                    .executes(ArenaCommands::listArenas))
                .then(Commands.literal("info")
                    .then(Commands.argument("name", StringArgumentType.string())
                        .suggests(ArenaCommands.suggestArenaNames())
                        .executes(ArenaCommands::arenaInfo))))
            
            // /mm checkpoint commands
            .then(Commands.literal("checkpoint")
                .then(Commands.literal("add")
                    .then(Commands.argument("arena", StringArgumentType.string())
                        .suggests(ArenaCommands.suggestArenaNames())
                        .then(Commands.argument("name", StringArgumentType.string())
                            .then(Commands.argument("pos1", BlockPosArgument.blockPos())
                                .then(Commands.argument("pos2", BlockPosArgument.blockPos())
                                    .then(Commands.argument("type", StringArgumentType.string())
                                        .suggests((context, builder) -> {
                                            builder.suggest("money");
                                            builder.suggest("upgrade");
                                            return builder.buildFuture();
                                        })
                                        .executes(ArenaCommands::addCheckpoint))))))))
            
            // /mm team commands
            .then(Commands.literal("team")
                .then(Commands.literal("create")
                    .then(Commands.argument("arena", StringArgumentType.string())
                        .suggests(ArenaCommands.suggestArenaNames())
                        .then(Commands.argument("team", StringArgumentType.string())
                            .then(Commands.argument("color", StringArgumentType.string())
                                .suggests((context, builder) -> {
                                    builder.suggest("red");
                                    builder.suggest("blue");
                                    builder.suggest("green");
                                    builder.suggest("yellow");
                                    builder.suggest("purple");
                                    builder.suggest("aqua");
                                    builder.suggest("white");
                                    builder.suggest("gray");
                                    builder.suggest("black");
                                    return builder.buildFuture();
                                })
                                .executes(TeamCommands::createTeam)))))
                .then(Commands.literal("addplayer")
                    .then(Commands.argument("arena", StringArgumentType.string())
                        .suggests(ArenaCommands.suggestArenaNames())
                        .then(Commands.argument("team", StringArgumentType.string())
                            .suggests(TeamCommands.suggestTeamNames())
                            .then(Commands.argument("player", EntityArgument.player())
                                .executes(TeamCommands::addPlayer)))))
                .then(Commands.literal("removeplayer")
                    .then(Commands.argument("arena", StringArgumentType.string())
                        .suggests(ArenaCommands.suggestArenaNames())
                        .then(Commands.argument("team", StringArgumentType.string())
                            .suggests(TeamCommands.suggestTeamNames())
                            .then(Commands.argument("player", EntityArgument.player())
                                .executes(TeamCommands::removePlayer)))))
                .then(Commands.literal("setspawn")
                    .then(Commands.argument("arena", StringArgumentType.string())
                        .suggests(ArenaCommands.suggestArenaNames())
                        .then(Commands.argument("team", StringArgumentType.string())
                            .suggests(TeamCommands.suggestTeamNames())
                            .then(Commands.argument("pos", BlockPosArgument.blockPos())
                                .executes(TeamCommands::setSpawn)))))
                .then(Commands.literal("list")
                    .then(Commands.argument("arena", StringArgumentType.string())
                        .suggests(ArenaCommands.suggestArenaNames())
                        .executes(TeamCommands::listTeams))))
            
            // /mm game commands
            .then(Commands.literal("game")
                .then(Commands.literal("start")
                    .then(Commands.argument("arena", StringArgumentType.string())
                        .suggests(ArenaCommands.suggestArenaNames())
                        .then(Commands.argument("type", StringArgumentType.string())
                            .suggests((context, builder) -> {
                                builder.suggest("storm");
                                return builder.buildFuture();
                            })
                            .executes(GameCommands::startGame))))
                .then(Commands.literal("end")
                    .then(Commands.argument("arena", StringArgumentType.string())
                        .suggests(ArenaCommands.suggestArenaNames())
                        .executes(GameCommands::endGame)))
                .then(Commands.literal("status")
                    .executes(GameCommands::gameStatus)))
        );
    }
}
