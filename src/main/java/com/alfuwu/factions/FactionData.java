package com.alfuwu.factions;

import java.util.List;
import java.util.UUID;

public record FactionData(String id, String name, String description, Integer color, Boolean priv, List<UUID> applicants, List<UUID> banned) {
}
