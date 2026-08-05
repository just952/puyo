package com.puyo.game.config;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;

/**
 * 게임 전체에서 공통으로 사용하는 뷰포트 설정.
 * 가상 해상도 1600×960 (16:9 비율, 가로 고정)을 기준으로
 * 실제 화면 크기에 맞춰 비율을 유지하며 스케일링합니다.
 */
public class GameViewport {
    // === 공통 ===
    /** 가상 가로 해상도 (가로 고정) */
    public static final float VIRTUAL_WIDTH = 1600f;

    /** 가상 세로 해상도 */
    public static final float VIRTUAL_HEIGHT = 960f;

    /** 한 칸의 가상 크기 */
    public static final float CELL_SIZE = 80f;

    /** 보드 열 수 */
    public static final int BOARD_COLS = 6;

    /** 보드 행 수 */
    public static final int BOARD_ROWS = 12;

    /** 보드 영역 너비 (6 * 80 = 480) */
    public static final float BOARD_WIDTH = BOARD_COLS * CELL_SIZE; // 480

    /** 보드 영역 높이 (12 * 80 = 960) */
    public static final float BOARD_HEIGHT = BOARD_ROWS * CELL_SIZE; // 960

    // === 싱글 플레이 (보드 왼쪽, 사이드 패널 오른쪽) ===
    public static final class Single {
        /** 보드 좌측 하단 X 오프셋 */
        public static final float BOARD_OFFSET_X = 80f;

        /** 보드 좌측 하단 Y 오프셋 */
        public static final float BOARD_OFFSET_Y = 0f;

        /** 사이드 패널 시작 X 위치 */
        public static final float SIDE_PANEL_X = BOARD_OFFSET_X + BOARD_WIDTH + 40f; // 600

        /** 사이드 패널 너비 */
        public static final float SIDE_PANEL_WIDTH = VIRTUAL_WIDTH - SIDE_PANEL_X - 80f; // ~920

        /** 사이드 패널 높이 */
        public static final float SIDE_PANEL_HEIGHT = VIRTUAL_HEIGHT;

        /** 다음 뿌요 프리뷰 X 위치 */
        public static final float NEXT_PREVIEW_X = SIDE_PANEL_X + 40f;

        /** 다음 뿌요 프리뷰 Y 위치 (상단부터) */
        public static final float NEXT_PREVIEW_Y = VIRTUAL_HEIGHT - 200f;

        /** 점수/연쇄 UI X 위치 */
        public static final float UI_X = SIDE_PANEL_X + 40f;

        /** 점수/연쇄 UI Y 위치 */
        public static final float UI_Y = VIRTUAL_HEIGHT - 100f;
    }

    // === 대전 모드 (두 보드 나란히 + 중앙 UI) ===
    public static final class Versus {
        /** 중앙 UI 너비 */
        public static final float CENTER_UI_WIDTH = 200f;

        /** 두 보드 + 중앙 UI 총 너비 */
        public static final float TOTAL_BOARDS_WIDTH = BOARD_WIDTH * 2 + CENTER_UI_WIDTH; // 1160

        /** 양쪽 여백 */
        public static final float SIDE_MARGIN = (VIRTUAL_WIDTH - TOTAL_BOARDS_WIDTH) / 2f; // 220

        /** P1 보드 X 오프셋 */
        public static final float P1_BOARD_OFFSET_X = SIDE_MARGIN; // 220

        /** P1 보드 Y 오프셋 */
        public static final float P1_BOARD_OFFSET_Y = 0f;

        /** P2 보드 X 오프셋 */
        public static final float P2_BOARD_OFFSET_X = SIDE_MARGIN + BOARD_WIDTH + CENTER_UI_WIDTH; // 900

        /** P2 보드 Y 오프셋 */
        public static final float P2_BOARD_OFFSET_Y = 0f;

        /** 중앙 UI X 오프셋 */
        public static final float CENTER_UI_OFFSET_X = SIDE_MARGIN + BOARD_WIDTH; // 700

        /** 중앙 UI Y 오프셋 */
        public static final float CENTER_UI_OFFSET_Y = 0f;

        /** 중앙 UI 너비 */
        public static final float CENTER_UI_WIDTH_ACTUAL = CENTER_UI_WIDTH;

        /** 중앙 UI 높이 */
        public static final float CENTER_UI_HEIGHT = VIRTUAL_HEIGHT;
    }

    // === 메뉴/UI (전체 화면 활용, 중앙 정렬) ===
    public static final class Menu {
        /** 콘텐츠 영역 너비 */
        public static final float CONTENT_WIDTH = 1200f;

        /** 콘텐츠 영역 높이 */
        public static final float CONTENT_HEIGHT = 720f;

        /** 콘텐츠 영역 X 오프셋 (중앙 정렬) */
        public static final float CONTENT_OFFSET_X = (VIRTUAL_WIDTH - CONTENT_WIDTH) / 2f; // 200

        /** 콘텐츠 영역 Y 오프셋 (중앙 정렬) */
        public static final float CONTENT_OFFSET_Y = (VIRTUAL_HEIGHT - CONTENT_HEIGHT) / 2f; // 120
    }

    private GameViewport() {
    }

    /**
     * FitViewport + OrthographicCamera 생성
     * 
     * @return 설정된 FitViewport
     */
    public static FitViewport createViewport() {
        OrthographicCamera camera = new OrthographicCamera();
        // y-down 좌표계로 설정 (0,0이 좌측 하단)
        camera.setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        return new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
    }
}