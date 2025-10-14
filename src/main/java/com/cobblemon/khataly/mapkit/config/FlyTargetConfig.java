package com.cobblemon.khataly.mapkit.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.*;

/**
 * Config per bersagli di volo (senza backup).
 * - Se il JSON è mancante o corrotto, viene rigenerato.
 * - Le entry invalide vengono scartate; le valide sono mantenute.
 * - Salvataggio atomico per evitare file troncati.
 */
public class FlyTargetConfig {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File("config/cobblemonmapkit/flytargets.json");

    /** Mappa in memoria dei target validi (chiave = name lower-case). */
    private static final Map<String, TargetInfo> targets = new HashMap<>();

    /** Versione schema per eventuali migrazioni future (non usata ora, solo serializzata). */
    private static final int CURRENT_SCHEMA_VERSION = 1;

    // ===========================
    //        DATA MODEL
    // ===========================
    public static class TargetInfo {
        public final RegistryKey<World> worldKey;
        public final BlockPos pos;

        public TargetInfo(RegistryKey<World> worldKey, BlockPos pos) {
            this.worldKey = worldKey;
            this.pos = pos;
        }
    }

    private static class ConfigData {
        Integer schemaVersion;               // opzionale; se null, assumiamo CURRENT_SCHEMA_VERSION
        List<FlyTargetData> targets = new ArrayList<>();
    }

    private static class FlyTargetData {
        String name;
        String worldKey;
        int x, y, z;

        FlyTargetData(String name, String worldKey, int x, int y, int z) {
            this.name = name;
            this.worldKey = worldKey;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    // ===========================
    //        API PUBBLICA
    // ===========================

    public static boolean addTarget(String name, RegistryKey<World> worldKey, BlockPos pos) {
        String key = normalizeKey(name);
        if (targets.containsKey(key)) return false;
        targets.put(key, new TargetInfo(worldKey, pos));
        save();
        return true;
    }

    public static boolean removeTarget(String name) {
        String key = normalizeKey(name);
        if (!targets.containsKey(key)) return false;
        targets.remove(key);
        save();
        return true;
    }

    public static TargetInfo getTarget(String name) {
        return targets.get(normalizeKey(name));
    }

    public static Map<String, TargetInfo> getAllTargets() {
        return Collections.unmodifiableMap(targets);
    }

    public static void reload() {
        load();
    }

    // ===========================
    //     LOAD & SAVE LOGICA
    // ===========================

    public static void load() {
        // Se non esiste, crea file base vuoto
        if (!CONFIG_FILE.exists()) {
            logInfo("Config non trovata: creo file vuoto.");
            safeRewrite(Collections.emptyMap());
            return;
        }

        Map<String, TargetInfo> loaded = new HashMap<>();
        boolean filePulito = true; // se diventa false → rigeneriamo

        try (FileReader reader = new FileReader(CONFIG_FILE)) {
            ConfigData data = GSON.fromJson(reader, ConfigData.class);

            if (data == null) {
                filePulito = false;
            } else {
                Integer ver = (data.schemaVersion == null) ? CURRENT_SCHEMA_VERSION : data.schemaVersion;
                if (!Objects.equals(ver, CURRENT_SCHEMA_VERSION)) {
                    // Nessuna migrazione al momento: riscriviamo il file con la versione corrente
                    logWarn("schemaVersion " + ver + " differente da " + CURRENT_SCHEMA_VERSION + ". Riscriverò il file.");
                    filePulito = false;
                }

                if (data.targets == null) {
                    filePulito = false;
                } else {
                    for (FlyTargetData d : data.targets) {
                        if (!isEntryValid(d)) {
                            logWarn("Entry invalida, salto: " + safeName(d));
                            filePulito = false; // riscriveremo “pulito”
                            continue;
                        }

                        Identifier id = Identifier.tryParse(d.worldKey);
                        if (id == null) {
                            logWarn("worldKey non valido: " + d.worldKey + " — salto entry " + d.name);
                            filePulito = false;
                            continue;
                        }

                        RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, id);
                        BlockPos pos = new BlockPos(d.x, d.y, d.z);

                        loaded.put(normalizeKey(d.name), new TargetInfo(worldKey, pos));
                    }
                }
            }
        } catch (JsonParseException e) {
            logError("JSON malformato: " + e.getMessage(), e);
            filePulito = false;
        } catch (IOException e) {
            logError("Errore IO in lettura del file di config", e);
            filePulito = false;
        }

        if (!filePulito) {
            // File corrotto/sporco: rigenera solo con le entry valide (se presenti)
            safeRewrite(loaded);
        } else {
            targets.clear();
            targets.putAll(loaded);
            logInfo("Loaded " + targets.size() + " fly targets.");
        }
    }

