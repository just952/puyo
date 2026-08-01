package com.puyo.game;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.puyo.game.menus.MenuItem;
import com.puyo.game.menus.MenuLoader;
import com.puyo.game.story.StoryModeManager;
import com.puyo.game.story.StageData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GameTest {
    private HeadlessApplication app;

    @Before
    public void setUp() {
        HeadlessApplicationConfiguration cfg = new HeadlessApplicationConfiguration();
        app = new HeadlessApplication(new HeadlessGame(), cfg);
    }

    @After
    public void tearDown() {
        if (app != null) app.exit();
    }

    // Minimal game for headless testing
    public static class HeadlessGame extends com.badlogic.gdx.Game {
        @Override
        public void create() {}
        @Override
        public void render() {}
    }

    @Test
    public void mainMenuLoadsCorrectItems() {
        MenuItem[] items = MenuLoader.loadMenu("main");
        assertEquals("main.json 에 정의된 5개 항목", 5, items.length);
        assertEquals("normal_mode", items[0].id);
        assertEquals("exit", items[4].id);
    }

    @Test
    public void storyModeSelectLoadsCorrectItems() {
        MenuItem[] items = MenuLoader.loadMenu("story_mode_select");
        assertEquals(4, items.length);
        assertEquals("story_stage_1", items[0].id);
        assertEquals("back_to_main", items[3].id);
    }

    @Test
    public void versusModeSelectLoadsCorrectItems() {
        MenuItem[] items = MenuLoader.loadMenu("versus_mode_select");
        assertEquals(2, items.length);
        assertEquals("versus_endless", items[0].id);
        assertEquals("back_to_main", items[1].id);
    }

    @Test
    public void optionsMenuLoadsCorrectItems() {
        MenuItem[] items = MenuLoader.loadMenu("options_menu");
        assertEquals(3, items.length);
        assertEquals("sound_on", items[0].id);
        assertEquals("back_to_main", items[2].id);
    }

    @Test
    public void storyModeManagerWorks() {
        StoryModeManager mgr = new StoryModeManager();
        assertEquals("초기 언락 스테이지 수", 1, mgr.getUnlockedStageCount());
        
        StageData stage1 = mgr.getStageAt(0);
        assertNotNull("Stage 1 데이터 존재", stage1);
        assertEquals("KIKIMORA", stage1.opponent);
        assertEquals("총 3개 스테이지", 3, mgr.getTotalStages());
    }

    @Test
    public void menuItemStructureIsCorrect() {
        MenuItem item = new MenuItem();
        item.id = "test";
        item.label = "Test Label";
        item.action = "push_screen";
        item.target = "target_screen";
        item.mode = "test_mode";

        assertEquals("test", item.id);
        assertEquals("Test Label", item.label);
        assertEquals("push_screen", item.action);
        assertEquals("target_screen", item.target);
        assertEquals("test_mode", item.mode);
    }
}
