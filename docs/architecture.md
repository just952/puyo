# Puyo Puyo 2 - 기술 아키텍처 문서

## 기술 스택

| 영역                | 기술                 | 버전   | 비고                             |
| ------------------- | -------------------- | ------ | -------------------------------- |
| 언어                | Java                 | 17     | LTS, Android 타겟                |
| 게임 프레임워크     | LibGDX               | 1.12.1 | 크로스 플랫폼 (Desktop/Android)  |
| 빌드 시스템         | Gradle               | 8.4    | Kotlin DSL 미사용 (Groovy)       |
| 안드로이드 플러그인 | AGP                  | 8.1.0  | compileSdk 33, minSdk 21         |
| NDK                 | Android NDK          | r25c+  | 네이티브 라이브러리 빌드용       |
| 백엔드 (Desktop)    | LWJGL3               | 3.3.2  | LibGDX 기본                      |
| 백엔드 (Android)    | Android SDK          | 33     | LibGDX 기본                      |
| 테스트 (Headless)   | gdx-backend-headless | 1.12.1 | CI 전용                          |
| CI/CD               | GitHub Actions       | -      | ubuntu-latest 러너 (검증용 유지) |
| 버전 관리           | Git                  | -      | GitHub 호스팅                    |

---

## 프로젝트 구조 (멀티 모듈)

```
puyo/
├── build.gradle              # 루트 빌드 설정 (AGP 8.1.0, libGDX 1.12.1)
├── settings.gradle           # 모듈 포함: core, desktop, android
├── gradle.properties         # JVM 옵션, 버전 상수, org.gradle.java.home=JDK 17
├── core/                     # 공통 게임 로직 (Pure Java + LibGDX API)
│   ├── build.gradle          # 의존성: gdx, gdx-ai, gdx-freetype, gdx-platform:natives-desktop(test)
│   ├── src/main/java/com/puyo/game/
│   │   ├── PuyoGame.java                 # 메인 게임 클래스 (Game 인터페이스 구현)
│   │   ├── GameMode.java                 # 게임 모드 열거형 (NORMAL, ENDLESS, VERSUS, OPTION)
│   │   ├── config/
│   │   │   ├── ConfigManager.java        # 설정 관리 (JSON 기반)
│   │   │   └── GameViewport.java         # 가상 해상도 1600x960, FitViewport 팩토리
│   │   ├── graphics/
│   │   │   ├── FontManager.java          # FreeTypeFontGenerator 한글 폰트 관리
│   │   │   ├── PuyoConnectState.java     # 뿌요 연결 상태 enum (v0.1.23~)
│   │   │   └── PuyoRenderer.java         # SpriteBatch + 아틀라스 렌더러 (v0.1.22~)
│   │   ├── input/
│   │   │   ├── InputHandler.java         # 키보드/터치 통합 입력 처리
│   │   │   └── TouchController.java      # 모바일 4버튼 터치 컨트롤러
│   │   ├── logic/
│   │   │   ├── ai/
│   │   │   │   └── AIController.java     # AI 대전 컨트롤러 (휴리스틱)
│   │   │   ├── engine/
│   │   │   │   ├── GameWorld.java        # 게임 루프, 보드, 페어, 상태 머신 오케스트레이터 (GamePhase enum으로 전체 상태 관리)
│   │   │   │   ├── GravityEngine.java    # 중력 처리 엔진 (stateless, Board 파라미터)
│   │   │   │   ├── MatchFinder.java      # 매칭 그룹 탐색 (static 메서드)
│   │   │   │   ├── SeparationManager.java # 가로 쌍 분리 로직
│   │   │   │   ├── LockDelayManager.java  # 락 딜레이 타이머/이동 카운트
│   │   │   │   ├── ChainManager.java      # 연쇄 상태 관리 (chainCount, currentGroups)
│   │   │   │   ├── PuyoPairGenerator.java # 랜덤 PuyoPair 생성
│   │   │   │   └── StatefulPuyo.java     # 상태를 가진 뿌요 모델 (POPPING/FALLING/SETTLING, v0.1.27~)
│   │   │   └── model/
│   │   │       ├── Board.java            # 6x12 보드, 중력, 부유 뿌요 탐색
│   │   │       ├── Puyo.java             # 단일 뿌요 (위치, 색상, 생존, 팝 애니메이션, inMiddle 반칸 상태 v0.1.24~)
│   │   │       ├── PuyoColor.java        # 뿌요 색상 열거형
│   │   │       ├── PuyoPair.java         # 뿌요 쌍 (회전, 이동, 위치)
│   │   │       └── StageData.java        # 스테이지 데이터 (상대, 배경, 난이도)
│   │   ├── menus/
│   │   │   ├── MenuAction.java           # 메뉴 액션 열거형
│   │   │   ├── MenuItem.java             # 메뉴 아이템 데이터
│   │   │   ├── MenuLoader.java           # JSON 메뉴 로딩 (classpath/internal 폴백)
│   │   │   ├── MenuLoaderTest.java       # 메뉴 로더 테스트
│   │   │   └── MenuSystemDemo.java       # 메뉴 시스템 데모
│   │   ├── screens/
│   │   │   ├── BaseScreen.java           # 공통 스크린 베이스 (뷰포트, 카메라)
│   │   │   ├── LoadingScreen.java        # 로딩 화면
│   │   │   ├── MenuScreen.java           # 메인 메뉴 화면
│   │   │   ├── PlayScreen.java           # 게임플레이 화면
│   │   │   └── StoryModeSelectScreen.java # 스토리 모드 선택 화면
│   │   └── story/
│   │       ├── StageData.java            # 스테이지 데이터 모델
│   │       └── StoryModeManager.java     # 스토리 모드 JSON 로딩, 스테이지 관리
│   ├── src/main/resources/assets/        # 공통 에셋 (JAR의 assets/ 하위에 포함)
│   │   ├── config/
│   │   ├── data/
│   │   └── NotoSansKR-Regular.ttf
│   └── src/test/java/com/puyo/game/
│       └── GameTest.java                 # 게임 로직 단위 테스트
├── desktop/                  # 데스크톱 런처 (LWJGL3)
│   ├── build.gradle          # gdx-backend-lwjgl3, gdx-platform:natives-desktop
│   └── src/main/java/com/puyo/game/DesktopLauncher.java
├── android/                  # 안드로이드 앱
│   ├── build.gradle          # AGP 8.1, compileSdk 33, ndk.abiFilters [arm64-v8a, armeabi-v7a]
│   │   # mergeNativeLibs 후 libgdx-freetype.so → libpenguin.so 복사 + Python lief로 SONAME 패치
│   └── src/main/
│       ├── java/com/puyo/game/AndroidLauncher.java # AndroidApplication 구현
│       ├── AndroidManifest.xml # minSdk 21, targetSdk 33, 가로 고정
│       └── res/                # strings, colors, styles, drawable
├── .github/workflows/android-build.yml # CI/CD 파이프라인
├── docs/                     # 설계/진행 문서
└── patch_soname.py           # 빌드 시 SONAME 패치용 Python 스크립트 (lief 사용)
```

