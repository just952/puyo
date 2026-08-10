package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 연쇄 처리를 전담하는 클래스.
 * GravityEngine과 MatchFinder를 조율하여 매칭→제거→중력 반복 처리를 수행합니다.
 * 팝 애니메이션 콜백을 지원합니다.
 */
public class ChainProcessor {

    private final GravityEngine gravityEngine;

    public ChainProcessor() {
        this.gravityEngine = new GravityEngine(null); // board는 processChain에서 설정
    }

    /**
     * 연쇄 처리 결과
     */
    public static class ChainResult {
        public int chainCount = 0;
        public int totalRemoved = 0;
        public List<List<Puyo>> allRemovedGroups = new ArrayList<>();
    }

    /**
     * 팝 애니메이션 콜백 인터페이스
     */
    public interface ChainCallback {
        /**
         * 팝 애니메이션 시작 (매칭 그룹 발견 시)
         */
        void onPopStart(List<Puyo> group);

        /**
         * 팝 애니메이션 완료 후 실제 제거
         */
        void onPopComplete(List<Puyo> group);

        /**
         * 중력 적용 완료 (다음 연쇄 체크 전)
         */
        void onGravityComplete();

        /**
         * 연쇄 종료
         */
        void onChainEnd(int totalRemoved, int chainCount);
    }

    /**
     * 콜백 없이 연쇄 처리 (동기식, 즉시 완료) - 테스트 및 동기식 사용용
     */
    public ChainResult processChain(Board board) {
        return processChain(board, null);
    }

    /**
     * 콜백과 함께 연쇄 처리 (동기식, 즉시 완료) - 테스트용
     * 비동기식(processChainStep)과 달리 팝 애니메이션 없이 즉시 처리
     */
    public ChainResult processChain(Board board, ChainCallback callback) {
        ChainResult result = new ChainResult();
        int chainCount = 0;
        int totalRemoved = 0;

        // GravityEngine에 board 설정
        gravityEngine.setBoard(board);

        while (true) {
            // 1. 매칭 그룹 찾기 (static 메서드 사용)
            List<List<Puyo>> groups = MatchFinder.findAllMatchingGroups(board);

            if (groups.isEmpty()) {
                // 매칭 없음 - 연쇄 종료
                break;
            }

            chainCount++;
            int removedThisChain = 0;

            // 2. 팝 애니메이션 시작 콜백
            if (callback != null) {
                for (List<Puyo> group : groups) {
                    callback.onPopStart(group);
                }
            }

            // 3. 팝 애니메이션 완료 콜백 (즉시 실행 - 동기식)
            if (callback != null) {
                for (List<Puyo> group : groups) {
                    callback.onPopComplete(group);
                }
            }

            // 4. 실제 보드에서 제거
            for (List<Puyo> group : groups) {
                removedThisChain += group.size();
                for (Puyo puyo : group) {
                    board.removePuyo(puyo);
                }
                result.allRemovedGroups.add(group);
            }

            totalRemoved += removedThisChain;

            // 5. 중력 적용
            gravityEngine.applyGravity();

            // 6. 중력 완료 콜백
            if (callback != null) {
                callback.onGravityComplete();
            }
        }

        result.chainCount = chainCount;
        result.totalRemoved = totalRemoved;

        // 7. 연쇄 종료 콜백
        if (callback != null) {
            callback.onChainEnd(totalRemoved, chainCount);
        }

        return result;
    }

    /**
     * 연쇄 처리 상태 (단계별 처리를 위해)
     */
    public static class ChainState {
        public int chainCount = 0;
        public int totalRemoved = 0;
        public List<List<Puyo>> currentGroups = null;
        public boolean waitingForPop = false;
    }

