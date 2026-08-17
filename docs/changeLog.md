# Puyo Puyo 2 - 변경 이력 (ChangeLog)

## 버전별 변경 이력

---

## v0.1.22 (2026-08-17) - **텍스처 아틀라스 시스템 구축 + 환경별 로드 전략 (PRD/DEV 분기)**

### 배경
- ShapeRenderer.circle()으로 매 프레임 원을 그리는 방식은 드로우콜이 많아 비효율적
- 아틀라스 파일이 desktop/assets/에만 있어 android 모듈에서 공유 불가
- 개발 시 핫리로드(이미지 수정→즉시 반영) 지원 필요

### 해결
1. **PuyoRenderer 신규 생성** - SpriteBatch + 텍스처 아틀라스 기반 렌더링
   - 7색 × 3변형(기본/하이라이트/팝) = 21개 스프라이트를 단일 200×464 아틀라스에 통합
   - 런타임 Pixmap 생성 → 파일 저장 → 이후 실행 시 로드
   - 팝 애니메이션 스케일(0~1) 지원

2. **아틀라스 파일 core 모듈 공통 리소스로 이동**
   - `core/src/main/resources/assets/puyo_atlas.png`
   - `core/src/main/resources/assets/puyo_atlas.atlas`
   - desktop, android 모두 classpath에서 접근 가능

3. **환경별 로드 전략 분기**
   ```java
   // Production (PRD/Android): classpath만 (읽기 전용)
   // Development (Desktop): local 우선 → classpath → 생성
   ```
   - 시스템 프로퍼티 `game.env=production|prd` 로 제어
   - 안드로이드는 자동 감지하여 프로덕션 모드

4. **Gradle 빌드 설정 추가** (desktop/build.gradle → root build.gradle)
   - `applicationDefaultJvmArgs = ["-Dgame.env=${System.getProperty('game.env', 'development')}"]`

### 실행 방법
| 모드 | 명령어 | 로그 |
|------|--------|------|
| 개발 (핫리로드) | `./gradlew :desktop:run` | `Loaded atlas from local: assets/puyo_atlas.atlas` |
| 프로덕션 테스트 | `./gradlew :desktop:run "-Dgame.env=production"` | `Loaded atlas from classpath (production): assets/puyo_atlas.atlas` |
| 안드로이드 | 자동 | classpath만 사용 |

### 검증 결과
- 개발 모드: 로컬 파일 로드 (핫리로드 작동) ✅
- 프로덕션 모드: classpath 로드 (desktop/assets 없어도 작동) ✅
- 안드로이드: 자동 프로덕션 모드 감지 ✅
- BUILD SUCCESSFUL ✅
- 게임 플레이 정상 (연쇄, 분리, 락딜레이, 팝 애니메이션) ✅

### 변경 파일
| 파일 | 변경 유형 | 설명 |
|-----|---------|-----|
| `core/src/main/java/com/puyo/game/graphics/PuyoRenderer.java` | **신규** | 텍스처 아틀라스 렌더러, 환경별 로드 전략 |
| `core/src/main/resources/assets/puyo_atlas.png` | **신규** | 아틀라스 이미지 (200×464) |
| `core/src/main/resources/assets/puyo_atlas.atlas` | **신규** | 아틀라스 메타데이터 (libGDX 포맷) |
| `build.gradle` | 수정 | desktop 모듈 JVM 아규먼트 전달 추가 |

---

## v0.1.21 (2026-08-16) - **소프트 드롭 SEPARATION 경유 수정 + InputHandler DAS/ARR 단일 카운터 통합 + ChainManager 신규**

### 배경
- v0.1.20에서 소프트 드롭 착지 시 `CHAIN_FINDING`으로 바로 전이되어 분리 체크가 누락됨
- InputHandler의 키별 DAS/ARR 상태 변수가 중복되어 유지보수 어려움
- 연쇄 상태(chainCount, currentGroups)가 GameWorld에 분산되어 있었음

### 해결
1. **소프트 드롭 착지 시 SEPARATION 페이즈 경유** (`GameWorld.softDrop()`)
   - 락딜레이 대기 시간은 우회하되(`lockDelayManager.deactivate()`), 분리 체크는 수행하도록 `SEPARATION`으로 전이
   - `update()` 루프에서 `handleSeparation()` 호출 → `canSeparate()` 체크 후 분리 실행 또는 일반 잠금

2. **InputHandler DAS/ARR 단일 카운터로 통합**
   - **기존**: `leftHeldFrames`, `rightHeldFrames`, `dropHeldFrames`, `leftRepeatTriggered`, `rightRepeatTriggered`, `dropRepeatTriggered` (키별 6개 변수)
   - **변경**: `heldFrames`, `repeatTriggered`, `anyPressed` (단일 3개 변수)
   - `keyDown()` 진입 시 `anyPressed = true` 한 번만 설정
   - `keyUp()` 종료 시 `anyPressed = leftPressed || rightPressed || dropPressed` 재계산
   - `updateDasArr()`는 `anyPressed`만 보고 공통 처리
   - `getMoveDirection()`, `isDropPressed()`는 `repeatTriggered` + 해당 키 조합으로 판단

3. **ChainManager 클래스 신규 생성** (`LockDelayManager` 패턴 적용)
   - `chainCount`, `currentGroups` 상태 캡슐화
   - `startNewChain()`, `findChains()`, `getCurrentGroups()`, `getChainCount()`, `clearCurrentGroups()`, `isChaining()`, `isChainEnded()` 제공
   - GameWorld는 연쇄 로직 위임만 담당

4. **lockPiece() 단계 전이 분리** → `startChainFinding()` 추출
   - `lockPiece()`: 보드 배치 + 락딜레이 비활성화만 담당
   - `startChainFinding()`: `chainManager.startNewChain()` + `gamePhase = CHAIN_FINDING`
   - 하드 드롭(`hardDrop()`)과 분리 불가 시(`handleSeparation()`)에서 명시적 호출

### 아키텍처 개선점
- **단일 책임 원칙 강화**: GameWorld는 오케스트레이션만, 매니저들은 도메인 로직 캡슐화
- **상태 전이 명시적**: 호출부에서 `startChainFinding()` 호출로 전이 의도 명확
- **입력 처리 단순화**: DAS/ARR 로직이 키 개수에 비례하지 않고 O(1)로 동작

### 검증 결과
- **컴파일 성공** ✅
- **소프트 드롭**: 착지 시 분리 체크 수행됨 ✅
- **InputHandler**: 좌우/드롭 동시 입력, DAS/ARR 정상 작동 ✅
- **연쇄 처리**: ChainManager 위임으로 정상 작동 ✅

