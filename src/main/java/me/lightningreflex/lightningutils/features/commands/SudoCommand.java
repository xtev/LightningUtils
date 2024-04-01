package me.lightningreflex.lightningutils.features.commands;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import com.velocitypowered.api.command.BrigadierCommand;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.proxy.Player;
import com.velocitypowered.proxy.connection.client.ConnectedPlayer;
import me.lightningreflex.lightningutils.LightningUtils;
import me.lightningreflex.lightningutils.utils.Placeholder;
import me.lightningreflex.lightningutils.utils.Utils;
import me.lightningreflex.lightningutils.configurations.impl.LangConfig;
import me.lightningreflex.lightningutils.configurations.impl.MainConfig;

import java.util.Optional;

public class SudoCommand {
    LangConfig.Commands.Sudo langSudo = LightningUtils.getLangConfig().getCommands().getSudo();
    MainConfig mainConfig = LightningUtils.getMainConfig();
    MainConfig.Commands commands = mainConfig.getCommands();

    public BrigadierCommand createBrigadierCommand(String command) {
        LiteralCommandNode<CommandSource> node = LiteralArgumentBuilder
            .<CommandSource>literal(command)
            .requires(ctx -> Utils.hasPermission(ctx, commands.getSudo().getPermission()))
            .then(
                BrigadierCommand.requiredArgumentBuilder(langSudo.getArguments().getPlayer(), StringArgumentType.word())
                    .suggests((ctx, builder) -> {
                        String argument = ctx.getArguments().containsKey(langSudo.getArguments().getPlayer())
                            ? StringArgumentType.getString(ctx, langSudo.getArguments().getPlayer())
                            : "";
                        LightningUtils.getProxy().getAllPlayers().stream().filter((player) -> player.getUsername().regionMatches(true, 0, argument, 0, argument.length())).forEach(player -> builder.suggest(player.getUsername()));
                        return builder.buildFuture();
                    })
                    .then(
                        BrigadierCommand.requiredArgumentBuilder(langSudo.getArguments().getText(), StringArgumentType.greedyString())
                        .executes(this::execute)
                        .build()
                    )
                .executes(this::executeError)
                .build()
            )
            .executes(this::executeError)
            .build();

        // BrigadierCommand implements Command
        return new BrigadierCommand(node);
    }

    private int execute(CommandContext<CommandSource> context) {
        String playerName = StringArgumentType.getString(context, langSudo.getArguments().getPlayer());
        Optional<Player> optionalPlayer = LightningUtils.getProxy().getPlayer(playerName);

        Placeholder.Builder builder = Placeholder.builder()
            .addPlaceholder("executor", (Player) context.getSource())// oohhh perhaps in the future I can make it
            // so I can supply a source, and it will automatically fill in the placeholders depending on if it's a player or console
            .addPlaceholder("optionalTargetName", playerName);

        if (optionalPlayer.isEmpty()) {
//            context.getSource().sendMessage(Utils.formatString(langSudo.getPlayer_not_found(), playerName));
            context.getSource().sendMessage(Utils.formatString(builder.fill(langSudo.getPlayer_not_found())));
            return 1;
        }
        Player victim = optionalPlayer.get();
        builder.addPlaceholder("target", victim);
        String text = StringArgumentType.getString(context, langSudo.getArguments().getText());
        builder.addPlaceholder("command", text);

        // cool reflection was useless, spent hours cooking, just to end up cooking water, and ending up with this
        ConnectedPlayer connectedVictim = (ConnectedPlayer) victim;
        // force a slash cause like, non-commands will cause a signature error
        if (!text.startsWith("/")) text = "/" + text;
        // notify player
        if (mainConfig.getSudo().isNotify())
//            victim.sendMessage(Utils.formatString(langSudo.getNotify(), ((Player) context.getSource()).getUsername(), text));
            victim.sendMessage(Utils.formatString(builder.fill(langSudo.getNotify())));
        context.getSource().sendMessage(Utils.formatString(builder.fill(langSudo.getSuccess())));
        connectedVictim.spoofChatInput(text);
        return 1; // indicates success
    }

    private int executeError(CommandContext<CommandSource> context) {
        Placeholder.Builder builder = Placeholder.builder()
            .addPlaceholder("executor", (Player) context.getSource());
        context.getSource().sendMessage(Utils.formatString(builder.fill(langSudo.getArguments().getInvalid_syntax())));
        return 1; // indicates success
    }
}
