# Puyo Puyo 2 - 남은 작업 목록 (TODO)

## 즉시 처리 필요 (Critical - P0)

| ID | 작업 | 상세 내용 | 상태 |
|------|------|----------|------|
| P0-1 | **PC 로컬 개발 환경 구축** | **JDK 17, Android SDK, NDK 설치 및 로컬 빌드/테스트/디버그 성공 - 최우선** | **계획됨** |
| P0-2 | **libpenguin.so 로드 실패 디버깅** | **PC에서 adb logcat + lldb로 실기기 연결 분석, 원인 해결** | **대기중 (PC 구축 후)** |
| P0-3 | **실기기(갤럭시 S23) 설치/실행 검증** | **PC 빌드 APK로 설치 후 검은 화면 없이 메인 메뉴 진입, 보드 정상 표시** | **대기중 (PC 구축 후)** |
| P0-4 | GitHub Actions 헤드리스 테스트 통과 | ./gradlew :core:test 통과 (6/6 테스트) | 진행중 (푸시 후 확인) |
| P0-5 | GitHub Actions APK 빌드 성공 | :android:assembleDebug 성공 및 아티팩트 업로드 | 대기중 |

---

## 높은 우선순위 (P1 - 핵심 기능)

| ID | 작업 | 상세 내용 | 난이도 | 예상일 |
|------|------|----------|--------|--------|
| P1-1 | PlayScreen 실제 게임플레이 로직 연결 | GameWorld 연동, 뿌요 낙하/회전/충돌/매칭/연쇄 렌더링 | 높음 | 3일 |
| P1-2 | 터치/키보드 입력 완전 구현 | 드래그 이동, 탭 회전, 하드 드롭, 터치 드래그 | 높음 | 2일 |
| P1-3 | 뿌요 렌더링 (ShapeRenderer -> SpriteBatch) | 텍스처 아틀라스 + SpriteBatch 교체 | 높음 | 2일 |
| P1-4 | 엔드리스 모드 구현 | 무한 모드, 점수/레벨 시스템 | 중간 | 2일 |
| P1-5 | 연쇄/콤보 UI 표시 개선 | 연쇄 카운트, 팝업 이펙트, 점수 애니메이션 | 중간 | 1일 |
| P1-6 | 게임 오버/리트라이 플로우 | 게임 오버 감지, 리트라이/메뉴 버튼 | 중간 | 1일 |

---

## 중간 우선순위 (P2 - 확장 기능)

| ID | 작업 | 상세 내용 | 난이도 | 예상일 |
|------|------|----------|--------|--------|
| P2-1 | AssetManager + 텍스처 아틀라스 | 리소스 통합 관리, 메모리 최적화 | 낮음 | 2일 |
| P2-2 | 사운드/이펙트 시스템 | AssetManager + Sound/Pool, 연쇄/폭발 효과음 | 낮음 | 2일 |
| P2-3 | 파티클/연쇄 이펙트 | ParticleEffect 연쇄/폭발 연출 | 낮음 | 2일 |
| P2-4 | AI 컨트롤러 | 휴리스틱/몬테카를로 AI 대전 | 중간 | 3일 |
| P2-5 | 온라인 대전 | WebSocket + 매칭 서버 | 높음 | 2주 |
| P2-6 | 안드로이드 APK 서명/배포 자동화 | Keystore 관리, Play Store 업로드 | 낮음 | 1일 |

---

## 낮음 우선순위 (P3 - 편의/폴리싱)

| ID | 작업 | 상세 내용 |
|------|------|----------|
| P3-1 | 세이브/로드 시스템 | Preferences + JSON 직렬화 |
| P3-2 | 업적/도전 과제 시스템 | 연쇄 횟수, 승리 횟수 등 |
| P3-3 | 스킨/테마 시스템 | 뿌요 스킨, 배경 테마 구매/해금 |
| P3-4 | 리플레이 시스템 | 경기 리플레이 저장/재생 |
| P3-5 | 다국어 지원 (i18n) | 한국어/영어/일어 등 |
| P3-6 | 접근성 개선 | 색약 모드, 고대비 모드 |

---

## 버그/기술부채 (Bugs & Tech Debt)

| ID | 현상 | 원인 추정 | 우선순위 |
|------|------|-----------|--------|
| #1 | 헤드리스에서 Gdx.gl null -> NPE | Headless GL 컨텍스트 없음 | 테스트 우회로 해결 |
| #2 | StoryModeManager 테스트 getUnlockedStageCount() 기대값 1 vs 실제 0 | 초기 currentStageIndex=0 vs 로직 차이 | 수정 필요 |
| #3 | MenuLoader classpath 리소스 미발견 (헤드리스) | Gdx.files.classpath() 미작동 | ClassLoader 폴백 추가로 해결 |
| #4 | **APK 설치 후 즉시 종료 - libpenguin.so 못 찾음** | **APK에 libpenguin.so 포함되나 실기기에서 dlopen failed** | **Critical - PC에서 디버깅 필요** |
| #5 | **libgdx-freetype.so -> libpenguin.so 이름 변경 시도 실패** | **네이티브 코드 내부 dlopen("libpenguin.so") 호출 추정, 단일 파일로도 해결 안됨** | **미해결 - PC에서 분석 필요** |
| #6 | 로컬 Termux에서 aapt2 작동 안함 | Android SDK 빌드 툴 미설치/호환성 | **PC 이전으로 해결 예정** |
| #7 | 로컬에서 단위 테스트 실행 불가 | gdx-platform natives-desktop 네이티브 lib 없음 | **PC 이전으로 해결 예정** |

---

## 완료 기준 (Definition of Done)

| 마일스톤 | 완료 조건 |
|----------|-----------|
| v0.1.1 (NEW) | **PC 로컬 개발 환경 구축 완료 (로컬 빌드/테스트/디버그)** |
| v0.1.2 (NEW) | **libpenguin.so 로드 실패 원인 분석 및 해결 (PC에서 adb/lldb 디버깅)** |
| v0.2.0 | 실제 게임플레이 (낙하/회전/매칭/연쇄 렌더링) |
| v0.3.0 | AI 대전 + 사운드/이펙트 + 세이브/로드 |
| v0.4.0 | 온라인 대전 베타 + 랭킹/업적 |
| v1.0.0 | Play Store 출시 빌드 |

---

## 완료된 작업 (이번 세션 - v0.1.4)

| ID | 작업 | 완료일 | 비고 |
|------|------|--------|------|
| V014-1 | libgdx-freetype.so -> libpenguin.so 이름 변경 시도 (android/build.gradle) | 2026-08-01 | **실패 - 실기기에서 여전히 dlopen failed** |
| V014-2 | AndroidLauncher에서 penguin만 로드 (gdx-freetype 제거) | 2026-08-01 | **실패 - 단일 로드로도 해결 안됨** |
| V014-3 | architecture.md에 네이티브 라이브러리 처리 및 PC 이전 계획 추가 | 2026-08-01 | 문서 현행화 |
| V014-4 | progress.md, todo.md, changeLog.md 현행화 (실패 기록 포함) | 2026-08-02 | 문서 동기화 |

---

> 다음 액션: **PC 개발 환경 구축 (JDK 17, Android SDK, NDK) > 프로젝트 클론 > 로컬 빌드/테스트 > adb로 실기기 연결하여 libpenguin.so 로드 실패 디버깅 > 원인 해결 후 PlayScreen 게임플레이 구현 착수**
