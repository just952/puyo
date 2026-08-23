package com.puyo.game.logic.engine;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.logic.model.PuyoColor;
import com.puyo.game.util.LogUtil;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

/**
 * 부유 뿌요 낙하 애니메이션 테스트 - 최신 로그 시나리오 기반.
 * 
 * 로그 상황:
 * 1. 연쇄 1단계: BLUE 6개 매치 팝 (4,1)(5,1)(5,2)(4,2)(4,3)(5,0)
 * 2. 팝 후 보드에서 column 4에 부유 뿌요 5개 발견:
 *    - (4,4) RED, (4,5) RED, (4,6) PURPLE, (4,7) PURPLE, (4,8) YELLOW
 * 3. column 4 바닥엔 grounded: (4,0) GREEN, (4,1) GREEN
 * 4. 예상 착지 위치: y=2,3,4,5,6 (grounded 위 연속 쌓임)
 * 5. 착지 후 바운스(SETTLING) → CHAIN_FINDING 전이 → 2차 매치 검색
 */
public class FallingAnimationTest {
    private HeadlessApplication app;
    private GameWorld gameWorld;

    @Before
    public void setUp() {
        HeadlessApplicationConfiguration cfg = new HeadlessApplicationConfiguration();
        app = new HeadlessApplication(new HeadlessGame(), cfg);
        
        gameWorld = new GameWorld();
    }

    @After
    public void tearDown() {
        if (app != null) app.exit();
        if (gameWorld != null) gameWorld.dispose();
    }

    public static class HeadlessGame extends com.badlogic.gdx.Game {
        @Override
        public void create() {}
        @Override
        public void render() {}
    }

