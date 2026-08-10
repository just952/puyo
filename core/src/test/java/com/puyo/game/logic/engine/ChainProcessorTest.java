package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoColor;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class ChainProcessorTest {

    private Board board;
    private ChainProcessor chainProcessor;

    @Before
    public void setUp() {
        board = new Board();
        chainProcessor = new ChainProcessor();
    }

    @Test
    public void processChain_noMatches_returnsZero() {
        // 3개만 연결 (매칭 안 됨)
        board.placePuyo(new Puyo(PuyoColor.RED, 0, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 1, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 2, 0));

        ChainProcessor.ChainResult result = chainProcessor.processChain(board);

        assertEquals("연쇄 없음", 0, result.chainCount);
        assertEquals("제거된 뿌요 없음", 0, result.totalRemoved);
        assertTrue("보드 변화 없음", result.chainCount == 0);
    }

    @Test
    public void processChain_singleChain_returnsOne() {
        // 가로 4개 연결
        board.placePuyo(new Puyo(PuyoColor.RED, 0, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 1, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 2, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 3, 0));

        ChainProcessor.ChainResult result = chainProcessor.processChain(board);

        assertEquals("1연쇄", 1, result.chainCount);
        assertEquals("4개 제거", 4, result.totalRemoved);
    }

    @Test
    public void processChain_twoSeparateGroupsSameChain() {
        // 첫 번째 그룹: RED 가로 4개 (y=0)
        board.placePuyo(new Puyo(PuyoColor.RED, 0, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 1, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 2, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 3, 0));

        // 두 번째 그룹: BLUE 가로 4개 (y=2)
        board.placePuyo(new Puyo(PuyoColor.BLUE, 0, 2));
        board.placePuyo(new Puyo(PuyoColor.BLUE, 1, 2));
        board.placePuyo(new Puyo(PuyoColor.BLUE, 2, 2));
        board.placePuyo(new Puyo(PuyoColor.BLUE, 3, 2));

        ChainProcessor.ChainResult result = chainProcessor.processChain(board);

        assertEquals("1연쇄 (동시 제거)", 1, result.chainCount);
        assertEquals("8개 제거", 8, result.totalRemoved);
    }

    @Test
    public void processChain_chainReaction_twoChains() {
        // 바닥: RED 4개 가로 (y=0) - 1연쇄에서 제거
        board.placePuyo(new Puyo(PuyoColor.RED, 0, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 1, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 2, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 3, 0));

        // BLUE: 같은 열(x=4)에 간격 두고 배치 - 초기엔 매칭 안 됨 (간격 있음)
        // y=3, y=5, y=7, y=9에 배치 - 중력 후 y=0,1,2,3으로 연결되어 4개 매칭
        board.placePuyo(new Puyo(PuyoColor.BLUE, 4, 3));
        board.placePuyo(new Puyo(PuyoColor.BLUE, 4, 5));
        board.placePuyo(new Puyo(PuyoColor.BLUE, 4, 7));
        board.placePuyo(new Puyo(PuyoColor.BLUE, 4, 9));

        // 초기 상태: RED 4개만 매칭 (BLUE는 떨어져 있어서 매칭 안 됨)
        // 1연쇄: RED 제거 → 중력 → BLUE가 y=0,1,2,3으로 낙하 → 4개 연결되어 2연쇄

        ChainProcessor.ChainResult result = chainProcessor.processChain(board);

        assertEquals("2연쇄", 2, result.chainCount);
        assertEquals("8개 제거", 8, result.totalRemoved);
    }

    @Test
    public void processChain_ojamaNotMatched() {
        // OJAMA 4개 연결해도 매칭 안 됨
        board.placePuyo(new Puyo(PuyoColor.OJAMA, 0, 0));
        board.placePuyo(new Puyo(PuyoColor.OJAMA, 1, 0));
        board.placePuyo(new Puyo(PuyoColor.OJAMA, 2, 0));
        board.placePuyo(new Puyo(PuyoColor.OJAMA, 3, 0));

        ChainProcessor.ChainResult result = chainProcessor.processChain(board);

        assertEquals("OJAMA 연쇄 없음", 0, result.chainCount);
        assertEquals("제거된 것 없음", 0, result.totalRemoved);
    }

    @Test
    public void processChain_gravityAfterClear() {
        // y=2에 RED 4개, y=0에 아무것도 없음
        board.placePuyo(new Puyo(PuyoColor.RED, 0, 2));
        board.placePuyo(new Puyo(PuyoColor.RED, 1, 2));
        board.placePuyo(new Puyo(PuyoColor.RED, 2, 2));
        board.placePuyo(new Puyo(PuyoColor.RED, 3, 2));

        ChainProcessor.ChainResult result = chainProcessor.processChain(board);

        assertEquals("1연쇄", 1, result.chainCount);
        // 제거 후 중력이 적용되어야 함
        for (int x = 0; x < 4; x++) {
            assertNull("제거된 위치 비어있음", board.getPuyoAt(x, 2));
        }
    }

    @Test
    public void processChain_verticalChain() {
        // 세로 4개 연결
        board.placePuyo(new Puyo(PuyoColor.GREEN, 0, 0));
        board.placePuyo(new Puyo(PuyoColor.GREEN, 0, 1));
        board.placePuyo(new Puyo(PuyoColor.GREEN, 0, 2));
        board.placePuyo(new Puyo(PuyoColor.GREEN, 0, 3));

        ChainProcessor.ChainResult result = chainProcessor.processChain(board);

        assertEquals("1연쇄", 1, result.chainCount);
        assertEquals("4개 제거", 4, result.totalRemoved);
    }
}