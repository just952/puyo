package com.puyo.game.logic.model;

public class Puyo {
    private final PuyoColor color;
    private int x;
    private int y;
    private boolean alive = true;
    private boolean inMiddle = false;

    // 애니메이션 상태
    private enum PopState {
        NONE, POPPING
    }

    private PopState popState = PopState.NONE;
    private float popTimer = 0f;
    private static final float POP_DURATION = 0.3f; // 0.3초 동안 팝 애니메이션
    private float popScale = 1.0f;

    // 착지 바운스 애니메이션 상태 (Squash & Stretch 2회 진동)
    private enum SettleState {
        NONE, SETTLING
    }

    private SettleState settleState = SettleState.NONE;
    private float settleTimer = 0f;
    private static final float SETTLE_DURATION = 0.35f; // 2회 바운스 총 시간
    private float settleScaleX = 1.0f;
    private float settleScaleY = 1.0f;

    public Puyo(PuyoColor color, int x, int y) {
        this.color = color;
        this.x = x;
        this.y = y;
        this.inMiddle=false;
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

    /**
     * 팝 애니메이션 시작 (제거될 때 호출)
     */
    public void startPop() {
        this.popState = PopState.POPPING;
        this.popTimer = 0f;
        this.popScale = 1.0f;
    }

    /**
     * 팝 애니메이션 업데이트
     * 
     * @param delta 프레임 시간
     * @return 애니메이션 완료 여부
     */
    public boolean updatePop(float delta) {
        if (popState != PopState.POPPING)
            return true;

        popTimer += delta;
        float progress = popTimer / POP_DURATION;

        if (progress >= 1.0f) {
            popTimer = POP_DURATION;
            popScale = 0f;
            popState = PopState.NONE;
            return true; // 애니메이션 완료
        }

        // 0~0.5: 커짐(1.0 -> 1.3), 0.5~1.0: 작아짐(1.3 -> 0)
        if (progress < 0.5f) {
            popScale = 1.0f + progress * 0.6f; // 1.0 -> 1.3
        } else {
            popScale = 1.3f - (progress - 0.5f) * 2.6f; // 1.3 -> 0
        }

        return false; // 애니메이션 진행 중
    }

    public float getPopScale() {
        return popScale;
    }

    public boolean isPopping() {
        return popState == PopState.POPPING;
    }

    /**
     * 착지 바운스 애니메이션 시작 (착지 시 호출)
     * Squash & Stretch: X축과 Y축이 180도 위상 차로 교차 진동 (2회)
     */
    public void startSettle() {
        this.settleState = SettleState.SETTLING;
        this.settleTimer = 0f;
        this.settleScaleX = 1.0f;
        this.settleScaleY = 1.0f;
    }

    /**
     * 착지 바운스 애니메이션 업데이트
     * 
     * @param delta 프레임 시간
     * @return 애니메이션 완료 여부
     */
    public boolean updateSettle(float delta) {
        if (settleState != SettleState.SETTLING)
            return true;

        settleTimer += delta;
        float progress = settleTimer / SETTLE_DURATION;

        if (progress >= 1.0f) {
            settleTimer = SETTLE_DURATION;
            settleScaleX = 1.0f;
            settleScaleY = 1.0f;
            settleState = SettleState.NONE;
            return true; // 애니메이션 완료
        }

        // 2회 진동: progress 0.0~1.0을 4구간으로 분할 (각 0.25)
        // 0.0~0.25: 1차 압축 (X확대, Y축소)   1.0->1.3, 1.0->0.7
        // 0.25~0.50: 1차 신장 (X축소, Y확대)  1.3->0.8, 0.7->1.2
        // 0.50~0.75: 2차 압축 (X확대, Y축소)   0.8->1.2, 1.2->0.8
        // 0.75~1.00: 2차 신장/정착 (X축소, Y확대) 1.2->0.9, 0.8->1.1 -> 1.0, 1.0
        
        float phaseProgress = (progress % 0.25f) / 0.25f; // 0~1 within phase
        int phase = (int)(progress / 0.25f); // 0, 1, 2, 3

        switch (phase) {
            case 0: // 1차 압축: 옆으로 퍼지고 납작해짐
                settleScaleX = 1.0f + phaseProgress * 0.3f; // 1.0 -> 1.3
                settleScaleY = 1.0f - phaseProgress * 0.3f; // 1.0 -> 0.7
                break;
            case 1: // 1차 신장: 좁아지고 길어짐
                settleScaleX = 1.3f - phaseProgress * 0.5f; // 1.3 -> 0.8
                settleScaleY = 0.7f + phaseProgress * 0.5f; // 0.7 -> 1.2
                break;
            case 2: // 2차 압축: 다시 옆으로 퍼짐 (감쇠)
                settleScaleX = 0.8f + phaseProgress * 0.4f; // 0.8 -> 1.2
                settleScaleY = 1.2f - phaseProgress * 0.4f; // 1.2 -> 0.8
                break;
            case 3: // 2차 신장/정착: 거의 원상복구
                settleScaleX = 1.2f - phaseProgress * 0.3f; // 1.2 -> 0.9
                settleScaleY = 0.8f + phaseProgress * 0.3f; // 0.8 -> 1.1
                break;
        }

        return false; // 애니메이션 진행 중
    }

    public float getSettleScaleX() {
        return settleScaleX;
    }

    public float getSettleScaleY() {
        return settleScaleY;
    }

    public boolean isSettling() {
        return settleState == SettleState.SETTLING;
    }

    /**
     * 단일 뿌요 아래로 이동 (분리 낙하용)
     */
    public void moveDown() {
        if ( inMiddle ) this.y--;

        inMiddle=!inMiddle;
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
                ", alive=" + alive +
                ", popping=" + (popState == PopState.POPPING) +
                '}';
    }
}
