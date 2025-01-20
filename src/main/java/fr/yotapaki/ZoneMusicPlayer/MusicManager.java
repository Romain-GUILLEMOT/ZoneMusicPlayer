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
import org.bukkit.scheduler.BukkitTask;

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
                String biome = musicJson.has("biome") ? musicJson.get("biome").getAsString() : null;
                musics.add(new Music(name, duration, regionId, biome));
            });
            plugin.getLogger().info("Configuration loaded successfully.");
        } catch (Exception e) {
            plugin.getLogger().severe("Failed to load configuration: " + e.getMessage());
        }
    }

    private void saveDefaultConfig(File configFile) {
        try {
            // Crée le dossier du plugin s'il n'existe pas
            if (!plugin.getDataFolder().exists()) {
                plugin.getDataFolder().mkdirs();
            }

            // Génère la configuration par défaut
            JsonObject defaultConfig = new JsonObject();
            defaultConfig.addProperty("min_delay", 5);
            defaultConfig.addProperty("max_delay", 20);

            // Exemple de musiques
            JsonArray defaultMusics = new JsonArray();
            defaultMusics.add(new Music("custom:pnj.intro.music", 120, "spawn", null).toJson()); // Musique associée à une région
            defaultMusics.add(new Music("custom:forest_theme", 100, null, "FOREST").toJson()); // Musique associée à un biome
            defaultMusics.add(new Music("custom:background.music", 150, null, null).toJson()); // Musique globale
            defaultConfig.add("musics", defaultMusics);

            // Écriture dans le fichier de configuration
            try (Writer writer = new FileWriter(configFile)) {
                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                gson.toJson(defaultConfig, writer);
            }

            plugin.getLogger().info("Default configuration file created successfully.");
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

    private void cancelPlayerTask(Player player) {
        Integer taskId = activePlayers.remove(player);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
    }
    private void handlePlayerMusic(Player player) {
        // Identifier la région actuelle et le biome
        String regionId = getCurrentRegion(player);
        String biome = player.getLocation().getBlock().getBiome().toString();

        // Vérification du contexte actuel (région ou biome ou global)
        String currentContext = currentMusic.get(player);
        String newContext = (regionId != null) ? "REGION:" + regionId : (biome != null ? "BIOME:" + biome : "GLOBAL");

        if (Objects.equals(currentContext, newContext)) {
            // Le joueur est déjà dans le même contexte

            return;
        }

        // Arrêter la musique actuelle si le joueur change de contexte
        stopMusicForPlayer(player);

        // Étape 1 : Priorité à la région
        if (regionId != null) {
            List<Music> regionMusics = musics.stream()
                    .filter(music -> Objects.equals(music.getRegionId(), regionId))
                    .toList();

            if (!regionMusics.isEmpty()) {
                Music randomMusic = regionMusics.get(new Random().nextInt(regionMusics.size())); // Choix aléatoire
                currentMusic.put(player, "REGION:" + regionId);
                playMusic(player, randomMusic, false);
                if (debugPlayers.contains(player)) {
                    player.sendMessage("DEBUG: New context: Region " + regionId);
                }
                return;
            }
        }

        // Étape 2 : Vérification du biome
        List<Music> biomeMusics = musics.stream()
                .filter(music -> Objects.equals(music.getBiome(), biome))
                .toList();

        if (!biomeMusics.isEmpty()) {
            Music randomMusic = biomeMusics.get(new Random().nextInt(biomeMusics.size())); // Choix aléatoire
            currentMusic.put(player, "BIOME:" + biome);
            playMusic(player, randomMusic, false);
            if (debugPlayers.contains(player)) {
                player.sendMessage("DEBUG: New context: Biome " + biome);
            }
            return;
        }

        // Étape 3 : Musique globale
        List<Music> globalMusics = musics.stream()
                .filter(music -> music.getRegionId() == null && music.getBiome() == null)
                .toList();

        if (!globalMusics.isEmpty()) {
            Music randomMusic = globalMusics.get(new Random().nextInt(globalMusics.size())); // Choix aléatoire
            currentMusic.put(player, "GLOBAL");
            playMusic(player, randomMusic, false);
            if (debugPlayers.contains(player)) {
                player.sendMessage("DEBUG: New context: Global");
            }
        }
    }

    private void playMusic(Player player, Music music, boolean notFirst) {
        // Annuler toute tâche en cours pour ce joueur
        cancelPlayerTask(player);
        var time = getRandomDelay();
        time += notFirst ? music.getDuration() : 0;
        if (debugPlayers.contains(player)) {
            player.sendMessage("Schedule music: " + music.getName() + " for player: " + player.getName() + " in "+ time + "s");
        }
        // Planifie la musique

        BukkitTask task = Bukkit.getScheduler().runTaskLater(plugin, () -> {
            player.playSound(player.getLocation(), music.getName(), SoundCategory.AMBIENT, 1.0f, 1.0f);
            if (debugPlayers.contains(player)) {
                player.sendMessage("Playing sound: " + music.getName());
            }

            // Relance une nouvelle musique aléatoire dans le même contexte
            List<Music> musicPool = musics.stream()
                    .filter(m -> Objects.equals(m.getRegionId(), music.getRegionId())
                            || Objects.equals(m.getBiome(), music.getBiome())
                            || (m.getRegionId() == null && m.getBiome() == null))
                    .toList();

            if (!musicPool.isEmpty()) {
                Music nextMusic = musicPool.get(new Random().nextInt(musicPool.size()));
                playMusic(player, nextMusic, true);
            }
        }, (time * 20L));

// Récupérer l'ID de la tâche
        int taskId = task.getTaskId();
        // Associe la tâche au joueur
        activePlayers.put(player, taskId);
    }

    private void stopMusicForPlayer(Player player) {
        Integer taskId = activePlayers.remove(player);
        if (taskId != null) {
            Bukkit.getScheduler().cancelTask(taskId);
        }
        musics.forEach(music -> player.stopSound(music.getName(), SoundCategory.AMBIENT));
        currentMusic.remove(player);
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