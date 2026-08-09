package com.puyo.game.logic.engine;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.PuyoPair;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoColor;

/**
 * Manages the game state: the board, the current falling pair, the next pair,
 * and handles the game loop logic (falling, locking, clearing, etc.).
 */
public class GameWorld {
    private static final int FIELD_WIDTH = Board.WIDTH;
    private static final int FIELD_HEIGHT = Board.HEIGHT;

    private final Board board;
    private PuyoPair currentPair;
    private PuyoPair nextPair;
    private final Random random;
    private boolean gameOver = false;
    private int score = 0;
    private float fallTimer = 0f;
    private float fallInterval = 0.5f; // seconds per cell fall (adjustable by level)
    private boolean lockDelayActive = false;
    private float lockDelayTimer = 0f;
    private static final float LOCK_DELAY_TIME = 0.5f; // seconds to lock after touching bottom
    private int lockDelayMoveCount = 0;
    private static final int MAX_LOCK_DELAY_MOVES = 15; // Tsu rules: max moves during lock delay
    private int currentChain = 0;
    private int totalRemoved = 0;

    // 통합된 낙하 시스템 (분리/연쇄 모두 처리)
    private static class FallingPuyo {
        Puyo puyo;
        boolean isFromSeparation; // true: 분리, false: 연쇄

        FallingPuyo(Puyo puyo, boolean isFromSeparation) {
            this.puyo = puyo;
            this.isFromSeparation = isFromSeparation;
        }
    }

    private List<FallingPuyo> fallingPuyos = new ArrayList<>();
    private float singleFallTimer = 0f;
    private static final float SINGLE_FALL_INTERVAL = 0.05f; // 단일 뿌요 낙하 속도 (소프트 드롭 속도)

    public GameWorld() {
        board = new Board();
        random = new Random();
        spawnNewPair();
        spawnNextPair();
    }

    /** Spawns a new pair at the top center and assigns it as the current pair. */
    public void spawnNewPair() {
        currentPair = createAndPositionPair();
        lockDelayMoveCount = 0; // 이동 카운터 리셋
        lockDelayActive = false; // 락 딜레이 상태 리셋
    }

    /** Spawns the next pair (for preview). */
    public void spawnNextPair() {
        nextPair = createAndPositionPair();
        lockDelayMoveCount = 0; // 이동 카운터 리셋
        lockDelayActive = false; // 락 딜레이 상태 리셋
    }

    /** Creates a new PuyoPair with random colors at the spawn position. */
    private PuyoPair createAndPositionPair() {
        PuyoColor c1 = randomColor();
        PuyoColor c2 = randomColor();
        PuyoPair pair = new PuyoPair(new Puyo(c1, 0, 0), new Puyo(c2, 0, 0));
        int startX = (FIELD_WIDTH / 2) - 1;
        int startY = FIELD_HEIGHT - 1;
        pair.setPosition(startX, startY);
        return pair;
    }

    private PuyoColor randomColor() {
        int r = random.nextInt(5);
        switch (r) {
            case 0:
                return PuyoColor.RED;
            case 1:
                return PuyoColor.GREEN;
            case 2:
                return PuyoColor.BLUE;
            case 3:
                return PuyoColor.YELLOW;
            case 4:
                return PuyoColor.PURPLE;
            default:
                return PuyoColor.RED;
        }
    }

    public boolean canFall() {
        return currentPair != null && board.canMoveDown(currentPair);
    }

    public boolean moveLeft() {
        if (currentPair == null)
            return false;
        if (board.canMoveLeft(currentPair)) {
            currentPair.moveLeft();
            resetLockDelay();
            return true;
        }
        return false;
    }

