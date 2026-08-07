package com.puyo.game.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.PixmapPacker;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.utils.Disposable;
import com.badlogic.gdx.utils.ObjectMap;

/**
 * 폰트 생성 및 캐싱 관리 클래스.
 * FreeTypeFontGenerator를 사용하여 런타임에 필요한 크기의 폰트를 생성하고 캐시합니다.
 * 한글 지원을 위해 NotoSansKR-Regular.ttf를 사용합니다.
 * 
 * Incremental 모드(동적 글리프 생성)를 사용하여 전체 한글(11,172자) + 영문 + 특수문자 지원.
 * 채팅 기능 등에서 모든 문자 동적 생성 가능.
 */
public class FontManager implements Disposable {
    private static FontManager instance;
    private final FreeTypeFontGenerator generator;
    private final ObjectMap<String, BitmapFont> fontCache = new ObjectMap<>();
    private boolean disposed = false;

    // 공통 파라미터 (incremental 모드용 기본 설정)
    private final FreeTypeFontParameter baseParam;
    private final PixmapPacker packer;

    private FontManager() {
        // TTF 폰트 파일에서 생성기 초기화
        // 플랫폼별로 리소스 경로가 다름:
        // - Android: APK의 assets/ 루트 기준 -> "NotoSansKR-Regular.ttf"
        // - Desktop: core JAR의 classpath 기준 (assets/ 폴더 하위) ->
        // "assets/NotoSansKR-Regular.ttf"
        String fontPath;
        if (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android) {
            fontPath = "NotoSansKR-Regular.ttf";
        } else {
            fontPath = "assets/NotoSansKR-Regular.ttf";
        }
        generator = new FreeTypeFontGenerator(Gdx.files.internal(fontPath));

        // 큰 아틀라스용 팩커 생성 (4096x4096)
        packer = new PixmapPacker(4096, 4096, Pixmap.Format.RGBA8888, 2, false);

        // Incremental 모드용 기본 파라미터 설정
        baseParam = new FreeTypeFontParameter();
        baseParam.incremental = true; // 핵심: 동적 글리프 생성 활성화
        baseParam.packer = packer; // 큰 아틀라스 사용
        baseParam.minFilter = Texture.TextureFilter.Linear;
        baseParam.magFilter = Texture.TextureFilter.Linear;
        baseParam.gamma = 1.8f;
        baseParam.borderWidth = 0;
        baseParam.borderColor = com.badlogic.gdx.graphics.Color.BLACK;
        baseParam.borderStraight = true;

        // 기본 문자셋: DEFAULT_CHARS(영문/숫자/기본기호) + 게임에서 실제 사용하는 한글만
        // 전체 한글 11,172자는 incremental 모드로 동적 생성
        baseParam.characters = FreeTypeFontGenerator.DEFAULT_CHARS + getGameSpecificChars();
    }

    /**
     * 게임 전용 문자들 (메뉴, UI 등)
     */
    private String getGameSpecificChars() {
        return "노말모드엔드리스인대전옵션종료스테이지키키모라백"
                + "연쇄점수레벨일시정지게임오버승리패배시작"
                + "초중급고급설정소리음악진동언어한국어영어";
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
     * 
     * @param size 폰트 크기 (픽셀)
     * @return BitmapFont
     */
    public BitmapFont getTitleFont(int size) {
        return getFont("title", size, true);
    }

    /**
     * 지정된 크기의 메뉴용 폰트 반환
     * 
     * @param size 폰트 크기 (픽셀)
     * @return BitmapFont
     */
    public BitmapFont getMenuFont(int size) {
        return getFont("menu", size, false);
    }

    /**
     * 지정된 크기의 UI용 폰트 반환 (점수, 연쇄 등)
     * 
     * @param size 폰트 크기 (픽셀)
     * @return BitmapFont
     */
    public BitmapFont getUIFont(int size) {
        return getFont("ui", size, false);
    }

    /**
     * 지정된 크기의 작은 폰트 반환 (보조 정보용)
     * 
     * @param size 폰트 크기 (픽셀)
     * @return BitmapFont
     */
    public BitmapFont getSmallFont(int size) {
        return getFont("small", size, false);
    }

    /**
     * 내부 폰트 생성/캐싱 로직 (Incremental 모드)
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
        // 기본 파라미터 복사
        param.incremental = baseParam.incremental;
        param.packer = baseParam.packer;
        param.minFilter = baseParam.minFilter;
        param.magFilter = baseParam.magFilter;
        param.gamma = baseParam.gamma;
        param.borderWidth = baseParam.borderWidth;
        param.borderColor = baseParam.borderColor;
        param.borderStraight = baseParam.borderStraight;
        param.characters = baseParam.characters; // 기본 문자셋 포함 (incremental로 추가 글리프 자동 생성)

        param.size = size;

        if (bold) {
            param.borderWidth = Math.max(1, size / 24);
        }

        BitmapFont font = generator.generateFont(param);
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
            packer.dispose();
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
