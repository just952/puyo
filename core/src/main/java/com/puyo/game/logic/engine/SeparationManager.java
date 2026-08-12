package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoPair;
import com.puyo.game.util.LogUtil;

/**
 * 뿌요쌍 분리(Separation) 로직을 전담하는 클래스.
 * 가로 상태(rotation 1 또는 3)에서 한쪽만 막혔을 때 쌍을 분리합니다.
 * 보드 조작은 하지 않고 액션만 반환합니다 (GameWorld가 실행).
 */
public class SeparationManager {

    /**
     * 분리 실행 결과
     */
    public static class SeparationResult {
        public boolean separated = false;
        public Puyo blockedPuyo = null;   // GameWorld가 placePuyo
        public Puyo freePuyo = null;      // GameWorld가 falling에 추가
    }

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
            // LogUtil.debug("Separation", "canSeparate: false - rotation=" + rotation + "(not horizontal)");
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
     * 쌍 분리 실행 (보드 조작 없음, 액션만 반환)
     * 
     * @param pair  현재 떨어지는 뿌요쌍
     * @param board 게임 보드
     * @return 분리 결과 (blockedPuyo, freePuyo 포함)
     */
    public SeparationResult separate(PuyoPair pair, Board board) {
        SeparationResult result = new SeparationResult();
        
        if (!canSeparate(pair, board)) {
            return result;
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
            return result;
        }

        // 결과만 반환 (GameWorld가 실제 보드 조작 수행)
        result.separated = true;
        result.blockedPuyo = blockedPuyo;
        result.freePuyo = freePuyo;
        
        LogUtil.debug("Separation", "SeparationResult: blocked=(" + blockedPuyo.getX() + "," + blockedPuyo.getY()
                + "), free=(" + freePuyo.getX() + "," + freePuyo.getY() + ")");
        
        return result;
    }
}