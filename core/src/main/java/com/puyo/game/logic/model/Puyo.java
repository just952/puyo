package com.puyo.game.logic.model;

import com.puyo.game.util.LogUtil;

public class Puyo {
    private final PuyoColor color;
    private int x;
    private int y;
    private boolean alive = true;
    private boolean inMiddle = false;

    // 통합 애니메이션 상태
    public enum State {
        NORMAL,      // 보드에 고정된 일반 상태 (기본값)
        FALLING,     // 낙하 중 (현재 쌍, 분리/부유)
        SETTLING,    // 착지 바운스 애니메이션 중
        POPPING      // 연쇄 팝 애니메이션 중
    }

    private State state = State.NORMAL;
    private float animTimer = 0f;
    private float animDuration = 0f;
    private float scaleX = 1.0f;
    private float scaleY = 1.0f;

    // 애니메이션 중 원본 위치 보존용 (낙하 중이 아닐 때만 유효)
    private int animOriginalX = -1;
    private int animOriginalY = -1;

    // 애니메이션별 상수
    private static final float POP_DURATION = 0.3f;
    private static final float SETTLE_DURATION = 0.35f;
    private static final float FALLING_ANIMATION_INTERVAL = 0.025f; // GameWorld와 동일

    public Puyo(PuyoColor color, int x, int y) {
        this.color = color;
        this.x = x;
        this.y = y;
        this.inMiddle = false;
    }

    public PuyoColor getColor() {
        return color;
    }

    public int getX() {
        return x;
    }

    public void setX(int x) {
        this.x = x;
    }

    public int getY() {
        return y;
    }

    public void setY(int y) {
        this.y = y;
    }

    public boolean isAlive() {
        return alive;
    }

    public void kill() {
        alive = false;
    }

    public void revive() {
        alive = true;
    }

    public boolean isInMiddle() {
        return inMiddle;
    }

    // ==========================================
    // 통합 애니메이션 상태 관리
    // ==========================================

    public State getState() {
        return state;
    }

    public void setState(State newState) {
        this.state = newState;
        this.animTimer = 0f;
        this.scaleX = 1.0f;
        this.scaleY = 1.0f;
        
        switch (newState) {
            case POPPING:
                this.animDuration = POP_DURATION;
                break;
            case SETTLING:
                this.animDuration = SETTLE_DURATION;
                break;
            case FALLING:
                this.animDuration = FALLING_ANIMATION_INTERVAL;
                break;
            default:
                this.animDuration = 0f;
        }
    }

    public boolean isAnimating() {
        return state != State.NORMAL;
    }

    public boolean isPopping() {
        return state == State.POPPING;
    }

    public boolean isSettling() {
        return state == State.SETTLING;
    }

    public boolean isFalling() {
        return state == State.FALLING;
    }

    public boolean isNormal() {
        return state == State.NORMAL;
    }

    /**
     * 통합 애니메이션 업데이트
     * @param delta 프레임 시간
     * @return 애니메이션 완료 여부 (true = 완료되어 상태가 NORMAL로 돌아감)
     */
    public boolean updateAnimation(float delta) {
        if (state == State.NORMAL) {
            return true;
        }

        animTimer += delta;
        float progress = animTimer / animDuration;

        if (progress >= 1.0f) {
            animTimer = animDuration;
            scaleX = 1.0f;
            scaleY = 1.0f;
            state = State.NORMAL;
            animTimer = 0f;
            return true; // 애니메이션 완료
        }

        switch (state) {
            case POPPING:
                updatePopAnimation(progress);
                break;
            case SETTLING:
                updateSettleAnimation(progress);
                break;
            case FALLING:
                // FALLING은 GameWorld에서 별도 처리 (타이머 기반 이동)
                break;
            default:
                break;
        }

        return false; // 애니메이션 진행 중
    }

    private void updatePopAnimation(float progress) {
        // 0~0.5: 커짐(1.0 -> 1.3), 0.5~1.0: 작아짐(1.3 -> 0)
        if (progress < 0.5f) {
            scaleX = scaleY = 1.0f + progress * 0.6f; // 1.0 -> 1.3
        } else {
            scaleX = scaleY = 1.3f - (progress - 0.5f) * 2.6f; // 1.3 -> 0
        }
    }

    private void updateSettleAnimation(float progress) {
        // 2회 진동: progress 0.0~1.0을 4구간으로 분할 (각 0.25)
        // 0.0~0.25: 1차 압축 (X확대, Y축소)   1.0->1.3, 1.0->0.7
        // 0.25~0.50: 1차 신장 (X축소, Y확대)  1.3->0.8, 0.7->1.2
        // 0.50~0.75: 2차 압축 (X확대, Y축소)   0.8->1.2, 1.2->0.8
        // 0.75~1.00: 2차 신장/정착 (X축소, Y확대) 1.2->0.9, 0.8->1.1 -> 1.0, 1.0
        
        float phaseProgress = (progress % 0.25f) / 0.25f; // 0~1 within phase
        int phase = (int)(progress / 0.25f); // 0, 1, 2, 3

        switch (phase) {
            case 0: // 1차 압축: 옆으로 퍼지고 납작해짐
                scaleX = 1.0f + phaseProgress * 0.3f; // 1.0 -> 1.3
                scaleY = 1.0f - phaseProgress * 0.3f; // 1.0 -> 0.7
                break;
            case 1: // 1차 신장: 좁아지고 길어짐
                scaleX = 1.3f - phaseProgress * 0.5f; // 1.3 -> 0.8
                scaleY = 0.7f + phaseProgress * 0.5f; // 0.7 -> 1.2
                break;
            case 2: // 2차 압축: 다시 옆으로 퍼짐 (감쇠)
                scaleX = 0.8f + phaseProgress * 0.4f; // 0.8 -> 1.2
                scaleY = 1.2f - phaseProgress * 0.4f; // 1.2 -> 0.8
                break;
            case 3: // 2차 신장/정착: 거의 원상복구
                scaleX = 1.2f - phaseProgress * 0.3f; // 1.2 -> 0.9
                scaleY = 0.8f + phaseProgress * 0.3f; // 0.8 -> 1.1
                break;
        }
    }

    public float getScaleX() {
        return scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    // 호환용 (기존 코드에서 popScale 사용 시)
    public float getPopScale() {
        return scaleX; // 팝은 균일 스케일
    }

    public float getSettleScaleX() {
        return scaleX;
    }

    public float getSettleScaleY() {
        return scaleY;
    }

    public float getAnimDuration() {
        return animDuration;
    }

    // 원본 위치 보존용
    public int getAnimOriginalX() {
        return animOriginalX;
    }

    public int getAnimOriginalY() {
        return animOriginalY;
    }

    public void setAnimOriginalPosition(int x, int y) {
        this.animOriginalX = x;
        this.animOriginalY = y;
    }

    public void clearAnimOriginalPosition() {
        this.animOriginalX = -1;
        this.animOriginalY = -1;
    }

    /**
     * 단일 뿌요 아래로 이동 (분리 낙하용)
     */
    public void moveDown() {
        if (inMiddle) this.y--;
        inMiddle = !inMiddle;
    }

    public boolean getInMiddle() {
        return inMiddle;
    }

    @Override
    public String toString() {
        return "Puyo{" +
                "color=" + color +
                ", x=" + x +
                ", y=" + y +
                ", state=" + state +
                ", inMiddle=" + inMiddle +
                '}';
    }
}
