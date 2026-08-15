package com.puyo.game.logic.engine;

import com.puyo.game.util.LogUtil;

/**
 * 락 딜레이(Lock Delay) 상태 관리 클래스.
 * GameWorld에서 인스턴스로 사용하거나, 정적 팩토리로 생성하여 사용.
 * Tsu 규칙: 0.5초 / 최대 15회 이동·회전
 */
public class LockDelayManager {

    public static final float LOCK_DELAY_TIME = 0.5f; // 초
    public static final int MAX_LOCK_DELAY_MOVES = 15; // Tsu 규칙: 최대 이동/회전 횟수

    private boolean active = false;
    private float timer = 0f;
    private int moves = 0;

    /** 락 딜레이 활성화 (바닥에 닿음) */
    public void activate() {
        active = true;
        timer = 0f;
        moves = 0;
        LogUtil.debug("LockDelay", "activate: active=true, timer=0, moves=0");
    }

    /** 락 딜레이 비활성화 (잠금/스폰/분리) */
    public void deactivate() {
        active = false;
        timer = 0f;
        moves = 0;
        LogUtil.debug("LockDelay", "deactivate: active=false, timer=0, moves=0");
    }

    /** 타이머와 이동 횟수만 리셋 (active 유지, 공중 이동/자동낙하 시) */
    public void resetTimerAndMoves() {
        timer = 0f;
        moves = 0;
        LogUtil.debug("LockDelay", "resetTimerAndMoves: timer=0, moves=0 (active=" + active + ")");
    }

    /** 시간 경과 기록 (매 프레임 호출) */
    public void recordTime(float delta) {
        if (active) {
            timer += delta;
        }
    }

    /** 이동/회전 기록 (이동 시 타이머 리셋) */
    public void recordMove() {
        if (active) {
            moves++;
            timer = 0f;
            LogUtil.debug("LockDelay", "recordMove: moves=" + moves + ", timer=0");
        }
    }

    /** 즉시 잠금해야 하는지 확인 */
    public boolean shouldLock() {
        if (!active) {
            return false;
        }
        boolean shouldLock = timer >= LOCK_DELAY_TIME || moves > MAX_LOCK_DELAY_MOVES;
        if (shouldLock) {
            LogUtil.debug("LockDelay",
                    String.format("shouldLock: timer=%.2f (>=%.1f? %b), moves=%d (>%d? %b)",
                            timer, LOCK_DELAY_TIME, timer >= LOCK_DELAY_TIME, moves, MAX_LOCK_DELAY_MOVES,
                            moves > MAX_LOCK_DELAY_MOVES));
        }
        return shouldLock;
    }

    // --- Getters (디버깅용) ---

    public boolean isActive() {
        return active;
    }

    public float getTimer() {
        return timer;
    }

    public int getMoves() {
        return moves;
    }

    public float getRemainingTime() {
        if (!active) return 0f;
        return Math.max(0f, LOCK_DELAY_TIME - timer);
    }

    public int getRemainingMoves() {
        if (!active) return MAX_LOCK_DELAY_MOVES;
        return Math.max(0, MAX_LOCK_DELAY_MOVES - moves);
    }
}
