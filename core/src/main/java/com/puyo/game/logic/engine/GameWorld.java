package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.PuyoPair;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoColor;
import com.puyo.game.util.LogUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * 게임 상태 머신: 보드, 현재/다음 쌍, 점수, 연쇄 수, 게임오버 상태 관리.
 * 모든 상태를 GameWorld에서 통합 관리하며, 매니저 클래스들은 순수 기능만 제공.
 */
public class GameWorld {
    private final Board board;
    private final PuyoPairGenerator pairGenerator;
    private final SeparationManager separationManager;
    private final GravityEngine gravityEngine;

    private PuyoPair currentPair;
    private PuyoPair nextPair;
    private boolean gameOver = false;
    private int score = 0;
    private float fallTimer = 0f;
    private float fallInterval = 0.5f; // 초당 셀 낙하 속도 (레벨별 조정 가능)

    // ==========================================
    // 통합된 게임 상태 머신
    // ==========================================
    private enum GamePhase {
        SPAWNING,           // 새 조각 생성/위치 설정
        FALLING,            // 일반 낙하 (이동/회전/락딜레이 포함)
        SEPARATING,         // 분리 낙하 애니메이션 진행 중
        CHAIN_FINDING,      // 연쇄: 매치 탐색
        CHAIN_POP_WAIT,     // 연쇄: 팝 애니메이션 대기
        CHAIN_GRAVITY,      // 연쇄: 중력 적용 후 부유 확인
        CHAIN_FLOATING,     // 연쇄: 부유 뿌요 낙하 애니메이션
        GAME_OVER
    }
    private GamePhase gamePhase = GamePhase.SPAWNING;

    // 락딜레이 상태 (GameWorld에서 직접 관리)
    private boolean lockDelayActive = false;
    private float lockDelayTimer = 0f;
    private int lockDelayMoves = 0;

    // 애니메이션 상태 (GameWorld에서 직접 관리)
    private List<com.puyo.game.logic.engine.FallingAnimationManager.FallingPuyo> fallingPuyos = new ArrayList<>();
    private float separationFallTimer = 0f;
    private float floatingFallTimer = 0f;
    private static final float SEPARATION_FALL_INTERVAL = 0.05f;
    private static final float FLOATING_FALL_INTERVAL = 0.05f;

    // 연쇄 상태
    private List<List<Puyo>> currentGroups = null;
    private int chainCount = 0;
    private int totalChainRemoved = 0;
    private int currentChain = 0;

    public GameWorld() {
        this(new Board());
    }

    /**
     * 테스트용 생성자 - 외부 보드 주입 가능
     */
    GameWorld(Board board) {
        this.board = board;
        pairGenerator = new PuyoPairGenerator();
        separationManager = new SeparationManager();
        gravityEngine = new GravityEngine();

        // 초기 상태는 SPAWNING, update()에서 처리
    }

    /** 현재 쌍을 새로 스폰하고 상태 초기화 */
    public void spawnNewPair() {
        currentPair = pairGenerator.generate();
        pairGenerator.positionAtSpawn(currentPair, Board.WIDTH, Board.HEIGHT);
        lockDelayActive = false;
        lockDelayTimer = 0f;
        lockDelayMoves = 0;
    }

    /** 다음 쌍을 스폰 (미리보기용) */
    public void spawnNextPair() {
        nextPair = pairGenerator.generate();
        pairGenerator.positionAtSpawn(nextPair, Board.WIDTH, Board.HEIGHT);
    }

    /** 현재 쌍이 낙하 가능한지 확인 */
    public boolean canFall() {
        return currentPair != null && board.canMoveDown(currentPair);
    }

    /** 왼쪽 이동 */
    public boolean moveLeft() {
        if (currentPair == null || gamePhase != GamePhase.FALLING)
            return false;
        if (board.canMoveLeft(currentPair)) {
            currentPair.moveLeft();
            if (lockDelayActive) {
                lockDelayMoves++;
                lockDelayTimer = 0f; // Tsu 규칙: 이동 시 타이머 리셋
                LogUtil.debug("GameWorld", "LockDelay recordMove: moveCount=" + lockDelayMoves + ", timer reset");
            }
            return true;
        }
        return false;
    }