---

## 아키텍처 패턴

| 레이어            | 구성 요소                                                                   | 설명                                       |
| ----------------- | --------------------------------------------------------------------------- | ------------------------------------------ |
| **엔트리 포인트** | `PuyoGame` (core), `AndroidLauncher` (android), `DesktopLauncher` (desktop) | 플랫폼별 초기화                            |
| **게임 루프**     | `GameWorld`                                                                 | 업데이트/렌더링 분리, 고정 타임스텝(1/60s) |
| **상태 관리**     | `Screen` 기반 (LibGDX)                                                      | Loading → Menu → Play/StorySelect          |
| **렌더링**        | `FitViewport` + `OrthographicCamera`                                        | 가상 해상도 1600x960, 자동 스케일링        |
| **데이터**        | JSON + `Json` (LibGDX)                                                      | 메뉴, 스테이지 데이터 외부화               |
| **리소스**        | `AssetManager` (예정)                                                       | 텍스처/사운드/폰트 통합 관리               |

---

## 엔진 모듈 구조 (v0.1.24~)

| 클래스 | 책임 | 비고 |
|--------|------|------|
| `GameWorld` | **메인 상태 머신** (오케스트레이터) | 게임 루프, 보드, 페어, 연쇄 처리, 락 딜레이, 분리, 스폰, 팝/낙하 애니메이션 등 전체 상태 관리. **GamePhase enum 11단계로 세분화** (`FALLING_AUTO`, `LOCK_DELAY`, `SEPARATION` 추가) |
| `SeparationManager` | 가로 쌍 분리 로직 | `SeparationResult` 반환, Board 조작 안 함 |
| `GravityEngine` | 중력 적용 (stateless) | `applyGravity(Board)` 파라미터 전달 |
| `MatchFinder` | 매칭 그룹 탐색 | static 메서드만, stateless 유틸리티 |
| `LockDelayManager` | 락 딜레이 타이머/이동 카운트 | Tsu 규칙: 0.5초, 15회 이동 제한. stateful 클래스 (`activate`, `deactivate`, `resetTimerAndMoves`, `recordTime`, `recordMove`, `shouldLock`) |
| `ChainManager` | 연쇄 상태 관리 (chainCount, currentGroups) | `LockDelayManager` 패턴 적용. `startNewChain`, `findChains`, `getCurrentGroups`, `getChainCount`, `clearCurrentGroups`, `isChaining`, `isChainEnded` |
| `PuyoPairGenerator` | 랜덤 PuyoPair 생성 | 스폰 위치 설정 포함 |

**주요 변경 (v0.1.24)**: **반칸 단위 부드러운 낙하 구현** - `Puyo.inMiddle` 토글 방식으로 모든 낙하 경로(자동/소프트/하드/분리/부유) 자동 적용, 아키텍처 변경 최소화 (3파일만 수정)

**주요 변경 (v0.1.21)**: 
- `softDrop()` 착지 시 `SEPARATION` 페이즈 경유 (락딜레이 우회하되 분리 체크 수행)
- `InputHandler` DAS/ARR 단일 카운터(`heldFrames`, `repeatTriggered`, `anyPressed`)로 통합
- `ChainManager` 신규 생성으로 연쇄 상태 캡슐화
- `lockPiece()`에서 단계 전이 분리 → `startChainFinding()` 명시적 호출

**주요 변경 (v0.1.20)**: `GamePhase.FALLING`을 3단계로 분리 (`FALLING_AUTO`, `LOCK_DELAY`, `SEPARATION`). 입력 허용 페이즈 명시화 (`FALLING_AUTO` && `LOCK_DELAY`만 허용). `getGamePhase()`, `recordLockDelayMove()` 추가로 PlayScreen 입력 제어 중앙화. 자동 낙하 중 락딜레이 로직 완전 제거로 상태 관리 단순화.

