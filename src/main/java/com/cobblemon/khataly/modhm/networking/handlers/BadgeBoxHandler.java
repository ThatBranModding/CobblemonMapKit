package com.cobblemon.khataly.modhm.networking.handlers;

import com.cobblemon.khataly.modhm.item.custom.BadgeCaseItem;
import com.cobblemon.khataly.modhm.networking.packet.badgebox.EjectBadgeC2SPacket;
import com.cobblemon.khataly.modhm.networking.packet.badgebox.InsertBadgeC2SPacket;
import com.cobblemon.khataly.modhm.networking.packet.badgebox.OpenBadgeBoxS2CPacket;
import com.cobblemon.khataly.modhm.networking.packet.badgebox.SyncBadgeBoxS2CPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntryList;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Set;

public final class BadgeBoxHandler {
    private BadgeBoxHandler() {}

    public static void register() {
        // C2S insert
        ServerPlayNetworking.registerGlobalReceiver(InsertBadgeC2SPacket.ID, (payload, ctx) -> {
            ctx.server().execute(() -> handleInsert(ctx.player(), ((InsertBadgeC2SPacket) payload).badgeId() /*, p.handUsed() ignorato */));
        });

        // C2S eject
        ServerPlayNetworking.registerGlobalReceiver(EjectBadgeC2SPacket.ID, (payload, ctx) -> {
            ctx.server().execute(() -> handleEject(ctx.player(), ((EjectBadgeC2SPacket) payload).badgeId() /*, p.handUsed() ignorato */));
        });
    }

    public static void sendOpen(ServerPlayerEntity sp, Hand hand, Set<Identifier> stored, int total) {
        ServerPlayNetworking.send(sp, new OpenBadgeBoxS2CPacket(hand, total, new ArrayList<>(stored)));
    }
    public static void sendSync(ServerPlayerEntity sp, Set<Identifier> stored, int total) {
        ServerPlayNetworking.send(sp, new SyncBadgeBoxS2CPacket(total, new ArrayList<>(stored)));
    }

    /* ===== Logica: CERCA IL CASE OVUNQUE ===== */

    private static void handleInsert(ServerPlayerEntity sp, Identifier badgeId) {
        ItemStack caseStack = findCaseStack(sp);
        if (caseStack.isEmpty() || !(caseStack.getItem() instanceof BadgeCaseItem)) return;

        Item badgeItem = Registries.ITEM.get(badgeId);
        if (!consumeOne(sp, badgeItem)) return;   // consuma 1 dall'inventario del player

        Set<Identifier> stored = new LinkedHashSet<>(BadgeCaseItem.readBadges(caseStack));
        if (stored.add(badgeId)) {
            BadgeCaseItem.writeBadges(caseStack, stored);
            sp.playSound(net.minecraft.sound.SoundEvents.ITEM_BUNDLE_INSERT, 1f, 1f);
            int total = totalCount();
            sendSync(sp, stored, total);
        }
    }

    private static void handleEject(ServerPlayerEntity sp, Identifier badgeId) {
        ItemStack caseStack = findCaseStack(sp);
        if (caseStack.isEmpty() || !(caseStack.getItem() instanceof BadgeCaseItem)) return;

        Set<Identifier> stored = new LinkedHashSet<>(BadgeCaseItem.readBadges(caseStack));
        if (stored.remove(badgeId)) {
            Item it = Registries.ITEM.get(badgeId);
            sp.getInventory().insertStack(new ItemStack(it));
            sp.playSound(net.minecraft.sound.SoundEvents.ITEM_BUNDLE_REMOVE_ONE, 1f, 1f);
            BadgeCaseItem.writeBadges(caseStack, stored);
            int total = totalCount();
            sendSync(sp, stored, total);
        }
    }

    private static int totalCount() {
        return Registries.ITEM.getEntryList(BadgeCaseItem.BADGE_TAG).map(RegistryEntryList::size).orElse(8);
    }

    private static ItemStack findCaseStack(ServerPlayerEntity sp) {
        // Main-hand
        ItemStack main = sp.getMainHandStack();
        if (main.getItem() instanceof BadgeCaseItem) return main;

        // Off-hand
        ItemStack off = sp.getOffHandStack();
        if (off.getItem() instanceof BadgeCaseItem) return off;

        // Inventario
        for (int i = 0; i < sp.getInventory().size(); i++) {
            ItemStack s = sp.getInventory().getStack(i);
            if (!s.isEmpty() && s.getItem() instanceof BadgeCaseItem) return s;
        }
        return ItemStack.EMPTY;
    }

    private static boolean consumeOne(ServerPlayerEntity sp, Item target) {
        if (target == null) return false;
        for (int i = 0; i < sp.getInventory().size(); i++) {
            ItemStack s = sp.getInventory().getStack(i);
            if (!s.isEmpty() && s.getItem() == target) { s.decrement(1); return true; }
        }
        return false;
    }
}

