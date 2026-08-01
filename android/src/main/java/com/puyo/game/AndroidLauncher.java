package com.puyo.game;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidLauncher extends AndroidApplication {
    static {
        // libgdx-freetype is packaged as libpenguin.so (legacy name expected by native code)
        System.loadLibrary("gdx");
        System.loadLibrary("penguin");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        initialize(new PuyoGame(), config);
    }
}
