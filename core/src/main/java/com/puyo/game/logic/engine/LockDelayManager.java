package com.puyo.game.logic.engine;

/**
 * 락 딜레이(Lock Delay) 관리를 전담하는 클래스.
 * Tsu 규칙: 바닥에 닿은 후 0.5초 동안 이동/회전 가능 (최대 15회), 초과 시 즉시 잠금.
 */
public class LockDelayManager {

    private static final float LOCK_DELAY_TIME = 0.5f; // 초
    private static final int MAX_LOCK_DELAY_MOVES = 15; // Tsu 규칙: 최대 이동/회전 횟수

    private boolean active = false;
    private float timer = 0f;
    private int moveCount = 0;

    /**
     * 락 딜레이 상태 리셋 (새 조각 스폰 시 또는 공중 이동 시)
     */
    public void reset() {
        active = false;
        timer = 0f;
        moveCount = 0;
    }

    /**
     * 락 딜레이 활성화 (바닥에 닿았을 때 호출)
     * 이미 활성화된 상태에서 다시 호출되면 타이머 리셋 (Tsu 규칙: 접지할 때마다 리셋)
     */
    public void activate() {
        active = true;
        timer = 0f;
    }

    /**
     * 이동/회전 기록 (락 딜레이 중에만 카운트)
     */
    public void recordMove() {
        if (active) {
            moveCount++;
        }
    }

    /**
     * 타이머 업데이트
     * 
     * @param delta 프레임 시간
     */
    public void update(float delta) {
        if (active) {
            timer += delta;
        }
    }

    /**
     * 즉시 잠금해야 하는지 확인
     * 
     * @return true면 즉시 잠금, false면 계속 대기
     */
    public boolean shouldLock() {
        if (!active) {
            return false;
        }
        // 시간 초과 또는 이동 횟수 초과 시 즉시 잠금 (15회 초과 = 16번째부터 잠금)
        return timer >= LOCK_DELAY_TIME || moveCount > MAX_LOCK_DELAY_MOVES;
    }

    /**
     * 락 딜레이가 활성화되어 있는지 확인
     */
    public boolean isActive() {
        return active;
    }

    /**
     * 남은 시간 반환 (디버그/표시용)
     */
    public float getRemainingTime() {
        if (!active)
            return 0f;
        return Math.max(0f, LOCK_DELAY_TIME - timer);
    }

    /**
     * 남은 이동 횟수 반환 (디버그/표시용)
     */
    public int getRemainingMoves() {
        if (!active)
            return MAX_LOCK_DELAY_MOVES;
        return Math.max(0, MAX_LOCK_DELAY_MOVES - moveCount);
    }

    /**
     * 락 딜레이 강제 종료 (조각 잠금 시)
     */
    public void forceLock() {
        active = false;
        timer = 0f;
        moveCount = 0;
    }
}