package com.cobblemon.khataly.mapkit.config;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import com.google.gson.*;
import com.google.gson.annotations.SerializedName;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.world.World;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.text.Normalizer;
import java.time.Instant;
import java.util.*;

/**
 * Grass Zones (3D: X/Z + range verticale Y):
 *  - Una zona per file: config/cobblemonmapkit/zones/<nome>.json
 *
 * Schema v6:
 *  - Per-zone ShinyMultiplier (double), serialized exactly as "ShinyMultiplier"
 *    - 1.0 = normal
 *    - 2.0 = double shiny chance (halve odds)
 *    - 0.5 = half shiny chance (double odds)
 *  - Keeps legacy read support for shinyOdds (1/N). If present and ShinyMultiplier missing,
 *    it converts assuming Cobblemon default base odds 8192: multiplier = 8192 / shinyOdds.
 *
 * Schema v5 legacy:
 *  - per-spawn "medium": "land" | "water" | "both"
 */
public class GrassZonesConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File ZONES_DIR = new File("config/cobblemonmapkit/zones");

    /** Bump for ShinyMultiplier + legacy shinyOdds migration. */
    private static final int CURRENT_SCHEMA_VERSION = 6;

    /** Used ONLY to convert legacy shinyOdds -> multiplier. */
    private static final double LEGACY_BASE_ODDS_FOR_CONVERSION = 8192.0;

    public enum TimeBand { DAY, NIGHT, BOTH }
    public enum MediumBand { LAND, WATER, BOTH }

    // ======== DATA MODEL ========
    public static final class SpawnEntry {
        public final String species;
        public final int minLevel;
        public final int maxLevel;
        public final int weight;
        public final TimeBand time;
        /** Variante opzionale (es. "alola", "hisui", "galar", ...). */
        public final String aspect;
        /** Medium restriction (default BOTH). */
        public final MediumBand medium;

        public SpawnEntry(String species, int minLevel, int maxLevel, int weight, TimeBand time, String aspect, MediumBand medium) {
            this.species = species;
            this.minLevel = minLevel;
            this.maxLevel = maxLevel;
            this.weight = weight;
            this.time = (time == null) ? TimeBand.BOTH : time;
            this.aspect = (aspect != null && !aspect.isBlank()) ? aspect : null;
            this.medium = (medium == null) ? MediumBand.BOTH : medium;
        }

        public SpawnEntry(String species, int minLevel, int maxLevel, int weight, TimeBand time, String aspect) {
            this(species, minLevel, maxLevel, weight, time, aspect, MediumBand.BOTH);
        }

        public SpawnEntry(String species, int minLevel, int maxLevel, int weight, TimeBand time) {
            this(species, minLevel, maxLevel, weight, time, null, MediumBand.BOTH);
        }

        public SpawnEntry(String species, int minLevel, int maxLevel, int weight) {
            this(species, minLevel, maxLevel, weight, TimeBand.BOTH, null, MediumBand.BOTH);
        }
    }

    public static final class Zone {
        private final UUID id;
        private final String name;
        private final RegistryKey<World> worldKey;
        private final int minX, minZ, maxX, maxZ;
        private final int minY, maxY;
        private final long timeCreated;
        private final List<SpawnEntry> spawns;

        /** Shiny multiplier for this zone (1.0 = normal). */
        private final double shinyMultiplier;

        public Zone(UUID id,
                    String name,
                    RegistryKey<World> worldKey,
                    int minX, int minZ, int maxX, int maxZ,
                    int minY, int maxY,
                    long timeCreated,
                    List<SpawnEntry> spawns,
                    double shinyMultiplier) {
            this.id = id;
            this.name = (name == null || name.isBlank()) ? ("Zone " + shortId(id)) : name.trim();
            this.worldKey = worldKey;
            this.minX = Math.min(minX, maxX);
            this.minZ = Math.min(minZ, maxZ);
            this.maxX = Math.max(minX, maxX);
            this.maxZ = Math.max(minZ, maxZ);
            this.minY = Math.min(minY, maxY);
            this.maxY = Math.max(minY, maxY);
            this.timeCreated = timeCreated;
            this.spawns = List.copyOf(spawns == null ? List.of() : spawns);

            double m = shinyMultiplier;
            if (Double.isNaN(m) || Double.isInfinite(m) || m <= 0) m = 1.0;
            this.shinyMultiplier = m;
        }

        public boolean contains(int x, int y, int z, RegistryKey<World> w) {
            if (!w.equals(worldKey)) return false;
            return x >= minX && x <= maxX
                    && z >= minZ && z <= maxZ
                    && y >= minY && y <= maxY;
        }

        // getters
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
        public List<SpawnEntry> spawns() { return spawns; }
        public double shinyMultiplier() { return shinyMultiplier; }

        public Zone withName(String newName) {
            return new Zone(id, newName, worldKey, minX, minZ, maxX, maxZ, minY, maxY, timeCreated, spawns, shinyMultiplier);
        }
        public Zone withShinyMultiplier(double newMult) {
            return new Zone(id, name, worldKey, minX, minZ, maxX, maxZ, minY, maxY, timeCreated, spawns, newMult);
        }
        public Zone withSpawns(List<SpawnEntry> newSpawns) {
            return new Zone(id, name, worldKey, minX, minZ, maxX, maxZ, minY, maxY, timeCreated, newSpawns, shinyMultiplier);
        }
    }

    // ======== ON-DISK STRUCTS ========
    private static class FileWrap {
        Integer schemaVersion;
        ZoneData zone;
    }
    private static class ConfigDataLegacy {
        Integer schemaVersion;
        List<ZoneData> zones;
    }
    private static class ZoneData {
        String id;
        String name;
        String worldKey;
        int minX, minZ, maxX, maxZ;

        Integer minY;
        Integer maxY;

        /** LEGACY: singolo Y. */
        Integer y;

        long timeCreated;
        List<SpawnData> spawns;

        /** LEGACY v5: 1/N odds. */
        Integer shinyOdds;

        /** v6: write exactly "ShinyMultiplier" */
        @SerializedName(value = "ShinyMultiplier", alternate = {"shinyMultiplier", "shiny_multiplier"})
        Double shinyMultiplier;
    }
    private static class SpawnData {
        String species;
        int minLevel, maxLevel, weight;
        String time;    // "day" | "night" | "both"
        String aspect;  // opzionale
        String medium;  // "land" | "water" | "both"
    }

    // ======== IN-MEMORY STATE ========
    private static final Map<UUID, Zone> ZONES = new LinkedHashMap<>();
    private static final Map<UUID, File> FILE_BY_ID = new HashMap<>();

    // ======== API ========
    public static void load() {
        ensureDir();
        ZONES.clear();
        FILE_BY_ID.clear();

        File legacy = new File("config/cobblemonmapkit/grass_zones.json");
        if (legacy.exists()) {
            migrateLegacy(legacy);
            return;
        }

        File[] files = ZONES_DIR.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json"));
        if (files == null || files.length == 0) {
            CobblemonMapKitMod.LOGGER.info("[GrassZonesConfig] No zones found.");
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
                CobblemonMapKitMod.LOGGER.warn("[GrassZonesConfig] Could not read {}: {}", f.getName(), e.getMessage());
                bad++;
            }
        }
        CobblemonMapKitMod.LOGGER.info("[GrassZonesConfig] Loaded {} zones ({} invalid).", ok, bad);
    }

    /** Overlap 3D con Y puntuale. */
    public static boolean overlaps(RegistryKey<World> worldKey, int minX, int minZ, int maxX, int maxZ, int y) {
        int aMinX = Math.min(minX, maxX);
        int aMaxX = Math.max(minX, maxX);
        int aMinZ = Math.min(minZ, maxZ);
        int aMaxZ = Math.max(minZ, maxZ);

        for (Zone z : ZONES.values()) {
            if (!z.worldKey().equals(worldKey)) continue;
            boolean xOverlap = aMinX <= z.maxX() && aMaxX >= z.minX();
            boolean zOverlap = aMinZ <= z.maxZ() && aMaxZ >= z.minZ();
            boolean yInside = y >= z.minY() && y <= z.maxY();
            if (xOverlap && zOverlap && yInside) return true;
        }
        return false;
    }

    /** Overlap 3D tra due prismi (range Y completo). */
    public static boolean overlaps(RegistryKey<World> worldKey, int minX, int minZ, int maxX, int maxZ, int minY, int maxY) {
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

    /** Overlap 2D (ignora Y). */
    public static boolean overlaps(RegistryKey<World> worldKey, int minX, int minZ, int maxX, int maxZ) {
        int aMinX = Math.min(minX, maxX);
        int aMaxX = Math.max(minX, maxX);
        int aMinZ = Math.min(minZ, maxZ);
        int aMaxZ = Math.max(minZ, maxZ);

        for (Zone z : ZONES.values()) {
            if (!z.worldKey().equals(worldKey)) continue;
            boolean xOverlap = aMinX <= z.maxX() && aMaxX >= z.minX();
            boolean zOverlap = aMinZ <= z.maxZ() && aMaxZ >= z.minZ();
            if (xOverlap && zOverlap) return true;
        }
        return false;
    }

    public static void save() {
        try {
            ensureDir();
            Set<File> keep = new HashSet<>();
            for (Zone z : ZONES.values()) {
                File f = writeZoneFile(z);
                keep.add(f);
                FILE_BY_ID.put(z.id(), f);
            }
            File[] files = ZONES_DIR.listFiles((dir, name) -> name.toLowerCase(Locale.ROOT).endsWith(".json"));
            if (files != null) {
                for (File f : files) {
                    if (!keep.contains(f)) {
                        boolean deleted = f.delete();
                        if (!deleted) CobblemonMapKitMod.LOGGER.warn("[GrassZonesConfig] Could not delete orphan {}", f.getName());
                    }
                }
            }
        } catch (IOException e) {
            CobblemonMapKitMod.LOGGER.error("[GrassZonesConfig] Save error: {}", e.getMessage(), e);
        }
    }

    // ----- addZone overloads -----

    public static UUID addZone(String name,
                               RegistryKey<World> worldKey,
                               int minX, int minZ, int maxX, int maxZ,
                               int y,
                               List<SpawnEntry> spawns) {
        return addZone(name, worldKey, minX, minZ, maxX, maxZ, y, y, spawns, 1.0);
    }

    public static UUID addZone(String name,
                               RegistryKey<World> worldKey,
                               int minX, int minZ, int maxX, int maxZ,
                               int minY, int maxY,
                               List<SpawnEntry> spawns) {
        return addZone(name, worldKey, minX, minZ, maxX, maxZ, minY, maxY, spawns, 1.0);
    }

    public static UUID addZone(String name,
                               RegistryKey<World> worldKey,
                               int minX, int minZ, int maxX, int maxZ,
                               int minY, int maxY,
                               List<SpawnEntry> spawns,
                               double shinyMultiplier) {
        UUID id = UUID.randomUUID();
        Zone z = new Zone(
                id, name, worldKey,
                minX, minZ, maxX, maxZ,
                minY, maxY,
                Instant.now().toEpochMilli(),
                spawns == null ? List.of() : spawns,
                shinyMultiplier
        );
        ZONES.put(id, z);
        try {
            File f = writeZoneFile(z);
            FILE_BY_ID.put(id, f);
        } catch (IOException e) {
            CobblemonMapKitMod.LOGGER.error("[GrassZonesConfig] Write error on addZone {}: {}", id, e.getMessage(), e);
        }
        return id;
    }

    public static boolean removeZone(UUID id) {
        Zone removed = ZONES.remove(id);
        File f = FILE_BY_ID.remove(id);
        if (removed != null) {
            if (f == null) f = guessFileByName(removed.name());
            if (f.exists() && !f.delete()) {
                CobblemonMapKitMod.LOGGER.warn("[GrassZonesConfig] Could not delete file {}", f.getName());
            }
            return true;
        }
        return false;
    }

    public static List<Zone> findAt(RegistryKey<World> wk, int x, int y, int z) {
        List<Zone> out = new ArrayList<>();
        for (Zone z0 : ZONES.values()) if (z0.contains(x, y, z, wk)) out.add(z0);
        return out;
    }

    public static boolean addSpawn(UUID zoneId, SpawnEntry entry) {
        Zone z = ZONES.get(zoneId); if (z == null) return false;
        List<SpawnEntry> ns = new ArrayList<>(z.spawns()); ns.add(entry);
        Zone nz = z.withSpawns(ns);
        ZONES.put(zoneId, nz);
        try {
            File f = writeZoneFile(nz);
            FILE_BY_ID.put(zoneId, f);
        } catch (IOException e) {
            CobblemonMapKitMod.LOGGER.error("[GrassZonesConfig] Write error on addSpawn {}: {}", zoneId, e.getMessage(), e);
        }
        return true;
    }

    public static boolean removeSpawn(UUID zoneId, String speciesId) {
        Zone z = ZONES.get(zoneId); if (z == null) return false;
        List<SpawnEntry> ns = new ArrayList<>();
        for (SpawnEntry e : z.spawns()) if (!e.species.equalsIgnoreCase(speciesId)) ns.add(e);
        Zone nz = z.withSpawns(ns);
        ZONES.put(zoneId, nz);
        try {
            File f = writeZoneFile(nz);
            FILE_BY_ID.put(zoneId, f);
        } catch (IOException e) {
            CobblemonMapKitMod.LOGGER.error("[GrassZonesConfig] Write error on removeSpawn {}: {}", zoneId, e.getMessage(), e);
        }
        return true;
    }

    /** Set per-zone shiny multiplier. */
    public static boolean setZoneShinyMultiplier(UUID zoneId, double shinyMultiplier) {
        Zone z = ZONES.get(zoneId); if (z == null) return false;
        Zone nz = z.withShinyMultiplier(shinyMultiplier);
        ZONES.put(zoneId, nz);
        try {
            File f = writeZoneFile(nz);
            FILE_BY_ID.put(zoneId, f);
        } catch (IOException e) {
            CobblemonMapKitMod.LOGGER.error("[GrassZonesConfig] Write error on setZoneShinyMultiplier {}: {}", zoneId, e.getMessage(), e);
        }
        return true;
    }

    public static boolean setZoneName(UUID zoneId, String newName) {
        if (newName == null || newName.isBlank()) return false;
        Zone z = ZONES.get(zoneId); if (z == null) return false;
        Zone nz = z.withName(newName);
        ZONES.put(zoneId, nz);
        try {
            File oldFile = FILE_BY_ID.get(zoneId);
            File newFile = writeZoneFile(nz);
            FILE_BY_ID.put(zoneId, newFile);
            if (oldFile != null && !sameFile(oldFile, newFile) && oldFile.exists()) {
                if (!isFileUsedByOtherZone(oldFile, zoneId)) {
                    boolean deleted = oldFile.delete();
                    if (!deleted) CobblemonMapKitMod.LOGGER.warn("[GrassZonesConfig] Could not delete old file {}", oldFile.getName());
                }
            }
        } catch (IOException e) {
            CobblemonMapKitMod.LOGGER.error("[GrassZonesConfig] Write error on setZoneName {}: {}", zoneId, e.getMessage(), e);
            return false;
        }
        return true;
    }

    public static Collection<Zone> getAll() { return Collections.unmodifiableCollection(ZONES.values()); }
    public static Zone get(UUID id) { return ZONES.get(id); }

    // ======== INTERNALS ========

    private static void migrateLegacy(File legacy) {
        CobblemonMapKitMod.LOGGER.info("[GrassZonesConfig] Migrating from legacy grass_zones.json to zones/ ...");
        List<Zone> loaded = new ArrayList<>();
        try (FileReader r = new FileReader(legacy)) {
            ConfigDataLegacy data = GSON.fromJson(r, ConfigDataLegacy.class);
            if (data != null && data.zones != null) {
                for (ZoneData zd : data.zones) {
                    try {
                        Zone z = fromZoneData(zd);
                        loaded.add(z);
                    } catch (Exception ex) {
                        CobblemonMapKitMod.LOGGER.warn("[GrassZonesConfig] Invalid legacy zone, skipping: {}", (zd != null ? zd.id : "<null>"));
                    }
                }
            }
        } catch (Exception e) {
            CobblemonMapKitMod.LOGGER.error("[GrassZonesConfig] Legacy read error: {}", e.getMessage(), e);
        }

        for (Zone z : loaded) {
            ZONES.put(z.id(), z);
            try {
                File f = writeZoneFile(z);
                FILE_BY_ID.put(z.id(), f);
            } catch (IOException e) {
                CobblemonMapKitMod.LOGGER.error("[GrassZonesConfig] Error writing migrated zone {}: {}", z.id(), e.getMessage(), e);
            }
        }

        File bak = new File(legacy.getParentFile(), legacy.getName() + ".bak");
        if (!legacy.renameTo(bak)) {
            CobblemonMapKitMod.LOGGER.warn("[GrassZonesConfig] Could not rename legacy file, leaving it in place.");
        }
        CobblemonMapKitMod.LOGGER.info("[GrassZonesConfig] Migration complete: {} zones.", ZONES.size());
    }

    private static TimeBand parseTime(String s) {
        if (s == null) return TimeBand.BOTH;
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "day" -> TimeBand.DAY;
            case "night" -> TimeBand.NIGHT;
            default -> TimeBand.BOTH;
        };
    }

    private static MediumBand parseMedium(String s) {
        if (s == null) return MediumBand.BOTH;
        return switch (s.toLowerCase(Locale.ROOT)) {
            case "land" -> MediumBand.LAND;
            case "water" -> MediumBand.WATER;
            default -> MediumBand.BOTH;
        };
    }

    private static void ensureDir() {
        File parent = ZONES_DIR.getParentFile();
        if (parent != null && !parent.exists()) {
            boolean okParent = parent.mkdirs();
            if (!okParent && !parent.exists()) {
                CobblemonMapKitMod.LOGGER.warn("[GrassZonesConfig] Unable to create parent dir: {}", parent.getAbsolutePath());
            }
        }
        if (!ZONES_DIR.exists()) {
            boolean ok = ZONES_DIR.mkdirs();
            if (!ok && !ZONES_DIR.exists()) {
                CobblemonMapKitMod.LOGGER.warn("[GrassZonesConfig] Unable to create zones dir: {}", ZONES_DIR.getAbsolutePath());
            }
        }
    }

    private static String shortId(UUID id) {
        String s = id.toString().replace("-", "");
        return s.substring(0, 6).toUpperCase(Locale.ROOT);
    }

    private static String sanitizeForFilename(String name) {
        String n = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("\\p{M}+", "");
        n = n.replaceAll("[^\\w\\-.\\s]", "_").trim();
        n = n.replaceAll("\\s+", " ");
        if (n.isEmpty()) n = "Zone";
        if (n.length() > 80) n = n.substring(0, 80).trim();
        return n;
    }

    private static File guessFileByName(String zoneName) {
        String base = sanitizeForFilename(zoneName);
        return new File(ZONES_DIR, base + ".json");
    }

    private static boolean sameFile(File a, File b) {
        try {
            return a.getCanonicalPath().equals(b.getCanonicalPath());
        } catch (IOException e) {
            return a.getAbsolutePath().equals(b.getAbsolutePath());
        }
    }

    private static boolean isFileUsedByOtherZone(File f, UUID currentId) {
        for (Map.Entry<UUID, File> e : FILE_BY_ID.entrySet()) {
            if (!e.getKey().equals(currentId) && sameFile(e.getValue(), f)) return true;
        }
        return false;
    }

    private static File uniqueFileForName(String desiredName, UUID ownerId) {
        String base = sanitizeForFilename(desiredName);
        File f = new File(ZONES_DIR, base + ".json");
        if (!existsDifferentOwner(f, ownerId)) return f;

        int i = 2;
        while (true) {
            File cand = new File(ZONES_DIR, base + " (" + i + ").json");
            if (!existsDifferentOwner(cand, ownerId)) return cand;
            i++;
        }
    }

    private static boolean existsDifferentOwner(File f, UUID ownerId) {
        if (!f.exists()) return false;
        try {
            Zone z = readZoneFile(f);
            return !z.id().equals(ownerId);
        } catch (Exception e) {
            return true;
        }
    }

    private static File writeZoneFile(Zone z) throws IOException {
        ensureDir();

        File current = FILE_BY_ID.get(z.id());
        File target = uniqueFileForName(z.name(), z.id());

        if (current != null && current.exists()) {
            if (sameFile(current, target)) return writeJson(target, z);
            File written = writeJson(target, z);
            if (!isFileUsedByOtherZone(current, z.id()) && current.exists()) {
                boolean deleted = current.delete();
                if (!deleted) CobblemonMapKitMod.LOGGER.warn("[GrassZonesConfig] Could not delete old file {}", current.getName());
            }
            return written;
        } else {
            return writeJson(target, z);
        }
    }

    private static File writeJson(File target, Zone z) throws IOException {
        ZoneData zd = toZoneData(z);
        FileWrap wrap = new FileWrap();
        wrap.schemaVersion = CURRENT_SCHEMA_VERSION;
        wrap.zone = zd;

        File tmp = new File(target.getParentFile(), target.getName() + ".tmp");
        try (FileWriter w = new FileWriter(tmp)) {
            GSON.toJson(wrap, w);
        }
        try {
            Files.move(tmp.toPath(), target.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (IOException atomicNotSupported) {
            CobblemonMapKitMod.LOGGER.debug("[GrassZonesConfig] ATOMIC_MOVE not supported for {}, falling back.", target.getName());
            Files.move(tmp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
        return target;
    }

    private static ZoneData toZoneData(Zone z) {
        ZoneData zd = new ZoneData();
        zd.id = z.id().toString();
        zd.name = z.name();
        zd.worldKey = z.worldKey().getValue().toString();
        zd.minX = z.minX(); zd.minZ = z.minZ(); zd.maxX = z.maxX(); zd.maxZ = z.maxZ();
        zd.minY = z.minY(); zd.maxY = z.maxY();
        zd.y = null;
        zd.timeCreated = z.timeCreated();

        // NEW: always write ShinyMultiplier exactly
        zd.shinyMultiplier = z.shinyMultiplier();

        // legacy field not written anymore
        zd.shinyOdds = null;

        zd.spawns = new ArrayList<>();
        for (SpawnEntry se : z.spawns()) {
            SpawnData sd = new SpawnData();
            sd.species = se.species;
            sd.minLevel = se.minLevel;
            sd.maxLevel = se.maxLevel;
            sd.weight = se.weight;
            sd.time = se.time.name().toLowerCase(Locale.ROOT);
            if (se.aspect != null && !se.aspect.isBlank()) sd.aspect = se.aspect;

            MediumBand m = (se.medium == null) ? MediumBand.BOTH : se.medium;
            sd.medium = m.name().toLowerCase(Locale.ROOT);

            zd.spawns.add(sd);
        }
        return zd;
    }

    private static Zone fromZoneData(ZoneData zd) {
        UUID id = UUID.fromString(zd.id);
        Identifier wid = Identifier.tryParse(zd.worldKey);
        if (wid == null) throw new IllegalArgumentException("bad worldKey");
        RegistryKey<World> wk = RegistryKey.of(RegistryKeys.WORLD, wid);

        int minY, maxY;
        if (zd.minY != null || zd.maxY != null) {
            int minY0 = zd.minY != null ? zd.minY : zd.maxY;
            int maxY0 = (zd.maxY != null) ? zd.maxY : minY0;
            minY = Math.min(minY0, maxY0);
            maxY = Math.max(minY0, maxY0);
        } else if (zd.y != null) {
            minY = zd.y;
            maxY = zd.y;
        } else {
            CobblemonMapKitMod.LOGGER.warn("[GrassZonesConfig] Missing Y info for zone {}, defaulting to 0..0", zd.id);
            minY = 0; maxY = 0;
        }

        List<SpawnEntry> spawns = new ArrayList<>();
        if (zd.spawns != null) {
            for (SpawnData sd : zd.spawns) {
                if (sd == null || sd.species == null || sd.species.isBlank()
                        || sd.minLevel <= 0 || sd.maxLevel < sd.minLevel || sd.weight <= 0) {
                    CobblemonMapKitMod.LOGGER.warn("[GrassZonesConfig] Invalid spawn in zone {}: {}", zd.id, sd);
                    continue;
                }

                TimeBand tb = parseTime(sd.time);
                MediumBand mb = parseMedium(sd.medium);

                spawns.add(new SpawnEntry(sd.species, sd.minLevel, sd.maxLevel, sd.weight, tb, sd.aspect, mb));
            }
        }

        long t = zd.timeCreated == 0 ? Instant.now().toEpochMilli() : zd.timeCreated;
        String name = (zd.name == null || zd.name.isBlank()) ? ("Zone " + shortId(id)) : zd.name;

        // NEW multiplier:
        double mult = 1.0;
        if (zd.shinyMultiplier != null) {
            mult = zd.shinyMultiplier;
        } else if (zd.shinyOdds != null && zd.shinyOdds > 0) {
            // legacy convert: multiplier = 8192 / shinyOdds
            mult = LEGACY_BASE_ODDS_FOR_CONVERSION / (double) zd.shinyOdds;
        }

        if (Double.isNaN(mult) || Double.isInfinite(mult) || mult <= 0) mult = 1.0;

        return new Zone(id, name, wk, zd.minX, zd.minZ, zd.maxX, zd.maxZ, minY, maxY, t, spawns, mult);
    }

    private static Zone readZoneFile(File f) throws IOException {
        try (FileReader r = new FileReader(f)) {
            FileWrap wrap = GSON.fromJson(r, FileWrap.class);
            int ver = (wrap != null && wrap.schemaVersion != null) ? wrap.schemaVersion : CURRENT_SCHEMA_VERSION;
            if (wrap == null || wrap.zone == null) throw new IOException("empty or invalid wrap");
            Zone z = fromZoneData(wrap.zone);
            if (ver != CURRENT_SCHEMA_VERSION) {
                CobblemonMapKitMod.LOGGER.warn("[GrassZonesConfig] schemaVersion {} != {} in {}, will rewrite on save.",
                        ver, CURRENT_SCHEMA_VERSION, f.getName());
            }
            return z;
        }
    }
}
