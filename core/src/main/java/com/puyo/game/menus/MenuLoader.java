package com.puyo.game.menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.Json;

public class MenuLoader {
    private static final String MENU_DIR = "assets/data/menus/";

    public static MenuItem[] loadMenu(String menuId) {
        // 1) classpath 리소스에서 먼저 시도 (테스트용)
        FileHandle file = Gdx.files.classpath(MENU_DIR + menuId + ".json");

        // 2) 없으면 internal (애셋/데스크톱/안드로이드) 폴백
        if (file == null || !file.exists()) {
            file = Gdx.files.internal(MENU_DIR + menuId + ".json");
        }

        if (file == null || !file.exists()) {
            Gdx.app.error("MenuLoader", "Menu file not found: " + menuId + ".json");
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
            Gdx.app.error("MenuLoader", "Failed to parse menu JSON: " + menuId, e);
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
