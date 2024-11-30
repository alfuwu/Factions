package com.alfuwu.factions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;

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
            UUID uuid = player.getUniqueId();
            String faction = factions.getPlayerFaction(uuid);
            FactionData factionData = faction != null ? factions.getFactionData(faction) : null;
            switch (args[0]) {
                case "join":
                    if (factions.getPlayerFaction(uuid) == null) {
                        FactionData fData = factions.getFactionData(args.length > 1 ? args[1] : "");
                        if (fData != null && args.length > 1) {
                            List<Player> players = factions.getPlayersInFaction(args[1]);
                            players.forEach(p -> p.sendMessage(player.displayName().append(Component.text(" has joined the faction"))));
                            TextColor fColor = fData.color() != null ? TextColor.color(fData.color()) : null;
                            sender.sendMessage(Component.text("You have joined the ").color(NamedTextColor.YELLOW)
                                    .append(Component.text("[" + fData.name() + "]").color(fColor != null ? fColor : NamedTextColor.YELLOW))
                                    .append(Component.text(" faction!").color(NamedTextColor.YELLOW)));
                            if (players.isEmpty())
                                sender.sendMessage(Component.text("You are now the Faction Leader of the ").color(NamedTextColor.GOLD)
                                        .append(Component.text("[" + fData.name() + "]").color(fColor != null ? fColor : NamedTextColor.GOLD))
                                        .append(Component.text("!").color(NamedTextColor.GOLD)));
                            else if (players.size() == 1)
                                sender.sendMessage(Component.text("You are now the Successor of the ").color(NamedTextColor.LIGHT_PURPLE)
                                        .append(Component.text("[" + fData.name() + "]").color(fColor != null ? fColor : NamedTextColor.LIGHT_PURPLE))
                                        .append(Component.text("!").color(NamedTextColor.LIGHT_PURPLE)));
                            Component p = Component.text("[" + fData.name() + "] ").color(fColor != null ? fColor : NamedTextColor.WHITE)
                                    .append(player.name().color(player.isOp() ? NamedTextColor.DARK_RED : fColor != null ? fColor : NamedTextColor.WHITE));
                            player.displayName(p);
                            player.playerListName(p);
                            factions.setPlayerData(uuid, args[1], (byte)(players.isEmpty() ? 1 : players.size() == 1 ? 2 : 0));
                            return true;
                        } else {
                            sender.sendMessage(Component.text("That faction doesn't exist!").color(NamedTextColor.RED));
                        }
                    } else {
                        sender.sendMessage(Component.text("You're already in a faction!").color(NamedTextColor.RED));
                    }
                    return false;
                case "leave":
                    if (faction == null) {
                        notInFaction(sender);
                    } else {
                        Integer c = factions.getFactionColor(faction);
                        TextColor fColor = c != null ? TextColor.color(c) : NamedTextColor.RED;
                        sender.sendMessage(Component.text("You have left the ").color(NamedTextColor.RED)
                                .append(Component.text("[" + factions.getFactionName(faction) + "]").color(fColor))
                                .append(Component.text(" faction").color(NamedTextColor.RED)));
                        factions.removePlayerData(uuid);
                        factions.getPlayersInFaction(faction).forEach(p -> p.sendMessage(player.displayName().append(Component.text(" has left the faction"))));
                        Component p = player.name().color(player.isOp() ? NamedTextColor.DARK_RED : null);
                        player.displayName(p);
                        player.playerListName(p);
                        return true;
                    }
                    return false;
                case "invite":

                    return true;
                case "leader":

                    return true;
                case "members":

                    return true;
                case "list":

                    return true;
                case "modify":
                    if (!factions.isFactionLeader(uuid)) {
                        invalid(sender);
                        break;
                    } else if (faction == null) {
                        notInFaction(sender);
                        break;
                    }
                    switch (args[1]) {
                        case "color":
                            boolean validHexColor = args[2].matches("#[0-9a-fA-F]{6}");
                            NamedTextColor n = NamedTextColor.NAMES.value(args[2]);
                            Integer c = validHexColor ? Integer.valueOf(Integer.parseInt(args[2].substring(1), 16)) : n != null ? n.value() : null;
                            factions.setFactionData(faction, factionData.name(), factionData.description(), c, factionData.priv(), factionData.applicants(), factionData.banned());
                            sender.sendMessage(Component.text("Updated ")
                                    .append(Component.text("[" + factionData.name() + "]").color(c != null ? TextColor.color(c) : NamedTextColor.WHITE))
                                    .append(Component.text("'s color")));
                            break;
                        case "name":
                            String name = String.join(" ", Arrays.stream(args).toList().subList(3, args.length));
                            factions.setFactionData(faction, name, factionData.description(), factionData.color(), factionData.priv(), factionData.applicants(), factionData.banned());
                            sender.sendMessage(Component.text("Updated ")
                                    .append(Component.text("[" + name + "]").color(factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.WHITE))
                                    .append(Component.text("'s name")));
                            break;
                        case "description":
                            String description = String.join(" ", Arrays.stream(args).toList().subList(3, args.length));
                            factions.setFactionData(faction, factionData.name(), description, factionData.color(), factionData.priv(), factionData.applicants(), factionData.banned());
                            sender.sendMessage(Component.text("Updated ")
                                    .append(Component.text("[" + factionData.name() + "]").color(factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.WHITE))
                                    .append(Component.text("'s description")));
                            break;
                        case "leader":
                            Player newLeader = factions.getServer().getPlayerExact(args[2]);
                            boolean sameFact = newLeader != null && faction.equals(factions.getPlayerFaction(newLeader.getUniqueId()));
                            if (sameFact && factions.getFactionFlags(newLeader.getUniqueId()) != 1) {
                                TextColor fColor = factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.GOLD;
                                factions.setPlayerData(player.getUniqueId(), faction, (byte)0);
                                factions.setPlayerData(newLeader.getUniqueId(), faction, (byte)1);
                                sender.sendMessage(Component.text("You are no longer the Faction Leader of the ").color(NamedTextColor.GOLD)
                                        .append(Component.text("[" + factionData.name() + "]").color(fColor))
                                        .append(Component.text(" faction")));
                                newLeader.sendMessage(Component.text("You are now the Faction Leader of the ").color(NamedTextColor.GOLD)
                                        .append(Component.text("[" + factionData.name() + "]").color(fColor))
                                        .append(Component.text("!").color(NamedTextColor.GOLD)));
                            } else if (newLeader == null) {
                                sender.sendMessage(Component.text("That player doesn't exist (or is not online at the moment)").color(NamedTextColor.RED));
                            } else if (!sameFact) {
                                sender.sendMessage(Component.text("That player isn't in your faction!").color(NamedTextColor.RED));
                            } else {
                                sender.sendMessage(Component.text("You're already the Faction Leader of your faction!").color(NamedTextColor.RED));
                            }
                            break;
                        case "successor":
                            Player newSuccessor = factions.getServer().getPlayerExact(args[2]);
                            boolean sameFact2 = newSuccessor != null && faction.equals(factions.getPlayerFaction(newSuccessor.getUniqueId()));
                            if (sameFact2 && factions.getFactionFlags(newSuccessor.getUniqueId()) == 0) {
                                TextColor fColor = factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.LIGHT_PURPLE;
                                factions.setPlayerData(player.getUniqueId(), faction, (byte)0);
                                factions.getAllPlayersInFaction(faction).stream().filter(p -> factions.isFactionSuccessor(p.getUniqueId())).forEach(p -> {
                                    if (p.isOnline())
                                        p.getPlayer().sendMessage(Component.text("You are no longer the Successor of the ").color(NamedTextColor.LIGHT_PURPLE)
                                                .append(Component.text("[" + factionData.name() + "]").color(fColor))
                                                .append(Component.text(" faction")));
                                    factions.setPlayerData(p.getUniqueId(), faction, (byte)0);
                                });
                                factions.setPlayerData(newSuccessor.getUniqueId(), faction, (byte)2);
                                newSuccessor.sendMessage(Component.text("You are now the Successor of the ").color(NamedTextColor.LIGHT_PURPLE)
                                        .append(Component.text("[" + factionData.name() + "]").color(fColor))
                                        .append(Component.text("!").color(NamedTextColor.LIGHT_PURPLE)));
                                sender.sendMessage(Component.text("Made ")
                                        .append(newSuccessor.name())
                                        .append(Component.text(" the Successor of your faction")).color(NamedTextColor.LIGHT_PURPLE));
                            } else if (newSuccessor == null) {
                                sender.sendMessage(Component.text("That player doesn't exist (or is not online at the moment)").color(NamedTextColor.RED));
                            } else if (!sameFact2) {
                                sender.sendMessage(Component.text("That player isn't in your faction!").color(NamedTextColor.RED));
                            } else {
                                sender.sendMessage(Component.text("You can't make that player the Successor of your faction").color(NamedTextColor.RED));
                            }
                            break;
                        case "private":
                            boolean newP = args[2].equalsIgnoreCase("true");
                            if (!newP && !args[2].equalsIgnoreCase("false")) {
                                sender.sendMessage(Component.text("Not a true/false value: ").color(NamedTextColor.RED)
                                        .append(Component.text(args[2]).color(NamedTextColor.GRAY)));
                                return false;
                            }
                            sender.sendMessage(Component.text("Set ")
                                    .append(Component.text("[" + factionData.name() + "]").color(factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.WHITE))
                                    .append(Component.text(" to be a "))
                                    .append(Component.text(newP ? "private" : "freely joinable").color(NamedTextColor.RED))
                                    .append(Component.text(" faction")));
                            factions.setFactionData(faction, factionData.name(), factionData.description(), factionData.color(), newP, factionData.applicants(), factionData.banned());
                            break;
                        default:
                            invalid(sender);
                            return false;
                    }
                    return true;
                case "ban":
                    if (!factions.isFactionLeader(uuid)) {
                        invalid(sender);
                        break;
                    } else if (faction == null) {
                        notInFaction(sender);
                        break;
                    }
                    return false;
                case "disband":
                    if (!factions.isFactionLeader(uuid)) {
                        invalid(sender);
                        break;
                    } else if (faction == null) {
                        notInFaction(sender);
                        break;
                    }
                    boolean bl = factions.removeFactionData(faction);
                    factions.getAllPlayersInFaction(args[1]).forEach(p -> {
                        factions.removePlayerData(p.getUniqueId());
                        if (p.isOnline()) {
                            Component playerName = player.name().color(player.isOp() ? NamedTextColor.DARK_RED : null);
                            p.getPlayer().displayName(playerName);
                            p.getPlayer().playerListName(playerName);
                            p.getPlayer().sendMessage(Component.text("Your faction has been disbanded by your Faction Leader").color(NamedTextColor.RED));
                        }
                    });
                    if (bl && factionData != null)
                        sender.sendMessage(Component.text("You have disbanded the ")
                                .append(Component.text("[" + factionData.name() + "]").color(factionData.color() != null ? TextColor.color(factionData.color()) : null))
                                .append(Component.text(" faction")));
                    else
                        sender.sendMessage(Component.text("No faction with the ID \"" + args[1] + "\" could be found").color(NamedTextColor.RED));
                    return bl;
                case "create":
                    if (!sender.isOp()) {
                        invalid(sender);
                        break;
                    }
                    boolean validColor = args[2].matches("#[0-9a-fA-F]{6}");
                    Integer c = validColor ? Integer.parseInt(args[2].substring(1), 16) : null;
                    String name = String.join(" ", Arrays.stream(args).toList().subList(validColor ? 3 : 2, args.length));
                    factions.setFactionData(args[1], name, "We do factiony things.", c, false, List.of(), List.of());
                    sender.sendMessage(Component.text("Created the ")
                            .append(Component.text("[" + name + "]").color(c != null ? TextColor.color(c) : NamedTextColor.WHITE))
                            .append(Component.text(" faction")));
                    return true;
                case "remove":
                    if (!sender.isOp()) {
                        invalid(sender);
                        break;
                    }
                    boolean bl2 = factions.removeFactionData(args[1]);
                    FactionData factionData2 = factions.getFactionData(args[1]);
                    factions.getAllPlayersInFaction(args[1]).forEach(p -> {
                        factions.removePlayerData(p.getUniqueId());
                        if (p.isOnline()) {
                            Component playerName = player.name().color(player.isOp() ? NamedTextColor.DARK_RED : null);
                            p.getPlayer().displayName(playerName);
                            p.getPlayer().playerListName(playerName);
                            p.getPlayer().sendMessage(Component.text("Your faction has been forcefully disbanded").color(NamedTextColor.RED));
                        }
                    });
                    if (bl2 && factionData2 != null)
                        sender.sendMessage(Component.text("Removed the ")
                                .append(Component.text("[" + factionData2.name() + "]").color(factionData2.color() != null ? TextColor.color(factionData2.color()) : null))
                                .append(Component.text(" faction")));
                    else
                        sender.sendMessage(Component.text("No faction with the ID \"" + args[1] + "\" could be found").color(NamedTextColor.RED));
                    return bl2;
                default:
                    invalid(sender);
            }
        } else {
            sender.sendMessage(Component.text("You must execute this command as a player").color(NamedTextColor.RED));
        }
        return false;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        boolean fl = sender instanceof Player player && factions.isFactionLeader(player.getUniqueId());
        String faction = sender instanceof Player player ? factions.getPlayerFaction(player.getUniqueId()) : null;
        boolean f = faction != null;
        switch (args.length) {
            case 1:
                return Stream.concat(Stream.concat(Stream.concat(
                        Stream.of("leader", "successor", "members", "list"), f ?
                        Stream.of("leave") : Stream.of("join", "apply")), fl ?
                        Stream.of("modify", "disband", "ban", "unban") : Stream.empty()), sender.isOp() ?
                        Stream.of("create", "remove") : Stream.empty()).filter(s -> s.startsWith(args[0])).toList();
            case 2:
                switch (args[0]) {
                    case "join", "apply", "leader", "members":
                        if (!args[0].equals("join") && !args[0].equals("apply") || !f)
                            return factions(args[1], args[0].equals("apply"));
                    case "invite":
                        return players(sender, args[1], true);
                    case "ban":
                        if (fl)
                            return factions.getAllPlayersInFaction(faction).stream().map(OfflinePlayer::getName).toList();
                    case "modify":
                        if (fl)
                            return Stream.of("color", "name", "description", "leader", "successor", "private").filter(s -> s.startsWith(args[1])).toList();
                }
            case 3:
                switch (args[0]) {
                    case "join":
                        if (sender.isOp())
                            return players(sender, args[2], true);
                    case "modify":
                        switch (args[1]) {
                            case "color":
                                return Stream.concat(NamedTextColor.NAMES.keys().stream(), Stream.of("#")).toList();
                            case "leader", "successor":
                                return players(sender, args[2], true);
                            case "private":
                                return Stream.of("true", "false").filter(s -> s.startsWith(args[2])).toList();
                        }
                }
        }
        return List.of();
    }

    private static void invalid(CommandSender sender) {
        sender.sendMessage(Component.text("That is not a valid subcommand!").color(NamedTextColor.RED));
    }

    private static void notInFaction(CommandSender sender) {
        sender.sendMessage(Component.text("You're not in a faction!").color(NamedTextColor.RED));
    }

    private List<String> factions(String arg, boolean p) {
        return factions.getFactions().stream().filter(s -> s.startsWith(arg)).filter(s -> p == factions.isFactionPrivate(s)).toList();
    }

    private List<String> players(CommandSender sender, String arg, boolean excludeSender) {
        return factions.getServer().getOnlinePlayers().stream().filter(p -> p != sender || !excludeSender).map(p -> ((TextComponent)p.displayName()).content()).filter(s -> s.startsWith(arg)).toList();
    }
}
