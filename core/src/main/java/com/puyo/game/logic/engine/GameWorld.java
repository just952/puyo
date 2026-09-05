package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.PuyoPair;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.util.LogUtil;
import com.puyo.game.input.InputProvider;
import com.puyo.game.input.InputCommand;

import java.util.ArrayList;
import java.util.List;

/**
 * 게임 상태 머신: 보드, 현재/다음 쌍, 점수, 연쇄 수, 게임오버 상태 관리.
 * 모든 상태를 GameWorld에서 통합 관리하며, 매니저 클래스들은 순수 기능만 제공.
 */
public class GameWorld {
    private final Board board;
    private final PuyoPairGenerator pairGenerator;
    private final SeparationManager separationManager;
    private final LockDelayManager lockDelayManager = new LockDelayManager();
    private final ChainManager chainManager = new ChainManager();

    private PuyoPair currentPair;
    private PuyoPair nextPair;
    private PuyoPair heldPair; // 홀드된 조각
    private boolean holdUsed = false; // 현재 조각에서 홀드 사용 여부 (한 조각당 1회 제한)
    private boolean gameOver = false;
    private int score = 0;
    private float fallTimer = 0f;
    private float fallInterval = 0.4f; // 초당 셀 낙하 속도 (레벨별 조정 가능)
    private int frameCount = 0; // 디버그용 프레임 카운터

    /** 새 조각이 방금 스폰되었는지 여부 (한 프레임만 true, DAS 리셋용) */
    private boolean justSpawned = false;

    // ==========================================
    // 통합된 게임 상태 머신 (단순화됨)
    // ==========================================
    public enum GamePhase {
        SPAWNING, // 새 조각 생성/위치 설정
        FALLING_AUTO, // 자동 낙하 (0.5초 간격, 이동/회전 입력 허용)
        LOCK_DELAY, // 락 딜레이 (착지 후 0.5초/15회 이동, 입력 허용)
        SEPARATION, // 락딜레이 종료 후 분리 체크 + 실행
        PUYO_EFFECT_ANIMATION, // 분리/부유 뿌요 낙하 애니메이션 + 착지 바운스
        CHAIN_FINDING, // 연쇄: 매치 탐색
        CHAIN_POP_ANIMATION, // 연쇄: 팝 애니메이션 재생 중
        CHAIN_FLOATING_CHECK, // 연쇄: 부유 뿌요 체크 후 낙하 준비
        GAME_OVER
    }

    private GamePhase gamePhase = GamePhase.SPAWNING;

