package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoColor;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class MatchFinderTest {

    private Board board;
    private MatchFinder matchFinder;

    @Before
    public void setUp() {
        board = new Board();
        matchFinder = new MatchFinder();
    }

    @Test
    public void findAllMatchingGroups_noMatches_returnsEmpty() {
        // 3개만 연결 (매칭 안 됨)
        board.placePuyo(new Puyo(PuyoColor.RED, 0, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 1, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 2, 0));

        List<List<Puyo>> groups = matchFinder.findAllMatchingGroups(board);

        assertTrue("3개는 매칭 안 됨", groups.isEmpty());
    }

    @Test
    public void findAllMatchingGroups_fourHorizontal_matchFound() {
        // 가로 4개 연결
        board.placePuyo(new Puyo(PuyoColor.RED, 0, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 1, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 2, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 3, 0));

        List<List<Puyo>> groups = matchFinder.findAllMatchingGroups(board);

        assertEquals("1개 그룹 발견", 1, groups.size());
        assertEquals("그룹 크기 4", 4, groups.get(0).size());
    }

    @Test
    public void findAllMatchingGroups_fourVertical_matchFound() {
        // 세로 4개 연결
        board.placePuyo(new Puyo(PuyoColor.BLUE, 0, 0));
        board.placePuyo(new Puyo(PuyoColor.BLUE, 0, 1));
        board.placePuyo(new Puyo(PuyoColor.BLUE, 0, 2));
        board.placePuyo(new Puyo(PuyoColor.BLUE, 0, 3));

        List<List<Puyo>> groups = matchFinder.findAllMatchingGroups(board);

        assertEquals("1개 그룹 발견", 1, groups.size());
        assertEquals("그룹 크기 4", 4, groups.get(0).size());
    }

    @Test
    public void findAllMatchingGroups_twoSeparateGroups_bothFound() {
        // 첫 번째 그룹: RED 가로 4개
        board.placePuyo(new Puyo(PuyoColor.RED, 0, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 1, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 2, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 3, 0));

        // 두 번째 그룹: BLUE 세로 4개
        board.placePuyo(new Puyo(PuyoColor.BLUE, 5, 0));
        board.placePuyo(new Puyo(PuyoColor.BLUE, 5, 1));
        board.placePuyo(new Puyo(PuyoColor.BLUE, 5, 2));
        board.placePuyo(new Puyo(PuyoColor.BLUE, 5, 3));

        List<List<Puyo>> groups = matchFinder.findAllMatchingGroups(board);

        assertEquals("2개 그룹 발견", 2, groups.size());
    }

    @Test
    public void findAllMatchingGroups_ojamaNotMatched() {
        // OJAMA 4개 연결해도 매칭 안 됨
        board.placePuyo(new Puyo(PuyoColor.OJAMA, 0, 0));
        board.placePuyo(new Puyo(PuyoColor.OJAMA, 1, 0));
        board.placePuyo(new Puyo(PuyoColor.OJAMA, 2, 0));
        board.placePuyo(new Puyo(PuyoColor.OJAMA, 3, 0));

        List<List<Puyo>> groups = matchFinder.findAllMatchingGroups(board);

        assertTrue("OJAMA는 매칭 안 됨", groups.isEmpty());
    }

    @Test
    public void findAllMatchingGroups_hardNotMatched() {
        // HARD 4개 연결해도 매칭 안 됨
        board.placePuyo(new Puyo(PuyoColor.HARD, 0, 0));
        board.placePuyo(new Puyo(PuyoColor.HARD, 1, 0));
        board.placePuyo(new Puyo(PuyoColor.HARD, 2, 0));
        board.placePuyo(new Puyo(PuyoColor.HARD, 3, 0));

        List<List<Puyo>> groups = matchFinder.findAllMatchingGroups(board);

        assertTrue("HARD는 매칭 안 됨", groups.isEmpty());
    }

    @Test
    public void findAllMatchingGroups_tShape_matchFound() {
        // T자 모양 (5개)
        board.placePuyo(new Puyo(PuyoColor.GREEN, 1, 0)); // 아래 중간
        board.placePuyo(new Puyo(PuyoColor.GREEN, 0, 1)); // 왼쪽
        board.placePuyo(new Puyo(PuyoColor.GREEN, 1, 1)); // 중간
        board.placePuyo(new Puyo(PuyoColor.GREEN, 2, 1)); // 오른쪽
        board.placePuyo(new Puyo(PuyoColor.GREEN, 1, 2)); // 위 중간

        List<List<Puyo>> groups = matchFinder.findAllMatchingGroups(board);

        assertEquals("1개 그룹 발견", 1, groups.size());
        assertEquals("그룹 크기 5", 5, groups.get(0).size());
    }

    @Test
    public void findAllMatchingGroups_differentColorsSeparateGroups() {
        // RED 4개, BLUE 4개 서로 인접하지만 다른 색
        board.placePuyo(new Puyo(PuyoColor.RED, 0, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 1, 0));
        board.placePuyo(new Puyo(PuyoColor.RED, 0, 1));
        board.placePuyo(new Puyo(PuyoColor.RED, 1, 1));

        board.placePuyo(new Puyo(PuyoColor.BLUE, 2, 0));
        board.placePuyo(new Puyo(PuyoColor.BLUE, 3, 0));
        board.placePuyo(new Puyo(PuyoColor.BLUE, 2, 1));
        board.placePuyo(new Puyo(PuyoColor.BLUE, 3, 1));

        List<List<Puyo>> groups = matchFinder.findAllMatchingGroups(board);

        assertEquals("2개 그룹 발견", 2, groups.size());
        assertEquals("첫 그룹 크기 4", 4, groups.get(0).size());
        assertEquals("둘째 그룹 크기 4", 4, groups.get(1).size());
    }
}