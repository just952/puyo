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
            PuyoColor.YELLOW, PuyoColor.PURPLE
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

    /**
     * 생성된 쌍을 스폰 위치(상단 중앙)에 배치
     * fieldHeight는 Board.TOTAL_HEIGHT(14)를 전달받음
     * 스폰 위치: y = TOTAL_HEIGHT - 2 = 12 (히든 영역 상단)
     */
    public void positionAtSpawn(PuyoPair pair, int fieldWidth, int fieldHeight) {
        int startX = (fieldWidth / 2) - 1;
        int startY = fieldHeight - 2;  // 히든 영역 상단(y=12)에서 스폰
        pair.setPosition(startX, startY);
    }
}
