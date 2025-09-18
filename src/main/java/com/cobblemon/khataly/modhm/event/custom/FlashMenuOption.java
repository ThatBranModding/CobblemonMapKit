package com.cobblemon.khataly.modhm.event.custom;

import com.cobblemon.khataly.modhm.HMMod;
import com.cobblemon.khataly.modhm.networking.packet.AnimationHMPacketS2C;
import com.cobblemon.khataly.modhm.networking.packet.FlashMenuC2SPacket;
import com.cobblemon.khataly.modhm.networking.packet.FlashMenuS2CPacket;
import com.cobblemon.khataly.modhm.networking.packet.FlashPacketC2S;
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

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class FlashMenuOption {

    // Flag lato client: indica quali Pokémon possono usare Flash
    private static final Map<UUID, Boolean> flashFlags = new HashMap<>();

    public static void register() {
        registerFlashMenuResponse();
        registerPokemonSentEvent();
    }

    /** Riceve il pacchetto dal server e aggiorna lo stato Flash */
    private static void registerFlashMenuResponse() {
        ClientPlayNetworking.registerGlobalReceiver(FlashMenuS2CPacket.ID, (payload, context) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> {
                flashFlags.put(payload.pokemonId(), payload.canFlash());
                System.out.println("[FlashMenuOption] Aggiornato flashFlags: " + flashFlags);
            });

            // registra l'opzione Flash solo per questo Pokémon
            registerGUIEvent(payload.pokemonId());
        });
    }

    /** Evento quando il Pokémon esce dalla Pokéball */
    private static void registerPokemonSentEvent() {
        CancelableObservable<PokemonSentPreEvent> observable = CobblemonEvents.POKEMON_SENT_PRE;
        observable.subscribe(Priority.HIGHEST, event -> {
            // Invia pacchetto al server per verificare se il Pokémon conosce Flash
            ClientPlayNetworking.send(new FlashMenuC2SPacket(event.getPokemon().getUuid()));
            System.out.println("[FlashMenuOption] Pacchetto FlashMenuC2SPacket inviato al server - UUID " + event.getPokemon().getUuid());
            return null;
        });
    }

    private static void registerGUIEvent(UUID serverFlashUuid) {
        EventObservable<PokemonInteractionGUICreationEvent> observable = CobblemonEvents.POKEMON_INTERACTION_GUI_CREATION;

        observable.subscribe(Priority.LOWEST, event -> {
            if (!flashFlags.getOrDefault(serverFlashUuid, false)) return null;

            // Controlla se Flash è già presente in questa GUI
            boolean flashAdded = false;
            for (Map.Entry<Orientation, InteractWheelOption> entry : event.getOptions().entries()) {
                InteractWheelOption existingOption = entry.getValue();
                if (existingOption != null &&
                        existingOption.getTooltipText() != null &&
                        existingOption.getTooltipText().equals("Flash")) {
                    flashAdded = true;
                    break;
                }
            }
            if (flashAdded) return null;

            Identifier icon = Identifier.of(HMMod.MOD_ID, "textures/gui/flash/icon_flash.png");
            String tooltip = "Flash";

            kotlin.jvm.functions.Function0<Vector3f> colourFunc = () -> new Vector3f(1f, 1f, 1f);
            kotlin.jvm.functions.Function0<Unit> onPressFunc = () -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                assert mc.player != null;

                ClientPlayNetworking.registerGlobalReceiver(AnimationHMPacketS2C.ID, (payload, context) -> {
                    mc.execute(() -> mc.setScreen(new AnimationMoveScreen(Text.literal("AnimationMoveScreen"), payload.pokemon())));
                });

                ClientPlayNetworking.send(new FlashPacketC2S(mc.player.getBlockPos()));

                System.out.println("[FlashMenuOption] Opzione Flash premuta per UUID " + serverFlashUuid);
                return Unit.INSTANCE;
            };

            InteractWheelOption flashOption = new InteractWheelOption(icon, null, tooltip, colourFunc, onPressFunc);

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

            // Aggiungi Flash una sola volta
            event.addOption(chosenOrientation, flashOption);

            return null;
        });
    }
}
