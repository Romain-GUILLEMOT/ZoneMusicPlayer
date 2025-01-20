package fr.yotapaki.ZoneMusicPlayer;

import com.google.gson.*;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.bukkit.BukkitRegionContainer;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;

import java.io.*;
import java.util.*;

public class MusicManager implements Listener {

    private final Main plugin;
    private final Map<Player, Integer> activePlayers = new HashMap<>();
    private final Map<Player, String> currentMusic = new HashMap<>();
    private final List<Music> musics = new ArrayList<>();
    private final Set<Player> debugPlayers = new HashSet<>();
    private int minDelay = 5;
    private int maxDelay = 20;

    public MusicManager(Main plugin) {
        this.plugin = plugin;
    }

    public void loadConfig() {
        File configFile = new File(plugin.getDataFolder(), "config.json");
        if (!configFile.exists()) {
            saveDefaultConfig(configFile);
        }

        try {
            JsonObject config = JsonParser.parseReader(new FileReader(configFile)).getAsJsonObject();
            minDelay = config.get("min_delay").getAsInt();
            maxDelay = config.get("max_delay").getAsInt();
            musics.clear();
            config.getAsJsonArray("musics").forEach(jsonElement -> {
                JsonObject musicJson = jsonElement.getAsJsonObject();
                String name = musicJson.get("name").getAsString();
                int duration = musicJson.get("duration").getAsInt();
                String regionId = musicJson.has("region_id") ? musicJson.get("region_id").getAsString() : null;
                musics.add(new Music(name, duration, regionId));
            });
            plugin.getLogger().info("Configuration loaded successfully.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load configuration: " + e.getMessage());
        }
    }

    private void saveDefaultConfig(File configFile) {
        try {
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            JsonObject defaultConfig = new JsonObject();
            defaultConfig.addProperty("min_delay", 5);
            defaultConfig.addProperty("max_delay", 20);
            JsonArray defaultMusics = new JsonArray();
            defaultMusics.add(new Music("custom:pnj.intro.music", 120, "spawn").toJson());
            defaultMusics.add(new Music("custom:background.music", 150, null).toJson());
            defaultConfig.add("musics", defaultMusics);

            try (Writer writer = new FileWriter(configFile)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(defaultConfig, writer);
            }
        } catch (IOException e) {
            plugin.getLogger().severe("Error creating default configuration file: " + e.getMessage());
        }
    }

    public void stopAllMusic() {
        activePlayers.forEach((player, taskId) -> Bukkit.getScheduler().cancelTask(taskId));
        activePlayers.clear();
        currentMusic.clear();
    }

    public void toggleDebug(Player player) {
        if (debugPlayers.contains(player)) {
            debugPlayers.remove(player);
            player.sendMessage("Debug mode disabled.");
        } else {
            debugPlayers.add(player);
            player.sendMessage("Debug mode enabled.");
        }
    }

    public void reloadConfig() {
        stopAllMusic();
        loadConfig();
    }

    @EventHandler
    public void onPlayerJoin(PlayerJoinEvent event) {
        handlePlayerMusic(event.getPlayer());
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        Location from = event.getFrom();
        Location to = event.getTo();
        if (to != null && !from.getBlock().equals(to.getBlock())) {
            handlePlayerMusic(player);
        }
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        stopMusicForPlayer(event.getPlayer());
    }

    private void handlePlayerMusic(Player player) {
        // Identifier la région actuelle du joueur
        String regionId = getCurrentRegion(player);
        player.sendMessage("===============");

        // Vérifier si le joueur est dans la même région qu'avant
        player.sendMessage("DEBUG 1:" + currentMusic);
        if (currentMusic.containsKey(player)) {
            String currentRegion = currentMusic.get(player);
            player.sendMessage("DEBUG 2:" + currentMusic.get(player));
            player.sendMessage("DEBUG 3:" + regionId);
            player.sendMessage("DEBUG 4:" + Objects.equals(currentRegion, regionId));
            if (Objects.equals(currentRegion, regionId)) {
                return; // Ne rien faire si le joueur est toujours dans la même région
            }
        }
        player.sendMessage("DEBUG 5: WTF");

        // Arrêter la musique actuelle si le joueur change de région
        stopMusicForPlayer(player);

        // Trouver une musique associée à la région actuelle
        Optional<Music> optionalMusic = musics.stream()
                .filter(music -> Objects.equals(music.getRegionId(), regionId))
                .findAny();

        if (optionalMusic.isPresent()) {
            // Jouer la musique de la nouvelle région
            Music music = optionalMusic.get();
            playMusic(player, music);
            if (debugPlayers.contains(player)) {
                player.sendMessage("Playing region music: " + music.getName() + " in region: " + regionId);
            }
        } else {
            // Si aucune musique régionale n'est définie, jouer une musique globale (si applicable)
            musics.stream()
                    .filter(music -> music.getRegionId() == null)
                    .findAny()
                    .ifPresent(music -> {
                        playMusic(player, music);
                        if (debugPlayers.contains(player)) {
                            player.sendMessage("Playing global music: " + music.getName());
                        }
                    });
        }
    }

    private void playMusic(Player player, Music music) {
        plugin.getLogger().info("Scheduling music: " + music.getName() + " for player: " + player.getName());

        // Enregistre la musique actuelle avant de la jouer
        currentMusic.put(player, music.getRegionId());

        // Planifie la musique après un délai aléatoire
        int taskId = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            plugin.getLogger().info("Playing sound: " + music.getName() + " for player: " + player.getName());
            player.playSound(player.getLocation(), music.getName(), SoundCategory.MUSIC, 1.0f, 1.0f);

            // Relance la musique après sa durée
            Bukkit.getScheduler().runTaskLater(plugin, () -> playMusic(player, music), music.getDuration() * 20L);
        }, getRandomDelay() * 20L).getTaskId();

        activePlayers.put(player, taskId);
    }

    private void stopMusicForPlayer(Player player) {
        Integer taskId = activePlayers.remove(player);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }

        musics.forEach(music -> player.stopSound(music.getName(), SoundCategory.MUSIC));
        if (currentMusic.containsKey(player)) {
            currentMusic.remove(player);
        }
    }

    private String getCurrentRegion(Player player) {
        BukkitRegionContainer container = (BukkitRegionContainer) WorldGuard.getInstance().getPlatform().getRegionContainer();
        RegionManager regionManager = container.get(BukkitAdapter.adapt(player.getWorld()));

        if (regionManager == null) {
            return null;
        }

        BlockVector3 position = BukkitAdapter.asBlockVector(player.getLocation());
        ApplicableRegionSet regions = regionManager.getApplicableRegions(position);

        for (ProtectedRegion region : regions) {
            return region.getId();
        }

        return null;
    }

    private int getRandomDelay() {
        return new Random().nextInt(maxDelay - minDelay + 1) + minDelay;
    }
}