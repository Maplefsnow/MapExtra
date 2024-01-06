package me.maplef.mapextra.commands;

import me.maplef.mapextra.MapExtra;
import me.maplef.mapextra.tasks.FlyCheck;
import me.maplef.mapextra.tasks.HasteCheck;
import me.maplef.mapextra.utils.CU;
import me.maplef.mapextra.utils.Database;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.economy.EconomyResponse;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class MapExtraCmd implements CommandExecutor, TabCompleter {
    FileConfiguration config = MapExtra.getInstance().getConfig();
    FileConfiguration messages = MapExtra.getInstance().getMessageConfig();
    private final Economy econ = MapExtra.getEconomy();

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        String msgStart = messages.getString("message-prefix");

        if(args.length == 0 || args[0].equals("help")){
            if(sender instanceof Player){
                Player player = (Player) sender;
                player.sendMessage(getHelpMessage());
            } else {
                Bukkit.getServer().getLogger().info(getHelpMessage());
            }
            return true;
        }

        switch (args[0]) {
            case "keepinv": {
                if(!(sender instanceof Player)){
                    Bukkit.getServer().getLogger().info("该指令只能由玩家执行！");
                    return true;
                }

                Player player = (Player) sender;

                try {
                    Map<String, Object> queryRes = Database.query(player, "player_info");

                    if(queryRes == null) throw new NullPointerException();

                    int keepinvFlag = (Integer) queryRes.get("keep_inv");

                    String sql = "UPDATE player_info SET keep_inv = ? WHERE UUID = ?;";
                    PreparedStatement ps = Database.c.prepareStatement(sql);

                    if(keepinvFlag == 1){
                        ps.setBoolean(1, false);
                        ps.setString(2, String.valueOf(player.getUniqueId()));
                        ps.execute();
                        player.sendMessage(CU.t(msgStart + "积分兑换死亡不掉落功能已 &4&l关闭"));
                    } else {
                        ps.setBoolean(1, true);
                        ps.setString(2, String.valueOf(player.getUniqueId()));
                        ps.execute();
                        player.sendMessage(CU.t(msgStart + "积分兑换死亡不掉落功能已 &a&l开启"));
                    }
                    return true;
                } catch (SQLException e) {
                    e.printStackTrace();
                    player.sendMessage(msgStart + "发生了亿点点错误，请联系管理员");
                    return true;
                }
            }

            case "fly": {
                if(!(sender instanceof Player)){
                    Bukkit.getServer().getLogger().info("该指令只能由玩家执行！");
                    return true;
                }

                Player player = (Player) sender;

                if(args.length != 2){
                    sender.sendMessage(getHelpMessage());
                    return true;
                }

                double playerMoney = econ.getBalance(player); int minutes;

                try{
                    minutes = Integer.parseInt(args[1]);
                } catch (NumberFormatException e){
                    player.sendMessage(CU.t(msgStart + "&c请输入一个整数"));
                    return true;
                }

                double cost = minutes * config.getDouble("fly.cost", 3.0);
                if(playerMoney < cost){
                    player.sendMessage(CU.t(replaceFlyPlaceholders(msgStart + messages.getString("fly.no-enough-money", ""), minutes, "")));
                    return true;
                }

                int checkInterval = config.getInt("check-interval", 20);

                LocalDateTime flyEndTime;
                long tmp = (long) Database.query(player, "player_info").get("fly_end");
                flyEndTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(tmp), ZoneId.systemDefault());

                EconomyResponse r = econ.withdrawPlayer(player, cost);
                if(r.transactionSuccess()){
                    if(flyEndTime.isBefore(LocalDateTime.now())) {
                        try {
                            PreparedStatement ps = Database.c.prepareStatement("UPDATE player_info SET fly_end = ? WHERE UUID = ?;");
                            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().plusMinutes(minutes)));
                            ps.setString(2, player.getUniqueId().toString());
                            ps.execute(); ps.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                        player.setAllowFlight(true); player.setFlySpeed(0.06f);
                        player.sendMessage(CU.t(replaceFlyPlaceholders(msgStart + messages.getString("fly.success", ""), minutes,
                                LocalDateTime.now().plusMinutes(minutes).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))));

                        new FlyCheck(player).runTaskTimer(MapExtra.getInstance(), 0, checkInterval);
                    } else {
                        try {
                            PreparedStatement ps = Database.c.prepareStatement("UPDATE player_info SET fly_end = ? WHERE UUID = ?;");
                            ps.setTimestamp(1, Timestamp.valueOf(flyEndTime.plusMinutes(minutes)));
                            ps.setString(2, player.getUniqueId().toString());
                            ps.execute(); ps.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                        player.sendMessage(CU.t(replaceFlyPlaceholders(msgStart + messages.getString("fly.append", ""), minutes,
                                flyEndTime.plusMinutes(minutes).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))));
                    }
                } else {
                    player.sendMessage(CU.t(msgStart + "&4发生错误：" + r.errorMessage));
                }

                return true;
            }

            case "haste": {
                if(!(sender instanceof Player)){
                    Bukkit.getServer().getLogger().info("该指令只能由玩家执行！");
                    return true;
                }

                Player player = (Player) sender;

                if(args.length != 2){
                    player.sendMessage(getHelpMessage());
                    return true;
                }

                double playerMoney = econ.getBalance(player); int time;

                try{
                    time = Integer.parseInt(args[1]);
                } catch (NumberFormatException e){
                    player.sendMessage(CU.t(msgStart + "&c请输入一个整数"));
                    return true;
                }

                double cost = time * config.getDouble("haste.cost");
                if(playerMoney < cost){
                    player.sendMessage(CU.t(replaceHastePlaceholders(msgStart + messages.getString("haste.no-enough-money"), time, "")));
                    return true;
                }

                int checkInterval = config.getInt("check-interval", 20);

                LocalDateTime hasteEndTime;
                long tmp = (long) Database.query(player, "player_info").get("haste_end");
                hasteEndTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(tmp), ZoneId.systemDefault());

                EconomyResponse r = econ.withdrawPlayer(player, cost);
                if(r.transactionSuccess()){
                    if(hasteEndTime.isBefore(LocalDateTime.now())) {
                        try {
                            PreparedStatement ps = Database.c.prepareStatement("UPDATE player_info SET haste_end = ? WHERE UUID = ?;");
                            ps.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now().plusMinutes(time)));
                            ps.setString(2, player.getUniqueId().toString());
                            ps.execute(); ps.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                        player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, time * 60 * 20, 4));

                        player.sendMessage(CU.t(replaceFlyPlaceholders(msgStart + messages.getString("haste.success", ""), time,
                                LocalDateTime.now().plusMinutes(time).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))));

                        new HasteCheck(player).runTaskTimer(MapExtra.getInstance(), 0, checkInterval);
                    } else {
                        try {
                            PreparedStatement ps = Database.c.prepareStatement("UPDATE player_info SET haste_end = ? WHERE UUID = ?;");
                            ps.setTimestamp(1, Timestamp.valueOf(hasteEndTime.plusMinutes(time)));
                            ps.setString(2, player.getUniqueId().toString());
                            ps.execute(); ps.close();
                        } catch (SQLException e) {
                            e.printStackTrace();
                        }

                        if(player.hasPotionEffect(PotionEffectType.FAST_DIGGING) && player.getPotionEffect(PotionEffectType.FAST_DIGGING).getAmplifier() == 4){
                            int durationRemain = player.getPotionEffect(PotionEffectType.FAST_DIGGING).getDuration();
                            player.removePotionEffect(PotionEffectType.FAST_DIGGING);
                            player.addPotionEffect(new PotionEffect(PotionEffectType.FAST_DIGGING, time * 60 * 20 + durationRemain, 4));
                        }

                        player.sendMessage(CU.t(replaceHastePlaceholders(msgStart + messages.getString("haste.append", ""), time,
                                hasteEndTime.plusMinutes(time).format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))));
                    }
                } else {
                    player.sendMessage(CU.t(msgStart + "&4发生错误：" + r.errorMessage));
                }
            }

            case "tp": {
                if(!(sender instanceof Player)){
                    Bukkit.getServer().getLogger().info("该指令只能由玩家执行！");
                    return true;
                }

                Player player = (Player) sender;

                if(args.length != 2){
                    player.sendMessage(getHelpMessage());
                    return true;
                }

                double playerMoney = econ.getBalance(player);
                Player target = Bukkit.getPlayer(args[1]);

                if(target == null) {
                    player.sendMessage();
                }

            }
        }

        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        if(args.length == 1){
            String[] allCommands = {"help", "haste", "fly", "keepinv"};

            List<String> commandList = new ArrayList<>();
            for(String commandName : allCommands)
                if(sender.hasPermission("mapextra." + commandName))
                    commandList.add(commandName);

            return commandList;
        }
        return null;
    }

    private String getHelpMessage(){
        String msg =
                "&a[MapExtra 帮助菜单]\n" +
                        "&e/mapex help &f- 显示此菜单\n" +
                        "&e/mapex keepinv &f- 切换是否自动使用货币免疫死亡掉落\n" +
                        "&e/mapex haste &f- 购买急迫V\n" +
                        "&e/mapex fly &f- 购买飞行（青春版）\n" +
                        "&e/mapex tp &f- 购买传送";
        return CU.t(msg);
    }

    private String replaceFlyPlaceholders(String str, int minutes, String endTimeStr){
        return str.replace("{cost}", String.valueOf(config.getInt("fly.cost") * minutes))
                .replace("{time}", String.valueOf(minutes))
                .replace("{end_time}", endTimeStr);
    }

    private String replaceHastePlaceholders(String str, int minutes, String endTimeStr) {
        return str.replace("{cost}", String.valueOf(config.getInt("haste.cost") * minutes))
                .replace("{time}", String.valueOf(minutes))
                .replace("{end_time}", endTimeStr);
    }
}