    public boolean moveRight() {
        if (currentPair == null)
            return false;
        if (board.canMoveRight(currentPair)) {
            currentPair.moveRight();
            resetLockDelay();
            return true;
        }
        return false;
    }

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
        resetLockDelay();
    }

    public void hardDrop() {
        if (currentPair == null)
            return;
        while (canFall()) {
            currentPair.moveDown();
        }
        lockPiece();
    }

    public void update(float delta) {
        if (gameOver) {
            return;
        }

        // 1. 통합된 낙하 처리 (분리/연쇄 모두) - 최우선, 조작 불가
        if (!fallingPuyos.isEmpty()) {
            updateFalling(delta);
            return;
        }

        // 2. 일반 쌍 뿌요 낙하 처리
        fallTimer += delta;
        if (fallTimer >= fallInterval) {
            fallTimer = 0f;
            if (canFall()) {
                currentPair.moveDown();
                resetLockDelay();
            } else {
                // 바닥에 닿음 - 가로 상태에서 분리 가능한지 확인
                if (isHorizontalAndCanSeparate()) {
                    separatePair();
                } else {
                    // 기존 락 딜레이 로직
                    if (!lockDelayActive) {
                        lockDelayActive = true;
                        lockDelayTimer = 0f;
                    } else {
                        lockDelayTimer += delta;
                        if (lockDelayTimer >= LOCK_DELAY_TIME) {
                            if (currentPair != null) {
                                lockPiece();
                            }
                        }
                    }
                }
            }
        }

        if (lockDelayActive) {
            lockDelayTimer += delta;
            if (lockDelayTimer >= LOCK_DELAY_TIME) {
                if (currentPair != null) {
                    lockPiece();
                }
            }
        }
    }

    /**
     * 가로 상태(rotation 1 또는 3)에서 한 쪽만 막혀서 분리 가능한지 확인
     */
    private boolean isHorizontalAndCanSeparate() {
        if (currentPair == null)
            return false;
        int rotation = currentPair.getRotation();
        // rotation 1: right(오른쪽), rotation 3: left(왼쪽)
        if (rotation != 1 && rotation != 3)
            return false;

        Puyo left = currentPair.getLeft();
        Puyo right = currentPair.getRight();

        boolean leftCanFall = board.canMoveDown(left);
        boolean rightCanFall = board.canMoveDown(right);

        // 한 쪽만 이동 가능하면 분리 가능
        return leftCanFall != rightCanFall;
    }

    /**
     * 쌍 분리: 한 쪽은 잠금, 다른 쪽은 단일 뿌요로 자동 낙하 시작
     */
    private void separatePair() {
        Puyo left = currentPair.getLeft();
        Puyo right = currentPair.getRight();

        boolean leftCanFall = board.canMoveDown(left);
        boolean rightCanFall = board.canMoveDown(right);

        Puyo blockedPuyo;
        Puyo freePuyo;

        if (leftCanFall && !rightCanFall) {
            blockedPuyo = right;
            freePuyo = left;
        } else if (rightCanFall && !leftCanFall) {
            blockedPuyo = left;
            freePuyo = right;
        } else {
            // 둘 다 가능하거나 둘 다 불가능하면 분리 안 함 (락 딜레이로)
            return;
        }

        // 막힌 쪽 즉시 잠금
        board.placePuyo(blockedPuyo);

        // 자유로운 쪽 단일 뿌요로 자동 낙하 시작
        fallingPuyos.add(new FallingPuyo(freePuyo, true)); // isFromSeparation=true
        currentPair = null; // 쌍 해제
        lockDelayActive = false;
        lockDelayTimer = 0f;
        lockDelayMoveCount = 0;
    }

    /**
     * 통합된 낙하/팝 업데이트 (분리/연쇄 모두 처리)
     * - 분리/낙하: 소프트 드롭 속도(SINGLE_FALL_INTERVAL)로 낙하
     * - 연쇄/팝: 팝 애니메이션(POP_DURATION) 매 프레임 처리
     */
    private void updateFalling(float delta) {
        // 1. 팝 애니메이션은 매 프레임 업데이트 (부드러운 애니메이션을 위해)
        // 모든 연쇄 뿌요의 팝 애니메이션을 동시에 진행
        boolean allPopDone = true;
        for (FallingPuyo fp : fallingPuyos) {
            if (!fp.isFromSeparation) {
                // 연쇄: 팝 애니메이션 (매 프레임 업데이트)
                if (!fp.puyo.isPopping()) {
                    fp.puyo.startPop();
                }
                boolean popDone = fp.puyo.updatePop(delta);
                if (!popDone) {
                    allPopDone = false;
                }
            }
        }

        // 2. 분리 낙하 처리 (SINGLE_FALL_INTERVAL 간격으로)
        singleFallTimer += delta;
        boolean shouldFall = (singleFallTimer >= SINGLE_FALL_INTERVAL);
        if (shouldFall) {
            singleFallTimer = 0f;
            // 분리 낙하 처리
            for (FallingPuyo fp : fallingPuyos) {
                if (fp.isFromSeparation && board.canMoveDown(fp.puyo)) {
                    fp.puyo.moveDown();
                }
            }
        }

        // 팝 완료 체크
        for (FallingPuyo fp : fallingPuyos) {
            if (!fp.isFromSeparation && fp.puyo.isPopping()) {
                return; // 팝 진행 중이면 이번 프레임은 여기서 종료
            }
        }

        // 모든 분리 낙하 완료 체크
        boolean allSeparationDone = true;
        for (FallingPuyo fp : fallingPuyos) {
            if (fp.isFromSeparation && board.canMoveDown(fp.puyo)) {
                return; // 아직 떨어질 곳 있으면 이번 프레임 종료
            }
        }

        // 모든 처리 완료 (팝 완료 AND 분리 낙하 완료)
        // 연쇄 완료: 보드에서 연쇄 뿌요들 제거
        for (FallingPuyo fp : fallingPuyos) {
            if (!fp.isFromSeparation) {
                board.removePuyo(fp.puyo);
            }
        }
        // 분리인 경우만 보드에 배치
        for (FallingPuyo fp : fallingPuyos) {
            if (fp.isFromSeparation) {
                board.placePuyo(fp.puyo);
            }
        }
        fallingPuyos.clear();
        singleFallTimer = 0f;

        // 팝 애니메이션 완료 후 중력 적용으로 남은 뿌요들이 내려오게 함
        board.applyGravity();

        checkMatchesAndSpawnNext(); // 연쇄 체크 (내부에서 다시 fallingPuyos 추가 가능)
    }

    /**
     * 단일 뿌요 착지 후 매칭/연쇄 체크 및 다음 쌍 스폰
     * 연쇄 발생 시 떠있는 뿌요들을 fallingPuyos에 추가하여 애니메이션 처리
     * 한 번에 한 단계만 처리 (연쇄당 한 번 호출)
     */
    private void checkMatchesAndSpawnNext() {
        List<List<Puyo>> groups = board.findAllMatchingGroups();

        if (groups.isEmpty()) {
            // 매칭 없음 - 연쇄 종료
            if (currentChain > 0) {
                int earned = totalRemoved * (currentChain + 1) * 10;
                addScore(earned);
            }
            currentChain = 0;
            totalRemoved = 0;

            if (board.isTopOut()) {
                gameOver = true;
                return;
            }

            // 다음 쌍 스폰
            currentPair = nextPair;
            spawnNextPair();

            fallTimer = 0f;
            resetLockDelay();
            return;
        }

        // 매칭 발견 - 연쇄 발생
        currentChain++;
        int removedThis = 0;
        for (List<Puyo> group : groups) {
            removedThis += group.size();
            // 팝 애니메이션 시작 (즉시 제거하지 않고 애니메이션용으로 유지)
            for (Puyo p : group) {
                p.startPop();
            }
        }
        totalRemoved += removedThis;

        // fallingPuyos에 추가하여 팝 애니메이션 처리 (isFromSeparation=false로 연쇄 표시)
        // board.removePuyos()는 팝 애니메이션 완료 후 updateFalling에서 처리
        for (List<Puyo> group : groups) {
            for (Puyo p : group) {
                fallingPuyos.add(new FallingPuyo(p, false)); // isFromSeparation=false (연쇄)
            }
        }
        // fallingPuyos가 채워지면 updateFalling에서 팝 애니메이션 처리 후 다시 호출됨
    }

    private void resetLockDelay() {
        if (lockDelayActive) {
            lockDelayMoveCount++;
            if (lockDelayMoveCount >= MAX_LOCK_DELAY_MOVES) {
                // 이동 제한 초과 → 즉시 잠금 (Tsu 규칙)
                lockPiece();
                return;
            }
            lockDelayTimer = 0f; // 타이머만 리셋 (lockDelayActive 유지)
        } else {
            lockDelayTimer = 0f;
            lockDelayMoveCount = 0; // 공중에서 움직일 때는 카운터 리셋
        }
    }

    private void lockPiece() {
        for (Puyo p : currentPair.getPuyos()) {
            if (p.isAlive()) {
                board.placePuyo(p);
            }
        }

        // 새로운 연쇄/애니메이션 시스템 사용
        checkMatchesAndSpawnNext();
    }

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

    public Puyo getFallingSinglePuyo() {
        if (!fallingPuyos.isEmpty()) {
            return fallingPuyos.get(0).puyo;
        }
        return null;
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
