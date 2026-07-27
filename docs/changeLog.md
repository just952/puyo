# Puyo Puyo 2 - 변경 이력 (ChangeLog)

## 📋 버전별 변경 이력

---

## v0.1.2 (2026-07-27) - 헤드리스 테스트 안정화 & 리소스 로딩 개선
### 🔧 수정
- **StoryModeManager.loadStages()** - Java ClassLoader 폴백 추가로 헤드리스 테스트 리소스 로딩 가능
  - `Thread.currentThread().getContextClassLoader()` → `getClass().getClassLoader()` 폴백 체인
  - `InputStream` 직접 읽기 후 `Json.fromJson()` 파싱
- **MenuLoader** - `Gdx.files.classpath()` → `internal()` 폴백 추가 (이미 완료)
- **GameTest** - GL 컨텍스트 없는 순수 로직 테스트로 재작성
  - `fullStartupFlow_noCrash`: LoadingScreen → MenuScreen 전환 검증
  - `mainMenuLoadsCorrectItems` - main.json 5개 항목 검증
  - `storyModeSelectLoadsCorrectItems` 등 6개 테스트 추가
- **테스트 리소스 복사** - `src/test/resources/data/menus/*.json`, `data/story/stages.json` 복사

### 📁 변경 파일
| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| `core/src/main/java/com/puyo/game/story/StoryModeManager.java` | 수정 | ClassLoader 폴백 추가, JSON 래퍼 파싱 |
| `core/src/test/java/com/puyo/game/GameTest.java` | 전체 재작성 | GL 없는 순수 로직 테스트 6개 |
| `core/src/test/resources/data/menus/*.json` (4개) | 신규 | 테스트용 메뉴 JSON 복사 |
| `core/src/test/resources/data/story/stages.json` | 신규 | 스토리 스테이지 데이터 복사 |
| `core/src/main/java/com/puyo/game/menus/MenuLoader.java` | 수정 | classpath → internal 폴백 |

### 커밋
- `c8cc148` - fix: StoryModeManager ClassLoader fallback for test resources
- `857bcde` - test: fix headless tests - copy menu JSON to test resources, avoid GL calls

---

## v0.1.2 (2026-07-26) - 헤드리스 테스트 안정화 & 리소스 로딩 개선
### ✨ 추가
- **MenuLoader** - `Gdx.files.classpath()` 우선 시도 → `internal()` 폴백
- **Menu JSON** - 래퍼 객체 제거, 플랫 배열 형식으로 변경 (`main_menu.json` → `main.json`)
- **GameTest** - 헤드리스 테스트 6종 추가 (메뉴 로드, 화면 전환, 스토리/대전/옵션 메뉴 로드)
- **리소스 복사** - `src/test/resources/data/menus/`, `data/story/` 복사 스크립트

### 📁 변경 파일
| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| `assets/data/menus/main.json` | 수정 | 래퍼 제거, 플랫 배열 |
| `assets/data/menus/story_mode_select.json` | 신규 | 스토리 모드 선택 메뉴 |
| `assets/data/menus/versus_mode_select.json` | 신규 | 대전 모드 선택 메뉴 |
| `assets/data/menus/options_menu.json` | 신규 | 옵션 메뉴 |
| `core/src/main/java/com/puyo/game/menus/MenuLoader.java` | 수정 | classpath → internal 폴백 |
| `core/src/main/java/com/puyo/game/menus/MenuLoader.java` | 수정 | classpath → internal 폴백 |
| `core/src/main/java/com/puyo/game/screens/PlayScreen.java` | 수정 | GL 없는 순수 로직 테스트로 재작성 |
| `core/src/test/java/com/puyo/game/GameTest.java` | 전체 재작성 | GL 없는 순수 로직 테스트 6종 추가 |
| `core/src/test/resources/data/menus/*.json` (4개) | 신규 | 테스트용 메뉴 JSON 복사 |
| `core/src/test/resources/data/story/stages.json` | 신규 | 스토리 스테이지 데이터 복사 |
| `core/build.gradle` | 수정 | `testImplementation gdx-platform:natives-desktop` 추가 |

### 커밋
- `f2a4996` - fix: headless tests pass - JSON flat arrays, classpath resource loading
- `857bcde` - test: fix headless tests - copy menu JSON to test resources, avoid GL calls

---

## v0.1.1 (2026-07-26) - JSON 플랫 배열 변경 & MenuLoader 클래스패스 폴백
### 🔧 수정
- **메뉴 JSON** - 래퍼 객체(`{menu_id, title, options:[...]}`) → 플랫 배열(`[...]`) 변경
- **MenuLoader** - `Gdx.files.classpath()` 우선 시도 → `internal()` 폴백
- **메뉴 파일명** - `main_menu.json` → `main.json` 통일

