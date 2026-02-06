package com.cobblemon.khataly.mapkit.config;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.*;

/**
 * Local weather zones:
 * - One zone per file: config/cobblemonmapkit/local_weather_zones/<name>.json
 * - Supports 3D volumes (minX..maxX, minZ..maxZ, minY..maxY)
 */
public final class LocalWeatherZonesConfig {

    private LocalWeatherZonesConfig() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File DIR = new File("config/cobblemonmapkit/local_weather_zones");
    private static final int CURRENT_SCHEMA_VERSION = 1;

    public enum LocalWeatherType {
        CLEAR,
        RAIN,
        THUNDER,
        SNOW,
        SANDSTORM
    }

    public static final class Zone {
        private final UUID id;
        private final String name;
        private final RegistryKey<World> worldKey;
        private final int minX, minZ, maxX, maxZ;
        private final int minY, maxY;
        private final long timeCreated;
        private final LocalWeatherType weatherType;

        public Zone(UUID id, String name, RegistryKey<World> worldKey,
                    int minX, int minZ, int maxX, int maxZ,
                    int minY, int maxY,
                    long timeCreated,
                    LocalWeatherType weatherType) {

            this.id = id;
            this.name = (name == null || name.isBlank()) ? ("WeatherZone " + id.toString().substring(0, 6)) : name.trim();
            this.worldKey = worldKey;

            this.minX = Math.min(minX, maxX);
            this.maxX = Math.max(minX, maxX);
            this.minZ = Math.min(minZ, maxZ);
            this.maxZ = Math.max(minZ, maxZ);

            this.minY = Math.min(minY, maxY);
            this.maxY = Math.max(minY, maxY);

            this.timeCreated = (timeCreated == 0L) ? Instant.now().toEpochMilli() : timeCreated;
            this.weatherType = (weatherType == null) ? LocalWeatherType.CLEAR : weatherType;
        }

        public boolean contains(int x, int y, int z, RegistryKey<World> wk) {
            if (!wk.equals(worldKey)) return false;
            return x >= minX && x <= maxX
                    && z >= minZ && z <= maxZ
                    && y >= minY && y <= maxY;
        }

        public int volume() {
            long dx = (long) (maxX - minX + 1);
            long dy = (long) (maxY - minY + 1);
            long dz = (long) (maxZ - minZ + 1);
            long v = dx * dy * dz;
            return (v > Integer.MAX_VALUE) ? Integer.MAX_VALUE : (int) v;
        }

        public UUID id() { return id; }
        public String name() { return name; }
        public RegistryKey<World> worldKey() { return worldKey; }
        public int minX() { return minX; }
        public int minZ() { return minZ; }
        public int maxX() { return maxX; }
        public int maxZ() { return maxZ; }
        public int minY() { return minY; }
        public int maxY() { return maxY; }
        public long timeCreated() { return timeCreated; }
        public LocalWeatherType weatherType() { return weatherType; }
    }

    // ===== on-disk =====
    private static final class FileWrap {
        Integer schemaVersion;
        ZoneData zone;
    }

    private static final class ZoneData {
        String id;
        String name;
        String worldKey;
        int minX, minZ, maxX, maxZ;
        int minY, maxY;
        long timeCreated;
        String weatherType; // "snow", "sandstorm", etc.
    }

    // ===== state =====
    private static final Map<UUID, Zone> ZONES = new LinkedHashMap<>();
    private static final Map<UUID, File> FILE_BY_ID = new HashMap<>();

    public static void load() {
        ensureDir();
        ZONES.clear();
        FILE_BY_ID.clear();

        File[] files = DIR.listFiles((d, n) -> n.toLowerCase(Locale.ROOT).endsWith(".json"));
        if (files == null || files.length == 0) {
            CobblemonMapKitMod.LOGGER.info("[LocalWeatherZonesConfig] No zones found.");
            return;
        }

        int ok = 0, bad = 0;
        for (File f : files) {
            try {
                Zone z = readZoneFile(f);
                ZONES.put(z.id(), z);
                FILE_BY_ID.put(z.id(), f);
                ok++;
            } catch (Exception e) {
                bad++;
                CobblemonMapKitMod.LOGGER.warn("[LocalWeatherZonesConfig] Could not read {}: {}", f.getName(), e.getMessage());
            }
        }

        CobblemonMapKitMod.LOGGER.info("[LocalWeatherZonesConfig] Loaded {} zones ({} invalid).", ok, bad);
    }

    public static Collection<Zone> getAll() {
        return Collections.unmodifiableCollection(ZONES.values());
    }

    public static Zone get(UUID id) {
        return ZONES.get(id);
    }

