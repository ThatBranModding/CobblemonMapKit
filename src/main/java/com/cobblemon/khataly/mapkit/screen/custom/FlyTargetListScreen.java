package com.cobblemon.khataly.mapkit.screen.custom;

import com.cobblemon.khataly.mapkit.networking.packet.fly.FlyMenuS2CPacket;
import com.cobblemon.khataly.mapkit.networking.packet.fly.FlyPacketC2S;
import com.cobblemon.khataly.mapkit.widget.SimpleButton;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

import java.util.List;
import java.util.UUID;

public class FlyTargetListScreen extends Screen {

    private final UUID pokemonId;
    private final List<FlyMenuS2CPacket.FlyTargetEntry> targets;

    // --- Scroll state ---
    private int scrollOffset = 0;
    private int maxScroll = 0;

    // --- Layout ---
    private static final int FIRST_Y = 40;
    private static final int ROW_SPACING = 25;
    private static final int BUTTON_H = 20;
    private static final int SCROLL_STEP = 12;

    private final int listTop = 36;
    private int listBottom = 0;

    public FlyTargetListScreen(MutableText title, UUID pokemonId, List<FlyMenuS2CPacket.FlyTargetEntry> targets) {
        super(title);
        this.pokemonId = pokemonId;
        this.targets = targets;
    }

    @Override
    protected void init() {
        MinecraftClient client = MinecraftClient.getInstance();
        if (client.player == null) return;

        this.listBottom = this.height - 52;
        int viewportHeight = this.listBottom - this.listTop;

        int contentHeight = targets.size() * ROW_SPACING;
        this.maxScroll = Math.max(0, contentHeight - viewportHeight);
        if (this.scrollOffset > this.maxScroll) this.scrollOffset = this.maxScroll;
        if (this.scrollOffset < 0) this.scrollOffset = 0;

        int y = FIRST_Y - scrollOffset;

        for (FlyMenuS2CPacket.FlyTargetEntry entry : targets) {
            if (y + BUTTON_H < listTop) { y += ROW_SPACING; continue; }
            if (y > listBottom) break;

            String name = entry.name();
            BlockPos pos = entry.pos();

            this.addDrawableChild(new SimpleButton(
                    this.width / 2 - 100,
                    y,
                    200,
                    BUTTON_H,
                    Text.literal(name), // ✅ ONLY show the location name
                    button -> {
                        Identifier worldId = Identifier.tryParse(entry.worldKey());
                        if (worldId != null) {
                            ClientPlayNetworking.send(new FlyPacketC2S(pokemonId, worldId, pos));
                        }
                        MinecraftClient.getInstance().setScreen(null);
                    }
            ));

            y += ROW_SPACING;
        }

        this.addDrawableChild(new SimpleButton(
                this.width / 2 - 100,
                this.height - 40,
                200,
                20,
                Text.literal("Close"),
                button -> MinecraftClient.getInstance().setScreen(null)
        ));
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double horizontalAmount, double verticalAmount) {
        if (mouseY < this.listTop || mouseY > this.listBottom) {
            return super.mouseScrolled(mouseX, mouseY, horizontalAmount, verticalAmount);
        }

        if (this.maxScroll <= 0) return true;

        this.scrollOffset -= (int) (verticalAmount * SCROLL_STEP);

        if (this.scrollOffset < 0) this.scrollOffset = 0;
        if (this.scrollOffset > this.maxScroll) this.scrollOffset = this.maxScroll;

        MinecraftClient client = MinecraftClient.getInstance();
        this.clearChildren();
        this.init(client, this.width, this.height);

        return true;
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 12, 0xFFFFFF);
        super.render(context, mouseX, mouseY, delta);
    }
}
