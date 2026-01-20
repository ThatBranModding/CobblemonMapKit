package com.cobblemon.khataly.mapkit.screen.custom;

import com.cobblemon.khataly.mapkit.CobblemonMapKitMod;
import com.cobblemon.khataly.mapkit.networking.packet.curiocase.EjectCurioC2SPacket;
import com.cobblemon.khataly.mapkit.networking.packet.curiocase.PolishCurioC2SPacket;
import com.mojang.blaze3d.systems.RenderSystem;
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
import net.minecraft.util.math.RotationAxis;
import org.lwjgl.opengl.GL11;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * CurioCaseScreen
 *  - Same layout as BadgeCaseScreen (2x4 = 8)
 *  - Separate background texture: textures/gui/curio_case.png
 *  - Uses Curio packets for polish/eject
 */
public class CurioCaseScreen extends Screen {
    private static final int GOLD_LIGHT = 0xFFF3DDA4;
    private static final int GOLD_BASE  = 0xFFE0C06A;
    private static final int GOLD_DARK  = 0xFF6A5220;
    private static final int CREAM_SOLID= 0xFFF6EEDC;
    private static final int SHADOW_30  = 0x4D000000;
    private static final int SHADOW_50  = 0x80000000;

    private static final Identifier BG_TEX =
            Identifier.of(CobblemonMapKitMod.MOD_ID, "textures/gui/curio_case.png");

    private List<ItemStack> curios;
    private List<Integer>   shines; // 0..100
    private int total;

    private final int panelW = 256;
    private final int panelH = 160;
    private int left, top;

    private final int rows = 2, cols = 4;
    private final float itemScale = 1.8f;
    private final int slotSize = 40;

    private static final double POLISH_PIXELS_PER_SHINE   = 110.0;
    private static final long   POLISH_PACKET_COOLDOWN_MS = 95L;
    private static final int    POLISH_PACKET_AMOUNT      = 1;

    private int polishingSlot = -1;
    private double lastX, lastY;
    private long lastPolishSoundMs = 0;

    private final List<Double> polishAccum = new ArrayList<>();
    private final List<Long>   lastPacketSentMs = new ArrayList<>();

    private final List<Float> polishHeat = new ArrayList<>();
    private final List<Long>  lastEmitMs = new ArrayList<>();

    private Identifier pendingAnimId = null;
    private CinematicInsert cinematic = null;
    private long screenShakeUntil = 0L;
    private final Random rnd = new Random();

    private final List<Spark> sparks = new ArrayList<>();

    @SuppressWarnings("unused")
    private final Hand handUsed;

    public CurioCaseScreen(Hand handUsed, List<ItemStack> curios, List<Integer> shines, int total) {
        super(Text.translatable("item." + CobblemonMapKitMod.MOD_ID + ".curio_case"));
        this.handUsed = handUsed;
        this.curios = new ArrayList<>(curios);
        this.shines = new ArrayList<>(shines);
        this.total  = Math.max(total, curios.size());
        ensurePerSlotArrays();
    }

    @Override public void tick() {
        var opts = MinecraftClient.getInstance().options;
        opts.attackKey.setPressed(false);
        opts.useKey.setPressed(false);
        super.tick();
    }

    public void queueInsertAnimation(Identifier id) { this.pendingAnimId = id; }

    @Override
    protected void init() {
        super.init();
        this.left = (this.width - panelW) / 2;
        this.top  = (this.height - panelH) / 2;

        if (pendingAnimId != null) {
            startCinematicInsert(pendingAnimId);
            pendingAnimId = null;
        }
    }

    @Override public boolean shouldPause() { return false; }

    private int gridStartX(){ return left + (panelW - cols * slotSize) / 2; }
    private int gridStartY(){ return top  + (panelH - rows * slotSize) / 2 + 8; }

    private int[] slotBounds(int idx) {
        int r = idx / cols, c = idx % cols;
        int x0 = gridStartX() + c * slotSize;
        int y0 = gridStartY() + r * slotSize;
        return new int[]{x0, y0, x0 + slotSize, y0 + slotSize};
    }

    private int[] itemDrawPos(int idx) {
        int[] b = slotBounds(idx);
        int itemPx = Math.round(16 * itemScale);
        int x = b[0] + (slotSize - itemPx) / 2;
        int y = b[1] + (slotSize - itemPx) / 2;
        return new int[]{x, y};
    }

