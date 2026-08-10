package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoColor;
import com.puyo.game.logic.model.PuyoPair;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class SeparationManagerTest {

    private Board board;
    private SeparationManager separationManager;

    @Before
    public void setUp() {
        board = new Board();
        separationManager = new SeparationManager();
    }

    @Test
    public void canSeparate_horizontalLeftBlocked_returnsTrue() {
        // rotation 3: left(왼쪽) - 가로 상태
        Puyo left = new Puyo(PuyoColor.RED, 2, 5); // 왼쪽 뿌요 (막힘)
        Puyo right = new Puyo(PuyoColor.BLUE, 3, 5); // 오른쪽 뿌요 (자유)
        PuyoPair pair = new PuyoPair(left, right);
        pair.setRotation(3);

        // 왼쪽만 막히도록 왼쪽 아래에 뿌요 배치
        board.placePuyo(new Puyo(PuyoColor.GREEN, 2, 4));

        assertTrue("왼쪽 막힘, 오른쪽 자유 → 분리 가능", separationManager.canSeparate(pair, board));
    }

    @Test
    public void canSeparate_horizontalRightBlocked_returnsTrue() {
        // rotation 1: right(오른쪽) - 가로 상태
        Puyo left = new Puyo(PuyoColor.RED, 2, 5); // 왼쪽 뿌요 (자유)
        Puyo right = new Puyo(PuyoColor.BLUE, 3, 5); // 오른쪽 뿌요 (막힘)
        PuyoPair pair = new PuyoPair(left, right);
        pair.setRotation(1);

        // 오른쪽만 막히도록 오른쪽 아래에 뿌요 배치
        board.placePuyo(new Puyo(PuyoColor.GREEN, 3, 4));

        assertTrue("오른쪽 막힘, 왼쪽 자유 → 분리 가능", separationManager.canSeparate(pair, board));
    }

    @Test
    public void canSeparate_bothCanFall_returnsFalse() {
        Puyo left = new Puyo(PuyoColor.RED, 2, 5);
        Puyo right = new Puyo(PuyoColor.BLUE, 3, 5);
        PuyoPair pair = new PuyoPair(left, right);
        pair.setRotation(1);

        // 둘 다 아래로 이동 가능 (막힌 것 없음)
        assertFalse("둘 다 자유 → 분리 안 함", separationManager.canSeparate(pair, board));
    }

    @Test
    public void canSeparate_bothBlocked_returnsFalse() {
        Puyo left = new Puyo(PuyoColor.RED, 2, 5);
        Puyo right = new Puyo(PuyoColor.BLUE, 3, 5);
        PuyoPair pair = new PuyoPair(left, right);
        pair.setRotation(1);

        // 둘 다 막히도록 아래에 뿌요 배치
        board.placePuyo(new Puyo(PuyoColor.GREEN, 2, 4));
        board.placePuyo(new Puyo(PuyoColor.GREEN, 3, 4));

        assertFalse("둘 다 막힘 → 분리 안 함", separationManager.canSeparate(pair, board));
    }

    @Test
    public void canSeparate_verticalRotation_returnsFalse() {
        Puyo left = new Puyo(PuyoColor.RED, 2, 5);
        Puyo right = new Puyo(PuyoColor.BLUE, 2, 6);
        PuyoPair pair = new PuyoPair(left, right);
        pair.setRotation(0); // 세로 상태

        board.placePuyo(new Puyo(PuyoColor.GREEN, 2, 4));

        assertFalse("세로 상태 → 분리 안 함", separationManager.canSeparate(pair, board));
    }

    @Test
    public void separate_leftBlocked_rightFalls() {
        // rotation 3: left(왼쪽)
        Puyo left = new Puyo(PuyoColor.RED, 2, 5); // 왼쪽 뿌요 (막힘)
        Puyo right = new Puyo(PuyoColor.BLUE, 3, 5); // 오른쪽 뿌요 (자유)
        PuyoPair pair = new PuyoPair(left, right);
        pair.setRotation(3);

        board.placePuyo(new Puyo(PuyoColor.GREEN, 2, 4)); // 왼쪽 막힘

        List<FallingPuyo> fallingPuyos = new ArrayList<>();
        boolean separated = separationManager.separate(pair, board, fallingPuyos);

        assertTrue("분리됨", separated);
        assertEquals("자유로운 오른쪽이 fallingPuyos에 추가", 1, fallingPuyos.size());
        assertTrue("분리 낙하 타입", fallingPuyos.get(0).isFromSeparation());
        assertEquals("자유로운 오른쪽 뿌요", right, fallingPuyos.get(0).puyo);
        assertNotNull("막힌 왼쪽은 보드에 배치됨", board.getPuyoAt(2, 5));
        assertNull("자유로운 오른쪽은 보드에 없음", board.getPuyoAt(3, 5));
    }

    @Test
    public void separate_rightBlocked_leftFalls() {
        // rotation 1: right(오른쪽)
        Puyo left = new Puyo(PuyoColor.RED, 2, 5); // 왼쪽 뿌요 (자유)
        Puyo right = new Puyo(PuyoColor.BLUE, 3, 5); // 오른쪽 뿌요 (막힘)
        PuyoPair pair = new PuyoPair(left, right);
        pair.setRotation(1);

        board.placePuyo(new Puyo(PuyoColor.GREEN, 3, 4)); // 오른쪽 막힘

        List<FallingPuyo> fallingPuyos = new ArrayList<>();
        boolean separated = separationManager.separate(pair, board, fallingPuyos);

        assertTrue("분리됨", separated);
        assertEquals("자유로운 왼쪽이 fallingPuyos에 추가", 1, fallingPuyos.size());
        assertTrue("분리 낙하 타입", fallingPuyos.get(0).isFromSeparation());
        assertEquals("자유로운 왼쪽 뿌요", left, fallingPuyos.get(0).puyo);
        assertNotNull("막힌 오른쪽은 보드에 배치됨", board.getPuyoAt(3, 5));
        assertNull("자유로운 왼쪽은 보드에 없음", board.getPuyoAt(2, 5));
    }

    @Test
    public void separate_notSeparable_returnsFalse() {
        Puyo left = new Puyo(PuyoColor.RED, 2, 5);
        Puyo right = new Puyo(PuyoColor.BLUE, 3, 5);
        PuyoPair pair = new PuyoPair(left, right);
        pair.setRotation(0); // 세로 상태

        List<FallingPuyo> fallingPuyos = new ArrayList<>();
        boolean separated = separationManager.separate(pair, board, fallingPuyos);

        assertFalse("세로 상태라 분리 안 됨", separated);
        assertTrue("fallingPuyos 비어있음", fallingPuyos.isEmpty());
    }
}