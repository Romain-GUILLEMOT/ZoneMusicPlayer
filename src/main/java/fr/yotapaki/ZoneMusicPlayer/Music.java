package fr.yotapaki.ZoneMusicPlayer;

import com.google.gson.JsonObject;

public class Music {

    private final String name;
    private final int duration;
    private final String regionId;
    private final String biome;

    public Music(String name, int duration, String regionId, String biome) {
        this.name = name;
        this.duration = duration;
        this.regionId = regionId;
        this.biome = biome;
    }

    public String getName() {
        return name;
    }

    public int getDuration() {
        return duration;
    }

    public String getRegionId() {
        return regionId;
    }
    public String getBiome() {
        return biome;
    }
    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("duration", duration);
        json.addProperty("region_id", regionId);
        json.addProperty("biome", biome);

        return json;
    }
}