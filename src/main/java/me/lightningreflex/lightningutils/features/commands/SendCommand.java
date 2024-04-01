package me.lightningreflex.lightningutils.features.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import me.lightningreflex.lightningutils.LightningUtils;
import me.lightningreflex.lightningutils.utils.Placeholder;
import me.lightningreflex.lightningutils.utils.Utils;
import me.lightningreflex.lightningutils.configurations.impl.LangConfig;
import me.lightningreflex.lightningutils.configurations.impl.MainConfig;

import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public class SendCommand {
    LangConfig.Commands.Send langSend = LightningUtils.getLangConfig().getCommands().getSend();
    MainConfig.Commands commands = LightningUtils.getMainConfig().getCommands();

    public BrigadierCommand createBrigadierCommand(String command) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal(command)
            .requires(ctx -> Utils.hasPermission(ctx, commands.getSend().getPermission()))
            .then(
                BrigadierCommand.requiredArgumentBuilder(langSend.getArguments().getFrom(), StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        String argument = ctx.getArguments().containsKey(langSend.getArguments().getFrom())
                            ? StringArgumentType.getString(ctx, langSend.getArguments().getFrom())
                            : "";
                        return getSuggestionsCompletableFuture(builder, argument);
                    })
                    .then(BrigadierCommand.requiredArgumentBuilder(langSend.getArguments().getTo(), StringArgumentType.word())
                        .suggests((ctx, builder) -> {
                            String argument = ctx.getArguments().containsKey(langSend.getArguments().getTo())
                                ? StringArgumentType.getString(ctx, langSend.getArguments().getTo())
                                : "";
                            return getSuggestionsCompletableFuture(builder, argument);
                        })
                        .executes(this::execute)
                        .build())
                    .executes(this::executeError)
                    .build()
            )
            .executes(this::executeError)
            .build();

        // BrigadierCommand implements Command
        return new BrigadierCommand(node);
    }

    private CompletableFuture<Suggestions> getSuggestionsCompletableFuture(SuggestionsBuilder builder, String argument) {
        LightningUtils.getProxy().getAllServers().stream().filter((server) -> ("+" + server.getServerInfo().getName()).regionMatches(true, 0, argument, 0, argument.length())).forEach(server -> builder.suggest("+" + server.getServerInfo().getName()));
        LightningUtils.getProxy().getAllPlayers().stream().filter((player) -> player.getUsername().regionMatches(true, 0, argument, 0, argument.length())).forEach(player -> builder.suggest(player.getUsername()));
        return builder.buildFuture();
    }

    private int execute(CommandContext<CommandSource> context) {
        String[] args = context.getInput().split(" ");
        Player player = (Player) context.getSource();

        Placeholder.Builder builder = Placeholder.builder()
            .addPlaceholder("executor", player);

        if (args[1].startsWith("+")) { // Send a server
            builder.addPlaceholder("optional_from_name", args[1].substring(1));
            Optional<RegisteredServer> sourceServer = LightningUtils.getProxy().getServer(args[1].substring(1));

            if (sourceServer.isPresent()) { // Check if server exists
                builder.addPlaceholder("from", sourceServer.get());
                if (args[2].startsWith("+")) { // Send to server
                    builder.addPlaceholder("optional_to_name", args[2].substring(1));
                    Optional<RegisteredServer> destServer = LightningUtils.getProxy().getServer(args[2].substring(1));

                    if (destServer.isPresent()) { // Check if server exists
                        // Server to server
                        builder.addPlaceholder("to", destServer.get());
                        player.sendMessage(Utils.formatString(builder.fill(langSend.getSuccesses().getServer_to_server())));
                        for (Player p : sourceServer.get().getPlayersConnected()) {
                            p.createConnectionRequest(destServer.get()).fireAndForget();
                            builder.addPlaceholder("receiver", p);
                            p.sendMessage(Utils.formatString(builder.fill(langSend.getWarnings().getServer_to_server())));
                        }

                    } else { // Server is invalid
                        player.sendMessage(Utils.formatString(builder.fill(langSend.getServer_does_not_exist().getTo())));
                    }


                } else { // Send to player
                    builder.addPlaceholder("optional_to_name", args[2]);
                    Optional<Player> destPlayer = LightningUtils.getProxy().getPlayer(args[2]);

                    if (destPlayer.isPresent()) { // Check if player exists
                        // Server to player
                        builder.addPlaceholder("to", destPlayer.get());
                        player.sendMessage(Utils.formatString(builder.fill(langSend.getSuccesses().getServer_to_player())));
                        for (Player p : sourceServer.get().getPlayersConnected()) {
                            p.createConnectionRequest(destPlayer.get().getCurrentServer().get().getServer()).fireAndForget();
                            builder.addPlaceholder("receiver", p);
                            p.sendMessage(Utils.formatString(builder.fill(langSend.getWarnings().getServer_to_player())));
                        }

                    } else { // Player is invalid
                        player.sendMessage(Utils.formatString(builder.fill(langSend.getPlayer_offline().getTo())));
                    }
                }

            } else { // Server is invalid
                player.sendMessage(Utils.formatString(builder.fill(langSend.getServer_does_not_exist().getFrom())));
            }




        } else { // Send a player
            builder.addPlaceholder("optional_from_name", args[1]);
            Optional<Player> sourcePlayer = LightningUtils.getProxy().getPlayer(args[1]);

            if (sourcePlayer.isPresent()) { // Check if player exists
                builder.addPlaceholder("from", sourcePlayer.get());
                if (args[2].startsWith("+")) { // Send to server
                    builder.addPlaceholder("optional_to_name", args[2].substring(1));
                    Optional<RegisteredServer> destServer = LightningUtils.getProxy().getServer(args[2].substring(1));

                    if (destServer.isPresent()) { // Check if server exists
                        // Player to server
                        builder.addPlaceholder("to", destServer.get());
                        player.sendMessage(Utils.formatString(builder.fill(langSend.getSuccesses().getPlayer_to_server())));
                        sourcePlayer.get().createConnectionRequest(destServer.get()).fireAndForget();
                        builder.addPlaceholder("receiver", sourcePlayer.get());
                        sourcePlayer.get().sendMessage(Utils.formatString(builder.fill(langSend.getWarnings().getPlayer_to_server())));

                    } else { // Server is invalid
                        player.sendMessage(Utils.formatString(builder.fill(langSend.getServer_does_not_exist().getTo())));
                    }


                } else { // Send to player
                    builder.addPlaceholder("optional_to_name", args[2]);
                    Optional<Player> destPlayer = LightningUtils.getProxy().getPlayer(args[2]);

                    if (destPlayer.isPresent()) { // Check if player exists
                        // Player to player
                        builder.addPlaceholder("to", destPlayer.get());
                        player.sendMessage(Utils.formatString(builder.fill(langSend.getSuccesses().getPlayer_to_player())));
                        sourcePlayer.get().createConnectionRequest(destPlayer.get().getCurrentServer().get().getServer()).fireAndForget();
                        builder.addPlaceholder("receiver", sourcePlayer.get());
                        sourcePlayer.get().sendMessage(Utils.formatString(builder.fill(langSend.getWarnings().getPlayer_to_player())));

                    } else { // Player is invalid
                        player.sendMessage(Utils.formatString(builder.fill(langSend.getPlayer_offline().getTo())));
                    }
                }

            } else { // Player is invalid
                player.sendMessage(Utils.formatString(builder.fill(langSend.getPlayer_offline().getFrom())));
            }
        }
        return 1; // indicates success
    }

    private int executeError(CommandContext<CommandSource> context) {
        Placeholder.Builder builder = Placeholder.builder()
            .addPlaceholder("executor", (Player) context.getSource());
        context.getSource().sendMessage(Utils.formatString(builder.fill(langSend.getArguments().getInvalid_syntax())));
        return 1; // indicates success
    }
}
