package com.puyo.game.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.puyo.game.config.ConfigManager;
import com.puyo.game.util.LogUtil;

/**
 * 데스크톱(키보드) 전용 입력 처리기.
 * InputProvider 인터페이스 구현.
 * core 모듈의 InputHandler에서 TouchController 의존성 제거 후 이동.
 */
public class DesktopInputHandler implements InputProvider, InputProcessor {
    // 키보드 입력 상태 (게임 액션용)
    private boolean leftPressed = false;
    private boolean rightPressed = false;
    private boolean rotatePressed = false;
    private boolean rotateCounterClockwisePressed = false; // 🆕 반시계방향 (Z 키)
    private boolean dropPressed = false;
    private boolean hardDropPressed = false;
    private boolean holdPressed = false;
    private boolean restartPressed = false;

    // 이전 프레임 키 상태 (엣지 감지용)
    private boolean prevLeftPressed = false;
    private boolean prevRightPressed = false;
    private boolean prevRotatePressed = false;
    private boolean prevRotateCounterClockwisePressed = false; // 🆕 반시계방향 prev
    private boolean prevDropPressed = false;
    private boolean prevHardDropPressed = false;
    private boolean prevHoldPressed = false;
    private boolean prevRestartPressed = false;

    // DAS/ARR 설정 (초 단위, ConfigManager에서 로드)
    private float DAS_DELAY_HORIZONTAL_SEC;
    private float ARR_INTERVAL_HORIZONTAL_SEC;
    private float DAS_DELAY_SOFTDROP_SEC;
    private float ARR_INTERVAL_SOFTDROP_SEC;

    // DAS/ARR 추적용 상태 (좌우/소프트드랍 독립 관리)
    private float horizontalHeldTimeSec = 0f;
    private float softDropHeldTimeSec = 0f;
    private boolean horizontalRepeatTriggered = false;
    private boolean softDropRepeatTriggered = false;

    // 첫 프레임 즉시 이동 감지용 플래그
    private boolean horizontalFirstFrame = false;
    private boolean softDropFirstFrame = false;

    // 이번 프레임 생성된 명령 (pollCommand에서 반환 후 클리어)
    private InputCommand pendingCommand = InputCommand.EMPTY;

    // 텍스트 입력 모드 관련
    private InputMode currentMode = InputMode.GAME_PLAY;
    private TextInputListener textInputListener;

    public DesktopInputHandler() {
        loadConfig();
        Gdx.input.setInputProcessor(this);
    }

    /** 설정 로드 (ConfigManager에서 DAS/ARR 값 읽기) */
    private void loadConfig() {
        ConfigManager.GameConfig config = ConfigManager.getInstance().getConfig();
        this.DAS_DELAY_HORIZONTAL_SEC = config.das_delay_horizontal_sec;
        this.ARR_INTERVAL_HORIZONTAL_SEC = config.arr_interval_horizontal_sec;
        this.DAS_DELAY_SOFTDROP_SEC = config.das_delay_softdrop_sec;
        this.ARR_INTERVAL_SOFTDROP_SEC = config.arr_interval_softdrop_sec;
    }

    @Override
    public void update(float delta) {
        if (currentMode == InputMode.TEXT_INPUT) {
            return;
        }

        // 1. 먼저 명령 구성 (이전 프레임의 prev 상태 사용 - 엣지 감지 위함)
        buildCommand();

        // 2. 그 다음 현재 상태를 이전 상태로 복사 (다음 프레임용)
        prevLeftPressed = leftPressed;
        prevRightPressed = rightPressed;
        prevRotatePressed = rotatePressed;
        prevRotateCounterClockwisePressed = rotateCounterClockwisePressed; // 🆕
        prevDropPressed = dropPressed;
        prevHardDropPressed = hardDropPressed;
        prevHoldPressed = holdPressed;
        prevRestartPressed = restartPressed;

        updateDasArr(delta);
    }
/** DAS/ARR 타이머 갱신 및 반복 트리거 계산 */
    private void updateDasArr(float delta) {
        boolean anyHorizontal = leftPressed || rightPressed;
        if (anyHorizontal) {
            if (horizontalFirstFrame) {
                horizontalRepeatTriggered = true;
                horizontalFirstFrame = false;
                horizontalHeldTimeSec = 0f;
            } else {
                horizontalHeldTimeSec += delta;
                if (horizontalHeldTimeSec < DAS_DELAY_HORIZONTAL_SEC) {
                    horizontalRepeatTriggered = false;
                } else {
                    float postDasTime = horizontalHeldTimeSec - DAS_DELAY_HORIZONTAL_SEC;
                    horizontalRepeatTriggered = (postDasTime % ARR_INTERVAL_HORIZONTAL_SEC) < delta;
                }
            }
        } else {
            horizontalHeldTimeSec = 0f;
            horizontalRepeatTriggered = false;
            horizontalFirstFrame = false;
        }

        if (dropPressed) {
            if (softDropFirstFrame) {
                softDropRepeatTriggered = true;
                softDropFirstFrame = false;
                softDropHeldTimeSec = 0f;
            } else {
                softDropHeldTimeSec += delta;
                if (softDropHeldTimeSec < DAS_DELAY_SOFTDROP_SEC) {
                    softDropRepeatTriggered = false;
                } else {
                    float postDasTime = softDropHeldTimeSec - DAS_DELAY_SOFTDROP_SEC;
                    softDropRepeatTriggered = (postDasTime % ARR_INTERVAL_SOFTDROP_SEC) < delta;
                }
            }
        } else {
            softDropHeldTimeSec = 0f;
            softDropRepeatTriggered = false;
            softDropFirstFrame = false;
        }
    }

