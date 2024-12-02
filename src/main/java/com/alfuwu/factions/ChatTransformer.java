package com.alfuwu.factions;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
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
        if (!factions.transformChat)
            return;
        var player = event.getPlayer();

        String faction = factions.getPlayerFaction(player.getUniqueId());
        String factionName = faction == null ? null : factions.getFactionName(faction);
        Integer c = factions.getFactionColor(faction);
        TextColor factionColor = c != null ? TextColor.color(c) : NamedTextColor.GRAY;

        // this probably causes a TEENCY amount of chat lag
        // but cool chat ;-;
        event.setCancelled(true);
        Audience.audience(event.getRecipients()).sendMessage(Component.text(factionName != null ? "[" + factionName + "] " : "").color(factionColor).clickEvent(ClickEvent.runCommand("/faction info " + faction))
                .append(player.name().color(player.isOp() && factions.specialOpNameColor ? NamedTextColor.DARK_RED : factionColor))
                .append(Component.text(factions.chatChar).color(TextColor.color(0x888888)).clickEvent(null)) // 0x888888 is in between dark gray and gray colors
                .append(Component.text(ChatColor.translateAlternateColorCodes('&', event.getMessage())).color(NamedTextColor.WHITE).clickEvent(null)));
    }
}
