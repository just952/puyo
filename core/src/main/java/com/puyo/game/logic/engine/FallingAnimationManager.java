package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.util.LogUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 팝 애니메이션과 기둥 낙하 애니메이션을 전담하는 클래스.
 * GameWorld의 updateFalling 로직을 분리했습니다.
 * Board 조작은 하지 않고 액션만 반환합니다 (GameWorld가 실행).
 */
public class FallingAnimationManager {

    private static final float SINGLE_FALL_INTERVAL = 0.05f; // 단일 뿌요 낙하 속도 (소프트 드롭 속도)
    private static final float POP_DURATION = 0.3f; // 팝 애니메이션 지속 시간

    private List<FallingPuyo> fallingPuyos = new ArrayList<>();
    private float singleFallTimer = 0f;
    private boolean popWasDone = false;
    private boolean wasPopJustDone = false;

    /**
     * 낙하 처리 시 수행해야 할 액션
     */
    public enum FallAction {
        NONE,               // 아무 액션 없음
        REMOVE_POPPED,      // 팝 완료된 뿌요들 보드에서 제거
        PLACE_SEPARATED,    // 분리 낙하 완료된 뿌요들 보드에 배치
        PLACE_FLOATING,     // 부유 낙하 완료된 뿌요들 보드에 배치
    }

    /**
     * 업데이트 결과 (다음에 수행할 액션 포함)
     */
    public static class UpdateResult {
        public FallAction action = FallAction.NONE;
        public List<Puyo> puyos = null; // 액션 대상 뿌요들
        public boolean done = false;    // 전체 애니메이션 완료 여부
    }

    /**
     * 분리 낙하용 단일 뿌요 추가
     */
    public void addSeparationFalling(Puyo puyo) {
        fallingPuyos.add(new FallingPuyo(puyo, FallingPuyo.FallType.SEPARATION));
    }

    /**
     * 연쇄 팝 애니메이션용 그룹 추가
     * 즉시 보드에서 제거하지 않고 원본 좌표만 저장
     * 제거는 UpdateResult.REMOVE_POPPED 액션으로 GameWorld가 수행
     */
    public void addChainFalling(List<Puyo> group) {
        for (Puyo puyo : group) {
            if (!puyo.isPopping()) {
                puyo.startPop();
            }
            // 원본 보드 좌표 저장 (생성자에서 자동 저장됨)
            FallingPuyo fp = new FallingPuyo(puyo, FallingPuyo.FallType.CHAIN_POP);
            fallingPuyos.add(fp);
            LogUtil.debug("FallingAnim", "☆☆☆☆☆☆☆ addChainFalling: added puyo at (" + puyo.getX() + "," + puyo.getY()
                    + ") color=" + puyo.getColor() + " hash=" + System.identityHashCode(puyo) + " listSize=" + fallingPuyos.size());
        }
    }

    /**
     * 떠있는 뿌요들(연쇄 후 공중에 뜬 기둥) 추가
     */
    public void addFloatingPuyos(List<Puyo> floating) {
        LogUtil.debug("FallingAnim", "addFloatingPuyos: adding " + floating.size() + " floating puyos");
        for (Puyo puyo : floating) {
            fallingPuyos.add(new FallingPuyo(puyo, FallingPuyo.FallType.FLOATING));
            LogUtil.debug("FallingAnim", "  addFloatingPuyos: added puyo at (" + puyo.getX() + "," + puyo.getY()
                    + ") color=" + puyo.getColor() + " hash=" + System.identityHashCode(puyo));
        }
    }

