package com.cobblemon.khataly.modhm.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.util.*;

/**
 * Gestisce l'elenco dei FlyTarget sbloccati per giocatore (per-UUID).
 * Salva su: config/modhm/player_flytargets/<uuid>.json
 */
public class PlayerFlyProgress {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File ROOT = new File("config/modhm/player_flytargets");
    private static final int CURRENT_SCHEMA_VERSION = 1;

    /** Cache in memoria: UUID -> set di nomi target (lowercase) sbloccati */
    private static final Map<UUID, Set<String>> cache = new HashMap<>();

    // ===================== API =====================

    /** Restituisce un set NON modificabile dei target sbloccati. */
    public static Set<String> getUnlocked(UUID uuid) {
        ensureLoaded(uuid);
        return Collections.unmodifiableSet(cache.getOrDefault(uuid, Collections.emptySet()));
    }

    /** True se il target (key lowercase) è sbloccato per il player. */
    public static boolean isUnlocked(UUID uuid, String keyLower) {
        ensureLoaded(uuid);
        return cache.getOrDefault(uuid, Collections.emptySet()).contains(keyLower);
    }

    /** Sblocca un target. Ritorna true se è stato aggiunto (nuovo), false se era già presente. */
    public static boolean unlock(UUID uuid, String keyLower) {
        ensureLoaded(uuid);
        Set<String> set = cache.computeIfAbsent(uuid, u -> new HashSet<>());
        boolean added = set.add(keyLower);
        if (added) save(uuid); // salvataggio immediato
        return added;
    }
    public static void clearAll(java.util.UUID uuid) {
        cache.put(uuid, new java.util.HashSet<>());
        save(uuid);
    }

    /** Sblocca in massa più target. */
    public static void unlockAll(UUID uuid, Collection<String> keysLower) {
        ensureLoaded(uuid);
        if (keysLower == null || keysLower.isEmpty()) return;
        Set<String> set = cache.computeIfAbsent(uuid, u -> new HashSet<>());
        if (set.addAll(keysLower)) save(uuid);
    }

    // ================== LOAD/SAVE ==================

    private static void ensureLoaded(UUID uuid) {
        if (cache.containsKey(uuid)) return;
        cache.put(uuid, new HashSet<>()); // evita rientri
        load(uuid);
    }

    private static File fileFor(UUID uuid) {
        return new File(ROOT, uuid.toString() + ".json");
    }

    private static void load(UUID uuid) {
        try {
            File dir = ROOT;
            if (!dir.exists() && !dir.mkdirs()) {
                logWarn("Impossibile creare cartella progressi: " + dir.getAbsolutePath());
            }
            File f = fileFor(uuid);
            if (!f.exists()) {
                // nuovo player: nessun target sbloccato
                cache.put(uuid, new HashSet<>());
                return;
            }
            try (FileReader r = new FileReader(f)) {
                Data d = GSON.fromJson(r, Data.class);
                if (d == null || d.unlocked == null) {
                    cache.put(uuid, new HashSet<>());
                } else {
                    // normalizza lowercase
                    Set<String> s = new HashSet<>();
                    for (String k : d.unlocked) if (k != null) s.add(k.toLowerCase(Locale.ROOT));
                    cache.put(uuid, s);
                }
            }
        } catch (Exception e) {
            logError("Errore nel caricamento progressi per " + uuid, e);
            cache.put(uuid, new HashSet<>());
        }
    }

    private static void save(UUID uuid) {
        try {
            File dir = ROOT;
            if (!dir.exists() && !dir.mkdirs()) {
                logWarn("Impossibile creare cartella progressi: " + dir.getAbsolutePath());
            }
            File f = fileFor(uuid);
            Data d = new Data();
            d.schemaVersion = CURRENT_SCHEMA_VERSION;
            d.unlocked = new ArrayList<>(cache.getOrDefault(uuid, Collections.emptySet()));

            File temp = new File(f.getParentFile(), f.getName() + ".tmp");
            try (FileWriter w = new FileWriter(temp)) {
                GSON.toJson(d, w);
            }
            try {
                java.nio.file.Files.move(
                        temp.toPath(), f.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING,
                        java.nio.file.StandardCopyOption.ATOMIC_MOVE
                );
            } catch (IOException atomicNotSupported) {
                java.nio.file.Files.move(
                        temp.toPath(), f.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
            }
        } catch (Exception e) {
            logError("Errore nel salvataggio progressi per " + uuid, e);
        }
    }

    // ================== MODEL/LOG ==================

    private static class Data {
        Integer schemaVersion;
        List<String> unlocked = new ArrayList<>();
    }

    private static void logWarn(String msg) {
        System.out.println("[PlayerFlyProgress][WARN] " + msg);
    }

    private static void logError(String msg, Throwable t) {
        System.err.println("[PlayerFlyProgress][ERROR] " + msg);
        if (t != null) {
            StringWriter sw = new StringWriter();
            t.printStackTrace(new PrintWriter(sw));
            System.err.println(sw.toString());
        }
    }
}
