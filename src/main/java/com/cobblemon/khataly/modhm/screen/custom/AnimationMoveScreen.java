package com.cobblemon.khataly.modhm.screen.custom;

import com.cobblemon.mod.common.client.gui.PokemonGuiUtilsKt;
import com.cobblemon.mod.common.client.render.models.blockbench.FloatingState;
import com.cobblemon.mod.common.entity.PoseType;
import com.cobblemon.mod.common.pokemon.RenderablePokemon;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

public class AnimationMoveScreen extends Screen {
    private final FloatingState floatingState = new FloatingState();
    private final RenderablePokemon pokemon;
    private final List<SpeedLine> lines = new ArrayList<>();
    private final Random random = new Random();
    private int timer = 0;

    private static class SpeedLine {
        int x, y;
        final int width = 50;
        final int height = 4;

        SpeedLine(int x, int y) {
            this.x = x;
            this.y = y;
        }

        void move() {
            this.x += 12;
        }

        boolean isOffScreen(int screenWidth) {
            return this.x > screenWidth;
        }
    }

    public AnimationMoveScreen(Text title, RenderablePokemon pokemon) {
        super(title);
        this.pokemon = pokemon;

        floatingState.setCurrentPose("stand");
        floatingState.setCurrentAspects(pokemon.getAspects());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        // Sfondo blu centrale
        int stripHeight = 120;
        int stripY = (this.height - stripHeight) / 2;
        context.fill(0, stripY, this.width, stripY + stripHeight, 0xFF3A80D9);

        // Disegna le linee di velocità
        for (SpeedLine line : lines) {
            context.fill(line.x, line.y, line.x + line.width, line.y + line.height, 0xFFFFFFFF);
        }

        // Centro della striscia blu
        int centerX = this.width / 2;
        int centerY = (this.height - stripHeight) / 2;

        // Scala Pokémon (resta 9.0f)
        float pokemonScale = 9.0f;

        context.getMatrices().push();

        // Porta il pivot al centro della banda blu
        context.getMatrices().translate(centerX, centerY, 0);

        // Compensa il pivot del modello (che è sui piedi) → lo alzo un po'
        context.getMatrices().translate(0, -stripHeight / 4f, 0);

        context.getMatrices().scale(pokemonScale, pokemonScale, pokemonScale);

        // Rotazione per rivolgerlo verso l’utente
        Quaternionf rotation = new Quaternionf()
                .rotateXYZ((float) Math.toRadians(15), (float) Math.toRadians(-30), 0);

        PokemonGuiUtilsKt.drawProfilePokemon(
                pokemon,
                context.getMatrices(),
                rotation,
                PoseType.STAND,
                floatingState,
                delta,
                9.0f,   // lasciato invariato
                true,
                true,
                1f, 1f, 1f, 1f
        );

        context.getMatrices().pop();

        super.render(context, mouseX, mouseY, delta);
    }

    @Override
    public void tick() {
        timer++;

        if (timer % 2 == 0) {
            int offset = random.nextInt(60) - 30;
            lines.add(new SpeedLine(-50, this.height / 2 + offset));
        }

        Iterator<SpeedLine> it = lines.iterator();
        while (it.hasNext()) {
            SpeedLine line = it.next();
            line.move();
            if (line.isOffScreen(this.width)) {
                it.remove();
            }
        }

        if (timer > 40 && this.client != null) {
            this.client.setScreen(null);
        }
    }

    @Override
    public void renderBackground(DrawContext context, int mouseX, int mouseY, float delta) {
        // Nessuno sfondo trasparente
    }

    @Override
    public boolean shouldCloseOnEsc() {
        return false;
    }
}
