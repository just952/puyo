package com.puyo.game.logic.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class Board {
    public static final int WIDTH = 6;
    public static final int HEIGHT = 12;
    public static final int VISIBLE_HEIGHT = HEIGHT; // Usually 12, but some games have hidden rows

    private Puyo[][] grid;

    public Board() {
        grid = new Puyo[WIDTH][HEIGHT];
        clear();
    }

    public void clear() {
        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                grid[x][y] = null;
            }
        }
    }

    public boolean isInside(int x, int y) {
        return x >= 0 && x < WIDTH && y >= 0 && y < HEIGHT;
    }

    public Puyo getPuyoAt(int x, int y) {
        if (!isInside(x, y)) {
            return null;
        }
        return grid[x][y];
    }

    public void setPuyoAt(int x, int y, Puyo puyo) {
        if (isInside(x, y)) {
            grid[x][y] = puyo;
        }
    }

    public boolean isEmpty(int x, int y) {
        return isInside(x, y) && getPuyoAt(x, y) == null;
    }

    public void placePuyo(Puyo puyo) {
        if (isInside(puyo.getX(), puyo.getY())) {
            grid[puyo.getX()][puyo.getY()] = puyo;
        }
    }

    public void removePuyo(Puyo puyo) {
        if (puyo != null && isInside(puyo.getX(), puyo.getY())) {
            grid[puyo.getX()][puyo.getY()] = null;
        }
    }

    public boolean canMoveLeft(PuyoPair pair) {
        for (Puyo p : pair.getPuyos()) {
            if (!isEmpty(p.getX() - 1, p.getY())) {
                return false;
            }
        }
        return true;
    }

    public boolean canMoveRight(PuyoPair pair) {
        for (Puyo p : pair.getPuyos()) {
            if (!isEmpty(p.getX() + 1, p.getY())) {
                return false;
            }
        }
        return true;
    }

    public boolean canMoveDown(PuyoPair pair) {
        for (Puyo p : pair.getPuyos()) {
            if (!isEmpty(p.getX(), p.getY() - 1)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Checks if the given pair can be placed at its current position (i.e., both
     * puyos are within bounds and the cells are empty).
     * 
     * @param pair the pair to check
     * @return true if the position is valid
     */
    public boolean canPlace(PuyoPair pair) {
        for (Puyo p : pair.getPuyos()) {
            if (!isEmpty(p.getX(), p.getY())) {
                return false;
            }
        }
        return true;
    }

    public void applyGravity() {
        // For each column, let puyos fall down to fill empty spaces below.
        for (int x = 0; x < WIDTH; x++) {
            int writeIdx = 0;
            for (int y = 0; y < HEIGHT; y++) {
                Puyo puyo = getPuyoAt(x, y);
                if (puyo != null) {
                    if (writeIdx != y) {
                        // move puyo down to writeIdx
                        puyo.setY(writeIdx);
                        grid[x][writeIdx] = puyo;
                        grid[x][y] = null;
                    }
                    writeIdx++;
                }
            }
            // clear the rest
            for (int y = writeIdx; y < HEIGHT; y++) {
                grid[x][y] = null;
            }
        }
    }

    // Find all connected puyos of the same color (excluding ojama and hard)
    public List<Puyo> findConnectedPuyos(Puyo start) {
        if (start == null || start.getColor() == PuyoColor.OJAMA || start.getColor() == PuyoColor.HARD) {
            return Collections.emptyList();
        }
        Set<Puyo> visited = new HashSet<>();
        List<Puyo> result = new ArrayList<>();
        dfs(start, start.getColor(), visited, result);
        return result;
    }

    private void dfs(Puyo puyo, PuyoColor color, Set<Puyo> visited, List<Puyo> result) {
        if (!visited.add(puyo)) {
            return;
        }
        result.add(puyo);
        int x = puyo.getX();
        int y = puyo.getY();
        // Check four directions
        int[] dx = { 1, -1, 0, 0 };
        int[] dy = { 0, 0, 1, -1 };
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];
            Puyo neighbor = getPuyoAt(nx, ny);
            if (neighbor != null && neighbor.getColor() == color && neighbor.isAlive() && !visited.contains(neighbor)) {
                dfs(neighbor, color, visited, result);
            }
        }
    }

    // Find all groups of 4 or more connected puyos of the same color
    public List<List<Puyo>> findAllMatchingGroups() {
        boolean[][] visited = new boolean[WIDTH][HEIGHT];
        List<List<Puyo>> groups = new ArrayList<>();

        for (int x = 0; x < WIDTH; x++) {
            for (int y = 0; y < HEIGHT; y++) {
                Puyo puyo = getPuyoAt(x, y);
                if (puyo != null && !visited[x][y] &&
                        puyo.getColor() != PuyoColor.OJAMA &&
                        puyo.getColor() != PuyoColor.HARD) {
                    List<Puyo> group = new ArrayList<>();
                    collectGroup(x, y, puyo.getColor(), visited, group);
                    if (group.size() >= 4) {
                        groups.add(group);
                    }
                }
            }
        }
        return groups;
    }

    private void collectGroup(int x, int y, PuyoColor color, boolean[][] visited, List<Puyo> group) {
        if (!isInside(x, y) || visited[x][y]) {
            return;
        }
        Puyo puyo = getPuyoAt(x, y);
        if (puyo == null || !puyo.isAlive() || puyo.getColor() != color) {
            return;
        }
        visited[x][y] = true;
        group.add(puyo);
        // Check neighbors
        collectGroup(x + 1, y, color, visited, group);
        collectGroup(x - 1, y, color, visited, group);
        collectGroup(x, y + 1, color, visited, group);
        collectGroup(x, y - 1, color, visited, group);
    }

    public void removePuyos(List<Puyo> puyos) {
        for (Puyo p : puyos) {
            if (p != null) {
                p.kill();
                removePuyo(p);
            }
        }
    }

    public int getHeightAtColumn(int x) {
        if (x < 0 || x >= WIDTH) {
            return 0;
        }
        for (int y = HEIGHT - 1; y >= 0; y--) {
            if (getPuyoAt(x, y) != null) {
                return y + 1; // height is the number of occupied cells from bottom
            }
        }
        return 0;
    }

    public boolean isTopOut() {
        for (int x = 0; x < WIDTH; x++) {
            if (getPuyoAt(x, HEIGHT - 1) != null) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (int y = HEIGHT - 1; y >= 0; y--) {
            sb.append("|");
            for (int x = 0; x < WIDTH; x++) {
                Puyo p = getPuyoAt(x, y);
                if (p == null) {
                    sb.append(" ");
                } else {
                    switch (p.getColor()) {
                        case RED:
                            sb.append("R");
                            break;
                        case GREEN:
                            sb.append("G");
                            break;
                        case BLUE:
                            sb.append("B");
                            break;
                        case YELLOW:
                            sb.append("Y");
                            break;
                        case PURPLE:
                            sb.append("P");
                            break;
                        case CYAN:
                            sb.append("C");
                            break;
                        case OJAMA:
                            sb.append("O");
                            break;
                        case HARD:
                            sb.append("H");
                            break;
                    }
                }
            }
            sb.append("|\n");
        }
        sb.append("+").append("-".repeat(WIDTH)).append("+\n");
        return sb.toString();
    }
}
