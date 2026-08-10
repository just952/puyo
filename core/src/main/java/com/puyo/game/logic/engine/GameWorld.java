package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.PuyoPair;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoColor;
import com.puyo.game.util.LogUtil;

import java.util.List;
import java.util.Random;

/**
 * 게임 상태 머신: 보드, 현재/다음 쌍, 점수, 연쇄 수, 게임오버 상태 관리.
 * 실제 로직은 각 매니저 클래스에 위임.
 */
public class GameWorld {
    private final Board board;
    private final PuyoPairGenerator pairGenerator;
    private final LockDelayManager lockDelayManager;
    private final SeparationManager separationManager;
    private final FallingAnimationManager fallingAnimationManager;
    private final ChainProcessor chainProcessor;

    private PuyoPair currentPair;
    private PuyoPair nextPair;
    private boolean gameOver = false;
    private int score = 0;
    private float fallTimer = 0f;
    private float fallInterval = 0.5f; // 초당 셀 낙하 속도 (레벨별 조정 가능)
    private int currentChain = 0;
    private int totalRemoved = 0;

    // 호환성을 위한 FallingPuyo 내부 클래스 (PlayScreen 등에서 사용)
    public static class FallingPuyo {
        public Puyo puyo;
        public boolean isFromSeparation;

        public FallingPuyo(Puyo puyo, boolean isFromSeparation) {
            this.puyo = puyo;
            this.isFromSeparation = isFromSeparation;
        }
    }

    public GameWorld() {
        this(new Board());
    }

    /**
     * 테스트용 생성자 - 외부 보드 주입 가능
     */
    GameWorld(Board board) {
        this.board = board;
        pairGenerator = new PuyoPairGenerator();
        lockDelayManager = new LockDelayManager();
        separationManager = new SeparationManager();
        fallingAnimationManager = new FallingAnimationManager();
        chainProcessor = new ChainProcessor();

        spawnNewPair();
        spawnNextPair();
    }

    /** 현재 쌍을 새로 스폰하고 락 딜레이 리셋 */
    public void spawnNewPair() {
        currentPair = pairGenerator.generate();
        pairGenerator.positionAtSpawn(currentPair, Board.WIDTH, Board.HEIGHT);
        lockDelayManager.reset();
    }

    /** 다음 쌍을 스폰 (미리보기용) */
    public void spawnNextPair() {
        nextPair = pairGenerator.generate();
        pairGenerator.positionAtSpawn(nextPair, Board.WIDTH, Board.HEIGHT);
        lockDelayManager.reset();
    }

    /** 현재 쌍이 낙하 가능한지 확인 */
    public boolean canFall() {
        return currentPair != null && board.canMoveDown(currentPair);
    }

    /** 왼쪽 이동 */
    public boolean moveLeft() {
        if (currentPair == null)
            return false;
        if (board.canMoveLeft(currentPair)) {
            currentPair.moveLeft();
            lockDelayManager.recordMove();
            return true;
        }
        return false;
    }

    /** 오른쪽 이동 */
    public boolean moveRight() {
        if (currentPair == null)
            return false;
        if (board.canMoveRight(currentPair)) {
            currentPair.moveRight();
            lockDelayManager.recordMove();
            return true;
        }
        return false;
    }

    /** 시계방향 회전 (벽 킥 포함) */
    public void rotateClockwise() {
        if (currentPair == null)
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
        lockDelayManager.recordMove();
    }

    /** 하드 드롭 */
    public void hardDrop() {
        if (currentPair == null)
            return;
        while (canFall()) {
            currentPair.moveDown();
        }
        lockPiece();
    }

    /** 메인 업데이트 루프 */
    public void update(float delta) {
        if (gameOver)
            return;

        // 1. 낙하/팝 애니메이션 처리 중이면 최우선 처리
        if (!fallingAnimationManager.isEmpty()) {
            boolean done = fallingAnimationManager.update(delta, board);
            if (done) {
                // 애니메이션 완료: 분리된 뿌요 배치, 부유 뿌요 배치 (팝 제거는 addChainFalling에서 이미 수행)
                fallingAnimationManager.placeSeparatedPuyos(board);
                fallingAnimationManager.placeFloatingPuyos(board);
                // 처리된 아이템들 정리 (중요: 무한 루프 방지)
                fallingAnimationManager.clear();

                // 연쇄 후 떠있는 뿌요들 낙하 애니메이션 추가
                List<Puyo> floating = board.getAllFloatingPuyos();
                if (!floating.isEmpty()) {
                    for (Puyo p : floating) {
                        board.removePuyo(p);
                    }
                    fallingAnimationManager.addFloatingPuyos(floating);
                } else {
                    // 떠있는 뿌요 없으면 다음 연쇄 단계 시작 (비동기식)
                    startNextChainStep();
                }
            }
            return;
        }

        // 2. 일반 쌍 뿌요 낙하 처리
        fallTimer += delta;
        if (fallTimer >= fallInterval) {
            fallTimer = 0f;
            if (canFall()) {
                currentPair.moveDown();
                lockDelayManager.reset(); // 공중 이동 시 락 딜레이 리셋
            } else {
                // 바닥에 닿음 - 분리 가능한지 확인
                if (separationManager.canSeparate(currentPair, board)) {
                    // 분리 실행
                    separationManager.separate(currentPair, board,
                            fallingAnimationManager.getInternalFallingPuyos());
                    currentPair = null;
                    lockDelayManager.forceLock();
                } else {
                    // 락 딜레이 시작/진행
                    if (!lockDelayManager.isActive()) {
                        lockDelayManager.activate();
                    }
                    lockDelayManager.update(delta);
                    if (lockDelayManager.shouldLock()) {
                        lockPiece();
                    }
                }
            }
        }

        // 락 딜레이 타이머 업데이트 (낙하 간격과 별도)
        if (lockDelayManager.isActive()) {
            lockDelayManager.update(delta);
            if (lockDelayManager.shouldLock()) {
                lockPiece();
            }
        }
    }

