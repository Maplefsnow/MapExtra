package me.maplef.mapextra.utils;

import me.maplef.mapextra.MapExtra;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class Database {
    static final FileConfiguration config = MapExtra.getInstance().getConfig();

    static final String JDBC_DRIVER = "com.mysql.jdbc.Driver";

    static final String MYSQL_HOST = config.getString("mysql-host");
    static final String PORT = config.getString("mysql-port");
    static final String DB_NAME = config.getString("mysql-database");
    static final String DB_URL = "jdbc:mysql://" + MYSQL_HOST + ":" + PORT + "/" + DB_NAME;

    static final String USERNAME = config.getString("mysql-username");
    static final String PASSWORD = config.getString("mysql-password");

    public static final Connection c = connect();

    public static void init() throws SQLException{
        if(config.getBoolean("use-mysql")){
            PreparedStatement ps = c.prepareStatement("CREATE TABLE IF NOT EXISTS player_info (" +
                    "    UUID           TEXT     NOT NULL," +
                    "    keep_inv       BOOLEAN  DEFAULT 0," +
                    "    haste_end      DATETIME," +
                    "    fly_end        DATETIME," +
                    "    chunk_load_end DATETIME"  +
                    ");");
            ps.execute();
            ps.close();
        } else {
            PreparedStatement ps = c.prepareStatement("CREATE TABLE IF NOT EXISTS player_info (" +
                    "    UUID           TEXT     PRIMARY KEY," +
                    "    keep_inv       BOOLEAN  DEFAULT (0)," +
                    "    haste_end      DATETIME," +
                    "    fly_end        DATETIME," +
                    "    chunk_load_end DATETIME"  +
                    ");");
            ps.execute();
            ps.close();
        }

        for (Player player : MapExtra.getInstance().getServer().getOnlinePlayers()) {
            if(query(player, "player_info") == null){
                String sql = "INSERT INTO player_info VALUES (?, ?, ?, ?, ?);";
                Connection c = Database.c;
                PreparedStatement ps = c.prepareStatement(sql);

                ps.setString(1, player.getUniqueId().toString());
                ps.setBoolean(2, false);
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

                ps.execute();
            }
        }
    }

    private static Connection connect() {
        if(config.getBoolean("use-mysql")){
            Connection conn = null;
            try{
                Class.forName(JDBC_DRIVER);
                conn = DriverManager.getConnection(DB_URL, USERNAME, PASSWORD);
            } catch (Exception e){
                e.printStackTrace();
            }
            return conn;
        } else {
            String url = "jdbc:sqlite:" + new File(MapExtra.getInstance().getDataFolder(), "database.db").getPath();
            Connection conn = null;
            try {
                conn = DriverManager.getConnection(url);
            } catch (SQLException e) {
                Bukkit.getLogger().warning(e.getClass().getName() + ": " + e.getMessage());
            }
            return conn;
        }
    }

    public static @Nullable Map<String, Object> query(UUID uuid, String table) {
        Map<String, Object> queryRes = new HashMap<>();

        try(Statement stmt = c.createStatement()){
            ResultSet res = stmt.executeQuery(String.format("SELECT * FROM %s;", table));
            while(res.next()){
                if(res.getString("UUID").equals(uuid.toString())){
                    ResultSetMetaData data = res.getMetaData();
                    for(int i = 1; i <= data.getColumnCount(); ++i)
                        queryRes.put(data.getColumnName(i), res.getObject(data.getColumnName(i)));
                    return queryRes;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static @Nullable Map<String, Object> query(Player player, String table) {
        Map<String, Object> queryRes = new HashMap<>();

        try(Statement stmt = c.createStatement()){
            ResultSet res = stmt.executeQuery(String.format("SELECT * FROM %s;", table));
            while(res.next()){
                if(res.getString("UUID").equals(player.getUniqueId().toString())){
                    ResultSetMetaData data = res.getMetaData();
                    for(int i = 1; i <= data.getColumnCount(); ++i)
                        queryRes.put(data.getColumnName(i), res.getObject(data.getColumnName(i)));
                    return queryRes;
                }
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
        return null;
    }
}
