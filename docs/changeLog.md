# Puyo Puyo 2 - 변경 이력 (ChangeLog)

## 버전별 변경 이력

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
| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| `android/build.gradle` | 수정 | `mergeNativeLibs` 후 SONAME 패치 태스크 추가 (`patch_soname.py` 호출) |
| `core/src/main/java/com/puyo/game/graphics/FontManager.java` | 수정 | `param.characters`에 필수 한글 문자 추가, 폰트 경로 `NotoSansKR-Regular.ttf`로 변경 |
| `android/src/main/java/com/puyo/game/AndroidLauncher.java` | 수정 | `System.loadLibrary("penguin")` 제거 (불필요) |
| `build.gradle` (root) | 수정 | `srcDirs = ['src/main/resources']`로 assets JAR 포함 |
| `android/build.gradle` | 수정 | `assets.srcDirs = ['src/main/assets']` 단순화, `../assets` 제거 |
| `core/src/main/resources/assets/` | 이동/추가 | 폰트, JSON 모두 core JAR의 assets 하위에 포함 |
| `patch_soname.py` | 신규 | Python lief로 SONAME 패치 스크립트 |
| `core/src/main/assets/fonts/` | 삭제 | 중복 폰트 제거 |
| `android/src/main/assets/` | 삭제 | 빈 폴더 제거 |
| 루트 `assets/` | 삭제 | 중복 에셋 제거 |
| `lib/`, `patchelf/`, `check_font.py`, `fix_deps.ps1`, `test.txt` | 삭제 | 임시 파일 정리 |

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
| 파일 | 변경 유형 | 설명 |
|------|----------|------|
| android/build.gradle | 수정 | libgdx-freetype.so → libpenguin.so 이름 변경 로직 (실패) |
| android/src/main/java/com/puyo/game/AndroidLauncher.java | 수정 | System.loadLibrary("gdx-freetype") 제거, penguin만 로드 (실패) |
| docs/architecture.md | 수정 | 네이티브 라이브러리 처리 실패 기록, PC 이전 계획 강화 |
| docs/progress.md | 수정 | 진행 현황 업데이트, 실패 기록, v0.1.1/v0.1.2 마일스톤 추가 |
| docs/todo.md | 수정 | P0-1~P0-3 재정의 (PC 구축 최우선), 완료 작업에 실패 기록 추가 |

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
- **MenuLoader** - Gdx.files.classpath() → internal() 폴백 추가
- **GameTest** - GL 컨텍스트 없는 순수 로직 테스트로 재작성 (6개 테스트)
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
- GameTest.java - 헤드리스 테스트 2종 추가

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

---

> 참고: v0.1.4는 **해결 실패 기록**입니다. PC 환경에서 재시도 후 성공 시 v0.1.2로 기록 예정.
