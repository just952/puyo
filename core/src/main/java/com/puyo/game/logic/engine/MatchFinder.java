package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoColor;
import com.puyo.game.util.LogUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 뿌요 매칭(연결 그룹 찾기)을 전담하는 클래스.
 * 상태를 가지지 않는 순수 함수형 유틸리티 클래스.
 * 모든 메서드는 static으로 제공됩니다.
 */
public class MatchFinder {

    /**
     * 보드에서 4개 이상 연결된 같은 색 뿌요 그룹을 모두 찾습니다.
     * OJAMA(방해 뿌요)와 HARD(단단한 뿌요)는 매칭 대상에서 제외됩니다.
     *
     * @param board 검사할 보드
     * @return 매칭된 그룹들의 리스트 (각 그룹은 Puyo 리스트)
     */
    public static List<List<Puyo>> findAllMatchingGroups(Board board) {
        boolean[][] visited = new boolean[Board.WIDTH][Board.TOTAL_HEIGHT];
        List<List<Puyo>> groups = new ArrayList<>();

        LogUtil.debug("MatchFinder", "=== findAllMatchingGroups START ===");
        LogUtil.debug("MatchFinder", "Board state:\n" + board.toString());
        LogUtil.debug("MatchFinder", "Called from stack trace: " + Thread.currentThread().getStackTrace()[2].getClassName() + "." + Thread.currentThread().getStackTrace()[2].getMethodName() + "()");

        for (int x = 0; x < Board.WIDTH; x++) {
            for (int y = 0; y < Board.TOTAL_HEIGHT; y++) {
                Puyo puyo = board.getPuyoAt(x, y);
                if (puyo != null && !visited[x][y] &&
                        puyo.getColor() != PuyoColor.OJAMA &&
                        puyo.getColor() != PuyoColor.HARD) {
                    List<Puyo> group = new ArrayList<>();
                    collectGroup(board, x, y, puyo.getColor(), visited, group);
                    if (group.size() >= 4) {
                        LogUtil.debug("MatchFinder",
                                "Found match group: color=" + puyo.getColor() + ", size=" + group.size());
                        for (Puyo p : group) {
                            LogUtil.debug("MatchFinder", "  Puyo at (" + p.getX() + "," + p.getY() + ")");
                        }
                        groups.add(group);
                    } else if (group.size() > 0) {
                        LogUtil.debug("MatchFinder",
                                "Group too small: color=" + puyo.getColor() + ", size=" + group.size() + " (ignored)");
                    }
                }
            }
        }

        LogUtil.debug("MatchFinder", "=== findAllMatchingGroups END: " + groups.size() + " groups found ===");
        return groups;
    }

    /**
     * 특정 위치에서 시작하여 같은 색상으로 연결된 뿌요들을 DFS로 수집합니다.
     */
    private static void collectGroup(Board board, int x, int y, PuyoColor color, boolean[][] visited,
            List<Puyo> group) {
        if (!board.isInside(x, y) || visited[x][y]) {
            return;
        }
        Puyo puyo = board.getPuyoAt(x, y);
        if (puyo == null || !puyo.isAlive() || puyo.getColor() != color) {
            return;
        }
        visited[x][y] = true;
        group.add(puyo);
        // 4방향 탐색
        collectGroup(board, x + 1, y, color, visited, group);
        collectGroup(board, x - 1, y, color, visited, group);
        collectGroup(board, x, y + 1, color, visited, group);
        collectGroup(board, x, y - 1, color, visited, group);
    }

    /**
     * 특정 뿌요에서 시작하여 연결된 같은 색 뿌요 그룹을 찾습니다.
     * (단일 그룹 탐색용)
     *
     * @param board 검사할 보드
     * @param start 시작 뿌요
     * @return 연결된 뿌요 리스트 (4개 미만이어도 반환)
     */
    public static List<Puyo> findConnectedGroup(Board board, Puyo start) {
        if (start == null || start.getColor() == PuyoColor.OJAMA || start.getColor() == PuyoColor.HARD) {
            return Collections.emptyList();
        }
        Set<Puyo> visited = new HashSet<>();
        List<Puyo> result = new ArrayList<>();
        dfs(board, start, start.getColor(), visited, result);
        return result;
    }

    private static void dfs(Board board, Puyo puyo, PuyoColor color, Set<Puyo> visited, List<Puyo> result) {
        if (!visited.add(puyo)) {
            return;
        }
        result.add(puyo);
        int x = puyo.getX();
        int y = puyo.getY();
        int[] dx = { 1, -1, 0, 0 };
        int[] dy = { 0, 0, 1, -1 };
        for (int i = 0; i < 4; i++) {
            int nx = x + dx[i];
            int ny = y + dy[i];
            Puyo neighbor = board.getPuyoAt(nx, ny);
            if (neighbor != null && neighbor.getColor() == color && neighbor.isAlive() && !visited.contains(neighbor)) {
                dfs(board, neighbor, color, visited, result);
            }
        }
    }
}