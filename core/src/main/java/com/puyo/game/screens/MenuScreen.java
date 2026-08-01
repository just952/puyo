package com.puyo.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.puyo.game.PuyoGame;
import com.puyo.game.GameMode;
import com.puyo.game.menus.MenuItem;
import com.puyo.game.menus.MenuLoader;
import com.puyo.game.menus.MenuAction;
import com.puyo.game.config.GameViewport;

public class MenuScreen extends BaseScreen {
    private final PuyoGame game;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private MenuItem[] menuItems;
    private int selectedIndex = 0;

    public MenuScreen(PuyoGame game) {
        super(game);
        this.game = game;
        this.batch = new SpriteBatch();
        this.font = new BitmapFont();
        this.menuItems = MenuLoader.loadMenu("main");
    }

    @Override
    public void show() {
        initViewport();
        this.menuItems = MenuLoader.loadMenu("main");
        selectedIndex = 0;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();
        font.getData().setScale(3f);
        
        String title = "PUYO PUYO 2";
        float titleWidth = font.draw(batch, title, 0, 0).width;
        font.draw(batch, title, (GameViewport.VIRTUAL_WIDTH - titleWidth) / 2f, GameViewport.VIRTUAL_HEIGHT - 150);

        font.getData().setScale(1.5f);
        
        float startY = GameViewport.VIRTUAL_HEIGHT / 2f + 100;
        float itemHeight = 80;
        
        for (int i = 0; i < menuItems.length; i++) {
            String label = menuItems[i].label;
            if (i == selectedIndex) {
                label = "> " + label + " <";
                font.setColor(1, 1, 0, 1);
            } else {
                font.setColor(1, 1, 1, 1);
            }
            
            float labelWidth = font.draw(batch, label, 0, 0).width;
            font.draw(batch, label, (GameViewport.VIRTUAL_WIDTH - labelWidth) / 2f, startY - i * itemHeight);
        }
        
        font.setColor(1, 1, 1, 1);
        batch.end();

        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selectedIndex = (selectedIndex + 1) % menuItems.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selectedIndex = (selectedIndex - 1 + menuItems.length) % menuItems.length;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            handleSelection();
        }
    }

    private void handleSelection() {
        MenuItem item = menuItems[selectedIndex];
        try {
            MenuAction action = MenuAction.valueOf(item.action.toUpperCase());
            
            switch (action) {
                case PUSH_SCREEN:
                    switch (item.target) {
                        case "story_mode_select":
                            game.setScreen(new StoryModeSelectScreen(game));
                            break;
                        case "versus_mode_select":
                            break;
                        case "options_menu":
                            break;
                    }
                    break;
                case START_GAME:
                    if ("endless".equals(item.mode)) {
                        game.setScreen(new PlayScreen(game, GameMode.ENDLESS));
                    }
                    break;
                case EXIT_GAME:
                    Gdx.app.exit();
                    break;
            }
        } catch (IllegalArgumentException e) {
            Gdx.app.error("MenuScreen", "Unknown action: " + item.action);
        }
    }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {
        batch.dispose();
        font.dispose();
    }
}
