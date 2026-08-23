package com.puyo.game.graphics;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.PuyoColor;
import com.puyo.game.config.GameViewport;
import com.puyo.game.util.LogUtil;

/**
 * 뿌요 렌더링 전용 클래스.
 * 텍스처 아틀라스를 런타임에 생성하여 SpriteBatch로 뿌요를 렌더링합니다.
 * ShapeRenderer.circle() 대비 드로우콜 감소, 배치 처리 가능.
 * 
 * 추후 실제 아트 에셋(아틀라스 PNG + .atlas 파일)로 교체 가능하도록
 * TextureRegion 기반 인터페이스 유지.
 * 
 * 하이브리드 모드 지원:
 * - 디자이너 모드: 15가지 연결 상태 각각 별도 이미지 (red_up, red_down, ...)
 * - 프로그래머 모드: 기본 뿌요 + 방향별 오버레이 4개로 런타임 합성
 */
public class PuyoRenderer implements Disposable {
    private static final int PUYO_SIZE = 64; // 아틀라스 내 개별 뿌요 텍스처 크기 (2의 거듭제곱)
    private static final int ATLAS_PADDING = 2;
    private static final int COLORS = 7; // RED, GREEN, BLUE, YELLOW, PURPLE, OJAMA, HARD
    
    // 프로그래머 모드 변형: 기본, 하이라이트, 팝, 오버레이_상, 오버레이_하, 오버레이_좌, 오버레이_우
    private static final int PROGRAMMER_VARIANTS = 7;
    // 디자이너 모드: 15가지 연결 상태 + 단일 (NONE)
    private static final int DESIGNER_STATES = 16;

    private static final String ATLAS_PATH = "assets/puyo_atlas.atlas";
    private static final String ATLAS_PNG_NAME = "puyo_atlas.png";

    private TextureAtlas textureAtlas;
    private Texture atlasTexture; // 직접 생성 시 사용
    
    // 하이브리드 모드 지원 필드
    private boolean designerMode = false;
    private TextureRegion[][][] designerRegions; // [color][connectState][0]
    private TextureRegion[][] programmerRegions; // [color][variant] - 7개
    
    // 기존 호환용 (디자이너 모드일 때 designerRegions[color][0][0] 참조)
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
        String[] variantNames = {"", "_highlight", "_pop", "_overlay_up", "_overlay_down", "_overlay_left", "_overlay_right"};

