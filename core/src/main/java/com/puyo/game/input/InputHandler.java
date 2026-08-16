package com.puyo.game.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;

/**
 * 키보드(PC) / 터치(모바일) 통합 입력 처리기.
 * 플랫폼 구분 없이 공통 조회 메서드 제공.
 */
public class InputHandler implements InputProcessor {
    /** 모바일 환경 여부 (터치 컨트롤러 사용 시 true) */
    private boolean isMobile = false;

    /** 터치 컨트롤러 참조 (모바일일 때만 사용) */
    private TouchController touchController;

    // 키보드 입력 상태
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean rotatePressed = false;
    private boolean dropPressed = false;
    private boolean hardDropPressed = false;

    // 이전 프레임 키 상태 (엣지 감지용)
    private boolean prevLeftPressed = false;
    private boolean prevRightPressed = false;
    private boolean prevRotatePressed = false;
    private boolean prevDropPressed = false;
    private boolean prevHardDropPressed = false;

    // DAS (Delayed Auto Shift) / ARR (Auto Repeat Rate) 설정
    // 60fps 기준 프레임 단위
    private static final int DAS_DELAY_FRAMES = 16; // 초기 지연: ~0.27초 (16프레임)
    private static final int ARR_INTERVAL_FRAMES = 2; // 반복 주기: 2프레임마다 (초당 30회)

    // DAS/ARR 추적용 상태 (단일 카운터로 통합)
    private int heldFrames = 0;
    private boolean repeatTriggered = false;
    private boolean anyPressed = false;

    public InputHandler() {
        // 기본 생성자 (데스크톱용)
    }

    public InputHandler(TouchController touchController) {
        this.touchController = touchController;
        this.isMobile = true;
    }

    /**
     * 모바일 모드 설정
     */
    public void setMobile(boolean mobile) {
        this.isMobile = mobile;
    }

    /**
     * 터치 컨트롤러 설정
     */
    public void setTouchController(TouchController touchController) {
        this.touchController = touchController;
        this.isMobile = touchController != null;
    }

