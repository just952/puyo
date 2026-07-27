# Puyo Puyo 2 - 기술 스택 및 아키텍처

## 🏗️ 프로젝트 구조 (Multi-module Gradle)

```
puyo/
├── build.gradle                 # 루트 빌드 설정 (AGP 8.1, Kotlin 1.8.0)
├── settings.gradle              # 모듈 포함: core, desktop, android
├── gradle/wrapper/              # Gradle Wrapper 8.4
├── core/                        # 공통 게임 로직 (Java Library)
│   ├── src/main/java/com/puyo/game/
    │   ├── PuyoGame.java              # 메인 게임 클래스 (Game 상속)
    │   ├── config/ConfigManager.java  # 설정 관리 (JSON)
    │   ├── logic/
    │   │   ├── model/                 # 도메인 모델
    │   │   │   ├── Puyo.java
    │   │   │   ├── PuyoColor.java
    │   │   │   ├── PuyoPair.java
    │   │   │   ├── Board.java
    │   │   │   ├── StageData.java
    │   │   │   └── MatchResult.java
    │   │   ├── engine/
    │   │   │   ├── GameWorld.java      # 메인 게임 루프/로직
    │   │   │   ├── Board.java          # 보드 상태 관리
    │   │   │   ├── PuyoPairGenerator.java
    │   │   │   ├── GravityEngine.java
    │   │   │   └── AIController.java  (예정)
    │   ├── menus/
    │   │   ├── MenuLoader.java       # JSON 메뉴 로딩
    │   │   ├── MenuItem.java
    │   │   └── MenuAction.java
    │   ├── screens/
    │   │   ├── BaseScreen.java
    │   │   ├── LoadingScreen.java
    │   │   ├── MenuScreen.java
    │   │   ├── PlayScreen.java
    │   │   └── StoryModeSelectScreen.java
    │   └── story/
    │       ├── StoryModeManager.java
    │       ├── StageData.java
    │       └── StageDataWrapper.java
    └── src/test/
        ├── resources/data/menus/      # 테스트용 메뉴 JSON
        └── src/test/java/.../GameTest.java  # 헤드리스 테스트
├── desktop/                         # 데스크톱 실행 모듈 (LWJGL3)
│   ├── src/main/java/com/puyo/game/DesktopLauncher.java
│   └── build.gradle (application 플러그인)
├── android/                         # 안드로이드 앱 모듈
    ├── src/main/
    │   ├── AndroidManifest.xml
    │   ├── java/com/puyo/game/AndroidLauncher.java
    │   └── res/values/{strings.xml, colors.xml, styles.xml}
    └── build.gradle (AGP 8.1, compileSdk 33)
└── .github/workflows/android-build.yml  # CI/CD 파이프라인
```