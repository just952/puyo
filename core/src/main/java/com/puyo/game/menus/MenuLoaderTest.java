package com.puyo.game.menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

public class MenuLoaderTest {
    public static void main(String[] args) {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        new HeadlessApplication(new com.badlogic.gdx.ApplicationAdapter() {
            @Override
            public void create() {
                MenuItem[] items = MenuLoader.loadMenu("main");
                Gdx.app.log("MenuLoaderTest", "Loaded " + items.length + " menu items:");
                for (MenuItem item : items) {
                    Gdx.app.log("MenuLoaderTest", "  - " + item.id + ": " + item.label + " (" + item.action + ")");
                }
                Gdx.app.exit();
            }
        }, config);
    }
}
