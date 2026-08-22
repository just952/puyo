# Puyo Puyo 2 - 변경 이력 (ChangeLog)

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

## v0.1.23 (2026-08-17)

### 🎨 **연결 효과 시스템 (Puyo Connection Effects)**

- `PuyoConnectState` enum 신규 생성 (16가지 연결 상태 비트마스크)
- `Board.java`에 `hasSameColorAt()` 메서드 추가 (인접 동일색 감지용)
- `PuyoRenderer` 하이브리드 모드 지원 (디자이너/프로그래머 모드 자동 감지)
- 아틀라스 7개 변형/색상 생성 (기본 3 + 오버레이 4)
- `drawConnected()` 하이브리드 렌더링 구현
- 아틀라스 방향 매핑 수정 (UP/DOWN/LEFT/RIGHT Pixmap 좌표계 기준)
- 연결 다리 캡(반원) 제거, 직사각형만으로 단순화

---

## v0.1.22 (2026-08-17)

### 🖼️ **텍스처 아틀라스 전환 (Texture Atlas Migration)**

- `PuyoRenderer` 신규 생성: SpriteBatch + 아틀라스, ShapeRenderer 완전 대체
- 아틀라스 파일 core 공통 리소스로 이동 (`core/src/main/resources/assets/`)
- 환경별 로드 전략: PRD=classpath, DEV=local 우선 (핫리로드)
- Gradle JVM 아규먼트 전달 설정 (`desktop/build.gradle`)

---

## v0.1.21 (2026-08-16)

### 🔧 **소프트 드롭/입력/연쇄 상태 관리 개선**

- 소프트 드롭 착지 시 SEPARATION 페이즈 경유 (락딜레이 우회하되 분리 체크)
- InputHandler DAS/ARR 단일 카운터 통합 (6개→3개 변수, anyPressed 플래그)
- ChainManager 신규 생성 (연쇄 상태 캡슐화, LockDelayManager 패턴 적용)

---

## v0.1.20 (2026-08-16)

### ⚙️ **GamePhase 3단계 분리 (FALLING → FALLING_AUTO / LOCK_DELAY / SEPARATION)**

- 단일 책임 원칙, 명시적 상태 전이, 입력 제어 중앙화
- GamePhase enum public 변경 + `getGamePhase()`/`recordLockDelayMove()` 추가
- 자동 낙하 중 락딜레이 로직 완전 제거
- LOCK_DELAY 중 공중 이탈 시 즉시 deactivate() + FALLING_AUTO 복귀

---

## v0.1.19 (2026-08-15)

### 🐛 **공중 락딜레이 비활성화 버그 수정**

- move/rotate/자동낙하 시 공중 탈출 시 deactivate 호출 추가

---

## v0.1.18 (2026-08-15)

### 🔧 **LockDelayManager 상태 관리 리팩토링 + 공중 잠금 버그 수정**

---

## v0.1.17 (2026-08-15)

### 🐛 **다음 뿌요 미리보기/실제 스폰 불일치 버그 수정**

---

## v0.1.16 (2026-08-15)

### 🔄 **Phase/FallType 통합 리팩토링 + 버그 수정**

- 부유 뿌요 순간이동 버그 수정 (즉시 중력 적용 제거)
- Phase 통합: SEPARATING + CHAIN_FLOATING → FALLING_ANIMATION
- FallType 단순화: SEPARATION + FLOATING → FALLING
- 불필요 파라미터 제거 (updateFallingAnimation, collectAndPlaceCompletedFalling)

---

## v0.1.15 (2026-08-12)

### 🏗️ **ChainProcessor 상태 머신 리팩토링 + 결합도 분리**

- Phase/Action/UpdateResult 도입
- 보드 조작 완전 분리 (GameWorld가 유일한 오케스트레이터)
- 순간이동 버그 수정 (중력 한 칸씩 적용)
- 매치 감지 지연 버그 수정 (분리 후 즉시 매치 체크)
- 불필요 코드 삭제 (100+ 줄 정리)
- 결합도 높은 테스트 6개 파일 삭제

---

## v0.1.14 (2026-08-12)

### 🔄 **ChainProcessor 리팩토링 + 버그 수정**

---

## v0.1.8 (2026-08-08)

### ⌨️ **DAS/ARR 구현 + 고스트 충돌 무시**

---

## v0.1.7 (2026-08-07)

### 🔧 **락 딜레이 완전 구현 + 회전/스폰/폰트/네이티브 수정**

---

## v0.1.5 (2026-08-03)

### 📱 **실기기 검증 완료 (갤럭시 S23)**

- libpenguin.so SONAME 패치 성공
- 한글 폰트 정상 적용
- 실기기 정상 실행 확인
- 에셋 구조 정리 완료

---

## v0.1.0 (2026-07-26)

### 🚀 **초기 프로젝트 설정**

- 멀티 모듈 Gradle 설정 (core, desktop, android)
- 코어 로직, 메뉴 시스템, CI 파이프라인 완성