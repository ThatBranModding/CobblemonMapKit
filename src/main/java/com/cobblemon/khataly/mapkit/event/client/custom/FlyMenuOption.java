package com.cobblemon.khataly.mapkit.event.client.custom;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import com.cobblemon.khataly.mapkit.networking.packet.fly.FlyMenuS2CPacket;
import com.cobblemon.khataly.mapkit.screen.custom.FlyTargetListScreen;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.cobblemon.mod.common.api.events.pokemon.interaction.PokemonInteractionGUICreationEvent;
import com.cobblemon.mod.common.api.reactive.EventObservable;
import com.cobblemon.mod.common.client.gui.interact.wheel.InteractWheelOption;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.joml.Vector3f;
import kotlin.Unit;

import java.util.List;
import java.util.UUID;

public class FlyMenuOption {

    private static boolean canAddFlyOption = false;
    private static List<FlyMenuS2CPacket.FlyTargetEntry> targets = List.of();
    private static UUID pokemonId = null;

    public static void register() {
        registerFlyMenuResponse();
        registerGUIEvent();
    }

    /** Receives the packet from server and updates cached state */
    private static void registerFlyMenuResponse() {
        ClientPlayNetworking.registerGlobalReceiver(FlyMenuS2CPacket.ID, (payload, context) -> {
            MinecraftClient mc = MinecraftClient.getInstance();
            mc.execute(() -> {
                canAddFlyOption = payload.canFly();
                targets = payload.targets();
                pokemonId = payload.pokemonId();
            });
        });
    }

    /** Adds Fly option to the interaction wheel ONLY when allowed */
    private static void registerGUIEvent() {
        EventObservable<PokemonInteractionGUICreationEvent> observable = CobblemonEvents.POKEMON_INTERACTION_GUI_CREATION;
        observable.subscribe(Priority.NORMAL, event -> {
            if (!canAddFlyOption) return null;

            Identifier icon = Identifier.of(CobblemonMapKitMod.MOD_ID, "textures/gui/fly/icon_fly.png");
            String tooltip = "Fly";

            kotlin.jvm.functions.Function0<Vector3f> colourFunc = () -> new Vector3f(1f, 1f, 1f);

            kotlin.jvm.functions.Function0<Unit> onPressFunc = () -> {
                MinecraftClient mc = MinecraftClient.getInstance();

                // IMPORTANT: FlyTargetListScreen now requires (title, pokemonId, targets)
                UUID pid = pokemonId;
                List<FlyMenuS2CPacket.FlyTargetEntry> list = targets;

                mc.execute(() -> {
                    if (pid != null && list != null) {
                        mc.setScreen(new FlyTargetListScreen(Text.literal("FLY Menu"), pid, list));
                    } else {
                        mc.setScreen(null);
                    }
                });

                return Unit.INSTANCE;
            };

            InteractWheelOption option = new InteractWheelOption(
                    icon,
                    null,
                    true,
                    tooltip,
                    colourFunc,
                    onPressFunc
            );

            event.addFillingOption(option);
            return Unit.INSTANCE;
        });
    }
}
