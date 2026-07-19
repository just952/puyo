package com.puyo.game.logic.model;

public enum PuyoColor {
    RED,
    BLUE,
    GREEN,
    YELLOW,
    PURPLE;

    public static PuyoColor getRandom() {
        return values()[(int) (Math.random() * values().length)];
    }
}