**주요 변경 (v0.1.15)**: `FallingAnimationManager` 클래스 삭제 → 로직을 `GameWorld` 내부에 private 메서드로 통합 (`updatePopAnimation`, `updateSeparationFalling`, `updateFloatingFalling`, `collectAndClearChainPop`, `collectCompletedFalling`, `canFallInColumn`). `FallingAnimationManager.FallingPuyo` 중첩 클래스 → 별도 파일 `StatefulPuyo.java`로 단일화 (v0.1.27~ `StatefulPuyo`로 리네이밍). `float[]` 래퍼 패턴 제거로 타이머 버그(프리징) 해결. 단일 `GamePhase` enum으로 모든 Phase 처리.

---

## GamePhase 상태 머신 상세 (v0.1.20~)

```java
public enum GamePhase {
    SPAWNING,           // 새 조각 생성/위치 설정
    FALLING_AUTO,       // 자동 낙하 (0.5초 간격, 이동/회전/하드드롭 입력 허용)
    LOCK_DELAY,         // 락 딜레이 (착지 후 0.5초/15회 이동, 이동/회전/소프트드롭 입력 허용)
    SEPARATION,         // 락딜레이 종료 후 분리 체크 + 실행 (입력 차단)
    FALLING_ANIMATION,  // 분리/부유 뿌요 낙하 애니메이션 (입력 차단)
    CHAIN_FINDING,      // 연쇄: 매치 탐색
    CHAIN_POP_ANIMATION, // 연쇄: 팝 애니메이션 재생 중
    CHAIN_FLOATING_CHECK, // 연쇄: 부유 뿌요 체크 후 낙하 준비
    GAME_OVER           // 게임 오버
}
```

### 상태 전이 플로우

```
SPAWNING 
    → FALLING_AUTO (자동 낙하 시작)
        → LOCK_DELAY (착지 시 activate())
            → SEPARATION (타이머/이동수 초과 시 shouldLock())
                → FALLING_ANIMATION (분리 가능 시)
                → CHAIN_FINDING (분리 불가 시 lockPiece())
            → FALLING_AUTO (공중 이탈 시 deactivate() + 복귀)
        → FALLING_AUTO (이동/회전으로 공중 이탈 시 deactivate() + 복귀)
    → FALLING_ANIMATION (분리/연쇄 낙하 중)
        → CHAIN_FINDING (낙하 완료 시)
    → CHAIN_FINDING → CHAIN_POP_ANIMATION → CHAIN_FLOATING_CHECK → (매치 시) CHAIN_FINDING / (매치 없음) SPAWNING
    → GAME_OVER
```

### 입력 허용 매트릭스

| 입력 액션 | FALLING_AUTO | LOCK_DELAY | SEPARATION | FALLING_ANIMATION | 기타 |
|-----------|--------------|------------|------------|-------------------|------|
| 좌/우 이동 | ✅ | ✅ | ❌ | ❌ | ❌ |
| 회전 | ✅ | ✅ | ❌ | ❌ | ❌ |
| 하드 드롭 | ✅ | ✅ | ❌ | ❌ | ❌ |
| 소프트 드롭 | ✅ | ✅ (recordLockDelayMove) | ❌ | ❌ | ❌ |

---

## 반칸 단위 부드러운 낙하 아키텍처 (v0.1.24~)

### 핵심 설계: Puyo 모델 중심의 상태 캡슐화

```java
// core/src/main/java/com/puyo/game/logic/model/Puyo.java
public class Puyo {
    private boolean inMiddle = false;  // 반칸 상태 플래그
    
    /**
     * 아래로 이동 (분리 낙하용)
     * 정수칸 ↔ 반칸 자동 토글
     */
    public void moveDown() {
        if (inMiddle) this.y--;  // 반칸 상태에서만 실제 y 감소
        inMiddle = !inMiddle;    // 매 호출마다 토글
    }
    
    public boolean getInMiddle() {
        return inMiddle;
    }
}
```

### 작동 원리

```
스폰(y=12, inMiddle=false) - 정수칸
  ↓ moveDown() 1회 호출
y=12, inMiddle=true  - 시각적 위치 11.5 (반칸 아래)
  ↓ moveDown() 2회 호출  
y=11, inMiddle=false - 시각적 위치 11.0 (정수칸 착지)
  ↓ ... 반복
```

- **짝수 번 `moveDown()` 호출 후 항상 `inMiddle=false`로 정수칸 착지 보장**
- 스폰 위치(y=12)에서 바닥(y=0)까지 12칸 = 24회 호출 → `inMiddle=false`
- 어떤 정수 높이에서 착지해도 이동 횟수는 항상 짝수 → **`inMiddle=false` 확정**

### 자동 적용되는 낙하 경로 (GameWorld 무수정)

| 낙하 경로 | 메서드 | moveDown() 호출 | 비고 |
|-----------|--------|-----------------|------|
| 기본 자동 낙하 | `handleFallingAuto()` | `currentPair.moveDown()` | 0.5초마다 1회 |
| 소프트 드롭 | `softDrop()` | `currentPair.moveDown()` | 키 홀드 시 매 프레임 |
| 하드 드롭 | `hardDrop()` | `while(canFall()) moveDown()` | 루프로 즉시 바닥까지 |
| 분리/부유 낙하 | `updateFallingAnimation()` | `fp.puyo.moveDown()` | 0.05초마다 열 단위 |

### 충돌 체크 확장 (Board.java)

```java
// core/src/main/java/com/puyo/game/logic/model/Board.java
public boolean isEmpty(int x, int y, boolean checkBelow) {
    if (checkBelow) y--;  // 반칸 상태면 아래칸 기준
    return isInside(x, y) && getPuyoAt(x, y) == null;
}

// 좌우 이동 시
public boolean canMoveLeft(PuyoPair pair) {
    for (Puyo p : pair.getPuyos()) {
        if (!isEmpty(p.getX() - 1, p.getY(), p.getInMiddle())) {
            return false;
        }
    }
    return true;
}

// 회전 시 (벽킥 포함)
public boolean canPlace(PuyoPair pair) {
    for (Puyo p : pair.getPuyos()) {
        if (!isEmpty(p.getX(), p.getY(), p.getInMiddle())) {
            return false;
        }
    }
    return true;
}
```