    /**
     * 팝 애니메이션을 기다리며 단계별 연쇄 처리 (비동기식)
     * GameWorld.update()에서 fallingAnimationManager가 비어있을 때 호출
     * 
     * @param board                   게임 보드
     * @param state                   연쇄 처리 상태
     * @param callback                콜백
     * @param fallingAnimationManager 팝 애니메이션 매니저 (대기 확인용)
     * @return true면 연쇄 완료(더 이상 처리할 것 없음), false면 팝 애니메이션 대기 중
     */
    public boolean processChainStep(Board board, ChainState state, ChainCallback callback,
            FallingAnimationManager fallingAnimationManager) {
        gravityEngine.setBoard(board);

        LogUtil.debug("ChainProcessor", "=== processChainStep START ===");
        LogUtil.debug("ChainProcessor", "State: chainCount=" + state.chainCount +
                ", totalRemoved=" + state.totalRemoved +
                ", waitingForPop=" + state.waitingForPop +
                ", currentGroups=" + (state.currentGroups != null ? state.currentGroups.size() : "null"));
        LogUtil.debug("ChainProcessor", "Board before:\n" + board.toString());

        // 팝 애니메이션 대기 중이면 스킵 (FallingAnimationManager가 처리 중)
        if (state.waitingForPop) {
            LogUtil.debug("ChainProcessor",
                    "Waiting for pop animation, fallingEmpty=" + fallingAnimationManager.isEmpty());
            if (!fallingAnimationManager.isEmpty()) {
                LogUtil.debug("ChainProcessor", "Pop animation still in progress, returning false");
                return false; // 아직 팝 애니메이션 진행 중
            }
            // 팝 애니메이션 완료됨
            LogUtil.debug("ChainProcessor", "Pop animation completed");
            state.waitingForPop = false;

            // 팝 완료 콜백 (이미 onPopStart에서 보드에서 제거했으므로 여기서는 카운트만)
            if (callback != null) {
                for (List<Puyo> group : state.currentGroups) {
                    LogUtil.debug("ChainProcessor", "Calling onPopComplete for group size=" + group.size());
                    callback.onPopComplete(group);
                }
            }

            // 제거 개수만 카운트 (실제 제거는 onPopStart에서 FallingAnimationManager가 이미 수행)
            state.totalRemoved += state.currentGroups.stream().mapToInt(List::size).sum();

            // 중력 적용
            LogUtil.debug("ChainProcessor", "Applying gravity...");
            gravityEngine.applyGravity();

            // 중력 완료 콜백
            if (callback != null) {
                callback.onGravityComplete();
            }

            state.currentGroups = null;
        }

        // 다음 매칭 그룹 찾기 (static 메서드 사용)
        LogUtil.debug("ChainProcessor", "Finding next matching groups...");
        List<List<Puyo>> groups = MatchFinder.findAllMatchingGroups(board);

        if (groups.isEmpty()) {
            // 연쇄 종료
            LogUtil.debug("ChainProcessor", "No more matches. Chain ending. totalRemoved=" + state.totalRemoved
                    + ", chainCount=" + state.chainCount);
            if (callback != null) {
                callback.onChainEnd(state.totalRemoved, state.chainCount);
            }
            LogUtil.debug("ChainProcessor", "=== processChainStep END (chain complete) ===");
            return true; // 연쇄 완료
        }

        // 새 연쇄 단계 시작
        state.chainCount++;
        state.currentGroups = groups;
        state.waitingForPop = true;

        LogUtil.debug("ChainProcessor", "New chain step: chainCount=" + state.chainCount + ", groups=" + groups.size());
        for (int i = 0; i < groups.size(); i++) {
            LogUtil.debug("ChainProcessor",
                    "  Group " + i + ": color=" + groups.get(i).get(0).getColor() + ", size=" + groups.get(i).size());
        }

        // 팝 애니메이션 시작 콜백
        if (callback != null) {
            for (List<Puyo> group : groups) {
                callback.onPopStart(group);
            }
        }

        LogUtil.debug("ChainProcessor", "=== processChainStep END (waiting for pop) ===");
        return false; // 팝 애니메이션 대기 중
    }
}