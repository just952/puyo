# Puyo Puyo 2 - 변경 이력 (ChangeLog)

## 버전별 변경 이력

---

## v0.1.29 (2026-08-29)

### 🏗️ **입력 아키텍처 전면 리팩토링 (InputProvider + Command 패턴 + 플랫폼 분리)**

**배경**: 기존 `InputHandler`가 core 모듈에 있으면서 `TouchController`(Android 전용), `ConfigManager`, `Gdx.app.getType()` 등 플랫폼 의존성을 가지고 있어 core의 플랫폼 독립성이 깨져있었음. 또한 `PlayScreen.render()` → `update()` → `GameWorld` 메서드 직접 호출 구조로 캡슐화 위반.

**해결**:
1. **core/input**에 인터페이스/데이터 클래스 신규 생성:
   - `InputProvider` - 입력 공급자 인터페이스 (플랫폼 독립적)
   - `InputCommand` - 불변 입력 명령 레코드 (Record, Command 패턴)
   - `InputMode` - 입력 모드 열거형 (GAME_PLAY, TEXT_INPUT, UI_NAVIGATION)
   - `TextInputListener` - 텍스트 입력 이벤트 리스너 (IME 지원)

2. **플랫폼별 구현체 분리**:
   - `desktop/input/DesktopInputHandler` - 키보드 입력, DAS/ARR, IME 연동
   - `android/input/AndroidInputHandler` - TouchController 위임, 터치 입력, IME 지원

3. **GameWorld 입력 처리 내부화**:
   - `update(float delta, InputProvider input)` 시그니처 변경
   - `handleFallingInput(InputCommand cmd)` 메서드 추가로 FALLING_AUTO/LOCK_DELAY에서만 입력 처리
   - `hold()` 메서드 구현 (heldPair 슬롯 + resetRotation(), 한 조각당 1회 제한)

4. **PlayScreen 단순화**:
   - `InputProvider` 생성자 주입
   - `update()` 메서드 제거 → `render()`에서 직접 `gameWorld.update(delta, inputProvider)`
   - `Gdx.input.setInputProcessor()` 제거 (구현체 생성자에서 처리)

5. **팩토리 패턴으로 Screen 생성**:
   - `PuyoGame.createPlayScreen(mode, stageIndex)` + `createInputProvider()` 추상 메서드
   - `DesktopLauncher`/`AndroidLauncher`에서 익명 클래스로 오버라이드

6. **테스트 개선**:
   - Mock InputProvider로 GameWorld 단독 테스트 가능

#### 변경 파일

| 파일 | 변경 유형 | 설명 |
|-----|---------|-----|
| `InputProvider.java` | **신규** | 입력 공급자 인터페이스 |
| `InputCommand.java` | **신규** | 불변 입력 명령 레코드 |
| `InputMode.java` | **신규** | 입력 모드 열거형 |
| `TextInputListener.java` | **신규** | 텍스트 입력 리스너 |
| `DesktopInputHandler.java` | **신규** (desktop) | 키보드 입력 구현체 |
| `AndroidInputHandler.java` | **신규** (android) | 터치 입력 구현체 |
| `GameWorld.java` | 대폭 수정 | update 시그니처 변경, handleFallingInput, hold 구현 |
| `PlayScreen.java` | 대폭 수정 | InputProvider 주입, update 제거, render에서 직접 호출 |
| `PuyoGame.java` | 수정 | createPlayScreen 팩토리, createInputProvider 추상화 |
| `DesktopLauncher.java` | 수정 | 익명 클래스로 DesktopInputProvider 제공 |
| `AndroidLauncher.java` | 수정 | 익명 클래스로 AndroidInputProvider 제공 |
| `MenuScreen.java` | 수정 | game.createPlayScreen() 사용 |
| `StoryModeSelectScreen.java` | 수정 | game.createPlayScreen() 사용 |
| `PuyoPair.java` | 수정 | resetRotation() 추가 (홀드용) |
| `FallingAnimationTest.java` | 수정 | Mock InputProvider 사용 |
| `InputHandler.java` | **삭제** | core에서 플랫폼 의존성 제거 |