    /**
     * 최신 로그 시나리오: column 4에 부유 5개가 grounded 2개 위에 낙하하는 경우.
     * 
     * 초기 보드 (CHAIN_POP_ANIMATION 완료 직후, 팝 후):
     * |......|  <- y=11
     * |......|  <- y=10
     * |......|  <- y=9
     * |...PY.|  <- y=8  : x=3:P, x=4:Y
     * |.P.YP.|  <- y=7  : x=1:P, x=3:Y, x=4:P
     * |.R.PP.|  <- y=6  : x=1:R, x=3:P, x=4:P
     * |.R.RR.|  <- y=5  : x=1:R, x=3:R, x=4:R, x=5:R
     * |.PRYR.|  <- y=4  : x=1:P, x=2:R, x=3:Y, x=4:R  ← 부유 시작 (y=4)
     * |.YGY..|  <- y=3  : x=1:Y, x=2:G, x=3:Y
     * |.RBY..|  <- y=2  : x=1:R, x=2:B, x=3:Y
     * |YRBG..|  <- y=1  : x=0:Y, x=1:R, x=2:B, x=3:G  ← column 4: y=1 GREEN (grounded)
     * |GRBYR.|  <- y=0  : x=0:G, x=1:R, x=2:B, x=3:Y, x=4:R  ← column 4: y=0 GREEN (grounded)
     * +------+
     * 
     * column 4 부유 뿌요 (y=4~8): R, R, P, P, Y (5개)
     * column 4 grounded (y=0,1): G, G (2개)
     * 
     * 예상 낙하 후 위치: y=2,3,4,5,6 (grounded 위에 연속 쌓임)
     * 착지 순서 (아래부터): y=4 RED → y=2, y=5 RED → y=3, y=6 PURPLE → y=4, y=7 PURPLE → y=5, y=8 YELLOW → y=6
     */
    @Test
    public void testFloatingPuyosFallOntoGroundedColumn() {
        // 1. 로그 보드 상태 재현 (팝 완료 직후)
        setupBoardFromLatestLog();
        
        // 2. CHAIN_FLOATING_CHECK 단계로 강제 설정
        setGamePhase(GameWorld.GamePhase.CHAIN_FLOATING_CHECK);
        
        // 3. update 호출로 단계 진행: CHAIN_FLOATING_CHECK → FALLING_ANIMATION
        gameWorld.update(0.016f);
        
        // FALLING_ANIMATION 단계로 전이되었는지 확인
        assertEquals("CHAIN_FLOATING_CHECK → FALLING_ANIMATION 전이", 
                     GameWorld.GamePhase.FALLING_ANIMATION, getGamePhase());
        
        // 부유 뿌요 5개가 animatingPuyos에 추가되었는지 확인
        assertEquals("부유 뿌요 5개가 애니메이션 리스트에 있어야 함", 
                     5, gameWorld.getAnimatingPuyos().size());
        
        // 모든 뿌요가 FALLING 상태인지 확인
        for (Puyo p : gameWorld.getAnimatingPuyos()) {
            assertEquals("부유 뿌요는 FALLING 상태여야 함", 
                         Puyo.State.FALLING, p.getState());
        }
        
        // 4. 낙하 애니메이션 완료까지 반복 update
        int maxFrames = 500; // 충분한 프레임
        boolean fallingCompleted = false;
        int lastAnimatingCount = -1;
        
        for (int frame = 0; frame < maxFrames; frame++) {
            gameWorld.update(0.016f);
            
            // animatingPuyos 크기 변화 로깅 (디버깅용)
            int currentAnimating = gameWorld.getAnimatingPuyos().size();
            if (currentAnimating != lastAnimatingCount) {
                System.out.println("Frame " + frame + ": animatingPuyos=" + currentAnimating + ", phase=" + getGamePhase());
                lastAnimatingCount = currentAnimating;
            }
            
            if (getGamePhase() == GameWorld.GamePhase.CHAIN_FINDING) {
                fallingCompleted = true;
                System.out.println("낙하+바운스 완료 프레임: " + frame);
                break;
            }
        }
        
        assertTrue("낙하+바운스 애니메이션이 완료되어야 함 (CHAIN_FINDING 전이)", fallingCompleted);
        
        // 5. 검증: 모든 뿌요가 바닥에 밀착했는지 확인 (공중에 떠있는 것 없는지)
        Board board = gameWorld.getBoard();
        List<Puyo> floatingAfterFall = board.getAllFloatingPuyos();
        
        System.out.println("낙하 후 보드 상태:\n" + board.toString());
        System.out.println("낙하 후 부유 뿌요 수: " + floatingAfterFall.size());
        if (!floatingAfterFall.isEmpty()) {
            for (Puyo p : floatingAfterFall) {
                System.out.println("  부유 남아있음: (" + p.getX() + "," + p.getY() + ") " + p.getColor());
            }
        }
        
        // 정상 동작 시: floatingAfterFall.size() == 0 (모두 바닥에 착지)
        assertEquals("모든 부유 뿌요가 바닥에 착지해야 함 (공중 정지 버그 없음)", 
                     0, floatingAfterFall.size());
        
        // 6. column 4 검증: grounded(y=0,1) 위에 5개가 연속 쌓여야 함 (y=2,3,4,5,6)
        verifyColumn4Stacking(board);
        
        // 7. column 4의 뿌요들 색상 순서 검증 (아래부터: RED, RED, PURPLE, PURPLE, YELLOW)
        verifyColumn4Colors(board);
    }
    
