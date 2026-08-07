package com.puyo.game;

import android.os.Bundle;
import android.view.View;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.puyo.game.input.InputHandler;
import com.puyo.game.input.TouchController;

public class AndroidLauncher extends AndroidApplication {
    static {
        System.loadLibrary("gdx");
        System.loadLibrary("gdx-freetype");
        // libpenguin.so는 gdx-freetype 네이티브 코드 내부에서 dlopen("libpenguin.so")로 로드됨
        // Java에서 별도 로드 불필요 (링커 네임스페이스 분리 문제 방지)
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 풀스크린 몰입 모드 (네비게이션 바/상태 바 숨김)
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);

        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        PuyoGame game = new PuyoGame();
        initialize(game, config);

        // 모바일용 터치 컨트롤러 설정
        // 게임 생성 후 첫 화면이 로드되면 입력 핸들러에 터치 컨트롤러 연결
        // 이는 PuyoGame 내부에서 처리하거나 첫 스크린에서 처리
    }
}
