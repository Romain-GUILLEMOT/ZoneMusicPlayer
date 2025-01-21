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
    private final Map<String, Set<String>> biomeGroups = new HashMap<>();
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

            // Charger les groupes de biomes
            biomeGroups.clear();
            JsonObject biomeGroupsJson = config.getAsJsonObject("biome_groups");
            for (String group : biomeGroupsJson.keySet()) {
                JsonArray biomes = biomeGroupsJson.getAsJsonArray(group);
                Set<String> biomeSet = new HashSet<>();
                for (JsonElement biome : biomes) {
                    biomeSet.add(biome.getAsString());
                }
                biomeGroups.put(group, biomeSet);
            }

            // Charger les musiques
            musics.clear();
            config.getAsJsonArray("musics").forEach(jsonElement -> {
                JsonObject musicJson = jsonElement.getAsJsonObject();
                String name = musicJson.get("name").getAsString();
                int duration = musicJson.get("duration").getAsInt();
                String regionId = musicJson.has("region_id") ? musicJson.get("region_id").getAsString() : null;
                String biomeGroup = musicJson.has("biome_group") ? musicJson.get("biome_group").getAsString() : null;
                musics.add(new Music(name, duration, regionId, biomeGroup));
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

            // Ajouter les groupes de biomes
            JsonObject biomeGroups = new JsonObject();
            biomeGroups.add("FOREST_GROUP", new Gson().toJsonTree(List.of("FOREST", "TAIGA", "DARK_FOREST")));
            biomeGroups.add("DESERT_GROUP", new Gson().toJsonTree(List.of("DESERT", "BADLANDS")));
            defaultConfig.add("biome_groups", biomeGroups);

            // Ajouter les musiques
            JsonArray defaultMusics = new JsonArray();
            defaultMusics.add(new Music("custom:region_spawn_music", 120, "spawn", null).toJson()); // Région
            defaultMusics.add(new Music("custom:forest_theme", 100, null, "FOREST").toJson()); // Biome seul
            defaultMusics.add(new Music("custom:desert_theme", 120, null, "DESERT_GROUP").toJson()); // Groupe de biomes
            defaultMusics.add(new Music("custom:global_theme", 150, null, null).toJson()); // Musique globale
            defaultConfig.add("musics", defaultMusics);

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
        String regionId = getCurrentRegion(player);
        String biome = player.getLocation().getBlock().getBiome().toString();

        // Identifier le groupe de biomes correspondant
        String biomeGroup = biomeGroups.entrySet().stream()
                .filter(entry -> entry.getValue().contains(biome))
                .map(Map.Entry::getKey)
                .findFirst()
                .orElse(null);

        // Définir le nouveau contexte
        String currentContext = currentMusic.get(player);
        if(regionId != null) {
            List<Music> filteredMusicsRegion = musics.stream().filter(music -> Objects.equals(music.getRegionId(), regionId)).toList();
            if(!filteredMusicsRegion.isEmpty()) {
                String newContext =  "REGION:" + regionId;
                if (Objects.equals(currentContext, newContext)) {
                    return;
                }
                if (debugPlayers.contains(player)) {
                    player.sendMessage("DEBUG: Attempting to play region music for region: " + regionId);
                }
                stopMusicForPlayer(player);
                playMusicForContext(player, "REGION:" + regionId, filteredMusicsRegion);
                return;
            }
        }
        if (biome != null && biomeMusicsExist(biome)) {
            List<Music> filteredMusicsBiome = musics.stream().filter(music -> Objects.equals(music.getBiome(), biome)).toList();
            if(!filteredMusicsBiome.isEmpty()) {
                String newContext = "BIOME:" + biome;
                if (Objects.equals(currentContext, newContext)) {
                    return;
                }
                if (debugPlayers.contains(player)) {
                    player.sendMessage("DEBUG: Attempting to play music for biome: " + biome);
                }
                stopMusicForPlayer(player);
                playMusicForContext(player, "BIOME:" + biome, filteredMusicsBiome);
                return;
            }
        }
        if (biomeGroup != null) {
            List<Music> filteredMusicsBiomeGroup = musics.stream().filter(music -> Objects.equals(music.getBiome(), biomeGroup)).toList();
            if(!filteredMusicsBiomeGroup.isEmpty()) {
                String newContext = "BIOME_GROUP:" + biomeGroup;
                if (Objects.equals(currentContext, newContext)) {
                    return;
                }
                if (debugPlayers.contains(player)) {
                    player.sendMessage("DEBUG: Attempting to play music for biome group: " + biomeGroup);
                }
                stopMusicForPlayer(player);
                playMusicForContext(player, "BIOME_GROUP:" + biomeGroup,filteredMusicsBiomeGroup);
                return;
            }

        }
        String newContext = "GLOBAL";
        if (Objects.equals(currentContext, newContext)) {
            return;
        }
        stopMusicForPlayer(player);

        List<Music> filteredMusics = musics.stream().filter(music -> music.getRegionId() == null && music.getBiome() == null).toList();

        if (debugPlayers.contains(player)) {
            player.sendMessage("DEBUG: Attempting to play global music.");
        }
        playMusicForContext(player, "GLOBAL", filteredMusics);
    }

    private boolean biomeMusicsExist(String biome) {
        return musics.stream().anyMatch(music -> Objects.equals(music.getBiome(), biome));
    }

    private void playMusicForContext(Player player, String context,  List<Music>  filteredMusics) {

        if (filteredMusics.isEmpty()) {
            if (debugPlayers.contains(player)) {
                player.sendMessage("DEBUG: No music available for context: " + context);
            }
            return;
        }

        Music randomMusic = filteredMusics.get(new Random().nextInt(filteredMusics.size()));
        currentMusic.put(player, context);

        playMusic(player, randomMusic, false, filteredMusics);
    }

    private void playMusic(Player player, Music music, boolean notFirst, List<Music>  filteredMusics) {
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

            if (!filteredMusics.isEmpty()) {
                Music nextMusic = filteredMusics.get(new Random().nextInt(filteredMusics.size()));
                playMusic(player, nextMusic, true, filteredMusics);
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