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
 * 부유 뿌요 낙하 애니메이션 버그 재현 테스트.
 * 로그에 나온 정확한 보드 상태에서 CHAIN_FLOATING_CHECK → FALLING_ANIMATION → CHAIN_FINDING
 * 단계 진행 시 부유 뿌요가 공중에 멈추는지(버그) 검증.
 */
public class FallingAnimationTest {
    private HeadlessApplication app;
    private GameWorld gameWorld;

    @Before
    public void setUp() {
        HeadlessApplicationConfiguration cfg = new HeadlessApplicationConfiguration();
        app = new HeadlessApplication(new HeadlessGame(), cfg);
        
        // GameWorld 생성 (테스트용 보드 주입 가능)
        gameWorld = new GameWorld();
    }

    @After
    public void tearDown() {
        if (app != null) app.exit();
        if (gameWorld != null) gameWorld.dispose();
    }

    // Minimal game for headless testing
    public static class HeadlessGame extends com.badlogic.gdx.Game {
        @Override
        public void create() {}
        @Override
        public void render() {}
    }

    /**
     * 로그에 나온 보드 상태 재현 후 부유 뿌요 낙하 테스트.
     * 
     * 초기 보드 (CHAIN_POP_ANIMATION 완료 직후):
     * |......|
     * |......|
     * |.B....|
     * |.P....|
     * |......|
     * |P.....|
     * |PR....|
     * |RBY..R|
     * |R.PP.Y|
     * |P..R.Y|
     * |....GY|
     * |....GR|
     * +------+
     * 
     * 부유 뿌요 13개:
     * Column 0: y=2(P), y=3(R), y=5(P), y=6(R) - 4개
     * Column 1: y=4(B), y=5(R), y=8(P), y=9(B) - 4개
     * Column 2: y=3(P), y=4(Y) - 2개
     * Column 3: y=2(R), y=3(P) - 2개
     * Column 5: y=8(R), y=9(Y) - 1개? wait, 로그에는 13개라고 나옴
     * 
     * 예상 올바른 동작: 모든 부유 뿌요가 바닥에 밀착하여 낙하
     * - Column 0: y=0,1,2,3 착지
     * - Column 1: y=0,1,2,3 착지  
     * - Column 2: y=0,1 착지
     * - Column 3: y=0,1 착지
     * 
     * 버그 동작: 열 단위 강체 낙하로 공중에 멈춤
     * - Column 1: y=4,5 뿌요가 y=0,1 착지, y=8,9 뿌요가 y=4,5에서 멈춤 (간격 유지)
     */
    @Test
    public void testFloatingPuyosFallToGround() {
        // 1. 로그 보드 상태 재현
        setupBoardFromLog();
        
        // 2. CHAIN_FLOATING_CHECK 단계로 강제 설정
        setGamePhase(GameWorld.GamePhase.CHAIN_FLOATING_CHECK);
        
        // 3. update 호출로 단계 진행: CHAIN_FLOATING_CHECK → FALLING_ANIMATION
        gameWorld.update(0.016f); // 1프레임 (약 16ms)
        
        // FALLING_ANIMATION 단계로 전이되었는지 확인
        assertEquals("CHAIN_FLOATING_CHECK → FALLING_ANIMATION 전이", 
                     GameWorld.GamePhase.FALLING_ANIMATION, getGamePhase());
        
        // 4. 낙하 애니메이션 완료까지 반복 update
        int maxFrames = 500; // 충분한 프레임 (0.025초 간격 * 500 = 12.5초)
        boolean fallingCompleted = false;
        
        for (int frame = 0; frame < maxFrames; frame++) {
            gameWorld.update(0.016f);
            
            if (getGamePhase() == GameWorld.GamePhase.CHAIN_FINDING) {
                fallingCompleted = true;
                System.out.println("낙하 완료 프레임: " + frame);
                break;
            }
        }
        
        assertTrue("낙하 애니메이션이 완료되어야 함 (CHAIN_FINDING 전이)", fallingCompleted);
        
        // 5. 검증: 모든 뿌요가 바닥에 밀착했는지 확인 (공중에 떠있는 것 없는지)
        Board board = gameWorld.getBoard();
        List<Puyo> floatingAfterFall = board.getAllFloatingPuyos();
        
        System.out.println("낙하 후 보드 상태:\n" + board.toString());
        System.out.println("낙하 후 부유 뿌요 수: " + floatingAfterFall.size());
        
        // 버그 재현 시: floatingAfterFall.size() > 0 (공중에 멈춘 뿌요 존재)
        // 정상 동작 시: floatingAfterFall.size() == 0 (모두 바닥에 착지)
        assertEquals("모든 부유 뿌요가 바닥에 착지해야 함 (공중 정지 버그 없음)", 
                     0, floatingAfterFall.size());
        
        // 6. 추가 검증: 각 열의 뿌요들이 연속해서 바닥에 밀착되어 있는지
        verifyContinuousStacking(board);
    }
    