    public static boolean overlaps(RegistryKey<World> worldKey,
                                   int minX, int minZ, int maxX, int maxZ,
                                   int minY, int maxY) {
        int aMinX = Math.min(minX, maxX);
        int aMaxX = Math.max(minX, maxX);
        int aMinZ = Math.min(minZ, maxZ);
        int aMaxZ = Math.max(minZ, maxZ);
        int aMinY = Math.min(minY, maxY);
        int aMaxY = Math.max(minY, maxY);

        for (Zone z : ZONES.values()) {
            if (!z.worldKey().equals(worldKey)) continue;

            boolean xOverlap = aMinX <= z.maxX() && aMaxX >= z.minX();
            boolean zOverlap = aMinZ <= z.maxZ() && aMaxZ >= z.minZ();
            boolean yOverlap = aMinY <= z.maxY() && aMaxY >= z.minY();
            if (xOverlap && zOverlap && yOverlap) return true;
        }
        return false;
    }

    /**
     * Returns the "best" zone at a point.
     * If multiple zones overlap, prefers the smallest volume (most specific).
     */
    public static Zone findBestAt(RegistryKey<World> wk, int x, int y, int z) {
        Zone best = null;
        int bestVol = Integer.MAX_VALUE;

        for (Zone zone : ZONES.values()) {
            if (!zone.contains(x, y, z, wk)) continue;
            int v = zone.volume();
            if (v < bestVol) {
                best = zone;
                bestVol = v;
            }
        }
        return best;
    }

    public static UUID addZone(String name,
                               RegistryKey<World> worldKey,
                               int minX, int minZ, int maxX, int maxZ,
                               int minY, int maxY,
                               LocalWeatherType type) {

        UUID id = UUID.randomUUID();
        Zone z = new Zone(id, name, worldKey, minX, minZ, maxX, maxZ, minY, maxY,
                Instant.now().toEpochMilli(), type);

        ZONES.put(id, z);

        try {
            File f = writeZoneFile(z);
            FILE_BY_ID.put(id, f);
        } catch (Exception e) {
            CobblemonMapKitMod.LOGGER.error("[LocalWeatherZonesConfig] Write error on addZone {}: {}", id, e.getMessage(), e);
        }

        return id;
    }

    // ===== internals =====

    private static void ensureDir() {
        if (!DIR.exists()) {
            boolean ok = DIR.mkdirs();
            if (!ok && !DIR.exists()) {
                CobblemonMapKitMod.LOGGER.warn("[LocalWeatherZonesConfig] Unable to create dir: {}", DIR.getAbsolutePath());
            }
        }
    }

    private static File fileFor(Zone z) {
        // simple: stable file name by UUID to avoid rename headaches
        return new File(DIR, z.id().toString() + ".json");
    }

    private static File writeZoneFile(Zone z) throws Exception {
        ensureDir();

        ZoneData zd = new ZoneData();
        zd.id = z.id().toString();
        zd.name = z.name();
        zd.worldKey = z.worldKey().getValue().toString();
        zd.minX = z.minX(); zd.minZ = z.minZ(); zd.maxX = z.maxX(); zd.maxZ = z.maxZ();
        zd.minY = z.minY(); zd.maxY = z.maxY();
        zd.timeCreated = z.timeCreated();
        zd.weatherType = z.weatherType().name().toLowerCase(Locale.ROOT);

        FileWrap wrap = new FileWrap();
        wrap.schemaVersion = CURRENT_SCHEMA_VERSION;
        wrap.zone = zd;

        File target = fileFor(z);
        File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
        try (FileWriter w = new FileWriter(tmp)) {
            GSON.toJson(wrap, w);
        }

        try {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception atomicNotSupported) {
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static Zone readZoneFile(File f) throws Exception {
        try (FileReader r = new FileReader(f)) {
            FileWrap wrap = GSON.fromJson(r, FileWrap.class);
            if (wrap == null || wrap.zone == null) throw new IllegalArgumentException("empty zone file");

            ZoneData zd = wrap.zone;

            UUID id = UUID.fromString(zd.id);
            Identifier wid = Identifier.tryParse(zd.worldKey);
            if (wid == null) throw new IllegalArgumentException("bad worldKey");

            RegistryKey<World> wk = RegistryKey.of(RegistryKeys.WORLD, wid);

            LocalWeatherType type = LocalWeatherType.CLEAR;
            if (zd.weatherType != null && !zd.weatherType.isBlank()) {
                try {
                    type = LocalWeatherType.valueOf(zd.weatherType.trim().toUpperCase(Locale.ROOT));
                } catch (Exception ignored) {}
            }

            return new Zone(id, zd.name, wk,
                    zd.minX, zd.minZ, zd.maxX, zd.maxZ,
                    zd.minY, zd.maxY,
                    zd.timeCreated,
                    type);
        }
    }
}
