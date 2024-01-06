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

public class HasteCheck extends BukkitRunnable {

    FileConfiguration config = MapExtra.getInstance().getConfig();
    FileConfiguration messages = MapExtra.getInstance().getMessageConfig();

    private final String msgPrefix = messages.getString("message-prefix");

    Player player;

    public HasteCheck(Player player) {
        this.player = player;
    }

    @Override
    public void run() {
        long tmp = (long) Database.query(player, "player_info").get("haste_end");
        LocalDateTime hasteEndTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(tmp), ZoneId.systemDefault());

        if(LocalDateTime.now().isAfter(hasteEndTime)) {
            if(player.hasPotionEffect(PotionEffectType.FAST_DIGGING)) player.removePotionEffect(PotionEffectType.FAST_DIGGING);

            player.sendMessage(CU.t(msgPrefix + messages.getString("fly.haste-end", "")));

            this.cancel();
        } else {
            if(!player.hasPotionEffect(PotionEffectType.FAST_DIGGING));
//                player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, -1, 4));
        }
    }
}
