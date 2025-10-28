package com.cobblemon.khataly.mapkit.block.entity.custom;

import com.cobblemon.khataly.mapkit.block.entity.ModBlockEntities;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public class TeleportBlockEntity extends BlockEntity {
    private String pairId;

    public TeleportBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TELEPORT_BLOCK_BE, pos, state);
    }

    public void setPairId(String id) {
        this.pairId = id;
        markDirty();
    }

    public String getPairId() {
        return pairId;
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);
        if (pairId != null) nbt.putString("PairID", pairId);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);
        if (nbt.contains("PairID")) pairId = nbt.getString("PairID");
    }
}
