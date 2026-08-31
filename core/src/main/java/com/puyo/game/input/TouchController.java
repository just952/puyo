package com.puyo.game.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.Disposable;
import com.puyo.game.config.ConfigManager;

/**
 * 모바일 전용 터치 컨트롤러.
 * 4버튼 레이아웃: 좌/우 이동, 회전, 드롭(더블탭=하드드롭)
 * 정규화 좌표(0~1) 기반 해상도 독립적 터치 영역.
 * DAS/ARR은 초 단위(float)로 관리하며 InputHandler와 동일 로직 적용.
 */
public class TouchController implements InputProcessor, Disposable {
    // 버튼 영역 (정규화 좌표 0~1, 화면 기준: 좌하단 0,0 / 우상단 1,1)
    // 가로 1600, 세로 960 기준

    // 왼쪽 엄지 영역 (하단 왼쪽 40% × 50%)
    private static final float LEFT_AREA_X = 0f;
    private static final float LEFT_AREA_Y = 0f;
    private static final float LEFT_AREA_WIDTH = 0.4f;
    private static final float LEFT_AREA_HEIGHT = 0.5f;

    // 왼쪽 영역 내부: 좌/우 버튼 (가로 절반씩)
    private static final float LEFT_BTN_WIDTH = 0.5f;

    // 오른쪽 엄지 영역 (하단 오른쪽 40% × 50%)
    private static final float RIGHT_AREA_X = 0.6f;
    private static final float RIGHT_AREA_Y = 0f;
    private static final float RIGHT_AREA_WIDTH = 0.4f;
    private static final float RIGHT_AREA_HEIGHT = 0.5f;

    // 오른쪽 영역 내부: 회전(상단), 드롭(하단)
    private static final float RIGHT_BTN_HEIGHT = 0.5f;

    // 터치 상태
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean rotatePressed = false;
    private boolean dropPressed = false;
    private boolean hardDropPressed = false;

    // 이전 프레임 상태 (엣지 감지용)
    private boolean prevRotatePressed = false;
    private boolean prevHardDropPressed = false;

    // 드래그 추적용
    private int activePointer = -1;
    private float dragStartX = 0f;
    private float dragStartY = 0f;
    private boolean dragStartedInLeftArea = false;

    // 더블탭 감지용
    private long lastDropTapTime = 0;
    private static final long DOUBLE_TAP_WINDOW_MS = 300;

    // DAS/ARR 설정 (초 단위, ConfigManager에서 로드)
    private float dasDelayHorizontalSec;
    private float arrIntervalHorizontalSec;
    private float dasDelaySoftdropSec;
    private float arrIntervalSoftdropSec;

    // DAS/ARR 추적용 상태 (좌우/소프트드랍 독립 관리)
    private float horizontalHeldTimeSec = 0f;
    private float softDropHeldTimeSec = 0f;
    private boolean horizontalRepeatTriggered = false;
    private boolean softDropRepeatTriggered = false;

    // 🆕 첫 프레임 즉시 이동 감지용 플래그 (터치 눌림 순간 true, updateDasArr에서 처리 후 false)
    private boolean horizontalFirstFrame = false;
    private boolean softDropFirstFrame = false;

    // 🆕 회전 롱프레스 감지용 (반시계방향)
    private float rotateHeldTimeSec = 0f;
    private boolean rotateLongPressTriggered = false;
    private static final float LONG_PRESS_THRESHOLD_SEC = 0.5f; // 0.5초 이상 홀드 시 반시계방향

    public TouchController() {
        loadConfig();
    }

