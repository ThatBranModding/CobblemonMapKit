package com.cobblemon.khataly.modhm.networking.handlers;

import com.cobblemon.khataly.modhm.item.custom.BadgeCaseItem;
import com.cobblemon.khataly.modhm.networking.packet.badgebox.*;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class BadgeBoxHandler {
    private BadgeBoxHandler() {}

    public static void register() {
        // lucidatura
        ServerPlayNetworking.registerGlobalReceiver(PolishBadgeC2SPacket.ID, (payload, ctx) ->
                ctx.server().execute(() ->
                        handlePolish(ctx.player(), ((PolishBadgeC2SPacket) payload).badgeId(), ((PolishBadgeC2SPacket) payload).amount()))
        );
        // rimozione
        ServerPlayNetworking.registerGlobalReceiver(EjectBadgeC2SPacket.ID, (payload, ctx) ->
                ctx.server().execute(() ->
                        handleEject(ctx.player(), ((EjectBadgeC2SPacket) payload).badgeId()))
        );
    }

    /* ===== SENDER ===== */

    public static void sendOpen(ServerPlayerEntity sp, Hand hand, List<BadgeCaseItem.BadgeData> data, int total, Optional<Identifier> animInserted) {
        var entries = toEntries(data);
        ServerPlayNetworking.send(sp, new OpenBadgeBoxS2CPacket(hand, total, entries, animInserted));
    }

    public static void sendSync(ServerPlayerEntity sp, List<BadgeCaseItem.BadgeData> data, int total) {
        var entries = toEntries(data);
        ServerPlayNetworking.send(sp, new SyncBadgeBoxS2CPacket(total, entries));
    }

    private static List<BadgeEntry> toEntries(List<BadgeCaseItem.BadgeData> data) {
        var list = new ArrayList<BadgeEntry>(data.size());
        for (var d : data) list.add(new BadgeEntry(d.id(), d.shine()));
        return list;
    }

    /* ===== LOGICA ===== */

    private static void handlePolish(ServerPlayerEntity sp, Identifier badgeId, int amount) {
        ItemStack caseStack = findCaseStack(sp);
        if (caseStack.isEmpty() || !(caseStack.getItem() instanceof BadgeCaseItem)) return;

        boolean changed = BadgeCaseItem.polish(caseStack, badgeId, Math.max(1, Math.min(10, amount)));
        if (!changed) return;

        sp.playSound(SoundEvents.ITEM_BRUSH_BRUSHING_GENERIC, 0.6f, 1.15f);
        var data  = BadgeCaseItem.readBadgesDataAndDecay(caseStack);
        int total = totalCount();
        sendSync(sp, data, total);
    }

    private static void handleEject(ServerPlayerEntity sp, Identifier badgeId) {
        ItemStack caseStack = findCaseStack(sp);
        if (caseStack.isEmpty() || !(caseStack.getItem() instanceof BadgeCaseItem)) return;

        boolean removed = BadgeCaseItem.remove(caseStack, badgeId);
        if (!removed) return;

        Item it = Registries.ITEM.get(badgeId);
        sp.getInventory().insertStack(new ItemStack(it));
        sp.playSound(SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 1f, 1f);

        var data  = BadgeCaseItem.readBadgesDataAndDecay(caseStack);
        int total = totalCount();
        sendSync(sp, data, total);
    }

    private static int totalCount() {
        return Registries.ITEM.getEntryList(BadgeCaseItem.BADGE_TAG).map(RegistryEntryList::size).orElse(8);
    }

    private static ItemStack findCaseStack(ServerPlayerEntity sp) {
        var main = sp.getMainHandStack();
        if (main.getItem() instanceof BadgeCaseItem) return main;
        var off = sp.getOffHandStack();
        if (off.getItem() instanceof BadgeCaseItem) return off;
        for (int i = 0; i < sp.getInventory().size(); i++) {
            var s = sp.getInventory().getStack(i);
            if (!s.isEmpty() && s.getItem() instanceof BadgeCaseItem) return s;
        }
        return ItemStack.EMPTY;
    }
}
