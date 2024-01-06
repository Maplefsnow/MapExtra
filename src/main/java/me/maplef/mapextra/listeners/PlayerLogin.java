package me.maplef.mapextra.listeners;

import me.maplef.mapextra.MapExtra;
import me.maplef.mapextra.tasks.FlyCheck;
import me.maplef.mapextra.utils.Database;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerLoginEvent;

import java.sql.*;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.UUID;

public class PlayerLogin implements Listener {
    FileConfiguration config = MapExtra.getInstance().getConfig();
    FileConfiguration messages = MapExtra.getInstance().getMessageConfig();

    @EventHandler
    public void buildDatabase(PlayerLoginEvent e) {
        UUID uuid = e.getPlayer().getUniqueId();

        try {
            if(Database.query(uuid, "player_info") == null){
                String sql = "INSERT INTO player_info VALUES (?, ?, ?, ?, ?);";
                Connection c = Database.c;
                PreparedStatement ps = c.prepareStatement(sql);

                ps.setString(1, uuid.toString());
                ps.setBoolean(2, false);
                ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
                ps.setTimestamp(4, Timestamp.valueOf(LocalDateTime.now()));
                ps.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));

                ps.execute();
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }

    @EventHandler
    public void startTask(PlayerLoginEvent e) {
        Player player = e.getPlayer();

        if(Database.query(player, "player_info") == null) return;

        LocalDateTime flyEndTime;
        long tmp = (long) Database.query(player, "player_info").get("fly_end");
        flyEndTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(tmp), ZoneId.systemDefault());

        if(LocalDateTime.now().isBefore(flyEndTime)){
            int checkInterval = config.getInt("check-interval", 20);
            new FlyCheck(player).runTaskTimer(MapExtra.getInstance(), 0, checkInterval);
        }
    }
}
