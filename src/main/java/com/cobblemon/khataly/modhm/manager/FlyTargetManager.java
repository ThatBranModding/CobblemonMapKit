package com.cobblemon.khataly.modhm.manager;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class FlyTargetManager {

    private static final Map<String, TargetInfo> targets = new HashMap<>();
    private static final Gson GSON = new Gson();
    private static final Path FILE_PATH = Path.of("config/modhm/flytargets.json");

    public static class TargetInfo {
        public final RegistryKey<World> worldKey;
        public final BlockPos pos;

        public TargetInfo(RegistryKey<World> worldKey, BlockPos pos) {
            this.worldKey = worldKey;
            this.pos = pos;
        }
    }

    // Classe interna per serializzazione/deserializzazione JSON
    private static class FlyTargetData {
        public String name;
        public String worldKey;
        public int x, y, z;

        public FlyTargetData(String name, String worldKey, int x, int y, int z) {
            this.name = name;
            this.worldKey = worldKey;
            this.x = x;
            this.y = y;
            this.z = z;
        }
    }

    public static boolean addTarget(String name, RegistryKey<World> worldKey, BlockPos pos) {
        String key = name.toLowerCase();
        if (targets.containsKey(key)) {
            return false; // target già esistente
        }
        targets.put(key, new TargetInfo(worldKey, pos));
        saveToFile(); // salva subito dopo modifica
        return true;
    }

    public static TargetInfo getTarget(String name) {
        return targets.get(name.toLowerCase());
    }

    public static Map<String, TargetInfo> getAllTargets() {
        return Collections.unmodifiableMap(targets);
    }

    public static boolean removeTarget(String name) {
        String key = name.toLowerCase();
        if (!targets.containsKey(key)) return false;
        targets.remove(key);
        saveToFile(); // salva subito dopo modifica
        return true;
    }

    public static void loadFromFile() {
        if (!Files.exists(FILE_PATH)) {
            System.out.println("[FlyTargetManager] No flytargets.json file found, starting with empty list.");
            return;
        }

        try (Reader reader = Files.newBufferedReader(FILE_PATH)) {
            Type listType = new TypeToken<List<FlyTargetData>>() {}.getType();
            List<FlyTargetData> dataList = GSON.fromJson(reader, listType);
            targets.clear();
            for (FlyTargetData data : dataList) {
                RegistryKey<World> worldKey = RegistryKey.of(RegistryKeys.WORLD, Identifier.of(data.worldKey));
                BlockPos pos = new BlockPos(data.x, data.y, data.z);
                targets.put(data.name.toLowerCase(), new TargetInfo(worldKey, pos));
            }
            System.out.println("[FlyTargetManager] Loaded " + dataList.size() + " fly targets.");
        } catch (IOException e) {
            System.err.println("[FlyTargetManager] Error loading flytargets.json:");
            e.printStackTrace();
        }
    }

    public static void saveToFile() {
        List<FlyTargetData> dataList = new ArrayList<>();
        for (var entry : targets.entrySet()) {
            String name = entry.getKey();
            TargetInfo info = entry.getValue();
            dataList.add(new FlyTargetData(name, info.worldKey.getValue().toString(),
                    info.pos.getX(), info.pos.getY(), info.pos.getZ()));
        }

        try {
            Files.createDirectories(FILE_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(FILE_PATH)) {
                GSON.toJson(dataList, writer);
            }
            System.out.println("[FlyTargetManager] Saved " + dataList.size() + " fly targets.");
        } catch (IOException e) {
            System.err.println("[FlyTargetManager] Error saving flytargets.json:");
            e.printStackTrace();
        }
    }
}
