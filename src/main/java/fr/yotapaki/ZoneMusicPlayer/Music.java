package fr.yotapaki.ZoneMusicPlayer;

import com.google.gson.JsonObject;

public class Music {

    private final String name;
    private final int duration;
    private final String regionId;

    public Music(String name, int duration, String regionId) {
        this.name = name;
        this.duration = duration;
        this.regionId = regionId;
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

    public JsonObject toJson() {
        JsonObject json = new JsonObject();
        json.addProperty("name", name);
        json.addProperty("duration", duration);
        json.addProperty("region_id", regionId);
        return json;
    }
}