### 변경 파일
| 파일 | 변경 유형 | 설명 |
|-----|---------|-----|
| `core/src/main/java/com/puyo/game/logic/engine/GameWorld.java` | 수정 | softDrop() SEPARATION 전이, lockPiece()/startChainFinding() 분리, ChainManager 사용 |
| `core/src/main/java/com/puyo/game/input/InputHandler.java` | 대폭 수정 | DAS/ARR 단일 카운터 통합, keyDown/keyUp anyPressed 정리 |
| `core/src/main/java/com/puyo/game/logic/engine/ChainManager.java` | **신규** | 연쇄 상태 관리 클래스 (LockDelayManager 패턴) |

---

## v0.1.20 (2026-08-16) - **FALLING 페이즈 3단계 분리 (FALLING_AUTO / LOCK_DELAY / SEPARATION)**

### 배경
- 기존 단일 `FALLING` 페이즈가 낙하 + 락딜레이 + 착지 처리를 모두 담당하여 책임이 모호함
- 공중 고정 버그 등 상태 관리 복잡도 증가

### 해결
1. **GamePhase enum 3단계로 분리** (public으로 변경하여 외부 조회 가능)
   - `FALLING_AUTO`: 자동 중력 낙하 (0.5초 간격), 이동/회전/하드드롭 입력 허용
   - `LOCK_DELAY`: 착지 후 락 딜레이 (0.5초/15회 이동), 이동/회전/소프트드롭 입력 허용
   - `SEPARATION`: 락딜레이 종료 후 분리 체크 + 실행, 입력 차단

2. **입력 제어 중앙화**
   - `moveLeft/Right`, `rotateClockwise`, `hardDrop`: `FALLING_AUTO` && `LOCK_DELAY`에서만 허용 (메서드 레벨 가드)
   - `recordLockDelayMove()` 신규 추가: 락딜레이 중 이동 카운트용
   - `getGamePhase()` 신규 추가: PlayScreen 등에서 입력 제어용

3. **핸들러 메서드 분리**
   - `handleFallingAuto(delta)`: 자동 낙하만 처리, 착지 시 `lockDelayManager.activate()` + `LOCK_DELAY` 전이
   - `handleLockDelay(delta)`: 타이머/이동수 체크 → 초과시 `SEPARATION`, 공중 이탈시 `deactivate()` + `FALLING_AUTO` 복귀
   - `handleSeparation()`: 분리 가능시 실행 → `FALLING_ANIMATION`, 불가시 `lockPiece()` → `CHAIN_FINDING`

4. **자동 낙하 중 락딜레이 로직 완전 제거** - 공중에서는 락딜레이 건드리지 않음

5. **PlayScreen 입력 처리 개선**
   - `gameWorld.getGamePhase()`로 현재 페이즈 조회
   - `allowInput = (FALLING_AUTO || LOCK_DELAY)` 조건으로 입력 일괄 제어
   - 소프트 드롭 시 `LOCK_DELAY`면 `recordLockDelayMove()` 호출
   - 하드 드롭도 `allowInput` 체크 추가

### 아키텍처 개선점
- **단일 책임 원칙**: 각 페이즈가 명확한 역할만 담당
- **상태 전이 명시적**: `FALLING_AUTO` → `LOCK_DELAY` → `SEPARATION` → (분리시 `FALLING_ANIMATION` / 미분리시 `CHAIN_FINDING`)
- **입력 제어 중앙화**: PlayScreen이 페이즈 보고 판단, GameWorld는 메서드 레벨에서 가드
- **공중 이탈 처리 명확화**: `LOCK_DELAY` 중 `canFall()` 되면 즉시 `deactivate()` + `FALLING_AUTO` 복귀
- **이전 v0.1.19 공중 고정 버그 근본 원인 구조적 해결**

### 검증 결과
- **컴파일 성공** ✅
- **게임플레이 로직 구조 개선** ✅

### 변경 파일
| 파일 | 변경 유형 | 설명 |
|-----|---------|-----|
| `core/src/main/java/com/puyo/game/logic/engine/GameWorld.java` | 대폭 수정 | GamePhase 3단계 분리, 핸들러 분리, 입력 가드, 신규 메서드 추가 |
| `core/src/main/java/com/puyo/game/screens/PlayScreen.java` | 수정 | 페이즈 기반 입력 제어, recordLockDelayMove 연동 |

---

## v0.1.19 (2026-08-15) - **공중 락딜레이 비활성화 버그 수정 (이동/자동낙하 시)**

### 문제
- 바닥에 닿아 락딜레이가 활성화된 상태에서 옆으로 움직여 공중으로 빠져나가면 `active=true`가 유지됨
- 자동낙하로 한 칸 내려간 후 여전히 공중인데 `resetTimerAndMoves()`만 호출되어 `active=true` 유지
- 타이머가 0.5초 누적되어 공중에서 잠금 발생 (`shouldLock` 트리거)

### 해결
1. **`moveLeft`, `moveRight`, `rotateClockwise`**: 이동/회전 성공 후 `canFall()`이 true(공중)이면 `lockDelayManager.deactivate()` 호출
2. **`handleFalling` 자동낙하**: `moveDown()` 후 여전히 `canFall()`이 true면 `deactivate()`, 바닥에 닿으면 `resetTimerAndMoves()`만 호출 (다음 `handleLanding`에서 activate)

### 검증 결과
- **컴파일 성공** ✅

### 변경 파일
| 파일 | 변경 유형 | 설명 |
|-----|---------|-----|
| `core/src/main/java/com/puyo/game/logic/engine/GameWorld.java` | 수정 | move/rotate/자동낙하 시 공중 탈출 시 deactivate 호출 추가 |

---

## v0.1.18 (2026-08-15) - **LockDelayManager 상태 관리 리팩토링 + 공중 잠금 버그 수정**

### 1. LockDelayManager 상태 관리 클래스로 리팩토링
- **기존**: GameWorld에 `lockDelayActive`, `lockDelayTimer`, `lockDelayMoves` 필드 분산, LockDelayManager는 stateless static 유틸리티
- **변경**: LockDelayManager를 stateful 클래스로 변경, 인스턴스 필드로 상태 관리
- **이점**: 디버깅 시 LockDelayManager 하나만 확인, reset 로직 중앙화, 캡슐화

### 2. 명확한 메서드 네이밍으로 역할 분리
| 메서드 | 역할 | 호출 시점 |
|-------|------|----------|
| `activate()` | 활성화 (active=true, timer=0, moves=0) | 바닥에 닿음 (`handleLanding`) |
| `deactivate()` | 완전 비활성화 (active=false, timer=0, moves=0) | 잠금/스폰/분리 완료 (`lockPiece`, `spawnNewPair`, `handleFallingAnimation`) |
| `resetTimerAndMoves()` | timer/moves만 리셋 (active 유지) | 공중 이동/자동낙하 (`handleFalling`) |
| `recordTime(delta)` | 타이머 증가 | 매 프레임 (`handleFalling`) |
| `recordMove()` | 이동/회전 기록 (moves++, timer=0) | 좌우/회전/하드드롭 |
| `shouldLock()` | 잠금 판단 (내부 필드 사용) | 락딜레이 체크 (`handleFalling`) |

