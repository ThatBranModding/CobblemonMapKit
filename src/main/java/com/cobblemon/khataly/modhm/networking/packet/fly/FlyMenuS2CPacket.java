package com.cobblemon.khataly.modhm.networking.packet.fly;

import com.cobblemon.khataly.modhm.HMMod;
import com.cobblemon.khataly.modhm.config.FlyTargetConfig;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public record FlyMenuS2CPacket(
        UUID pokemonId,
        boolean canFly,
        List<FlyTargetEntry> targets
) implements CustomPayload {

    public static final CustomPayload.Id<FlyMenuS2CPacket> ID =
            new CustomPayload.Id<>(Identifier.of(HMMod.MOD_ID, "show_fly_menu_s2c"));

    public record FlyTargetEntry(String name, String worldKey, BlockPos pos) {}

    // Codec manuale per UUID
    private static final PacketCodec<RegistryByteBuf, UUID> UUID_CODEC = new PacketCodec<RegistryByteBuf, UUID>() {
        @Override
        public UUID decode(RegistryByteBuf buf) {
            long most = buf.readLong();
            long least = buf.readLong();
            return new UUID(most, least);
        }

        @Override
        public void encode(RegistryByteBuf buf, UUID uuid) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    };

    private static final PacketCodec<RegistryByteBuf, FlyTargetEntry> ENTRY_CODEC = PacketCodec.tuple(
            PacketCodecs.STRING, FlyTargetEntry::name,
            PacketCodecs.STRING, FlyTargetEntry::worldKey,
            BlockPos.PACKET_CODEC, FlyTargetEntry::pos,
            FlyTargetEntry::new
    );

    public static final PacketCodec<RegistryByteBuf, FlyMenuS2CPacket> CODEC = PacketCodec.tuple(
            UUID_CODEC, FlyMenuS2CPacket::pokemonId,
            PacketCodecs.BOOL, FlyMenuS2CPacket::canFly,
            ENTRY_CODEC.collect(PacketCodecs.toList()), FlyMenuS2CPacket::targets,
            FlyMenuS2CPacket::new
    );

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static FlyMenuS2CPacket fromServerData(UUID pokemonId, boolean canFly, Map<String, FlyTargetConfig.TargetInfo> targets) {
        List<FlyTargetEntry> entries = new ArrayList<>();
        for (var entry : targets.entrySet()) {
            FlyTargetConfig.TargetInfo info = entry.getValue();
            entries.add(new FlyTargetEntry(
                    entry.getKey(),
                    info.worldKey.getValue().toString(),
                    info.pos
            ));
        }
        return new FlyMenuS2CPacket(pokemonId, canFly, entries);
    }
}