    /**
     * 최신 로그 기반 보드 상태 재현.
     * BLUE 6개 팝 완료 후 상태.
     */
    private void setupBoardFromLatestLog() {
        Board board = gameWorld.getBoard();
        board.clear();
        
        // y=0 (바닥): |GRBYR.| -> x=0:G, x=1:R, x=2:B, x=3:Y, x=4:R
        placePuyo(board, 0, 0, PuyoColor.GREEN);
        placePuyo(board, 1, 0, PuyoColor.RED);
        placePuyo(board, 2, 0, PuyoColor.BLUE);
        placePuyo(board, 3, 0, PuyoColor.YELLOW);
        placePuyo(board, 4, 0, PuyoColor.GREEN);  // column 4 grounded
        // x=5: 빈칸
        
        // y=1: |YRBG..| -> x=0:Y, x=1:R, x=2:B, x=3:G
        placePuyo(board, 0, 1, PuyoColor.YELLOW);
        placePuyo(board, 1, 1, PuyoColor.RED);
        placePuyo(board, 2, 1, PuyoColor.BLUE);
        placePuyo(board, 3, 1, PuyoColor.GREEN);
        placePuyo(board, 4, 1, PuyoColor.GREEN);  // column 4 grounded
        
        // y=2: |.RBY..| -> x=1:R, x=2:B, x=3:Y
        placePuyo(board, 1, 2, PuyoColor.RED);
        placePuyo(board, 2, 2, PuyoColor.BLUE);
        placePuyo(board, 3, 2, PuyoColor.YELLOW);
        
        // y=3: |.YGY..| -> x=1:Y, x=2:G, x=3:Y
        placePuyo(board, 1, 3, PuyoColor.YELLOW);
        placePuyo(board, 2, 3, PuyoColor.GREEN);
        placePuyo(board, 3, 3, PuyoColor.YELLOW);
        
        // y=4: |.PRYR.| -> x=1:P, x=2:R, x=3:Y, x=4:R  ← 부유 시작
        placePuyo(board, 1, 4, PuyoColor.PURPLE);
        placePuyo(board, 2, 4, PuyoColor.RED);
        placePuyo(board, 3, 4, PuyoColor.YELLOW);
        placePuyo(board, 4, 4, PuyoColor.RED);      // 부유 1
        
        // y=5: |.R.RR.| -> x=1:R, x=3:R, x=4:R, x=5:R
        placePuyo(board, 1, 5, PuyoColor.RED);
        placePuyo(board, 3, 5, PuyoColor.RED);
        placePuyo(board, 4, 5, PuyoColor.RED);      // 부유 2
        placePuyo(board, 5, 5, PuyoColor.RED);
        
        // y=6: |.R.PP.| -> x=1:R, x=3:P, x=4:P
        placePuyo(board, 1, 6, PuyoColor.RED);
        placePuyo(board, 3, 6, PuyoColor.PURPLE);
        placePuyo(board, 4, 6, PuyoColor.PURPLE);   // 부유 3
        
        // y=7: |.P.YP.| -> x=1:P, x=3:Y, x=4:P
        placePuyo(board, 1, 7, PuyoColor.PURPLE);
        placePuyo(board, 3, 7, PuyoColor.YELLOW);
        placePuyo(board, 4, 7, PuyoColor.PURPLE);   // 부유 4
        
        // y=8: |...PY.| -> x=3:P, x=4:Y
        placePuyo(board, 3, 8, PuyoColor.PURPLE);
        placePuyo(board, 4, 8, PuyoColor.YELLOW);   // 부유 5
        
        // y=9,10,11: 빈칸
        
        // 검증: getAllFloatingPuyos가 column 4에서 5개 찾는지 확인
        List<Puyo> floatingBefore = board.getAllFloatingPuyos();
        System.out.println("초기 부유 뿌요 수: " + floatingBefore.size());
        for (Puyo p : floatingBefore) {
            System.out.println("  부유: (" + p.getX() + "," + p.getY() + ") " + p.getColor() + " state=" + p.getState());
        }
        // column 4에서 5개, 다른 열에서도 부유 있을 수 있음
        // 로그에서는 column 4만 5개 나왔음 (다른 열은 grounded 있음)
        assertTrue("최소 column 4의 5개 부유 뿌요는 있어야 함", floatingBefore.size() >= 5);
        
        // column 4 부유 5개 확인
        int col4Floating = 0;
        for (Puyo p : floatingBefore) {
            if (p.getX() == 4) col4Floating++;
        }
        assertEquals("column 4에서 5개 부유", 5, col4Floating);
    }
    
    private void placePuyo(Board board, int x, int y, PuyoColor color) {
        Puyo puyo = new Puyo(color, x, y);
        board.placePuyo(puyo);
    }
    
