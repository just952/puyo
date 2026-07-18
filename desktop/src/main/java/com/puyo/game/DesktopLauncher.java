package com.puyo.game;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;

public class DesktopLauncher {
    public static void main (String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setForegroundFPS(60);
        config.setTitle("Retro Puyo Puyo");
        config.setWindowedMode(480, 800); // 세로형 화면 비율 (뿌요뿌요 보드 기준)
        new Lwjgl3Application(new PuyoGame(), config);
    }
}
