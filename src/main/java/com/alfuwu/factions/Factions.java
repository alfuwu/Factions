package com.alfuwu.factions;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.server.TabCompleteEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public final class Factions extends JavaPlugin {

    @Override
    public void onEnable() {
        getServer().getCommandMap().register("faction", new FactionCommand(this));
    }

    @Override
    public void onDisable() {

    }
}
