package com.alfuwu.factions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

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
            String name = factions.getFactionName(faction);
            Integer c = factions.getFactionColor(faction);
            TextColor fColor = c != null ? TextColor.color(c) : NamedTextColor.WHITE;
            Component p = Component.text("[" + name + "] ").color(fColor)
                    .append(player.name().color(player.isOp() ? NamedTextColor.DARK_RED : fColor));
            player.displayName(p);
            player.playerListName(p);
            if (c != null)
                event.joinMessage(p.append(Component.text(" joined the game").color(NamedTextColor.YELLOW)));
        }
    }

    @EventHandler
    public void onPlayerLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        String faction = factions.getPlayerFaction(player.getUniqueId());
        if (faction != null)
            event.quitMessage(player.displayName()
                    .append(Component.text(" left the game").color(NamedTextColor.YELLOW)));
    }
}
