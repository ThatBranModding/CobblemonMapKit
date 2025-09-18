package com.cobblemon.khataly.modhm.screen.custom;

import com.cobblemon.khataly.modhm.networking.packet.FlyMenuS2CPacket;
import com.cobblemon.khataly.modhm.networking.packet.FlyPacketC2S;
import com.cobblemon.khataly.modhm.widget.SimpleButton;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;

import java.util.List;

public class FlyTargetListScreen extends Screen {

    private final List<FlyMenuS2CPacket.FlyTargetEntry> targets;

    public FlyTargetListScreen(MutableText title, List<FlyMenuS2CPacket.FlyTargetEntry> targets) {
        super(title);
        this.targets = targets;
    }

    @Override
    protected void init() {
        int y = 40;

        // Mostra tutti i target ricevuti dal server
        for (FlyMenuS2CPacket.FlyTargetEntry entry : targets) {
            this.addDrawableChild(new SimpleButton(
                    this.width / 2 - 100,
                    y,
                    200,
                    20,
                    Text.literal(entry.name() + " @ " + entry.pos().toShortString()),
                    button -> {
                        ClientPlayNetworking.send(new FlyPacketC2S(entry.pos()));
                        assert client != null;
                        client.setScreen(null);
                    }
            ));
            y += 25;
        }

        // Bottone chiudi
        this.addDrawableChild(new SimpleButton(
                this.width / 2 - 100,
                this.height - 40,
                200,
                20,
                Text.literal("Chiudi"),
                button -> MinecraftClient.getInstance().setScreen(null)
        ));
    }
}