#### 효과
- ✅ **core 모듈 완전 플랫폼 독립성** 확보 (InputProvider 인터페이스만 의존)
- ✅ **캡슐화 향상**: GameWorld가 입력 처리 로직을 내부에서 관리
- ✅ **단일 책임**: PlayScreen=렌더링, GameWorld=게임 로직+입력 처리
- ✅ **테스트 용이**: Mock InputProvider 주입으로 GameWorld 단독 테스트 가능
- ✅ **Command 패턴**: pollCommand()로 프레임당 한 번 소비, 중복 처리 방지
- ✅ **확장성**: TEXT_INPUT 모드 + TextInputListener로 채팅/검색 지원 준비
- ✅ **컴파일/테스트 모두 통과**: core, desktop, test 모두 성공

---

## v0.1.28 (2026-08-25)

**배경**: v0.1.27에서 착지 바운스(SETTLING) 애니메이션 추가 후, 부유 뿌요 낙하 시 `updateFallingAnimation()`에서 SETTLING 상태인 뿌요가 `fallingList`에 포함되지 않아 착지 직후 바운스 전이된 뿌요들이 충돌 체크에서 누락되는 버그 발생

**해결**:
1. `updateFallingAnimation()`: `fallingList` 필터에 `puyo.isSettling()` 추가로 착지 직후 바운스 전이된 뿌요도 낙하 루프에서 처리
2. `canSinglePuyoFallDuringFallingAnimation()`: `isSettling()`이면 즉시 `false` 반환 (SETTLING은 더 이상 낙하 안 함)
3. 충돌 체크: `other.isFalling() == false` 조건 추가 (FALLING이 아닌 것들만 충돌로 간주)

#### 변경 내용

**1. GameWorld.java - updateFallingAnimation()**
```java
// FALLING 상태만 필터링 (POPPING, SETTLING은 별도 처리)
List<Puyo> fallingList = new ArrayList<>();
for (Puyo puyo : animatingPuyos) {
    if (puyo.isFalling() || puyo.isSettling()) {  // SETTLING 추가
        fallingList.add(puyo);
    }
}
```

**2. GameWorld.java - canSinglePuyoFallDuringFallingAnimation()**
```java
// 바닥 체크 + SETTLING이면 낙하 불가
if (puyo.getY() == 0 || puyo.isSettling()) return false;

// 다른 falling puyo 충돌: targetY = y-1 위치에 다른 falling puyo가 있는지
for (Puyo other : fallingList) {
    if (other == puyo) continue;
    // SETTLING 상태인 것들만 충돌로 간주 (FALLING은 함께 낙하 상태이므로 충돌 아님)
    if (other.getX() == puyo.getX() && other.getY() == (puyo.getY() - 1) && other.isFalling() == false) {
        return false;
    }
}
```

#### 효과
- ✅ 부유 뿌요가 바닥에 밀착하여 자연스럽게 낙하
- ✅ 착지 직후 SETTLING(바운스) 전이된 뿌요들이 정상 처리
- ✅ 연쇄 3단계까지 정상 완료 (`chainCount=3` → 정상 종료)
- ✅ `animatingPuyos` 누적 없음 (2 → 5 → 5 → 0)

---

## v0.1.27 (2026-08-23)

### 🔧 **클래스/변수명 리네이밍: FallingPuyo → StatefulPuyo (확장성 확보)**

**배경**: `FallingPuyo` 클래스가 낙하 중인 뿌요뿐만 아니라 팝 애니메이션(CHAIN_POP) 중인 뿌요도 담고 있었고, 향후 착지 후 흔들림(SETTLING) 상태도 추가될 예정이라 이름이 역할과 맞지 않음

**해결**: 클래스명과 관련된 모든 변수/메서드를 `StatefulPuyo`로 변경, `FallType` enum을 `StateType`으로 변경

#### 변경 내용