    /**
     * 낙하/팝 애니메이션 업데이트
     * 
     * @param delta 프레임 시간
     * @param board 게임 보드 (낙하 판정용 읽기 전용)
     * @return 업데이트 결과 (액션, 대상 뿌요, 완료 여부 포함)
     */
    public UpdateResult update(float delta, Board board) {
        UpdateResult result = new UpdateResult();

        if (fallingPuyos.isEmpty()) {
            result.done = true;
            return result; // 처리할 것 없음
        }

        // 1. 팝 애니메이션은 매 프레임 업데이트 (부드러운 애니메이션을 위해)
        boolean allPopDone = true;
        for (FallingPuyo fp : fallingPuyos) {
            if (fp.isChainPop()) {
                // 연쇄: 팝 애니메이션 (매 프레임 업데이트)
                boolean popDone = fp.puyo.updatePop(delta);
                if (!popDone) {
                    allPopDone = false;
                }
            }
        }

        // 팝 진행 중이면 이번 프레임은 여기서 종료 (타이머 누적 안 함)
        if (!allPopDone) {
            result.done = false;
            result.action = FallAction.NONE;
            return result;
        }

        // 팝이 방금 완료되었을 때만 타이머 리셋 (한 번만)
        if (wasPopJustDone) {
            singleFallTimer = 0f;
            wasPopJustDone = false;
        } else if (!popWasDone) {
            // 팝이 진행 중이었다가 이번에 완료됨
            wasPopJustDone = true;
        }
        popWasDone = allPopDone;

        // 팝 완료 직후: REMOVE_POPPED 액션 반환 (한 번만)
        if (wasPopJustDone) {
            List<Puyo> poppedPuyos = new ArrayList<>();
            List<FallingPuyo> toRemove = new ArrayList<>();
            for (FallingPuyo fp : fallingPuyos) {
                if (fp.isChainPop()) {
                    poppedPuyos.add(fp.puyo);
                    toRemove.add(fp);
                }
            }
            if (!poppedPuyos.isEmpty()) {
                LogUtil.debug("FallingAnim", "☆☆☆☆☆☆☆ REMOVE_POPPED: returning " + poppedPuyos.size() + " puyos, listSize before clear=" + fallingPuyos.size());
                // CHAIN_POP 엔트리들을 리스트에서 제거 (누적 방지)
                fallingPuyos.removeAll(toRemove);
                LogUtil.debug("FallingAnim", "☆☆☆☆☆☆☆ REMOVE_POPPED: removed CHAIN_POP entries, listSize after clear=" + fallingPuyos.size());
                result.action = FallAction.REMOVE_POPPED;
                result.puyos = poppedPuyos;
                result.done = false;
                return result;
            }
        }

        // 2. 분리/낙하 처리 (SINGLE_FALL_INTERVAL 간격으로)
        singleFallTimer += delta;
        boolean shouldFall = (singleFallTimer >= SINGLE_FALL_INTERVAL);
        if (shouldFall) {
            singleFallTimer = 0f;
            // 분리/낙하 처리: 열(column) 단위로 기둥 낙하
            Map<Integer, List<FallingPuyo>> columns = new HashMap<>();
            for (FallingPuyo fp : fallingPuyos) {
                if (fp.type == FallingPuyo.FallType.SEPARATION || fp.type == FallingPuyo.FallType.FLOATING) {
                    int x = fp.puyo.getX();
                    columns.computeIfAbsent(x, k -> new ArrayList<>()).add(fp);
                }
            }

            // 각 열(column)별로 독립적으로 한 칸 이동 처리
            // 한 열이 멈추더라도 다른 열은 계속 낙하
            for (List<FallingPuyo> column : columns.values()) {
                // 열 내 가장 아래쪽 뿌요(최소 Y)만 체크
                FallingPuyo bottomFp = null;
                int minY = Integer.MAX_VALUE;
                for (FallingPuyo fp : column) {
                    if (fp.puyo.getY() < minY) {
                        minY = fp.puyo.getY();
                        bottomFp = fp;
                    }
                }

                // 가장 아래쪽 뿌요만 체크해서 열 전체 이동 여부 결정
                // 보드 그리드 + 같은 열의 다른 falling puyos 모두 고려
                boolean canColumnFall = bottomFp != null && canFallInColumn(board, column, bottomFp.puyo);

                // 이동 가능하면 열 전체 한 칸 이동
                if (canColumnFall) {
                    for (FallingPuyo fp : column) {
                        fp.puyo.moveDown(); // 한 칸만 이동 (Puyo 상태 변경, Board 조작 아님)
                    }
                }
            }
        }

        // 3. 분리/부유 낙하 완료 체크
        // 열(column)별로 맨 아래 뿌요만 체크해서 완료 여부 결정 (이동 로직과 일치)
        Map<Integer, List<FallingPuyo>> columnsForCheck = new HashMap<>();
        for (FallingPuyo fp : fallingPuyos) {
            if (fp.type == FallingPuyo.FallType.SEPARATION || fp.type == FallingPuyo.FallType.FLOATING) {
                int x = fp.puyo.getX();
                columnsForCheck.computeIfAbsent(x, k -> new ArrayList<>()).add(fp);
            }
        }

        boolean anyCanFall = false;
        for (List<FallingPuyo> column : columnsForCheck.values()) {
            // 열 내 가장 아래쪽 뿌요만 체크
            FallingPuyo bottomFp = null;
            int minY = Integer.MAX_VALUE;
            for (FallingPuyo fp : column) {
                if (fp.puyo.getY() < minY) {
                    minY = fp.puyo.getY();
                    bottomFp = fp;
                }
            }
            if (bottomFp != null) {
                boolean canFall = canFallInColumn(board, column, bottomFp.puyo);
                if (canFall) {
                    anyCanFall = true;
                    break; // 이 열의 맨 아래가 아직 움직일 수 있으면 완료 안 됨
                }
            }
        }

        if (anyCanFall) {
            result.done = false;
            result.action = FallAction.NONE;
            return result; // 아직 낙하 중
        }

        // 4. 모든 낙하 완료: 배치 액션 반환
        List<FallingPuyo> separatedFalling = new ArrayList<>();
        List<FallingPuyo> floatingFalling = new ArrayList<>();
        for (FallingPuyo fp : fallingPuyos) {
            if (fp.type == FallingPuyo.FallType.SEPARATION) {
                separatedFalling.add(fp);
            } else if (fp.type == FallingPuyo.FallType.FLOATING) {
                floatingFalling.add(fp);
            }
        }

        // 분리 낙하 완료된 것부터 배치
        if (!separatedFalling.isEmpty()) {
            List<Puyo> separatedPuyos = new ArrayList<>();
            for (FallingPuyo fp : separatedFalling) {
                separatedPuyos.add(fp.puyo);
            }
            // 처리된 항목들 내부 리스트에서 제거 (무한 루프 방지)
            fallingPuyos.removeAll(separatedFalling);
            
            result.action = FallAction.PLACE_SEPARATED;
            result.puyos = separatedPuyos;
            // 처리 후 리스트가 비었으면 완료 (부유 낙하가 없으면)
            result.done = fallingPuyos.isEmpty();
            return result;
        }

        // 부유 낙하 완료된 것 배치
        if (!floatingFalling.isEmpty()) {
            List<Puyo> floatingPuyos = new ArrayList<>();
            for (FallingPuyo fp : floatingFalling) {
                floatingPuyos.add(fp.puyo);
            }
            // 처리된 항목들 내부 리스트에서 제거
            fallingPuyos.removeAll(floatingFalling);
            
            result.action = FallAction.PLACE_FLOATING;
            result.puyos = floatingPuyos;
            // 처리 후 리스트가 비었으면 완료
            result.done = fallingPuyos.isEmpty();
            return result;
        }

        // 모든 처리 완료 (팝 완료 AND 분리/부유 낙하 완료)
        result.done = true;
        result.action = FallAction.NONE;
        return result;
    }

