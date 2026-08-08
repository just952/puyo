# Puyo Puyo 2 - 변경 이력 (ChangeLog)

## 버전별 변경 이력

---

## v0.1.8 (2026-08-08) - **DAS/ARR 키 반복 이동 구현 + 화면 밖 뿌요(고스트) 충돌 무시로 원작 느낌 살림**

### 추가

1. **DAS/ARR (Delayed Auto Shift / Auto Repeat Rate) 입력 시스템** (`InputHandler.java`)
   - 키 누름 즉시 1회 이동 (첫 프레임)
   - DAS_DELAY_FRAMES = 16프레임 (~0.27초) 지연 후 자동 반복 시작
   - ARR_INTERVAL_FRAMES = 2프레임마다 1칸씩 반복 이동 (초당 30회)
   - 키 떼면 카운터 완전 리셋, 좌우 동시 누름 시 상쇄

2. **화면 밖(위쪽) 뿌요 고스트 충돌 무시** (`Board.java`)
   - `isInsideVisible(Puyo p)` 헬퍼 메서드 추가: `p.getY() < HEIGHT`만 체크
   - `canMoveLeft`, `canMoveRight`, `canMoveDown`, `canPlace` 모두 적용
   - 스폰 시 right 뿌요가 y=12(화면 밖)에 있어도 left 뿌요만으로 좌우 이동 가능
   - 원작 뿌요뿌요와 동일: 필드 상단에서 좌우로 피할 수 있음

### 변경 파일

| 파일                                                       | 변경 유형 | 설명                                                                 |
| ---------------------------------------------------------- | --------- | -------------------------------------------------------------------- |
| `core/src/main/java/com/puyo/game/input/InputHandler.java` | 수정      | DAS/ARR 상태 필드/상수 추가, updateDasArr(), getMoveDirection() 수정 |
| `core/src/main/java/com/puyo/game/logic/model/Board.java`  | 수정      | isInsideVisible() 추가, 4개 충돌 체크 메서드에 적용                  |

### 검증 결과

- `:core:compileJava` / `:desktop:compileJava` / `:desktop:run` 모두 성공
- 데스크톱 앱 실행 확인 - 게임플레이 진입, 스테이지 로드 정상

### 커밋

- `b158b15` - feat: DAS/ARR input + ghost puyo collision ignore

---

## v0.1.7 (2026-08-07) - **락 딜레이(Tsu 규칙) 완전 구현, 회전 버그 수정, 다음 블록 스폰 버그 수정, 폰트 증분 로딩, 안드로이드 네이티브 라이브러리 로드 수정**

### 해결된 버그

1. **락 딜레이 메커니즘 (Lock Delay) - Tsu 규칙 완전 구현** (`GameWorld.java`)
   - **문제**: 뿌요가 바닥에 닿아도 계속 움직이면 절대 잠기지 않음
   - **원인**: `resetLockDelay()`에서 `lockDelayActive` 매번 `false`로 리셋, 이동 카운터 공중에서도 누적
   - **해결**:
     - `lockDelayMoveCount` 추가로 이동/회전 15회 제한 구현
     - `lockDelayActive` 상태 관리 개선 (스폰/잠금 시 리셋)
     - 공중 이동 시 카운터 리셋, 락 딜레이 중일 때만 카운트
   - **Tsu 규칙**: 락 딜레이 0.5초, 이동/회전 15회 제한, 초과 시 즉시 잠금

2. **뿌요 회전 안 되는 버그** (`PuyoPair.java`, `PlayScreen.java`)
   - **문제**: 회전 키를 눌러도 뿌요가 회전하지 않음
   - **원인**:
     1. `PuyoPair.rotateClockwise()`가 `setPosition()` 호출 안 함
     2. `PlayScreen`에서 `gameWorld.rotateClockwise()` 대신 `getCurrentPair().rotateClockwise()` 직접 호출 (벽 킥 무시)
     3. `render()`에서 `inputHandler.update()` 중복 호출로 엣지 감지 실패
   - **해결**:
     - `PuyoPair.rotateClockwise()`/`rotateCounterClockwise()`에 `setPosition()` 추가
     - `PlayScreen`에서 `gameWorld.rotateClockwise()` 사용 (벽 킥 포함)
     - `render()`에서 `inputHandler.update()` 제거, `update()`에서 한 번만 호출

3. **다음 블록 바닥 생성 버그** (`GameWorld.java`)
   - **문제**: 다음 뿌요가 상단 중앙이 아닌 바닥(0,0)에서 생성
   - **원인**: `spawnNextPair()`에서 `setPosition()` 미호출
   - **해결**: `createAndPositionPair()` 공통 메서드로 추출하여 스폰 위치 설정

