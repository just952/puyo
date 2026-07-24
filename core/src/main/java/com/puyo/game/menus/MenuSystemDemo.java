package com.puyo.game.menus;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;

/**
 * Simple demo to test the menu system without requiring a full GUI environment
 */
public class MenuSystemDemo {
    public static void main(String[] args) {
        // Configure headless application (for testing without display)
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        
        new HeadlessApplication(new com.badlogic.gdx.ApplicationAdapter() {
            @Override
            public void create() {
                Gdx.app.log("MenuSystemDemo", "=== Menu System Test ===");
                
                // Test loading the main menu
                MenuItem[] menuItems = MenuLoader.loadMenu("main");
                Gdx.app.log("MenuSystemDemo", "Loaded menu: main");
                Gdx.app.log("MenuSystemDemo", "Number of items: " + menuItems.length);
                
                for (int i = 0; i < Math.min(menuItems.length, 5); i++) { // Show first 5 items
                    MenuItem item = menuItems[i];
                    Gdx.app.log("MenuSystemDemo", 
                        "  [" + i + "] " + item.id + 
                        ": " + item.label + 
                        " | Action: " + item.action +
                        (item.target != null && !item.target.isEmpty() ? " | Target: " + item.target : "") +
                        (item.mode != null && !item.mode.isEmpty() ? "mode": ""));
                }
                
                if (menuItems.length > 5) {
                    Gdx.app.log("MenuSystemDemo", "  ... and " + (menuItems.length - 5) + " more items");
                }
                
                // Test that we can access the properties correctly
                if (menuItems.length > 0) {
                    MenuItem firstItem = menuItems[0];
                    Gdx.app.log("MenuSystemDemo", 
                        "First item verification: " +
                        "id=normal_mode? " + "normal_mode".equals(firstItem.id) + ", " +
                        "label=노말 모드? " + "노말 모드".equals(firstItem.label) + ", " +
                        "action=push_screen? " + "push_screen".equals(firstItem.action));
                }
                
                Gdx.app.log("MenuSystemDemo", "=== Test Complete ===");
                Gdx.app.exit();
            }
        }, config);
    }
}
