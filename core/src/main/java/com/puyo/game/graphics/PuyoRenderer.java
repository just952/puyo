package com.puyo.game.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.puyo.game.logic.model.PuyoColor;
import com.puyo.game.config.GameViewport;

/**
 * 뿌요 렌더링 전용 클래스.
 * 텍스처 아틀라스를 런타임에 생성하여 SpriteBatch로 뿌요를 렌더링합니다.
 * ShapeRenderer.circle() 대비 드로우콜 감소, 배치 처리 가능.
 * 
 * 추후 실제 아트 에셋(아틀라스 PNG + .atlas 파일)로 교체 가능하도록
 * TextureRegion 기반 인터페이스 유지.
 */
public class PuyoRenderer implements Disposable {
    private static final int PUYO_SIZE = 64; // 아틀라스 내 개별 뿌요 텍스처 크기 (2의 거듭제곱)
    private static final int ATLAS_PADDING = 2;
    private static final int COLORS = 7; // RED, GREEN, BLUE, YELLOW, PURPLE, OJAMA, HARD
    private static final int VARIANTS = 3; // 기본, 하이라이트링, 팝용(작은것)

    private static final String ATLAS_PATH = "assets/puyo_atlas.atlas";
    private static final String ATLAS_PNG_NAME = "puyo_atlas.png";

    private TextureAtlas textureAtlas;
    private Texture atlasTexture; // 직접 생성 시 사용
    private TextureRegion[][] regions; // [color][variant]
    private boolean disposed = false;

    public PuyoRenderer() {
        // 환경에 따른 로드 전략 분기
        if (isProductionEnvironment()) {
            // PRD/안드로이드: classpath만 사용 (읽기 전용, 배포용)
            loadFromClasspathOrThrow();
        } else {
            // DEV/데스크톱: local 우선 (핫리로드), 없으면 classpath, 없으면 생성
            if (!loadFromLocal()) {
                if (!loadFromClasspath()) {
                    generateAtlas();
                    saveToLocal();
                }
            }
        }
    }

    /**
     * 프로덕션 환경 여부 판단
     * - 안드로이드: 무조건 프로덕션
     * - 데스크톱: 시스템 프로퍼티 game.env=production|prd 면 프로덕션
     */
    private boolean isProductionEnvironment() {
        // 안드로이드는 무조건 프로덕션
        if (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android) {
            return true;
        }
        // 데스크톱: 시스템 프로퍼티로 제어 (기본값 development)
        String env = System.getProperty("game.env", "development");
        return "production".equalsIgnoreCase(env) || "prd".equalsIgnoreCase(env);
    }

    /**
     * Classpath에서 로드 (실패 시 예외 발생 - 프로덕션용)
     */
    private void loadFromClasspathOrThrow() {
        try {
            if (!Gdx.files.internal(ATLAS_PATH).exists()) {
                throw new IllegalStateException("Production atlas not found in classpath: " + ATLAS_PATH);
            }
            textureAtlas = new TextureAtlas(Gdx.files.internal(ATLAS_PATH));
            initRegionsFromAtlas();
            Gdx.app.log("PuyoRenderer", "Loaded atlas from classpath (production): " + ATLAS_PATH);
        } catch (Exception e) {
            Gdx.app.error("PuyoRenderer", "Failed to load production atlas", e);
            throw new RuntimeException("Production atlas load failed", e);
        }
    }

    /**
     * Classpath에서 텍스처 아틀라스 로드 (배포/안드로이드용)
     * core/src/main/resources/assets/ 에 있는 파일 사용
     */
    private boolean loadFromClasspath() {
        try {
            if (Gdx.files.internal(ATLAS_PATH).exists()) {
                textureAtlas = new TextureAtlas(Gdx.files.internal(ATLAS_PATH));
                initRegionsFromAtlas();
                Gdx.app.log("PuyoRenderer", "Loaded atlas from classpath: " + ATLAS_PATH);
                return true;
            }
        } catch (Exception e) {
            Gdx.app.log("PuyoRenderer", "Failed to load atlas from classpath: " + e.getMessage());
        }
        return false;
    }

