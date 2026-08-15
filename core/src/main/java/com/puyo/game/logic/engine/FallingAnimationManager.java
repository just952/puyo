package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.util.LogUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 팝 애니메이션과 기둥 낙하 애니메이션을 전담하는 유틸리티 클래스.
 * 상태를 가지지 않는 순수 함수형으로 제공됩니다 (GameWorld가 상태 관리).
 * 모든 메서드는 static으로 제공됩니다.
 */
public class FallingAnimationManager {

    /**
     * 낙하 중인 뿌요 정보를 담는 클래스 (static nested class로 외부에서 직접 사용 가능).
     * 팝 애니메이션용, 분리 낙하용, 기둥 낙하용(부유 뿌요)을 구분합니다.
     */
    public static class FallingPuyo {
        public Puyo puyo;
        public FallType type;

        // 원본 보드 좌표 (애니메이션 중에도 보드 상의 원래 위치 보존용)
        public int originalX;
        public int originalY;

        public enum FallType {
            CHAIN_POP, // 연쇄 팝 애니메이션
            SEPARATION, // 쌍 분리 낙하
            FLOATING // 연쇄 후 부유 뿌요 낙하
        }

        public FallingPuyo(Puyo puyo, FallType type) {
            this.puyo = puyo;
            this.type = type;
            this.originalX = puyo.getX();
            this.originalY = puyo.getY();
        }

        // 호환용
        public boolean isFromSeparation() {
            return type == FallType.SEPARATION;
        }

        public boolean isFloating() {
            return type == FallType.FLOATING;
        }

        public boolean isChainPop() {
            return type == FallType.CHAIN_POP;
        }
    }

    // 액션 타입 (GameWorld 내부에서도 사용 가능하도록 public)
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

    private static final float SINGLE_FALL_INTERVAL = 0.05f; // 단일 뿌요 낙하 속도 (소프트 드롭 속도)
    private static final float POP_DURATION = 0.3f; // 팝 애니메이션 지속 시간

    // 인스턴스 생성 방지
    private FallingAnimationManager() {}

    /**
     * 연쇄 팝 애니메이션 업데이트
     * 
     * @param delta 프레임 시간
     * @param fallingPuyos 낙하 중인 뿌요 리스트 (상태는 외부 관리)
     * @return 모든 팝 애니메이션 완료 여부
     */
    public static boolean updatePop(float delta, List<FallingPuyo> fallingPuyos) {
        boolean allPopDone = true;
        for (FallingPuyo fp : fallingPuyos) {
            if (fp.isChainPop()) {
                boolean popDone = fp.puyo.updatePop(delta);
                if (!popDone) {
                    allPopDone = false;
                }
            }
        }
        return allPopDone;
    }

    /**
     * 팝 완료 후 CHAIN_POP 엔트리들을 리스트에서 제거하고 제거할 뿌요 리스트 반환
     * 
     * @param fallingPuyos 낙하 중인 뿌요 리스트
     * @return 팝 완료되어 보드에서 제거할 뿌요들 (빈 리스트면 팝 완료되지 않음)
     */
    public static List<Puyo> collectAndClearChainPop(List<FallingPuyo> fallingPuyos) {
        List<Puyo> poppedPuyos = new ArrayList<>();
        List<FallingPuyo> toRemove = new ArrayList<>();
        for (FallingPuyo fp : fallingPuyos) {
            if (fp.isChainPop()) {
                poppedPuyos.add(fp.puyo);
                toRemove.add(fp);
            }
        }
        if (!poppedPuyos.isEmpty()) {
            LogUtil.debug("FallingAnim", "☆☆☆☆☆☆☆ collectAndClearChainPop: " + poppedPuyos.size() + " puyos, listSize before=" + fallingPuyos.size());
            fallingPuyos.removeAll(toRemove);
            LogUtil.debug("FallingAnim", "☆☆☆☆☆☆☆ collectAndClearChainPop: removed CHAIN_POP, listSize after=" + fallingPuyos.size());
        }
        return poppedPuyos;
    }

