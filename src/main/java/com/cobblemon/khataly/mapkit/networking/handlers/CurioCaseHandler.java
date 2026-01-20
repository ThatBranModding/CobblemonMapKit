package com.cobblemon.khataly.mapkit.networking.handlers;

import com.cobblemon.khataly.mapkit.item.custom.CurioCaseItem;
import com.cobblemon.khataly.mapkit.networking.packet.curiocase.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class CurioCaseHandler {
    private CurioCaseHandler() {}

    public static void register() {
        ServerPlayNetworking.registerGlobalReceiver(PolishCurioC2SPacket.ID, (payload, ctx) ->
                ctx.server().execute(() -> handlePolish(ctx.player(), payload.curioId(), payload.amount()))
        );

        ServerPlayNetworking.registerGlobalReceiver(EjectCurioC2SPacket.ID, (payload, ctx) ->
                ctx.server().execute(() -> handleEject(ctx.player(), payload.curioId()))
        );
    }

    /* ===== SENDER ===== */

    public static void sendOpen(ServerPlayerEntity sp, Hand hand, List<CurioCaseItem.CurioData> data, int totalSlots, Optional<Identifier> animInserted) {
        int safeTotal = CurioCaseItem.getMaxSlots();
        List<CurioEntry> entries = toEntriesClamped(data, safeTotal);
        ServerPlayNetworking.send(sp, new OpenCurioCaseS2CPacket(hand, safeTotal, entries, animInserted));
    }

    public static void sendSync(ServerPlayerEntity sp, List<CurioCaseItem.CurioData> data, int totalSlots) {
        int safeTotal = CurioCaseItem.getMaxSlots();
        List<CurioEntry> entries = toEntriesClamped(data, safeTotal);
        ServerPlayNetworking.send(sp, new SyncCurioCaseS2CPacket(safeTotal, entries));
    }

    private static List<CurioEntry> toEntriesClamped(List<CurioCaseItem.CurioData> data, int max) {
        int n = Math.min(max, data.size());
        var list = new ArrayList<CurioEntry>(n);
        for (int i = 0; i < n; i++) {
            var d = data.get(i);
            if (d.id() == null) continue;
            list.add(new CurioEntry(d.id(), d.shine()));
        }
        return list;
    }

    /* ===== LOGIC ===== */

    private static void handlePolish(ServerPlayerEntity sp, Identifier curioId, int amount) {
        ItemStack caseStack = findCaseStack(sp);
        if (caseStack.isEmpty() || !(caseStack.getItem() instanceof CurioCaseItem)) return;

        boolean changed = CurioCaseItem.polish(caseStack, curioId, Math.max(1, Math.min(10, amount)));
        if (!changed) return;

        sp.playSound(SoundEvents.ITEM_BRUSH_BRUSHING_GENERIC, 0.6f, 1.15f);

        var data = CurioCaseItem.readCuriosDataAndDecay(caseStack);
        sendSync(sp, data, CurioCaseItem.getMaxSlots());
    }

    private static void handleEject(ServerPlayerEntity sp, Identifier curioId) {
        ItemStack caseStack = findCaseStack(sp);
        if (caseStack.isEmpty() || !(caseStack.getItem() instanceof CurioCaseItem)) return;

        boolean removed = CurioCaseItem.remove(caseStack, curioId);
        if (!removed) return;

        Item it = Registries.ITEM.get(curioId);
        sp.getInventory().insertStack(new ItemStack(it));
        sp.playSound(SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 1f, 1f);

        var data = CurioCaseItem.readCuriosDataAndDecay(caseStack);
        sendSync(sp, data, CurioCaseItem.getMaxSlots());
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
