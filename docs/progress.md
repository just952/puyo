# Puyo Puyo 2 - 개발 진행 현황

## 최종 업데이트: 2026-08-16

---

## 완료된 작업 (Done)

| 영역                | 항목                                                                  | 상태     | 비고                                                                 |
| ------------------- | --------------------------------------------------------------------- | -------- | -------------------------------------------------------------------- |
| 빌드 시스템         | Gradle 8.4 + AGP 8.1.0 설정                                           | 완료     | 루트/코어/데스크톱/안드로이드 멀티 모듈                              |
| 코어 게임 로직      | 보드(6x12), 중력, 매칭(4개 이상), 연쇄 처리                           | 완료     | Board, GravityEngine                                                 |
|                     | Puyo/PuyoPair/Board 모델                                              | 완료     | 불변/가변 분리 설계                                                  |
|                     | 중력 적용(아래로 낙하)                                                | 완료     | applyGravity()                                                       |
|                     | 4개 이상 연결 시 제거 + 연쇄 처리                                     | 완료     | findAllMatchingGroups() + applyGravity() 루프                        |
|                     | PuyoPair 생성/회전/이동/하드드롭                                      | 완료     | PuyoPair, PuyoPairGenerator                                          |
|                     | 점수/연쇄 계산                                                        | 완료     | GameWorld                                                            |
|                     | **락 딜레이 (Lock Delay) - Tsu 규칙**                                 | **완료** | 0.5초 딜레이, 이동/회전 15회 제한, 초과 시 즉시 잠금                 |
|                     | **회전 시스템 (벽 킥 포함)**                                          | **완료** | PuyoPair 회전 + setPosition, GameWorld 벽 킥 처리                    |
|                     | **스폰 위치 통일**                                                    | **완료** | createAndPositionPair() 공통 메서드로 상단 중앙 스폰 보장            |
| 메뉴 시스템         | JSON 데이터 기반 동적 메뉴                                            | 완료     | MenuLoader, MenuItem, MenuAction                                     |
|                     | main.json, story_mode_select.json 등                                  | 완료     | 4개 메뉴 파일                                                        |
|                     | MenuLoader 클래스패스/애셋 폴백 로딩                                  | 완료     | 테스트/앱 모두 지원                                                  |
| 화면/스크린         | LoadingScreen → MenuScreen 전환                                       | 완료     | LoadingScreen.render() 즉시 전환                                     |
|                     | MenuScreen (키보드/터치 입력)                                         | 완료     | 위/아래/엔터/백스페이스                                              |
|                     | StoryModeSelectScreen (스테이지 선택)                                 | 완료     | 잠금/해금 표시                                                       |
|                     | PlayScreen (게임플레이 진입점)                                        | 완료     | GameMode별 분기                                                      |
| 뷰포트/카메라       | 가상 해상도 960x1600 (3:5) 설정                                       | 완료     | GameViewport 설정 클래스                                             |
|                     | FitViewport + OrthographicCamera                                      | 완료     | 자동 비율 유지 스케일링                                              |
|                     | BaseScreen 공통 뷰포트 관리                                           | 완료     | initViewport(), resize() 자동 처리                                   |
|                     | PlayScreen/MenuScreen/StoryModeSelectScreen/LoadingScreen 뷰포트 적용 | 완료     | 가상 좌표계 렌더링 완료                                              |
| 스토리 모드         | StoryModeManager (JSON 기반)                                          | 완료     | 3 스테이지, 언락/승리 조건                                           |
|                     | stages.json (래퍼 객체 파싱)                                          | 완료     | StoryDataWrapper                                                     |
|                     | clear_to_advance 승리 조건                                            | 완료     | 2승/2승/3승                                                          |
| 입력 시스템         | **DAS/ARR (Delayed Auto Shift / Auto Repeat Rate) 구현**              | **완료** | 원작 뿌요뿌요 방식: 16프레임 지연 후 2프레임마다 반복 이동           |
|                     | **화면 밖 뿌요(고스트) 충돌 무시**                                    | **완료** | 스폰 시 상단 뿌요만 보여도 좌우 이동 가능 (원작 방식)                |
|                     | **뿌요쌍 분리 로직 (Single Puyo Separation)**                         | **완료** | 가로 상태에서 한쪽만 막히면 분리, 단일 뿌요 자동 낙하 (0.08초/칸)    |
|                     | **단일 뿌요 낙하 속도 분리**                                          | **완료** | 별도 타이머(singleFallTimer)로 소프트 드롭 속도(0.08초/칸) 적용      |
|                     | **팝 애니메이션 (Pop Animation)**                                     | **완료** | Puyo 모델 팝 상태, updateFalling 통합, 렌더링 스케일 (0.3초)         |
|                     | **연쇄 후 기둥 낙하 동시 애니메이션**                                 | **완료** | Board.getAllFloatingPuyos() 수정, 열 단위 동시 낙하, 0.05f 속도 적용 |
|                     | **기둥 낙하 렌더링 깜빡임 해결**                                      | **완료** | drawFallingPuyos() 추가로 모든 fallingPuyos 렌더링                   |
|                     | **낙하 속도 통일 (0.05f)**                                            | **완료** | SINGLE_FALL_INTERVAL 0.05f 적용, 분리/기둥 동일 속도                 |
| 폰트/한글           | NotoSansKR-Regular.ttf 적용                                           | **완료** | Google Fonts에서 정상 파일 다운로드                                  |
|                     | 한글 깨짐(X박스) 해결                                                 | **완료** | FontManager.param.characters에 필수 문자 명시                        |
|                     | 폰트 경로 정리                                                        | **완료** | core/src/main/resources/assets/ 하나로 통합                          |
|                     | **증분 폰트 로딩 (Incremental)**                                      | **완료** | FreeTypeFontParameter.incremental=true, 동적 글리프 생성             |
| 네이티브 라이브러리 | libpenguin.so SONAME 패치                                             | **완료** | Python lief로 SONAME 'libpenguin.so' 패치                            |
|                     | libgdx-freetype.so → libpenguin.so 복사                               | **완료** | mergeNativeLibs 후 자동 복사                                         |
|                     | AndroidLauncher 단일 로드                                             | **완료** | gdx, gdx-freetype만 로드 (penguin 불필요)                            |
|                     | **안드로이드 네이티브 로드 수정**                                     | **완료** | System.loadLibrary("penguin") 제거, extractNativeLibs 제거           |
| 안드로이드 모듈     | AndroidLauncher, AndroidManifest.xml                                  | 완료     | AGP 8.1, compileSdk 33                                               |
|                     | 리소스: strings, colors, styles                                       | 완료     | 기본 리소스 완성                                                     |
|                     | 네이티브 라이브러리 패키징                                            | **완료** | libgdx.so, libpenguin.so APK에 포함, SONAME 패치됨, 실기기 로드 성공 |
| 데스크톱 런처       | LWJGL3 백엔드, 480x800 세로 화면                                      | 완료     | DesktopLauncher                                                      |
| 헤드리스 테스트     | gdx-backend-headless + natives-desktop                                | 완료     | CI/PC 로컬 모두 실행 가능                                            |
| GitHub Actions CI   | android-build.yml                                                     | 완료     | 테스트 → APK 빌드 → 아티팩트 업로드 (검증용 유지)                    |
| 문서화              | design.md, architecture.md, progress.md, changeLog.md, todo.md        | 완료     | docs/ 폴더                                                           |
| 에셋 구조 정리      | assets 폴더 중복 제거                                                 | **완료** | core/src/main/resources/assets/ 단일 소스                            |
|                     | 중복 폰트/JSON 제거                                                   | **완료** | 폰트 1개, JSON 모두 core로 이동                                      |
| CI/CD 검증          | GitHub Actions 헤드리스 테스트 통과                                   | **완료** | PC 로컬에서도 실행 가능                                              |
|                     | APK 빌드 검증                                                         | **완료** | libpenguin.so 정상 포함, SONAME 패치됨                               |
| 실기기 검증         | 갤럭시 S23 설치/실행 검증                                             | **완료** | PC 빌드 APK로 정상 실행 확인 (2026-08-03)                            |
| 네이티브 로드       | libgdx.so, libpenguin.so 로드 성공                                    | **완료** | 로그에 크래시 없음, 한글 정상 표시 (2026-08-03)                      |
| 게임플레이 구현     | PlayScreen 실제 게임플레이 로직 연결                                  | **완료** | GameWorld 연동, 뿌요 낙하/회전/매칭/연쇄 렌더링 (2026-08-06)         |
| 뷰포트/레이아웃     | 가로 고정 1600×960 + 모드별 레이아웃                                  | **완료** | GameViewport 전면 재작성, 단일 가로 해상도 (2026-08-06)              |
| 터치 컨트롤러       | 4버튼 레이아웃 (좌/우/회전/드롭+하드드롭)                             | **완료** | TouchController + InputHandler 통합 (2026-08-06)                     |
| 모바일 가로 고정    | AndroidManifest landscape + 몰입 모드                                 | **완료** | 적용됨 (2026-08-06 확인)                                             |
| 데스크톱 가로 고정  | DesktopLauncher 1600×960 창 크기, 비율 유지                           | **완료** | (2026-08-06 완료)                                                    |
| **엔진 리팩토링**   | **ChainProcessor 삭제, GameWorld 단일 상태 머신 통합**                | **완료** | v0.1.14 (2026-08-12), 결합도 분리, 순간이동/지연 버그 수정           |
| **보드 조작 분리**  | **액션 기반 패턴으로 GameWorld가 유일한 오케스트레이터**              | **완료** | v0.1.14, FallingAnimationManager/SeparationManager 보드 조작 없음    |
| **불필요 코드 정리**| **동기식 processChain, ChainResult, gravityEngine 필드, 결합도 높은 테스트 삭제** | **완료** | v0.1.14, 100+ 줄 정리                                                 |
| **버그 수정**      | **부유 뿌요 순간이동 버그 수정**                                     | **완료** | v0.1.16 (2026-08-15), 즉시 중력 적용 제거로 자연스러운 애니메이션 복구 |
| **Phase 통합**    | **SEPARATING + CHAIN_FLOATING → FALLING_ANIMATION**                  | **완료** | v0.1.16, 중복 낙하 로직 단일화, 약 200줄 감소                         |
| **FallType 단순화**| **SEPARATION + FLOATING → FALLING**                                  | **완료** | v0.1.16, enum/메서드/분기 로직 정리                                   |
| **파라미터 제거**  | **updateFallingAnimation, collectAndPlaceCompletedFalling 단순화**    | **완료** | v0.1.16, 타입 필터링 불필요 제거                                      |
| **GamePhase 이름 변경** | **CHAIN_POP_WAIT→CHAIN_POP_ANIMATION, CHAIN_GRAVITY→CHAIN_FLOATING_CHECK** | **완료** | v0.1.16, Phase 역할 명확화로 가독성 개선                                |
| **버그 수정**      | **공중 락딜레이 비활성화 버그 수정**                                 | **완료** | v0.1.19 (2026-08-15), 이동/자동낙하 시 공중 탈출 시 deactivate 호출 |
| **GamePhase 3단계 분리** | **FALLING → FALLING_AUTO / LOCK_DELAY / SEPARATION**              | **완료** | v0.1.20 (2026-08-16), 단일 책임 원칙, 명시적 상태 전이, 입력 제어 중앙화 |
| **소프트 드롭 수정** | **착지 시 SEPARATION 페이즈 경유 (락딜레이 우회하되 분리 체크)**     | **완료** | v0.1.21 (2026-08-16), 분리 가능 시 실행, 불가 시 일반 잠금            |
| **InputHandler 리팩토링** | **DAS/ARR 단일 카운터 통합 (6개→3개 변수)**                       | **완료** | v0.1.21 (2026-08-16), anyPressed 플래그로 키 개수 무관 O(1) 처리     |
| **ChainManager 신규** | **연쇄 상태 캡슐화 (LockDelayManager 패턴)**                          | **완료** | v0.1.21 (2026-08-16), chainCount/currentGroups 관리                  |
| | **텍스처 아틀라스 전환** | **ShapeRenderer → SpriteBatch + 텍스처 아틀라스**                   | **완료** | v0.1.22 (2026-08-17), 런타임 생성→파일 저장/로드, core/assets 이동   |
| | **아틀라스 환경별 로드** | **PRD=classpath, DEV=local 우선 (핫리로드)**                        | **완료** | v0.1.22 (2026-08-17), -Dpuyo.env=production 으로 제어                 |

