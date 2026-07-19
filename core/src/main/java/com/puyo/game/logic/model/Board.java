package com.puyo.game.logic.model;

/**
 * 게임 보드의 상태와 기본 조작을 담당하는 클래스입니다.
 */
public class Board {
    public static final int WIDTH = 6;
    public static final int HEIGHT = 12;

    private final Puyo[][] grid;

    public Board() {
        this.grid = new Puyo[WIDTH][HEIGHT];
    }

    public int getWidth() {
        return WIDTH;
    }

    public int getHeight() {
        return HEIGHT;
    }

    /**
     * 특정 위치에 뿌요가 있는지 확인합니다.
     */
    public boolean isOccupied(int x, int y) {
        if (x < 0 || x >= WIDTH || y < 0 || y >= HEIGHT) {
            return true; // 경계 밖은 충돌로 간주
        }
        return grid[x][y]!= null;
    }

    /**
     * 특정 위치의 뿌요를 가져옵니다.
     */
    public Puyo getPuyo(int x, int y) {
        if (isOccupied(x, y)) {
            return grid[x][y];
        }
        return null;
    }

    /**
     * 특정 위치에 뿌요를 배치합니다.
     */
    public void setPuyo(int x, int y, Puyo puyo) {
        if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) {
            grid[x][y] = puyo;
        }
    }

    /**
     * 특정 위치의 뿌요를 제거합니다.
     */
    public void clearPuyo(int x, int y) {
        if (x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT) {
            grid[x][y] = null;
        }
    }

    /**
     * 보드 전체를 반환합니다.
     */
    public Puyo[][] getGrid() {
        return grid;
    }

    public void clearBoard() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                grid[x][y] = null;
            }
        }
    }
}