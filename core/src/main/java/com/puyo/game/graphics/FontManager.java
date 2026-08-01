package com.puyo.game.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * 폰트 생성 및 캐싱 관리 클래스.
 * FreeTypeFontGenerator를 사용하여 런타임에 필요한 크기의 폰트를 생성하고 캐시합니다.
 * 한글 지원을 위해 NotoSansKR-Regular.ttf를 사용합니다.
 */
public class FontManager implements Disposable {
    private static FontManager instance;
    private final FreeTypeFontGenerator generator;
    private final ObjectMap<String, BitmapFont> fontCache = new ObjectMap<>();
    private boolean disposed = false;

    private FontManager() {
        // TTF 폰트 파일에서 생성기 초기화
        generator = new FreeTypeFontGenerator(Gdx.files.internal("fonts/NotoSansKR-Regular.ttf"));
    }

    /**
     * 싱글톤 인스턴스 반환
     */
    public static FontManager getInstance() {
        if (instance == null) {
            instance = new FontManager();
        }
        return instance;
    }

    /**
     * 지정된 크기의 타이틀용 폰트 반환 (Bold 스타일)
     * @param size 폰트 크기 (픽셀)
     * @return BitmapFont
     */
    public BitmapFont getTitleFont(int size) {
        return getFont("title", size, true);
    }

    /**
     * 지정된 크기의 메뉴용 폰트 반환
     * @param size 폰트 크기 (픽셀)
     * @return BitmapFont
     */
    public BitmapFont getMenuFont(int size) {
        return getFont("menu", size, false);
    }

    /**
     * 지정된 크기의 UI용 폰트 반환 (점수, 연쇄 등)
     * @param size 폰트 크기 (픽셀)
     * @return BitmapFont
     */
    public BitmapFont getUIFont(int size) {
        return getFont("ui", size, false);
    }

    /**
     * 지정된 크기의 작은 폰트 반환 (보조 정보용)
     * @param size 폰트 크기 (픽셀)
     * @return BitmapFont
     */
    public BitmapFont getSmallFont(int size) {
        return getFont("small", size, false);
    }

    /**
     * 내부 폰트 생성/캐싱 로직
     */
    private BitmapFont getFont(String prefix, int size, boolean bold) {
        if (disposed) {
            throw new IllegalStateException("FontManager has been disposed");
        }

        String key = prefix + "_" + size + (bold ? "_bold" : "");
        BitmapFont cached = fontCache.get(key);
        if (cached != null) {
            return cached;
        }

        FreeTypeFontParameter param = new FreeTypeFontParameter();
        param.size = size;
        param.borderWidth = 0;
        param.borderColor = com.badlogic.gdx.graphics.Color.BLACK;
        param.borderStraight = true;
        param.mipmap = true;
        param.minFilter = Texture.TextureFilter.MipMapLinearLinear;
        param.magFilter = Texture.TextureFilter.Linear;
        param.gamma = 1.8f; // 감마 보정으로 한글 가독성 향상
        param.hinting = FreeTypeFontParameter.Hinting.AutoFull;

        // Bold가 필요하면 크기를 약간 키우고 테두리로 시뮬레이션 (FreeType에서 직접 bold 지원 안 함)
        if (bold) {
            param.borderWidth = Math.max(1, size / 24);
        }

        BitmapFont font = generator.generateFont(param);
        // 텍스처 필터 설정으로 부드러운 확대/축소
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        fontCache.put(key, font);
        return font;
    }

    /**
     * 미리 정의된 크기의 폰트들 미리 생성 (로딩 화면에서 호출 권장)
     */
    public void preloadCommonFonts() {
        // 타이틀: 48, 36
        getTitleFont(48);
        getTitleFont(36);
        
        // 메뉴: 32, 28
        getMenuFont(32);
        getMenuFont(28);
        
        // UI: 24, 20, 18
        getUIFont(24);
        getUIFont(20);
        getUIFont(18);
        
        // 작은 폰트: 16, 14
        getSmallFont(16);
        getSmallFont(14);
    }

    /**
     * 특정 키의 폰트 제거 (메모리 관리용)
     */
    public void removeFont(String prefix, int size, boolean bold) {
        String key = prefix + "_" + size + (bold ? "_bold" : "");
        BitmapFont font = fontCache.remove(key);
        if (font != null) {
            font.dispose();
        }
    }

    /**
     * 모든 캐시된 폰트와 생성기 해제
     */
    @Override
    public void dispose() {
        if (!disposed) {
            for (BitmapFont font : fontCache.values()) {
                font.dispose();
            }
            fontCache.clear();
            generator.dispose();
            disposed = true;
            instance = null;
        }
    }

    /**
     * 앱 종료 시 정리용 정적 메서드
     */
    public static void disposeInstance() {
        if (instance != null) {
            instance.dispose();
        }
    }
}