    @Override
    public boolean keyDown(int keycode) {
        if (isMobile)
            return false; // 모바일에서는 키보드 입력 무시

        // 방향키/드롭키가 눌리면 anyPressed = true
        anyPressed = true;

        switch (keycode) {
            case Input.Keys.LEFT:
            case Input.Keys.A:
                leftPressed = true;
                return true;
            case Input.Keys.RIGHT:
            case Input.Keys.D:
                rightPressed = true;
                return true;
            case Input.Keys.UP:
            case Input.Keys.W:
            case Input.Keys.X: // 시계방향 회전
            case Input.Keys.NUM_1:
                rotatePressed = true;
                return true;
            case Input.Keys.Z: // 반시계방향 회전
            case Input.Keys.NUM_2:
                // 반시계방향은 별도 처리 필요시 추가
                return true;
            case Input.Keys.DOWN:
            case Input.Keys.S:
                dropPressed = true;
                return true;
            case Input.Keys.SPACE:
            case Input.Keys.ENTER:
                hardDropPressed = true;
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean keyUp(int keycode) {
        if (isMobile)
            return false;

        switch (keycode) {
            case Input.Keys.LEFT:
            case Input.Keys.A:
                leftPressed = false;
                break;
            case Input.Keys.RIGHT:
            case Input.Keys.D:
                rightPressed = false;
                break;
            case Input.Keys.UP:
            case Input.Keys.W:
            case Input.Keys.X:
            case Input.Keys.NUM_1:
                rotatePressed = false;
                break;
            case Input.Keys.DOWN:
            case Input.Keys.S:
                dropPressed = false;
                break;
            case Input.Keys.SPACE:
            case Input.Keys.ENTER:
                hardDropPressed = false;
                break;
            default:
                return false;
        }

        // 방향키/드롭키 중 하나라도 남아있으면 anyPressed 유지
        anyPressed = leftPressed || rightPressed || dropPressed;
        return true;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (!isMobile || touchController == null)
            return false;
        return touchController.touchDown(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (!isMobile || touchController == null)
            return false;
        return touchController.touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (!isMobile || touchController == null)
            return false;
        return touchController.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        if (!isMobile || touchController == null)
            return false;
        return touchController.touchCancelled(screenX, screenY, pointer, button);
    }

    /**
     * 매 프레임 호출하여 이전 상태 업데이트 (엣지 감지용) 및 DAS/ARR 처리
     */
    public void update() {
        prevLeftPressed = leftPressed;
        prevRightPressed = rightPressed;
        prevRotatePressed = rotatePressed;
        prevDropPressed = dropPressed;
        prevHardDropPressed = hardDropPressed;

        // DAS/ARR 처리 (데스크톱 키보드만)
        if (!isMobile) {
            updateDasArr();
        }

        if (isMobile && touchController != null) {
            touchController.update();
        }
    }

    /**
     * DAS (Delayed Auto Shift) / ARR (Auto Repeat Rate) 업데이트
     * - 아무 방향키/드롭키가 눌리면 공유 카운터 증가
     * - 첫 프레임 즉시 repeatTriggered = true
     * - DAS_DELAY_FRAMES 이후부터 ARR_INTERVAL_FRAMES 간격으로 반복
     */
    private void updateDasArr() {
        if (anyPressed) {
            heldFrames++;
            if (heldFrames == 1) {
                repeatTriggered = true;  // 첫 프레임 즉시
            } else if (heldFrames > DAS_DELAY_FRAMES) {
                repeatTriggered = ((heldFrames - DAS_DELAY_FRAMES) % ARR_INTERVAL_FRAMES == 0);
            } else {
                repeatTriggered = false;  // DAS 지연 중
            }
        } else {
            heldFrames = 0;
            repeatTriggered = false;
        }
    }

    // === 공통 조회 메서드 ===

    /**
     * 좌우 이동 방향 반환 (-1: 왼쪽, 0: 없음, 1: 오른쪽)
     * DAS/ARR 적용: 첫 프레임 즉시 이동 + DAS 지연 후 ARR 주기 반복 이동
     */
    public int getMoveDirection() {
        if (isMobile && touchController != null) {
            return touchController.getMoveDirection();
        }

        // 양쪽 동시 누름: 상쇄
        if (leftPressed && rightPressed) {
            return 0;
        }

        // 왼쪽 이동: 첫 프레임 또는 ARR 반복 트리거 시
        if (leftPressed && repeatTriggered) {
            return -1;
        }

        // 오른쪽 이동: 첫 프레임 또는 ARR 반복 트리거 시
        if (rightPressed && repeatTriggered) {
            return 1;
        }

        return 0;
    }

    /**
     * 회전 키가 눌렸는지 확인 (엣지 감지: 이번 프레임에만 true)
     */
    public boolean isRotatePressed() {
        if (isMobile && touchController != null) {
            return touchController.isRotatePressed();
        }
        return rotatePressed && !prevRotatePressed;
    }

    /**
     * 소프트 드롭 키가 눌려 있는지 확인 (DAS/ARR 적용)
     */
    public boolean isDropPressed() {
        if (isMobile && touchController != null) {
            return touchController.isDropPressed();
        }
        return dropPressed && repeatTriggered;
    }

    /**
     * 하드 드롭 키가 눌렸는지 확인 (엣지 감지: 이번 프레임에만 true)
     */
    public boolean isHardDropPressed() {
        if (isMobile && touchController != null) {
            return touchController.isHardDropPressed();
        }
        return hardDropPressed && !prevHardDropPressed;
    }

    /**
     * DAS/ARR 상태 리셋 (새 조각 스폰 시 호출)
     * 키가 눌려 있어도 heldFrames, repeatTriggered만 초기화하여
     * 첫 프레임 즉시 이동 + DAS 지연 재시작 보장
     */
    public void resetDasArr() {
        heldFrames = 0;
        repeatTriggered = false;
        // anyPressed는 현재 키 상태 반영이므로 유지
    }

    /**
     * 리소스 해제
     */
    public void dispose() {
        if (touchController != null) {
            touchController.dispose();
            touchController = null;
        }
    }
}
