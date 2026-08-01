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

public class StoryModeSelectScreen extends BaseScreen {
    private final PuyoGame game;
    private final SpriteBatch batch;
    private final BitmapFont font;
    private final StoryModeManager storyManager;
    private int selectedIndex = 0;

    public StoryModeSelectScreen(PuyoGame game) {
        super(game);
        this.game = game;
        this.batch = new SpriteBatch();
        this.font = new BitmapFont(); // later replace with TTF
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
        font.getData().setScale(2f);
        
        // Title
        String title = "STORY MODE";
        float titleWidth = font.draw(batch, title, 0, 0).width;
        font.draw(batch, title, (GameViewport.VIRTUAL_WIDTH - titleWidth) / 2f, GameViewport.VIRTUAL_HEIGHT - 150);

        font.getData().setScale(1.2f);
        
        int unlocked = storyManager.getUnlockedStageCount();
        int total = storyManager.getTotalStages();

        float startY = GameViewport.VIRTUAL_HEIGHT / 2f + 200;
        float lineHeight = 60;
        
        for (int i = 0; i < total; i++) {
            StageData stage = storyManager.getStageAt(i);
            if (stage == null) continue;
            
            boolean isUnlocked = i < unlocked;
            String status = isUnlocked ? "UNLOCKED" : "LOCKED";
            String label = (i + 1) + ". " + stage.opponent + " (" + status + ")";
            
            if (i == selectedIndex) {
                label = "> " + label + " <";
                font.setColor(1, 1, 0, 1);
            } else {
                font.setColor(isUnlocked ? 1 : 0.5f, isUnlocked ? 1 : 0.5f, isUnlocked ? 1 : 0.5f, 1);
            }
            
            if (stage.dialogue != null && stage.dialogue.length > 0) {
                String dialogueText = "   \"" + stage.dialogue[0] + "\"";
                float labelWidth = font.draw(batch, label, 0, 0).width;
                font.draw(batch, label, (GameViewport.VIRTUAL_WIDTH - labelWidth) / 2f, startY - i * lineHeight);
                float dialogueWidth = font.draw(batch, dialogueText, 0, 0).width;
                font.draw(batch, dialogueText, (GameViewport.VIRTUAL_WIDTH - dialogueWidth) / 2f, startY - i * lineHeight - 30);
            } else {
                float labelWidth = font.draw(batch, label, 0, 0).width;
                font.draw(batch, label, (GameViewport.VIRTUAL_WIDTH - labelWidth) / 2f, startY - i * lineHeight);
            }
            font.setColor(1, 1, 1, 1);
        }

        // Instructions
        String instructions = "UP/DOWN: Select  ENTER: Start  BACK: Return";
        float instrWidth = font.draw(batch, instructions, 0, 0).width;
        font.draw(batch, instructions, (GameViewport.VIRTUAL_WIDTH - instrWidth) / 2f, 100);
        
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
