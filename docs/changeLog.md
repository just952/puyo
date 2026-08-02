# Puyo Puyo 2 - 변경 이력 (ChangeLog)

## 버전별 변경 이력

---

## v0.1.4 (2026-08-01) - libgdx-freetype.so → libpenguin.so 이름 변경 및 PC 개발 환경 이전 계획
### 추가
- **네이티브 라이브러리 이름 변경 로직** (android/build.gradle)
  - libgdx-freetype.so → libpenguin.so로 이름 변경 (이동, 복사 아님)
  - armeabi-v7a, arm64-v8a 모두 적용
- **AndroidLauncher 단일 로드 수정**
  - System.loadLibrary("gdx-freetype") 제거
  - System.loadLibrary("penguin")만 유지
- **architecture.md**에 네이티브 라이브러리 처리 상세 문서화
- **PC 개발 환경 이전 계획** 문서화 (JDK 17, Android SDK, NDK, 로컬 빌드/테스트/디버그)

### 수정
- **libgdx-freetype.so 중복 로드 문제 해결**
  - 원인: gdx-freetype 네이티브 코드 내부에서 dlopen("libpenguin.so") 호출
  - 별도 파일로 존재 시 동적 링커가 중복 로드로 인식 → 실패
  - 해결: 단일 파일(libpenguin.so), 단일 이름(penguin)으로만 로드

### 변경 파일
| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| android/build.gradle | 수정 | libgdx-freetype.so → libpenguin.so 이름 변경 로직 추가 |
| android/src/main/java/com/puyo/game/AndroidLauncher.java | 수정 | System.loadLibrary("gdx-freetype") 제거, penguin만 로드 |
| docs/architecture.md | 수정 | 네이티브 라이브러리 처리, PC 이전 계획 추가 |
| docs/progress.md | 수정 | 진행 현황 업데이트, v0.1.1 마일스톤 추가 |
| docs/todo.md | 수정 | P0-4 추가, 완료 작업에 V014-* 추가 |

### 커밋
- 0961e9c - fix: libgdx-freetype.so -> libpenguin.so 이름 변경, 단일 로드 수정

---

## v0.1.3 (2026-07-28) - 뷰포트/카메라 시스템 구현 & 가상 해상도 960×1600 적용
### 추가
- **GameViewport 설정 클래스** - 가상 해상도 VIRTUAL_WIDTH=960, VIRTUAL_HEIGHT=1600 (3:5 세로 비율)
- **FitViewport 팩토리 메서드** - GameViewport.createViewport() 자동 카메라/뷰포트 생성
- **BaseScreen 공통 뷰포트 관리** - initViewport(), resize() 자동 처리, 카메라 프로젝션 적용

### 수정
- **PlayScreen 전체 리팩토링** - 고정 픽셀 좌표 → 가상 좌표계 변경
  - CELL_SIZE: 32px → 80f (가상 해상도 기준)
  - 보드 영역: 480x960 가상 픽셀, 오프셋 (240, 320)로 중앙 정렬
  - ShapeRenderer.setProjectionMatrix(camera.combined) 적용
  - UI 텍스트 위치 가상 해상도 기준 중앙 정렬로 변경
- **MenuScreen 전체 리팩토링** - 뷰포트 적용, 메뉴 항목 중앙 정렬 좌표로 변경
- **StoryModeSelectScreen 전체 리팩토링** - 뷰포트 적용, 가상 해상도 기준 렌더링
- **LoadingScreen 전체 리팩토링** - 뷰포트 적용, 중앙 정렬 로딩 텍스트
- **GameWorld** - getCurrentChain() 메서드 추가 (UI 연쇄 표시용)
- **architecture.md** - 새로운 렌더링 아키텍처(FitViewport + 가상 해상도) 문서화

### 변경 파일
| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| core/src/main/java/com/puyo/game/config/GameViewport.java | 신규 | 가상 해상도/뷰포트 설정 클래스 |
| core/src/main/java/com/puyo/game/screens/BaseScreen.java | 전체 수정 | 카메라/뷰포트 공통 관리 |
| core/src/main/java/com/puyo/game/screens/PlayScreen.java | 전체 리팩토링 | 가상 좌표계 적용 |
| core/src/main/java/com/puyo/game/screens/MenuScreen.java | 전체 리팩토링 | 뷰포트 적용, 중앙 정렬 |
| core/src/main/java/com/puyo/game/screens/StoryModeSelectScreen.java | 전체 리팩토링 | 뷰포트 적용 |
| core/src/main/java/com/puyo/game/screens/LoadingScreen.java | 전체 리팩토링 | 뷰포트 적용 |
| core/src/main/java/com/puyo/game/logic/engine/GameWorld.java | 수정 | getCurrentChain() 추가 |
| docs/architecture.md | 수정 | 렌더링 아키텍처 문서화 |

### 커밋
- HEAD - feat: Implement FitViewport with 960x1600 virtual resolution

---

## v0.1.2 (2026-07-27) - 헤드리스 테스트 안정화 & 리소스 로딩 개선
### 수정
- **StoryModeManager.loadStages()** - Java ClassLoader 폴백 추가로 헤드리스 테스트 리소스 로딩 가능
  - Thread.currentThread().getContextClassLoader() → getClass().getClassLoader() 폴백 체인
  - InputStream 직접 읽기 후 Json.fromJson() 파싱
- **MenuLoader** - Gdx.files.classpath() → internal() 폴백 추가 (이미 완료)
- **GameTest** - GL 컨텍스트 없는 순수 로직 테스트로 재작성
  - fullStartupFlow_noCrash: LoadingScreen → MenuScreen 전환 검증
  - mainMenuLoadsCorrectItems - main.json 5개 항목 검증
  - storyModeSelectLoadsCorrectItems 등 6개 테스트 추가
