package com.puyo.game;

import com.badlogic.gdx.Application;
import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.puyo.game.config.ConfigManager;

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
}