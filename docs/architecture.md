# Puyo Puyo 2 - 기술 아키텍처 문서

## 기술 스택
| 영역 | 기술 | 버전 | 비고 |
|------|------|------|------|
| 언어 | Java | 17 | LTS, Android 타겟 |
| 게임 프레임워크 | LibGDX | 1.12.1 | 크로스 플랫폼 (Desktop/Android) |
| 빌드 시스템 | Gradle | 8.4 | Kotlin DSL 미사용 (Groovy) |
| 안드로이드 플러그인 | AGP | 8.1.0 | compileSdk 33, minSdk 21 |
| NDK | Android NDK | r25c+ | 네이티브 라이브러리 빌드용 |
| 백엔드 (Desktop) | LWJGL3 | 3.3.2 | LibGDX 기본 |
| 백엔드 (Android) | Android SDK | 33 | LibGDX 기본 |
| 테스트 (Headless) | gdx-backend-headless | 1.12.1 | CI 전용 |
| CI/CD | GitHub Actions | - | ubuntu-latest 러너 (검증용 유지) |
| 버전 관리 | Git | - | GitHub 호스팅 |

---

## 프로젝트 구조 (멀티 모듈)

```
puyo/
├── build.gradle              # 루트 빌드 설정 (AGP 8.1.0, libGDX 1.12.1)
├── settings.gradle           # 모듈 포함: core, desktop, android
├── gradle.properties         # JVM 옵션, 버전 상수, org.gradle.java.home=JDK 17
├── core/                     # 공통 게임 로직 (Pure Java + LibGDX API)
│   ├── build.gradle          # 의존성: gdx, gdx-ai, gdx-freetype, gdx-platform:natives-desktop(test)
│   └── src/main/java/com/puyo/game/
│       ├── PuyoGame.java            # 메인 게임 클래스 (Game 인터페이스 구현)
│       ├── config/GameViewport.java # 가상 해상도 960x1600, FitViewport 팩토리
│       ├── graphics/FontManager.java # FreeTypeFontGenerator 한글 폰트 관리
│       ├── logic/engine/GameWorld.java # 게임 루프, 보드, 페어, 중력, 매칭, 연쇄
│       ├── logic/model/             # Puyo, PuyoColor, PuyoPair, Board, StageData
│       ├── menus/MenuLoader.java    # JSON 메뉴 로딩 (classpath/internal 폴백)
│       ├── screens/                 # BaseScreen, LoadingScreen, MenuScreen, PlayScreen, StoryModeSelectScreen
│       └── story/StoryModeManager.java # 스토리 모드 JSON 로딩, 스테이지 관리
│   └── src/main/resources/assets/   # 공통 에셋 (JAR의 assets/ 하위에 포함)
│       ├── config/
│       ├── data/
│       └── NotoSansKR-Regular.ttf
├── desktop/                  # 데스크톱 런처 (LWJGL3)
│   ├── build.gradle          # gdx-backend-lwjgl3, gdx-platform:natives-desktop
│   └── src/main/java/com/puyo/game/DesktopLauncher.java
├── android/                  # 안드로이드 앱
│   ├── build.gradle          # AGP 8.1, compileSdk 33, ndk.abiFilters [arm64-v8a, armeabi-v7a]
│   │   # mergeNativeLibs 후 libgdx-freetype.so → libpenguin.so 복사 + Python lief로 SONAME 패치
│   └── src/main/
│       ├── java/com/puyo/game/AndroidLauncher.java # AndroidApplication 구현
│       ├── AndroidManifest.xml # minSdk 21, targetSdk 33, 세로 고정
│       └── res/                # strings, colors, styles, drawable
├── .github/workflows/android-build.yml # CI/CD 파이프라인
├── docs/                     # 설계/진행 문서
└── patch_soname.py           # 빌드 시 SONAME 패치용 Python 스크립트 (lief 사용)
```

---

## 아키텍처 패턴

| 레이어 | 구성 요소 | 설명 |
|--------|----------|------|
| **엔트리 포인트** | `PuyoGame` (core), `AndroidLauncher` (android), `DesktopLauncher` (desktop) | 플랫폼별 초기화 |
| **게임 루프** | `GameWorld` | 업데이트/렌더링 분리, 고정 타임스텝(1/60s) |
| **상태 관리** | `Screen` 기반 (LibGDX) | Loading → Menu → Play/StorySelect |
| **렌더링** | `FitViewport` + `OrthographicCamera` | 가상 해상도 960x1600, 자동 스케일링 |
| **데이터** | JSON + `Json` (LibGDX) | 메뉴, 스테이지 데이터 외부화 |
| **리소스** | `AssetManager` (예정) | 텍스처/사운드/폰트 통합 관리 |

---

## 렌더링 아키텍처 (v0.1.3~)

