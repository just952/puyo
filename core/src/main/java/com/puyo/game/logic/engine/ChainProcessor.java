package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 연쇄 처리를 전담하는 클래스.
 * 상태 머신 기반으로 매칭→팝→중력→부유확인 단계를 순차적으로 처리합니다.
 * 실제 보드 조작(중력, 제거, 배치)은 GameWorld에서 담당합니다.
 */
public class ChainProcessor {

    /**
     * 연쇄 처리 단계
     */
    public enum Phase {
        IDLE,               // 대기 중
        FINDING_MATCHES,    // 매칭 그룹 탐색 중
        WAITING_POP,        // 팝 애니메이션 대기 중
        POP_COMPLETED,      // 팝 완료, 중력 적용 필요
        APPLYING_GRAVITY,   // 중력 적용 완료, 부유 확인 필요
        CHECKING_FLOATING,  // 부유 뿌요 확인 및 낙하 애니메이션 추가 중
        DONE                // 연쇄 완료
    }

    /**
     * 다음에 수행해야 할 액션 (GameWorld가 실행)
     */
    public enum Action {
        NONE,               // 아무 액션 없음
        START_POP,          // 팝 애니메이션 시작 (groups 포함)
        APPLY_GRAVITY,      // 중력 적용
        CHECK_FLOATING,     // 부유 뿌요 확인 및 낙하 애니메이션 시작
        NEXT_CHAIN_STEP     // 다음 연쇄 단계로 (매칭 탐색)
    }

    /**
     * 업데이트 결과
     */
    public static class UpdateResult {
        public boolean done = false;           // 연쇄 완료 여부
        public Action action = Action.NONE;    // 다음 액션
        public List<List<Puyo>> groups = null; // 팝할 그룹 (START_POP 시)
        public List<Puyo> floatingPuyos = null; // 부유 뿌요 (CHECK_FLOATING 시)
        public int chainCount = 0;
        public int totalRemoved = 0;
    }

    private Phase phase = Phase.IDLE;
    private List<List<Puyo>> currentGroups = null;
    private int chainCount = 0;
    private int totalRemoved = 0;

    public ChainProcessor() {
    }