---

## 진행 중 (In Progress)

| 영역                 | 작업 | 진행도 | 비고 |
| -------------------- | ---- | ------ | ---- |
| (진행중인 작업 없음) | -    | -      | -    |

---

## 남은 작업 (Backlog)

| 우선순위 | 영역       | 작업                                                        | 난이도 |
| -------- | ---------- | ----------------------------------------------------------- | ------ |
| P1       | 게임플레이 | 메인 메뉴 → 노말 모드 → 1스테이지 진입 E2E 플로우           | 중간   |
| P1       | 게임플레이 | PlayScreen 실제 게임플레이 로직 연결 (GameWorld 연동)       | 중간   |
| P1       | 렌더링     | Puyo 렌더링 (ShapeRenderer → SpriteBatch + 텍스처 아틀라스) | 중간   |
| P1       | 입력       | 입력 처리 (터치 드래그/탭/키보드) 완전 구현                 | 중간   |
| P1       | 모드       | 엔드리스 모드 구현 (무한 모드, 점수/레벨 시스템)            | 중간   |
| P2       | 리소스     | AssetManager + 텍스처 아틀라스 통합 관리                    | 낮음   |
| P2       | 사운드     | 사운드/이펙트 시스템 (AssetManager + Sound/Pool)            | 낮음   |
| P2       | 이펙트     | 파티클/연쇄 이펙트 (ParticleEffect)                         | 낮음   |
| P2       | AI         | AI 컨트롤러 (휴리스틱/몬테카를로)                           | 중간   |
| P3       | 온라인     | 온라인 대전 (WebSocket + 매칭 서버)                         | 높음   |
| P3       | 배포       | 안드로이드 APK 서명/배포 자동화 (Keystore, Play Store)      | 낮음   |