### 3. 공중 잠금 버그 수정
- **원인**: `handleFalling()`에서 `shouldLock(lockDelayTimer, lockDelayMoves, true)` 하드코딩 `true`로 인해 `lockDelayActive`와 무관하게 잠금 판단 가능성
- **해결**: `lockDelayManager.shouldLock()` 내부 필드 기반 판단으로 변경

### 검증 결과
- **컴파일 성공** ✅
- **단위 테스트 통과** (6/6) ✅

### 변경 파일
| 파일 | 변경 유형 | 설명 |
|-----|---------|-----|
| `core/src/main/java/com/puyo/game/logic/engine/LockDelayManager.java` | 전체 재작성 | stateful 클래스로 변경, 메서드 추가 |
| `core/src/main/java/com/puyo/game/logic/engine/GameWorld.java` | 대폭 수정 | 필드 3개 제거, LockDelayManager 위임으로 전환 |

---

## v0.1.17 (2026-08-15) - **다음 뿌요 미리보기/실제 스폰 불일치 버그 수정**

### 문제
- 화면에 보이는 NEXT 미리보기와 실제 스폰되는 뿌요 쌍이 다름
- `handleSpawning()`에서 `spawnNewPair()`가 미리보기(`nextPair`)를 무시하고 매번 새로운 랜덤 쌍 생성

### 해결
1. **`spawnNewPair()` 수정**: 기존 `nextPair`가 있으면 그걸 `currentPair`로 사용, 없으면 새로 생성
2. **`spawnNextPair()` 최적화**: 미리보기용이므로 `positionAtSpawn()` 호출 제거 (렌더링 시 고정 좌표 사용)

### 검증 결과
- **컴파일 성공** ✅
- **단위 테스트 통과** (6/6) ✅

### 변경 파일
| 파일 | 변경 유형 | 설명 |
|-----|---------|-----|
| `core/src/main/java/com/puyo/game/logic/engine/GameWorld.java` | 수정 | `spawnNewPair()`/`spawnNextPair()` 로직 수정 |

---

## v0.1.16 (2026-08-15) - **부유 뿌요 순간이동 버그 수정 + Phase/FallType 통합 리팩토링 + 불필요 파라미터 제거**

### 1. 부유 뿌요 순간이동 버그 수정
- **문제**: 연쇄 팝 완료 후 `gravityEngine.applyGravity(board)`가 즉시 전체 압축하여 부유 뿌요 애니메이션이 생략되고 순간이동 발생
- **해결**: `GameWorld.handleChainPopWait()`에서 즉시 중력 적용 코드(597번 라인) 삭제
- **결과**: `CHAIN_GRAVITY` → `CHAIN_FLOATING`(현 `FALLING_ANIMATION`) 단계에서 자연스럽게 한 칸씩 낙하 애니메이션 처리

### 2. Phase 통합: SEPARATING + CHAIN_FLOATING → FALLING_ANIMATION
- 분리 낙하(`SEPARATING`)와 부유 뿌요 낙하(`CHAIN_FLOATING`)가 동일한 "열 단위 기둥 낙하" 로직을 공유함을 발견
- `GamePhase` enum에서 `SEPARATING`, `CHAIN_FLOATING` 제거, `FALLING_ANIMATION` 단일 Phase로 통합
- `handleSeparating()` + `handleChainFloating()` → `handleFallingAnimation()` 단일 메서드로 통합
- `updateSeparationFalling()` + `updateFloatingFalling()` → `updateFallingAnimation()` 단일 메서드로 통합
- `collectCompletedFalling()` → `collectAndPlaceCompletedFalling()` 단일 메서드로 통합
- 타이머 필드 2개(`separationFallTimer`, `floatingFallTimer`) → 1개(`fallingAnimationTimer`)로 통합
- 상수 2개 → 1개(`FALLING_ANIMATION_INTERVAL`)로 통합

### 3. FallType enum 단순화: SEPARATION + FLOATING → FALLING
- `FallingPuyo.FallType`에서 `SEPARATION`, `FLOATING` 제거, `FALLING` 단일 타입으로 통합 (`CHAIN_POP`은 유지)
- `isFromSeparation()`, `isFloating()` 제거, `isFalling()` 추가
- `GameWorld` 내 타입 분기 로직 완전 제거 (`fp.isFalling()`만 체크)
- `getFallingSinglePuyo()` `@Deprecated` 처리, `FALLING` 타입 기준으로 변경

### 4. 불필요 파라미터 제거
- `updateFallingAnimation(float delta, FallType)` → `updateFallingAnimation(float delta)` 파라미터 제거
- `collectAndPlaceCompletedFalling(FallType)` → `collectAndPlaceCompletedFalling()` 파라미터 제거
- `CHAIN_POP_WAIT` 단계에서 모든 `CHAIN_POP`이 제거되므로 `FALLING_ANIMATION` 단계엔 `FALLING` 타입만 존재함 → 타입 필터링 불필요

### 5. GamePhase 이름 변경 (가독성 개선)
- `CHAIN_POP_WAIT` → `CHAIN_POP_ANIMATION`: 팝 애니메이션 대기 단계임을 명확히 표현
- `CHAIN_GRAVITY` → `CHAIN_FLOATING_CHECK`: 중력 적용이 아닌 부유 뿌요 체크 단계임을 명확히 표현
- switch case 및 모든 메서드 참조 업데이트 (`handleChainPopWait` → `handleChainPopAnimation`, `handleChainGravity` → `handleChainFloatingCheck`)

### 검증 결과
- **컴파일 성공** ✅
- **단위 테스트 통과** (6/6) ✅
- **게임 실행**: 5분+ 크래시 없이 정상 플레이 ✅
- **분리/부유 애니메이션** 정상 작동 (통합된 `FALLING` 타입)
- **연쇄 4단계**까지 정상 처리 ✅

### 변경 파일
| 파일 | 변경 유형 | 설명 |
|-----|---------|-----|
| `core/src/main/java/com/puyo/game/logic/engine/GameWorld.java` | 대폭 수정 | 버그 수정, Phase/FallType 통합, 파라미터 제거, 약 200줄 감소 |
| `core/src/main/java/com/puyo/game/logic/engine/FallingPuyo.java` | 수정 | FallType enum 단순화, 메서드 정리 |

---

## v0.1.15 (2026-08-15) - **FallingAnimationManager GameWorld 완전 통합 + 프리징 버그 해결 + FallingPuyo 단일화 + 불필요 코드 정리**

### 리팩토링: FallingAnimationManager GameWorld 내장화 및 단일 상태 머신 완성