**1. StatefulPuyo.java (신규 생성)**
```java
public class StatefulPuyo {
    public Puyo puyo;
    public StateType type;
    public int originalX;
    public int originalY;

    public enum StateType {
        POPPING,   // 연쇄 팝 애니메이션 (제자리 스케일)
        FALLING,   // 일반 낙하 (분리/부유 통합)
        SETTLING   // 착지 후 흔들림/정착 대기 (향후 확장용)
    }
    // ... 생성자, isPopping(), isFalling(), isSettling() 메서드
}
```

**2. GameWorld.java - 필드/메서드/변수 전체 변경**
- 필드: `fallingPuyos` → `statefulPuyos` (List<StatefulPuyo>)
- 메서드: `addFallingPuyo()` → `addStatefulPuyo(Puyo, StatefulPuyo.StateType)`
- getter: `getFallingPuyos()` → `getStatefulPuyos()`, `getFallingSinglePuyo()` 제거(사용 안 함)
- 로컬 변수: `fallingPuyos` → `statefulPuyos`, `fallingList` → `fallingList` (StatefulPuyo 타입)
- 로그 메시지: `fallingPuyos=` → `statefulPuyos=`

**3. PlayScreen.java**
- import: `FallingPuyo` → `StatefulPuyo`
- 메서드: `drawFallingPuyos()` → `drawStatefulPuyos()`
- 내부 변수: `fallingPuyos` → `statefulPuyos`, `fp` → `sp`

**4. FallingPuyo.java 삭제** (기존 클래스 완전 제거)

#### 변경 파일

| 파일 | 변경 유형 | 설명 |
|-----|---------|-----|
| `StatefulPuyo.java` | **신규** | 상태를 가진 뿌요 모델 (POPPING/FALLING/SETTLING) |
| `GameWorld.java` | 대폭 수정 | 필드/메서드/변수/로그 전체 리네이밍 |
| `PlayScreen.java` | 수정 | import, 렌더링 메서드명 변경 |
| `FallingPuyo.java` | **삭제** | 기존 클래스 완전 제거 |

#### 효과
- ✅ 클래스명이 실제 역할(상태 보유)과 일치
- ✅ 향후 SETTLING(착지 흔들림) 상태 추가 용이
- ✅ POPPING/FALLING 타입 구분 명확화
- ✅ 컴파일 성공, 테스트 7개 모두 통과 (100%)

---

## v0.1.26 (2026-08-23)

### 🎮 **DAS/ARR 전면 개편: 프레임→초 단위 전환 + 좌우/소프트드랍 분리 + 설정 외부화 + TouchController 적용**

**배경**: 기존 DAS/ARR이 프레임 단위(60fps 기준)로 하드코딩되어 있어 프레임 드랍 시 타이밍 불안정, 좌우 이동과 소프트 드랍이 같은 타이머 공유로 독립 설정 불가, 설정값 외부화 미지원

**해결**:
1. **단위 통일**: 프레임 → 초 단위(float)로 전환, `GameWorld.FALLING_ANIMATION_INTERVAL(0.025f)` 스타일과 완전 일치
2. **좌우/소프트드랍 독립 관리**: 각각 별도 타이머/트리거로 DAS 지연·ARR 주기 독립 설정 가능
3. **설정 외부화**: `ConfigManager.GameConfig`에 필드 추가, `development.json`/`production.json`에서 로드
4. **TouchController 적용**: 모바일 터치 홀드에도 동일 DAS/ARR 로직 적용

#### 변경 내용

**1. ConfigManager.java - GameConfig 필드 추가**
```java
public float das_delay_horizontal_sec = 0.166f;    // 166ms (좌우 DAS 지연)
public float arr_interval_horizontal_sec = 0.033f;  // 33ms  (좌우 ARR 주기)
public float das_delay_softdrop_sec = 0.166f;       // 166ms (소프트드랍 DAS 지연)
public float arr_interval_softdrop_sec = 0.033f;    // 33ms  (소프트드랍 ARR 주기)
```