- **반칸 상태(`inMiddle=true`) 시 아래칸(y-1) 기준 옆면/회전 충돌 체크**
- 시각적으로 반칸 걸쳐 있을 때 실제 장애물은 한 칸 아래에 있음
- 정수칸 상태(`inMiddle=false`) 시 기존 로직 그대로 적용

### 렌더링 오프셋 (PlayScreen.java)

```java
// core/src/main/java/com/puyo/game/screens/PlayScreen.java
private void drawPuyo(Puyo puyo, float x, float y) {
    float scale = puyo.getPopScale();
    if (scale <= 0) return;
    
    // 반칸 상태 시 절반 셀 크기만큼 아래로 오프셋
    float offsetY = puyo.getInMiddle() ? GameViewport.CELL_SIZE / 2 : 0;
    puyoRenderer.draw(batch, puyo.getColor(), x, y - offsetY, GameViewport.CELL_SIZE, scale);
}
```

- **렌더링만 반칸 아래로 그림, 로직 좌표는 정수 유지**
- 보드에 고정된 뿌요들은 `inMiddle=false`이므로 영향 없음
- `drawFallingPuyos()`도 동일 로직 적용 (분리/부유 낙하 포함)

### 효과 및 장점

| 측면 | 효과 |
|------|------|
| **시각적 품질** | 원작 같은 부드러운 낙하 애니메이션 (1초/1칸) |
| **조작감** | 반칸 상태에서 좌우/회전 시 자연스러운 충돌 판정 |
| **아키텍처** | GameWorld/상태머신/매니저 클래스 **무수정** |
| **코드 변경** | **단 3파일**(Puyo, Board, PlayScreen) 최소 수정 |
| **확장성** | 분리/부유/연쇄 낙하 모두 자동 적용 |
| **안전성** | 착지 시점 `inMiddle=false` 보장으로 락딜레이/고정 로직 안전 |

---

## 렌더링 아키텍처 (v0.2.0~)

### 텍스처 아틀라스 시스템 (v0.1.22~)

```java
// core/src/main/java/com/puyo/game/graphics/PuyoRenderer.java
public class PuyoRenderer implements Disposable {
    // 7색 × 7변형 = 49개 스프라이트를 단일 아틀라스에 통합 (v0.1.23~)
    // PuyoColor: RED, GREEN, BLUE, YELLOW, PURPLE, OJAMA, HARD
    // Variant (프로그래머 모드): 기본, 하이라이트링, 팝용, 오버레이_상/하/좌/우 (7개)
    // 연결 상태 (디자이너 모드): 16가지 (NONE + 15가지 방향 조합)
    
    // 하이브리드 모드 지원
    // - 디자이너 모드: 아틀라스에 15가지 완성형 이미지 존재 시 자동 감지 (red_up, red_down, red_up_down 등)
    // - 프로그래머 모드: 기본 뿌요 + 방향별 오버레이 4개 런타임 합성
    
    // 환경별 로드 전략
    // - Production (PRD/Android): classpath만 (core/resources/assets/)
    // - Development (Desktop): local 우선 → classpath → 생성
}
```

**아틀라스 파일 구조:**
```
core/src/main/resources/assets/
├── puyo_atlas.png      # 464×464, 7색 × 7변형 (64×64 each, 2px padding) v0.1.23~
└── puyo_atlas.atlas    # libGDX TextureAtlas 포맷 메타데이터
```

**장점:**
- 드로우콜 감소: 수십 개 뿌요 → 1회 SpriteBatch로 배치 처리
- 메모리 효율: 단일 텍스처 바인딩
- 확장성: 아티스트 교체 시 PNG만 교체하면 됨
- 표준 포맷: TexturePacker로 생성 가능
- **하이브리드 지원**: 프로그래머 아트 → 디자이너 에셋 코드 수정 없이 교체 가능

### 가상 해상도 시스템 (가로 고정 1600×960)

```java
// core/src/main/java/com/puyo/game/config/GameViewport.java
public class GameViewport {
    // === 공통 ===
    public static final float VIRTUAL_WIDTH = 1600f;
    public static final float VIRTUAL_HEIGHT = 960f;
    public static final float CELL_SIZE = 80f;
    public static final int BOARD_COLS = 6;
    public static final int BOARD_ROWS = 12;
    public static final float BOARD_WIDTH = BOARD_COLS * CELL_SIZE;   // 480
    public static final float BOARD_HEIGHT = BOARD_ROWS * CELL_SIZE;  // 960

    // === 싱글 플레이 (보드 왼쪽, 사이드 패널 오른쪽) ===
    public static final class Single {
        public static final float BOARD_OFFSET_X = 80f;
        public static final float BOARD_OFFSET_Y = 0f;
        public static final float SIDE_PANEL_X = BOARD_OFFSET_X + BOARD_WIDTH + 40f; // 600
        public static final float SIDE_PANEL_WIDTH = VIRTUAL_WIDTH - SIDE_PANEL_X - 80f; // ~920
    }

    // === 대전 모드 (두 보드 나란히 + 중앙 UI) ===
    public static final class Versus {
        public static final float CENTER_UI_WIDTH = 200f;
        public static final float TOTAL_BOARDS_WIDTH = BOARD_WIDTH * 2 + CENTER_UI_WIDTH; // 1160
        public static final float SIDE_MARGIN = (VIRTUAL_WIDTH - TOTAL_BOARDS_WIDTH) / 2f; // 220

        public static final float P1_BOARD_OFFSET_X = SIDE_MARGIN; // 220
        public static final float P1_BOARD_OFFSET_Y = 0f;

        public static final float P2_BOARD_OFFSET_X = SIDE_MARGIN + BOARD_WIDTH + CENTER_UI_WIDTH; // 900
        public static final float P2_BOARD_OFFSET_Y = 0f;

        public static final float CENTER_UI_OFFSET_X = SIDE_MARGIN + BOARD_WIDTH; // 700
        public static final float CENTER_UI_OFFSET_Y = 0f;
    }

    // === 메뉴/UI (전체 화면 활용) ===
    public static final class Menu {
        public static final float CONTENT_WIDTH = 1200f;
        public static final float CONTENT_HEIGHT = 720f;
        public static final float CONTENT_OFFSET_X = (VIRTUAL_WIDTH - CONTENT_WIDTH) / 2f; // 200
        public static final float CONTENT_OFFSET_Y = (VIRTUAL_HEIGHT - CONTENT_HEIGHT) / 2f; // 120
    }

    public static FitViewport createViewport() {
        OrthographicCamera camera = new OrthographicCamera();
        camera.setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        return new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
    }
}
```

