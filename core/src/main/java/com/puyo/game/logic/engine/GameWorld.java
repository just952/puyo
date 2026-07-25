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
        // Position the pair so that it appears at the top center.
        // The reference point (left puyo for rotation 0) should be at x = (WIDTH/2) - 1, y = HEIGHT - 1 (or higher to be invisible)
        // We'll set it to be just above the visible area so it falls in.
        int startX = (FIELD_WIDTH / 2) - 1; // left puyo x
        int startY = FIELD_HEIGHT; // start above the visible top (so it falls in)
        currentPair.setPosition(startX, startY);
    }

    /** Spawns the next pair (for preview). */
    public void spawnNextPair() {
        PuyoColor c1 = randomColor();
        PuyoColor c2 = randomColor();
        nextPair = new PuyoPair(new Puyo(c1, 0, 0), new Puyo(c2, 0, 0));
        // Position for preview: we don't need to set it here; the UI can decide where to show it.
    }

    private PuyoColor randomColor() {
        // Exclude ojama and hard for normal drops; they come from garbage.
        int r = random.nextInt(5); // 0-4 for the 5 normal colors
        switch (r) {
            case 0: return PuyoColor.RED;
            case 1: return PuyoColor.GREEN;
            case 2: return PuyoColor.BLUE;
            case 3: return PuyoColor.YELLOW;
            case 4: return PuyoColor.PURPLE;
            default: return PuyoColor.RED; // fallback
        }
    }

    /**
     * Returns true if the current pair can move down by one cell.
     */
    public boolean canFall() {
        return board.canMoveDown(currentPair);
    }

    /**
     * Moves the current pair left if possible.
     * @return true if moved
     */
    public boolean moveLeft() {
        if (board.canMoveLeft(currentPair)) {
            currentPair.moveLeft();
            resetLockDelay();
            return true;
        }
        return false;
    }

    /**
     * Moves the current pair right if possible.
     * @return true if moved
     */
    public boolean moveRight() {
        if (board.canMoveRight(currentPair)) {
            currentPair.moveRight();
            resetLockDelay();
            return true;
        }
        return false;
    }

    /**
     * Rotates the current pair clockwise if possible.
     * @return true if rotated
     */
    public boolean rotateClockwise() {
        // Try to rotate
        int originalRotation = currentPair.getRotation();
        currentPair.rotateClockwise();
        // Check if the new position is valid
        if (board.canPlace(currentPair)) {
            resetLockDelay();
            return true;
        } else {
            // Try wall kicks: simple attempt to move left/right if rotation causes collision
            // For simplicity, we just revert the rotation.
            currentPair.setRotation(originalRotation);
            return false;
        }
    }

    /**
     * Rotates the current pair counter-clockwise if possible.
     * @return true if rotated
     */

    public boolean rotateCounterClockwise() {
        int originalRotation = currentPair.getRotation();
        currentPair.rotateCounterClockwise();
        if (board.canPlace(currentPair)) {
            resetLockDelay();
            return true;
        } else {
            currentPair.setRotation(originalRotation);
            return false;
        }
    }
    /**
     * Drops the current pair to the bottom as far as it can go.
     */
    public void hardDrop() {
        int dropDistance = 0;
        while (canFall()) {
            currentPair.moveDown();
            dropDistance++;
        }
        lockPiece();
    }

    /**
     * Called when the player presses down for soft drop.
     * We'll just move down one step if possible.
     */
    public void softDrop() {
        if (canFall()) {
            currentPair.moveDown();
        }
    }

    /**
     * Updates the game state by the given delta time.
     * @param delta time in seconds since last update
     */
    public void update(float delta) {
        if (gameOver) {
            return;
        }

        // Handle falling
        fallTimer += delta;
        if (fallTimer >= fallInterval) {
            fallTimer = 0f;
            if (canFall()) {
                currentPair.moveDown();
                resetLockDelay();
            } else {
                // Cannot fall further
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

        // Optional: handle lock delay timing
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

    /**
     * Locks the current pair into the board, checks for clears, and spawns a new pair.
     */
    private void lockPiece() {
        // Place the two puyos into the board
        for (Puyo p : currentPair.getPuyos()) {
            if (p.isAlive()) {
                board.placePuyo(p);
            }
        }

        // Check for matches and resolve chains
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
            int earned = totalRemoved * (chainCount + 1) * 10; // simple scoring
            addScore(earned);
        }

        // Check for game over (if any puyo is placed above the visible top)
        if (board.isTopOut()) {
            gameOver = true;
        }

        // Spawn the next pair as current, and generate a new next pair
        currentPair = nextPair;
        spawnNextPair();

        // Reset fall timer and lock delay for the new pair
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

    public void dispose() {
        // Nothing to dispose
    }
}
