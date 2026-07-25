package com.puyo.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.InputProcessor;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.puyo.game.GameMode;
import com.puyo.game.PuyoGame;
import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.engine.GameWorld;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoColor;
import com.puyo.game.logic.model.PuyoPair;
import com.puyo.game.story.StoryModeManager;
import com.puyo.game.story.StageData;

public class PlayScreen extends BaseScreen implements InputProcessor {
    private final PuyoGame game;
    private static final int CELL_SIZE = 32;
    private int boardOffsetX;
    private int boardOffsetY;

    private final GameMode mode;
    private final StoryModeManager storyManager;
    private final GameWorld gameWorld;
    private boolean isInitialized = false;
    private float stateTime = 0f;
    private ShapeRenderer shapeRenderer;

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
        Gdx.input.setInputProcessor(this);
        shapeRenderer = new ShapeRenderer();
        // Compute board offsets based on current screen size
        boardOffsetX = (Gdx.graphics.getWidth() - Board.WIDTH * CELL_SIZE) / 2;
        boardOffsetY = (Gdx.graphics.getHeight() - Board.HEIGHT * CELL_SIZE) / 2;
    }

    @Override
    public void render(float delta) {
        // Update game logic
        update(delta);

        // Clear screen
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Render the game world
        if (mode == GameMode.NORMAL) {
            Gdx.gl.glClearColor(0.2f, 0.2f, 0.4f, 1); // bluish for story
        } else if (mode == GameMode.ENDLESS) {
            Gdx.gl.glClearColor(0.1f, 0.3f, 0.1f, 1); // greenish for endless
        } else {
            Gdx.gl.glClearColor(0.3f, 0.1f, 0.1f, 1); // reddish for vs
        }
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // Draw board grid
        drawBoard();

        // Draw current pair
        drawCurrentPair();

        // Draw next pair (optional)
        drawNextPair();

        // Log debug info
        stateTime += delta;
        if (stateTime > 1f) {
            stateTime = 0f;
            if (gameWorld.getCurrentPair() != null) {
                Gdx.app.log("PlayScreen", "Pair at: (" + gameWorld.getCurrentPair().getLeft().getX() + "," + gameWorld.getCurrentPair().getLeft().getY() + ") and ("
                        + gameWorld.getCurrentPair().getRight().getX() + "," + gameWorld.getCurrentPair().getRight().getY() + ")");
            }
        }
    }

    private void drawBoard() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.1f, 0.1f, 0.1f, 1f); // dark background

        // Draw filled cells for occupied positions
        for (int x = 0; x < Board.WIDTH; x++) {
            for (int y = 0; y < Board.HEIGHT; y++) {
                Puyo puyo = gameWorld.getBoard().getPuyoAt(x, y);
                if (puyo != null) {
                    float screenX = boardOffsetX + x * CELL_SIZE;
                    float screenY = boardOffsetY + y * CELL_SIZE;
                    shapeRenderer.rect(screenX, screenY, CELL_SIZE, CELL_SIZE);
                }
            }
        }
        shapeRenderer.end();

        // Draw grid lines
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);

        // Vertical lines
        for (int x = 0; x <= Board.WIDTH; x++) {
            float screenX = boardOffsetX + x * CELL_SIZE;
            shapeRenderer.line(screenX, boardOffsetY, screenX, boardOffsetY + Board.HEIGHT * CELL_SIZE);
        }

        // Horizontal lines
        for (int y = 0; y <= Board.HEIGHT; y++) {
            float screenY = boardOffsetY + y * CELL_SIZE;
            shapeRenderer.line(boardOffsetX, screenY, boardOffsetX + Board.WIDTH * CELL_SIZE, screenY);
        }

        shapeRenderer.end();
    }

    private void drawCurrentPair() {
        PuyoPair pair = gameWorld.getCurrentPair();
        if (pair == null) return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Draw left puyo
        Puyo left = pair.getLeft();
        if (left != null && left.isAlive()) {
            float screenX = boardOffsetX + left.getX() * CELL_SIZE + CELL_SIZE / 2f;
            float screenY = boardOffsetY + left.getY() * CELL_SIZE + CELL_SIZE / 2f;
            shapeRenderer.setColor(getColorForPuyo(left.getColor()));
            shapeRenderer.circle(screenX, screenY, CELL_SIZE / 2f - 1);
        }

        // Draw right puyo
        Puyo right = pair.getRight();
        if (right != null && right.isAlive()) {
            float screenX = boardOffsetX + right.getX() * CELL_SIZE + CELL_SIZE / 2f;
            float screenY = boardOffsetY + right.getY() * CELL_SIZE + CELL_SIZE / 2f;
            shapeRenderer.setColor(getColorForPuyo(right.getColor()));
            shapeRenderer.circle(screenX, screenY, CELL_SIZE / 2f - 1);
        }

        shapeRenderer.end();

        // Draw outline for visibility
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1f, 1f, 1f, 0.8f);

        if (left != null && left.isAlive()) {
            float screenX = boardOffsetX + left.getX() * CELL_SIZE + CELL_SIZE / 2f;
            float screenY = boardOffsetY + left.getY() * CELL_SIZE + CELL_SIZE / 2f;
            shapeRenderer.circle(screenX, screenY, CELL_SIZE / 2f - 1);
        }

        if (right != null && right.isAlive()) {
            float screenX = boardOffsetX + right.getX() * CELL_SIZE + CELL_SIZE / 2f;
            float screenY = boardOffsetY + right.getY() * CELL_SIZE + CELL_SIZE / 2f;
            shapeRenderer.circle(screenX, screenY, CELL_SIZE / 2f - 1);
        }

        shapeRenderer.end();
    }

    private void drawNextPair() {
        PuyoPair nextPair = gameWorld.getNextPair();
        if (nextPair == null) return;

        float offsetX = boardOffsetX - 100;
        float offsetY = boardOffsetY + 50;
        float size = CELL_SIZE / 2f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        Puyo left = nextPair.getLeft();
        if (left != null && left.isAlive()) {
            shapeRenderer.setColor(getColorForPuyo(left.getColor()));
            shapeRenderer.circle(offsetX, offsetY, size);
        }

        Puyo right = nextPair.getRight();
        if (right != null && right.isAlive()) {
            shapeRenderer.setColor(getColorForPuyo(right.getColor()));
            shapeRenderer.circle(offsetX + size * 1.5f, offsetY, size);
        }

        shapeRenderer.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(1f, 1f, 1f, 0.8f);

        if (left != null && left.isAlive()) {
            shapeRenderer.circle(offsetX, offsetY, size);
        }

        if (right != null && right.isAlive()) {
            shapeRenderer.circle(offsetX + size * 1.5f, offsetY, size);
        }

        shapeRenderer.end();
    }

    private Color getColorForPuyo(PuyoColor color) {
        switch (color) {
            case RED: return Color.RED;
            case GREEN: return Color.GREEN;
            case BLUE: return Color.BLUE;
            case YELLOW: return Color.YELLOW;
            case PURPLE: return Color.PURPLE;
            case CYAN: return Color.CYAN;
            case OJAMA: return Color.DARK_GRAY;
            case HARD: return Color.BLACK;
            default: return Color.WHITE;
        }
    }

    private Vector2 worldToScreen(int col, int row) {
        return new Vector2(boardOffsetX + col * CELL_SIZE, boardOffsetY + row * CELL_SIZE);
    }

    private void update(float delta) {
        if (gameWorld.isGameOver()) {
            // Handle game over: maybe show a screen
            return;
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
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }

    // InputProcessor implementation
    @Override
    public boolean keyDown(int keycode) {
        if (gameWorld.isGameOver()) {
            return false;
        }
        switch (keycode) {
            case Input.Keys.LEFT:
                gameWorld.moveLeft();
                return true;
            case Input.Keys.RIGHT:
                gameWorld.moveRight();
                return true;
            case Input.Keys.UP:
                // Rotate clockwise
                gameWorld.getCurrentPair().rotateClockwise();
                return true;
            case Input.Keys.DOWN:
                // Soft drop
                if (gameWorld.canFall()) {
                    gameWorld.getCurrentPair().moveDown();
                }
                return true;
            case Input.Keys.SPACE:
                // Hard drop
                gameWorld.hardDrop();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean keyUp(int keycode) {
        return false;
    }

    @Override
    public boolean keyTyped(char character) {
        return false;
    }

    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
        return false;
    }

    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) {
        return false;
    }

    @Override
    public boolean mouseMoved(int screenX, int screenY) {
        return false;
    }

    @Override
    public boolean scrolled(float amountX, float amountY) {
        return false;
    }

    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) {
        return false;
    }
}
