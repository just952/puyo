package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoColor;
import org.junit.Test;

import static org.junit.Assert.*;

public class GravityEngineTest {

    @Test
    public void applyGravity_singlePuyoFallsToBottom() {
        Board board = new Board();
        Puyo puyo = new Puyo(PuyoColor.RED, 2, 5);
        board.placePuyo(puyo);

        GravityEngine engine = new GravityEngine(board);
        boolean moved = engine.applyGravity();

        assertTrue("뿌요가 움직여야 함", moved);
        assertEquals("뿌요가 바닥(y=0)에 있어야 함", 0, puyo.getY());
        assertEquals("보드에 y=0 위치에 뿌요가 있어야 함", puyo, board.getPuyoAt(2, 0));
        assertNull("원래 위치(y=5)는 비어있어야 함", board.getPuyoAt(2, 5));
    }

    @Test
    public void applyGravity_multiplePuyosInSameColumn_stackAtBottom() {
        Board board = new Board();
        Puyo bottom = new Puyo(PuyoColor.RED, 1, 3);
        Puyo top = new Puyo(PuyoColor.BLUE, 1, 7);
        board.placePuyo(bottom);
        board.placePuyo(top);

        GravityEngine engine = new GravityEngine(board);
        boolean moved = engine.applyGravity();

        assertTrue("뿌요들이 움직여야 함", moved);
        assertEquals("아래 뿌요가 y=0", 0, bottom.getY());
        assertEquals("위 뿌요가 y=1 (아래 뿌요 바로 위)", 1, top.getY());
    }

    @Test
    public void applyGravity_noEmptySpace_noMovement() {
        Board board = new Board();
        Puyo puyo = new Puyo(PuyoColor.RED, 0, 0);
        board.placePuyo(puyo);

        GravityEngine engine = new GravityEngine(board);
        boolean moved = engine.applyGravity();

        assertFalse("이미 바닥에 있으면 움직이지 않아야 함", moved);
        assertEquals(0, puyo.getY());
    }

    @Test
    public void applyGravity_emptyColumn_noChange() {
        Board board = new Board();
        GravityEngine engine = new GravityEngine(board);
        boolean moved = engine.applyGravity();

        assertFalse("빈 열은 변화 없어야 함", moved);
    }

    @Test
    public void applyGravity_independentColumns() {
        Board board = new Board();
        // Column 0: puyo at y=5
        board.placePuyo(new Puyo(PuyoColor.RED, 0, 5));
        // Column 2: puyo at y=3
        board.placePuyo(new Puyo(PuyoColor.BLUE, 2, 3));

        GravityEngine engine = new GravityEngine(board);
        engine.applyGravity();

        assertEquals("Column 0: 바닥에 쌓임", 0, board.getPuyoAt(0, 0).getY());
        assertEquals("Column 2: 바닥에 쌓임", 0, board.getPuyoAt(2, 0).getY());
    }
}