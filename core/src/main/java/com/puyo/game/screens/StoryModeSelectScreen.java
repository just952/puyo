package com.puyo.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.puyo.game.PuyoGame;

public class StoryModeSelectScreen implements Screen {
    private final PuyoGame game;

    public StoryModeSelectScreen(PuyoGame game) {
        this.game = game;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.2f, 0.2f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        
        // TODO: Implement actual story mode selection screen
        Gdx.app.log("StoryModeSelectScreen", "Rendering - TODO: Implement actual screen");
    }

    @Override
    public void resize(int width, int height) {
        // TODO: Implement resize handling
    }

    @Override
    public void show() {
        Gdx.app.log("StoryModeSelectScreen", "Show");
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void dispose() {
    }
}
