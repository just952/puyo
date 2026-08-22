package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.util.LogUtil;


/**
 * 뿌요의 낙하, 소멸 및 연쇄 반응을 처리하는 엔진입니다.
 */
public class GravityEngine {

    public GravityEngine() {
    }

    /**
     * 모든 뿌요를 아래로 떨어뜨려 빈 칸을 채웁니다 (압축 중력 알고리즘).
     * 각 열단위로 투-포인터(Two-pointer) 방식으로 물리적인 중력을 연산합니다.
     * 
     * @param board 게임 보드
     * @return 뿌요가 움직였는지 여부
     */
    @Deprecated
    public boolean applyGravity(Board board) {
        boolean moved = false;
        int width = Board.WIDTH;
        int height = Board.TOTAL_HEIGHT;

        LogUtil.debug("GravityEngine", "=== applyGravity START ===");
        LogUtil.debug("GravityEngine", "Board before gravity:\n" + board.toString());

        for (int x = 0; x < width; x++) {
            int writeY = 0; // 뿌요가 새로 위치할(쌓일) 아래쪽 인덱스
            for (int readY = 0; readY < height; readY++) {
                Puyo current = board.getPuyoAt(x, readY);
                if (current != null) {
                    if (readY != writeY) {
                        LogUtil.debug("GravityEngine", "Moving puyo from (" + x + "," + readY + ") to (" + x + ","
                                + writeY + ") color=" + current.getColor());
                        board.setPuyoAt(x, writeY, current);
                        board.setPuyoAt(x, readY, null);
                        current.setY(writeY); // Puyo의 내부 좌표도 업데이트
                        moved = true;
                    }
                    writeY++;
                }
            }
        }

        LogUtil.debug("GravityEngine", "Board after gravity:\n" + board.toString());
        LogUtil.debug("GravityEngine", "=== applyGravity END: moved=" + moved + " ===");
        return moved;
    }

    // findMatches, findGroup, clearPositions 메서드는 더 이상 사용되지 않음 (MatchFinder 사용)
    // 필요시 Board 파라미터를 받아서 static 메서드로 구현 가능

    /**
     * 위치 정보를 담는 내부 클래스입니다.
     */
    public static class Position {
        public final int x, y;

        public Position(int x, int y) {
            this.x = x;
            this.y = y;
        }
    }
}