4. **폰트 로딩 지연 최적화** (`FontManager.java`)
   - **문제**: 한글 11,172자 미리 생성으로 로딩 화면 지연
   - **해결**:
     - `FreeTypeFontParameter.incremental = true` 동적 글리프 생성
     - 기본 문자셋만 미리 생성 (DEFAULT_CHARS + 게임용 한글)
     - 나머지 11,172자는 런타임 동적 생성

5. **안드로이드 네이티브 라이브러리 로드 실패** (`AndroidLauncher.java`)
   - **문제**: `libpenguin.so` dlopen 실패
   - **해결**:
     - `System.loadLibrary("penguin")` 제거 (gdx-freetype가 내부에서 dlopen)
     - `android:extractNativeLibs` 제거

### 리팩토링

- **GameWorld.java** - 스폰 로직 통합: `createAndPositionPair()` 공통 메서드 추출

### 변경 파일

| 파일                                                           | 변경 유형 | 설명                                          |
| -------------------------------------------------------------- | --------- | --------------------------------------------- |
| `core/src/main/java/com/puyo/game/logic/engine/GameWorld.java` | 수정      | 락 딜레이 구현, 스폰 로직 리팩토링, 버그 수정 |
| `core/src/main/java/com/puyo/game/logic/model/PuyoPair.java`   | 수정      | 회전 시 위치 갱신 로직 추가                   |
| `core/src/main/java/com/puyo/game/screens/PlayScreen.java`     | 수정      | 회전 처리 로직 수정, 입력 처리 순서 수정      |
| `core/src/main/java/com/puyo/game/graphics/FontManager.java`   | 수정      | Incremental 폰트 생성 적용, 한글 로딩 최적화  |
| `android/src/main/java/com/puyo/game/AndroidLauncher.java`     | 수정      | 네이티브 라이브러리 로드 방식 수정            |

### 검증 결과

- `:core:compileJava` / `:desktop:compileJava` / `:desktop:run` 모두 성공
- 데스크톱 앱 2분 51초 크래시 없는 실행
- 회전 키(↑/W/X/1, Z/2) 정상 작동
- 락 딜레이: 15회 이동/회전 또는 0.5초 후 자동 잠금
- 다음 블록 상단 중앙 정상 생성
- 한글/영문 폰트 정상 렌더링

### 커밋

- `b814c7f` - feat(engine): implement Tsu rules lock delay move limit
- `ea5636f` - refactor: extract pair creation logic to eliminate duplication
- `48b84f1` - 다음 생성시 바닥생성 버그 수정
- `b2192cb` - 회전키 버그 수정
- `9d58fad` - feat(graphics): switch FontManager to incremental mode for dynamic glyph generation
- `f4e683d` - fix: handle platform-specific font resource paths for Android and Desktop

---

## v0.1.6 (2026-08-06) - **GameViewport 1600×960 가로 고정 리팩토링 완료, 터치 컨트롤러 구현, 데스크톱/모바일 가로 모드 적용**

### 추가

1. **GameViewport 전면 재작성 (가로 고정 1600×960)**
   - 가상 해상도 960×1600 (세로) → 1600×960 (가로) 변경
   - `GameViewport.Single` - 싱글 플레이 레이아웃 (보드 왼쪽 480×960, 사이드 패널 오른쪽)
   - `GameViewport.Versus` - 대전 모드 레이아웃 (P1보드|중앙UI|P2보드)
   - `GameViewport.Menu` - 메뉴/UI 중앙 정렬 레이아웃

2. **입력 시스템 통합 (InputHandler + TouchController)**
   - `InputHandler` - 키보드(PC)/터치(모바일) 통합 인터페이스, isMobile 플래그 분기
   - `TouchController` - 4버튼 레이아웃 (좌/우 이동, 회전, 드롭/더블탭 하드드롭), 정규화 좌표(0~1) 기반
   - 더블탭(300ms) 감지로 하드 드롭 구현

3. **PlayScreen 게임플레이 레이아웃 적용**
   - GameViewport.Single 상수 사용으로 보드/다음뿌요/UI 위치 재조정
   - InputHandler 연동으로 키보드/터치 통합 입력 처리
   - InputProcessor 직접 구현 제거, InputHandler 위임 방식

4. **메뉴 화면 가로 레이아웃 적용**
   - MenuScreen, StoryModeSelectScreen - GameViewport.Menu 중앙 정렬 영역 사용

5. **데스크톱 런처 가로 고정**
   - DesktopLauncher 1600×960 창 크기, setResizable(true)로 비율 유지 리사이즈

