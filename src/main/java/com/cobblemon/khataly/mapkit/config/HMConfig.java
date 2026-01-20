package com.cobblemon.khataly.mapkit.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;

public class HMConfig {

    private static final Logger LOGGER = LoggerFactory.getLogger(HMConfig.class);
    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    private static final File CONFIG_FILE = new File("config/cobblemonmapkit/hm.json");

    // --- Respawn times ---
    public static int ROCKSMASH_RESPAWN = 60;
    public static int CUT_RESPAWN = 60;
    public static int STRENGTH_RESPAWN = 60;

    // --- Flash duration ---
    public static int FLASH_DURATION = 60;

    // --- Required Items ---
    public static RequiredItem ROCKSMASH = new RequiredItem("mapkit:steel_badge", "❌ You need the Steel Badge to use Rock Smash!");
    public static RequiredItem FLY = new RequiredItem(null, "❌ You need a special item to use Fly!");
    public static RequiredItem CUT = new RequiredItem(null, "❌ You need a knife or machete to use Cut!");
    public static RequiredItem STRENGTH = new RequiredItem(null, "❌ You need a heavy item to use Strength!");
    public static RequiredItem FLASH = new RequiredItem(null, "❌ You need a Flash Item to use Flash!");
    public static RequiredItem TELEPORT = new RequiredItem(null, "❌ You need a Teleport Item to use Teleport!");
    public static RequiredItem ROCKCLIMB = new RequiredItem(null, "❌ You need climbing gear to use Rock Climb!");
    public static RequiredItem ULTRAHOLE = new RequiredItem(null, "❌ You need Ultrabeast to summon a Ultra Hole!");

    // --- UltraHole default settings ---
    public static UltraHoleSettings ULTRAHOLE_SETTINGS =
            new UltraHoleSettings("minecraft:the_end", 0, 64, 0, 800);

    // --- Strength animation window ---
    public static StrengthAnimationWindow STRENGTH_ANIMATION_WINDOW =
            new StrengthAnimationWindow(false, 120);

    // ------------------------------------------------------------
    // LOAD
    // ------------------------------------------------------------
    public static void load() {
        try {
            if (!CONFIG_FILE.exists()) {
                save();
                return;
            }

            String jsonContent = Files.readString(CONFIG_FILE.toPath());

            if (!jsonContent.contains("\"respawn_time_seconds\"")
                    || !jsonContent.contains("\"flash_duration_seconds\"")
                    || !jsonContent.contains("\"required_items\"")
                    || !jsonContent.contains("\"ultrahole_settings\"")) {

                LOGGER.warn("⚠️ Config obsolete or corrupted, regenerating defaults.");
                CONFIG_FILE.delete();
                save();
                return;
            }

            FileReader reader = new FileReader(CONFIG_FILE);
            ConfigData data = GSON.fromJson(reader, ConfigData.class);
            reader.close();

            boolean missing =
                    data.required_items == null
                            || data.required_items.rocksmash == null
                            || data.required_items.fly == null
                            || data.required_items.cut == null
                            || data.required_items.strength == null
                            || data.required_items.flash == null
                            || data.required_items.teleport == null
                            || data.required_items.rockclimb == null
                            || data.required_items.ultrahole == null
                            || data.ultrahole_settings == null
                            || data.ultrahole_settings.destinationDimension == null;

            if (missing) {
                LOGGER.warn("⚠️ Missing required config entries, regenerating.");
                CONFIG_FILE.delete();
                save();
                return;
            }

            if (data.respawn_time_seconds != null) {
                ROCKSMASH_RESPAWN = data.respawn_time_seconds.rocksmash;
                CUT_RESPAWN = data.respawn_time_seconds.cut;
                STRENGTH_RESPAWN = data.respawn_time_seconds.strength;
            }

            if (data.flash_duration_seconds != null) {
                FLASH_DURATION = data.flash_duration_seconds;
            }

            ROCKSMASH = data.required_items.rocksmash;
            FLY = data.required_items.fly;
            CUT = data.required_items.cut;
            STRENGTH = data.required_items.strength;
            FLASH = data.required_items.flash;
            TELEPORT = data.required_items.teleport;
            ROCKCLIMB = data.required_items.rockclimb;
            ULTRAHOLE = data.required_items.ultrahole;

            ULTRAHOLE_SETTINGS = data.ultrahole_settings;

            // Strength animation window (optional)
            if (data.strength_animation_window != null) {
                if (data.strength_animation_window.seconds < 0)
                    data.strength_animation_window.seconds = 0;

                STRENGTH_ANIMATION_WINDOW = data.strength_animation_window;
            } else {
                STRENGTH_ANIMATION_WINDOW = new StrengthAnimationWindow(false, 120);
            }

        } catch (IOException e) {
            LOGGER.error("Error loading HM config", e);
        }
    }

