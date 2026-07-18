package com.puyo.game.screens;

import com.badlogic.gdx.Gdx;
import com.puyo.game.PuyoGame;
import com.badlogic.gdx.graphics.GL20;

public class PlayScreen extends BaseScreen {

    public PlayScreen(PuyoGame game) {
        super(game);
    }

    @Override
    public void show() {
        Gdx.app.log("PlayScreen", "Play Screen Shown");
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.3f, 0.1f, 1); // 진한 초록색 배경
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }
}
