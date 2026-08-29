package com.puyo.game;

import android.os.Bundle;
import android.view.View;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;
import com.puyo.game.input.AndroidInputHandler;
import com.puyo.game.input.InputProvider;

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
        
        // PuyoGame 익명 클래스로 AndroidInputProvider 제공
        PuyoGame game = new PuyoGame() {
            @Override
            protected InputProvider createInputProvider() {
                return new AndroidInputHandler();
            }
        };
        
        initialize(game, config);
    }
}
