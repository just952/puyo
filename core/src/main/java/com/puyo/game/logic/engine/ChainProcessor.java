package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;
import com.puyo.game.util.LogUtil;

import java.util.ArrayList;
import java.util.List;

/**
 * 연쇄 처리를 전담하는 클래스.
 * GravityEngine과 MatchFinder를 조율하여 매칭→제거→중력 반복 처리를 수행합니다.
 * 상태 머신 기반으로 팝 애니메이션 대기 등을 처리합니다.
 */
public class ChainProcessor {

    private final GravityEngine gravityEngine;

    /**
     * 연쇄 처리 단계
     */
    public enum Phase {
        IDLE,               // 대기 중
        FINDING_MATCHES,    // 매칭 그룹 탐색 중
        WAITING_POP,        // 팝 애니메이션 대기 중
        APPLYING_GRAVITY,   // 중력 적용 중
        CHECKING_FLOATING,  // 부유 뿌요 확인 및 낙하 애니메이션 추가 중
        DONE                // 연쇄 완료
    }

    private Phase phase = Phase.IDLE;
    private List<List<Puyo>> currentGroups = null;
    private int chainCount = 0;
    private int totalRemoved = 0;

    public ChainProcessor() {
        this.gravityEngine = new GravityEngine(null); // board는 update에서 설정
    }

    /**
     * 연쇄 처리 결과 (동기식 processChain용)
     */
    public static class ChainResult {
        public int chainCount = 0;
        public int totalRemoved = 0;
        public List<List<Puyo>> allRemovedGroups = new ArrayList<>();
    }

    /**
     * 콜백 없이 연쇄 처리 (동기식, 즉시 완료) - 테스트 및 동기식 사용용
     */
    public ChainResult processChain(Board board) {
        return processChain(board, null);
    }

    /**
     * 콜백과 함께 연쇄 처리 (동기식, 즉시 완료) - 테스트용
     * 비동기식(update)과 달리 팝 애니메이션 없이 즉시 처리
     */
    public ChainResult processChain(Board board, Object callback) {
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
        }

        ChainResult finalResult = new ChainResult();
        finalResult.chainCount = chainCount;
        finalResult.totalRemoved = totalRemoved;
        return finalResult;
    }

    /**
     * 연쇄 처리 상태 업데이트 (비동기식, 프레임당 1스텝)
     * GameWorld.update()에서 fallingAnimationManager가 비어있을 때 호출
     * 
     * @param board                   게임 보드
     * @param fallingAnim             팝 애니메이션 매니저 (대기 확인용)
     * @param delta                   프레임 시간
     * @return true면 연쇄 완료(더 이상 처리할 것 없음), false면 진행 중
     */
    public boolean update(Board board, FallingAnimationManager fallingAnim, float delta) {
        gravityEngine.setBoard(board);

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
                return false;

            case FINDING_MATCHES:
                LogUtil.debug("ChainProcessor", "Finding next matching groups...");
                List<List<Puyo>> groups = MatchFinder.findAllMatchingGroups(board);

                if (groups.isEmpty()) {
                    // 매칭 없음 - 연쇄 종료
                    LogUtil.debug("ChainProcessor", "No more matches. Chain ending. totalRemoved=" + totalRemoved
                            + ", chainCount=" + chainCount);
                    phase = Phase.DONE;
                    return true; // 연쇄 완료
                }

                // 새 연쇄 단계 시작
                chainCount++;
                currentGroups = groups;
                LogUtil.debug("ChainProcessor", "New chain step: chainCount=" + chainCount + ", groups=" + groups.size());
                for (int i = 0; i < groups.size(); i++) {
                    LogUtil.debug("ChainProcessor",
                            "  Group " + i + ": color=" + groups.get(i).get(0).getColor() + ", size=" + groups.get(i).size());
                }

                // 팝 애니메이션 시작
                LogUtil.debug("ChainProcessor", "Starting pop animation for " + groups.size() + " groups");
                for (List<Puyo> group : groups) {
                    fallingAnim.addChainFalling(board, group);
                }
                phase = Phase.WAITING_POP;
                return false; // 팝 애니메이션 대기 중

            case WAITING_POP:
                // 팝 애니메이션 대기 중이면 스킵 (FallingAnimationManager가 처리 중)
                if (!fallingAnim.isEmpty()) {
                    return false; // 아직 팝 애니메이션 진행 중
                }
                // 팝 애니메이션 완료됨
                LogUtil.debug("ChainProcessor", "Pop animation completed");
                
                // 팝 완료: 보드에서 제거 (이미 onPopStart에서 FallingAnimationManager가 즉시 제거했으므로 여기서는 카운트만)
                if (currentGroups != null) {
                    int removed = currentGroups.stream().mapToInt(List::size).sum();
                    totalRemoved += removed;
                    LogUtil.debug("ChainProcessor", "Pop completed, removed " + removed + " puyos");
                }

                // 중력 적용
                LogUtil.debug("ChainProcessor", "Applying gravity...");
                gravityEngine.applyGravity();
                phase = Phase.CHECKING_FLOATING;
                return false;

            case CHECKING_FLOATING:
                // 부유 뿌요 낙하 애니메이션 대기 중
                if (!fallingAnim.isEmpty()) {
                    LogUtil.debug("ChainProcessor", "CHECKING_FLOATING: fallingAnim not empty, waiting... size=" + fallingAnim.getFallingPuyos().size());
                    return false; // 부유 낙하 애니메이션 진행 중
                }
                
                // 부유 뿌요들 확인 및 낙하 애니메이션 추가
                List<Puyo> floating = board.getAllFloatingPuyos();
                LogUtil.debug("ChainProcessor", "CHECKING_FLOATING: floating.size()=" + floating.size() + ", board:\n" + board.toString());
                if (!floating.isEmpty()) {
                    LogUtil.debug("ChainProcessor", "Found " + floating.size() + " floating puyos, starting fall animation");
                    for (Puyo p : floating) {
                        LogUtil.debug("ChainProcessor", "  Removing floating puyo at (" + p.getX() + "," + p.getY() + ") color=" + p.getColor());
                        board.removePuyo(p);
                    }
                    fallingAnim.addFloatingPuyos(floating);
                    phase = Phase.CHECKING_FLOATING; // 재진입 (낙하 완료까지 대기)
                    LogUtil.debug("ChainProcessor", "CHECKING_FLOATING -> re-enter (waiting for fall animation)");
                    return false;
                }
                
                // 부유 뿌요 없으면 다음 연쇄 단계로
                LogUtil.debug("ChainProcessor", "No floating puyos, phase CHECKING_FLOATING -> FINDING_MATCHES");
                phase = Phase.FINDING_MATCHES;
                return false;

            case DONE:
                return true; // 연쇄 완료

            default:
                return true;
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