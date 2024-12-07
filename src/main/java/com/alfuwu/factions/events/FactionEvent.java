package com.alfuwu.factions.events;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public abstract class FactionEvent extends Event {
    @Override
    public @NotNull HandlerList getHandlers() {
        return null;
    }
}
