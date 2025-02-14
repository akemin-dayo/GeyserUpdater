package com.alysaa.geyserupdater.velocity;

import com.alysaa.geyserupdater.common.util.CheckBuildFile;
import com.alysaa.geyserupdater.common.util.OSUtils;
import com.alysaa.geyserupdater.common.util.ScriptCreator;
import com.alysaa.geyserupdater.velocity.command.GeyserUpdaterCommand;
import com.alysaa.geyserupdater.velocity.util.GeyserVelocityCheckBuildNum;
import com.alysaa.geyserupdater.velocity.util.VelocityJoinListener;
import com.alysaa.geyserupdater.velocity.util.bstats.Metrics;

import com.google.inject.Inject;
import com.moandjiezana.toml.Toml;
import org.slf4j.Logger;

import com.velocitypowered.api.event.PostOrder;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.event.proxy.ProxyShutdownEvent;
import com.velocitypowered.api.plugin.Dependency;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Timer;
import java.util.TimerTask;

@Plugin(id = "geyserupdater", name = "GeyserUpdater", version = "1.3.0-SNAPSHOT", description = "Automatically downloads new builds of Geyser and applies them on server restart.", authors = {"Jens"},
        dependencies = {@Dependency(id = "geyser")})
public class VelocityUpdater {
    public static ProxyServer server;
    public static Logger logger;
    public Path dataDirectory;
    public static Toml configf;
    private final Metrics.Factory metricsFactory;
    @Inject
    public VelocityUpdater(ProxyServer server, Logger logger, @DataDirectory final Path folder, Metrics.Factory metricsFactory) {
        this.server = server;
        com.alysaa.geyserupdater.velocity.VelocityUpdater.logger = logger;
        configf = loadConfig(folder);
        this.metricsFactory = metricsFactory;
    }
    @Subscribe
    public void onProxyInitialization(ProxyInitializeEvent event) {
        logger.info("GeyserUpdater has been enabled!");
        // Create folder for storing new Geyser jar
        createUpdateFolder();
        // Make startup script
        makeScriptFile();
        // Register our only command
        server.getCommandManager().register("geyserupdate", new GeyserUpdaterCommand());
        // Player alert if a restart is required when they join
        server.getEventManager().register(this, new VelocityJoinListener());
        // Auto update Geyser if enabled in the config
        this.startAutoUpdate();
        // Check if downloaded Geyser file exists periodically
        TimerTask task = new TimerTask() {
            public void run() {
                CheckBuildFile.checkVelocityFile(true);
            }
        };
        Timer timer = new Timer("Timer");
        timer.schedule(task, 60*30*1000,60*60*121000);
        Metrics metrics = metricsFactory.make(this, 10673);
    }
    public void onDisable() {
        try {
            this.moveGeyser();
        } catch (IOException e) {
            logger.error("An I/O error occurred while attempting to update Geyser!");
        }
        try {
            this.deleteBuild();
        } catch (Exception ignored) {
        }
    }
    public void createUpdateFolder() {
        // Creating BuildUpdate folder
        File updateDir = new File("plugins/GeyserUpdater/BuildUpdate");
        if (!updateDir.exists()) {
            try {
                updateDir.mkdirs();
            } catch (Exception ignored) { }
        }
    }
    private void makeScriptFile() {
        if (configf.getBoolean("Auto-Script-Generating")) {
            if (OSUtils.isWindows() || OSUtils.isLinux() || OSUtils.isMac()) {
                try {
                    ScriptCreator.createScript(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else {
                logger.warn("Your operating system is not supported! GeyserUpdater only supports automatic script creation for Linux, macOS, and Windows.");
            }
        }
    }
    public void startAutoUpdate() {
        if (configf.getBoolean("Auto-Update-Geyser")) {
            // Checking for the build numbers of current build.
            TimerTask task = new TimerTask() {
                public void run() {
                    GeyserVelocityCheckBuildNum.checkBuildNumberVelocity();
                }
            };
            Timer timer = new Timer("Timer");
            timer.schedule(task, 0,60*60*24*1000);
        }
    }
    public void moveGeyser() throws IOException {
        // Moving Geyser Jar to Plugins folder "Overwriting".
        File fileToCopy = new File("plugins/GeyserUpdater/BuildUpdate/Geyser-Velocity.jar");
        FileInputStream input = new FileInputStream(fileToCopy);
        File newFile = new File("plugins/Geyser-Velocity.jar");
        FileOutputStream output = new FileOutputStream(newFile);
        byte[] buf = new byte[1024];
        int bytesRead;
        while ((bytesRead = input.read(buf)) > 0) {
            output.write(buf, 0, bytesRead);
        }
        input.close();
        output.close();
    }
    private void deleteBuild() throws IOException {
        Path file = Paths.get("plugins/GeyserUpdater/BuildUpdate/Geyser-Velocity.jar");
        Files.delete(file);
    }
    private Toml loadConfig(Path path) {
        File folder = path.toFile();
        File file = new File(folder, "config.toml");
        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        if (!file.exists()) {
            try (InputStream input = getClass().getResourceAsStream("/" + file.getName())) {
                if (input != null) {
                    Files.copy(input, file.toPath());
                } else {
                    file.createNewFile();
                }
            } catch (IOException exception) {
                exception.printStackTrace();
                return null;
            }
        }

        return new Toml().read(file);
    }
    @Subscribe(order = PostOrder.LAST)
    public void onShutdown(ProxyShutdownEvent event) {
        onDisable();
    }

    public Logger getLogger() {
        return logger;
    }

    public ProxyServer getProxyServer() {
        return server;
    }


    public Path getDataDirectory() {
        return dataDirectory;
    }
}






