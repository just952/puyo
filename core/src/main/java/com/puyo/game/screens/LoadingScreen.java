package com.puyo.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.puyo.game.PuyoGame;
import com.puyo.game.config.GameViewport;

public class LoadingScreen extends BaseScreen {
    private final SpriteBatch batch;
    private final BitmapFont font;
    private float timer = 0f;
    
    public LoadingScreen(PuyoGame game) {
        super(game);
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
    }

    @Override
    public void show() {
        initViewport();
        timer = 0f;
        Gdx.app.log("LoadingScreen", "Resources loading started...");
    }

    @Override
    public void render(float delta) {
        timer += delta;
        
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        font.getData().setScale(3f);
        
        String text = "LOADING";
        // Animated dots
        int dots = (int)(timer * 2) % 4;
        text += ".".repeat(dots);
        
        float textWidth = font.draw(batch, text, 0, 0).width;
        font.draw(batch, text, (GameViewport.VIRTUAL_WIDTH - textWidth) / 2f, GameViewport.VIRTUAL_HEIGHT / 2f);
        
        batch.end();

        // Auto-transition to MenuScreen after 1.5 seconds
        if (timer > 1.5f) {
            game.setScreen(new MenuScreen(game));
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
