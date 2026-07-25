package com.puyo.game.logic.ai;

import java.util.Random;
import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.PuyoPair;

/**
 * Simple AI controller for Puyo Puyo.
 * Given a board and a falling pair, it decides where to place the pair.
 * The difficulty affects how well the AI plays.
 */
public class AIController {
    private final Random random;
    private final double skillLevel; // 0.0 to 1.0, where 0 is random, 1 is perfect (for this simple AI)

    public AIController(double skillLevel) {
        this.random = new Random();
        this.skillLevel = Math.max(0.0, Math.min(1.0, skillLevel));
    }

    /**
     * Returns the intended movement for the AI.
     * @param board the current board
     * @param pair the current falling pair
     * @return an enum representing the action: NONE, LEFT, RIGHT, ROTATE, DROP
     */
    public Action getAction(Board board, PuyoPair pair) {
        // With probability (1 - skillLevel), do a random action
        if (random.nextDouble() > skillLevel) {
            return getRandomAction();
        }

        // Otherwise, try to compute a good move (simplified)
        // For now, we just try to drop it as is, but we could implement a search.
        // Since a full search is complex, we'll just try to move towards the center and drop.
        int targetX = Board.WIDTH / 2;
        int currentX = (pair.getLeft().getX() + pair.getRight().getX()) / 2;

        if (currentX < targetX && board.canMoveLeft(pair)) {
            return Action.LEFT;
        } else if (currentX > targetX && board.canMoveRight(pair)) {
            return Action.RIGHT;
        } else {
            // If we are roughly in the center, try to rotate if beneficial, else drop
            // For simplicity, we just drop if we can, else try to rotate once.
            if (board.canMoveDown(pair)) {
                return Action.DOWN;
            } else {
                // If we can't drop, we are likely locked; try to rotate to see if we can fit elsewhere
                // But we don't want to rotate infinitely. We'll just return NONE and let the game lock it.
                return Action.NONE;
            }
        }
    }

    private Action getRandomAction() {
        int r = random.nextInt(5);
        switch (r) {
            case 0: return Action.LEFT;
            case 1: return Action.RIGHT;
            case 2: return Action.DOWN;
            case 3: return Action.ROTATE;
            case 4: return Action.NONE;
            default: return Action.NONE;
        }
    }

    public enum Action {
        LEFT, RIGHT, DOWN, ROTATE, NONE
    }
}