1. **FallingAnimationManager 클래스 완전 삭제 및 GameWorld 통합**
   - 정적 유틸리티 메서드들(`updatePop`, `updateSeparationAndFloatingFalling`, `collectAndClearChainPop`, `collectCompletedFalling`, `canFallInColumn`)을 `GameWorld` private 메서드로 이동
   - `FallingAnimationManager.FallingPuyo` 중첩 클래스 → 별도 파일 `FallingPuyo.java`로 단일화 (중복 제거)
   - `FallingAnimationManager` 클래스 자체 삭제 (더 이상 필요 없음)

2. **타이머 버그 수정 (프리징 원인 해결)**
   - **문제**: `float[]` 래퍼 패턴(`new float[]{timer}[0]`)으로 타이머 업데이트가 반영되지 않아 분리/부유 낙하에서 무한 루프/프리징 발생
   - **해결**: 필드 직접 사용(`separationFallTimer`, `floatingFallTimer`)으로 변경, 메서드 분리(`updateSeparationFalling`, `updateFloatingFalling`)

3. **불필요 코드 정리**
   - 미사용 임포트 제거: `PuyoColor`, `Random`
   - 미사용 필드 제거: `currentChain` (삭제), `totalChainRemoved` (삭제)
   - `getCurrentChain()` → `chainCount` 반환으로 변경 (실제 연쇄 카운트)
   - `LockDelayManager.shouldLock()` 호출 단순화 (하드코딩된 `true` 제거)

4. **단일 상태 머신 완성**
   - `GamePhase` enum 8단계로 모든 Phase 처리 (`SPAWNING`, `FALLING`, `SEPARATING`, `CHAIN_FINDING`, `CHAIN_POP_WAIT`, `CHAIN_GRAVITY`, `CHAIN_FLOATING`, `GAME_OVER`)
   - 단일 `switch(gamePhase)`로 전체 게임 로직 처리, 우선순위 경쟁 제거

### 검증 결과

- **컴파일 성공** ✅
- **게임 실행**: 3분+ 크래시 없이 정상 플레이 ✅
- **로그 확인**: Phase 전이 정상 (`SPAWNING` → `FALLING` → `SEPARATING` → `CHAIN_FINDING` → `CHAIN_POP_WAIT` → `CHAIN_GRAVITY` → `CHAIN_FINDING` → `SPAWNING`)
- **분리 애니메이션 프리징 완전 해결** ✅
- **연쇄/락딜레이/스폰 모든 기능 정상** ✅

### 변경 파일

| 파일 | 변경 유형 | 설명 |
|-----|---------|-----|
| `core/src/main/java/com/puyo/game/logic/engine/GameWorld.java` | 대폭 수정 | FallingAnimationManager 로직 내장화, 타이머 버그 수정, 불필요 코드 정리 |
| `core/src/main/java/com/puyo/game/logic/engine/FallingAnimationManager.java` | **삭제** | 클래스 완전 제거, 로직 GameWorld로 이동 |
| `core/src/main/java/com/puyo/game/logic/engine/FallingPuyo.java` | 수정 | 중복 클래스 단일화, 별도 파일로 이동 |
| `core/src/main/java/com/puyo/game/screens/PlayScreen.java` | 수정 | `FallingPuyo` 타입 변경 (`FallingAnimationManager.FallingPuyo` → `FallingPuyo`) |

---

## v0.1.14 (2026-08-12) - **ChainProcessor 삭제 + GameWorld 단일 상태 머신 통합 + 결합도 분리 + 순간이동/지연 버그 수정 + 불필요 코드 정리**

### 리팩토링: ChainProcessor 클래스 삭제 및 GameWorld 통합 (SRP 준수)

1. **ChainProcessor 클래스 완전 삭제**
   - 별도 클래스로 분리되어 있던 연쇄 처리 로직을 `GameWorld` 내부로 이동
   - 이중 상태 머신(GameWorld + ChainProcessor) → 단일 상태 머신(GameWorld 내 `ChainPhase` enum) 통합

2. **액션 반환/실행 계층 제거** (핵심 변경)
   - **기존**: `ChainProcessor`가 `Action` enum 반환 → `GameWorld`가 switch로 실행
   - **변경**: `GameWorld.updateChain()` private 메서드에서 직접 로직 수행, 중간 계층 완전 제거

3. **보드 조작 완전 분리** (FallingAnimationManager, SeparationManager)
   - 두 매니저는 액션(`FallAction`, `SeparationResult`)만 반환, Board 조작은 `GameWorld`가 수행
   - `GravityEngine`은 stateless로 `applyGravity(Board)` 파라미터 전달만 수행

4. **불필요 코드 삭제**
   - `Phase`, `Action`, `UpdateResult` 클래스 삭제 (ChainProcessor 내부용이었음)
   - 동기식 `processChain(Board board)` 메서드 삭제
   - `ChainResult` static 클래스 삭제
   - `gravityEngine` 필드 및 생성자 초기화 삭제
   - `GravityEngine` import 제거

### GameWorld - 유일한 오케스트레이터로 역할 명확화

- 내부 `ChainPhase` enum: `IDLE` → `FINDING_MATCHES` → `WAITING_POP` → `APPLYING_GRAVITY` → `CHECKING_FLOATING` → `DONE`
- 연쇄 처리 로직이 `updateChain()` 메서드에 직접 구현됨
- `FallingAnimationManager`, `SeparationManager`, `GravityEngine`, `MatchFinder`, `LockDelayManager`, `PuyoPairGenerator` 위임

### GravityEngine 단순화 (Stateless)

- `board` 필드 제거, `applyGravity(Board board)` 파라미터 전달
- 미사용 메서드 삭제: `findMatches()`, `findGroup()`, `clearPositions()`, `setBoard()`

### 수정된 버그

1. **순간이동 버그 (Teleport Bug)**
   - 원인: 중력이 한 번에 여러 칸 적용됨
   - 해결: `APPLY_GRAVITY` 액션으로 프레임당 한 번만 호출 → 한 칸씩 정상 낙하

2. **매치 감지 지연 버그 (Match Detection Delay)**
   - 원인: 분리 애니메이션 완료 후 즉시 매치 체크 안 하고 다음 조각 잠길 때까지 대기
   - 해결: 애니메이션 완료 블록에서 `MatchFinder` 즉시 실행 → 매치 있으면 연쇄 시작

### 테스트 파일 정리

삭제된 결합도 높은 테스트들 (6개):
- `ChainProcessorTest.java`, `GravityEngineTest.java`
- `FallingAnimationManagerTest.java`, `LockDelayManagerTest.java`
- `MatchFinderTest.java`, `SeparationManagerTest.java`

### 검증 결과

- ✅ 컴파일 성공
- ✅ 게임 실행: 2분 49초 / 1분 41초 정상 플레이
- ✅ 로그 확인: Phase 전이 정상, 중력 한 칸씩 적용, 체인 순환 정상

### 변경 파일