        for (int colorIdx = 0; colorIdx < COLORS; colorIdx++) {
            for (int variant = 0; variant < PROGRAMMER_VARIANTS; variant++) {
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
     * TextureAtlas에서 regions 배열 초기화 (하이브리드 모드 지원)
     */
    private void initRegionsFromAtlas() {
        // 디자이너 모드 감지: 15가지 연결 상태 이미지 존재 여부 확인
        designerMode = checkDesignerMode();
        
        if (designerMode) {
            initDesignerRegions();
        } else {
            initProgrammerRegions();
        }
        
        // 기존 호환용 regions도 설정 (기본 변형만)
        regions = new TextureRegion[COLORS][3];
        for (int colorIdx = 0; colorIdx < COLORS; colorIdx++) {
            if (designerMode) {
                regions[colorIdx][0] = designerRegions[colorIdx][0][0]; // single
            } else {
                regions[colorIdx][0] = programmerRegions[colorIdx][0]; // base
                regions[colorIdx][1] = programmerRegions[colorIdx][1]; // highlight
                regions[colorIdx][2] = programmerRegions[colorIdx][2]; // pop
            }
        }
    }
    
    /**
     * 디자이너 모드 감지: red_up, red_down 등 4방향 기본 연결 이미지 존재 여부
     */
    private boolean checkDesignerMode() {
        String[] testNames = {"red_up", "red_down", "red_left", "red_right"};
        for (String name : testNames) {
            if (textureAtlas.findRegion(name) == null) {
                return false;
            }
        }
        return true;
    }
    
    /**
     * 디자이너 모드 regions 초기화: 15가지 연결 상태 + single
     */
    private void initDesignerRegions() {
        designerRegions = new TextureRegion[COLORS][DESIGNER_STATES][1];
        String[] colorNames = {"red", "green", "blue", "yellow", "purple", "ojama", "hard"};
        String[] stateNames = {"", "up", "down", "left", "right", "up_down", "left_right", 
                              "up_right", "up_left", "down_right", "down_left",
                              "up_left_right", "down_left_right", "up_down_right", "up_down_left", "all"};
        
        for (int colorIdx = 0; colorIdx < COLORS; colorIdx++) {
            for (int state = 0; state < DESIGNER_STATES; state++) {
                String name = colorNames[colorIdx] + (state == 0 ? "_single" : "_" + stateNames[state]);
                TextureRegion region = textureAtlas.findRegion(name);
                // 누락된 상태는 single로 폴백
                designerRegions[colorIdx][state][0] = region != null ? region : textureAtlas.findRegion(colorNames[colorIdx] + "_single");
            }
        }
        Gdx.app.log("PuyoRenderer", "Designer mode enabled: 15 connection states per color");
    }
    
    /**
     * 프로그래머 모드 regions 초기화: 기본 3개 + 오버레이 4개 = 7개
     */
    private void initProgrammerRegions() {
        programmerRegions = new TextureRegion[COLORS][PROGRAMMER_VARIANTS];
        String[] colorNames = {"red", "green", "blue", "yellow", "purple", "ojama", "hard"};
        String[] variantNames = {"", "_highlight", "_pop", "_overlay_up", "_overlay_down", "_overlay_left", "_overlay_right"};
        
        for (int colorIdx = 0; colorIdx < COLORS; colorIdx++) {
            for (int variant = 0; variant < PROGRAMMER_VARIANTS; variant++) {
                String name = colorNames[colorIdx] + variantNames[variant];
                TextureRegion region = textureAtlas.findRegion(name);
                programmerRegions[colorIdx][variant] = region;
                if (region == null) {
                    Gdx.app.error("PuyoRenderer", "Region not found: " + name);
                }
            }
        }
        Gdx.app.log("PuyoRenderer", "Programmer mode: base + 4 overlay variants per color");
    }

    /**
     * 아틀라스 다시 그려서 PNG 저장 (최초 저장용)
     * 7개 변형 모두 저장 (기본 3개 + 오버레이 4개)
     */
    private void regenerateAndSavePng(String pngPath) {
        int atlasWidth = (PUYO_SIZE + ATLAS_PADDING) * PROGRAMMER_VARIANTS + ATLAS_PADDING;
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

            // Variant 0: 기본 뿌요
            drawPuyoVariant(pixmap, ATLAS_PADDING, y, PUYO_SIZE, baseColor, true, false);

            // Variant 1: 하이라이트 링 강조 버전
            drawPuyoVariant(pixmap, ATLAS_PADDING + (PUYO_SIZE + ATLAS_PADDING), y, PUYO_SIZE, baseColor, true, true);

            // Variant 2: 팝 애니메이션용 작은 뿌요
            drawPuyoVariant(pixmap, ATLAS_PADDING + 2 * (PUYO_SIZE + ATLAS_PADDING), y, PUYO_SIZE, baseColor, true, false);

            // Variant 3-6: 연결 오버레이 4개 (반투명 글로우 - 방향별)
            drawConnectOverlay(pixmap, ATLAS_PADDING + 3 * (PUYO_SIZE + ATLAS_PADDING), y, PUYO_SIZE, baseColor, PuyoConnectState.UP);
            drawConnectOverlay(pixmap, ATLAS_PADDING + 4 * (PUYO_SIZE + ATLAS_PADDING), y, PUYO_SIZE, baseColor, PuyoConnectState.DOWN);
            drawConnectOverlay(pixmap, ATLAS_PADDING + 5 * (PUYO_SIZE + ATLAS_PADDING), y, PUYO_SIZE, baseColor, PuyoConnectState.LEFT);
            drawConnectOverlay(pixmap, ATLAS_PADDING + 6 * (PUYO_SIZE + ATLAS_PADDING), y, PUYO_SIZE, baseColor, PuyoConnectState.RIGHT);
        }

        com.badlogic.gdx.graphics.PixmapIO.writePNG(Gdx.files.local(pngPath), pixmap);
        pixmap.dispose();
    }

    /**
     * 프로그래머용 플레이스홀더 아틀라스 생성.
     * 각 색상별로: 기본 원, 하이라이트 링 포함, 팝용, 연결 오버레이 4개(상/하/좌/우) = 7가지 변형 생성.
     * 추후 TexturePacker로 만든 실제 아틀라스로 교체 예정.
     */
    private void generateAtlas() {
        int atlasWidth = (PUYO_SIZE + ATLAS_PADDING) * PROGRAMMER_VARIANTS + ATLAS_PADDING;
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

            // Variant 3-6: 연결 오버레이 4개 (반투명 글로우 - 방향별)
            drawConnectOverlay(pixmap, ATLAS_PADDING + 3 * (PUYO_SIZE + ATLAS_PADDING), y, PUYO_SIZE, baseColor, PuyoConnectState.UP);
            drawConnectOverlay(pixmap, ATLAS_PADDING + 4 * (PUYO_SIZE + ATLAS_PADDING), y, PUYO_SIZE, baseColor, PuyoConnectState.DOWN);
            drawConnectOverlay(pixmap, ATLAS_PADDING + 5 * (PUYO_SIZE + ATLAS_PADDING), y, PUYO_SIZE, baseColor, PuyoConnectState.LEFT);
            drawConnectOverlay(pixmap, ATLAS_PADDING + 6 * (PUYO_SIZE + ATLAS_PADDING), y, PUYO_SIZE, baseColor, PuyoConnectState.RIGHT);
        }

        atlasTexture = new Texture(pixmap);
        atlasTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        pixmap.dispose();

        // TextureRegion 배열 생성 (기존 호환용 regions)
        regions = new TextureRegion[COLORS][PROGRAMMER_VARIANTS];
        for (int colorIdx = 0; colorIdx < COLORS; colorIdx++) {
            for (int variant = 0; variant < PROGRAMMER_VARIANTS; variant++) {
                int x = ATLAS_PADDING + variant * (PUYO_SIZE + ATLAS_PADDING);
                int y = ATLAS_PADDING + colorIdx * (PUYO_SIZE + ATLAS_PADDING);
                regions[colorIdx][variant] = new TextureRegion(atlasTexture, x, y, PUYO_SIZE, PUYO_SIZE);
            }
        }

        // programmerRegions도 동일하게 설정
        programmerRegions = regions;

        Gdx.app.log("PuyoRenderer", "Generated placeholder atlas: " + atlasWidth + "x" + atlasHeight + " (7 variants per color)");
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
     * 연결 오버레이 그리기 (단순 직사각형만)
     * 자신의 스프라이트 영역 내에서만 그림: 뿌요 가장자리(innerRadius)에서 스프라이트 경계까지
     * 캡(반원) 없음 - 직사각형만으로 깔끔하게 연결
     */
    private void drawConnectOverlay(Pixmap pixmap, int x, int y, int size, Color baseColor, PuyoConnectState direction) {
        // 연결 색상 (원본 색상 유지, 알파 0.85)
        Color bridgeColor = new Color(baseColor);
        bridgeColor.a = 0.85f;
        
        int centerX = x + size / 2;
        int centerY = y + size / 2;
        int radius = size / 2 - 2;
        int innerRadius = radius - 4;
        
        // 다리 두께: 뿌요 내부 반지름의 70%
        int bridgeThickness = (int)(innerRadius * 0.7f);
        int halfThickness = bridgeThickness / 2;
        
        // 다리 길이: 뿌요 내부 가장자리에서 스프라이트 경계까지
        int bridgeLength = (size / 2) - innerRadius; // 64픽셀 기준 6픽셀
        if (bridgeLength < 4) bridgeLength = 4;
        
        pixmap.setColor(bridgeColor);
        
        switch (direction) {
            case UP: {
                // 위쪽(화면상 위): 뿌요 위쪽 가장자리부터 스프라이트 상단까지
                int startY = centerY - innerRadius;  // 뿌요 위쪽 가장자리
                int endY = y;  // 스프라이트 상단 경계
                int actualLength = startY - endY;
                
                // 단순 직사각형만 (캡 없음)
                pixmap.fillRectangle(
                    centerX - halfThickness,
                    endY,
                    bridgeThickness,
                    actualLength
                );
                
                // 하이라이트 (좌측 밝은 선)
                Color highlightColor = new Color(baseColor).lerp(Color.WHITE, 0.4f);
                highlightColor.a = 0.6f;
                pixmap.setColor(highlightColor);
                pixmap.fillRectangle(
                    centerX - halfThickness + 1,
                    endY + 2,
                    Math.max(1, halfThickness / 3),
                    Math.max(1, actualLength - 4)
                );
                break;
            }
            case DOWN: {
                // 아래쪽: 뿌요 아래쪽 가장자리부터 스프라이트 하단까지
                int startY = centerY + innerRadius;
                int endY = y + size;
                int actualLength = endY - startY;
                
                pixmap.fillRectangle(
                    centerX - halfThickness,
                    startY,
                    bridgeThickness,
                    actualLength
                );
                
                Color highlightColor = new Color(baseColor).lerp(Color.WHITE, 0.4f);
                highlightColor.a = 0.6f;
                pixmap.setColor(highlightColor);
                pixmap.fillRectangle(
                    centerX - halfThickness + 1,
                    startY + 2,
                    Math.max(1, halfThickness / 3),
                    Math.max(1, actualLength - 4)
                );
                break;
            }
            case LEFT: {
                // 왼쪽: 뿌요 왼쪽 가장자리부터 스프라이트 좌측까지
                int startX = centerX - innerRadius;
                int endX = x;
                int actualLength = startX - endX;
                
                pixmap.fillRectangle(
                    endX,
                    centerY - halfThickness,
                    actualLength,
                    bridgeThickness
                );
                
                Color highlightColor = new Color(baseColor).lerp(Color.WHITE, 0.4f);
                highlightColor.a = 0.6f;
                pixmap.setColor(highlightColor);
                pixmap.fillRectangle(
                    endX + 2,
                    centerY - halfThickness + 1,
                    Math.max(1, actualLength - 4),
                    Math.max(1, halfThickness / 3)
                );
                break;
            }
            case RIGHT: {
                // 오른쪽: 뿌요 오른쪽 가장자리부터 스프라이트 우측까지
                int startX = centerX + innerRadius;
                int endX = x + size;
                int actualLength = endX - startX;
                
                pixmap.fillRectangle(
                    startX,
                    centerY - halfThickness,
                    actualLength,
                    bridgeThickness
                );
                
                Color highlightColor = new Color(baseColor).lerp(Color.WHITE, 0.4f);
                highlightColor.a = 0.6f;
                pixmap.setColor(highlightColor);
                pixmap.fillRectangle(
                    startX + 2,
                    centerY - halfThickness + 1,
                    Math.max(1, actualLength - 4),
                    Math.max(1, halfThickness / 3)
                );
                break;
            }
            default:
                return;
        }
    }

    /**
     * 뿌요 그리기 (기본 변형) - 균일 스케일
     * 
     * @param batch SpriteBatch
     * @param color 뿌요 색상
     * @param x 화면 X 좌표 (왼쪽 아래)
     * @param y 화면 Y 좌표 (왼쪽 아래)
     * @param cellSize 한 칸 크기 (보통 80f)
     * @param scale 팝 애니메이션 스케일 (1.0 = 정상)
     */
    public void draw(SpriteBatch batch, PuyoColor color, float x, float y, float cellSize, float scale) {
        draw(batch, color, x, y, cellSize, scale, scale);
    }

    /**
     * 뿌요 그리기 (기본 변형) - 비균일 스케일 지원 (착지 바운스용)
     * 
     * @param batch SpriteBatch
     * @param color 뿌요 색상
     * @param x 화면 X 좌표 (왼쪽 아래)
     * @param y 화면 Y 좌표 (왼쪽 아래)
     * @param cellSize 한 칸 크기 (보통 80f)
     * @param scaleX X축 스케일
     * @param scaleY Y축 스케일
     */
    public void draw(SpriteBatch batch, PuyoColor color, float x, float y, float cellSize, float scaleX, float scaleY) {
        if (disposed || color == null) return;
        
        int colorIdx = color.ordinal();
        if (colorIdx < 0 || colorIdx >= COLORS) return;
        
        if (scaleX <= 0 || scaleY <= 0) return;

        TextureRegion region = regions[colorIdx][0]; // 기본 변형
        float drawWidth = cellSize * scaleX;
        float drawHeight = cellSize * scaleY;
        float offsetX = (cellSize - drawWidth) / 2f;
        float offsetY = (cellSize - drawHeight) / 2f;

        //LogUtil.debug("PuyoRenderer", String.format("Drawing Puyo: color=%s, x=%.2f, y=%.2f, cellSize=%.2f, scaleX=%.2f, scaleY=%.2f",             color.name(), x, y, cellSize, scaleX, scaleY));

        batch.draw(region, 
            x + offsetX, y + offsetY,
            drawWidth, drawHeight);
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
    
    /**
     * 연결된 뿌요 그리기 (하이브리드 모드 지원)
     * 디자이너 모드: 15가지 상태별 완성 이미지 사용
     * 프로그래머 모드: 기본 뿌요 + 방향별 오버레이 4개 합성
     * 
     * @param batch SpriteBatch
     * @param board 게임 보드 (인접 뿌요 체크용)
     * @param gridX 보드 X 좌표 (0-5)
     * @param gridY 보드 Y 좌표 (0-13)
     * @param color 뿌요 색상
     * @param screenX 화면 X 좌표
     * @param screenY 화면 Y 좌표
     * @param cellSize 한 칸 크기
     * @param scale 스케일 (팝 애니메이션용)
     */
    public void drawConnected(SpriteBatch batch, Board board, int gridX, int gridY, 
                              PuyoColor color, float screenX, float screenY, float cellSize, float scale) {
        if (disposed || color == null || board == null) return;
        
        int colorIdx = color.ordinal();
        if (colorIdx < 0 || colorIdx >= COLORS) return;
        
        if (scale <= 0) return;
        
        if (designerMode) {
            // 디자이너 모드: 완성된 15가지 상태 이미지 사용
            PuyoConnectState state = PuyoConnectState.fromBoard(board, gridX, gridY, color);
            TextureRegion region = designerRegions[colorIdx][state.ordinal()][0];
            drawRegion(batch, region, screenX, screenY, cellSize, scale);
        } else {
            // 프로그래머 모드: 기본 + 오버레이 합성
            // 1. 기본 뿌요 그리기
            TextureRegion baseRegion = programmerRegions[colorIdx][0];
            drawRegion(batch, baseRegion, screenX, screenY, cellSize, scale);
            
            // 2. 4방향 체크하여 오버레이 그리기
            if (board.hasSameColorAt(gridX, gridY + 1, color)) {
                drawRegion(batch, programmerRegions[colorIdx][3], screenX, screenY, cellSize, scale); // overlay_up
            }
            if (board.hasSameColorAt(gridX, gridY - 1, color)) {
                drawRegion(batch, programmerRegions[colorIdx][4], screenX, screenY, cellSize, scale); // overlay_down
            }
            if (board.hasSameColorAt(gridX - 1, gridY, color)) {
                drawRegion(batch, programmerRegions[colorIdx][5], screenX, screenY, cellSize, scale); // overlay_left
            }
            if (board.hasSameColorAt(gridX + 1, gridY, color)) {
                drawRegion(batch, programmerRegions[colorIdx][6], screenX, screenY, cellSize, scale); // overlay_right
            }
        }
    }
    
    /**
     * 공통 영역 그리기 헬퍼
     */
    private void drawRegion(SpriteBatch batch, TextureRegion region, float x, float y, float cellSize, float scale) {
        if (region == null) return;
        float drawSize = cellSize * scale;
        float offset = (cellSize - drawSize) / 2f;
        batch.draw(region, x + offset, y + offset, drawSize, drawSize);
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