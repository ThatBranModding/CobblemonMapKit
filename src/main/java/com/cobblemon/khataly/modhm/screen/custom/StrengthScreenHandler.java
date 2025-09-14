package com.cobblemon.khataly.modhm.screen.custom;

import com.cobblemon.khataly.modhm.screen.ModScreenHandlers;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.util.math.BlockPos;

public class StrengthScreenHandler extends ScreenHandler {
    private BlockPos pos;

    public StrengthScreenHandler(int syncId, PlayerInventory playerInventory, BlockPos pos) {
        this(syncId, playerInventory, playerInventory.player.getWorld().getBlockEntity(pos));
        this.pos = pos;
    }

    public StrengthScreenHandler(int syncId, PlayerInventory playerInventory, BlockEntity blockEntity) {
        super(ModScreenHandlers.STRENGHT_SCREEN_HANDLER, syncId);

    }

    public BlockPos getPos() {
        return pos;
    }
    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }
}