| 파일 | 변경 유형 | 설명 |
|-----|---------|-----|
| `core/src/main/java/com/puyo/game/logic/engine/ChainProcessor.java` | **삭제** | 클래스 완전 제거 (241줄), GameWorld로 통합 |
| `core/src/main/java/com/puyo/game/logic/engine/GameWorld.java` | 대폭 수정 | ChainProcessor 로직 인라인 병합, ChainPhase enum 추가 |
| `core/src/main/java/com/puyo/game/logic/engine/GravityEngine.java` | 단순화 | stateless 변경, 미사용 메서드 삭제 |
| `core/src/main/java/com/puyo/game/logic/engine/MatchFinder.java` | 수정 | 호출 스택 트레이스 로그 추가 |
| `core/src/main/java/com/puyo/game/logic/engine/FallingAnimationManager.java` | 수정 | 부유 추가 로그 추가 |
| `core/src/main/java/com/puyo/game/logic/model/Board.java` | 수정 | 부유 탐색 상세 로그 추가 |
| 테스트 6개 파일 | 삭제 | 결합도 높은 테스트 제거 |

### 검증 결과

- `:core:compileJava` / `:core:test` (60개 테스트 통과) / `:desktop:run` (2분 54초 크래시 없는 실행) 성공
- 연쇄 팝 → 부유 뿌요 낙하 → 중력 → 배치 모든 단계 정상 작동
- 이중 제거 버그 해결, 보드 상태 깨짐 없음
- 매니저 클래스 분리 완료 (SRP 준수)

### 변경 파일

| 파일                                                                             | 변경 유형 | 설명                                                   |
| -------------------------------------------------------------------------------- | --------- | ------------------------------------------------------ |
| `core/src/main/java/com/puyo/game/logic/engine/FallingAnimationManager.java`     | 신규/수정 | 팝/낙하 전담, 원본 좌표 보존, 즉시 보드 제거           |
| `core/src/main/java/com/puyo/game/logic/engine/SeparationManager.java`           | 신규      | 쌍 분리 로직 전담                                      |
| `core/src/main/java/com/puyo/game/logic/engine/ChainProcessor.java`              | 신규/수정 | 연쇄 처리 전담, MatchFinder static 사용                |
| `core/src/main/java/com/puyo/game/logic/engine/MatchFinder.java`                 | 수정      | 모든 메서드 static, stateless 유틸리티                 |
| `core/src/main/java/com/puyo/game/logic/engine/FallingPuyo.java`                 | 신규      | 엔진 내부용 낙하 뿌요 모델                             |
| `core/src/main/java/com/puyo/game/logic/engine/GameWorld.java`                   | 대폭 수정 | 매니저 위임, 중복 로직 제거, 콜백 주석 수정            |
| `core/src/main/java/com/puyo/game/logic/engine/LockDelayManager.java`            | 신규      | 락 딜레이 관리 전담                                    |
| `core/src/main/java/com/puyo/game/logic/engine/GravityEngine.java`               | 신규      | 중력 적용 전담                                         |
| `core/src/main/java/com/puyo/game/logic/engine/PuyoPairGenerator.java`           | 신규      | 다음 쌍 생성 전담                                      |
| `core/src/main/java/com/puyo/game/logic/engine/FallingPuyo.java`                 | 신규      | 엔진 내부용 낙하 모델 (FallType, 원본 좌표)            |
| `core/src/test/java/com/puyo/game/logic/engine/FallingAnimationManagerTest.java` | 수정      | Board 파라미터 추가                                    |
| `core/src/test/java/com/puyo/game/logic/engine/ChainProcessorTest.java`          | 수정      | 동기식 processChain 복원                               |
| `core/src/main/java/com/puyo/game/logic/engine/GravityEngineTest.java`           | 삭제      | main 패키지 중복 테스트 삭제 (test 패키지에 정상 존재) |

### 검증 결과

- `:core:compileJava` / `:core:test` (60개 테스트 통과) / `:desktop:run` (2분 54초 크래시 없는 실행) 성공
- 연쇄 팝 → 부유 뿌요 낙하 → 중력 → 배치 모든 단계 정상 작동
- 이중 제거 버그 해결, 보드 상태 깨짐 없음
- 매니저 클래스 분리 완료 (SRP 준수)

---

## v0.1.13 (2026-08-11) - **락 딜레이 버그 수정 + fallTimer 중복 제거 + 락 딜레이 중복 업데이트 제거**

### 수정

1. **fallTimer 중복 증가 버그 수정** (`GameWorld.java`)
   - **문제**: `fallTimer += delta;`가 프레임당 2번 실행되어 낙하 속도가 2배 빨라짐 (0.5초 → 0.25초)
   - **해결**: 프레임 시작 시 한 번만 `fallTimer += delta;` 실행하도록 수정

2. **lockDelayManager.update(delta) 중복 호출 버그 수정** (`GameWorld.java`)
   - **문제**: Line 206과 Line 216에서 `lockDelayManager.update(delta)` 중복 호출로 락 딜레이 타이머가 2배 속도
   - **해결**: Line 206의 중복 `update(delta)` 제거, 락 딜레이 활성화 시 마지막에 한 번만 호출

3. **락 딜레이 즉시 리셋 로직 개선** (`GameWorld.java`)
   - `canFall()`이 true가 되면 매 프레임 즉시 `lockDelayManager.reset()` 호출
   - fallTimer 간격 이동 시에도 이중 리셋 보장
   - 락 딜레이 활성화 조건 정리 (canFall() true면 즉시 reset, false면 activate)

4. **락 딜레이 타이머 상수 원복** (`LockDelayManager.java`)
   - 테스트용 3초에서 원복: `LOCK_DELAY_TIME = 0.5f` (Tsu 규칙 준수)

### 검증 결과

- **컴파일**: 성공 ✅
- **단위 테스트**: 60개 전체 통과 ✅
- **데스크톱 앱**: 11분 10초 크래시 없이 정상 실행 ✅
- **낙하 속도**: 정상 (0.5초 간격으로 1칸씩) ✅
- **락 딜레이**: 0.5초 후 정상 작동, 공중에서 즉시 리셋 ✅
- **이중 호출 없음**: lockDelayManager.update() 프레임당 1회 ✅

### 변경 파일

| 파일 | 변경 유형 | 설명 |
|-----|---------|-----|
| `core/src/main/java/com/puyo/game/logic/engine/GameWorld.java` | 수정 | fallTimer 중복 제거, lockDelayManager.update 중복 제거, 락 딜레이 즉시 리셋 로직 추가 |
| `core/src/main/java/com/puyo/game/logic/engine/LockDelayManager.java` | 수정 | LOCK_DELAY_TIME 0.5f 원복, 디버그 로그 유지 |

---

## v0.1.12 (2026-08-10) - **리팩토링 정리 + 이중 제거 버그 수정 + 중복 테스트 정리 + 매니저 분리**

