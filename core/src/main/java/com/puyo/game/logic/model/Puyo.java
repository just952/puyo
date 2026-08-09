package com.puyo.game.logic.model;

public class Puyo {
    private final PuyoColor color;
    private int x;
    private int y;
    private boolean alive = true;

    // 애니메이션 상태
    private enum PopState {
        NONE, POPPING
    }

    private PopState popState = PopState.NONE;
    private float popTimer = 0f;
    private static final float POP_DURATION = 0.3f; // 0.3초 동안 팝 애니메이션
    private float popScale = 1.0f;

    public Puyo(PuyoColor color, int x, int y) {
        this.color = color;
        this.x = x;
        this.y = y;
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
     * 단일 뿌요 아래로 이동 (분리 낙하용)
     */
    public void moveDown() {
        this.y--;
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
