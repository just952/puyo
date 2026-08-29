package com.puyo.game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.puyo.game.config.ConfigManager;
import com.puyo.game.input.InputProvider;
import com.puyo.game.screens.PlayScreen;
import com.puyo.game.GameMode;

public class PuyoGame extends Game {
    @Override
    public void create() {
        ConfigManager configManager = ConfigManager.getInstance();
        ConfigManager.GameConfig config = configManager.getConfig();

        // 로그 레벨 적용 (예시)
        String logLevel = config.log_level.toLowerCase();

        switch (logLevel) {
            case "debug":
                Gdx.app.setLogLevel(Application.LOG_DEBUG);
                break;
            case "info":
                Gdx.app.setLogLevel(Application.LOG_INFO);
                break;
            case "error":
                Gdx.app.setLogLevel(Application.LOG_ERROR);
                break;
            default:
                Gdx.app.setLogLevel(Application.LOG_INFO);
        }

        Gdx.app.log("GameStart", "Environment: " + config.env);
        Gdx.app.log("PuyoGame", "Game Created!");

        // TODO: Replace LoadingScreen with MenuScreen after LoadingScreen completes
        setScreen(new com.puyo.game.screens.LoadingScreen(this));
    }

    @Override
    public void render() {
        // Ensure GL is available before clearing the screen
        if (Gdx.gl != null) {
            Gdx.gl.glClearColor(0, 0, 0, 1);
            Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        }
        super.render(); // 현재 설정된 Screen의 render() 호출
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    // === PlayScreen 팩토리 메서드 ===

    public PlayScreen createPlayScreen(GameMode mode) {
        return createPlayScreen(mode, -1);
    }

    public PlayScreen createPlayScreen(GameMode mode, int storyStageIndex) {
        InputProvider inputProvider = createInputProvider();
        return new PlayScreen(this, mode, storyStageIndex, inputProvider);
    }

    /** 플랫폼별 InputProvider 생성 (데스크톱/안드로이드 런처에서 반드시 오버라이드해야 함) */
    protected InputProvider createInputProvider() {
        throw new UnsupportedOperationException("createInputProvider() must be overridden by platform launcher");
    }
}