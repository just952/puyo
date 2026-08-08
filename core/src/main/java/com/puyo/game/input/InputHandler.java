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

    // DAS/ARR 추적용 상태
    private int leftHeldFrames = 0;
    private int rightHeldFrames = 0;
    private boolean leftRepeatTriggered = false;
    private boolean rightRepeatTriggered = false;

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
     * - 키 누름 즉시 1회 이동 (첫 프레임)
     * - DAS_DELAY_FRAMES 이후부터 ARR_INTERVAL_FRAMES 간격으로 반복 이동
     */
    private void updateDasArr() {
        // 왼쪽 키 처리
        if (leftPressed) {
            leftHeldFrames++;
            if (leftHeldFrames == 1) {
                // 첫 프레임: 즉시 이동 트리거 (getMoveDirection에서 처리)
                leftRepeatTriggered = true;
            } else if (leftHeldFrames > DAS_DELAY_FRAMES) {
                // DAS 지연 후: ARR 주기로 반복 트리거
                if ((leftHeldFrames - DAS_DELAY_FRAMES) % ARR_INTERVAL_FRAMES == 0) {
                    leftRepeatTriggered = true;
                } else {
                    leftRepeatTriggered = false;
                }
            } else {
                // DAS 지연 중: 반복 안 함
                leftRepeatTriggered = false;
            }
        } else {
            // 키 뗌: 카운터 리셋
            leftHeldFrames = 0;
            leftRepeatTriggered = false;
        }

        // 오른쪽 키 처리
        if (rightPressed) {
            rightHeldFrames++;
            if (rightHeldFrames == 1) {
                rightRepeatTriggered = true;
            } else if (rightHeldFrames > DAS_DELAY_FRAMES) {
                if ((rightHeldFrames - DAS_DELAY_FRAMES) % ARR_INTERVAL_FRAMES == 0) {
                    rightRepeatTriggered = true;
                } else {
                    rightRepeatTriggered = false;
                }
            } else {
                rightRepeatTriggered = false;
            }
        } else {
            rightHeldFrames = 0;
            rightRepeatTriggered = false;
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
        if (leftPressed && leftRepeatTriggered) {
            return -1;
        }

        // 오른쪽 이동: 첫 프레임 또는 ARR 반복 트리거 시
        if (rightPressed && rightRepeatTriggered) {
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