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
import com.puyo.game.graphics.PuyoRenderer;
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
    private final PuyoRenderer puyoRenderer;
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
        this.puyoRenderer = new PuyoRenderer();
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
            // 새 조각 스폰 시 DAS/ARR 리셋 (키를 누른 채로 있어도 첫 프레임 즉시 이동 보장)
            if (gameWorld.isJustSpawned()) {
                inputHandler.resetDasArr();
            }

            // 현재 Phase 확인
            GameWorld.GamePhase phase = gameWorld.getGamePhase();
            
            // FALLING_AUTO, LOCK_DELAY에서만 조작 허용
            boolean allowInput = (phase == GameWorld.GamePhase.FALLING_AUTO 
                               || phase == GameWorld.GamePhase.LOCK_DELAY);

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

            // 소프트 드롭 - GameWorld.softDrop() 위임 (착지 시 즉시 잠금, 락딜레이 우회)
            if (allowInput && inputHandler.isDropPressed()) {
                gameWorld.softDrop();
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

        // Draw placed puyos with connection effects
        Board board = gameWorld.getBoard();
        batch.begin();
        for (int y = 0; y < Board.HEIGHT; y++) {
            for (int x = 0; x < Board.WIDTH; x++) {
                Puyo puyo = board.getPuyoAt(x, y);
                if (puyo != null) {
                    // 연결 효과 포함 그리기
                    puyoRenderer.drawConnected(batch, board, x, y, puyo.getColor(),
                            GameViewport.Single.BOARD_OFFSET_X + x * GameViewport.CELL_SIZE,
                            GameViewport.Single.BOARD_OFFSET_Y + y * GameViewport.CELL_SIZE,
                            GameViewport.CELL_SIZE, puyo.getPopScale());
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
        float scale = puyo.getPopScale(); // 팝 애니메이션 스케일 적용
        
        // 스케일이 0이면 그리지 않음 (완전히 사라짐)
        if (scale <= 0)
            return;

        // PuyoRenderer를 사용하여 SpriteBatch로 그리기
        puyoRenderer.draw(batch, puyo.getColor(), x, y - (puyo.getInMiddle() ? GameViewport.CELL_SIZE / 2 : 0 ) , GameViewport.CELL_SIZE, scale);
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

        // Game Over 팝업 (배치 후 렌더링하여 최상단에 표시)
        if (gameWorld.isGameOver()) {
            drawGameOverPopup();
        }
    }

    /**
     * 게임 오버 레이어 팝업 렌더링
     * 반투명 오버레이 + 중앙 흰색 팝업 박스 + 텍스트
     */
    private void drawGameOverPopup() {
        // 1. 반투명 전체 화면 오버레이
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.7f); // 검은색 70% 투명도
        shapeRenderer.rect(0, 0, GameViewport.VIRTUAL_WIDTH, GameViewport.VIRTUAL_HEIGHT);
        shapeRenderer.end();

        // 2. 중앙 팝업 박스 (흰색 둥근 사각형 느낌)
        float popupWidth = 400f;
        float popupHeight = 220f;
        float popupX = (GameViewport.VIRTUAL_WIDTH - popupWidth) / 2f;
        float popupY = (GameViewport.VIRTUAL_HEIGHT - popupHeight) / 2f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 1f, 1f, 0.95f); // 흰색 95% 불투명도
        shapeRenderer.rect(popupX, popupY, popupWidth, popupHeight);
        shapeRenderer.end();

        // 팝업 테두리
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f); // 어두운 회색 테두리
        shapeRenderer.rect(popupX, popupY, popupWidth, popupHeight);
        shapeRenderer.end();

        // 3. 팝업 내부 텍스트
        batch.begin();

        BitmapFont titleFont = fontManager.getTitleFont(48);
        titleFont.getData().setScale(1f);
        titleFont.setColor(Color.RED);

        String gameOverText = "GAME OVER";
        float textWidth = titleFont.draw(batch, gameOverText, 0, 0).width;
        titleFont.draw(batch, gameOverText,
                GameViewport.VIRTUAL_WIDTH / 2f - textWidth / 2f,
                popupY + popupHeight - 50);

        BitmapFont uiFont2 = fontManager.getUIFont(24);
        uiFont2.getData().setScale(1f);
        uiFont2.setColor(Color.BLACK);

        String restartText = "Touch or Press ENTER to Restart";
        float restartWidth = uiFont2.draw(batch, restartText, 0, 0).width;
        uiFont2.draw(batch, restartText,
                GameViewport.VIRTUAL_WIDTH / 2f - restartWidth / 2f,
                popupY + 60);

        // 점수 표시
        String scoreText = "Final Score: " + gameWorld.getScore();
        float scoreWidth = uiFont2.draw(batch, scoreText, 0, 0).width;
        uiFont2.draw(batch, scoreText,
                GameViewport.VIRTUAL_WIDTH / 2f - scoreWidth / 2f,
                popupY + 110);

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
        if (puyoRenderer != null)
            puyoRenderer.dispose();
        // 폰트는 FontManager가 관리하므로 여기서 dispose 하지 않음
    }
}
