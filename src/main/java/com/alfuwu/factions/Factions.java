package com.alfuwu.factions;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.List;

public final class Factions extends JavaPlugin implements CommandExecutor, Listener {

    @Override
    public void onEnable() {
        getCommand("faction").setExecutor(this);
        getServer().getPluginManager().registerEvents(this, this);
    }

    @Override
    public void onDisable() {

    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        return false;
    }

    @EventHandler
    public void autoComplete(TabCompleteEvent event) {
        if ("/faction".equals(event.getBuffer()))
            event.setCompletions(List.of("join", "leave", "invite", "leader", "members", "list"));
    }
}
