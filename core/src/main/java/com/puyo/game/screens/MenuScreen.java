package com.puyo.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.puyo.game.PuyoGame;
import com.puyo.game.menus.MenuItem;
import com.puyo.game.menus.MenuLoader;
import com.puyo.game.menus.MenuAction;

public class MenuScreen implements Screen {
    private final PuyoGame game;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private MenuItem[] menuItems;
    private int selectedIndex = 0;

    public MenuScreen(PuyoGame game) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.font = new BitmapFont(); // Will be replaced with custom TTF font later
        this.menuItems = MenuLoader.loadMenu("main");
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        font.draw(batch, "PUYO PUYO 2", 200, 400);

        for (int i = 0; i < menuItems.length; i++) {
            String label = menuItems[i].label;
            if (i == selectedIndex) {
                label = "> " + label + " <"; // Indicator for selected item
            }
            font.draw(batch, label, 250, 300 - i * 40);
        }
        batch.end();

        // Input handling
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
                            // TODO: Implement StoryModeSelectScreen
                            // game.setScreen(new StoryModeSelectScreen(game));
                            break;
                        case "versus_mode_select":
                            // TODO: Implement VersusModeSelectScreen
                            // game.setScreen(new VersusModeSelectScreen(game));
                            break;
                        case "options_menu":
                            // TODO: Implement OptionsScreen
                            // game.setScreen(new OptionsScreen(game));
                            break;
                    }
                    break;
                case START_GAME:
                    if ("endless".equals(item.mode)) {
                        // TODO: Pass actual game mode to PlayScreen
                        // game.setScreen(new PlayScreen(game, GameMode.ENDLESS));
                    }
                    break;
                case EXIT_GAME:
                    Gdx.app.exit();
                    break;
                case POP_SCREEN:
                    // TODO: Implement screen popping logic
                    break;
                case NONE:
                    // No action
                    break;
            }
        } catch (IllegalArgumentException e) {
            Gdx.app.error("MenuScreen", "Unknown action: " + item.action);
        }
    }

    @Override
    public void resize(int width, int height) {
    }

    @Override
    public void show() {
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
        batch.dispose();
        font.dispose();
    }
}
