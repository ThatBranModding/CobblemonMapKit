package com.cobblemon.khataly.modhm.event.custom;

import com.cobblemon.khataly.modhm.HMMod;
import com.cobblemon.khataly.modhm.networking.packet.AnimationHMPacketS2C;
import com.cobblemon.khataly.modhm.networking.packet.TeleportMenuC2SPacket;
import com.cobblemon.khataly.modhm.networking.packet.TeleportMenuS2CPacket;
import com.cobblemon.khataly.modhm.networking.packet.TeleportPacketC2S;
import com.cobblemon.khataly.modhm.screen.custom.AnimationMoveScreen;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.PokemonSentPreEvent;
import com.cobblemon.mod.common.api.events.pokemon.interaction.PokemonInteractionGUICreationEvent;
import com.cobblemon.mod.common.api.reactive.CancelableObservable;
import com.cobblemon.mod.common.api.reactive.EventObservable;
import com.cobblemon.mod.common.client.gui.interact.wheel.InteractWheelOption;
import com.cobblemon.mod.common.client.gui.interact.wheel.Orientation;
import com.google.common.collect.Multimap;
import kotlin.Unit;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

import java.util.*;

public class TeleportMenuOption {

    // Flag lato client: indica quali Pokémon possono usare Teleport
    private static final Map<UUID, Boolean> tpFlags = new HashMap<>();

    public static void register() {
        registerTeleportMenuResponse();
        registerPokemonSentEvent();
    }

    /** Riceve il pacchetto dal server e aggiorna lo stato Teleport */
    private static void registerTeleportMenuResponse() {
        ClientPlayNetworking.registerGlobalReceiver(TeleportMenuS2CPacket.ID, (payload, context) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> {
                tpFlags.put(payload.pokemonId(), payload.canTeleport());
                System.out.println("[TeleportMenuOption] Aggiornato tpFlags: " + tpFlags);
            });

            // registra l'opzione Teleport solo per questo Pokémon
            registerGUIEvent(payload.pokemonId());
        });
    }

    /** Evento quando il Pokémon esce dalla Pokéball */
    private static void registerPokemonSentEvent() {
        CancelableObservable<PokemonSentPreEvent> observable = CobblemonEvents.POKEMON_SENT_PRE;
        observable.subscribe(Priority.HIGHEST, event -> {
            // Invia pacchetto al server per verificare se il Pokémon conosce Teleport
            ClientPlayNetworking.send(new TeleportMenuC2SPacket(event.getPokemon().getUuid()));
            System.out.println("[TeleportMenuOption] Pacchetto TeleportMenuC2SPacket inviato al server - UUID " + event.getPokemon().getUuid());
            return null;
        });
    }

    private static void registerGUIEvent(UUID serverTpUuid) {
        EventObservable<PokemonInteractionGUICreationEvent> observable = CobblemonEvents.POKEMON_INTERACTION_GUI_CREATION;

        observable.subscribe(Priority.LOWEST, event -> {
            if (!tpFlags.getOrDefault(serverTpUuid, false)) return null;

            // Controlla se Teleport è già presente in questa GUI
            boolean teleportAdded = false;
            for (Map.Entry<Orientation, InteractWheelOption> entry : event.getOptions().entries()) {
                InteractWheelOption existingOption = entry.getValue();
                if (existingOption != null &&
                        existingOption.getTooltipText() != null &&
                        existingOption.getTooltipText().equals("Teleport")) {
                    teleportAdded = true;
                    break;
                }
            }
            if (teleportAdded) return null;

            Identifier icon = Identifier.of(HMMod.MOD_ID, "textures/gui/teleport/icon_teleport.png");
            String tooltip = "Teleport";

            kotlin.jvm.functions.Function0<Vector3f> colourFunc = () -> new Vector3f(1f, 1f, 1f);
            kotlin.jvm.functions.Function0<Unit> onPressFunc = () -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                assert mc.player != null;

                ClientPlayNetworking.registerGlobalReceiver(AnimationHMPacketS2C.ID, (payload, context) -> {
                    mc.execute(() -> mc.setScreen(new AnimationMoveScreen(Text.literal("AnimationMoveScreen"), payload.pokemon())));
                });

                ClientPlayNetworking.send(new TeleportPacketC2S(mc.player.getBlockPos()));

                System.out.println("[TeleportMenuOption] Opzione Teleport premuta per UUID " + serverTpUuid);
                return Unit.INSTANCE;
            };

            InteractWheelOption tpOption = new InteractWheelOption(icon, null, tooltip, colourFunc, onPressFunc);

            // default TOP_LEFT
            Orientation chosenOrientation = Orientation.TOP_LEFT;

            // Se occupato, trova prima posizione libera
            Multimap<Orientation, InteractWheelOption> options = event.getOptions();
            if (options.containsKey(chosenOrientation) && !options.get(chosenOrientation).isEmpty()) {
                for (Orientation orientation : Orientation.values()) {
                    if (!options.containsKey(orientation) || options.get(orientation).isEmpty()) {
                        chosenOrientation = orientation;
                        break;
                    }
                }
            }

            // Aggiungi Teleport una sola volta
            event.addOption(chosenOrientation, tpOption);

            return null;
        });
    }
}