### 📁 변경 파일
| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| `assets/data/menus/main.json` | 수정 | 래퍼 제거, 플랫 배열 |
| `assets/data/menus/story_mode_select.json` | 신규 | 스토리 모드 선택 메뉴 |
| `assets/data/menus/versus_mode_select.json` | 신규 | 대전 모드 선택 메뉴 |
| `assets/data/menus/options_menu.json` | 신규 | 옵션 메뉴 |
| `core/src/main/java/com/puyo/game/menus/MenuLoader.java` | 수정 | 클래스패스 → 내부 폴백 |

---

## v0.1.1 (2026-07-26) - LibGDX 헤드리스 테스트 지원
### 🔧 수정
- `core/build.gradle` - `testImplementation "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop"` 추가
- `GameTest.java` - 헤드리스 테스트 2종 추가 (메뉴 로드, 메뉴 구조 검증)

---

## v0.1.0 (2026-07-26) - 초기 프로젝트 설정 및 핵심 로직 구현
### 🎉 초기 구현 완료
| 영역 | 구현 내용 |
|------|-----------|
| **빌드 시스템** | Gradle 8.4 + AGP 8.1.0, 멀티 모듈 (core/desktop/android) |
| **코어 게임 로직** | Board(6×12), 중력, 매칭(4개 이상), 연쇄 처리, Puyo/PuyoPair/Board 모델 |
| **메뉴 시스템** | JSON 기반 동적 메뉴 (MenuLoader, MenuItem, MenuAction) |
| **화면/스크린** | LoadingScreen → MenuScreen → PlayScreen, StoryModeSelectScreen |
| **스토리 모드** | StoryModeManager (JSON 기반), 3 스테이지, 언락/승리 조건 |
| **안드로이드 모듈** | AndroidLauncher, AndroidManifest.xml, AGP 8.1, compileSdk 33 |
| **데스크톱 런처** | LWJGL3 백엔드, 480×800 세로 화면 |
| **헤드리스 테스트** | gdx-backend-headless + natives-desktop |
| **GitHub Actions CI** | android-build.yml (테스트 → APK 빌드 → 아티팩트 업로드) |

### 📁 초기 생성 파일 (주요)
| 파일 | 설명 |
|------|------|
| `build.gradle` (root) | AGP 8.1.0, Kotlin 1.8.0, libGDX 1.12.1 |
| `settings.gradle` | `include 'core', 'desktop', 'android'` |
| `core/src/main/java/.../PuyoGame.java` | 메인 게임 클래스 (Game 상속) |
| `core/src/main/java/.../logic/engine/GameWorld.java` | 게임 루프, 보드, 페어, 중력, 매칭, 연쇄 |
| `core/src/main/java/.../logic/model/` | Puyo, PuyoColor, PuyoPair, Board, StageData |
| `core/src/main/java/.../menus/MenuLoader.java` | JSON 메뉴 로딩 |
| `core/src/main/java/.../screens/` | LoadingScreen, MenuScreen, PlayScreen, StoryModeSelectScreen |
| `core/src/main/java/.../story/StoryModeManager.java` | 스토리 모드 관리 |
| `android/build.gradle` | AGP 8.1, compileSdk 33, NDK abiFilters |
| `.github/workflows/android-build.yml` | CI/CD 파이프라인 |

---

## 📋 파일별 변경 이력 요약

| 파일 | 생성/수정 횟수 | 주요 변경 사유 |
|------|----------------|----------------|
| `StoryModeManager.java` | 3회 | ClassLoader 폴백, JSON 래퍼 파싱, 테스트 리소스 지원 |
| `MenuLoader.java` | 2회 | 클래스패스 폴백, 플랫 JSON 배열 지원 |
| `GameTest.java` | 3회 | GL 제거, 순수 로직 테스트, 리소스 로드 검증 |
| `assets/data/menus/*.json` | 2회 | 래퍼 제거, 플랫 배열, 파일명 통일 |
| `build.gradle` (root) | 3회 | AGP 업그레이드, headless natives 추가, gradlePluginPortal 수정 |
| `android-build.yml` | 2회 | 테스트 단계 추가, SDK 설치 단순화 |
| `core/build.gradle` | 2회 | headless natives 테스트 의존성 추가 |

---


## v0.0.9 (2026-07-12) - 초기 개발
- fd716e6 로직 구현
- e21a50b StoryModeManager 에 주석추가
- 38fe855 StoryModeManager 충돌 수정
- 5bf2a89 storyModeManager 추가
- 9c5a0c0 메뉴 틀 작성
- 08cf904 7.22
- 0ec4e5b 3rd step
- d112924 step 2 까지
- 206afca 3rd commit
- 548e51c 2nd commit
- 32a3a68 test commit
- fd716e6 로직 구현 (2026-07-25) – 게임 로직 초기 구현: Board, PuyoPair, GameWorld 등 핵심 클래스 추가

> **참고**: 이 문서는 주요 작업 단위(커밋 단위) 기준으로 작성되었습니다. 세부 라인 단위 변경은 `git log -p` 또는 GitHub 커밋 히스토리 참조 바랍니다.