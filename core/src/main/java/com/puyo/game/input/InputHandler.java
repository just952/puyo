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
                return true;
            case Input.Keys.RIGHT:
            case Input.Keys.D:
                rightPressed = false;
                return true;
            case Input.Keys.UP:
            case Input.Keys.W:
            case Input.Keys.X:
            case Input.Keys.NUM_1:
                rotatePressed = false;
                return true;
            case Input.Keys.DOWN:
            case Input.Keys.S:
                dropPressed = false;
                return true;
            case Input.Keys.SPACE:
            case Input.Keys.ENTER:
                hardDropPressed = false;
                return true;
            default:
                return false;
        }
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
     * 매 프레임 호출하여 이전 상태 업데이트 (엣지 감지용)
     */
    public void update() {
        prevLeftPressed = leftPressed;
        prevRightPressed = rightPressed;
        prevRotatePressed = rotatePressed;
        prevDropPressed = dropPressed;
        prevHardDropPressed = hardDropPressed;

        if (isMobile && touchController != null) {
            touchController.update();
        }
    }

    // === 공통 조회 메서드 ===

    /**
     * 좌우 이동 방향 반환 (-1: 왼쪽, 0: 없음, 1: 오른쪽)
     */
    public int getMoveDirection() {
        if (isMobile && touchController != null) {
            return touchController.getMoveDirection();
        }
        if (leftPressed && !rightPressed)
            return -1;
        if (rightPressed && !leftPressed)
            return 1;
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
     * 소프트 드롭 키가 눌려 있는지 확인 (홀드 감지)
     */
    public boolean isDropPressed() {
        if (isMobile && touchController != null) {
            return touchController.isDropPressed();
        }
        return dropPressed;
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
     * 리소스 해제
     */
    public void dispose() {
        if (touchController != null) {
            touchController.dispose();
            touchController = null;
        }
    }
}