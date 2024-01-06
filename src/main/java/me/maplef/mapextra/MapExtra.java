package me.maplef.mapextra;

import me.maplef.mapextra.commands.MapExtraCmd;
import me.maplef.mapextra.listeners.PlayerDeath;
import me.maplef.mapextra.listeners.PlayerLogin;
import me.maplef.mapextra.utils.Database;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.sql.SQLException;

public final class MapExtra extends JavaPlugin {

    private static MapExtra instance;
    private static Economy econ = null;

    private FileConfiguration messageConfig;

    @Override
    public void onEnable() {
        // Plugin startup logic

        instance = this;

        if (!(setupEconomy())) {
            Bukkit.getServer().getLogger().severe(String.format("[%s] 找不到前置插件 vault，请安装该插件！", getDescription().getName()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }

        this.saveDefaultConfig();
        this.saveResource("messages.yml", false);

        this.registerConfig();

        this.getServer().getPluginManager().registerEvents(new PlayerDeath(), this);
        this.getServer().getPluginManager().registerEvents(new PlayerLogin(), this);

        this.getCommand("mapextra").setExecutor(new MapExtraCmd());
        this.getCommand("mapextra").setTabCompleter(new MapExtraCmd());

        try {
            Database.init();
        } catch (SQLException e){
            e.printStackTrace();
        }
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

        try {
            Database.c.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) return false;

        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) return false;

        econ = rsp.getProvider();
        return true;
    }

    private void registerConfig() {
        messageConfig = YamlConfiguration.loadConfiguration(new File(this.getDataFolder(), "messages.yml"));
    }

    public static Economy getEconomy() {
        return econ;
    }

    public static MapExtra getInstance() {
        return instance;
    }

    public FileConfiguration getMessageConfig() {
        return messageConfig;
    }
}
