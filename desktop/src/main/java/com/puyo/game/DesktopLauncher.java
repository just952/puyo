package com.puyo.game;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setForegroundFPS(60);
        config.setTitle("Retro Puyo Puyo");
        // 가로 고정 1600x960 (16:9 비율)
        config.setWindowedMode(1600, 960);
        // 리사이즈 가능, 비율 유지
        config.setResizable(true);
        new Lwjgl3Application(new PuyoGame(), config);
    }
}
