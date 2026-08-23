package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Puyo;

/**
 * 상태를 가진 뿌요 정보를 담는 클래스.
 * 낙하 중인 뿌요(FALLING), 팝 애니메이션 중인 뿌요(POPPING), 
 * 착지 중 흔들리는 뿌요(SETTLING) 등을 구분합니다.
 */
public class StatefulPuyo {
    public Puyo puyo;
    public StateType type;

    // 원본 보드 좌표 (애니메이션 중에도 보드 상의 원래 위치 보존용)
    public int originalX;
    public int originalY;

    public enum StateType {
        POPPING,   // 연쇄 팝 애니메이션 (제자리 스케일)
        FALLING,   // 일반 낙하 (분리/부유 통합)
        SETTLING   // 착지 후 흔들림/정착 대기 (향후 확장용)
    }

    public StatefulPuyo(Puyo puyo, StateType type) {
        this.puyo = puyo;
        this.type = type;
        this.originalX = puyo.getX();
        this.originalY = puyo.getY();
    }

    public boolean isPopping() {
        return type == StateType.POPPING;
    }

    public boolean isFalling() {
        return type == StateType.FALLING;
    }

    public boolean isSettling() {
        return type == StateType.SETTLING;
    }
}