package com.cobblemon.khataly.mapkit.item.custom;

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

import java.util.Locale;

public class WeatherWandItem extends Item {
    public WeatherWandItem(Settings settings) { super(settings); }

    private static final String NBT_MODE = "weather_mode"; // "snow" | "sandstorm" | "rain" | "clear" etc.

    // Cycle order (edit/add later)
    private static final String[] MODES = new String[] {
            "snow",
            "sandstorm"
            // add more later: "rain", "thunder", "clear"
    };

    /** Shift + right click on a block: cycles weather mode and shows feedback (action bar) */
    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        PlayerEntity player = ctx.getPlayer();
        if (player == null) return ActionResult.PASS;

        if (player.isSneaking()) {
            ItemStack stack = ctx.getStack();
            String next = cycleMode(stack);

            if (!ctx.getWorld().isClient()) {
                ((ServerPlayerEntity) player).sendMessage(
                        Text.literal("Weather Wand: " + pretty(next) + " mode"),
                        true // action bar
                );
            }
            return ActionResult.SUCCESS;
        }

        return ActionResult.PASS;
    }

    /** Right click (air): starts "using" so your client selection code can detect isUsingItem() */
    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        user.setCurrentHand(hand);
        return TypedActionResult.success(user.getStackInHand(hand));
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    // ===================== Helpers =====================

    public static String getMode(ItemStack stack) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (data == null) return MODES[0]; // default

        NbtCompound tag = data.copyNbt();
        String m = tag.getString(NBT_MODE);
        if (m == null || m.isBlank()) return MODES[0];

        return m.trim().toLowerCase(Locale.ROOT);
    }

    public static void setMode(ItemStack stack, String mode) {
        NbtComponent data = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound tag = (data != null) ? data.copyNbt() : new NbtCompound();
        tag.putString(NBT_MODE, mode);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(tag));
    }

    private static String cycleMode(ItemStack stack) {
        String cur = getMode(stack);

        int idx = 0;
        for (int i = 0; i < MODES.length; i++) {
            if (MODES[i].equalsIgnoreCase(cur)) {
                idx = i;
                break;
            }
        }

        int nextIdx = (idx + 1) % MODES.length;
        String next = MODES[nextIdx];
        setMode(stack, next);
        return next;
    }

    private static String pretty(String s) {
        if (s == null || s.isBlank()) return "Snow";
        String t = s.replace('_', ' ').trim();
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }
}
