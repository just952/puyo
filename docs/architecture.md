# Puyo Puyo 2 - 기술 스택 및 아키텍처

## 🏗️ 프로젝트 구조 (Multi-module Gradle)

```
puyo/
├── build.gradle                 # 루트 빌드 설정 (AGP 8.1, Kotlin 1.8.0)
├── settings.gradle              # 모듈 포함: core, desktop, android
├── gradle/wrapper/              # Gradle Wrapper 8.4
├── core/                        # 공통 게임 로직 (Java Library)
│   ├── src/main/java/com/puyo/game/
│   │   ├── PuyoGame.java              # 메인 게임 클래스 (Game 상속)
│   │   ├── config/ConfigManager.java  # 설정 관리 (JSON)
│   │   ├── config/GameViewport.java   # 공통 뷰포트 설정 (신규)
│   │   ├── logic/
│   │   │   ├── model/                 # 도메인 모델
│   │   │   │   ├── Puyo.java
│   │   │   │   ├── PuyoColor.java
│   │   │   │   ├── PuyoPair.java
│   │   │   │   ├── Board.java
│   │   │   │   ├── StageData.java
│   │   │   │   └── MatchResult.java
│   │   │   ├── engine/
│   │   │   │   ├── GameWorld.java      # 메인 게임 루프/로직
│   │   │   │   ├── Board.java          # 보드 상태 관리
│   │   │   │   ├── PuyoPairGenerator.java
│   │   │   │   ├── GravityEngine.java
│   │   │   │   └── AIController.java  (예정)
│   │   ├── menus/
│   │   │   ├── MenuLoader.java       # JSON 메뉴 로딩
│   │   │   ├── MenuItem.java
│   │   │   └── MenuAction.java
│   │   ├── screens/
│   │   │   ├── BaseScreen.java
│   │   │   ├── LoadingScreen.java
│   │   │   ├── MenuScreen.java
│   │   │   ├── PlayScreen.java
│   │   │   └── StoryModeSelectScreen.java
│   │   └── story/
│   │       ├── StoryModeManager.java
│   │       ├── StageData.java
│   │       └── StageDataWrapper.java
│   └── src/test/
│       ├── resources/data/menus/      # 테스트용 메뉴 JSON
│       └── src/test/java/.../GameTest.java  # 헤드리스 테스트
├── desktop/                         # 데스크톱 실행 모듈 (LWJGL3)
│   ├── src/main/java/com/puyo/game/DesktopLauncher.java
│   └── build.gradle (application 플러그인)
├── android/                         # 안드로이드 앱 모듈
│   ├── src/main/
│   │   ├── AndroidManifest.xml
│   │   ├── java/com/puyo/game/AndroidLauncher.java
│   │   └── res/values/{strings.xml, colors.xml, styles.xml}
│   └── build.gradle (AGP 8.1, compileSdk 33)
└── .github/workflows/android-build.yml  # CI/CD 파이프라인
```

## 🛠 기술 스택

| 영역 | 기술 | 버전 | 비고 |
|------|------|------|------|
| 언어 | Java | 17 | Android + Desktop 공통 |
| 게임 프레임워크 | LibGDX | 1.12.1 | 핵심 렌더링/입력/오디오 |
| 빌드 시스템 | Gradle | 8.4 | Multi-project 구성 |
| Android | AGP | 8.1.0 | compileSdk 33, targetSdk 33 |
| Desktop | LWJGL3 | 3.3.2 | LibGDX 백엔드 |
| 테스트 | JUnit 5 | 5.10.0 | 헤드리스 단위 테스트 |
| CI/CD | GitHub Actions | - | Android APK 빌드 및 테스트 |

## 🎯 아키텍처 패턴

### 핵심 설계 원칙
1. **플랫폼 독립적 코어** - `core` 모듈은 LibGDX API만 사용, 플랫폼별 코드 없음
2. **Screen 기반 상태 관리** - LibGDX `Game` / `Screen` 패턴으로 화면 전환
3. **데이터 주도 UI** - 메뉴/스테이지 JSON으로 정의, `MenuLoader`로 동적 생성
4. **엔티티-컴포넌트 지향** - `GameWorld`가 `Board`, `PuyoPair` 등 게임 오브젝트 관리

### 주요 클래스 책임

