package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.PuyoPair;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.util.LogUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 게임 상태 머신: 보드, 현재/다음 쌍, 점수, 연쇄 수, 게임오버 상태 관리.
 * 모든 상태를 GameWorld에서 통합 관리하며, 매니저 클래스들은 순수 기능만 제공.
 */
public class GameWorld {
    private final Board board;
    private final PuyoPairGenerator pairGenerator;
    private final SeparationManager separationManager;
    private final LockDelayManager lockDelayManager = new LockDelayManager();

    private PuyoPair currentPair;
    private PuyoPair nextPair;
    private boolean gameOver = false;
    private int score = 0;
    private float fallTimer = 0f;
    private float fallInterval = 0.5f; // 초당 셀 낙하 속도 (레벨별 조정 가능)

    // ==========================================
    // 통합된 게임 상태 머신 (단순화됨)
    // ==========================================
    public enum GamePhase {
        SPAWNING, // 새 조각 생성/위치 설정
        FALLING_AUTO, // 자동 낙하 (0.5초 간격, 이동/회전 입력 허용)
        LOCK_DELAY, // 락 딜레이 (착지 후 0.5초/15회 이동, 입력 허용)
        SEPARATION, // 락딜레이 종료 후 분리 체크 + 실행
        FALLING_ANIMATION, // 분리/부유 뿌요 낙하 애니메이션
        CHAIN_FINDING, // 연쇄: 매치 탐색
        CHAIN_POP_ANIMATION, // 연쇄: 팝 애니메이션 재생 중
        CHAIN_FLOATING_CHECK, // 연쇄: 부유 뿌요 체크 후 낙하 준비
        GAME_OVER
    }

    private GamePhase gamePhase = GamePhase.SPAWNING;

    // 애니메이션 상태 (GameWorld에서 직접 관리)
    private List<FallingPuyo> fallingPuyos = new ArrayList<>();
    private float fallingAnimationTimer = 0f;
    private static final float FALLING_ANIMATION_INTERVAL = 0.05f;

    // 연쇄 상태
    private List<List<Puyo>> currentGroups = null;
    private int chainCount = 0;

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