### 입력 아키텍처 (v0.2.0~)

```java
// core/src/main/java/com/puyo/game/input/InputHandler.java
public class InputHandler {
    // PC 키보드 / 모바일 터치 통합 인터페이스
    // - isMobile 플래그로 분기
    // - 공통 조회 메서드: getMoveDirection(), isRotatePressed(), isDropPressed(), isHardDropPressed()
    // - DAS/ARR: 단일 카운터(heldFrames, repeatTriggered, anyPressed)로 통합
}

// core/src/main/java/com/puyo/game/input/TouchController.java (모바일 전용)
public class TouchController implements InputProcessor, Disposable {
    // 4버튼 레이아웃: 좌/우 이동, 회전, 드롭(더블탭=하드드롭)
    // 정규화 좌표(0~1) 기반 해상도 독립적 터치 영역
    // 시각적 피드백: 누름 상태 시 알파/크기 변경
}
```

| 플랫폼     | 입력 방식                          | 구현 클래스                        |
| ---------- | ---------------------------------- | ---------------------------------- |
| **PC**     | 키보드 (WASD/방향키 + Space/Enter) | `InputHandler` (키보드 분기)       |
| **모바일** | 터치 오버레이 (4버튼)              | `InputHandler` + `TouchController` |

---

## 안드로이드 모듈 상세 (v0.2.0~)

### AndroidManifest.xml - 가로 고정

```xml
<activity
    android:name=".AndroidLauncher"
    android:screenOrientation="landscape"
    android:configChanges="orientation|screenSize|keyboardHidden">
</activity>
```

### AndroidLauncher.java - 몰입 모드

```java
// 풀스크린 몰입 모드 (네비게이션 바/상태 바 숨김)
getWindow().getDecorView().setSystemUiVisibility(
    View.SYSTEM_UI_FLAG_FULLSCREEN
    | View.SYSTEM_UI_FLAG_HIDE_NEVIGATION
    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
);
```

### 네이티브 라이브러리 패키징 (기존 유지)

- `libgdx.so`, `libpenguin.so` (SONAME 패치됨) → APK `lib/arm64-v8a/`, `lib/armeabi-v7a/`
- `mergeNativeLibs` 후 Python `lief`로 `libpenguin.so` SONAME 패치

---

## 데스크톱 런처 (v0.2.0~)

```java
// desktop/src/main/java/com/puyo/game/DesktopLauncher.java
Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
config.setWindowedMode(1600, 960);  // 가로 고정
config.setResizable(true);  // 비율 유지 리사이즈
config.setForegroundFPS(60);
```

### 적용된 화면들

| 화면                    | 뷰포트 적용 | 렌더링 방식                           |
| ----------------------- | ----------- | ------------------------------------- |
| `LoadingScreen`         | ✅          | ShapeRenderer + 카메라 프로젝션       |
| `MenuScreen`            | ✅          | ShapeRenderer + 중앙 정렬 텍스트      |
| `StoryModeSelectScreen` | ✅          | ShapeRenderer + 그리드 레이아웃       |
| `PlayScreen`            | ✅          | SpriteBatch + 아틀라스 + 반칸 오프셋  |

---

## 네이티브 라이브러리 처리 (중요: 현재 미해결)

### 문제 현황

| 항목                 | 상태               | 비고                                               |
| -------------------- | ------------------ | -------------------------------------------------- |
| `libgdx.so`          | APK 포함됨         | arm64-v8a: 163KB, armeabi-v7a: 160KB               |
| `libgdx-freetype.so` | **이름 변경 시도** | → `libpenguin.so`로 이름 변경                      |
| `libpenguin.so`      | APK 포함됨         | arm64-v8a: 797KB, armeabi-v7a: 757KB               |
| **실기기 로드**      | **실패**           | `dlopen failed: library "libpenguin.so" not found` |

### 시도한 해결 (모두 실패)

```gradle
// android/build.gradle - 이름 변경 로직 (커밋 0961e9c)
tasks.register('renameFreetypeToPenguin', Copy) {
    from libDir
    include "**/libgdx-freetype.so"
    into libDir
    rename "libgdx-freetype.so", "libpenguin.so"
    // armeabi-v7a, arm64-v8a 모두 처리
}
```

