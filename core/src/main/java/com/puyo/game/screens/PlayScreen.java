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
import com.puyo.game.config.GameViewport;

public class PlayScreen extends BaseScreen implements InputProcessor {
    private final PuyoGame game;
    private final GameMode mode;
    private final StoryModeManager storyManager;
    private final GameWorld gameWorld;
    private float stateTime = 0f;
    private ShapeRenderer shapeRenderer;
    private BitmapFont font;
    private SpriteBatch batch;

    public PlayScreen(PuyoGame game, GameMode mode) {
        this(game, mode, -1);
    }

    public PlayScreen(PuyoGame game, GameMode mode, int storyStageIndex) {
        super(game);
        this.game = game;
        this.mode = mode;
        this.storyManager = (mode == GameMode.NORMAL) ? new StoryModeManager(storyStageIndex) : null;
        this.gameWorld = new GameWorld();
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
            }
        }
        Gdx.input.setInputProcessor(this);
        
        // 뷰포트/카메라 초기화
        initViewport();
        
        shapeRenderer = new ShapeRenderer();
        font = new BitmapFont();
        batch = new SpriteBatch();
    }

    @Override
    public void render(float delta) {
        // Update game logic
        update(delta);

        // Clear screen
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 카메라 업데이트 및 프로젝션 매트릭스 적용
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // Draw game board
        drawBoard();
        drawCurrentPair();
        drawNextPair();
        drawUI();
    }

    private void update(float delta) {
        if (gameWorld.isGameOver()) {
            return;
        }
        stateTime += delta;
        gameWorld.update(delta);
    }

    private void drawBoard() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        
        // Board background
        shapeRenderer.setColor(0.1f, 0.1f, 0.15f, 1);
        shapeRenderer.rect(
            GameViewport.BOARD_OFFSET_X,
            GameViewport.BOARD_OFFSET_Y,
            GameViewport.BOARD_WIDTH,
            GameViewport.BOARD_HEIGHT
        );

        // Grid lines
        shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 1);
        for (int x = 0; x <= Board.WIDTH; x++) {
            float lineX = GameViewport.BOARD_OFFSET_X + x * GameViewport.CELL_SIZE;
            shapeRenderer.rectLine(lineX, GameViewport.BOARD_OFFSET_Y, 
                lineX, GameViewport.BOARD_OFFSET_Y + GameViewport.BOARD_HEIGHT, 1f);
        }
        for (int y = 0; y <= Board.HEIGHT; y++) {
            float lineY = GameViewport.BOARD_OFFSET_Y + y * GameViewport.CELL_SIZE;
            shapeRenderer.rectLine(GameViewport.BOARD_OFFSET_X, lineY, 
                GameViewport.BOARD_OFFSET_X + GameViewport.BOARD_WIDTH, lineY, 1f);
        }
        shapeRenderer.end();

        // Draw placed puyos
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        Board board = gameWorld.getBoard();
        for (int x = 0; x < Board.WIDTH; x++) {
            for (int y = 0; y < Board.HEIGHT; y++) {
                Puyo puyo = board.getPuyoAt(x, y);
                if (puyo != null && puyo.isAlive()) {
                    drawPuyo(puyo.getX(), puyo.getY(), puyo.getColor());
                }
            }
        }
        shapeRenderer.end();
    }

    private void drawCurrentPair() {
        PuyoPair pair = gameWorld.getCurrentPair();
        if (pair != null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            for (Puyo puyo : pair.getPuyos()) {
                if (puyo.isAlive()) {
                    drawPuyo(puyo.getX(), puyo.getY(), puyo.getColor());
                }
            }
            shapeRenderer.end();
        }
    }

    private void drawNextPair() {
        PuyoPair nextPair = gameWorld.getNextPair();
        if (nextPair != null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            // Next pair preview area (right side of board)
            float previewX = GameViewport.BOARD_OFFSET_X + GameViewport.BOARD_WIDTH + 40;
            float previewY = GameViewport.BOARD_OFFSET_Y + GameViewport.BOARD_HEIGHT - 200;
            
            shapeRenderer.setColor(0.3f, 0.3f, 0.4f, 1);
            shapeRenderer.rect(previewX - 20, previewY - 20, GameViewport.CELL_SIZE * 2 + 40, GameViewport.CELL_SIZE * 2 + 40);
            
            for (Puyo puyo : nextPair.getPuyos()) {
                drawPuyoAt(previewX + puyo.getX() * GameViewport.CELL_SIZE, 
                          previewY + puyo.getY() * GameViewport.CELL_SIZE, 
                          puyo.getColor());
            }
            shapeRenderer.end();

            batch.begin();
            font.draw(batch, "NEXT", previewX, previewY + GameViewport.CELL_SIZE * 2 + 40);
            batch.end();
        }
    }

    private void drawPuyo(int boardX, int boardY, PuyoColor color) {
        float screenX = GameViewport.BOARD_OFFSET_X + boardX * GameViewport.CELL_SIZE;
        float screenY = GameViewport.BOARD_OFFSET_Y + boardY * GameViewport.CELL_SIZE;
        drawPuyoAt(screenX, screenY, color);
    }

    private void drawPuyoAt(float x, float y, PuyoColor color) {
        float radius = GameViewport.CELL_SIZE / 2f - 2;
        float centerX = x + GameViewport.CELL_SIZE / 2f;
        float centerY = y + GameViewport.CELL_SIZE / 2f;

        shapeRenderer.setColor(getColorForPuyo(color));
        shapeRenderer.circle(centerX, centerY, radius);

        // Highlight
        shapeRenderer.setColor(1, 1, 1, 0.3f);
        shapeRenderer.circle(centerX - radius * 0.3f, centerY + radius * 0.3f, radius * 0.4f);
    }

    private void drawUI() {
        batch.begin();
        font.getData().setScale(2f);
        
        // Score
        font.draw(batch, "SCORE: " + gameWorld.getScore(), 
            GameViewport.BOARD_OFFSET_X, GameViewport.BOARD_OFFSET_Y + GameViewport.BOARD_HEIGHT + 80);
        
        // Chain
        if (gameWorld.getCurrentChain() > 0) {
            font.setColor(Color.YELLOW);
            font.draw(batch, "CHAIN: " + gameWorld.getCurrentChain() + "x!", 
                GameViewport.VIRTUAL_WIDTH / 2f - 100, GameViewport.VIRTUAL_HEIGHT - 100);
            font.setColor(Color.WHITE);
        }

        // Story mode info
        if (mode == GameMode.NORMAL && storyManager != null) {
            StageData current = storyManager.getCurrentStage();
            if (current != null) {
                font.draw(batch, "STAGE: " + storyManager.getCurrentStageNumber() + "/" + storyManager.getTotalStages(), 
                    GameViewport.VIRTUAL_WIDTH - 300, GameViewport.VIRTUAL_HEIGHT - 50);
                font.draw(batch, "VS: " + current.opponent, 
                    GameViewport.VIRTUAL_WIDTH - 300, GameViewport.VIRTUAL_HEIGHT - 90);
            }
        }
        
        font.getData().setScale(1f);
        batch.end();
    }

    private Color getColorForPuyo(PuyoColor color) {
        switch (color) {
            case RED: return Color.RED;
            case GREEN: return Color.GREEN;
            case BLUE: return Color.BLUE;
            case YELLOW: return Color.YELLOW;
            case PURPLE: return Color.MAGENTA;
            case CYAN: return Color.CYAN;
            case OJAMA: return Color.GRAY;
            case HARD: return Color.BLACK;
            default: return Color.WHITE;
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
                gameWorld.getCurrentPair().rotateClockwise();
                return true;
            case Input.Keys.DOWN:
                if (gameWorld.canFall()) {
                    gameWorld.getCurrentPair().moveDown();
                }
                return true;
            case Input.Keys.SPACE:
                gameWorld.hardDrop();
                return true;
            default:
                return false;
        }
    }

    @Override
    public boolean keyUp(int keycode) { return false; }
    @Override
    public boolean keyTyped(char character) { return false; }
    @Override
    public boolean touchDown(int screenX, int screenY, int pointer, int button) { return false; }
    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) { return false; }
    @Override
    public boolean touchDragged(int screenX, int screenY, int pointer) { return false; }
    @Override
    public boolean mouseMoved(int screenX, int screenY) { return false; }
    @Override
    public boolean scrolled(float amountX, float amountY) { return false; }
    @Override
    public boolean touchCancelled(int screenX, int screenY, int pointer, int button) { return false; }

    @Override
    public void resize(int width, int height) {
        super.resize(width, height);
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (font != null) font.dispose();
        if (batch != null) batch.dispose();
    }
}
