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
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoPair;
import com.puyo.game.util.LogUtil;
import com.puyo.game.story.StoryModeManager;
import com.puyo.game.story.StageData;
import com.puyo.game.config.GameViewport;
import com.puyo.game.graphics.FontManager;
import com.puyo.game.graphics.PuyoRenderer;
import com.puyo.game.input.InputProvider;

import java.util.List;

public class PlayScreen extends BaseScreen {
    private final PuyoGame game;
    private final GameMode mode;
    private final StoryModeManager storyManager;
    private final GameWorld gameWorld;
    private final FontManager fontManager;
    private final PuyoRenderer puyoRenderer;
    private final InputProvider inputProvider;
    
    private ShapeRenderer shapeRenderer;
    private SpriteBatch batch;

    public PlayScreen(PuyoGame game, GameMode mode, InputProvider inputProvider) {
        this(game, mode, -1, inputProvider);
    }

    public PlayScreen(PuyoGame game, GameMode mode, int storyStageIndex, InputProvider inputProvider) {
        super(game);
        this.game = game;
        this.mode = mode;
        this.storyManager = (mode == GameMode.NORMAL) ? new StoryModeManager(storyStageIndex) : null;
        this.gameWorld = new GameWorld();
        this.fontManager = FontManager.getInstance();
        this.puyoRenderer = new PuyoRenderer();
        this.inputProvider = inputProvider;
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

        initViewport();

        shapeRenderer = new ShapeRenderer();
        batch = new SpriteBatch();
        // InputProvider는 생성자에서 이미 Gdx.input.setInputProcessor(this) 호출함
    }