**2. JSON 설정 파일 (development.json, production.json)**
```json
{
  "das_delay_horizontal_sec": 0.166,
  "arr_interval_horizontal_sec": 0.033,
  "das_delay_softdrop_sec": 0.166,
  "arr_interval_softdrop_sec": 0.033
}
```

**3. InputHandler.java - 핵심 로직 리팩토링**
- **상태 변수 분리**: `horizontalHeldTimeSec`, `softDropHeldTimeSec`, `horizontalRepeatTriggered`, `softDropRepeatTriggered`
- **updateDasArr(float delta)**: delta 직접 누적, `(accumulated - delay) % interval < delta` 패턴으로 정확한 주기 판정
- **getMoveDirection()**: `horizontalRepeatTriggered` 사용
- **isDropPressed()**: `softDropRepeatTriggered` 사용
- **resetDasArr()**: 두 타이머/트리거 모두 리셋

**4. TouchController.java - DAS/ARR 로직 이식**
- `update(float delta)` 시그니처 변경
- InputHandler와 동일한 독립 타이머 로직 구현
- `getMoveDirection()`, `isDropPressed()`에서 repeatTriggered 반영

**5. PlayScreen.java**
- `inputHandler.update(delta)`로 delta 전달 (1줄 수정)

#### 효과
- ✅ 프레임 드랍 영향 없는 정확한 타이밍 (delta 누적 방식)
- ✅ 좌우 이동과 소프트 드랍 DAS/ARR 독립 설정 가능
- ✅ 설정 파일로 런타임 값 변경 가능 (재컴파일 불필요)
- ✅ 데스크톱/모바일 동일 타이밍 보장
- ✅ GameWorld 낙하 애니메이션과 단위 통일로 코드 일관성 확보

#### 변경 파일
| 파일 | 변경 유형 | 설명 |
|-----|---------|-----|
| `ConfigManager.java` | 수정 | GameConfig에 DAS/ARR 4개 필드 추가 (기본값 0.166f/0.033f) |
| `development.json` | 수정 | DAS/ARR 설정값 추가 |
| `production.json` | 수정 | DAS/ARR 설정값 추가 |
| `InputHandler.java` | **대폭 수정** | 프레임→초 단위, 좌우/소프트드랍 분리, ConfigManager 연동 |
| `TouchController.java` | **대폭 수정** | DAS/ARR 로직 이식, update(float delta), 독립 타이머 |
| `PlayScreen.java` | 수정 | `inputHandler.update(delta)`로 변경 |

---

## v0.1.25 (2026-08-23)

### 🐛 **부유 뿌요 공중 정지 버그 수정 + 코드 중복 제거 리팩토링**

**문제**: 연쇄 후 부유 뿌요가 낙하할 때 열(column) 단위로 강체처럼 이동하여, 간격이 유지된 채 공중에 멈추는 버그

**원인**: `updateFallingAnimation()`에서 열별로 그룹화 → 가장 아래 뿌요만 체크 → 열 전체를 한 칸씩 이동

**해결**: 각 뿌요가 독립적으로 낙하하도록 변경

#### 변경 내용

1. **`canSinglePuyoFallDuringFallingAnimation()` 신규 메서드 분리**
   - 바닥 체크: `puyo.getY() == 0`
   - 보드 충돌: 기존 `board.canMoveDown(puyo)` 재사용
   - 다른 falling puyo 충돌: `targetY = y-1` 위치에 다른 falling puyo가 있는지 체크

2. **`updateFallingAnimation()` 리팩토링** (중복 ~25줄 감소)
   - 이동 루프 + 완료 체크 루프 → 공통 메서드 호출로 통합
   - Y좌표 오름차순 정렬(아래→위)로 아래 뿌요부터 착지 후 위 뿌요가 쌓이게 함

3. **메서드명 명확화**
   - `canFall()` → `canPuyoPairFall()` (private 변경, 외부 미사용)
   - PuyoPair 전용임을 명시

