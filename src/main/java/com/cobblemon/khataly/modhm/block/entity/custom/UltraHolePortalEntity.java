package com.cobblemon.khataly.modhm.block.entity.custom;

import com.cobblemon.khataly.modhm.block.entity.ModBlockEntities;
import com.cobblemon.khataly.modhm.config.ModConfig;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockEntityUpdateS2CPacket;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class UltraHolePortalEntity extends BlockEntity {

    private int lifetime;
    private String targetDimension;
    private double targetX, targetY, targetZ;
    private Runnable onRemove;

    public UltraHolePortalEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.ULTRAHOLE_ROCK_BE, pos, state);

        this.targetDimension = ModConfig.ULTRAHOLE_SETTINGS.destinationDimension;
        this.targetX = ModConfig.ULTRAHOLE_SETTINGS.x;
        this.targetY = ModConfig.ULTRAHOLE_SETTINGS.y;
        this.targetZ = ModConfig.ULTRAHOLE_SETTINGS.z;
        this.lifetime = ModConfig.ULTRAHOLE_SETTINGS.durationTicks;
    }

    public void setTarget(String dimension, double x, double y, double z) {
        this.targetDimension = dimension;
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        markDirty();
    }

    public void setOnRemove(Runnable onRemove) {
        this.onRemove = onRemove;
    }

    @Nullable
    public ServerWorld getTargetWorld(MinecraftServer server) {
        if (server == null) return null;
        return server.getWorld(
                net.minecraft.registry.RegistryKey.of(
                        net.minecraft.registry.RegistryKeys.WORLD,
                        Identifier.tryParse(targetDimension)
                )
        );
    }

    public void removePortal() {
        if (world != null) {
            world.setBlockState(pos, Blocks.AIR.getDefaultState());
            if (onRemove != null) onRemove.run();
        }
    }

    public void tick() {
        if (world == null || world.isClient) return;

        // Teletrasporto
        Box box = new Box(pos).expand(0.2, 0.2, 0.2);
        List<ServerPlayerEntity> playersInside = world.getEntitiesByClass(ServerPlayerEntity.class, box, p -> true);

        for (ServerPlayerEntity player : playersInside) {
            ServerWorld targetWorld = getTargetWorld(player.getServer());
            if (targetWorld == null) continue;

            double x = targetX;
            double y = targetY;
            double z = targetZ;

            if (targetWorld.getRegistryKey().getValue().toString().equals("minecraft:overworld")) {
                BlockPos spawnPos = player.getSpawnPointPosition();
                if (spawnPos == null) spawnPos = targetWorld.getSpawnPos();
                x = spawnPos.getX() + 0.5;
                y = spawnPos.getY();
                z = spawnPos.getZ() + 0.5;
            }

            player.teleport(targetWorld, x, y, z, player.getYaw(), player.getPitch());
            removePortal();
        }

        // Riduci lifetime
        lifetime--;
        if (lifetime <= 0) removePortal();
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        nbt.putString("targetDimension", targetDimension);
        nbt.putDouble("targetX", targetX);
        nbt.putDouble("targetY", targetY);
        nbt.putDouble("targetZ", targetZ);
        nbt.putInt("lifetime", lifetime);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        targetDimension = nbt.getString("targetDimension");
        targetX = nbt.getDouble("targetX");
        targetY = nbt.getDouble("targetY");
        targetZ = nbt.getDouble("targetZ");
        lifetime = nbt.getInt("lifetime");
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return BlockEntityUpdateS2CPacket.create(this);
    }

    @Override
    public NbtCompound toInitialChunkDataNbt(RegistryWrapper.WrapperLookup registryLookup) {
        return createNbt(registryLookup);
    }
}
