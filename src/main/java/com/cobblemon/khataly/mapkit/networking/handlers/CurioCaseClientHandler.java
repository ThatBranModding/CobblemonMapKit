package com.cobblemon.khataly.mapkit.networking.handlers;

import com.cobblemon.khataly.mapkit.networking.packet.curiocase.OpenCurioCaseS2CPacket;
import com.cobblemon.khataly.mapkit.networking.packet.curiocase.SyncCurioCaseS2CPacket;
import com.cobblemon.khataly.mapkit.screen.custom.CurioCaseScreen;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;

import java.util.ArrayList;
import java.util.List;

public final class CurioCaseClientHandler {
    private CurioCaseClientHandler() {}

    public static void register() {
        ClientPlayNetworking.registerGlobalReceiver(OpenCurioCaseS2CPacket.ID, (payload, ctx) -> {
            ctx.client().execute(() -> {
                var mc = MinecraftClient.getInstance();
                if (mc.player == null) return;

                int totalSlots = payload.totalSlots();

                List<ItemStack> stacks = new ArrayList<>(totalSlots);
                List<Integer> shines = new ArrayList<>(totalSlots);

                payload.curios().forEach(e -> {
                    Item it = Registries.ITEM.get(e.id());
                    stacks.add(new ItemStack(it));
                    shines.add(e.shine());
                });

                while (stacks.size() < totalSlots) stacks.add(ItemStack.EMPTY);
                while (shines.size() < totalSlots) shines.add(0);

                var screen = new CurioCaseScreen(payload.handUsed(), stacks, shines, totalSlots);

                payload.animInsertedId().ifPresent(screen::queueInsertAnimation);

                mc.setScreen(screen);
            });
        });

        ClientPlayNetworking.registerGlobalReceiver(SyncCurioCaseS2CPacket.ID, (payload, ctx) -> {
            ctx.client().execute(() -> {
                if (MinecraftClient.getInstance().currentScreen instanceof CurioCaseScreen screen) {
                    int totalSlots = payload.totalSlots();

                    List<ItemStack> stacks = new ArrayList<>(totalSlots);
                    List<Integer> shines = new ArrayList<>(totalSlots);

                    payload.curios().forEach(e -> {
                        Item it = Registries.ITEM.get(e.id());
                        stacks.add(new ItemStack(it));
                        shines.add(e.shine());
                    });

                    while (stacks.size() < totalSlots) stacks.add(ItemStack.EMPTY);
                    while (shines.size() < totalSlots) shines.add(0);

                    screen.applySync(stacks, shines, totalSlots);
                }
            });
        });
    }
}
