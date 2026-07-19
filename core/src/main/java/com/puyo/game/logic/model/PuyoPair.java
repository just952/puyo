package com.puyo.game.logic.model;

/**
 * 플레이어가 조작하는 두 개의 뿌요 쌍을 관리하는 클래스입니다.
 * puyo1은 회전의 중심이 되는 축(Pivot)이며, puyo2는 축을 중심으로 회전하는 뿌요입니다.
 */
public class PuyoPair {
    private final Puyo puyo1;
    private final Puyo puyo2;
    private int offsetX;  // 중심 축(puyo1)의 X 오프셋 (열 위치)
    private int offsetY;  // 중심 축(puyo1)의 Y 오프셋 (행 위치)
    private int rotation; // 0: UP, 1: RIGHT, 2: DOWN, 3: LEFT

    public PuyoPair(Puyo p1, Puyo p2) {
        this.puyo1 = p1;
        this.puyo2 = p2;
        this.offsetX = 2; // 보통 가로 6열 중 중앙 부근에서 생성
        this.offsetY = 11; // 보드 상단(11행)에서 생성
        this.rotation = 0; // 세로 배치(UP)
    }

    public Puyo getPuyo1() { return puyo1; }
    public Puyo getPuyo2() { return puyo2; }

    public int getX1(int boardX, int boardY) { return boardX + offsetX; }
    public int getY1(int boardY) { return boardY + offsetY; }
    
    /**
     * 회전 상태에 따라 동반 뿌요(puyo2)의 X 좌표를 반환합니다.
     */
    public int getX2(int boardX, int boardY, int currentRotation) {
        int x1 = getX1(boardX, boardY);
        switch (currentRotation) {
            case 1: return x1 + 1; // RIGHT
            case 3: return x1 - 1; // LEFT
            case 0: // UP
            case 2: // DOWN
            default: return x1;
        }
    }
    
    /**
     * 회전 상태에 따라 동반 뿌요(puyo2)의 Y 좌표를 반환합니다.
     */
    public int getY2(int boardY, int currentRotation) {
        int y1 = boardY + offsetY;
        switch (currentRotation) {
            case 0: return y1 + 1; // UP
            case 2: return y1 - 1; // DOWN
            case 1: // RIGHT
            case 3: // LEFT
            default: return y1;
        }
    }

    public int getX2(int boardX, int boardY) {
        return getX2(boardX, boardY, this.rotation);
    }

    public int getY2(int boardY) {
        return getY2(boardY, this.rotation);
    }

    // 실제 게임 로직(Controller)에서 호출할 위치 업데이트용
    public void setPosition(int offsetX, int offsetY, int rotation) {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.rotation = (rotation % 4 + 4) % 4; // 0 ~ 3 사이로 안전하게 정규화
    }

    public void rotateClockwise() {
        this.rotation = (this.rotation + 1) % 4;
    }

    public void rotateCounterClockwise() {
        this.rotation = (this.rotation + 3) % 4;
    }

    public int getOffsetX() { return offsetX; }
    public int getOffsetY() { return offsetY; }
    public int getRotation() { return rotation; }
}
