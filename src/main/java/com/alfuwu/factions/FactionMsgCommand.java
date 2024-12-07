package com.alfuwu.factions;

import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.UUID;

public class FactionMsgCommand extends Command {
    public final Factions factions;

    protected FactionMsgCommand(Factions factions) {
        super("factionmsg", "Message all online members of your faction", "/factionmsg <message>", List.of("f", "fmsg"));
        this.factions = factions;
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull String label, @NotNull String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("Only players can use this command").color(NamedTextColor.RED));
            return false;
        }

        if (args.length == 0) {
            player.sendMessage(Component.text("Usage: ").color(NamedTextColor.GOLD)
                    .append(Component.text("/factionmsg <message>").color(NamedTextColor.RED))
                    .appendNewline().append(Component.text("Sends a message to all online members of your faction").color(NamedTextColor.YELLOW)));
            return true;
        }

        UUID playerUUID = player.getUniqueId();
        String factionId = factions.getPlayerFaction(playerUUID);

        if (factionId == null) {
            player.sendMessage(Component.text("You're not in a faction!").color(NamedTextColor.RED));
            return false;
        }

        FactionData factionData = factions.getFactionData(factionId);
        if (factionData == null) {
            player.sendMessage(Component.text("Could not retrieve faction data").color(NamedTextColor.RED));
            return false;
        }

        String message = String.join(" ", args);
        TextColor c = factionData.color() != null ? TextColor.color(factionData.color()) : NamedTextColor.RED;
        Component formattedMessage = Component.text("[").color(NamedTextColor.GOLD)
                .append(player.name().color(c))
                .append(Component.text(" -> ").color(NamedTextColor.GOLD))
                .append(Component.text(factionData.name()).color(c))
                .append(Component.text("] ").color(NamedTextColor.GOLD))
                .append(Component.text(ChatColor.translateAlternateColorCodes('&', message)).color(NamedTextColor.WHITE));

        Audience.audience(factions.getPlayersInFaction(factionId)).sendMessage(formattedMessage);
        return true;
    }

    @Override
    public @NotNull List<String> tabComplete(@NotNull CommandSender sender, @NotNull String alias, @NotNull String[] args) throws IllegalArgumentException {
        return List.of();
    }
}
