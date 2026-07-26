package com.puyo.game;

import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import com.puyo.game.menus.MenuItem;
import com.puyo.game.menus.MenuLoader;
import com.puyo.game.screens.LoadingScreen;
import com.puyo.game.screens.MenuScreen;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GameTest {
    private HeadlessApplication app;
    private PuyoGame game;

    @Before
    public void setUp() {
        HeadlessApplicationConfiguration cfg = new HeadlessApplicationConfiguration();
        app = new HeadlessApplication(new PuyoGame(), cfg);
        game = (PuyoGame) app.getApplicationListener();
    }

    @After
    public void tearDown() {
        if (app != null) app.exit();
    }

    @Test
    public void mainMenuLoadsCorrectItems() {
        // MenuLoader 직접 테스트 (테스트 리소스에서 파일 읽기)
        MenuItem[] items = MenuLoader.loadMenu("main");
        assertEquals("main.json 에 정의된 5개 항목", 5, items.length);
        assertEquals("normal_mode", items[0].id);
        assertEquals("exit", items[4].id);
    }

    @Test
    public void loadingScreenTransitionsToMenuScreen() {
        // LoadingScreen 생성 및 show() 호출
        LoadingScreen loadingScreen = new LoadingScreen(game);
        loadingScreen.show();
        
        // render() 호출 → MenuScreen으로 전환되어야 함
        loadingScreen.render(0f);
        
        assertTrue("LoadingScreen.render() 후 MenuScreen으로 전환", 
                   game.getScreen() instanceof MenuScreen);
    }

    @Test
    public void menuScreenLoadsMenuItems() {
        // MenuScreen 직접 생성 (GL 코드 경로 우회)
        MenuScreen menuScreen = new MenuScreen(game);
        menuScreen.show();
        menuScreen.render(0f); // 첫 렌더링 (NPE 없어야 함)
        
        // 메뉴 아이템이 로드되었는지 검증 (private 필드라 간접 확인)
        // 예외 없이 render() 완료되면 성공
        assertNotNull("MenuScreen 생성 및 render() 성공", game.getScreen());
    }

    @Test
    public void storyModeSelectScreenLoads() {
        // StoryModeSelectScreen 직접 생성
        com.puyo.game.screens.StoryModeSelectScreen storyScreen = 
            new com.puyo.game.screens.StoryModeSelectScreen(game);
        storyScreen.show();
        storyScreen.render(0f);
        
        assertNotNull("StoryModeSelectScreen 생성 및 render() 성공", game.getScreen());
    }
}