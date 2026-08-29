package com.puyo.game.input;

/**
 * 입력 공급자 인터페이스.
 * 플랫폼별 구현체(Desktop/Android)가 이 인터페이스를 구현하며,
 * GameWorld는 이 인터페이스만 의존하여 core 모듈이 platform 독립적임.
 * 향후 텍스트 입력(채팅/검색) 지원을 위해 모드 전환 기능 포함.
 */
public interface InputProvider {
    /**
     * 프레임마다 호출되어 내부 상태(DAS/ARR 타이머, 엣지 감지 등) 갱신.
     * @param delta 프레임 시간 (초)
     */
    void update(float delta);

    /**
     * 이번 프레임의 입력 명령을 반환하고 내부 버퍼를 비움 (polling).
     * 한 프레임에 한 번만 호출되어야 함.
     * @return 입력 명령 객체
     */
    InputCommand pollCommand();

    /**
     * DAS/ARR 상태 리셋 (새 조각 스폰 시 호출).
     * 키가 눌려 있어도 heldTime, repeatTriggered만 초기화하여
     * 첫 프레임 즉시 이동 + DAS 지연 재시작 보장.
     */
    void resetDasArr();

    /** 리소스 해제 */
    void dispose();

    // === 텍스트 입력 지원 (로비/검색 화면용) ===

    /** 현재 입력 모드 반환 */
    InputMode getInputMode();

    /** 입력 모드 변경 (게임 ↔ 텍스트 ↔ UI) */
    void setInputMode(InputMode mode);

    /** 텍스트 입력 리스너 등록 (TEXT_INPUT 모드에서만 작동) */
    void setTextInputListener(TextInputListener listener);

    /** 텍스트 입력 리스너 해제 */
    void clearTextInputListener();
}