---

## 알려진 이슈 (Known Issues)

| ID  | 현상                                                               | 원인 추정                             | 상태                                   |
| --- | ------------------------------------------------------------------ | ------------------------------------- | -------------------------------------- |
| #1  | 헤드리스에서 Gdx.gl null -> PuyoGame.render() NPE                  | Headless GL 컨텍스트 없음             | 테스트 우회로 해결 (HeadlessGame 사용) |
| #2  | StoryModeManager 테스트 getUnlockedStageCount() 기대값 1 vs 실제 0 | 초기 currentStageIndex=0 vs 로직 차이 | 수정 필요                              |
| #3  | MenuLoader classpath 리소스 미발견 (헤드리스)                      | Gdx.files.classpath() 미작동          | ClassLoader 폴백 추가로 해결           |

---

## 진행률 요약

전체 진행률: **85%**

- 코어 로직: 100%
- 메뉴/스크린: 95%
- 뷰포트/카메라: 100%
- 스토리 모드: 80%
- 안드로이드 빌드: **95%** (네이티브 로드/폰트/에셋 모두 해결)
- 헤드리스 테스트: 100%
- CI/CD 파이프라인: 100%
- 로컬 개발 환경: **100%** (PC 환경 구축 완료)
- 실기기 검증: **100%** (갤럭시 S23 정상 실행 확인)

