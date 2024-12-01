package com.alfuwu.factions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
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
import java.util.concurrent.atomic.AtomicBoolean;
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
                            if (fData.banned().contains(uuid)) {
                                sender.sendMessage(Component.text("You're banned from this faction").color(NamedTextColor.RED));
                                return false;
                            }
                            OfflinePlayer leader = factions.getFactionLeader(args[1]);
                            if (fData.priv() && leader != null) {
                                if (fData.applicants().contains(uuid)) {
                                    sender.sendMessage(Component.text("You've already applied to this faction!").color(NamedTextColor.RED));
                                    return false;
                                } else {
                                    sender.sendMessage(Component.text("You have applied to the ").color(NamedTextColor.YELLOW)
                                            .append(Component.text("[" + fData.name() + "]").color(fData.color() != null ? TextColor.color(fData.color()) : NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/faction info " + args[1])))
                                            .append(Component.text(" faction").color(NamedTextColor.YELLOW)));
                                    fData.applicants().add(uuid);
                                    factions.setFactionData(args[1], fData.name(), fData.description(), fData.color(), true, fData.applicants(), fData.banned());
                                    if (leader.isOnline())
                                        leader.getPlayer().sendMessage(application(player, true));
                                }
                            } else {
                                joinFaction(sender, player, args[1], fData);
                            }
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
                        TextColor fColor = factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.RED;
                        sender.sendMessage(Component.text("You have left the ").color(NamedTextColor.RED)
                                .append(Component.text("[" + factions.getFactionName(faction) + "]").color(fColor).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                .append(Component.text(" faction").color(NamedTextColor.RED)));
                        List<OfflinePlayer> allPlayers = factions.getAllPlayersInFaction(faction);
                        if (factions.isFactionLeader(uuid) && allPlayers.size() > 1) {
                            OfflinePlayer successor = factions.getFactionSuccessor(faction);
                            if (successor == null)
                                successor = allPlayers.stream().filter(p -> p != player).toList().getFirst();
                            final OfflinePlayer finalSuccessor = successor;
                            factions.getPlayersInFaction(faction).forEach(p -> {
                                if (p != player)
                                    p.sendMessage(Component.text(finalSuccessor.getName() != null ? finalSuccessor.getName() : "null").color(NamedTextColor.RED)
                                            .append(Component.text(" is now the Faction Leader of the ").color(NamedTextColor.GOLD))
                                            .append(Component.text("[" + factionData.name() + "]").color(factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.GOLD).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                            .append(Component.text("!").color(NamedTextColor.GOLD)));
                            });
                        }
                        factions.removePlayerData(uuid);
                        factions.getPlayersInFaction(faction).forEach(p -> p.sendMessage(player.name().color(NamedTextColor.RED).append(Component.text(" has left the faction").color(NamedTextColor.YELLOW))));
                        Component p = player.name().color(player.isOp() ? NamedTextColor.DARK_RED : null);
                        player.displayName(p);
                        player.playerListName(p);
                        return true;
                    }
                    return false;
                case "invite":
                    if (faction == null) {
                        invalid(sender);
                    } else if (args.length != 2) {
                        sender.sendMessage(Component.text("Invalid arguments (expected <playername>)").color(NamedTextColor.RED));
                    } else {
                        Player invitee = factions.getServer().getPlayerExact(args[1]);
                        if (invitee != null) {
                            if (factions.getPlayerFaction(invitee.getUniqueId()) != null) {
                                sender.sendMessage(Component.text("That player is already in a faction").color(NamedTextColor.RED));
                                return false;
                            }
                            TextColor fColor = factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.RED;
                            AtomicBoolean sentMsg = new AtomicBoolean(false);
                            invitee.sendMessage(player.name().color(fColor)
                                    .append(Component.text(" has invited you to join the ").color(NamedTextColor.GOLD))
                                    .append(Component.text("[" + factionData.name() + "]").color(fColor).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                    .append(Component.text(" faction").color(NamedTextColor.GOLD))
                                    .appendNewline().append(Component.text("[ACCEPT] ").color(NamedTextColor.GREEN).clickEvent(ClickEvent.callback((audience) -> {
                                        if (!sentMsg.get()) {
                                            joinFaction(invitee, invitee, faction, factionData);
                                            sentMsg.set(true);
                                        } else {
                                            invitee.sendMessage(Component.text("You've already interacted with this invitation").color(NamedTextColor.RED));
                                        }
                                    })))
                                    .append(Component.text("[DECLINE]").color(NamedTextColor.RED).clickEvent(ClickEvent.callback((audience) -> {
                                        if (!sentMsg.get()) {
                                            sender.sendMessage(player.name().color(NamedTextColor.RED)
                                                    .append(Component.text(" has declined your faction invitation").color(NamedTextColor.GOLD)));
                                            sentMsg.set(true);
                                        } else {
                                            invitee.sendMessage(Component.text("You've already interacted with this invitation").color(NamedTextColor.RED));
                                        }
                                    }))));
                            return true;
                        } else {
                            sender.sendMessage(Component.text("That player doesn't exist (or is not online at the moment)").color(NamedTextColor.RED));
                        }
                    }
                    return false;
                case "leader":
                    String factionId = args.length == 1 ? faction : args[1];
                    if (factionId == null) {
                        sender.sendMessage(Component.text("You're not in a faction, so you don't have a Faction Leader").color(NamedTextColor.RED));
                        return false;
                    }
                    OfflinePlayer leader = factions.getFactionLeader(factionId);
                    if (leader != null) {
                        Integer c = factions.getFactionColor(factionId);
                        String name = factions.getFactionName(factionId);
                        TextColor fColor = c != null ? TextColor.color(c) : null;
                        sender.sendMessage(Component.text("The Faction Leader for the ")
                                .append(Component.text("[" + name + "]").color(fColor).clickEvent(ClickEvent.runCommand("/faction info " + factionId)))
                                .append(Component.text(" is " + leader.getName())));
                        return true;
                    } else {
                        sender.sendMessage(Component.text("Could not find a Faction Leader for faction \"" + factionId + "\"").color(NamedTextColor.RED));
                        return false;
                    }
                case "successor":
                    String factionId2 = args.length == 1 ? faction : args[1];
                    if (factionId2 == null) {
                        sender.sendMessage(Component.text("You're not in a faction!")
                                .appendNewline().append(Component.text("Please use "))
                                .append(Component.text("/faction successor <faction id>").color(NamedTextColor.GRAY))
                                .append(Component.text(" to get the Successor of a specific faction")).color(NamedTextColor.RED));
                        return false;
                    }
                    OfflinePlayer successor = factions.getFactionSuccessor(factionId2);
                    if (successor != null) {
                        Integer c = factions.getFactionColor(factionId2);
                        String name = factions.getFactionName(factionId2);
                        TextColor fColor = c != null ? TextColor.color(c) : null;
                        sender.sendMessage(Component.text("The Successor of the ")
                                .append(Component.text("[" + name + "]").color(fColor).clickEvent(ClickEvent.runCommand("/faction info " + factionId2)))
                                .append(Component.text(" is " + successor.getName())));
                        return true;
                    } else {
                        sender.sendMessage(Component.text("Could not find a Successor for faction \"" + factionId2 + "\"").color(NamedTextColor.RED));
                        return false;
                    }
                case "members":
                    String factionId3 = args.length == 1 ? faction : args[1];
                    if (factionId3 == null) {
                        notInFaction(sender);
                        return false;
                    }
                    FactionData factionData2 = factions.getFactionData(factionId3);
                    List<OfflinePlayer> players = factions.getAllPlayersInFaction(faction);
                    Component text = Component.text("Members of the ")
                            .append(Component.text("[" + factionData2.name() + "]").color(factionData2.color() != null ? TextColor.color(factionData2.color()) : null).clickEvent(ClickEvent.runCommand("/faction info " + factionId3)))
                            .append(Component.text(" faction:")).appendNewline();
                    boolean bl = false;
                    for (OfflinePlayer p : players) {
                        byte flags = factions.getFactionFlags(p.getUniqueId());
                        text = text.append(Component.text(bl ? ", " : "").color(NamedTextColor.GRAY))
                                .append(Component.text((p.getName() != null ? p.getName() : "[Unknown]") + (flags == 1 ? " [LEADER]" : flags == 2 ? " [SUCCESSOR]" : "")).color(p.isOnline() ? flags == 1 ? NamedTextColor.GOLD : flags == 2 ? NamedTextColor.LIGHT_PURPLE : null : flags == 1 ? TextColor.color(0xbb7700) : flags == 2 ? NamedTextColor.DARK_PURPLE : NamedTextColor.DARK_GRAY));
                        bl = true;
                    }
                    sender.sendMessage(text);
                    return true;
                case "list":
                    List<String> factionIds = factions.getFactions();
                    Component text2 = Component.text("List of all factions:").appendNewline();
                    boolean bl2 = false;
                    for (String faction2 : factionIds) {
                        FactionData fData = factions.getFactionData(faction2);
                        text2 = text2.append(Component.text(bl2 ? ", " : "").color(NamedTextColor.GRAY))
                                .append(Component.text("[" + fData.name() + "]").color(fData.color() != null ? TextColor.color(fData.color()) : null).clickEvent(ClickEvent.runCommand("/faction info " + faction2)));
                        bl2 = true;
                    }
                    sender.sendMessage(text2);
                    return true;
                case "info":
                    String factionId4 = args.length == 1 ? faction : args[1];
                    FactionData factionData3 = factions.getFactionData(factionId4);
                    if (factionData3 != null) {
                        TextColor fColor = factionData3.color() != null ? TextColor.color(factionData3.color()) : null;
                        OfflinePlayer factionLeader = factions.getFactionLeader(factionId4);
                        OfflinePlayer factionSuccessor = factions.getFactionSuccessor(factionId4);
                        sender.sendMessage(Component.text("[" + factionData3.name() + "]").color(fColor)
                                .append(Component.text(" - ").color(NamedTextColor.GRAY))
                                .append(Component.text(factionData3.description()).color(NamedTextColor.WHITE))
                                .appendNewline().append(Component.text("Faction Leader: ").color(NamedTextColor.GOLD))
                                .append(Component.text(factionLeader != null && factionLeader.getName() != null ? factionLeader.getName() : "[None]").color(fColor))
                                .appendNewline().append(Component.text("Faction Successor: ").color(NamedTextColor.LIGHT_PURPLE))
                                .append(Component.text(factionSuccessor != null && factionSuccessor.getName() != null ? factionSuccessor.getName() : "[None]").color(fColor)));
                    } else {
                        sender.sendMessage(Component.text("Could not find an applicable faction").color(NamedTextColor.RED));
                    }
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
                            TextColor fColor = c != null ? TextColor.color(c) : null;
                            sender.sendMessage(Component.text("Updated ")
                                    .append(Component.text("[" + factionData.name() + "]").color(fColor != null ? fColor : NamedTextColor.WHITE).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                    .append(Component.text("'s color")));
                            factions.getPlayersInFaction(faction).forEach(p -> updatePlayerName(p, factionData.name(), fColor));
                            break;
                        case "name":
                            String name = String.join(" ", Arrays.stream(args).toList().subList(2, args.length));
                            factions.setFactionData(faction, name, factionData.description(), factionData.color(), factionData.priv(), factionData.applicants(), factionData.banned());
                            TextColor fColor2 = factionData.color() != null ? TextColor.color(factionData.color()) : null;
                            sender.sendMessage(Component.text("Updated ")
                                    .append(Component.text("[" + name + "]").color(fColor2 != null ? fColor2 : NamedTextColor.WHITE).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                    .append(Component.text("'s name")));
                            factions.getPlayersInFaction(faction).forEach(p -> updatePlayerName(p, name, fColor2));
                            break;
                        case "description":
                            String description = String.join(" ", Arrays.stream(args).toList().subList(2, args.length));
                            factions.setFactionData(faction, factionData.name(), description, factionData.color(), factionData.priv(), factionData.applicants(), factionData.banned());
                            sender.sendMessage(Component.text("Updated ")
                                    .append(Component.text("[" + factionData.name() + "]").color(factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.WHITE).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                    .append(Component.text("'s description")));
                            break;
                        case "leader":
                            Player newLeader = factions.getServer().getPlayerExact(args[2]);
                            boolean sameFact = newLeader != null && faction.equals(factions.getPlayerFaction(newLeader.getUniqueId()));
                            if (sameFact && factions.getFactionFlags(newLeader.getUniqueId()) != 1) {
                                TextColor fColor3 = factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.GOLD;
                                factions.getPlayersInFaction(faction).forEach(p -> {
                                    if (p != player)
                                        p.sendMessage(Component.text(newLeader.getName()).color(NamedTextColor.RED)
                                                .append(Component.text(" is now the Faction Leader of the ").color(NamedTextColor.GOLD))
                                                .append(Component.text("[" + factionData.name() + "]").color(fColor3).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                                .append(Component.text("!").color(NamedTextColor.GOLD)));
                                });
                                factions.setPlayerData(uuid, faction, (byte) 0);
                                factions.setPlayerData(newLeader.getUniqueId(), faction, (byte) 1);
                                sender.sendMessage(Component.text("You are no longer the Faction Leader of the ").color(NamedTextColor.GOLD)
                                        .append(Component.text("[" + factionData.name() + "]").color(fColor3).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                        .append(Component.text(" faction")));
                                newLeader.sendMessage(Component.text("You are now the Faction Leader of the ").color(NamedTextColor.GOLD)
                                        .append(Component.text("[" + factionData.name() + "]").color(fColor3).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
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
                                TextColor fColor3 = factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.LIGHT_PURPLE;
                                factions.getAllPlayersInFaction(faction).stream().filter(p -> factions.isFactionSuccessor(p.getUniqueId())).forEach(p -> {
                                    if (p.isOnline())
                                        p.getPlayer().sendMessage(Component.text("You are no longer the Successor of the ").color(NamedTextColor.LIGHT_PURPLE)
                                                .append(Component.text("[" + factionData.name() + "]").color(fColor3).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                                .append(Component.text(" faction")));
                                    factions.setPlayerData(p.getUniqueId(), faction, (byte) 0);
                                });
                                factions.getPlayersInFaction(faction).forEach(p -> {
                                    if (p != player)
                                        p.sendMessage(Component.text(newSuccessor.getName()).color(NamedTextColor.RED)
                                                .append(Component.text(" is now the Successor of the ").color(NamedTextColor.LIGHT_PURPLE))
                                                .append(Component.text("[" + factionData.name() + "]").color(fColor3).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                                .append(Component.text("!").color(NamedTextColor.LIGHT_PURPLE)));
                                });
                                factions.setPlayerData(newSuccessor.getUniqueId(), faction, (byte) 2);
                                newSuccessor.sendMessage(Component.text("You are now the Successor of the ").color(NamedTextColor.LIGHT_PURPLE)
                                        .append(Component.text("[" + factionData.name() + "]").color(fColor3).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
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
                                    .append(Component.text("[" + factionData.name() + "]").color(factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.WHITE).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
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
                    List<UUID> banned = factionData.banned();
                    OfflinePlayer offlinePlayer = factions.getServer().getOfflinePlayer(args[1]);
                    if (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) {
                        boolean isBanned = banned.contains(offlinePlayer.getUniqueId());
                        if (!isBanned) {
                            banned.add(offlinePlayer.getUniqueId());
                            String bannedfaction = factions.getPlayerFaction(offlinePlayer.getUniqueId());
                            if (faction.equals(bannedfaction)) {
                                factions.setPlayerData(offlinePlayer.getUniqueId(), null, (byte) 0);
                                Component playerName = offlinePlayer.getPlayer().name().color(offlinePlayer.isOp() ? NamedTextColor.DARK_RED : null);
                                offlinePlayer.getPlayer().displayName(playerName);
                                offlinePlayer.getPlayer().playerListName(playerName);
                                factions.getPlayersInFaction(faction).stream().filter(p -> p != player).forEach(p -> p.sendMessage(Component.text(offlinePlayer.getName()).color(NamedTextColor.RED)
                                        .append(Component.text(" has been banned from the faction").color(NamedTextColor.GOLD))));
                            }
                            factions.setFactionData(faction, factionData.name(), factionData.description(), factionData.color(), factionData.priv(), factionData.applicants(), banned);
                            sender.sendMessage(Component.text("Banned ").color(NamedTextColor.GOLD)
                                    .append(Component.text(offlinePlayer.getName()).color(NamedTextColor.RED))
                                    .append(Component.text(" from your faction").color(NamedTextColor.GOLD)));
                            if (offlinePlayer.isOnline())
                                offlinePlayer.getPlayer().sendMessage(Component.text("You have been banned from the ").color(NamedTextColor.RED)
                                        .append(Component.text("[" + factionData.name() + "]").color(factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                        .append(Component.text(" faction").color(NamedTextColor.RED)));
                            return true;
                        } else {
                            sender.sendMessage(Component.text("That player is already banned from your faction").color(NamedTextColor.RED));
                        }
                    } else {
                        sender.sendMessage(Component.text("That player doesn't exist").color(NamedTextColor.RED));
                    }
                    return false;
                case "unban":
                    if (!factions.isFactionLeader(uuid)) {
                        invalid(sender);
                        break;
                    } else if (faction == null) {
                        notInFaction(sender);
                        break;
                    }
                    List<UUID> banned2 = factionData.banned();
                    OfflinePlayer offlinePlayer2 = factions.getServer().getOfflinePlayer(args[1]);
                    if (offlinePlayer2.hasPlayedBefore() && offlinePlayer2.getName() != null) {
                        boolean wasBanned = banned2.remove(offlinePlayer2.getUniqueId());
                        if (wasBanned) {
                            factions.setFactionData(faction, factionData.name(), factionData.description(), factionData.color(), factionData.priv(), factionData.applicants(), banned2);
                            sender.sendMessage(Component.text("Unbanned ").color(null)
                                    .append(Component.text(offlinePlayer2.getName()).color(NamedTextColor.RED))
                                    .append(Component.text(" from your faction").color(null)));
                            if (offlinePlayer2.isOnline())
                                offlinePlayer2.getPlayer().sendMessage(Component.text("You have been unbanned from the ").color(NamedTextColor.GOLD)
                                        .append(Component.text("[" + factionData.name() + "]").color(factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.GOLD).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                        .append(Component.text(" faction").color(NamedTextColor.GOLD)));
                            return true;
                        } else {
                            sender.sendMessage(Component.text("That player isn't banned!").color(NamedTextColor.RED));
                        }
                    } else {
                        sender.sendMessage(Component.text("That player doesn't exist").color(NamedTextColor.RED));
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
                    boolean bl3 = factions.removeFactionData(faction);
                    factions.getAllPlayersInFaction(args[1]).forEach(p -> {
                        factions.removePlayerData(p.getUniqueId());
                        if (p.isOnline()) {
                            Component playerName = player.name().color(player.isOp() ? NamedTextColor.DARK_RED : null);
                            p.getPlayer().displayName(playerName);
                            p.getPlayer().playerListName(playerName);
                            p.getPlayer().sendMessage(Component.text("Your faction has been disbanded by your Faction Leader").color(NamedTextColor.RED));
                        }
                    });
                    if (bl3 && factionData != null)
                        sender.sendMessage(Component.text("You have disbanded the ")
                                .append(Component.text("[" + factionData.name() + "]").color(factionData.color() != null ? TextColor.color(factionData.color()) : null))
                                .append(Component.text(" faction")));
                    else
                        sender.sendMessage(Component.text("No faction with the ID \"" + args[1] + "\" could be found").color(NamedTextColor.RED));
                    return bl3;
                case "applicants":
                    if (!factions.isFactionLeader(uuid) || faction != null && !factionData.priv()) {
                        invalid(sender);
                        break;
                    } else if (faction == null) {
                        notInFaction(sender);
                        break;
                    }
                    if (args.length == 1) { // list applicants
                        if (factionData.applicants().isEmpty()) {
                            sender.sendMessage(Component.text("Your faction has no applicants").color(NamedTextColor.GOLD));
                        } else {
                            Component text3 = Component.text("Applicants:");
                            for (UUID applicant : factionData.applicants()) {
                                OfflinePlayer offlinePlayer3 = factions.getServer().getOfflinePlayer(applicant);
                                String name = offlinePlayer3.getName();
                                if (name != null)
                                    text3 = text3.appendNewline()
                                            .append(application(offlinePlayer3, false));
                            }
                            sender.sendMessage(text3);
                        }
                        return true;
                    } else if (args.length == 3) {
                        if (args[1].equals("accept") || args[1].equals("a")) {
                            joinFaction(null, factions.getServer().getOfflinePlayer(args[2]), faction, factionData);
                        } else if (args[1].equals("deny") || args[1].equals("d")) {
                            if (factionData.applicants().remove(factions.getServer().getOfflinePlayer(args[2]).getUniqueId())) {
                                sender.sendMessage(Component.text("Denied ")
                                        .append(Component.text(args[2]).color(NamedTextColor.RED))
                                        .append(Component.text("'s application")));
                                factions.setFactionData(faction, factionData.name(), factionData.description(), factionData.color(), true, factionData.applicants(), factionData.banned());
                            } else {
                                sender.sendMessage(Component.text("That player hasn't applied to your faction!").color(NamedTextColor.RED));
                            }
                        } else {
                            sender.sendMessage(Component.text("Invalid arguments (expected <accept|deny> <playername>").color(NamedTextColor.RED));
                        }
                        return true;
                    }
                    sender.sendMessage(Component.text("Invalid arguments (expected <accept|deny> <playername>").color(NamedTextColor.RED));
                    break;
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
                    boolean bl4 = factions.removeFactionData(args[1]);
                    FactionData factionData5 = factions.getFactionData(args[1]);
                    factions.getAllPlayersInFaction(args[1]).forEach(p -> {
                        factions.removePlayerData(p.getUniqueId());
                        if (p.isOnline()) {
                            Component playerName = player.name().color(player.isOp() ? NamedTextColor.DARK_RED : null);
                            p.getPlayer().displayName(playerName);
                            p.getPlayer().playerListName(playerName);
                            p.getPlayer().sendMessage(Component.text("Your faction has been forcefully disbanded").color(NamedTextColor.RED));
                        }
                    });
                    if (bl4 && factionData5 != null)
                        sender.sendMessage(Component.text("Removed the ")
                                .append(Component.text("[" + factionData5.name() + "]").color(factionData5.color() != null ? TextColor.color(factionData5.color()) : null))
                                .append(Component.text(" faction")));
                    else
                        sender.sendMessage(Component.text("No faction with the ID \"" + args[1] + "\" could be found").color(NamedTextColor.RED));
                    return bl4;
                default:
                    invalid(sender);
                    break;
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
        boolean priv = f && factions.isFactionPrivate(faction);
        switch (args.length) {
            case 1:
                return Stream.concat(Stream.concat(Stream.concat(Stream.concat(
                        Stream.of("leader", "successor", "members", "list", "info"), f ?
                        Stream.of("leave", "invite") : Stream.of("join", "apply")), fl ?
                        Stream.of("modify", "disband", "ban", "unban") : Stream.empty()), fl && priv ?
                        Stream.of("applicants") : Stream.empty()), sender.isOp() ?
                        Stream.of("create", "remove") : Stream.empty()).filter(s -> s.startsWith(args[0])).toList();
            case 2:
                switch (args[0]) {
                    case "join", "leader", "successor", "members", "info":
                        if (!args[0].equals("join") || !f)
                            return factions(args[1]);
                    case "invite":
                        return players(sender, args[1], true);
                    case "ban":
                        if (fl)
                            return Stream.concat(players(sender, args[1], true).stream(), factions.getAllPlayersInFaction(faction).stream().filter(p -> !p.isOnline()).map(OfflinePlayer::getName).filter(s -> s != null && s.startsWith(args[1]))).toList();
                    case "unban":
                        if (fl)
                            return Stream.concat(factions.getServer().getOnlinePlayers().stream(), Arrays.stream(factions.getServer().getOfflinePlayers())).filter(p -> factions.getBannedPlayersForFaction(faction).contains(p.getUniqueId())).map(OfflinePlayer::getName).filter(s -> s != null && s.startsWith(args[1])).toList();
                    case "modify":
                        if (fl)
                            return Stream.of("color", "name", "description", "leader", "successor", "private").filter(s -> s.startsWith(args[1])).toList();
                    case "applicants":
                        if (fl && priv)
                            return Stream.of("accept", "deny", "a", "d").filter(s -> s.startsWith(args[1])).toList();
                }
            case 3:
                switch (args[0]) {
                    case "join":
                        if (sender.isOp())
                            return players(sender, args[2], true);
                    case "modify":
                        if (fl)
                            switch (args[1]) {
                                case "color":
                                    return Stream.concat(NamedTextColor.NAMES.keys().stream(), Stream.of("#")).filter(s -> s.startsWith(args[2])).toList();
                                case "leader", "successor":
                                    return players(sender, args[2], true);
                                case "private":
                                    return Stream.of("true", "false").filter(s -> s.startsWith(args[2])).toList();
                            }
                    case "applicants":
                        if (fl && priv && (args[1].equals("accept") || args[1].equals("a") || args[1].equals("deny") || args[1].equals("d")))
                            return factions.getApplicantsForFaction(faction).stream().map(u -> factions.getServer().getOfflinePlayer(u).getName()).filter(s -> s != null && s.startsWith(args[2])).toList();
                }
        }
        return List.of();
    }

    private void joinFaction(CommandSender sender, OfflinePlayer player, String id, FactionData fData) {
        List<Player> players = factions.getPlayersInFaction(id);
        TextColor fColor = fData.color() != null ? TextColor.color(fData.color()) : null;
        players.forEach(p -> p.sendMessage(Component.text(player.getName() != null ? player.getName() : "null").color(fColor != null ? fColor : NamedTextColor.RED).append(Component.text(" has joined the faction").color(NamedTextColor.YELLOW))));
        if (sender != null)
            sender.sendMessage(Component.text("You have joined the ").color(NamedTextColor.YELLOW)
                    .append(Component.text("[" + fData.name() + "]").color(fColor != null ? fColor : NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/faction info " + id)))
                    .append(Component.text(" faction!").color(NamedTextColor.YELLOW)));
        if (players.isEmpty() && sender != null) {
            sender.sendMessage(Component.text("You are now the Faction Leader of the ").color(NamedTextColor.GOLD)
                    .append(Component.text("[" + fData.name() + "]").color(fColor != null ? fColor : NamedTextColor.GOLD).clickEvent(ClickEvent.runCommand("/faction info " + id)))
                    .append(Component.text("!").color(NamedTextColor.GOLD)));
        } else if (players.size() == 1 && sender != null) {
            sender.sendMessage(Component.text("You are now the Successor of the ").color(NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text("[" + fData.name() + "]").color(fColor != null ? fColor : NamedTextColor.LIGHT_PURPLE).clickEvent(ClickEvent.runCommand("/faction info " + id)))
                    .append(Component.text("!").color(NamedTextColor.LIGHT_PURPLE)));
            players.getFirst().sendMessage(Component.text(sender.getName()).color(NamedTextColor.RED)
                    .append(Component.text(" is now the Successor of the ").color(NamedTextColor.LIGHT_PURPLE))
                    .append(Component.text("[" + fData.name() + "]").color(fColor != null ? fColor : NamedTextColor.LIGHT_PURPLE).clickEvent(ClickEvent.runCommand("/faction info " + id)))
                    .append(Component.text("!").color(NamedTextColor.LIGHT_PURPLE)));
        }
        if (player.isOnline())
            updatePlayerName(player.getPlayer(), fData.name(), fColor);
        if (fData.applicants().remove(player.getUniqueId()))
            factions.setFactionData(id, fData.name(), fData.description(), fData.color(), fData.priv(), fData.applicants(), fData.banned());
        factions.setPlayerData(player.getUniqueId(), id, (byte)(players.isEmpty() ? 1 : players.size() == 1 ? 2 : 0));
    }

    private static void updatePlayerName(Player player, String faction, TextColor color) {
        Component p = Component.text("[" + faction + "] ").color(color != null ? color : NamedTextColor.WHITE).clickEvent(ClickEvent.runCommand("/faction info " + faction))
                .append(player.name().color(player.isOp() ? NamedTextColor.DARK_RED : color != null ? color : NamedTextColor.WHITE));
        player.displayName(p);
        player.playerListName(p);
    }

    private static Component application(OfflinePlayer applier, boolean msg) {
        return Component.text((applier.getName() != null ? applier.getName() : "null") + (msg ? " would like to join your faction" : "")).color(NamedTextColor.YELLOW)
                .append(Component.text(" [ACCEPT]").color(NamedTextColor.GREEN).clickEvent(ClickEvent.runCommand("/faction applicants a " + applier.getName())))
                .append(Component.text(" [DENY]").color(NamedTextColor.RED).clickEvent(ClickEvent.runCommand("/faction applicants d " + applier.getName())));
    }

    private static void invalid(CommandSender sender) {
        sender.sendMessage(Component.text("That is not a valid subcommand!").color(NamedTextColor.RED));
    }

    private static void notInFaction(CommandSender sender) {
        sender.sendMessage(Component.text("You're not in a faction!").color(NamedTextColor.RED));
    }

    private List<String> factions(String arg) {
        return factions.getFactions().stream().filter(s -> s.startsWith(arg)).toList();
    }

    private List<String> players(CommandSender sender, String arg, boolean excludeSender) {
        return factions.getServer().getOnlinePlayers().stream().filter(p -> p != sender || !excludeSender).map(p -> ((TextComponent)p.displayName()).content()).filter(s -> s.startsWith(arg)).toList();
    }
}
