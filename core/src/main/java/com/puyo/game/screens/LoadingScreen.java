package com.puyo.game.screens;

import com.badlogic.gdx.Gdx;
import com.puyo.game.PuyoGame;

public class LoadingScreen extends BaseScreen {
    
    public LoadingScreen(PuyoGame game) {
        super(game);
    }

    @Override
    public void show() {
        Gdx.app.log("LoadingScreen", "Resources loading started...");
        // 실제 리소스 로딩 로직은 AssetManager 연동 후 추가
    }

    @Override
    public void render(float delta) {
        // 임시로 바로 MenuScreen으로 전환
        game.setScreen(new MenuScreen(game));
    }
}
