package com.puyo.game.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.Disposable;

/**
 * 모바일 전용 터치 컨트롤러.
 * 4버튼 레이아웃: 좌/우 이동, 회전, 드롭(더블탭=하드드롭)
 * 정규화 좌표(0~1) 기반 해상도 독립적 터치 영역.
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

    public TouchController() {
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
            return true;
        }

        // 오른쪽 영역 체크 (회전/드롭)
        if (isInRightArea(normX, normY)) {
            float relativeY = (normY - RIGHT_AREA_Y) / RIGHT_AREA_HEIGHT;
            if (relativeY > 0.5f) {
                // 상단: 회전
                rotatePressed = true;
                prevRotatePressed = false; // 엣지 감지용
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
     * 매 프레임 호출하여 이전 상태 업데이트 (엣지 감지용)
     */
    public void update() {
        prevRotatePressed = rotatePressed;
        prevHardDropPressed = hardDropPressed;

        // 하드 드롭은 한 프레임만 true
        if (hardDropPressed) {
            hardDropPressed = false;
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
     */
    public int getMoveDirection() {
        if (leftPressed && !rightPressed)
            return -1;
        if (rightPressed && !leftPressed)
            return 1;
        return 0;
    }

    /**
     * 회전 버튼이 눌렸는지 확인 (엣지 감지)
     */
    public boolean isRotatePressed() {
        return rotatePressed && !prevRotatePressed;
    }

    /**
     * 드롭 버튼이 눌려 있는지 확인 (홀드 감지)
     */
    public boolean isDropPressed() {
        return dropPressed;
    }

    /**
     * 하드 드롭 버튼이 눌렸는지 확인 (엣지 감지)
     */
    public boolean isHardDropPressed() {
        return hardDropPressed && !prevHardDropPressed;
    }

    @Override
    public void dispose() {
        // 리소스 해제 필요 시 구현
    }
}