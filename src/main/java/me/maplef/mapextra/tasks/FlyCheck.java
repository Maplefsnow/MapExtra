package me.maplef.mapextra.tasks;

import me.maplef.mapextra.MapExtra;
import me.maplef.mapextra.utils.CU;
import me.maplef.mapextra.utils.Database;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitRunnable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

public class FlyCheck extends BukkitRunnable {
    FileConfiguration config = MapExtra.getInstance().getConfig();
    FileConfiguration messages = MapExtra.getInstance().getMessageConfig();

    private final String msgPrefix = messages.getString("message-prefix");

    Player player;

    public FlyCheck(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        if(!player.isOnline()) {
            this.cancel();
            return;
        }

        long tmp = (long) Database.query(player, "player_info").get("fly_end");
        LocalDateTime flyEndTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(tmp), ZoneId.systemDefault());

        if(LocalDateTime.now().isAfter(flyEndTime)) {
            player.setAllowFlight(false); player.setFlying(false);
            player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW_FALLING, 20 * config.getInt("fly.slow-falling-time"), 0));

            player.sendMessage(CU.t(msgPrefix + messages.getString("fly.fly-end", "")));

            this.cancel();
        } else {
            if(!player.getAllowFlight()) player.setAllowFlight(true);
        }
    }
}
