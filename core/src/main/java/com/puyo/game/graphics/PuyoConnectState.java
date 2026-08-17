package com.puyo.game.graphics;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.PuyoColor;

/**
 * 뿌요 연결 상태를 비트마스크로 표현하는 Enum.
 * 렌더링 시점에 보드 상태를 보고 계산하며, Puyo 객체에 저장하지 않음.
 */
public enum PuyoConnectState {
    NONE(0),
    UP(1 << 0),
    DOWN(1 << 1),
    LEFT(1 << 2),
    RIGHT(1 << 3),
    UP_DOWN(UP.mask | DOWN.mask),
    LEFT_RIGHT(LEFT.mask | RIGHT.mask),
    UP_RIGHT(UP.mask | RIGHT.mask),
    UP_LEFT(UP.mask | LEFT.mask),
    DOWN_RIGHT(DOWN.mask | RIGHT.mask),
    DOWN_LEFT(DOWN.mask | LEFT.mask),
    UP_LEFT_RIGHT(UP.mask | LEFT.mask | RIGHT.mask),
    DOWN_LEFT_RIGHT(DOWN.mask | LEFT.mask | RIGHT.mask),
    UP_DOWN_RIGHT(UP.mask | DOWN.mask | RIGHT.mask),
    UP_DOWN_LEFT(UP.mask | DOWN.mask | LEFT.mask),
    ALL(UP.mask | DOWN.mask | LEFT.mask | RIGHT.mask);

    public final int mask;

    PuyoConnectState(int mask) {
        this.mask = mask;
    }

    public boolean has(int dir) {
        return (mask & dir) != 0;
    }

    /**
     * 보드 상태를 보고 현재 뿌요의 연결 상태 계산
     */
    public static PuyoConnectState fromBoard(Board board, int x, int y, PuyoColor color) {
        int mask = 0;
        if (board.hasSameColorAt(x, y + 1, color)) mask |= UP.mask;
        if (board.hasSameColorAt(x, y - 1, color)) mask |= DOWN.mask;
        if (board.hasSameColorAt(x - 1, y, color)) mask |= LEFT.mask;
        if (board.hasSameColorAt(x + 1, y, color)) mask |= RIGHT.mask;
        return values()[mask]; // 0~15 인덱스 매핑 (enum 순서가 mask 순서와 일치)
    }
}