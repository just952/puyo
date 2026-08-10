package com.puyo.game.logic.engine;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class LockDelayManagerTest {

    private LockDelayManager lockDelayManager;

    @Before
    public void setUp() {
        lockDelayManager = new LockDelayManager();
    }

    @Test
    public void initialState_inactive() {
        assertFalse("초기에는 비활성", lockDelayManager.isActive());
        assertFalse("초기에는 잠금 안 함", lockDelayManager.shouldLock());
        assertEquals("초기 남은 시간 0", 0f, lockDelayManager.getRemainingTime(), 0.001f);
        assertEquals("초기 남은 이동 15", 15, lockDelayManager.getRemainingMoves());
    }

    @Test
    public void activate_thenShouldLockAfterTime() {
        lockDelayManager.activate();
        assertTrue("활성화됨", lockDelayManager.isActive());
        assertFalse("바로 잠금 안 함", lockDelayManager.shouldLock());

        // 0.5초 경과
        lockDelayManager.update(0.5f);
        assertTrue("시간 초과 후 잠금", lockDelayManager.shouldLock());
    }

    @Test
    public void activate_thenShouldLockAfterMoves() {
        lockDelayManager.activate();

        // 15회 이동
        for (int i = 0; i < 15; i++) {
            lockDelayManager.recordMove();
            assertFalse("15회 미만은 잠금 안 함", lockDelayManager.shouldLock());
        }

        // 16번째 이동
        lockDelayManager.recordMove();
        assertTrue("15회 초과 시 즉시 잠금", lockDelayManager.shouldLock());
    }

    @Test
    public void reset_clearsState() {
        lockDelayManager.activate();
        lockDelayManager.update(0.3f);
        lockDelayManager.recordMove();
        lockDelayManager.recordMove();

        lockDelayManager.reset();

        assertFalse("리셋 후 비활성", lockDelayManager.isActive());
        assertFalse("리셋 후 잠금 안 함", lockDelayManager.shouldLock());
        assertEquals("리셋 후 남은 시간 0", 0f, lockDelayManager.getRemainingTime(), 0.001f);
        assertEquals("리셋 후 남은 이동 15", 15, lockDelayManager.getRemainingMoves());
    }

    @Test
    public void forceLock_clearsState() {
        lockDelayManager.activate();
        lockDelayManager.update(0.3f);
        lockDelayManager.recordMove();

        lockDelayManager.forceLock();

        assertFalse("강제 잠금 후 비활성", lockDelayManager.isActive());
        assertFalse("강제 잠금 후 잠금 안 함", lockDelayManager.shouldLock());
    }

    @Test
    public void recordMove_onlyCountsWhenActive() {
        // 비활성 상태에서 이동 기록
        lockDelayManager.recordMove();
        lockDelayManager.recordMove();

        lockDelayManager.activate();

        // 활성 상태에서 이동 기록
        lockDelayManager.recordMove();
        lockDelayManager.recordMove();

        // 총 2회만 카운트되어야 함 (비활성 시 기록 무시)
        assertEquals("활성 상태에서만 카운트", 2, 15 - lockDelayManager.getRemainingMoves());
    }

    @Test
    public void activate_twice_resetsTimer() {
        lockDelayManager.activate();
        lockDelayManager.update(0.3f);

        // 다시 활성화 (타이머 리셋)
        lockDelayManager.activate();

        // 0.2초만 경과해도 총 0.5초 안 되었으므로 잠금 안 함
        lockDelayManager.update(0.2f);
        assertFalse("재활성화 시 타이머 리셋", lockDelayManager.shouldLock());

        // 추가 0.3초 경과하면 잠금
        lockDelayManager.update(0.3f);
        assertTrue("총 0.5초 경과 시 잠금", lockDelayManager.shouldLock());
    }

    @Test
    public void remainingTime_decreases() {
        lockDelayManager.activate();
        assertEquals("초기 0.5초", 0.5f, lockDelayManager.getRemainingTime(), 0.001f);

        lockDelayManager.update(0.2f);
        assertEquals("0.2초 후 0.3초 남음", 0.3f, lockDelayManager.getRemainingTime(), 0.001f);

        lockDelayManager.update(0.3f);
        assertEquals("0.5초 후 0 남음", 0f, lockDelayManager.getRemainingTime(), 0.001f);
    }

    @Test
    public void remainingMoves_decreases() {
        lockDelayManager.activate();
        assertEquals("초기 15회", 15, lockDelayManager.getRemainingMoves());

        lockDelayManager.recordMove();
        assertEquals("1회 후 14회", 14, lockDelayManager.getRemainingMoves());

        for (int i = 0; i < 14; i++) {
            lockDelayManager.recordMove();
        }
        assertEquals("15회 후 0회", 0, lockDelayManager.getRemainingMoves());
    }
}