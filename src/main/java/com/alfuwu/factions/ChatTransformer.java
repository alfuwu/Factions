package com.alfuwu.factions;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatTransformer implements Listener {
    public final Factions factions;

    public ChatTransformer(Factions factions) {
        this.factions = factions;
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        var player = event.getPlayer();

        String faction = factions.getPlayerFaction(player.getUniqueId());
        String factionName = faction == null ? null : factions.getFactionName(faction);
        Integer c = factions.getFactionColor(faction);
        TextColor factionColor = c != null ? TextColor.color(c) : NamedTextColor.WHITE;

        // this probably causes a TEENCY amount of chat lag
        // but cool chat ;-;
        event.setCancelled(true);
        for (Player p : event.getRecipients())
            p.sendMessage(Component.text(factionName != null ? "[" + factionName + "] " : "").color(factionColor)
                    .append(player.name().color(player.isOp() ? NamedTextColor.DARK_RED : factionColor))
                    .append(Component.text(" > ").color(NamedTextColor.GRAY))
                    .append(Component.text(ChatColor.translateAlternateColorCodes('&', event.getMessage())).color(NamedTextColor.WHITE)));
    }
}
