package com.puyo.game.config;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * 게임 전체에서 공통으로 사용하는 뷰포트 설정.
 * 가상 해상도 960×1600 (3:5 비율, 세로 모드 기준)을 기준으로
 * 실제 화면 크기에 맞춰 비율을 유지하며 스케일링합니다.
 */
public class GameViewport {
    /** 가상 가로 해상도 */
    public static final float VIRTUAL_WIDTH = 960f;
    
    /** 가상 세로 해상도 */
    public static final float VIRTUAL_HEIGHT = 1600f;
    
    /** 보드 영역의 가상 좌표계에서의 크기 (6칸 × 12칸) */
    public static final float BOARD_WIDTH = 480f;   // 6 * 80
    public static final float BOARD_HEIGHT = 960f;  // 12 * 80
    
    /** 한 칸의 가상 크기 */
    public static final float CELL_SIZE = 80f;
    
    /** 보드 좌측 하단 오프셋 (가상 좌표계 기준) */
    public static final float BOARD_OFFSET_X = (VIRTUAL_WIDTH - BOARD_WIDTH) / 2f;  // 240
    public static final float BOARD_OFFSET_Y = (VIRTUAL_HEIGHT - BOARD_HEIGHT) / 2f; // 320

    private GameViewport() {}

    /**
     * FitViewport + OrthographicCamera 생성
     * @return 설정된 FitViewport
     */
    public static FitViewport createViewport() {
        OrthographicCamera camera = new OrthographicCamera();
        // y-down 좌표계로 설정 (0,0이 좌측 하단)
        camera.setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        return new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
    }
}
