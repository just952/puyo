package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoPair;
import com.puyo.game.util.LogUtil;

import java.util.List;

/**
 * 뿌요쌍 분리(Separation) 로직을 전담하는 클래스.
 * 가로 상태(rotation 1 또는 3)에서 한쪽만 막혔을 때 쌍을 분리합니다.
 */
public class SeparationManager {

    /**
     * 가로 상태에서 분리 가능한지 확인
     * 
     * @param pair  현재 떨어지는 뿌요쌍
     * @param board 게임 보드
     * @return 분리 가능하면 true
     */
    public boolean canSeparate(PuyoPair pair, Board board) {
        if (pair == null) {
            return false;
        }
        int rotation = pair.getRotation();
        // rotation 1: right(오른쪽), rotation 3: left(왼쪽) - 가로 상태
        if (rotation != 1 && rotation != 3) {
            LogUtil.debug("Separation", "canSeparate: false - rotation=" + rotation + " (not horizontal)");
            return false;
        }

        Puyo left = pair.getLeft();
        Puyo right = pair.getRight();

        boolean leftCanFall = board.canMoveDown(left);
        boolean rightCanFall = board.canMoveDown(right);

        LogUtil.debug("Separation",
                "canSeparate: left=(" + left.getX() + "," + left.getY() + ") canFall=" + leftCanFall +
                        ", right=(" + right.getX() + "," + right.getY() + ") canFall=" + rightCanFall);

        // 한 쪽만 이동 가능하면 분리 가능
        boolean canSeparate = leftCanFall != rightCanFall;
        LogUtil.debug("Separation", "canSeparate: " + canSeparate);
        return canSeparate;
    }

    /**
     * 쌍 분리 실행
     * 막힌 쪽은 즉시 잠금, 자유로운 쪽은 단일 뿌요로 자동 낙하 시작
     * 
     * @param pair         현재 떨어지는 뿌요쌍
     * @param board        게임 보드
     * @param fallingPuyos 낙하 중인 뿌요 리스트 (자유로운 쪽 추가됨)
     * @return 분리되었으면 true, 아니면 false
     */
    public boolean separate(PuyoPair pair, Board board, List<FallingPuyo> fallingPuyos) {
        if (!canSeparate(pair, board)) {
            return false;
        }

        Puyo left = pair.getLeft();
        Puyo right = pair.getRight();

        boolean leftCanFall = board.canMoveDown(left);
        boolean rightCanFall = board.canMoveDown(right);

        Puyo blockedPuyo;
        Puyo freePuyo;

        if (leftCanFall && !rightCanFall) {
            blockedPuyo = right;
            freePuyo = left;
            LogUtil.debug("Separation", "Separating: right BLOCKED at (" + right.getX() + "," + right.getY()
                    + "), left FREE at (" + left.getX() + "," + left.getY() + ")");
        } else if (rightCanFall && !leftCanFall) {
            blockedPuyo = left;
            freePuyo = right;
            LogUtil.debug("Separation", "Separating: left BLOCKED at (" + left.getX() + "," + left.getY()
                    + "), right FREE at (" + right.getX() + "," + right.getY() + ")");
        } else {
            // 둘 다 가능하거나 둘 다 불가능하면 분리 안 함
            LogUtil.debug("Separation", "Separating: both can fall or both cannot fall - no separation");
            return false;
        }

        // 막힌 쪽 즉시 잠금
        LogUtil.debug("Separation", "Placing blocked puyo at (" + blockedPuyo.getX() + "," + blockedPuyo.getY()
                + ") color=" + blockedPuyo.getColor());
        board.placePuyo(blockedPuyo);

        // 자유로운 쪽을 보드에서 제거 (중복 방지)
        board.removePuyo(freePuyo);

        // 자유로운 쪽 단일 뿌요로 자동 낙하 시작
        LogUtil.debug("Separation", "Adding free puyo to falling: (" + freePuyo.getX() + "," + freePuyo.getY()
                + ") color=" + freePuyo.getColor() + " hash=" + System.identityHashCode(freePuyo));
        fallingPuyos.add(new FallingPuyo(freePuyo, FallingPuyo.FallType.SEPARATION)); // isFromSeparation=true

        return true;
    }
}