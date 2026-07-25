package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoColor;
import com.puyo.game.logic.model.PuyoPair;

import java.util.List;

/**
 * GravityEngine 및 보드 상태 로직을 독립적으로 검증하기 위한 단위 테스트 실행 클래스입니다.
 */
public class GravityEngineTest {

    public static void main(String[] args) {
        System.out.println("=========================================");
        System.out.println("Puyo Core Logic Unit Tests Starting...");
        System.out.println("=========================================");

        try {
            testTwoPointerGravity();
            testDfsMatchingAndClearing();
            testFourDirectionalRotation();

            System.out.println("\n=========================================");
            System.out.println("ALL TESTS PASSED SUCCESSFULLY! 🎉");
            System.out.println("=========================================");
        } catch (Throwable t) {
            System.err.println("\n❌ TEST FAILED with exception:");
            t.printStackTrace();
            System.exit(1);
        }
    }

    private static void testTwoPointerGravity() {
        System.out.println("Running testTwoPointerGravity...");
        Board board = new Board();
        GravityEngine engine = new GravityEngine(board);

        // x=0 열에 공중에 떠 있는 뿌요 설정
        // y=0: null, y=1: RED, y=2: null, y=3: BLUE, y=4: null, y=5: GREEN
        board.setPuyoAt(0, 1, new Puyo(PuyoColor.RED, 0, 1));
        board.setPuyoAt(0, 3, new Puyo(PuyoColor.BLUE, 0, 3));
        board.setPuyoAt(0, 5, new Puyo(PuyoColor.GREEN, 0, 5));

        System.out.println("  [Before Gravity] Column 0 state (y=0 to 5):");
        for (int y = 0; y <= 5; y++) {
            Puyo p = board.getPuyoAt(0, y);
            System.out.println("    y=" + y + ": " + (p != null ? p.getColor() : "empty"));
        }

        boolean moved = engine.applyGravity();

        System.out.println("  [After Gravity] Column 0 state (y=0 to 5):");
        for (int y = 0; y <= 5; y++) {
            Puyo p = board.getPuyoAt(0, y);
            System.out.println("    y=" + y + ": " + (p != null ? p.getColor() : "empty"));
        }

        // 검증: 바닥부터 순서대로 RED, BLUE, GREEN이 쌓여야 함
        assert moved : "Gravity should report that movement occurred";
        assert board.getPuyoAt(0, 0) != null && board.getPuyoAt(0, 0).getColor() == PuyoColor.RED : "y=0 should be RED";
        assert board.getPuyoAt(0, 1) != null && board.getPuyoAt(0, 1).getColor() == PuyoColor.BLUE : "y=1 should be BLUE";
        assert board.getPuyoAt(0, 2) != null && board.getPuyoAt(0, 2).getColor() == PuyoColor.GREEN : "y=2 should be GREEN";
        assert board.getPuyoAt(0, 3) == null : "y=3 should now be empty";
        assert board.getPuyoAt(0, 4) == null : "y=4 should now be empty";
        assert board.getPuyoAt(0, 5) == null : "y=5 should now be empty";

        System.out.println("  => testTwoPointerGravity: PASSED!");
    }

