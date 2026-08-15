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
 * 실제 로직은 각 매니저 클래스에 위임.
 */
public class GameWorld {
    private final Board board;
    private final PuyoPairGenerator pairGenerator;
    private final LockDelayManager lockDelayManager;
    private final SeparationManager separationManager;
    private final FallingAnimationManager fallingAnimationManager;
    private final GravityEngine gravityEngine;

    private PuyoPair currentPair;
    private PuyoPair nextPair;
    private boolean gameOver = false;
    private int score = 0;
    private float fallTimer = 0f;
    private float fallInterval = 0.5f; // 초당 셀 낙하 속도 (레벨별 조정 가능)
    private int currentChain = 0;
    private int totalRemoved = 0;

    // 연쇄 처리 상태 머신 (기존 ChainProcessor 내장)
    private enum ChainPhase {
        FINDING_MATCHES,    // 연쇄 시작점 + 다음 단계 진입점
        WAITING_POP,        // 팝 애니메이션 대기 중
        CHECKING_FLOATING,  // 부유 뿌요 확인 및 낙하 애니메이션 추가 중
        INACTIVE            // 연쇄 비활성 (구 IDLE/DONE 통합)
    }
    private ChainPhase chainPhase = ChainPhase.INACTIVE;
    private List<List<Puyo>> currentGroups = null;
    private int chainCount = 0;
    private int totalChainRemoved = 0;

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
            FallingAnimationManager.UpdateResult animResult = fallingAnimationManager.update(delta, board);
            
            // 액션 실행
            switch (animResult.action) {
                case REMOVE_POPPED:
                    // 팝 완료된 뿌요들 보드에서 제거
                    LogUtil.debug("GameWorld", "☆☆☆☆☆☆☆ REMOVE_POPPED received: " + (animResult.puyos != null ? animResult.puyos.size() : 0) + " puyos, fallingAnim size before=" + fallingAnimationManager.getFallingPuyos().size());
                    if (animResult.puyos != null) {
                        for (Puyo p : animResult.puyos) {
                            board.removePuyo(p);
                        }
                    }
                    LogUtil.debug("GameWorld", "☆☆☆☆☆☆☆ REMOVE_POPPED done: fallingAnim size after=" + fallingAnimationManager.getFallingPuyos().size());
                    break;
                    
                case PLACE_SEPARATED:
                    // 분리 낙하 완료된 뿌요들 보드에 배치
                    if (animResult.puyos != null) {
                        for (Puyo p : animResult.puyos) {
                            board.placePuyo(p);
                        }
                    }
                    break;
                    
                case PLACE_FLOATING:
                    // 부유 낙하 완료된 뿌요들 보드에 배치
                    if (animResult.puyos != null) {
                        for (Puyo p : animResult.puyos) {
                            board.placePuyo(p);
                        }
                    }
                    break;
                    
                case NONE:
                default:
                    // 아무 액션 없음 (애니메이션 진행 중)
                    break;
            }

