package com.puyo.game.logic.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a pair of puyos that fall together.
 */
public class PuyoPair {
    private Puyo left;
    private Puyo right;
    private int rotation; // 0: up, 1: right, 2: down, 3: left
    private static final int[][] OFFSETS = {
            { 0, 1 }, // 0: up
            { 1, 0 }, // 1: right
            { 0, -1 }, // 2: down
            { -1, 0 } // 3: left
    };

    public PuyoPair(Puyo left, Puyo right) {
        this.left = left;
        this.right = right;
        this.rotation = 0; // initial rotation: up (right is above left)
        // Ensure the puyos are positioned correctly: left at (x,y), right at (x, y+1)
        // for rotation 0.
        // We'll adjust in setPosition.
    }

    public Puyo getLeft() {
        return left;
    }

    public Puyo getRight() {
        return right;
    }

    public int getRotation() {
        return rotation;
    }

    public void setRotation(int rotation) {
        this.rotation = rotation;
    }

    /**
     * Sets the position of the pair based on the grid coordinates of the
     * "reference" point.
     * We'll define the reference point as the position of the left puyo when
     * rotation is 0.
     * For other rotations, we compute the positions accordingly.
     * 
     * @param x the x coordinate of the reference point
     * @param y the y coordinate of the reference point
     */
    public void setPosition(int x, int y) {
        int[] offset = OFFSETS[rotation];
        left.setX(x);
        left.setY(y);
        right.setX(x + offset[0]);
        right.setY(y + offset[1]);
    }

    /**
     * Returns the positions of the two puyos as an array of [x,y] pairs.
     * 
     * @return array where [0] is left puyo [x,y], [1] is right puyo [x,y]
     */
    public int[][] getPositions() {
        return new int[][] {
                { left.getX(), left.getY() },
                { right.getX(), right.getY() }
        };
    }

    /**
     * Returns the x coordinate of the left puyo given the reference point (the
     * position of the left puyo when rotation=0).
     * 
     * @param refX the x coordinate of the reference point
     * @param refY the y coordinate of the reference point (ignored for X)
     * @return the x coordinate of the left puyo
     */
    public int getX1(int refX, int refY) {
        return refX;
    }

    /**
     * Returns the y coordinate of the left puyo given the reference point.
     * 
     * @param refX the x coordinate of the reference point (ignored for Y)
     * @param refY the y coordinate of the reference point
     * @return the y coordinate of the left puyo
     */
    public int getY1(int refX, int refY) {
        return refY;
    }

    /**
     * Returns the x coordinate of the right puyo given the reference point.
     * 
     * @param refX the x coordinate of the reference point
     * @param refY the y coordinate of the reference point
     * @return the x coordinate of the right puyo
     */
    public int getX2(int refX, int refY) {
        return refX + OFFSETS[rotation][0];
    }

    /**
     * Returns the y coordinate of the right puyo given the reference point.
     * 
     * @param refX the x coordinate of the reference point (ignored for Y)
     * @param refY the y coordinate of the reference point
     * @return the y coordinate of the right puyo
     */
    public int getY2(int refX, int refY) {
        return refY + OFFSETS[rotation][1];
    }

    public List<Puyo> getPuyos() {
        List<Puyo> list = new ArrayList<>();
        list.add(left);
        list.add(right);
        return list;
    }

    public void moveLeft() {
        left.setX(left.getX() - 1);
        right.setX(right.getX() - 1);
    }

    public void moveRight() {
        left.setX(left.getX() + 1);
        right.setX(right.getX() + 1);
    }

    public void moveDown() {
        //left.setY(left.getY() - 1);
        //right.setY(right.getY() - 1);
        left.moveDown();
        right.moveDown();
    }

    /**
     * Rotates the pair clockwise.
     * Note: This does not change the position of the reference point (the left
     * puyo's position for rotation 0).
     * After rotation, the actual positions of the puyos change according to the new
     * offset.
     * The caller should ensure that the new position does not collide with the
     * board.
     */
    public void rotateClockwise() {
        rotation = (rotation + 1) % 4;
        setPosition(left.getX(), left.getY());
    }

    /**
     * Rotates the pair counter-clockwise.
     */
    public void rotateCounterClockwise() {
        rotation = (rotation + 3) % 4;
        setPosition(left.getX(), left.getY());
    }

    public String toString() {
        return "PuyoPair [left" + left + " /  right" + right + " / rotation=" + rotation + "]";
    }

}
