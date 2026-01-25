package com.cobblemon.khataly.mapkit.config;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import com.google.gson.*;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Fly targets catalog:
 * - Stored at: config/cobblemonmapkit/fly_targets.json
 * - API kept compatible with your commands/watchers:
 *     - TargetInfo class
 *     - getAllTargets()
 *     - addTarget(name, worldKey, pos)
 *     - removeTarget(name)
 *     - reload()
 */
public final class FlyTargetConfig {

    private FlyTargetConfig() {}

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final File DIR = new File("config/cobblemonmapkit");
    private static final File FILE = new File(DIR, "fly_targets.json");

    private static final int CURRENT_SCHEMA = 1;

    /** In-memory catalog: key is ORIGINAL display name (as saved), but lookup is case-insensitive. */
    private static final Map<String, TargetInfo> TARGETS = new LinkedHashMap<>();
    /** Lowercase key -> original key. */
    private static final Map<String, String> KEY_INDEX = new HashMap<>();

    // =========================
    // Public model (expected)
    // =========================
    public static final class TargetInfo {
        public final RegistryKey<World> worldKey;
        public final BlockPos pos;

        public TargetInfo(RegistryKey<World> worldKey, BlockPos pos) {
            this.worldKey = worldKey;
            this.pos = pos;
        }
    }

    // =========================
    // On-disk model
    // =========================
    private static final class Root {
        Integer schemaVersion;
        List<TargetRow> targets;
    }

    private static final class TargetRow {
        String name;
        String worldKey;
        int x, y, z;
    }

    // =========================
    // API used across project
    // =========================
    public static void load() {
        ensureDir();
        TARGETS.clear();
        KEY_INDEX.clear();

        if (!FILE.exists()) {
            save(); // write empty file
            CobblemonMapKitMod.LOGGER.info("[FlyTargetConfig] No fly_targets.json found, created empty.");
            return;
        }

        try (FileReader r = new FileReader(FILE)) {
            Root root = GSON.fromJson(r, Root.class);
            if (root == null || root.targets == null) return;

            for (TargetRow row : root.targets) {
                if (row == null) continue;
                if (row.name == null || row.name.isBlank()) continue;
                Identifier wid = Identifier.tryParse(row.worldKey);
                if (wid == null) continue;

                RegistryKey<World> wk = RegistryKey.of(RegistryKeys.WORLD, wid);
                BlockPos pos = new BlockPos(row.x, row.y, row.z);
                putInternal(row.name.trim(), new TargetInfo(wk, pos));
            }

            CobblemonMapKitMod.LOGGER.info("[FlyTargetConfig] Loaded {} fly targets.", TARGETS.size());
        } catch (Exception e) {
            CobblemonMapKitMod.LOGGER.error("[FlyTargetConfig] Failed to read fly_targets.json: {}", e.getMessage(), e);
        }
    }

    /** Used by your command: /flytarget reload (etc.) */
    public static void reload() {
        load();
    }

    /** Used by command/watchers/menus */
    public static Map<String, TargetInfo> getAllTargets() {
        return Collections.unmodifiableMap(TARGETS);
    }

    /** Add or replace by name (case-insensitive). */
    public static boolean addTarget(String name, RegistryKey<World> worldKey, BlockPos pos) {
        if (name == null || name.isBlank() || worldKey == null || pos == null) return false;

        // If a target exists with same name ignoring case, replace the stored key but preserve original casing of new name.
        removeTarget(name);

        putInternal(name.trim(), new TargetInfo(worldKey, pos));
        save();
        return true;
    }

    /** Remove by NAME (case-insensitive). */
    public static boolean removeTarget(String name) {
        if (name == null || name.isBlank()) return false;
        String keyLower = name.trim().toLowerCase(Locale.ROOT);

        String originalKey = KEY_INDEX.remove(keyLower);
        if (originalKey == null) {
            return false;
        }

        TARGETS.remove(originalKey);
        save();
        return true;
    }

    // =========================
    // Internals
    // =========================
    private static void putInternal(String displayName, TargetInfo info) {
        TARGETS.put(displayName, info);
        KEY_INDEX.put(displayName.toLowerCase(Locale.ROOT), displayName);
    }

    private static void ensureDir() {
        if (!DIR.exists()) {
            boolean ok = DIR.mkdirs();
            if (!ok && !DIR.exists()) {
                CobblemonMapKitMod.LOGGER.warn("[FlyTargetConfig] Unable to create config dir: {}", DIR.getAbsolutePath());
            }
        }
    }

    private static void save() {
        ensureDir();

        Root root = new Root();
        root.schemaVersion = CURRENT_SCHEMA;
        root.targets = new ArrayList<>();

        for (Map.Entry<String, TargetInfo> e : TARGETS.entrySet()) {
            String name = e.getKey();
            TargetInfo info = e.getValue();
            if (info == null || info.worldKey == null || info.pos == null) continue;

            TargetRow row = new TargetRow();
            row.name = name;
            row.worldKey = info.worldKey.getValue().toString();
            row.x = info.pos.getX();
            row.y = info.pos.getY();
            row.z = info.pos.getZ();
            root.targets.add(row);
        }

        File tmp = new File(FILE.getParentFile(), FILE.getName() + ".tmp");
        try (FileWriter w = new FileWriter(tmp)) {
            GSON.toJson(root, w);
        } catch (Exception e) {
            CobblemonMapKitMod.LOGGER.error("[FlyTargetConfig] Failed writing temp fly_targets.json: {}", e.getMessage(), e);
            return;
        }

        try {
            Files.move(tmp.toPath(), FILE.toPath(),
                    StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
        } catch (Exception atomicNotSupported) {
            try {
                Files.move(tmp.toPath(), FILE.toPath(),
                        StandardCopyOption.REPLACE_EXISTING);
            } catch (Exception e) {
                CobblemonMapKitMod.LOGGER.error("[FlyTargetConfig] Failed saving fly_targets.json: {}", e.getMessage(), e);
            }
        }
    }
}