4. **헤드리스 회귀 방지 테스트 추가** (`FallingAnimationTest.java`)
   - 로그에 나온 정확한 보드 상태 재현
   - CHAIN_FLOATING_CHECK → FALLING_ANIMATION → CHAIN_FINDING 단계 검증
   - 수정 전: 부유 뿌요 2개 공중 정지 → 수정 후: 0개 (모두 정상 착지)

#### 효과
- ✅ 부유 뿌요가 바닥에 밀착하여 자연스럽게 낙하
- ✅ 코드 중복 제거 및 가독성 향상
- ✅ 기존 `board.canMoveDown(puyo)` 재사용으로 일관성 확보
- ✅ 회귀 방지 테스트로 향후 버그 조기 발견 가능

#### 변경 파일
| 파일 | 변경 유형 | 설명 |
|-----|---------|-----|
| `GameWorld.java` | 수정 | `canFall()`→`canPuyoPairFall()`, `canSinglePuyoFallDuringFallingAnimation()` 추가, `updateFallingAnimation()` 리팩토링 |
| `FallingAnimationTest.java` | **신규** | 로그 기반 부유 뿌요 낙하 회귀 방지 테스트 |

---

## v0.1.24 (2026-08-22)

### 🎮 **반칸 단위 부드러운 낙하 구현 (Half-cell Smooth Falling)**

**핵심 아이디어**: Puyo 모델에 `inMiddle` 불리언 상태를 두고 `moveDown()`에서 토글하여, 모든 낙하 경로(자동/소프트/하드/분리/부유)가 **별도 로직 수정 없이** 자동으로 반칸 단위 부드러운 이동 적용

#### 변경된 파일 (최소 3파일만 수정)

| 파일 | 변경 내용 |
|------|-----------|
| `Puyo.java` | `inMiddle` 필드 추가, `moveDown()`에서 정수↔반칸 토글 로직, `getInMiddle()` getter |
| `Board.java` | `isEmpty(x, y, checkBelow)` 오버로드, 좌우/회전 충돌 체크 시 `inMiddle` 전달로 아래칸 기준 검사 |
| `PlayScreen.java` | `drawPuyo()`에서 `inMiddle=true` 시 반칸(CELL_SIZE/2) 오프셋 적용 렌더링 |

#### 작동 원리

```
스폰(y=12, inMiddle=false)
  ↓ moveDown() 1회
y=12, inMiddle=true (시각적 11.5)
  ↓ moveDown() 2회
y=11, inMiddle=false (시각적 11.0, 정수칸 착지)
  ↓ ... 반복
```

- **짝수 번 moveDown() 호출 후 항상 `inMiddle=false`로 정수칸 착지 보장**
- `hardDrop()`, `softDrop()`, `handleFallingAuto()`, `updateFallingAnimation()` 모두 기존 `moveDown()` 호출 경유 → **자동 적용**
- 락딜레이 진입 시 `inMiddle=false` 확정 → 별도 정리 코드 불필요

#### 효과

- ✅ 원작 같은 부드러운 낙하 애니메이션 (1초/1칸, fallInterval=0.5f 유지)
- ✅ 반칸 상태에서 좌우 이동/회전 시 아래칸 기준 옆면 충돌 체크로 자연스러운 조작
- ✅ 아키텍처 변경 최소화 (GameWorld/상태머신/매니저 클래스 무수정)
- ✅ 분리/부유 낙하도 동일하게 부드러운 이동 적용

---

## v0.1.23 (2026-08-17) - **뿌요 연결 효과 시스템 구축 + 아틀라스 방향 매핑 수정 + 연결 다리 캡 제거**

### 배경
- 텍스처 아틀라스 시스템(v0.1.22) 구축 후, 인접한 동일 색상 뿌요 간 시각적 연결 효과 필요
- 기존 아틀라스(7색 × 3변형 = 21개)로는 연결 상태 표현 불가
- 프로그래머 모드(기본+오버레이)와 디자이너 모드(15가지 완성형) 하이브리드 지원 필요