---

## 다음 마일스톤 (v0.2.0 목표)

| 마일스톤 | 목표일         | 핵심 포함 사항                                   |
| -------- | -------------- | ------------------------------------------------ |
| v0.2.0   | **2026-08-15** | **실제 게임플레이 (낙하/회전/매칭/연쇄 렌더링)** |
| v0.3.0   | 2026-08-31     | AI 대전 + 사운드/이펙트 + 세이브/로드            |
| v0.4.0   | 2026-09-30     | 온라인 대전 베타 + 랭킹/업적                     |
| v1.0.0   | 2026-10-31     | Play Store 출시 빌드                             |

---

## 변경 이력 (Changelog)

| 날짜       | 버전  | 주요 변경                                                                                                                             |
| ---------- | ----- | ------------------------------------------------------------------------------------------------------------------------------------- |
| 2026-08-16 | 0.1.21 | **소프트 드롭 SEPARATION 경유 수정 + InputHandler DAS/ARR 단일 카운터 통합 + ChainManager 신규**                                     |
| 2026-08-16 | 0.1.20 | **FALLING 페이즈 3단계 분리 (FALLING_AUTO / LOCK_DELAY / SEPARATION) - 단일 책임, 명시적 상태 전이, 입력 제어 중앙화**               |
| 2026-08-15 | 0.1.19 | **공중 락딜레이 비활성화 버그 수정 (이동/자동낙하 시)**                                                                               |
| 2026-08-15 | 0.1.18 | **LockDelayManager 상태 관리 리팩토링 + 공중 잠금 버그 수정**                                                                       |
| 2026-08-15 | 0.1.17 | **다음 뿌요 미리보기/실제 스폰 불일치 버그 수정**                                                                                     |
| 2026-08-15 | 0.1.16 | **부유 뿌요 순간이동 버그 수정 + Phase/FallType 통합 리팩토링 + 불필요 파라미터 제거**                                               |
| 2026-08-15 | 0.1.15 | **FallingAnimationManager GameWorld 완전 통합 + 프리징 버그 해결 + FallingPuyo 단일화 + 불필요 코드 정리**                          |
| 2026-08-12 | 0.1.14 | **ChainProcessor 상태 머신 리팩토링 + 결합도 분리 + 순간이동/지연 버그 수정 + 불필요 코드 정리**                                     |
| 2026-08-08 | 0.1.8 | **DAS/ARR 키 반복 이동 구현 + 화면 밖 뿌요(고스트) 충돌 무시로 원작 느낌 살림**                                                       |
| 2026-08-07 | 0.1.7 | **락 딜레이(Tsu 규칙) 완전 구현, 회전 버그 수정, 다음 블록 스폰 버그 수정, 폰트 증분 로딩, 안드로이드 네이티브 라이브러리 로드 수정** |
| 2026-08-03 | 0.1.5 | **PC 로컬 환경에서 libpenguin.so SONAME 패치 성공, 한글 폰트 정상 적용, 실기기(갤럭시 S23) 정상 실행 확인, 에셋 구조 정리 완료**      |
| 2026-08-02 | 0.1.4 | libpenguin.so 실기기 로드 실패 확인, PC 개발 환경 이전 결정                                                                           |
| 2026-08-01 | 0.1.3 | libgdx-freetype.so → libpenguin.so 이름 변경 시도, AndroidLauncher 단일 로드 수정 (해결 안됨)                                         |
| 2026-07-28 | 0.1.2 | 뷰포트/카메라 시스템 구현, 가상 해상도 960×1600, FitViewport 적용                                                                     |
| 2026-07-27 | 0.1.1 | 헤드리스 테스트 안정화 & 리소스 로딩 개선                                                                                             |
| 2026-07-26 | 0.1.0 | 초기 프로젝트 설정, 코어 로직, 메뉴 시스템, CI 파이프라인 완성                                                                        |

---

> **다음 액션**: **P1-7 뿌요 렌더링 SpriteBatch+아틀라스 전환** (텍스처 아틀라스 생성, ShapeRenderer → SpriteBatch 교체)