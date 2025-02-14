package com.alysaa.geyserupdater.spigot;

import com.alysaa.geyserupdater.common.util.CheckBuildFile;
import com.alysaa.geyserupdater.common.util.OSUtils;
import com.alysaa.geyserupdater.spigot.command.GeyserCommand;
import com.alysaa.geyserupdater.spigot.util.CheckSpigotRestart;
import com.alysaa.geyserupdater.spigot.util.GeyserSpigotCheckBuildNum;
import com.alysaa.geyserupdater.spigot.util.SpigotJoinListener;
import com.alysaa.geyserupdater.spigot.util.SpigotResourceUpdateChecker;
import com.alysaa.geyserupdater.spigot.util.bstats.Metrics;

import org.bukkit.Bukkit;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.logging.Logger;

public class SpigotUpdater extends JavaPlugin {
    public static SpigotUpdater plugin;
    private FileConfiguration config;

    public static Plugin getPlugin() {
        return plugin;
    }

    @Override
    public void onEnable() {
        new Metrics(this, 10202);
        this.getCommand("geyserupdate").setExecutor(new GeyserCommand());
        createFiles();
        checkConfigVer();
        plugin = this;
        // If true start auto updating
        if (getConfig().getBoolean("Auto-Update-Geyser")) {
            try {
                Timer StartAutoUpdate;
                StartAutoUpdate = new Timer();
                StartAutoUpdate.schedule(new StartUpdate(), 0, 1000 * 60 * 1440);
                // Auto Update Cycle on Startup and each 24h after startup
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        // Enable File Checking here
        Timer StartFileCheck;
        StartFileCheck = new Timer();
        // File Checking every 12h after 30min after server start
        StartFileCheck.schedule(new StartTimer(), 1000 * 60 * 30, 1000 * 60 * 720);
        // Logger for check update on GeyserUpdater
        versionCheck();
        // Player alert if a restart is required when they join
        Bukkit.getServer().getPluginManager().registerEvents(new SpigotJoinListener(), this);
        // Check if a restart script already exists
        // We create one if it doesn't
        if (getConfig().getBoolean("Auto-Script-Generating")) {
            if (OSUtils.isWindows() || OSUtils.isLinux() || OSUtils.isMac()) {
                try {
                    CheckSpigotRestart.checkYml();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                System.out.println("[GeyserUpdater] Your operating system is not supported! GeyserUpdater only supports automatic script creation for Linux, macOS, and Windows.");
            }
        }
    }
    public void checkConfigVer() {
        Logger logger = this.getLogger();
        //Change version number only when editing config.yml!
        if (!(getConfig().getInt("version") == 1)) {
                logger.info("Your copy of config.yml is outdated. Please delete it and let a fresh copy of config.yml be regenerated!");
            }
        }
    public void versionCheck() {
        SpigotUpdater plugin = this;
        Logger logger = this.getLogger();
        String pluginVersion = this.getDescription().getVersion();
        String version = SpigotResourceUpdateChecker.getVersion(plugin);
        if (version.equals(pluginVersion)) {
            logger.info("You are using the latest version of GeyserUpdater!");
        } else {
            logger.info("There is a new update available for GeyserUpdater! Download it now at https://www.spigotmc.org/resources/geyserupdater.88555/.");
        }
    }
    private void createFiles() {
        File configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            configFile.getParentFile().mkdirs();
            saveResource("config.yml", false);
        }
        config = new YamlConfiguration();
        try {
            config.load(configFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
        File updateDir = new File("plugins/update");
        if (!updateDir.exists()) {
            try {
                updateDir.mkdirs();
            } catch (Exception ignored) {}
        }
    }
    private class StartTimer extends TimerTask {
        @Override
        public void run() {
            CheckBuildFile.checkSpigotFile(false);
        }
    }
    private class StartUpdate extends TimerTask {
        @Override
        public void run() {
            GeyserSpigotCheckBuildNum.checkBuildNumberSpigot();
        }
    }
}
