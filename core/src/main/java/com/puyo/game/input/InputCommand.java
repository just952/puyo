package com.puyo.game.input;

/**
 * 한 프레임의 입력 명령을 담는 불변 데이터 클래스 (Record).
 * GameWorld가 이 객체를 통해 입력을 처리하므로 테스트/리플레이/네트워크 동기화 용이.
 */
public record InputCommand(
    int moveDirection,                   // -1: 왼쪽, 0: 없음, 1: 오른쪽
    boolean rotatePressed,               // 회전 (시계방향, 엣지 감지)
    boolean rotateCounterClockwisePressed, // 회전 (반시계방향, 엣지 감지) - 롱프레스/별도 키
    boolean dropPressed,                 // 소프트 드롭 (DAS/ARR 적용)
    boolean hardDropPressed,             // 하드 드롭 (엣지 감지)
    boolean holdPressed,                 // 홀드 (엣지 감지)
    boolean restartPressed               // 재시작 (게임오버 시, 엣지 감지)
) {
    /** 빈 명령 (입력 없음) */
    public static final InputCommand EMPTY = new InputCommand(0, false, false, false, false, false, false);
}