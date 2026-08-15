package com.puyo.game.logic.engine;

import com.puyo.game.util.LogUtil;

/**
 * 락 딜레이(Lock Delay) 관리를 위한 유틸리티 클래스.
 * 상태를 가지지 않는 순수 함수형으로 제공됩니다 (GameWorld가 상태 관리).
 * 모든 메서드는 static으로 제공됩니다.
 */
public class LockDelayManager {

    public static final float LOCK_DELAY_TIME = 0.5f; // 초
    public static final int MAX_LOCK_DELAY_MOVES = 15; // Tsu 규칙: 최대 이동/회전 횟수

    // 인스턴스 생성 방지
    private LockDelayManager() {}

    /**
     * 락 딜레이 타이머 업데이트
     * 
     * @param delta 프레임 시간
     * @param timer 현재 타이머 값 (참조로 업데이트됨)
     * @param active 락 딜레이 활성화 여부
     */
    public static void update(float delta, float[] timer, boolean active) {
        if (active) {
            timer[0] += delta;
        }
    }

    /**
     * 즉시 잠금해야 하는지 확인
     * 
     * @param timer 현재 타이머 값
     * @param moveCount 현재 이동 횟수
     * @param active 락 딜레이 활성화 여부
     * @return true면 즉시 잠금, false면 계속 대기
     */
    public static boolean shouldLock(float timer, int moveCount, boolean active) {
        if (!active) {
            return false;
        }
        boolean shouldLock = timer >= LOCK_DELAY_TIME || moveCount > MAX_LOCK_DELAY_MOVES;
        if (shouldLock) {
            LogUtil.debug("LockDelay",
                    String.format("LockDelay SHOULD LOCK: timer=%.2f (>=%.1f? %b), moveCount=%d (>%d? %b)",
                            timer, LOCK_DELAY_TIME, timer >= LOCK_DELAY_TIME, moveCount, MAX_LOCK_DELAY_MOVES,
                            moveCount > MAX_LOCK_DELAY_MOVES));
        }
        return shouldLock;
    }

    /**
     * 남은 시간 반환 (디버그/표시용)
     */
    public static float getRemainingTime(float timer, boolean active) {
        if (!active)
            return 0f;
        return Math.max(0f, LOCK_DELAY_TIME - timer);
    }

    /**
     * 남은 이동 횟수 반환 (디버그/표시용)
     */
    public static int getRemainingMoves(int moveCount, boolean active) {
        if (!active)
            return MAX_LOCK_DELAY_MOVES;
        return Math.max(0, MAX_LOCK_DELAY_MOVES - moveCount);
    }
}
