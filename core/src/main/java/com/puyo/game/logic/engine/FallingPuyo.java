package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Puyo;

/**
 * 낙하 중인 뿌요 정보를 담는 클래스.
 * 팝 애니메이션용, 분리 낙하용, 기둥 낙하용(부유 뿌요)을 구분합니다.
 */
public class FallingPuyo {
    public Puyo puyo;
    public FallType type;

    // 원본 보드 좌표 (애니메이션 중에도 보드 상의 원래 위치 보존용)
    public int originalX;
    public int originalY;

    public enum FallType {
        CHAIN_POP, // 연쇄 팝 애니메이션
        SEPARATION, // 쌍 분리 낙하
        FLOATING // 연쇄 후 부유 뿌요 낙하
    }

    public FallingPuyo(Puyo puyo, FallType type) {
        this.puyo = puyo;
        this.type = type;
        this.originalX = puyo.getX();
        this.originalY = puyo.getY();
    }

    // 호환용
    public boolean isFromSeparation() {
        return type == FallType.SEPARATION;
    }

    public boolean isFloating() {
        return type == FallType.FLOATING;
    }

    public boolean isChainPop() {
        return type == FallType.CHAIN_POP;
    }
}