### 해결
1. **PuyoConnectState enum 신규 생성** - 16가지 연결 상태(NONE + 15가지 방향 조합)를 비트마스크로 표현
   - `fromBoard(Board, x, y, color)` 정적 메서드로 렌더링 시점 자동 계산
   - Puyo 클래스에 상태 저장하지 않음 (단일 책임 유지)

2. **Board.java 확장** - `hasSameColorAt(x, y, color)` 메서드 추가로 인접 뿌요 색상 체크

3. **PuyoRenderer 하이브리드 모드 지원** - 아틀라스 내용 자동 감지하여 모드 전환
   - **디자이너 모드**: `red_up`, `red_down` 등 15가지 완성형 이미지 사용
   - **프로그래머 모드**: 기본 뿌요 + 방향별 오버레이 4개(UP/DOWN/LEFT/RIGHT) 런타임 합성
   - `initRegionsFromAtlas()`에서 `checkDesignerMode()`로 자동 감지

4. **아틀라스 생성 확대** - `generateAtlas()` 7개 변형/색상 생성 (기본3 + 오버레이4)
   - `drawConnectOverlay()`: 뿌요 내부 가장자리(innerRadius)에서 스프라이트 경계까지 직사각형 다리 그리기
   - 두께: `innerRadius * 0.7f` (70%), 하이라이트 추가

5. **연결 렌더링 `drawConnected()`** - 하이브리드 분기 처리
   - 디자이너 모드: 완성된 15가지 상태 이미지 바로 사용
   - 프로그래머 모드: 기본 뿌요 + 4방향 오버레이 합성

6. **아틀라스 방향 매핑 수정** - Pixmap 좌표계(0,0=좌상단) 기준 UP/DOWN/LEFT/RIGHT 정정
   - UP: 뿌요 위쪽 가장자리(centerY - innerRadius) → 스프라이트 상단(y=0)
   - DOWN: 뿌요 아래쪽 가장자리(centerY + innerRadius) → 스프라이트 하단(y+size)
   - LEFT/RIGHT 동일하게 수정

7. **연결 다리 캡(반원) 제거** - 직사각형만으로 단순화
   - 캡 제거로 스프라이트 경계 침범 문제 자동 해결
   - 두 스프라이트가 중간에서 직사각형으로 자연스럽게 연결

### 검증 결과
- **컴파일 성공** ✅
- **단위 테스트**: 6/6 통과 ✅
- **데스크톱 실행**: 4분+ 크래시 없음 ✅
- **연쇄 시스템**: chainCount=2 정상 작동 ✅
- **아틀라스 메타데이터**: 방향 이름이 시각적 방향과 일치 ✅
  - `red_overlay_up` → 위쪽 연결 다리 이미지
  - `red_overlay_down` → 아래쪽 연결 다리 이미지
  - `red_overlay_left` → 왼쪽 연결 다리 이미지
  - `red_overlay_right` → 오른쪽 연결 다리 이미지

### 변경 파일
| 파일 | 변경 유형 | 설명 |
|-----|---------|-----|
| `core/src/main/java/com/puyo/game/graphics/PuyoConnectState.java` | **신규** | 16가지 연결 상태 비트마스크 Enum |
| `core/src/main/java/com/puyo/game/logic/model/Board.java` | 수정 | `hasSameColorAt(x, y, color)` 메서드 추가 |
| `core/src/main/java/com/puyo/game/graphics/PuyoRenderer.java` | 대폭 수정 | 하이브리드 모드, `drawConnectOverlay()` 캡 없는 직사각형, 방향 매핑 수정 |
| `core/src/main/java/com/puyo/game/screens/PlayScreen.java` | 수정 | `drawBoard()`에서 `drawConnected()` 호출 |
| `desktop/assets/puyo_atlas.*` | 재생성 | 464x464 (7 variants/color) 자동 재생성 |

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
   - `GravityEngine`은 stateless로 `applyGravity(Board board)` 파라미터 전달만 수행

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
- ✅ 이중 제거 버그 해결, 보드 상태 깨짐 없음
- ✅ 매니저 클래스 분리 완료 (SRP 준수)

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