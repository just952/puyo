package com.puyo.game.logic.engine;

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

    // 분리된 단일 뿌요 자동 낙하 처리용
    private Puyo fallingSinglePuyo = null;
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

        // 1. 분리된 단일 뿌요 자동 낙하 처리 (최우선 - 조작 불가)
        if (fallingSinglePuyo != null) {
            updateFallingSinglePuyo(delta);
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
                            lockPiece();
                        }
                    }
                }
            }
        }

        if (lockDelayActive) {
            lockDelayTimer += delta;
            if (lockDelayTimer >= LOCK_DELAY_TIME) {
                lockPiece();
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
        fallingSinglePuyo = freePuyo;
        currentPair = null; // 쌍 해제
        lockDelayActive = false;
        lockDelayTimer = 0f;
        lockDelayMoveCount = 0;
    }

    /**
     * 분리된 단일 뿌요 자동 낙하 업데이트 (조작 불가)
     * 단일 뿌요는 소프트 드롭 속도(SINGLE_FALL_INTERVAL)로 낙하
     */
    private void updateFallingSinglePuyo(float delta) {
        singleFallTimer += delta;
        if (singleFallTimer >= SINGLE_FALL_INTERVAL) {
            singleFallTimer = 0f;
            if (board.canMoveDown(fallingSinglePuyo)) {
                fallingSinglePuyo.moveDown();
            } else {
                // 바닥에 닿음 - 잠금 및 매칭 체크
                board.placePuyo(fallingSinglePuyo);
                fallingSinglePuyo = null;
                singleFallTimer = 0f; // 타이머 리셋
                checkMatchesAndSpawnNext();
            }
        }
    }

    /**
     * 단일 뿌요 착지 후 매칭/연쇄 체크 및 다음 쌍 스폰
     */
    private void checkMatchesAndSpawnNext() {
        boolean chainOccurred = false;
        int chainCount = 0;
        int totalRemoved = 0;
        do {
            List<List<Puyo>> groups = board.findAllMatchingGroups();
            if (groups.isEmpty()) {
                break;
            }
            chainOccurred = true;
            chainCount++;
            int removedThis = 0;
            for (List<Puyo> group : groups) {
                removedThis += group.size();
                board.removePuyos(group);
            }
            totalRemoved += removedThis;
            board.applyGravity();
        } while (chainOccurred);

        if (chainOccurred) {
            currentChain = chainCount;
            int earned = totalRemoved * (chainCount + 1) * 10;
            addScore(earned);
        } else {
            currentChain = 0;
        }

        if (board.isTopOut()) {
            gameOver = true;
            return;
        }

        // 다음 쌍 스폰
        currentPair = nextPair;
        spawnNextPair();

        fallTimer = 0f;
        resetLockDelay();
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

        boolean chainOccurred = false;
        int chainCount = 0;
        int totalRemoved = 0;
        do {
            List<List<Puyo>> groups = board.findAllMatchingGroups();
            if (groups.isEmpty()) {
                break;
            }
            chainOccurred = true;
            chainCount++;
            int removedThis = 0;
            for (List<Puyo> group : groups) {
                removedThis += group.size();
                board.removePuyos(group);
            }
            totalRemoved += removedThis;
            board.applyGravity();
        } while (chainOccurred);

        if (chainOccurred) {
            currentChain = chainCount;
            int earned = totalRemoved * (chainCount + 1) * 10;
            addScore(earned);
        } else {
            currentChain = 0;
        }

        if (board.isTopOut()) {
            gameOver = true;
        }

        currentPair = nextPair;
        spawnNextPair();

        fallTimer = 0f;
        resetLockDelay();
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
        return fallingSinglePuyo;
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