### 변경 파일

| 파일                                                                  | 변경 유형   | 설명                                                         |
| --------------------------------------------------------------------- | ----------- | ------------------------------------------------------------ |
| `core/src/main/java/com/puyo/game/config/GameViewport.java`           | 전체 재작성 | 1600×960 가로 고정, Single/Versus/Menu 레이아웃 클래스 추가  |
| `core/src/main/java/com/puyo/game/input/InputHandler.java`            | 신규        | 키보드/터치 통합 입력 처리기                                 |
| `core/src/main/java/com/puyo/game/input/TouchController.java`         | 신규        | 모바일 4버튼 터치 컨트롤러                                   |
| `core/src/main/java/com/puyo/game/screens/PlayScreen.java`            | 대폭 수정   | Single 레이아웃 적용, InputHandler 연동, InputProcessor 제거 |
| `core/src/main/java/com/puyo/game/screens/MenuScreen.java`            | 수정        | Menu 레이아웃 상수 사용                                      |
| `core/src/main/java/com/puyo/game/screens/StoryModeSelectScreen.java` | 수정        | Menu 레이아웃 상수 사용                                      |
| `desktop/src/main/java/com/puyo/game/DesktopLauncher.java`            | 수정        | 1600×960 창 크기, 리사이즈 비율 유지                         |
| `android/src/main/java/com/puyo/game/AndroidLauncher.java`            | 수정        | TouchController import 추가                                  |
| `core/src/main/java/com/puyo/game/graphics/FontManager.java`          | 수정        | 폰트 경로 assets/ 하위로 변경                                |
| `core/src/main/java/com/puyo/game/menus/MenuLoader.java`              | 수정        | 메뉴 경로 assets/data/menus/로 변경                          |
| `core/src/main/java/com/puyo/game/story/StoryModeManager.java`        | 수정        | 스토리 데이터 경로 assets/data/story/로 변경                 |

### 검증 결과

- `:core:compileJava` / `:desktop:compileJava` / `:android:compileDebugJavaWithJavac` 모두 성공
- `:core:test` 6/6 테스트 통과
- 데스크톱 앱 실행 확인 - 메인 메뉴 → 스토리 선택 → 게임 화면(ENDLESS) 정상 진입

### 커밋

- `286a742` - feat: GameViewport 1600x960 landscape refactor + touch controller

---

## v0.1.5 (2026-08-03) - **libpenguin.so SONAME 패치 성공, 한글 폰트 정상 적용, 실기기 정상 실행**

### 해결 내용

1. **libpenguin.so SONAME 패치 완료**
   - Python `lief` 라이브러리로 `libgdx-freetype.so` 복사본의 SONAME을 `libpenguin.so`로 변경
   - `llvm-objcopy --set-soname` 미지원으로 Python 스크립트(`patch_soname.py`)로 우회 해결
   - `mergeDebugNativeLibs` 태스크 후 자동 실행하도록 `android/build.gradle`에 통합

2. **한글 폰트 정상 적용**
   - Google Fonts API(`https://fonts.gstatic.com/s/notosanskr/v39/...`)에서 정상 TTF 다운로드 (5.87MB)
   - Git 커밋 시 CRLF 변환으로 손상된 원본 파일 교체
   - `FontManager.param.characters`에 메뉴/게임 필수 한글 문자 명시로 X박스 문제 해결

3. **실기기(갤럭시 S23, Android 14) 정상 실행 확인**
   - APK 설치 → 실행 → 메인 메뉴 진입 → 한글 정상 표시 → 크래시 없음
   - 로그에 `Unable to open libpenguin.so` 경고 있으나 앱 크래시 없이 실행 지속
   - 폰트 로딩 에러(`Error reading file: fonts/NotoSansKR-Regular.ttf`) 해결

4. **에셋 구조 정리**
   - `core/src/main/resources/assets/` 단일 소스로 통합
   - 루트 `assets/`, `android/src/main/assets/` 중복 제거
   - `build.gradle` (root): `srcDirs = ['src/main/resources']`로 JAR의 `assets/` 하위 포함
   - `android/build.gradle`: `assets.srcDirs = ['src/main/assets']` (안드로이드 전용만)

### 변경 파일