    /** 오른쪽 이동 */
    public boolean moveRight() {
        if (currentPair == null || gamePhase != GamePhase.FALLING)
            return false;
        if (board.canMoveRight(currentPair)) {
            currentPair.moveRight();
            if (lockDelayActive) {
                lockDelayMoves++;
                lockDelayTimer = 0f;
                LogUtil.debug("GameWorld", "LockDelay recordMove: moveCount=" + lockDelayMoves + ", timer reset");
            }
            return true;
        }
        return false;
    }

    /** 시계방향 회전 (벽 킥 포함) */
    public void rotateClockwise() {
        if (currentPair == null || gamePhase != GamePhase.FALLING)
            return;
        currentPair.rotateClockwise();
        if (!board.canPlace(currentPair)) {
            if (board.canMoveLeft(currentPair)) {
                currentPair.moveLeft();
            } else if (board.canMoveRight(currentPair)) {
                currentPair.moveRight();
            } else {
                currentPair.rotateCounterClockwise();
            }
        }
        if (lockDelayActive) {
            lockDelayMoves++;
            lockDelayTimer = 0f;
            LogUtil.debug("GameWorld", "LockDelay recordMove: moveCount=" + lockDelayMoves + ", timer reset");
        }
    }

    /** 하드 드롭 */
    public void hardDrop() {
        if (currentPair == null || gamePhase != GamePhase.FALLING)
            return;
        while (canFall()) {
            currentPair.moveDown();
        }
        lockPiece();
    }

    /** 메인 업데이트 루프 - 단일 switch로 모든 상태 처리 */
    public void update(float delta) {
        if (gameOver)
            return;

        switch (gamePhase) {
            case SPAWNING: {
                handleSpawning();
                break;
            }
            case FALLING: {
                handleFalling(delta);
                break;
            }
            case SEPARATING: {
                handleSeparating(delta);
                break;
            }
            case CHAIN_FINDING: {
                handleChainFinding();
                break;
            }
            case CHAIN_POP_WAIT: {
                handleChainPopWait(delta);
                break;
            }
            case CHAIN_GRAVITY: {
                handleChainGravity();
                break;
            }
            case CHAIN_FLOATING: {
                handleChainFloating(delta);
                break;
            }
            case GAME_OVER:
                break;
        }
    }

    // ==========================================
    // 각 Phase별 처리 메서드
    // ==========================================

    private void handleSpawning() {
        spawnNewPair();
        spawnNextPair();
        fallTimer = 0f;
        chainCount = 0;
        totalChainRemoved = 0;
        currentGroups = null;
        fallingPuyos.clear();
        separationFallTimer = 0f;
        floatingFallTimer = 0f;
        gamePhase = GamePhase.FALLING;
        LogUtil.debug("GameWorld", "Phase: SPAWNING -> FALLING, new pair spawned");
    }

    private void handleFalling(float delta) {
        // 락딜레이 타이머 업데이트 및 체크
        if (lockDelayActive) {
            lockDelayTimer += delta;

            if (LockDelayManager.shouldLock(lockDelayTimer, lockDelayMoves, true)) {
                LogUtil.debug("GameWorld", "LockDelay shouldLock triggered lockPiece");
                lockPiece();
                return;
            }
        }

        // 자동 낙하 타이머
        fallTimer += delta;
        if (fallTimer >= fallInterval) {
            fallTimer = 0f;
            if (canFall()) {
                currentPair.moveDown();
                // Tsu 규칙: 공중에서 이동 시 락딜레이 리셋
                if (lockDelayActive) {
                    lockDelayTimer = 0f;
                    lockDelayMoves = 0;
                    LogUtil.debug("GameWorld", "Air move: LockDelay reset");
                }
            } else {
                // 바닥에 닿음
                handleLanding();
            }
        }
    }

