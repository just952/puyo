package com.puyo.game.input;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.puyo.game.config.ConfigManager;

/**
 * 키보드(PC) / 터치(모바일) 통합 입력 처리기.
 * 플랫폼 구분 없이 공통 조회 메서드 제공.
 * DAS/ARR은 초 단위(float)로 관리하며 GameWorld.FALLING_ANIMATION_INTERVAL(0.025f) 스타일과 통일.
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

    // DAS/ARR 설정 (초 단위, ConfigManager에서 로드)
    private float DAS_DEPLAY_HORIZONTAL_SEC;
    private float ARR_INTERVAL_HORIZONTAL_SEC;
    private float DAS_DELAY_SOFTDROP_SEC;
    private float ARR_INTERVAL_SOFTDROP_SEC;

    // DAS/ARR 추적용 상태 (좌우/소프트드랍 독립 관리)
    private float horizontalHeldTimeSec = 0f;
    private float softDropHeldTimeSec = 0f;
    private boolean horizontalRepeatTriggered = false;
    private boolean softDropRepeatTriggered = false;

    // 🆕 첫 프레임 즉시 이동 감지용 플래그 (키/터치 눌림 순간 true, updateDasArr에서 처리 후 false)
    private boolean horizontalFirstFrame = false;
    private boolean softDropFirstFrame = false;

    public InputHandler() {
        // 기본 생성자 (데스크톱용) - 설정 로드
        loadConfig();
    }

    public InputHandler(TouchController touchController) {
        this.touchController = touchController;
        this.isMobile = true;
        loadConfig();
    }

    /**`
     * 설정 로드 (ConfigManager에서 DAS/ARR 값 읽기)
     */
    private void loadConfig() {
        ConfigManager.GameConfig config = ConfigManager.getInstance().getConfig();
        this.DAS_DEPLAY_HORIZONTAL_SEC = config.das_delay_horizontal_sec;
        this.ARR_INTERVAL_HORIZONTAL_SEC = config.arr_interval_horizontal_sec;
        this.DAS_DELAY_SOFTDROP_SEC = config.das_delay_softdrop_sec;
        this.ARR_INTERVAL_SOFTDROP_SEC = config.arr_interval_softdrop_sec;
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

        // 방향키/드롭키가 눌리면 anyPressed 개념은 이제 각 키별로 분리됨
        switch (keycode) {
            case Input.Keys.LEFT:
            case Input.Keys.A:
                leftPressed = true;
                horizontalFirstFrame = true; // 🆕 첫 프레임 즉시 이동 플래그
                return true;
            case Input.Keys.RIGHT:
            case Input.Keys.D:
                rightPressed = true;
                horizontalFirstFrame = true; // 🆕 첫 프레임 즉시 이동 플래그
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
                softDropFirstFrame = true; // 🆕 첫 프레임 즉시 이동 플래그
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
     * 
     * @param delta 프레임 시간(초)
     */
    public void update(float delta) {
        prevLeftPressed = leftPressed;
        prevRightPressed = rightPressed;
        prevRotatePressed = rotatePressed;
        prevDropPressed = dropPressed;
        prevHardDropPressed = hardDropPressed;

        // DAS/ARR 처리 (데스크톱 키보드만)
        if (!isMobile) {
            updateDasArr(delta);
        }

        if (isMobile && touchController != null) {
            touchController.update(delta);
        }
    }

    /**
     * DAS (Delayed Auto Shift) / ARR (Auto Repeat Rate) 업데이트
     * - 좌우 이동 키와 소프트 드랍 키를 독립적으로 관리
     * - 첫 프레임 즉시 repeatTriggered = true
     * - DAS 지연 후 ARR 간격으로 반복
     * 
     * @param delta 프레임 시간(초)
     */
    private void updateDasArr(float delta) {
        // 좌우 이동 키 체크
        boolean horizontalPressed = leftPressed || rightPressed;

        if (horizontalPressed) {
            if (horizontalFirstFrame) {
                // 🆕 첫 프레임: 즉시 트리거 후 플래그 해제
                horizontalRepeatTriggered = true;
                horizontalFirstFrame = false;
                horizontalHeldTimeSec = 0f; // 타이머 시작
            } else {
                horizontalHeldTimeSec += delta;
                if (horizontalHeldTimeSec < DAS_DEPLAY_HORIZONTAL_SEC) {
                    // DAS 지연 중: 반복 없음
                    horizontalRepeatTriggered = false;
                } else {
                    // DAS 지연 후: ARR 주기로 반복
                    // (누적시간 - 지연) % 주기 < delta 면 이번 프레임에 트리거
                    float postDasTime = horizontalHeldTimeSec - DAS_DEPLAY_HORIZONTAL_SEC;
                    horizontalRepeatTriggered = (postDasTime % ARR_INTERVAL_HORIZONTAL_SEC) < delta;
                }
            }
        } else {
            horizontalHeldTimeSec = 0f;
            horizontalRepeatTriggered = false;
            horizontalFirstFrame = false; // 🆕 리셋
        }

        // 소프트 드랍 키 체크 (독립적)
        if (dropPressed) {
            if (softDropFirstFrame) {
                // 🆕 첫 프레임: 즉시 트리거 후 플래그 해제
                softDropRepeatTriggered = true;
                softDropFirstFrame = false;
                softDropHeldTimeSec = 0f; // 타이머 시작
            } else {
                softDropHeldTimeSec += delta;
                if (softDropHeldTimeSec < DAS_DELAY_SOFTDROP_SEC) {
                    // DAS 지연 중: 반복 없음
                    softDropRepeatTriggered = false;
                } else {
                    // DAS 지연 후: ARR 주기로 반복
                    float postDasTime = softDropHeldTimeSec - DAS_DELAY_SOFTDROP_SEC;
                    softDropRepeatTriggered = (postDasTime % ARR_INTERVAL_SOFTDROP_SEC) < delta;
                }
            }
        } else {
            softDropHeldTimeSec = 0f;
            softDropRepeatTriggered = false;
            softDropFirstFrame = false; // 🆕 리셋
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
        return dropPressed && softDropRepeatTriggered;
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
     * 키가 눌려 있어도 heldTime, repeatTriggered만 초기화하여
     * 첫 프레임 즉시 이동 + DAS 지연 재시작 보장
     */
    public void resetDasArr() {
        horizontalHeldTimeSec = 0f;
        softDropHeldTimeSec = 0f;
        horizontalRepeatTriggered = false;
        softDropRepeatTriggered = false;
        horizontalFirstFrame = false; // 🆕
        softDropFirstFrame = false;   // 🆕
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