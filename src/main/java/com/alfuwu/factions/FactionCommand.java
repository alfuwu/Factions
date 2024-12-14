package com.alfuwu.factions;

import net.kyori.adventure.audience.Audience;
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
import java.util.Objects;
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
            UUID uuid = player.getUniqueId();
            if (args.length == 0 || args.length == 1 && args[0].equals("help")) {
                Component message = Component.text("Faction Command Usage:\n").color(NamedTextColor.RED)
                        .append(Component.text("/faction join <id> [playername]").color(NamedTextColor.GOLD))
                        .append(Component.text(" - Join a faction (server operators can forcefully make others join a faction with this command)\n").color(NamedTextColor.WHITE))
                        .append(Component.text("/faction leave [playername]").color(NamedTextColor.GOLD))
                        .append(Component.text(" - Leave your faction (server operators can forcefully make others leave their faction with this command)\n").color(NamedTextColor.WHITE))
                        .append(Component.text("/faction invite <playername>").color(NamedTextColor.GOLD))
                        .append(Component.text(" - Invite a factionless player to your faction\n").color(NamedTextColor.WHITE))
                        .append(Component.text("/faction list").color(NamedTextColor.GOLD))
                        .append(Component.text(" - List all factions\n").color(NamedTextColor.WHITE))
                        .append(Component.text("/faction info [id]").color(NamedTextColor.GOLD))
                        .append(Component.text(" - View faction details").color(NamedTextColor.WHITE));
                if (!factions.opOnlyFactionCreation)
                    message = message.append(Component.text("\n/faction create <id> [color] <name>").color(NamedTextColor.GOLD))
                            .append(Component.text(" - Create a new faction").color(NamedTextColor.WHITE));
                if (factions.isFactionLeader(uuid))
                    message = message.append(Component.text("\nFaction Leader Commands:\n").color(NamedTextColor.RED))
                        .append(Component.text("/faction modify <subcommand>").color(NamedTextColor.GOLD))
                        .append(Component.text(" - Modify your faction (color, name, description, etc)\n").color(NamedTextColor.WHITE))
                        .append(Component.text("/faction disband").color(NamedTextColor.GOLD))
                        .append(Component.text(" - Disband your faction\n").color(NamedTextColor.WHITE))
                        .append(Component.text("/faction ban <playername>").color(NamedTextColor.GOLD))
                        .append(Component.text(" - Ban a player from your faction\n").color(NamedTextColor.WHITE))
                        .append(Component.text("/faction unban <playername>").color(NamedTextColor.GOLD))
                        .append(Component.text(" - Unban a player from your faction").color(NamedTextColor.WHITE));
                if (player.isOp() && !factions.opOnlyFactionCreation)
                    message = message.append(Component.text("\nOperator Commands:\n").color(NamedTextColor.RED))
                            .append(Component.text("/faction remove <id>").color(NamedTextColor.GOLD))
                            .append(Component.text(" - Remove a faction").color(NamedTextColor.WHITE));
                else if (player.isOp())
                    message = message.append(Component.text("\nOperator Commands:\n").color(NamedTextColor.RED))
                            .append(Component.text("/faction create <id> [color] <name>").color(NamedTextColor.GOLD))
                            .append(Component.text(" - Create a new faction\n").color(NamedTextColor.WHITE))
                            .append(Component.text("/faction remove <id>").color(NamedTextColor.GOLD))
                            .append(Component.text(" - Remove a faction").color(NamedTextColor.WHITE));
                sender.sendMessage(message);
                return true;
            }
            String faction = factions.getPlayerFaction(uuid);
            FactionData factionData = faction != null ? factions.getFactionData(faction) : null;
            switch (args[0]) {
                case "join":
                    boolean forcedJoin = false;
                    if (args.length < 2) {
                        sender.sendMessage(Component.text("Please provide a faction ID").color(NamedTextColor.RED));
                        return false;
                    }
                    if (sender.isOp() && args.length >= 3) {
                        OfflinePlayer forcedPlayer = factions.getServer().getOfflinePlayer(args[2]);
                        if (forcedPlayer.hasPlayedBefore()) {
                            uuid = forcedPlayer.getUniqueId();
                            forcedJoin = true;
                        } else {
                            sender.sendMessage(Component.text("That player doesn't exist!").color(NamedTextColor.RED));
                            return false;
                        }
                    }
                    if (factions.getPlayerFaction(uuid) == null) {
                        FactionData fData = factions.getFactionData(args[1]);
                        if (fData != null) {
                            OfflinePlayer joinee = factions.getServer().getOfflinePlayer(uuid);
                            if (fData.banned().contains(uuid) && !forcedJoin) {
                                sender.sendMessage(Component.text("You're banned from this faction").color(NamedTextColor.RED));
                                return false;
                            }
                            OfflinePlayer leader = factions.getFactionLeader(args[1]);
                            if (fData.priv() && leader != null && !forcedJoin) {
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
                                joinFaction(joinee, args[1], fData);
                                if (forcedJoin)
                                    sender.sendMessage(Component.text("Successfully added ")
                                            .append(Component.text(joinee.getName() != null ? joinee.getName() : "null").color(NamedTextColor.RED))
                                            .append(Component.text(" to the "))
                                            .append(Component.text("[" + fData.name() + "]").color(fData.color() != null ? TextColor.color(fData.color()) : null).clickEvent(ClickEvent.runCommand("/faction info " + args[1])))
                                            .append(Component.text(" faction")));
                            }
                            return true;
                        } else {
                            sender.sendMessage(Component.text("That faction doesn't exist!").color(NamedTextColor.RED));
                        }
                    } else {
                        sender.sendMessage(Component.text((forcedJoin ? "That player is" : "You're") + " already in a faction!").color(NamedTextColor.RED));
                    }
                    return false;
                case "leave":
                    boolean forcedLeave = false;
                    String leftFact = faction;
                    FactionData lfData = factionData;
                    if (sender.isOp() && args.length >= 2) {
                        OfflinePlayer forcedPlayer = factions.getServer().getOfflinePlayer(args[1]);
                        if (forcedPlayer.hasPlayedBefore()) {
                            uuid = forcedPlayer.getUniqueId();
                            leftFact = factions.getPlayerFaction(uuid);
                            lfData = factions.getFactionData(leftFact);
                            forcedLeave = true;
                        } else {
                            sender.sendMessage(Component.text("That player doesn't exist!").color(NamedTextColor.RED));
                            return false;
                        }
                    }
                    OfflinePlayer leavee = factions.getServer().getOfflinePlayer(uuid);
                    if (leftFact == null) {
                        if (forcedLeave)
                            sender.sendMessage(Component.text("That player isn't in a faction!").color(NamedTextColor.RED));
                        else
                            notInFaction(sender);
                    } else {
                        TextColor fColor = lfData.color() != null ? TextColor.color(lfData.color()) : NamedTextColor.RED;
                        if (leavee.isOnline())
                            leavee.getPlayer().sendMessage(Component.text("You have left the ").color(NamedTextColor.RED)
                                    .append(Component.text("[" + lfData.name() + "]").color(fColor).clickEvent(ClickEvent.runCommand("/faction info " + leftFact)))
                                    .append(Component.text(" faction").color(NamedTextColor.RED)));
                        List<OfflinePlayer> allPlayers = factions.getAllPlayersInFaction(leftFact);
                        if (factions.isFactionLeader(uuid) && allPlayers.size() > 1)
                            succession(leavee, leftFact, lfData, allPlayers);
                        factions.removePlayerData(uuid);
                        Audience.audience(factions.getPlayersInFaction(leftFact)).sendMessage(Component.text(leavee.getName() != null ? leavee.getName() : "null").color(NamedTextColor.RED)
                                .append(Component.text(" has left the faction").color(NamedTextColor.YELLOW)));
                        if (leavee.isOnline()) {
                            Component p = Component.text(leavee.getName() != null ? leavee.getName() : "null").color(leavee.isOp() && factions.specialOpNameColor ? NamedTextColor.DARK_RED : null);
                            leavee.getPlayer().displayName(p);
                            leavee.getPlayer().customName(p);
                            leavee.getPlayer().playerListName(p);
                        }
                        if (forcedLeave)
                            sender.sendMessage(Component.text("Successfully removed ")
                                    .append(Component.text(leavee.getName() != null ? leavee.getName() : "null").color(NamedTextColor.RED))
                                    .append(Component.text(" from the "))
                                    .append(Component.text("[" + lfData.name() + "]").color(lfData.color() != null ? TextColor.color(lfData.color()) : null).clickEvent(ClickEvent.runCommand("/faction info " + leftFact)))
                                    .append(Component.text(" faction")));
                        return true;
                    }
                    return false;
                case "invite":
                    if (faction == null) {
                        invalid(sender);
                    } else if (args.length < 2) {
                        sender.sendMessage(Component.text("Invalid arguments (expected <playername>)").color(NamedTextColor.RED));
                    } else {
                        Player invitee = factions.getServer().getPlayerExact(args[1]);
                        if (invitee != null) {
                            if (factions.getPlayerFaction(invitee.getUniqueId()) != null) {
                                if (invitee == player)
                                    sender.sendMessage(Component.text("You can't send an invite to yourself, silly!").color(NamedTextColor.RED));
                                else
                                   sender.sendMessage(Component.text("That player is already in a faction").color(NamedTextColor.RED));
                                return false;
                            }
                            sender.sendMessage(Component.text("Sent an invitation to ").color(NamedTextColor.GOLD)
                                    .append(invitee.displayName()));
                            TextColor fColor = factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.RED;
                            AtomicBoolean sentMsg = new AtomicBoolean(false);
                            invitee.sendMessage(player.name().color(fColor)
                                    .append(Component.text(" has invited you to join the ").color(NamedTextColor.GOLD))
                                    .append(Component.text("[" + factionData.name() + "]").color(fColor).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                    .appendNewline().append(Component.text("[ACCEPT] ").color(NamedTextColor.GREEN).clickEvent(ClickEvent.callback((audience) -> {
                                        if (!sentMsg.get()) {
                                            sender.sendMessage(invitee.displayName()
                                                    .append(Component.text(" has accepted your invitation!").color(NamedTextColor.GOLD)));
                                            joinFaction(invitee, faction, factionData);
                                            sentMsg.set(true);
                                        } else {
                                            invitee.sendMessage(Component.text("You've already interacted with this invitation").color(NamedTextColor.RED));
                                        }
                                    })))
                                    .append(Component.text("[DECLINE]").color(NamedTextColor.RED).clickEvent(ClickEvent.callback((audience) -> {
                                        if (!sentMsg.get()) {
                                            sender.sendMessage(player.name().color(NamedTextColor.RED)
                                                    .append(Component.text(" has declined your invitation").color(NamedTextColor.GOLD)));
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
                        sender.sendMessage(Component.text("You're not in a faction!")
                                .appendNewline().append(Component.text("Please use "))
                                .append(Component.text("/faction leader <faction id>").color(NamedTextColor.GRAY))
                                .append(Component.text(" to get the Faction Leader of a specific faction")).color(NamedTextColor.RED));
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
                    String factionId3 = args.length == 1 ? null : args[1];
                    FactionData factionData2 = factions.getFactionData(factionId3);
                    if (factionId3 == null) {
                        notInFaction(sender);
                        return false;
                    } else if (factionData2 == null) {
                        sender.sendMessage(Component.text("That faction doesn't exist!").color(NamedTextColor.RED));
                        return false;
                    }
                    List<OfflinePlayer> players = factions.getAllPlayersInFaction(factionId3);
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
                    if (factionId4 == null) {
                        notInFaction(sender);
                        return false;
                    } else if (factionData3 != null) {
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
                    } else if (faction == null) { // this should never be true
                        // but JUST IN CASE
                        notInFaction(sender);
                        break;
                    }
                    if (args.length == 1) {
                        sender.sendMessage(Component.text("/faction modify").color(NamedTextColor.RED)
                                .append(Component.text(" - use this command to modify parts of your faction, such as its color, name, description, etc").color(NamedTextColor.GOLD)));
                        return true;
                    }
                    switch (args[1]) {
                        case "color":
                            boolean validHexColor = args.length >= 3 && args[2].matches("#?[0-9a-fA-F]{6}");
                            NamedTextColor n = args.length >= 3 ? NamedTextColor.NAMES.value(args[2]) : null;
                            Integer c = validHexColor ? Integer.valueOf(Integer.parseInt(args[2].substring(args[2].startsWith("#") ? 1 : 0), 16)) : n != null ? n.value() : null;
                            factions.setFactionData(faction, factionData.name(), factionData.description(), c, factionData.priv(), factionData.applicants(), factionData.banned());
                            TextColor fColor = c != null ? TextColor.color(c) : null;
                            sender.sendMessage(Component.text("Updated ")
                                    .append(Component.text("[" + factionData.name() + "]").color(fColor != null ? fColor : NamedTextColor.WHITE).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                    .append(Component.text("'s color")));
                            factions.getPlayersInFaction(faction).forEach(p -> updatePlayerName(p, faction, factionData.name(), fColor));
                            break;
                        case "name":
                            String name = String.join(" ", Arrays.stream(args).toList().subList(2, args.length));
                            factions.setFactionData(faction, name, factionData.description(), factionData.color(), factionData.priv(), factionData.applicants(), factionData.banned());
                            TextColor fColor2 = factionData.color() != null ? TextColor.color(factionData.color()) : null;
                            sender.sendMessage(Component.text("Updated ")
                                    .append(Component.text("[" + name + "]").color(fColor2 != null ? fColor2 : NamedTextColor.WHITE).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                    .append(Component.text("'s name")));
                            factions.getPlayersInFaction(faction).forEach(p -> updatePlayerName(p, faction, name, fColor2));
                            break;
                        case "description":
                            String description = String.join(" ", Arrays.stream(args).toList().subList(2, args.length));
                            factions.setFactionData(faction, factionData.name(), description, factionData.color(), factionData.priv(), factionData.applicants(), factionData.banned());
                            sender.sendMessage(Component.text("Updated ")
                                    .append(Component.text("[" + factionData.name() + "]").color(factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.WHITE).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                    .append(Component.text("'s description")));
                            break;
                        case "leader":
                            if (args.length < 3) {
                                sender.sendMessage(Component.text("Please provide a player's name").color(NamedTextColor.RED));
                                return false;
                            }
                            Player newLeader = factions.getServer().getPlayerExact(args[2]);
                            boolean sameFact = newLeader != null && faction.equals(factions.getPlayerFaction(newLeader.getUniqueId()));
                            if (sameFact && factions.getFactionFlags(newLeader.getUniqueId()) != 1) {
                                TextColor fColor3 = factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.GOLD;
                                Audience.audience(factions.getPlayersInFaction(faction).stream().filter(p -> p != player).toArray(Player[]::new)).sendMessage(Component.text(newLeader.getName()).color(NamedTextColor.RED)
                                        .append(Component.text(" is now the Faction Leader of the ").color(NamedTextColor.GOLD))
                                        .append(Component.text("[" + factionData.name() + "]").color(fColor3).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                        .append(Component.text("!").color(NamedTextColor.GOLD)));
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
                            if (args.length < 3) {
                                sender.sendMessage(Component.text("Please provide a player's name").color(NamedTextColor.RED));
                                return false;
                            }
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
                                Audience.audience(factions.getPlayersInFaction(faction).stream().filter(p -> p != player).toArray(Player[]::new)).sendMessage(Component.text(newSuccessor.getName()).color(NamedTextColor.RED)
                                        .append(Component.text(" is now the Successor of the ").color(NamedTextColor.LIGHT_PURPLE))
                                        .append(Component.text("[" + factionData.name() + "]").color(fColor3).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                                        .append(Component.text("!").color(NamedTextColor.LIGHT_PURPLE)));
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
                            if (args.length < 3) {
                                sender.sendMessage(Component.text("Please provide a true/false value").color(NamedTextColor.RED));
                                return false;
                            }
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
                    if (args.length < 2) {
                        sender.sendMessage(Component.text("Please provide a player's name").color(NamedTextColor.RED));
                        return false;
                    }
                    List<UUID> banned = factionData.banned();
                    OfflinePlayer offlinePlayer = factions.getServer().getOfflinePlayer(args[1]);
                    if (offlinePlayer.hasPlayedBefore() && offlinePlayer.getName() != null) {
                        boolean isBanned = banned.contains(offlinePlayer.getUniqueId());
                        if (!isBanned) {
                            banned.add(offlinePlayer.getUniqueId());
                            String bannedFaction = factions.getPlayerFaction(offlinePlayer.getUniqueId());
                            if (faction.equals(bannedFaction)) {
                                List<OfflinePlayer> allPlayers = factions.getAllPlayersInFaction(faction);
                                if (offlinePlayer == player && allPlayers.size() > 1) // faction leader banned themselves
                                    succession(player, faction, factionData, allPlayers);
                                factions.removePlayerData(offlinePlayer.getUniqueId());
                                Component playerName = offlinePlayer.getPlayer().name().color(offlinePlayer.isOp() && factions.specialOpNameColor ? NamedTextColor.DARK_RED : null);
                                offlinePlayer.getPlayer().displayName(playerName);
                                offlinePlayer.getPlayer().customName(playerName);
                                offlinePlayer.getPlayer().playerListName(playerName);
                                Audience.audience(factions.getPlayersInFaction(faction).stream().filter(p -> p != player).toArray(Player[]::new)).sendMessage(Component.text(offlinePlayer.getName()).color(NamedTextColor.RED)
                                        .append(Component.text(" has been banned from the faction").color(NamedTextColor.GOLD)));
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
                    if (args.length < 2) {
                        sender.sendMessage(Component.text("Please provide a banned player's name").color(NamedTextColor.RED));
                        return false;
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
                    factions.getAllPlayersInFaction(faction).forEach(p -> {
                        factions.removePlayerData(p.getUniqueId());
                        if (p.isOnline()) {
                            Component playerName = player.name().color(player.isOp() && factions.specialOpNameColor ? NamedTextColor.DARK_RED : null);
                            p.getPlayer().displayName(playerName);
                            p.getPlayer().customName(playerName);
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
                            OfflinePlayer joinee = factions.getServer().getOfflinePlayer(args[2]);
                            if (factions.getPlayerFaction(joinee.getUniqueId()) != null) {
                                sender.sendMessage(Component.text("This application has expired").color(NamedTextColor.RED));
                                factionData.applicants().remove(joinee.getUniqueId());
                                factions.setFactionData(faction, factionData.name(), factionData.description(), factionData.color(), true, factionData.applicants(), factionData.banned());
                                break;
                            } else if (!joinee.hasPlayedBefore()) {
                                sender.sendMessage(Component.text("That player doesn't exist!").color(NamedTextColor.RED));
                                break;
                            } else if (!factionData.applicants().contains(joinee.getUniqueId())) {
                                sender.sendMessage(Component.text("That player hasn't applied to your faction!").color(NamedTextColor.RED));
                                break;
                            }
                            joinFaction(joinee, faction, factionData);
                        } else if (args[1].equals("deny") || args[1].equals("d")) {
                            if (factionData.applicants().remove(factions.getServer().getOfflinePlayer(args[2]).getUniqueId())) {
                                sender.sendMessage(Component.text("Denied ")
                                        .append(Component.text(args[2]).color(NamedTextColor.RED))
                                        .append(Component.text("'s application")));
                                factions.setFactionData(faction, factionData.name(), factionData.description(), factionData.color(), true, factionData.applicants(), factionData.banned());
                            } else {
                                sender.sendMessage(Component.text("That player hasn't applied to your faction!").color(NamedTextColor.RED));
                                break;
                            }
                        } else {
                            sender.sendMessage(Component.text("Invalid arguments (expected <accept|deny> <playername>").color(NamedTextColor.RED));
                            break;
                        }
                        return true;
                    }
                    sender.sendMessage(Component.text("Invalid arguments (expected <accept|deny> <playername>").color(NamedTextColor.RED));
                    break;
                case "create":
                    if (!sender.isOp() && factions.opOnlyFactionCreation) {
                        invalid(sender);
                        break;
                    }
                    if (args.length < 2) {
                        sender.sendMessage(Component.text("Please provide a faction ID").color(NamedTextColor.RED));
                        return false;
                    }
                    String id = args[1];
                    List<String> currentFactionIds = factions.getFactions();
                    if (!currentFactionIds.contains(id)) {
                        boolean validColor = args.length >= 3 && args[2].matches("#[0-9a-fA-F]{6}");
                        Integer c = validColor ? Integer.parseInt(args[2].substring(1), 16) : null;
                        String name = String.join(" ", Arrays.stream(args).toList().subList(validColor ? 3 : 2, args.length));
                        factions.setFactionData(args[1], name, "We do factiony things.", c, false, List.of(), List.of());
                        sender.sendMessage(Component.text("Created the ")
                                .append(Component.text("[" + name + "]").color(c != null ? TextColor.color(c) : NamedTextColor.WHITE))
                                .append(Component.text(" faction")));
                        return true;
                    } else {
                        sender.sendMessage(Component.text("A faction already exists with that ID").color(NamedTextColor.RED));
                    }
                    return false;
                case "remove":
                    if (!sender.isOp()) {
                        invalid(sender);
                        break;
                    }
                    if (args.length < 2) {
                        sender.sendMessage(Component.text("Please provide a faction ID").color(NamedTextColor.RED));
                        return false;
                    }
                    FactionData factionData5 = factions.getFactionData(args[1]);
                    boolean bl4 = factions.removeFactionData(args[1]);
                    factions.getAllPlayersInFaction(args[1]).forEach(p -> {
                        factions.removePlayerData(p.getUniqueId());
                        if (p.isOnline()) {
                            Component playerName = player.name().color(player.isOp() && factions.specialOpNameColor ? NamedTextColor.DARK_RED : null);
                            p.getPlayer().displayName(playerName);
                            p.getPlayer().customName(playerName);
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
                return Stream.concat(Stream.concat(Stream.concat(Stream.concat(Stream.concat(Stream.concat(
                        Stream.of("leader", "successor", "members", "list", "info", "help"), f && !sender.isOp() ?
                        Stream.of("leave", "invite") : Stream.of("join")), fl ?
                        Stream.of("modify", "disband", "ban", "unban") : Stream.empty()), fl && priv ?
                        Stream.of("applicants") : Stream.empty()), sender.isOp() ?
                        Stream.of("leave", "join", "remove") : Stream.empty()), f && sender.isOp() ?
                        Stream.of("invite") : Stream.empty()), sender.isOp() || !factions.opOnlyFactionCreation ?
                        Stream.of("create") : Stream.empty()).filter(s -> s.startsWith(args[0])).toList();
            case 2:
                switch (args[0]) {
                    case "join", "leader", "successor", "members", "info", "remove":
                        if ((!args[0].equals("join") || !f || sender.isOp()) && (!args[0].equals("remove") || sender.isOp()))
                            return factions(args[1]);
                    case "invite", "leave":
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
                    case "create": // why does this need to exist
                        return List.of();
                }
            case 3:
                switch (args[0]) {
                    case "join":
                        if (sender.isOp())
                            return Stream.concat(Arrays.stream(factions.getServer().getOfflinePlayers()), factions.getServer().getOnlinePlayers().stream()).filter(p -> p != sender).map(OfflinePlayer::getName).filter(s -> s != null && s.startsWith(args[2])).toList();
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
                    case "create":
                        if ((sender.isOp() || !factions.opOnlyFactionCreation) && "#".startsWith(args[2]))
                            return List.of("#");
                }
        }
        return List.of();
    }

    private void joinFaction(OfflinePlayer player, String id, FactionData fData) {
        List<OfflinePlayer> players = factions.getAllPlayersInFaction(id);
        TextColor fColor = fData.color() != null ? TextColor.color(fData.color()) : null;
        Audience.audience(players.stream().filter(OfflinePlayer::isOnline).map(OfflinePlayer::getPlayer).filter(Objects::nonNull).toArray(Player[]::new)).sendMessage(Component.text(player.getName() != null ? player.getName() : "null").color(fColor != null ? fColor : NamedTextColor.RED).append(Component.text(" has joined the faction").color(NamedTextColor.YELLOW)));
        if (player.isOnline())
            player.getPlayer().sendMessage(Component.text("You have joined the ").color(NamedTextColor.YELLOW)
                    .append(Component.text("[" + fData.name() + "]").color(fColor != null ? fColor : NamedTextColor.YELLOW).clickEvent(ClickEvent.runCommand("/faction info " + id)))
                    .append(Component.text(" faction!").color(NamedTextColor.YELLOW)));
        if (players.isEmpty() && player.isOnline()) {
            player.getPlayer().sendMessage(Component.text("You are now the Faction Leader of the ").color(NamedTextColor.GOLD)
                    .append(Component.text("[" + fData.name() + "]").color(fColor != null ? fColor : NamedTextColor.GOLD).clickEvent(ClickEvent.runCommand("/faction info " + id)))
                    .append(Component.text("!").color(NamedTextColor.GOLD)));
        } else if (players.size() == 1 && player.isOnline()) {
            player.getPlayer().sendMessage(Component.text("You are now the Successor of the ").color(NamedTextColor.LIGHT_PURPLE)
                    .append(Component.text("[" + fData.name() + "]").color(fColor != null ? fColor : NamedTextColor.LIGHT_PURPLE).clickEvent(ClickEvent.runCommand("/faction info " + id)))
                    .append(Component.text("!").color(NamedTextColor.LIGHT_PURPLE)));
            if (players.getFirst().isOnline())
                players.getFirst().getPlayer().sendMessage(Component.text(player.getName()).color(NamedTextColor.RED)
                        .append(Component.text(" is now the Successor of the ").color(NamedTextColor.LIGHT_PURPLE))
                        .append(Component.text("[" + fData.name() + "]").color(fColor != null ? fColor : NamedTextColor.LIGHT_PURPLE).clickEvent(ClickEvent.runCommand("/faction info " + id)))
                        .append(Component.text("!").color(NamedTextColor.LIGHT_PURPLE)));
        }
        if (player.isOnline())
            updatePlayerName(player.getPlayer(), id, fData.name(), fColor);
        if (fData.applicants().remove(player.getUniqueId()))
            factions.setFactionData(id, fData.name(), fData.description(), fData.color(), fData.priv(), fData.applicants(), fData.banned());
        factions.setPlayerData(player.getUniqueId(), id, (byte)(players.isEmpty() ? 1 : players.size() == 1 ? 2 : 0));
    }

    private void succession(OfflinePlayer player, String faction, FactionData factionData, List<OfflinePlayer> players) {
        OfflinePlayer successor = factions.getFactionSuccessor(faction);
        if (successor == null)
            successor = players.stream().filter(p -> p != player).toList().getFirst();
        final OfflinePlayer finalSuccessor = successor;
        factions.getPlayersInFaction(faction).forEach(p -> {
            if (p != player)
                p.sendMessage(Component.text(finalSuccessor.getName() != null ? finalSuccessor.getName() : "null").color(NamedTextColor.RED)
                        .append(Component.text(" is now the Faction Leader of the ").color(NamedTextColor.GOLD))
                        .append(Component.text("[" + factionData.name() + "]").color(factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.GOLD).clickEvent(ClickEvent.runCommand("/faction info " + faction)))
                        .append(Component.text("!").color(NamedTextColor.GOLD)));
        });
    }

    private void updatePlayerName(Player player, String id, String faction, TextColor color) {
        Component p = Component.text("[" + faction + "] ").color(color != null ? color : NamedTextColor.WHITE).clickEvent(ClickEvent.runCommand("/faction info " + id))
                .append(player.name().color(player.isOp() && factions.specialOpNameColor ? NamedTextColor.DARK_RED : color != null ? color : NamedTextColor.WHITE));
        player.displayName(p);
        player.customName(p);
        player.playerListName(p);
    }

    private static Component application(OfflinePlayer applier, boolean msg) {
        return Component.text((applier.getName() != null ? applier.getName() : "null") + (msg ? " would like to join your faction" : "")).color(NamedTextColor.YELLOW)
                .append(Component.text(" - ").color(NamedTextColor.GRAY).clickEvent(ClickEvent.callback((audience) -> audience.sendMessage(Component.text("Why are you clicking this, silly").color(NamedTextColor.GREEN)))))
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
