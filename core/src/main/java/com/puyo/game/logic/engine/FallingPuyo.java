package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Puyo;

/**
 * 낙하 중인 뿌요 정보를 담는 클래스.
 * 팝 애니메이션용(CHAIN_POP)과 일반 낙하용(FALLING)을 구분합니다.
 */
public class FallingPuyo {
    public Puyo puyo;
    public FallType type;

    // 원본 보드 좌표 (애니메이션 중에도 보드 상의 원래 위치 보존용)
    public int originalX;
    public int originalY;

    public enum FallType {
        CHAIN_POP, // 연쇄 팝 애니메이션 (제자리 스케일)
        FALLING    // 일반 낙하 (분리/부유 통합: 열 단위 기둥 낙하)
    }

    public FallingPuyo(Puyo puyo, FallType type) {
        this.puyo = puyo;
        this.type = type;
        this.originalX = puyo.getX();
        this.originalY = puyo.getY();
    }

    public boolean isChainPop() {
        return type == FallType.CHAIN_POP;
    }

    public boolean isFalling() {
        return type == FallType.FALLING;
    }
}