| 클래스 | 책임 |
|--------|------|
| `PuyoGame` | LibGDX `Game` 구현, 전역 설정/로그 레벨 관리 |
| `GameWorld` | 게임 루프, 입력 처리 위임, 보드/뿌요 상태 갱신, 연쇄 판정 |
| `Board` | 6×12 그리드 상태, 중력 적용, 매칭 그룹 탐색, 라인 클리어 |
| `PuyoPair` | 현재 조작 중인 뿌요 쌍 (이동, 회전, 하드 드롭) |
| `GravityEngine` | 중력 시뮬레이션, 낙하 애니메이션 처리 |
| `MenuLoader` | JSON → `MenuItem[]` 파싱, 액션 매핑 |
| `StoryModeManager` | 스테이지 진행도, 잠금/해금, AI 난이도 관리 |
| `ConfigManager` | `assets/config/*.json` 로드, 환경별 설정 분리 |
| `GameViewport` | 공통 뷰포트/카메라 설정, 가상 해상도(960×1600) 관리 |

## 📱 렌더링 아키텍처 (Viewport 도입 완료)

### 가상 해상도 설정
```
VIRTUAL_WIDTH  = 960
VIRTUAL_HEIGHT = 1600
```
- 세로 모드 기준 960×1600 (3:5 비율)
- `FitViewport`로 화면 크기에 맞춰 비율 유지하며 스케일링
- Letterboxing/Pillarboxing 자동 처리

### 구현 구조
```java
// GameViewport.java - 팩토리 클래스
public class GameViewport {
    public static final float VIRTUAL_WIDTH = 960f;
    public static final float VIRTUAL_HEIGHT = 1600f;
    
    public static FitViewport createViewport() {
        OrthographicCamera camera = new OrthographicCamera();
        camera.setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        return new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
    }
}

// BaseScreen.java - 공통 뷰포트 관리
public abstract class BaseScreen implements Screen {
    protected final PuyoGame game;
    protected OrthographicCamera camera;
    protected FitViewport viewport;
    
    @Override
    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }
}
```

### 각 Screen에서의 사용
```java
// PlayScreen, MenuScreen 등에서
@Override
public void show() {
    viewport = GameViewport.createViewport();
    camera = viewport.getCamera();
}

@Override
public void render(float delta) {
    camera.update();
    batch.setProjectionMatrix(camera.combined);
    shapeRenderer.setProjectionMatrix(camera.combined);
    // ... 렌더링 로직 (가상 해상도 기준 좌표 사용)
}
```

### 좌표계 변경
| 구분 | 수정 전 | 수정 후 |
|------|---------|---------|
| 보드 셀 크기 | `CELL_SIZE = 32` (픽셀) | `CELL_SIZE = 80` (가상 해상도 단위, 960/12=80) |
| 보드 크기 | 192×384 픽셀 | 480×960 가상 단위 |
| 메뉴 텍스트 | 고정 픽셀 좌표 | 가상 해상도 기준 상대 좌표 |

## 📦 빌드 및 배포

### Gradle 설정 요약
- **root/build.gradle**: 공통 의존성 버전 관리 (LibGDX 1.12.1)
- **core/build.gradle**: `java-library` 플러그인, JUnit 5 테스트
- **desktop/build.gradle**: `application` 플러그인, LWJGL3 네이티브 포함
- **android/build.gradle**: AGP 8.1, `com.android.application`, 네이티브 라이브러리 패키징 (`gdx-platform`에서 `.so` 추출)

### CI/CD 파이프라인 (`.github/workflows/android-build.yml`)
1. **JDK 17 + Android SDK** 설정 (공식 `android-actions/setup-android` 액션)
2. **헤드리스 테스트** - `./gradlew :core:test` (GL 없이 순수 로직 검증)
3. **디버그 APK 빌드** - `./gradlew :android:assembleDebug`
4. **네이티브 라이브러리 검증** - APK 내 `lib/arm64-v8a/libgdx.so`, `lib/armeabi-v7a/libgdx.so` 크기 확인 (0 byte가 아닌지)
5. **아티팩트 업로드** - APK + 테스트 리포트

## 🧪 테스트 전략

| 테스트 유형 | 대상 | 실행 환경 |
|------------|------|-----------|
| 단위 테스트 | `Board`, `GravityEngine`, `PuyoPairGenerator`, `MenuLoader` | JVM (헤드리스, GL 불필요) |
| 통합 테스트 | `GameWorld` 게임 루프, 연쇄 판정 | JVM (헤드리스) |
| UI 테스트 | 미구현 (예정: Robolectric/Espresso) | - |

## 📝 변경 이력
- 2026-07-31: Viewport/Camera 도입, 가상 해상도 960×1600 설정, 렌더링 아키텍처 리팩토링
- 2026-07-30: GitHub Actions에서 Android SDK 공식 액션으로 마이그레이션, 네이티브 라이브러리 패키징 수정
- 2026-07-29: Multi-module Gradle 구조 확립, CI 파이프라인 구축