### 가상 해상도 시스템
```java
// core/src/main/java/com/puyo/game/config/GameViewport.java
public class GameViewport {
    public static final int VIRTUAL_WIDTH = 960;
    public static final int VIRTUAL_HEIGHT = 1600;  // 3:5 세로 비율
    public static final float CELL_SIZE = 80f;      // 960 / 12 = 80
    public static final float BOARD_OFFSET_X = 240f; // (960 - 6*80) / 2
    public static final float BOARD_OFFSET_Y = 320f; // 상단 여백
    
    public static FitViewport createViewport() {
        OrthographicCamera camera = new OrthographicCamera();
        camera.setToOrtho(false, VIRTUAL_WIDTH, VIRTUAL_HEIGHT);
        return new FitViewport(VIRTUAL_WIDTH, VIRTUAL_HEIGHT, camera);
    }
}
```

### 적용된 화면들
| 화면 | 뷰포트 적용 | 렌더링 방식 |
|------|------------|------------|
| `LoadingScreen` | ✅ | ShapeRenderer + 카메라 프로젝션 |
| `MenuScreen` | ✅ | ShapeRenderer + 중앙 정렬 텍스트 |
| `StoryModeSelectScreen` | ✅ | ShapeRenderer + 그리드 레이아웃 |
| `PlayScreen` | ✅ | ShapeRenderer + 가상 좌표계 보드/뿌요 |

---

## 네이티브 라이브러리 처리 (중요: 현재 미해결)

### 문제 현황
| 항목 | 상태 | 비고 |
|------|------|------|
| `libgdx.so` | APK 포함됨 | arm64-v8a: 163KB, armeabi-v7a: 160KB |
| `libgdx-freetype.so` | **이름 변경 시도** | → `libpenguin.so`로 이름 변경 |
| `libpenguin.so` | APK 포함됨 | arm64-v8a: 797KB, armeabi-v7a: 757KB |
| **실기기 로드** | **실패** | `dlopen failed: library "libpenguin.so" not found` |

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

| 테스트 유형 | 도구 | 실행 환경 | 대상 |
|-------------|------|-----------|------|
| 단위 테스트 (로직) | JUnit 5 + LibGDX Headless | GitHub Actions / PC 로컬 | GameWorld, Board, PuyoPair, StoryModeManager |
| 통합 테스트 (메뉴) | JUnit 5 + LibGDX Headless | GitHub Actions / PC 로컬 | MenuLoader, MenuScreen 네비게이션 |
| UI 테스트 | Manual / Espresso (예정) | 실기기 / 에뮬레이터 | 화면 전환, 입력 처리 |
| 네이티브 로드 테스트 | adb + logcat + lldb | **PC 로컬 + 실기기** | libgdx.so, libpenguin.so 로드 검증 |

### 헤드리스 테스트 구성 (core/build.gradle)
```groovy
testImplementation "com.badlogicgames.gdx:gdx-backend-headless:$gdxVersion"
testImplementation "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop"
```

---

## 알려진 제약사항

| 제약사항 | 영향도 | 대응 방안 |
|----------|--------|-----------|
| **Termux에서 aapt2 미작동** | 로컬 APK 빌드 불가 | **PC 이전으로 해결** |
| **Termux에서 네이티브 lib 미포함** | 로컬 단위 테스트 불가 | **PC 이전으로 해결 (natives-desktop 자동 다운로드)** |
| **GitHub Actions에서 실기기 테스트 불가** | 네이티브 로드 검증 불가 | **PC + 실기기 adb 연결로 해결** |
| **libgdx-freetype 네이티브 의존성 불확실** | libpenguin.so 로드 실패 | **PC에서 lldb로 심볼/의존성 분석 후 해결** |
| **CI 러너 NDK 버전 고정 어려움** | 재현성 저하 | **PC 로컬에서 NDK 버전 고정하여 빌드** |

---

## 향후 아키텍처 개선 계획

| 영역 | 계획 | 우선순위 |
|------|------|---------|
| 리소스 관리 | `AssetManager` 도입 + 텍스처 아틀라스 | P1 |
| 네이티브 빌드 | 프리빌트 `.so` 대신 소스에서 NDK 빌드 (CMake) | P2 |
| 의존성 주입 | 수동 팩토리 → Dagger/Hilt 또는 수동 DI 컨테이너 | P3 |
| 상태 관리 | Screen 기반 → 상태 머신/이벤트 버스 | P3 |
| 네트워크 | WebSocket + Protobuf (온라인 대전) | P2 |

---

> **핵심**: 현재 네이티브 라이브러리 로드 실패는 **GitHub Actions/Termux 환경의 한계**입니다. **PC 로컬 환경에서 adb + lldb + Android Studio로 정밀 디버깅** 후 해결하는 것이 가장 빠른 경로입니다.
