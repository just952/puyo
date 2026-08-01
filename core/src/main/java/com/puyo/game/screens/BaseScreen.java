package com.puyo.game.screens;

import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import com.puyo.game.PuyoGame;
import com.puyo.game.config.GameViewport;

/**
 * 모든 Screen의 기본 클래스.
 * 공통 뷰포트/카메라 관리와 resize 처리를 제공합니다.
 */
public abstract class BaseScreen implements Screen {
    protected final PuyoGame game;
    
    /** 화면 좌표 변환용 카메라 */
    protected OrthographicCamera camera;
    
    /** 비율 유지 스케일링용 뷰포트 */
    protected FitViewport viewport;

    public BaseScreen(PuyoGame game) {
        this.game = game;
    }

    @Override
    public void show() {}

    @Override
    public void render(float delta) {}

    @Override
    public void resize(int width, int height) {
        if (viewport != null) {
            viewport.update(width, height, true);
        }
    }

    @Override
    public void pause() {}

    @Override
    public void resume() {}

    @Override
    public void hide() {}

    @Override
    public void dispose() {}

    /**
     * 카메라 반환 (렌더러 프로젝션 매트릭스 설정용)
     */
    protected OrthographicCamera getCamera() {
        return camera;
    }

    /**
     * 뷰포트 반환
     */
    protected Viewport getViewport() {
        return viewport;
    }

    /**
     * 표준 뷰포트 초기화 (show()에서 호출 권장)
     */
    protected void initViewport() {
        viewport = GameViewport.createViewport();
        camera = (OrthographicCamera) viewport.getCamera();
    }
}