    /**
     * 로컬 파일에서 텍스처 아틀라스 로드 (데스크톱 개발용)
     */
    private boolean loadFromLocal() {
        try {
            String localPath = "assets/" + ATLAS_PNG_NAME.replace(".png", ".atlas");
            if (Gdx.files.local(localPath).exists()) {
                textureAtlas = new TextureAtlas(Gdx.files.local(localPath));
                initRegionsFromAtlas();
                Gdx.app.log("PuyoRenderer", "Loaded atlas from local: " + localPath);
                return true;
            }
        } catch (Exception e) {
            Gdx.app.log("PuyoRenderer", "Failed to load atlas from local: " + e.getMessage());
        }
        return false;
    }

    /**
     * 로컬 파일에 저장 (데스크톱 개발 환경에서만)
     * 안드로이드에서는 쓰기 불가하므로 저장 시도하지 않음
     */
    private void saveToLocal() {
        // 안드로이드에서는 로컬 쓰기 불가/비권장
        if (Gdx.app.getType() == com.badlogic.gdx.Application.ApplicationType.Android) {
            Gdx.app.log("PuyoRenderer", "Android detected, skipping local save");
            return;
        }

        try {
            String pngPath = "assets/" + ATLAS_PNG_NAME;
            String atlasPath = "assets/puyo_atlas.atlas";

            saveAtlasAsPng(pngPath);

            String atlasData = generateAtlasMetadata();
            Gdx.files.local(atlasPath).writeString(atlasData, false);

            Gdx.app.log("PuyoRenderer", "Saved atlas to local: " + pngPath + ", " + atlasPath);
        } catch (Exception e) {
            Gdx.app.error("PuyoRenderer", "Failed to save atlas to local", e);
        }
    }

    /**
     * 현재 아틀라스 텍스처를 PNG로 저장
     */
    private void saveAtlasAsPng(String pngPath) {
        // TextureAtlas에서 바로 저장하는 것은 복잡하므로 다시 그려서 저장
        regenerateAndSavePng(pngPath);
    }

    /**
     * 아틀라스 메타데이터(.atlas 파일 내용) 생성
     */
    private String generateAtlasMetadata() {
        StringBuilder sb = new StringBuilder();
        sb.append(ATLAS_PNG_NAME).append("\n");
        sb.append("format: RGBA8888\n");
        sb.append("filter: Linear,Linear\n");
        sb.append("repeat: none\n");

        String[] colorNames = {"red", "green", "blue", "yellow", "purple", "ojama", "hard"};
        String[] variantNames = {"", "_highlight", "_pop"};

        for (int colorIdx = 0; colorIdx < COLORS; colorIdx++) {
            for (int variant = 0; variant < VARIANTS; variant++) {
                String name = colorNames[colorIdx] + variantNames[variant];
                int x = ATLAS_PADDING + variant * (PUYO_SIZE + ATLAS_PADDING);
                int y = ATLAS_PADDING + colorIdx * (PUYO_SIZE + ATLAS_PADDING);

                sb.append(name).append("\n");
                sb.append("  rotate: false\n");
                sb.append("  xy: ").append(x).append(", ").append(y).append("\n");
                sb.append("  size: ").append(PUYO_SIZE).append(", ").append(PUYO_SIZE).append("\n");
                sb.append("  orig: ").append(PUYO_SIZE).append(", ").append(PUYO_SIZE).append("\n");
                sb.append("  offset: 0, 0\n");
                sb.append("  index: -1\n");
            }
        }

        return sb.toString();
    }

    /**
     * TextureAtlas에서 regions 배열 초기화
     */
    private void initRegionsFromAtlas() {
        String[] colorNames = {"red", "green", "blue", "yellow", "purple", "ojama", "hard"};
        String[] variantNames = {"", "_highlight", "_pop"};

        regions = new TextureRegion[COLORS][VARIANTS];
        for (int colorIdx = 0; colorIdx < COLORS; colorIdx++) {
            for (int variant = 0; variant < VARIANTS; variant++) {
                String name = colorNames[colorIdx] + variantNames[variant];
                TextureRegion region = textureAtlas.findRegion(name);
                if (region != null) {
                    regions[colorIdx][variant] = region;
                } else {
                    Gdx.app.error("PuyoRenderer", "Region not found: " + name);
                }
            }
        }
    }