    // 애니메이션 상태 (GameWorld에서 직접 관리) - Puyo 직접 관리
    private List<Puyo> animatingPuyos = new ArrayList<>();
    private float fallingAnimationTimer = 0f;
    private static final float FALLING_ANIMATION_INTERVAL = 0.025f;

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
        pairGenerator.positionAtSpawn(currentPair, Board.WIDTH, Board.TOTAL_HEIGHT);
        lockDelayManager.deactivate();
        justSpawned = true; // 새 조각 스폰 알림 (DAS 리셋용, 한 프레임 후 자동 해제)
        holdUsed = false; // 새 조각 스폰 시 홀드 사용 가능하도록 리셋
    }

    /** 다음 쌍을 스폰 (미리보기용) */
    public void spawnNextPair() {
        nextPair = pairGenerator.generate();
        // 미리보기용이므로 보드 스폰 위치 설정 불필요 (렌더링 시 고정 좌표 사용)
    }

    /** 현재 쌍(PuyoPair)이 낙하 가능한지 확인 */
    private boolean canPuyoPairFall() {
        return currentPair != null && board.canMoveDown(currentPair);
    }

    /** 왼쪽 이동 */
    public boolean movePairLeft() {
        if (currentPair == null)
            return false;

        // FALLING_AUTO, LOCK_DELAY에서만 이동 허용
        if (gamePhase != GamePhase.FALLING_AUTO && gamePhase != GamePhase.LOCK_DELAY) {
            return false;
        }

        if (board.canMoveLeft(currentPair)) {
            currentPair.moveLeft();

            for (Puyo p : currentPair.getPuyos()) {
                if (p.isAlive() && p.isLockWaiting()) {  // SETTLING || PENDING
                    p.setState(Puyo.State.MOVABLE);
                    animatingPuyos.remove(p);
                }
            }

            if (lockDelayManager.isActive()) {
                // 공중으로 빠져나가면 락딜레이 비활성화
                if (canPuyoPairFall()) {
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
    public boolean movePairRight() {
        if (currentPair == null)
            return false;

        // FALLING_AUTO, LOCK_DELAY에서만 이동 허용
        if (gamePhase != GamePhase.FALLING_AUTO && gamePhase != GamePhase.LOCK_DELAY) {
            return false;
        }

        if (board.canMoveRight(currentPair)) {
            currentPair.moveRight();

            for (Puyo p : currentPair.getPuyos()) {
                if (p.isAlive() && p.isLockWaiting()) {  // SETTLING || PENDING
                    p.setState(Puyo.State.MOVABLE);
                    animatingPuyos.remove(p);
                }
            }

            if (lockDelayManager.isActive()) {
                // 공중으로 빠져나가면 락딜레이 비활성화
                if (canPuyoPairFall()) {
                    lockDelayManager.deactivate();
                } else {
                    lockDelayManager.recordMove();
                }
            }
            return true;
        }
        return false;
    }

    /** 회전 (벽 킥 포함) */
    public void rotatePair(boolean isClockwise) {
        if (currentPair == null)
            return;

        // FALLING_AUTO, LOCK_DELAY에서만 회전 허용
        if (gamePhase != GamePhase.FALLING_AUTO && gamePhase != GamePhase.LOCK_DELAY) {
            return;
        }

        if (isClockwise) {
            currentPair.rotateClockwise();
        } else {
            currentPair.rotateCounterClockwise();
        }

        if (!board.canPlace(currentPair)) {
            if (board.canMoveLeft(currentPair)) {
                currentPair.moveLeft();
            } else if (board.canMoveRight(currentPair)) {
                currentPair.moveRight();
            } else {
                if (isClockwise) {
                    currentPair.rotateCounterClockwise(); // 되돌리기
                } else {
                    currentPair.rotateClockwise(); // 되돌리기
                }
            }
        }
        // 회전 성공 시: SETTLING/PENDING → MOVABLE 취소
        for (Puyo p : currentPair.getPuyos()) {
            if (p.isAlive() && p.isLockWaiting()) {
                p.setState(Puyo.State.MOVABLE);
                animatingPuyos.remove(p);
            }
        }

        if (lockDelayManager.isActive()) {
            // 회전 후 공중으로 빠져나가면 락딜레이 비활성화
            if (canPuyoPairFall()) {
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

        while (canPuyoPairFall()) {
            currentPair.moveDown();
        }

        lockDelayManager.deactivate();
        gamePhase = GamePhase.SEPARATION;
        LogUtil.debug("GameWorld", "hardDrop landed ->SEPARATION (bypass lock delay)");
    }

    /** 소프트 드롭: 한 칸 내리고 착지 시 락딜레이 우회하여 SEPARATION으로 전이 */
    public boolean softDrop() {
        if (currentPair == null)
            return false;

        // FALLING_AUTO, LOCK_DELAY에서만 허용
        if (gamePhase != GamePhase.FALLING_AUTO && gamePhase != GamePhase.LOCK_DELAY) {
            return false;
        }

        if (canPuyoPairFall()) {
            currentPair.moveDown();
            //LogUtil.debug("GameWorld", "softDrop moved down, currentPair=" + currentPair);
            // 공중 상태면 락딜레이 비활성화
            if (lockDelayManager.isActive()) {
                lockDelayManager.deactivate();
            }
            return true; // 이동함
        } else {
            // 착지 → 락딜레이 우회하고 즉시 SEPARATION으로 전이 (분리 체크 수행)
            lockDelayManager.deactivate();
            gamePhase = GamePhase.SEPARATION;
            LogUtil.debug("GameWorld", "softDrop landed -> SEPARATION (bypass lock delay)");
            return false; // 이동 못함(착지함)
        }
    }

    /** 홀드: 현재 조각을 홀드 슬롯과 교체 (한 조각당 1회 제한) */
    public void hold() {
        if (currentPair == null)
            return;

        // FALLING_AUTO, LOCK_DELAY에서만 홀드 허용
        if (gamePhase != GamePhase.FALLING_AUTO && gamePhase != GamePhase.LOCK_DELAY) {
            return;
        }

        // 이미 홀드 사용했으면 무시
        if (holdUsed) {
            return;
        }

        if (heldPair == null) {
            // 홀드 슬롯이 비어있으면 현재 조각을 홀드로 이동하고 다음 조각 스폰
            heldPair = currentPair;
            heldPair.resetRotation(); // 회전 상태 초기화
            pairGenerator.positionAtSpawn(heldPair, Board.WIDTH, Board.TOTAL_HEIGHT);
            spawnNewPair(); // nextPair가 currentPair가 되고 새 nextPair 생성
            holdUsed = true;
            LogUtil.debug("GameWorld", "Hold: stored current pair, spawned next");
        } else {
            // 홀드 슬롯에 조각이 있으면 현재 조각과 교체
            PuyoPair temp = currentPair;
            currentPair = heldPair;
            heldPair = temp;
            heldPair.resetRotation(); // 회전 상태 초기화
            pairGenerator.positionAtSpawn(currentPair, Board.WIDTH, Board.TOTAL_HEIGHT);
            holdUsed = true;
            LogUtil.debug("GameWorld", "Hold: swapped with held pair");
        }

        // 락딜레이 리셋
        lockDelayManager.deactivate();
        fallTimer = 0f;
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

    /** 새 조각이 방금 스폰되었는지 여부 (한 프레임만 true, DAS 리셋용) */
    public boolean isJustSpawned() {
        return justSpawned;
    }

    /** 메인 업데이트 루프 - 입력 처리 포함 */
    public void update(float delta, InputProvider input) {
        // 1. 입력 업데이트 및 명령 획득
        input.update(delta);
        InputCommand cmd = input.pollCommand();

        frameCount++;
        if (frameCount % 300 == 0) {
            LogUtil.debug("GameWorld",
                    "Phase: " + gamePhase + ", score=" + score + ", chain=" + chainManager.getChainCount());
        }

        // 2. 게임오버 시 재시작 입력만 처리
        if (gamePhase == GamePhase.GAME_OVER) {
            if (cmd.restartPressed()) {
                restartGame();
            }
            return;
        }

        // 3. justSpawned 플래그 클리어 (한 프레임만 유지)
        if (justSpawned) {
            justSpawned = false;
        }

        // 4. 입력 처리 (FALLING_AUTO, LOCK_DELAY에서만) - switch 앞!
        handleFallingInput(cmd);

        // 5. 상태 머신 처리
        switch (gamePhase) {
            case SPAWNING:
                handleSpawning();
                break;
            case FALLING_AUTO:
                handleFallingAuto(delta);
                break;
            case LOCK_DELAY:
                handleLockDelay(delta);
                break;
            case SEPARATION:
                handleSeparation();
                break;
            case PUYO_EFFECT_ANIMATION:
                handlePuyoEffectAnimation(delta);
                break;
            case CHAIN_FINDING:
                handleChainFinding();
                break;
            case CHAIN_POP_ANIMATION:
                handlePopAnimation(delta);
                break;
            case CHAIN_FLOATING_CHECK:
                handleFloatingCheck();
                break;
            case GAME_OVER:
                break;
        }
    }

    /** 낙하 중 입력 처리 (FALLING_AUTO, LOCK_DELAY에서만 동작) */
    private void handleFallingInput(InputCommand cmd) {
        // 해당 페이즈가 아니면 즉시 리턴
        if (gamePhase != GamePhase.FALLING_AUTO && gamePhase != GamePhase.LOCK_DELAY) {
            return;
        }
        if (currentPair == null)
            return;

        // 좌우 이동
        if (cmd.moveDirection() != 0) {
            if (cmd.moveDirection() < 0)
                movePairLeft();
            else
                movePairRight();
        }

        // 회전 (시계방향)
        if (cmd.rotatePressed())
            rotatePair(true);

        // 회전 (반시계방향) - 롱프레스/Z키
        if (cmd.rotateCounterClockwisePressed())
            rotatePair(false);

        // 소프트 드롭
        if (cmd.dropPressed())
            softDrop();

        // 하드 드롭
        if (cmd.hardDropPressed())
            hardDrop();

        // 홀드
        if (cmd.holdPressed())
            hold();
    }

    // ==========================================
    // 각 Phase별 처리 메서드
    // ==========================================

    private void handleSpawning() {
        spawnNewPair();
        spawnNextPair();
        // 게임 오버 체크: 스폰 위치에 배치 불가능하면 게임 오버
        if (!board.canPlace(currentPair)) {
            gameOver = true;
            gamePhase = GamePhase.GAME_OVER;
            LogUtil.info("GameWorld", "GAME OVER: Cannot place new pair at spawn");
            return;
        }
        fallTimer = 0f;
        chainManager.startNewChain();
        animatingPuyos.clear();
        fallingAnimationTimer = 0f;
        gamePhase = GamePhase.FALLING_AUTO;
        LogUtil.debug("GameWorld", "Phase: SPAWNING -> FALLING_AUTO, new pair spawned");
    }

    /**
     * 자동 낙하 처리 (0.5초 간격)
     * 착지 시 락딜레이 활성화 후 LOCK_DELAY로 전이
     * fallInterval 도래하면 내려갈 수 있나 검사. 가능하면 한칸 내림. 안되면 lockDalay 시작.
     */
    private void handleFallingAuto(float delta) {
        fallTimer += delta;
        if (fallTimer >= fallInterval) {
            fallTimer = 0f;

            if (canPuyoPairFall()) {
                currentPair.moveDown();
                // 자동 낙하 중에는 락딜레이 건드리지 않음 (공중이니까)
            } else {
                // 착지! → 착지한 뿌요만 SETTLING 시작 + 락딜레이 활성화
                if ( currentPair.getRotation() == 0 || currentPair.getRotation() == 2 ) {
                for (Puyo p : currentPair.getPuyos()) {
                        p.setState(Puyo.State.SETTLING);
                        animatingPuyos.add(p);
                        LogUtil.debug("GameWorld",
                                "handleFallingAuto: added Vertical SETTLING puyo at (" + p.getX() + "," + p.getY() + "," + p.getColor() + ") on landing");
                    }
                } else {
                    for (Puyo p : currentPair.getPuyos()) {
                        if (p.isAlive() && !board.canMoveDown(p)) { // 개별 뿌요 착지 체크
                            p.setState(Puyo.State.SETTLING);
                            animatingPuyos.add(p);
                            LogUtil.debug("GameWorld",
                                    "handleFallingAuto: added Horizontal SETTLING puyo at (" + p.getX() + "," + p.getY() + "," + p.getColor() + ") on landing");
                        }
                    }
                }
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

        // 락딜레이 중에도 SETTLING 애니메이션 진행 (내부에서 PENDING으로 자동 전이)
        updateSettlingAnimation(delta);

        if (lockDelayManager.shouldLock()) {
            LogUtil.debug("GameWorld", "LockDelay expired -> SEPARATION");
            gamePhase = GamePhase.SEPARATION;
            return;
        }

        // 공중 이탈 시: SETTLING/PENDING 취소 → FALLING_AUTO
        if (canPuyoPairFall()) {
            cancelPuyoPairSettling();
            lockDelayManager.deactivate();
            gamePhase = GamePhase.FALLING_AUTO;
            LogUtil.debug("GameWorld", "Phase: LOCK_DELAY -> FALLING_AUTO (back in air)");
            return;
        }

        // 착지 상태인데 이동/회전으로 settling이 취소됐을 수 있음 → 재착지 체크 후 재시작
        restartSettlingForLandedPuys();
    }

    // 착지한 뿌요만 SETTLING 재시작 (이미 settling/pending이면 스킵)
    private void restartSettlingForLandedPuys() {
        if (currentPair == null) return;
        for (Puyo p : currentPair.getPuyos()) {
            if (p.isAlive() && p.getState() == Puyo.State.MOVABLE && !board.canMoveDown(p)) {
                p.setState(Puyo.State.SETTLING);
                animatingPuyos.add(p);
                LogUtil.debug("GameWorld", "restartSettling: added SETTLING puyo at (" + p.getX() + "," + p.getY() + ")");
            }
        }
    }

    // 현재 쌍의 모든 SETTLING/PENDING 취소 → MOVABLE
    private void cancelPuyoPairSettling() {
        if (currentPair != null) {
            for (Puyo p : currentPair.getPuyos()) {
                if (p.isAlive() && p.isLockWaiting()) {  // SETTLING || PENDING
                    p.setState(Puyo.State.MOVABLE);
                    animatingPuyos.remove(p);
                }
            }
        }
    }

    /**
     * 분리 체크 + 실행 (통합 단계)
     * 락딜레이 종료 후 호출됨
     */
    private void handleSeparation() {
        if (currentPair == null) {
            LogUtil.debug("GameWorld", "SEPARATION: currentPair is null, skipping");
            gamePhase = GamePhase.CHAIN_FINDING;
            return;
        } else {

            if (separationManager.canSeparate(currentPair, board)) {
                // 분리 가능: 실행
                SeparationManager.SeparationResult sepResult = separationManager.separate(currentPair, board);
                if (sepResult.separated) {
                    // board.placePuyo(sepResult.blockedPuyo); // 고정된 뿌요 보드배치
                    Puyo blockedPuyo = sepResult.blockedPuyo;
                    if ( !blockedPuyo.isLockWaiting()) {
                        blockedPuyo.setState(Puyo.State.SETTLING);
                        animatingPuyos.add(blockedPuyo);
                    }

                    // 자유 낙하하는 뿌요는 FALLING 상태로 animatingPuyos에 추가
                    Puyo freePuyo = sepResult.freePuyo;
                    freePuyo.setState(Puyo.State.FALLING);
                    animatingPuyos.add(freePuyo);

                    LogUtil.debug("GameWorld", "Phase: SEPARATION -> PUYO_EFFECT_ANIMATION (separated)");
                } else {
                    // 여기로 들어 올 것 같지 않음.
                    LogUtil.info("GameWorld", "SEPARATION: canSeparate true but separate() failed -> startSettling");
                }
            } else {
                // 분리 불가: softDrop/hardDrop 경로만 SETTLING 시작
                // 자동 낙하 경로(LOCK_DELAY)는 이미 SETTLING/PENDING 상태
                for (Puyo p : currentPair.getPuyos()) {
                    if (p.isAlive() && p.getState() == Puyo.State.MOVABLE) {
                        p.setState(Puyo.State.SETTLING);
                        animatingPuyos.add(p);
                        LogUtil.debug("GameWorld",
                                "startSettling: added SETTLING puyo at (" + p.getX() + "," + p.getY() + ")");
                    }
                }
                LogUtil.debug("GameWorld",
                        "Phase: SEPARATION -> PUYO_EFFECT_ANIMATION (settle bounce), puyos=" + animatingPuyos.size());
            }

            lockDelayManager.deactivate();
            // currentPair = null;
            fallingAnimationTimer = 0f;
            gamePhase = GamePhase.PUYO_EFFECT_ANIMATION;
        }
    }

    /**
     * 통합된 이펙트 애니메이션 핸들러 (분리/부유 통합: FALLING + SETTLING 바운스)
     * 각 업데이트 메서드가 내부에서 할 일 없음을 판단하므로 사전 체크 불필요.
     */
    private void handlePuyoEffectAnimation(float delta) {
        // animatingPuyos가 비어있으면 즉시 전이 (최소한의 가드만)
        if (animatingPuyos.isEmpty()) {
            gamePhase = GamePhase.CHAIN_FINDING;
            return;
        }

        // 각 업데이트 메서드가 내부에서 "할 일 없음" 판단
        boolean stillFalling = updateFallingAnimation(delta);
        boolean stillSettling = updateSettlingAnimation(delta);

        LogUtil.debug("GameWorld", "handlePuyoEffectAnimation: stillFalling=" + stillFalling + ", stillSettling="
                + stillSettling + ", animatingPuyos=" + animatingPuyos.size());

        // 낙하 완료 + SETTLING 완료 (PENDING만 남음) → 보드에 확정 배치
        if (!stillFalling && !stillSettling) {
            // 모든 낙하 완료 + 바운스 완료: PLACED로 확정 배치
            for (Puyo p : animatingPuyos) {
                if (p.isSettling() || p.isPending()) {
                    p.setState(Puyo.State.PLACED);
                }
                board.placePuyo(p);
            }
            animatingPuyos.clear();
            currentPair = null;
            lockDelayManager.deactivate();

            // 연쇄 찾기 단계로
            gamePhase = GamePhase.CHAIN_FINDING;
            LogUtil.debug("GameWorld", "Phase: PUYO_EFFECT_ANIMATION -> CHAIN_FINDING (locked)");
        }
    }    

    private void handleChainFinding() {
        boolean found = chainManager.findChains(board);

        if (!found) {
            // 연쇄 종료
            LogUtil.debug("GameWorld", "Chain ended. chainCount=" + chainManager.getChainCount());
            gamePhase = GamePhase.SPAWNING;
            return;
        }

        // 새 연쇄 단계 시작
        int removed = chainManager.getCurrentGroups().stream().mapToInt(List::size).sum();
        LogUtil.debug("GameWorld",
                "New chain step: chainCount=" + chainManager.getChainCount() + ", groups="
                        + chainManager.getCurrentGroups().size()
                        + ", removed=" + removed);

        // 팝 애니메이션 시작
        for (List<Puyo> group : chainManager.getCurrentGroups()) {
            for (Puyo puyo : group) {
                if (!puyo.isPopping()) {
                    puyo.setState(Puyo.State.POPPING);
                }
                animatingPuyos.add(puyo);
            }
        }
        gamePhase = GamePhase.CHAIN_POP_ANIMATION;
        LogUtil.debug("GameWorld",
                "Phase: CHAIN_FINDING -> CHAIN_POP_ANIMATION, animatingPuyos=" + animatingPuyos.size());
    }

    private void handlePopAnimation(float delta) {
        boolean allPopDone = updatePopAnimation(delta);

        if (allPopDone) {
            // 팝 완료: POPPING 엔트리 수집 및 제거, 보드에서 제거
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
                p.setState(Puyo.State.FALLING);
                animatingPuyos.add(p);
            }
            fallingAnimationTimer = 0f;
            gamePhase = GamePhase.PUYO_EFFECT_ANIMATION;
            LogUtil.debug("GameWorld", "Phase: CHAIN_FLOATING_CHECK -> PUYO_EFFECT_ANIMATION (FALLING)");
        } else {
            // 부유 없으면 다음 연쇄 단계
            gamePhase = GamePhase.CHAIN_FINDING;
        }
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
        for (Puyo puyo : animatingPuyos) {
            if (puyo.isPopping()) {
                boolean popDone = puyo.updateAnimation(delta);
                if (!popDone) {
                    allPopDone = false;
                }
            }
        }
        return allPopDone;
    }

    /**
     * 팝 완료된 POPPING 엔트리 수집 및 리스트에서 제거
     * chainManager.getCurrentGroups() 기준으로 수집하여 상태 변경 후에도 정상 동작
     * 
     * @return 보드에서 제거할 뿌요들
     */
    private List<Puyo> collectAndClearChainPop() {
        List<Puyo> poppedPuyos = new ArrayList<>();
        List<Puyo> toRemove = new ArrayList<>();

        // chainManager가 가진 현재 팝 그룹 기준으로 수집 (상태 무관)
        for (List<Puyo> group : chainManager.getCurrentGroups()) {
            for (Puyo puyo : group) {
                if (animatingPuyos.contains(puyo)) {
                    poppedPuyos.add(puyo);
                    toRemove.add(puyo);
                }
            }
        }
        if (!poppedPuyos.isEmpty()) {
            LogUtil.debug("GameWorld", "collectAndClearChainPop: " + poppedPuyos.size() + " puyos, listSize before="
                    + animatingPuyos.size());
            animatingPuyos.removeAll(toRemove);
            LogUtil.debug("GameWorld",
                    "collectAndClearChainPop: removed POPPING, listSize after=" + animatingPuyos.size());
        }
        return poppedPuyos;
    }

    /**
     * 통합된 낙하 애니메이션 업데이트 (FALLING 상태 전체 대상)
     * 각 뿌요가 독립적으로 낙하하며, 아래쪽 뿌요부터 순차 처리하여
     * 이미 착지한 뿌요 위에 쌓이게 함 (뿌요뿌요 규칙).
     * 착지한 뿌요는 SETTLING 상태로 전이하여 바운스 애니메이션 시작.
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

        // FALLING , SETTLING 상태만 필터링 (POPPING 은 별도 처리)
        List<Puyo> fallingList = new ArrayList<>();
        for (Puyo puyo : animatingPuyos) {
            if (puyo.isFalling() || puyo.isSettling()) {
                fallingList.add(puyo);
                LogUtil.debug("GameWorld", "updateFallingAnimation[fallingList added] " + puyo);
            }
        }

        if (fallingList.isEmpty()) {
            return false;
        }

        // Y좌표 오름차순 정렬 (바닥쪽부터 처리: y=0이 바닥)
        fallingList.sort((a, b) -> Integer.compare(a.getY(), b.getY()));

        // 각 뿌요별로 독립적으로 이동 가능 여부 체크 및 이동
        boolean anyMoved = false;
        for (Puyo puyo : fallingList) {
            if (canSinglePuyoFallDuringFallingAnimation(puyo, fallingList)) {
                puyo.moveDown();
                LogUtil.debug("GameWorld", "updateFallingAnimation[Move down] " + puyo);
                anyMoved = true;
            } else {
                // 더 이상 이동 불가 = 착지함 → SETTLING 전이 + 바운스 시작
                if (puyo.getState() == Puyo.State.FALLING) {
                    puyo.setState(Puyo.State.SETTLING);
                    LogUtil.debug("GameWorld", "updateFallingAnimation[to SETTLE] " + puyo);
                }
            }
        }

        // 이동했으면 아직 낙하 중
        if (anyMoved) {
            return true;
        }

        // 아무도 이동 못 했으면 완료 체크 (여전히 이동 가능한 게 있는지)
        for (Puyo puyo : fallingList) {
            if (canSinglePuyoFallDuringFallingAnimation(puyo, fallingList)) {
                return true; // 아직 이동 가능한 뿌요가 있음
            }
        }

        return false; // 모두 착지 완료 (SETTLING으로 전이됨)
    }

    /**
     * 착지 바운스 애니메이션 업데이트 (SETTLING 상태 전체 대상)
     * 모든 SETTLING 뿌요가 독립적으로 바운스 애니메이션 재생.
     * 
     * @param delta 프레임 시간
     * @return 아직 바운스 중이면 true, 모두 완료면 false
     */
    private boolean updateSettlingAnimation(float delta) {
        // SETTLING 상태만 필터링
        List<Puyo> settlingList = new ArrayList<>();
        for (Puyo puyo : animatingPuyos) {
            if (puyo.isSettling()) {
                settlingList.add(puyo);
            }
        }

        if (settlingList.isEmpty()) {
            return false;
        }

        boolean anySettling = false;
        for (Puyo puyo : settlingList) {
            boolean settleDone = puyo.updateAnimation(delta);
            if (!settleDone) {
                anySettling = true;
            }
        }

        return anySettling; // 하나라도 진행 중이면 true
    }

    /**
     * 낙하/바운스 애니메이션 완료된 뿌요들 수집 및 보드에 배치
     */
    private void collectAndPlaceCompletedAnimation() {
        if (animatingPuyos.isEmpty())
            return;
        for (Puyo puyo : animatingPuyos) {
            board.placePuyo(puyo);
        }
        LogUtil.debug("GameWorld", "Completed animating puyos placed: " + animatingPuyos.size());
        animatingPuyos.clear();
    }

    /**
     * 낙하 애니메이션 중 단일 뿌요가 한 칸 아래로 이동 가능한지 확인.
     * 보드 충돌(이미 착지한 것) + 다른 falling puyo 충돌 체크.
     */
    private boolean canSinglePuyoFallDuringFallingAnimation(Puyo puyo, List<Puyo> fallingList) {
        // 바닥 체크
        if (puyo.getY() == 0 || puyo.isSettling())
            return false;

        // 보드 충돌 체크 (이미 착지한 뿌요들)
        if (!board.canMoveDown(puyo))
            return false;

        // 다른 falling puyo 충돌: targetY = y-1 위치에 다른 falling puyo가 있는지
        for (Puyo other : fallingList) {
            if (other == puyo)
                continue;
            
            // 같이  떨어지고 있는 뿌요는 충돌 체크에서 제외
            if (other.getX() == puyo.getX() && other.getY() == (puyo.getY() - 1) && other.isFalling() == false) { 
                return false;
            }
        }
        return true;
    }


    /** 뿌요 위치 확정 - 착지 바운스 애니메이션 위해 animatingPuyos에 SETTLING으로 추가 */
    private void startSettling() {
        if (currentPair == null)
            return;

        LogUtil.debug("GameWorld", "startSettling called, currentPair=" + currentPair);
        for (Puyo p : currentPair.getPuyos()) {
            if (p.isAlive()) {
                p.setState(Puyo.State.SETTLING);
                animatingPuyos.add(p);
                LogUtil.debug("GameWorld", "startSettling: added SETTLING puyo at (" + p.getX() + "," + p.getY() + ")");
            }
        }

        lockDelayManager.deactivate();
        currentPair = null;
        fallingAnimationTimer = 0f;
        gamePhase = GamePhase.PUYO_EFFECT_ANIMATION;
        LogUtil.debug("GameWorld",
                "startSettling -> PUYO_EFFECT_ANIMATION (settle bounce), puyos=" + animatingPuyos.size());
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

    /** 호환용: 모든 애니메이션 중인 뿌요 리스트 반환 (렌더링용) */
    public List<Puyo> getAnimatingPuyos() {
        return new ArrayList<>(animatingPuyos);
    }

    public int getScore() {
        return score;
    }

    public void addScore(int points) {
        this.score += points;
    }

    public int getCurrentChain() {
        return chainManager.getChainCount();
    }

    public void dispose() {
    }

    /** 게임 재시작 (게임오버 시 호출) */
    public void restartGame() {
        board.clear();
        currentPair = null;
        nextPair = null;
        heldPair = null;
        holdUsed = false;
        gameOver = false;
        score = 0;
        fallTimer = 0f;
        frameCount = 0;
        justSpawned = false;
        lockDelayManager.deactivate();
        chainManager.startNewChain();
        animatingPuyos.clear();
        fallingAnimationTimer = 0f;
        gamePhase = GamePhase.SPAWNING;
        spawnNextPair();
        spawnNewPair();
        LogUtil.info("GameWorld", "Game restarted");
    }
}