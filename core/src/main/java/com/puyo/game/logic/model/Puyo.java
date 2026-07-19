package com.puyo.game.logic.model;

/**
 * 게임 보드 상의 한 칸을 차지하는 개별 뿌요 클래스입니다.
 */
public class Puyo {
    private final PuyoColor color;

    public Puyo(PuyoColor color) {
        this.color = color;
    }

    public PuyoColor getColor() {
        return color;
    }
}