```java
// AndroidLauncher.java - 단일 로드
static {
    System.loadLibrary("gdx");
    System.loadLibrary("penguin");  // gdx-freetype 제거, penguin만
}
```

```bash
# GitHub Actions에서 SONAME 패치 시도
patchelf --set-soname libpenguin.so lib/arm64-v8a/libpenguin.so
patchelf --set-soname libpenguin.so lib/armeabi-v7a/libpenguin.so
# APK 재패키징 + 디버그 키 재서명
```

### 실패 원인 추정

1. **gdx-freetype 네이티브 코드 내부**에서 `dlopen("libpenguin.so")` 호출 시 **이미 로드된 라이브러리를 찾지 못함**
2. `android:extractNativeLibs="true"` (기본값)이나 **압축 해제 경로/권한 문제** 가능성
3. **GitHub Actions 러너의 NDK/SDK 버전** 차이로 인한 네이티브 심볼/의존성 불일치
4. **Termux 환경에서 `aapt2`, `lldb`, `readelf` 등 디버깅 도구 부재**로 정밀 분석 불가

### 결정: PC 로컬 개발 환경으로 이전

```
# PC에서 필요한 환경
- JDK 17 (Temurin/OpenJDK)
- Android SDK: cmdline-tools, platform-tools, platforms;android-33, build-tools;33.0.2
- NDK: r25c+ (Android Studio SDK Manager에서 설치)
- Android Studio (선택) 또는 IntelliJ IDEA + Android 플러그인
- adb + lldb (NDK에 포함) for 실기기 디버깅
```

### PC에서 수행할 디버깅

```bash
# 1. 로컬 빌드
./gradlew :android:assembleDebug

# 2. 실기기 설치 및 로그 확인
adb install -r android/build/outputs/apk/debug/android-debug.apk
adb logcat -s "com.puyo.game" "AndroidRuntime" "DEBUG"

# 3. 네이티브 라이브러리 로드 경로 확인
adb shell "ls -la /data/app/com.puyo.game-*/lib/arm64/"
adb shell "cat /proc/$(pidof com.puyo.game)/maps | grep penguin"

# 4. lldb로 네이티브 크래시 분석 (필요시)
adb shell lldb --attach $(pidof com.puyo.game)
```

---

## 빌드/배포 파이프라인

### GitHub Actions (현재: CI 검증용 유지)

```yaml
# .github/workflows/android-build.yml
jobs:
  test-and-build:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v4
      - name: Setup JDK 17
      - name: Setup Android SDK
      - name: Run Headless Tests
        run: ./gradlew :core:test
      - name: Build Debug APK
        run: ./gradlew :android:assembleDebug
      - name: Upload APK Artifact
        uses: actions/upload-artifact@v4
```

### PC 로컬 환경 (신규: 주 개발/디버깅 환경)

```bash
# 1. 환경 설정 (최초 1회)
# - JDK 17 설치
# - Android Studio 설치 → SDK Manager에서 SDK 33, NDK r25c+ 설치
# - ANDROID_HOME, ANDROID_SDK_ROOT 환경변수 설정

# 2. 프로젝트 클론 및 동기화
git clone https://github.com/just952/puyo.git
cd puyo

# 3. 로컬 빌드/테스트/실행
./gradlew :core:test              # 헤드리스 테스트 (natives-desktop 자동 다운로드)
./gradlew :android:assembleDebug  # APK 빌드 (aapt2 로컬 작동)
adb install -r android/build/outputs/apk/debug/android-debug.apk

# 4. 디버깅
# - Android Studio / IntelliJ: 브레이크포인트, 변수 검사, 스택 트레이스
# - adb logcat: 실시간 로그
# - lldb: 네이티브 크래시 분석
```

---

## 테스트 전략

| 테스트 유형          | 도구                      | 실행 환경                | 대상                                         |
| -------------------- | ------------------------- | ------------------------ | -------------------------------------------- |
| 단위 테스트 (로직)   | JUnit 5 + LibGDX Headless | GitHub Actions / PC 로컬 | GameWorld, Board, PuyoPair, StoryModeManager |
| 통합 테스트 (메뉴)   | JUnit 5 + LibGDX Headless | GitHub Actions / PC 로컬 | MenuLoader, MenuScreen 네비게이션            |
| UI 테스트            | Manual / Espresso (예정)  | 실기기 / 에뮬레이터      | 화면 전환, 입력 처리                         |
| 네이티브 로드 테스트 | adb + logcat + lldb       | **PC 로컬 + 실기기**     | libgdx.so, libpenguin.so 로드 검증           |

### 헤드리스 테스트 구성 (core/build.gradle)

```groovy
testImplementation "com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion"
testImplementation "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop"
```

---

## 알려진 제약사항

| 제약사항                                   | 영향도                  | 대응 방안                                            |
| ------------------------------------------ | ----------------------- | ---------------------------------------------------- |
| **Termux에서 aapt2 미작동**                | 로컬 APK 빌드 불가      | **PC 이전으로 해결**                                 |
| **Termux에서 네이티브 lib 미포함**         | 로컬 단위 테스트 불가   | **PC 이전으로 해결 (natives-desktop 자동 다운로드)** |
| **GitHub Actions에서 실기기 테스트 불가**  | 네이티브 로드 검증 불가 | **PC + 실기기 adb 연결로 해결**                      |
| **libgdx-freetype 네이티브 의존성 불확실** | libpenguin.so 로드 실패 | **PC에서 lldb로 심볼/의존성 분석 후 해결**           |
| **CI 러너 NDK 버전 고정 어려움**           | 재현성 저하             | **PC 로컬에서 NDK 버전 고정하여 빌드**               |

---

## 향후 아키텍처 개선 계획

