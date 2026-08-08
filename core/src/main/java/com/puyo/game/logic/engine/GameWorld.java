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
    private int currentChain = 0;

    public GameWorld() {
        board = new Board();
        random = new Random();
        spawnNewPair();
        spawnNextPair();
    }

    /** Spawns a new pair at the top center and assigns it as the current pair. */
    public void spawnNewPair() {
        PuyoColor c1 = randomColor();
        PuyoColor c2 = randomColor();
        currentPair = new PuyoPair(new Puyo(c1, 0, 0), new Puyo(c2, 0, 0));
        int startX = (FIELD_WIDTH / 2) - 1;
        int startY = FIELD_HEIGHT - 1; // 보드 내부(0~11)에서 시작
        currentPair.setPosition(startX, startY);
    }

    /** Spawns the next pair (for preview). */
    public void spawnNextPair() {
        PuyoColor c1 = randomColor();
        PuyoColor c2 = randomColor();
        nextPair = new PuyoPair(new Puyo(c1, 0, 0), new Puyo(c2, 0, 0));
        int startX = (FIELD_WIDTH / 2) - 1;
        int startY = FIELD_HEIGHT - 1;
        nextPair.setPosition(startX, startY);
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
        return board.canMoveDown(currentPair);
    }

    public boolean moveLeft() {
        if (board.canMoveLeft(currentPair)) {
            currentPair.moveLeft();
            resetLockDelay();
            return true;
        }
        return false;
    }

    public boolean moveRight() {
        if (board.canMoveRight(currentPair)) {
            currentPair.moveRight();
            resetLockDelay();
            return true;
        }
        return false;
    }

    public void rotateClockwise() {
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
        while (canFall()) {
            currentPair.moveDown();
        }
        lockPiece();
    }

    public void update(float delta) {
        if (gameOver) {
            return;
        }

        fallTimer += delta;
        if (fallTimer >= fallInterval) {
            fallTimer = 0f;
            if (canFall()) {
                currentPair.moveDown();
                resetLockDelay();
            } else {
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

        if (lockDelayActive) {
            lockDelayTimer += delta;
            if (lockDelayTimer >= LOCK_DELAY_TIME) {
                lockPiece();
            }
        }
    }

    private void resetLockDelay() {
        lockDelayActive = false;
        lockDelayTimer = 0f;
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