    /** Salvataggio atomico: scrive su .tmp e poi move/replace. */
    public static void save() {
        try {
            File dir = CONFIG_FILE.getParentFile();
            if (dir != null && !dir.exists()) {
                if (!dir.mkdirs()) {
                    logWarn("Impossibile creare la directory di configurazione: " + dir.getAbsolutePath());
                }
            }

            ConfigData data = new ConfigData();
            data.schemaVersion = CURRENT_SCHEMA_VERSION;
            for (var entry : targets.entrySet()) {
                String name = entry.getKey();
                TargetInfo info = entry.getValue();
                data.targets.add(new FlyTargetData(
                        name,
                        info.worldKey.getValue().toString(),
                        info.pos.getX(), info.pos.getY(), info.pos.getZ()
                ));
            }

            File temp = new File(CONFIG_FILE.getParent(), CONFIG_FILE.getName() + ".tmp");

            // Scrittura su file temporaneo
            try (FileWriter writer = new FileWriter(temp)) {
                GSON.toJson(data, writer);
            }

            // Move atomico ove supportato; fallback al replace semplice
            try {
                Files.move(
                        temp.toPath(),
                        CONFIG_FILE.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                        StandardCopyOption.ATOMIC_MOVE
                );
            } catch (IOException atomicNotSupported) {
                Files.move(
                        temp.toPath(),
                        CONFIG_FILE.toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (IOException e) {
            logError("Errore durante il salvataggio del file di config", e);
        }
    }

    // ===========================
    //         SUPPORTO
    // ===========================

    private static boolean isEntryValid(FlyTargetData d) {
        if (d == null) return false;
        return d.name != null && !d.name.isBlank()
                && d.worldKey != null && !d.worldKey.isBlank()
                && d.y >= -2048 && d.y <= 4096;
    }

    private static void safeRewrite(Map<String, TargetInfo> validEntries) {
        try {
            File dir = CONFIG_FILE.getParentFile();
            if (dir != null && !dir.exists()) {
                if (!dir.mkdirs()) {
                    logWarn("Impossibile creare la directory di configurazione: " + dir.getAbsolutePath());
                }
            }
            targets.clear();
            if (validEntries != null) targets.putAll(validEntries);
            save(); // serializza lo stato corrente (solo entry valide)
            logInfo("File rigenerato con " + targets.size() + " target validi.");
        } catch (Exception ex) {
            logError("Errore durante la riscrittura sicura del file di config", ex);
        }
    }

    private static String normalizeKey(String name) {
        return name == null ? "" : name.toLowerCase(Locale.ROOT);
    }

    private static String safeName(FlyTargetData d) {
        return (d == null) ? "<null>" : (d.name == null ? "<no-name>" : d.name);
    }

    private static void logInfo(String msg) {
        System.out.println("[FlyTargetConfig] " + msg);
    }

    private static void logWarn(String msg) {
        System.out.println("[FlyTargetConfig][WARN] " + msg);
    }

    private static void logError(String msg) {
        System.err.println("[FlyTargetConfig][ERROR] " + msg);
    }

    private static void logError(String msg, Throwable t) {
        System.err.println("[FlyTargetConfig][ERROR] " + msg);
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            System.err.println(sw.toString());
        }
    }
}