| 파일                                                             | 변경 유형 | 설명                                                                                |
| ---------------------------------------------------------------- | --------- | ----------------------------------------------------------------------------------- |
| `android/build.gradle`                                           | 수정      | `mergeNativeLibs` 후 SONAME 패치 태스크 추가 (`patch_soname.py` 호출)               |
| `core/src/main/java/com/puyo/game/graphics/FontManager.java`     | 수정      | `param.characters`에 필수 한글 문자 추가, 폰트 경로 `NotoSansKR-Regular.ttf`로 변경 |
| `android/src/main/java/com/puyo/game/AndroidLauncher.java`       | 수정      | `System.loadLibrary("penguin")` 제거 (불필요)                                       |
| `build.gradle` (root)                                            | 수정      | `srcDirs = ['src/main/resources']`로 assets JAR 포함                                |
| `android/build.gradle`                                           | 수정      | `assets.srcDirs = ['src/main/assets']` 단순화, `../assets` 제거                     |
| `core/src/main/resources/assets/`                                | 이동/추가 | 폰트, JSON 모두 core JAR의 assets 하위에 포함                                       |
| `patch_soname.py`                                                | 신규      | Python lief로 SONAME 패치 스크립트                                                  |
| `core/src/main/assets/fonts/`                                    | 삭제      | 중복 폰트 제거                                                                      |
| `android/src/main/assets/`                                       | 삭제      | 빈 폴더 제거                                                                        |
| 루트 `assets/`                                                   | 삭제      | 중복 에셋 제거                                                                      |
| `lib/`, `patchelf/`, `check_font.py`, `fix_deps.ps1`, `test.txt` | 삭제      | 임시 파일 정리                                                                      |

### 커밋

- `5cb5ec4` - fix: Android native lib loading & font issues for local PC build
- `cc6c4c1` - chore: remove temporary utility scripts and build artifacts

---

## v0.1.4 (2026-08-02) - libpenguin.so 실기기 로드 실패 확인, PC 개발 환경 이전 결정

### 현상

- APK에 `libpenguin.so` (arm64-v8a: 797KB, armeabi-v7a: 757KB) 정상 포함 확인
- `readelf -d`로 SONAME `[libpenguin.so]` 정상 확인
- `patchelf --set-soname`으로 SONAME 수정 후 재서명까지 완료
- **하지만 실기기(갤럭시 S23, Android 14)에서 여전히 `dlopen failed: library "libpenguin.so" not found` 발생**

### 시도한 해결 방법 (모두 실패)

1. **libgdx-freetype.so → libpenguin.so 이름 변경** (android/build.gradle copy + rename)
2. **AndroidLauncher에서 System.loadLibrary("gdx-freetype") 제거**, `System.loadLibrary("penguin")`만 단일 로드
3. **SONAME 패치** (`patchelf --set-soname libpenguin.so`) 후 APK 재패키징 + 디버그 키 재서명
4. **armeabi-v7a / arm64-v8a 모두 적용** 확인

### 원인 추정

- gdx-freetype 네이티브 코드 내부에서 `dlopen("libpenguin.so")` 호출 시 **동적 링커가 이미 로드된 libpenguin.so를 찾지 못함**
- `android:extractNativeLibs="true"` (기본값)인데도 압축 해제되지 않거나 경로 불일치 가능성
- **GitHub Actions 러너의 Android SDK/NDK 버전 차이**로 인한 네이티브 라이브러리 빌드/패키징 문제
- Termux 환경에서 로컬 디버깅 불가 (aapt2, lldb 미작동)

### 결정 사항

> **GitHub Actions + Termux 환경으로는 네이티브 라이브러리 로드 문제 디버깅/해결 불가능**
> **PC 로컬 개발 환경으로 이전하여 adb + lldb + Android Studio로 정밀 분석 필요**

### 변경 파일

| 파일                                                     | 변경 유형 | 설명                                                           |
| -------------------------------------------------------- | --------- | -------------------------------------------------------------- |
| android/build.gradle                                     | 수정      | libgdx-freetype.so → libpenguin.so 이름 변경 로직 (실패)       |
| android/src/main/java/com/puyo/game/AndroidLauncher.java | 수정      | System.loadLibrary("gdx-freetype") 제거, penguin만 로드 (실패) |
| docs/architecture.md                                     | 수정      | 네이티브 라이브러리 처리 실패 기록, PC 이전 계획 강화          |
| docs/progress.md                                         | 수정      | 진행 현황 업데이트, 실패 기록, v0.1.1/v0.1.2 마일스톤 추가     |
| docs/todo.md                                             | 수정      | P0-1~P0-3 재정의 (PC 구축 최우선), 완료 작업에 실패 기록 추가  |

### 커밋

- 0961e9c - fix: libgdx-freetype.so → libpenguin.so 이름 변경, 단일 로드 수정 (실패)
- c519213 - docs: 현행화 (실패 기록 포함)

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

