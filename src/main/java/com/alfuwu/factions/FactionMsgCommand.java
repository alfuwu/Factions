package com.alfuwu.factions;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class FactionMsgCommand extends Command {
    public final Factions factions;

    protected FactionMsgCommand(Factions factions) {
        super("factionmsg", "Message all online members of your faction", "/factionmsg <message>", List.of("f", "fmsg"));
        this.factions = factions;
    }

    @Override
    public boolean execute(@NotNull CommandSender commandSender, @NotNull String s, @NotNull String[] strings) {
        return false;
    }
}
