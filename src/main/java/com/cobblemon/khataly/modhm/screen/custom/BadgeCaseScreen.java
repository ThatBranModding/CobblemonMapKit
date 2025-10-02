package com.cobblemon.khataly.modhm.screen.custom;

import com.cobblemon.khataly.modhm.HMMod;
import com.cobblemon.khataly.modhm.item.custom.BadgeCaseItem;
import com.cobblemon.khataly.modhm.networking.packet.badgebox.EjectBadgeC2SPacket;
import com.cobblemon.khataly.modhm.networking.packet.badgebox.InsertBadgeC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class BadgeCaseScreen extends Screen {

    private final Hand handUsed;
    private List<ItemStack> badges;
    private int total;

    private final int panelW = 176;
    private final int panelH = 120;
    private int left, top;
    private final int rows = 2, cols = 4, slot = 18;

    public BadgeCaseScreen(Hand handUsed, List<ItemStack> badges, int total) {
        super(Text.translatable("item."+ HMMod.MOD_ID +".badge_case"));
        this.handUsed = handUsed;
        this.badges = new ArrayList<>(badges);
        this.total = Math.max(total, badges.size());
    }

    public void applySync(List<ItemStack> newBadges, int total) {
        this.badges = new ArrayList<>(newBadges);
        this.total = Math.max(total, newBadges.size());
    }

    @Override
    protected void init() {
        super.init();
        this.left = (this.width - panelW) / 2;
        this.top  = (this.height - panelH) / 2;
    }

    @Override public boolean shouldPause() { return false; }

    private int[] slotPos(int idx) {
        int startX = left + (panelW - (cols * slot)) / 2;
        int startY = top + 28;
        int r = idx / cols, c = idx % cols;
        return new int[]{ startX + c*slot, startY + r*slot };
    }

    private int hoveredSlotIndex(int mouseX, int mouseY) {
        int startX = left + (panelW - (cols * slot)) / 2;
        int startY = top + 28;
        int idx = 0;
        for (int r=0; r<rows; r++) {
            for (int c=0; c<cols; c++) {
                int x = startX + c*slot; int y = startY + r*slot;
                if (mouseX>=x && mouseX<x+16 && mouseY>=y && mouseY<y+16) return idx;
                idx++;
                if (idx>=total) return -1;
            }
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (button == 1) { // right click
            int idx = hoveredSlotIndex((int)mouseX, (int)mouseY);
            boolean shift = hasShiftDown();

            // SHIFT + right click su uno slot pieno -> EJECT
            if (shift && idx >= 0 && idx < badges.size()) {
                Identifier id = Registries.ITEM.getId(badges.get(idx).getItem());
                ClientPlayNetworking.send(new EjectBadgeC2SPacket(id, handUsed));
                return true;
            }

            // right click con una medaglia in mano -> INSERT
            var mc = MinecraftClient.getInstance();
            if (mc.player != null) {
                ItemStack held = mc.player.getMainHandStack();
                if (held.isIn(BadgeCaseItem.BADGE_TAG)) {
                    Identifier id = Registries.ITEM.getId(held.getItem());
                    ClientPlayNetworking.send(new InsertBadgeC2SPacket(id, handUsed));
                    return true;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Nessuno sfondo trasparente
    }
    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        this.renderBackground(ctx, mouseX, mouseY, delta);
        ctx.fill(left, top, left+panelW, top+panelH, 0xAA000000);
        ctx.drawCenteredTextWithShadow(this.textRenderer,
                Text.translatable("tooltip."+HMMod.MOD_ID+".badge_case.count", badges.size(), total),
                left + panelW/2, top + 8, 0xFFFFFF);

        // griglia
        int idx=0;
        int startX = left + (panelW - (cols * slot)) / 2;
        int startY = top + 28;
        for (int r=0; r<rows; r++) {
            for (int c=0; c<cols; c++) {
                int x = startX + c*slot, y = startY + r*slot;
                ctx.fill(x, y, x+16, y+16, 0x33FFFFFF);
                if (idx < badges.size()) {
                    ItemStack st = badges.get(idx);
                    ctx.drawItem(st, x, y);
                    ctx.drawItemInSlot(this.textRenderer, st, x, y);
                }
                idx++;
                if (idx >= total) break;
            }
        }

        // tooltip su slot
        int hov = hoveredSlotIndex(mouseX, mouseY);
        if (hov >= 0 && hov < badges.size()) {
            var mc = this.client; // MinecraftClient
            var stack = badges.get(hov);

            // Tooltip base dell'item + nostra riga custom
            assert mc != null;
            java.util.List<net.minecraft.text.Text> lines = new java.util.ArrayList<>(
                    getTooltipFromItem(mc, stack)
            );
            lines.add(net.minecraft.text.Text.translatable("tooltip." + HMMod.MOD_ID + ".badge_case.remove_hint"));

            ctx.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }


        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public boolean keyPressed(int key, int scan, int mod) {
        if (key == 256) { // ESC
            this.close();
            return true;
        }
        return super.keyPressed(key, scan, mod);
    }
}
