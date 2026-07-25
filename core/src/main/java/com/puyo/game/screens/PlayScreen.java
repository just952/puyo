package com.puyo.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.puyo.game.GameMode;
import com.puyo.game.PuyoGame;
import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.engine.GameWorld;
import com.puyo.game.logic.model.PuyoColor;
import com.puyo.game.story.StoryModeManager;
import com.puyo.game.story.StageData;

public class PlayScreen extends BaseScreen {
    private final PuyoGame game;
    private final GameMode mode;
    private final StoryModeManager storyManager;
    private final GameWorld gameWorld;
    private boolean isInitialized = false;
    private float stateTime = 0f;

    public PlayScreen(PuyoGame game, GameMode mode) {
        this(game, mode, -1);
    }

    public PlayScreen(PuyoGame game, GameMode mode, int storyStageIndex) {
        super(game);
        this.game = game;
        this.mode = mode;
        this.storyManager = (mode == GameMode.NORMAL) ? new StoryModeManager(storyStageIndex) : null;
        this.gameWorld = new GameWorld();

        // If we are in story mode and a specific stage index was given, set the storyMode.currentStageIndex to that
        // The StoryModeManager constructor with startIndex already set it.
    }

    @Override
    public void show() {
        Gdx.app.log("PlayScreen", "Play Screen Shown - Mode: " + mode);
        if (mode == GameMode.NORMAL && storyManager != null) {
            StageData current = storyManager.getCurrentStage();
            if (current != null) {
                Gdx.app.log("PlayScreen", "Starting Stage " + storyManager.getCurrentStageNumber()
                        + "/" + storyManager.getTotalStages()
                        + " vs " + current.opponent);
                // Apply story mode settings to game world (e.g., adjust drop speed based on AI difficulty?)
                // For now, we just note it.
            }
        }
        // Reset game world for a fresh game
        // Note: GameWorld constructor already creates a new board and spawnNew ㅈ we want to keep the same gameWorld instance, but we need to reset it.
        // We'll create a new one each time? Or reset the existing one.
        // For simplicity, we'll create a new GameWorld in the constructor and not reset.
        // But if we want to replay, we need a reset method. We'll skip for now.
    }

    @Override
    public void render(float delta) {
        // Update game logic
        update(delta);

        // Clear screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Render the game world (for now, just clear and log)
        // In a full implementation, we would draw the board, current pair, next pair, score, etc.
        // We'll leave the rendering as a stub for now, but we can at least clear to a color based on mode.
        if (mode == GameMode.NORMAL) {
            Gdx.gl.glClearColor(0.2f, 0.2f, 0.4f, 1); // bluish for story
        } else if (mode == GameMode.ENDLESS) {
            Gdx.gl.glClearColor(0.1f, 0.3f, 0.1f, 1); // greenish for endless
        } else {
            Gdx.gl.glClearColor(0.3f, 0.1f, 0.1f, 1); // reddish for vs
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // TODO: Actual rendering of the game field, pieces, UI, etc.
        // For now, we just log the state occasionally to see if it's working.
        stateTime += delta;
        if (stateTime > 1f) {
            stateTime = 0f;
            // Log some debug info
            if (gameWorld.getCurrentPair() != null) {
                Gdx.app.log("PlayScreen", "Pair at: (" + gameWorld.getCurrentPair().getLeft().getX() + "," + gameWorld.getCurrentPair().getLeft().getY() + ") and ("
                        + gameWorld.getCurrentPair().getRight().getX() + "," + gameWorld.getCurrentPair().getRight().getY() + ")");
            }
        }
    }

    private void update(float delta) {
        if (gameWorld.isGameOver()) {
            // Handle game over: maybe show a screen
            return;
        }

        // Process input
        if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
            gameWorld.moveLeft();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
            gameWorld.moveRight();
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.UP)) {
            // Rotate
            gameWorld.getCurrentPair().rotateClockwise(); // We don't have rotate in PuyoPair yet, but we can add later
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.DOWN)) {
            // Soft drop
            gameWorld.softDrop(); // We don't have softDrop in GameWorld yet; we have moveDown and canFall
            // We'll add a method to GameWorld for soft drop, or just call moveDown if canFall
            if (gameWorld.canFall()) {
                gameWorld.getCurrentPair().moveDown();
            }
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            // Hard drop
            gameWorld.hardDrop();
        }

        // Update game world logic (auto-fall, locking, clearing)
        gameWorld.update(delta);
    }

    // Helper methods to delegate to gameWorld
    public boolean canFall() {
        return gameWorld.canFall();
    }

    public void softDrop() {
        if (canFall()) {
            gameWorld.getCurrentPair().moveDown();
        }
    }

    public void hardDrop() {
        gameWorld.hardDrop();
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