    /**
     * 연쇄 처리 상태 업데이트 (비동기식, 프레임당 1스텝)
     * 보드 조작은 하지 않고 다음에 수행할 액션만 반환합니다.
     * 
     * @param board                   게임 보드
     * @param fallingAnim             팝 애니메이션 매니저 (대기 확인용)
     * @param delta                   프레임 시간
     * @return 업데이트 결과 (다음 액션 포함)
     */
    public UpdateResult update(Board board, FallingAnimationManager fallingAnim, float delta) {
        UpdateResult result = new UpdateResult();
        result.chainCount = chainCount;
        result.totalRemoved = totalRemoved;

        LogUtil.debug("ChainProcessor", "=== update START ===");
        LogUtil.debug("ChainProcessor", "Phase: " + phase +
                ", chainCount=" + chainCount +
                ", totalRemoved=" + totalRemoved +
                ", fallingAnim.isEmpty()=" + fallingAnim.isEmpty() +
                ", fallingAnim.size=" + fallingAnim.getFallingPuyos().size());

        switch (phase) {
            case IDLE:
                LogUtil.debug("ChainProcessor", "Phase IDLE -> FINDING_MATCHES");
                phase = Phase.FINDING_MATCHES;
                result.done = false;
                return result;

            case FINDING_MATCHES:
                LogUtil.debug("ChainProcessor", "Finding next matching groups...");
                List<List<Puyo>> groups = MatchFinder.findAllMatchingGroups(board);

                if (groups.isEmpty()) {
                    // 매칭 없음 - 연쇄 종료
                    LogUtil.debug("ChainProcessor", "No more matches. Chain ending. totalRemoved=" + totalRemoved
                            + ", chainCount=" + chainCount);
                    phase = Phase.DONE;
                    result.done = true;
                    return result;
                }

                // 새 연쇄 단계 시작
                chainCount++;
                currentGroups = groups;
                LogUtil.debug("ChainProcessor", "New chain step: chainCount=" + chainCount + ", groups=" + groups.size());
                for (int i = 0; i < groups.size(); i++) {
                    LogUtil.debug("ChainProcessor",
                            "  Group " + i + ": color=" + groups.get(i).get(0).getColor() + ", size=" + groups.get(i).size());
                }

                // 팝 애니메이션 시작 액션 반환
                LogUtil.debug("ChainProcessor", "Action: START_POP for " + groups.size() + " groups");
                phase = Phase.WAITING_POP;
                result.done = false;
                result.action = Action.START_POP;
                result.groups = groups;
                return result;

            case WAITING_POP:
                // 팝 애니메이션 대기 중이면 스킵 (FallingAnimationManager가 처리 중)
                if (!fallingAnim.isEmpty()) {
                    result.done = false;
                    result.action = Action.NONE;
                    return result; // 아직 팝 애니메이션 진행 중
                }
                // 팝 애니메이션 완료됨
                LogUtil.debug("ChainProcessor", "Pop animation completed");
                
                // 팝 완료: 카운트만
                if (currentGroups != null) {
                    int removed = currentGroups.stream().mapToInt(List::size).sum();
                    totalRemoved += removed;
                    LogUtil.debug("ChainProcessor", "Pop completed, removed " + removed + " puyos");
                }

                // 중력 적용 액션 반환
                LogUtil.debug("ChainProcessor", "Action: APPLY_GRAVITY");
                phase = Phase.APPLYING_GRAVITY;
                result.done = false;
                result.action = Action.APPLY_GRAVITY;
                return result;

            case APPLYING_GRAVITY:
                // 중력 적용 완료 후 부유 확인 액션 반환
                LogUtil.debug("ChainProcessor", "Action: CHECK_FLOATING");
                phase = Phase.CHECKING_FLOATING;
                result.done = false;
                result.action = Action.CHECK_FLOATING;
                return result;

            case CHECKING_FLOATING:
                // 부유 뿌요 낙하 애니메이션 대기 중
                if (!fallingAnim.isEmpty()) {
                    LogUtil.debug("ChainProcessor", "CHECKING_FLOATING: fallingAnim not empty, waiting... size=" + fallingAnim.getFallingPuyos().size());
                    result.done = false;
                    result.action = Action.NONE;
                    return result; // 부유 낙하 애니메이션 진행 중
                }
                
                // 부유 뿌요들 확인 및 낙하 애니메이션 추가 액션 반환
                List<Puyo> floating = board.getAllFloatingPuyos();
                LogUtil.debug("ChainProcessor", "CHECKING_FLOATING: floating.size()=" + floating.size() + ", board:\n" + board.toString());
                if (!floating.isEmpty()) {
                    LogUtil.debug("ChainProcessor", "Found " + floating.size() + " floating puyos, Action: CHECK_FLOATING");
                    for (Puyo p : floating) {
                        LogUtil.debug("ChainProcessor", "  Floating puyo at (" + p.getX() + "," + p.getY() + ") color=" + p.getColor());
                    }
                    phase = Phase.CHECKING_FLOATING; // 재진입 (낙하 완료까지 대기)
                    result.done = false;
                    result.action = Action.CHECK_FLOATING;
                    result.floatingPuyos = floating;
                    LogUtil.debug("ChainProcessor", "CHECKING_FLOATING -> re-enter (waiting for fall animation)");
                    return result;
                }
                
                // 부유 뿌요 없으면 다음 연쇄 단계로
                LogUtil.debug("ChainProcessor", "No floating puyos, Action: NEXT_CHAIN_STEP");
                phase = Phase.FINDING_MATCHES;
                result.done = false;
                result.action = Action.NEXT_CHAIN_STEP;
                return result;

            case DONE:
                result.done = true;
                result.action = Action.NONE;
                return result;

            default:
                result.done = true;
                result.action = Action.NONE;
                return result;
        }
    }

    /**
     * 연쇄 상태 리셋 (새 조각 스폰 시)
     */
    public void reset() {
        phase = Phase.IDLE;
        currentGroups = null;
        chainCount = 0;
        totalRemoved = 0;
    }

    /**
     * 연쇄 처리 시작 (lockPiece 후 호출)
     */
    public void startChain() {
        if (phase == Phase.IDLE) {
            phase = Phase.FINDING_MATCHES;
        }
    }

    /**
     * 연쇄가 진행 중인지 확인 (IDLE이 아니면 진행 중)
     */
    public boolean hasActiveChain() {
        return phase != Phase.IDLE && phase != Phase.DONE;
    }

    /**
     * 연쇄 완료 여부 확인
     */
    public boolean isDone() {
        return phase == Phase.DONE;
    }

    /**
     * 현재 연쇄 횟수 반환
     */
    public int getChainCount() {
        return chainCount;
    }

    /**
     * 총 제거된 뿌요 수 반환
     */
    public int getTotalRemoved() {
        return totalRemoved;
    }
}