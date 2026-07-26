package com.puyo.game;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.backends.headless.HeadlessApplication;
import com.badlogic.gdx.backends.headless.HeadlessApplicationConfiguration;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class GameTest {
    private PuyoGame game;
    private HeadlessApplication headlessApp;

    @Before
    public void setUp() {
        HeadlessApplicationConfiguration config = new HeadlessApplicationConfiguration();
        config.renderInterval = 0; // render as fast as possible
        headlessApp = new HeadlessApplication(new PuyoGame(), config);
        game = (PuyoGame) headlessApp.getApplicationListener();
    }

    @After
    public void tearDown() {
        if (headlessApp != null) {
            headlessApp.exit();
        }
    }

    @Test
    public void gameCreatesScreen() {
        // Initially screen should be null until first render?
        // Let's advance a frame
        headlessApp.render(); // calls create, resize, render
        assertNotNull("Game screen should be set after init", game.getScreen());
    }
}