            if (animResult.done) {
                // 애니메이션 완료: 정리
                fallingAnimationManager.clear();

                // 분리/부유 낙하 완료 후 보드에 매치가 있는지 확인
                // 매치가 있으면 즉시 연쇄 처리 시작 (지연 방지)
                List<List<Puyo>> groups = MatchFinder.findAllMatchingGroups(board);
                if (!groups.isEmpty()) {
                    LogUtil.debug("GameWorld", "Animation done: found " + groups.size() + " match groups, starting chain");
                    startChain();
                }

                // 분리 애니메이션 완료 후 currentPair가 null이면 다음 쌍 스폰
                if (currentPair == null && !hasActiveChain()) {
                    currentPair = nextPair;
                    spawnNextPair();
                    fallTimer = 0f;
                    lockDelayManager.reset();
                    resetChain();
                }

                // 연쇄 처리 계속 진행 (상태 머신이 다음 단계 처리)
            }
            return;
        }

        // 2. 연쇄 처리 상태 머신 업데이트 (연쇄가 진행 중일 때만)
        if (hasActiveChain()) {
            updateChain(delta);
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
                // 분리 실행 (액션만 반환, GameWorld가 보드 조작 수행)
                SeparationManager.SeparationResult sepResult = separationManager.separate(currentPair, board);
                if (sepResult.separated) {
                    // 막힌 쪽 즉시 잠금
                    LogUtil.debug("GameWorld", "Placing blocked puyo at (" + sepResult.blockedPuyo.getX() + "," + sepResult.blockedPuyo.getY()
                            + ") color=" + sepResult.blockedPuyo.getColor());
                    board.placePuyo(sepResult.blockedPuyo);

                    // 자유로운 쪽 단일 뿌요로 자동 낙하 시작
                    LogUtil.debug("GameWorld", "Adding free puyo to falling: (" + sepResult.freePuyo.getX() + "," + sepResult.freePuyo.getY()
                            + ") color=" + sepResult.freePuyo.getColor() + " hash=" + System.identityHashCode(sepResult.freePuyo));
                    fallingAnimationManager.addSeparationFalling(sepResult.freePuyo);
                }
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

        // 4. 연쇄도 애니메이션도 없고 현재 조각도 없으면 다음 조각 스폰
        if (currentPair == null && !hasActiveChain() && fallingAnimationManager.isEmpty()) {
            LogUtil.debug("GameWorld", "No active piece/chain/anim, spawning next pair");
            currentPair = nextPair;
            spawnNextPair();
            fallTimer = 0f;
            lockDelayManager.reset();
            resetChain();
        }
    }

    // ===== 연쇄 처리 내부 메서드들 (기존 ChainProcessor 로직 인라인) =====

    /**
     * 연쇄 처리 상태 업데이트 (비동기식, 프레임당 1스텝)
     */
    private void updateChain(float delta) {
        LogUtil.debug("GameWorld", "=== updateChain START ===");
        LogUtil.debug("GameWorld", "Phase: " + chainPhase +
                ", chainCount=" + chainCount +
                ", totalRemoved=" + totalChainRemoved +
                ", fallingAnim.isEmpty()=" + fallingAnimationManager.isEmpty() +
                ", fallingAnim.size=" + fallingAnimationManager.getFallingPuyos().size());

        switch (chainPhase) {
            /*case IDLE:
                LogUtil.debug("GameWorld", "Phase IDLE -> FINDING_MATCHES");
                chainPhase = ChainPhase.FINDING_MATCHES;
                return;*/

            case FINDING_MATCHES:
                LogUtil.debug("GameWorld", "Finding next matching groups...");
                List<List<Puyo>> groups = MatchFinder.findAllMatchingGroups(board);

                if (groups.isEmpty()) {
                    // 매칭 없음 - 연쇄 종료
                    LogUtil.debug("GameWorld", "No more matches. Chain ending. totalRemoved=" + totalChainRemoved
                            + ", chainCount=" + chainCount);
                    chainPhase = ChainPhase.INACTIVE;
                    // 스폰은 메인 update 루프에서 처리 (중복 방지)
                    return;
                }

                // 새 연쇄 단계 시작
                chainCount++;
                currentGroups = groups;
                LogUtil.debug("GameWorld", "New chain step: chainCount=" + chainCount + ", groups=" + groups.size());
                for (int i = 0; i < groups.size(); i++) {
                    LogUtil.debug("GameWorld",
                            "  Group " + i + ": color=" + groups.get(i).get(0).getColor() + ", size=" + groups.get(i).size());
                }

                // 팝 애니메이션 시작
                LogUtil.debug("GameWorld", "Action: START_POP for " + groups.size() + " groups");
                chainPhase = ChainPhase.WAITING_POP;
                for (List<Puyo> group : groups) {
                    fallingAnimationManager.addChainFalling(group);
                }
                return;

            case WAITING_POP:
                // 팝 애니메이션 대기 중이면 스킵 (FallingAnimationManager가 처리 중)
                if (!fallingAnimationManager.isEmpty()) {
                    return; // 아직 팝 애니메이션 진행 중
                }
                // 팝 애니메이션 완료됨
                LogUtil.debug("GameWorld", "Pop animation completed");
                
                // 팝 완료: 카운트만
                if (currentGroups != null) {
                    int removed = currentGroups.stream().mapToInt(List::size).sum();
                    totalChainRemoved += removed;
                    LogUtil.debug("GameWorld", "Pop completed, removed " + removed + " puyos");
                }

                // 중력 적용
                LogUtil.debug("GameWorld", "Action: APPLY_GRAVITY");
                //chainPhase = ChainPhase.APPLYING_GRAVITY;
                chainPhase = ChainPhase.CHECKING_FLOATING;
                gravityEngine.applyGravity(board);
                return;

            /*case APPLYING_GRAVITY:
                // 중력 적용 완료 후 부유 확인
                LogUtil.debug("GameWorld", "Action: CHECK_FLOATING");
                chainPhase = ChainPhase.CHECKING_FLOATING;
                // 바로 다음 프레임에서 CHECKING_FLOATING 처리되도록 return
                return;*/

            case CHECKING_FLOATING:
                // 부유 뿌요 낙하 애니메이션 대기 중
                if (!fallingAnimationManager.isEmpty()) {
                    LogUtil.debug("GameWorld", "CHECKING_FLOATING: fallingAnim not empty, waiting... size=" + fallingAnimationManager.getFallingPuyos().size());
                    return; // 부유 낙하 애니메이션 진행 중
                }
                
                // 부유 뿌요들 확인 및 낙하 애니메이션 추가
                List<Puyo> floating = board.getAllFloatingPuyos();
                LogUtil.debug("GameWorld", "CHECKING_FLOATING: floating.size()=" + floating.size() + ", board:\n" + board.toString());
                if (!floating.isEmpty()) {
                    LogUtil.debug("GameWorld", "Found " + floating.size() + " floating puyos, Action: CHECK_FLOATING");
                    for (Puyo p : floating) {
                        LogUtil.debug("GameWorld", "  Floating puyo at (" + p.getX() + "," + p.getY() + ") color=" + p.getColor());
                    }
                    for (Puyo p : floating) {
                        board.removePuyo(p);
                    }
                    fallingAnimationManager.addFloatingPuyos(floating);
                    LogUtil.debug("GameWorld", "CHECKING_FLOATING -> re-enter (waiting for fall animation)");
                    return;
                }
                
                // 부유 뿌요 없으면 다음 연쇄 단계로
                LogUtil.debug("GameWorld", "No floating puyos, Action: NEXT_CHAIN_STEP");
                chainPhase = ChainPhase.FINDING_MATCHES;
                return;

            //case DONE:
            case INACTIVE:    
                return;

            default:
                //chainPhase = ChainPhase.DONE;
                chainPhase = ChainPhase.INACTIVE;
                return;
        }
    }

    /**
     * 연쇄 상태 리셋 (새 조각 스폰 시)
     */
    private void resetChain() {
        chainPhase = ChainPhase.INACTIVE;
        currentGroups = null;
        chainCount = 0;
        totalChainRemoved = 0;
    }

    /**
     * 연쇄 처리 시작 (lockPiece 후 호출)
     */
    private void startChain() {
        chainPhase = ChainPhase.FINDING_MATCHES;
    }

    /**
     * 연쇄가 진행 중인지 확인
     */
    private boolean hasActiveChain() {
        return chainPhase != ChainPhase.INACTIVE;
    }

    /**
     * 현재 조각 잠금 및 연쇄 처리 시작
     */
    private void lockPiece() {
        if (currentPair == null)
            return;

        for (Puyo p : currentPair.getPuyos()) {
            if (p.isAlive()) {
                board.placePuyo(p);
            }
        }

        // 연쇄 상태 초기화 후 시작
        resetChain();
        startChain();
        
        // 락 딜레이 강제 종료 (이중 잠금 방지)
        lockDelayManager.forceLock();
        currentPair = null;
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