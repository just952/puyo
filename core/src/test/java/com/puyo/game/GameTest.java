package com.puyo.game;

import com.badlogic.gdx.Gdx;
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
    public void fullStartupFlow_noCrash() {
        // 1️⃣ create() → LoadingScreen
        app.getApplicationListener().render();   // create() 호출
        assertTrue(game.getScreen() instanceof LoadingScreen);

        // 2️⃣ LoadingScreen.show() → render() → MenuScreen으로 전환
        app.getApplicationListener().render();   // LoadingScreen.render() → setScreen(MenuScreen)
        assertTrue(game.getScreen() instanceof MenuScreen);

        // 3️⃣ MenuScreen 첫 렌더링
        app.getApplicationListener().render();
        // 예외 없이 여기까지 오면 성공
    }

    @Test
    public void mainMenuLoadsCorrectItems() {
        // MenuLoader 직접 테스트 (헤드리스에서도 파일 읽기 가능)
        MenuItem[] items = MenuLoader.loadMenu("main");
        assertEquals("main.json 에 정의된 5개 항목", 5, items.length);
        assertEquals("normal_mode", items[0].id);
        assertEquals("exit", items[4].id);
    }
}