        // 초기 상태는 SPAWNING, update()에서 처리
    }

    /** 현재 쌍을 새로 스폰하고 상태 초기화 */
    public void spawnNewPair() {
        // 미리보기로 보여준 nextPair를 현재 쌍으로 사용
        if (nextPair != null) {
            currentPair = nextPair;
        } else {
            currentPair = pairGenerator.generate();
        }
        pairGenerator.positionAtSpawn(currentPair, Board.WIDTH, Board.HEIGHT);
        lockDelayManager.deactivate();
    }

    /** 다음 쌍을 스폰 (미리보기용) */
    public void spawnNextPair() {
        nextPair = pairGenerator.generate();
        // 미리보기용이므로 보드 스폰 위치 설정 불필요 (렌더링 시 고정 좌표 사용)
    }

    /** 현재 쌍이 낙하 가능한지 확인 */
    public boolean canFall() {
        return currentPair != null && board.canMoveDown(currentPair);
    }

    /** 왼쪽 이동 */
    public boolean moveLeft() {
        if (currentPair == null)
            return false;

        // FALLING_AUTO, LOCK_DELAY에서만 이동 허용
        if (gamePhase != GamePhase.FALLING_AUTO && gamePhase != GamePhase.LOCK_DELAY) {
            return false;
        }

        if (board.canMoveLeft(currentPair)) {
            currentPair.moveLeft();
            if (lockDelayManager.isActive()) {
                // 공중으로 빠져나오면 락딜레이 비활성화
                if (canFall()) {
                    lockDelayManager.deactivate();
                } else {
                    lockDelayManager.recordMove();
                }
            }
            return true;
        }
        return false;
    }

    /** 오른쪽 이동 */
    public boolean moveRight() {
        if (currentPair == null)
            return false;

        // FALLING_AUTO, LOCK_DELAY에서만 이동 허용
        if (gamePhase != GamePhase.FALLING_AUTO && gamePhase != GamePhase.LOCK_DELAY) {
            return false;
        }

        if (board.canMoveRight(currentPair)) {
            currentPair.moveRight();
            if (lockDelayManager.isActive()) {
                // 공중으로 빠져나가면 락딜레이 비활성화
                if (canFall()) {
                    lockDelayManager.deactivate();
                } else {
                    lockDelayManager.recordMove();
                }
            }
            return true;
        }
        return false;
    }

    /** 시계방향 회전 (벽 킥 포함) */
    public void rotateClockwise() {
        if (currentPair == null)
            return;

        // FALLING_AUTO, LOCK_DELAY에서만 회전 허용
        if (gamePhase != GamePhase.FALLING_AUTO && gamePhase != GamePhase.LOCK_DELAY) {
            return;
        }

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
        if (lockDelayManager.isActive()) {
            // 회전 후 공중으로 빠져나가면 락딜레이 비활성화
            if (canFall()) {
                lockDelayManager.deactivate();
            } else {
                lockDelayManager.recordMove();
            }
        }
    }

    /** 하드 드롭 */
    public void hardDrop() {
        if (currentPair == null)
            return;

        // FALLING_AUTO, LOCK_DELAY에서만 하드 드롭 허용
        if (gamePhase != GamePhase.FALLING_AUTO && gamePhase != GamePhase.LOCK_DELAY) {
            return;
        }

        while (canFall()) {
            currentPair.moveDown();
        }
        lockPiece();
        startChainFinding();
        LogUtil.debug("GameWorld", "Phase: HARD_DROP -> CHAIN_FINDING");
    }

    /** 소프트 드롭 / 락딜레이 중 이동 기록용 */
    public void recordLockDelayMove() {
        if (lockDelayManager.isActive()) {
            lockDelayManager.recordMove();
        }
    }

    /** 현재 게임 페이즈 반환 (PlayScreen 등에서 입력 제어용) */
    public GamePhase getGamePhase() {
        return gamePhase;
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
            case FALLING_AUTO: {
                handleFallingAuto(delta);
                break;
            }
            case LOCK_DELAY: {
                handleLockDelay(delta);
                break;
            }
            case SEPARATION: {
                handleSeparation();
                break;
            }
            case FALLING_ANIMATION: {
                handleFallingAnimation(delta);
                break;
            }
            case CHAIN_FINDING: {
                handleChainFinding();
                break;
            }
            case CHAIN_POP_ANIMATION: {
                handlePopAnimation(delta);
                break;
            }
            case CHAIN_FLOATING_CHECK: {
                handleFloatingCheck();
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
        currentGroups = null;
        fallingPuyos.clear();
        fallingAnimationTimer = 0f;
        gamePhase = GamePhase.FALLING_AUTO;
        LogUtil.debug("GameWorld", "Phase: SPAWNING -> FALLING_AUTO, new pair spawned");
    }

    /**
     * 자동 낙하 처리 (0.5초 간격)
     * 착지 시 락딜레이 활성화 후 LOCK_DELAY로 전이
     */
    private void handleFallingAuto(float delta) {
        fallTimer += delta;
        if (fallTimer >= fallInterval) {
            fallTimer = 0f;
            if (canFall()) {
                currentPair.moveDown();
                // 자동 낙하 중에는 락딜레이 건드리지 않음 (공중이니까)
            } else {
                // 착지! → 락딜레이 활성화하고 LOCK_DELAY로 전이
                lockDelayManager.activate();
                gamePhase = GamePhase.LOCK_DELAY;
                LogUtil.debug("GameWorld", "Phase: FALLING_AUTO -> LOCK_DELAY (landed, lock delay activated)");
            }
        }
    }

    /**
     * 락 딜레이 단계 처리
     * - 시간/이동횟수 초과 시 SEPARATION으로
     * - 공중 이탈 시 FALLING_AUTO로 복귀
     * - 사용자 입력은 moveLeft/Right/rotate/softDrop에서 recordMove() 호출
     */
    private void handleLockDelay(float delta) {
        lockDelayManager.recordTime(delta);

        if (lockDelayManager.shouldLock()) {
            LogUtil.debug("GameWorld", "LockDelay expired -> SEPARATION");
            gamePhase = GamePhase.SEPARATION;
            return;
        }

        // 공중 이탈 시 락딜레이 해제 → 자동 낙하로
        if (canFall()) {
            lockDelayManager.deactivate();
            gamePhase = GamePhase.FALLING_AUTO;
            LogUtil.debug("GameWorld", "Phase: LOCK_DELAY -> FALLING_AUTO (back in air)");
        }
        // 사용자 입력(moveLeft/Right/rotate/softDrop) 시 recordMove() 호출됨
    }

    /**
     * 분리 체크 + 실행 (통합 단계)
     * 락딜레이 종료 후 호출됨
     */
    private void handleSeparation() {
        if (currentPair != null && separationManager.canSeparate(currentPair, board)) {
            // 분리 가능: 실행
            SeparationManager.SeparationResult sepResult = separationManager.separate(currentPair, board);
            if (sepResult.separated) {
                board.placePuyo(sepResult.blockedPuyo);
                addFallingPuyo(sepResult.freePuyo, FallingPuyo.FallType.FALLING);
                fallingAnimationTimer = 0f;
                lockDelayManager.deactivate();
                currentPair = null;
                gamePhase = GamePhase.FALLING_ANIMATION;
                LogUtil.debug("GameWorld", "Phase: SEPARATION -> FALLING_ANIMATION (separated)");
            } else {
                LogUtil.info("GameWorld", "SEPARATION: canSeparate true but separate() failed -> lockPiece");
                lockPiece();
                startChainFinding();
            }
        } else {
            // 분리 불가: 일반 잠금 → 연쇄 탐색
            LogUtil.debug("GameWorld", "SEPARATION: no separation -> lockPiece -> CHAIN_FINDING");
            lockPiece();
            startChainFinding();
        }
    }

    private void addFallingPuyo(Puyo puyo, FallingPuyo.FallType type) {
        fallingPuyos.add(new FallingPuyo(puyo, type));
    }

    // ==========================================
    // 애니메이션 로직 (기존 유지)
    // ==========================================

    /**
     * 팝 애니메이션 업데이트
     * 
     * @return 모든 팝 완료 여부
     */
    private boolean updatePopAnimation(float delta) {
        boolean allPopDone = true;
        for (FallingPuyo fp : fallingPuyos) {
            if (fp.isChainPop()) {
                boolean popDone = fp.puyo.updatePop(delta);
                if (!popDone) {
                    allPopDone = false;
                }
            }
        }
        return allPopDone;
    }

    /**
     * 팝 완료된 CHAIN_POP 엔트리 수집 및 리스트에서 제거
     * 
     * @return 보드에서 제거할 뿌요들
     */
    private List<Puyo> collectAndClearChainPop() {
        List<Puyo> poppedPuyos = new ArrayList<>();
        List<FallingPuyo> toRemove = new ArrayList<>();
        for (FallingPuyo fp : fallingPuyos) {
            if (fp.isChainPop()) {
                poppedPuyos.add(fp.puyo);
                toRemove.add(fp);
            }
        }
        if (!poppedPuyos.isEmpty()) {
            LogUtil.debug("GameWorld", "collectAndClearChainPop: " + poppedPuyos.size() + " puyos, listSize before="
                    + fallingPuyos.size());
            fallingPuyos.removeAll(toRemove);
            LogUtil.debug("GameWorld",
                    "collectAndClearChainPop: removed CHAIN_POP, listSize after=" + fallingPuyos.size());
        }
        return poppedPuyos;
    }

    /**
     * 통합된 낙하 애니메이션 업데이트 (FALLING 타입 전체 대상)
     * 
     * @param delta 프레임 시간
     * @return 아직 낙하 중이면 true, 완료면 false
     */
    private boolean updateFallingAnimation(float delta) {
        fallingAnimationTimer += delta;
        if (fallingAnimationTimer < FALLING_ANIMATION_INTERVAL) {
            return true; // 아직 시간 안 됨, 낙하 중으로 간주
        }
        fallingAnimationTimer = 0f;

        // 전체 fallingPuyos를 열(column) 단위로 기둥 낙하
        Map<Integer, List<FallingPuyo>> columns = new HashMap<>();
        for (FallingPuyo fp : fallingPuyos) {
            int x = fp.puyo.getX();
            columns.computeIfAbsent(x, k -> new ArrayList<>()).add(fp);
        }

        // 각 열(column)별로 독립적으로 한 칸 이동 처리
        boolean anyMoved = false;
        for (List<FallingPuyo> column : columns.values()) {
            // 열 내 가장 아래쪽 뿌요(최소 Y)만 체크
            FallingPuyo bottomFp = null;
            int minY = Integer.MAX_VALUE;
            for (FallingPuyo fp : column) {
                if (fp.puyo.getY() < minY) {
                    minY = fp.puyo.getY();
                    bottomFp = fp;
                }
            }

            // 가장 아래쪽 뿌요만 체크해서 열 전체 이동 여부 결정
            if (bottomFp != null && canFallInColumn(board, column, bottomFp.puyo)) {
                for (FallingPuyo fp : column) {
                    fp.puyo.moveDown();
                }
                anyMoved = true;
            }
        }

        // 이동했으면 아직 낙하 중
        if (anyMoved) {
            return true;
        }

        // 아무도 이동 못 했으면 완료 체크
        boolean anyCanFall = false;
        for (List<FallingPuyo> column : columns.values()) {
            FallingPuyo bottomFp = null;
            int minY = Integer.MAX_VALUE;
            for (FallingPuyo fp : column) {
                if (fp.puyo.getY() < minY) {
                    minY = fp.puyo.getY();
                    bottomFp = fp;
                }
            }
            if (bottomFp != null && canFallInColumn(board, column, bottomFp.puyo)) {
                anyCanFall = true;
                break;
            }
        }

        return anyCanFall; // true면 아직 낙하 중, false면 완료
    }

    /**
     * 낙하 애니메이션 완료된 뿌요들 수집 및 보드에 배치
     */
    private void collectAndPlaceCompletedFalling() {
        if (fallingPuyos.isEmpty())
            return;
        for (FallingPuyo fp : fallingPuyos) {
            board.placePuyo(fp.puyo);
        }
        LogUtil.debug("GameWorld", "Completed falling puyos placed: " + fallingPuyos.size());
        fallingPuyos.clear();
    }

    /**
     * 특정 열에서 특정 뿌요가 한 칸 아래로 이동 가능한지 체크
     */
    private boolean canFallInColumn(Board board, List<FallingPuyo> column, Puyo puyo) {
        int x = puyo.getX();
        int targetY = puyo.getY() - 1;

        if (targetY < 0) {
            return false; // 바닥
        }

        // 보드 그리드 체크
        if (board.getPuyoAt(x, targetY) != null) {
            return false; // 보드에 다른 뿌요가 있음
        }

        // 같은 열의 다른 falling puyos 체크
        for (FallingPuyo fp : column) {
            if (fp != null && fp.puyo != puyo && fp.puyo.getX() == x && fp.puyo.getY() == targetY) {
                return false; // 같은 열의 다른 falling puyo가 있음
            }
        }

        return true;
    }

    /**
     * 통합된 낙하 애니메이션 핸들러 (분리/부유 통합: FALLING 타입)
     */
    private void handleFallingAnimation(float delta) {
        // FALLING 타입만 처리하면 됨 (CHAIN_POP은 별도 처리됨)
        boolean hasFalling = false;
        for (FallingPuyo fp : fallingPuyos) {
            if (fp.isFalling()) {
                hasFalling = true;
                break;
            }
        }

        if (!hasFalling) {
            // 처리할 것이 없으면 CHAIN_FINDING으로
            gamePhase = GamePhase.CHAIN_FINDING;
            return;
        }

        boolean stillFalling = updateFallingAnimation(delta);

        if (!stillFalling) {
            // 낙하 완료: 보드에 배치
            collectAndPlaceCompletedFalling();

            // 정리
            currentPair = null;
            lockDelayManager.deactivate();

            // 무조건 연쇄 찾기 단계로
            gamePhase = GamePhase.CHAIN_FINDING;
            LogUtil.debug("GameWorld", "Phase: FALLING_ANIMATION -> CHAIN_FINDING");
        }
    }

    private void handleChainFinding() {
        currentGroups = MatchFinder.findAllMatchingGroups(board);

        if (currentGroups.isEmpty()) {
            // 연쇄 종료
            LogUtil.debug("GameWorld", "Chain ended. chainCount=" + chainCount);
            gamePhase = GamePhase.SPAWNING;
            return;
        }

        // 새 연쇄 단계 시작
        chainCount++;
        int removed = currentGroups.stream().mapToInt(List::size).sum();
        LogUtil.debug("GameWorld", "New chain step: chainCount=" + chainCount + ", groups=" + currentGroups.size()
                + ", removed=" + removed);

        // 팝 애니메이션 시작
        for (List<Puyo> group : currentGroups) {
            for (Puyo puyo : group) {
                if (!puyo.isPopping()) {
                    puyo.startPop();
                }
                addFallingPuyo(puyo, FallingPuyo.FallType.CHAIN_POP);
            }
        }
        gamePhase = GamePhase.CHAIN_POP_ANIMATION;
        LogUtil.debug("GameWorld", "Phase: CHAIN_FINDING -> CHAIN_POP_ANIMATION, fallingPuyos=" + fallingPuyos.size());
    }

    private void handlePopAnimation(float delta) {
        boolean allPopDone = updatePopAnimation(delta);

        if (allPopDone) {
            // 팝 완료: CHAIN_POP 엔트리 수집 및 제거, 보드에서 제거
            List<Puyo> poppedPuyos = collectAndClearChainPop();
            if (!poppedPuyos.isEmpty()) {
                for (Puyo p : poppedPuyos) {
                    board.removePuyo(p);
                }
                LogUtil.debug("GameWorld", "Popped puyos removed: " + poppedPuyos.size());
            }

            // 부유 뿌요 체크 단계로
            gamePhase = GamePhase.CHAIN_FLOATING_CHECK;
            LogUtil.debug("GameWorld", "Phase: CHAIN_POP_ANIMATION -> CHAIN_FLOATING_CHECK");
        }
    }

    private void handleFloatingCheck() {
        // 부유 뿌요 확인
        List<Puyo> floating = board.getAllFloatingPuyos();
        if (!floating.isEmpty()) {
            LogUtil.debug("GameWorld", "Found " + floating.size() + " floating puyos, starting falling animation");
            for (Puyo p : floating) {
                board.removePuyo(p);
                addFallingPuyo(p, FallingPuyo.FallType.FALLING);
            }
            fallingAnimationTimer = 0f;
            gamePhase = GamePhase.FALLING_ANIMATION;
            LogUtil.debug("GameWorld", "Phase: CHAIN_FLOATING_CHECK -> FALLING_ANIMATION (FALLING)");
        } else {
            // 부유 없으면 다음 연쇄 단계
            gamePhase = GamePhase.CHAIN_FINDING;
        }
    }

    /** 현재 조각 잠금 (보드에 배치) */
    private void lockPiece() {
        if (currentPair == null)
            return;

        for (Puyo p : currentPair.getPuyos()) {
            if (p.isAlive()) {
                board.placePuyo(p);
            }
        }

        lockDelayManager.deactivate();
        currentPair = null;
        LogUtil.debug("GameWorld", "lockPiece completed");
    }

    /** 연쇄 찾기 단계 초기화 및 전이 */
    private void startChainFinding() {
        chainCount = 0;
        currentGroups = null;
        gamePhase = GamePhase.CHAIN_FINDING;
        LogUtil.debug("GameWorld", "Phase: -> CHAIN_FINDING (chain init)");
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

    /**
     * @deprecated FallType이 FALLING으로 통합됨. {@link #getFallingPuyos()} 사용 권장.
     *             호환용: 낙하 중인 단일 뿌요 반환 (첫 번째 FALLING 타입)
     */
    @Deprecated
    public Puyo getFallingSinglePuyo() {
        for (FallingPuyo fp : fallingPuyos) {
            if (fp.isFalling()) {
                return fp.puyo;
            }
        }
        return null;
    }

    /** 호환용: 모든 낙하 중인 뿌요 리스트 반환 (렌더링용) */
    public List<FallingPuyo> getFallingPuyos() {
        return new ArrayList<>(fallingPuyos);
    }

    public int getScore() {
        return score;
    }

    public void addScore(int points) {
        this.score += points;
    }

    public int getCurrentChain() {
        return chainCount;
    }

    public void dispose() {
    }
}