    // ------------------------------------------------------------
    // SAVE
    // ------------------------------------------------------------
    public static void save() {
        try {
            File parent = CONFIG_FILE.getParentFile();
            if (parent != null && !parent.exists()) parent.mkdirs();

            ConfigData data = new ConfigData();

            data.respawn_time_seconds.rocksmash = ROCKSMASH_RESPAWN;
            data.respawn_time_seconds.cut = CUT_RESPAWN;
            data.respawn_time_seconds.strength = STRENGTH_RESPAWN;
            data.flash_duration_seconds = FLASH_DURATION;

            data.required_items.rocksmash = ROCKSMASH;
            data.required_items.fly = FLY;
            data.required_items.cut = CUT;
            data.required_items.strength = STRENGTH;
            data.required_items.flash = FLASH;
            data.required_items.teleport = TELEPORT;
            data.required_items.rockclimb = ROCKCLIMB;
            data.required_items.ultrahole = ULTRAHOLE;

            data.ultrahole_settings = ULTRAHOLE_SETTINGS;
            data.strength_animation_window = STRENGTH_ANIMATION_WINDOW;

            FileWriter writer = new FileWriter(CONFIG_FILE);
            GSON.toJson(data, writer);
            writer.close();
        } catch (IOException e) {
            LOGGER.error("Error saving HM config", e);
        }
    }

    // ------------------------------------------------------------
    // INTERNAL JSON CLASSES
    // ------------------------------------------------------------

    private static class ConfigData {
        RespawnTimes respawn_time_seconds = new RespawnTimes();
        Integer flash_duration_seconds = FLASH_DURATION;
        RequiredItems required_items = new RequiredItems();
        UltraHoleSettings ultrahole_settings = new UltraHoleSettings();

        // IMPORTANT: no static default here
        StrengthAnimationWindow strength_animation_window;
    }

    private static class RespawnTimes {
        int rocksmash = ROCKSMASH_RESPAWN;
        int cut = CUT_RESPAWN;
        int strength = STRENGTH_RESPAWN;
    }

    public static class RequiredItem {
        public String item;
        public String message;

        public RequiredItem() {}
        public RequiredItem(String item, String message) {
            this.item = item;
            this.message = message;
        }
    }

    private static class RequiredItems {
        RequiredItem rocksmash;
        RequiredItem fly;
        RequiredItem cut;
        RequiredItem strength;
        RequiredItem flash;
        RequiredItem teleport;
        RequiredItem rockclimb;
        RequiredItem ultrahole;
    }

    public static class UltraHoleSettings {
        public String destinationDimension;
        public double x;
        public double y;
        public double z;
        public int durationTicks;

        public UltraHoleSettings() {
            this("minecraft:the_end", 0, 64, 0, 800);
        }

        public UltraHoleSettings(String destinationDimension, double x, double y, double z, int durationTicks) {
            this.destinationDimension = destinationDimension;
            this.x = x;
            this.y = y;
            this.z = z;
            this.durationTicks = durationTicks;
        }
    }

    public static class StrengthAnimationWindow {
        public boolean enabled;
        public int seconds;

        public StrengthAnimationWindow() {
            this(false, 120);
        }

        public StrengthAnimationWindow(boolean enabled, int seconds) {
            this.enabled = enabled;
            this.seconds = seconds;
        }
    }
}
