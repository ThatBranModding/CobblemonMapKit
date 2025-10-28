package com.cobblemon.khataly.mapkit.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtOps;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.PersistentState;
import net.minecraft.world.World;
import net.minecraft.util.math.BlockPos;

import java.util.*;

public class TeleportPairRegistry extends PersistentState {

    public record TeleportLocation(RegistryKey<World> dimension, BlockPos pos) {
        public static final Codec<TeleportLocation> CODEC = RecordCodecBuilder.create(i -> i.group(
                World.CODEC.fieldOf("dimension").forGetter(TeleportLocation::dimension),
                BlockPos.CODEC.fieldOf("pos").forGetter(TeleportLocation::pos)
        ).apply(i, TeleportLocation::new));
    }

    public record Pair(TeleportLocation a, TeleportLocation b) {
        public static final Codec<Pair> CODEC = RecordCodecBuilder.create(i -> i.group(
                TeleportLocation.CODEC.fieldOf("a").forGetter(Pair::a),
                TeleportLocation.CODEC.optionalFieldOf("b").forGetter(p -> Optional.ofNullable(p.b()))
        ).apply(i, (a, bOpt) -> new Pair(a, bOpt.orElse(null))));
    }

    private final Map<String, Pair> pairs = new HashMap<>();

    public static final Codec<TeleportPairRegistry> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.unboundedMap(Codec.STRING, Pair.CODEC).fieldOf("pairs").forGetter(TeleportPairRegistry::getPairs)
    ).apply(i, TeleportPairRegistry::new));

    public static final Type<TeleportPairRegistry> TYPE =
            new Type<>(TeleportPairRegistry::new, TeleportPairRegistry::readNbt, null);

    public TeleportPairRegistry(Map<String, Pair> loaded) {
        this.pairs.putAll(loaded);
    }

    public TeleportPairRegistry() {}

    public Map<String, Pair> getPairs() {
        return pairs;
    }

    public String addTeleport(ServerWorld world, BlockPos pos) {
        TeleportLocation loc = new TeleportLocation(world.getRegistryKey(), pos);

        for (Map.Entry<String, Pair> e : pairs.entrySet()) {
            Pair p = e.getValue();
            if (p.b() == null) {
                pairs.put(e.getKey(), new Pair(p.a(), loc));
                markDirty();
                return e.getKey();
            }
        }

        String id = UUID.randomUUID().toString();
        pairs.put(id, new Pair(loc, null));
        markDirty();
        return id;
    }

    public TeleportLocation getOther(String id, BlockPos me, RegistryKey<World> currentDim) {
        Pair p = pairs.get(id);
        if (p == null || p.b() == null) return null;
        TeleportLocation a = p.a();
        TeleportLocation b = p.b();
        if (a.dimension().equals(currentDim) && a.pos().equals(me)) return b;
        if (b.dimension().equals(currentDim) && b.pos().equals(me)) return a;
        return null;
    }

    public int cleanup(ServerWorld world) {
        int size0 = pairs.size();
        pairs.entrySet().removeIf(e -> {
            Pair p = e.getValue();
            boolean aOk = world.getServer().getWorld(p.a().dimension()) != null &&
                    Objects.requireNonNull(world.getServer().getWorld(p.a().dimension())).getBlockEntity(p.a().pos()) != null;
            boolean bOk = p.b() == null || (world.getServer().getWorld(p.b().dimension()) != null &&
                    Objects.requireNonNull(world.getServer().getWorld(p.b().dimension())).getBlockEntity(p.b().pos()) != null);
            return !aOk || !bOk;
        });
        int removed = size0 - pairs.size();
        if (removed > 0) markDirty();
        return removed;
    }

    @Override
    public NbtCompound writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        Codec.unboundedMap(Codec.STRING, Pair.CODEC)
                .encodeStart(NbtOps.INSTANCE, pairs)
                .resultOrPartial(err -> System.err.println("TeleportPair save error: " + err))
                .ifPresent(enc -> nbt.put("pairs", enc));
        return nbt;
    }

    public static TeleportPairRegistry readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        if (!nbt.contains("pairs", NbtElement.COMPOUND_TYPE)) return new TeleportPairRegistry();
        var result = Codec.unboundedMap(Codec.STRING, Pair.CODEC).parse(NbtOps.INSTANCE, nbt.get("pairs"));
        Map<String, Pair> loaded = result.resultOrPartial(err -> System.err.println("TeleportPair load error: " + err))
                .orElse(new HashMap<>());
        return new TeleportPairRegistry(loaded);
    }

    public static TeleportPairRegistry get(ServerWorld world) {
        ServerWorld overworld = world.getServer().getOverworld();
        return overworld.getPersistentStateManager().getOrCreate(TYPE, "teleport_pairs");
    }
}
