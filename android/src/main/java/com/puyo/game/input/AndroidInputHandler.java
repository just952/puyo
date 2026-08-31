package com.puyo.game.input;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.utils.Disposable;
import com.puyo.game.config.ConfigManager;

/**
 * 안드로이드(터치) 전용 입력 처리기.
 * InputProvider 인터페이스 구현.
 * TouchController를 내부에서 사용하여 터치 입력을 처리.
 */
public class AndroidInputHandler implements InputProvider, InputProcessor, Disposable {
    private final TouchController touchController;

    // 텍스트 입력 모드 관련
    private InputMode currentMode = InputMode.GAME_PLAY;
    private TextInputListener textInputListener;

    // 이번 프레임 생성된 명령
    private InputCommand pendingCommand = InputCommand.EMPTY;

    public AndroidInputHandler() {
        this.touchController = new TouchController();
        Gdx.input.setInputProcessor(this);
    }

    @Override
    public void update(float delta) {
        if (currentMode == InputMode.TEXT_INPUT) {
            // 텍스트 입력 모드: TouchController 업데이트 불필요
            return;
        }

        // GAME_PLAY 모드: TouchController 업데이트 (DAS/ARR 등)
        touchController.update(delta);
        
        // 명령 구성
        buildCommand();
    }

    private void buildCommand() {
        int moveDirection = touchController.getMoveDirection();
        boolean rotate = touchController.isRotatePressed();
        boolean rotateCCW = touchController.isRotateCounterClockwisePressed(); // 🆕 반시계방향 (롱프레스)
        boolean drop = touchController.isDropPressed();
        boolean hardDrop = touchController.isHardDropPressed();
        
        // 홀드/재시작은 터치에서 별도 처리 필요시 추가
        // 현재 TouchController에는 hold/restart 없으므로 false
        boolean hold = false;
        boolean restart = false;

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
        touchController.resetDasArr();
    }

    @Override
    public void dispose() {
        if (touchController != null) {
            touchController.dispose();
        }
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
            // Android IME 표시
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

    // === InputProcessor 위임 (TouchController로 전달) ===

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        if (currentMode == InputMode.TEXT_INPUT) {
            return false; // 텍스트 입력 모드에서는 터치 게임 입력 무시
        }
        return touchController.touchDown(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        if (currentMode == InputMode.TEXT_INPUT) {
            return false;
        }
        return touchController.touchUp(screenX, screenY, pointer, button);
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        if (currentMode == InputMode.TEXT_INPUT) {
            return false;
        }
        return touchController.touchDragged(screenX, screenY, pointer);
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        if (currentMode == InputMode.TEXT_INPUT) {
            return false;
        }
        return touchController.touchCancelled(screenX, screenY, pointer, button);
    }

    @Override public boolean keyDown(int keycode) { return false; }
    @Override public boolean keyUp(int keycode) { return false; }
    
    @Override
    public boolean keyTyped(char character) { 
        // Android에서 하드웨어 키보드 연결 시 문자 입력 처리
        if (currentMode == InputMode.TEXT_INPUT && textInputListener != null) {
            if (character == '\b') textInputListener.onBackspace();
            else if (character == '\n' || character == '\r') textInputListener.onEnter();
            else if (character == 27) textInputListener.onEscape();
            else if (character >= 32) textInputListener.onTextInput(character);
            return true;
        }
        return false; 
    }
    
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