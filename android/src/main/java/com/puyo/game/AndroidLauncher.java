package com.puyo.game;

import android.os.Bundle;
import com.badlogic.gdx.backends.android.AndroidApplication;
import com.badlogic.gdx.backends.android.AndroidApplicationConfiguration;

public class AndroidLauncher extends AndroidApplication {
    static {
        // libgdx-freetype.so (internally called "penguin" in older libgdx versions)
        // Load libgdx first, then load freetype with both possible names
        System.loadLibrary("gdx");
        try {
            System.loadLibrary("gdx-freetype");
        } catch (UnsatisfiedLinkError e) {
            // Fallback: some libgdx versions expect "penguin" as the library name
            System.loadLibrary("penguin");
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AndroidApplicationConfiguration config = new AndroidApplicationConfiguration();
        initialize(new PuyoGame(), config);
    }
}
