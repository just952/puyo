package com.puyo.game.logic.model;

public class Puyo {
    private final PuyoColor color;
    private int x;
    private int y;
    private boolean alive = true;

    public Puyo(PuyoColor color, int x, int y) {
        this.color = color;
        this.x = x;
        this.y = y;
    }

    public PuyoColor getColor() {
        return color;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isAlive() {
        return alive;
    }

    public void kill() {
        alive = false;
    }

    public void revive() {
        alive = true;
    }

    /**
     * 단일 뿌요 아래로 이동 (분리 낙하용)
     */
    public void moveDown() {
        this.y--;
    }

    @Override
    public String toString() {
        return "Puyo{" +
                "color=" + color +
                ", x=" + x +
                ", y=" + y +
                ", alive=" + alive +
                '}';
    }
}
