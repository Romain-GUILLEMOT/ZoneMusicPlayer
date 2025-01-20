package fr.yotapaki.ZoneMusicPlayer;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    private MusicManager musicManager;

    @Override
    public void onEnable() {
        getLogger().info("ZoneMusicPlayer is starting...");
        musicManager = new MusicManager(this);
        musicManager.loadConfig();

        // Enregistrer les événements
        Bukkit.getPluginManager().registerEvents(musicManager, this);

        // Enregistrer les commandes
        getCommand("zonemusic").setExecutor(new CommandManager(this, musicManager));

        getLogger().info("ZoneMusicPlayer has been successfully enabled.");
    }

    @Override
    public void onDisable() {
        getLogger().info("ZoneMusicPlayer is shutting down...");
        musicManager.stopAllMusic();
        getLogger().info("ZoneMusicPlayer has been successfully disabled.");
    }

    public MusicManager getMusicManager() {
        return musicManager;
    }
}