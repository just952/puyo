package com.puyo.game;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidLauncher extends AndroidApplication {
    static {
        System.loadLibrary("gdx");
        System.loadLibrary("gdx-freetype");
        // System.loadLibrary("penguin");  // 테스트: libgdx-freetype.so만 로드해도 되는지 확인
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        initialize(new PuyoGame(), config);
    }
}
