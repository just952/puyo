package com.puyo.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.puyo.game.PuyoGame;
import com.puyo.game.story.StoryModeManager;
import com.puyo.game.GameMode;
import com.puyo.game.story.StageData;
import com.puyo.game.config.GameViewport;
import com.puyo.game.graphics.FontManager;

public class StoryModeSelectScreen extends BaseScreen {
    private final PuyoGame game;
    private final SpriteBatch batch;
    private final FontManager fontManager;
    private final StoryModeManager storyManager;
    private int selectedIndex = 0;

    public StoryModeSelectScreen(PuyoGame game) {
        super(game);
        this.game = game;
        this.batch = new SpriteBatch();
        this.fontManager = FontManager.getInstance();
        this.storyManager = new StoryModeManager();
        this.selectedIndex = 0;
    }

    @Override
    public void show() {
        initViewport();
        selectedIndex = 0;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        camera.update();
        batch.setProjectionMatrix(camera.combined);

        batch.begin();

        // 타이틀 폰트 (36px)
        BitmapFont titleFont = fontManager.getTitleFont(36);
        String title = "STORY MODE";
        float titleWidth = titleFont.draw(batch, title, 0, 0).width;
        // 메뉴 레이아웃: 중앙 정렬 영역 사용
        float contentCenterX = GameViewport.Menu.CONTENT_OFFSET_X + GameViewport.Menu.CONTENT_WIDTH / 2f;
        titleFont.draw(batch, title, contentCenterX - titleWidth / 2f,
                GameViewport.Menu.CONTENT_OFFSET_Y + GameViewport.Menu.CONTENT_HEIGHT - 50);

        // 메뉴 폰트 (24px)
        BitmapFont menuFont = fontManager.getMenuFont(24);

        int unlocked = storyManager.getUnlockedStageCount();
        int total = storyManager.getTotalStages();

        float startY = GameViewport.Menu.CONTENT_OFFSET_Y + GameViewport.Menu.CONTENT_HEIGHT / 2f + 150;
        float lineHeight = 60;

        for (int i = 0; i < total; i++) {
            StageData stage = storyManager.getStageAt(i);
            if (stage == null)
                continue;

            boolean isUnlocked = i < unlocked;
            String status = isUnlocked ? "UNLOCKED" : "LOCKED";
            String label = (i + 1) + ". " + stage.opponent + " (" + status + ")";

            if (i == selectedIndex) {
                label = "> " + label + " <";
                menuFont.setColor(1, 1, 0, 1);
            } else {
                menuFont.setColor(isUnlocked ? 1 : 0.5f, isUnlocked ? 1 : 0.5f, isUnlocked ? 1 : 0.5f, 1);
            }

            if (stage.dialogue != null && stage.dialogue.length > 0) {
                String dialogueText = "   \"" + stage.dialogue[0] + "\"";
                float labelWidth = menuFont.draw(batch, label, 0, 0).width;
                menuFont.draw(batch, label, contentCenterX - labelWidth / 2f, startY - i * lineHeight);
                float dialogueWidth = menuFont.draw(batch, dialogueText, 0, 0).width;
                menuFont.draw(batch, dialogueText, contentCenterX - dialogueWidth / 2f, startY - i * lineHeight - 30);
            } else {
                float labelWidth = menuFont.draw(batch, label, 0, 0).width;
                menuFont.draw(batch, label, contentCenterX - labelWidth / 2f, startY - i * lineHeight);
            }
            menuFont.setColor(1, 1, 1, 1);
        }

        // Instructions - 작은 폰트 (16px)
        BitmapFont smallFont = fontManager.getSmallFont(16);
        String instructions = "UP/DOWN: Select  ENTER: Start  BACK: Return";
        float instrWidth = smallFont.draw(batch, instructions, 0, 0).width;
        smallFont.draw(batch, instructions, contentCenterX - instrWidth / 2f,
                GameViewport.Menu.CONTENT_OFFSET_Y + 50);

        batch.end();

        // input
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            selectedIndex = (selectedIndex + 1) % total;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            selectedIndex = (selectedIndex - 1 + total) % total;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            if (selectedIndex < unlocked) {
                storyManager.setCurrentStageIndex(selectedIndex);
                game.setScreen(new PlayScreen(game, GameMode.NORMAL, selectedIndex));
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE) || Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.setScreen(new MenuScreen(game));
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
