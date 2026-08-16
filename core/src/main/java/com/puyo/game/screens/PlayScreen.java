package com.puyo.game.screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.puyo.game.GameMode;
import com.puyo.game.PuyoGame;
import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.engine.GameWorld;
import com.puyo.game.logic.engine.FallingPuyo;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoColor;
import com.puyo.game.logic.model.PuyoPair;

import java.util.List;
import com.puyo.game.story.StoryModeManager;
import com.puyo.game.story.StageData;
import com.puyo.game.config.GameViewport;
import com.puyo.game.graphics.FontManager;
import com.puyo.game.input.InputHandler;
import com.puyo.game.input.TouchController;

public class PlayScreen extends BaseScreen {
    private final PuyoGame game;
    private final GameMode mode;
    private final StoryModeManager storyManager;
    private final GameWorld gameWorld;
    private float stateTime = 0f;
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;
    private final FontManager fontManager;
    private InputHandler inputHandler;

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

        // 뷰포트/카메라 초기화
        initViewport();

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();

        // 입력 핸들러 초기화 (데스크톱: 키보드만, 모바일: 터치 컨트롤러 추가)
        // Android 플랫폼 감지
        boolean isAndroid = "Android".equals(com.badlogic.gdx.Gdx.app.getType().name());
        if (isAndroid) {
            TouchController touchController = new TouchController();
            inputHandler = new InputHandler(touchController);
        } else {
            inputHandler = new InputHandler();
        }
        Gdx.input.setInputProcessor(inputHandler);
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
        drawFallingPuyos();
        drawNextPair();
        drawUI();
    }

    private void update(float delta) {
        if (gameWorld.isGameOver()) {
            // 게임 오버 시 리스타트 처리 (ENTER 키)
            if (inputHandler != null && inputHandler.isHardDropPressed()) {
                game.setScreen(new PlayScreen(game, mode));
            }
            return;
        }
        stateTime += delta;

        // 입력 처리 (InputHandler를 통해 키보드/터치 통합)
        if (inputHandler != null) {
            // 현재 Phase 확인
            com.puyo.game.logic.engine.GameWorld.GamePhase phase = gameWorld.getGamePhase();
            
            // FALLING_AUTO, LOCK_DELAY에서만 조작 허용
            boolean allowInput = (phase == com.puyo.game.logic.engine.GameWorld.GamePhase.FALLING_AUTO 
                               || phase == com.puyo.game.logic.engine.GameWorld.GamePhase.LOCK_DELAY);

            // 회전
            if (allowInput && inputHandler.isRotatePressed()) {
                gameWorld.rotateClockwise();
            }

            inputHandler.update();

            // 좌우 이동
            if (allowInput) {
                int moveDir = inputHandler.getMoveDirection();
                if (moveDir < 0) {
                    gameWorld.moveLeft();
                } else if (moveDir > 0) {
                    gameWorld.moveRight();
                }
            }

            // 소프트 드롭 - Phase 체크 추가 + 락딜레이 중이면 move 기록
            if (allowInput && inputHandler.isDropPressed()) {
                if (gameWorld.canFall() && gameWorld.getCurrentPair() != null) {
                    gameWorld.getCurrentPair().moveDown();
                    if (phase == com.puyo.game.logic.engine.GameWorld.GamePhase.LOCK_DELAY) {
                        gameWorld.recordLockDelayMove();
                    }
                }
            }

            // 하드 드롭
            if (allowInput && inputHandler.isHardDropPressed()) {
                gameWorld.hardDrop();
            }
        }

        gameWorld.update(delta);
    }

    private void drawBoard() {
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Board background
        shapeRenderer.setColor(0.1f, 0.1f, 0.15f, 1);
        shapeRenderer.rect(
                GameViewport.Single.BOARD_OFFSET_X,
                GameViewport.Single.BOARD_OFFSET_Y,
                GameViewport.BOARD_WIDTH,
                GameViewport.BOARD_HEIGHT);

        // Grid lines
        shapeRenderer.setColor(0.2f, 0.2f, 0.3f, 1);
        for (int x = 1; x < Board.WIDTH; x++) {
            float lineX = GameViewport.Single.BOARD_OFFSET_X + x * GameViewport.CELL_SIZE;
            shapeRenderer.rectLine(lineX, GameViewport.Single.BOARD_OFFSET_Y, lineX,
                    GameViewport.Single.BOARD_OFFSET_Y + GameViewport.BOARD_HEIGHT, 1f);
        }
        for (int y = 1; y < Board.HEIGHT; y++) {
            float lineY = GameViewport.Single.BOARD_OFFSET_Y + y * GameViewport.CELL_SIZE;
            shapeRenderer.rectLine(GameViewport.Single.BOARD_OFFSET_X, lineY,
                    GameViewport.Single.BOARD_OFFSET_X + GameViewport.BOARD_WIDTH, lineY, 1f);
        }

        shapeRenderer.end();

        // Draw placed puyos
        Board board = gameWorld.getBoard();
        batch.begin();
        for (int y = 0; y < Board.HEIGHT; y++) {
            for (int x = 0; x < Board.WIDTH; x++) {
                Puyo puyo = board.getPuyoAt(x, y);
                if (puyo != null) {
                    drawPuyo(puyo, GameViewport.Single.BOARD_OFFSET_X + x * GameViewport.CELL_SIZE,
                            GameViewport.Single.BOARD_OFFSET_Y + y * GameViewport.CELL_SIZE);
                }
            }
        }
        batch.end();
    }

    private void drawCurrentPair() {
        PuyoPair pair = gameWorld.getCurrentPair();
        if (pair == null)
            return;

        batch.begin();
        // PuyoPair의 left/right puyo는 이미 board 좌표를 가지고 있음
        Puyo left = pair.getLeft();
        Puyo right = pair.getRight();

        drawPuyo(left,
                GameViewport.Single.BOARD_OFFSET_X + left.getX() * GameViewport.CELL_SIZE,
                GameViewport.Single.BOARD_OFFSET_Y + left.getY() * GameViewport.CELL_SIZE);
        drawPuyo(right,
                GameViewport.Single.BOARD_OFFSET_X + right.getX() * GameViewport.CELL_SIZE,
                GameViewport.Single.BOARD_OFFSET_Y + right.getY() * GameViewport.CELL_SIZE);
        batch.end();
    }

    private void drawFallingPuyos() {
        List<FallingPuyo> fallingPuyos = gameWorld.getFallingPuyos();
        if (fallingPuyos == null || fallingPuyos.isEmpty())
            return;

        batch.begin();
        for (FallingPuyo fp : fallingPuyos) {
            Puyo puyo = fp.puyo;
            drawPuyo(puyo,
                    GameViewport.Single.BOARD_OFFSET_X + puyo.getX() * GameViewport.CELL_SIZE,
                    GameViewport.Single.BOARD_OFFSET_Y + puyo.getY() * GameViewport.CELL_SIZE);
        }
        batch.end();
    }

    private void drawNextPair() {
        PuyoPair next = gameWorld.getNextPair();
        if (next == null)
            return;

        batch.begin();
        // 새로운 싱글 레이아웃: 사이드 패널의 NEXT_PREVIEW 위치 사용
        float nextX = GameViewport.Single.NEXT_PREVIEW_X;
        float nextY = GameViewport.Single.NEXT_PREVIEW_Y;

        // Next pair is shown in preview area (not on board)
        Puyo left = next.getLeft();
        Puyo right = next.getRight();

        drawPuyo(left, nextX, nextY);
        drawPuyo(right, nextX, nextY + GameViewport.CELL_SIZE);
        batch.end();
    }

    private void drawPuyo(Puyo puyo, float x, float y) {
        Color color = getColorForPuyo(puyo.getColor());
        float scale = puyo.getPopScale(); // 팝 애니메이션 스케일 적용
        float radius = (GameViewport.CELL_SIZE / 2f - 2) * scale;

        // 스케일이 0이면 그리지 않음 (완전히 사라짐)
        if (radius <= 0)
            return;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(color);
        shapeRenderer.circle(x + GameViewport.CELL_SIZE / 2f, y + GameViewport.CELL_SIZE / 2f,
                radius);
        shapeRenderer.end();

        // Draw highlight
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(Color.WHITE);
        shapeRenderer.circle(x + GameViewport.CELL_SIZE / 2f, y + GameViewport.CELL_SIZE / 2f,
                radius);
        shapeRenderer.end();
    }

    private void drawUI() {
        batch.begin();

        // UI 폰트 (24px)
        BitmapFont uiFont = fontManager.getUIFont(24);
        uiFont.getData().setScale(1f);

        // Score
        uiFont.draw(batch, "SCORE: " + gameWorld.getScore(),
                GameViewport.Single.UI_X, GameViewport.VIRTUAL_HEIGHT - 50);

        // Chain
        if (gameWorld.getCurrentChain() > 0) {
            uiFont.setColor(Color.YELLOW);
            uiFont.draw(batch, "CHAIN: " + gameWorld.getCurrentChain(),
                    GameViewport.Single.UI_X, GameViewport.VIRTUAL_HEIGHT - 90);
            uiFont.setColor(Color.WHITE);
        }

        // Next label
        uiFont.draw(batch, "NEXT",
                GameViewport.Single.NEXT_PREVIEW_X,
                GameViewport.Single.NEXT_PREVIEW_Y + GameViewport.CELL_SIZE + 20);

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
                uiFont.draw(batch,
                        "STAGE: " + storyManager.getCurrentStageNumber() + "/" + storyManager.getTotalStages(),
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
            case RED:
                return Color.RED;
            case GREEN:
                return Color.GREEN;
            case BLUE:
                return Color.BLUE;
            case YELLOW:
                return Color.YELLOW;
            case PURPLE:
                return Color.MAGENTA;
            case OJAMA:
                return Color.GRAY;
            case HARD:
                return Color.BLACK;
            default:
                return Color.WHITE;
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
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (shapeRenderer != null)
            shapeRenderer.dispose();
        if (batch != null)
            batch.dispose();
        // 폰트는 FontManager가 관리하므로 여기서 dispose 하지 않음
    }
}
