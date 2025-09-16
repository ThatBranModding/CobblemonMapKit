package com.cobblemon.khataly.modhm.event.custom;

import com.cobblemon.khataly.modhm.HMMod;
import com.cobblemon.khataly.modhm.config.ModConfig;
import com.cobblemon.khataly.modhm.networking.packet.FlashMenuC2SPacket;
import com.cobblemon.khataly.modhm.networking.packet.FlashMenuS2CPacket;
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
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;

public class FlashMenuOption {

    // Flag globale lato client: indica se aggiungere Flash alla ruota
    private static boolean canAddFlashOption = false;

    public static void register() {
        registerFlashMenuResponse();
        registerPokemonSentEvent();
        registerGUIEvent();
        registerGUICloseListener();
    }

    /** Riceve il pacchetto dal server e aggiorna il flag globale */
    private static void registerFlashMenuResponse() {
        ClientPlayNetworking.registerGlobalReceiver(FlashMenuS2CPacket.ID, (payload, context) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> {
                canAddFlashOption = payload.canFlash(); // true se il Pokémon può usare Flash
                System.out.println("[FlashMenuOption] Flag aggiornato: canAddFlashOption = " + canAddFlashOption);
            });
        });
    }

    /** Evento quando il Pokémon esce dalla Pokéball */
    private static void registerPokemonSentEvent() {
        CancelableObservable<PokemonSentPreEvent> observable = CobblemonEvents.POKEMON_SENT_PRE;
        observable.subscribe(Priority.NORMAL, event -> {
            // Invia pacchetto al server per verificare se il Pokémon conosce Flash
            ClientPlayNetworking.send(new FlashMenuC2SPacket(event.getPokemon().getUuid()));
            System.out.println("[FlashMenuOption] Pacchetto FlashMenuC2SPacket inviato al server");
            return null;
        });
    }

    /** Evento della GUI di interazione: aggiunge Flash solo se il flag è true */
    private static void registerGUIEvent() {
        EventObservable<PokemonInteractionGUICreationEvent> observable = CobblemonEvents.POKEMON_INTERACTION_GUI_CREATION;
        observable.subscribe(Priority.NORMAL, event -> {
            if (!canAddFlashOption) return null;

            Identifier icon = Identifier.of(HMMod.MOD_ID, "textures/gui/flash/icon_flash.png");
            Identifier secondaryIcon = null;
            String tooltip = "Flash";

            kotlin.jvm.functions.Function0<Vector3f> colourFunc = () -> new Vector3f(1f, 1f, 1f);
            kotlin.jvm.functions.Function0<Unit> onPressFunc = () -> {
                MinecraftClient mc = MinecraftClient.getInstance();
                mc.execute(() -> {
                    if (mc.player != null) {

                        // 🔹 Controlla se Night Vision è già attivo
                        if (mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.NIGHT_VISION)) {
                            mc.player.sendMessage(Text.literal("❗ Flash is already active!"), false);
                            return; // esci senza applicare di nuovo l'effetto
                        }

                        int durationSeconds = ModConfig.FLASH_DURATION;
                        int durationTicks = durationSeconds * 20;

                        // Applica Night Vision
                        mc.player.addStatusEffect(new net.minecraft.entity.effect.StatusEffectInstance(
                                net.minecraft.entity.effect.StatusEffects.NIGHT_VISION,
                                durationTicks,
                                0,
                                false,
                                false
                        ));

                        System.out.println("[FlashMenuOption] Night Vision applicato per " + durationSeconds + " secondi!");

                        // Rimozione programmata usando un thread separato
                        new Thread(() -> {
                            try {
                                Thread.sleep(durationSeconds * 1000L);
                            } catch (InterruptedException ignored) {}
                            mc.execute(() -> {
                                if (mc.player != null && mc.player.hasStatusEffect(net.minecraft.entity.effect.StatusEffects.NIGHT_VISION)) {
                                    mc.player.removeStatusEffect(net.minecraft.entity.effect.StatusEffects.NIGHT_VISION);
                                    System.out.println("[FlashMenuOption] Night Vision terminato!");
                                }
                            });
                        }).start();
                    }
                });
                return Unit.INSTANCE;
            };


            InteractWheelOption option = new InteractWheelOption(icon, null, tooltip, colourFunc, onPressFunc);

            // default TOP_LEFT
            Orientation chosenOrientation = Orientation.TOP_LEFT;

            // Se TOP_LEFT occupato, cerca il primo libero
            Multimap<Orientation, InteractWheelOption> options = event.getOptions();
            if (options.containsKey(chosenOrientation) && !options.get(chosenOrientation).isEmpty()) {
                for (Orientation orientation : Orientation.values()) {
                    if (!options.containsKey(orientation) || options.get(orientation).isEmpty()) {
                        chosenOrientation = orientation;
                        break;
                    }
                }
            }

            // aggiungi l’opzione
            event.addOption(chosenOrientation, option);

            return null;
        });
    }

    /** Resetta il flag quando si chiude la GUI di interazione */
    private static void registerGUICloseListener() {
        MinecraftClient mc = MinecraftClient.getInstance();
        mc.execute(() -> {
            mc.setScreen(new Screen(Text.literal("")) {
                @Override
                public void removed() {
                    canAddFlashOption = false; // reset flag quando la GUI si chiude
                    super.removed();
                    System.out.println("[FlashMenuOption] Flag canAddFlashOption resettato");
                }
            });
        });
    }
}
