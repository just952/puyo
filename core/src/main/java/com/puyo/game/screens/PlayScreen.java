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
import com.puyo.game.graphics.FontManager;

public class PlayScreen extends BaseScreen implements InputProcessor {
    private final PuyoGame game;
    private final GameMode mode;
    private final StoryModeManager storyManager;
    private final GameWorld gameWorld;
    private float stateTime = 0f;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private final FontManager fontManager;

    public PlayScreen(PuyoGame game, GameMode mode) {
        this(game, mode, -1);
    }

    public PlayScreen(PuyoGame game, GameMode mode, int storyStageIndex) {
        super(game);
        this.game = game;
        this.mode = mode;
        this.storyManager = (mode == GameMode.NORMAL) ? new StoryModeManager(storyStageIndex) : null;
        this.gameWorld = new GameWorld();
        this.fontManager = FontManager.getInstance();
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
        for (int x = 1; x < Board.WIDTH; x++) {
            float lineX = GameViewport.BOARD_OFFSET_X + x * GameViewport.CELL_SIZE;
            shapeRenderer.rectLine(lineX, GameViewport.BOARD_OFFSET_Y, lineX, GameViewport.BOARD_OFFSET_Y + GameViewport.BOARD_HEIGHT, 1f);
        }
        for (int y = 1; y < Board.HEIGHT; y++) {
            float lineY = GameViewport.BOARD_OFFSET_Y + y * GameViewport.CELL_SIZE;
            shapeRenderer.rectLine(GameViewport.BOARD_OFFSET_X, lineY, GameViewport.BOARD_OFFSET_X + GameViewport.BOARD_WIDTH, lineY, 1f);
        }
        
        shapeRenderer.end();
        
        // Draw placed puyos
        Board board = gameWorld.getBoard();
        batch.begin();
        for (int y = 0; y < Board.HEIGHT; y++) {
            for (int x = 0; x < Board.WIDTH; x++) {
                Puyo puyo = board.getPuyo(x, y);
                if (puyo != null) {
                    drawPuyo(puyo, GameViewport.BOARD_OFFSET_X + x * GameViewport.CELL_SIZE,
                        GameViewport.BOARD_OFFSET_Y + y * GameViewport.CELL_SIZE);
                }
            }
        }
        batch.end();
    }

    private void drawCurrentPair() {
        PuyoPair pair = gameWorld.getCurrentPair();
        if (pair == null) return;
        
        batch.begin();
        drawPuyo(pair.getPuyo1(), pair.getX1() * GameViewport.CELL_SIZE + GameViewport.BOARD_OFFSET_X,
            pair.getY1() * GameViewport.CELL_SIZE + GameViewport.BOARD_OFFSET_Y);
        drawPuyo(pair.getPuyo2(), pair.getX2() * GameViewport.CELL_SIZE + GameViewport.BOARD_OFFSET_X,
            pair.getY2() * GameViewport.CELL_SIZE + GameViewport.BOARD_OFFSET_Y);
        batch.end();
    }

    private void drawNextPair() {
        PuyoPair next = gameWorld.getNextPair();
        if (next == null) return;
        
        batch.begin();
        float nextX = GameViewport.BOARD_OFFSET_X + GameViewport.BOARD_WIDTH + 40;
        float nextY = GameViewport.BOARD_OFFSET_Y + GameViewport.BOARD_HEIGHT - 100;
        drawPuyo(next.getPuyo1(), nextX, nextY + GameViewport.CELL_SIZE);
        drawPuyo(next.getPuyo2(), nextX, nextY);
        batch.end();
    }

    private void drawPuyo(Puyo puyo, float x, float y) {
        Color color = getColorForPuyo(puyo.getColor());
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(color);
        shapeRenderer.circle(x + GameViewport.CELL_SIZE / 2f, y + GameViewport.CELL_SIZE / 2f, GameViewport.CELL_SIZE / 2f - 2);
        shapeRenderer.end();
        
        // Draw highlight
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(x + GameViewport.CELL_SIZE / 2f, y + GameViewport.CELL_SIZE / 2f, GameViewport.CELL_SIZE / 2f - 2);
        shapeRenderer.end();
    }

    private void drawUI() {
        batch.begin();
        
        // UI 폰트 (24px)
        BitmapFont uiFont = fontManager.getUIFont(24);
        uiFont.getData().setScale(1f);
        
        // Score
        uiFont.draw(batch, "SCORE: " + gameWorld.getScore(), 
            GameViewport.BOARD_OFFSET_X, GameViewport.VIRTUAL_HEIGHT - 50);
        
        // Chain
        if (gameWorld.getCurrentChain() > 0) {
            uiFont.setColor(Color.YELLOW);
            uiFont.draw(batch, "CHAIN: " + gameWorld.getCurrentChain(), 
                GameViewport.BOARD_OFFSET_X, GameViewport.VIRTUAL_HEIGHT - 90);
            uiFont.setColor(Color.WHITE);
        }
        
        // Next label
        uiFont.draw(batch, "NEXT", 
            GameViewport.BOARD_OFFSET_X + GameViewport.BOARD_WIDTH + 20,
            GameViewport.BOARD_OFFSET_Y + GameViewport.BOARD_HEIGHT - 120);
        
        // Game Over
        if (gameWorld.isGameOver()) {
            // 타이틀 폰트 (48px) 사용
            BitmapFont titleFont = fontManager.getTitleFont(48);
            titleFont.getData().setScale(1f);
            String msg = "GAME OVER";
            float w = titleFont.draw(batch, msg, 0, 0).width;
            titleFont.draw(batch, msg, 
                GameViewport.VIRTUAL_WIDTH / 2f - w / 2f, GameViewport.VIRTUAL_HEIGHT / 2f + 50);
            
            uiFont.draw(batch, "Press ENTER to restart", 
                GameViewport.VIRTUAL_WIDTH / 2f - 100, GameViewport.VIRTUAL_HEIGHT / 2f - 20);
        }

        // Story mode info
        if (mode == GameMode.NORMAL && storyManager != null) {
            StageData current = storyManager.getCurrentStage();
            if (current != null) {
                uiFont.draw(batch, "STAGE: " + storyManager.getCurrentStageNumber() + "/" + storyManager.getTotalStages(), 
                    GameViewport.VIRTUAL_WIDTH - 300, GameViewport.VIRTUAL_HEIGHT - 50);
                uiFont.draw(batch, "VS: " + current.opponent, 
                    GameViewport.VIRTUAL_WIDTH - 300, GameViewport.VIRTUAL_HEIGHT - 90);
            }
        }
        
        uiFont.getData().setScale(1f);
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
            case Input.Keys.ENTER:
                if (gameWorld.isGameOver()) {
                    game.setScreen(new PlayScreen(game, mode));
                }
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
        if (batch != null) batch.dispose();
        // 폰트는 FontManager가 관리하므로 여기서 dispose 하지 않음
    }
}
