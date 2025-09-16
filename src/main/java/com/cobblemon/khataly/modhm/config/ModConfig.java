package com.cobblemon.khataly.modhm.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class ModConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/modhm/config.json");

    public static int ROCKSMASH_RESPAWN = 60;
    public static int CUT_RESPAWN = 60;
    public static int STRENGTH_RESPAWN = 60;

    // 🔹 Nuova variabile per la durata di Flash/Night Vision
    public static int FLASH_DURATION = 60; // in secondi

    public static void load() {
        try {
            if (!CONFIG_FILE.exists()) {
                save(); // crea il file con valori di default
            }

            FileReader reader = new FileReader(CONFIG_FILE);
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            reader.close();

            if (data.respawn_time_seconds != null) {
                ROCKSMASH_RESPAWN = data.respawn_time_seconds.rocksmash;
                CUT_RESPAWN = data.respawn_time_seconds.cut;
                STRENGTH_RESPAWN = data.respawn_time_seconds.strength;
            }

            // Leggi FLASH_DURATION dal file JSON
            if (data.flash_duration_seconds != null) {
                FLASH_DURATION = data.flash_duration_seconds;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void save() {
        try {
            CONFIG_FILE.getParentFile().mkdirs();
            ConfigData data = new ConfigData();
            data.respawn_time_seconds.rocksmash = ROCKSMASH_RESPAWN;
            data.respawn_time_seconds.cut = CUT_RESPAWN;
            data.respawn_time_seconds.strength = STRENGTH_RESPAWN;
            data.flash_duration_seconds = FLASH_DURATION; // salva durata Flash/Night Vision

            FileWriter writer = new FileWriter(CONFIG_FILE);
            GSON.toJson(data, writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class ConfigData {
        RespawnTimes respawn_time_seconds = new RespawnTimes();
        Integer flash_duration_seconds = 60; // default
    }

    private static class RespawnTimes {
        int rocksmash = 60;
        int cut = 60;
        int strength = 60;
    }
}
