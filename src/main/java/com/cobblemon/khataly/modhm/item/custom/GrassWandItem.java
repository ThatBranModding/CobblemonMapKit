package com.cobblemon.khataly.modhm.item.custom;

import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

public class GrassWandItem extends Item {
    public GrassWandItem(Settings settings) { super(settings); }

    private static final String NBT_MODE = "grass_mode"; // "tall" | "short"

    /** Shift + click destro su un blocco: toggla modalità e mostra feedback */
    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        PlayerEntity player = ctx.getPlayer();
        if (player == null) return ActionResult.PASS;

        if (player.isSneaking()) {
            ItemStack stack = ctx.getStack();
            boolean tall = !isTallMode(stack);
            setTallMode(stack, tall);
            if (!ctx.getWorld().isClient()) {
                ((ServerPlayerEntity) player).sendMessage(
                        Text.literal(tall ? "Grass Wand: Tall grass mode" : "Grass Wand: Short grass mode"),
                        true // action bar
                );
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    /* ===================== Helpers modalità ===================== */

    public static boolean isTallMode(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return false; // default short
        NbtCompound tag = data.copyNbt();
        return "tall".equalsIgnoreCase(tag.getString(NBT_MODE));
    }

    public static void setTallMode(ItemStack stack, boolean tall) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound tag = (data != null) ? data.copyNbt() : new NbtCompound();
        tag.putString(NBT_MODE, tall ? "tall" : "short");
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
    }
}
