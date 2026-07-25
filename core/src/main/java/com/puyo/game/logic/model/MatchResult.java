package com.puyo.game.logic.model;

import java.util.List;

/**
 * Data class representing the result of a match check.
 */
public class MatchResult {
    private final List<Puyo> matchedPuyos;
    private final int chainCount;
    private final int score;

    public MatchResult(List<Puyo> matchedPuyos, int chainCount, int score) {
        this.matchedPuyos = matchedPuyos;
        this.chainCount = chainCount;
        this.score = score;
    }

    public List<Puyo> getMatchedPuyos() {
        return matchedPuyos;
    }

    public int getChainCount() {
        return chainCount;
    }

    public int getScore() {
        return score;
    }

    public int getPuyoCount() {
        return matchedPuyos.size();
    }

    public boolean isEmpty() {
        return matchedPuyos == null || matchedPuyos.isEmpty();
    }
}