    /** 연쇄 처리 상태 (ChainProcessor에서 사용) */
    private ChainProcessor.ChainState chainState = new ChainProcessor.ChainState();

    /** 현재 조각 잠금 및 연쇄 처리 시작 */
    private void lockPiece() {
        if (currentPair == null)
            return;

        for (Puyo p : currentPair.getPuyos()) {
            if (p.isAlive()) {
                board.placePuyo(p);
            }
        }

        // 연쇄 상태 초기화
        resetChainState();

        // 첫 연쇄 단계 시작 (ChainProcessor.processChainStep 호출)
        startNextChainStep();
    }

    /** 연쇄 상태 초기화 */
    private void resetChainState() {
        chainState.chainCount = 0;
        chainState.totalRemoved = 0;
        chainState.currentGroups = null;
        chainState.waitingForPop = false;
        LogUtil.debug("GameWorld", "resetChainState: chainState reset");
    }

    /** 다음 연쇄 단계 시작 */
    private void startNextChainStep() {
        boolean chainDone = chainProcessor.processChainStep(board, chainState,
                new ChainProcessor.ChainCallback() {
                    @Override
                    public void onPopStart(List<Puyo> group) {
                        LogUtil.debug("GameWorld", "onPopStart: group size=" + group.size());
                        fallingAnimationManager.addChainFalling(board, group);
                    }

                    @Override
                    public void onPopComplete(List<Puyo> group) {
                        LogUtil.debug("GameWorld", "onPopComplete: group size=" + group.size());
                        // 실제 제거는 onPopStart에서 FallingAnimationManager가 이미 수행 (board.removePuyo)
                    }

                    @Override
                    public void onGravityComplete() {
                        LogUtil.debug("GameWorld", "onGravityComplete");
                        // 중력 완료 후 떠있는 뿌요들 확인하여 낙하 애니메이션 추가
                        List<Puyo> floating = board.getAllFloatingPuyos();
                        if (!floating.isEmpty()) {
                            LogUtil.debug("GameWorld", "onGravityComplete: floating count=" + floating.size());
                            for (Puyo p : floating) {
                                board.removePuyo(p);
                            }
                            fallingAnimationManager.addFloatingPuyos(floating);
                        }
                    }

                    @Override
                    public void onChainEnd(int totalRemoved, int chainCount) {
                        LogUtil.debug("GameWorld",
                                "onChainEnd: totalRemoved=" + totalRemoved + ", chainCount=" + chainCount);
                        GameWorld.this.currentChain = chainCount;
                        GameWorld.this.totalRemoved = totalRemoved;
                        if (chainCount > 0) {
                            int earned = totalRemoved * (chainCount + 1) * 10;
                            addScore(earned);
                        }

                        if (board.isTopOut()) {
                            gameOver = true;
                            return;
                        }

                        // 다음 쌍 스폰
                        currentPair = nextPair;
                        spawnNextPair();
                        fallTimer = 0f;
                        lockDelayManager.reset();

                        // 연쇄 완료 시 chainState 초기화
                        resetChainState();
                    }
                }, fallingAnimationManager);

        // chainDone이 true면 연쇄 완료 (currentPair 스폰은 onChainEnd에서 처리)
        // false면 팝 애니메이션 대기 중 (update에서 fallingAnimationManager 처리 대기)
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
        return fallingAnimationManager.getFallingSinglePuyo();
    }

    /** 호환용: 모든 낙하 중인 뿌요 리스트 반환 (렌더링용) */
    public List<FallingPuyo> getFallingPuyos() {
        List<com.puyo.game.logic.engine.FallingPuyo> internal = fallingAnimationManager.getFallingPuyos();
        List<FallingPuyo> result = new java.util.ArrayList<>();
        for (com.puyo.game.logic.engine.FallingPuyo fp : internal) {
            result.add(new FallingPuyo(fp.puyo, fp.isFromSeparation()));
        }
        return result;
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
}
