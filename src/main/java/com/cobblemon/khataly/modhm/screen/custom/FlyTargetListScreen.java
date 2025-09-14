package com.cobblemon.khataly.modhm.screen.custom;

import com.cobblemon.khataly.modhm.manager.FlyTargetManager;
import com.cobblemon.khataly.modhm.networking.packet.FlyPacketC2S;
import com.cobblemon.khataly.modhm.widget.SimpleButton;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public class FlyTargetListScreen extends Screen {
    public FlyTargetListScreen(MutableText title) {
        super(title);
    }
    @Override
    protected void init() {
        int y = 40;
        Map<String, FlyTargetManager.TargetInfo> targets = FlyTargetManager.getAllTargets();

        for (Map.Entry<String, FlyTargetManager.TargetInfo> entry : targets.entrySet()) {
            String name = entry.getKey();
            BlockPos pos = entry.getValue().pos;

            this.addDrawableChild(new SimpleButton(
                    this.width / 2 - 100,
                    y,
                    200,
                    20,
                    Text.literal(name + " @ " + pos.getX() + ", " + pos.getY() + ", " + pos.getZ()),
                    button -> {
                        ClientPlayNetworking.send(new FlyPacketC2S(pos));
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
