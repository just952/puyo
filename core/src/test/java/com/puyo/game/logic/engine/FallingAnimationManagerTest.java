package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoColor;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

/**
 * FallingAnimationManager 단위 테스트
 * 팝 애니메이션, 분리 낙하, 부유 뿌요 낙하 로직 검증
 */
public class FallingAnimationManagerTest {

    private Board board;
    private FallingAnimationManager manager;

    @Before
    public void setUp() {
        board = new Board();
        manager = new FallingAnimationManager();
    }

    @Test
    public void testAddChainFalling_addsPoppingPuyos() {
        List<Puyo> group = new ArrayList<>();
        Puyo p1 = new Puyo(PuyoColor.RED, 2, 5);
        Puyo p2 = new Puyo(PuyoColor.RED, 3, 5);
        group.add(p1);
        group.add(p2);

        manager.addChainFalling(board, group);

        List<FallingPuyo.FallType> types = getFallingTypes();
        assertEquals("팝 애니메이션 2개 추가", 2, types.size());
        assertTrue("CHAIN_POP 타입", types.contains(FallingPuyo.FallType.CHAIN_POP));
    }

    @Test
    public void testAddFloatingPuyos_addsFloatingPuyos() {
        List<Puyo> floating = new ArrayList<>();
        floating.add(new Puyo(PuyoColor.BLUE, 1, 3));
        floating.add(new Puyo(PuyoColor.BLUE, 1, 4));

        manager.addFloatingPuyos(floating);

        List<FallingPuyo.FallType> types = getFallingTypes();
        assertEquals("부유 뿌요 2개 추가", 2, types.size());
        assertTrue("FLOATING 타입", types.contains(FallingPuyo.FallType.FLOATING));
    }

    @Test
    public void testAddSeparationFalling_addsSeparationPuyo() {
        Puyo puyo = new Puyo(PuyoColor.GREEN, 2, 5);
        manager.addSeparationFalling(puyo);

        List<FallingPuyo.FallType> types = getFallingTypes();
        assertEquals("분리 낙하 1개 추가", 1, types.size());
        assertTrue("SEPARATION 타입", types.contains(FallingPuyo.FallType.SEPARATION));
    }

    @Test
    public void testUpdate_emptyManager_returnsTrue() {
        boolean done = manager.update(0.016f, board);
        assertTrue("빈 매니저는 즉시 완료", done);
    }

    @Test
    public void testPlaceSeparatedPuyos_placesOnBoard() {
        Puyo puyo = new Puyo(PuyoColor.RED, 2, 5);
        manager.addSeparationFalling(puyo);

        // 낙하 완료까지 충분히 업데이트 (0.05f 간격 * 5칸 = 0.25f, 프레임당 0.016f * 20 = 0.32f)
        for (int i = 0; i < 25; i++) {
            manager.update(0.016f, board);
        }
        manager.placeSeparatedPuyos(board);

        assertNotNull("보드에 배치됨", board.getPuyoAt(2, 0));
    }

    @Test
    public void testPlaceFloatingPuyos_placesOnBoard() {
        Puyo puyo = new Puyo(PuyoColor.BLUE, 2, 3);
        manager.addFloatingPuyos(List.of(puyo));

        // 낙하 완료까지 충분히 업데이트
        for (int i = 0; i < 25; i++) {
            manager.update(0.016f, board);
        }
        manager.placeFloatingPuyos(board);

        assertNotNull("보드 바닥에 배치됨", board.getPuyoAt(2, 0));
    }

    @Test
    public void testFloatingPuyoFallsToBottom() {
        Puyo puyo = new Puyo(PuyoColor.YELLOW, 3, 5);
        manager.addFloatingPuyos(List.of(puyo));

        // 5칸 낙하: 0.05f * 5 = 0.25f, 프레임당 0.016f * 20 = 0.32f
        for (int i = 0; i < 25; i++) {
            manager.update(0.016f, board);
        }

        manager.placeFloatingPuyos(board);

        assertNotNull("바닥(y=0)에 배치됨", board.getPuyoAt(3, 0));
        assertEquals("색상 유지", PuyoColor.YELLOW, board.getPuyoAt(3, 0).getColor());
    }

    @Test
    public void testMultipleColumnsFallIndependently() {
        manager.addFloatingPuyos(List.of(
                new Puyo(PuyoColor.RED, 1, 5),
                new Puyo(PuyoColor.BLUE, 4, 3)));

        // 낙하 완료까지 업데이트
        for (int i = 0; i < 30; i++) {
            manager.update(0.016f, board);
        }

        manager.placeFloatingPuyos(board);

        assertNotNull("열 1 바닥에 배치", board.getPuyoAt(1, 0));
        assertNotNull("열 4 바닥에 배치", board.getPuyoAt(4, 0));
    }

    @Test
    public void testFloatingPuyoStacksOnExistingPuyo() {
        board.placePuyo(new Puyo(PuyoColor.GREEN, 2, 0));

        manager.addFloatingPuyos(List.of(new Puyo(PuyoColor.RED, 2, 3)));

        for (int i = 0; i < 25; i++) {
            manager.update(0.016f, board);
        }

        manager.placeFloatingPuyos(board);

        assertNotNull("기존 뿌요 위에 배치", board.getPuyoAt(2, 1));
        assertEquals("RED 색상", PuyoColor.RED, board.getPuyoAt(2, 1).getColor());
    }

    @Test
    public void testCanFallInColumn_considersOtherFallingPuyos() {
        // 같은 열에 두 뿌요가 연속으로 있을 때 아래쪽이 먼저 움직이고 위쪽이 따라감
        manager.addFloatingPuyos(List.of(
                new Puyo(PuyoColor.RED, 2, 4), // 위
                new Puyo(PuyoColor.BLUE, 2, 3) // 아래
        ));

        // 첫 업데이트: 아래쪽만 한 칸 이동 (y=3 -> y=2)
        manager.update(0.06f, board); // 0.05f 간격 넘김

        // 아래쪽은 y=2, 위쪽은 y=4 (아직 이동 못 함)
        List<FallingPuyo> falling = manager.getFallingPuyos();
        int bottomY = Integer.MAX_VALUE;
        for (FallingPuyo fp : falling) {
            if (fp.puyo.getY() < bottomY)
                bottomY = fp.puyo.getY();
        }
        assertEquals("아래쪽만 한 칸 이동", 2, bottomY);
    }

    @Test
    public void testUpdate_popAnimationBlocksFall() {
        // 팝 중인 뿌요가 있으면 낙하 차단
        Puyo popping = new Puyo(PuyoColor.RED, 2, 3);
        popping.startPop(); // 팝 시작
        manager.addChainFalling(board, List.of(popping));

        // 팝 진행 중 (0.3f 지속)
        boolean done = manager.update(0.016f, board);
        assertFalse("팝 진행 중이면 낙하 안 됨", done);

        // 팝 완료까지 업데이트
        for (int i = 0; i < 20; i++) {
            done = manager.update(0.016f, board);
        }
        // 팝 완료 후에는 빈 매니저이므로 true 반환
        assertTrue("팝 완료 후 완료 반환", done);
    }

    private List<FallingPuyo.FallType> getFallingTypes() {
        List<FallingPuyo.FallType> types = new ArrayList<>();
        for (FallingPuyo fp : manager.getFallingPuyos()) {
            types.add(fp.type);
        }
        return types;
    }
}