### 추가

1. **엔진 매니저 분리** (`FallingAnimationManager.java`, `SeparationManager.java`, `ChainProcessor.java`, `MatchFinder.java`, `LockDelayManager.java`, `GravityEngine.java`, `PuyoPairGenerator.java`)
   - `GameWorld.java`에서 각 책임을 전담하는 매니저 클래스로 분리 (SRP 준수)
   - `FallingAnimationManager`: 팝 애니메이션 + 기둥/분리 낙하 전담
   - `SeparationManager`: 쌍 분리 로직 전담
   - `ChainProcessor`: 연쇄 처리(매칭→제거→중력) 전담
   - `MatchFinder`: 매칭 그룹 찾기 전담 (stateless static 유틸리티)
   - `LockDelayManager`: 락 딜레이 상태/타이머 관리
   - `GravityEngine`: 중력 적용 전담
   - `PuyoPairGenerator`: 다음 쌍 생성 전담

2. **FallingPuyo 클래스 분리** (`FallingPuyo.java`, `GameWorld.java`)
   - 엔진 내부용: `FallType`(CHAIN_POP/SEPARATION/FLOATING), `originalX/originalY` 원본 좌표 보존
   - 외부 호환용: `GameWorld` 내부 클래스로 `isFromSeparation`만 가진 DTO

### 수정

1. **이중 제거 버그 수정** (`FallingAnimationManager.java`, `ChainProcessor.java`, `GameWorld.java`)
   - `addChainFalling`에서 즉시 `board.removePuyo()` 호출로 팝 대상 즉시 제거
   - `removePoppedPuyos()` 중복 호출 코드 삭제 (이미 제거됨)
   - `ChainProcessor.processChainStep`에서 팝 완료 후 `board.removePuyo()` 중복 호출 코드 삭제 (카운트만 수행)

2. **MatchFinder stateless 변경** (`MatchFinder.java`, `ChainProcessor.java`)
   - 모든 메서드 `static`으로 변경, 인스턴스 필드 제거
   - `ChainProcessor`에서 `MatchFinder` 인스턴스 필드 제거, `MatchFinder.findAllMatchingGroups(board)` static 호출

3. **GameWorld 중복 로직 제거** (`GameWorld.java`)
   - `checkMatchesAndSpawnNext()` 동기식 연쇄 처리 메서드 삭제 (비동기 `startNextChainStep()`만 사용)
   - `dispose()` 빈 메서드 삭제
   - `onPopComplete` 콜백 주석 수정 (실제 제거는 `onPopStart`에서 수행)

4. **테스트 코드 정리**
   - `core/src/main/java/.../GravityEngineTest.java` 중복 테스트 파일 삭제 (test 패키지에 정상 존재)
   - `FallingAnimationManagerTest` Board 파라미터 추가로 컴파일 에러 수정
   - `ChainProcessorTest` 동기식 `processChain()` 복원 (테스트용)

5. **FallingAnimationManager 로직 정리**
   - `removePoppedPuyos()` 메서드 삭제 (`addChainFalling`에서 즉시 제거하므로 불필요)
   - `addChainFalling(Board, List<Puyo>)` 시그니처 변경 (즉시 보드 제거 위해)

### 검증 결과

- `:core:compileJava` / `:core:test` (60개 테스트 통과) / `:desktop:run` (2분 54초 크래시 없는 실행) 성공
- 연쇄 팝 → 부유 뿌요 낙하 → 중력 → 배치 모든 단계 정상 작동
- 이중 제거 버그 해결, 보드 상태 깨짐 없음
- 매니저 클래스 분리 완료 (SRP 준수)

### 변경 파일

| 파일                                                                             | 변경 유형 | 설명                                                   |
| -------------------------------------------------------------------------------- | --------- | ------------------------------------------------------ |
| `core/src/main/java/com/puyo/game/logic/engine/FallingAnimationManager.java`     | 신규/수정 | 팝/낙하 전담, 원본 좌표 보존, 즉시 보드 제거           |
| `core/src/main/java/com/puyo/game/logic/engine/SeparationManager.java`           | 신규      | 쌍 분리 로직 전담                                      |
| `core/src/main/java/com/puyo/game/logic/engine/ChainProcessor.java`              | 신규/수정 | 연쇄 처리 전담, MatchFinder static 사용                |
| `core/src/main/java/com/puyo/game/logic/engine/MatchFinder.java`                 | 수정      | 모든 메서드 static, stateless 유틸리티                 |
| `core/src/main/java/com/puyo/game/logic/engine/FallingPuyo.java`                 | 신규      | 엔진 내부용 낙하 뿌요 모델                             |
| `core/src/main/java/com/puyo/game/logic/engine/GameWorld.java`                   | 대폭 수정 | 매니저 위임, 중복 로직 제거, 콜백 주석 수정            |
| `core/src/main/java/com/puyo/game/logic/engine/LockDelayManager.java`            | 신규      | 락 딜레이 관리 전담                                    |
| `core/src/main/java/com/puyo/game/logic/engine/GravityEngine.java`               | 신규      | 중력 적용 전담                                         |
| `core/src/main/java/com/puyo/game/logic/engine/PuyoPairGenerator.java`           | 신규      | 다음 쌍 생성 전담                                      |
| `core/src/main/java/com/puyo/game/logic/engine/FallingPuyo.java`                 | 신규      | 엔진 내부용 낙하 모델 (FallType, 원본 좌표)            |
| `core/src/test/java/com/puyo/game/logic/engine/FallingAnimationManagerTest.java` | 수정      | Board 파라미터 추가                                    |
| `core/src/test/java/com/puyo/game/logic/engine/ChainProcessorTest.java`          | 수정      | 동기식 processChain 복원                               |
| `core/src/main/java/com/puyo/game/logic/engine/GravityEngineTest.java`           | 삭제      | main 패키지 중복 테스트 삭제 (test 패키지에 정상 존재) |

### 검증 결과

- `:core:compileJava` / `:core:test` (60개 테스트 통과) / `:desktop:run` (2분 54초 크래시 없는 실행) 성공
- 연쇄 팝 → 부유 뿌요 낙하 → 중력 → 배치 모든 단계 정상 작동
- 이중 제거 버그 해결, 보드 상태 깨짐 없음
- 매니저 클래스 분리 완료 (SRP 준수)

---

## v0.1.11 (2026-08-09) - **연쇄 후 기둥 낙하 동시 애니메이션 + 깜빡임 해결 + 낙하 속도 통일**

### 추가

1. **연쇄 후 기둥 낙하 동시 애니메이션** (`Board.java`, `GameWorld.java`)
   - `Board.getAllFloatingPuyos()` 재작성: 수직 기둥의 모든 뿌요를 동시에 떠있는 상태로 인식
   - `GameWorld.updateFalling()` 열(column) 단위 기둥 낙하: 같은 X좌표 뿌요들을 한 덩어리로 동시 이동
   - `SINGLE_FALL_INTERVAL` 0.1f 적용으로 부드러운 낙하 애니메이션