    /**
     * 아틀라스 다시 그려서 PNG 저장 (최초 저장용)
     */
    private void regenerateAndSavePng(String pngPath) {
        int atlasWidth = (PUYO_SIZE + ATLAS_PADDING) * VARIANTS + ATLAS_PADDING;
        int atlasHeight = (PUYO_SIZE + ATLAS_PADDING) * COLORS + ATLAS_PADDING;

        Pixmap pixmap = new Pixmap(atlasWidth, atlasHeight, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.CLEAR);
        pixmap.fill();

        Color[] puyoColors = {
            Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW,
            Color.MAGENTA, Color.GRAY, Color.BLACK
        };

        for (int colorIdx = 0; colorIdx < COLORS; colorIdx++) {
            Color baseColor = puyoColors[colorIdx];
            int y = ATLAS_PADDING + colorIdx * (PUYO_SIZE + ATLAS_PADDING);

            drawPuyoVariant(pixmap, ATLAS_PADDING, y, PUYO_SIZE, baseColor, true, false);
            drawPuyoVariant(pixmap, ATLAS_PADDING + (PUYO_SIZE + ATLAS_PADDING), y, PUYO_SIZE, baseColor, true, true);
            drawPuyoVariant(pixmap, ATLAS_PADDING + 2 * (PUYO_SIZE + ATLAS_PADDING), y, PUYO_SIZE, baseColor, true, false);
        }

        com.badlogic.gdx.graphics.PixmapIO.writePNG(Gdx.files.local(pngPath), pixmap);
        pixmap.dispose();
    }

    /**
     * 프로그래머용 플레이스홀더 아틀라스 생성.
     * 각 색상별로: 기본 원, 하이라이트 링 포함, 작은 크기(팝 애니메이션용) 3가지 변형 생성.
     * 추후 TexturePacker로 만든 실제 아틀라스로 교체 예정.
     */
    private void generateAtlas() {
        int atlasWidth = (PUYO_SIZE + ATLAS_PADDING) * VARIANTS + ATLAS_PADDING;
        int atlasHeight = (PUYO_SIZE + ATLAS_PADDING) * COLORS + ATLAS_PADDING;

        Pixmap pixmap = new Pixmap(atlasWidth, atlasHeight, Pixmap.Format.RGBA8888);
        pixmap.setColor(Color.CLEAR);
        pixmap.fill();

        // 색상 매핑 (PuyoColor enum 순서와 일치)
        Color[] puyoColors = {
            Color.RED,       // RED
            Color.GREEN,     // GREEN
            Color.BLUE,      // BLUE
            Color.YELLOW,    // YELLOW
            Color.MAGENTA,   // PURPLE
            Color.GRAY,      // OJAMA
            Color.BLACK      // HARD
        };

        for (int colorIdx = 0; colorIdx < COLORS; colorIdx++) {
            Color baseColor = puyoColors[colorIdx];
            int y = ATLAS_PADDING + colorIdx * (PUYO_SIZE + ATLAS_PADDING);

            // Variant 0: 기본 뿌요 (그라데이션 원 + 흰색 하이라이트)
            drawPuyoVariant(pixmap, ATLAS_PADDING, y, PUYO_SIZE, baseColor, true, false);

            // Variant 1: 하이라이트 링 강조 버전 (더 밝은 하이라이트)
            drawPuyoVariant(pixmap, ATLAS_PADDING + (PUYO_SIZE + ATLAS_PADDING), y, PUYO_SIZE, baseColor, true, true);

            // Variant 2: 팝 애니메이션용 작은 뿌요 (기본과 동일, 스케일로 크기 조절)
            drawPuyoVariant(pixmap, ATLAS_PADDING + 2 * (PUYO_SIZE + ATLAS_PADDING), y, PUYO_SIZE, baseColor, true, false);
        }

        atlasTexture = new Texture(pixmap);
        atlasTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();

        // TextureRegion 배열 생성
        regions = new TextureRegion[COLORS][VARIANTS];
        for (int colorIdx = 0; colorIdx < COLORS; colorIdx++) {
            for (int variant = 0; variant < VARIANTS; variant++) {
                int x = ATLAS_PADDING + variant * (PUYO_SIZE + ATLAS_PADDING);
                int y = ATLAS_PADDING + colorIdx * (PUYO_SIZE + ATLAS_PADDING);
                regions[colorIdx][variant] = new TextureRegion(atlasTexture, x, y, PUYO_SIZE, PUYO_SIZE);
            }
        }

        Gdx.app.log("PuyoRenderer", "Generated placeholder atlas: " + atlasWidth + "x" + atlasHeight);
    }

