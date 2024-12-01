package com.alfuwu.factions;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public final class Factions extends JavaPlugin {
    public boolean transformChat;
    public String chatChar;
    public boolean specialOpNameColor;
    private Connection connection;

    @Override
    public void onEnable() {
        getServer().getPluginManager().registerEvents(new ChatTransformer(this), this);
        getServer().getPluginManager().registerEvents(new PlayerListener(this), this);
        getServer().getCommandMap().register("faction", new FactionCommand(this));
        getServer().getCommandMap().register("factionmsg", new FactionMsgCommand(this));
        try {
            // Initialize database connection
            connectToDatabase();
            setupDatabase();
        } catch (SQLException e) {
            getLogger().severe("Could not set up the database: " + e.getMessage());
        }

        saveDefaultConfig();
        FileConfiguration config = getConfig();
        transformChat = config.getBoolean("transform chat", true);
        chatChar = config.getString("chat char", " » ");
        specialOpNameColor = config.getBoolean("special op name color", true);
        saveConfig();
    }

    @Override
    public void onDisable() {
        closeDatabase();
    }

    private void connectToDatabase() throws SQLException {
        File pluginFolder = getDataFolder();
        if (!pluginFolder.exists()) {
            boolean folderCreated = pluginFolder.mkdirs();
            if (folderCreated)
                getLogger().info("Plugin folder created: " + pluginFolder.getAbsolutePath());
        }

        File databaseFile = new File(pluginFolder, "data.db");
        if (!databaseFile.exists()) {
            try {
                boolean fileCreated = databaseFile.createNewFile();
                if (fileCreated)
                    getLogger().info("Database file created: " + databaseFile.getAbsolutePath());
            } catch (IOException e) {
                getLogger().severe("Could not create database file: " + e.getMessage());
                throw new SQLException("Failed to create database file", e);
            }
        }

        // SQLite connection URL
        String url = "jdbc:sqlite:" + databaseFile.getAbsolutePath();
        connection = DriverManager.getConnection(url);
        getLogger().info("Connected to SQLite database.");
    }

    private void setupDatabase() throws SQLException {
        String createTableSQL = "CREATE TABLE IF NOT EXISTS players (" +
                "uuid TEXT PRIMARY KEY, " +
                "faction_id TEXT NOT NULL, " +
                "flags TINYINT NOT NULL" +
                ");";
        String createFactionsTableSQL = "CREATE TABLE IF NOT EXISTS factions (" +
                "id TEXT PRIMARY KEY, " +
                "name TEXT NOT NULL, " +
                "description TEXT NOT NULL, " +
                "color SMALLINT, " +
                "private TINYINT, " +
                "applicants TEXT NOT NULL, " +
                "banned TEXT NOT NULL" +
                ");";

        try (Statement stmt = connection.createStatement()) {
            stmt.execute(createTableSQL);
            stmt.execute(createFactionsTableSQL);
            getLogger().info("Database tables created successfully.");
        }
    }

    private void closeDatabase() {
        if (connection != null) {
            try {
                connection.close();
                getLogger().info("Database connection closed.");
            } catch (SQLException e) {
                getLogger().severe("Could not close the database connection: " + e.getMessage());
            }
        }
    }

    public void setPlayerData(UUID uuid, String factionId, Byte flags) {
        String upsertSQL = "INSERT INTO players (uuid, faction_id, flags) " +
                "VALUES (?, ?, ?) " +
                "ON CONFLICT(uuid) DO UPDATE SET faction_id = ?, flags = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(upsertSQL)) {
            pstmt.setString(1, uuid.toString());
            pstmt.setString(2, factionId);
            pstmt.setByte(3, flags);
            // CONFLICT
            pstmt.setString(4, factionId);
            pstmt.setByte(5, flags);
            pstmt.executeUpdate();
            getLogger().info("Faction data updated for player: " + uuid);
        } catch (SQLException e) {
            getLogger().severe("Could not update faction data: " + e.getMessage());
        }
    }

    public void removePlayerData(UUID uuid) {
        String deleteSQL = "DELETE FROM players WHERE uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(deleteSQL)) {
            pstmt.setString(1, uuid.toString());
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0)
                getLogger().info("Removed data for player with UUID: " + uuid);
            else
                getLogger().info("No record found for player with UUID: " + uuid);
        } catch (SQLException e) {
            getLogger().severe("Could not remove player from database: " + e.getMessage());
        }
    }

    public void setFactionData(String id, String displayName, String description, Integer color, Boolean priv, List<UUID> applicants, List<UUID> banned) {
        String upsertSQL = "INSERT INTO factions (id, name, description, color, private, applicants, banned) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?) " +
                "ON CONFLICT(id) DO UPDATE SET name = ?, description = ?, color = ?, private = ?, applicants = ?, banned = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(upsertSQL)) {
            pstmt.setString(1, id);
            pstmt.setString(2, displayName);
            pstmt.setString(3, description);
            pstmt.setInt(4, color);
            pstmt.setByte(5, (byte)(priv ? 1 : 0));
            pstmt.setString(6, String.join(",", applicants.stream().map(UUID::toString).toList()));
            pstmt.setString(7, String.join(",", banned.stream().map(UUID::toString).toList()));
            // CONFLICT
            pstmt.setString(8, displayName);
            pstmt.setString(9, description);
            pstmt.setInt(10, color);
            pstmt.setByte(11, (byte)(priv ? 1 : 0));
            pstmt.setString(12, String.join(",", applicants.stream().map(UUID::toString).toList()));
            pstmt.setString(13, String.join(",", banned.stream().map(UUID::toString).toList()));
            pstmt.executeUpdate();
            getLogger().info("Faction data updated for faction: " + id);
        } catch (SQLException e) {
            getLogger().severe("Could not update faction data: " + e.getMessage());
        }
    }

    public boolean removeFactionData(String faction) {
        String deleteSQL = "DELETE FROM factions WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(deleteSQL)) {
            pstmt.setString(1, faction);
            int rowsAffected = pstmt.executeUpdate();

            if (rowsAffected > 0)
                getLogger().info("Successfully removed faction with ID: " + faction);
            else
                getLogger().info("No record found for faction with ID: " + faction);
            return rowsAffected > 0;
        } catch (SQLException e) {
            getLogger().severe("Could not remove player from database: " + e.getMessage());
        }
        return false;
    }

    public List<String> getFactions() {
        List<String> factions = new ArrayList<>();
        String querySQL = "SELECT id FROM factions";

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next())
                    factions.add(rs.getString("id"));
            }
        } catch (SQLException e) {
            getLogger().severe("Could not retrieve factions: " + e.getMessage());
        }

        return factions;
    }

    public List<OfflinePlayer> getAllPlayersInFaction(String faction) {
        List<OfflinePlayer> players = new ArrayList<>();
        String querySQL = "SELECT uuid FROM players WHERE faction_id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setString(1, faction);

            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    String uuidString = rs.getString("uuid");
                    UUID uuid = UUID.fromString(uuidString);

                    Player player = Bukkit.getPlayer(uuid);
                    players.add(Objects.requireNonNullElseGet(player, () -> Bukkit.getOfflinePlayer(uuid)));
                }
            }
        } catch (SQLException e) {
            getLogger().severe("Could not retrieve online players in faction '" + faction + "': " + e.getMessage());
        }

        return players;
    }

    public List<Player> getPlayersInFaction(String faction) {
        return getAllPlayersInFaction(faction).stream().filter(OfflinePlayer::isOnline).map(OfflinePlayer::getPlayer).toList();
    }

    public byte getFactionFlags(UUID uuid) {
        String querySQL = "SELECT flags FROM players WHERE uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setString(1, uuid.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next())
                    return rs.getByte("flags");
            }
        } catch (SQLException e) {
            getLogger().severe("Could not query faction leader status: " + e.getMessage());
        }
        return 0;
    }

    public boolean isFactionLeader(UUID uuid) {
        return getFactionFlags(uuid) == 1;
    }

    public boolean isFactionSuccessor(UUID uuid) {
        return getFactionFlags(uuid) == 2;
    }

    public String getPlayerFaction(UUID uuid) {
        String querySQL = "SELECT faction_id FROM players WHERE uuid = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setString(1, uuid.toString());

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next())
                    return rs.getString("faction_id");
            }
        } catch (SQLException e) {
            getLogger().severe("Could not query player's faction: " + e.getMessage());
        }

        return null;
    }

    public Integer getFactionColor(String id) {
        String querySQL = "SELECT color FROM factions WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setString(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next())
                    return rs.getInt("color");
            }
        } catch (SQLException e) {
            getLogger().severe("Could not query faction data: " + e.getMessage());
        }

        return null;
    }

    public @NotNull String getFactionName(String id) {
        String querySQL = "SELECT name FROM factions WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setString(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next())
                    return rs.getString("name");
            }
        } catch (SQLException e) {
            getLogger().severe("Could not query faction data: " + e.getMessage());
        }

        return "null";
    }

    public OfflinePlayer getFactionLeader(String id) {
        OfflinePlayer leader = null;
        for (OfflinePlayer p : getAllPlayersInFaction(id))
            if (isFactionLeader(p.getUniqueId()))
                leader = p;
        return leader;
    }

    public OfflinePlayer getFactionSuccessor(String id) {
        OfflinePlayer successor = null;
        for (OfflinePlayer p : getAllPlayersInFaction(id))
            if (isFactionSuccessor(p.getUniqueId()))
                successor = p;
        return successor;
    }

    public boolean isFactionPrivate(String id) {
        String querySQL = "SELECT private FROM factions WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setString(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next())
                    return rs.getByte("private") == 1;
            }
        } catch (SQLException e) {
            getLogger().severe("Could not query faction data: " + e.getMessage());
        }

        return false;
    }

    public List<UUID> getListFromSQL(String id, String column) {
        String querySQL = "SELECT " + column + " FROM factions WHERE id = ?";

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setString(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next())
                    return Arrays.stream(rs.getString(column).split(",")).map(UUID::fromString).collect(Collectors.toList());
            }
        } catch (SQLException e) {
            getLogger().severe("Could not query faction data: " + e.getMessage());
        }

        return List.of();
    }

    public List<UUID> getApplicantsForFaction(String id) {
        return getListFromSQL(id, "applicants");
    }

    public List<UUID> getBannedPlayersForFaction(String id) {
        return getListFromSQL(id, "banned");
    }

    public FactionData getFactionData(String id) {
        String querySQL = "SELECT name, description, color, private, applicants, banned FROM factions WHERE id = ?";
        FactionData factionData = null;

        try (PreparedStatement pstmt = connection.prepareStatement(querySQL)) {
            pstmt.setString(1, id);

            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next())
                    factionData = new FactionData(id, rs.getString("name"), rs.getString("description"), rs.getInt("color"), rs.getBoolean("private"), rs.getString("applicants").isEmpty() ? new ArrayList<>() : Arrays.stream(rs.getString("applicants").split(",")).map(UUID::fromString).collect(Collectors.toList()), rs.getString("banned").isEmpty() ? new ArrayList<>() : Arrays.stream(rs.getString("banned").split(",")).map(UUID::fromString).collect(Collectors.toList()));
            }
        } catch (SQLException e) {
            getLogger().severe("Could not retrieve data for faction with ID: " + id + ": " + e.getMessage());
        }

        return factionData;
    }
}
