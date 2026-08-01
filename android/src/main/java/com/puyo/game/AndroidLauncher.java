package com.puyo.game;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidLauncher extends AndroidApplication {
    static {
        // libgdx-freetype.so (libpenguin.so) depends on libgdx.so
        // Load in correct order to avoid "libpenguin.so not found" error
        System.loadLibrary("gdx");
        System.loadLibrary("gdx-freetype");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        initialize(new PuyoGame(), config);
    }
}
