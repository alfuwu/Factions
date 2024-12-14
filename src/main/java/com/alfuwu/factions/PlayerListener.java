package com.alfuwu.factions;

import net.kyori.adventure.text.*;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerAdvancementDoneEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.List;
import java.util.stream.Stream;

public class PlayerListener implements Listener {
    public final Factions factions;

    public PlayerListener(Factions factions) {
        this.factions = factions;
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String faction = factions.getPlayerFaction(player.getUniqueId());
        if (faction != null) {
            FactionData factionData = factions.getFactionData(faction);
            TextColor fColor = factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.WHITE;
            Component p = Component.text("[" + factionData.name() + "] ").color(fColor).clickEvent(ClickEvent.runCommand("/faction info " + faction))
                    .append(player.name().color(player.isOp() && factions.specialOpNameColor ? NamedTextColor.DARK_RED : fColor));
            player.displayName(p);
            player.customName(p);
            player.playerListName(p);
            boolean l = factions.isFactionLeader(player.getUniqueId());
            if (l && !factions.getFactionLeader(faction).getUniqueId().equals(player.getUniqueId())) {
                l = false;
                factions.setPlayerData(player.getUniqueId(), faction, factions.getFactionSuccessor(faction) == null ? (byte)2 : (byte)0);
            }
            if (l && factionData.priv() && !factionData.applicants().isEmpty())
                player.sendMessage(Component.text("Your faction has applicants waiting to be approved by you").color(NamedTextColor.GOLD));
        } else if (player.isOp() && factions.specialOpNameColor) {
            player.displayName(player.name().color(NamedTextColor.DARK_RED));
            player.customName(player.name().color(NamedTextColor.DARK_RED));
            player.playerListName(player.name().color(NamedTextColor.DARK_RED));
        }
        if (event.joinMessage() instanceof TranslatableComponent translatable)
            event.joinMessage(translatable.arguments(player.displayName()));
        else
            event.joinMessage(player.displayName().colorIfAbsent(NamedTextColor.YELLOW)
                    .append(Component.text(" joined the game").color(NamedTextColor.YELLOW)));
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerLeave(PlayerQuitEvent event) {
        if (event.quitMessage() instanceof TranslatableComponent translatable)
            event.quitMessage(translatable.arguments(event.getPlayer().displayName()));
        else
            event.quitMessage(event.getPlayer().displayName().colorIfAbsent(NamedTextColor.YELLOW)
                    .append(Component.text(" left the game").color(NamedTextColor.YELLOW)));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (event.deathMessage() instanceof TranslatableComponent translatable) {
            List<? extends ComponentLike> args = translatable.arguments();
            args = Stream.concat(Stream.concat(Stream.of(event.getPlayer().displayName()), event.getDamageSource().getCausingEntity() instanceof Player attacker ? Stream.of(attacker.displayName()) : Stream.empty()), args.subList(event.getDamageSource().getCausingEntity() instanceof Player ? 2 : 1, args.size()).stream()).toList();
            event.deathMessage(translatable.arguments(args));
        }
    }

    @EventHandler
    public void onPlayerAdvancement(PlayerAdvancementDoneEvent event) {
        if (event.message() instanceof TranslatableComponent translatable) {
            List<? extends ComponentLike> args = translatable.arguments();
            args = Stream.concat(Stream.of(event.getPlayer().displayName()), args.subList(1, args.size()).stream()).toList();
            event.message(translatable.arguments(args));
        }
    }
}
