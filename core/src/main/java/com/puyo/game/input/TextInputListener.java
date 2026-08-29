package com.puyo.game.input;

/** 텍스트 입력 이벤트 리스너 (IME, 키보드 문자 입력용) */
@FunctionalInterface
public interface TextInputListener {
    /** 문자 입력 시 호출 (IME 합성 완료된 최종 문자) */
    void onTextInput(char character);
    
    /** 백스페이스/삭제 키 */
    default void onBackspace() {}
    
    /** 엔터 키 (전송/확정) */
    default void onEnter() {}
    
    /** 이스케이프 키 (취소) */
    default void onEscape() {}
}