    private void handleLanding() {
        // 분리 가능한지 확인
        if (currentPair != null && separationManager.canSeparate(currentPair, board)) {
            SeparationManager.SeparationResult sepResult = separationManager.separate(currentPair, board);
            if (sepResult.separated) {
                // 막힌 쪽 즉시 잠금
                LogUtil.debug("GameWorld", "Placing blocked puyo at (" + sepResult.blockedPuyo.getX() + "," + sepResult.blockedPuyo.getY()
                        + ") color=" + sepResult.blockedPuyo.getColor());
                board.placePuyo(sepResult.blockedPuyo);

                // 자유로운 쪽 단일 뿌요로 자동 낙하 시작
                LogUtil.debug("GameWorld", "Adding free puyo to separating: (" + sepResult.freePuyo.getX() + "," + sepResult.freePuyo.getY()
                        + ") color=" + sepResult.freePuyo.getColor());
                addFallingPuyo(sepResult.freePuyo, FallingAnimationManager.FallingPuyo.FallType.SEPARATION);
                separationFallTimer = 0f;
                gamePhase = GamePhase.SEPARATING;
                LogUtil.debug("GameWorld", "Phase: FALLING -> SEPARATING");
            } else {
                activateLockDelay();
            }
        } else {
            activateLockDelay();
        }
    }

    private void activateLockDelay() {
        if (!lockDelayActive) {
            lockDelayActive = true;
            lockDelayTimer = 0f;
            lockDelayMoves = 0;
            LogUtil.debug("GameWorld", "LockDelay activated");
        }
    }

    private void addFallingPuyo(Puyo puyo, FallingAnimationManager.FallingPuyo.FallType type) {
        fallingPuyos.add(new FallingAnimationManager.FallingPuyo(puyo, type));
    }

    private void handleSeparating(float delta) {
        boolean stillFalling = FallingAnimationManager.updateSeparationAndFloatingFalling(
                delta, board, fallingPuyos, new float[]{separationFallTimer}, SEPARATION_FALL_INTERVAL);
        separationFallTimer = new float[]{separationFallTimer}[0];

        if (!stillFalling) {
            // 분리 낙하 완료: 배치
            List<Puyo> placeSeparated = new ArrayList<>();
            List<Puyo> placeFloating = new ArrayList<>();
            FallingAnimationManager.collectCompletedFalling(fallingPuyos, placeSeparated, placeFloating);

            if (!placeSeparated.isEmpty()) {
                for (Puyo p : placeSeparated) {
                    board.placePuyo(p);
                }
                LogUtil.debug("GameWorld", "Separated puyos placed: " + placeSeparated.size());
            }

            currentPair = null;
            lockDelayActive = false;
            lockDelayTimer = 0f;
            lockDelayMoves = 0;

            // 연쇄 체크
            List<List<Puyo>> groups = MatchFinder.findAllMatchingGroups(board);
            if (!groups.isEmpty()) {
                LogUtil.debug("GameWorld", "Separation done: found " + groups.size() + " match groups, starting chain");
                gamePhase = GamePhase.CHAIN_FINDING;
            } else {
                gamePhase = GamePhase.SPAWNING;
            }
        }
    }

    private void handleChainFinding() {
        currentGroups = MatchFinder.findAllMatchingGroups(board);

        if (currentGroups.isEmpty()) {
            // 연쇄 종료
            LogUtil.debug("GameWorld", "Chain ended. totalRemoved=" + totalChainRemoved + ", chainCount=" + chainCount);
            gamePhase = GamePhase.SPAWNING;
            return;
        }

        // 새 연쇄 단계 시작
        chainCount++;
        int removed = currentGroups.stream().mapToInt(List::size).sum();
        totalChainRemoved += removed;
        LogUtil.debug("GameWorld", "New chain step: chainCount=" + chainCount + ", groups=" + currentGroups.size() + ", removed=" + removed);

        // 팝 애니메이션 시작
        for (List<Puyo> group : currentGroups) {
            for (Puyo puyo : group) {
                if (!puyo.isPopping()) {
                    puyo.startPop();
                }
                addFallingPuyo(puyo, FallingAnimationManager.FallingPuyo.FallType.CHAIN_POP);
            }
        }
        gamePhase = GamePhase.CHAIN_POP_WAIT;
        LogUtil.debug("GameWorld", "Phase: CHAIN_FINDING -> CHAIN_POP_WAIT, fallingPuyos=" + fallingPuyos.size());
    }