    /**
     * 분리/부유 낙하 업데이트 (한 칸 이동)
     * 
     * @param delta 프레임 시간
     * @param board 게임 보드
     * @param fallingPuyos 낙하 중인 뿌요 리스트
     * @param fallTimer 낙하 타이머 (참조로 업데이트됨)
     * @param fallInterval 낙하 간격
     * @return 아직 낙하 중인 열이 있으면 true, 모두 완료면 false
     */
    public static boolean updateSeparationAndFloatingFalling(float delta, Board board, List<FallingPuyo> fallingPuyos,
                                                              float[] fallTimer, float fallInterval) {
        fallTimer[0] += delta;
        if (fallTimer[0] < fallInterval) {
            return true; // 아직 시간 안 됨, 낙하 중으로 간주
        }
        fallTimer[0] = 0f;

        // 분리/낙하 처리: 열(column) 단위로 기둥 낙하
        Map<Integer, List<FallingPuyo>> columns = new HashMap<>();
        for (FallingPuyo fp : fallingPuyos) {
            if (fp.type == FallingPuyo.FallType.SEPARATION || fp.type == FallingPuyo.FallType.FLOATING) {
                int x = fp.puyo.getX();
                columns.computeIfAbsent(x, k -> new ArrayList<>()).add(fp);
            }
        }

        // 각 열(column)별로 독립적으로 한 칸 이동 처리
        boolean anyMoved = false;
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
            if (bottomFp != null && canFallInColumn(board, column, bottomFp.puyo)) {
                for (FallingPuyo fp : column) {
                    fp.puyo.moveDown();
                }
                anyMoved = true;
            }
        }

        // 이동했으면 아직 낙하 중
        if (anyMoved) {
            return true;
        }

        // 아무도 이동 못 했으면 완료 체크 (아래 canFallInColumn과 동일 로직으로 재확인)
        boolean anyCanFall = false;
        for (List<FallingPuyo> column : columns.values()) {
            FallingPuyo bottomFp = null;
            int minY = Integer.MAX_VALUE;
            for (FallingPuyo fp : column) {
                if (fp.puyo.getY() < minY) {
                    minY = fp.puyo.getY();
                    bottomFp = fp;
                }
            }
            if (bottomFp != null && canFallInColumn(board, column, bottomFp.puyo)) {
                anyCanFall = true;
                break;
            }
        }

        return anyCanFall; // true면 아직 낙하 중, false면 완료
    }

    /**
     * 분리/부유 낙하 완료 시 배치할 뿌요들 수집 및 리스트에서 제거
     * 
     * @param fallingPuyos 낙하 중인 뿌요 리스트
     * @param placeSeparated 분리 완료 뿌요 리스트 (out parameter)
     * @param placeFloating 부유 완료 뿌요 리스트 (out parameter)
     * @return 처리할 것이 있으면 true
     */
    public static boolean collectCompletedFalling(List<FallingPuyo> fallingPuyos,
                                                   List<Puyo> placeSeparated,
                                                   List<Puyo> placeFloating) {
        List<FallingPuyo> separatedFalling = new ArrayList<>();
        List<FallingPuyo> floatingFalling = new ArrayList<>();
        for (FallingPuyo fp : fallingPuyos) {
            if (fp.type == FallingPuyo.FallType.SEPARATION) {
                separatedFalling.add(fp);
            } else if (fp.type == FallingPuyo.FallType.FLOATING) {
                floatingFalling.add(fp);
            }
        }

        boolean hasAny = false;
        if (!separatedFalling.isEmpty()) {
            for (FallingPuyo fp : separatedFalling) {
                placeSeparated.add(fp.puyo);
            }
            fallingPuyos.removeAll(separatedFalling);
            hasAny = true;
        }
        if (!floatingFalling.isEmpty()) {
            for (FallingPuyo fp : floatingFalling) {
                placeFloating.add(fp.puyo);
            }
            fallingPuyos.removeAll(floatingFalling);
            hasAny = true;
        }
        return hasAny;
    }

    /**
     * 특정 열에서 특정 뿌요가 한 칸 아래로 이동 가능한지 체크
     * 보드 그리드 + 같은 열의 다른 falling puyos 모두 고려
     */
    private static boolean canFallInColumn(Board board, List<FallingPuyo> column, Puyo puyo) {
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