    /**
     * column 4: grounded(y=0,1) 위에 5개가 연속 쌓여야 함 (y=2,3,4,5,6)
     */
    private void verifyColumn4Stacking(Board board) {
        // y=0,1은 grounded (GREEN, GREEN)
        assertNotNull("y=0 grounded", board.getPuyoAt(4, 0));
        assertNotNull("y=1 grounded", board.getPuyoAt(4, 1));
        assertEquals("y=0 GREEN", PuyoColor.GREEN, board.getPuyoAt(4, 0).getColor());
        assertEquals("y=1 GREEN", PuyoColor.GREEN, board.getPuyoAt(4, 1).getColor());
        
        // y=2~6은 낙하한 부유 뿌요들 (연속, 간격 없음)
        for (int y = 2; y <= 6; y++) {
            assertNotNull("column 4 y=" + y + "에 뿌요 있어야 함", board.getPuyoAt(4, y));
        }
        
        // y=7 이상은 비어있어야 함
        for (int y = 7; y < Board.TOTAL_HEIGHT; y++) {
            assertNull("column 4 y=" + y + "는 비어있어야 함", board.getPuyoAt(4, y));
        }
        
        // 연속성 검증: 각 뿌요가 바로 아래 뿌요 위에 있어야 함
        int lastY = 1; // grounded top
        for (int y = 2; y <= 6; y++) {
            Puyo p = board.getPuyoAt(4, y);
            assertNotNull("연속 쌓임 검증 y=" + y, p);
            // 간격 체크는 verifyContinuousStacking에서 수행
        }
    }
    
    /**
     * column 4 색상 순서 검증 (아래부터 위로): RED, RED, PURPLE, PURPLE, YELLOW
     * 원래 부유 순서 (아래부터): y=4 RED, y=5 RED, y=6 PURPLE, y=7 PURPLE, y=8 YELLOW
     * 낙하 후 (y=2부터): RED, RED, PURPLE, PURPLE, YELLOW
     */
    private void verifyColumn4Colors(Board board) {
        PuyoColor[] expected = {
            PuyoColor.RED,      // y=2 (원래 y=4)
            PuyoColor.RED,      // y=3 (원래 y=5)
            PuyoColor.PURPLE,   // y=4 (원래 y=6)
            PuyoColor.PURPLE,   // y=5 (원래 y=7)
            PuyoColor.YELLOW    // y=6 (원래 y=8)
        };
        
        for (int i = 0; i < expected.length; i++) {
            int y = 2 + i;
            Puyo p = board.getPuyoAt(4, y);
            assertNotNull("column 4 y=" + y + "에 뿌요 있어야 함", p);
            assertEquals("column 4 y=" + y + " 색상", expected[i], p.getColor());
            assertEquals("column 4 y=" + y + " 상태는 NORMAL", Puyo.State.NORMAL, p.getState());
        }
    }
    
    /**
     * 전체 보드 연속 쌓임 검증 (간격 없음)
     */
    private void verifyContinuousStacking(Board board) {
        for (int x = 0; x < Board.WIDTH; x++) {
            int lastY = -1;
            for (int y = 0; y < Board.TOTAL_HEIGHT; y++) {
                Puyo p = board.getPuyoAt(x, y);
                if (p != null) {
                    if (lastY >= 0) {
                        assertEquals("Column " + x + "에서 뿌요 간격 없음 (연속 쌓임)", 
                                     lastY + 1, y);
                    }
                    lastY = y;
                }
            }
        }
    }
    
    // 리플렉션으로 private 필드 접근
    private GameWorld.GamePhase getGamePhase() {
        try {
            java.lang.reflect.Field field = GameWorld.class.getDeclaredField("gamePhase");
            field.setAccessible(true);
            return (GameWorld.GamePhase) field.get(gameWorld);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private void setGamePhase(GameWorld.GamePhase phase) {
        try {
            java.lang.reflect.Field field = GameWorld.class.getDeclaredField("gamePhase");
            field.setAccessible(true);
            field.set(gameWorld, phase);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}