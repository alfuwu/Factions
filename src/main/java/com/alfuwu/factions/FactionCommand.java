package com.alfuwu.factions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.Team;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FactionCommand extends Command {
    public final Factions factions;

    protected FactionCommand(Factions factions) {
        super("faction", "Allows interacting with the factions present in the server", "/faction", List.of());
        this.factions = factions;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) {
        if (sender instanceof Player player) {
            if (args.length == 0) {
                sender.sendMessage(getUsage());
                return true;
            }
            switch (args[0]) {
                case "join":
                    if (player.getScoreboard().getPlayerTeam(player) == null) {
                        Team team = factions.getServer().getScoreboardManager().getMainScoreboard().getTeam(args.length > 1 ? args[1] : "");
                        if (team != null && args.length > 1) {
                            team.sendMessage(player.displayName().append(Component.text(" has joined the faction")));
                            team.addPlayer(player);
                            TextColor teamColor = team.hasColor() ? team.color() : NamedTextColor.WHITE;
                            sender.sendMessage(Component.text("You have joined the ")
                                    .append(Component.text("[").color(teamColor))
                                    .append(team.displayName().color(teamColor))
                                    .append(Component.text("]").color(teamColor))
                                    .append(Component.text(" faction!")));
                            return true;
                        } else {
                            sender.sendMessage(Component.text("That faction doesn't exist!").color(NamedTextColor.RED));
                        }
                    } else {
                        sender.sendMessage(Component.text("You're already in a faction!").color(NamedTextColor.RED));
                    }
                    return false;
                case "leave":
                    Team team = player.getScoreboard().getPlayerTeam(player);
                    if (team == null) {
                        sender.sendMessage(Component.text("You're not in a faction!").color(NamedTextColor.RED));
                    } else {
                        TextColor teamColor = team.hasColor() ? team.color() : NamedTextColor.RED;
                        sender.sendMessage(Component.text("You have left the ").color(NamedTextColor.RED)
                                .append(Component.text("[").color(teamColor))
                                .append(team.displayName().color(teamColor))
                                .append(Component.text("]").color(teamColor))
                                .append(Component.text(" faction").color(NamedTextColor.RED)));
                        team.removePlayer(player);
                        team.sendMessage(player.displayName().append(Component.text(" has left the faction")));
                        return true;
                    }
                    return false;
                case "invite":

                    return false;
                case "leader":

                    return false;
                case "members":

                    return false;
                case "list":

                    return true;
                default:
                    sender.sendMessage(Component.text("That is not a valid subcommand!").color(NamedTextColor.RED));
            }
        } else {
            sender.sendMessage(Component.text("You must execute this command as a player").color(NamedTextColor.RED));
        }
        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        switch (args.length) {
            case 1:
                return List.of("join", "leave", "invite", "leader", "members", "list");
            case 2:
                switch (args[0]) {
                    case "join", "leader", "members":
                        return factions.getServer().getScoreboardManager().getMainScoreboard().getTeams().stream().map(Team::getName).toList();
                    case "invite":
                        return factions.getServer().getOnlinePlayers().stream().filter(p -> p != sender).map(p -> ((TextComponent)p.displayName()).content()).toList();
                }
        }
        return List.of();
    }
}
