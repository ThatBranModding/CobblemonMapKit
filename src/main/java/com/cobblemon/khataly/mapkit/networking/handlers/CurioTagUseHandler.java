package com.cobblemon.khataly.mapkit.networking.handlers;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import com.cobblemon.khataly.mapkit.item.custom.CurioCaseItem;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;
import net.minecraft.util.TypedActionResult;

import java.util.Optional;

public final class CurioTagUseHandler {
    private CurioTagUseHandler() {}

    public static void register() {
        UseItemCallback.EVENT.register((player, world, hand) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (world.isClient || !(player instanceof ServerPlayerEntity sp)) return TypedActionResult.pass(stack);
            if (stack.isEmpty()) return TypedActionResult.pass(stack);

            // Not a curio? (tag check)
            if (!Registries.ITEM.getEntry(stack.getItem()).isIn(CurioCaseItem.CURIO_TAG))
                return TypedActionResult.pass(stack);

            // Find curio case in inventory/hands
            ItemStack caseStack = findCaseStack(sp);
            if (caseStack.isEmpty() || !(caseStack.getItem() instanceof CurioCaseItem)) {
                sp.sendMessage(Text.translatable("msg." + CobblemonMapKitMod.MOD_ID + ".curio_case.missing"), true);
                return TypedActionResult.pass(stack);
            }

            // Full?
            if (CurioCaseItem.isFull(caseStack)) {
                sp.sendMessage(Text.translatable("msg." + CobblemonMapKitMod.MOD_ID + ".curio_case.full"), true);
                return TypedActionResult.pass(stack);
            }

            Identifier id = Registries.ITEM.getId(stack.getItem());
            boolean added = CurioCaseItem.addCurio(caseStack, id);
            if (!added) return TypedActionResult.pass(stack);

            // Consume 1 and open GUI with insert animation
            stack.decrement(1);

            var data = CurioCaseItem.readCuriosDataAndDecay(caseStack);
            CurioCaseHandler.sendOpen(sp, hand, data, CurioCaseItem.getMaxSlots(), Optional.of(id));
            sp.playSound(SoundEvents.ITEM_BUNDLE_INSERT, 1f, 1f);

            return TypedActionResult.success(stack);
        });
    }

    private static ItemStack findCaseStack(ServerPlayerEntity sp) {
        var main = sp.getMainHandStack();
        if (main.getItem() instanceof CurioCaseItem) return main;

        var off = sp.getOffHandStack();
        if (off.getItem() instanceof CurioCaseItem) return off;

        for (int i = 0; i < sp.getInventory().size(); i++) {
            var s = sp.getInventory().getStack(i);
            if (!s.isEmpty() && s.getItem() instanceof CurioCaseItem) return s;
        }
        return ItemStack.EMPTY;
    }
}
