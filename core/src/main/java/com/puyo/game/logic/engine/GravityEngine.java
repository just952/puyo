package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoColor;

import java.util.ArrayList;
import java.util.List;

/**
 * 뿌요의 낙하, 소멸 및 연쇄 반응을 처리하는 엔진입니다.
 */
public class GravityEngine {
    private final Board board;

    public GravityEngine(Board board) {
        this.board = board;
    }

    /**
     * 모든 뿌요를 아래로 떨어뜨려 빈 칸을 채웁니다 (압축 중력 알고리즘).
     * 각 열단위로 투-포인터(Two-pointer) 방식으로 물리적인 중력을 연산합니다.
     * @return 뿌요가 움직였는지 여부
     */
    public boolean applyGravity() {
        boolean moved = false;
        int width = board.getWidth();
        int height = board.getHeight();

        for (int x = 0; x < width; x++) {
            int writeY = 0; // 뿌요가 새로 위치할(쌓일) 아래쪽 인덱스
            for (int readY = 0; readY < height; readY++) {
                Puyo current = board.getPuyo(x, readY);
                if (current != null) {
                    if (readY != writeY) {
                        board.setPuyo(x, writeY, current);
                        board.clearPuyo(x, readY);
                        moved = true;
                    }
                    writeY++;
                }
            }
        }
        return moved;
    }

    /**
     * 보드에서 소멸할 그룹을 찾아냅니다.
     * @return 소멸할 위치 목록 (Position 리스트)
     */
    public List<Position> findMatches() {
        List<Position> toClear = new ArrayList<>();
        boolean[][] visited = new boolean[board.getWidth()][board.getHeight()];

        for (int x = 0; x < board.getWidth(); x++) {
            for (int y = 0; y < board.getHeight(); y++) {
                Puyo puyo = board.getPuyo(x, y);
                if (puyo != null && !visited[x][y]) {
                    List<Position> group = new ArrayList<>();
                    findGroup(x, y, puyo.getColor(), visited, group);
                    
                    if (group.size() >= 4) {
                        toClear.addAll(group);
                    }
                }
            }
        }
        return toClear;
    }

    private void findGroup(int x, int y, PuyoColor color, boolean[][] visited, List<Position> group) {
        if (x < 0 || x >= board.getWidth() || y < 0 || y >= board.getHeight() || 
            visited[x][y] || board.getPuyo(x, y) == null || board.getPuyo(x, y).getColor() != color) {
            return;
        }

        visited[x][y] = true;
        group.add(new Position(x, y));

        // 인접한 4방향 탐색
        findGroup(x + 1, y, color, visited, group);
        findGroup(x - 1, y, color, visited, group);
        findGroup(x, y + 1, color, visited, group);
        findGroup(x, y - 1, color, visited, group);
    }

    /**
     * 소멸할 위치를 보드에서 제거합니다.
     */
    public void clearPositions(List<Position> positions) {
        for (Position pos : positions) {
            board.clearPuyo(pos.x, pos.y);
        }
    }

    /**
     * 위치 정보를 담는 내부 클래스입니다.
     */
    public static class Position {
        public final int x, y;
        public Position(int x, int y) { this.x = x; this.y = y; }
    }
}
