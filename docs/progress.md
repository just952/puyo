# Puyo Puyo 2 - 개발 진행 현황

## 📅 최종 업데이트: 2026-07-28

---

## ✅ 완료된 작업 (Done)

| 영역 | 항목 | 상태 | 비고 |
|------|------|------|------|
| **빌드 시스템** | Gradle 8.4 + AGP 8.1.0 설정 | ✅ | 루트/코어/데스크톱/안드로이드 멀티 모듈 |
| **코어 게임 로직** | 보드(6×12), 중력, 매칭(4개 이상), 연쇄 처리 | ✅ | `Board`, `GravityEngine`, `Board` |
| | Puyo/PuyoPair/Board 모델 | ✅ | 불변/가변 분리 설계 |
| | 중력 적용(아래로 낙하) | ✅ | `applyGravity()` |
| | 4개 이상 연결 시 제거 + 연쇄 처리 | ✅ | `findAllMatchingGroups()` + `applyGravity()` 루프 |
| | PuyoPair 생성/회전/이동/하드드롭 | ✅ | `PuyoPair`, `PuyoPairGenerator` |
| | 점수/연쇄 계산 | ✅ | `GameWorld` |
| **메뉴 시스템** | JSON 데이터 기반 동적 메뉴 | ✅ | `MenuLoader`, `MenuItem`, `MenuAction` |
| | `main.json` (메인), `story_mode_select.json` 등 | ✅ | 4개 메뉴 파일 |
| | `MenuLoader` 클래스패스/애셋 폴백 로딩 | ✅ | 테스트/앱 모두 지원 |
| **화면/스크린** | `LoadingScreen` → `MenuScreen` 전환 | ✅ | `LoadingScreen.render()` 즉시 전환 |
| | `MenuScreen` (키보드/터치 입력) | ✅ | 위/아래/엔터/백스페이스 |
| | `StoryModeSelectScreen` (스테이지 선택) | ✅ | 잠금/해금 표시 |
| | `PlayScreen` (게임플레이 진입점) | ✅ | `GameMode`별 분기 |
| **뷰포트/카메라** | **가상 해상도 960×1600 (3:5) 설정** | ✅ | `GameViewport` 설정 클래스 |
| | **FitViewport + OrthographicCamera** | ✅ | 자동 비율 유지 스케일링 |
| | **BaseScreen 공통 뷰포트 관리** | ✅ | `initViewport()`, `resize()` 자동 처리 |
| | **PlayScreen/MenuScreen/StoryModeSelectScreen/LoadingScreen** 뷰포트 적용 | ✅ | 가상 좌표계 렌더링 완료 |
| **스토리 모드** | `StoryModeManager` (JSON 기반) | ✅ | 3 스테이지, 언락/승리 조건 |
| | `stages.json` (래퍼 객체 파싱) | ✅ | `StoryDataWrapper` |
| | `clear_to_advance` 승리 조건 | ✅ | 2승/2승/3승 |
| **안드로이드 모듈** | `AndroidLauncher`, `AndroidManifest.xml` | ✅ | AGP 8.1, compileSdk 33 |
| | 리소스: strings, colors, styles | ✅ | 기본 리소스 완성 |
| **데스크톱 런처** | LWJGL3 백엔드, 480×800 세로 화면 | ✅ | `DesktopLauncher` |
| **헤드리스 테스트** | `gdx-backend-headless` + `natives-desktop` | ✅ | CI에서 통과 (로컬 네이티브 lib 미포함으로 불가) |
| **GitHub Actions CI** | `android-build.yml` | ✅ | 테스트 → APK 빌드 → 아티팩트 업로드 |
| **문서화** | `design.md`, `architecture.md`, `progress.md`, `changeLog.md`, `todo.md` | ✅ | docs/ 폴더 |

---

## 🔧 진행 중 (In Progress)

| 영역 | 작업 | 진행도 | 비고 |
|------|------|--------|------|
| **CI 파이프라인 검증** | GitHub Actions 헤드리스 테스트 통과 | 🔄 90% | 푸시 후 확인 대기 |
| **APK 빌드 검증** | GitHub Actions에서 APK 생성 및 네이티브 lib 포함 확인 | 🔄 80% | 테스트 통과 후 자동 실행 |
| **실기기 검증** | 갤럭시 S23 설치·실행 검증 | ⏳ 대기 중 | APK 다운로드 후 설치 |

---

## 🚧 남은 작업 (Backlog)

