package com.alfuwu.factions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.UUID;

public class PlayerListener implements Listener {
    public final Factions factions;

    public PlayerListener(Factions factions) {
        this.factions = factions;
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        String faction = factions.getPlayerFaction(player.getUniqueId());
        if (faction != null) {
            FactionData factionData = factions.getFactionData(faction);
            TextColor fColor = factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.WHITE;
            Component p = Component.text("[" + factionData.name() + "] ").color(fColor)
                    .append(player.name().color(player.isOp() && factions.specialOpNameColor ? NamedTextColor.DARK_RED : fColor));
            player.displayName(p);
            player.customName(p);
            player.playerListName(p);
            if (factions.isFactionLeader(player.getUniqueId()) && factionData.priv() && !factionData.applicants().isEmpty())
                player.sendMessage(Component.text("Your faction has applicants waiting to be approved by you").color(NamedTextColor.GOLD));
        } else if (player.isOp() && factions.specialOpNameColor) {
            player.displayName(player.name().color(NamedTextColor.DARK_RED));
            player.customName(player.name().color(NamedTextColor.DARK_RED));
            player.playerListName(player.name().color(NamedTextColor.DARK_RED));
        }
        event.joinMessage(player.displayName().colorIfAbsent(NamedTextColor.YELLOW)
                .append(Component.text(" joined the game").color(NamedTextColor.YELLOW)));
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        event.quitMessage(event.getPlayer().displayName().colorIfAbsent(NamedTextColor.YELLOW)
                .append(Component.text(" left the game").color(NamedTextColor.YELLOW)));
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        event.deathMessage(event.getPlayer().displayName()
                .append(Component.text(event.getDeathMessage() != null ? event.getDeathMessage().replace(event.getPlayer().getName(), "") : "").color(NamedTextColor.WHITE)));
    }
}
