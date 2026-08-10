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
 */
public class FallingAnimationManager {

    private static final float SINGLE_FALL_INTERVAL = 0.05f; // 단일 뿌요 낙하 속도 (소프트 드롭 속도)
    private static final float POP_DURATION = 0.3f; // 팝 애니메이션 지속 시간

    private List<FallingPuyo> fallingPuyos = new ArrayList<>();
    private float singleFallTimer = 0f;

    /**
     * 분리 낙하용 단일 뿌요 추가
     */
    public void addSeparationFalling(Puyo puyo) {
        fallingPuyos.add(new FallingPuyo(puyo, FallingPuyo.FallType.SEPARATION));
    }

    /**
     * 연쇄 팝 애니메이션용 그룹 추가
     * 즉시 보드에서 제거하고 원본 좌표를 저장
     */
    public void addChainFalling(Board board, List<Puyo> group) {
        for (Puyo puyo : group) {
            if (!puyo.isPopping()) {
                puyo.startPop();
            }
            // 원본 보드 좌표 저장 (생성자에서 자동 저장됨)
            FallingPuyo fp = new FallingPuyo(puyo, FallingPuyo.FallType.CHAIN_POP);
            fallingPuyos.add(fp);
            // 즉시 보드에서 제거 - 애니메이션 중 위치 변경되어도 원본 좌표 보존
            board.removePuyo(puyo);
            LogUtil.debug("FallingAnim", "addChainFalling: added puyo at (" + puyo.getX() + "," + puyo.getY()
                    + ") color=" + puyo.getColor() + " hash=" + System.identityHashCode(puyo));
        }
    }

    /**
     * 떠있는 뿌요들(연쇄 후 공중에 뜬 기둥) 추가
     */
    public void addFloatingPuyos(List<Puyo> floating) {
        for (Puyo puyo : floating) {
            fallingPuyos.add(new FallingPuyo(puyo, FallingPuyo.FallType.FLOATING));
            LogUtil.debug("FallingAnim", "addFloatingPuyos: added puyo at (" + puyo.getX() + "," + puyo.getY()
                    + ") color=" + puyo.getColor() + " hash=" + System.identityHashCode(puyo));
        }
    }

    /**
     * 낙하/팝 애니메이션 업데이트
     * 
     * @param delta 프레임 시간
     * @param board 게임 보드
     * @return 모든 처리가 완료되었으면 true, 진행 중이면 false
     */
    public boolean update(float delta, Board board) {
        if (fallingPuyos.isEmpty()) {
            return true; // 처리할 것 없음
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

        // 팝 진행 중이면 이번 프레임은 여기서 종료
        if (!allPopDone) {
            return false;
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
                        fp.puyo.moveDown(); // 한 칸만 이동
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
            if (bottomFp != null && canFallInColumn(board, column, bottomFp.puyo)) {
                return false; // 이 열의 맨 아래가 아직 움직일 수 있으면 완료 안 됨
            }
        }

        // 모든 처리 완료 (팝 완료 AND 분리/부유 낙하 완료)
        return true;
    }

    /**
     * 분리 낙하 완료된 뿌요들을 보드에 배치
     */
    public void placeSeparatedPuyos(Board board) {
        LogUtil.debug("FallingAnim", "=== placeSeparatedPuyos START ===");
        for (FallingPuyo fp : fallingPuyos) {
            if (fp.type == FallingPuyo.FallType.SEPARATION) {
                LogUtil.debug("FallingAnim", "Placing separated puyo at (" + fp.puyo.getX() + "," + fp.puyo.getY()
                        + ") color=" + fp.puyo.getColor() + " hash=" + System.identityHashCode(fp.puyo));
                board.placePuyo(fp.puyo);
            }
        }
        LogUtil.debug("FallingAnim", "=== placeSeparatedPuyos END ===");
    }

    /**
     * 부유 낙하 완료된 뿌요들(연쇄 후 공중에 뜬 기둥)을 보드에 배치
     * 이미 중력이 적용된 최종 위치에 배치
     */
    public void placeFloatingPuyos(Board board) {
        LogUtil.debug("FallingAnim", "=== placeFloatingPuyos START ===");
        for (FallingPuyo fp : fallingPuyos) {
            if (fp.type == FallingPuyo.FallType.FLOATING) {
                LogUtil.debug("FallingAnim", "Placing floating puyo at final (" + fp.puyo.getX() + "," + fp.puyo.getY()
                        + ") color=" + fp.puyo.getColor() + " hash=" + System.identityHashCode(fp.puyo));
                board.placePuyo(fp.puyo);
            }
        }
        LogUtil.debug("FallingAnim", "=== placeFloatingPuyos END ===");
    }

    /**
     * 낙하 리스트 초기화
     */
    public void clear() {
        fallingPuyos.clear();
        singleFallTimer = 0f;
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

    /**
     * 내부 fallingPuyos 리스트 반환 (SeparationManager 등에서 사용)
     * package-private으로 제한
     */
    List<FallingPuyo> getInternalFallingPuyos() {
        return fallingPuyos;
    }
}