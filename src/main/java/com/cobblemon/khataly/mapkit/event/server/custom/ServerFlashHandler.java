package com.cobblemon.khataly.mapkit.event.server.custom;

import com.cobblemon.khataly.mapkit.networking.packet.flash.FlashMenuS2CPacket;
import com.cobblemon.khataly.mapkit.util.PlayerUtils;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;

public class ServerFlashHandler {
    public static void register() {
        CobblemonEvents.POKEMON_SENT_PRE.subscribe(Priority.NORMAL, event -> {


            // Invia il pacchetto al client
            ServerPlayerEntity player = event.getPokemon().getOwnerPlayer();
            if (player == null) return null; // NPC

            boolean canFlash = PlayerUtils.pokemonHasMoveToGUI(player,event.getPokemon().getUuid(), "flash");
            System.out.println("hasmoveguiFlash: " + canFlash);
            ServerPlayNetworking.send(player, FlashMenuS2CPacket.fromServerData(event.getPokemon().getUuid(), canFlash));

            return null;
        });
    }
}