    /**
     * 낙하 리스트 초기화
     */
    public void clear() {
        fallingPuyos.clear();
        singleFallTimer = 0f;
        popWasDone = false;
        wasPopJustDone = false;
    }

    /**
     * 낙하 중인 뿌요가 있는지 확인
     */
    public boolean isEmpty() {
        return fallingPuyos.isEmpty();
    }

    /**
     * 현재 낙하 중인 모든 뿌요 리스트 반환 (렌더링용)
     */
    public List<FallingPuyo> getFallingPuyos() {
        return new ArrayList<>(fallingPuyos);
    }

    /**
     * 분리 낙하 중인 단일 뿌요 반환 (호환용)
     */
    public Puyo getFallingSinglePuyo() {
        for (FallingPuyo fp : fallingPuyos) {
            if (fp.type == FallingPuyo.FallType.SEPARATION) {
                return fp.puyo;
            }
        }
        return null;
    }

    /**
     * 특정 열에서 특정 뿌요가 한 칸 아래로 이동 가능한지 체크
     * 보드 그리드 + 같은 열의 다른 falling puyos 모두 고려
     */
    private boolean canFallInColumn(Board board, List<FallingPuyo> column, Puyo puyo) {
        int x = puyo.getX();
        int targetY = puyo.getY() - 1;

        if (targetY < 0) {
            return false; // 바닥
        }

        // 보드 그리드 체크
        if (board.getPuyoAt(x, targetY) != null) {
            return false; // 보드에 다른 뿌요가 있음
        }

        // 같은 열의 다른 falling puyos 체크
        for (FallingPuyo fp : column) {
            if (fp != null && fp.puyo != puyo && fp.puyo.getX() == x && fp.puyo.getY() == targetY) {
                return false; // 같은 열의 다른 falling puyo가 있음
            }
        }

        return true;
    }
}