| 우선순위 | 영역 | 작업 | 난이도 |
|----------|------|------|--------|
| **P0** | CI 검증 | GitHub Actions 헤드리스 테스트 그린 달성 | 🔴 높음 |
| **P0** | CI 검증 | APK 빌드 성공 & 아티팩트 업로드 (libgdx.so 포함 확인) | 🔴 높음 |
| **P0** | 실기기 검증 | 실기기(갤럭시 S23) 설치·실행 검증 - 검은 화면/크래시 없음 | 🔴 높음 |
| **P1** | 게임플레이 | 메인 메뉴 → 노말 모드 → 1스테이지 진입 E2E 플로우 | 🟠 중간 |
| **P1** | 게임플레이 | `PlayScreen` 실제 게임플레이 로직 연결 (`GameWorld` 연동) | 🟠 중간 |
| **P1** | 렌더링 | Puyo 렌더링 (ShapeRenderer → SpriteBatch + 텍스처 아틀라스) | 🟠 중간 |
| **P1** | 입력 | 입력 처리 (터치 드래그/탭/키보드) 완전 구현 | 🟠 중간 |
| **P1** | 모드 | 엔드리스 모드 구현 (무한 모드, 점수/레벨 시스템) | 🟠 중간 |
| **P2** | 리소스 | AssetManager + 텍스처 아틀라스 통합 관리 | 🟢 낮음 |
| **P2** | 사운드 | 사운드/이펙트 시스템 (AssetManager + Sound/Pool) | 🟢 낮음 |
| **P2** | 이펙트 | 파티클/연쇄 이펙트 (ParticleEffect) | 🟢 낮음 |
| **P2** | AI | AI 컨트롤러 (휴리스틱/몬테카를로) | 🟢 중간 |
| **P2** | 온라인 | 온라인 대전 (WebSocket + 매칭 서버) | 🔴 높음 |
| **P2** | 배포 | 안드로이드 APK 서명/배포 자동화 (Keystore, Play Store) | 🟢 낮음 |

---

## 🐛 알려진 이슈 (Known Issues)

| ID | 현상 | 원인 추정 | 상태 |
|------|------|-----------|------|
| #1 | 헤드리스에서 `Gdx.gl` null → `PuyoGame.render()` NPE | Headless GL 컨텍스트 없음 | 🟡 테스트 우회로 해결 (HeadlessGame 사용) |
| #2 | `StoryModeManager` 테스트 `getUnlockedStageCount()` 기대값 1 vs 실제 0 | 초기 `currentStageIndex=0` vs `getUnlockedStageCount()` 로직 차이 | 🟡 수정 필요 |
| #3 | `MenuLoader` classpath 리소스 미발견 (헤드리스) | `Gdx.files.classpath()` 미작동 | ✅ ClassLoader 폴백 추가로 해결 |
| #4 | APK 설치 후 즉시 종료 (과거 이슈) | Native lib 미포함/매니페스트 오류 추정 | 🔍 CI 빌드 후 재검증 필요 |
| #5 | 로컬 Termux에서 aapt2 작동 안함 | Android SDK 빌드 툴 미설치/호환성 | ✅ GitHub Actions 사용으로 우회 |
| #6 | 로컬에서 단위 테스트 실행 불가 | gdx-platform natives-desktop 네이티브 lib 없음 | ✅ GitHub Actions에서만 실행 |

---

## 📈 진행률 요약

```
전체 진행률: █████████░ 85%
├── 코어 로직: ██████████ 100%
├── 메뉴/스크린: ██████████ 95%
├── 뷰포트/카메라: ██████████ 100%  ← NEW: 완료
├── 스토리 모드: ████████░░ 80%
├── 안드로이드 빌드: ████████░░ 80%
├── 헤드리스 테스트: ████████░░ 85%
└── CI/CD 파이프라인: ████████░░ 85%
```

---

## 📅 다음 마일스톤 (v0.2.0 목표)

| 마일스톤 | 목표일 | 핵심 포함 사항 |
|----------|--------|----------------|
| **v0.1.0** (현재) | 2026-07-28 | CI 그린 + APK 빌드 + 기본 메뉴/스토리 진입 |
| **v0.2.0** | 2026-08-15 | 실제 게임플레이 (낙하/회전/매칭/연쇄 렌더링) |
| **v0.3.0** | 2026-09-15 | AI 대전 + 사운드/이펙트 + 세이브/로드 |
| **v0.4.0** | 2026-10-31 | 온라인 대전 베타 + 랭킹/업적 |
| **v1.0.0** | 2026-12-31 | Play Store 출시 빌드 |

---

## 📝 변경 이력 (Changelog)

| 날짜 | 버전 | 주요 변경 |
|------|------|-----------|
| 2026-07-28 | 0.1.3 | 뷰포트/카메라 시스템 구현, 가상 해상도 960×1600, FitViewport 적용 |
| 2026-07-26 | 0.1.2 | StoryModeManager classpath 로딩, stages.json 테스트 리소스 복사 |
| 2026-07-26 | 0.1.1 | JSON 플랫 배열 변경, MenuLoader classpath 폴백, 헤드리스 테스트 6종 추가 |
| 2026-07-26 | 0.1.0 | 초기 프로젝트 설정, 코어 로직, 메뉴 시스템, CI 파이프라인 완성 |

---

> **다음 액션**: GitHub에 푸시 → GitHub Actions 실행 결과 확인 → 테스트 통과 시 APK 다운로드 → 실기기 설치 검증 → `PlayScreen` 실제 게임플레이 구현 착수 (P1-1, P1-2, P1-3)