    private void handleChainPopWait(float delta) {
        boolean allPopDone = FallingAnimationManager.updatePop(delta, fallingPuyos);

        if (allPopDone) {
            // 팝 완료: CHAIN_POP 엔트리 수집 및 제거, 보드에서 제거
            List<Puyo> poppedPuyos = FallingAnimationManager.collectAndClearChainPop(fallingPuyos);
            if (!poppedPuyos.isEmpty()) {
                for (Puyo p : poppedPuyos) {
                    board.removePuyo(p);
                }
                LogUtil.debug("GameWorld", "Popped puyos removed: " + poppedPuyos.size());
            }

            // 중력 적용
            gravityEngine.applyGravity(board);
            gamePhase = GamePhase.CHAIN_GRAVITY;
            LogUtil.debug("GameWorld", "Phase: CHAIN_POP_WAIT -> CHAIN_GRAVITY");
        }
    }

    private void handleChainGravity() {
        // 부유 뿌요 확인
        List<Puyo> floating = board.getAllFloatingPuyos();
        if (!floating.isEmpty()) {
            LogUtil.debug("GameWorld", "Found " + floating.size() + " floating puyos, starting floating fall");
            for (Puyo p : floating) {
                board.removePuyo(p);
                addFallingPuyo(p, FallingAnimationManager.FallingPuyo.FallType.FLOATING);
            }
            floatingFallTimer = 0f;
            gamePhase = GamePhase.CHAIN_FLOATING;
            LogUtil.debug("GameWorld", "Phase: CHAIN_GRAVITY -> CHAIN_FLOATING");
        } else {
            // 부유 없으면 다음 연쇄 단계
            gamePhase = GamePhase.CHAIN_FINDING;
        }
    }

    private void handleChainFloating(float delta) {
        boolean stillFalling = FallingAnimationManager.updateSeparationAndFloatingFalling(
                delta, board, fallingPuyos, new float[]{floatingFallTimer}, FLOATING_FALL_INTERVAL);
        floatingFallTimer = new float[]{floatingFallTimer}[0];

        if (!stillFalling) {
            // 부유 낙하 완료: 배치
            List<Puyo> placeSeparated = new ArrayList<>();
            List<Puyo> placeFloating = new ArrayList<>();
            FallingAnimationManager.collectCompletedFalling(fallingPuyos, placeSeparated, placeFloating);

            if (!placeFloating.isEmpty()) {
                for (Puyo p : placeFloating) {
                    board.placePuyo(p);
                }
                LogUtil.debug("GameWorld", "Floating puyos placed: " + placeFloating.size());
            }

            fallingPuyos.clear();
            gamePhase = GamePhase.CHAIN_FINDING;
            LogUtil.debug("GameWorld", "Phase: CHAIN_FLOATING -> CHAIN_FINDING");
        }
    }

    /** 현재 조각 잠금 및 연쇄 처리 시작 */
    private void lockPiece() {
        if (currentPair == null)
            return;

        for (Puyo p : currentPair.getPuyos()) {
            if (p.isAlive()) {
                board.placePuyo(p);
            }
        }

        lockDelayActive = false;
        lockDelayTimer = 0f;
        lockDelayMoves = 0;
        currentPair = null;

        // 연쇄 시작
        chainCount = 0;
        totalChainRemoved = 0;
        currentGroups = null;
        gamePhase = GamePhase.CHAIN_FINDING;
        LogUtil.debug("GameWorld", "lockPiece -> CHAIN_FINDING");
    }

    // --- Getters ---

    public boolean isGameOver() {
        return gameOver;
    }

    public Board getBoard() {
        return board;
    }

    public PuyoPair getCurrentPair() {
        return currentPair;
    }

    public PuyoPair getNextPair() {
        return nextPair;
    }

    /** 호환용: 분리 낙하 중인 단일 뿌요 반환 */
    public Puyo getFallingSinglePuyo() {
        for (com.puyo.game.logic.engine.FallingAnimationManager.FallingPuyo fp : fallingPuyos) {
            if (fp.type == com.puyo.game.logic.engine.FallingAnimationManager.FallingPuyo.FallType.SEPARATION) {
                return fp.puyo;
            }
        }
        return null;
    }

    /** 호환용: 모든 낙하 중인 뿌요 리스트 반환 (렌더링용) */
    public List<com.puyo.game.logic.engine.FallingAnimationManager.FallingPuyo> getFallingPuyos() {
        return new ArrayList<>(fallingPuyos);
    }

    public int getScore() {
        return score;
    }

    public void addScore(int points) {
        this.score += points;
    }

    public int getCurrentChain() {
        return currentChain;
    }

    public void dispose() {
    }
}