    /**
     * 로그에 나온 정확한 보드 상태 재현.
     * Board.toString()은 y=HEIGHT-1(11)부터 y=0(바닥)까지 출력.
     * 로그 보드:
     * |......|  <- y=11
     * |......|  <- y=10
     * |.B....|  <- y=9
     * |.P....|  <- y=8
     * |......|  <- y=7
     * |P.....|  <- y=6
     * |PR....|  <- y=5
     * |RBY..R|  <- y=4
     * |R.PP.Y|  <- y=3
     * |P..R.Y|  <- y=2
     * |....GY|  <- y=1
     * |....GR|  <- y=0
     * +------+
     * 
     * getAllFloatingPuyos 예상 결과 (로그 기준 13개):
     * Column 0: y=2,3,4,5,6 (P,R,R,P,P) - 5개 (y=0,1 비어있으므로 모두 부유)
     * Column 1: y=4,5,8,9 (B,R,P,B) - 4개 (y=0-3 비어있으므로 모두 부유)
     * Column 2: y=3,4 (P,Y) - 2개 (y=0-2 비어있으므로 모두 부유)
     * Column 3: y=2,3 (R,P) - 2개 (y=0,1 비어있으므로 모두 부유)
     * Column 4: y=0,1 (G,G) - 접지됨 (부유 아님)
     * Column 5: y=0,1,2,3,4,5 (R,Y,Y,Y,R,R) - 모두 접지됨 (부유 아님)
     * Total: 13개
     */
    private void setupBoardFromLog() {
        Board board = gameWorld.getBoard();
        board.clear();
        
        // y=0 (바닥): |....GR| -> x=4:G, x=5:R
        placePuyo(board, 4, 0, PuyoColor.GREEN);
        placePuyo(board, 5, 0, PuyoColor.RED);
        
        // y=1: |....GY| -> x=4:G, x=5:Y
        placePuyo(board, 4, 1, PuyoColor.GREEN);
        placePuyo(board, 5, 1, PuyoColor.YELLOW);
        
        // y=2: |P..R.Y| -> x=0:P, x=3:R, x=5:Y
        placePuyo(board, 0, 2, PuyoColor.PURPLE);
        placePuyo(board, 3, 2, PuyoColor.RED);
        placePuyo(board, 5, 2, PuyoColor.YELLOW);
        
        // y=3: |R.PP.Y| -> x=0:R, x=2:P, x=3:P, x=5:Y
        placePuyo(board, 0, 3, PuyoColor.RED);
        placePuyo(board, 2, 3, PuyoColor.PURPLE);
        placePuyo(board, 3, 3, PuyoColor.PURPLE);
        placePuyo(board, 5, 3, PuyoColor.YELLOW);
        
        // y=4: |RBY..R| -> x=0:R, x=1:B, x=2:Y, x=5:R
        placePuyo(board, 0, 4, PuyoColor.RED);
        placePuyo(board, 1, 4, PuyoColor.BLUE);
        placePuyo(board, 2, 4, PuyoColor.YELLOW);
        placePuyo(board, 5, 4, PuyoColor.RED);
        
        // y=5: |PR....| -> x=0:P, x=1:R
        placePuyo(board, 0, 5, PuyoColor.PURPLE);
        placePuyo(board, 1, 5, PuyoColor.RED);
        
        // y=6: |P.....| -> x=0:P
        placePuyo(board, 0, 6, PuyoColor.PURPLE);
        
        // y=7: |......| -> 없음
        
        // y=8: |.P....| -> x=1:P
        placePuyo(board, 1, 8, PuyoColor.PURPLE);
        
        // y=9: |.B....| -> x=1:B
        placePuyo(board, 1, 9, PuyoColor.BLUE);
        
        // y=10: |......| -> 없음
        // y=11: |......| -> 없음
        
        // 검증: 로그에서 getAllFloatingPuyos가 13개 찾았는지 확인
        List<Puyo> floatingBefore = board.getAllFloatingPuyos();
        System.out.println("초기 부유 뿌요 수: " + floatingBefore.size());
        for (Puyo p : floatingBefore) {
            System.out.println("  부유: (" + p.getX() + "," + p.getY() + ") " + p.getColor());
        }
        assertEquals("로그와 동일한 13개 부유 뿌요", 13, floatingBefore.size());
    }
    
    private void placePuyo(Board board, int x, int y, PuyoColor color) {
        Puyo puyo = new Puyo(color, x, y);
        board.placePuyo(puyo);
    }
    
    /**
     * 각 열의 뿌요들이 바닥부터 연속해서 쌓여있는지 검증 (간격 없음)
     */
    private void verifyContinuousStacking(Board board) {
        for (int x = 0; x < Board.WIDTH; x++) {
            int lastY = -1;
            for (int y = 0; y < Board.TOTAL_HEIGHT; y++) {
                Puyo p = board.getPuyoAt(x, y);
                if (p != null) {
                    if (lastY >= 0) {
                        // 이전 뿌요 바로 위에 있어야 함 (간격 1)
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