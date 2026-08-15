package com.puyo.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.puyo.game.PuyoGame;
import com.puyo.game.GameMode;
import com.puyo.game.menus.MenuItem;
import com.puyo.game.menus.MenuLoader;
import com.puyo.game.menus.MenuAction;
import com.puyo.game.config.GameViewport;
import com.puyo.game.graphics.FontManager;

public class MenuScreen extends BaseScreen {
    private final PuyoGame game;
    private final SpriteBatch batch;
    private final FontManager fontManager;
    private MenuItem[] menuItems;
    private int selectedIndex = 0;

    public MenuScreen(PuyoGame game) {
        super(game);
        this.game = game;
        this.batch = new SpriteBatch();
        this.fontManager = FontManager.getInstance();
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

        // 타이틀 폰트 (48px)
        com.badlogic.gdx.graphics.g2d.BitmapFont titleFont = fontManager.getTitleFont(48);
        String title = "PUYO PUYO 2";
        float titleWidth = titleFont.draw(batch, title, 0, 0).width;
        // 메뉴 레이아웃: 중앙 정렬 영역 사용
        float contentCenterX = GameViewport.Menu.CONTENT_OFFSET_X + GameViewport.Menu.CONTENT_WIDTH / 2f;
        titleFont.draw(batch, title, contentCenterX - titleWidth / 2f,
                GameViewport.Menu.CONTENT_OFFSET_Y + GameViewport.Menu.CONTENT_HEIGHT - 50);

        // 메뉴 폰트 (32px)
        com.badlogic.gdx.graphics.g2d.BitmapFont menuFont = fontManager.getMenuFont(32);

        float startY = GameViewport.Menu.CONTENT_OFFSET_Y + GameViewport.Menu.CONTENT_HEIGHT / 2f + 100;
        float itemHeight = 80;

        for (int i = 0; i < menuItems.length; i++) {
            String label = menuItems[i].label;
            if (i == selectedIndex) {
                label = "> " + label + " <";
                menuFont.setColor(1, 1, 0, 1);
            } else {
                menuFont.setColor(1, 1, 1, 1);
            }

            float labelWidth = menuFont.draw(batch, label, 0, 0).width;
            menuFont.draw(batch, label, contentCenterX - labelWidth / 2f, startY - i * itemHeight);
        }

        menuFont.setColor(1, 1, 1, 1);
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
        // 폰트는 FontManager가 관리하므로 여기서 dispose 하지 않음
    }
}