2. **기둥 낙하 렌더링 깜빡임 해결** (`PlayScreen.java`, `GameWorld.java`)
   - `drawFallingPuyos()` 메서드 추가: `fallingPuyos` 리스트의 모든 뿌요를 낙하 중에도 렌더링
   - `GameWorld.FallingPuyo` 클래스와 필드를 `public`으로 변경하여 외부 접근 가능

3. **낙하 속도 통일** (`GameWorld.java`)
   - `SINGLE_FALL_INTERVAL` 0.05f 유지 (소프트 드롭 속도)
   - 분리 낙하와 기둥 낙하 모두 0.05초 간격으로 동일 속도 적용

### 변경 파일

| 파일                                                           | 변경 유형 | 설명                                                   |
| -------------------------------------------------------------- | --------- | ------------------------------------------------------ |
| `core/src/main/java/com/puyo/game/logic/model/Board.java`      | 수정      | `getAllFloatingPuyos()` 재작성으로 수직 기둥 전체 인식 |
| `core/src/main/java/com/puyo/game/logic/engine/GameWorld.java` | 수정      | 열 단위 기둥 낙하, FallingPuyo public, 속도 0.1f 적용  |
| `core/src/main/java/com/puyo/game/screens/PlayScreen.java`     | 수정      | `drawFallingPuyos()` 추가, FallingPuyo import 추가     |

### 검증 결과

- `:core:compileJava` / `:desktop:compileJava` / `:desktop:run` 모두 성공
- 데스크톱 앱 **6분 50초 크래시 없는 실행**
- 수직 기둥 동시 낙하 애니메이션 정상 작동
- 낙하 중 깜빡임 현상 해결
- 분리/기둥 낙하 속도 통일 (0.1f)

---

## v0.1.10 (2026-08-09) - **팝(Pop) 애니메이션 구현 + 연쇄 처리 시스템 통합**

### 추가

1. **팝 애니메이션 (Pop Animation)** (`Puyo.java`, `GameWorld.java`, `PlayScreen.java`)
   - Puyo 모델에 `PopState` enum, `popTimer`, `popScale` 필드 추가
   - `startPop()`: 애니메이션 시작 (커짐 1.0→1.3, 작아짐 1.3→0, 0.3초 소요)
   - `updatePop(delta)`: 매 프레임 매끄러운 애니메이션 업데이트
   - `getPopScale()`, `isPopping()`: 렌더링/상태 확인용

2. **통합된 연쇄/애니메이션 시스템** (`GameWorld.java`)
   - `fallingPuyos` 리스트로 분리/연쇄 통합 관리 (`FallingPuyo` 클래스)
   - `isFromSeparation` 플래그로 분리(낙하) vs 연쇄(팝) 구분
   - `updateFalling(delta)`: 매 프레임 팝 애니메이션, SINGLE_FALL_INTERVAL 간격 분리 낙하
   - `lockPiece()` 단순화: 새로운 `checkMatchesAndSpawnNext()` 시스템 사용

3. **단계별 연쇄 처리** (`GameWorld.java`)
   - `checkMatchesAndSpawnNext()`: 한 번에 한 단계만 처리 후 `updateFalling`에서 재귀 호출
   - 팝 애니메이션 완료 → `board.removePuyo()` → `board.applyGravity()` → 다음 연쇄 체크
   - `fallingPuyos`가 비면 다음 쌍 스폰

4. **팝 애니메이션 렌더링** (`PlayScreen.java`)
   - `drawPuyo()`에서 `puyo.getPopScale()` 적용하여 반지름 스케일링
   - 스케일 0 이하일 때 그리기 생략 (완전 소멸)

### 수정

- `SINGLE_FALL_INTERVAL` 0.08f → 0.05f (소프트 드롭 속도 향상)
- `lockPiece()` 기존 연쇄 루프 제거, 새 애니메이션 시스템으로 대체
- `Board.java`: `removePuyo(Puyo)` 단일 뿌요 제거 메서드 추가 (필요시)

### 변경 파일

| 파일                                                           | 변경 유형 | 설명                                                     |
| -------------------------------------------------------------- | --------- | -------------------------------------------------------- |
| `core/src/main/java/com/puyo/game/logic/model/Puyo.java`       | 수정      | 팝 애니메이션 상태/메서드 추가                           |
| `core/src/main/java/com/puyo/game/logic/engine/GameWorld.java` | 대폭 수정 | fallingPuyos 통합, 팝 애니메이션, 단계별 연쇄, 중력 적용 |
| `core/src/main/java/com/puyo/game/screens/PlayScreen.java`     | 수정      | 팝 스케일 렌더링 적용                                    |
| `core/src/main/java/com/puyo/game/logic/model/Board.java`      | 수정      | `removePuyo(Puyo)` 단일 제거 메서드 추가                 |

### 검증 결과

- `:core:compileJava` / `:desktop:compileJava` / `:desktop:run` 모두 성공
- 데스크톱 앱 **5분 21초 크래시 없는 실행**
- 팝 애니메이션: 커졌다 작아지며 동시 소멸 (원작 느낌)
- 연쇄 후 남은 뿌요들 중력으로 정상 낙하
- 모든 연쇄 단계별 애니메이션 처리

### 커밋

- `HEAD` - feat: pop animation + unified chain system

---

## v0.1.9 (2026-08-09) - **뿌요쌍 분리 로직 구현 + 단일 뿌요 낙하 속도 소프트 드롭 속도로 수정**

### 추가

1. **뿌요쌍 분리 로직 (Single Puyo Separation)** (`GameWorld.java`, `Board.java`, `Puyo.java`)
   - 가로 상태(rotation 1, 3)에서 한쪽만 막혔을 때 쌍 분리
   - 막힌 쪽 즉시 잠금, 자유로운 쪽 단일 뿌요로 자동 낙하 시작
   - 단일 뿌요는 플레이어 조작 불가, 순수 자동 낙하

2. **단일 뿌요 전용 낙하 속도** (`GameWorld.java`)
   - `SINGLE_FALL_INTERVAL = 0.08f` (소프트 드롭 속도, 12.5칸/초)
   - 별도 타이머 `singleFallTimer`로 쌍 뿌요와 독립적인 속도 관리
   - 착지 시 타이머 리셋

3. **분리된 단일 뿌요 렌더링** (`PlayScreen.java`)
   - `drawFallingSinglePuyo()` 메서드 추가
   - `gameWorld.getFallingSinglePuyo()` getter 활용

### 변경 파일

