package com.cobblemon.khataly.modhm.event.custom;

import com.cobblemon.khataly.modhm.HMMod;
import com.cobblemon.khataly.modhm.networking.packet.AnimationHMPacketS2C;
import com.cobblemon.khataly.modhm.networking.packet.FlyMenuC2SPacket;
import com.cobblemon.khataly.modhm.networking.packet.FlyMenuS2CPacket;
import com.cobblemon.khataly.modhm.screen.custom.AnimationMoveScreen;
import com.cobblemon.khataly.modhm.screen.custom.FlyTargetListScreen;
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

public class FlyMenuOption {

    // Flag lato client: indica quali Pokémon possono usare Fly
    private static final Map<UUID, Boolean> flyFlags = new HashMap<>();
    private static List<FlyMenuS2CPacket.FlyTargetEntry> cachedTargets = new ArrayList<>();

    public static void register() {
        registerFlyMenuResponse();
        registerPokemonSentEvent();
    }

    /** Riceve il pacchetto dal server e aggiorna lo stato Fly */
    private static void registerFlyMenuResponse() {
        ClientPlayNetworking.registerGlobalReceiver(FlyMenuS2CPacket.ID, (payload, context) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> {
                flyFlags.put(payload.pokemonId(), payload.canFly());
                cachedTargets = payload.targets();
                System.out.println("[FlyMenuOption] Aggiornato FlyFlags: " + flyFlags +
                        ", targets ricevuti: " + cachedTargets.size());
            });

            // registra l'opzione Fly per questo Pokémon
            registerGUIEvent(payload.pokemonId());
        });
    }

    /** Evento quando il Pokémon esce dalla Pokéball */
    private static void registerPokemonSentEvent() {
        CancelableObservable<PokemonSentPreEvent> observable = CobblemonEvents.POKEMON_SENT_PRE;
        observable.subscribe(Priority.HIGHEST, event -> {
            ClientPlayNetworking.send(new FlyMenuC2SPacket(event.getPokemon().getUuid()));
            System.out.println("[FlyMenuOption] Pacchetto FlyMenuC2SPacket inviato al server - UUID " + event.getPokemon().getUuid());
            return null;
        });
    }

    private static void registerGUIEvent(UUID serverFlyUuid) {
        EventObservable<PokemonInteractionGUICreationEvent> observable = CobblemonEvents.POKEMON_INTERACTION_GUI_CREATION;

        observable.subscribe(Priority.LOWEST, event -> {
            if (!flyFlags.getOrDefault(serverFlyUuid, false)) return null;

            // Controlla se l'opzione Fly è già presente in questa apertura GUI
            boolean flyAdded = false;
            for (Map.Entry<Orientation, InteractWheelOption> entry : event.getOptions().entries()) {
                InteractWheelOption existingOption = entry.getValue();
                if (existingOption != null &&
                        existingOption.getTooltipText() != null &&
                        existingOption.getTooltipText().equals("Fly")) {
                    flyAdded = true;
                    break;
                }
            }

            if (flyAdded) {
                return null; // Fly già aggiunto, non duplicare
            }

            Identifier icon = Identifier.of(HMMod.MOD_ID, "textures/gui/fly/icon_fly.png");
            String tooltip = "Fly";

            kotlin.jvm.functions.Function0<Vector3f> colourFunc = () -> new Vector3f(1f, 1f, 1f);
            kotlin.jvm.functions.Function0<Unit> onPressFunc = () -> {
                MinecraftClient mc = MinecraftClient.getInstance();

                ClientPlayNetworking.registerGlobalReceiver(AnimationHMPacketS2C.ID, (payload, context) -> {
                    mc.execute(() -> mc.setScreen(new AnimationMoveScreen(Text.literal("AnimationMoveScreen"), payload.pokemon())));
                });

                mc.execute(() -> mc.setScreen(new FlyTargetListScreen(Text.literal("FLY Menu"), cachedTargets)));
                System.out.println("[FlyMenuOption] Opzione Fly premuta per UUID " + serverFlyUuid);
                return Unit.INSTANCE;
            };

            InteractWheelOption flyOption = new InteractWheelOption(icon, null, tooltip, colourFunc, onPressFunc);

            // Default TOP_LEFT
            Orientation chosenOrientation = Orientation.TOP_LEFT;

            // Se TOP_LEFT occupato, trova prima posizione libera
            Multimap<Orientation, InteractWheelOption> options = event.getOptions();
            if (options.containsKey(chosenOrientation) && !options.get(chosenOrientation).isEmpty()) {
                for (Orientation orientation : Orientation.values()) {
                    if (!options.containsKey(orientation) || options.get(orientation).isEmpty()) {
                        chosenOrientation = orientation;
                        break;
                    }
                }
            }

            // Aggiungi Fly una sola volta
            event.addOption(chosenOrientation, flyOption);

            return null;
        });
    }



}
