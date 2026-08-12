package com.puyo.game.logic.model;

import com.puyo.game.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

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

    /**
     * 화면 안(y < HEIGHT)에 있는 뿌요만 충돌 체크.
     * 화면 밖(위쪽)에 있는 뿌요는 "고스트" 상태로 통과 가능 (원작 방식).
     */
    private boolean isInsideVisible(Puyo p) {
        return p.getY() < HEIGHT;
    }

    public boolean canMoveLeft(PuyoPair pair) {
        for (Puyo p : pair.getPuyos()) {
            if (isInsideVisible(p) && !isEmpty(p.getX() - 1, p.getY())) {
                return false;
            }
        }
        return true;
    }

    public boolean canMoveRight(PuyoPair pair) {
        for (Puyo p : pair.getPuyos()) {
            if (isInsideVisible(p) && !isEmpty(p.getX() + 1, p.getY())) {
                return false;
            }
        }
        return true;
    }

    public boolean canMoveDown(PuyoPair pair) {
        for (Puyo p : pair.getPuyos()) {
            if (isInsideVisible(p) && !isEmpty(p.getX(), p.getY() - 1)) {
                return false;
            }
        }
        return true;
    }

    /**
     * 단일 뿌요가 아래로 이동 가능한지 확인 (분리 낙하용).
     * 화면 안(y < HEIGHT)에 있는 뿌요만 체크.
     */
    public boolean canMoveDown(Puyo p) {
        return isInsideVisible(p) && isEmpty(p.getX(), p.getY() - 1);
    }

    /**
     * Checks if the given pair can be placed at its current position.
     * Only checks puyos that are inside the visible board (y < HEIGHT).
     *
     * @param pair the pair to check
     * @return true if the position is valid
     */
    public boolean canPlace(PuyoPair pair) {
        for (Puyo p : pair.getPuyos()) {
            if (isInsideVisible(p) && !isEmpty(p.getX(), p.getY())) {
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

    /**
     * 공중에 떠있는 모든 뿌요를 반환 (아래가 비어있거나 떠있는 뿌요가 있는 뿌요들)
     * 연쇄 후 낙하 애니메이션용 - 같은 열의 연속된 떠있는 뿌요들을 모두 포함
     * 아래(y=0)부터 위로 스캔하여 첫 번째 떠있는 뿌요를 찾고, 그 위쪽 모든 뿌요를 포함
     */
    public List<Puyo> getAllFloatingPuyos() {
        List<Puyo> floating = new ArrayList<>();
        LogUtil.debug("Board", "getAllFloatingPuyos START, board:\n" + this.toString());
        for (int x = 0; x < WIDTH; x++) {
            // 각 열에서 가장 높은 접지된(grounded) 뿌요의 y좌표를 찾음
            // 접지된 뿌요: 바닥에 있거나(y=0), 바로 아래에 다른 뿌요가 있는 경우
            // 가장 높은 접지된 뿌요 위에 있는 모든 뿌요가 떠있는 상태

            int highestGroundedY = -1;
            for (int y = 0; y < HEIGHT; y++) {
                Puyo puyo = getPuyoAt(x, y);
                if (puyo != null && puyo.isAlive()) {
                    // 이 뿌요가 접지되어 있는지 확인: 바닥에 있거나, 바로 아래에 뿌요가 있음
                    boolean isGrounded = (y == 0) || (getPuyoAt(x, y - 1) != null);
                    if (isGrounded) {
                        highestGroundedY = y;
                    } else {
                        // 접지되지 않은 첫 번째 뿌요 발견 - 이 뿌요와 그 위쪽 모든 뿌요가 떠있는 상태
                        LogUtil.debug("Board", "getAllFloatingPuyos: Found floating puyo at (" + x + "," + y
                                + ") color=" + puyo.getColor() + ", highestGroundedY=" + highestGroundedY);
                        for (int fy = y; fy < HEIGHT; fy++) {
                            Puyo floatingPuyo = getPuyoAt(x, fy);
                            if (floatingPuyo != null && floatingPuyo.isAlive()) {
                                floating.add(floatingPuyo);
                                LogUtil.debug("Board", "  Adding floating puyo at (" + x + "," + fy + ") color="
                                        + floatingPuyo.getColor());
                            }
                        }
                        break; // 첫 번째 떠있는 뿌요를 찾았으면 중단 (그 위는 모두 떠있음)
                    }
                }
            }
        }
        LogUtil.debug("Board", "getAllFloatingPuyos END: Total floating=" + floating.size());
        return floating;
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