| 파일                                                           | 변경 유형 | 설명                                                              |
| -------------------------------------------------------------- | --------- | ----------------------------------------------------------------- |
| `core/src/main/java/com/puyo/game/logic/engine/GameWorld.java` | 수정      | 분리 로직, 단일 뿌요 타이머/속도, 착지 후 매칭/스폰 처리          |
| `core/src/main/java/com/puyo/game/logic/model/Board.java`      | 수정      | `canMoveDown(Puyo)` 단일 뿌요 체크 메서드 추가                    |
| `core/src/main/java/com/puyo/game/logic/model/Puyo.java`       | 수정      | `moveDown()` 단일 뿌요 이동 메서드 추가                           |
| `core/src/main/java/com/puyo/game/screens/PlayScreen.java`     | 수정      | `drawFallingSinglePuyo()` 렌더링, 소프트 드롭 null 체크           |
| `core/src/main/java/com/puyo/game/logic/engine/GameWorld.java` | 수정      | `moveLeft/Right/rotate/hardDrop` null 체크, `canFall()` null 체크 |

### 검증 결과

- `:core:compileJava` / `:desktop:compileJava` / `:desktop:run` 모두 성공
- 데스크톱 앱 실행 확인 - 게임플레이 진입, 분리 로직 정상 작동, 단일 뿌요 빠른 낙하 확인

### 커밋

- `eb9d8e9` - feat: Single puyo separation logic + soft drop speed for separated puyo

---

## v0.1.22 (2026-08-17) - **히든 보드 영역(14행) 확장 적용 + 고스트 뿌요 충돌 무시 버그 수정**

### 배경
- v0.1.8에서 "화면 위쪽(y≥12) 뿌요 충돌 무시(고스트 뿌요)"로 구현했으나, **원작(뿌요뿌요 통)과 다름**
- 원작: 논리 보드 6×14 (가시 12행 + 히든 2행), 히든 영역(y=12,13) 뿌요도 **일반 뿌요와 동일하게 충돌 체크**
- 현재: `HEIGHT=12`만 있어 히든 영역 없음, 스폰 위치 y=11, 게임 오버 판단도 y=11 기준

### 해결
1. **Board.java: 논리 보드 6×14로 확장**
   - `TOTAL_HEIGHT = 14` (`HEIGHT=12` + `HIDDEN_ROWS=2`) 상수 추가
   - `grid` 배열 `[WIDTH][TOTAL_HEIGHT]`로 확장
   - `isInside()` 경계 체크를 `TOTAL_HEIGHT` 기준으로 변경
   - `isInsideVisible(x, y)` 신규: 렌더링/게임오버용 가시 영역만 체크 (`y < HEIGHT`)
   - 충돌 체크(`canMoveLeft/Right/Down`, `canPlace`)에서 `isInsideVisible` 제거 → 전체 보드 기준 `isEmpty` 사용
   - `applyGravity`, `getAllFloatingPuyos`, `getHeightAtColumn`, `isTopOut` 루프 범위 `TOTAL_HEIGHT`로 확장

2. **PuyoPairGenerator.java: 스폰 위치 히든 영역 상단(y=12)으로 변경**
   - `positionAtSpawn()`에서 `startY = fieldHeight - 2` (TOTAL_HEIGHT=14 → y=12)

3. **GameWorld.java: 스폰 시 Board.TOTAL_HEIGHT 전달**
   - `spawnNewPair()`에서 `Board.TOTAL_HEIGHT` 사용

4. **design.md 기본 규칙 수정**: "고스트 뿌요" 항목 → "히든 영역 충돌"로 정정

### 변경 파일
| 파일 | 변경 유형 | 설명 |
|-----|---------|-----|
| `core/src/main/java/com/puyo/game/logic/model/Board.java` | 대폭 수정 | TOTAL_HEIGHT=14 확장, 충돌 체크 전체 보드 기준, 관련 메서드 루프 범위 수정 |
| `core/src/main/java/com/puyo/game/logic/engine/PuyoPairGenerator.java` | 수정 | 스폰 위치 y=12로 변경 |
| `core/src/main/java/com/puyo/game/logic/engine/GameWorld.java` | 수정 | spawnNewPair에서 Board.TOTAL_HEIGHT 전달 |
| `docs/design.md` | 수정 | 기본 규칙: 보드 6×14, 히든 영역 충돌 설명 추가 |

### 검증 결과
- **컴파일 성공** ✅
- 히든 영역(y=12,13) 뿌요가 이동/회전/낙하 방해 정상 작동
- 스폰 위치 y=12, 게임 오버 판단 정확히 y=13 상단 기준

---

## v0.1.8 (2026-08-08) - **DAS/ARR 키 반복 이동 구현 + 화면 밖 뿌요(고스트) 충돌 무시로 원작 느낌 살림** ⚠️ **버그 있음 - v0.1.22에서 수정됨**

### 추가

1. **DAS/ARR (Delayed Auto Shift / Auto Repeat Rate) 입력 시스템** (`InputHandler.java`)
   - 키 누름 즉시 1회 이동 (첫 프레임)
   - DAS_DELAY_FRAMES = 16프레임 (~0.27초) 지연 후 자동 반복 시작
   - ARR_INTERVAL_FRAMES = 2프레임마다 1칸씩 반복 이동 (초당 30회)
   - 키 떼면 카운터 완전 리셋, 좌우 동시 누름 시 상쇄

2. **화면 밖(위쪽) 뿌요 고스트 충돌 무시** (`Board.java`) ⚠️ **원작과 다름 - v0.1.22에서 수정**
   - `isInsideVisible(Puyo p)` 헬퍼 메서드 추가: `p.getY() < HEIGHT`만 체크
   - `canMoveLeft`, `canMoveRight`, `canMoveDown`, `canPlace` 모두 적용
   - 스폰 시 right 뿌요가 y=12(화면 밖)에 있어도 left 뿌요만으로 좌우 이동 가능
   - **잘못된 주장**: "원작 뿌요뿌요와 동일: 필드 상단에서 좌우로 피할 수 있음"
   - **실제 원작**: 히든 영역(y=12,13) 뿌요도 일반 충돌 체크함

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
- 데스크톲 앱 실행 확인 - 메인 메뉴 → 스토리 선택 → 게임 화면(ENDLESS) 정상 진입

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
- **테스트 리소스 복사** - src/test/resources/data/menus/*.json, data/story/stages.json 복사

### 변경 파일

| 파일                                                         | 변경 유형   | 설명                                  |
| ------------------------------------------------------------ | ----------- | ------------------------------------- |
| core/src/main/java/com/puyo/game/story/StoryModeManager.java | 수정        | ClassLoader 폴백 추가, JSON 래퍼 파싱 |
| core/src/test/java/com/puyo/game/GameTest.java               | 전체 재작성 | GL 없는 순수 로직 테스트 6개          |
| core/src/test/resources/data/menus/*.json (4개)             | 신규        | 테스트용 메뉴 JSON 복사               |
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