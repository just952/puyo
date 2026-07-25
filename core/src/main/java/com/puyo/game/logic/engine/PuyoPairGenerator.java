package com.puyo.game.logic.engine;

import java.util.Random;
import com.puyo.game.logic.model.PuyoColor;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoPair;

/**
 * Generates random pairs of puyos for the game.
 */
public class PuyoPairGenerator {
    private static final PuyoColor[] COLORS = {
        PuyoColor.RED, PuyoColor.GREEN, PuyoColor.BLUE,
        PuyoColor.YELLOW, PuyoColor.PURPLE, PuyoColor.CYAN
    };
    private final Random random;

    public PuyoPairGenerator() {
        this.random = new Random();
    }

    public PuyoPair generate() {
        PuyoColor c1 = COLORS[random.nextInt(COLORS.length)];
        PuyoColor c2 = COLORS[random.nextInt(COLORS.length)];
        Puyo p1 = new Puyo(c1, 0, 0); // position will be set by the game
        Puyo p2 = new Puyo(c2, 0, 0);
        return new PuyoPair(p1, p2);
    }

    public PuyoPair generatePairWithColors(PuyoColor c1, PuyoColor c2) {
        Puyo p1 = new Puyo(c1, 0, 0);
        Puyo p2 = new Puyo(c2, 0, 0);
        return new PuyoPair(p1, p2);
    }
}