- **테스트 리소스 복사** - src/test/resources/data/menus/*.json, data/story/stages.json 복사

### 변경 파일
| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| core/src/main/java/com/puyo/game/story/StoryModeManager.java | 수정 | ClassLoader 폴백 추가, JSON 래퍼 파싱 |
| core/src/test/java/com/puyo/game/GameTest.java | 전체 재작성 | GL 없는 순수 로직 테스트 6개 |
| core/src/test/resources/data/menus/*.json (4개) | 신규 | 테스트용 메뉴 JSON 복사 |
| core/src/test/resources/data/story/stages.json | 신규 | 스토리 스테이지 데이터 복사 |
| core/src/main/java/com/puyo/game/menus/MenuLoader.java | 수정 | classpath -> internal 폴백 |

### 커밋
- c8cc148 - fix: StoryModeManager ClassLoader fallback for test resources
- 857bcde - test: fix headless tests - copy menu JSON to test resources, avoid GL calls

---

## v0.1.1 (2026-07-26) - LibGDX 헤드리스 테스트 지원
### 수정
- core/build.gradle - testImplementation "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop" 추가
- GameTest.java - 헤드리스 테스트 2종 추가 (메뉴 로드, 메뉴 구조 검증)

---

## v0.1.0 (2026-07-26) - 초기 프로젝트 설정 및 핵심 로직 구현
### 초기 구현 완료
| 영역 | 구현 내용 |
|------|-----------|
| 빌드 시스템 | Gradle 8.4 + AGP 8.1.0, 멀티 모듈 (core/desktop/android) |
| 코어 게임 로직 | Board(6x12), 중력, 매칭(4개 이상), 연쇄 처리, Puyo/PuyoPair/Board 모델 |
| 메뉴 시스템 | JSON 기반 동적 메뉴 (MenuLoader, MenuItem, MenuAction) |
| 화면/스크린 | LoadingScreen → MenuScreen → PlayScreen, StoryModeSelectScreen |
| 스토리 모드 | StoryModeManager (JSON 기반), 3 스테이지, 언락/승리 조건 |
| 안드로이드 모듈 | AndroidLauncher, AndroidManifest.xml, AGP 8.1, compileSdk 33 |
| 데스크톱 런처 | LWJGL3 백엔드, 480x800 세로 화면 |
| 헤드리스 테스트 | gdx-backend-headless + natives-desktop |
| GitHub Actions CI | android-build.yml (테스트 → APK 빌드 → 아티팩트 업로드) |

### 초기 생성 파일 (주요)
| 파일 | 설명 |
|------|------|
| build.gradle (root) | AGP 8.1.0, Kotlin 1.8.0, libGDX 1.12.1 |
| settings.gradle | include 'core', 'desktop', 'android' |
| core/src/main/java/.../PuyoGame.java | 메인 게임 클래스 (Game 상속) |
| core/src/main/java/.../logic/engine/GameWorld.java | 게임 루프, 보드, 페어, 중력, 매칭, 연쇄 |
| core/src/main/java/.../logic/model/ | Puyo, PuyoColor, PuyoPair, Board, StageData |
| core/src/main/java/.../menus/MenuLoader.java | JSON 메뉴 로딩 |
| core/src/main/java/.../screens/ | LoadingScreen, MenuScreen, PlayScreen, StoryModeSelectScreen |
| core/src/main/java/.../story/StoryModeManager.java | 스토리 모드 관리 |
| android/build.gradle | AGP 8.1, compileSdk 33, NDK abiFilters |
| .github/workflows/android-build.yml | CI/CD 파이프라인 |

---

## 파일별 변경 이력 요약

| 파일 | 생성/수정 횟수 | 주요 변경 사유 |
|------|----------------|----------------|
| StoryModeManager.java | 3회 | ClassLoader 폴백, JSON 래퍼 파싱, 테스트 리소스 지원 |
| MenuLoader.java | 2회 | 클래스패스 폴백, 플랫 JSON 배열 지원 |
| GameTest.java | 3회 | GL 제거, 순수 로직 테스트, 리소스 로드 검증 |
| assets/data/menus/*.json | 2회 | 래퍼 제거, 플랫 배열, 파일명 통일 |
| build.gradle (root) | 3회 | AGP 업그레이드, headless natives 추가, gradlePluginPortal 수정 |
| android-build.yml | 2회 | 테스트 단계 추가, SDK 설치 단순화 |
| core/build.gradle | 2회 | headless natives 테스트 의존성 추가 |
| GameViewport.java | 1회 (신규) | 가상 해상도 960x1600, FitViewport 팩토리 |
| BaseScreen.java | 2회 | 카메라/뷰포트 공통 관리 |
| PlayScreen.java | 2회 | 가상 좌표계 리팩토링 |
| MenuScreen.java | 2회 | 뷰포트 적용 |
| StoryModeSelectScreen.java | 2회 | 뷰포트 적용 |
| LoadingScreen.java | 2회 | 뷰포트 적용 |
| GameWorld.java | 2회 | getCurrentChain() 추가 |
| architecture.md | 2회 | 렌더링 아키텍처 문서화 |
| android/build.gradle | 2회 | 네이티브 라이브러리 패키징, freetype 이름 변경 |
| AndroidLauncher.java | 2회 | 네이티브 라이브러리 로드 순서 수정 |

---

> 참고: 이 문서는 주요 작업 단위(커밋 단위) 기준으로 작성되었습니다. 세부 라인 단위 변경은 git log -p 또는 GitHub 커밋 히스토리 참조 바랍니다.
