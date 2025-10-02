package com.cobblemon.khataly.modhm.screen.custom;

import com.cobblemon.khataly.modhm.HMMod;
import com.cobblemon.khataly.modhm.networking.packet.badgebox.EjectBadgeC2SPacket;
import com.cobblemon.khataly.modhm.networking.packet.badgebox.PolishBadgeC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class BadgeCaseScreen extends Screen {

    private static final Identifier BG_TEX = Identifier.of(HMMod.MOD_ID, "textures/gui/badge_case.png");

    private List<ItemStack> badges;
    private List<Integer>   shines; // 0..100
    private int total;

    private final int panelW = 256;
    private final int panelH = 160;
    private int left, top;

    private final int rows = 2, cols = 4;

    // badge più grandi
    private final float itemScale = 1.6f;
    private final int   slotSize  = 40; // cornice sobria, badge centrati

    private final List<Flying> anims = new ArrayList<>();
    private Identifier pendingAnimId = null;

    public BadgeCaseScreen(Hand handUsed, List<ItemStack> badges, List<Integer> shines, int total) {
        super(Text.translatable("item." + HMMod.MOD_ID + ".badge_case"));
        this.badges = new ArrayList<>(badges);
        this.shines = new ArrayList<>(shines);
        this.total  = Math.max(total, badges.size());
    }

    /** blocca attack/use per evitare “traballo” del braccio */
    @Override public void tick() {
        var opts = MinecraftClient.getInstance().options;
        opts.attackKey.setPressed(false);
        opts.useKey.setPressed(false);
        super.tick();
    }

    /** chiamalo subito dopo setScreen */
    public void queueInsertAnimation(Identifier badgeId) { this.pendingAnimId = badgeId; }

    @Override protected void init() {
        super.init();
        this.left = (this.width - panelW) / 2;
        this.top  = (this.height - panelH) / 2;

        if (pendingAnimId != null) {
            startOpenInsertAnimation(pendingAnimId);
            pendingAnimId = null;
        }
    }

    @Override public boolean shouldPause() { return false; }

    /* ====== animazioni ====== */
    private static float easeOutCubic(float t){ return 1f - (float)Math.pow(1f - t, 3); }
    private class Flying {
        final ItemStack stack; final float sx, sy, ex, ey; final long startMs; final long durMs;
        Flying(ItemStack st, float sx, float sy, float ex, float ey, long dur){
            this.stack=st; this.sx=sx; this.sy=sy; this.ex=ex; this.ey=ey; this.durMs=dur; this.startMs=System.currentTimeMillis();
        }
        boolean render(DrawContext ctx){
            long now = System.currentTimeMillis();
            float t = Math.min(1f, (now - startMs) / (float)durMs);
            float e = easeOutCubic(t);
            float x = sx + (ex - sx) * e;
            float y = sy + (ey - sy) * e;

            ctx.getMatrices().push();
            ctx.getMatrices().translate(0, 0, 200);
            ctx.getMatrices().scale(itemScale, itemScale, 1f);
            int dx = Math.round(x / itemScale);
            int dy = Math.round(y / itemScale);
            ctx.drawItem(stack, dx, dy);
            ctx.drawItemInSlot(textRenderer, stack, dx, dy);
            ctx.getMatrices().pop();

            return t >= 1f;
        }
    }

    public void startOpenInsertAnimation(Identifier badgeId) {
        if (this.width == 0 || this.height == 0) { this.pendingAnimId = badgeId; return; }
        int idx = indexOfBadgeId(badgeId);
        if (idx < 0) return;
        int[] pos = itemDrawPos(idx);
        float ex = pos[0], ey = pos[1];
        float sx = left + panelW / 2f - 8, sy = top + panelH + 20; // dal basso verso lo slot
        var it = Registries.ITEM.get(badgeId);
        anims.add(new Flying(new ItemStack(it), sx, sy, ex, ey, 450));
    }

    public void applySync(List<ItemStack> newBadges, List<Integer> newShines, int total) {
        for (int i = 0; i < Math.min(newBadges.size(), rows*cols); i++) {
            var st = newBadges.get(i);
            if (i >= badges.size() || badges.get(i).isEmpty() || badges.get(i).getItem() != st.getItem()) {
                int[] pos = itemDrawPos(i);
                float ex = pos[0], ey = pos[1];
                float sx = left + panelW / 2f - 8, sy = top + panelH + 20;
                anims.add(new Flying(st.copy(), sx, sy, ex, ey, 350));
            }
        }
        this.badges = new ArrayList<>(newBadges);
        this.shines = new ArrayList<>(newShines);
        this.total  = Math.max(total, newBadges.size());
    }

    /* ====== layout ====== */
    private int gridStartX(){ return left + (panelW - cols * slotSize) / 2; }
    private int gridStartY(){ return top  + (panelH - rows * slotSize) / 2 + 8; }

    private int[] slotBounds(int idx) {
        int r = idx / cols, c = idx % cols;
        int x0 = gridStartX() + c * slotSize;
        int y0 = gridStartY() + r * slotSize;
        return new int[]{x0, y0, x0 + slotSize, y0 + slotSize};
    }

    /** posizione centrata dell’item scalato */
    private int[] itemDrawPos(int idx) {
        int[] b = slotBounds(idx);
        int itemPx = Math.round(16 * itemScale);
        int x = b[0] + (slotSize - itemPx) / 2;
        int y = b[1] + (slotSize - itemPx) / 2;
        return new int[]{x, y};
    }

    private int indexOfBadgeId(Identifier id){
        for (int i=0;i<badges.size();i++){
            if (Registries.ITEM.getId(badges.get(i).getItem()).equals(id)) return i;
        }
        return -1;
    }

    private int hoveredSlotIndex(int mx, int my){
        for (int i=0;i<Math.min(total, rows*cols);i++){
            int[] b = slotBounds(i);
            if (mx>=b[0] && mx<b[2] && my>=b[1] && my<b[3]) return i;
        }
        return -1;
    }

    /* ====== input: lucidatura + rimozione ====== */

    private int polishingSlot = -1;
    private double lastX, lastY, strokeAccum = 0;
    private long lastPolishSoundMs = 0;

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int idx = hoveredSlotIndex((int)mouseX, (int)mouseY);

        // Shift + Right Click => eject
        if (button == 1 && hasShiftDown() && idx >= 0 && idx < badges.size() && !badges.get(idx).isEmpty()) {
            var id = Registries.ITEM.getId(badges.get(idx).getItem());
            ClientPlayNetworking.send(new EjectBadgeC2SPacket(id));
            return true;
        }

        // Left click => inizia lucidatura
        if (button == 0 && idx >= 0 && idx < badges.size() && !badges.get(idx).isEmpty()) {
            polishingSlot = idx;
            lastX = mouseX; lastY = mouseY; strokeAccum = 0;
            return true;
        }

        // Left click fuori => chiudi
        if (button == 0 && (mouseX < left || mouseX > left+panelW || mouseY < top || mouseY > top+panelH)) {
            this.close(); return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (button == 0 && polishingSlot >= 0 && polishingSlot < badges.size()) {
            double dxl = mouseX - lastX, dyl = mouseY - lastY;
            strokeAccum += Math.hypot(dxl, dyl);
            lastX = mouseX; lastY = mouseY;

            if (strokeAccum >= 20) {
                strokeAccum = 0;
                var id = Registries.ITEM.getId(badges.get(polishingSlot).getItem());
                ClientPlayNetworking.send(new PolishBadgeC2SPacket(id, 5));

                // suono (rate-limited)
                long now = System.currentTimeMillis();
                if (now - lastPolishSoundMs > 110 && MinecraftClient.getInstance().player != null) {
                    MinecraftClient.getInstance().player.playSound(SoundEvents.ITEM_BRUSH_BRUSHING_GENERIC, 0.35f, 1.15f);
                    lastPolishSoundMs = now;
                }

                // feedback visual locale
                int curr = polishingSlot < shines.size() ? shines.get(polishingSlot) : 0;
                if (polishingSlot < shines.size()) shines.set(polishingSlot, Math.min(100, curr + 5));
            }
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) polishingSlot = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    /* ====== render ====== */

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        // background pannello
        if (MinecraftClient.getInstance().getResourceManager().getResource(BG_TEX).isPresent()) {
            ctx.drawTexture(BG_TEX, left, top, 0, 0, panelW, panelH, panelW, panelH);
        } else {
            ctx.fill(left, top, left+panelW, top+panelH, 0xCC1E1E1E);
            ctx.fill(left+3, top+3, left+panelW-3, top+panelH-3, 0xFF2A2A2A);
        }

        // titolo
        ctx.drawCenteredTextWithShadow(this.textRenderer, Text.literal("GYM BADGES"),
                left + panelW/2, top + 6, 0xFFE8E0C8);

        // griglia 2×4 (cornice sobria)
        for (int i=0;i<Math.min(total, rows*cols);i++){
            int[] b = slotBounds(i);
            ctx.fill(b[0], b[1], b[2], b[3], 0x22000000);
            ctx.fill(b[0]+1, b[1]+1, b[2]-1, b[3]-1, 0x22000000);
        }

        // items scalati
        int itemPx = Math.round(16 * itemScale);
        for (int i=0;i<Math.min(badges.size(), rows*cols);i++){
            var st = badges.get(i);
            if (st.isEmpty()) continue;
            int[] p = itemDrawPos(i);

            ctx.getMatrices().push();
            ctx.getMatrices().scale(itemScale, itemScale, 1f);
            int dx = Math.round(p[0] / itemScale);
            int dy = Math.round(p[1] / itemScale);
            ctx.drawItem(st, dx, dy);
            ctx.drawItemInSlot(this.textRenderer, st, dx, dy);
            ctx.getMatrices().pop();

            int shine = (i < shines.size() ? shines.get(i) : 0);
            if (shine > 0) drawStars(ctx, p[0], p[1], itemPx, shine);
        }

        // animazioni sopra
        anims.removeIf(flying -> flying.render(ctx));

        // tooltip
        int hov = hoveredSlotIndex(mouseX, mouseY);
        if (hov >= 0 && hov < badges.size() && !badges.get(hov).isEmpty()) {
            var stack = badges.get(hov);
            assert this.client != null;
            List<Text> lines = new ArrayList<>(getTooltipFromItem(this.client, stack));
            lines.add(Text.translatable("tooltip."+HMMod.MOD_ID+".badge_case.remove_hint"));
            lines.add(Text.translatable("tooltip."+HMMod.MOD_ID+".badge_case.polish_hint"));
            ctx.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        super.render(ctx, mouseX, mouseY, delta);
    }

    /** solo stelline (niente overlay bianco) */
    private void drawStars(DrawContext ctx, int x, int y, int size, int shine) {
        // densità in base allo shine
        int stars = Math.max(1, shine / 20); // 1..5
        long tick = System.currentTimeMillis() / 120;
        for (int i = 0; i < stars; i++) {
            if ((tick + i) % 3 != 0) continue;
            int sx = x + 3 + (int)(Math.random() * Math.max(1, (size - 6)));
            int sy = y + 3 + (int)(Math.random() * Math.max(1, (size - 6)));
            int c = (220 << 24) | 0xFFFFFF;
            ctx.fill(sx, sy, sx+1, sy+1, c);
            ctx.fill(sx+2, sy, sx+3, sy+1, c);
            ctx.fill(sx+1, sy-1, sx+2, sy+2, c);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {

    }
}
