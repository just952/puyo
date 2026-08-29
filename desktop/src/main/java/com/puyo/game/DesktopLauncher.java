package com.puyo.game;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import com.puyo.game.input.DesktopInputHandler;
import com.puyo.game.input.InputProvider;

public class DesktopLauncher {
    public static void main(String[] arg) {
        Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
        config.setForegroundFPS(60);
        config.setTitle("Retro Puyo Puyo");
        config.setWindowedMode(1600, 960);
        config.setResizable(true);
        
        // PuyoGame 익명 클래스로 InputProvider 오버라이드
        new Lwjgl3Application(new PuyoGame() {
            @Override
            protected InputProvider createInputProvider() {
                return new DesktopInputHandler();
            }
        }, config);
    }
}
