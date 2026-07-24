package com.puyo.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.puyo.game.PuyoGame;
import com.puyo.game.GameMode;
import com.puyo.game.story.StoryModeManager;
import com.puyo.game.story.StageData;

public class PlayScreen extends BaseScreen {
    private final PuyoGame game;
    private final GameMode mode;
    private final StoryModeManager storyManager;
    private int storyStageIndex = -1; // -1 indicates not in story mode or not using specific stage

    public PlayScreen(PuyoGame game, GameMode mode) {
        this(game, mode, -1);
    }

    public PlayScreen(PuyoGame game, GameMode mode, int storyStageIndex) {
        super(game);
        this.game = game;
        this.mode = mode;
        this.storyStageIndex = storyStageIndex;
        if (mode == GameMode.NORMAL && storyStageIndex >= 0) {
            this.storyManager = new StoryModeManager(storyStageIndex);
        } else {
            this.storyManager = new StoryModeManager(); // default start at 0
        }
    }

    @Override
    public void show() {
        Gdx.app.log("PlayScreen", "Play Screen Shown - Mode: " + mode);
        if (mode == GameMode.NORMAL) {
            StageData current = storyManager.getCurrentStage();
            if (current != null) {
                Gdx.app.log("PlayScreen", "Starting Stage " + storyManager.getCurrentStageNumber()
                        + "/" + storyManager.getTotalStages()
                        + " vs " + current.opponent);
            }
        }
    }

    @Override
    public void render(float delta) {
        // clear screen with color based on mode
        if (mode == GameMode.NORMAL) {
            Gdx.gl.glClearColor(0.2f, 0.2f, 0.4f, 1); // bluish for story
        } else if (mode == GameMode.ENDLESS) {
            Gdx.gl.glClearColor(0.1f, 0.3f, 0.1f, 1); // greenish for endless
        } else {
            Gdx.gl.glClearColor(0.3f, 0.1f, 0.1f, 1); // reddish for vs
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // TODO: actual game logic here
        // For demo, we can show some info
        // In real implementation, we would update game world, render puyos, etc.

        // For now just clear and log occasionally
        if (Gdx.graphics.getDeltaTime() > 0) {
            // just to avoid spamming log
        }
    }

    // Methods to be called by game logic when player wins/loses a match
    public void onPlayerWinMatch() {
        if (mode == GameMode.NORMAL) {
            storyManager.onPlayerWin();
            Gdx.app.log("PlayStorage", "Player won match. Stage wins: "
                    + storyManager.getCurrentStageNumber()
                    + "/" + storyManager.getTotalStages());
            // If story completed, we could transition to completion screen
            if (storyManager.isStoryComplete()) {
                // TODO: transition to story complete screen
                Gdx.app.log("PlayStorage", "Story completed!");
            }
        }
    }

    public void onPlayerLoseMatch() {
        if (mode == GameMode.NORMAL) {
            storyManager.onPlayerLose();
            Gdx.app.log("PlayStorage", "Player lost match. Resetting win count for stage.");
        }
    }

    @Override
    public void resize(int width, int height) {
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
    }
}