| 영역          | 계획                                            | 우선순위 |
| ------------- | ----------------------------------------------- | -------- |
| 리소스 관리   | `AssetManager` 도입 + 텍스처 아틀라스           | P1       |
| 네이티브 빌드 | 프리빌트 `.so` 대신 소스에서 NDK 빌드 (CMake)   | P2       |
| 의존성 주입   | 수동 팩토리 → Dagger/Hilt 또는 수동 DI 컨테이너 | P3       |
| 상태 관리     | Screen 기반 → 상태 머신/이벤트 버스             | P3       |
| 네트워크      | WebSocket + Protobuf (온라인 대전)              | P2       |

---

> **핵심**: 현재 네이티브 라이브러리 로드 실패는 **GitHub Actions/Termux 환경의 한계**입니다. **PC 로컬 환경에서 adb + lldb + Android Studio로 정밀 디버깅** 후 해결하는 것이 가장 빠른 경로입니다.

---

## 핵심 게임 루프 및 렌더링 파이프라인 (v0.2.0~)

### 1. 진입점 및 메인 루프

```
PuyoGame (extends Game)
├── create() → LoadingScreen 설정
├── render() → super.render() 호출 → 현재 Screen의 render() 실행
└── dispose() → 리소스 정리
```

**PuyoGame.render()**:

```java
@Override
public void render() {
    if (Gdx.gl != null) {
        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
    }
    super.render();  // 현재 Screen의 render() 호출
}
```

### 2. Screen 전환 및 더블 버퍼링

- **Screen 전환**: `game.setScreen(new PlayScreen(...))` 호출 시
  1. 현재 Screen `hide()` → `dispose()`
  2. 새 Screen `show()` → `initViewport()` 등 초기화
  3. 다음 `render()`부터 새 Screen의 `render()` 호출

- **더블 버퍼링**: LibGDX가 자동 처리
  1. `Gdx.gl.glClear()` → 백버퍼 클리어
  2. `batch.setProjectionMatrix(camera.combined)` → 카메라 행렬 적용
  3. 그리기 명령들이 백버퍼에 기록
  4. 프레임 끝 → LibGDX가 백버퍼→프론트버퍼 스왑

### 3. 매 프레임 반복 (PlayScreen → GameWorld)

```
PlayScreen.render(delta)
├── update(delta)          // 입력 처리 + 게임 로직
│   ├── InputHandler 처리
│   │   ├── 현재 Phase 조회: gameWorld.getGamePhase()
│   │   ├── allowInput = (FALLING_AUTO || LOCK_DELAY)
│   │   ├── isRotatePressed() → gameWorld.rotateClockwise()
│   │   ├── getMoveDirection() → gameWorld.moveLeft/Right()
│   │   ├── isDropPressed() → currentPair.moveDown() + recordLockDelayMove()
│   │   └── isHardDropPressed() → gameWorld.hardDrop()
│   └── gameWorld.update(delta)  // 핵심 게임 로직
│
└── render()               // 렌더링
    ├── drawBoard()        // 고정된 뿌요들
    ├── drawCurrentPair()  // 현재 떨어지는 쌍 (반칸 오프셋 적용)
    ├── drawFallingPuyos() // 분리/부유 뿌요 (반칸 오프셋 적용)
    ├── drawNextPair()     // 다음 뿌요 프리뷰
    └── drawUI()           // 점수, 연쇄, 스테이지 등
```

### 4. GameWorld.update(delta) - 핵심 게임 루프 (v0.1.20~)

```java
public void update(float delta) {
    if (gameOver) return;

    // 1. 통합된 낙하/팝 처리 (분리/연쇄 모두) - 최우선, 조작 불가
    if (!fallingPuyos.isEmpty()) {
        updateFallingAnimation(delta);
        return;
    }

    // 2. 단일 switch로 모든 상태 처리
    switch (gamePhase) {
        case SPAWNING:
            handleSpawning();
            break;
        case FALLING_AUTO:
            handleFallingAuto(delta);
            break;
        case LOCK_DELAY:
            handleLockDelay(delta);
            break;
        case SEPARATION:
            handleSeparation();
            break;
        case FALLING_ANIMATION:
            handleFallingAnimation(delta);
            break;
        case CHAIN_FINDING:
            handleChainFinding();
            break;
        case CHAIN_POP_ANIMATION:
            handlePopAnimation(delta);
            break;
        case CHAIN_FLOATING_CHECK:
            handleFloatingCheck();
            break;
    }
}
```

### handleFallingAuto (자동 낙하만 담당)

```java
private void handleFallingAuto(float delta) {
    fallTimer += delta;
    if (fallTimer >= fallInterval) {
        fallTimer = 0f;
        if (canFall()) {
            currentPair.moveDown();  // → Puyo.moveDown() 경유로 반칸 토글 자동 적용
            // 자동 낙하 중에는 락딜레이 건드리지 않음 (공중이니까)
        } else {
            // 착지! → 락딜레이 활성화하고 LOCK_DELAY로 전이
            lockDelayManager.activate();
            gamePhase = GamePhase.LOCK_DELAY;
            LogUtil.debug("GameWorld", "Phase: FALLING_AUTO -> LOCK_DELAY (landed, lock delay activated)");
        }
    }
}
```

### handleLockDelay (락 딜레이 단계 전담)