    /**
     * 설정 로드 (ConfigManager에서 DAS/ARR 값 읽기)
     */
    private void loadConfig() {
        ConfigManager.GameConfig config = ConfigManager.getInstance().getConfig();
        this.dasDelayHorizontalSec = config.das_delay_horizontal_sec;
        this.arrIntervalHorizontalSec = config.arr_interval_horizontal_sec;
        this.dasDelaySoftdropSec = config.das_delay_softdrop_sec;
        this.arrIntervalSoftdropSec = config.arr_interval_softdrop_sec;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        // 화면 좌표를 정규화 좌표로 변환 (0~1)
        float normX = screenX / (float) Gdx.graphics.getWidth();
        float normY = 1f - (screenY / (float) Gdx.graphics.getHeight()); // y축 뒤집기

        // 이미 다른 포인터가 활성화되어 있으면 무시 (멀티터치 방지)
        if (activePointer != -1 && activePointer != pointer) {
            return false;
        }
        activePointer = pointer;
        dragStartX = normX;
        dragStartY = normY;

        // 왼쪽 영역 체크 (좌/우 이동)
        if (isInLeftArea(normX, normY)) {
            if (normX < LEFT_AREA_X + LEFT_AREA_WIDTH * LEFT_BTN_WIDTH) {
                leftPressed = true;
            } else {
                rightPressed = true;
            }
            dragStartedInLeftArea = true;
            horizontalFirstFrame = true; // 🆕 첫 프레임 즉시 이동 플래그
            return true;
        }

        // 오른쪽 영역 체크 (회전/드롭)
        if (isInRightArea(normX, normY)) {
            float relativeY = (normY - RIGHT_AREA_Y) / RIGHT_AREA_HEIGHT;
            if (relativeY > 0.5f) {
                // 상단: 회전
                rotatePressed = true;
                prevRotatePressed = false; // 엣지 감지용
                // 🆕 롱프레스 타이머 시작
                rotateHeldTimeSec = 0f;
                rotateLongPressTriggered = false;
            } else {
                // 하단: 드롭 - 더블탭으로 하드 드롭 감지
                long now = System.currentTimeMillis();
                if (now - lastDropTapTime < DOUBLE_TAP_WINDOW_MS) {
                    // 더블탭 = 하드 드롭
                    hardDropPressed = true;
                    prevHardDropPressed = false;
                    dropPressed = false; // 소프트 드롭 취소
                } else {
                    // 싱글탭 = 소프트 드롭
                    dropPressed = true;
                    softDropFirstFrame = true; // 🆕 첫 프레임 즉시 이동 플래그
                }
                lastDropTapTime = now;
            }
            return true;
        }

        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (pointer != activePointer)
            return false;
        activePointer = -1;

        leftPressed = false;
        rightPressed = false;
        rotatePressed = false;
        dropPressed = false;
        dragStartedInLeftArea = false;
        // hardDropPressed는 엣지 감지 후 자동 리셋됨
        horizontalFirstFrame = false; // 🆕 리셋
        softDropFirstFrame = false;   // 🆕 리셋
        // 🆕 회전 롱프레스 리셋
        rotateHeldTimeSec = 0f;
        rotateLongPressTriggered = false;

        return true;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (pointer != activePointer)
            return false;

        // 화면 좌표를 정규화 좌표로 변환
        float normX = screenX / (float) Gdx.graphics.getWidth();
        float normY = 1f - (screenY / (float) Gdx.graphics.getHeight());

        // 왼쪽 영역에서 드래그를 시작한 경우: 영역을 벗어나도 이동 방향 유지
        if (dragStartedInLeftArea) {
            // 시작 위치 대비 현재 위치의 상대적 X 변화량으로 방향 결정
            float deltaX = normX - dragStartX;
            if (deltaX < -0.02f) { // 왼쪽으로 충분히 드래그
                leftPressed = true;
                rightPressed = false;
            } else if (deltaX > 0.02f) { // 오른쪽으로 충분히 드래그
                leftPressed = false;
                rightPressed = true;
            } else { // 중앙 영역 (데드존)
                leftPressed = false;
                rightPressed = false;
            }
        } else {
            // 오른쪽 영역에서 시작한 드래그: 버튼 영역 내에 있을 때만 활성화
            leftPressed = isInLeftBtn(normX, normY, true);
            rightPressed = isInLeftBtn(normX, normY, false);
        }

        return true;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public boolean keyDown(int keycode) {
        return false;
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    /**
     * 매 프레임 호출하여 이전 상태 업데이트 (엣지 감지용) 및 DAS/ARR 처리
     * 
     * @param delta 프레임 시간(초)
     */
    public void update(float delta) {
        prevRotatePressed = rotatePressed;
        prevHardDropPressed = hardDropPressed;

        // 하드 드롭은 한 프레임만 true
        if (hardDropPressed) {
            hardDropPressed = false;
        }

        // 🆕 회전 롱프레스 감지 (반시계방향)
        if (rotatePressed) {
            rotateHeldTimeSec += delta;
            if (!rotateLongPressTriggered && rotateHeldTimeSec >= LONG_PRESS_THRESHOLD_SEC) {
                rotateLongPressTriggered = true;
            }
        } else {
            rotateHeldTimeSec = 0f;
            rotateLongPressTriggered = false;
        }

        // DAS/ARR 처리 (터치 홀드 시 적용)
        updateDasArr(delta);
    }

    /**
     * DAS (Delayed Auto Shift) / ARR (Auto Repeat Rate) 업데이트
     * - 좌우 이동 터치와 소프트 드랍 터치를 독립적으로 관리
     * - 첫 프레임 즉시 repeatTriggered = true
     * - DAS 지연 후 ARR 간격으로 반복
     * 
     * @param delta 프레임 시간(초)
     */
    private void updateDasArr(float delta) {
        // 좌우 이동 터치 체크
        boolean horizontalPressed = leftPressed || rightPressed;

        if (horizontalPressed) {
            if (horizontalFirstFrame) {
                // 🆕 첫 프레임: 즉시 트리거 후 플래그 해제
                horizontalRepeatTriggered = true;
                horizontalFirstFrame = false;
                horizontalHeldTimeSec = 0f; // 타이머 시작
            } else {
                horizontalHeldTimeSec += delta;
                if (horizontalHeldTimeSec < dasDelayHorizontalSec) {
                    // DAS 지연 중: 반복 없음
                    horizontalRepeatTriggered = false;
                } else {
                    // DAS 지연 후: ARR 주기로 반복
                    float postDasTime = horizontalHeldTimeSec - dasDelayHorizontalSec;
                    horizontalRepeatTriggered = (postDasTime % arrIntervalHorizontalSec) < delta;
                }
            }
        } else {
            horizontalHeldTimeSec = 0f;
            horizontalRepeatTriggered = false;
            horizontalFirstFrame = false; // 🆕 리셋
        }

        // 소프트 드랍 터치 체크 (독립적)
        if (dropPressed) {
            if (softDropFirstFrame) {
                // 🆕 첫 프레임: 즉시 트리거 후 플래그 해제
                softDropRepeatTriggered = true;
                softDropFirstFrame = false;
                softDropHeldTimeSec = 0f; // 타이머 시작
            } else {
                softDropHeldTimeSec += delta;
                if (softDropHeldTimeSec < dasDelaySoftdropSec) {
                    // DAS 지연 중: 반복 없음
                    softDropRepeatTriggered = false;
                } else {
                    // DAS 지연 후: ARR 주기로 반복
                    float postDasTime = softDropHeldTimeSec - dasDelaySoftdropSec;
                    softDropRepeatTriggered = (postDasTime % arrIntervalSoftdropSec) < delta;
                }
            }
        } else {
            softDropHeldTimeSec = 0f;
            softDropRepeatTriggered = false;
            softDropFirstFrame = false; // 🆕 리셋
        }
    }

    // === 헬퍼 메서드 ===

    private boolean isInLeftArea(float x, float y) {
        return x >= LEFT_AREA_X && x < LEFT_AREA_X + LEFT_AREA_WIDTH
                && y >= LEFT_AREA_Y && y < LEFT_AREA_Y + LEFT_AREA_HEIGHT;
    }

    private boolean isInRightArea(float x, float y) {
        return x >= RIGHT_AREA_X && x < RIGHT_AREA_X + RIGHT_AREA_WIDTH
                && y >= RIGHT_AREA_Y && y < RIGHT_AREA_Y + RIGHT_AREA_HEIGHT;
    }

    private boolean isInLeftBtn(float x, float y, boolean isLeftBtn) {
        if (!isInLeftArea(x, y))
            return false;
        float relativeX = (x - LEFT_AREA_X) / LEFT_AREA_WIDTH;
        if (isLeftBtn) {
            return relativeX < LEFT_BTN_WIDTH;
        } else {
            return relativeX >= LEFT_BTN_WIDTH;
        }
    }

    // === 공통 조회 메서드 (InputHandler에서 호출) ===

    /**
     * 좌우 이동 방향 반환 (-1: 왼쪽, 0: 없음, 1: 오른쪽)
     * DAS/ARR 적용: 첫 프레임 즉시 이동 + DAS 지연 후 ARR 주기 반복 이동
     */
    public int getMoveDirection() {
        // 양쪽 동시 누름: 상쇄
        if (leftPressed && rightPressed) {
            return 0;
        }

        // 왼쪽 이동: 첫 프레임 또는 ARR 반복 트리거 시
        if (leftPressed && horizontalRepeatTriggered) {
            return -1;
        }

        // 오른쪽 이동: 첫 프레임 또는 ARR 반복 트리거 시
        if (rightPressed && horizontalRepeatTriggered) {
            return 1;
        }

        return 0;
    }

    /**
     * 회전 버튼이 눌렸는지 확인 (엣지 감지)
     */
    public boolean isRotatePressed() {
        return rotatePressed && !prevRotatePressed;
    }

    /**
     * 회전 버튼 롱프레스 확인 (반시계방향, 엣지 감지)
     * 0.5초 이상 홀드 시 한 번만 true 반환
     */
    public boolean isRotateCounterClockwisePressed() {
        return rotateLongPressTriggered;
    }

    /**
     * 드롭 버튼이 눌려 있는지 확인 (DAS/ARR 적용)
     */
    public boolean isDropPressed() {
        return dropPressed && softDropRepeatTriggered;
    }

    /**
     * 하드 드롭 버튼이 눌렸는지 확인 (엣지 감지)
     */
    public boolean isHardDropPressed() {
        return hardDropPressed && !prevHardDropPressed;
    }

    /**
     * DAS/ARR 상태 리셋 (새 조각 스폰 시 호출)
     */
    public void resetDasArr() {
        horizontalHeldTimeSec = 0f;
        softDropHeldTimeSec = 0f;
        horizontalRepeatTriggered = false;
        softDropRepeatTriggered = false;
        horizontalFirstFrame = false;
        softDropFirstFrame = false;
        rotateHeldTimeSec = 0f;
        rotateLongPressTriggered = false;
    }

    @Override
    public void dispose() {
        // 리소스 해제 필요 시 구현
    }
}