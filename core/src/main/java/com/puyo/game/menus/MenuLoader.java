package com.puyo.game.menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;
import com.badlogic.gdx.utils.Array;

public class MenuLoader {
    private static final String MENU_DIR = "data/menus/";

    public static MenuItem[] loadMenu(String menuId) {
        FileHandle file = Gdx.files.internal(MENU_DIR + menuId + ".json");
        if (!file.exists()) {
            Gdx.app.error("MenuLoader", "Menu file not found: " + menuId + ".json");
            return new MenuItem[0];
        }
        Json json = new Json();
        return json.fromJson(MenuItem[].class, file);
    }
}