    private int indexOfId(Identifier id){
        for (int i=0;i<curios.size();i++){
            if (!curios.get(i).isEmpty() && Registries.ITEM.getId(curios.get(i).getItem()).equals(id)) return i;
        }
        return -1;
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (cinematic != null) return true;

        int idx = hoveredSlotIndex((int)mouseX, (int)mouseY);

        if (button == 1 && hasShiftDown() && idx >= 0 && idx < curios.size() && !curios.get(idx).isEmpty()) {
            var id = Registries.ITEM.getId(curios.get(idx).getItem());
            ClientPlayNetworking.send(new EjectCurioC2SPacket(id));
            return true;
        }

        if (button == 0 && idx >= 0 && idx < curios.size() && !curios.get(idx).isEmpty()) {
            polishingSlot = idx;
            lastX = mouseX; lastY = mouseY;
            return true;
        }

        if (button == 0 && (mouseX < left || mouseX > left+panelW || mouseY < top || mouseY > top+panelH)) {
            this.close(); return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dx, double dy) {
        if (cinematic != null) return true;

        if (button == 0 && polishingSlot >= 0 && polishingSlot < curios.size()) {
            double dxl = mouseX - lastX, dyl = mouseY - lastY;
            double dist = Math.hypot(dxl, dyl);
            lastX = mouseX; lastY = mouseY;

            double acc = polishAccum.get(polishingSlot) + dist;
            long now = System.currentTimeMillis();
            long lastPkt = lastPacketSentMs.get(polishingSlot);

            while (acc >= POLISH_PIXELS_PER_SHINE) {
                if (now - lastPkt >= POLISH_PACKET_COOLDOWN_MS) {
                    acc -= POLISH_PIXELS_PER_SHINE;

                    var id = Registries.ITEM.getId(curios.get(polishingSlot).getItem());
                    ClientPlayNetworking.send(new PolishCurioC2SPacket(id, POLISH_PACKET_AMOUNT));
                    lastPacketSentMs.set(polishingSlot, now);

                    if (now - lastPolishSoundMs > 150 && MinecraftClient.getInstance().player != null) {
                        MinecraftClient.getInstance().player.playSound(SoundEvents.ITEM_BRUSH_BRUSHING_GENERIC, 1.5f, 1.1f);
                        lastPolishSoundMs = now;
                    }

                    int curr = polishingSlot < shines.size() ? shines.get(polishingSlot) : 0;
                    if (polishingSlot < shines.size()) shines.set(polishingSlot, Math.min(100, curr + POLISH_PACKET_AMOUNT));

                    float heat = Math.min(1f, polishHeat.get(polishingSlot) + 0.25f);
                    polishHeat.set(polishingSlot, heat);
                    spawnSlotSparks(polishingSlot, 3);
                } else {
                    break;
                }
                now = System.currentTimeMillis();
                lastPkt = lastPacketSentMs.get(polishingSlot);
            }

            float heat = Math.min(1f, polishHeat.get(polishingSlot) + (float)(dist / 120.0));
            polishHeat.set(polishingSlot, heat);

            polishAccum.set(polishingSlot, acc);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dx, dy);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button) {
        if (button == 0) polishingSlot = -1;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    private int hoveredSlotIndex(int mx, int my){
        for (int i = 0; i < rows * cols; i++) {
            int[] b = slotBounds(i);
            if (mx>=b[0] && mx<b[2] && my>=b[1] && my<b[3]) return i;
        }
        return -1;
    }

    public void applySync(List<ItemStack> newCurios, List<Integer> newShines, int total) {
        this.curios = new ArrayList<>(newCurios);
        this.shines = new ArrayList<>(newShines);
        this.total  = Math.max(total, newCurios.size());
        ensurePerSlotArrays();
    }

    private void startCinematicInsert(Identifier id) {
        int idx = indexOfId(id);
        if (idx < 0 || idx >= curios.size()) return;
        ItemStack st = curios.get(idx).copy();
        if (st.isEmpty()) return;

        int[] pos = itemDrawPos(idx);
        int targetX = pos[0];
        int targetY = pos[1];

        float giantScale = 3.4f;
        int giantPx = Math.round(16 * giantScale);
        int centerX = left + (panelW - giantPx) / 2;
        int centerY = top  + (panelH - giantPx) / 2 - 6;

        this.cinematic = new CinematicInsert(st, idx, centerX, centerY, giantScale, targetX, targetY);
        if (client != null && client.player != null) {
            client.player.playSound(SoundEvents.UI_TOAST_CHALLENGE_COMPLETE, 0.6f, 1f);
        }
    }

    @Override
    public void render(DrawContext ctx, int mouseX, int mouseY, float delta) {
        long now = System.currentTimeMillis();

        ctx.getMatrices().push();
        if (now < screenShakeUntil) {
            float sx = (rnd.nextFloat() - 0.5f) * 2f;
            float sy = (rnd.nextFloat() - 0.5f) * 2f;
            ctx.getMatrices().translate(sx, sy, 0);
        }

        if (MinecraftClient.getInstance().getResourceManager().getResource(BG_TEX).isPresent()) {
            ctx.drawTexture(BG_TEX, left, top, 0, 0, panelW, panelH, panelW, panelH);
        } else {
            ctx.fill(left, top, left+panelW, top+panelH, 0xCC1E1E1E);
            ctx.fill(left+3, top+3, left+panelW-3, top+panelH-3, 0xFF2A2A2A);
        }

        drawGoldTitle(ctx, Text.translatable("item." + CobblemonMapKitMod.MOD_ID + ".curio_case"));

        for (int i = 0; i < rows * cols; i++) {
            int[] b = slotBounds(i);
            ctx.fill(b[0], b[1], b[2], b[3], 0x22000000);
            ctx.fill(b[0]+1, b[1]+1, b[2]-1, b[3]-1, 0x22000000);
        }

        for (int i = 0; i < Math.min(curios.size(), rows*cols); i++) {
            float heat = polishHeat.get(i);
            float dt = (delta <= 0 ? 1f/60f : delta / 20f);
            heat = Math.max(0f, heat - dt * 0.8f);
            polishHeat.set(i, heat);

            if (heat > 0f) {
                long last = lastEmitMs.get(i);
                long interval = (long) (220 - 170 * heat);
                if (now - last >= interval) {
                    spawnSlotSparks(i, 1 + (heat > 0.5f ? 1 : 0));
                    lastEmitMs.set(i, now);
                }
            }
        }

        int itemPx = Math.round(16 * itemScale);
        for (int i=0;i<Math.min(curios.size(), rows*cols);i++){
            var st = curios.get(i);
            if (st.isEmpty()) continue;
            int[] p = itemDrawPos(i);

            if (cinematic != null && i == cinematic.slotIndex) {
                // cinematic draws it
            } else {
                if (polishHeat.get(i) > 0f) {
                    int cx = p[0] + itemPx/2;
                    int cy = p[1] + itemPx/2;
                    drawSoftDisc(ctx, cx, cy, (int)(itemPx*0.6f), 0x22000000);
                }
                drawItemScaled(ctx, st, p[0], p[1]);
                int shine = (i < shines.size() ? shines.get(i) : 0);
                if (shine > 0) drawStarsLeveled(ctx, p[0], p[1], itemPx, shine);
            }
        }

        if (cinematic != null) {
            if (!cinematic.render(ctx)) {
                int finishedSlot = cinematic.slotIndex;
                cinematic = null;
                spawnSlotSparks(finishedSlot, 10);
                screenShakeUntil = System.currentTimeMillis() + 180;
                if (client != null && client.player != null) {
                    client.player.playSound(SoundEvents.ITEM_BUNDLE_INSERT, 0.9f, 1.1f);
                }
            }
        }

        renderSparks(ctx);

        int hov = hoveredSlotIndex(mouseX, mouseY);
        if (hov >= 0 && hov < curios.size() && !curios.get(hov).isEmpty() && cinematic == null) {
            var stack = curios.get(hov);
            assert this.client != null;
            List<Text> lines = new ArrayList<>(getTooltipFromItem(this.client, stack));
            lines.add(Text.translatable("tooltip."+ CobblemonMapKitMod.MOD_ID+".curio_case.remove_hint"));
            lines.add(Text.translatable("tooltip."+ CobblemonMapKitMod.MOD_ID+".curio_case.polish_hint"));
            ctx.drawTooltip(this.textRenderer, lines, mouseX, mouseY);
        }

        ctx.getMatrices().pop();
        super.render(ctx, mouseX, mouseY, delta);
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {}

    private void drawItemScaled(DrawContext ctx, ItemStack st, int x, int y) {
        ctx.getMatrices().push();
        ctx.getMatrices().scale(itemScale, itemScale, 1f);
        int dx = Math.round(x / itemScale);
        int dy = Math.round(y / itemScale);
        ctx.drawItem(st, dx, dy);
        ctx.drawItemInSlot(this.textRenderer, st, dx, dy);
        ctx.getMatrices().pop();
    }

    private void drawGoldTitle(DrawContext ctx, Text title) {
        final int sideMargin = 40;
        final int barW = panelW - sideMargin * 2;
        final int barH = 22;
        final int barX = left + sideMargin;
        final int barY = top + 6;

        ctx.getMatrices().push();
        ctx.getMatrices().translate(0, 0, 200f);

        ctx.fill(barX + 2, barY + 3, barX + barW + 2, barY + barH + 3, SHADOW_30);
        ctx.fill(barX, barY, barX + barW, barY + barH, CREAM_SOLID);

        ctx.fill(barX, barY, barX + barW, barY + 1, GOLD_DARK);
        ctx.fill(barX, barY + barH - 1, barX + barW, barY + barH, GOLD_DARK);
        ctx.fill(barX, barY, barX + 1, barY + barH, GOLD_DARK);
        ctx.fill(barX + barW - 1, barY, barX + barW, barY + barH, GOLD_DARK);
        ctx.fill(barX + 1, barY + 1, barX + barW - 1, barY + 2, GOLD_LIGHT);
        ctx.fill(barX + 1, barY + barH - 2, barX + barW - 1, barY + barH - 1, GOLD_BASE);

        String s = title.getString();
        int tw = this.textRenderer.getWidth(s);
        int tx = barX + (barW - tw) / 2;
        int ty = barY + (barH - 8) / 2;

        ctx.drawText(this.textRenderer, s, tx + 1, ty + 1, SHADOW_50, false);
        ctx.drawText(this.textRenderer, s, tx - 1, ty, GOLD_DARK, false);
        ctx.drawText(this.textRenderer, s, tx + 1, ty, GOLD_DARK, false);
        ctx.drawText(this.textRenderer, s, tx, ty - 1, GOLD_DARK, false);
        ctx.drawText(this.textRenderer, s, tx, ty + 1, GOLD_DARK, false);
        ctx.drawText(this.textRenderer, s, tx, ty, GOLD_BASE, false);

        long t = System.currentTimeMillis() / 120;
        boolean blink = (t % 2) == 0;
        int decoY = ty - 1;
        int gap = 8;
        int leftStarX  = tx - gap - this.textRenderer.getWidth("✦");
        int rightStarX = tx + tw + gap;
        int starColor = blink ? GOLD_LIGHT : GOLD_BASE;
        ctx.drawText(this.textRenderer, "✦", leftStarX,  decoY, starColor, false);
        ctx.drawText(this.textRenderer, "✦", rightStarX, decoY, starColor, false);

        ctx.getMatrices().pop();
    }

    private void drawStarsLeveled(DrawContext ctx, int x, int y, int size, int shine) {
        final int level =
                (shine < 25) ? 0 :
                        (shine < 50) ? 1 :
                                (shine < 75) ? 2 :
                                        (shine < 90) ? 3 : 4;
        if (level == 0) return;

        int baseCount  = switch (level) { case 1 -> 2; case 2 -> 3; case 3 -> 4; default -> 6; };
        int extraFromShine = Math.max(0, (shine - 25) / 15);
        int stars = Math.min(10, baseCount + extraFromShine);

        int color = switch (level) {
            case 1 -> 0xFFEEF7FF;
            case 2 -> 0xFFFFFFFF;
            case 3 -> 0xFFFFE3A0;
            default -> 0xFFFFC84A;
        };

        int alpha = 200;
        int argb  = (alpha << 24) | (color & 0xFFFFFF);

        long tick = System.currentTimeMillis() / 100;
        for (int i = 0; i < stars; i++) {
            if (((tick + i) % 3) != 0) continue;
            int sx = x + 3 + rnd.nextInt(Math.max(1, size - 6));
            int sy = y + 3 + rnd.nextInt(Math.max(1, size - 6));
            int r = (level >= 4) ? 2 : (level >= 3 ? 2 : 1);
            drawStar5(ctx, sx, sy, r, argb);
        }
    }

    private enum ShineTier { T0, T1, T2, T3, T4 }

    private void spawnSlotSparks(int slotIndex, int count) {
        int[] p = itemDrawPos(slotIndex);
        int size = Math.round(16 * itemScale);

        int color = 0xFFFFFF;
        int alpha = 200;
        int baseSize = 2;

        if (sparks.size() > 800) {
            int toRemove = sparks.size() - 800 + (count * 2);
            if (toRemove > 0) sparks.subList(0, Math.min(toRemove, sparks.size())).clear();
        }

        for (int i = 0; i < count; i++) {
            float cx = p[0] + size/2f + (rnd.nextFloat()-0.5f) * (size * 0.35f);
            float cy = p[1] + size/2f + (rnd.nextFloat()-0.5f) * (size * 0.35f);
            int s = baseSize + (rnd.nextInt(2));
            sparks.add(Spark.create(cx, cy, color, alpha, s));
        }
    }

    private void renderSparks(DrawContext ctx) {
        long now = System.currentTimeMillis();

        RenderSystem.enableBlend();
        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);

        sparks.removeIf(s -> {
            float t = (now - s.t0) / (float) s.lifeMs;
            if (t >= 1f) return true;

            float fade = 1f - t;
            float pulse = 0.9f + 0.3f * (float)Math.sin((now - s.t0) * 0.02);

            float x = s.x + s.vx * t;
            float y = s.y + s.vy * t + 0.5f * s.g * t * t;

            int a = Math.min(255, Math.max(0, (int)(fade * s.alpha)));
            int argb = (a << 24) | (s.color & 0xFFFFFF);

            drawStar5(ctx, (int)x, (int)y, (int)(s.size * pulse), argb);
            return false;
        });

        RenderSystem.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.disableBlend();
    }

    private void drawStar5(DrawContext ctx, int cx, int cy, int size, int argb) {
        int r = Math.max(1, size);
        ctx.fill(cx - r, cy,     cx + r + 1, cy + 1, argb);
        ctx.fill(cx,     cy - r, cx + 1,     cy + r + 1, argb);
        for (int i = -r; i <= r; i++) {
            ctx.fill(cx + i, cy + i, cx + i + 1, cy + i + 1, argb);
            ctx.fill(cx + i, cy - i, cx + i + 1, cy - i + 1, argb);
        }
    }

    private void drawSoftDisc(DrawContext ctx, int cx, int cy, int radius, int argb) {
        int r2 = radius * radius;
        for (int y = -radius; y <= radius; y++) {
            int yy = y*y;
            for (int x = -radius; x <= radius; x++) {
                if (x*x + yy <= r2) ctx.fill(cx + x, cy + y, cx + x + 1, cy + y + 1, argb);
            }
        }
    }

    private static float easeOutCubic(float t){ return 1f - (float)Math.pow(1f - t, 3); }
    private static float easeInOutCubic(float t){
        return t < 0.5f ? 4f*t*t*t : 1f - (float)Math.pow(-2f*t + 2f, 3)/2f;
    }

    private static class Spark {
        final float x, y, vx, vy, g;
        final long t0, lifeMs;
        final int color;
        final int alpha;
        final int size;

        private Spark(float x, float y, float vx, float vy, float g, long lifeMs, int color, int alpha, int size) {
            this.x=x; this.y=y; this.vx=vx; this.vy=vy; this.g=g;
            this.lifeMs=lifeMs; this.t0=System.currentTimeMillis();
            this.color=color; this.alpha=alpha; this.size=size;
        }
        static Spark create(float x, float y, int color, int alpha, int size) {
            Random r = new Random();
            float ang = (float) (r.nextFloat() * Math.PI * 2f);
            float spd = 2.4f + r.nextFloat() * 3.1f;
            float upKick = 0.8f + r.nextFloat() * 0.6f;
            float vx = (float) Math.cos(ang) * spd;
            float vy = (float) Math.sin(ang) * spd - upKick;
            float g  = 0.85f;
            long life = 600 + r.nextInt(400);
            return new Spark(x, y, vx, vy, g, life, color, alpha, size);
        }
    }

    private class CinematicInsert {
        final ItemStack stack;
        final int slotIndex;
        final int targetX, targetY;
        final int centerX, centerY;
        final float bigScale;

        final long tStart = System.currentTimeMillis();
        final long dIn = 250;
        final long dSpin = 300;
        final long dOut = 320;

        CinematicInsert(ItemStack st, int slotIndex, int centerX, int centerY, float bigScale, int targetX, int targetY) {
            this.stack = st;
            this.slotIndex = slotIndex;
            this.centerX = centerX;
            this.centerY = centerY;
            this.bigScale = bigScale;
            this.targetX = targetX;
            this.targetY = targetY;
        }

        boolean render(DrawContext ctx) {
            long now = System.currentTimeMillis();
            long dt = now - tStart;

            if (dt <= dIn) {
                float t = easeOutCubic(dt / (float)dIn);
                float startX = left + panelW/2f - 8;
                float startY = top + panelH + 24;
                float x = lerp(startX, centerX, t);
                float y = lerp(startY, centerY, t);
                float s = lerp(itemScale, bigScale, t);
                float rot = (float)Math.toRadians(20 * t);
                drawItemAt(ctx, stack, x, y, s, rot);
                return true;

            } else if (dt <= dIn + dSpin) {
                float t = easeInOutCubic((dt - dIn) / (float)dSpin);
                float rot = (float)Math.toRadians(720 * t);
                drawItemAt(ctx, stack, centerX, centerY, bigScale, rot);
                return true;

            } else if (dt <= dIn + dSpin + dOut) {
                float t = easeOutCubic((dt - dIn - dSpin) / (float)dOut);
                float cx = (centerX + targetX) / 2f;
                float cy = Math.min(centerY, targetY) - 30;
                float[] p = quadBezier(centerX, centerY, cx, cy, targetX, targetY, t);
                float x = p[0], y = p[1];
                float s = lerp(bigScale, itemScale, t);
                float rot = (float)Math.toRadians(45 * (1f - t));
                drawItemAt(ctx, stack, x, y, s, rot);

                if (t > 0.95f && now - screenShakeUntil > 300) {
                    spawnSlotSparks(slotIndex, 6);
                    screenShakeUntil = now + 140;
                }
                return true;

            } else {
                return false;
            }
        }

        private float lerp(float a, float b, float t) { return a + (b - a) * t; }
        private float[] quadBezier(float x0, float y0, float cx, float cy, float x1, float y1, float t) {
            float u = 1f - t;
            float x = u*u*x0 + 2*u*t*cx + t*t*x1;
            float y = u*u*y0 + 2*u*t*cy + t*t*y1;
            return new float[]{x, y};
        }
    }

    private void drawItemAt(DrawContext ctx, ItemStack st, float x, float y, float scale, float rotZ) {
        ctx.getMatrices().push();
        ctx.getMatrices().translate(x, y, 300f);
        ctx.getMatrices().translate(8, 8, 0);
        ctx.getMatrices().multiply(RotationAxis.POSITIVE_Z.rotation(rotZ));
        ctx.getMatrices().scale(scale, scale, 1f);
        ctx.getMatrices().translate(-8, -8, 0);
        ctx.drawItem(st, 0, 0);
        ctx.drawItemInSlot(this.textRenderer, st, 0, 0);
        ctx.getMatrices().pop();
    }

    private void ensurePerSlotArrays() {
        int n = curios.size();
        while (polishAccum.size() < n) polishAccum.add(0.0);
        while (lastPacketSentMs.size() < n) lastPacketSentMs.add(0L);
        while (polishHeat.size() < n) polishHeat.add(0f);
        while (lastEmitMs.size() < n) lastEmitMs.add(0L);
        if (polishAccum.size() > n) polishAccum.subList(n, polishAccum.size()).clear();
        if (lastPacketSentMs.size() > n) lastPacketSentMs.subList(n, lastPacketSentMs.size()).clear();
        if (polishHeat.size() > n) polishHeat.subList(n, polishHeat.size()).clear();
        if (lastEmitMs.size() > n) lastEmitMs.subList(n, lastEmitMs.size()).clear();
    }
}
