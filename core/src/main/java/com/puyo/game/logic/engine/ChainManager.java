package com.puyo.game.logic.engine;

import com.puyo.game.logic.model.Board;
import com.puyo.game.logic.model.Puyo;

import java.util.List;

/**
 * 연쇄(Chain) 상태 관리 클래스.
 * LockDelayManager와 유사한 패턴으로 연쇄 관련 상태를 캡슐화.
 * 연쇄 탐색, 팝 그룹 관리, 연쇄 카운트 등을 담당.
 */
public class ChainManager {

    private int chainCount = 0;
    private List<List<Puyo>> currentGroups = null;

    /** 새 연쇄 시작 (초기화) */
    public void startNewChain() {
        chainCount = 0;
        currentGroups = null;
    }

    /** 연쇄 탐색 수행 - 매치된 그룹이 있으면 true 반환 */
    public boolean findChains(Board board) {
        currentGroups = MatchFinder.findAllMatchingGroups(board);
        if (currentGroups.isEmpty()) {
            return false;
        }
        chainCount++;
        return true;
    }

    /** 현재 팝 대상 그룹들 반환 */
    public List<List<Puyo>> getCurrentGroups() {
        return currentGroups;
    }

    /** 현재 연쇄 수 반환 */
    public int getChainCount() {
        return chainCount;
    }

    /** 팝 완료 후 현재 그룹 클리어 */
    public void clearCurrentGroups() {
        currentGroups = null;
    }

    /** 연쇄 진행 중인지 확인 (그룹이 있는 상태) */
    public boolean isChaining() {
        return currentGroups != null && !currentGroups.isEmpty();
    }

    /** 연쇄 종료 여부 확인 (그룹이 없는 상태) */
    public boolean isChainEnded() {
        return currentGroups == null || currentGroups.isEmpty();
    }
}