    private static void testDfsMatchingAndClearing() {
        System.out.println("\nRunning testDfsMatchingAndClearing...");
        Board board = new Board();
        GravityEngine engine = new GravityEngine(board);

        // 3개짜리 인접 그룹 (소멸되면 안 됨)
        board.setPuyoAt(1, 0, new Puyo(PuyoColor.YELLOW, 1, 0));
        board.setPuyoAt(2, 0, new Puyo(PuyoColor.YELLOW, 2, 0));
        board.setPuyoAt(2, 1, new Puyo(PuyoColor.YELLOW, 2, 1));

        // 4개짜리 인접 그룹 (소멸되어야 함)
        board.setPuyoAt(4, 0, new Puyo(PuyoColor.RED, 4, 0));
        board.setPuyoAt(5, 0, new Puyo(PuyoColor.RED, 5, 0));
        board.setPuyoAt(5, 1, new Puyo(PuyoColor.RED, 5, 1));
        board.setPuyoAt(5, 2, new Puyo(PuyoColor.RED, 5, 2));

        List<GravityEngine.Position> matches = engine.findMatches();
        System.out.println("  Matches found size: " + matches.size() + " (Expected: 4)");

        assert matches.size() == 4 : "Expected exactly 4 matched positions (for RED group)";
        for (GravityEngine.Position pos : matches) {
            Puyo p = board.getPuyoAt(pos.x, pos.y);
            assert p != null && p.getColor() == PuyoColor.RED : "Matched positions must be RED";
        }

        // 제거 적용
        engine.clearPositions(matches);

        // 검증: RED는 지워지고 YELLOW는 남아야 함
        assert board.getPuyoAt(4, 0) == null : "(4,0) should be cleared";
        assert board.getPuyoAt(5, 0) == null : "(5,0) should be cleared";
        assert board.getPuyoAt(5, 1) == null : "(5,1) should be cleared";
        assert board.getPuyoAt(5, 2) == null : "(5,2) should be cleared";

        assert board.getPuyoAt(1, 0) != null && board.getPuyoAt(1, 0).getColor() == PuyoColor.YELLOW : "(1,0) should remain YELLOW";
        assert board.getPuyoAt(2, 0) != null && board.getPuyoAt(2, 0).getColor() == PuyoColor.YELLOW : "(2,0) should remain YELLOW";
        assert board.getPuyoAt(2, 1) != null && board.getPuyoAt(2, 1).getColor() == PuyoColor.YELLOW : "(2,1) should remain YELLOW";

        System.out.println("  => testDfsMatchingAndClearing: PASSED!");
    }

    private static void testFourDirectionalRotation() {
        System.out.println("\nRunning testFourDirectionalRotation...");
        Puyo p1 = new Puyo(PuyoColor.BLUE, 0, 0);
        Puyo p2 = new Puyo(PuyoColor.GREEN, 0, 0);
        PuyoPair pair = new PuyoPair(p1, p2);

        // Set the pair's position so that the left puyo (p1) is at (2,11)
        pair.setPosition(2, 11);

        System.out.println("  Initial rotation: " + pair.getRotation() + " (0: UP)");
        assert p1.getX() == 2 && p1.getY() == 11 : "p1 should be at (2,11)";
        assert p2.getX() == 2 && p2.getY() == 12 : "p2 should be at (2,12) for UP";

        // 시계 방향 회전 -> 1: RIGHT
        pair.rotateClockwise();
        System.out.println("  After Clockwise: " + pair.getRotation() + " (1: RIGHT)");
        assert p2.getX() == 3 && p2.getY() == 11 : "p2 should be at (3,11) for RIGHT";

        // 시계 방향 회전 -> 2: DOWN
        pair.rotateClockwise();
        System.out.println("  After Clockwise: " + pair.getRotation() + " (2: DOWN)");
        assert p2.getX() == 2 && p2.getY() == 10 : "p2 should be at (2,10) for DOWN";

        // 시계 방향 회전 -> 3: LEFT
        pair.rotateClockwise();
        System.out.println("  After Clockwise: " + pair.getRotation() + " (3: LEFT)");
        assert p2.getX() == 1 && p2.getY() == 11 : "p2 should be at (1,11) for LEFT";

        // 시계 방향 회전 -> 0: UP
        pair.rotateClockwise();
        System.out.println("  After Clockwise: " + pair.getRotation() + " (0: UP)");
        assert p2.getX() == 2 && p2.getY() == 12 : "p2 should be at (2,12) for UP";

        // 반시계 방향 회전 -> 3: LEFT
        pair.rotateCounterClockwise();
        System.out.println("  After CounterClockwise: " + pair.getRotation() + " (3: LEFT)");
        assert p2.getX() == 1 && p2.getY() == 11 : "p2 should be at (1,11) for LEFT";

        System.out.println("  => testFourDirectionalRotation: PASSED!");
    }
}
