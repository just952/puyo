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

public class StoryModeSelectScreen implements Screen {
    private final PuyoGame game;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final StoryModeManager storyManager;
    private int selectedIndex = 0;

    public StoryModeSelectScreen(PuyoGame game) {
        this.game = game;
        this.batch = new SpriteBatch();
        this.font = new BitmapFont(); // later replace with TTF
        this.storyManager = new StoryModeManager();
        this.selectedIndex = 0;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        batch.begin();
        font.draw(batch, "스토리 모드 선택", 200, 450);

        int unlocked = storyManager.getUnlockedStageCount();
        int total = storyManager.getTotalStages();

        // Show each stage
        float startY = 380;
        float lineHeight = 30;
        for (int i = 0; i < total; i++) {
            StageData stage = storyManager.getStageAt(i);
            if (stage == null) continue;
            String status = (i < unlocked) ? "선택 가능" : "잠금";
            String label = (i + 1) + ". " + stage.opponent + " (" + status + ")";
            if (i == selectedIndex) {
                label = "> " + label + " <"; // highlight selected
            }
            // Optionally show a snippet of dialogue
            if (stage.dialogue != null && stage.dialogue.length > 0) {
                label += "\n   \"" + stage.dialogue[0] + "\"";
            }
            float y = startY - i * lineHeight * 1.5f; // each entry takes two lines if dialogue present
            font.draw(batch, label, 200, y);
        }

        // Instructions
        float instrY = 50;
        font.draw(batch, "(위/아래로 이동, 엔터로 선택, 백스페이스로 뒤로)", 200, instrY);
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
