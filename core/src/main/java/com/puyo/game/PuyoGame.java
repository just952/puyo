package com.puyo.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;

public class PuyoGame extends Game {
    @Override
    public void create() {
        Gdx.app.log("PuyoGame", "Game Created!");
        setScreen(new com.puyo.game.screens.LoadingScreen(this));
    }

    @Override
    public void render() {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        super.render(); // 현재 설정된 Screen의 render() 호출
    }
    
    @Override
    public void dispose() {
        super.dispose();
    }
}
