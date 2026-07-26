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
            Gdx.app.error("MenuLoader", "Menu file not found: " + file.path());
            MenuItem errorItem = new MenuItem();
            errorItem.id = "error";
            errorItem.label = "Error: missing menu";
            errorItem.action = "";
            errorItem.target = "";
            errorItem.mode = "";
            return new MenuItem[] { errorItem };
        }
        Json json = new Json();
        try {
            return json.fromJson(MenuItem[].class, file);
        } catch (Exception e) {
            Gdx.app.error("MenuLoader", "Failed to parse menu JSON", e);
            MenuItem errorItem = new MenuItem();
            errorItem.id = "error";
            errorItem.label = "Error: invalid menu";
            errorItem.action = "";
            errorItem.target = "";
            errorItem.mode = "";
            return new MenuItem[] { errorItem };
        }
    }
}