```java
private void handleLockDelay(float delta) {
    lockDelayManager.recordTime(delta);

    if (lockDelayManager.shouldLock()) {
        LogUtil.debug("GameWorld", "LockDelay expired -> SEPARATION");
        gamePhase = GamePhase.SEPARATION;
        return;
    }

    // 공중 이탈 시 락딜레이 해제 → 자동 낙하로
    if (canFall()) {
        lockDelayManager.deactivate();
        gamePhase = GamePhase.FALLING_AUTO;
        LogUtil.debug("GameWorld", "Phase: LOCK_DELAY -> FALLING_AUTO (back in air)");
    }
    // 사용자 입력(moveLeft/Right/rotate/softDrop) 시 recordMove() 호출됨
}
```

### handleSeparation (분리 체크 + 실행)

```java
private void handleSeparation() {
    if (currentPair != null && separationManager.canSeparate(currentPair, board)) {
        // 분리 가능: 실행
        SeparationManager.SeparationResult sepResult = separationManager.separate(currentPair, board);
        if (sepResult.separated) {
            board.placePuyo(sepResult.blockedPuyo);
            addStatefulPuyo(sepResult.freePuyo, StatefulPuyo.StateType.FALLING);
            fallingAnimationTimer = 0f;
            lockDelayManager.deactivate();
            currentPair = null;
            gamePhase = GamePhase.FALLING_ANIMATION;
            LogUtil.debug("GameWorld", "Phase: SEPARATION -> FALLING_ANIMATION (separated)");
        } else {
            LogUtil.info("GameWorld", "SEPARATION: canSeparate true but separate() failed -> lockPiece");
            lockPiece();
            startChainFinding();
        }
    } else {
        // 분리 불가: 일반 잠금 → 연쇄 탐색
        LogUtil.debug("GameWorld", "SEPARATION: no separation -> lockPiece -> CHAIN_FINDING");
        lockPiece();
        startChainFinding();
    }
}
```

### 5. 입력 처리 (InputHandler / TouchController)

```java
// 키보드 (PC) - InputHandler
keyDown/keyUp → 상태 플래그 설정
update() → 이전 프레임 상태 저장 (엣지 감지용)
updateDasArr() → 단일 카운터로 DAS/ARR 처리 (anyPressed 기준)

// 터치 (모바일) - TouchController implements InputProcessor
touchDown/Up/Dragged → 버튼 영역별 플래그 설정
정규화 좌표(0~1) 사용으로 해상도 독립적

// 공통 조회 메서드 (InputHandler)
getMoveDirection()  // -1, 0, 1 (repeatTriggered + left/rightPressed)
isRotatePressed()   // 엣지 감지 (한 번만 true)
isDropPressed()     // 홀드 감지 (repeatTriggered + dropPressed)
isHardDropPressed() // 엣지 감지
```

### 6. 화면 렌더링 파이프라인 (PlayScreen.render)

```java
@Override
public void render(float delta) {
    // 1. Update game logic
    update(delta);

    // 2. Clear screen (백버퍼 클리어)
    Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

    // 3. 카메라/프로젝션 매트릭스 업데이트
    camera.update();
    batch.setProjectionMatrix(camera.combined);
    shapeRenderer.setProjectionMatrix(camera.combined);

    // 4. 그리기 (백버퍼에 기록)
    drawBoard();              // 고정된 뿌요들
    drawCurrentPair();        // 현재 떨어지는 쌍 (반칸 오프셋)
    drawFallingPuyos();       // 분리/부유 뿌요 (반칸 오프셋)
    drawNextPair();           // 다음 뿌요 프리뷰
    drawUI();                 // 점수, 연쇄, 스테이지 등

    // 5. 프레임 끝 → LibGDX가 백버퍼→프론트버퍼 스왑
}
```

### 7. Screen 전환 흐름 (메뉴 → 게임)

```java
// MenuScreen에서
if (inputHandler.isEnterPressed()) {
    game.setScreen(new PlayScreen(game, GameMode.NORMAL));
}

// PuyoGame.setScreen() 호출 시
// 1. 현재 Screen.hide() → dispose()
// 2. 새 Screen.show() → initViewport() 등 초기화
// 3. 다음 render()부터 새 Screen의 render() 호출
```

---

## 핵심 설계 포인트 요약

| 구분            | 내용                                                                         |
| --------------- | ---------------------------------------------------------------------------- |
| **게임 루프**   | `PuyoGame.render()` → `Screen.render()` → `update()` + `render()`            |
| **타임스텝**    | 고정 아님 (delta 누적), `fallInterval=0.5s`로 낙하 제어 (반칸 토글로 1초/1칸) |
| **입력 처리**   | `InputHandler`가 키보드/터치 통합, `update()`에서 엣지 감지, **페이즈 기반 허용 제어** |
| **상태 관리**   | `GameWorld`가 보드, 현재/다음 쌍, 점수, 연쇄 등 전체 상태 보유, **GamePhase 11단계** |
| **분리 로직**   | 가로 쌍(rotation 1,3)에서 한쪽만 막히면 분리 → 단일 뿌요 자동 낙하 (0.05s)   |
| **락 딜레이**   | 바닥에 닿으면 0.5초/15회 이동 제한 후 강제 잠금 (Tsu 규칙), **stateful Manager** |
| **연쇄 처리**   | `lockPiece()` → 매칭 찾기 → 제거 → 중력 적용 → 반복                          |
| **반칸 낙하**   | `Puyo.inMiddle` 토글로 모든 낙하 경로 자동 부드러운 이동 (v0.1.24~)          |
| **렌더링**      | `SpriteBatch` + 아틀라스, `FitViewport`로 가상 해상도(1600×960) 유지         |
| **더블 버퍼링** | LibGDX 자동 처리 (`glClear` → 그리기 → 스왑)                                 |
| **Screen 전환** | `Game.setScreen()` → hide/dispose → show/initViewport → 다음 프레임부터 적용 |