    /** 현재 키 상태로부터 InputCommand 구성 */
    private void buildCommand() {
        int moveDirection = 0;
        if (leftPressed && rightPressed) {
            moveDirection = 0;
        } else if (leftPressed && horizontalRepeatTriggered) {
            moveDirection = -1;
        } else if (rightPressed && horizontalRepeatTriggered) {
            moveDirection = 1;
        }

        boolean rotate = rotatePressed && !prevRotatePressed;
        //LogUtil.debug("DesktopInputHandler", "- rotatePressed: " + rotatePressed + ", prevRotatePressed: " + prevRotatePressed + ", rotate: " + rotate);
        
        // 🆕 반시계방향 회전 (Z 키)
        boolean rotateCCW = rotateCounterClockwisePressed && !prevRotateCounterClockwisePressed;
        
        boolean drop = dropPressed && softDropRepeatTriggered;
        boolean hardDrop = hardDropPressed && !prevHardDropPressed;
        boolean hold = holdPressed && !prevHoldPressed;
        boolean restart = restartPressed && !prevRestartPressed;

        pendingCommand = new InputCommand(moveDirection, rotate, rotateCCW, drop, hardDrop, hold, restart);
    }

    @Override
    public InputCommand pollCommand() {
        InputCommand cmd = pendingCommand;
        pendingCommand = InputCommand.EMPTY;
        return cmd;
    }

    @Override
    public void resetDasArr() {
        horizontalHeldTimeSec = 0f;
        softDropHeldTimeSec = 0f;
        horizontalRepeatTriggered = false;
        softDropRepeatTriggered = false;
        horizontalFirstFrame = false;
        softDropFirstFrame = false;
    }

    @Override
    public void dispose() {
    }

    // === 텍스트 입력 모드 지원 ===

    @Override
    public InputMode getInputMode() {
        return currentMode;
    }

    @Override
    public void setInputMode(InputMode mode) {
        this.currentMode = mode;
        if (mode == InputMode.TEXT_INPUT) {
            Gdx.input.getTextInput(new TextInputListenerAdapter(), "", "", "");
        }
    }

    @Override
    public void setTextInputListener(TextInputListener listener) {
        this.textInputListener = listener;
    }

    @Override
    public void clearTextInputListener() {
        this.textInputListener = null;
    }

    // === InputProcessor 구현 (키보드) ===

    @Override
    public boolean keyDown(int keycode) {
        if (currentMode == InputMode.TEXT_INPUT) {
            return false;
        }

        switch (keycode) {
            case Input.Keys.LEFT:
            case Input.Keys.A:
                leftPressed = true;
                horizontalFirstFrame = true;
                return true;
            case Input.Keys.RIGHT:
            case Input.Keys.D:
                rightPressed = true;
                horizontalFirstFrame = true;
                return true;
            case Input.Keys.UP:
            case Input.Keys.W:
            case Input.Keys.NUMPAD_8:
            case Input.Keys.X: // 🆕 X 키도 시계방향 회전 (일반적 뿌요뿌요 키맵)
                rotatePressed = true;
                return true;
            case Input.Keys.Z: // 🆕 Z 키 = 반시계방향 회전
                rotateCounterClockwisePressed = true;
                return true;
            case Input.Keys.DOWN:
            case Input.Keys.S:
            case Input.Keys.NUMPAD_2:
                dropPressed = true;
                softDropFirstFrame = true;
                return true;
            case Input.Keys.SPACE:
                hardDropPressed = true;
                return true;
            case Input.Keys.SHIFT_LEFT:
            case Input.Keys.SHIFT_RIGHT:
            case Input.Keys.C:
                holdPressed = true;
                return true;
            case Input.Keys.ENTER:
            case Input.Keys.ESCAPE:
                restartPressed = true;
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean keyUp(int keycode) {
        if (currentMode == InputMode.TEXT_INPUT) {
            return false;
        }

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
            case Input.Keys.NUMPAD_8:
            case Input.Keys.X:
                rotatePressed = false;
                return true;
            case Input.Keys.Z: // 🆕 Z 키 = 반시계방향 회전 해제
                rotateCounterClockwisePressed = false;
                return true;
            case Input.Keys.DOWN:
            case Input.Keys.S:
            case Input.Keys.NUMPAD_2:
                dropPressed = false;
                return true;
            case Input.Keys.SPACE:
                hardDropPressed = false;
                return true;
            case Input.Keys.SHIFT_LEFT:
            case Input.Keys.SHIFT_RIGHT:
            case Input.Keys.C:
                holdPressed = false;
                return true;
            case Input.Keys.ENTER:
            case Input.Keys.ESCAPE:
                restartPressed = false;
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean keyTyped(char character) {
        if (currentMode == InputMode.TEXT_INPUT && textInputListener != null) {
            if (character == '\b') {
                textInputListener.onBackspace();
            } else if (character == '\n' || character == '\r') {
                textInputListener.onEnter();
            } else if (character == 27) {
                textInputListener.onEscape();
            } else if (character >= 32) {
                textInputListener.onTextInput(character);
            }
            return true;
        }
        return false;
    }

    @Override public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }
    @Override public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override public boolean scrolled(float amountX, float amountY) { return false; }

    /** libGDX TextInputListener 어댑터 (IME 콜백용) */
    private class TextInputListenerAdapter implements com.badlogic.gdx.Input.TextInputListener {
        @Override public void input(String text) {
            if (textInputListener != null) {
                for (char c : text.toCharArray()) {
                    textInputListener.onTextInput(c);
                }
            }
        }
        @Override public void canceled() {
            if (textInputListener != null) {
                textInputListener.onEscape();
            }
        }
    }
}