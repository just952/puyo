package com.puyo.game.screens;

import com.badlogic.gdx.Gdx;
import com.puyo.game.PuyoGame;
import com.badlogic.gdx.graphics.GL20;

public class MenuScreen extends BaseScreen {

    public MenuScreen(PuyoGame game) {
        super(game);
    }

    @Override
    public void show() {
        Gdx.app.log("MenuScreen", "Menu Screen Shown");
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.2f, 1); // 진한 남색 배경
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 터치나 키보드 입력 시 PlayScreen으로 전환 (임시)
        if (Gdx.input.justTouched()) {
            game.setScreen(new PlayScreen(game));
        }
    }
}