    /**
     * 단일 뿌요 변형 그리기 (Pixmap에 직접 그림)
     */
    private void drawPuyoVariant(Pixmap pixmap, int x, int y, int size, Color baseColor, boolean withHighlight, boolean strongHighlight) {
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        int radius = size / 2 - 2;
        int innerRadius = radius - 4;

        // 1. 바깥 그림자/테두리 (어두운 색)
        Color shadowColor = new Color(baseColor).mul(0.4f);
        pixmap.setColor(shadowColor);
        pixmap.fillCircle(centerX + 1, centerY - 1, radius);

        // 2. 메인 바디 (그라데이션 효과: 바깥은 어둡게, 안은 밝게)
        for (int r = radius; r >= innerRadius; r--) {
            float t = (float)(radius - r) / (radius - innerRadius);
            Color c = new Color(baseColor).lerp(Color.WHITE, t * 0.3f);
            pixmap.setColor(c);
            pixmap.drawCircle(centerX, centerY, r);
        }

        // 3. 내부 채우기
        pixmap.setColor(baseColor);
        pixmap.fillCircle(centerX, centerY, innerRadius - 1);

        // 4. 하이라이트 (위쪽 반원)
        if (withHighlight) {
            Color highlight = strongHighlight ? Color.WHITE : new Color(1, 1, 1, 0.6f);
            pixmap.setColor(highlight);
            for (int r = innerRadius - 2; r >= innerRadius - 6; r--) {
                // 위쪽 180도만 그리기
                for (int angle = 0; angle <= 180; angle++) {
                    double rad = Math.toRadians(angle);
                    int hx = centerX + (int)(Math.cos(rad) * r);
                    int hy = centerY + (int)(Math.sin(rad) * r);
                    if (hx >= x && hx < x + size && hy >= y && hy < y + size) {
                        pixmap.drawPixel(hx, hy);
                    }
                }
            }
            
            // 중앙 작은 하이라이트 점
            pixmap.setColor(Color.WHITE);
            pixmap.fillCircle(centerX - 3, centerY + 4, 3);
        }
    }

    /**
     * 뿌요 그리기 (기본 변형)
     * 
     * @param batch SpriteBatch
     * @param color 뿌요 색상
     * @param x 화면 X 좌표 (왼쪽 아래)
     * @param y 화면 Y 좌표 (왼쪽 아래)
     * @param cellSize 한 칸 크기 (보통 80f)
     * @param scale 팝 애니메이션 스케일 (1.0 = 정상)
     */
    public void draw(SpriteBatch batch, PuyoColor color, float x, float y, float cellSize, float scale) {
        if (disposed || color == null) return;
        
        int colorIdx = color.ordinal();
        if (colorIdx < 0 || colorIdx >= COLORS) return;
        
        if (scale <= 0) return;

        TextureRegion region = regions[colorIdx][0]; // 기본 변형
        float drawSize = cellSize * scale;
        float offset = (cellSize - drawSize) / 2f;

        batch.draw(region, 
            x + offset, y + offset,
            drawSize, drawSize);
    }

    /**
     * 뿌요 그리기 (하이라이트 변형 - 현재 조작 중인 뿌요 등)
     */
    public void drawHighlighted(SpriteBatch batch, PuyoColor color, float x, float y, float cellSize, float scale) {
        if (disposed || color == null) return;
        
        int colorIdx = color.ordinal();
        if (colorIdx < 0 || colorIdx >= COLORS) return;
        
        if (scale <= 0) return;

        TextureRegion region = regions[colorIdx][1]; // 하이라이트 변형
        float drawSize = cellSize * scale;
        float offset = (cellSize - drawSize) / 2f;

        batch.draw(region,
            x + offset, y + offset,
            drawSize, drawSize);
    }

    /**
     * 팝 애니메이션용 뿌요 그리기 (작은 변형)
     * scale 파라미터로 크기 조절하므로 기본 변형과 동일하게 사용 가능
     */
    public void drawPop(SpriteBatch batch, PuyoColor color, float x, float y, float cellSize, float scale) {
        draw(batch, color, x, y, cellSize, scale);
    }

    /**
     * 아틀라스 텍스처 반환 (디버깅/검사용)
     */
    public Texture getAtlasTexture() {
        if (textureAtlas != null) {
            return textureAtlas.getTextures().first();
        }
        return atlasTexture;
    }

    @Override
    public void dispose() {
        if (!disposed) {
            if (textureAtlas != null) {
                textureAtlas.dispose();
                textureAtlas = null;
            }
            if (atlasTexture != null) {
                atlasTexture.dispose();
                atlasTexture = null;
            }
            regions = null;
            disposed = true;
        }
    }
}