    @Override
    public void render(float delta) {
        // 1. 게임 로직 업데이트 (입력 처리 포함)
        gameWorld.update(delta, inputProvider);

        // 2. 화면 클리어
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        // 3. 카메라/프로젝션 업데이트
        camera.update();
        batch.setProjectionMatrix(camera.combined);
        shapeRenderer.setProjectionMatrix(camera.combined);

        // 4. 그리기
        drawBoard();
        drawCurrentPair();
        drawStatefulPuyos();
        drawNextPair();
        drawUI();
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
                if (puyo != null && puyo.isAlive()) {
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
        PuyoPair currentPair = gameWorld.getCurrentPair();
        if (currentPair != null) {
            batch.begin();
            for (Puyo p : currentPair.getPuyos()) {
                if (p.isAlive()) {
                    float x = GameViewport.Single.BOARD_OFFSET_X + p.getX() * GameViewport.CELL_SIZE;
                    float y = GameViewport.Single.BOARD_OFFSET_Y + p.getY() * GameViewport.CELL_SIZE;
                    drawPuyo(p, x, y);
                }
            }
            batch.end();
        }
    }

    private void drawStatefulPuyos() {
        List<Puyo> animating = gameWorld.getAnimatingPuyos();
        if (!animating.isEmpty()) {
            batch.begin();
            for (Puyo p : animating) {
                if (p.isAlive()) {
                    float x = GameViewport.Single.BOARD_OFFSET_X + p.getX() * GameViewport.CELL_SIZE;
                    float y = GameViewport.Single.BOARD_OFFSET_Y + p.getY() * GameViewport.CELL_SIZE;
                    drawPuyo(p, x, y);
                }
            }
            batch.end();
        }
    }

    /**
     * 뿌요 그리기 헬퍼 - 상태별 스케일, inMiddle 반칸 오프셋, 비균일 스케일 지원
     */
    private void drawPuyo(Puyo puyo, float x, float y) {
        // 상태별 스케일 결정
        float scaleX = 1.0f;
        float scaleY = 1.0f;
        
        switch (puyo.getState()) {
            case POPPING:
                scaleX = scaleY = puyo.getScaleX(); // 팝은 균일 스케일
                break;
            case SETTLING:
                scaleX = puyo.getScaleX();
                scaleY = puyo.getScaleY();
                break;
            case FALLING:
                // 낙하 중에도 스케일 적용 (반칸 처리용)
                scaleX = scaleY = 1.0f;
                break;
            default: // NORMAL
                scaleX = scaleY = 1.0f;
        }
        
        // 스케일이 0이면 그리지 않음 (팝 완료 시)
        if (scaleX <= 0 || scaleY <= 0)
            return;

        // Y 오프셋 계산 (반칸 상태 고려: CELL_SIZE/2 = 40픽셀)
        float offsetY = puyo.getInMiddle() ? GameViewport.CELL_SIZE / 2 : 0;

        // PuyoRenderer를 사용하여 SpriteBatch로 그리기 (비균일 스케일 지원)
        if (puyo.isSettling()) {
            puyoRenderer.draw(batch, puyo.getColor(), x, y - offsetY, GameViewport.CELL_SIZE, scaleX, scaleY);
        } else {
            puyoRenderer.draw(batch, puyo.getColor(), x, y - offsetY, GameViewport.CELL_SIZE, scaleX);
        }
    }

    private void drawNextPair() {
        PuyoPair nextPair = gameWorld.getNextPair();
        if (nextPair != null) {
            batch.begin();
            // 다음 뿌요 프리뷰 위치 (사이드 패널)
            float previewX = GameViewport.Single.NEXT_PREVIEW_X;
            float previewY = GameViewport.Single.NEXT_PREVIEW_Y;
            for (Puyo p : nextPair.getPuyos()) {
                drawPuyo(p, previewX, previewY);
                previewY -= GameViewport.CELL_SIZE;
            }
            batch.end();
        }
    }

    private void drawUI() {
        batch.begin();
        BitmapFont uiFont = fontManager.getUIFont(24);
        uiFont.getData().setScale(1f);
        uiFont.setColor(Color.WHITE);

        // 점수
        uiFont.draw(batch, "Score: " + gameWorld.getScore(), 
                GameViewport.Single.UI_X, GameViewport.VIRTUAL_HEIGHT - 50);

        // 연쇄 수
        int chain = gameWorld.getCurrentChain();
        if (chain > 0) {
            uiFont.setColor(Color.YELLOW);
            uiFont.draw(batch, "CHAIN x" + chain, 
                    GameViewport.Single.UI_X, GameViewport.VIRTUAL_HEIGHT - 90);
            uiFont.setColor(Color.WHITE);
        }

        // NEXT 라벨
        uiFont.draw(batch, "NEXT",
                GameViewport.Single.NEXT_PREVIEW_X,
                GameViewport.Single.NEXT_PREVIEW_Y + GameViewport.CELL_SIZE + 20);

        // 스토리 모드 정보
        if (mode == GameMode.NORMAL && storyManager != null) {
            StageData current = storyManager.getCurrentStage();
            if (current != null) {
                uiFont.draw(batch, "Stage: " + storyManager.getCurrentStageNumber()
                        + "/" + storyManager.getTotalStages(),
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

    private void drawGameOverPopup() {
        // 1. 반투명 전체 화면 오버레이
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0f, 0f, 0f, 0.7f);
        shapeRenderer.rect(0, 0, GameViewport.VIRTUAL_WIDTH, GameViewport.VIRTUAL_HEIGHT);
        shapeRenderer.end();

        // 2. 중앙 팝업 박스
        float popupWidth = 400f;
        float popupHeight = 220f;
        float popupX = (GameViewport.VIRTUAL_WIDTH - popupWidth) / 2f;
        float popupY = (GameViewport.VIRTUAL_HEIGHT - popupHeight) / 2f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(1f, 1f, 1f, 0.95f);
        shapeRenderer.rect(popupX, popupY, popupWidth, popupHeight);
        shapeRenderer.end();

        // 팝업 테두리
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);
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
        // InputProvider가 직접 Gdx.input.setInputProcessor 관리하므로 여기선 아무것도 안 함
    }

    @Override
    public void dispose() {
        if (shapeRenderer != null) shapeRenderer.dispose();
        if (batch != null) batch.dispose();
        if (puyoRenderer != null) puyoRenderer.dispose();
        if (inputProvider != null) inputProvider.dispose();
        // 폰트는 FontManager가 관리하므로 여기서 dispose 하지 않음
    }
}
