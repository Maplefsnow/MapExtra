package me.maplef.mapextra.listeners;

import me.maplef.mapextra.MapExtra;
import me.maplef.mapextra.utils.CU;
import me.maplef.mapextra.utils.Database;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;

import java.util.UUID;

public class PlayerDeath implements Listener {
    FileConfiguration config = MapExtra.getInstance().getConfig();
    FileConfiguration messages = MapExtra.getInstance().getMessageConfig();

    private final String msgPrefix = messages.getString("message-prefix");

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent e) {
        Player player = e.getEntity();
        UUID playerUUID = e.getEntity().getUniqueId();

        int keep = 0;
        if(Database.query(playerUUID, "player_info") != null){
            Object tmp = Database.query(playerUUID, "player_info").get("keep_inv");
            if(tmp != null)
                keep = (Integer) tmp;
        } else {
            e.getEntity().sendMessage("ERROR!");
        }

        if(keep == 1){
            if(config.getBoolean("keep-inventory.enable")){
                Economy econ = MapExtra.getEconomy();

                double money = econ.getBalance(player);
                int cost = config.getInt("keep-inventory.cost");

                if(money < cost){
                    player.sendMessage(CU.t(msgPrefix + replacePlaceholders(messages.getString("keep-inv.no-enough-money", ""))));
                    return;
                }

                EconomyResponse r = econ.withdrawPlayer(player, cost);
                if(r.transactionSuccess()){
                    player.sendMessage(CU.t(msgPrefix + replacePlaceholders(messages.getString("keep-inv.success", ""))));
                    e.setKeepInventory(true); e.setKeepLevel(true);
                    e.getDrops().clear(); e.setDroppedExp(0);
                } else {
                    player.sendMessage(CU.t(msgPrefix + "&4发生错误：" + r.errorMessage));
                }
            } else {
                player.sendMessage(CU.t(msgPrefix + replacePlaceholders(messages.getString("keep-inv.function-forbidden", ""))));
            }
        }

        Location deathLocation = player.getLocation();
        if(config.getBoolean("keep-inventory.death-log", false)){
            String msg = String.format("玩家 %s 在世界 %s 的 (%d, %d, %d) 位置死亡，背包物品已%s",
                    player.getName(), deathLocation.getWorld(),
                    deathLocation.getBlockX(), deathLocation.getBlockY(), deathLocation.getBlockZ(),
                    e.getKeepInventory() ? "保留" : "掉落");
            Bukkit.getServer().getLogger().info(msg);
        }
        if(config.getBoolean("keep-inventory.send-death-info", false)){
            String msg = String.format("&e你在世界 %s 的 (%d, %d, %d) 位置死亡，背包物品已%s",
                    deathLocation.getWorld(),
                    deathLocation.getBlockX(), deathLocation.getBlockY(), deathLocation.getBlockZ(),
                    e.getKeepInventory() ? "保留" : "掉落");
            player.sendMessage(CU.t(msgPrefix + msg));
        }
    }

    private String replacePlaceholders(String str){
        return str.replace("{cost}", String.valueOf(config.getInt("keep-inventory.cost", 0)));
    }
}