| 파일                                                                | 변경 유형     | 설명                           |
| ------------------------------------------------------------------- | ------------- | ------------------------------ |
| core/src/main/java/com/puyo/game/config/GameViewport.java           | 신규          | 가상 해상도/뷰포트 설정 클래스 |
| core/src/main/java/com/puyo/game/screens/BaseScreen.java            | 전체 수정     | 카메라/뷰포트 공통 관리        |
| core/src/main/java/com/puyo/game/screens/PlayScreen.java            | 전체 리팩토링 | 가상 좌표계 적용               |
| core/src/main/java/com/puyo/game/screens/MenuScreen.java            | 전체 리팩토링 | 뷰포트 적용, 중앙 정렬         |
| core/src/main/java/com/puyo/game/screens/StoryModeSelectScreen.java | 전체 리팩토링 | 뷰포트 적용                    |
| core/src/main/java/com/puyo/game/screens/LoadingScreen.java         | 전체 리팩토링 | 뷰포트 적용                    |
| core/src/main/java/com/puyo/game/logic/engine/GameWorld.java        | 수정          | getCurrentChain() 추가         |
| docs/architecture.md                                                | 수정          | 렌더링 아키텍처 문서화         |

### 커밋

- HEAD - feat: Implement FitViewport with 960x1600 virtual resolution

---

## v0.1.2 (2026-07-27) - 헤드리스 테스트 안정화 & 리소스 로딩 개선

### 수정

- **StoryModeManager.loadStages()** - Java ClassLoader 폴백 추가로 헤드리스 테스트 리소스 로딩 가능
- **MenuLoader** - Gdx.files.classpath() → internal() 폴백 추가
- **GameTest** - GL 컨텍스트 없는 순수 로직 테스트로 재작성 (6개 테스트)
- **테스트 리소스 복사** - src/test/resources/data/menus/\*.json, data/story/stages.json 복사

### 변경 파일

| 파일                                                         | 변경 유형   | 설명                                  |
| ------------------------------------------------------------ | ----------- | ------------------------------------- |
| core/src/main/java/com/puyo/game/story/StoryModeManager.java | 수정        | ClassLoader 폴백 추가, JSON 래퍼 파싱 |
| core/src/test/java/com/puyo/game/GameTest.java               | 전체 재작성 | GL 없는 순수 로직 테스트 6개          |
| core/src/test/resources/data/menus/\*.json (4개)             | 신규        | 테스트용 메뉴 JSON 복사               |
| core/src/test/resources/data/story/stages.json               | 신규        | 스토리 스테이지 데이터 복사           |
| core/src/main/java/com/puyo/game/menus/MenuLoader.java       | 수정        | classpath -> internal 폴백            |

### 커밋

- c8cc148 - fix: StoryModeManager ClassLoader fallback for test resources
- 857bcde - test: fix headless tests - copy menu JSON to test resources, avoid GL calls

---

## v0.1.1 (2026-07-26) - LibGDX 헤드리스 테스트 지원

### 수정

- core/build.gradle - testImplementation "com.badlogicgames.gdx:gdx-platform:$gdxVersion:natives-desktop" 추가
- GameTest.java - 헤드리스 테스트 2종 추가

---

## v0.1.0 (2026-07-26) - 초기 프로젝트 설정 및 핵심 로직 구현

### 초기 구현 완료

| 영역              | 구현 내용                                                              |
| ----------------- | ---------------------------------------------------------------------- |
| 빌드 시스템       | Gradle 8.4 + AGP 8.1.0, 멀티 모듈 (core/desktop/android)               |
| 코어 게임 로직    | Board(6x12), 중력, 매칭(4개 이상), 연쇄 처리, Puyo/PuyoPair/Board 모델 |
| 메뉴 시스템       | JSON 기반 동적 메뉴 (MenuLoader, MenuItem, MenuAction)                 |
| 화면/스크린       | LoadingScreen → MenuScreen → PlayScreen, StoryModeSelectScreen         |
| 스토리 모드       | StoryModeManager (JSON 기반), 3 스테이지, 언락/승리 조건               |
| 안드로이드 모듈   | AndroidLauncher, AndroidManifest.xml, AGP 8.1, compileSdk 33           |
| 데스크톱 런처     | LWJGL3 백엔드, 480x800 세로 화면                                       |
| 헤드리스 테스트   | gdx-backend-headless + natives-desktop                                 |
| GitHub Actions CI | android-build.yml (테스트 → APK 빌드 → 아티팩트 업로드)                |

---

> 참고: v0.1.4는 **해결 실패 기록**입니다. PC 환경에서 재시도 후 성공 시 v0.1.2로 기록 예정.
