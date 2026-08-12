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
    private final GravityEngine gravityEngine;

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
        gravityEngine = new GravityEngine();

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

                // 분리/부유 낙하 완료 후 보드에 매치가 있는지 확인
                // 매치가 있으면 즉시 연쇄 처리 시작 (지연 방지)
                List<List<Puyo>> groups = MatchFinder.findAllMatchingGroups(board);
                if (!groups.isEmpty()) {
                    LogUtil.debug("GameWorld", "Animation done: found " + groups.size() + " match groups, starting chain");
                    chainProcessor.startChain();
                }

                // 분리 애니메이션 완료 후 currentPair가 null이면 다음 쌍 스폰
                if (currentPair == null && !chainProcessor.hasActiveChain()) {
                    currentPair = nextPair;
                    spawnNextPair();
                    fallTimer = 0f;
                    lockDelayManager.reset();
                    chainProcessor.reset();
                }

                // 연쇄 처리 계속 진행 (상태 머신이 다음 단계 처리)
            }
            return;
        }

        // 2. 연쇄 처리 상태 머신 업데이트 (연쇄가 진행 중일 때만)
        if (chainProcessor.hasActiveChain()) {
            ChainProcessor.UpdateResult result = chainProcessor.update(board, fallingAnimationManager, delta);
            
            // 액션 실행
            switch (result.action) {
                case START_POP:
                    // 팝 애니메이션 시작
                    if (result.groups != null) {
                        LogUtil.debug("GameWorld", "Executing START_POP for " + result.groups.size() + " groups");
                        for (List<Puyo> group : result.groups) {
                            fallingAnimationManager.addChainFalling(board, group);
                        }
                    }
                    break;
                    
                case APPLY_GRAVITY:
                    // 중력 적용
                    LogUtil.debug("GameWorld", "Executing APPLY_GRAVITY");
                    gravityEngine.applyGravity(board);
                    break;
                    
                case CHECK_FLOATING:
                    // 부유 뿌요 확인 및 낙하 애니메이션 시작
                    if (result.floatingPuyos != null && !result.floatingPuyos.isEmpty()) {
                        LogUtil.debug("GameWorld", "Executing CHECK_FLOATING for " + result.floatingPuyos.size() + " floating puyos");
                        for (Puyo p : result.floatingPuyos) {
                            board.removePuyo(p);
                        }
                        fallingAnimationManager.addFloatingPuyos(result.floatingPuyos);
                    }
                    break;
                    
                case NEXT_CHAIN_STEP:
                    // 다음 연쇄 단계로 - 아무것도 안 함 (다음 update에서 FINDING_MATCHES 실행)
                    LogUtil.debug("GameWorld", "Executing NEXT_CHAIN_STEP");
                    break;
                    
                case NONE:
                default:
                    // 아무 액션 없음
                    break;
            }

            if (result.done) {
                // 연쇄 완료
                int earned = result.totalRemoved * (result.chainCount + 1) * 10;
                if (earned > 0) {
                    addScore(earned);
                    LogUtil.debug("GameWorld", "Chain end: chainCount=" + result.chainCount + 
                            ", totalRemoved=" + result.totalRemoved + ", earned=" + earned);
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
                chainProcessor.reset();
            }
            return;
        }

        // 3. 일반 쌍 뿌요 낙하 처리
        fallTimer += delta;

        // 매 프레임 canFall() 체크하여 락 딜레이 즉시 리셋/활성화 관리
        boolean canFallNow = canFall();

        if (canFallNow) {
            // 공중에 떠있으면 락 딜레이 즉시 리셋
            if (lockDelayManager.isActive()) {
                LogUtil.debug("GameWorld", String.format("canFall became true, resetting LockDelay. pair pos=(%d,%d)",
                        currentPair != null ? currentPair.getLeft().getX() : -1,
                        currentPair != null ? currentPair.getLeft().getY() : -1));
                lockDelayManager.reset();
            }

            // 낙하 타이머 처리 (이미 fallTimer += delta 위에서 처리했으므로 여기선 안 함)
            if (fallTimer >= fallInterval) {
                fallTimer = 0f;
                currentPair.moveDown();
                // fallTimer 간격 이동 시에도 락 딜레이 리셋 (이중 보장)
                if (lockDelayManager.isActive()) {
                    LogUtil.debug("GameWorld", "fallTimer moveDown, resetting LockDelay");
                    lockDelayManager.reset();
                }
            }
        } else {
            // 바닥에 닿음 - 분리 가능한지 확인
            if (currentPair != null && separationManager.canSeparate(currentPair, board)) {
                // 분리 실행
                separationManager.separate(currentPair, board,
                        fallingAnimationManager.getInternalFallingPuyos());
                currentPair = null;
                lockDelayManager.forceLock();
            } else if (currentPair != null) {
                // 락 딜레이 시작/진행 (쌍이 있을 때만)
                if (!lockDelayManager.isActive()) {
                    LogUtil.debug("GameWorld", String.format("LockDelay activated: canFall=%b, pair pos=(%d,%d)",
                            canFallNow,
                            currentPair.getLeft().getX(),
                            currentPair.getLeft().getY()));
                    lockDelayManager.activate();
                }
                if (lockDelayManager.shouldLock()) {
                    LogUtil.debug("GameWorld", "LockDelay shouldLock triggered lockPiece");
                    lockPiece();
                }
            } else if (lockDelayManager.isActive()) {
                // currentPair가 null이고 락 딜레이가 활성화되어 있으면 강제 종료 (한 번만)
                lockDelayManager.forceLock();
            }
        }

        // 락 딜레이 타이머 업데이트 (락 딜레이 활성화 시에만) - 여기서만 호출
        if (lockDelayManager.isActive() && currentPair != null) {
            lockDelayManager.update(delta);
            if (lockDelayManager.shouldLock()) {
                LogUtil.debug("GameWorld", "LockDelay shouldLock (separate timer) triggered lockPiece");
                lockPiece();
            }
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

        // 연쇄 상태 초기화 후 시작
        chainProcessor.reset();
        chainProcessor.startChain();
        
        // 락 딜레이 강제 종료 (이중 잠금 방지)
        lockDelayManager.forceLock();
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

    public void dispose() {
    }
}